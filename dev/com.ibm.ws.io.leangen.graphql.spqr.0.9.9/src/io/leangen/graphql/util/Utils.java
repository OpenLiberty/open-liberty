package io.leangen.graphql.util;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A collection of utility methods
 */
@SuppressWarnings("WeakerAccess")
public class Utils {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Optional<T> or(Optional<T> left, Optional<T> right) {
        return left.isPresent() ? left : right;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Optional<T> or(Optional<T> left, Supplier<Optional<T>> right) {
        return left.isPresent() ? left : right.get();
    }

    public static boolean isEmpty(String string) {
        return !isNotEmpty(string);
    }

    public static boolean isNotEmpty(String string) {
        return string != null && !string.isEmpty();
    }

    public static String coalesce(String... values) {
        return Arrays.stream(values)
                .filter(Utils::isNotEmpty)
                .findFirst()
                .orElse(null);
    }

    public static String capitalize(final String str) {
        final char firstChar = str.charAt(0);
        final char newChar = Character.toUpperCase(firstChar);
        if (firstChar == newChar) {
            // already capitalized
            return str;
        }

        char[] newChars = new char[str.length()];
        newChars[0] = newChar;
        str.getChars(1, str.length(), newChars, 1);
        return String.valueOf(newChars);
    }

    public static boolean isArrayEmpty(Object array) {
        return !isArrayNotEmpty(array);
    }

    public static boolean isArrayNotEmpty(Object array) {
        return array != null && Array.getLength(array) != 0;
    }

    @SafeVarargs
    public static <T> Stream<T> concat(Stream<T>... streams) {
        return Arrays.stream(streams).reduce(Stream::concat).orElse(Stream.empty());
    }

    public static String[] emptyArray() {
        return EMPTY_STRING_ARRAY;
    }

    public static String[] emptyIfNull(String[] array) {
        return array == null ? emptyArray() : array;
    }

    public static String requireNonEmpty(String value) {
        if (isEmpty(value)) {
            throw new IllegalArgumentException("Empty string is not a valid value");
        }
        return value;
    }

    public static <T> List<T> singletonList(T element) {
        return element == null ? Collections.emptyList() : Collections.singletonList(element);
    }
}
