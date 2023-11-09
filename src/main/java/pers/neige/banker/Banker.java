package pers.neige.banker;

import org.bukkit.plugin.java.JavaPlugin;
import pers.neige.banker.loot.LootGenerator;
import pers.neige.banker.manager.LootManager;
import pers.neige.neigeitems.scanner.ClassScanner;

import java.util.HashSet;

public class Banker extends JavaPlugin {
    private static JavaPlugin INSTANCE;
    private ClassScanner scanner = null;

    public static JavaPlugin getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        HashSet<String> except = new HashSet<>();
        except.add("pers.neige.banker.libs");
        scanner = new ClassScanner(
                this,
                "pers.neige.banker",
                except
        );

        scanner.onEnable();
        for (Class<?> clazz : scanner.getClasses()) {
            if (LootGenerator.class.isAssignableFrom(clazz)) {
                LootManager.INSTANCE.addGenerator(clazz.getSimpleName().toUpperCase(), (Class<? extends LootGenerator>) clazz);
            }
        }
    }

    @Override
    public void onDisable() {
        scanner.onDisable();
    }
}