package com.hiddengems.entity;

/** The badges a player can earn. They are awarded automatically by the number of keys collected. */
public enum Badge {
    FIRST_KEY("First Key", "Collect your first key", 1),
    EXPLORER("Explorer", "Collect 5 keys", 5),
    COLLECTOR("Collector", "Collect 10 keys", 10);

    private final String title;
    private final String description;
    private final int keysRequired;

    Badge(String title, String description, int keysRequired) {
        this.title = title;
        this.description = description;
        this.keysRequired = keysRequired;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getKeysRequired() {
        return keysRequired;
    }
}