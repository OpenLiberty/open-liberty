package io.leangen.graphql.execution.relay;

import java.util.List;

import graphql.relay.Edge;
import graphql.relay.PageInfo;

/**
 * Created by bojan.tomic on 4/6/16.
 */
public interface Page<N> extends Connection<Edge<N>> {
    List<Edge<N>> getEdges();

    PageInfo getPageInfo();
}
