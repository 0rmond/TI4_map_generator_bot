package ti4.commands.bothelper;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import ti4.helpers.Constants;

public class ServerLimitStats extends BothelperSubcommandData {
    public ServerLimitStats(){
        super(Constants.SERVER_LIMIT_STATS, "Server Limit Stats");
    }

    public void execute(SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();

        int memberCount = guild.getMemberCount();
        int memberMax = guild.getMaxMembers();
        int boostCount = guild.getBoostCount();
        int roleCount = guild.getRoles().size(); //250

        //CHANNELS
        List<GuildChannel> channels = guild.getChannels();
        int channelCount = channels.size(); //500
        long pbdChannelCount = channels.stream().filter(c -> c.getName().startsWith("pbd")).count();
        long categoryChannelCount = channels.stream().filter(c -> c.getType() == ChannelType.CATEGORY).count();

        //THREADS
        List<ThreadChannel> threadChannels = guild.getThreadChannels().stream().filter(c -> !c.isArchived()).toList();
        int threadCount = threadChannels.size(); //1000
        List<ThreadChannel> threadChannelsArchived = guild.getThreadChannels().stream().filter(c -> c.isArchived()).toList();
        int threadArchivedCount = threadChannelsArchived.size();
        long cardsInfoThreadCount = threadChannels.stream().filter(t -> t.getName().startsWith(Constants.CARDS_INFO_THREAD_PREFIX)).count();
        long botThreadCount = threadChannels.stream().filter(t -> t.getName().contains("-bot-map-updates")).count();
        long roundThreadCount = threadChannels.stream().filter(t -> t.getName().contains("-round-")).count();
        long privateThreadCount = threadChannels.stream().filter(t -> !t.isPublic()).count();
        long publicThreadCount = threadChannels.stream().filter(t -> t.isPublic()).count();

        int emojiCount = guild.getEmojis().size();
        int emojiMax = guild.getMaxEmojis();

        StringBuilder sb = new StringBuilder("## Server Limit Statistics:\n");
        sb.append("### Server: ").append(guild.getName()).append("\n");
        sb.append("- ").append(memberCount).append(" / " + memberMax + getPercentage(memberCount, memberMax) + " - members").append("\n");
        sb.append("- ").append(boostCount).append(" - boosts").append("\n");
        sb.append("- ").append(emojiCount).append(" / " + emojiMax + getPercentage(emojiCount, emojiMax) + " - emojis").append("\n");
        sb.append("- ").append(roleCount).append(" / 250" + getPercentage(roleCount, 250) + " - roles").append("\n");
        sb.append("### Channels:\n");
        sb.append("- ").append("**").append(channelCount).append(" / 500" + getPercentage(channelCount, 500) + " - channels**").append("\n");
        sb.append(" - ").append(categoryChannelCount).append("   " + getPercentage(categoryChannelCount, channelCount) + "  categories").append("\n");
        sb.append(" - ").append(pbdChannelCount).append("   " + getPercentage(pbdChannelCount, channelCount) + "  'pbd' channels").append("\n");
        sb.append("### Threads:\n");
        sb.append("- ").append("**").append(threadCount).append(" / 1000" + getPercentage(threadCount, 1000) + " - threads**").append("\n");
        sb.append(" - ").append("   " + threadArchivedCount + " - loaded archived threads").append("\n");
        sb.append("- ").append(privateThreadCount).append("   " + getPercentage(privateThreadCount, threadCount) + "  private threads").append("\n");
        sb.append(" - ").append(cardsInfoThreadCount).append("   " + getPercentage(cardsInfoThreadCount, threadCount) + "  'Cards Info' threads (/cards_info)").append("\n");
        sb.append("- ").append(publicThreadCount).append("   " + getPercentage(publicThreadCount, threadCount) + "  public threads").append("\n");
        sb.append(" - ").append(botThreadCount).append("   " + getPercentage(botThreadCount, threadCount) + "  '-bot-map-updates' threads").append("\n");
        sb.append(" - ").append(roundThreadCount).append("   " + getPercentage(roundThreadCount, threadCount) + "  '-round-' threads (/sc play and combat)").append("\n");
        sendMessage(sb.toString());
    }

    private String getPercentage(double numerator, double denominator) {
        NumberFormat formatPercent = NumberFormat.getPercentInstance();
        formatPercent.setMinimumFractionDigits(1);
        String formatted = formatPercent.format(denominator == 0 ? 0.0 : (numerator / denominator));
        formatted = " *(" + formatted + ")* ";
        return formatted;
    }
}
