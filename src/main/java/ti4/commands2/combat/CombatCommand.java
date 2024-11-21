package ti4.commands2.combat;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import ti4.commands2.CommandHelper;
import ti4.commands2.ParentCommand;
import ti4.commands2.Subcommand;
import ti4.helpers.Constants;

public class CombatCommand implements ParentCommand {

    private final Map<String, Subcommand> subcommands = Stream.of(
                    new CombatRoll(),
                    new StartCombat())
            .collect(Collectors.toMap(Subcommand::getName, subcommand -> subcommand));

    @Override
    public String getName() {
        return Constants.COMBAT;
    }

    @Override
    public String getDescription() {
        return "Combat";
    }

    @Override
    public boolean accept(SlashCommandInteractionEvent event) {
        return ParentCommand.super.accept(event) &&
            CommandHelper.acceptIfPlayerInGameAndGameChannel(event);
    }

    @Override
    public Map<String, Subcommand> getSubcommands() {
        return subcommands;
    }
}