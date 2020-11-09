package me.ddivad.judgebot.services.infractions

import com.gitlab.kordlib.core.entity.Guild
import com.gitlab.kordlib.core.entity.Member
import me.ddivad.judgebot.dataclasses.*
import me.ddivad.judgebot.embeds.createInfractionEmbed
import me.ddivad.judgebot.services.DatabaseService
import me.ddivad.judgebot.services.LoggingService
import me.jakejmattson.discordkt.api.annotations.Service
import me.jakejmattson.discordkt.api.extensions.sendPrivateMessage
import org.joda.time.DateTime

@Service
class InfractionService(private val configuration: Configuration,
                        private val databaseService: DatabaseService,
                        private val loggingService: LoggingService,
                        private val banService: BanService,
                        private val muteService: MuteService) {
    suspend fun infract(target: Member, guild: Guild, userRecord: GuildMember, infraction: Infraction): Infraction {
        var rule: Rule? = null
        if (infraction.ruleNumber != null) {
            rule = databaseService.guilds.getRule(guild, infraction.ruleNumber)
        }
        return databaseService.users.addInfraction(guild, userRecord, infraction).also {
            target.asUser().sendPrivateMessage {
                createInfractionEmbed(guild, configuration[guild.id.longValue]!!, target, it, rule)
            }
            loggingService.infractionApplied(guild, target.asUser(), it)
            applyPunishment(guild, target, userRecord, it)
        }
    }

    private suspend fun applyPunishment(guild: Guild, target: Member, guildMember: GuildMember, infraction: Infraction) {
        when (infraction.punishment?.punishment) {
            PunishmentType.NONE -> return
            PunishmentType.MUTE -> muteService.applyMute(target, infraction.punishment?.duration!!, infraction.reason)
            PunishmentType.BAN -> {
                val clearTime = infraction.punishment!!.duration?.let { DateTime().millis.plus(it) }
                val punishment = Punishment(target.id.value, InfractionType.Ban, infraction.reason, infraction.moderator, clearTime)
                banService.banUser(target, guild, infraction.moderator, punishment)
            }
        }
    }
}
