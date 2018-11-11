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

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A trie is a tree-like data structure that allows searching by prefix.
 * The tree is built up from the characters of the string key, and the
 * search proceeds character by character down the tree.
 * <br>
 *     This implementation allows finding the longest matching prefix
 *     of a given key. It does not support <code>null</code> keys
 *     or values. When a value is removed, the map is not structurally
 *     modified, so there is a potential to leak if multiple keys are
 *     stored transiently in this trie.
 * @param <V> the value type stored in this map
 */
public class ConcurrentTrie<V> extends AbstractMap<String, V> implements Trie<V>, Iterable<Map.Entry<String, V>> {
    private static final Node<Void> EMPTY_NODE = new RootNode<Void>();

    /** The root node of the tree, usually representing the empty string */
    private final Node<V> root;

    private ConcurrentTrie(Node<V> root) {
        this.root = root;
    }

    public ConcurrentTrie() {
        this(new RootNode<V>());
    }

    private Node<V> getOrCreateNode(String key) {
        if (key.isEmpty()) return root;
        ChildNode<V> childNode = root.getOrCreateChildNode(key);
        while (childNode.depth() < key.length())
            childNode = childNode.getOrCreateChildNode(key);
        return childNode;
    }

    private Node<V> getLongestPrefixNode(String key) {
        Node<V> closest = root;
        Node<V> n = root;
        while (n.depth() < key.length()) {
            n = n.getChildNode(key);
            if (n == null) break;
            if (n.get() != null) closest = n;
        }
        return closest;
    }

    private AtomicReference<? extends V> getEffectiveNode(String key) {
        Node<V> n = root;
        while (n.depth() < key.length()) {
            n = n.getChildNode(key);
            if (n == null) return ConcurrentTrie.<V>emptyNode();
        }
        return n;
    }

    @SuppressWarnings("unchecked")
    private static <T> Node<? extends T> emptyNode() {return (Node<T>) EMPTY_NODE;}

    public V put(String key, V v) {return getOrCreateNode(key).getAndSet(v);}

    public V remove(String key) {return getEffectiveNode(key).getAndSet(null);}

    public V get(String key) {return getEffectiveNode(key).get();}

    public V getLongestPrefixValue(String key) {return getLongestPrefixNode(key).get();}

    public Trie.Entry<V> getLongestPrefixEntry(String key) {return getLongestPrefixNode(key).getEntry();}

    @Override
    public boolean isEmpty() {return !!!iterator().hasNext();}

    @Override
    public V get(Object key) {return key instanceof String ? get((String) key) : null;}

    @Override
    public boolean containsKey(Object key) {return get(key) != null;}

    @Override
    public V remove(Object key) { return key instanceof String ? remove((String) key) : null;}

    @Override
    public Iterator<Map.Entry<String, V>> iterator() {return new TrieIterator();}

    @Override
    public Set<Map.Entry<String, V>> entrySet() {return new EntrySet();}

    private class EntrySet extends AbstractSet<Map.Entry<String, V>> {
        @Override
        public Iterator<Map.Entry<String, V>> iterator() {return new TrieIterator();}

        @Override
        public int size() {
            int i = 0;
            for (Object o : this) i++;
            return i;
        }
    }

    /**
     * Iterate through the nodes in depth-first-traversal preorder,
     * i.e. the order in which nodes are first visited under a
     * depth-first-traversal.
     * <br>
     * This iterator is blocking and thread-safe, but it is
     * not intended to be used concurrently.
     */
    private class TrieIterator implements Iterator<Map.Entry<String, V>> {
        Node<V> node = root;
        Map.Entry<String, V> prev, next;

        @Override
        synchronized public boolean hasNext() {
            return next != null || findNextValue();
        }

        @Override
        synchronized public Map.Entry<String, V> next() {
            if (hasNext()) {
                prev = next;
                next = null;
                return prev;
            }
            throw new NoSuchElementException();
        }

        private boolean findNextValue() {
            do {
                // first, discount the end condition
                if (hasCompleted()) return false;
                // remember the entry for the current node, if there is a value
                next = node.getEntry();
                // set the node to examine after next()
                advanceNode();
            } while (next == null);
            return true;
        }

        boolean hasCompleted() {
            return node == null;
        }

        // Move to the next node in sequence.
        // Visit node, then children, then siblings.
        private void advanceNode() {
            // first try descending
            Node<V> p = node;
            Node<V> n = p.firstChild();
            while (n == null && p != null) {
                // no children so look at siblings then ascend
                // don't descend: siblings must be visited before their children
                // look for next sibling
                n = p.nextSibling();
                // ascend the tree before looking for siblings again
                p = p.getParent();
            }
            // Either n is not null, or we have nothing left to iterate over:
            // either way, n is the next node
            node = n;
        }

        @Override
        synchronized public void remove() {
            if (prev == null)
                throw new IllegalStateException();
            prev.setValue(null);
            prev = null;
        }
    }
}
