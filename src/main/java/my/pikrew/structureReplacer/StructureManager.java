package my.pikrew.structureReplacer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

    public StructureManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.structuresDir = new File(plugin.getDataFolder(), "structures");

        if (!structuresDir.exists()) {
            structuresDir.mkdirs();
        }
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

        // Save blocks
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

                    blocks.add(blockData);
                }
            }
        }

        config.set("blocks", blocks);
        config.save(structureFile);
    }

    public void pasteStructure(String name, Location location) throws IOException {
        File structureFile = new File(structuresDir, name + ".yml");

        if (!structureFile.exists()) {
            throw new IOException("Structure file not found: " + name);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(structureFile);
        List<Map<?, ?>> blocks = config.getMapList("blocks");

        World world = location.getWorld();
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();

        for (Map<?, ?> blockInfo : blocks) {
            int x = baseX + (Integer) blockInfo.get("x");
            int y = baseY + (Integer) blockInfo.get("y");
            int z = baseZ + (Integer) blockInfo.get("z");

            String materialName = (String) blockInfo.get("material");
            String blockDataString = (String) blockInfo.get("data");

            try {
                Material material = Material.valueOf(materialName);
                Block block = world.getBlockAt(x, y, z);

                block.setType(material);

                if (blockDataString != null && !blockDataString.isEmpty()) {
                    try {
                        BlockData blockData = plugin.getServer().createBlockData(blockDataString);
                        block.setBlockData(blockData);
                    } catch (IllegalArgumentException e) {
                        // Fallback if block data is invalid
                        plugin.getLogger().warning("Invalid block data for " + materialName + ": " + blockDataString);
                    }
                }

            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown material: " + materialName);
            }
        }
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