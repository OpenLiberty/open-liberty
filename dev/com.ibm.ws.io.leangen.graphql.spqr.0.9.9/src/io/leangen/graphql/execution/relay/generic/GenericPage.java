package io.leangen.graphql.execution.relay.generic;

import java.util.List;

import graphql.relay.Edge;
import graphql.relay.PageInfo;
import io.leangen.graphql.execution.relay.Page;

/**
 * Created by bojan.tomic on 5/16/16.
 */
public class GenericPage<N> implements Page<N> {

    private List<Edge<N>> edges;
    private PageInfo pageInfo;

    @SuppressWarnings("WeakerAccess")
    public GenericPage(List<Edge<N>> edges, PageInfo pageInfo) {
        this.edges = edges;
        this.pageInfo = pageInfo;
    }

    @Override
    public List<Edge<N>> getEdges() {
        return edges;
    }

    @Override
    public PageInfo getPageInfo() {
        return pageInfo;
    }
}
