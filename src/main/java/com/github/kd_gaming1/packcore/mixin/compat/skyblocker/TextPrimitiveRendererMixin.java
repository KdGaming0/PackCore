package com.github.kd_gaming1.packcore.mixin.compat.skyblocker;

import com.github.kd_gaming1.packcore.util.CaxtonFontDetector;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Dynamically gates Skyblocker's cached Caxton pipeline references inside
 * {@code TextPrimitiveRenderer}.
 *
 * <p>Skyblocker caches the results of {@code CaxtonCompatibility.getTextPipeline()}
 * and {@code CaxtonCompatibility.getSeeThroughTextPipeline()} into
 * {@code static final} fields ({@code CAXTON_NORMAL}, {@code CAXTON_SEE_THROUGH})
 * at class-load time. When Caxton is installed but no Caxton font pack is
 * selected, those fields still hold real pipeline objects, causing white-box
 * rendering.
 *
 * <p>Rather than mutating those {@code static final} fields (disallowed for
 * trusted final on Java 16+), this mixin redirects every GETSTATIC of the
 * two fields. When {@link CaxtonFontDetector#isActive()} is {@code false},
 * the redirect returns {@code null} so Skyblocker's own null-check falls
 * through to vanilla rendering. When a Caxton font <em>is</em> active, the
 * redirect returns the real cached value — no restart required.
 *
 * <p>Both redirects use {@code require = 0, expect = 0}: if Skyblocker
 * refactors and either field disappears, the mixin silently no-ops rather
 * than crashing at startup.
 *
 * <p>Applied only when both {@code skyblocker} and {@code caxton} are loaded
 * (gated by {@code PackCoreCompatMixinPlugin}).
 */
@Mixin(targets = "de.hysky.skyblocker.utils.render.primitive.TextPrimitiveRenderer", remap = false)
public class TextPrimitiveRendererMixin {

    private static final String TPR_DESC =
            "Lde/hysky/skyblocker/utils/render/primitive/TextPrimitiveRenderer;";
    private static final String PIPELINE_DESC =
            "Lcom/mojang/blaze3d/pipeline/RenderPipeline;";

    private static final MethodHandle CAXTON_NORMAL_GETTER = resolveGetter("CAXTON_NORMAL");
    private static final MethodHandle CAXTON_SEE_THROUGH_GETTER = resolveGetter("CAXTON_SEE_THROUGH");

    @Redirect(
            method = "*",
            at = @At(
                    value = "FIELD",
                    target = TPR_DESC + "CAXTON_NORMAL:" + PIPELINE_DESC,
                    opcode = Opcodes.GETSTATIC
            ),
            require = 0,
            expect = 0,
            remap = false
    )
    private static RenderPipeline packcore$gateCaxtonNormal() {
        if (!CaxtonFontDetector.isActive()) return null;
        return invokeGetter(CAXTON_NORMAL_GETTER);
    }

    @Redirect(
            method = "*",
            at = @At(
                    value = "FIELD",
                    target = TPR_DESC + "CAXTON_SEE_THROUGH:" + PIPELINE_DESC,
                    opcode = Opcodes.GETSTATIC
            ),
            require = 0,
            expect = 0,
            remap = false
    )
    private static RenderPipeline packcore$gateCaxtonSeeThrough() {
        if (!CaxtonFontDetector.isActive()) return null;
        return invokeGetter(CAXTON_SEE_THROUGH_GETTER);
    }

    private static MethodHandle resolveGetter(String name) {
        try {
            Class<?> tpr = Class.forName(
                    "de.hysky.skyblocker.utils.render.primitive.TextPrimitiveRenderer");
            return MethodHandles.lookup().findStaticGetter(tpr, name, RenderPipeline.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static RenderPipeline invokeGetter(MethodHandle getter) {
        if (getter == null) return null;
        try {
            return (RenderPipeline) getter.invokeExact();
        } catch (Throwable ignored) {
            return null;
        }
    }
}