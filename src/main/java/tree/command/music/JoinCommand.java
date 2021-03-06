package tree.command.music;

import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.AudioManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tree.Config;
import tree.command.util.MessageUtil;
import tree.command.util.music.AudioPlayerAdapter;
import tree.command.util.music.GuildMusicManager;
import tree.command.util.speech.AudioReceiveListener;
import tree.commandutil.CommandManager;
import tree.commandutil.type.MusicCommand;
import tree.db.DatabaseManager;
import tree.util.LoggerUtil;

import javax.xml.crypto.Data;

/**
 * Created by Valued Customer on 8/12/2017.
 */
public class JoinCommand implements MusicCommand {
    private DatabaseManager db = DatabaseManager.getInstance();
    private String commandName;
    private AudioPlayerAdapter audioPlayer;
    private static Logger logger = LoggerFactory.getLogger(LeaveCommand.class);

    public JoinCommand(String commandName) {
        this.commandName = commandName;
        audioPlayer = AudioPlayerAdapter.audioPlayerAdapter;
    }

    private void join(Guild guild, MessageChannel msgChan, Message message, Member member) {
        VoiceChannel voiceChan = member.getVoiceState().getChannel();

        if (!member.getVoiceState().inVoiceChannel()) {
            MessageUtil.sendError("You must be in a voice channel.", msgChan);
            return;
        }

        if (!db.isAllowedVoiceChannel(guild, voiceChan)) {
            MessageUtil.sendError("I'm not allowed to join that channel!", msgChan);
            return;
        }


        AudioManager audioManager = guild.getAudioManager();
        audioManager.openAudioConnection(member.getVoiceState().getChannel());
        audioPlayer.connectToMusicChannel(guild.getAudioManager(), member);
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan, Message message, Member member, String[] args) {
        if (args.length != 1) {
            LoggerUtil.logMessage(logger, message, "Only one argument allowed.");
            return;
        }
        join(guild, msgChan, message, member);
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Join the music channel.";
    }

    @Override
    public String getCommandName() {
        return commandName;
    }


}
