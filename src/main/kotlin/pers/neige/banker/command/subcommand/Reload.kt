package pers.neige.banker.command.subcommand

import org.bukkit.command.CommandSender
import pers.neige.banker.manager.ConfigManager
import pers.neige.neigeitems.utils.SchedulerUtils.async

object Reload {
    fun reloadCommand(sender: CommandSender) {
        async {
            // 准备重载
            ConfigManager.reload()
            sender.sendMessage(ConfigManager.reloaded)
        }
    }
}