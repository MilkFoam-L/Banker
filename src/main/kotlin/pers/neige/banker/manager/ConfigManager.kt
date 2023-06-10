package pers.neige.banker.manager

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import pers.neige.banker.Banker.plugin
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.Platform
import taboolib.module.metrics.Metrics
import taboolib.platform.BukkitPlugin
import java.io.*

/**
 * 配置文件管理器, 用于管理config.yml文件, 对其中缺少的配置项进行主动补全, 同时释放默认配置文件
 */
object ConfigManager {
    /**
     * 获取默认Config
     */
    private val originConfig: FileConfiguration =
        plugin.getResource("config.yml")?.let {
            val reader = InputStreamReader(it, "UTF-8")
            val config = YamlConfiguration.loadConfiguration(reader)
            reader.close()
            config
        } ?: YamlConfiguration()

    /**
     * 获取配置文件
     */
    val config get() = plugin.config

    /**
     * 是否记录全部MM怪物的伤害统计信息(包含未配置死后执行指令的怪物)
     */
    var logAll = config.getBoolean("LogAll")

    /**
     * 怪物死亡提示文本
     */
    var deathMessage = config.getString("Messages.Death")

    /**
     * 伤害统计查看提示
     */
    var damageMessageString = config.getString("Messages.Damage")

    /**
     * 怪物伤害统计前缀, 设置为 DamagePrefix: [] 代表不发送
     */
    var damageMessagePrefix = config.getStringList("Messages.DamagePrefix")

    /**
     * 怪物伤害统计
     */
    var damageMessage = config.getString("Messages.DamageInfo")

    /**
     * 怪物伤害统计后缀, 设置为 DamagePrefix: [] 代表不发送
     */
    var damageMessageSuffix = config.getStringList("Messages.DamageSuffix")

    /**
     * 指令包类型错误提示
     */
    var lootTypeError = config.getString("Messages.LootTypeError")

    var invalidPlugin = config.getString("Messages.InvalidPlugin")

    var reloaded = config.getString("Messages.Reloaded")

    /**
     * 加载默认配置文件
     */
    @Awake(LifeCycle.INIT)
    fun saveResource() {
        plugin.saveResourceNotWarn("Mobs${File.separator}Banker${File.separator}ExampleMobs.yml", Bukkit.getPluginManager().getPlugin("MythicMobs"))
        plugin.saveDefaultConfig()
        // 加载bstats
        Metrics(18146, plugin.description.version, Platform.BUKKIT)
    }

    /**
     * 对当前Config查缺补漏
     */
    @Awake(LifeCycle.LOAD)
    fun loadConfig() {
        originConfig.getKeys(true).forEach { key ->
            if (!plugin.config.contains(key)) {
                plugin.config.set(key, originConfig.get(key))
            } else {
                val completeValue = originConfig.get(key)
                val value = plugin.config.get(key)
                if (completeValue is ConfigurationSection && value !is ConfigurationSection) {
                    plugin.config.set(key, completeValue)
                } else {
                    plugin.config.set(key, value)
                }
            }
        }
        plugin.saveConfig()
    }

    /**
     * 重载配置管理器
     */
    fun reload() {
        plugin.reloadConfig()
        loadConfig()

        logAll = config.getBoolean("LogAll")
        deathMessage = config.getString("Messages.Death")
        damageMessageString = config.getString("Messages.Damage")
        damageMessagePrefix = config.getStringList("Messages.DamagePrefix")
        damageMessage = config.getString("Messages.DamageInfo")
        damageMessageSuffix = config.getStringList("Messages.DamageSuffix")
        lootTypeError = config.getString("Messages.LootTypeError")
        invalidPlugin = config.getString("Messages.InvalidPlugin")
        reloaded = config.getString("Messages.Reloaded")
    }

    private fun BukkitPlugin.saveResourceNotWarn(resourcePath: String, targetPlugin: Plugin? = this) {
        this.getResource(resourcePath.replace('\\', '/'))?.let { inputStream ->
            val outFile = File((targetPlugin ?: this).dataFolder, resourcePath)
            val lastIndex: Int = resourcePath.lastIndexOf(File.separator)
            val outDir = File((targetPlugin ?: this).dataFolder, resourcePath.substring(0, if (lastIndex >= 0) lastIndex else 0))
            if (!outDir.exists()) {
                outDir.mkdirs()
            }
            if (!outFile.exists()) {
                try {
                    var len: Int
                    val fileOutputStream = FileOutputStream(outFile)
                    val buf = ByteArray(1024)
                    while (inputStream.read(buf).also { len = it } > 0) {
                        (fileOutputStream as OutputStream).write(buf, 0, len)
                    }
                    fileOutputStream.close()
                    inputStream.close()
                } catch (ex: IOException) {}
            }
        }
    }
}
