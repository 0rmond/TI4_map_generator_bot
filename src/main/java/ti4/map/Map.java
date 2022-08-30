package ti4.map;


import ti4.generator.Mapper;
import ti4.helpers.Constants;
import ti4.helpers.DisplayType;
import ti4.helpers.Helper;

import javax.annotation.CheckForNull;
import java.awt.*;
import java.util.List;
import java.util.*;

public class Map {

    private String ownerID;
    private String ownerName = "";
    private String name;

    private HashMap<String, UnitHolder> planets = new HashMap<>();

    @CheckForNull
    private DisplayType displayTypeForced = null;
    private int playerCountForMap = 6;
    private int vp = 10;
    private boolean communityMode = false;
    private boolean allianceMode = false;

    //UserID, UserName
    private LinkedHashMap<String, Player> players = new LinkedHashMap<>();
    private MapStatus mapStatus = MapStatus.open;

    private HashMap<Integer, Boolean> scPlayed = new HashMap<>();
    private String speaker = "";
    private String creationDate;
    private String customName = "";
    private long lastModifiedDate;
    private int round = 1;

    private List<String> secretObjectives;
    private List<String> actionCards;
    private LinkedHashMap<String, Integer> discardActionCards = new LinkedHashMap<>();

    private List<String> agendas;
    private LinkedHashMap<Integer, Integer> scTradeGoods = new LinkedHashMap<>();
    private LinkedHashMap<String, Integer> discardAgendas = new LinkedHashMap<>();
    private LinkedHashMap<String, Integer> sentAgendas = new LinkedHashMap<>();
    private LinkedHashMap<String, Integer> laws = new LinkedHashMap<>();
    private LinkedHashMap<String, String> lawsInfo = new LinkedHashMap<>();
    private LinkedHashMap<String, Integer> revealedPublicObjectives = new LinkedHashMap<>();
    private LinkedHashMap<String, Integer> customPublicVP = new LinkedHashMap<>();
    private LinkedHashMap<String, List<String>> scoredPublicObjectives = new LinkedHashMap<>();
    private ArrayList<String> publicObjectives1 = new ArrayList<>();
    private ArrayList<String> publicObjectives2 = new ArrayList<>();
    private ArrayList<String> soToPoList = new ArrayList<>();
    private ArrayList<String> purgedPN = new ArrayList<>();

    private ArrayList<String> explore = new ArrayList<>();
    private ArrayList<String> discardExplore = new ArrayList<>();
    private ArrayList<String> relics = new ArrayList<>();

    public Map() {
        creationDate = Helper.getDateRepresentation(new Date().getTime());
        lastModifiedDate = new Date().getTime();

        HashMap<String, String> secretObjectives = Mapper.getSecretObjectives();
        this.secretObjectives = new ArrayList<>(secretObjectives.keySet());
        Collections.shuffle(this.secretObjectives);

        HashMap<String, String> actionCards = Mapper.getActionCards();
        this.actionCards = new ArrayList<>(actionCards.keySet());
        Collections.shuffle(this.actionCards);

        HashMap<String, String> agendas = Mapper.getAgendas();
        this.agendas = new ArrayList<>(agendas.keySet());
        Collections.shuffle(this.agendas);

        Set<String> po1 = Mapper.getPublicObjectivesState1().keySet();
        Set<String> po2 = Mapper.getPublicObjectivesState2().keySet();
        publicObjectives1.addAll(po1);
        publicObjectives2.addAll(po2);
        Collections.shuffle(publicObjectives1);
        Collections.shuffle(publicObjectives2);
        addCustomPO(Constants.CUSTODIAN, 1);

        Set<String> exp = Mapper.getExplores().keySet();
        Set<String> rel = Mapper.getRelics().keySet();
        explore.addAll(exp);
        relics.addAll(rel);
        Collections.shuffle(explore);
        Collections.shuffle(relics);

        //Default SC initialization
        for (int i = 0; i < 8; i++) {
            scTradeGoods.put(i + 1, 0);
        }
    }

    public void setPurgedPN(String purgedPN) {
        this.purgedPN.add(purgedPN);
    }

    public void setPurgedPNs(ArrayList<String> purgedPN) {
        this.purgedPN = purgedPN;
    }

    public ArrayList<String> getPurgedPN() {
        return purgedPN;
    }

    public int getVp() {
        return vp;
    }

    public void setVp(int vp) {
        this.vp = vp;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        if (round <= 0) {
            this.round = 1;
        } else {
            this.round = round;
        }
    }

    public String getCreationDate() {
        return creationDate;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public boolean isCommunityMode() {
        return communityMode;
    }

    public boolean isAllianceMode() {
        return allianceMode;
    }

    public void setAllianceMode(boolean allianceMode) {
        this.allianceMode = allianceMode;
    }

    public void setCommunityMode(boolean communityMode) {
        this.communityMode = communityMode;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public long getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(long lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    //Position, Tile
    private HashMap<String, Tile> tileMap = new HashMap<>();

    public HashMap<Integer, Boolean> getScPlayed() {
        return scPlayed;
    }

    public DisplayType getDisplayTypeForced() {
        return displayTypeForced;
    }

    public void setDisplayTypeForced(DisplayType displayTypeForced) {
        this.displayTypeForced = displayTypeForced;
    }

    public int getPlayerCountForMap() {
        return playerCountForMap;
    }

    public void setPlayerCountForMap(int playerCountForMap) {
        this.playerCountForMap = playerCountForMap;
    }

    public void setSCPlayed(Integer scNumber, Boolean playedStatus) {
        this.scPlayed.put(scNumber, playedStatus);
    }

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public void setSentAgenda(String id) {
        Collection<Integer> values = sentAgendas.values();
        int identifier = new Random().nextInt(1000);
        while (values.contains(identifier)) {
            identifier = new Random().nextInt(1000);
        }
        sentAgendas.put(id, identifier);
    }

    public void addDiscardAgenda(String id) {
        Collection<Integer> values = discardAgendas.values();
        int identifier = new Random().nextInt(1000);
        while (values.contains(identifier)) {
            identifier = new Random().nextInt(1000);
        }
        discardAgendas.put(id, identifier);
    }

    public void addRevealedPublicObjective(String id) {
        Collection<Integer> values = revealedPublicObjectives.values();
        int identifier = 0;
        while (values.contains(identifier)) {
            identifier++;
        }
        revealedPublicObjectives.put(id, identifier);
    }

    public LinkedHashMap<Integer, Integer> getScTradeGoods() {
        return scTradeGoods;
    }

    public void setScTradeGoods(LinkedHashMap<Integer, Integer> scTradeGoods) {
        this.scTradeGoods = scTradeGoods;
    }

    public void setScTradeGood(Integer sc, Integer tradeGoodCount) {
        scTradeGoods.put(sc, tradeGoodCount);
    }

    public void setScPlayed(HashMap<Integer, Boolean> scPlayed) {
        this.scPlayed = scPlayed;
    }

    public LinkedHashMap<String, Integer> getRevealedPublicObjectives() {
        return revealedPublicObjectives;
    }

    public ArrayList<String> getPublicObjectives1() {
        return publicObjectives1;
    }

    public ArrayList<String> getPublicObjectives2() {
        return publicObjectives2;
    }

    public java.util.Map.Entry<String, Integer> revealState1() {
        return revealObjective(publicObjectives1);
    }

    public java.util.Map.Entry<String, Integer> revealState2() {
        return revealObjective(publicObjectives2);
    }

    public java.util.Map.Entry<String, Integer> revealObjective(ArrayList<String> objectiveList) {
        if (!objectiveList.isEmpty()) {
            String id = objectiveList.get(0);
            objectiveList.remove(id);
            addRevealedPublicObjective(id);
            for (java.util.Map.Entry<String, Integer> entry : revealedPublicObjectives.entrySet()) {
                if (entry.getKey().equals(id)) {
                    return entry;
                }
            }
        }
        return null;
    }

    public boolean shuffleObjectiveBackIntoDeck(Integer idNumber) {
        String id = "";
        for (java.util.Map.Entry<String, Integer> po : revealedPublicObjectives.entrySet()) {
            if (po.getValue().equals(idNumber)) {
                id = po.getKey();
                break;
            }
        }
        if (!id.isEmpty()) {
            revealedPublicObjectives.remove(id);
            Set<String> po1 = Mapper.getPublicObjectivesState1().keySet();
            Set<String> po2 = Mapper.getPublicObjectivesState2().keySet();
            if (po1.contains(id)) {
                publicObjectives1.add(id);
                Collections.shuffle(publicObjectives1);
            } else if (po2.contains(id)) {
                publicObjectives2.add(id);
                Collections.shuffle(publicObjectives2);
            }
            return true;
        }
        return false;
    }

    public boolean scorePublicObjective(String userID, Integer idNumber) {
        String id = "";
        for (java.util.Map.Entry<String, Integer> po : revealedPublicObjectives.entrySet()) {
            if (po.getValue().equals(idNumber)) {
                id = po.getKey();
                break;
            }
        }
        if (!id.isEmpty()) {
            List<String> scoredPlayerList = scoredPublicObjectives.computeIfAbsent(id, key -> new ArrayList<>());
            if (!Constants.CUSTODIAN.equals(id) && scoredPlayerList.contains(userID)) {
                return false;
            }
            scoredPlayerList.add(userID);
            scoredPublicObjectives.put(id, scoredPlayerList);
            return true;
        }
        return false;
    }

    public boolean unscorePublicObjective(String userID, Integer idNumber) {
        String id = "";
        for (java.util.Map.Entry<String, Integer> po : revealedPublicObjectives.entrySet()) {
            if (po.getValue().equals(idNumber)) {
                id = po.getKey();
                break;
            }
        }
        if (!id.isEmpty()) {
            List<String> scoredPlayerList = scoredPublicObjectives.computeIfAbsent(id, key -> new ArrayList<>());
            if (scoredPlayerList.contains(userID)) {
                scoredPlayerList.remove(userID);
                scoredPublicObjectives.put(id, scoredPlayerList);
                return true;
            }
        }
        return false;
    }

    public Integer addCustomPO(String poName, int vp) {
        customPublicVP.put(poName, vp);
        addRevealedPublicObjective(poName);
        return revealedPublicObjectives.get(poName);
    }

    public boolean removeCustomPO(Integer idNumber) {

        String id = "";
        for (java.util.Map.Entry<String, Integer> po : revealedPublicObjectives.entrySet()) {
            if (po.getValue().equals(idNumber)) {
                id = po.getKey();
                break;
            }
        }
        if (!id.isEmpty()) {
            revealedPublicObjectives.remove(id);
            soToPoList.remove(id);
            customPublicVP.remove(id);
            scoredPublicObjectives.remove(id);
            return true;
        }
        return false;
    }

    public boolean removeSOFromGame(String soID) {
        return secretObjectives.remove(soID);
    }

    public boolean removeAgendaFromGame(String soID) {
        return agendas.remove(soID);
    }

    public LinkedHashMap<String, Integer> getCustomPublicVP() {
        return customPublicVP;
    }

    public void setCustomPublicVP(LinkedHashMap<String, Integer> customPublicVP) {
        this.customPublicVP = customPublicVP;
    }

    public void setRevealedPublicObjectives(LinkedHashMap<String, Integer> revealedPublicObjectives) {
        this.revealedPublicObjectives = revealedPublicObjectives;
    }

    public void setScoredPublicObjectives(LinkedHashMap<String, List<String>> scoredPublicObjectives) {
        this.scoredPublicObjectives = scoredPublicObjectives;
    }

    public void setPublicObjectives1(ArrayList<String> publicObjectives1) {
        this.publicObjectives1 = publicObjectives1;
    }

    public void setPublicObjectives2(ArrayList<String> publicObjectives2) {
        this.publicObjectives2 = publicObjectives2;
    }

    public ArrayList<String> getSoToPoList() {
        return soToPoList;
    }

    public void setSoToPoList(ArrayList<String> soToPoList) {
        this.soToPoList = soToPoList;
    }

    public void addToSoToPoList(String id) {
        soToPoList.add(id);
    }

    public void removeFromSoToPoList(String id) {
        soToPoList.remove(id);
    }



    public LinkedHashMap<String, List<String>> getScoredPublicObjectives() {
        return scoredPublicObjectives;
    }

    public LinkedHashMap<String, Integer> getLaws() {
        return laws;
    }

    public LinkedHashMap<String, String> getLawsInfo() {
        return lawsInfo;
    }

    public void setAgendas(List<String> agendas) {
        this.agendas = agendas;
    }

    public void shuffleAgendas() {
        Collections.shuffle(agendas);
    }

    public void resetAgendas() {
        HashMap<String, String> agendas = Mapper.getAgendas();
        this.agendas = new ArrayList<>(agendas.keySet());
        Collections.shuffle(this.agendas);
        discardAgendas = new LinkedHashMap<>();
    }

    public void setDiscardAgendas(LinkedHashMap<String, Integer> discardAgendas) {
        this.discardAgendas = discardAgendas;
    }

    public void setSentAgendas(LinkedHashMap<String, Integer> sentAgendas) {
        this.sentAgendas = sentAgendas;
    }

    public void setLaws(LinkedHashMap<String, Integer> laws) {
        this.laws = laws;
    }

    public void setLawsInfo(LinkedHashMap<String, String> lawsInfo) {
        this.lawsInfo = lawsInfo;
    }

    public List<String> getAgendas() {
        return agendas;
    }

    public LinkedHashMap<String, Integer> getSentAgendas() {
        return sentAgendas;
    }

    public LinkedHashMap<String, Integer> getDiscardAgendas() {
        return discardAgendas;
    }

    public boolean addLaw(Integer idNumber, String optionalText) {
        String id = "";
        for (java.util.Map.Entry<String, Integer> agendas : discardAgendas.entrySet()) {
            if (agendas.getValue().equals(idNumber)) {
                id = agendas.getKey();
                break;
            }
        }
        if (!id.isEmpty()) {

            Collection<Integer> values = laws.values();
            int identifier = new Random().nextInt(1000);
            while (values.contains(identifier)) {
                identifier = new Random().nextInt(1000);
            }
            discardAgendas.remove(id);
            laws.put(id, identifier);
            if (optionalText != null) {
                lawsInfo.put(id, optionalText);
            }
            return true;
        }
        return false;
    }

    public boolean removeLaw(Integer idNumber) {
        String id = "";
        for (java.util.Map.Entry<String, Integer> ac : laws.entrySet()) {
            if (ac.getValue().equals(idNumber)) {
                id = ac.getKey();
                break;
            }
        }
        if (!id.isEmpty()) {
            laws.remove(id);
            lawsInfo.remove(id);
            addDiscardAgenda(id);
            return true;
        }
        return false;
    }

    public boolean putAgendaTop(Integer idNumber) {
        if (sentAgendas.containsValue(idNumber)) {

            String id = "";
            for (java.util.Map.Entry<String, Integer> ac : sentAgendas.entrySet()) {
                if (ac.getValue().equals(idNumber)) {
                    id = ac.getKey();
                    break;
                }
            }
            if (!id.isEmpty()) {
                agendas.remove(id);
                agendas.add(0, id);
                sentAgendas.remove(id);
                return true;
            }
        }
        return false;
    }

    public boolean putAgendaBottom(Integer idNumber) {
        if (sentAgendas.containsValue(idNumber)) {
            String id = "";
            for (java.util.Map.Entry<String, Integer> ac : sentAgendas.entrySet()) {
                if (ac.getValue().equals(idNumber)) {
                    id = ac.getKey();
                    break;
                }
            }
            if (!id.isEmpty()) {
                agendas.remove(id);
                agendas.add(id);
                sentAgendas.remove(id);
                return true;
            }
        }
        return false;
    }

    @CheckForNull
    public java.util.Map.Entry<String, Integer> drawAgenda() {
        if (!agendas.isEmpty()) {
            for (String id : agendas) {
                if (!sentAgendas.containsKey(id)) {
                    setSentAgenda(id);
                    for (java.util.Map.Entry<String, Integer> entry : sentAgendas.entrySet()) {
                        if (entry.getKey().equals(id)) {
                            return entry;
                        }
                    }
                }
            }
        }
        return null;
    }

    public String lookAtTopAgenda() {
        return agendas.get(0);
    }

    public String lookAtBottomAgenda() {
        return agendas.get(agendas.size() - 1);
    }

    public String revealAgenda() {
        String id = agendas.remove(0);
        addDiscardAgenda(id);
        return id;
    }

    @CheckForNull
    public LinkedHashMap<String, Integer> drawActionCard(String userID) {
        if (!actionCards.isEmpty()) {
            String id = actionCards.get(0);
            Player player = getPlayer(userID);
            if (player != null) {
                actionCards.remove(id);
                player.setActionCard(id);
                return player.getActionCards();
            }
        } else {
            actionCards.addAll(discardActionCards.keySet());
            discardActionCards.clear();
            Collections.shuffle(actionCards);
            return drawActionCard(userID);
        }
        return null;
    }

    private ArrayList<String> getExplores(String reqType, List<String> superDeck) {
        ArrayList<String> deck = new ArrayList<String>();
        for (String id : superDeck) {
            String card = Mapper.getExplore(id);
            if (card != null) {
                String[] split = card.split(";");
                String type = split[1];
                if (reqType.equalsIgnoreCase(type)) {
                    deck.add(id);
                }
            }
        }
        return deck;
    }

    public ArrayList<String> getExploreDeck(String reqType) {
        return getExplores(reqType, explore);
    }

    public ArrayList<String> getExploreDiscard(String reqType) {
        return getExplores(reqType, discardExplore);
    }

    public String drawExplore(String reqType) {
        List<String> deck = getExplores(reqType, explore);
        if (!deck.isEmpty()) {
            Collections.shuffle(deck);
            String id = deck.get(0);
            discardExplore(id);
            return id;
        } else {
            deck = getExplores(reqType, discardExplore);
            if (!deck.isEmpty()) {
                explore.addAll(deck);
                Collections.shuffle(explore);
                discardExplore.removeAll(deck);
                return drawExplore(reqType);
            }
        }
        return null;
    }

    public void discardExplore(String id) {
        explore.remove(id);
        if (Mapper.getExplore(id) != null) {
            discardExplore.add(id);
        }
    }

    public void purgeExplore(String id) {
        explore.remove(id);
        discardExplore.remove(id);
    }

    public void addExplore(String id) {
        if (Mapper.getExplore(id) != null) {
            explore.add(id);
        }
        discardExplore.remove(id);
    }

    public void resetExplore() {
        explore.clear();
        discardExplore.clear();
        Set<String> exp = Mapper.getExplores().keySet();
        explore.addAll(exp);
    }

    public String drawRelic() {
        ArrayList<String> relics_ = new ArrayList<>(relics);
        relics_.remove(Constants.ENIGMATIC_DEVICE);
        if (relics_.isEmpty()) {
            return "";
        }
        Collections.shuffle(relics_);
        String remove = relics_.remove(0);
        relics.remove(remove);
        return remove;
    }

    public boolean shuffleRelicBack(String relicID) {
        if (!relics.contains(relicID)){
            relics.add(relicID);
            Collections.shuffle(relics);
            return true;
        }
        return false;
    }

    @CheckForNull
    public String drawActionCardAndDiscard() {
        if (!actionCards.isEmpty()) {
            String id = actionCards.get(0);
            actionCards.remove(id);
            setDiscardActionCard(id);
            return id;
        } else {
            actionCards.addAll(discardActionCards.keySet());
            discardActionCards.clear();
            Collections.shuffle(actionCards);
            return drawActionCardAndDiscard();
        }
    }

    @CheckForNull
    public LinkedHashMap<String, Integer> drawSecretObjective(String userID) {
        if (!secretObjectives.isEmpty()) {
            String id = secretObjectives.get(0);
            Player player = getPlayer(userID);
            if (player != null) {
                secretObjectives.remove(id);
                player.setSecret(id);
                return player.getSecrets();
            }
        }
        return null;
    }

    private void setDiscardActionCard(String id) {
        Collection<Integer> values = discardActionCards.values();
        int identifier = new Random().nextInt(1000);
        while (values.contains(identifier)) {
            identifier = new Random().nextInt(1000);
        }
        discardActionCards.put(id, identifier);
    }

    public boolean discardActionCard(String userID, Integer acIDNumber) {
        Player player = getPlayer(userID);
        if (player != null) {
            LinkedHashMap<String, Integer> actionCards = player.getActionCards();
            String acID = "";
            for (java.util.Map.Entry<String, Integer> ac : actionCards.entrySet()) {
                if (ac.getValue().equals(acIDNumber)) {
                    acID = ac.getKey();
                    break;
                }
            }
            if (!acID.isEmpty()) {
                player.removeActionCard(acIDNumber);
                setDiscardActionCard(acID);
                return true;
            }
        }
        return false;
    }

    public void shuffleActionCards() {
        Collections.shuffle(actionCards);
    }

    public LinkedHashMap<String, Integer> getDiscardActionCards() {
        return discardActionCards;
    }

    public boolean pickActionCard(String userID, Integer acIDNumber) {
        Player player = getPlayer(userID);
        if (player != null) {
            String acID = "";
            for (java.util.Map.Entry<String, Integer> ac : discardActionCards.entrySet()) {
                if (ac.getValue().equals(acIDNumber)) {
                    acID = ac.getKey();
                    break;
                }
            }
            if (!acID.isEmpty()) {
                discardActionCards.remove(acID);
                player.setActionCard(acID);
                return true;
            }
        }
        return false;
    }

    public boolean shuffleActionCardBackIntoDeck(Integer acIDNumber) {
        String acID = "";
        for (java.util.Map.Entry<String, Integer> ac : discardActionCards.entrySet()) {
            if (ac.getValue().equals(acIDNumber)) {
                acID = ac.getKey();
                break;
            }
        }
        if (!acID.isEmpty()) {
            discardActionCards.remove(acID);
            actionCards.add(acID);
            Collections.shuffle(actionCards);
            return true;

        }
        return false;
    }

    public boolean scoreSecretObjective(String userID, Integer soIDNumber, ti4.map.Map map) {
        Player player = getPlayer(userID);
        if (player != null) {
            LinkedHashMap<String, Integer> secrets = player.getSecrets();
            String soID = "";
            for (java.util.Map.Entry<String, Integer> so : secrets.entrySet()) {
                if (so.getValue().equals(soIDNumber)) {
                    soID = so.getKey();
                    break;
                }
            }
            if (!soID.isEmpty()) {
                player.removeSecret(soIDNumber);
                player.setSecretScored(soID, map);
                return true;
            }
        }
        return false;
    }

    public boolean unscoreSecretObjective(String userID, Integer soIDNumber) {
        Player player = getPlayer(userID);
        if (player != null) {
            LinkedHashMap<String, Integer> secrets = player.getSecretsScored();
            String soID = "";
            for (java.util.Map.Entry<String, Integer> so : secrets.entrySet()) {
                if (so.getValue().equals(soIDNumber)) {
                    soID = so.getKey();
                    break;
                }
            }
            if (!soID.isEmpty()) {
                player.removeSecretScored(soIDNumber);
                player.setSecret(soID);
                return true;
            }
        }
        return false;
    }

    public boolean discardSecretObjective(String userID, Integer soIDNumber) {
        Player player = getPlayer(userID);
        if (player != null) {
            LinkedHashMap<String, Integer> secrets = player.getSecrets();
            String soID = "";
            for (java.util.Map.Entry<String, Integer> so : secrets.entrySet()) {
                if (so.getValue().equals(soIDNumber)) {
                    soID = so.getKey();
                    break;
                }
            }
            if (!soID.isEmpty()) {
                player.removeSecret(soIDNumber);
                secretObjectives.add(soID);
                Collections.shuffle(secretObjectives);
                return true;
            }
        }
        return false;
    }

    @CheckForNull
    public LinkedHashMap<String, Integer> getSecretObjective(String userID) {
        Player player = getPlayer(userID);
        if (player != null) {
            return player.getSecrets();
        }
        return null;
    }

    @CheckForNull
    public LinkedHashMap<String, Integer> getScoredSecretObjective(String userID) {
        Player player = getPlayer(userID);
        if (player != null) {
            return player.getSecretsScored();
        }
        return null;
    }

    public void addSecretObjective(String id) {
        if (!secretObjectives.contains(id)) {
            secretObjectives.add(id);
            Collections.shuffle(this.secretObjectives);
        }
    }

    public List<String> getSecretObjectives() {
        return secretObjectives;
    }

    public List<String> getActionCards() {
        return actionCards;
    }

    public List<String> getAllExplores() {
        return explore;
    }

    public List<String> getAllExploreDiscard() {
        return discardExplore;
    }

    public void setExploreDeck(ArrayList<String> deck) {
        explore = deck;
    }

    public void setExploreDiscard(ArrayList<String> discard) {
        discardExplore = discard;
    }

    public String pickExplore(String id) {
        if (explore.contains(id)) {
            discardExplore(id);
            return id;
        } else if (discardExplore.contains(id)) {
            return id;
        }
        return null;
    }

    public List<String> getAllRelics() {
        return relics;
    }

    public void setRelics(ArrayList<String> deck) {
        deck = new ArrayList<>(new HashSet<>(deck));
        relics = deck;
    }

    public void setSecretObjectives(List<String> secretObjectives) {
        this.secretObjectives = secretObjectives;
    }

    public void setActionCards(List<String> actionCards) {
        this.actionCards = actionCards;
    }

    public void setDiscardActionCards(LinkedHashMap<String, Integer> discardActionCards) {
        this.discardActionCards = discardActionCards;
    }

    public String getOwnerID() {
        return ownerID;
    }

    public String getOwnerName() {
        return ownerName == null ? "" : ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getName() {
        return name;
    }

    public HashMap<String, Tile> getTileMap() {
        return tileMap;
    }

    public Tile getTile(String tileID) {
        return tileMap.values().stream()
                .filter(tile -> tile.getTileID().equals(tileID))
                .findFirst()
                .orElse(null);
    }

    public Tile getTileByPosition(String position) {
        return tileMap.get(position);
    }

    public boolean isTileDuplicated(String tileID) {
        return tileMap.values().stream()
                .filter(tile -> tile.getTileID().equals(tileID))
                .count() > 1;
    }

    public void addPlayer(String id, String name) {
        if (MapStatus.open.equals(mapStatus)) {
            Player player = new Player(id, name);
            players.put(id, player);
        }
    }

    public Player addPlayerLoad(String id, String name) {
        Player player = new Player(id, name);
        players.put(id, player);
        return player;
    }

    public LinkedHashMap<String, Player> getPlayers() {
        return players;
    }

    public void setPlayers(LinkedHashMap<String, Player> players) {
        this.players = players;
    }

    public void setPlayer(String playerID, Player player) {
        players.put(playerID, player);
    }

    public Player getPlayer(String userID) {
        return players.get(userID);
    }

    public Set<String> getPlayerIDs() {
        return players.keySet();
    }

    public void removePlayer(String playerID) {
        if (MapStatus.open.equals(mapStatus)) {
            players.remove(playerID);
        }
    }

    public void removePlayerForced(String playerID) {
        players.remove(playerID);
    }

    public void setMapStatus(MapStatus status) {
        mapStatus = status;
    }

    public boolean isMapOpen() {
        return mapStatus == MapStatus.open;
    }

    public String getMapStatus() {
        return mapStatus.value;
    }

    public void setOwnerID(String ownerID) {
        this.ownerID = ownerID;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTileMap(HashMap<String, Tile> tileMap) {
        this.tileMap = tileMap;
        planets.clear();
    }

    public void clearTileMap() {
        this.tileMap.clear();
        planets.clear();
    }

    public void setTile(Tile tile) {
        tileMap.put(tile.getPosition(), tile);
        planets.clear();
    }

    public void removeTile(String position) {
        tileMap.remove(position);
        planets.clear();
    }

    public HashMap<String, UnitHolder> getPlanetsInfo() {
        if (planets.isEmpty()) {
            getPlanets();
        }
        return planets;
    }

    public void clearPlanetsCache(){
        planets.clear();
    }

    public Set<String> getPlanets() {
        if (planets.isEmpty()) {
            for (Tile tile : tileMap.values()) {
                for (java.util.Map.Entry<String, UnitHolder> unitHolderEntry : tile.getUnitHolders().entrySet()) {
                    if (unitHolderEntry.getValue() instanceof Planet) {
                        planets.put(unitHolderEntry.getKey(), unitHolderEntry.getValue());
                    }
                }
            }
            planets.put("custodiavigilia", new Planet("custodiavigilia", new Point(0, 0)));
        }
        return planets.keySet();
    }
}
