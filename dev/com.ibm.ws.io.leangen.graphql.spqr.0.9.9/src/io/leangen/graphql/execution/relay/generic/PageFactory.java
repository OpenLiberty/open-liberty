package io.leangen.graphql.execution.relay.generic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import graphql.relay.ConnectionCursor;
import graphql.relay.DefaultConnectionCursor;
import graphql.relay.DefaultEdge;
import graphql.relay.DefaultPageInfo;
import graphql.relay.Edge;
import graphql.relay.PageInfo;
import io.leangen.graphql.execution.relay.Connection;
import io.leangen.graphql.execution.relay.CursorProvider;
import io.leangen.graphql.execution.relay.Page;

/**
 * Created by bojan.tomic on 2/19/17.
 */
@SuppressWarnings("WeakerAccess")
public class PageFactory {

    public static <N> Page<N> createOffsetBasedPage(List<N> nodes, long count, long offset) {
        return createOffsetBasedPage(nodes, offset, hasNextPage(nodes.size(), count, offset), hasPreviousPage(count, offset));
    }

    public static <N> Page<N> createOffsetBasedPage(List<N> nodes, long offset, boolean hasNextPage, boolean hasPreviousPage) {
        return createPage(nodes, offsetBasedCursorProvider(offset), hasNextPage, hasPreviousPage);
    }

    public static <N, P extends Page<N>> P createOffsetBasedPage(List<N> nodes, long count, long offset, BiFunction<List<Edge<N>>, PageInfo, P> pageCreator) {
        BiFunction<N, ConnectionCursor, Edge<N>> edgeCreator = DefaultEdge::new;
        return createOffsetBasedConnection(nodes, count, offset, edgeCreator, pageCreator);
    }

    public static <N, E extends Edge<N>, C extends Connection<E>> C createOffsetBasedConnection(
            List<N> nodes, long count, long offset, BiFunction<N, ConnectionCursor, E> edgeCreator, BiFunction<List<E>, PageInfo, C> connectionCreator) {

        List<E> edges = createEdges(nodes, offsetBasedCursorProvider(offset), edgeCreator);
        return connectionCreator.apply(edges, createPageInfo(edges, hasNextPage(nodes.size(), count, offset), hasPreviousPage(count, offset)));
    }

    public static <N> Page<N> createPage(List<N> nodes, CursorProvider<N> cursorProvider, boolean hasNextPage, boolean hasPreviousPage) {
        List<Edge<N>> edges = createEdges(nodes, cursorProvider);
        return new GenericPage<>(edges, createPageInfo(edges, hasNextPage, hasPreviousPage));
    }

    public static <N> List<Edge<N>> createEdges(List<N> nodes, CursorProvider<N> cursorProvider) {
        BiFunction<N, ConnectionCursor, Edge<N>> edgeCreator = DefaultEdge::new;
        return createEdges(nodes, cursorProvider, edgeCreator);
    }

    public static <N, E extends Edge<N>> List<E> createEdges(List<N> nodes, CursorProvider<N> cursorProvider, BiFunction<N, ConnectionCursor, E> edgeCreator) {
        List<E> edges = new ArrayList<>(nodes.size());
        int index = 0;
        for (N node : nodes) {
            edges.add(edgeCreator.apply(node, cursorProvider.createCursor(node, index++)));
        }
        return edges;
    }

    public static <N, E extends Edge<N>> PageInfo createOffsetBasedPageInfo(List<E> edges, long count, long offset) {
        return createPageInfo(edges, hasNextPage(edges.size(), count, offset), hasPreviousPage(count, offset));
    }

    public static <N, E extends Edge<N>> PageInfo createPageInfo(List<E> edges, boolean hasNextPage, boolean hasPreviousPage) {
        ConnectionCursor firstCursor = null;
        ConnectionCursor lastCursor = null;
        if (!edges.isEmpty()) {
            firstCursor = edges.get(0).getCursor();
            lastCursor = edges.get(edges.size() - 1).getCursor();
        }
        return new DefaultPageInfo(firstCursor, lastCursor, hasPreviousPage, hasNextPage);
    }

    public static <N> CursorProvider<N> offsetBasedCursorProvider(long offset) {
        return (node, index) -> new DefaultConnectionCursor(Long.toString(offset + index + 1));
    }

    public static boolean hasNextPage(long nodes, long count, long offset) {
        return offset + nodes < count;
    }

    public static boolean hasPreviousPage(long count, long offset) {
        return offset > 0 && count > 0;
    }
}
