package io.leangen.graphql.metadata.messages;

import io.leangen.graphql.util.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface MessageBundle {

    Pattern VARIABLE_PATTERN = Pattern.compile("(\\$\\{([^}]+)\\})");

    String getMessage(String key);

    default boolean containsKey(String key) {
        return getMessage(key) != null;
    }

    default String interpolate(String template) {
        if (Utils.isEmpty(template)) {
            return template;
        }
        StringBuffer sb = new StringBuffer(template.length());
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String replacement = getMessage(matcher.group(2));
            if (replacement == null) {
                replacement = matcher.group(1); //leave unmatched variables in place
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
