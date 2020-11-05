package me.ddivad.judgebot

import com.gitlab.kordlib.gateway.Intent
<<<<<<< HEAD
import com.gitlab.kordlib.gateway.PrivilegedIntent
=======
>>>>>>> 63cd7656c9035e2c078eb824e971c674ac10d7f3
import me.ddivad.judgebot.dataclasses.Configuration
import me.ddivad.judgebot.services.BotStatsService
import me.ddivad.judgebot.services.MuteService
import me.ddivad.judgebot.services.PermissionsService
import me.ddivad.judgebot.services.requiredPermissionLevel
import me.jakejmattson.discordkt.api.dsl.bot
import me.jakejmattson.discordkt.api.extensions.addInlineField
import java.awt.Color

@PrivilegedIntent
suspend fun main(args: Array<String>) {
    val token = System.getenv("BOT_TOKEN") ?: null
    val defaultPrefix = System.getenv("DEFAULT_PREFIX") ?: "<none>"

    require(token != null) { "Expected the bot token as an environment variable" }

    bot(token) {
        prefix {
            val configuration = discord.getInjectionObjects(Configuration::class)

            guild?.let { configuration[guild!!.id.longValue]?.prefix } ?: defaultPrefix
        }

        configure {
            allowMentionPrefix = true
            commandReaction = null
            theme = Color.MAGENTA
        }

        mentionEmbed {
            val botStats = it.discord.getInjectionObjects(BotStatsService::class)
            val channel = it.channel
            val self = channel.kord.getSelf()

            color = it.discord.configuration.theme

            thumbnail {
                url = self.avatar.url
            }

            field {
                name = self.tag
                value = "A bot for managing discord infractions in an intelligent and user-friendly way."
            }

            addInlineField("Prefix", it.prefix())
            addInlineField("Contributors", "ddivad#0001")

            val kotlinVersion = KotlinVersion.CURRENT
            val versions = it.discord.versions
            field {
                name = "Build Info"
                value = "```" +
                        "Version:   1.0.0-Beta-1\n" +
                        "DiscordKt: ${versions.library}\n" +
                        "Kotlin:    $kotlinVersion" +
                        "```"
            }

            field {
                name = "Uptime"
                value = botStats.uptime
            }
            field {
                name = "Ping"
                value = botStats.ping
            }
        }

        permissions {
            val permissionsService = discord.getInjectionObjects(PermissionsService::class)
            val permission = command.requiredPermissionLevel
            if (guild != null)
                permissionsService.hasClearance(guild!!, user, permission)
            else
                false
        }

        onStart {
            val muteService = this.getInjectionObjects(MuteService::class)
            muteService.initGuilds()
        }

<<<<<<< HEAD
        intents {
            +Intent.GuildMessages
            +Intent.DirectMessages
            +Intent.GuildBans
            +Intent.Guilds
            +Intent.GuildMembers
            +Intent.GuildMessageReactions
        }
=======
//        intents {
//            +Intent.GuildMessages
//        }
>>>>>>> 63cd7656c9035e2c078eb824e971c674ac10d7f3
    }
}