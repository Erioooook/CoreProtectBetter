package net.coreprotect.listener;

import net.coreprotect.CoreProtect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Слушатель для логирования срабатываний нажимных плит.
 * Быстрый вариант логирования: вызывает hook.logPlacement с pseudoUser "pp:<actor>".
 */
public class PressurePlateListener implements Listener {

    private final Plugin plugin;
    private final CoreProtectHook coreProtectHook;
    private final Map<UUID, String> itemOwnerMap = new ConcurrentHashMap<>();

    public PressurePlateListener(Plugin plugin, CoreProtectHook coreProtectHook) {
        this.plugin = plugin;
        this.coreProtectHook = coreProtectHook;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPhysical(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!isPressurePlate(block.getType())) return;

        Player player = event.getPlayer();
        handlePressurePlateActivation(block.getLocation(), "player:" + player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        Block block = event.getBlock();
        if (block == null) return;
        if (!isPressurePlate(block.getType())) return;

        Entity entity = event.getEntity();
        String actor = detectActorFromEntity(entity);
        handlePressurePlateActivation(block.getLocation(), actor);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        itemOwnerMap.put(item.getUniqueId(), event.getPlayer().getName());
        // автo-удаление через 30 секунд
        new BukkitRunnable() {
            @Override
            public void run() {
                itemOwnerMap.remove(item.getUniqueId());
            }
        }.runTaskLater(plugin, 20L * 30);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        itemOwnerMap.remove(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        itemOwnerMap.remove(event.getItem().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        // Оставлено для расширения: можно помечать источник диспенсера
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        // При необходимости можно пометить shooter -> actor
    }

    private boolean isPressurePlate(Material m) {
        String name = m.name();
        return name.endsWith("_PRESSURE_PLATE") || name.equals("SENSITIVE_PRESSURE_PLATE");
    }

    private String detectActorFromEntity(Entity entity) {
        if (entity instanceof Player) {
            return "player:" + ((Player) entity).getName();
        }
        if (entity instanceof Item) {
            Item item = (Item) entity;
            String owner = itemOwnerMap.get(item.getUniqueId());
            if (owner != null) return "item_from:" + owner;
            return "item_entity";
        }
        // Projectile shooter handling можно добавить при необходимости
        return entity.getType().name().toLowerCase();
    }

    private void handlePressurePlateActivation(Location loc, String actor) {
        if (coreProtectHook != null && coreProtectHook.isEnabled()) {
            String pseudoUser = "pp:" + actor;
            Block block = loc.getBlock();
            BlockData bd = block.getBlockData();
            coreProtectHook.logPlacement(pseudoUser, loc, block.getType(), bd);
            return;
        }
        // fallback для отладки
        Bukkit.getLogger().info("[CoreProtectBetter] pressure_plate at " + loc + " activated by " + actor);
    }

    public interface CoreProtectHook {
        boolean isEnabled();
        void logPlacement(String user, Location loc, Material material, BlockData data);
    }
}
