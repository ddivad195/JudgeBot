package me.ddivad.judgebot.services.database

import com.gitlab.kordlib.core.entity.Guild
import com.gitlab.kordlib.core.entity.Member
import me.ddivad.judgebot.dataclasses.GuildMemberDetails
import me.ddivad.judgebot.dataclasses.GuildMember
import me.ddivad.judgebot.dataclasses.Infraction
import me.jakejmattson.discordkt.api.annotations.Service
import org.litote.kmongo.eq

@Service
class UserOperations(private val connection: ConnectionService) {
    private val userCollection = connection.db.getCollection<GuildMember>("Users")

    suspend fun getOrCreateUser(target: Member, guildId: String): GuildMember {
            val userRecord = userCollection.findOne(GuildMember::userId eq target.id.value)
            return if(userRecord != null) {
                userRecord.ensureGuildDetailsPresent(guildId)
                userRecord
            } else {
                val guildMember = GuildMember(target.id.value)
                guildMember.guilds.add(GuildMemberDetails(guildId))
                userCollection.insertOne(guildMember)
                guildMember
            }
    }

    private suspend fun updateUser(user: GuildMember): GuildMember {
        userCollection.updateOne(GuildMember::userId eq user.userId, user)
        return user
    }

    suspend fun addNote(guild: Guild, user: GuildMember, note: String, moderator: String): GuildMember {
        user.addNote(note, moderator, guild)
        return this.updateUser(user)
    }

    suspend fun deleteNote(guild: Guild, user: GuildMember, noteId: Int): GuildMember {
        user.deleteNote(noteId, guild)
        return this.updateUser(user)
    }

    suspend fun cleanseNotes(guild: Guild, user: GuildMember): GuildMember {
        user.cleanseNotes(guild)
        return this.updateUser(user)
    }

    suspend fun addInfraction(guild: Guild, user: GuildMember, infraction: Infraction): GuildMember {
        user.addInfraction(infraction, guild)
        return this.updateUser(user)
    }

    suspend fun cleanseInfractions(guild: Guild, user: GuildMember): GuildMember {
        user.cleanseInfractions(guild)
        return this.updateUser(user)
    }

    suspend fun incrementUserHistory(user: GuildMember, guildId: String): GuildMember {
        user.incrementHistoryCount(guildId)
        return this.updateUser(user)
    }
}