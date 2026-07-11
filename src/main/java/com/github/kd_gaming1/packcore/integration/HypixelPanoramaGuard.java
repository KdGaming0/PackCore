package com.github.kd_gaming1.packcore.integration;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link PackResources} delegate that hides PackCore's branded title-screen panorama from a wrapped
 * pack, letting our mod-resource panorama show through instead.
 *
 * <p>PackCore ships its panorama at the vanilla path {@code minecraft:textures/gui/title/background/…}.
 * Every resource pack sits above mod resources in Minecraft's load stack, so a pack that ships its own
 * files there overrides ours. This guard wraps the Hypixel server pack (see {@code PackMixin}) so those
 * specific textures report as absent — Minecraft then falls through to the next pack, i.e. our panorama.
 * Every other resource in the wrapped pack is forwarded untouched, so the pack still applies fully.
 *
 * <p>Panoramas only render on the title screen, so hiding these files has no in-game visual effect.
 */
public final class HypixelPanoramaGuard implements PackResources {

    /** Client-resource path prefix of the branded title background; nothing else lives in this folder. */
    private static final String TITLE_BACKGROUND_PREFIX = "textures/gui/title/background/";

    private final PackResources delegate;

    public HypixelPanoramaGuard(PackResources delegate) {
        this.delegate = delegate;
    }

    /** True for the title-background textures we hide so PackCore's panorama wins. */
    private static boolean isHiddenPanorama(PackType type, Identifier id) {
        return type == PackType.CLIENT_RESOURCES
                && id.getNamespace().equals("minecraft")
                && id.getPath().startsWith(TITLE_BACKGROUND_PREFIX);
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        if (isHiddenPanorama(type, id)) return null;
        return delegate.getResource(type, id);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        delegate.listResources(type, namespace, path, (id, supplier) -> {
            if (!isHiddenPanorama(type, id)) output.accept(id, supplier);
        });
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        return delegate.getRootResource(elements);
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return delegate.getNamespaces(type);
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionType<T> section) throws IOException {
        return delegate.getMetadataSection(section);
    }

    @Override
    public PackLocationInfo location() {
        return delegate.location();
    }

    @Override
    public String packId() {
        return delegate.packId();
    }

    @Override
    public Optional<KnownPack> knownPackInfo() {
        return delegate.knownPackInfo();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
