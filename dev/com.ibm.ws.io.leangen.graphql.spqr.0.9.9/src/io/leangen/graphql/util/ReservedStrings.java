package io.leangen.graphql.util;

public class ReservedStrings {

    public static final String NULL = "\u200B\u202F\uFEFF<null>\u200B\u202F\uFEFF";

    public static String decode(String value) {
        return NULL.equals(value) ? null : value;
    }
}
