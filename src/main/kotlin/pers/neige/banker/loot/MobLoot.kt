package pers.neige.banker.loot

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import pers.neige.banker.manager.ConfigManager
import pers.neige.banker.manager.LootManager.parseGenerator
import java.util.concurrent.ConcurrentHashMap

/**
 * 怪物战利品配置
 *
 * @param configs 战利品ID: 对应的配置
 * @constructor 解析一系列单独的战利品配置, 组合即为怪物对应的战利品配置
 */
class MobLoot(configs: Map<String, ConfigurationSection>) {
    private val loots = ConcurrentHashMap<String, LootGenerator>()

    init {
        configs.forEach { (id, config) ->
            parseGenerator(config)?.also {
                loots[id] = it
            } ?: let {
                Bukkit.getLogger().info(ConfigManager.lootTypeError?.replace("{type}", config.getString("LootType") ?: "null"))
            }
        }
    }

    fun run(
        damageData: Map<String, Double>,
        sortedDamageData: List<Map.Entry<String, Double>>,
        totalDamage: Double,
        params: MutableMap<String, String>? = null
    ) {
        loots.forEach { (id, loot) ->
            loot.run(damageData, sortedDamageData, totalDamage, params)
        }
    }
}
