package de.firemage.autograder.api;

import java.util.Arrays;
import java.util.Comparator;

public enum JavaVersion {
    JAVA_7("7", 7),
    JAVA_8("8", 8),
    JAVA_9("9", 9),
    JAVA_10("10", 10),
    JAVA_11("11", 11),
    JAVA_12("12", 12),
    JAVA_13("13", 13),
    JAVA_14("14", 14),
    JAVA_15("15", 15),
    JAVA_16("16", 16),
    JAVA_17("17", 17),
    JAVA_21("21", 21),
    JAVA_25("25", 25);

    private final String versionString;
    private final int versionNumber;

    JavaVersion(String versionString, int versionNumber) {
        this.versionString = versionString;
        this.versionNumber = versionNumber;
    }

    public static JavaVersion fromString(String s) {
        return Arrays.stream(JavaVersion.class.getEnumConstants())
                .filter(v -> v.getVersionString().equals(s))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Unknown java version"));
    }

    public static JavaVersion latest() {
        return Arrays.stream(JavaVersion.values()).max(Comparator.comparingInt(JavaVersion::getVersionNumber)).orElseThrow();
    }

    public static boolean isValidJavaVersion(String s) {
        return Arrays.stream(JavaVersion.class.getEnumConstants())
                .anyMatch(v -> v.getVersionString().equals(s));
    }

    public String getVersionString() {
        return this.versionString;
    }

    public int getVersionNumber() {
        return this.versionNumber;
    }
}
