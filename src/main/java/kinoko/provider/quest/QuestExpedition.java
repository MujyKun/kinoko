package kinoko.provider.quest;

import kinoko.provider.ProviderError;
import kinoko.provider.WzProvider;
import kinoko.provider.wz.serialize.WzProperty;

public class QuestExpedition {

    private final int expedIndex;
    private final String expedName;
    private final int levelMin;
    private final int levelMax;
    private final int userCount;

    public QuestExpedition(int expedIndex, String expedName, int levelMin, int levelMax, int userCount) {
        this.expedIndex = expedIndex;
        this.expedName = expedName;
        this.levelMin = levelMin;
        this.levelMax = levelMax;
        this.userCount = userCount;
    }

    public int getExpedIndex() {
        return expedIndex;
    }

    public String getExpedName() {
        return expedName;
    }

    public int getMinLevel() {
        return levelMin;
    }

    public int getMaxLevel() {
        return levelMax;
    }

    public int getUserCount() {
        return userCount;
    }

    @Override
    public String toString() {
        return "QuestExpedition{" +
                "expedIndex=" + expedIndex +
                ", expedName='" + expedName + '\'' +
                ", levelMin=" + levelMin +
                ", levelMax=" + levelMax +
                ", userCount=" + userCount +
                '}';
    }

    public static QuestExpedition from(int expedIndex, WzProperty expedInfo) throws ProviderError {
        String expedName = "";
        int levelMin = 0;
        int levelMax = 200;
        int userCount = 6;
        for (var infoEntry : expedInfo.getItems().entrySet()) {
            switch (infoEntry.getKey()) {
                case "name" -> {
                    expedName = WzProvider.getString(infoEntry.getValue());
                }
                case "levelMin" -> {
                    if (infoEntry.getValue() instanceof String) {
                        levelMin = Integer.parseInt((String) infoEntry.getValue());
                    }
                    else
                        levelMin = (int) infoEntry.getValue();
                }
                case "levelMax" -> {
                    if (infoEntry.getValue() instanceof String) {
                        levelMax = Integer.parseInt((String) infoEntry.getValue());
                    }
                    else
                        levelMax = (int) infoEntry.getValue();
                }
                case "userCount" -> {
                    userCount = (int) infoEntry.getValue();
                }
            }
        }
        return new QuestExpedition(
                expedIndex,
                expedName,
                levelMin,
                levelMax,
                userCount
        );
    }

}