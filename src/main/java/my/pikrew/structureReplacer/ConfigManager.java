package my.pikrew.structureReplacer;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConfigManager {

    private JavaPlugin plugin;
    private File configFile;
    private FileConfiguration config;
    private Map<String, String> replacements;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.replacements = new HashMap<>();

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

        // Default configuration
        config.set("version", "1.0");
        config.set("enable-structure-replacement", true);
        config.set("debug", false);

        // Example replacements
        config.set("replacements.village_plains", "my_custom_village");
        config.set("replacements.pillager_outpost", "my_custom_outpost");

        // Add comments
        config.setComments("version", java.util.Arrays.asList("StructureReplacer Configuration File"));
        config.setComments("enable-structure-replacement",
                java.util.Arrays.asList("Set to false to disable all structure replacements"));
        config.setComments("replacements",
                java.util.Arrays.asList("Structure replacements: vanilla_structure: custom_structure"));

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
                replacements.put(key, value);
            }
        }

        plugin.getLogger().info("Loaded " + replacements.size() + " structure replacements");
    }

    public void addReplacement(String vanillaStructure, String customStructure) {
        replacements.put(vanillaStructure, customStructure);
    }

    public boolean removeReplacement(String vanillaStructure) {
        return replacements.remove(vanillaStructure) != null;
    }

    public Map<String, String> getReplacements() {
        return new HashMap<>(replacements);
    }

    public String getReplacement(String vanillaStructure) {
        return replacements.get(vanillaStructure);
    }

    public boolean hasReplacement(String vanillaStructure) {
        return replacements.containsKey(vanillaStructure);
    }

    public boolean isEnabled() {
        return config.getBoolean("enable-structure-replacement", true);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }

    public void setEnabled(boolean enabled) {
        config.set("enable-structure-replacement", enabled);
    }

    public void setDebug(boolean debug) {
        config.set("debug", debug);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}