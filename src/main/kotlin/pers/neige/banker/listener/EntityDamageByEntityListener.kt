package pers.neige.banker.listener

import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageByEntityEvent
import pers.neige.banker.manager.ConfigManager
import pers.neige.neigeitems.annotation.Listener
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object EntityDamageByEntityListener {
    val data = ConcurrentHashMap<UUID, ConcurrentHashMap<String, Double>>()

    @Listener(eventPriority = EventPriority.MONITOR)
    fun listener(event: EntityDamageByEntityEvent) {
        // 获取收到伤害的实体
        val defender = event.entity

        // 如果受到伤害的不是MM怪物, 停止操作
        if (!pers.neige.neigeitems.manager.HookerManager.mythicMobsHooker!!.isMythicMob(defender)) return

        // 获取MM怪物ID
        val mythicId = pers.neige.neigeitems.manager.HookerManager.mythicMobsHooker!!.getMythicId(defender)
        // 如果该怪物没有配置战利品, 且不开启LogAll选项, 停止操作
        if (!MobInfoReloadedListener.mobConfigs.containsKey(mythicId) && !ConfigManager.logAll) return

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
}