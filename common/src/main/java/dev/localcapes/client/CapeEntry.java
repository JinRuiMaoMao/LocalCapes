package dev.localcapes.client;

import net.minecraft.resources.ResourceLocation;

public final class CapeEntry {
    public final String id;
    public final String category;
    public final String displayName;
    public final ResourceLocation texture;
    public final boolean localFile;

    public CapeEntry(String id, String category, String displayName, ResourceLocation texture, boolean localFile) {
        this.id = id;
        this.category = category;
        this.displayName = displayName;
        this.texture = texture;
        this.localFile = localFile;
    }
}
