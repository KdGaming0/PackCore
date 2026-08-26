package com.github.kd_gaming1.packcore.mixin.compat;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Mixin config plugin for {@code packcore.compat.mixins.json}.
 *
 * <p>Gates each compat mixin on the presence of its required mods:
 * <ul>
 *   <li>Mixins under the {@code .skyblocker.*} subpackage require both
 *       {@code skyblocker} and {@code caxton}.</li>
 *   <li>Mixins under the {@code .caxton.*} subpackage require only
 *       {@code caxton}.</li>
 *   <li>Mixins under the {@code .bobby.*} subpackage require the affected
 *       Bobby {@code 5.2.13+mc26.1} release.</li>
 * </ul>
 *
 * <p>Any other mixin in the config is applied unconditionally — though at
 * the time of writing none exist.
 */
public class PackCoreCompatMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER =
            LoggerFactory.getLogger("PackCore/CompatMixinPlugin");

    private static final String BASE_PACKAGE =
            "com.github.kd_gaming1.packcore.mixin.compat.";
    private static final String SKYBLOCKER_PREFIX = BASE_PACKAGE + "skyblocker.";
    private static final String CAXTON_PREFIX = BASE_PACKAGE + "caxton.";
    private static final String BOBBY_PREFIX = BASE_PACKAGE + "bobby.";
    private static final String AFFECTED_BOBBY_VERSION = "5.2.13+mc26.1";

    private boolean skyblockerPresent;
    private boolean caxtonPresent;
    private boolean bobbyCompatRequired;

    @Override
    public void onLoad(String mixinPackage) {
        FabricLoader loader = FabricLoader.getInstance();
        skyblockerPresent = loader.isModLoaded("skyblocker");
        caxtonPresent = loader.isModLoaded("caxton");
        bobbyCompatRequired = loader.getModContainer("bobby")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .filter(AFFECTED_BOBBY_VERSION::equals)
                .isPresent();

        LOGGER.info("Compat mixin gating: skyblocker={}, caxton={}, bobbyFixes={}",
                skyblockerPresent, caxtonPresent, bobbyCompatRequired);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(SKYBLOCKER_PREFIX)) {
            return skyblockerPresent && caxtonPresent;
        }
        if (mixinClassName.startsWith(CAXTON_PREFIX)) {
            return caxtonPresent;
        }
        if (mixinClassName.startsWith(BOBBY_PREFIX)) {
            return bobbyCompatRequired;
        }
        return true;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return List.of(); }
    @Override
    public void preApply(
            String unusedTargetClassName,
            ClassNode unusedTargetClass,
            String unusedMixinClassName,
            IMixinInfo unusedMixinInfo
    ) {}

    @Override
    public void postApply(
            String unusedTargetClassName,
            ClassNode unusedTargetClass,
            String unusedMixinClassName,
            IMixinInfo unusedMixinInfo
    ) {}
}
