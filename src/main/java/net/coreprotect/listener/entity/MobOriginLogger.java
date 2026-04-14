package net.coreprotect.listener.entity;

import org.bukkit.Location;
import org.bukkit.entity.*;
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

    private void log(String prefix, Entity mob, Location loc) {
        String mobName = mob.getType().name();
        String target = getTargetName(mob);
        String tag = "#" + mobName + "-" + target + "-" + prefix;

        Queue.queueCustom(tag, loc);
    }

    // -----------------------------
    // EXPLOSIONS (Ghast, Creeper, Wither, EnderDragon, etc.)
    // -----------------------------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobExplosion(EntityExplodeEvent event) {
        Entity source = event.getEntity();

        if (source instanceof Mob || source instanceof Explosive) {
            log("explosion", source, event.getLocation());
        }
    }

    // -----------------------------
    // FIRE (Ghast fireball, Blaze fireball, Wither skull)
    // -----------------------------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobFire(BlockIgniteEvent event) {
        Entity igniter = event.getIgnitingEntity();
        if (igniter == null) return;

        if (igniter instanceof Projectile) {
            Projectile p = (Projectile) igniter;
            if (p.getShooter() instanceof Entity) {
                igniter = (Entity) p.getShooter();
            }
        }

        if (igniter instanceof Mob) {
            log("fire", igniter, event.getBlock().getLocation());
        }
    }

    // -----------------------------
    // BLOCK DESTRUCTION (Enderman, Ravager, Wither, Dragon)
    // -----------------------------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobBlockChange(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();

        if (entity instanceof Mob || entity instanceof EnderDragon) {
            log("destruction", entity, event.getBlock().getLocation());
        }
    }

    // -----------------------------
    // PROJECTILE IMPACT (fireballs, wither skulls)
    // -----------------------------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onProjectileImpact(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        if (projectile.getShooter() instanceof Entity) {
            Entity shooter = (Entity) projectile.getShooter();

            if (shooter instanceof Mob) {
                Location loc = projectile.getLocation();
                log("projectile", shooter, loc);
            }
        }
    }
}
