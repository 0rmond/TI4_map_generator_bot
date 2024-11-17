package ti4.commands2.cardsso;

import java.util.List;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import ti4.commands2.GameStateSubcommand;
import ti4.helpers.Constants;
import ti4.helpers.SecretObjectiveHelper;
import ti4.map.Game;
import ti4.map.Player;
import ti4.message.MessageHelper;

class UnscoreSO extends GameStateSubcommand {

    public UnscoreSO() {
        super(Constants.UNSCORE_SO, "Unscore Secret Objective", true, true);
        addOptions(new OptionData(OptionType.INTEGER, Constants.SECRET_OBJECTIVE_ID, "Scored Secret objective ID that is sent between ()").setRequired(true));
        addOptions(new OptionData(OptionType.STRING, Constants.FACTION_COLOR, "Faction or Color for which you set stats")
            .setAutoComplete(true));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        Game game = getGame();
        Player player = getPlayer();
        int soId = event.getOption(Constants.SECRET_OBJECTIVE_ID).getAsInt();
        boolean scored = game.unscoreSecretObjective(player.getUserID(), soId);
        if (!scored) {
            List<String> scoredSOs = player.getSecretsScored().entrySet().stream()
                .map(e -> "> (" + e.getValue() + ") " + SecretObjectiveHelper.getSecretObjectiveRepresentationShort(e.getKey()))
                .toList();
            StringBuilder sb = new StringBuilder("Secret Objective ID found - please retry.\nYour current scored SOs are:\n");
            scoredSOs.forEach(sb::append);
            if (scoredSOs.isEmpty()) sb.append("> None");
            MessageHelper.sendMessageToChannel(event.getMessageChannel(), sb.toString());
            return;
        }

        MessageHelper.sendMessageToChannel(event.getMessageChannel(), "Unscored SO " + soId);
        SecretObjectiveHelper.sendSecretObjectiveInfo(game, player, event);
    }
}