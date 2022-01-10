package me.ddivad.judgebot.commands

import dev.kord.common.kColor
import dev.kord.rest.Image
import dev.kord.x.emoji.Emojis
import dev.kord.x.emoji.addReaction
import kotlinx.coroutines.flow.toList
import me.ddivad.judgebot.arguments.LowerUserArg
import me.ddivad.judgebot.conversations.ResetUserConversation
import me.ddivad.judgebot.conversations.guildChoiceConversation
import me.ddivad.judgebot.dataclasses.*
import me.ddivad.judgebot.embeds.createHistoryEmbed
import me.ddivad.judgebot.embeds.createLinkedAccountMenu
import me.ddivad.judgebot.embeds.createSelfHistoryEmbed
import me.ddivad.judgebot.services.DatabaseService
import me.ddivad.judgebot.services.LoggingService
import me.ddivad.judgebot.services.infractions.BanService
import me.jakejmattson.discordkt.arguments.*
import me.jakejmattson.discordkt.commands.commands
import me.jakejmattson.discordkt.extensions.*
import java.awt.Color
import java.text.SimpleDateFormat

@Suppress("unused")
fun createUserCommands(
    databaseService: DatabaseService,
    config: Configuration,
    loggingService: LoggingService,
    banService: BanService
) = commands("User") {
    command("history", "h", "H") {
        description = "Use this to view a user's record."
        requiredPermission = Permissions.MODERATOR
        execute(UserArg) {
            val user = databaseService.users.getOrCreateUser(args.first, guild)
            databaseService.users.incrementUserHistory(user, guild)
            respondMenu {
                createHistoryEmbed(args.first, user, guild, config, databaseService)
            }
        }
    }

    command("alts") {
        description = "Use this to view a user's alt accounts."
        requiredPermission = Permissions.MODERATOR
        execute(UserArg) {
            val target = args.first
            val user = databaseService.users.getOrCreateUser(args.first, guild)
            databaseService.users.incrementUserHistory(user, guild)
            val linkedAccounts = user.getLinkedAccounts(guild)

            if (linkedAccounts.isEmpty()) {
                respond("User ${target.mention} has no alt accounts recorded.")
                return@execute
            }

            respondMenu {
                createLinkedAccountMenu(linkedAccounts, guild, config, databaseService)
            }
        }
    }

    command("whatpfp") {
        description = "Perform a reverse image search of a User's profile picture"
        requiredPermission = Permissions.MODERATOR
        execute(UserArg) {
            val user = args.first
            val reverseSearchUrl = "<https://www.google.com/searchbyimage?&image_url=${user.pfpUrl}>"
            respond {
                title = "${user.tag}'s pfp"
                color = Color.MAGENTA.kColor
                description = "[Reverse Search]($reverseSearchUrl)"
                image = "${user.pfpUrl}?size=512"
            }
        }
    }

    command("ban") {
        description = "Ban a member from this guild."
        requiredPermission = Permissions.STAFF
        execute(LowerUserArg, IntegerArg("Delete message days").optional(0), EveryArg) {
            val (target, deleteDays, reason) = args
            if (deleteDays > 7) {
                respond("Delete days cannot be more than **7**. You tried with **${deleteDays}**")
                return@execute
            }
            val ban = Punishment(target.id.toString(), InfractionType.Ban, reason, author.id.toString())
            banService.banUser(target, guild, ban, deleteDays).also {
                loggingService.userBanned(guild, target, ban)
                respond("User ${target.mention} banned")
            }
        }
    }

    command("unban") {
        description = "Unban a banned member from this guild."
        requiredPermission = Permissions.STAFF
        execute(UserArg) {
            val user = args.first
            guild.getBanOrNull(user.id)?.let {
                banService.unbanUser(guild, user)
                respond("${user.tag} unbanned")
                return@execute
            }
            respond("${user.mention} isn't banned from this guild.")
        }
    }

    command("setBanReason") {
        description = "Set a ban reason for a banned user"
        requiredPermission = Permissions.STAFF
        execute(UserArg, EveryArg("Reason")) {
            val (user, reason) = args
            val ban = Ban(user.id.toString(), author.id.toString(), reason)
            if (guild.getBanOrNull(user.id) != null) {
                if (!databaseService.guilds.checkBanExists(guild, user.id.toString())) {
                    databaseService.guilds.addBan(guild, ban)
                } else {
                    databaseService.guilds.editBanReason(guild, user.id.toString(), reason)
                }
                respond("Ban reason for ${user.username} set to: $reason")
            } else respond("User ${user.username} isn't banned")

        }
    }

    command("getBanReason") {
        description = "Get a ban reason for a banned user"
        requiredPermission = Permissions.STAFF
        execute(UserArg) {
            val user = args.first
            guild.getBanOrNull(user.id)?.let {
                val reason = databaseService.guilds.getBanOrNull(guild, user.id.toString())?.reason ?: it.reason
                respond(reason ?: "No reason logged")
                return@execute
            }
            respond("${user.username} isn't banned from this guild.")
        }
    }

    command("selfHistory") {
        description = "View your infraction history (contents will be DM'd)"
        requiredPermission = Permissions.NONE
        execute {
            val user = author
            val mutualGuilds = author.mutualGuilds.toList().filter { config[it.id.value] != null }

            if (mutualGuilds.size == 1 || guild != null) {
                val currentGuild = guild ?: mutualGuilds.first()
                val guildMember = databaseService.users.getOrCreateUser(user, currentGuild)

                user.sendPrivateMessage {
                    createSelfHistoryEmbed(user, guildMember, currentGuild, config)
                }
                this.message?.addReaction(Emojis.whiteCheckMark)
            } else {
                guildChoiceConversation(mutualGuilds, config).startPrivately(discord, author)
            }
        }
    }

    command("link") {
        description = "Link a user's alt account with their main"
        requiredPermission = Permissions.STAFF
        execute(UserArg("Main Account"), UserArg("Alt Account")) {
            val (main, alt) = args
            val mainRecord = databaseService.users.getOrCreateUser(main, guild)
            val altRecord = databaseService.users.getOrCreateUser(alt, guild)
            databaseService.users.addLinkedAccount(guild, mainRecord, alt.id.toString())
            databaseService.users.addLinkedAccount(guild, altRecord, main.id.toString())
            respond("Linked accounts ${main.mention} and ${alt.mention}")
        }
    }

    command("unlink") {
        description = "Link a user's alt account with their main"
        requiredPermission = Permissions.STAFF
        execute(UserArg("Main Account"), UserArg("Alt Account")) {
            val (main, alt) = args
            val mainRecord = databaseService.users.getOrCreateUser(main, guild)
            val altRecord = databaseService.users.getOrCreateUser(alt, guild)
            databaseService.users.removeLinkedAccount(guild, mainRecord, alt.id.toString())
            databaseService.users.removeLinkedAccount(guild, altRecord, main.id.toString())
            respond("Unlinked accounts ${main.mention} and ${alt.mention}")
        }
    }

    command("reset") {
        description = "Reset a user's record, and any linked accounts"
        requiredPermission = Permissions.STAFF
        execute(LowerUserArg) {
            val target = args.first
            ResetUserConversation(databaseService, config)
                .createResetConversation(guild, target)
                .startPublicly(discord, author, channel)
        }
    }

    command("deletedMessages") {
        description = "View a users messages deleted using the delete message reaction"
        requiredPermission = Permissions.STAFF
        execute(LowerUserArg) {
            val target = args.first
            val guildMember = databaseService.users.getOrCreateUser(target, guild).getGuildInfo(guild.id.toString())
            val guildConfiguration = config[guild.asGuild().id.value]

            val deletedMessages = databaseService.messageDeletes
                .getMessageDeletesForMember(guild.id.toString(), target.id.toString())
                .sortedByDescending { it.dateTime }
                .map { "Deleted on **${SimpleDateFormat("dd/MM/yyyy HH:mm").format(it.dateTime)}** \n[Message Link](${it.messageLink})" }
                .chunked(6)

            respondMenu {
                deletedMessages.forEachIndexed { index, list ->
                    page {
                        color = discord.configuration.theme
                        author {
                            name = "Deleted messages for ${target.tag}"
                            icon = target.pfpUrl
                        }
                        description = """
                            **Showing messages deleted using ${guildConfiguration?.reactions?.deleteMessageReaction}**
                            ${target.tag} has **${guildMember.deletedMessageCount.deleteReaction}** deletions
                        """.trimIndent()

                        list.forEach {
                            field {
                                value = it
                            }
                        }

                        footer {
                            icon = guild.getIconUrl(Image.Format.PNG) ?: ""
                            text = "${guild.name} | Page ${index + 1} of ${deletedMessages.size}"
                        }
                    }
                }
                if (deletedMessages.size > 1) {
                    buttons {
                        button("Prev.", Emojis.arrowLeft) {
                            previousPage()
                        }
                        button("Next", Emojis.arrowRight) {
                            nextPage()
                        }
                    }
                }
            }
        }
    }
}
