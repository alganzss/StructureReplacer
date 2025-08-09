package my.pikrew.structureReplacer;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class StructureReplacer extends JavaPlugin implements Listener, CommandExecutor {

    private SelectionManager selectionManager;
    private StructureManager structureManager;
    private ConfigManager configManager;
    private StructureListener structureListener;

    @Override
    public void onEnable() {
        // Initialize managers
        this.selectionManager = new SelectionManager();
        this.configManager = new ConfigManager(this);
        this.structureManager = new StructureManager(this);
        this.structureListener = new StructureListener(this, structureManager, configManager);

        // Load config
        configManager.loadConfig();

        // Register events
        getServer().getPluginManager().registerEvents(this.structureListener, this);
        getServer().getPluginManager().registerEvents(this.selectionManager, this);

        // Register commands
        getCommand("structurereplacer").setExecutor(this);
        getCommand("sr").setExecutor(this);

        getLogger().info("StructureReplacer plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        configManager.saveConfig();
        getLogger().info("StructureReplacer plugin has been disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cCommand ini hanya bisa digunakan oleh player!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand":
                selectionManager.giveWand(player);
                player.sendMessage("§aAnda mendapat selection wand! Klik kiri untuk pos1, klik kanan untuk pos2.");
                break;

            case "save":
                if (args.length < 2) {
                    player.sendMessage("§cGunakan: /sr save <nama_structure>");
                    return true;
                }
                saveStructure(player, args[1]);
                break;

            case "replace":
                if (args.length < 3) {
                    player.sendMessage("§cGunakan: /sr replace <vanilla_structure> <custom_structure>");
                    return true;
                }
                replaceStructure(player, args[1], args[2]);
                break;

            case "list":
                listStructures(player);
                break;

            case "listreplace":
                listReplacements(player);
                break;

            case "remove":
                if (args.length < 2) {
                    player.sendMessage("§cGunakan: /sr remove <vanilla_structure>");
                    return true;
                }
                removeReplacement(player, args[1]);
                break;

            case "reload":
                reloadPlugin(player);
                break;

            default:
                showHelp(player);
                break;
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage("§6=== StructureReplacer Commands ===");
        player.sendMessage("§e/sr wand §7- Dapatkan selection tool");
        player.sendMessage("§e/sr save <nama> §7- Simpan area yang dipilih sebagai structure");
        player.sendMessage("§e/sr replace <vanilla> <custom> §7- Ganti vanilla structure dengan custom structure");
        player.sendMessage("§e/sr list §7- Lihat daftar custom structure");
        player.sendMessage("§e/sr listreplace §7- Lihat daftar penggantian structure");
        player.sendMessage("§e/sr remove <vanilla> §7- Hapus penggantian structure");
        player.sendMessage("§e/sr reload §7- Reload plugin dan config");
    }

    private void saveStructure(Player player, String name) {
        if (!selectionManager.hasSelection(player)) {
            player.sendMessage("§cAnda belum memilih area! Gunakan wand untuk memilih area.");
            return;
        }

        Selection selection = selectionManager.getSelection(player);

        try {
            structureManager.saveStructure(name, selection);
            player.sendMessage("§aStructure '" + name + "' berhasil disimpan!");
        } catch (IOException e) {
            player.sendMessage("§cGagal menyimpan structure: " + e.getMessage());
            getLogger().log(Level.SEVERE, "Failed to save structure", e);
        }
    }

    private void replaceStructure(Player player, String vanillaStructure, String customStructure) {
        if (!structureManager.structureExists(customStructure)) {
            player.sendMessage("§cCustom structure '" + customStructure + "' tidak ditemukan!");
            return;
        }

        configManager.addReplacement(vanillaStructure, customStructure);
        configManager.saveConfig();

        player.sendMessage("§aVanilla structure '" + vanillaStructure + "' akan diganti dengan '" + customStructure + "'");
        player.sendMessage("§eGunakan /sr reload untuk menerapkan perubahan.");
    }

    private void listStructures(Player player) {
        Map<String, File> structures = structureManager.getAvailableStructures();

        if (structures.isEmpty()) {
            player.sendMessage("§cTidak ada custom structure yang tersimpan.");
            return;
        }

        player.sendMessage("§6=== Custom Structures ===");
        for (String name : structures.keySet()) {
            player.sendMessage("§e- " + name);
        }
    }

    private void listReplacements(Player player) {
        Map<String, String> replacements = configManager.getReplacements();

        if (replacements.isEmpty()) {
            player.sendMessage("§cTidak ada penggantian structure yang aktif.");
            return;
        }

        player.sendMessage("§6=== Structure Replacements ===");
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            player.sendMessage("§e" + entry.getKey() + " §7-> §a" + entry.getValue());
        }
    }

    private void removeReplacement(Player player, String vanillaStructure) {
        if (configManager.removeReplacement(vanillaStructure)) {
            configManager.saveConfig();
            player.sendMessage("§aPenggantian untuk '" + vanillaStructure + "' berhasil dihapus!");
            player.sendMessage("§eGunakan /sr reload untuk menerapkan perubahan.");
        } else {
            player.sendMessage("§cPenggantian untuk '" + vanillaStructure + "' tidak ditemukan!");
        }
    }

    private void reloadPlugin(Player player) {
        configManager.loadConfig();
        structureListener.reloadReplacements();
        player.sendMessage("§aPlugin berhasil di-reload!");
    }

    // Getters for managers
    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    public StructureManager getStructureManager() {
        return structureManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}

// Selection class - handles area selection for structure saving
class Selection {
    private Location pos1;
    private Location pos2;

    public void setPos1(Location pos1) {
        this.pos1 = pos1.clone();
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2.clone();
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null && pos1.getWorld().equals(pos2.getWorld());
    }

    public World getWorld() {
        if (!isComplete()) {
            return null;
        }
        return pos1.getWorld();
    }

    public int getMinX() {
        return Math.min(pos1.getBlockX(), pos2.getBlockX());
    }

    public int getMaxX() {
        return Math.max(pos1.getBlockX(), pos2.getBlockX());
    }

    public int getMinY() {
        return Math.min(pos1.getBlockY(), pos2.getBlockY());
    }

    public int getMaxY() {
        return Math.max(pos1.getBlockY(), pos2.getBlockY());
    }

    public int getMinZ() {
        return Math.min(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public int getMaxZ() {
        return Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public int getWidth() {
        return getMaxX() - getMinX() + 1;
    }

    public int getHeight() {
        return getMaxY() - getMinY() + 1;
    }

    public int getLength() {
        return getMaxZ() - getMinZ() + 1;
    }

    public int getVolume() {
        return getWidth() * getHeight() * getLength();
    }

    public Location getMinPoint() {
        return new Location(getWorld(), getMinX(), getMinY(), getMinZ());
    }

    public Location getMaxPoint() {
        return new Location(getWorld(), getMaxX(), getMaxY(), getMaxZ());
    }
}