package my.pikrew.structureReplacer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StructureManager {

    private JavaPlugin plugin;
    private File structuresDir;
    private TerrainAdapter terrainAdapter;
    private ConfigManager configManager;
    private Random random; // Tambahkan field random

    // Material mapping untuk adaptasi biome
    private static final Map<String, Map<Material, Material>> BIOME_MATERIAL_MAPPING = new HashMap<>();

    static {
        // Desert adaptations
        Map<Material, Material> desertMapping = new HashMap<>();
        desertMapping.put(Material.OAK_PLANKS, Material.SANDSTONE);
        desertMapping.put(Material.OAK_LOG, Material.SANDSTONE_WALL);
        desertMapping.put(Material.COBBLESTONE, Material.SANDSTONE);
        desertMapping.put(Material.STONE_BRICKS, Material.CUT_SANDSTONE);
        desertMapping.put(Material.DIRT_PATH, Material.SAND);
        desertMapping.put(Material.GRASS_BLOCK, Material.SAND);
        desertMapping.put(Material.SHORT_GRASS, Material.DEAD_BUSH);
        BIOME_MATERIAL_MAPPING.put("DESERT", desertMapping);

        // Snowy adaptations
        Map<Material, Material> snowyMapping = new HashMap<>();
        snowyMapping.put(Material.OAK_PLANKS, Material.SPRUCE_PLANKS);
        snowyMapping.put(Material.OAK_LOG, Material.SPRUCE_LOG);
        snowyMapping.put(Material.DIRT_PATH, Material.SNOW_BLOCK);
        snowyMapping.put(Material.GRASS_BLOCK, Material.SNOW_BLOCK);
        snowyMapping.put(Material.SHORT_GRASS, Material.SNOW);
        snowyMapping.put(Material.STONE, Material.PACKED_ICE);
        BIOME_MATERIAL_MAPPING.put("SNOWY", snowyMapping);

        // Taiga adaptations
        Map<Material, Material> taigaMapping = new HashMap<>();
        taigaMapping.put(Material.OAK_PLANKS, Material.SPRUCE_PLANKS);
        taigaMapping.put(Material.OAK_LOG, Material.SPRUCE_LOG);
        taigaMapping.put(Material.OAK_STAIRS, Material.SPRUCE_STAIRS);
        taigaMapping.put(Material.OAK_SLAB, Material.SPRUCE_SLAB);
        taigaMapping.put(Material.OAK_FENCE, Material.SPRUCE_FENCE);
        taigaMapping.put(Material.SHORT_GRASS, Material.FERN);
        BIOME_MATERIAL_MAPPING.put("TAIGA", taigaMapping);

        // Jungle adaptations
        Map<Material, Material> jungleMapping = new HashMap<>();
        jungleMapping.put(Material.OAK_PLANKS, Material.JUNGLE_PLANKS);
        jungleMapping.put(Material.OAK_LOG, Material.JUNGLE_LOG);
        jungleMapping.put(Material.COBBLESTONE, Material.MOSSY_COBBLESTONE);
        jungleMapping.put(Material.STONE_BRICKS, Material.MOSSY_STONE_BRICKS);
        jungleMapping.put(Material.SHORT_GRASS, Material.JUNGLE_SAPLING);
        BIOME_MATERIAL_MAPPING.put("JUNGLE", jungleMapping);

        // Savanna adaptations
        Map<Material, Material> savannaMapping = new HashMap<>();
        savannaMapping.put(Material.OAK_PLANKS, Material.ACACIA_PLANKS);
        savannaMapping.put(Material.OAK_LOG, Material.ACACIA_LOG);
        savannaMapping.put(Material.OAK_STAIRS, Material.ACACIA_STAIRS);
        savannaMapping.put(Material.OAK_SLAB, Material.ACACIA_SLAB);
        savannaMapping.put(Material.OAK_FENCE, Material.ACACIA_FENCE);
        savannaMapping.put(Material.GRASS_BLOCK, Material.COARSE_DIRT);
        BIOME_MATERIAL_MAPPING.put("SAVANNA", savannaMapping);

        // Swamp adaptations
        Map<Material, Material> swampMapping = new HashMap<>();
        swampMapping.put(Material.OAK_PLANKS, Material.DARK_OAK_PLANKS);
        swampMapping.put(Material.OAK_LOG, Material.DARK_OAK_LOG);
        swampMapping.put(Material.COBBLESTONE, Material.MOSSY_COBBLESTONE);
        swampMapping.put(Material.DIRT_PATH, Material.MUD);
        swampMapping.put(Material.SHORT_GRASS, Material.BROWN_MUSHROOM);
        BIOME_MATERIAL_MAPPING.put("SWAMP", swampMapping);

        // Ocean adaptations
        Map<Material, Material> oceanMapping = new HashMap<>();
        oceanMapping.put(Material.COBBLESTONE, Material.PRISMARINE);
        oceanMapping.put(Material.STONE_BRICKS, Material.PRISMARINE_BRICKS);
        oceanMapping.put(Material.OAK_PLANKS, Material.DARK_PRISMARINE);
        oceanMapping.put(Material.TORCH, Material.SEA_LANTERN);
        oceanMapping.put(Material.DIRT_PATH, Material.PRISMARINE);
        BIOME_MATERIAL_MAPPING.put("OCEAN", oceanMapping);
    }

    public StructureManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.structuresDir = new File(plugin.getDataFolder(), "structures");
        this.random = new Random(); // Initialize random field

        if (!structuresDir.exists()) {
            structuresDir.mkdirs();
        }
    }

    public void setTerrainAdapter(TerrainAdapter terrainAdapter) {
        this.terrainAdapter = terrainAdapter;
    }

    public void setConfigManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void saveStructure(String name, Selection selection) throws IOException {
        if (!selection.isComplete()) {
            throw new IllegalArgumentException("Selection is not complete");
        }

        File structureFile = new File(structuresDir, name + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        // Save metadata
        config.set("name", name);
        config.set("width", selection.getWidth());
        config.set("height", selection.getHeight());
        config.set("length", selection.getLength());
        config.set("created", System.currentTimeMillis());

        // Save blocks dengan metadata tambahan untuk adaptasi
        List<Map<String, Object>> blocks = new ArrayList<>();
        World world = selection.getWorld();

        for (int x = selection.getMinX(); x <= selection.getMaxX(); x++) {
            for (int y = selection.getMinY(); y <= selection.getMaxY(); y++) {
                for (int z = selection.getMinZ(); z <= selection.getMaxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);

                    if (block.getType() == Material.AIR) {
                        continue; // Skip air blocks to save space
                    }

                    Map<String, Object> blockData = new HashMap<>();
                    blockData.put("x", x - selection.getMinX());
                    blockData.put("y", y - selection.getMinY());
                    blockData.put("z", z - selection.getMinZ());
                    blockData.put("material", block.getType().name());
                    blockData.put("data", block.getBlockData().getAsString());

                    // Tambah kategori block untuk adaptasi yang lebih baik
                    blockData.put("category", categorizeBlock(block.getType()));

                    // Tambah informasi struktural
                    blockData.put("structural_role", getStructuralRole(block, selection, x, y, z));

                    blocks.add(blockData);
                }
            }
        }

        config.set("blocks", blocks);
        config.save(structureFile);
    }

    public void pasteStructure(String name, Location location) throws IOException {
        pasteStructureWithAdaptation(name, location, true);
    }

    public void pasteStructureWithAdaptation(String name, Location location, boolean adaptToTerrain) throws IOException {
        File structureFile = new File(structuresDir, name + ".yml");

        if (!structureFile.exists()) {
            throw new IOException("Structure file not found: " + name);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(structureFile);
        List<Map<?, ?>> blocks = config.getMapList("blocks");

        World world = location.getWorld();
        Biome biome = world.getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();

        // Fase 1: Adaptasi terrain jika diperlukan
        if (adaptToTerrain && terrainAdapter != null) {
            terrainAdapter.adaptStructureToTerrain(location, name);
        }

        // Fase 2: Tempatkan struktur dengan adaptasi material
        Map<Material, Material> materialMapping = getBiomeMaterialMapping(biome);

        for (Map<?, ?> blockInfo : blocks) {
            int x = baseX + (Integer) blockInfo.get("x");
            int y = baseY + (Integer) blockInfo.get("y");
            int z = baseZ + (Integer) blockInfo.get("z");

            String materialName = (String) blockInfo.get("material");
            String blockDataString = (String) blockInfo.get("data");
            String category = (String) blockInfo.get("category");
            String structuralRole = (String) blockInfo.get("structural_role");

            try {
                Material originalMaterial = Material.valueOf(materialName);
                Material adaptedMaterial = adaptMaterialToBiome(originalMaterial, materialMapping, category);

                Block block = world.getBlockAt(x, y, z);
                block.setType(adaptedMaterial);

                // Sesuaikan block data jika perlu
                if (blockDataString != null && !blockDataString.isEmpty()) {
                    try {
                        // Adaptasi block data untuk material yang berubah
                        String adaptedBlockData = adaptBlockData(blockDataString, originalMaterial, adaptedMaterial);
                        BlockData blockData = plugin.getServer().createBlockData(adaptedBlockData);
                        block.setBlockData(blockData);
                    } catch (IllegalArgumentException e) {
                        if (configManager != null && configManager.isDebugEnabled()) {
                            plugin.getLogger().warning("Invalid block data for " + adaptedMaterial + ": " + blockDataString);
                        }
                    }
                }

            } catch (IllegalArgumentException e) {
                if (configManager != null && configManager.isDebugEnabled()) {
                    plugin.getLogger().warning("Unknown material: " + materialName);
                }
            }
        }

        // Fase 3: Post-processing untuk details natural
        if (adaptToTerrain) {
            addNaturalDetails(location, name, biome, config);
        }
    }

    /**
     * Menambahkan detail natural setelah struktur ditempatkan
     */
    private void addNaturalDetails(Location center, String structureName, Biome biome, YamlConfiguration config) {
        World world = center.getWorld();
        Random random = new Random();

        int width = config.getInt("width", 10);
        int height = config.getInt("height", 10);
        int length = config.getInt("length", 10);

        // Tambah weathering effects
        addWeatheringEffects(center, width, height, length, biome, random);

        // Tambah vegetation overgrowth
        addVegetationOvergrowth(center, width, height, length, biome, random);

        // Tambah random variations
        addRandomVariations(center, width, height, length, biome, random);
    }

    private void addWeatheringEffects(Location center, int width, int height, int length, Biome biome, Random random) {
        World world = center.getWorld();
        String biomeName = biome.toString().toUpperCase();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    if (random.nextDouble() < 0.05) { // 5% chance
                        Location loc = center.clone().add(x, y, z);
                        Block block = world.getBlockAt(loc);
                        Material weathered = getWeatheredVersion(block.getType(), biomeName);

                        if (weathered != null) {
                            block.setType(weathered);
                        }
                    }
                }
            }
        }
    }

    private void addVegetationOvergrowth(Location center, int width, int height, int length, Biome biome, Random random) {
        World world = center.getWorld();
        String biomeName = biome.toString().toUpperCase();

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                if (random.nextDouble() < 0.1) { // 10% chance
                    Location loc = center.clone().add(x, height, z);
                    Block block = world.getBlockAt(loc);

                    if (block.getType() == Material.AIR) {
                        Material vegetation = getVegetationForBiome(biomeName, random);
                        if (vegetation != null) {
                            block.setType(vegetation);
                        }
                    }
                }
            }
        }
    }

    private void addRandomVariations(Location center, int width, int height, int length, Biome biome, Random random) {
        World world = center.getWorld();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    if (random.nextDouble() < 0.03) { // 3% chance
                        Location loc = center.clone().add(x, y, z);
                        Block block = world.getBlockAt(loc);
                        Material variation = getVariationMaterial(block.getType(), random);

                        if (variation != null) {
                            block.setType(variation);
                        }
                    }
                }
            }
        }
    }

    private Material getWeatheredVersion(Material material, String biomeName) {
        if (biomeName.contains("DESERT") || biomeName.contains("BADLANDS")) {
            // Desert weathering
            switch (material) {
                case COBBLESTONE:
                    return Material.MOSSY_COBBLESTONE;
                case STONE_BRICKS:
                    return Material.CRACKED_STONE_BRICKS;
                case OAK_PLANKS:
                    return random.nextBoolean() ? Material.OAK_PLANKS : null; // Some planks disappear
                default:
                    break;
            }
        } else if (biomeName.contains("JUNGLE") || biomeName.contains("SWAMP")) {
            // Tropical/humid weathering
            switch (material) {
                case COBBLESTONE:
                    return Material.MOSSY_COBBLESTONE;
                case STONE_BRICKS:
                    return Material.MOSSY_STONE_BRICKS;
                case STONE:
                    return Material.MOSSY_COBBLESTONE;
                default:
                    break;
            }
        } else if (biomeName.contains("SNOWY") || biomeName.contains("FROZEN")) {
            // Cold weathering
            switch (material) {
                case STONE_BRICKS:
                    return Material.CRACKED_STONE_BRICKS;
                case COBBLESTONE:
                    return Material.COBBLESTONE; // No change, cold preserves
                default:
                    break;
            }
        }

        return null; // No weathering
    }

    private Material getVegetationForBiome(String biomeName, Random random) {
        if (biomeName.contains("JUNGLE")) {
            Material[] jungleVeg = {Material.VINE, Material.JUNGLE_SAPLING, Material.COCOA};
            return jungleVeg[random.nextInt(jungleVeg.length)];
        } else if (biomeName.contains("SWAMP")) {
            Material[] swampVeg = {Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.LILY_PAD};
            return swampVeg[random.nextInt(swampVeg.length)];
        } else if (biomeName.contains("PLAINS") || biomeName.contains("MEADOW")) {
            Material[] plainsVeg = {Material.SHORT_GRASS, Material.DANDELION, Material.POPPY};
            return plainsVeg[random.nextInt(plainsVeg.length)];
        } else if (biomeName.contains("TAIGA")) {
            Material[] taigaVeg = {Material.FERN, Material.LARGE_FERN, Material.SWEET_BERRY_BUSH};
            return taigaVeg[random.nextInt(taigaVeg.length)];
        }

        return null;
    }

    private Material getVariationMaterial(Material original, Random random) {
        switch (original) {
            case COBBLESTONE:
                Material[] cobbleVariations = {Material.STONE, Material.ANDESITE, Material.DIORITE};
                return cobbleVariations[random.nextInt(cobbleVariations.length)];

            case STONE_BRICKS:
                Material[] brickVariations = {Material.CRACKED_STONE_BRICKS, Material.CHISELED_STONE_BRICKS};
                return brickVariations[random.nextInt(brickVariations.length)];

            case OAK_PLANKS:
                if (random.nextDouble() < 0.1) { // 10% chance to become different wood
                    Material[] woodVariations = {Material.BIRCH_PLANKS, Material.SPRUCE_PLANKS};
                    return woodVariations[random.nextInt(woodVariations.length)];
                }
                break;
            default:
                break;
        }

        return null;
    }

    private String categorizeBlock(Material material) {
        String name = material.name().toLowerCase();

        if (name.contains("planks") || name.contains("log") || name.contains("wood")) {
            return "wood";
        } else if (name.contains("stone") || name.contains("cobble") || name.contains("brick")) {
            return "stone";
        } else if (name.contains("dirt") || name.contains("grass") || name.contains("sand")) {
            return "ground";
        } else if (name.contains("glass") || name.contains("pane")) {
            return "glass";
        } else if (name.contains("door") || name.contains("fence") || name.contains("gate")) {
            return "structural";
        } else if (name.contains("stairs") || name.contains("slab")) {
            return "architectural";
        } else if (name.contains("torch") || name.contains("lantern") || name.contains("lamp")) {
            return "lighting";
        } else if (material.isBlock() && !material.isSolid()) {
            return "decoration";
        }

        return "misc";
    }

    private String getStructuralRole(Block block, Selection selection, int x, int y, int z) {
        boolean isEdge = (x == selection.getMinX() || x == selection.getMaxX() ||
                z == selection.getMinZ() || z == selection.getMaxZ());
        boolean isCorner = (x == selection.getMinX() || x == selection.getMaxX()) &&
                (z == selection.getMinZ() || z == selection.getMaxZ());
        boolean isFoundation = (y == selection.getMinY() || y == selection.getMinY() + 1);
        boolean isRoof = (y >= selection.getMaxY() - 2);

        if (isFoundation) return "foundation";
        if (isRoof) return "roof";
        if (isCorner) return "corner";
        if (isEdge) return "wall";

        return "interior";
    }

    private Map<Material, Material> getBiomeMaterialMapping(Biome biome) {
        String biomeName = biome.toString().toUpperCase();

        for (String key : BIOME_MATERIAL_MAPPING.keySet()) {
            if (biomeName.contains(key)) {
                return BIOME_MATERIAL_MAPPING.get(key);
            }
        }

        return new HashMap<>(); // No mapping
    }

    private Material adaptMaterialToBiome(Material original, Map<Material, Material> mapping, String category) {
        // Prioritas mapping eksplisit
        if (mapping.containsKey(original)) {
            return mapping.get(original);
        }

        // Fallback berdasarkan kategori
        return original; // Tidak ada adaptasi
    }

    private String adaptBlockData(String originalData, Material originalMaterial, Material newMaterial) {
        // Jika material sama, return data asli
        if (originalMaterial == newMaterial) {
            return originalData;
        }

        // Adaptasi sederhana - ganti nama material dalam data string
        String oldMaterialName = originalMaterial.name().toLowerCase();
        String newMaterialName = newMaterial.name().toLowerCase();

        return originalData.replace(oldMaterialName, newMaterialName);
    }

    public boolean structureExists(String name) {
        File structureFile = new File(structuresDir, name + ".yml");
        return structureFile.exists();
    }

    public Map<String, File> getAvailableStructures() {
        Map<String, File> structures = new HashMap<>();

        if (!structuresDir.exists()) {
            return structures;
        }

        File[] files = structuresDir.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files != null) {
            for (File file : files) {
                String name = file.getName().substring(0, file.getName().length() - 4);
                structures.put(name, file);
            }
        }

        return structures;
    }

    public StructureInfo getStructureInfo(String name) throws IOException {
        File structureFile = new File(structuresDir, name + ".yml");

        if (!structureFile.exists()) {
            return null;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(structureFile);

        StructureInfo info = new StructureInfo();
        info.name = config.getString("name", name);
        info.width = config.getInt("width", 0);
        info.height = config.getInt("height", 0);
        info.length = config.getInt("length", 0);
        info.created = config.getLong("created", 0);
        info.blockCount = config.getMapList("blocks").size();

        return info;
    }

    public static class StructureInfo {
        public String name;
        public int width;
        public int height;
        public int length;
        public long created;
        public int blockCount;

        public int getVolume() {
            return width * height * length;
        }
    }
}