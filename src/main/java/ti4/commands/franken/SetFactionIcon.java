package ti4.commands.franken;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.helpers.Constants;
import ti4.helpers.Helper;
import ti4.map.Game;
import ti4.map.Player;

public class SetFactionIcon extends FrankenSubcommandData {

    public SetFactionIcon() {
        super(Constants.SET_FACTION_ICON, "Set franken faction icon to use");
        addOptions(new OptionData(OptionType.STRING, Constants.FACTION_EMOJI, "Emoji to use"));
        addOptions(new OptionData(OptionType.BOOLEAN, Constants.RESET, "Reset icon to default"));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game activeGame = getActiveGame();
        Player player = activeGame.getPlayer(getUser().getId());
        player = Helper.getGamePlayer(activeGame, player, event, null);
        if (player == null) {
            sendMessage("Player could not be found");
            return;
        }

        if (event.getOption(Constants.RESET) != null && event.getOption(Constants.RESET).getAsBoolean()) {
            player.setFactionEmoji(null);
            return;
        }

        String factionEmoji = event.getOption(Constants.FACTION_EMOJI, null, OptionMapping::getAsString);
        if (factionEmoji != null) {
            player.setFactionEmoji(factionEmoji);
        }
    }
    
}