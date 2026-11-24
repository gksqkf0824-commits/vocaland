package vocaland.model;

/** A vocabulary entry shared by learning and games. */
public record Word(String id, String english, String meaning, String difficulty) {}
