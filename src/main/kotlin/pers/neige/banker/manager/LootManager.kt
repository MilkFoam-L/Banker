package pers.neige.banker.manager

import org.bukkit.configuration.ConfigurationSection
import pers.neige.banker.loot.LootGenerator
import java.lang.reflect.Constructor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object LootManager {
    /**
     * 解析配置, 返回战利品生成器(无法解析则返回null)
     *
     * @param data 待解析配置
     * @return 战利品生成器(无法解析则为null)
     */
    fun parseGenerator(data: ConfigurationSection): LootGenerator? {
        val type = data.getString("LootType")
        val upperType = type?.uppercase(Locale.getDefault())
        return lootGenerators[upperType]?.newInstance(data)
    }

    /**
     * 添加新的战利品生成器
     *
     * @param type 生成器ID
     * @param generator 一个解析配置并返回生成器的Function
     */
    fun addGenerator(type: String, generator: Class<out LootGenerator>) {
        lootGenerators[type] = generator.getDeclaredConstructor(ConfigurationSection::class.java)
    }

    val lootGenerators = ConcurrentHashMap<String, Constructor<out LootGenerator>>()
}