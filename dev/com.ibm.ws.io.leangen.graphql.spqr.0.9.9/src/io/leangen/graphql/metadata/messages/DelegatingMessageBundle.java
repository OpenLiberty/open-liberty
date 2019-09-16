package io.leangen.graphql.metadata.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DelegatingMessageBundle implements MessageBundle {

    private final List<MessageBundle> delegates = new ArrayList<>();

    @Override
    public String getMessage(String key) {
        return delegates.stream()
                .filter(bundle -> bundle.containsKey(key))
                .findFirst()
                .map(bundle -> bundle.getMessage(key))
                .orElse(null);
    }

    public DelegatingMessageBundle withBundles(MessageBundle... messageBundle) {
        Collections.addAll(delegates, messageBundle);
        return this;
    }
}
