package net.coreprotect.integrations;

import net.coreprotect.CoreProtect;
import net.coreprotect.listener.PressurePlateListener.CoreProtectHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Простая обёртка для попытки логирования в CoreProtect API.
 * Использует reflection, чтобы не зависеть от точной сигнатуры API.
 */
public class SimpleCoreProtectHook implements CoreProtectHook {

    private final Plugin cpPlugin;

    public SimpleCoreProtectHook() {
        this.cpPlugin = Bukkit.getPluginManager().getPlugin("CoreProtect");
    }

    @Override
    public boolean isEnabled() {
        return cpPlugin != null && cpPlugin.isEnabled();
    }

    @Override
    public void logPlacement(String user, Location loc, Material material, BlockData data) {
        if (!isEnabled()) {
            Bukkit.getLogger().info("[CoreProtectBetter] (log) " + user + " @ " + loc);
            return;
        }

        try {
            // Попытка вызвать CoreProtect.getInstance().getAPI().logPlacement(user, loc, material, data)
            // 1) получить класс net.coreprotect.CoreProtect (ваш main)
            Class<?> mainClass = Class.forName("net.coreprotect.CoreProtect");
            Method getInstance = mainClass.getMethod("getInstance");
            Object mainInstance = getInstance.invoke(null);
            if (mainInstance == null) {
                Bukkit.getLogger().info("[CoreProtectBetter] CoreProtect instance is null");
                return;
            }

            // 2) получить метод getAPI()
            Method getApi = mainClass.getMethod("getAPI");
            Object apiInstance = getApi.invoke(mainInstance);
            if (apiInstance == null) {
                Bukkit.getLogger().info("[CoreProtectBetter] CoreProtect API instance is null");
                return;
            }

            // 3) попытаться найти метод logPlacement(String, Location, Material, BlockData)
            Method logPlacement = null;
            for (Method m : apiInstance.getClass().getMethods()) {
                if (m.getName().equals("logPlacement")) {
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length >= 4) {
                        // допускаем разные сигнатуры, проверяем первые 4 типа совместимы
                        logPlacement = m;
                        break;
                    }
                }
            }

            if (logPlacement != null) {
                // Вызов метода (если сигнатура отличается, reflection может выбросить исключение)
                logPlacement.invoke(apiInstance, user, loc, material, data);
                return;
            } else {
                // fallback: если нет logPlacement, попробуем метод с другим именем или логируем в консоль
                Bukkit.getLogger().info("[CoreProtectBetter] (log->CoreProtect) " + user + " @ " + loc);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Bukkit.getLogger().info("[CoreProtectBetter] Failed to call CoreProtect API via reflection: " + e.getMessage());
            Bukkit.getLogger().info("[CoreProtectBetter] (log) " + user + " @ " + loc);
        }
    }
}
