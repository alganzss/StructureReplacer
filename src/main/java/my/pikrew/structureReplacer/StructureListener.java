package my.pikrew.structureReplacer;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;

import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

public class StructureListener implements Listener {

    private JavaPlugin plugin;
    private StructureManager structureManager;
    private ConfigManager configManager;
    private TerrainAdapter terrainAdapter;
    private Map<String, String> replacements;

    public StructureListener(JavaPlugin plugin, StructureManager structureManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.structureManager = structureManager;
        this.configManager = configManager;
        this.terrainAdapter = new TerrainAdapter(plugin, configManager);

        // Set terrain adapter ke structure manager
        structureManager.setTerrainAdapter(terrainAdapter);
        structureManager.setConfigManager(configManager);

        reloadReplacements();
    }

    public void reloadReplacements() {
        this.replacements = configManager.getReplacements();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onStructureGrow(StructureGrowEvent event) {
        if (!configManager.isEnabled()) {
            return;
        }

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Structure grow detected at " + event.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkPopulate(org.bukkit.event.world.ChunkPopulateEvent event) {
        if (!configManager.isEnabled()) {
            return;
        }

        // Delay untuk memastikan chunk sudah fully populated
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            checkAndReplaceStructures(event.getChunk());
        }, 5L); // Increased delay untuk stability
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        if (!configManager.isEnabled()) {
            return;
        }

        if (event.isNewChunk()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                checkAndReplaceStructures(event.getChunk());
            }, 8L); // Longer delay for new chunks
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldInit(org.bukkit.event.world.WorldInitEvent event) {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("World " + event.getWorld().getName() + " initialized. Structure replacement active.");
        }
    }

    private boolean hasVerticalStructure(org.bukkit.Chunk chunk, Material material, int minHeight) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int consecutiveHeight = 0;
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    if (world.getBlockAt(chunkX + x, y, chunkZ + z).getType() == material) {
                        consecutiveHeight++;
                        if (consecutiveHeight >= minHeight) {
                            return true;
                        }
                    } else {
                        consecutiveHeight = 0;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasLargeStructureFootprint(org.bukkit.Chunk chunk, Material material, int minBlocks) {
        return countMaterialInChunk(chunk, material) >= minBlocks;
    }

    private boolean hasSmallStructureFootprint(org.bukkit.Chunk chunk, Material material, int maxSize) {
        int count = countMaterialInChunk(chunk, material);
        return count > 0 && count <= maxSize * maxSize;
    }

    private boolean hasMassiveStructure(org.bukkit.Chunk chunk, Material material, int minBlocks) {
        return countMaterialInChunk(chunk, material) >= minBlocks;
    }

    private boolean isOverWater(org.bukkit.Chunk chunk) {
        return hasBlockTypeInChunkAtLevel(chunk, Material.WATER, 62, 65);
    }

    private int countMaterialInChunk(org.bukkit.Chunk chunk, Material material) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;
        int count = 0;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    if (world.getBlockAt(chunkX + x, y, chunkZ + z).getType() == material) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private boolean hasBlockTypeInChunkAtLevel(org.bukkit.Chunk chunk, Material material, int minY, int maxY) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (world.getBlockAt(chunkX + x, y, chunkZ + z).getType() == material) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasBlockTypeInChunk(org.bukkit.Chunk chunk, Material material) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    if (world.getBlockAt(chunkX + x, y, chunkZ + z).getType() == material) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Location findStructureInChunk(org.bukkit.Chunk chunk, String structureName) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    Location loc = new Location(world, chunkX + x, y, chunkZ + z);
                    if (isStructureOrigin(loc, structureName)) {
                        return loc;
                    }
                }
            }
        }
        return null;
    }

    private boolean isStructureOrigin(Location location, String structureName) {
        switch (structureName.toLowerCase()) {
            case "village_plains":
            case "village_desert":
            case "village_savanna":
            case "village_snowy":
            case "village_taiga":
                return isVillageCenter(location);
            case "pillager_outpost":
                return isPillagerOutpostBase(location);
            case "desert_pyramid":
                return isDesertPyramidBase(location);
            case "jungle_pyramid":
                return isJunglePyramidBase(location);
            case "igloo":
                return isIglooBase(location);
            case "witch_hut":
                return isWitchHutBase(location);
            case "ocean_monument":
                return isOceanMonumentBase(location);
            case "woodland_mansion":
                return isWoodlandMansionBase(location);
            default:
                return false;
        }
    }

    private boolean isVillageCenter(Location location) {
        Material blockType = location.getBlock().getType();
        return blockType == Material.BELL ||
                blockType == Material.COBBLESTONE ||
                hasNearbyBlocks(location, 5, Material.BELL, Material.COBBLESTONE_WALL);
    }

    private boolean isPillagerOutpostBase(Location location) {
        return hasNearbyBlocks(location, 3, Material.DARK_OAK_LOG, Material.COBBLESTONE);
    }

    private boolean isDesertPyramidBase(Location location) {
        return location.getBlock().getType() == Material.SANDSTONE &&
                hasNearbyBlocks(location, 5, Material.SANDSTONE);
    }

    private boolean isJunglePyramidBase(Location location) {
        return hasNearbyBlocks(location, 3, Material.COBBLESTONE, Material.MOSSY_COBBLESTONE);
    }

    private boolean isIglooBase(Location location) {
        return location.getBlock().getType() == Material.SNOW_BLOCK &&
                hasNearbyBlocks(location, 3, Material.SNOW_BLOCK);
    }

    private boolean isWitchHutBase(Location location) {
        return hasNearbyBlocks(location, 3, Material.SPRUCE_PLANKS, Material.CAULDRON);
    }

    private boolean isOceanMonumentBase(Location location) {
        return hasNearbyBlocks(location, 5, Material.PRISMARINE, Material.SEA_LANTERN);
    }

    private boolean isWoodlandMansionBase(Location location) {
        return hasNearbyBlocks(location, 5, Material.DARK_OAK_PLANKS, Material.COBBLESTONE);
    }

    private boolean hasNearbyBlocks(Location center, int radius, Material... materials) {
        World world = center.getWorld();
        for (Material material : materials) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Location checkLoc = center.clone().add(x, y, z);
                        if (world.getBlockAt(checkLoc).getType() == material) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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

    private int getStructureClearRadius(String structureName) {
        switch (structureName.toLowerCase()) {
            case "village_plains":
            case "village_desert":
            case "village_savanna":
            case "village_snowy":
            case "village_taiga":
                return 30;
            case "pillager_outpost":
                return 20;
            case "desert_pyramid":
            case "jungle_pyramid":
                return 15;
            case "igloo":
                return 10;
            case "witch_hut":
                return 8;
            case "ocean_monument":
                return 60;
            case "woodland_mansion":
                return 50;
            default:
                return 20;
        }
    }

    private String formatLocation(Location loc) {
        return String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private void checkAndReplaceStructures(org.bukkit.Chunk chunk) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX() * 16;
        int chunkZ = chunk.getZ() * 16;

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Checking chunk [" + chunk.getX() + "," + chunk.getZ() + "] for structures...");
        }

        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            String vanillaStructure = replacement.getKey();
            String customStructure = replacement.getValue();

            if (!structureManager.structureExists(customStructure)) {
                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().warning("Custom structure not found: " + customStructure);
                }
                continue;
            }

            if (mightContainStructure(chunk, vanillaStructure)) {
                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().info("Potential " + vanillaStructure + " detected in chunk [" + chunk.getX() + "," + chunk.getZ() + "]");
                }

                Location structureLocation = findStructureInChunk(chunk, vanillaStructure);

                if (structureLocation != null) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Confirmed " + vanillaStructure + " at " + formatLocation(structureLocation) +
                                ", replacing with " + customStructure);
                    }

                    // Multiple-stage replacement untuk hasil yang lebih natural
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        performNaturalStructureReplacement(structureLocation, vanillaStructure, customStructure);
                    }, 10L); // Delay lebih lama untuk proses yang lebih kompleks
                }
            }
        }
    }

    /**
     * Performs natural structure replacement dengan multiple stages
     */
    private void performNaturalStructureReplacement(Location structureLocation, String vanillaStructure, String customStructure) {
        try {
            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Starting natural replacement process for " + vanillaStructure);
            }

            // Stage 1: Pre-analysis - analyze surrounding terrain
            TerrainAnalysis analysis = analyzeTerrainAround(structureLocation, vanillaStructure);

            // Stage 2: Smart clearing - only clear what's necessary
            performSmartClearing(structureLocation, vanillaStructure, analysis);

            // Stage 3: Place structure with adaptation
            Location adjustedLocation = calculateOptimalPlacement(structureLocation, vanillaStructure, analysis);

            // Use enhanced structure manager with terrain adaptation
            structureManager.pasteStructureWithAdaptation(customStructure, adjustedLocation, true);

            // Stage 4: Post-placement integration
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                integrateWithSurroundings(adjustedLocation, customStructure, analysis);
            }, 20L); // Wait for structure to be placed

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Successfully replaced " + vanillaStructure + " with " + customStructure +
                        " using natural integration at " + formatLocation(adjustedLocation));
            }

        } catch (IOException e) {
            plugin.getLogger().warning("Failed to place custom structure " + customStructure + ": " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().severe("Unexpected error during natural structure replacement: " + e.getMessage());
        }
    }

    /**
     * Analyze terrain around structure location
     */
    private TerrainAnalysis analyzeTerrainAround(Location center, String structureName) {
        World world = center.getWorld();
        TerrainAnalysis analysis = new TerrainAnalysis();

        int radius = getAnalysisRadius(structureName);
        analysis.biome = world.getBiome(center.getBlockX(), center.getBlockY(), center.getBlockZ());
        analysis.centerLocation = center.clone();

        // Analyze elevation points
        for (int angle = 0; angle < 360; angle += 30) {
            double radians = Math.toRadians(angle);
            int x = (int) (center.getX() + radius * Math.cos(radians));
            int z = (int) (center.getZ() + radius * Math.sin(radians));

            int groundLevel = findGroundLevel(new Location(world, x, 64, z));
            analysis.elevationPoints.add(groundLevel);
        }

        // Calculate terrain statistics
        analysis.avgElevation = analysis.elevationPoints.stream().mapToInt(Integer::intValue).sum() / analysis.elevationPoints.size();
        analysis.minElevation = analysis.elevationPoints.stream().mapToInt(Integer::intValue).min().orElse(64);
        analysis.maxElevation = analysis.elevationPoints.stream().mapToInt(Integer::intValue).max().orElse(64);
        analysis.elevationVariance = analysis.maxElevation - analysis.minElevation;

        // Analyze dominant materials in area
        analysis.dominantMaterials = analyzeDominantMaterials(center, radius);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Terrain analysis: Biome=" + analysis.biome +
                    ", Avg elevation=" + analysis.avgElevation +
                    ", Variance=" + analysis.elevationVariance);
        }

        return analysis;
    }

    private Map<Material, Integer> analyzeDominantMaterials(Location center, int radius) {
        Map<Material, Integer> materialCount = new java.util.HashMap<>();
        World world = center.getWorld();
        Random random = new Random();

        // Sample materials dalam radius (tidak semua block untuk performance)
        for (int i = 0; i < 50; i++) {
            int x = (int) (center.getX() + (random.nextDouble() - 0.5) * radius * 2);
            int z = (int) (center.getZ() + (random.nextDouble() - 0.5) * radius * 2);
            int y = findGroundLevel(new Location(world, x, 64, z));

            Material groundMaterial = world.getBlockAt(x, y, z).getType();
            materialCount.put(groundMaterial, materialCount.getOrDefault(groundMaterial, 0) + 1);
        }

        return materialCount;
    }

    /**
     * Smart clearing yang hanya clear area yang benar-benar diperlukan
     */
    private void performSmartClearing(Location location, String structureName, TerrainAnalysis analysis) {
        int clearRadius = getSmartClearRadius(structureName, analysis.elevationVariance);
        World world = location.getWorld();

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Smart clearing with radius " + clearRadius + " for " + structureName);
        }

        // Clear dengan pattern yang lebih natural
        Random random = new Random();

        for (int x = -clearRadius; x <= clearRadius; x++) {
            for (int z = -clearRadius; z <= clearRadius; z++) {
                int distance = (int) Math.sqrt(x * x + z * z);

                if (distance <= clearRadius) {
                    // Probabilitas clearing menurun dengan jarak
                    double clearProbability = 1.0 - ((double) distance / clearRadius);
                    clearProbability = Math.pow(clearProbability, 1.5); // Smooth falloff

                    if (random.nextDouble() < clearProbability) {
                        Location clearLoc = location.clone().add(x, 0, z);
                        clearVerticallyAtLocation(clearLoc, structureName, analysis);
                    }
                }
            }
        }
    }

    private void clearVerticallyAtLocation(Location location, String structureName, TerrainAnalysis analysis) {
        World world = location.getWorld();
        int groundLevel = findGroundLevel(location);
        int clearHeight = getClearHeight(structureName);

        // Clear dari ground level ke atas
        for (int y = 0; y < clearHeight; y++) {
            Block block = world.getBlockAt(location.getBlockX(), groundLevel + y, location.getBlockZ());

            // Hanya clear block yang perlu di-clear
            if (shouldClearBlock(block.getType(), structureName)) {
                block.setType(Material.AIR);
            }
        }
    }

    private boolean shouldClearBlock(Material material, String structureName) {
        // Jangan clear natural elements yang bagus untuk tetap ada
        if (material == Material.WATER || material == Material.LAVA) {
            return false;
        }

        // Preserve some natural decorations
        if (material.name().contains("FLOWER") ||
                material.name().contains("MUSHROOM") ||
                material == Material.SHORT_GRASS) {
            return Math.random() < 0.3; // 30% chance to clear
        }

        // Clear man-made structures
        return material.isSolid() || material.name().contains("LOG") || material.name().contains("PLANKS");
    }

    /**
     * Calculate optimal placement considering terrain
     */
    private Location calculateOptimalPlacement(Location original, String structureName, TerrainAnalysis analysis) {
        Location optimal = original.clone();

        // Adjust Y based on terrain analysis
        if (analysis.elevationVariance > 10) {
            // High variance terrain - place on average level
            optimal.setY(analysis.avgElevation);
        } else {
            // Low variance - use standard adjustment
            optimal.setY(findGroundLevel(original));
        }

        // Fine-tune based on structure type
        optimal = adjustPlacementForStructureType(optimal, structureName, analysis);

        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Optimal placement calculated: " + formatLocation(optimal) +
                    " (original: " + formatLocation(original) + ")");
        }

        return optimal;
    }

    private Location adjustPlacementForStructureType(Location location, String structureName, TerrainAnalysis analysis) {
        Location adjusted = location.clone();

        switch (structureName.toLowerCase()) {
            case "village_plains":
            case "village_desert":
            case "village_savanna":
            case "village_snowy":
            case "village_taiga":
                // Villages prefer flat areas, slight elevation above average
                adjusted.setY(analysis.avgElevation + 1);
                break;

            case "pillager_outpost":
                // Outposts prefer elevated positions for strategic advantage
                adjusted.setY(Math.max(analysis.avgElevation + 2, analysis.maxElevation - 3));
                break;

            case "desert_pyramid":
            case "jungle_pyramid":
                // Pyramids need flat foundation
                adjusted.setY(analysis.avgElevation);
                break;

            case "witch_hut":
                // Witch huts in swamps, prefer water level
                if (analysis.biome.toString().contains("SWAMP")) {
                    adjusted.setY(Math.max(62, analysis.avgElevation));
                } else {
                    adjusted.setY(analysis.avgElevation + 1);
                }
                break;

            case "igloo":
                // Igloos prefer higher, snowy areas
                adjusted.setY(Math.max(analysis.avgElevation, analysis.maxElevation - 2));
                break;

            case "ocean_monument":
                // Ocean monuments go underwater
                adjusted.setY(Math.min(45, analysis.minElevation - 5));
                break;

            case "woodland_mansion":
                // Mansions prefer elevated forest clearings
                adjusted.setY(analysis.avgElevation + 3);
                break;

            default:
                adjusted.setY(analysis.avgElevation);
                break;
        }

        return adjusted;
    }

    /**
     * Integrate structure with surroundings after placement
     */
    private void integrateWithSurroundings(Location structureLocation, String structureName, TerrainAnalysis analysis) {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Integrating " + structureName + " with surroundings");
        }

        // Add connecting elements
        addConnectingPaths(structureLocation, structureName, analysis);

        // Blend edges
        blendStructureEdges(structureLocation, structureName, analysis);

        // Add environmental details
        addEnvironmentalDetails(structureLocation, structureName, analysis);

        // Add random natural elements
        addRandomNaturalElements(structureLocation, structureName, analysis);
    }

    private void addConnectingPaths(Location center, String structureName, TerrainAnalysis analysis) {
        if (!shouldHavePaths(structureName)) {
            return;
        }

        World world = center.getWorld();
        Random random = new Random();
        Material pathMaterial = getPathMaterialForBiome(analysis.biome);

        // Create 2-3 paths leading away from structure
        int numPaths = random.nextInt(2) + 2;

        for (int i = 0; i < numPaths; i++) {
            double angle = (2 * Math.PI * i) / numPaths + (random.nextDouble() - 0.5) * 0.5;
            createNaturalPath(center, angle, pathMaterial, 15 + random.nextInt(10));
        }
    }

    private void createNaturalPath(Location start, double angle, Material pathMaterial, int length) {
        World world = start.getWorld();
        Random random = new Random();

        double x = 0, z = 0;

        for (int i = 0; i < length; i++) {
            // Path meander naturally
            angle += (random.nextDouble() - 0.5) * 0.2;

            x += Math.cos(angle) * (0.7 + random.nextDouble() * 0.6);
            z += Math.sin(angle) * (0.7 + random.nextDouble() * 0.6);

            Location pathLoc = start.clone().add(x, 0, z);
            int groundY = findGroundLevel(pathLoc);
            pathLoc.setY(groundY);

            // Create path with random width
            int width = random.nextInt(2) + 1;

            for (int dx = -width; dx <= width; dx++) {
                for (int dz = -width; dz <= width; dz++) {
                    if (random.nextDouble() < 0.7) {
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
        }
    }

    private void blendStructureEdges(Location center, String structureName, TerrainAnalysis analysis) {
        World world = center.getWorld();
        Random random = new Random();
        int radius = getStructureRadius(structureName) + 2;

        // Find structure boundary and blend with terrain
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int distance = (int) Math.sqrt(x * x + z * z);

                if (distance >= radius - 3 && distance <= radius) {
                    Location edgeLoc = center.clone().add(x, 0, z);
                    blendLocationWithTerrain(edgeLoc, analysis, random);
                }
            }
        }
    }

    private void blendLocationWithTerrain(Location location, TerrainAnalysis analysis, Random random) {
        World world = location.getWorld();
        int groundY = findGroundLevel(location);

        // Add some terrain variation
        if (random.nextDouble() < 0.3) {
            Material blendMaterial = getDominantTerrainMaterial(analysis);

            // Place blend blocks
            for (int y = 0; y < 2; y++) {
                if (random.nextDouble() < 0.5) {
                    Block block = world.getBlockAt(location.getBlockX(), groundY + y, location.getBlockZ());
                    if (block.getType() == Material.AIR) {
                        block.setType(blendMaterial);
                    }
                }
            }
        }
    }

    private void addEnvironmentalDetails(Location center, String structureName, TerrainAnalysis analysis) {
        World world = center.getWorld();
        Random random = new Random();
        int radius = getStructureRadius(structureName) + 5;

        // Add biome-specific details around structure
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * radius;

            int x = (int) (center.getX() + Math.cos(angle) * distance);
            int z = (int) (center.getZ() + Math.sin(angle) * distance);
            int y = findGroundLevel(new Location(world, x, 64, z));

            Location detailLoc = new Location(world, x, y + 1, z);
            Block detailBlock = world.getBlockAt(detailLoc);

            if (detailBlock.getType() == Material.AIR) {
                Material detail = getEnvironmentalDetail(analysis.biome, random);
                if (detail != null) {
                    detailBlock.setType(detail);
                }
            }
        }
    }

    private void addRandomNaturalElements(Location center, String structureName, TerrainAnalysis analysis) {
        World world = center.getWorld();
        Random random = new Random();

        // Add some boulders or natural features
        if (random.nextDouble() < 0.4) {
            addNaturalBoulders(center, analysis, random);
        }

        // Add water features if appropriate
        if (shouldHaveWaterFeature(analysis.biome, structureName) && random.nextDouble() < 0.3) {
            addWaterFeature(center, analysis, random);
        }

        // Add vegetation clusters
        if (random.nextDouble() < 0.6) {
            addVegetationClusters(center, analysis, random);
        }
    }

    private void addNaturalBoulders(Location center, TerrainAnalysis analysis, Random random) {
        World world = center.getWorld();
        Material boulderMaterial = getBoulderMaterial(analysis.biome);

        int numBoulders = random.nextInt(3) + 1;

        for (int i = 0; i < numBoulders; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 10 + random.nextDouble() * 15;

            int x = (int) (center.getX() + Math.cos(angle) * distance);
            int z = (int) (center.getZ() + Math.sin(angle) * distance);
            int y = findGroundLevel(new Location(world, x, 64, z));

            // Create small boulder cluster
            int boulderSize = random.nextInt(2) + 1;

            for (int bx = 0; bx < boulderSize; bx++) {
                for (int bz = 0; bz < boulderSize; bz++) {
                    for (int by = 0; by < boulderSize; by++) {
                        if (random.nextDouble() < 0.7) {
                            Block boulderBlock = world.getBlockAt(x + bx, y + by + 1, z + bz);
                            if (boulderBlock.getType() == Material.AIR) {
                                boulderBlock.setType(boulderMaterial);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addWaterFeature(Location center, TerrainAnalysis analysis, Random random) {
        World world = center.getWorld();

        // Create small pond or stream
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = 8 + random.nextDouble() * 12;

        int x = (int) (center.getX() + Math.cos(angle) * distance);
        int z = (int) (center.getZ() + Math.sin(angle) * distance);
        int y = findGroundLevel(new Location(world, x, 64, z)) - 1;

        // Create small water feature
        int featureSize = random.nextInt(3) + 2;

        for (int wx = -featureSize; wx <= featureSize; wx++) {
            for (int wz = -featureSize; wz <= featureSize; wz++) {
                int waterDistance = (int) Math.sqrt(wx * wx + wz * wz);

                if (waterDistance <= featureSize && random.nextDouble() < 0.8) {
                    Block waterBlock = world.getBlockAt(x + wx, y, z + wz);
                    if (waterBlock.getType().isSolid()) {
                        waterBlock.setType(Material.WATER);
                    }
                }
            }
        }
    }

    private void addVegetationClusters(Location center, TerrainAnalysis analysis, Random random) {
        World world = center.getWorld();
        Material vegetation = getVegetationForBiome(analysis.biome, random);

        if (vegetation == null) return;

        int numClusters = random.nextInt(3) + 2;

        for (int i = 0; i < numClusters; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 5 + random.nextDouble() * 15;

            int centerX = (int) (center.getX() + Math.cos(angle) * distance);
            int centerZ = (int) (center.getZ() + Math.sin(angle) * distance);

            // Create cluster
            int clusterSize = random.nextInt(4) + 2;

            for (int j = 0; j < clusterSize * clusterSize; j++) {
                int x = centerX + random.nextInt(clusterSize) - clusterSize / 2;
                int z = centerZ + random.nextInt(clusterSize) - clusterSize / 2;
                int y = findGroundLevel(new Location(world, x, 64, z));

                Block vegBlock = world.getBlockAt(x, y + 1, z);
                Block groundBlock = world.getBlockAt(x, y, z);

                if (vegBlock.getType() == Material.AIR &&
                        (groundBlock.getType() == Material.GRASS_BLOCK ||
                                groundBlock.getType() == Material.DIRT) &&
                        random.nextDouble() < 0.6) {
                    vegBlock.setType(vegetation);
                }
            }
        }
    }

    // Helper methods untuk material selection
    private Material getPathMaterialForBiome(Biome biome) {
        String biomeName = biome.toString().toUpperCase();

        if (biomeName.contains("DESERT")) return Material.SANDSTONE;
        if (biomeName.contains("SNOWY")) return Material.PACKED_ICE;
        if (biomeName.contains("SWAMP")) return Material.MUD;
        if (biomeName.contains("TAIGA")) return Material.COARSE_DIRT;
        if (biomeName.contains("SAVANNA")) return Material.COARSE_DIRT;
        if (biomeName.contains("JUNGLE")) return Material.PODZOL;

        return Material.DIRT_PATH; // Default
    }

    private Material getDominantTerrainMaterial(TerrainAnalysis analysis) {
        return analysis.dominantMaterials.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Material.GRASS_BLOCK);
    }

    private Material getEnvironmentalDetail(Biome biome, Random random) {
        String biomeName = biome.toString().toUpperCase();

        if (biomeName.contains("DESERT")) {
            Material[] details = {Material.DEAD_BUSH, Material.CACTUS};
            return details[random.nextInt(details.length)];
        } else if (biomeName.contains("JUNGLE")) {
            Material[] details = {Material.JUNGLE_SAPLING, Material.COCOA, Material.VINE};
            return details[random.nextInt(details.length)];
        } else if (biomeName.contains("TAIGA")) {
            Material[] details = {Material.FERN, Material.LARGE_FERN, Material.SWEET_BERRY_BUSH};
            return details[random.nextInt(details.length)];
        } else if (biomeName.contains("PLAINS")) {
            Material[] details = {Material.SHORT_GRASS, Material.DANDELION, Material.POPPY};
            return details[random.nextInt(details.length)];
        } else if (biomeName.contains("SWAMP")) {
            Material[] details = {Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.LILY_PAD};
            return details[random.nextInt(details.length)];
        }

        return null;
    }

    private Material getBoulderMaterial(Biome biome) {
        String biomeName = biome.toString().toUpperCase();

        if (biomeName.contains("DESERT")) return Material.SANDSTONE;
        if (biomeName.contains("SNOWY")) return Material.PACKED_ICE;
        if (biomeName.contains("JUNGLE")) return Material.MOSSY_COBBLESTONE;
        if (biomeName.contains("OCEAN")) return Material.PRISMARINE;

        return Material.COBBLESTONE; // Default
    }

    private Material getVegetationForBiome(Biome biome, Random random) {
        String biomeName = biome.toString().toUpperCase();

        if (biomeName.contains("JUNGLE")) {
            Material[] veg = {Material.JUNGLE_SAPLING, Material.LARGE_FERN, Material.FERN};
            return veg[random.nextInt(veg.length)];
        } else if (biomeName.contains("TAIGA")) {
            Material[] veg = {Material.FERN, Material.LARGE_FERN, Material.SPRUCE_SAPLING};
            return veg[random.nextInt(veg.length)];
        } else if (biomeName.contains("PLAINS")) {
            Material[] veg = {Material.SHORT_GRASS, Material.TALL_GRASS, Material.DANDELION};
            return veg[random.nextInt(veg.length)];
        } else if (biomeName.contains("SAVANNA")) {
            Material[] veg = {Material.SHORT_GRASS, Material.ACACIA_SAPLING};
            return veg[random.nextInt(veg.length)];
        }

        return Material.SHORT_GRASS;
    }

    // Utility methods
    private boolean shouldHavePaths(String structureName) {
        return structureName.toLowerCase().contains("village") ||
                structureName.toLowerCase().contains("outpost") ||
                structureName.toLowerCase().contains("mansion");
    }

    private boolean shouldHaveWaterFeature(Biome biome, String structureName) {
        String biomeName = biome.toString().toUpperCase();
        return !biomeName.contains("DESERT") && !biomeName.contains("NETHER") &&
                !structureName.toLowerCase().contains("pyramid");
    }

    private int getAnalysisRadius(String structureName) {
        return getStructureRadius(structureName) + 10;
    }

    private int getSmartClearRadius(String structureName, int elevationVariance) {
        int baseRadius = getStructureClearRadius(structureName);

        // Reduce clearing on varied terrain
        if (elevationVariance > 15) {
            return Math.max(5, baseRadius - 5);
        } else if (elevationVariance > 8) {
            return Math.max(8, baseRadius - 2);
        }

        return baseRadius;
    }

    private int getClearHeight(String structureName) {
        switch (structureName.toLowerCase()) {
            case "village_plains":
            case "village_desert":
            case "village_savanna":
            case "village_snowy":
            case "village_taiga":
                return 15;
            case "woodland_mansion":
                return 25;
            case "pillager_outpost":
                return 20;
            default:
                return 12;
        }
    }

    // Keep all the original methods for structure detection
    private boolean mightContainStructure(org.bukkit.Chunk chunk, String structureName) {
        switch (structureName.toLowerCase()) {
            case "village_plains":
                return containsVillageBlocks(chunk) && isInPlainsLikeBiome(chunk);
            case "village_desert":
                return containsDesertVillageBlocks(chunk) && isInDesertBiome(chunk);
            case "village_savanna":
                return containsVillageBlocks(chunk) && isInSavannaBiome(chunk);
            case "village_snowy":
                return containsSnowyVillageBlocks(chunk) && isInSnowyBiome(chunk);
            case "village_taiga":
                return containsTaigaVillageBlocks(chunk) && isInTaigaBiome(chunk);
            case "pillager_outpost":
                return containsPillagerOutpostBlocks(chunk) && isInOutpostBiome(chunk);
            case "desert_pyramid":
                return containsDesertPyramidBlocks(chunk) && isInDesertBiome(chunk);
            case "jungle_pyramid":
                return containsJunglePyramidBlocks(chunk) && isInJungleBiome(chunk);
            case "igloo":
                return containsIglooBlocks(chunk) && isInSnowyBiome(chunk);
            case "witch_hut":
                return containsWitchHutBlocks(chunk) && isInSwampBiome(chunk);
            case "ocean_monument":
                return containsOceanMonumentBlocks(chunk) && isInOceanBiome(chunk);
            case "woodland_mansion":
                return containsWoodlandMansionBlocks(chunk) && isInDarkForestBiome(chunk);
            default:
                return false;
        }
    }

    // All the biome detection and structure detection methods remain the same as in original
    private boolean isInPlainsLikeBiome(org.bukkit.Chunk chunk) {
        org.bukkit.block.Biome biome = chunk.getBlock(8, 64, 8).getBiome();
        String biomeName = biome.toString();
        return biomeName.contains("PLAINS") || biomeName.contains("MEADOW");
    }

    private boolean isInDesertBiome(org.bukkit.Chunk chunk) {
        org.bukkit.block.Biome biome = chunk.getBlock(8, 64, 8).getBiome();
        String biomeName = biome.toString();
        return biomeName.contains("DESERT");
    }

    private boolean isInSavannaBiome(org.bukkit.Chunk chunk) {
        org.bukkit.block.Biome biome = chunk.getBlock(8, 64, 8).getBiome();
        String biomeName = biome.toString();
        return biomeName.contains("SAVANNA");
    }

    private boolean isInSnowyBiome(org.bukkit.Chunk chunk) {
        org.bukkit.block.Biome biome = chunk.getBlock(8, 64, 8).getBiome();
        String biomeName = biome.toString();
        return biomeName.contains("SNOWY") || biomeName.contains("FROZEN") ||
                biomeName.contains("ICE");
    }

    private boolean isInTaigaBiome(org.bukkit.Chunk chunk) {
        org.bukkit.block.Biome biome = chunk.getBlock(8, 64, 8).getBiome();
        String biomeName = biome.toString();
        return biomeName.contains("TAIGA");
    }

    private boolean isInOutpostBiome(org.bukkit.Chunk chunk) {
        org.bukkit.block.Biome biome = chunk.getBlock(8, 64, 8).getBiome();
        String biomeName = biome.toString();
        return biomeName.contains("PLAINS") || biomeName.contains("DESERT") ||
                biomeName.contains("SAVANNA") || biomeName.contains("TAIGA");
    }

    private boolean isInJungleBiome(org.bukkit.Chunk chunk) {
        org.bukkit.block.Biome biome = chunk.getBlock(8, 64, 8).getBiome();
        String biomeName = biome.toString();
        return biomeName.contains("JUNGLE");
    }

    private boolean isInSwampBiome(org.bukkit.Chunk chunk) {
        org.bukkit.block.Biome biome = chunk.getBlock(8, 64, 8).getBiome();
        String biomeName = biome.toString();
        return biomeName.contains("SWAMP");
    }

    private boolean isInOceanBiome(org.bukkit.Chunk chunk) {
        org.bukkit.block.Biome biome = chunk.getBlock(8, 64, 8).getBiome();
        String biomeName = biome.toString();
        return biomeName.contains("OCEAN") || biomeName.contains("SEA");
    }

    private boolean isInDarkForestBiome(org.bukkit.Chunk chunk) {
        org.bukkit.block.Biome biome = chunk.getBlock(8, 64, 8).getBiome();
        String biomeName = biome.toString();
        return biomeName.contains("DARK_FOREST") || biomeName.contains("ROOFED_FOREST");
    }

    // Include all the block detection methods from original StructureListener
    private boolean containsVillageBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 3,
                Material.COBBLESTONE,
                Material.OAK_PLANKS,
                Material.OAK_LOG,
                Material.DIRT_PATH,
                Material.HAY_BLOCK,
                Material.BELL,
                Material.COMPOSTER
        );
    }

    private boolean containsDesertVillageBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 3,
                Material.SANDSTONE,
                Material.SMOOTH_SANDSTONE,
                Material.SANDSTONE_STAIRS,
                Material.DIRT_PATH,
                Material.HAY_BLOCK,
                Material.BELL,
                Material.DEAD_BUSH
        );
    }

    private boolean containsSnowyVillageBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 3,
                Material.SPRUCE_PLANKS,
                Material.SPRUCE_LOG,
                Material.COBBLESTONE,
                Material.SNOW_BLOCK,
                Material.DIRT_PATH,
                Material.BELL,
                Material.CAMPFIRE
        );
    }

    private boolean containsTaigaVillageBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 3,
                Material.SPRUCE_PLANKS,
                Material.SPRUCE_LOG,
                Material.COBBLESTONE,
                Material.DIRT_PATH,
                Material.BELL,
                Material.CAMPFIRE,
                Material.SWEET_BERRY_BUSH
        );
    }

    private boolean containsPillagerOutpostBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 2,
                Material.DARK_OAK_PLANKS,
                Material.DARK_OAK_LOG,
                Material.COBBLESTONE,
                Material.DARK_OAK_FENCE,
                Material.WHITE_BANNER,
                Material.IRON_BARS
        ) && hasVerticalStructure(chunk, Material.DARK_OAK_LOG, 10);
    }

    private boolean containsDesertPyramidBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 2,
                Material.SANDSTONE,
                Material.CHISELED_SANDSTONE,
                Material.SANDSTONE_STAIRS,
                Material.SANDSTONE_SLAB,
                Material.TNT,
                Material.STONE_PRESSURE_PLATE
        ) && hasLargeStructureFootprint(chunk, Material.SANDSTONE, 15);
    }

    private boolean containsJunglePyramidBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 2,
                Material.COBBLESTONE,
                Material.MOSSY_COBBLESTONE,
                Material.JUNGLE_LOG,
                Material.JUNGLE_LEAVES,
                Material.REDSTONE_WIRE,
                Material.STICKY_PISTON,
                Material.TRIPWIRE_HOOK
        );
    }

    private boolean containsIglooBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 2,
                Material.SNOW_BLOCK,
                Material.ICE,
                Material.RED_CARPET,
                Material.FURNACE,
                Material.RED_BED
        ) && hasSmallStructureFootprint(chunk, Material.SNOW_BLOCK, 8);
    }

    private boolean containsWitchHutBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 2,
                Material.SPRUCE_PLANKS,
                Material.SPRUCE_LOG,
                Material.MUSHROOM_STEM,
                Material.CAULDRON,
                Material.CRAFTING_TABLE,
                Material.FLOWER_POT
        ) && isOverWater(chunk);
    }

    private boolean containsOceanMonumentBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 2,
                Material.PRISMARINE,
                Material.PRISMARINE_BRICKS,
                Material.DARK_PRISMARINE,
                Material.SEA_LANTERN,
                Material.SPONGE,
                Material.WET_SPONGE
        ) && hasLargeStructureFootprint(chunk, Material.PRISMARINE, 30);
    }

    private boolean containsWoodlandMansionBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 3,
                Material.DARK_OAK_PLANKS,
                Material.DARK_OAK_LOG,
                Material.COBBLESTONE,
                Material.WHITE_WOOL,
                Material.BLUE_WOOL,
                Material.BOOKSHELF,
                Material.REDSTONE_TORCH
        ) && hasMassiveStructure(chunk, Material.DARK_OAK_PLANKS, 50);
    }

    // Helper methods
    private boolean hasBlocksInChunk(org.bukkit.Chunk chunk, int minRequired, Material... materials) {
        int foundCount = 0;
        for (Material material : materials) {
            if (hasBlockTypeInChunk(chunk, material)) {
                foundCount++;
                if (foundCount >= minRequired) {
                    return true;
                }
            }
        }
        return false;
    }

    // TerrainAnalysis class untuk menyimpan data analisis terrain
    private static class TerrainAnalysis {
        public Biome biome;
        public Location centerLocation;
        public java.util.List<Integer> elevationPoints = new java.util.ArrayList<>();
        public int avgElevation;
        public int minElevation;
        public int maxElevation;
        public int elevationVariance;
        public Map<Material, Integer> dominantMaterials = new java.util.HashMap<>();
    }
}
