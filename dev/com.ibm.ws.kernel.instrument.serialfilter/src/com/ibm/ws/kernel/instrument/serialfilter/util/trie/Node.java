/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.util.trie;

import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A node in a trie, holding a reference to a value
 * (or null if no value is stored in this position).
 * This node implementation supports successive
 * @param <T>
 */
abstract class Node<T> extends AtomicReference<T> {
    final ConcurrentNavigableMap<Character, ChildNode<T>> children = new ConcurrentSkipListMap<Character, ChildNode<T>>();

    abstract int depth();

    final ChildNode<T> getOrCreateChildNode(String key) {
        char c = key.charAt(depth());
        ChildNode<T> result = children.get(c);
        if (result != null) return result;
        ChildNode<T> newChild = new ChildNode<T>(this, key);
        result = children.putIfAbsent(c, newChild);
        return result == null ? newChild : result;
    }

    final ChildNode<T> getChildNode(String key) {
        char c = key.charAt(depth());
        return children.get(c);
    }

    final ChildNode<T> firstChild() {
        Map.Entry<?, ChildNode<T>> e = children.firstEntry();
        return e == null ? null : e.getValue();
    }

    abstract Node<T> getParent();

    abstract ChildNode<T> nextSibling();

    abstract String getStringSoFar();

    final Trie.Entry<T> getEntry() {
        T t = get();
        return t == null ? null : new EntryImpl(t);
    }

    /**
     * Although the key in a Node is immutable, the value is subject to change.
     * This inner class represents a point-in-time view of the value in a node.
     * It can be used to set the value, and it will update its own view as well
     * as the outer Node object's value. However, it will not see updates via
     * any other route.
     */
    private final class EntryImpl implements Trie.Entry<T> {
        private volatile T t;

        EntryImpl(T t) {this.t = t;}

        @Override
        public String getKey() {return Node.this.getStringSoFar();}

        @Override
        public T getValue() {return t;}

        @Override
        public T setValue(T t) {return Node.this.getAndSet(this.t = t);}

        @Override
        public boolean equals(Object other) {
            if (other == null) return false;
            if (!!!(other instanceof java.util.Map.Entry)) return false;
            java.util.Map.Entry that = (java.util.Map.Entry) other;
            return getKey().equals(that.getKey()) && t.equals(that.getValue());
        }

        @Override
        public int hashCode() {return (getKey().hashCode()) ^ (t.hashCode());}

        @Override
        public String toString() {return getKey() + "=" + getValue();}

    }
}
