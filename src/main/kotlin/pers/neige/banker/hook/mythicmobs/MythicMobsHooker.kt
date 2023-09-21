package pers.neige.banker.hook.mythicmobs

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import pers.neige.banker.loot.MobLoot
import pers.neige.banker.manager.ConfigManager
import pers.neige.neigeitems.event.MobInfoReloadedEvent
import taboolib.common.platform.event.EventPriority
import taboolib.common.platform.event.ProxyListener
import taboolib.common.platform.function.registerBukkitListener
import taboolib.module.chat.RawMessage
import taboolib.platform.BukkitAdapter
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * MM挂钩
 */
abstract class MythicMobsHooker {
    /**
     * 判断实体是否为MM生物
     *
     * @param entity 待判断实体
     * @return 该实体是否为MM生物
     */
    abstract fun isMythicMob(entity: Entity): Boolean

    /**
     * 获取MM实体的ID(非MM实体返回null)
     *
     * @param entity MM实体
     * @return MM实体ID(非MM实体返回null)
     */
    abstract fun getMythicId(entity: Entity): String?

    /**
     * 实体受伤事件, 监听器优先级MONITOR.
     */
    abstract val damageListener: ProxyListener

    /**
     * MM怪物死亡事件监听器, 监听器优先级NORMAL
     */
    abstract val deathListener: ProxyListener

    private val bukkitAdapter = BukkitAdapter()

    private val df2 = DecimalFormat("#0.00")

    private val data = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Double>>()

    private val mobConfigs = HashMap<String, MobLoot>()

    private val reloadListener = registerBukkitListener(MobInfoReloadedEvent::class.java, priority = EventPriority.MONITOR) {
        loadMobConfigs()
    }

    internal fun damageEvent(event: EntityDamageByEntityEvent) {
        // 获取收到伤害的实体
        val defender = event.entity

        // 如果受到伤害的不是MM怪物, 停止操作
        if (!isMythicMob(defender)) return

        // 获取MM怪物ID
        val mythicId = getMythicId(defender)
        // 如果该怪物没有配置战利品, 且不开启LogAll选项, 停止操作
        if (!mobConfigs.containsKey(mythicId) && !ConfigManager.logAll) return

        // 获取造成伤害的实体
        var attacker: Entity? = event.damager
        // 如果是投射物造成的伤害, 将伤害者记录为投掷物的发射者
        if (attacker is Projectile) {
            attacker = attacker.shooter as? Entity
        }

        if (attacker == null || attacker !is Player) return

        // 获取当前MM怪物的伤害数据
        val mobData = data.computeIfAbsent(defender.uniqueId) { ConcurrentHashMap() }

        // 取事件最终伤害与受伤害实体剩余生命中的最小值, 作为本次记录的最终伤害
        val finalDamage = event.finalDamage.coerceAtMost((defender as? LivingEntity)?.health ?: 0.0)
        // 如果最终伤害大于0
        if (finalDamage > 0) {
            // 进行伤害加和
            mobData[attacker.name] = mobData.computeIfAbsent(attacker.name) { 0.0 } + finalDamage
        }
    }

    internal fun deathEvent(
        entity: Entity,
        mythicId: String,
        mobLevel: Int
    ) {
        val mobConfig = mobConfigs[mythicId]

        // 如果该怪物没有伤害数据, 或者有伤害数据, 但是没有配置战利品, 且不开启LogAll选项, 则停止操作
        if (!data.containsKey(entity.uniqueId) || (mobConfig == null && !ConfigManager.logAll)) return

        // 获取伤害数据, 不存在伤害数据就停止操作
        val damageData = data[entity.uniqueId] ?: return
        // 伤害数据排序
        val sortedDamageData = damageData.entries.toMutableList().also {
            it.sortWith { o1, o2 -> o2.value.compareTo(o1.value) }
        }
        // 计算总伤害
        var totalDamage = 0.0
        damageData.entries.forEach { entry ->
            totalDamage += entry.value
        }

        // 对每个玩家发送伤害统计信息
        sendStatisticsMessage(sortedDamageData, entity.name, totalDamage)
        // 构建怪物参数
        val params = mutableMapOf<String, String>().also { map ->
            if (entity is LivingEntity) {
                map["mobMaxHealth"] = df2.format(entity.maxHealth)
            }
            map["mobId"] = mythicId
            map["mobLevel"] = mobLevel.toString()
            val location = entity.location
            map["mobLocationX"] = df2.format(location.x)
            map["mobLocationY"] = df2.format(location.y)
            map["mobLocationZ"] = df2.format(location.z)
            map["mobLocationYaw"] = df2.format(location.yaw)
            map["mobLocationPitch"] = df2.format(location.pitch)
            map["mobWorld"] = entity.world.name
            map["mobName"] = entity.name
            entity.customName?.let {
                map["mobCustomName"] = it
            }
            map["playerAmount"] = damageData.size.toString()
        }
        // 发送战利品
        mobConfig?.run(damageData, sortedDamageData, totalDamage, params)
        // 移除对应伤害记录
        data.remove(entity.uniqueId)
    }

    internal fun loadMobConfigs() {
        // 遍历怪物配置
        pers.neige.neigeitems.manager.HookerManager.mythicMobsHooker?.mobInfos?.entries?.forEach { entry ->
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

    private fun sendStatisticsMessage(
        sortedDamageData: List<MutableMap.MutableEntry<String, Double>>,
        activeMobName: String,
        totalDamage: Double
    ) {
        // 构建信息
        val finalMessage = RawMessage()
        // 将待组合文本纳入数组
        val deathMessageArray = ConfigManager.deathMessage!!
                .replace("{monster}", activeMobName)
                .split("{damagemessage}")

        // 开始构建伤害统计Json
        val hoverMessage = RawMessage()
        hoverMessage.append(ConfigManager.damageMessageString!!)

        val hoverText = StringBuilder()

        // 加入伤害统计前缀
        for (prefix in ConfigManager.damageMessagePrefix) {
            hoverText.append(
                prefix
                    .replace("{monster}", activeMobName)
                    .replace("{totaldamage}", df2.format(totalDamage)) + "\n"
            )
        }
        // 加入伤害统计排名
        sortedDamageData.forEachIndexed { index, entry ->
            hoverText.append(
                ConfigManager.damageMessage!!
                    .replace("{ranking}", (index + 1).toString())
                    .replace("{player}", entry.key)
                    .replace("{damage}", df2.format(entry.value))
                    .replace("{percentage}", df2.format(entry.value*100/totalDamage) + "%") + "\n"
            )
        }
        // 加入伤害统计后缀
        ConfigManager.damageMessageSuffix.forEachIndexed { index, suffix ->
            hoverText.append(suffix)
            if (index != ConfigManager.damageMessageSuffix.lastIndex) {
                hoverText.append("\n")
            }
        }
        // 添加伤害统计悬浮文本
        hoverMessage.hoverText(hoverText.toString())

        // 组合文本
        deathMessageArray.forEachIndexed { index, message ->
            finalMessage.append(message)
            if ((index + 1) != deathMessageArray.size) {
                finalMessage.append(hoverMessage)
            }
        }

        // 遍历玩家ID
        sortedDamageData.forEach { entry ->
            // 获取玩家
            val attacker = Bukkit.getPlayer(entry.key)
            // 如果玩家在线
            if (attacker != null) {
                // 发送信息
                finalMessage.sendTo(bukkitAdapter.adaptCommandSender(attacker))
            }
        }
    }
}