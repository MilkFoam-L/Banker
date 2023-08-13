package pers.neige.banker.loot.impl

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import pers.neige.banker.loot.LootGenerator
import pers.neige.neigeitems.manager.ActionManager
import kotlin.math.min

class Rank(data: ConfigurationSection) : LootGenerator(data) {
    // 获取战利品动作
    private val lootAction = let {
        var lootAction = data.get("LootAction")
        if (lootAction == null) {
            lootAction = arrayListOf<Any>()
        } else if (lootAction !is List<*>) {
            lootAction = arrayListOf(lootAction)
        }
        lootAction as List<*>
    }

    private val guaranteeAction: Any? = data.get("GuaranteeAction")

    override fun run(
        damageData: Map<String, Double>,
        sortedDamageData: List<Map.Entry<String, Double>>,
        totalDamage: Double,
        params: MutableMap<String, String>?
    ) {
        val length = min((lootAction).size, sortedDamageData.size)
        // 遍历每个战利品配置
        for (index in 0 until length) {
            // 获取玩家ID
            val name = sortedDamageData[index].key
            // 获取在线玩家, 玩家不在线则停止执行
            val player = Bukkit.getPlayer(name) ?: continue
            // 执行动作
            (params?.toMutableMap<String, Any?>() ?: mutableMapOf()).also { map ->
                map["rank"] = (index + 1).toString()
                map["damage"] = "%.2f".format(damageData[name])
                map["totalDamage"] = "%.2f".format(totalDamage)
                map["lootAmount"] = lootAction.size
                // 执行动作
                ActionManager.runAction(
                    player,
                    lootAction[index],
                    map,
                    map
                )
            }
        }
        // 如果存在没领到战利品的人
        if (sortedDamageData.size > lootAction.size) {
            // 如果存在保底战利品
            if (guaranteeAction != null) {
                // 发放保底战利品
                for (index in lootAction.size until sortedDamageData.size) {
                    // 获取玩家ID
                    val name = sortedDamageData[index].key
                    // 获取在线玩家, 玩家不在线则停止执行
                    val player = Bukkit.getPlayer(name) ?: continue
                    (params?.toMutableMap<String, Any?>() ?: mutableMapOf()).also { map ->
                        map["rank"] = (index + 1).toString()
                        map["damage"] = "%.2f".format(damageData[name])
                        map["totalDamage"] = "%.2f".format(totalDamage)
                        map["lootAmount"] = lootAction.size
                        // 执行动作
                        ActionManager.runAction(
                            player,
                            guaranteeAction,
                            map,
                            map
                        )
                    }
                }
            }
        }
    }
}