package pers.neige.banker.command

import org.bukkit.command.CommandExecutor
import org.bukkit.command.PluginCommand
import org.bukkit.command.TabCompleter
import pers.neige.banker.Banker
import pers.neige.banker.command.subcommand.Help
import pers.neige.banker.command.subcommand.Reload
import pers.neige.neigeitems.annotation.Awake
import pers.neige.neigeitems.utils.CommandUtils

/**
 * 插件指令
 */
object Command {
    var command: PluginCommand? = null

    @Awake(lifeCycle = Awake.LifeCycle.ENABLE)
    fun init() {
        command = CommandUtils.newPluginCommand("banker", Banker.getInstance())?.apply {
            permission = "*"
            executor = CommandExecutor { sender, _, _, args ->
                if (args.isNotEmpty()) {
                    when (args[0]) {
                        "reload" -> Reload.reloadCommand(sender)
                        "help" -> Help.help(sender, args.getOrNull(1)?.toIntOrNull() ?: 1)
                        else -> Help.help(sender)
                    }
                } else {
                    Help.help(sender)
                }
                return@CommandExecutor true
            }
            tabCompleter = TabCompleter { _, _, _, args ->
                when (args.size) {
                    1 -> arrayListOf("reload", "help")
                    2 -> {
                        when (args[0]) {
                            "help" -> (1..Help.commandsPages).toList().map { it.toString() }
                            else -> arrayListOf()
                        }
                    }
                    else -> arrayListOf()
                }
            }
            CommandUtils.getCommandMap().register("banker", this)
        }
    }
}