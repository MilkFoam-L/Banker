package pers.neige.banker.loot.impl

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import pers.neige.banker.loot.LootGenerator
import pers.neige.neigeitems.manager.ActionManager
import pers.neige.neigeitems.utils.SamplingUtils

class Separate(data: ConfigurationSection) : LootGenerator(data) {
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
        params: MutableMap<String, Any?>?
    ) {
        val names = damageData.keys.toHashSet()
        // 遍历每个战利品配置
        for (action in lootAction) {
            // 随机选取玩家ID
            SamplingUtils.weight(damageData, totalDamage)?.let { name ->
                names.remove(name)
                // 获取在线玩家, 玩家不在线则停止执行
                Bukkit.getPlayer(name)?.let { player ->
                    (params?.toMutableMap() ?: mutableMapOf()).also { map ->
                        map["damage"] = "%.2f".format(damageData[name])
                        map["totalDamage"] = "%.2f".format(totalDamage)
                        map["lootAmount"] = lootAction.size
                        // 执行动作
                        ActionManager.runAction(
                            player,
                            action,
                            map,
                            map
                        )
                    }
                }
            }
        }
        // 对其他玩家执行保底动作
        names.forEach { name ->
            // 获取在线玩家, 玩家不在线则停止执行
            Bukkit.getPlayer(name)?.let { player ->
                (params?.toMutableMap<String, Any?>() ?: mutableMapOf()).also { map ->
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