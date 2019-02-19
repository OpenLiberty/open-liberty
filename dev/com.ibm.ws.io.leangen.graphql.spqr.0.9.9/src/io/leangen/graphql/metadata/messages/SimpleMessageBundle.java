package io.leangen.graphql.metadata.messages;

import java.util.Map;

public class SimpleMessageBundle implements MessageBundle {

    private final Map<String, String> variables;

    public SimpleMessageBundle(Map<String, String> variables) {
        this.variables = variables;
    }

    @Override
    public String getMessage(String key) {
        return variables.get(key);
    }
}
