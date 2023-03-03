/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.repository.resolver;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generic walk implementations
 * <p>
 * These are used by {@link FeatureTreeWalker} but are implemented separately to separate the walking logic from the feature details
 */
public class Walker {

    /**
     * Walk a tree of elements breadth first
     * <p>
     * Any cycles in the tree are detected and the element and its children are ignored when it is encountered as a descendant of itself
     *
     * @param <T>         the element type
     * @param root        the starting element
     * @param forEach     a consumer called when each element is visited
     * @param getChildren a function to get the children of an element
     */
    public static <T> void walkBreadthFirst(T root, Consumer<? super T> forEach, Function<T, Collection<? extends T>> getChildren) {
        walkCollectionBreadthFirst(Collections.singleton(root), forEach, getChildren);
    }

    /**
     * Walk a tree of elements breadth first
     * <p>
     * Any cycles in the tree are detected and the element and its children are ignored when it is encountered as a descendant of itself
     *
     * @param <T>         the element type
     * @param roots       the starting elements
     * @param forEach     a consumer called when each element is visited
     * @param getChildren a function to get the children of an element
     */
    public static <T> void walkCollectionBreadthFirst(Collection<? extends T> roots, Consumer<? super T> forEach, Function<T, Collection<? extends T>> getChildren) {
        Deque<WalkElement<T>> queue = new ArrayDeque<>(); // Queue of the next things to walk
        for (T root : roots) {
            queue.add(new WalkElement<>(root, null));
        }

        while (!queue.isEmpty()) {
            WalkElement<T> current = queue.pollFirst();
            forEach.accept(current.item());

            for (T child : getChildren.apply(current.item())) {
                if (!current.hasAncestor(child)) { // check for loops
                    queue.addLast(new WalkElement<T>(child, current));
                }
            }
        }
    }

    /**
     * Walk a tree of elements depth first, parents are visited before their children
     * <p>
     * Any cycles in the tree are detected and the element and its children are ignored when it is encountered as a descendant of itself
     *
     * @param <T>         the element type
     * @param roots       the starting elements
     * @param forEach     a consumer called when each element is visited
     * @param getChildren a function to get the children of an element
     */
    public static <T> void walkDepthFirst(T root, Consumer<? super T> forEach, Function<T, Collection<? extends T>> getChildren) {
        walkDepthFirst(root, forEach, getChildren, new HashSet<T>());
    }

    private static <T> void walkDepthFirst(T item, Consumer<? super T> forEach, Function<T, Collection<? extends T>> getChildren, HashSet<T> stack) {
        stack.add(item);

        forEach.accept(item);

        for (T child : getChildren.apply(item)) {
            if (!stack.contains(child)) {
                walkDepthFirst(child, forEach, getChildren, stack);
            }
        }

        stack.remove(item);
    }

    /**
     * Helper class to keep track of the ancestors of an element when doing a breadth first search
     * <p>
     * These effectively form a linked stack
     *
     * @param <T> the element type
     */
    private static class WalkElement<T> {
        private final T item;
        private final WalkElement<T> parent;

        public WalkElement(T item, WalkElement<T> parent) {
            super();
            this.item = item;
            this.parent = parent;
        }

        public T item() {
            return item;
        }

        public boolean hasAncestor(T search) {
            WalkElement<T> current = this;
            while (current != null) {
                if (current.item.equals(search)) {
                    return true;
                }
                current = current.parent;
            }
            return false;
        }

    }

}
