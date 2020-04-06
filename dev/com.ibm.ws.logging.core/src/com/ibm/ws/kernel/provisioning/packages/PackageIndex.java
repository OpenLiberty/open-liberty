/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioning.packages;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Stack;

import com.ibm.ws.ffdc.FFDCSelfIntrospectable;

/**
 * A trie used for associating data with packages.
 *
 */
public class PackageIndex<T> implements FFDCSelfIntrospectable, Iterable<T> {
    private static final String WILDCARD = "*";
    
    private static String nl = AccessController.doPrivileged(
    		new PrivilegedAction<String>() {
    			@Override
    			public String run() {
    				return System.getProperty("line.separator");
    			}
            }
    );

    /**
     * Each node is a package segment, which may not contain a wildcard (i.e. a* is not
     * supported).
     * <p>
     * Each node tracks two kinds of kids: exact kids (b), and the wildcard kid (*).
     * They should be searched for a match in that order to ensure that we find the best match.
     */
    static class Node<T> {

        Node<T> wildcardKid = null;
        ArrayList<Node<T>> exactKids = null;

        final String seg;

        T value = null;

        public Node(String seg) {
            this.seg = seg;
        }

        public String getSegment() {
            return seg;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        /**
         * When inserting: we need to match exactly on the segment:
         * if it is a wildcard, we have to find or create the wildcard kid.
         * if it is an exact name, we have to find or create an exact kid
         * Maintain sorted list exactKids to facilitate best match
         * The longest strings should be first.
         *
         * @param segment
         * @return
         */
        public Node<T> findOrCreateChild(String segment) {
            Node<T> result;
            if (segment.equals(WILDCARD)) {
                if (wildcardKid == null)
                    wildcardKid = new Node<T>(WILDCARD);
                result = wildcardKid;
            } else {
                if (exactKids == null) {
                    exactKids = new ArrayList<Node<T>>(5);
                }
                result = insert(segment, exactKids);
            }
            return result;
        }

        /**
         * Find a child with the given segment name.
         */
        public Node<T> findChild(String segment) {
            if (exactKids != null) {
                for (Node<T> n : exactKids) {
                    if (segment.equals(n.getSegment()))
                        return n;
                }
            }
            // no match
            return null;
        }

        /**
         * Insert the node into the specified list.
         *
         * @param n The new node to insert
         * @param list The target list
         * @return the list: allow list to be created lazily.
         */
        private Node<T> insert(String newSegment, List<Node<T>> list) {
            Node<T> result = null;
            if (list.isEmpty()) {
                result = new Node<T>(newSegment);
                list.add(result);
            } else {
                ListIterator<Node<T>> i = list.listIterator();
                while (i.hasNext()) {
                    Node<T> next = i.next();

                    String s = next.getSegment();
                    int c = newSegment.compareTo(s);

                    if (c == 0) {
                        result = next;
                        break;
                    } else if (c > 0) {
                        i.previous(); // insert before this one
                        break;
                    }
                }
                if (result == null) {
                    result = new Node<T>(newSegment);
                    i.add(result);
                }
            }

            return result;
        }

        @Override
        public String toString() {
            return "Node[seg=" + seg
                   + ", *=" + (wildcardKid != null ? 1 : 0)
                   + ", exact=" + (exactKids != null ? exactKids.size() : 0)
                   + "]";
        }
    }

    /**
     * The root: in our case, always the beginning of the string.
     */
    protected final Node<T> root = new Node<T>("");

    /**
     * Add a new value using the given key.
     *
     * @param key Key, this is string
     * @param value Value to associate with the key
     * @return true if key successfully added, false if there was a collision.
     */
    public synchronized boolean add(String key, T value) {
        if (key.length() > 3 && key.endsWith(WILDCARD) && key.charAt(key.length() - 2) != '.') {
            throw new IllegalArgumentException("Unsupported use of wildcard in key " + key);
        }

        // Find the key in the structure (build out to it)
        Node<T> current = internalFind(key, false);

        // Return false if the key already exists
        if (current.getValue() != null)
            return false;

        // Set the value and return true
        current.setValue(value);
        return true;
    }

    /**
     * Find the element for the given string. If this string contains a wildcard,
     * it will only match if the wildcard is in the same place as one of the input
     * keys. For full package/class/group names, it will return the element for
     * the longest-prefix-match first: Given this class name, the value for
     * com.ibm.ws.logging.internal.* will be returned, if present, before * would
     * be.
     *
     * @param key
     * @return The matching element using longest-prefix-match
     */
    public T find(String key) {
        Node<T> current = internalFind(key, true);

        return current == null ? null : current.getValue();
    }

    /**
     * This does the actual work for both add and find (as the paths are very similar).
     * Note that matching behavior will change based on whether or not we're adding elements
     * into the trie. If we're adding, the match will be exact: A * is a exactly and only a *.
     * If we are searching, then the * behaves as a wildcard for matching.
     *
     * @param key Key to lookup or to add.
     * @param search If true, we're in search mode. Otherwise, we're adding.
     * @return Matched TrieNode, or null if not found.
     */
    private Node<T> internalFind(String key, boolean search) {
        int lastPos = 0;
        int nextDot = -1;
        Node<T> current = root;
        Node<T> wildcard = null;
        boolean done = false;

        while (!done) {
            String segment;

            nextDot = key.indexOf('.', lastPos);
            if (nextDot > 0) {
                segment = key.substring(lastPos, nextDot);
            } else {
                segment = key.substring(lastPos); // remainder
                done = true; // last segment
            }

            if (search) { // SEARCH, called from find(..)
                if (current.wildcardKid != null) {
                    wildcard = current.wildcardKid;
                }

                // Look for the node for the next package segment
                Node<T> next = current.findChild(segment);
                if (next == null) {
                    if (wildcard == null) {
                        // no match...
                        return null;
                    }
                    current = wildcard;
                } else {
                    current = next;
                }
            } else { // ADD, called from add(..)
                // Find or create new node, then proceed to the next
                current = current.findOrCreateChild(segment);
            }

            lastPos = nextDot + 1;
        }

        return current;
    }

    /**
     * Compact any empty space after a read-only trie has been constructed
     */
    public void compact() {
        // Bare-bones iterator: no filter, don't bother with package strings
        for (NodeIterator<T> i = this.getNodeIterator(); i.hasNext();) {
            NodeIndex<T> n = i.next();
            if (n.node.exactKids != null)
                n.node.exactKids.trimToSize();
        }
    }

    /**
     * Debugging during test, dump for FFDC
     *
     * @return String representation of the contents of the structure.
     */
    public String dump() {
        StringBuilder s = new StringBuilder();
        int c = 0;
        // we need the flavor of iterator that does build up a package string so we can
        // include it in the generated string
        s.append(nl);
        for (NodeIterator<T> i = this.getNodeIterator(null); i.hasNext();) {
            NodeIndex<T> n = i.next();
            c++;
            s.append('\t').append(n.pkg).append(" = ").append(n.node.getValue()).append(nl);
        }

        s.append("\t---> ").append(c).append(" elements");
        return s.toString();
    }

    /**
     * Get the output of the dump method for inclusion in an FFDC
     * (should that ever occur).
     *
     * @see com.ibm.ws.ffdc.FFDCSelfIntrospectable#introspectSelf()
     */
    @Override
    public String[] introspectSelf() {
        return new String[] { dump() };
    }

    @Override
    public Iterator<T> iterator() {
        return iterator(null);
    }

    public Iterator<T> iterator(Filter<T> valueFilter) {
        final NodeIterator<T> nodeIterator = getNodeIterator(valueFilter);
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return nodeIterator.hasNext();
            }

            @Override
            public T next() {
                NodeIndex<T> n = nodeIterator.next();
                return n.getValue();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public Iterator<String> packageIterator() {
        return packageIterator(null);
    }

    public Iterator<String> packageIterator(Filter<T> valueFilter) {
        final NodeIterator<T> nodeIterator = getNodeIterator(valueFilter);
        return new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return nodeIterator.hasNext();
            }

            @Override
            public String next() {
                NodeIndex<T> n = nodeIterator.next();
                return n.pkg;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public interface Filter<T> {
        boolean includeValue(String packageName, T value);
    }

    /**
     * Get an internal iterator for nodes that allows filtering based on
     * packages or values.
     *
     * @param filter
     * @return
     */
    NodeIterator<T> getNodeIterator(Filter<T> filter) {
        return new NodeIterator<T>(root, filter, true);
    }

    /**
     * Get an internal iterator for nodes that does not build up a package name
     *
     * @return
     */
    NodeIterator<T> getNodeIterator() {
        return new NodeIterator<T>(root, null, false);
    }

    static class NodeIterator<T> implements Iterator<NodeIndex<T>> {

        private final NodeIndex<T> rootIdx;
        private final Stack<NodeIndex<T>> nodeStack;
        private final Filter<T> valueFilter;
        private NodeIndex<T> currentNode = null;

        NodeIterator(Node<T> root, Filter<T> valueFilter, boolean buildPackageName) {
            rootIdx = new NodeIndex<T>(buildPackageName ? "" : null, root);
            nodeStack = new Stack<NodeIndex<T>>();
            nodeStack.push(rootIdx);

            this.valueFilter = (valueFilter != null ? valueFilter : new Filter<T>() {
                @Override
                public boolean includeValue(String packageName, T value) {
                    return true;
                }
            });
        }

        @Override
        public boolean hasNext() {
            while (currentNode == null && !nodeStack.isEmpty()) {
                currentNode = nodeStack.pop();

                // depth-first: go for the farthest kid and work backwards
                while (currentNode.hasMoreKids()) {
                    nodeStack.push(currentNode);
                    currentNode = new NodeIndex<T>(currentNode.pkg, currentNode.getNextKid());
                }

                // If the current node doesn't have a value, we need to pop the next element off
                // the stack (which may have more kids..)
                if (!currentNode.hasValue() || !valueFilter.includeValue(currentNode.pkg, currentNode.getValue())) {
                    currentNode = null;
                }
            }

            return currentNode != null;
        }

        @Override
        public NodeIndex<T> next() {
            NodeIndex<T> n = currentNode;
            currentNode = null;
            return n;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    static class NodeIndex<T> {
        final int kidSize;
        final Node<T> node;
        final boolean hasWildcard;
        final String pkg;

        int kidIndex = 0;
        boolean visitedWidcard = false;

        NodeIndex(String pkgRoot, Node<T> node) {
            this.node = node;
            this.kidSize = node.exactKids == null ? 0 : node.exactKids.size();
            this.hasWildcard = node.wildcardKid != null;

            if (pkgRoot == null) {
                this.pkg = null;
            } else {
                String segment = node.getSegment();
                if (segment.isEmpty())
                    this.pkg = "";
                else {
                    this.pkg = (pkgRoot.isEmpty()) ? segment : pkgRoot + "." + segment;
                }
            }
        }

        /**
         * @return
         */
        public T getValue() {
            return node.value;
        }

        boolean hasValue() {
            return node.value != null;
        }

        boolean hasMoreKids() {
            return (kidIndex < kidSize)
                   || (hasWildcard && !visitedWidcard);
        }

        Node<T> getNextKid() {
            if (kidIndex < kidSize)
                return node.exactKids.get(kidIndex++);
            else if (hasWildcard && !visitedWidcard) {
                visitedWidcard = true;
                return node.wildcardKid;
            }
            return null;
        }

        @Override
        public String toString() {
            return "(" + pkg + ", kids=" + kidIndex + "/" + kidSize + ", wildcard=" + hasWildcard + "/" + visitedWidcard + ", value=" + node.value + ")";
        }
    }
}
