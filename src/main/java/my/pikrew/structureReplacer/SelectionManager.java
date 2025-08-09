package my.pikrew.structureReplacer;

import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;

public class SelectionManager implements Listener {

    private Map<UUID, Selection> selections = new HashMap<>();
    private static final Material WAND_MATERIAL = Material.WOODEN_AXE;

    public void giveWand(Player player) {
        ItemStack wand = new ItemStack(WAND_MATERIAL);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName("§6Structure Selection Wand");
        meta.setLore(Arrays.asList(
                "§7Klik kiri untuk set pos1",
                "§7Klik kanan untuk set pos2"
        ));
        wand.setItemMeta(meta);

        player.getInventory().addItem(wand);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != WAND_MATERIAL) {
            return;
        }

        if (item.getItemMeta() == null ||
                !item.getItemMeta().hasDisplayName() ||
                !item.getItemMeta().getDisplayName().contains("Selection Wand")) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        event.setCancelled(true);

        Location loc = event.getClickedBlock().getLocation();
        UUID playerId = player.getUniqueId();

        if (!selections.containsKey(playerId)) {
            selections.put(playerId, new Selection());
        }

        Selection selection = selections.get(playerId);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selection.setPos1(loc);
            player.sendMessage("§aPos1 set ke: §e" + formatLocation(loc));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selection.setPos2(loc);
            player.sendMessage("§aPos2 set ke: §e" + formatLocation(loc));
        }

        if (selection.isComplete()) {
            int volume = selection.getVolume();
            player.sendMessage("§aSelection selesai! Volume: §e" + volume + " blocks");
        }
    }

    private String formatLocation(Location loc) {
        return String.format("(%d, %d, %d)", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public boolean hasSelection(Player player) {
        Selection selection = selections.get(player.getUniqueId());
        return selection != null && selection.isComplete();
    }

    public Selection getSelection(Player player) {
        return selections.get(player.getUniqueId());
    }

    public void clearSelection(Player player) {
        selections.remove(player.getUniqueId());
    }
}
