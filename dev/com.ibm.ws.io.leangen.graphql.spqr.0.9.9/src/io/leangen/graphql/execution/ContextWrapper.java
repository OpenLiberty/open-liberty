package io.leangen.graphql.execution;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.leangen.graphql.util.GraphQLUtils.CLIENT_MUTATION_ID;

/**
 * Created by bojan.tomic on 5/22/17.
 */
public class ContextWrapper {
    private final Object context;
    private final Map<String, Object> extensions;

    public ContextWrapper(Object context) {
        this.context = context;
        this.extensions = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public <T> T getContext() {
        return (T) context;
    }

    public String getClientMutationId() {
        return getExtension(CLIENT_MUTATION_ID);
    }

    public void setClientMutationId(String clientMutationId) {
        putExtension(CLIENT_MUTATION_ID, clientMutationId);
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtension(String key) {
        return (T) extensions.get(key);
    }

    public void putExtension(String key, Object extension) {
        extensions.put(key, extension);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T removeExtension(String key) {
        return (T) extensions.remove(key);
    }

}
