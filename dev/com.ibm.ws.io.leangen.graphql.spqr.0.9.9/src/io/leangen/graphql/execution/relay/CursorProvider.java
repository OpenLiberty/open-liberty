package io.leangen.graphql.execution.relay;

import graphql.relay.ConnectionCursor;

/**
 * Created by bojan.tomic on 5/17/16.
 */
@FunctionalInterface
public interface CursorProvider<N> {

    ConnectionCursor createCursor(N node, int index);
}
