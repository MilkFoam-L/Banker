package pers.neige.banker.loot

import org.bukkit.configuration.ConfigurationSection
import java.util.*

/**
 * 战利品生成器
 *
 * @property data 战利品对应的配置
 * @property type 生成器ID
 * @constructor 解析配置, 获取对应的战利品分配逻辑
 */
abstract class LootGenerator(private val data: ConfigurationSection) {
    open val type: String = this.javaClass.name.uppercase(Locale.getDefault())

    /**
     * 根据伤害记录执行战利品分配
     *
     * @param damageData 玩家ID: 伤害数值
     * @param sortedDamageData 根据伤害数值降序排列的的damageData
     * @param totalDamage 总伤害数值
     */
    abstract fun run(
        damageData: Map<String, Double>,
        sortedDamageData: List<Map.Entry<String, Double>>,
        totalDamage: Double,
        params: MutableMap<String, Any?>? = null
    )
}