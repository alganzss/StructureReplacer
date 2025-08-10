package my.pikrew.structureReplacer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class TerrainAdapter {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    // Material palettes untuk berbagai biome
    private static final Map<String, MaterialPalette> BIOME_PALETTES = new HashMap<>();

    static {
        // Plains palette
        BIOME_PALETTES.put("PLAINS", new MaterialPalette()
                .addFoundation(Material.STONE, Material.COBBLESTONE, Material.MOSSY_COBBLESTONE)
                .addWall(Material.OAK_PLANKS, Material.COBBLESTONE, Material.STONE_BRICKS)
                .addRoof(Material.OAK_PLANKS, Material.DARK_OAK_PLANKS, Material.SPRUCE_PLANKS)
                .addPath(Material.DIRT_PATH, Material.COBBLESTONE, Material.GRAVEL)
                .addDecoration(Material.SHORT_GRASS, Material.DANDELION, Material.POPPY)
        );

        // Desert palette
        BIOME_PALETTES.put("DESERT", new MaterialPalette()
                .addFoundation(Material.SANDSTONE, Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE)
                .addWall(Material.SANDSTONE, Material.SMOOTH_SANDSTONE, Material.TERRACOTTA)
                .addRoof(Material.SANDSTONE_SLAB, Material.SMOOTH_SANDSTONE_SLAB, Material.RED_TERRACOTTA)
                .addPath(Material.SAND, Material.SANDSTONE, Material.SMOOTH_SANDSTONE)
                .addDecoration(Material.DEAD_BUSH, Material.CACTUS, Material.SAND)
        );

        // Taiga palette
        BIOME_PALETTES.put("TAIGA", new MaterialPalette()
                .addFoundation(Material.STONE, Material.COBBLESTONE, Material.MOSSY_COBBLESTONE)
                .addWall(Material.SPRUCE_PLANKS, Material.SPRUCE_LOG, Material.COBBLESTONE)
                .addRoof(Material.SPRUCE_PLANKS, Material.DARK_OAK_PLANKS, Material.SPRUCE_LOG)
                .addPath(Material.DIRT_PATH, Material.COARSE_DIRT, Material.PODZOL)
                .addDecoration(Material.FERN, Material.LARGE_FERN, Material.SWEET_BERRY_BUSH)
        );

        // Snowy palette
        BIOME_PALETTES.put("SNOWY", new MaterialPalette()
                .addFoundation(Material.COBBLESTONE, Material.STONE, Material.STONE_BRICKS)
                .addWall(Material.SPRUCE_PLANKS, Material.SPRUCE_LOG, Material.COBBLESTONE)
                .addRoof(Material.SPRUCE_PLANKS, Material.SPRUCE_STAIRS, Material.SNOW_BLOCK)
                .addPath(Material.PACKED_ICE, Material.SNOW_BLOCK, Material.ICE)
                .addDecoration(Material.SNOW, Material.SNOW_BLOCK, Material.ICE)
        );

        // Savanna palette
        BIOME_PALETTES.put("SAVANNA", new MaterialPalette()
                .addFoundation(Material.STONE, Material.COBBLESTONE, Material.RED_SANDSTONE)
                .addWall(Material.ACACIA_PLANKS, Material.ACACIA_LOG, Material.TERRACOTTA)
                .addRoof(Material.ACACIA_PLANKS, Material.RED_TERRACOTTA, Material.ORANGE_TERRACOTTA)
                .addPath(Material.COARSE_DIRT, Material.DIRT_PATH, Material.RED_SAND)
                .addDecoration(Material.SHORT_GRASS, Material.ACACIA_SAPLING, Material.DEAD_BUSH)
        );

        // Jungle palette
        BIOME_PALETTES.put("JUNGLE", new MaterialPalette()
                .addFoundation(Material.MOSSY_COBBLESTONE, Material.MOSSY_STONE_BRICKS, Material.COBBLESTONE)
                .addWall(Material.JUNGLE_PLANKS, Material.JUNGLE_LOG, Material.MOSSY_COBBLESTONE)
                .addRoof(Material.JUNGLE_PLANKS, Material.JUNGLE_LEAVES, Material.MOSSY_COBBLESTONE)
                .addPath(Material.DIRT, Material.PODZOL, Material.MOSSY_COBBLESTONE)
                .addDecoration(Material.JUNGLE_SAPLING, Material.COCOA, Material.VINE)
        );

        // Swamp palette
        BIOME_PALETTES.put("SWAMP", new MaterialPalette()
                .addFoundation(Material.COBBLESTONE, Material.MOSSY_COBBLESTONE, Material.MUD)
                .addWall(Material.DARK_OAK_PLANKS, Material.DARK_OAK_LOG, Material.MOSSY_COBBLESTONE)
                .addRoof(Material.DARK_OAK_PLANKS, Material.DARK_OAK_LOG, Material.MUSHROOM_STEM)
                .addPath(Material.MUD, Material.MUDDY_MANGROVE_ROOTS, Material.CLAY)
                .addDecoration(Material.LILY_PAD, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM)
        );

        // Ocean palette
        BIOME_PALETTES.put("OCEAN", new MaterialPalette()
                .addFoundation(Material.PRISMARINE, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE)
                .addWall(Material.PRISMARINE, Material.PRISMARINE_BRICKS, Material.SEA_LANTERN)
                .addRoof(Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE, Material.SEA_LANTERN)
                .addPath(Material.PRISMARINE, Material.SAND, Material.GRAVEL)
                .addDecoration(Material.KELP, Material.SEA_PICKLE, Material.TUBE_CORAL)
        );
    }

    public TerrainAdapter(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Menyesuaikan struktur dengan terrain dan biome sekitar
     */
    public void adaptStructureToTerrain(Location location, String structureName) {
        World world = location.getWorld();
        Biome biome = world.getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Adapting structure " + structureName + " to biome: " + biome);
        }

        // 1. Level terrain di sekitar struktur
        levelTerrain(location, structureName);

        // 2. Buat foundation yang natural
        createNaturalFoundation(location, structureName, biome);

        // 3. Tambahkan path yang natural
        createNaturalPaths(location, structureName, biome);

        // 4. Tambahkan vegetasi dan dekorasi
        addNaturalVegetation(location, structureName, biome);

        // 5. Sesuaikan dengan elevasi terrain
        adjustToElevation(location, structureName);
    }

    /**
     * Menyamakan level tanah di sekitar struktur
     */
    private void levelTerrain(Location center, String structureName) {
        int radius = getStructureRadius(structureName);
        World world = center.getWorld();

        // Temukan level tanah rata-rata
        int avgGroundLevel = calculateAverageGroundLevel(center, radius);

        // Level terrain secara bertahap (tidak terlalu drastis)
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location loc = center.clone().add(x, 0, z);
                int distance = (int) Math.sqrt(x * x + z * z);

                if (distance <= radius) {
                    smoothTerrainAtLocation(loc, avgGroundLevel, distance, radius);
                }
            }
        }
    }

    /**
     * Membuat foundation yang natural
     */
    private void createNaturalFoundation(Location center, String structureName, Biome biome) {
        MaterialPalette palette = getBiomePalette(biome);
        int radius = getStructureRadius(structureName) - 2;
        World world = center.getWorld();

        Random random = new Random();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location loc = center.clone().add(x, -1, z);
                int distance = (int) Math.sqrt(x * x + z * z);

                if (distance <= radius && random.nextDouble() < 0.7) {
                    // Pilih material foundation secara acak dari palette
                    Material foundationMaterial = palette.getRandomFoundation(random);

                    // Tempatkan dengan variasi kedalaman
                    int depth = random.nextInt(2) + 1;
                    for (int y = 0; y < depth; y++) {
                        Block block = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - y, loc.getBlockZ());
                        if (!block.getType().isSolid() || block.getType() == Material.DIRT ||
                                block.getType() == Material.GRASS_BLOCK) {
                            block.setType(foundationMaterial);
                        }
                    }
                }
            }
        }
    }

    /**
     * Membuat path yang natural menuju struktur
     */
    private void createNaturalPaths(Location center, String structureName, Biome biome) {
        MaterialPalette palette = getBiomePalette(biome);
        Random random = new Random();

        // Buat 2-4 path dari arah yang berbeda
        int numPaths = random.nextInt(3) + 2;

        for (int i = 0; i < numPaths; i++) {
            double angle = (2 * Math.PI * i) / numPaths + (random.nextDouble() - 0.5) * 0.5;
            createSinglePath(center, angle, palette, random);
        }
    }

    private void createSinglePath(Location center, double angle, MaterialPalette palette, Random random) {
        World world = center.getWorld();
        Material pathMaterial = palette.getRandomPath(random);

        int pathLength = random.nextInt(15) + 10;
        double x = 0, z = 0;

        for (int i = 0; i < pathLength; i++) {
            x += Math.cos(angle) * (0.8 + random.nextDouble() * 0.4);
            z += Math.sin(angle) * (0.8 + random.nextDouble() * 0.4);

            Location pathLoc = center.clone().add(x, 0, z);
            pathLoc.setY(findGroundLevel(pathLoc));

            // Buat path dengan lebar bervariasi
            int width = random.nextInt(2) + 1;
            for (int dx = -width; dx <= width; dx++) {
                for (int dz = -width; dz <= width; dz++) {
                    if (random.nextDouble() < 0.8) {
                        Location pathBlockLoc = pathLoc.clone().add(dx, 0, dz);
                        Block pathBlock = world.getBlockAt(pathBlockLoc);

                        if (pathBlock.getType() == Material.GRASS_BLOCK ||
                                pathBlock.getType() == Material.DIRT ||
                                pathBlock.getType() == Material.SAND) {
                            pathBlock.setType(pathMaterial);
                        }
                    }
                }
            }

            // Tambah variasi ke angle untuk path yang lebih natural
            angle += (random.nextDouble() - 0.5) * 0.3;
        }
    }

    /**
     * Menambahkan vegetasi dan dekorasi natural
     */
    private void addNaturalVegetation(Location center, String structureName, Biome biome) {
        MaterialPalette palette = getBiomePalette(biome);
        Random random = new Random();
        World world = center.getWorld();

        int radius = getStructureRadius(structureName) + 5;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                Location loc = center.clone().add(x, 0, z);
                loc.setY(findGroundLevel(loc));

                int distance = (int) Math.sqrt(x * x + z * z);

                // Probabilitas menurun dengan jarak
                double probability = Math.max(0.1, 1.0 - (distance / (double) radius));

                if (random.nextDouble() < probability * 0.3) {
                    Block groundBlock = world.getBlockAt(loc);
                    Block aboveBlock = world.getBlockAt(loc.add(0, 1, 0));

                    if ((groundBlock.getType() == Material.GRASS_BLOCK ||
                            groundBlock.getType() == Material.SAND ||
                            groundBlock.getType() == Material.DIRT) &&
                            aboveBlock.getType() == Material.AIR) {

                        Material decoration = palette.getRandomDecoration(random);
                        aboveBlock.setType(decoration);
                    }
                }
            }
        }
    }

    /**
     * Menyesuaikan struktur dengan elevasi terrain
     */
    private void adjustToElevation(Location center, String structureName) {
        // Implementasi untuk menyesuaikan tinggi struktur dengan terrain
        World world = center.getWorld();
        int radius = getStructureRadius(structureName);

        // Temukan titik-titik key di sekitar struktur
        List<Integer> elevations = new ArrayList<>();

        for (int angle = 0; angle < 360; angle += 45) {
            double radians = Math.toRadians(angle);
            int x = (int) (center.getX() + radius * Math.cos(radians));
            int z = (int) (center.getZ() + radius * Math.sin(radians));

            elevations.add(findGroundLevel(new Location(world, x, 0, z)));
        }

        // Jika ada variasi elevasi yang besar, buat terracing
        int minElevation = Collections.min(elevations);
        int maxElevation = Collections.max(elevations);

        if (maxElevation - minElevation > 5) {
            createTerracing(center, structureName, minElevation, maxElevation);
        }
    }

    /**
     * Membuat terracing untuk medan yang berbukit
     */
    private void createTerracing(Location center, String structureName, int minElevation, int maxElevation) {
        World world = center.getWorld();
        int radius = getStructureRadius(structureName);

        // Buat beberapa level teras
        int numLevels = Math.min(4, (maxElevation - minElevation) / 2);

        for (int level = 0; level < numLevels; level++) {
            int terraceHeight = minElevation + (level * (maxElevation - minElevation) / numLevels);
            int terraceRadius = radius - (level * radius / numLevels);

            createTerrace(center, terraceRadius, terraceHeight, world);
        }
    }

    private void createTerrace(Location center, int radius, int height, World world) {
        Random random = new Random();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distance = (int) Math.sqrt(x * x + z * z);

                if (distance == radius || distance == radius - 1) {
                    // Buat retaining wall
                    Location loc = center.clone().add(x, height, z);

                    for (int y = 0; y < 3; y++) {
                        Block block = world.getBlockAt(loc.getBlockX(), loc.getBlockY() + y, loc.getBlockZ());
                        if (block.getType() == Material.AIR) {
                            // Pilih material yang sesuai dengan biome
                            Material wallMaterial = random.nextBoolean() ? Material.COBBLESTONE : Material.STONE;
                            block.setType(wallMaterial);
                        }
                    }
                }
            }
        }
    }

    // Helper methods
    private int calculateAverageGroundLevel(Location center, int radius) {
        World world = center.getWorld();
        int totalHeight = 0;
        int count = 0;

        for (int x = -radius; x <= radius; x += 2) {
            for (int z = -radius; z <= radius; z += 2) {
                Location loc = center.clone().add(x, 0, z);
                totalHeight += findGroundLevel(loc);
                count++;
            }
        }

        return count > 0 ? totalHeight / count : 64;
    }

    private void smoothTerrainAtLocation(Location location, int targetLevel, int distance, int maxRadius) {
        World world = location.getWorld();
        int currentGroundLevel = findGroundLevel(location);

        // Smooth factor berdasarkan jarak (lebih halus di pinggir)
        double smoothFactor = 1.0 - ((double) distance / maxRadius);
        smoothFactor = Math.max(0.1, smoothFactor);

        int adjustedLevel = (int) (currentGroundLevel + (targetLevel - currentGroundLevel) * smoothFactor);

        if (adjustedLevel > currentGroundLevel) {
            // Tambah tanah
            for (int y = currentGroundLevel; y < adjustedLevel; y++) {
                Block block = world.getBlockAt(location.getBlockX(), y, location.getBlockZ());
                if (block.getType() == Material.AIR) {
                    block.setType(Material.DIRT);
                }
            }
            // Top layer dengan grass
            Block topBlock = world.getBlockAt(location.getBlockX(), adjustedLevel, location.getBlockZ());
            if (topBlock.getType() == Material.DIRT) {
                topBlock.setType(Material.GRASS_BLOCK);
            }
        } else if (adjustedLevel < currentGroundLevel) {
            // Kurangi tanah
            for (int y = currentGroundLevel; y > adjustedLevel; y--) {
                Block block = world.getBlockAt(location.getBlockX(), y, location.getBlockZ());
                if (block.getType().isSolid()) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

    private int findGroundLevel(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
            Material blockType = world.getBlockAt(x, y, z).getType();
            Material blockAbove = world.getBlockAt(x, y + 1, z).getType();

            if (blockType.isSolid() && (blockAbove == Material.AIR || !blockAbove.isSolid())) {
                return y;
            }
        }

        return 64; // Default sea level
    }

    private MaterialPalette getBiomePalette(Biome biome) {
        String biomeName = biome.toString().toUpperCase();

        // Cari palette yang cocok
        for (String key : BIOME_PALETTES.keySet()) {
            if (biomeName.contains(key)) {
                return BIOME_PALETTES.get(key);
            }
        }

        // Default ke plains jika tidak ada yang cocok
        return BIOME_PALETTES.get("PLAINS");
    }

    private int getStructureRadius(String structureName) {
        switch (structureName.toLowerCase()) {
            case "village_plains":
            case "village_desert":
            case "village_savanna":
            case "village_snowy":
            case "village_taiga":
                return 25;
            case "pillager_outpost":
                return 15;
            case "desert_pyramid":
            case "jungle_pyramid":
                return 12;
            case "igloo":
                return 8;
            case "witch_hut":
                return 6;
            case "ocean_monument":
                return 35;
            case "woodland_mansion":
                return 40;
            default:
                return 15;
        }
    }

    // Material palette class
    private static class MaterialPalette {
        private final List<Material> foundation = new ArrayList<>();
        private final List<Material> walls = new ArrayList<>();
        private final List<Material> roofs = new ArrayList<>();
        private final List<Material> paths = new ArrayList<>();
        private final List<Material> decorations = new ArrayList<>();

        public MaterialPalette addFoundation(Material... materials) {
            foundation.addAll(Arrays.asList(materials));
            return this;
        }

        public MaterialPalette addWall(Material... materials) {
            walls.addAll(Arrays.asList(materials));
            return this;
        }

        public MaterialPalette addRoof(Material... materials) {
            roofs.addAll(Arrays.asList(materials));
            return this;
        }

        public MaterialPalette addPath(Material... materials) {
            paths.addAll(Arrays.asList(materials));
            return this;
        }

        public MaterialPalette addDecoration(Material... materials) {
            decorations.addAll(Arrays.asList(materials));
            return this;
        }

        public Material getRandomFoundation(Random random) {
            return foundation.isEmpty() ? Material.STONE : foundation.get(random.nextInt(foundation.size()));
        }

        public Material getRandomWall(Random random) {
            return walls.isEmpty() ? Material.COBBLESTONE : walls.get(random.nextInt(walls.size()));
        }

        public Material getRandomRoof(Random random) {
            return roofs.isEmpty() ? Material.OAK_PLANKS : roofs.get(random.nextInt(roofs.size()));
        }

        public Material getRandomPath(Random random) {
            return paths.isEmpty() ? Material.DIRT_PATH : paths.get(random.nextInt(paths.size()));
        }

        public Material getRandomDecoration(Random random) {
            return decorations.isEmpty() ? Material.SHORT_GRASS : decorations.get(random.nextInt(decorations.size()));
        }
    }
}