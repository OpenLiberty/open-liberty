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
import java.util.function.Function;

/**
 * Generic walk implementations
 * <p>
 * These are used by {@link FeatureTreeWalker} but are implemented separately to separate the walking logic from the feature details
 */
public class Walker {

    public enum WalkDecision {
        WALK_CHILDREN,
        IGNORE_CHILDREN
    }

    /**
     * Walk a tree of elements breadth first
     * <p>
     * Any cycles in the tree are detected and the element and its children are ignored when it is encountered as a descendant of itself
     *
     * @param <T>         the element type
     * @param root        the starting element
     * @param visit       a function called when each element is visited and returns whether to visit the elements children. The arguments are the parent feature and the visited
     *                        feature.
     * @param getChildren a function to get the children of an element
     */
    public static <T> void walkBreadthFirst(T root, VisitFunction<? super T> visit, Function<T, Collection<? extends T>> getChildren) {
        walkCollectionBreadthFirst(Collections.singleton(root), visit, getChildren);
    }

    /**
     * Walk a tree of elements breadth first
     * <p>
     * Any cycles in the tree are detected and the element and its children are ignored when it is encountered as a descendant of itself
     *
     * @param <T>         the element type
     * @param roots       the starting elements
     * @param visit       a function called when each element is visited and returns whether to visit the elements children
     * @param getChildren a function to get the children of an element
     */
    public static <T> void walkCollectionBreadthFirst(Collection<? extends T> roots, VisitFunction<? super T> visit,
                                                      Function<T, Collection<? extends T>> getChildren) {
        Deque<WalkElement<T>> queue = new ArrayDeque<>(); // Queue of the next things to walk
        for (T root : roots) {
            queue.add(new WalkElement<>(root, null));
        }

        while (!queue.isEmpty()) {
            WalkElement<T> current = queue.pollFirst();
            WalkDecision decision = visit.apply(current.parent(), current.item());

            if (decision == WalkDecision.WALK_CHILDREN) {
                for (T child : getChildren.apply(current.item())) {
                    if (!current.hasAncestor(child)) { // check for loops
                        queue.addLast(new WalkElement<T>(child, current));
                    }
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
     * @param visit       a function called when each element is visited and returns whether to visit the elements children
     * @param getChildren a function to get the children of an element
     */
    public static <T> void walkDepthFirst(T root, VisitFunction<? super T> visit, Function<T, Collection<? extends T>> getChildren) {
        walkDepthFirst(root, null, visit, getChildren, new HashSet<T>());
    }

    private static <T> void walkDepthFirst(T item, T parent, VisitFunction<? super T> visit, Function<T, Collection<? extends T>> getChildren,
                                           HashSet<T> stack) {
        stack.add(item);

        WalkDecision descision = visit.apply(parent, item);

        if (descision == WalkDecision.WALK_CHILDREN) {
            for (T child : getChildren.apply(item)) {
                if (!stack.contains(child)) {
                    walkDepthFirst(child, item, visit, getChildren, stack);
                }
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

        public T parent() {
            return parent == null ? null : parent.item();
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

    /**
     * A function to be called when visiting an item
     *
     * @param <T> the item type
     */
    public static interface VisitFunction<T> {
        /**
         * Visit an item
         *
         * @param parent the item which has this item as its child, @{code null} for root items
         * @param item   the item
         * @return whether to walk the children of {@code item}
         */
        public WalkDecision apply(T parent, T item);
    }

}
