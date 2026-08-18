package dev.localcapes.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import dev.architectury.platform.Platform;
import dev.localcapes.LocalCapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class CapeManager {
    public static final List<String> CATEGORY_ORDER = List.of(
            "account",
            "staff",
            "event_physical",
            "event_virtual",
            "personal",
            "competition",
            "volunteer",
            "local"
    );

    private static final Pattern UUID_DASHED = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern UUID_FLAT = Pattern.compile("^[0-9a-fA-F]{32}$");
    private static final String NONE = "none";

    private static final Map<String, ResourceLocation> BY_NAME = new HashMap<>();
    private static final Map<UUID, ResourceLocation> BY_UUID = new HashMap<>();
    private static final Set<ResourceLocation> DYNAMIC = new HashSet<>();
    private static final List<CapeEntry> BUNDLED = new ArrayList<>();
    private static final List<CapeEntry> LOCAL_FILES = new ArrayList<>();
    @Nullable
    private static ResourceLocation defaultCape;
    @Nullable
    private static ResourceLocation selected;
    private static boolean unequipped;

    private CapeManager() {
    }

    public static Path getCapeDir() {
        return Platform.getConfigFolder().resolve(LocalCapes.MOD_ID);
    }

    public static int reload(ResourceManager resources) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return 0;
        }

        Path dir = getCapeDir();
        try {
            Files.createDirectories(dir);
            writeReadme(dir);
        } catch (IOException e) {
            LocalCapes.LOGGER.error("Could not create cape folder {}", dir, e);
            return 0;
        }

        TextureManager textures = minecraft.getTextureManager();
        for (ResourceLocation id : DYNAMIC) {
            textures.release(id);
        }
        DYNAMIC.clear();
        BY_NAME.clear();
        BY_UUID.clear();
        defaultCape = null;
        LOCAL_FILES.clear();
        BUNDLED.clear();

        if (resources != null) {
            scanBundled(resources);
        }

        int loaded = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
            for (Path file : stream) {
                if (loadCape(textures, file)) {
                    loaded++;
                }
            }
        } catch (IOException e) {
            LocalCapes.LOGGER.error("Failed to scan cape folder {}", dir, e);
        }

        loadSelection();
        LocalCapes.LOGGER.info("Loaded {} bundled and {} local cape(s)", BUNDLED.size(), loaded);
        return BUNDLED.size() + loaded;
    }

    private static void scanBundled(ResourceManager resources) {
        Map<ResourceLocation, Resource> found = resources.listResources(
                "textures/cape",
                location -> location.getNamespace().equals(LocalCapes.MOD_ID) && location.getPath().endsWith(".png")
        );
        for (ResourceLocation location : found.keySet()) {
            String path = location.getPath();
            String relative = path.substring("textures/cape/".length(), path.length() - 4);
            int slash = relative.indexOf('/');
            if (slash <= 0) {
                continue;
            }
            String category = relative.substring(0, slash);
            String stem = relative.substring(slash + 1);
            BUNDLED.add(new CapeEntry(stem, category, displayName(stem), location, false));
        }
        BUNDLED.sort(Comparator
                .comparingInt((CapeEntry e) -> {
                    int i = CATEGORY_ORDER.indexOf(e.category);
                    return i < 0 ? 99 : i;
                })
                .thenComparing(e -> e.displayName, String.CASE_INSENSITIVE_ORDER));
    }

    private static boolean loadCape(TextureManager textures, Path file) {
        String filename = file.getFileName().toString();
        String stem = filename.substring(0, filename.length() - 4);

        try (InputStream in = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(in);
            int width = image.getWidth();
            int height = image.getHeight();
            if (width != 64 || (height != 32 && height != 64)) {
                LocalCapes.LOGGER.warn(
                        "{} is {}x{} (vanilla capes are 64x32). It will still load, but UV mapping may look wrong.",
                        filename, width, height
                );
            }

            ResourceLocation id = new ResourceLocation(LocalCapes.MOD_ID, "dynamic/" + sanitize(stem));
            textures.register(id, new DynamicTexture(image));
            DYNAMIC.add(id);
            indexCape(stem, id);
            LOCAL_FILES.add(new CapeEntry(stem, "local", displayName(stem), id, true));
            return true;
        } catch (IOException e) {
            LocalCapes.LOGGER.error("Could not load cape {}", file, e);
            return false;
        }
    }

    private static void indexCape(String stem, ResourceLocation id) {
        if (stem.equalsIgnoreCase("default")) {
            defaultCape = id;
            return;
        }
        if (UUID_DASHED.matcher(stem).matches()) {
            BY_UUID.put(UUID.fromString(stem), id);
            return;
        }
        if (UUID_FLAT.matcher(stem).matches()) {
            String dashed = stem.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                    "$1-$2-$3-$4-$5"
            );
            BY_UUID.put(UUID.fromString(dashed), id);
            return;
        }
        BY_NAME.put(stem.toLowerCase(Locale.ROOT), id);
    }

    private static String sanitize(String stem) {
        String cleaned = stem.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
        return cleaned.isEmpty() ? "cape" : cleaned;
    }

    public static String displayName(String stem) {
        String[] parts = stem.split("[_-]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    public static List<CapeEntry> entries() {
        List<CapeEntry> all = new ArrayList<>(BUNDLED.size() + LOCAL_FILES.size());
        all.addAll(BUNDLED);
        all.addAll(LOCAL_FILES);
        return all;
    }

    public static List<CapeEntry> entriesIn(String category) {
        List<CapeEntry> result = new ArrayList<>();
        for (CapeEntry entry : entries()) {
            if (entry.category.equals(category)) {
                result.add(entry);
            }
        }
        return result;
    }

    public static boolean hasCape(AbstractClientPlayer player) {
        return getCape(player) != null;
    }

    @Nullable
    public static ResourceLocation getSelected() {
        return unequipped ? null : selected;
    }

    public static boolean isSelected(CapeEntry entry) {
        ResourceLocation current = getSelected();
        return current != null && current.equals(entry.texture);
    }

    public static void wear(CapeEntry entry) {
        unequipped = false;
        selected = entry.texture;
        saveSelection();
    }

    public static void unequip() {
        unequipped = true;
        selected = null;
        saveSelection();
    }

    @Nullable
    public static ResourceLocation getCape(AbstractClientPlayer player) {
        ResourceLocation specific = fileCape(player);
        Minecraft minecraft = Minecraft.getInstance();
        boolean self = minecraft != null && minecraft.player == player;
        if (self) {
            if (specific != null) {
                return specific;
            }
            if (unequipped) {
                return null;
            }
            if (selected != null) {
                return selected;
            }
        } else if (specific != null) {
            return specific;
        }
        return defaultCape;
    }

    @Nullable
    private static ResourceLocation fileCape(AbstractClientPlayer player) {
        GameProfile profile = player.getGameProfile();
        ResourceLocation byUuid = BY_UUID.get(profile.getId());
        if (byUuid != null) {
            return byUuid;
        }
        String name = profile.getName();
        if (name != null) {
            return BY_NAME.get(name.toLowerCase(Locale.ROOT));
        }
        return null;
    }

    private static Path selectionFile() {
        return getCapeDir().resolve("selected.txt");
    }

    private static void loadSelection() {
        Path file = selectionFile();
        if (!Files.exists(file)) {
            unequipped = false;
            selected = firstBundled();
            return;
        }
        try {
            String line = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (line.isEmpty() || NONE.equalsIgnoreCase(line)) {
                unequipped = true;
                selected = null;
                return;
            }
            ResourceLocation location = ResourceLocation.tryParse(line);
            unequipped = false;
            selected = location != null ? location : firstBundled();
        } catch (IOException e) {
            LocalCapes.LOGGER.error("Could not read cape selection", e);
        }
    }

    @Nullable
    private static ResourceLocation firstBundled() {
        return BUNDLED.isEmpty() ? null : BUNDLED.get(0).texture;
    }

    private static void saveSelection() {
        try {
            Files.createDirectories(getCapeDir());
            String value = unequipped || selected == null ? NONE : selected.toString();
            Files.writeString(selectionFile(), value, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LocalCapes.LOGGER.error("Could not save cape selection", e);
        }
    }

    private static void writeReadme(Path dir) throws IOException {
        Path readme = dir.resolve("README.txt");
        if (Files.exists(readme)) {
            return;
        }
        String text = """
                LocalCapes
                ==========
                Open your inventory and click 披风, or press H, to pick a bundled cape.
                Extra PNGs in this folder also appear under the Local category.

                Naming extra files:
                  YourMinecraftName.png   - shown on that player
                  uuid.png                - shown on that UUID
                  default.png             - fallback if you have not picked a cape
                """;
        Files.writeString(readme, text, StandardCharsets.UTF_8);
    }
}
