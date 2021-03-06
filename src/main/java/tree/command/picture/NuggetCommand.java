package tree.command.picture;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import javafx.util.Pair;
import net.dv8tion.jda.core.MessageBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Picture;
import tree.Config;
import tree.command.util.MessageUtil;
import tree.commandutil.CommandManager;
import tree.commandutil.type.PictureCommand;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by Admin on 7/31/2017.
 */
public class NuggetCommand implements PictureCommand {
    private String commandName;
    private static final int NUMBER_NUG_PHOTOS = 136;
    private static final String[] fileExtensions = {".jpg", ".gif", ".png"};
    private static final int SPAM_NUG_COUNT = 4;
    private PriorityQueue<PicturePair<Integer, Long>> photoIDsPosted;
    private int numNugCount = 0;
    private boolean nugPicAllowed = true;
    private boolean warningSent = false;
    private Stopwatch nuggetStopWatch;
    private Set<Integer> pastIDs;
    private ScheduledExecutorService scheduler;

    class PicturePair<K, V> implements Comparable<PicturePair<K,V>> {
        private K k;
        private V v;

        /** is the first element an instance of Comparable? */
        final boolean keyComparable;

        /** is the second element an instance of Comparable? */
        final boolean valueComparable;


        public PicturePair(K k, V v) {
            this.k = k;
            this.v = v;
            this.keyComparable = (k instanceof Comparable);
            this.valueComparable = (v instanceof Comparable);
        }

        public K getKey() {
            return k;
        }

        public V getValue() {
            return v;
        }

        @Override
        public int compareTo(PicturePair<K, V> o) {
            if (keyComparable) {
                int isComp = ((Comparable<K>) k).compareTo(o.k);
                if (isComp > 0) {
                    return 1;
                }
                if (isComp < 0) {
                    return -1;
                }
            }

            if (valueComparable) {
                int isComp = ((Comparable<V>) v).compareTo(o.v);
                if (isComp > 0) {
                    return 1;
                }
                if (isComp < 0) {
                    return -1;
                }
            }
            return 0;
        }
    }

    private String getOSNuggetPath() {
        String osName = Config.getOsName();
        if (osName.indexOf("win") >= 0) {
            return "discord-dau\\etc\\nug\\";
        } else if (osName.indexOf("nux") >= 0){
            return "discord-dau/etc/nug/";
        } else {
            return null;
        }
    }

    public NuggetCommand(String commandName) {
        this.commandName = commandName;
        photoIDsPosted = new PriorityQueue<>(Comparator.naturalOrder());
        nuggetStopWatch = Stopwatch.createUnstarted();
        nuggetStopWatch.start();
        pastIDs = new HashSet<>();
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("Picture Thread Pool").build();
        scheduler = Executors.newScheduledThreadPool(1, factory);
        scheduler.scheduleAtFixedRate(() -> {
                nugPicAllowed = true;
                warningSent = false;
                numNugCount = 0;
            },0, 20, TimeUnit.SECONDS);
    }

    @Override
    public String help() {
        return "``" + CommandManager.botToken + commandName + "``: Sends a random nugget picture to chat.";
    }

    @Override
    public void execute(Guild guild, MessageChannel msgChan,
                        Message message, Member member, String[] args) {
        if (!nugPicAllowed) {
            if (!warningSent) {
                warningSent = true;
                if (numNugCount >= SPAM_NUG_COUNT) {
                    guild.getJDA()
                            .getUserById(Config.CONFIG.getAdminID())
                            .openPrivateChannel()
                            .queue(chan ->
                                    chan.sendMessage("You got some spam bro. Seems that " +
                                            member.getUser().getName() + " is spamming at " +
                                            MessageUtil.timeStamp(message) + ".")
                                            .queue());
                }
                msgChan.sendMessage("Too many nugget command requests. Calm down bro.")
                        .queue();
            }
            return;
        }
        writeRandomNugPhoto(msgChan);
    }

    public String getCommandName() {
        return commandName;
    }

    private boolean tryFileExtension(String filePath, String filename, String ext) {
        File nugFile = new File(filePath + filename + ext);
        return nugFile.exists();
    }

    private int getRandomID() {
        int rand = (int) Math.ceil(Math.random() * NUMBER_NUG_PHOTOS);
        if (pastIDs.contains(rand)) {
            return getRandomID();
        }
        return rand;
    }

    private void incrementNuggetCount() {
        if (++numNugCount >= SPAM_NUG_COUNT) {
            nugPicAllowed = false;
        }
    }


    public void writeRandomNugPhoto(MessageChannel msgChan) {
        incrementNuggetCount(); //TODO: Figure out why some files can't be sent.
        int rand = getRandomID();
        String nugFilePath = Config.CONFIG.getFilePath() + getOSNuggetPath();
        String nugFileName = "nug" + rand;
        File outputNugFile = null;

        for (String ext : fileExtensions) {
            if (tryFileExtension(nugFilePath, nugFileName, ext)) {
                // To prevent the bot from posting the same photo five
                // consecutive times, keep track of photo IDs.
                if (photoIDsPosted.size() >= 5) {
                    PicturePair p = photoIDsPosted.poll();
                    pastIDs.remove(p.getKey());
                    System.out.println("ID = " + p.getKey() + "time = " + p.getValue());
                }
                photoIDsPosted.offer(new PicturePair<>(rand, System.currentTimeMillis()));
                pastIDs.add(rand);
                outputNugFile = new File(nugFilePath + nugFileName + ext).getAbsoluteFile();
                convertAndSendImage(outputNugFile, msgChan);
                return;
            }
        }
        System.out.println("No files found with the given extension.");
        numNugCount--;
        nugPicAllowed = true;
        if (rand == 4) {
            System.out.println("Yes, this was the PNG file");
        }
        writeRandomNugPhoto(msgChan);
    }

    private boolean convertAndSendImage(File outputNugFile, MessageChannel msgChan) {
        int fileNameLength = outputNugFile.getName().length();
        if (outputNugFile.getName()
                .substring(fileNameLength - 4, fileNameLength)
                .equals(".gif")) {
            Message msg = new MessageBuilder().append(" ").build();
            msgChan.sendFile(outputNugFile, msg).queue();
            return true;
        }
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(FileUtils.readFileToByteArray(outputNugFile));
            BufferedImage image = ImageIO.read(bais);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);

            File nugToDiscord = new File("nug.png");
            OutputStream outputStream = new FileOutputStream(nugToDiscord);
            baos.writeTo(outputStream);
            baos.close();

            Message msg = new MessageBuilder().append(" ").build();
            msgChan.sendFile(nugToDiscord, msg).queue((m) -> {},
                    (m) -> msgChan.sendMessage("You are being rate limited" +
                    " for too many nugget requests.").queueAfter(2, TimeUnit.SECONDS));
            bais.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("File could not be created.");
            return false;
        }
        return true;
    }
}
