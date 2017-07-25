package com.jda.main;

import com.jda.util.DataUtil;
import com.jda.util.DiscordReadUtil;
import com.jda.util.MessageUtil;
import com.jda.util.SerializableMessageHistory;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * Created by Valued Customer on 7/20/2017.
 */
public class MainApplication extends ListenerAdapter {
    private DiscordReadUtil discUtil;
    private DataUtil dataUtil;
    private static final String[] channelIDList = {"249764885246902272", "247929469552033792",
    "269577202016845824", "247135478069854209", "247248468626636800",
    "248243893273886720", "247134894558281730"};

    private static OkHttpClient.Builder setupBuilder(OkHttpClient.Builder builder) {
        builder = builder.connectTimeout(60000, TimeUnit.MILLISECONDS);
        builder = builder.readTimeout(60000, TimeUnit.MILLISECONDS);
        builder = builder.writeTimeout(60000, TimeUnit.MILLISECONDS);
        return builder;
    }

    public static void main(String[] args)
            throws LoginException, RateLimitedException, InterruptedException {
        /* Get the credentials file. */
        if (args[0] == null) {
            System.out.println("No filename supplied. Error.");
        }
        DataUtil dataUtil = new DataUtil();
        Map<String, Object> creds = dataUtil.retrieveCreds(args[0]);
        dataUtil.setCreds(creds);
        /* Create the bot and add the listeners for the bot. */
        String token = (String) creds.get("token");
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        JDA jda = new JDABuilder(AccountType.BOT)
                .setHttpClientBuilder(setupBuilder(builder))
                .setToken(token)
                .buildBlocking();
        jda.addEventListener(new MainApplication(dataUtil));
    }

    public MainApplication(DataUtil dataUtil) {
        this.dataUtil = dataUtil;
        discUtil = new DiscordReadUtil(dataUtil);
    }


    /**
     * Get the current message based on the !curr event.
     * @param event - event of a received message.
     */
    private void getCurrMessage(MessageReceivedEvent event) {
        String publicMessage = String.format(MessageUtil.timeStamp(event.getMessage()) +
                       MessageUtil.userMsg(event.getMessage()));
        String privateMessage = String.format("[PM] %s: %s\n", event.getAuthor().getName(),
                event.getMessage().getContent());
        if (event.isFromType(ChannelType.PRIVATE)) {
            System.out.printf(privateMessage);
        }
        else {
            System.out.printf(publicMessage);
        }
    }

    /**
     * Confirm that the user is palat, so the commands won't be abused.
     * @param event - event of a received message.
     * @return boolean depending if user is palat.
     */
    private boolean isMainUser(MessageReceivedEvent event) {
        String name = event.getAuthor().getId();
        return name.equals("192372494202568706");
    }

    @Override
    /**
     * The main message listener for testing purposes.
     * @param{event} - event of a received message.
     */
    public void onMessageReceived(MessageReceivedEvent event) {
        /* Allow only the admin to access the following commands. */
        if (!isMainUser(event)) {
            return;
        }
        JDA jda = event.getJDA();
        String msgContent = event.getMessage().getContent();
        if (msgContent.equals("!test")) {
            /* Test if reaction word is created. */
            event.getMessage().addReaction("\uD83D\uDE02").queue();
        } else if (msgContent.startsWith("!get") &&
                MessageUtil.getCheckIfValidDate(msgContent)) {
            /* Get Message History from channel. */
            discUtil.getDailyHistory(event.getTextChannel(), msgContent,
                    MessageUtil.checkIfWrite(msgContent));
        } else if (msgContent.equals("!curr")) {
            /* Get current message from channel. */
            getCurrMessage(event);
        } else if (msgContent.startsWith("!writeData") &&
                MessageUtil.getCheckIfValidDate(msgContent)) {
            dataUtil.resetMap();
            for (String channelID : channelIDList) {
                // Get the channel and message history to iterate over.
                TextChannel channel = event.getGuild().getTextChannelById(channelID);
                discUtil.getDailyHistory(channel, "!get " +
                        msgContent.replace("!writeData ", ""), false);
                // Set the message history to null to reset for the next operation.
            }
            dataUtil.writeAllChannelDataExcel(event.getTextChannel());
        }

    }
}
