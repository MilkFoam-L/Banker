package pers.neige.banker.listener

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import pers.neige.banker.Banker.plugin
import pers.neige.banker.listener.EntityDamageByEntityListener.data
import pers.neige.banker.listener.MobInfoReloadedListener.mobConfigs
import pers.neige.banker.manager.ConfigManager
import pers.neige.neigeitems.manager.HookerManager.mythicMobsHooker
import pers.neige.neigeitems.utils.ListenerUtils
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.submit
import taboolib.module.chat.RawMessage
import taboolib.platform.BukkitAdapter
import java.text.DecimalFormat
import kotlin.math.roundToInt

object MythicMobDeathListener {
    private val bukkitAdapter = BukkitAdapter()

    private val df2 = DecimalFormat("#0.00")

    @Awake(LifeCycle.ACTIVE)
    fun register() {
        ListenerUtils.registerListener(
            mythicMobsHooker!!.deathEventClass,
            EventPriority.MONITOR,
            plugin,
        ) { event ->
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                deathEvent(
                    mythicMobsHooker!!.getEntity(event)!!,
                    mythicMobsHooker!!.getInternalName(event)!!,
                    mythicMobsHooker!!.getMobLevel(event)!!.roundToInt(),
                )
            })
        }
    }

    private fun deathEvent(
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