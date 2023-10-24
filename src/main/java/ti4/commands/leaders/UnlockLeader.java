package ti4.commands.leaders;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import ti4.helpers.Constants;
import ti4.helpers.Emojis;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.Leader;
import ti4.map.Player;
import ti4.message.MessageHelper;

public class UnlockLeader extends LeaderAction {
    public UnlockLeader() {
        super(Constants.UNLOCK_LEADER, "Unlock leader");
    }

    @Override
    void action(SlashCommandInteractionEvent event, String leaderID, Game activeGame, Player player) {
        unlockLeader(event, leaderID, activeGame, player);
    }

    public void unlockLeader(GenericInteractionCreateEvent event, String leaderID, Game activeGame, Player player) {
        Leader playerLeader = player.unsafeGetLeader(leaderID);
        MessageChannel channel = activeGame.getMainGameChannel();
        if (activeGame.isFoWMode()) channel = player.getPrivateChannel();

        if (playerLeader != null){
            playerLeader.setLocked(false);
            MessageHelper.sendMessageToChannel(channel, Emojis.getFactionLeaderEmoji(playerLeader));
          String message = player.getRepresentation() +
              " unlocked " +
              Helper.getLeaderFullRepresentation(playerLeader);
            MessageHelper.sendMessageToChannel(channel, message);
            if (playerLeader.isExhausted()){
                MessageHelper.sendMessageToChannel(channel, "Leader is also exhausted");
            }
        } else {
            MessageHelper.sendMessageToChannel(channel, "Leader not found");
        }
    }
}
