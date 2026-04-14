package net.coreprotect.listener.entity;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import net.coreprotect.consumer.Queue;

public class MobOriginLogger implements Listener {

    private String getTargetName(Entity entity) {
        if (entity instanceof Mob) {
            Mob mob = (Mob) entity;
            if (mob.getTarget() instanceof Player) {
                return mob.getTarget().getName();
            }
        }
        return "unknown";
    }

    private void log(Entity mob, Location loc, String type) {
        String mobName = mob.getType().name();
        String target = getTargetName(mob);

        String user = mobName + "-" + target + "-" + type;

        BlockState fake = loc.getBlock().getState();

        Queue.queueBlockBreak(user, fake, Material.AIR, "air", 0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        Entity e = event.getEntity();
        if (e instanceof Mob) {
            log(e, event.getLocation(), "explosion");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        Entity e = event.getIgnitingEntity();
        if (e == null) return;

        if (e instanceof Projectile) {
            Projectile p = (Projectile) e;
            if (p.getShooter() instanceof Entity) {
                e = (Entity) p.getShooter();
            }
        }

        if (e instanceof Mob) {
            log(e, event.getBlock().getLocation(), "fire");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockChange(EntityChangeBlockEvent event) {
        Entity e = event.getEntity();
        if (e instanceof Mob) {
            log(e, event.getBlock().getLocation(), "destruction");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile p = event.getEntity();
        if (p.getShooter() instanceof Entity) {
            Entity shooter = (Entity) p.getShooter();
            if (shooter instanceof Mob) {
                log(shooter, p.getLocation(), "projectile");
            }
        }
    }
}

        }
    }
}
