package dev.localcapes.client;

import net.minecraft.resources.ResourceLocation;

public final class CapeEntry {
    public static final String GAME_MINECRAFT = "minecraft";
    public static final String GAME_DUNGEONS = "dungeons";

    public final String id;
    public final String category;
    public final String game;
    public final String displayName;
    public final ResourceLocation texture;
    public final boolean localFile;

    public CapeEntry(
            String id,
            String category,
            String game,
            String displayName,
            ResourceLocation texture,
            boolean localFile
    ) {
        this.id = id;
        this.category = category;
        this.game = game;
        this.displayName = displayName;
        this.texture = texture;
        this.localFile = localFile;
    }
}
