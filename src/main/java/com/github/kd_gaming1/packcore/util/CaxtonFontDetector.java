package com.github.kd_gaming1.packcore.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Detects whether Caxton is actively using a custom typeface for the default
 * Minecraft font ({@code minecraft:default}).
 *
 * <p>Skyblocker integration is handled entirely by mixins
 * ({@code CaxtonCompatibilityMixin} and {@code TextPrimitiveRendererMixin}),
 * so this class only needs to expose {@link #isActive()} for them to read.
 *
 * <p>The probe caches Caxton classes and methods once they resolve
 * successfully, while still fetching Minecraft's current font renderer,
 * CaxtonTextRenderer, and FontStorage dynamically on each recompute. This
 * avoids latching transient Caxton state failures while keeping the hot
 * reflection path small.
 */
public final class CaxtonFontDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger("PackCore/CaxtonFontDetector");

    private static final int PROBE_CODEPOINT = 'A';

    /** Updated by {@link #recompute()}; read freely by any thread. */
    private static volatile boolean active = false;

    private CaxtonFontDetector() {}

    public static boolean isActive() {
        return active;
    }

    /**
     * Re-probe Caxton's font state and update the cached flag.
     * <p>Should be called on the render thread (e.g. at the TAIL of
     * {@code FontSet.setFonts}) so the probe sees a fully-installed font list.
     */
    public static void recompute() {
        boolean prev = active;
        boolean next = probe();
        active = next;
        if (prev != next) {
            LOGGER.info("Caxton active state changed: {} -> {}", prev, next);
        }
        LOGGER.debug("Caxton probe: prev={}, next={}", prev, next);
    }

    /**
     * Probes Caxton's font storage to determine if a Caxton typeface is
     * providing glyphs for the default font.
     */
    private static boolean probe() {
        if (!FabricLoader.getInstance().isModLoaded("caxton")) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) {
            return false;
        }

        CaxtonProbeHandles handles = CaxtonProbeHandles.resolve();
        if (handles == null) {
            return false;
        }

        try {
            Object ctr = handles.getCaxtonTextRenderer.invoke(mc.font);
            if (ctr == null) {
                LOGGER.warn("CaxtonTextRenderer is null");
                return false;
            }

            @SuppressWarnings("unchecked")
            Function<Object, Object> fsa =
                    (Function<Object, Object>) handles.getFontStorageAccessor.invoke(ctr);

            Identifier defaultFontId = Identifier.withDefaultNamespace("default");
            Object fontStorage = fsa.apply(defaultFontId);
            if (fontStorage == null) {
                LOGGER.warn("FontStorage for minecraft:default is null");
                return false;
            }

            Object glyphResult = handles.getCaxtonGlyph.invoke(
                    fontStorage, PROBE_CODEPOINT, false, Style.EMPTY);
            if (glyphResult == null) {
                return false;
            }

            if (handles.isCaxton != null) {
                return (boolean) handles.isCaxton.invoke(glyphResult);
            }

            if (handles.isLegacy != null) {
                return !(boolean) handles.isLegacy.invoke(glyphResult);
            }

            return handles.getCaxtonFont.invoke(glyphResult) != null;

        } catch (Exception e) {
            // Treat unexpected probe failures as inactive to avoid false-positive "active=true".
            LOGGER.warn("Probe threw unexpected exception — treating as inactive", e);
            return false;
        }
    }

    private static final class CaxtonProbeHandles {

        private static volatile CaxtonProbeHandles cached = null;

        private final Method getCaxtonTextRenderer;
        private final Method getFontStorageAccessor;
        private final Method getCaxtonGlyph;
        private final Method isCaxton;
        private final Method isLegacy;
        private final Method getCaxtonFont;

        private CaxtonProbeHandles(
                Method getCaxtonTextRenderer,
                Method getFontStorageAccessor,
                Method getCaxtonGlyph,
                Method isCaxton,
                Method isLegacy,
                Method getCaxtonFont
        ) {
            this.getCaxtonTextRenderer = getCaxtonTextRenderer;
            this.getFontStorageAccessor = getFontStorageAccessor;
            this.getCaxtonGlyph = getCaxtonGlyph;
            this.isCaxton = isCaxton;
            this.isLegacy = isLegacy;
            this.getCaxtonFont = getCaxtonFont;
        }

        private static CaxtonProbeHandles resolve() {
            CaxtonProbeHandles handles = cached;
            if (handles != null) {
                return handles;
            }

            try {
                Class<?> hasCaxtonClass =
                        Class.forName("xyz.flirora.caxton.render.HasCaxtonTextRenderer");
                Class<?> caxtonTextRendererClass =
                        Class.forName("xyz.flirora.caxton.render.CaxtonTextRenderer");
                Class<?> caxtonFontStorageClass =
                        Class.forName("xyz.flirora.caxton.font.CaxtonFontStorage");
                Class<?> glyphResultClass =
                        Class.forName("xyz.flirora.caxton.font.CaxtonGlyphResult");

                handles = new CaxtonProbeHandles(
                        hasCaxtonClass.getMethod("getCaxtonTextRenderer"),
                        caxtonTextRendererClass.getMethod("getFontStorageAccessor"),
                        caxtonFontStorageClass.getMethod(
                                "getCaxtonGlyph", int.class, boolean.class, Style.class),
                        optionalMethod(glyphResultClass, "isCaxton"),
                        optionalMethod(glyphResultClass, "isLegacy"),
                        optionalMethod(glyphResultClass, "getCaxtonFont")
                );

                cached = handles;
                return handles;
            } catch (ClassNotFoundException e) {
                LOGGER.warn("Caxton class not found — is caxton loaded correctly?", e);
                return null;
            } catch (Exception e) {
                LOGGER.warn("Failed to resolve Caxton probe handles", e);
                return null;
            }
        }

        private static Method optionalMethod(Class<?> owner, String name) {
            try {
                return owner.getMethod(name);
            } catch (NoSuchMethodException ignored) {
                return null;
            }
        }
    }
}