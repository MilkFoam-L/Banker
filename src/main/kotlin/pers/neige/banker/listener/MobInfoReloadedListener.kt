package pers.neige.banker.listener

import org.bukkit.configuration.ConfigurationSection
import pers.neige.banker.loot.MobLoot
import pers.neige.neigeitems.event.MobInfoReloadedEvent
import pers.neige.neigeitems.manager.HookerManager.mythicMobsHooker
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import java.util.HashMap

object MobInfoReloadedListener {
    val mobConfigs = HashMap<String, MobLoot>()

    @SubscribeEvent(priority = taboolib.common.platform.event.EventPriority.MONITOR, ignoreCancelled = true)
    fun listener(event: MobInfoReloadedEvent) {
        loadMobConfigs()
    }

    @Awake(LifeCycle.ACTIVE)
    fun loadMobConfigs() {
        // 遍历怪物配置
        mythicMobsHooker?.mobInfos?.entries?.forEach { entry ->
            // 获取怪物ID
            val mythicId = entry.key
            // 获取怪物配置
            val config = entry.value
            // 获取Banker配置项
            val banker = config.getConfigurationSection("Banker")
            // 当Banker配置项存在时进行进一步操作
            if (banker != null) {
                // 准备存储当前怪物的战利品设置
                val loots = HashMap<String, ConfigurationSection>()
                // 遍历每一个战利品设置
                banker.getKeys(false).forEach { key ->
                    // 获取当前战利品配置项
                    val loot = banker.getConfigurationSection(key)
                    // 如果当前配置项存在
                    if (loot != null) {
                        // 存储该战利品配置
                        loots[key] = loot
                    }
                }
                // 如果存在战利品配置
                if (loots.isNotEmpty()) {
                    // 存储
                    mobConfigs[mythicId] = MobLoot(loots)
                }
            }
        }
    }
}