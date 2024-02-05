package pers.neige.banker.listener

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.event.EventPriority
import pers.neige.banker.loot.MobLoot
import pers.neige.neigeitems.annotation.Awake
import pers.neige.neigeitems.annotation.Listener
import pers.neige.neigeitems.event.MobInfoReloadedEvent
import pers.neige.neigeitems.manager.HookerManager.mythicMobsHooker
import java.util.concurrent.ConcurrentHashMap

object MobInfoReloadedListener {
    val mobConfigs = ConcurrentHashMap<String, MobLoot>()

    @Listener(eventPriority = EventPriority.MONITOR)
    fun listener(event: MobInfoReloadedEvent) {
        loadMobConfigs()
    }

    @Awake(lifeCycle = Awake.LifeCycle.ACTIVE)
    fun loadMobConfigs() {
        val temp = ConcurrentHashMap<String, MobLoot>()
        // 遍历怪物配置
        mythicMobsHooker?.mobInfos?.forEach { (mythicId, config) ->
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
                    temp[mythicId] = MobLoot(loots)
                }
            }
        }
        val iterator = mobConfigs.iterator()
        while(iterator.hasNext()) {
            val entry = iterator.next()
            if (!temp.containsKey(entry.key)) {
                iterator.remove()
            }
        }
        temp.forEach { (key, value) ->
            mobConfigs[key] = value
        }
    }
}