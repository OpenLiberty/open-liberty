package io.leangen.graphql.metadata.messages;

public class EmptyMessageBundle implements MessageBundle {

    public static final EmptyMessageBundle INSTANCE = new EmptyMessageBundle();

    @Override
    public String getMessage(String key) {
        return null;
    }
}
