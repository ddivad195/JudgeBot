package me.ddivad.judgebot.commands

import me.ddivad.judgebot.conversations.ConfigurationConversation
import me.ddivad.judgebot.dataclasses.Configuration
import me.ddivad.judgebot.embeds.createConfigEmbed
import me.ddivad.judgebot.services.DatabaseService
import me.ddivad.judgebot.services.PermissionLevel
import me.ddivad.judgebot.services.requiredPermissionLevel
import me.jakejmattson.discordkt.api.arguments.ChannelArg
import me.jakejmattson.discordkt.api.arguments.EveryArg
import me.jakejmattson.discordkt.api.arguments.RoleArg
import me.jakejmattson.discordkt.api.dsl.commands
import me.jakejmattson.discordkt.api.services.ConversationService

fun guildConfigCommands(configuration: Configuration,
                        conversationService: ConversationService,
                        databaseService: DatabaseService) = commands("Configuration") {
    command("configure") {
        description = "Configure a guild to use Judgebot."
        requiredPermissionLevel = PermissionLevel.Administrator
        execute {
            if (configuration.hasGuildConfig(guild!!.id.longValue))
                return@execute respond("Guild configuration exists. To modify it use the commands to set values.")

            conversationService.startPublicConversation<ConfigurationConversation>(author, channel.asChannel(), guild!!)
            databaseService.guilds.setupGuild(guild!!)
            respond("Guild setup")
        }
    }

    command("viewconfig") {
        description = "View the configuration vales for this guild."
        requiredPermissionLevel = PermissionLevel.Staff
        execute {
            if (!configuration.hasGuildConfig(guild!!.id.longValue))
                return@execute respond("Please run the **configure** command to set this initially.")

            val config = configuration[guild!!.id.longValue] ?: return@execute
            respond {
                createConfigEmbed(config, guild!!)
            }
        }
    }

    command("setprefix") {
        description = "Set the bot prefix."
        requiredPermissionLevel = PermissionLevel.Administrator
        execute(EveryArg) {
            if (!configuration.hasGuildConfig(guild!!.id.longValue))
                return@execute respond("Please run the **configure** command to set this initially.")

            val prefix = args.first
            configuration[guild!!.id.longValue]?.prefix = prefix
            configuration.save()

            respond("Prefix set to: **$prefix**")
        }
    }

    command("setstaffrole") {
        description = "Set the bot staff role."
        requiredPermissionLevel = PermissionLevel.Administrator
        execute(RoleArg) {
            if (!configuration.hasGuildConfig(guild!!.id.longValue))
                return@execute respond("Please run the **configure** command to set this initially.")

            val role = args.first
            configuration[guild!!.id.longValue]?.staffRole = role.id.value
            configuration.save()

            respond("Role set to: **${role.name}**")
        }
    }

    command("setadminrole") {
        description = "Set the bot admin role."
        requiredPermissionLevel = PermissionLevel.Administrator
        execute(RoleArg) {
            if (!configuration.hasGuildConfig(guild!!.id.longValue))
                return@execute respond("Please run the **configure** command to set this initially.")

            val role = args.first
            configuration[guild!!.id.longValue]?.adminRole = role.id.value
            configuration.save()

            respond("Role set to: **${role.name}**")
        }
    }

    command("setlogchannel") {
        description = "Set the channel that the bot logs will be sent."
        requiredPermissionLevel = PermissionLevel.Administrator
        execute(ChannelArg) {
            if (!configuration.hasGuildConfig(guild!!.id.longValue))
                return@execute respond("Please run the **configure** command to set this initially.")

            val channel = args.first
            configuration[guild!!.id.longValue]?.loggingConfiguration?.loggingChannel = channel.id.value
            configuration.save()

            respond("Channel set to: **${channel.name}**")
        }
    }

    command("setalertchannel") {
        description = "Set the channel that the bot alerts will be sent."
        requiredPermissionLevel = PermissionLevel.Administrator
        execute(ChannelArg) {
            if (!configuration.hasGuildConfig(guild!!.id.longValue))
                return@execute respond("Please run the **configure** command to set this initially.")

            val channel = args.first
            configuration[guild!!.id.longValue]?.loggingConfiguration?.alertChannel = channel.id.value
            configuration.save()

            respond("Channel set to: **${channel.name}**")
        }
    }

    command("setmuterole") {
        description = "Set the role to be used to mute members."
        requiredPermissionLevel = PermissionLevel.Administrator
        execute(RoleArg) {
            if (!configuration.hasGuildConfig(guild!!.id.longValue))
                return@execute respond("Please run the **configure** command to set this initially.")

            val role = args.first
            configuration[guild!!.id.longValue]?.mutedRole = role.id.value
            configuration.save()
            respond("Role set to: **${role.name}**")
        }
    }
}