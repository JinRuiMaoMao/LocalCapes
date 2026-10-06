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
import java.nio.file.StandardCopyOption;
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
    /** Grid order follows the wiki write-up sequence. IDs not listed yet sort last. */
    private static final List<String> DISPLAY_ORDER = List.of(
            "pan",
            "migrator",
            "vanilla",
            "common",
            "mojang_classic",
            "mojang",
            "microsoft_xbox_360",
            "4j_studios",
            "mojang_studios",
            "minecon_2011",
            "minecon_2012",
            "minecon_2013",
            "minecon_2015",
            "minecon_2016",
            "minecraft_experience",
            "moonlight_trail",
            "crafter",
            "founders",
            "progress_pride",
            "cherry_blossom",
            "followers",
            "purple_heart",
            "15th_anniversary",
            "mcc_15th_year",
            "mojang_office",
            "home",
            "menace",
            "yearn",
            "copper",
            "zombie_horse",
            "builder",
            "aurora",
            "bacon",
            "millionth_customer",
            "dannybstyle",
            "julianclark",
            "cheapsh0t",
            "mrmessiah",
            "prismarine",
            "turtle",
            "birthday",
            "valentine",
            "oxeye",
            "blueprint",
            "scrolls_champion",
            "cobalt",
            "translator",
            "chinese_translator",
            "moderator",
            "mapmaker",
            "hero",
            "sinister",
            "phantom",
            "year1",
            "downpour",
            "prism",
            "cloudy_climb",
            "iceologer",
            "glow",
            "luminous_night",
            "amethyst",
            "gift_wrap",
            "cow_crusader",
            "turtle_shell",
            "fauna_faire",
            "ominous",
            "hammer",
            "iron_golem",
            "mystery",
            "red_royal",
            "twisted",
            "hero_mcd2",
            "soul",
            "corrupted_creeper",
            "special",
            "mojang_mcd2",
            "slice_slayer"
    );

    private static final Pattern UUID_DASHED = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    private static final Pattern UUID_FLAT = Pattern.compile("^[0-9a-fA-F]{32}$");
    private static final String NONE = "none";
    public static final ResourceLocation VANILLA_ELYTRA = new ResourceLocation("textures/entity/elytra.png");

    private static final Map<String, ResourceLocation> BY_NAME = new HashMap<>();
    private static final Map<UUID, ResourceLocation> BY_UUID = new HashMap<>();
    private static final Set<ResourceLocation> DYNAMIC = new HashSet<>();
    private static final Set<ResourceLocation> NO_ELYTRA = new HashSet<>();
    private static final List<CapeEntry> BUNDLED = new ArrayList<>();
    private static final List<CapeEntry> LOCAL_FILES = new ArrayList<>();
    @Nullable
    private static ResourceLocation defaultCape;
    @Nullable
    private static ResourceLocation selected;
    private static boolean unequipped;
    private static String gameFilter = CapeEntry.GAME_MINECRAFT;
    private static boolean nightGlow = true;

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
        NO_ELYTRA.clear();
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
        loadFilter();
        loadNightGlow();
        LocalCapes.LOGGER.info("Loaded {} bundled and {} local cape(s)", BUNDLED.size(), loaded);
        return BUNDLED.size() + loaded;
    }

    private static void scanBundled(ResourceManager resources) {
        Map<ResourceLocation, Resource> found = resources.listResources(
                "textures/cape",
                location -> location.getNamespace().equals(LocalCapes.MOD_ID) && location.getPath().endsWith(".png")
        );
        for (Map.Entry<ResourceLocation, Resource> resource : found.entrySet()) {
            ResourceLocation location = resource.getKey();
            try (InputStream in = resource.getValue().open(); NativeImage image = NativeImage.read(in)) {
                if (!hasElytraPixels(image)) {
                    NO_ELYTRA.add(location);
                }
            } catch (IOException e) {
                LocalCapes.LOGGER.warn("Could not inspect bundled cape {}", location, e);
            }
            String path = location.getPath();
            String relative = path.substring("textures/cape/".length(), path.length() - 4);
            int slash = relative.indexOf('/');
            if (slash <= 0) {
                continue;
            }
            String game = CapeEntry.GAME_MINECRAFT;
            String rest = relative;
            String top = relative.substring(0, slash);
            if (!CapeEntry.GAME_MINECRAFT.equals(top) && CapeEntry.GAMES.contains(top)) {
                game = top;
                rest = relative.substring(slash + 1);
            }
            int restSlash = rest.indexOf('/');
            String category;
            String stem;
            if (restSlash <= 0) {
                category = game;
                stem = rest;
            } else {
                category = rest.substring(0, restSlash);
                stem = rest.substring(restSlash + 1);
            }
            if (stem.isEmpty()) {
                continue;
            }
            BUNDLED.add(new CapeEntry(stem, category, game, displayName(stem), location, false));
        }
        BUNDLED.sort(Comparator
                .comparingInt((CapeEntry e) -> {
                    int i = DISPLAY_ORDER.indexOf(e.id);
                    return i < 0 ? DISPLAY_ORDER.size() : i;
                })
                .thenComparing(e -> e.id, String.CASE_INSENSITIVE_ORDER));
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
            if (!hasElytraPixels(image)) {
                NO_ELYTRA.add(id);
            }
            textures.register(id, new DynamicTexture(image));
            DYNAMIC.add(id);
            indexCape(stem, id);
            LOCAL_FILES.add(new CapeEntry(stem, "local", CapeEntry.GAME_MINECRAFT, displayName(stem), id, true));
            return true;
        } catch (IOException e) {
            LocalCapes.LOGGER.error("Could not load cape {}", file, e);
            return false;
        }
    }

    /** The elytra occupies (22,0)-(46,22) of a 64-wide cape texture; HD textures scale with width. */
    private static boolean hasElytraPixels(NativeImage image) {
        int scale = Math.max(1, image.getWidth() / 64);
        int maxX = Math.min(image.getWidth(), 46 * scale);
        int maxY = Math.min(image.getHeight(), 22 * scale);
        for (int y = 0; y < maxY; y++) {
            for (int x = 22 * scale; x < maxX; x++) {
                if ((image.getPixelRGBA(x, y) >>> 24) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ResourceLocation elytraTexture(ResourceLocation cape) {
        return NO_ELYTRA.contains(cape) ? VANILLA_ELYTRA : cape;
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

    public static List<CapeEntry> entriesForGame(String game) {
        List<CapeEntry> result = new ArrayList<>();
        for (CapeEntry entry : BUNDLED) {
            if (entry.game.equals(game)) {
                result.add(entry);
            }
        }
        if (CapeEntry.GAME_MINECRAFT.equals(game)) {
            result.addAll(LOCAL_FILES);
        }
        return result;
    }

    public static String getGameFilter() {
        return gameFilter;
    }

    public static void setGameFilter(String game) {
        gameFilter = CapeEntry.GAMES.contains(game) ? game : CapeEntry.GAME_MINECRAFT;
        saveFilter();
    }

    public static boolean isNightGlow() {
        return nightGlow;
    }

    public static boolean toggleNightGlow() {
        nightGlow = !nightGlow;
        saveNightGlow();
        return nightGlow;
    }

    public static boolean shouldNightGlow(AbstractClientPlayer player) {
        return nightGlow
                && hasCape(player)
                && player.level() != null
                && CapeGlow.isDarkEnough(player.level(), player);
    }

    @Nullable
    public static CapeEntry importCape(Path source) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || !Files.isRegularFile(source)) {
            return null;
        }

        Path dir = getCapeDir();
        try {
            Files.createDirectories(dir);
            try (InputStream in = Files.newInputStream(source);
                 NativeImage ignored = NativeImage.read(in)) {
            }

            String stem = stemFromFilename(source.getFileName().toString());
            if (stem.isEmpty()) {
                stem = "cape";
            }
            Path dest = uniqueLocalPath(dir, stem);
            Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);

            reload(minecraft.getResourceManager());
            String id = dest.getFileName().toString();
            id = id.substring(0, id.length() - 4);
            for (CapeEntry entry : LOCAL_FILES) {
                if (entry.id.equals(id)) {
                    return entry;
                }
            }
        } catch (IOException e) {
            LocalCapes.LOGGER.error("Could not import cape from {}", source, e);
        }
        return null;
    }

    public static boolean deleteLocal(CapeEntry entry) {
        if (!entry.localFile) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return false;
        }
        Path file = getCapeDir().resolve(entry.id + ".png");
        if (!Files.isRegularFile(file)) {
            return false;
        }
        try {
            if (isSelected(entry)) {
                unequip();
            }
            Files.delete(file);
            reload(minecraft.getResourceManager());
            return true;
        } catch (IOException e) {
            LocalCapes.LOGGER.error("Could not delete cape {}", file, e);
            return false;
        }
    }

    private static String stemFromFilename(String filename) {
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".png")) {
            return sanitize(filename);
        }
        return sanitize(filename.substring(0, filename.length() - 4));
    }

    private static Path uniqueLocalPath(Path dir, String stem) throws IOException {
        Path dest = dir.resolve(stem + ".png");
        if (!Files.exists(dest)) {
            return dest;
        }
        for (int i = 1; i < 1000; i++) {
            dest = dir.resolve(stem + "_" + i + ".png");
            if (!Files.exists(dest)) {
                return dest;
            }
        }
        throw new IOException("Too many capes named " + stem);
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

    private static Path filterFile() {
        return getCapeDir().resolve("filter.txt");
    }

    private static void loadFilter() {
        Path file = filterFile();
        if (!Files.exists(file)) {
            gameFilter = CapeEntry.GAME_MINECRAFT;
            return;
        }
        try {
            String line = Files.readString(file, StandardCharsets.UTF_8).trim();
            String game = line.toLowerCase(Locale.ROOT);
            gameFilter = CapeEntry.GAMES.contains(game) ? game : CapeEntry.GAME_MINECRAFT;
        } catch (IOException e) {
            LocalCapes.LOGGER.error("Could not read cape filter", e);
        }
    }

    private static void saveFilter() {
        try {
            Files.createDirectories(getCapeDir());
            Files.writeString(filterFile(), gameFilter, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LocalCapes.LOGGER.error("Could not save cape filter", e);
        }
    }

    private static Path glowFile() {
        return getCapeDir().resolve("glow.txt");
    }

    private static void loadNightGlow() {
        Path file = glowFile();
        if (!Files.exists(file)) {
            nightGlow = true;
            return;
        }
        try {
            String line = Files.readString(file, StandardCharsets.UTF_8).trim();
            nightGlow = !"off".equalsIgnoreCase(line) && !"false".equalsIgnoreCase(line);
        } catch (IOException e) {
            LocalCapes.LOGGER.error("Could not read cape glow setting", e);
        }
    }

    private static void saveNightGlow() {
        try {
            Files.createDirectories(getCapeDir());
            Files.writeString(glowFile(), nightGlow ? "on" : "off", StandardCharsets.UTF_8);
        } catch (IOException e) {
            LocalCapes.LOGGER.error("Could not save cape glow setting", e);
        }
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
                Extra PNGs in this folder also appear in the Minecraft list.

                Naming extra files:
                  YourMinecraftName.png   - shown on that player
                  uuid.png                - shown on that UUID
                  default.png             - fallback if you have not picked a cape
                """;
        Files.writeString(readme, text, StandardCharsets.UTF_8);
    }
}
