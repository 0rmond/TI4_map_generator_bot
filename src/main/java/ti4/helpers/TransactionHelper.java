package ti4.helpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import ti4.buttons.Buttons;
import ti4.commands.cardsac.ACInfo;
import ti4.commands.cardspn.PNInfo;
import ti4.commands.cardspn.PlayPN;
import ti4.commands.leaders.CommanderUnlockCheck;
import ti4.commands.player.ClearDebt;
import ti4.commands.relic.RelicSendFragments;
import ti4.commands.uncategorized.CardsInfo;
import ti4.generator.Mapper;
import ti4.helpers.Units.UnitType;
import ti4.listeners.annotations.ButtonHandler;
import ti4.map.Game;
import ti4.map.Player;
import ti4.map.UnitHolder;
import ti4.message.MessageHelper;
import ti4.model.PromissoryNoteModel;

public class TransactionHelper {

    public static void acceptTransactionOffer(Player p1, Player p2, Game game, ButtonInteractionEvent event) {
        List<String> transactionItems = p1.getTransactionItemsWithPlayer(p2);
        List<Player> players = new ArrayList<>();
        players.add(p1);
        players.add(p2);
        boolean debtOnly = true;
        MessageChannel channel = p1.getCorrectChannel();
        if (game.getName().equalsIgnoreCase("pbd1000")) {
            channel = game.getTableTalkChannel();
            MessageHelper.sendMessageToChannel(game.getMainGameChannel(), p1.getRepresentation(false, false) + " and" + p2.getRepresentation(false, false) + " have transacted");
        }

        String publicSummary = "A transaction has been ratified:\n" + buildTransactionOffer(p1, p2, game, true);
        String privateSummary = "The following transaction has been accepted:\n" + buildTransactionOffer(p1, p2, game, false);
        MessageHelper.sendMessageToChannel(channel, publicSummary);
        for (Player sender : players) {
            Player receiver = p2;
            if (sender == p2) {
                receiver = p1;
            }
            for (String item : transactionItems) {
                if (item.contains("sending" + sender.getFaction()) && item.contains("receiving" + receiver.getFaction())) {
                    String thingToTransact = item.split("_")[2];
                    String furtherDetail = item.split("_")[3];
                    int amountToTransact = 1;
                    if (((thingToTransact.equalsIgnoreCase("ACs") || thingToTransact.equalsIgnoreCase("PNs")) && furtherDetail.contains("generic"))) {
                        amountToTransact = Integer.parseInt("" + furtherDetail.charAt(furtherDetail.length() - 1));
                        furtherDetail = furtherDetail.substring(0, furtherDetail.length() - 1);
                    }
                    String spoofedButtonID = "send_" + thingToTransact + "_" + receiver.getFaction() + "_" + furtherDetail;
                    if (!thingToTransact.toLowerCase().contains("debt")) {
                        debtOnly = false;
                    }
                    switch (thingToTransact) {
                        case "ACs" -> {
                            switch (furtherDetail) {
                                case "generic" -> {
                                    for (int x = 0; x < amountToTransact; x++) {
                                        String buttonID = "transact_ACs_" + receiver.getFaction();
                                        resolveSpecificTransButtonsOld(game, sender, buttonID, event);
                                    }
                                }
                                default -> resolveSpecificTransButtonPress(game, sender, spoofedButtonID, event, false);

                            }
                        }
                        case "PNs" -> {
                            switch (furtherDetail) {
                                case "generic" -> {
                                    List<Button> stuffToTransButtons = ButtonHelper.getForcedPNSendButtons(game, receiver, sender);
                                    String message = sender.getRepresentationUnfogged()
                                        + "Please select the promissory note you would like to send.";
                                    MessageHelper.sendMessageToChannelWithButtons(sender.getCardsInfoThread(), message, stuffToTransButtons);
                                }
                                default -> resolveSpecificTransButtonPress(game, sender, spoofedButtonID, event, false);

                            }
                        }
                        case "Planets" -> ButtonHelperFactionSpecific.resolveHacanMechTradeStepOne(sender, game, event, "send_" + furtherDetail + "_" + receiver.getFaction());
                        case "AlliancePlanets" -> {
                            String exhausted = "exhausted";
                            if (!furtherDetail.contains(exhausted)) {
                                exhausted = "refreshed";
                            }
                            furtherDetail = furtherDetail.replace(exhausted, "");

                            ButtonHelper.resolveAllianceMemberPlanetTrade(sender, game, event, "send_" + furtherDetail + "_" + receiver.getFaction() + "_" + exhausted);
                        }
                        case "dmz" -> ButtonHelper.resolveDMZTrade(sender, game, event, "send_" + furtherDetail + "_" + receiver.getFaction());
                        default -> resolveSpecificTransButtonPress(game, sender, spoofedButtonID, event, false);
                    }
                }
            }
        }

        // Send Summary to Player's CardsInfo threads
        MessageHelper.sendMessageToChannel(p1.getCardsInfoThread(), p1.getRepresentationUnfogged() + " " + privateSummary);
        MessageHelper.sendMessageToChannel(p2.getCardsInfoThread(), p2.getRepresentationUnfogged() + " " + privateSummary);

        p1.clearTransactionItemsWithPlayer(p2);
        if (!debtOnly) {
            ButtonHelperAbilities.pillageCheck(p1, game);
            ButtonHelperAbilities.pillageCheck(p2, game);
        }
    }

    private static String buildTransactionOffer(Player p1, Player p2, Game game, boolean hidePrivateCardText) {
        List<String> transactionItems = p1.getTransactionItemsWithPlayer(p2);
        StringBuilder trans = new StringBuilder();
        List<Player> players = new ArrayList<>();
        players.add(p1);
        players.add(p2);
        for (Player player : players) {
            trans.append("> ").append(player.getRepresentation(false, false, true)).append(" gives:\n");
            boolean sendingNothing = true;
            for (String item : transactionItems) {
                if (!item.contains("sending" + player.getFaction())) {
                    continue;
                }
                trans.append("> - ");
                sendingNothing = false;
                String thingToTransact = item.split("_")[2];
                String furtherDetail = item.split("_")[3];
                int amountToTransact = 1;
                if (thingToTransact.equalsIgnoreCase("frags") || ((thingToTransact.equalsIgnoreCase("PNs") || thingToTransact.equalsIgnoreCase("ACs")) && furtherDetail.contains("generic"))) {
                    amountToTransact = Integer.parseInt("" + furtherDetail.charAt(furtherDetail.length() - 1));
                    furtherDetail = furtherDetail.substring(0, furtherDetail.length() - 1);
                }
                switch (thingToTransact) {
                    case "TGs" -> {
                        amountToTransact = Integer.parseInt(furtherDetail);
                        if (amountToTransact > 4) {
                            trans.append(amountToTransact).append(Emojis.tg);
                        } else {
                            trans.append(Emojis.tg(amountToTransact));
                        }
                    }
                    case "Comms" -> {
                        amountToTransact = Integer.parseInt(furtherDetail);
                        if (amountToTransact > 4) {
                            trans.append(amountToTransact).append(Emojis.comm);
                        } else {
                            trans.append(Emojis.comm(amountToTransact));
                        }
                    }
                    case "SendDebt" -> {
                        amountToTransact = Integer.parseInt(furtherDetail);
                        trans.append("Send ").append(amountToTransact).append(" debt tokens");
                    }
                    case "ClearDebt" -> {
                        amountToTransact = Integer.parseInt(furtherDetail);
                        trans.append("Clear ").append(amountToTransact).append(" debt tokens");
                    }
                    case "shipOrders" -> trans.append(Mapper.getRelic(furtherDetail).getName()).append(Emojis.axis);
                    case "starCharts" -> trans.append(Mapper.getRelic(furtherDetail).getName()).append(Emojis.DiscordantStars);
                    case "ACs" -> {
                        switch (furtherDetail) {
                            case "generic" -> {
                                trans.append(amountToTransact).append(" ").append(Emojis.ActionCard).append(" to be specified verbally");
                            }
                            default -> {
                                int acNum = Integer.parseInt(furtherDetail);
                                String acID = null;
                                if (!player.getActionCards().containsValue(acNum)) {
                                    continue;
                                }
                                for (Map.Entry<String, Integer> ac : player.getActionCards().entrySet()) {
                                    if (ac.getValue().equals(acNum)) {
                                        acID = ac.getKey();
                                    }
                                }
                                trans.append(Emojis.ActionCard);
                                if (!hidePrivateCardText) {
                                    trans.append(" ").append(Mapper.getActionCard(acID).getName());
                                }
                            }

                        }
                    }
                    case "PNs" -> {
                        switch (furtherDetail) {
                            case "generic" -> {
                                if (!hidePrivateCardText) {
                                    trans.append(amountToTransact).append(" ").append(Emojis.PN).append(" to be specified verbally");
                                } else {
                                    trans.append(Emojis.PN);
                                }
                            }
                            default -> {
                                String id = null;
                                int pnIndex;
                                try {
                                    pnIndex = Integer.parseInt(furtherDetail);
                                    for (Map.Entry<String, Integer> pn : player.getPromissoryNotes().entrySet()) {
                                        if (pn.getValue().equals(pnIndex)) {
                                            id = pn.getKey();
                                        }
                                    }
                                } catch (NumberFormatException e) {
                                    id = furtherDetail.replace("fin9", "_");
                                }
                                if (id == null) {
                                    // continue;
                                }
                                trans.append(Emojis.PN);
                                if (!hidePrivateCardText) {
                                    trans.append(" ").append(StringUtils.capitalize(Mapper.getPromissoryNote(id).getColor().orElse(""))).append(" ").append(Mapper.getPromissoryNote(id).getName());
                                }
                            }
                        }
                    }
                    case "Frags" -> trans.append(Emojis.getFragEmoji(furtherDetail).repeat(amountToTransact));
                    case "Planets", "AlliancePlanets", "dmz" -> trans.append(Helper.getPlanetRepresentationPlusEmojiPlusResourceInfluence(furtherDetail, game));
                    case "action" -> trans.append("An in-game ").append(furtherDetail).append(" action");
                    default -> trans.append(" some odd thing: `").append(item).append("`");
                }
                trans.append("\n");
            }
            if (sendingNothing) {
                String nothing = game.getStoredValue(player.getFaction() + "NothingMessage");
                if (nothing.isEmpty()) {
                    nothing = getNothingMessage();
                    game.setStoredValue(player.getFaction() + "NothingMessage", nothing);
                }
                trans.append("> ").append(nothing).append("\n");
            }
        }

        return trans.toString();
    }

    private static final List<String> nothingMessages = Arrays.asList(
        "Nothing But Respect And Good Will",
        "Some Pocket Lint",
        "Sunshine and Rainbows",
        "A Sense of Pride and Accomplishment",
        "A Crisp High Five",
        "A Well Written Thank-You Note",
        "Heartfelt Thanks",
        "The Best Vibes",
        "A Blessing",
        "Good Karma",
        "A Mewling Kitten",
        "A Lost Virtual Puppy",
        "A Fortune Cookie",
        "A Firm Handshake",
        "A Friendly Wave",
        "Well Wishes",
        "A Home-cooked Meal",
        "$1000 In Monopoly Money",
        "Forgiveness For Past Mistakes",
        "A Lucky Rock",
        "A Warm Cup of Tea",
        "A Poorly Drawn But Deeply Meaningful Picture",
        "An Unexpected Hug",
        "A Magic Trick",
        "A Pair of Comfy Socks",
        "A Whiff of Fresh Cookies",
        "A Charming Smile",
        "A Promise to Call Later",
        "A Supportive Cheer",
        "A Playful Joke",
        "A Chance to See A Beautiful Sunset",
        "A Treasure Map",
        "A Song",
        "A Book Recommendation",
        "A Cozy Blanket",
        "A Cheery Greeting",
        "A Bucket of Joy",
        "A Gentle Reminder",
        "A Heartwarming Story",
        "A Whisper of Kindness",
        "An Expired Gift Certificate",
        "A Free Trial CD for AOL",
        "Compost For Your Garden",
        "A Tupperware Party Invitation",
        "A Picture of a Sandwich",
        "Thoughtful Advice About Your Current Situation",
        "Zip; Zilch; Nada",
        "Approximately " + String.format("%,d", 5 * ThreadLocalRandom.current().nextInt(200, 2000) + ThreadLocalRandom.current().nextInt(1, 5)) + " Unique Snow Globes",
        "Forgiveness For Future Mistakes (Terms and Conditions Apply)",
        "A Token Labelled \"Traid Gud\"",
        "A Hill of Beans",
        "This Small Portrait of Benjamin Franklin, Done in Oil Paints",
        "Favourable Timing on Bureaucracy",
        "A Comfortable Sinecure, When I'm Galactic Emperor",
        "This Box of One Dozen Starving, Crazed Weasels",
        "A jpeg Depicting a Cartoon Monkey",
        "A Somewhat Rare Beanie Baby",
        "Some Good News About Our Lord and Saviour, Harrugh Gefhara",
        "No Spoilers for that TV Show You're Watching",
        "Payment in Exposure",
        "A VHS Recording of an Episode of _Bill Nye The Science Guy_",
        "A Short But Comprehensive Lecture on Medieval Siege Weaponry",
        "An _E.T. the Extra-Terrestrial_ Cartridge for the Atari 2600",
        "A Nice Solid Thumbs Up",
        "A Handful of Dog Treats",
        "One (1) Peppercorn",
        "Poutine",
        "The Deputy Speakership",
        "Half a Slice of Pizza, With or Without Pineapple",
        "The Wi-Fi Password",
        "A Second-Hand Toothbrush",
        "A Lamp That Might Contain a Genie, But Probably Doesn't",
        "An Acoustic Rendition of _Wonderwall_",
        "As Many Spiders as You Desire",
        "Fruit Salad (yummy, yummy)",
        "One Chocolate Chip Muffin Amongst Eleven Raisin Muffins",
        "Invoking the A̴̰̽̑ͅn̶͙͝ĉ̸̤̜̽i̶̯̯͋ě̶͓̜͑n̶̤̩̉t̸̯̎͊͜ ̷́ͅP̶̘̀a̸̧̔̅c̶̣̋̔t̷̺̪͛͋",
        "Some Perfunctory Laughter at Your Next Attempt at a Joke",
        "The Front Half of Our Pantomime Horse",
        "~~False~~ Reassurances",
        "A Big Mouth Billy Bass",
        "More Cowbell",
        "Some Subpar Macaroni Art",
        "A Chocolate Teapot",
        "Some Week-Old Sushi",
        "My Second Finest Bottle of Wine Drink™",
        "A Riddle, Wrapped in a Mystery, Inside an Enigma, Coated in Chocolate",
        "A Brand-New Luxury Car, Missing Only Fuel, Tires and Car",
        "Either \"Peace\" or \"Peas\"; the Ambassador Failed to Elaborate",
        "A Year's Supply of Brussels Sprouts",
        "A Nintendo Power Glove; ***Now You're Playing With Power***",
        "A Wooden Spoon",
        "An Ingot of Pyrite",
        "A White Elephant",
        "Ennui",
        "A Smurf TV Tray",
        "A Creepy Doll",
        "A Ziploc Bag of Ranch Dressing",
        "Nothing. And Furthermore, Carthage Must be Destroyed!",
        "All of the Goulash",
        "Waldo's Location",
        "A Billet of Ea-nāṣir's Finest Copper",
        "All the Silver in Fort Knox",
        "A Controlling Share of The Bereg Jet Ski Company",
        "A Handful of Specially Marked Cereal Boxtops",
        "An Aperture Science Thing We Don't Know What It Does",
        "Nothing, Because I'm a Cheapskate",
        "A Brick, Delivery Speed TBD",
        "An Inanimate Carbon Rod",
        "A Set of Left-Handed Sarween Tools",
        "A Bridge That's For Sale",
        "In return for this small, helpful deed // A limerick is what I shall cede // It won't cost me a dime // If I trade you this rhyme // To brighten your day, yes indeed!",
        "The Chameleon's Dish",
        "The Sound of One Hand Clapping",
        "An Unpaired Sock",
        "\"101 Ways To Make Toast\"",
        "A Chess Set With 31 Missing Pieces",
        "Your Horoscope Reading",
        "Just Deserts",
        "Surprise and Delight",
        "`//Could somebody get ChatGPT to generate a few more messages - Dev`",
        "Some Free Candy, From My Windowless Van");

    public static String getNothingMessage() {
        if (ThreadLocalRandom.current().nextInt(1000000) == 0) {
            return "The joy of sharing a one in a million empty transaction offer message";
        }

        int result = ThreadLocalRandom.current().nextInt(0, nothingMessages.size() + 20);
        try {
            return nothingMessages.get(result);
        } catch (Exception e) {
            return "Nothing";
        }
    }

    @ButtonHandler("transaction")
    public static void transaction(Player player, Game game) {
        List<Button> buttons;
        buttons = TransactionHelper.getPlayersToTransact(game, player);
        String message = player.getRepresentation() + " Use the buttons to select which player you wish to transact with";
        MessageHelper.sendMessageToChannelWithButtons(player.getCardsInfoThread(), message, buttons);
    }

    @ButtonHandler("newTransact_")
    public static void resolveSpecificTransButtonsNew(Game game, Player player, String buttonID, ButtonInteractionEvent event) {
        List<Button> stuffToTransButtons = new ArrayList<>();
        buttonID = buttonID.replace("newTransact_", "");
        String thingToTrans = buttonID.substring(0, buttonID.indexOf("_"));
        String senderFaction = buttonID.split("_")[1];
        String receiverFaction = buttonID.split("_")[2];
        Player p1 = game.getPlayerFromColorOrFaction(senderFaction);
        Player p2 = game.getPlayerFromColorOrFaction(receiverFaction);
        if (p2 == null) {
            return;
        }
        boolean requesting = (p1 != player);
        Player opposing = p2;
        if (player == p2) {
            opposing = p1;
        }
        String message = "Current Transaction Offer is:\n" + TransactionHelper.buildTransactionOffer(player, opposing, game, false) + "\n";
        String requestOrOffer = "offer";
        if (requesting) {
            requestOrOffer = "request";
        }
        switch (thingToTrans) {
            case "TGs" -> {
                message = message + " Click the amount of TGs you would like to " + requestOrOffer;
                for (int x = 1; x < p1.getTg() + 1; x++) {
                    Button transact = Buttons.green(
                        "offerToTransact_TGs_" + p1.getFaction() + "_" + p2.getFaction() + "_" + x, "" + x);
                    stuffToTransButtons.add(transact);
                }
            }
            case "Comms" -> {
                message = message + " Click the amount of commodities you would like to " + requestOrOffer;
                for (int x = 1; x < p1.getCommodities() + 1; x++) {
                    Button transact = Buttons.green(
                        "offerToTransact_Comms_" + p1.getFaction() + "_" + p2.getFaction() + "_" + x, "" + x);
                    stuffToTransButtons.add(transact);
                }

            }
            case "ClearDebt" -> {
                message = message + " Click the amount of debt you would like to " + requestOrOffer + " cleared";
                for (int x = 1; x < p1.getDebtTokenCount(p2.getColor()) + 1; x++) {
                    Button transact = Buttons.green(
                        "offerToTransact_ClearDebt_" + p1.getFaction() + "_" + p2.getFaction() + "_" + x,
                        "" + x);
                    stuffToTransButtons.add(transact);
                }

            }
            case "SendDebt" -> {
                message = message + " Click the amount of debt you would like to " + requestOrOffer;
                for (int x = 1; x < 6; x++) {
                    Button transact = Buttons.green(
                        "offerToTransact_SendDebt_" + p1.getFaction() + "_" + p2.getFaction() + "_" + x, "" + x);
                    stuffToTransButtons.add(transact);
                }

            }
            case "shipOrders" -> {
                message = message + " Click the axis order you would like to " + requestOrOffer;
                for (String shipOrder : ButtonHelper.getPlayersShipOrders(p1)) {
                    Button transact = Buttons.green(
                        "offerToTransact_shipOrders_" + p1.getFaction() + "_" + p2.getFaction() + "_" + shipOrder,
                        Mapper.getRelic(shipOrder).getName());
                    stuffToTransButtons.add(transact);
                }

            }
            case "starCharts" -> {
                message = message + " Click the star chart you would like to " + requestOrOffer;
                for (String shipOrder : ButtonHelper.getPlayersStarCharts(p1)) {
                    Button transact = Buttons.green(
                        "offerToTransact_starCharts_" + p1.getFaction() + "_" + p2.getFaction() + "_" + shipOrder,
                        Mapper.getRelic(shipOrder).getName());
                    stuffToTransButtons.add(transact);
                }
            }
            case "Planets" -> {
                message = message + " Click the planet you would like to " + requestOrOffer;
                for (String planet : p1.getPlanetsAllianceMode()) {
                    if (planet.contains("custodia") || planet.contains("ghoti")) {
                        continue;
                    }
                    if (ButtonHelper.getUnitHolderFromPlanetName(planet, game).getUnitCount(UnitType.Mech,
                        p1.getColor()) > 0) {
                        stuffToTransButtons.add(Buttons.gray(
                            "offerToTransact_Planets_" + p1.getFaction() + "_" + p2.getFaction() + "_" + planet,
                            Helper.getPlanetRepresentation(planet, game)));
                    }
                }

            }
            case "AlliancePlanets" -> {
                message = message + " Click the planet you would like to " + requestOrOffer;
                for (String planet : p1.getPlanets()) {
                    if (planet.contains("custodia") || planet.contains("ghoti")) {
                        continue;
                    }
                    UnitHolder unitHolder = ButtonHelper.getUnitHolderFromPlanetName(planet, game);
                    if (unitHolder != null && unitHolder.getUnitColorsOnHolder().contains(p2.getColorID())) {
                        String refreshed = "refreshed";
                        if (p1.getExhaustedPlanets().contains(planet)) {
                            refreshed = "exhausted";
                        }
                        stuffToTransButtons.add(Buttons.gray(
                            "offerToTransact_AlliancePlanets_" + p1.getFaction() + "_" + p2.getFaction() + "_"
                                + planet + refreshed,
                            Helper.getPlanetRepresentation(planet, game)));
                    }
                }
            }
            case "ACs" -> {
                if (requesting) {
                    message = message + player.getRepresentation(false, false)
                        + " Click the number of ACs you'd like to request. Since ACs are private info, you will have to use messages to explain what ACs you want, these buttons will just make sure that the player is offered buttons to send.";
                    int limit = Math.min(7, p2.getAc());
                    for (int x = 1; x < limit + 1; x++) {
                        Button transact = Buttons.green(
                            "offerToTransact_ACs_" + p1.getFaction() + "_" + p2.getFaction() + "_generic" + x,
                            x + " ACs");
                        stuffToTransButtons.add(transact);
                    }
                } else {
                    message = message + player.getRepresentation(false, false)
                        + " Click the GREEN button that indicates the AC you would like to " + requestOrOffer;
                    for (String acShortHand : p1.getActionCards().keySet()) {
                        Button transact = Buttons.green(
                            "offerToTransact_ACs_" + p1.getFaction() + "_" + p2.getFaction() + "_"
                                + p1.getActionCards().get(acShortHand),
                            Mapper.getActionCard(acShortHand).getName());
                        stuffToTransButtons.add(transact);
                    }
                }
            }
            case "PNs" -> {
                if (requesting) {
                    message = message + player.getRepresentation(false, false)
                        + " Click the PN you'd like to request. Since PNs are private info, all of the player's starting PNs which are not in play areas are available, though the player may not currently hold all of these. Click TBD Note if you want someone else's PN, and it will give the player the option to send it.";
                    for (String pnShortHand : p1.getPromissoryNotesOwned()) {
                        if (ButtonHelper.anyoneHaveInPlayArea(game, pnShortHand)) {
                            continue;
                        }
                        PromissoryNoteModel promissoryNote = Mapper.getPromissoryNote(pnShortHand);
                        Player owner = game.getPNOwner(pnShortHand);
                        if (p1.getPromissoryNotes().containsKey(pnShortHand)) {
                            stuffToTransButtons.add(Buttons.green("offerToTransact_PNs_" + p1.getFaction() + "_" + p2.getFaction() + "_" + p1.getPromissoryNotes().get(pnShortHand), promissoryNote.getName()).withEmoji(Emoji.fromFormatted(owner.getFactionEmoji())));
                        } else {
                            stuffToTransButtons.add(Buttons.green("offerToTransact_PNs_" + p1.getFaction() + "_" + p2.getFaction() + "_" + pnShortHand.replace("_", "fin9"), promissoryNote.getName()).withEmoji(Emoji.fromFormatted(owner.getFactionEmoji())));
                        }

                    }
                    Button transact = Button
                        .primary("offerToTransact_PNs_" + p1.getFaction() + "_" + p2.getFaction() + "_"
                            + "generic1", "TBD PN");

                    stuffToTransButtons.add(transact);
                } else {
                    message = message + p1.getRepresentation(true, false) + " Click the PN you would like to "
                        + requestOrOffer;
                    for (String pnShortHand : p1.getPromissoryNotes().keySet()) {
                        if (p1.getPromissoryNotesInPlayArea().contains(pnShortHand)
                            || (p2.getAbilities().contains("hubris") && pnShortHand.endsWith("an"))) {
                            continue;
                        }
                        PromissoryNoteModel promissoryNote = Mapper.getPromissoryNote(pnShortHand);
                        Player owner = game.getPNOwner(pnShortHand);
                        Button transact = Buttons.green("offerToTransact_PNs_" + p1.getFaction() + "_" + p2.getFaction() + "_" + p1.getPromissoryNotes().get(pnShortHand), promissoryNote.getName()).withEmoji(Emoji.fromFormatted(owner.getFactionEmoji()));

                        stuffToTransButtons.add(transact);
                    }
                }
                MessageHelper.sendMessageToChannel(player.getCardsInfoThread(),
                    "Reminder that, unlike other things, you may only send a person 1 PN in a transaction.");
            }
            case "Frags" -> {
                message = message + " Click the amount of fragments you would like to " + requestOrOffer;
                for (int x = 1; x < p1.getCrf() + 1; x++) {
                    Button transact = Buttons.blue(
                        "offerToTransact_Frags_" + p1.getFaction() + "_" + p2.getFaction() + "_CRF" + x, "Cultural Fragments (x" + x + ")");
                    stuffToTransButtons.add(transact);
                }

                for (int x = 1; x < p1.getIrf() + 1; x++) {
                    Button transact = Buttons.green(
                        "offerToTransact_Frags_" + p1.getFaction() + "_" + p2.getFaction() + "_IRF" + x, "Industrial Fragments (x" + x + ")");
                    stuffToTransButtons.add(transact);
                }

                for (int x = 1; x < p1.getHrf() + 1; x++) {
                    Button transact = Buttons.red(
                        "offerToTransact_Frags_" + p1.getFaction() + "_" + p2.getFaction() + "_HRF" + x, "Hazardous Fragments (x" + x + ")");
                    stuffToTransButtons.add(transact);
                }

                for (int x = 1; x < p1.getUrf() + 1; x++) {
                    Button transact = Buttons.gray(
                        "offerToTransact_Frags_" + p1.getFaction() + "_" + p2.getFaction() + "_URF" + x, "Frontier Fragments (x" + x + ")");
                    stuffToTransButtons.add(transact);
                }

            }
        }
        event.getMessage().delete().queue();
        MessageHelper.sendMessageToChannelWithButtons(player.getCardsInfoThread(), message, stuffToTransButtons);
    }

    @ButtonHandler("offerToTransact_")
    public static void resolveOfferToTransact(Game game, Player player, String buttonID, ButtonInteractionEvent event) {
        String item = buttonID.split("_")[1];
        String sender = buttonID.split("_")[2];
        String receiver = buttonID.split("_")[3];
        String extraDetail = buttonID.split("_")[4];
        Player p1 = game.getPlayerFromColorOrFaction(sender);
        Player p2 = game.getPlayerFromColorOrFaction(receiver);
        if (item.equalsIgnoreCase("washComms")) {
            int oldP1Comms = p1.getCommodities();
            int newP1Comms = 0;
            int totalWashPowerP1 = p1.getCommodities() + p1.getTg();
            int totalWashPowerP2 = p2.getCommodities() + p2.getTg();
            if (oldP1Comms > totalWashPowerP2) {
                newP1Comms = oldP1Comms - totalWashPowerP2;
            }
            int commsOfP1Washed = oldP1Comms - newP1Comms;
            int oldP2Comms = p2.getCommodities();
            int newP2Comms = 0;
            if (oldP2Comms > totalWashPowerP1) {
                newP2Comms = oldP2Comms - totalWashPowerP1;
            }
            int commsOfP2Washed = oldP2Comms - newP2Comms;
            int tgP1Sent = commsOfP2Washed - commsOfP1Washed;
            int tgP2Sent = commsOfP1Washed - commsOfP2Washed;
            if (commsOfP1Washed > 0) {
                player.addTransactionItem("sending" + p1.getFaction() + "_receiving" + p2.getFaction() + "_Comms_" + commsOfP1Washed);
            }
            if (commsOfP2Washed > 0) {
                player.addTransactionItem("sending" + p2.getFaction() + "_receiving" + p1.getFaction() + "_Comms_" + commsOfP2Washed);
            }
            if (tgP1Sent > 0) {
                player.addTransactionItem("sending" + p1.getFaction() + "_receiving" + p2.getFaction() + "_TGs_" + tgP1Sent);
            }
            if (tgP2Sent > 0) {
                player.addTransactionItem("sending" + p2.getFaction() + "_receiving" + p1.getFaction() + "_TGs_" + tgP2Sent);
            }
        } else {
            player.addTransactionItem("sending" + sender + "_receiving" + receiver + "_" + item + "_" + extraDetail);
        }

        if ((item.equalsIgnoreCase("tgs") || item.equalsIgnoreCase("Comms")) && p2.getDebtTokenCount(p1.getColor()) > 0 && !p1.hasAbility("binding_debts")) {
            int amount = Math.min(p2.getDebtTokenCount(p1.getColor()), Integer.parseInt(extraDetail));
            player.addTransactionItem("sending" + receiver + "_receiving" + sender + "_ClearDebt_" + amount);
        }
        Player opposing = p2;
        if (player == p2) {
            opposing = p1;
        }
        String message = "Current Transaction Offer is:\n" + TransactionHelper.buildTransactionOffer(player, opposing, game, false)
            + "\n## Click something else that you want to request from " + p1.getRepresentation(false, false);
        if (p1 == player) {
            message = "Current Transaction Offer is:\n" + TransactionHelper.buildTransactionOffer(player, opposing, game, false)
                + "\n## Click something else that YOU want to offer";
        }
        event.getMessage().delete().queue();
        MessageHelper.sendMessageToChannel(player.getCardsInfoThread(), message, getStuffToTransButtonsNew(game, player, p1, p2));
    }

    @ButtonHandler("getNewTransaction_")
    public static void getNewTransaction(Game game, Player player, String buttonID, ButtonInteractionEvent event) {
        String sender = buttonID.split("_")[1];
        String receiver = buttonID.split("_")[2];
        Player p1 = game.getPlayerFromColorOrFaction(sender);
        Player p2 = game.getPlayerFromColorOrFaction(receiver);
        Player opposing = p2;
        if (player == p2) {
            opposing = p1;
        }
        String message = "Current Transaction Offer is:\n" + TransactionHelper.buildTransactionOffer(player, opposing, game, false)
            + "\n## Click something that you want to request from " + p1.getRepresentation(false, false);
        if (p1 == player) {
            message = "Current Transaction Offer is:\n" + TransactionHelper.buildTransactionOffer(player, opposing, game, false)
                + "\n## Click something that YOU want to offer";
        }
        event.getMessage().delete().queue();
        MessageHelper.sendMessageToChannel(player.getCardsInfoThread(), message, getStuffToTransButtonsNew(game, player, p1, p2));
    }

    @ButtonHandler("sendOffer_")
    public static void sendOffer(Game game, Player player, String buttonID, ButtonInteractionEvent event) {
        Player p2 = game.getPlayerFromColorOrFaction(buttonID.split("_")[1]);
        MessageHelper.sendMessageToChannel(player.getCorrectChannel(), player.getFactionEmoji() + " sent a transaction offer to " + p2.getFactionEmoji());
        if (game.getTableTalkChannel() != null) {
            String offerMessage = "Trade offer from " + player.getFactionEmoji() + " to " + p2.getFactionEmoji() + ":\n" + TransactionHelper.buildTransactionOffer(player, p2, game, true);
            MessageHelper.sendMessageToChannel(game.getTableTalkChannel(), offerMessage);
        }

        List<Button> buttons = new ArrayList<>();
        buttons.add(Buttons.red("rescindOffer_" + p2.getFaction(), "Rescind Offer"));
        MessageHelper.sendMessageToChannelWithButtons(player.getCardsInfoThread(), player.getRepresentationNoPing() + " you sent a transaction offer to " + p2.getRepresentationNoPing() + ":\n" + buildTransactionOffer(player, p2, game, false), buttons);

        event.getMessage().delete().queue();

        int offerNumber = 1;
        String key = "offerFrom" + player.getFaction() + "To" + p2.getFaction();
        if (!game.getStoredValue(key).isEmpty()) {
            offerNumber = Integer.parseInt(game.getStoredValue(key)) + 1;
        }
        game.setStoredValue(key, offerNumber + "");

        buttons = new ArrayList<>();
        buttons.add(Buttons.green("acceptOffer_" + player.getFaction() + "_" + offerNumber, "Accept"));
        buttons.add(Buttons.red("rejectOffer_" + player.getFaction(), "Reject"));
        buttons.add(Buttons.red("resetOffer_" + player.getFaction(), "Reject and CounterOffer"));
        MessageHelper.sendMessageToChannelWithButtons(p2.getCardsInfoThread(), p2.getRepresentation() + " you have received a transaction offer from " + player.getRepresentationNoPing() + ":\n" + buildTransactionOffer(player, p2, game, false), buttons);
        checkTransactionLegality(game, p2, player);
    }

    @ButtonHandler("transact_")
    public static void resolveSpecificTransButtonsOld(Game game, Player p1, String buttonID, ButtonInteractionEvent event) {
        String finChecker = "FFCC_" + p1.getFaction() + "_";

        List<Button> stuffToTransButtons = new ArrayList<>();
        buttonID = buttonID.replace("transact_", "");
        String thingToTrans = buttonID.substring(0, buttonID.indexOf("_"));
        String factionToTrans = buttonID.substring(buttonID.indexOf("_") + 1);
        Player p2 = game.getPlayerFromColorOrFaction(factionToTrans);
        if (p2 == null) {
            return;
        }

        switch (thingToTrans) {
            case "TGs" -> {
                String message = "Click the amount of TGs you would like to send";
                for (int x = 1; x < p1.getTg() + 1; x++) {
                    Button transact = Buttons.green(finChecker + "send_TGs_" + p2.getFaction() + "_" + x, "" + x);
                    stuffToTransButtons.add(transact);
                }
                MessageHelper.sendMessageToChannelWithButtons(event.getChannel(), message, stuffToTransButtons);
            }
            case "Comms" -> {
                String message = "Click the amount of commodities you would like to send";
                for (int x = 1; x < p1.getCommodities() + 1; x++) {
                    Button transact = Buttons.green(finChecker + "send_Comms_" + p2.getFaction() + "_" + x, "" + x);
                    stuffToTransButtons.add(transact);
                }
                MessageHelper.sendMessageToChannelWithButtons(event.getChannel(), message, stuffToTransButtons);
            }
            case "ClearDebt" -> {
                String message = "Click the amount of debt you would like to clear";
                for (int x = 1; x < p1.getDebtTokenCount(p2.getColor()) + 1; x++) {
                    Button transact = Buttons.green(finChecker + "send_ClearDebt_" + p2.getFaction() + "_" + x,
                        "" + x);
                    stuffToTransButtons.add(transact);
                }
                MessageHelper.sendMessageToChannelWithButtons(event.getChannel(), message, stuffToTransButtons);
            }
            case "SendDebt" -> {
                String message = "Click the amount of debt you would like to send";
                for (int x = 1; x < 6; x++) {
                    Button transact = Buttons.green(finChecker + "send_SendDebt_" + p2.getFaction() + "_" + x, "" + x);
                    stuffToTransButtons.add(transact);
                }
                MessageHelper.sendMessageToChannelWithButtons(event.getChannel(), message, stuffToTransButtons);
            }
            case "shipOrders" -> {
                String message = "Click the axis order you would like to send";
                for (String shipOrder : ButtonHelper.getPlayersShipOrders(p1)) {
                    Button transact = Buttons.green(
                        finChecker + "send_shipOrders_" + p2.getFaction() + "_" + shipOrder,
                        Mapper.getRelic(shipOrder).getName());
                    stuffToTransButtons.add(transact);
                }
                MessageHelper.sendMessageToChannelWithButtons(event.getChannel(), message, stuffToTransButtons);
            }
            case "starCharts" -> {
                String message = "Click the star chart you would like to send";
                for (String shipOrder : ButtonHelper.getPlayersStarCharts(p1)) {
                    Button transact = Buttons.green(
                        finChecker + "send_starCharts_" + p2.getFaction() + "_" + shipOrder,
                        Mapper.getRelic(shipOrder).getName());
                    stuffToTransButtons.add(transact);
                }
                MessageHelper.sendMessageToChannelWithButtons(event.getChannel(), message, stuffToTransButtons);
            }
            case "Planets" -> {
                String message = "Click the planet you would like to send";
                MessageHelper.sendMessageToChannelWithButtons(event.getChannel(), message,
                    ButtonHelperFactionSpecific.getTradePlanetsWithHacanMechButtons(p1, p2, game));
            }
            case "AlliancePlanets" -> {
                String message = "Click the planet you would like to send";
                MessageHelper.sendMessageToChannelWithButtons(event.getChannel(), message,
                    ButtonHelper.getTradePlanetsWithAlliancePartnerButtons(p1, p2, game));
            }
            case "ACs" -> {
                String message = p1.getRepresentation()
                    + " Click the GREEN button that indicates the AC you would like to send";
                for (String acShortHand : p1.getActionCards().keySet()) {
                    Button transact = Buttons.green(
                        finChecker + "send_ACs_" + p2.getFaction() + "_" + p1.getActionCards().get(acShortHand),
                        Mapper.getActionCard(acShortHand).getName());
                    stuffToTransButtons.add(transact);
                }
                MessageHelper.sendMessageToChannelWithButtons(p1.getCardsInfoThread(), message, stuffToTransButtons);
            }
            case "PNs" -> {
                PNInfo.sendPromissoryNoteInfo(game, p1, false);
                String message = p1.getRepresentationUnfogged() + " Click the PN you would like to send.";

                for (String pnShortHand : p1.getPromissoryNotes().keySet()) {
                    if (p1.getPromissoryNotesInPlayArea().contains(pnShortHand)
                        || (p2.getAbilities().contains("hubris") && pnShortHand.endsWith("an"))) {
                        continue;
                    }
                    PromissoryNoteModel promissoryNote = Mapper.getPromissoryNote(pnShortHand);
                    Player owner = game.getPNOwner(pnShortHand);
                    Button transact;
                    if (game.isFowMode()) {
                        transact = Buttons.green(
                            finChecker + "send_PNs_" + p2.getFaction() + "_"
                                + p1.getPromissoryNotes().get(pnShortHand),
                            owner.getColor() + " " + promissoryNote.getName());
                    } else {
                        transact = Buttons.green(finChecker + "send_PNs_" + p2.getFaction() + "_" + p1.getPromissoryNotes().get(pnShortHand), promissoryNote.getName()).withEmoji(Emoji.fromFormatted(owner.getFactionEmoji()));
                    }
                    stuffToTransButtons.add(transact);
                }
                MessageHelper.sendMessageToChannelWithButtons(p1.getCardsInfoThread(), message, stuffToTransButtons);
                MessageHelper.sendMessageToChannel(p1.getCardsInfoThread(),
                    "Reminder that, unlike other things, you may only send a person 1 PN in a transaction.");
            }
            case "Frags" -> {
                String message = "Click the amount of fragments you would like to send";

                if (p1.getCrf() > 0) {
                    for (int x = 1; x < p1.getCrf() + 1; x++) {
                        Button transact = Buttons.blue(finChecker + "send_Frags_" + p2.getFaction() + "_CRF" + x,
                            "Cultural Fragments (" + x + ")");
                        stuffToTransButtons.add(transact);
                    }
                }
                if (p1.getIrf() > 0) {
                    for (int x = 1; x < p1.getIrf() + 1; x++) {
                        Button transact = Buttons.green(finChecker + "send_Frags_" + p2.getFaction() + "_IRF" + x,
                            "Industrial Fragments (" + x + ")");
                        stuffToTransButtons.add(transact);
                    }
                }
                if (p1.getHrf() > 0) {
                    for (int x = 1; x < p1.getHrf() + 1; x++) {
                        Button transact = Buttons.red(finChecker + "send_Frags_" + p2.getFaction() + "_HRF" + x,
                            "Hazardous Fragments (" + x + ")");
                        stuffToTransButtons.add(transact);
                    }
                }

                if (p1.getUrf() > 0) {
                    for (int x = 1; x < p1.getUrf() + 1; x++) {
                        Button transact = Buttons.gray(finChecker + "send_Frags_" + p2.getFaction() + "_URF" + x,
                            "Frontier Fragments (" + x + ")");
                        stuffToTransButtons.add(transact);
                    }
                }
                MessageHelper.sendMessageToChannelWithButtons(event.getChannel(), message, stuffToTransButtons);

            }
        }
    }

    public static void resolveSpecificTransButtonPress(Game game, Player p1, String buttonID, ButtonInteractionEvent event, boolean oldWay) {
        String finChecker = "FFCC_" + p1.getFaction() + "_";
        buttonID = buttonID.replace("send_", "");
        List<Button> goAgainButtons = new ArrayList<>();

        String thingToTrans = buttonID.substring(0, buttonID.indexOf("_"));
        buttonID = buttonID.replace(thingToTrans + "_", "");
        String factionToTrans = buttonID.substring(0, buttonID.indexOf("_"));
        String amountToTrans = buttonID.substring(buttonID.indexOf("_") + 1);
        Player p2 = game.getPlayerFromColorOrFaction(factionToTrans);
        String message2 = "";
        String ident = p1.getRepresentation();
        String ident2 = p2.getRepresentation();
        switch (thingToTrans) {
            case "TGs" -> {
                int tgAmount = Integer.parseInt(amountToTrans);
                p1.setTg(p1.getTg() - tgAmount);
                p2.setTg(p2.getTg() + tgAmount);
                CommanderUnlockCheck.checkPlayer(p2, "hacan");
                message2 = ident + " sent " + tgAmount + " TGs to " + ident2;
                if (!p2.hasAbility("binding_debts") && p2.getDebtTokenCount(p1.getColor()) > 0 && oldWay) {
                    int amount = Math.min(tgAmount, p2.getDebtTokenCount(p1.getColor()));
                    ClearDebt.clearDebt(p2, p1, amount);
                    message2 = message2 + "\n" + ident2 + " cleared " + amount + " debt tokens owned by " + ident;
                }
            }
            case "Comms" -> {
                int tgAmount = Integer.parseInt(amountToTrans);
                p1.setCommodities(p1.getCommodities() - tgAmount);
                if (!p1.isPlayerMemberOfAlliance(p2)) {
                    int targetTG = p2.getTg();
                    targetTG += tgAmount;
                    p2.setTg(targetTG);
                } else {
                    int targetTG = p2.getCommodities();
                    targetTG += tgAmount;
                    if (targetTG > p2.getCommoditiesTotal()) {
                        targetTG = p2.getCommoditiesTotal();
                    }
                    p2.setCommodities(targetTG);
                }

                CommanderUnlockCheck.checkPlayer(p2, "hacan");
                ButtonHelperFactionSpecific.resolveDarkPactCheck(game, p1, p2, tgAmount);
                message2 = ident + " sent " + tgAmount + " Commodities to " + ident2;
                if (!p2.hasAbility("binding_debts") && p2.getDebtTokenCount(p1.getColor()) > 0 && oldWay) {
                    int amount = Math.min(tgAmount, p2.getDebtTokenCount(p1.getColor()));
                    ClearDebt.clearDebt(p2, p1, amount);
                    message2 = message2 + "\n" + ident2 + " cleared " + amount + " debt tokens owned by " + ident;
                }

            }
            case "WashComms" -> {
                int oldP1Tg = p1.getTg();
                int oldP2tg = p2.getTg();
                int oldP1Comms = p1.getCommodities();
                int newP1Comms = 0;
                int totalWashPowerP1 = p1.getCommodities() + p1.getTg();
                int totalWashPowerP2 = p2.getCommodities() + p2.getTg();
                if (oldP1Comms > totalWashPowerP2) {
                    newP1Comms = oldP1Comms - totalWashPowerP2;

                }
                int oldP2Comms = p2.getCommodities();
                int newP2Comms = 0;
                if (oldP2Comms > totalWashPowerP1) {
                    newP2Comms = oldP2Comms - totalWashPowerP1;
                }
                p1.setCommodities(newP1Comms);
                p2.setCommodities(newP2Comms);
                p1.setTg(p1.getTg() + (oldP1Comms - newP1Comms));
                p2.setTg(p2.getTg() + (oldP2Comms - newP2Comms));
                CommanderUnlockCheck.checkPlayer(p2, "hacan");
                CommanderUnlockCheck.checkPlayer(p1, "hacan");
                ButtonHelperFactionSpecific.resolveDarkPactCheck(game, p1, p2, oldP1Comms);
                ButtonHelperFactionSpecific.resolveDarkPactCheck(game, p2, p1, oldP2Comms);
                // ButtonHelperAbilities.pillageCheck(p1, game);
                // ButtonHelperAbilities.pillageCheck(p2, game);
                String id1 = p1.getFactionEmojiOrColor();
                String id2 = p2.getFactionEmojiOrColor();
                message2 = ident + " washed their " + (oldP1Comms - newP1Comms) + " Commodities with " + ident2 + "  ("
                    + id1 + " TGs went from (" + oldP1Tg + "->" + p1.getTg() + "))\n" + id2
                    + " washed their " + (oldP2Comms - newP2Comms) + " Commodities with " + id1 + " (" + id2
                    + " TGs went from (" + oldP2tg + "->" + p2.getTg() + "))";
            }
            case "shipOrders" -> {
                message2 = ident + " sent " + Mapper.getRelic(amountToTrans).getName() + " to " + ident2;
                p1.removeRelic(amountToTrans);
                p2.addRelic(amountToTrans);
            }
            case "SendDebt" -> {
                message2 = ident + " sent " + amountToTrans + " debt tokens to " + ident2;
                p2.addDebtTokens(p1.getColor(), Integer.parseInt(amountToTrans));
                CommanderUnlockCheck.checkPlayer(p2, "vaden");
            }
            case "ClearDebt" -> {
                message2 = ident + " cleared " + amountToTrans + " debt tokens of " + ident2;
                p1.removeDebtTokens(p2.getColor(), Integer.parseInt(amountToTrans));
            }
            case "starCharts" -> {
                message2 = ident + " sent " + Mapper.getRelic(amountToTrans).getName() + " to " + ident2;
                p1.removeRelic(amountToTrans);
                p2.addRelic(amountToTrans);
            }
            case "ACs" -> {

                message2 = ident + " sent AC #" + amountToTrans + " to " + ident2;
                int acNum = Integer.parseInt(amountToTrans);
                String acID = null;
                if (!p1.getActionCards().containsValue(acNum)) {
                    MessageHelper.sendMessageToChannel(event.getMessageChannel(), "Could not find that AC, no AC sent");
                    return;
                }
                for (Map.Entry<String, Integer> ac : p1.getActionCards().entrySet()) {
                    if (ac.getValue().equals(acNum)) {
                        acID = ac.getKey();
                    }
                }
                p1.removeActionCard(acNum);
                p2.setActionCard(acID);
                ButtonHelper.checkACLimit(game, event, p2);
                ACInfo.sendActionCardInfo(game, p2);
                ACInfo.sendActionCardInfo(game, p1);
                if (!p1.hasAbility("arbiters") && !p2.hasAbility("arbiters")) {
                    if (game.isFowMode()) {
                        MessageHelper.sendMessageToChannel(p1.getPrivateChannel(), message2);
                        MessageHelper.sendMessageToChannel(p2.getPrivateChannel(), message2);
                    } else {
                        MessageHelper.sendMessageToChannel(game.getMainGameChannel(), message2);
                    }
                    return;
                }
            }
            case "PNs" -> {
                String id = null;
                int pnIndex;
                try {
                    pnIndex = Integer.parseInt(amountToTrans);
                } catch (NumberFormatException e) {
                    if (p1.getPromissoryNotes().containsKey(amountToTrans)) {
                        id = amountToTrans;
                    }
                    MessageHelper.sendMessageToChannel(p1.getCorrectChannel(), "# " + p1.getRepresentation() + " heads up, a PN failed to send. This is likely due to you not having the PN to send. Maybe you already gave it to someone else and forgot?");
                    return;
                }
                if (id == null) {
                    for (Map.Entry<String, Integer> pn : p1.getPromissoryNotes().entrySet()) {
                        if (pn.getValue().equals(pnIndex)) {
                            id = pn.getKey();
                            break;
                        }
                    }
                }
                if (id == null) {
                    MessageHelper.sendMessageToChannel(event.getMessageChannel(), "Could not find that PN, no PN sent");
                    return;
                }
                p1.removePromissoryNote(id);
                p2.setPromissoryNote(id);
                if (id.contains("dspnveld")) {
                    PlayPN.resolvePNPlay(id, p2, game, event);
                }
                boolean sendSftT = false;
                boolean sendAlliance = false;
                String promissoryNoteOwner = Mapper.getPromissoryNote(id).getOwner();
                if ((id.endsWith("_sftt") || id.endsWith("_an")) && !promissoryNoteOwner.equals(p2.getFaction())
                    && !promissoryNoteOwner.equals(p2.getColor())
                    && !p2.isPlayerMemberOfAlliance(game.getPlayerFromColorOrFaction(promissoryNoteOwner))) {
                    p2.setPromissoryNotesInPlayArea(id);
                    if (id.endsWith("_sftt")) {
                        sendSftT = true;
                    } else {
                        sendAlliance = true;
                        if (game.getPNOwner(id).hasLeaderUnlocked("bentorcommander")) {
                            p2.setCommoditiesTotal(p2.getCommoditiesTotal() + 1);
                        }
                    }
                }
                PNInfo.sendPromissoryNoteInfo(game, p1, false);
                CardsInfo.sendVariousAdditionalButtons(game, p1);
                PNInfo.sendPromissoryNoteInfo(game, p2, false);
                CardsInfo.sendVariousAdditionalButtons(game, p2);
                String text = sendSftT ? "**Support for the Throne** " : (sendAlliance ? "**Alliance** " : "");
                message2 = p1.getRepresentation() + " sent " + Emojis.PN + text + "PN to " + ident2;
                Helper.checkEndGame(game, p2);
            }
            case "Frags" -> {

                String fragType = amountToTrans.substring(0, 3).toUpperCase();
                int fragNum = Integer.parseInt(amountToTrans.charAt(3) + "");
                String trait = switch (fragType) {
                    case "CRF" -> "cultural";
                    case "HRF" -> "hazardous";
                    case "IRF" -> "industrial";
                    case "URF" -> "frontier";
                    default -> "";
                };
                new RelicSendFragments().sendFrags(event, p1, p2, trait, fragNum, game);
                message2 = "";
            }
        }
        Button button = Buttons.gray(finChecker + "transactWith_" + p2.getColor(),
            "Send something else to player?");
        Button done = Buttons.gray("finishTransaction_" + p2.getColor(), "Done With This Transaction");

        goAgainButtons.add(button);
        goAgainButtons.add(Buttons.green("demandSomething_" + p2.getColor(), "Expect something in return"));
        goAgainButtons.add(done);
        if (game.isFowMode()) {
            MessageHelper.sendMessageToChannel(p1.getPrivateChannel(), message2);
            if (oldWay) {
                MessageHelper.sendMessageToChannelWithButtons(p1.getPrivateChannel(),
                    ident + " Use Buttons To Complete Transaction", goAgainButtons);
            }
            MessageHelper.sendMessageToChannel(p2.getPrivateChannel(), message2);
        } else {
            TextChannel channel = game.getMainGameChannel();
            if (game.getName().equalsIgnoreCase("pbd1000")) {
                channel = game.getTableTalkChannel();
            }
            if (oldWay || (message2.toLowerCase().contains("alliance") || message2.toLowerCase().contains("support"))) {
                MessageHelper.sendMessageToChannel(channel, message2);
            }
            if (oldWay) {
                MessageHelper.sendMessageToChannelWithButtons(game.getMainGameChannel(),
                    ident + " Use Buttons To Complete Transaction", goAgainButtons);
            }
        }
    }

    public static boolean canTheseTwoTransact(Game game, Player player, Player player2) {
        return player == player2 || !"action".equalsIgnoreCase(game.getPhaseOfGame())
            || player.hasAbility("guild_ships") || player.getPromissoryNotesInPlayArea().contains("convoys")
            || player2.getPromissoryNotesInPlayArea().contains("convoys") || player2.hasAbility("guild_ships")
            || player2.getNeighbouringPlayers().contains(player)
            || player.getNeighbouringPlayers().contains(player2);
    }

    public static void checkTransactionLegality(Game game, Player player, Player player2) {
        StringBuilder sb = new StringBuilder();
        sb.append(player.getRepresentationUnfogged()).append(" this is a friendly reminder that you ");
        if (!canTheseTwoTransact(game, player, player2)) {
            sb.append("are not neighbors with ").append(player2.getRepresentation(false, false));
            MessageHelper.sendMessageToChannel(player.getCardsInfoThread(), sb.toString());
        }
        if (player.hasAbility("policy_the_people_control") && !"action".equalsIgnoreCase(game.getPhaseOfGame())) {
            sb.append("cannot transact during the agenda phase due to the ").append(Emojis.olradin).append("Control policy");
            MessageHelper.sendMessageToChannel(player.getCorrectChannel(), sb.toString());
        }
        if (player2.hasAbility("policy_the_people_control") && !"action".equalsIgnoreCase(game.getPhaseOfGame())) {
            sb.append(player2.getRepresentation(false, false)).append(" cannot transact during the agenda phase due to their ").append(Emojis.olradin).append("Control policy");
            MessageHelper.sendMessageToChannel(player.getCorrectChannel(), sb.toString());
        }
    }

    public static List<Button> getPlayersToTransact(Game game, Player p) {
        List<Button> playerButtons = new ArrayList<>();
        String finChecker = "FFCC_" + p.getFaction() + "_";
        for (Player player : game.getPlayers().values()) {
            if (player.isRealPlayer()) {
                if (player.getFaction().equalsIgnoreCase(p.getFaction())) {
                    continue;
                }
                String faction = player.getFaction();
                if (faction != null && Mapper.isValidFaction(faction)) {
                    Button button;
                    if (!game.isFowMode()) {
                        String label = player.getUserName();
                        if (!canTheseTwoTransact(game, p, player)) {
                            label = player.getUserName() + "(Not Neighbors)";
                        }
                        button = Buttons.gray(finChecker + "transactWith_" + faction, label);

                        String factionEmojiString = player.getFactionEmoji();
                        button = button.withEmoji(Emoji.fromFormatted(factionEmojiString));
                    } else {
                        button = Buttons.gray(finChecker + "transactWith_" + player.getColor(), player.getColor());
                    }
                    playerButtons.add(button);
                }

            }
        }
        return playerButtons;
    }

    public static List<Button> getStuffToTransButtonsNew(Game game, Player player, Player p1, Player p2) {
        List<Button> stuffToTransButtons = new ArrayList<>();
        if (p1.getTg() > 0) {
            Button transact = Buttons.green("newTransact_TGs_" + p1.getFaction() + "_" + p2.getFaction(), "TGs");
            stuffToTransButtons.add(transact);
        }
        if (p1.getDebtTokenCount(p2.getColor()) > 0) {
            Button transact = Buttons.blue("newTransact_ClearDebt_" + p1.getFaction() + "_" + p2.getFaction(),
                "Clear Debt");
            stuffToTransButtons.add(transact);
        }
        stuffToTransButtons
            .add(Buttons.red("newTransact_SendDebt_" + p1.getFaction() + "_" + p2.getFaction(), "Send Debt"));
        if (p1.getCommodities() > 0 && !p1.hasAbility("military_industrial_complex")) {
            Button transact = Buttons.green("newTransact_Comms_" + p1.getFaction() + "_" + p2.getFaction(),
                "Commodities");
            stuffToTransButtons.add(transact);
        }

        if (p1 == player && !game.isFowMode() && (p1.getCommodities() > 0 || p2.getCommodities() > 0)
            && !p1.hasAbility("military_industrial_complex")
            && !p1.getAllianceMembers().contains(p2.getFaction())) {
            Button transact = Buttons.gray("offerToTransact_washComms_" + player.getFaction() + "_" + p2.getFaction() + "_0",
                "Wash Both Players Comms");
            stuffToTransButtons.add(transact);
        }
        if (!ButtonHelper.getPlayersShipOrders(p1).isEmpty()) {
            Button transact = Buttons.gray("newTransact_shipOrders_" + p1.getFaction() + "_" + p2.getFaction(),
                "Axis Orders");
            stuffToTransButtons.add(transact);
        }
        if (ButtonHelper.getNumberOfStarCharts(p1) > 0) {
            Button transact = Buttons.gray("newTransact_starCharts_" + p1.getFaction() + "_" + p2.getFaction(),
                "Star Charts");
            stuffToTransButtons.add(transact);
        }
        if ((p1.hasAbility("arbiters") || p2.hasAbility("arbiters")) && p1.getAc() > 0) {
            Button transact = Buttons.green("newTransact_ACs_" + p1.getFaction() + "_" + p2.getFaction(),
                "Action Cards");
            stuffToTransButtons.add(transact);
        }
        if (p1.getPnCount() > 0) {
            Button transact = Buttons.green("newTransact_PNs_" + p1.getFaction() + "_" + p2.getFaction(),
                "Promissory Notes");
            stuffToTransButtons.add(transact);
        }
        if (!p1.getFragments().isEmpty()) {
            Button transact = Buttons.green("newTransact_Frags_" + p1.getFaction() + "_" + p2.getFaction(),
                "Fragments");
            stuffToTransButtons.add(transact);
        }
        if (!ButtonHelperFactionSpecific.getTradePlanetsWithHacanMechButtons(p1, p2, game).isEmpty()) {
            Button transact = Button
                .success("newTransact_Planets_" + p1.getFaction() + "_" + p2.getFaction(), "Planets").withEmoji(Emoji.fromFormatted(Emojis.getFactionIconFromDiscord("hacan")));
            stuffToTransButtons.add(transact);
        }
        if (!ButtonHelper.getTradePlanetsWithAlliancePartnerButtons(p1, p2, game).isEmpty()) {
            Button transact = Buttons.green("newTransact_AlliancePlanets_" + p1.getFaction() + "_" + p2.getFaction(), "Alliance Planets").withEmoji(Emoji.fromFormatted(Emojis.getFactionIconFromDiscord(p2.getFaction())));
            stuffToTransButtons.add(transact);
        }
        if ((game.getPhaseOfGame().toLowerCase().contains("agenda") || game.getPhaseOfGame().toLowerCase().contains("strategy"))
            && !"no".equalsIgnoreCase(ButtonHelper.playerHasDMZPlanet(p1, game))) {
            Button transact = Buttons.gray(
                "offerToTransact_dmz_" + p1.getFaction() + "_" + p2.getFaction() + "_"
                    + ButtonHelper.playerHasDMZPlanet(p1, game),
                "Trade " + Mapper.getPlanet(ButtonHelper.playerHasDMZPlanet(p1, game)).getName() + " (DMZ)");
            stuffToTransButtons.add(transact);
        }

        if (player == p1) {
            stuffToTransButtons.add(Buttons.gray("resetOffer_" + p2.getFaction(), "Reset Offer"));
            stuffToTransButtons.add(
                Buttons.red("getNewTransaction_" + p2.getFaction() + "_" + p1.getFaction(), "Ask for Stuff"));
            stuffToTransButtons.add(Buttons.gray("sendOffer_" + p2.getFaction(), "Send the Offer"));
        } else {
            stuffToTransButtons.add(Buttons.gray("resetOffer_" + p1.getFaction(), "Reset Offer"));
            stuffToTransButtons
                .add(Buttons.red("getNewTransaction_" + p2.getFaction() + "_" + p1.getFaction(), "Offer More"));
            stuffToTransButtons.add(Buttons.gray("sendOffer_" + p1.getFaction(), "Send the Offer"));
        }

        return stuffToTransButtons;
    }

    @ButtonHandler("send_")
    public static void send(ButtonInteractionEvent event, Player player, String buttonID, Game game) {
        TransactionHelper.resolveSpecificTransButtonPress(game, player, buttonID, event, true);
        ButtonHelper.deleteMessage(event);
    }

    public static List<Button> getStuffToTransButtonsOld(Game game, Player p1, Player p2) {
        String finChecker = "FFCC_" + p1.getFaction() + "_";
        List<Button> stuffToTransButtons = new ArrayList<>();
        if (p1.getTg() > 0) {
            Button transact = Buttons.green(finChecker + "transact_TGs_" + p2.getFaction(), "TGs");
            stuffToTransButtons.add(transact);
        }
        if (p1.getDebtTokenCount(p2.getColor()) > 0) {
            Button transact = Buttons.blue(finChecker + "transact_ClearDebt_" + p2.getFaction(), "Clear Debt");
            stuffToTransButtons.add(transact);
        }
        stuffToTransButtons.add(Buttons.red(finChecker + "transact_SendDebt_" + p2.getFaction(), "Send Debt"));
        if (p1.getCommodities() > 0 && !p1.hasAbility("military_industrial_complex")) {
            Button transact = Buttons.green(finChecker + "transact_Comms_" + p2.getFaction(), "Commodities");
            stuffToTransButtons.add(transact);
        }

        if (!game.isFowMode() && (p1.getCommodities() > 0 || p2.getCommodities() > 0)
            && !p1.hasAbility("military_industrial_complex")
            && !p1.getAllianceMembers().contains(p2.getFaction())) {
            Button transact = Buttons.gray(finChecker + "send_WashComms_" + p2.getFaction() + "_0",
                "Wash Both Players Comms");
            stuffToTransButtons.add(transact);
        }
        if (!ButtonHelper.getPlayersShipOrders(p1).isEmpty()) {
            Button transact = Buttons.gray(finChecker + "transact_shipOrders_" + p2.getFaction(), "Axis Orders");
            stuffToTransButtons.add(transact);
        }
        if (ButtonHelper.getNumberOfStarCharts(p1) > 0) {
            Button transact = Buttons.gray(finChecker + "transact_starCharts_" + p2.getFaction(), "Star Charts");
            stuffToTransButtons.add(transact);
        }
        if ((p1.hasAbility("arbiters") || p2.hasAbility("arbiters")) && p1.getAc() > 0) {
            Button transact = Buttons.green(finChecker + "transact_ACs_" + p2.getFaction(), "Action Cards");
            stuffToTransButtons.add(transact);
        }
        if (p1.getPnCount() > 0) {
            Button transact = Buttons.green(finChecker + "transact_PNs_" + p2.getFaction(), "Promissory Notes");
            stuffToTransButtons.add(transact);
        }
        if (!p1.getFragments().isEmpty()) {
            Button transact = Buttons.green(finChecker + "transact_Frags_" + p2.getFaction(), "Fragments");
            stuffToTransButtons.add(transact);
        }
        if (!ButtonHelperFactionSpecific.getTradePlanetsWithHacanMechButtons(p1, p2, game).isEmpty()) {
            Button transact = Buttons.green(finChecker + "transact_Planets_" + p2.getFaction(), "Planets").withEmoji(Emoji.fromFormatted(Emojis.getFactionIconFromDiscord("hacan")));
            stuffToTransButtons.add(transact);
        }
        if (!ButtonHelper.getTradePlanetsWithAlliancePartnerButtons(p1, p2, game).isEmpty()) {
            Button transact = Button
                .success(finChecker + "transact_AlliancePlanets_" + p2.getFaction(), "Alliance Planets").withEmoji(Emoji.fromFormatted(Emojis.getFactionIconFromDiscord(p2.getFaction())));
            stuffToTransButtons.add(transact);
        }
        if (game.getPhaseOfGame().toLowerCase().contains("agenda")
            && !"no".equalsIgnoreCase(ButtonHelper.playerHasDMZPlanet(p1, game))) {
            Button transact = Buttons.gray(
                finChecker + "resolveDMZTrade_" + ButtonHelper.playerHasDMZPlanet(p1, game) + "_" + p2.getFaction(),
                "Trade " + Mapper.getPlanet(ButtonHelper.playerHasDMZPlanet(p1, game)).getName() + " (DMZ)");
            stuffToTransButtons.add(transact);
        }
        return stuffToTransButtons;
    }

    @ButtonHandler("rescindOffer_")
    public static void rescindOffer(ButtonInteractionEvent event, Player player, String buttonID, Game game) {
        Player p2 = game.getPlayerFromColorOrFaction(buttonID.split("_")[1]);
        if (p2 != null) {
            MessageHelper.sendMessageToChannel(p2.getCardsInfoThread(), p2.getRepresentation() + " the latest offer from " + player.getRepresentation(false, false) + " has been rescinded.");
            MessageHelper.sendMessageToChannel(player.getCardsInfoThread(), player.getRepresentation() + "you rescinded the latest offer to " + p2.getRepresentation(false, false));
            player.clearTransactionItemsWithPlayer(p2);
            ButtonHelper.deleteMessage(event);
        }
    }

    @ButtonHandler("rejectOffer_")
    public static void rejectOffer(ButtonInteractionEvent event, Player player, String buttonID, Game game) {
        Player p1 = game.getPlayerFromColorOrFaction(buttonID.split("_")[1]);
        if (p1 != null) {
            MessageHelper.sendMessageToChannel(p1.getCardsInfoThread(), p1.getRepresentation() + " your offer to " + player.getRepresentation(false, false) + " has been rejected.");
            ButtonHelper.deleteMessage(event);
        }
    }

    @ButtonHandler("acceptOffer_")
    public static void acceptOffer(ButtonInteractionEvent event, Game game, Player player, String buttonID) {
        Player p1 = game.getPlayerFromColorOrFaction(buttonID.split("_")[1]);
        if (buttonID.split("_").length > 2) {
            String offerNum = buttonID.split("_")[2];
            String key = "offerFrom" + p1.getFaction() + "To" + player.getFaction();
            String oldOffer = game.getStoredValue(key);
            if (!offerNum.equalsIgnoreCase(oldOffer)) {
                MessageHelper.sendMessageToChannel(event.getChannel(),
                    "Only the most recent offer is acceptable. This is an old transaction offer and it can no longer be accepted");
                return;
            }
        }
        TransactionHelper.acceptTransactionOffer(p1, player, game, event);
        ButtonHelper.deleteMessage(event);
    }

    @ButtonHandler("finishTransaction_")
    public static void finishTransaction(ButtonInteractionEvent event, Player player, String buttonID, Game game) {
        String player2Color = buttonID.split("_")[1];
        Player player2 = game.getPlayerFromColorOrFaction(player2Color);
        ButtonHelperAbilities.pillageCheck(player, game);
        ButtonHelperAbilities.pillageCheck(player2, game);
        ButtonHelper.deleteMessage(event);
    }

    @ButtonHandler("demandSomething_")
    public static void demandSomething(ButtonInteractionEvent event, Player player, String buttonID, Game game) {
        String player2Color = buttonID.split("_")[1];
        Player p2 = game.getPlayerFromColorOrFaction(player2Color);
        if (p2 != null) {
            List<Button> buttons = TransactionHelper.getStuffToTransButtonsOld(game, p2, player);
            String message = p2.getRepresentation()
                + " you have been given something on the condition that you give something in return. Hopefully the player explained what. If you don't hand it over, please return what they sent. Use buttons to send something to "
                + player.getFactionEmojiOrColor();
            MessageHelper.sendMessageToChannelWithButtons(p2.getCorrectChannel(), message, buttons);
            ButtonHelper.deleteMessage(event);
        }
    }

    @ButtonHandler("transactWith_")
    @ButtonHandler("resetOffer_")
    public static void transactWith(ButtonInteractionEvent event, Player player, String buttonID, Game game) {
        String faction = buttonID.split("_")[1];
        Player p2 = game.getPlayerFromColorOrFaction(faction);
        if (p2 != null) {
            player.clearTransactionItemsWithPlayer(p2);
            List<Button> buttons = TransactionHelper.getStuffToTransButtonsOld(game, player, p2);
            if (!game.isFowMode() && game.isNewTransactionMethod()) {
                buttons = TransactionHelper.getStuffToTransButtonsNew(game, player, player, p2);
            }
            String message = player.getRepresentation(true, false) + " Use the buttons to select what you want to transact with " + p2.getRepresentation(false, false);
            MessageHelper.sendMessageToChannelWithButtons(event.getMessageChannel(), message, buttons);
            TransactionHelper.checkTransactionLegality(game, player, p2);
            ButtonHelper.deleteMessage(event);
        }
    }
}
