package my.pikrew.structureReplacer;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {

    private JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;
    private Map<String, String> replacements;

    // Cache untuk settings yang sering diakses
    private volatile boolean enabled = true;
    private volatile boolean debugEnabled = false;
    private volatile boolean terrainBlendingEnabled = true;
    private volatile int maxStructuresPerChunk = 1;
    private volatile int structureDetectionRadius = 20;

    // Performance settings
    private volatile int chunkProcessingDelay = 5;
    private volatile int maxConcurrentReplacements = 3;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.replacements = new ConcurrentHashMap<>();

        // Create plugin data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
    }

    public void loadConfig() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        loadReplacements();
        loadCachedSettings();
    }

    public void saveConfig() {
        if (config == null) {
            return;
        }

        // Save replacements to config
        config.set("replacements", null); // Clear existing
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            config.set("replacements." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    private void createDefaultConfig() {
        config = new YamlConfiguration();

        // Basic settings
        config.set("version", "1.1");
        config.set("enable-structure-replacement", true);
        config.set("debug", false);
        config.set("terrain-blending", true);

        // Performance settings
        config.set("performance.max-structures-per-chunk", 1);
        config.set("performance.structure-detection-radius", 20);
        config.set("performance.chunk-processing-delay-ticks", 5);
        config.set("performance.max-concurrent-replacements", 3);
        config.set("performance.use-async-processing", true);
        config.set("performance.cache-chunk-analysis", true);

        // Terrain blending settings
        config.set("terrain-blending.blend-radius-multiplier", 1.0);
        config.set("terrain-blending.vegetation-density", 0.3);
        config.set("terrain-blending.add-biome-features", true);

        // Example replacements with more variety
        config.set("replacements.village_plains", "custom_plains_village");
        config.set("replacements.village_desert", "custom_desert_village");
        config.set("replacements.pillager_outpost", "custom_outpost");
        config.set("replacements.desert_pyramid", "custom_pyramid");
        config.set("replacements.witch_hut", "custom_witch_hut");

        // Add helpful comments
        config.setComments("version", java.util.Arrays.asList(
                "StructureReplacer Configuration File",
                "Version 1.1 - Optimized with terrain blending"
        ));

        config.setComments("enable-structure-replacement",
                java.util.Arrays.asList("Set to false to disable all structure replacements"));

        config.setComments("terrain-blending",
                java.util.Arrays.asList("Enable automatic terrain adjustment around replaced structures"));

        config.setComments("performance",
                java.util.Arrays.asList("Performance optimization settings"));

        config.setComments("performance.max-structures-per-chunk",
                java.util.Arrays.asList("Maximum number of structures to replace per chunk (prevents lag)"));

        config.setComments("performance.chunk-processing-delay-ticks",
                java.util.Arrays.asList("Delay in ticks before processing chunks (higher = less lag)"));

        config.setComments("replacements",
                java.util.Arrays.asList(
                        "Structure replacements: vanilla_structure: custom_structure",
                        "Available vanilla structures:",
                        "- village_plains, village_desert, village_savanna, village_taiga, village_snowy",
                        "- pillager_outpost, desert_pyramid, jungle_pyramid, igloo, witch_hut",
                        "- ocean_monument, woodland_mansion, ruined_portal, shipwreck"
                ));

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create config.yml: " + e.getMessage());
        }
    }

    private void loadReplacements() {
        replacements.clear();

        if (!config.contains("replacements")) {
            return;
        }

        Set<String> keys = config.getConfigurationSection("replacements").getKeys(false);
        for (String key : keys) {
            String value = config.getString("replacements." + key);
            if (value != null && !value.isEmpty()) {
                replacements.put(key.toLowerCase(), value); // Normalize to lowercase
            }
        }

        plugin.getLogger().info("Loaded " + replacements.size() + " structure replacements");
    }

    private void loadCachedSettings() {
        // Cache frequently accessed settings untuk menghindari repeated config calls
        enabled = config.getBoolean("enable-structure-replacement", true);
        debugEnabled = config.getBoolean("debug", false);
        terrainBlendingEnabled = config.getBoolean("terrain-blending", true);
        maxStructuresPerChunk = config.getInt("performance.max-structures-per-chunk", 1);
        structureDetectionRadius = config.getInt("performance.structure-detection-radius", 20);
        chunkProcessingDelay = config.getInt("performance.chunk-processing-delay-ticks", 5);
        maxConcurrentReplacements = config.getInt("performance.max-concurrent-replacements", 3);
    }

    public void addReplacement(String vanillaStructure, String customStructure) {
        replacements.put(vanillaStructure.toLowerCase(), customStructure);
    }

    public boolean removeReplacement(String vanillaStructure) {
        return replacements.remove(vanillaStructure.toLowerCase()) != null;
    }

    public Map<String, String> getReplacements() {
        return new HashMap<>(replacements);
    }

    public String getReplacement(String vanillaStructure) {
        return replacements.get(vanillaStructure.toLowerCase());
    }

    public boolean hasReplacement(String vanillaStructure) {
        return replacements.containsKey(vanillaStructure.toLowerCase());
    }

    // Cached getters untuk performance
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isTerrainBlendingEnabled() {
        return terrainBlendingEnabled;
    }

    public int getMaxStructuresPerChunk() {
        return maxStructuresPerChunk;
    }

    public int getStructureDetectionRadius() {
        return structureDetectionRadius;
    }

    public int getChunkProcessingDelay() {
        return chunkProcessingDelay;
    }

    public int getMaxConcurrentReplacements() {
        return maxConcurrentReplacements;
    }

    public boolean isAsyncProcessingEnabled() {
        return config.getBoolean("performance.use-async-processing", true);
    }

    public boolean isCacheEnabled() {
        return config.getBoolean("performance.cache-chunk-analysis", true);
    }

    // Terrain blending settings
    public double getBlendRadiusMultiplier() {
        return config.getDouble("terrain-blending.blend-radius-multiplier", 1.0);
    }

    public double getVegetationDensity() {
        return config.getDouble("terrain-blending.vegetation-density", 0.3);
    }

    public boolean shouldAddBiomeFeatures() {
        return config.getBoolean("terrain-blending.add-biome-features", true);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        config.set("enable-structure-replacement", enabled);
    }

    public void setDebug(boolean debug) {
        this.debugEnabled = debug;
        config.set("debug", debug);
    }

    public void setTerrainBlending(boolean terrainBlending) {
        this.terrainBlendingEnabled = terrainBlending;
        config.set("terrain-blending", terrainBlending);
    }

    public void reloadCachedSettings() {
        loadCachedSettings();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    // Helper method untuk mendapatkan semua struktur yang tersedia
    public Set<String> getAllAvailableVanillaStructures() {
        return Set.of(
                "village_plains", "village_desert", "village_savanna", "village_taiga", "village_snowy",
                "pillager_outpost", "desert_pyramid", "jungle_pyramid", "igloo", "witch_hut",
                "ocean_monument", "woodland_mansion", "ruined_portal", "shipwreck", "buried_treasure"
        );
    }
}