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

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class StructureListener implements Listener {

    private JavaPlugin plugin;
    private StructureManager structureManager;
    private ConfigManager configManager;
    private Map<String, String> replacements;

    public StructureListener(JavaPlugin plugin, StructureManager structureManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.structureManager = structureManager;
        this.configManager = configManager;
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

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            checkAndReplaceStructures(event.getChunk());
        }, 2L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        if (!configManager.isEnabled()) {
            return;
        }

        if (event.isNewChunk()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                checkAndReplaceStructures(event.getChunk());
            }, 3L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldInit(org.bukkit.event.world.WorldInitEvent event) {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("World " + event.getWorld().getName() + " initialized. Structure replacement active.");
        }
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

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        performStructureReplacement(structureLocation, vanillaStructure, customStructure);
                    }, 1L);
                }
            }
        }
    }

    private void performStructureReplacement(Location structureLocation, String vanillaStructure, String customStructure) {
        try {
            if (shouldClearArea(vanillaStructure)) {
                clearStructureArea(structureLocation, vanillaStructure);
            }

            Location placementLoc = adjustPlacementLocation(structureLocation, vanillaStructure);
            structureManager.pasteStructure(customStructure, placementLoc);

            if (configManager.isDebugEnabled()) {
                plugin.getLogger().info("Successfully replaced " + vanillaStructure + " with " + customStructure +
                        " at " + formatLocation(structureLocation));
            }

        } catch (IOException e) {
            plugin.getLogger().warning("Failed to place custom structure " + customStructure + ": " + e.getMessage());
        } catch (Exception e) {
            plugin.getLogger().severe("Unexpected error during structure replacement: " + e.getMessage());
        }
    }

    private boolean shouldClearArea(String structureName) {
        switch (structureName.toLowerCase()) {
            case "village_plains":
            case "village_desert":
            case "village_savanna":
            case "village_snowy":
            case "village_taiga":
            case "pillager_outpost":
            case "woodland_mansion":
            case "ocean_monument":
                return true;

            case "desert_pyramid":
            case "jungle_pyramid":
            case "igloo":
            case "witch_hut":
                return false;

            default:
                return true;
        }
    }

    private Location adjustPlacementLocation(Location original, String structureName) {
        Location adjusted = original.clone();

        switch (structureName.toLowerCase()) {
            case "village_plains":
            case "village_desert":
            case "village_savanna":
            case "village_snowy":
            case "village_taiga":
                adjusted.setY(findGroundLevel(original));
                break;

            case "pillager_outpost":
                adjusted.setY(findGroundLevel(original) + 1);
                break;

            case "desert_pyramid":
            case "jungle_pyramid":
                adjusted.setY(findGroundLevel(original));
                break;

            case "witch_hut":
                adjusted.setY(Math.max(63, findGroundLevel(original)) + 1);
                break;

            case "igloo":
                adjusted.setY(findSnowLevel(original));
                break;

            case "ocean_monument":
                adjusted.setY(findSeaFloor(original));
                break;

            default:
                adjusted.setY(findGroundLevel(original));
                break;
        }

        return adjusted;
    }

    private int findGroundLevel(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
            Material blockType = world.getBlockAt(x, y, z).getType();
            Material blockAbove = world.getBlockAt(x, y + 1, z).getType();

            if (blockType.isSolid() && (blockAbove == Material.AIR || blockAbove == Material.SHORT_GRASS)) {
                return y + 1;
            }
        }

        return 64;
    }

    private int findSnowLevel(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--) {
            Material blockType = world.getBlockAt(x, y, z).getType();

            if (blockType == Material.SNOW_BLOCK || blockType == Material.ICE ||
                    blockType == Material.PACKED_ICE || blockType == Material.BLUE_ICE) {
                return y + 1;
            }
        }

        return findGroundLevel(location);
    }

    private int findSeaFloor(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();

        for (int y = 30; y >= world.getMinHeight(); y--) {
            Material blockType = world.getBlockAt(x, y, z).getType();
            Material blockAbove = world.getBlockAt(x, y + 1, z).getType();

            if (blockType.isSolid() && blockAbove == Material.WATER) {
                return y + 1;
            }
        }

        return 30;
    }

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

            case "ruined_portal":
                return containsRuinedPortalBlocks(chunk);

            case "shipwreck":
                return containsShipwreckBlocks(chunk) && isInOceanBiome(chunk);

            case "buried_treasure":
                return containsBuriedTreasureBlocks(chunk) && isNearBeach(chunk);

            default:
                return false;
        }
    }

    // Biome detection methods
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

    private boolean isNearBeach(org.bukkit.Chunk chunk) {
        org.bukkit.block.Biome biome = chunk.getBlock(8, 64, 8).getBiome();
        String biomeName = biome.toString();
        return biomeName.contains("BEACH") || biomeName.contains("SHORE") ||
                isNearWater(chunk);
    }

    private boolean isNearWater(org.bukkit.Chunk chunk) {
        return hasBlockTypeInChunk(chunk, Material.WATER);
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
                return isVillageCenter(location);
            case "pillager_outpost":
                return isPillagerOutpostBase(location);
            case "desert_pyramid":
                return isDesertPyramidBase(location);
            default:
                return false;
        }
    }

    private void clearStructureArea(Location location, String structureName) {
        int radius = getStructureClearRadius(structureName);
        World world = location.getWorld();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 0; y < 50; y++) {
                    Location blockLoc = location.clone().add(x, y, z);
                    world.getBlockAt(blockLoc).setType(Material.AIR);
                }
            }
        }
    }

    private int getStructureClearRadius(String structureName) {
        switch (structureName.toLowerCase()) {
            case "village_plains":
            case "village_desert":
            case "village_savanna":
            case "village_snowy":
            case "village_taiga":
                return 50;
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
                return 80;
            default:
                return 20;
        }
    }

    // Structure detection helper methods
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

    private boolean containsRuinedPortalBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 2,
                Material.OBSIDIAN,
                Material.CRYING_OBSIDIAN,
                Material.NETHERRACK,
                Material.MAGMA_BLOCK,
                Material.LAVA,
                Material.GOLD_BLOCK,
                Material.FIRE
        );
    }

    private boolean containsShipwreckBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 2,
                Material.OAK_PLANKS,
                Material.SPRUCE_PLANKS,
                Material.OAK_LOG,
                Material.SPRUCE_LOG,
                Material.OAK_FENCE,
                Material.CHEST,
                Material.TRAPPED_CHEST
        ) && isUnderWater(chunk);
    }

    private boolean containsBuriedTreasureBlocks(org.bukkit.Chunk chunk) {
        return hasBlocksInChunk(chunk, 1,
                Material.CHEST
        ) && hasBlocksInChunk(chunk, 2,
                Material.SAND,
                Material.SANDSTONE,
                Material.GRAVEL
        );
    }

    // Enhanced helper methods
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

    private boolean isUnderWater(org.bukkit.Chunk chunk) {
        return hasBlockTypeInChunkAtLevel(chunk, Material.WATER, 50, 70) &&
                countMaterialInChunk(chunk, Material.WATER) > 100;
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

    // Structure origin detection helper methods
    private boolean isVillageCenter(Location location) {
        Material blockType = location.getBlock().getType();
        return blockType == Material.BELL ||
                blockType == Material.COBBLESTONE ||
                hasNearbyBlocks(location, 5,
                        Material.BELL,
                        Material.COBBLESTONE_WALL);
    }

    private boolean isPillagerOutpostBase(Location location) {
        return hasNearbyBlocks(location, 3,
                Material.DARK_OAK_LOG,
                Material.COBBLESTONE);
    }

    private boolean isDesertPyramidBase(Location location) {
        return location.getBlock().getType() == Material.SANDSTONE &&
                hasNearbyBlocks(location, 5, Material.SANDSTONE);
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

    private String formatLocation(Location loc) {
        return String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
}