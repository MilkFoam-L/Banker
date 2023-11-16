package pers.neige.banker.loot.impl

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import pers.neige.banker.loot.LootGenerator
import pers.neige.neigeitems.manager.ActionManager

class All(data: ConfigurationSection) : LootGenerator(data) {
    // 获取战利品动作
    private val lootAction = let {
        var lootAction = data.get("LootAction")
        if (lootAction !is List<*>) {
            lootAction = arrayListOf(lootAction)
        }
        lootAction as List<*>
    }

    override fun run(
        damageData: Map<String, Double>,
        sortedDamageData: List<Map.Entry<String, Double>>,
        totalDamage: Double,
        params: MutableMap<String, Any?>?
    ) {
        // 遍历玩家ID
        damageData.forEach { (name, damage) ->
            // 获取在线玩家
            val player = Bukkit.getPlayer(name)
            // 玩家不在线则停止执行
            if (player != null) {
                (params?.toMutableMap() ?: mutableMapOf()).also { map ->
                    map["damage"] = "%.2f".format(damage)
                    map["totalDamage"] = "%.2f".format(totalDamage)
                    // 执行动作
                    ActionManager.runAction(
                        player,
                        lootAction,
                        map,
                        map
                    )
                }
            }
        }
    }
}