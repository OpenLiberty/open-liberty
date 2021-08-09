package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * TreeMap is an implementation of SortedMap. All optional operations are
 * supported, adding and removing. The values can be any objects. The keys can
 * be any objects which are comparable to each other either using their natural
 * order or a specified Comparator.
 */
public abstract class AbstractTreeMap extends AbstractMap implements SortedMap {

    long size;
    Token root;
    Comparator comparator;

    int modCount;

    /**
     * Entry is an internal class which is used to hold the entries of a
     * TreeMap.
     */
    abstract static class Entry extends AbstractMapEntry {

        Token parent, left, right;
        boolean color;

        Entry(Object key) {
            super(key);
        }

        Entry(Object key, Token value) {
            super(key, value);
        }

        boolean getColor() throws ObjectManagerException {
            return color;
        }

        void setColor(boolean color) throws ObjectManagerException {
            this.color = color;
        }

        Entry getLeft() throws ObjectManagerException {
            return left == null ? null : (Entry) left.getManagedObject();
        }

        void setLeft(Entry left) throws ObjectManagerException {
            this.left = left == null ? null : left.getToken();
        }

        Entry getParent() throws ObjectManagerException {
            return parent == null ? null : (Entry) parent.getManagedObject();
        }

        void setParent(Entry parent) throws ObjectManagerException {
            this.parent = parent == null ? null : parent.getToken();
        }

        Entry getRight() throws ObjectManagerException {
            return right == null ? null : (Entry) right.getManagedObject();
        }

        void setRight(Entry right) throws ObjectManagerException {
            this.right = right == null ? null : right.getToken();
        }
    }

    abstract class TreeMapIterator implements Iterator {

        private final AbstractTreeMap backingMap;
        private int expectedModCount;
        private final AbstractMapEntry.Type type;
        private boolean hasEnd = false;
        private Entry node, lastNode;
        private Object endKey;

        TreeMapIterator(AbstractTreeMap map, AbstractMapEntry.Type value)
            throws ObjectManagerException {
            backingMap = map;
            type = value;
            expectedModCount = map.modCount;
            if (map.getRoot() != null)
                node = AbstractTreeMap.minimum(map.getRoot());
        }

        TreeMapIterator(AbstractTreeMap map, AbstractMapEntry.Type value,
                        Entry startNode, boolean checkEnd, Object end) {
            backingMap = map;
            type = value;
            expectedModCount = map.modCount;
            node = startNode;
            hasEnd = checkEnd;
            endKey = end;
        }

        /**
         * @see com.ibm.ws.objectManager.Iterator#hasNext()
         */
        public boolean hasNext() {
            return node != null;
        }

        /**
         * @see com.ibm.ws.objectManager.Iterator#next()
         */
        public Object next() throws ObjectManagerException {
            if (expectedModCount == backingMap.modCount) {
                if (node != null) {
                    lastNode = node;
                    node = AbstractTreeMap.successor(node);
                    if (hasEnd && node != null) {
                        Comparator c = backingMap.comparator();
                        if (c == null) {
                            if (((Comparable) endKey).compareTo(node.key) <= 0)
                                node = null;
                        } else {
                            if (c.compare(endKey, node.key) <= 0)
                                node = null;
                        }
                    }
                    return type.get(lastNode);
                }
                throw new NoSuchElementException();
            }
            throw new ConcurrentModificationException();
        }

        public void remove() throws ObjectManagerException {
            if (expectedModCount == backingMap.modCount) {
                if (lastNode != null) {
                    backingMap.rbDelete(lastNode);
                    lastNode = null;
                    expectedModCount++;
                } else
                    throw new IllegalStateException();
            } else
                throw new ConcurrentModificationException();
        }
    }

    abstract class SubMap extends AbstractMapView implements SortedMap {

        final AbstractTreeMap backingMap;
        boolean hasStart, hasEnd;
        Object startKey, endKey;

        class SubMapSet extends AbstractSetView implements Set {

            final AbstractTreeMap backingMap;
            boolean hasStart, hasEnd;
            Object startKey, endKey;
            final AbstractMapEntry.Type type;

            SubMapSet(AbstractTreeMap map, AbstractMapEntry.Type theType) {
                backingMap = map;
                type = theType;
            }

            SubMapSet(boolean starting, Object start, AbstractTreeMap map,
                      boolean ending, Object end, AbstractMapEntry.Type theType) {
                this(map, theType);
                hasStart = starting;
                startKey = start;
                hasEnd = ending;
                endKey = end;
            }

            void checkRange(Object key) {
                if (backingMap.comparator() == null) {
                    Comparable object = (Comparable) key;
                    if (hasStart && object.compareTo(startKey) < 0)
                        throw new IllegalArgumentException();
                    if (hasEnd && object.compareTo(endKey) >= 0)
                        throw new IllegalArgumentException();
                } else {
                    if (hasStart && backingMap.comparator().compare(key, startKey) < 0)
                        throw new IllegalArgumentException();
                    if (hasEnd && backingMap.comparator().compare(key, endKey) >= 0)
                        throw new IllegalArgumentException();
                }
            }

            boolean checkRange(Object key, boolean hasStart, boolean hasEnd) {
                if (backingMap.comparator() == null) {
                    Comparable object = (Comparable) key;
                    if (hasStart && object.compareTo(startKey) < 0)
                        return false;
                    if (hasEnd && object.compareTo(endKey) >= 0)
                        return false;
                } else {
                    if (hasStart && backingMap.comparator().compare(key, startKey) < 0)
                        return false;
                    if (hasEnd && backingMap.comparator().compare(key, endKey) >= 0)
                        return false;
                }
                return true;
            }

            public boolean isEmpty() throws ObjectManagerException {
                if (hasStart) {
                    AbstractTreeMap.Entry node = backingMap.findAfter(startKey);
                    return node == null || !checkRange(node.key, false, hasEnd);
                }
                return backingMap.findBefore(endKey) == null;
            }

            /**
             * @see com.ibm.ws.objectManager.AbstractCollectionView#iterator()
             */
            public Iterator iterator() throws ObjectManagerException {
                AbstractTreeMap.Entry startNode;
                if (hasStart) {
                    startNode = backingMap.findAfter(startKey);
                    if (startNode != null && !checkRange(startNode.key, false, hasEnd))
                        startNode = null;
                } else {
                    startNode = backingMap.findBefore(endKey);
                    if (startNode != null)
                        startNode = minimum(backingMap.getRoot());
                }
                return makeTreeMapIterator(type, startNode, hasEnd, endKey);
            }

            /**
             * @see com.ibm.ws.objectManager.AbstractCollectionView#size()
             */
            public long size() throws ObjectManagerException {
                long size = 0;
                Iterator it = iterator();
                while (it.hasNext()) {
                    size++;
                    it.next();
                }
                return size;
            }
        }

        SubMap(Object start, AbstractTreeMap map, Object end) {
            backingMap = map;
            hasStart = start != null;
            startKey = start;
            hasEnd = end != null;
            endKey = end;
        }

        void checkRange(Object key) {
            if (backingMap.comparator() == null) {
                Comparable object = (Comparable) key;
                if (hasStart && object.compareTo(startKey) < 0)
                    throw new IllegalArgumentException();
                if (hasEnd && object.compareTo(endKey) >= 0)
                    throw new IllegalArgumentException();
            } else {
                if (hasStart && backingMap.comparator().compare(key, startKey) < 0)
                    throw new IllegalArgumentException();
                if (hasEnd && backingMap.comparator().compare(key, endKey) >= 0)
                    throw new IllegalArgumentException();
            }
        }

        boolean checkRange(Object key, boolean hasStart, boolean hasEnd) {
            if (backingMap.comparator() == null) {
                Comparable object = (Comparable) key;
                if (hasStart && object.compareTo(startKey) < 0)
                    return false;
                if (hasEnd && object.compareTo(endKey) >= 0)
                    return false;
            } else {
                if (hasStart && backingMap.comparator().compare(key, startKey) < 0)
                    return false;
                if (hasEnd && backingMap.comparator().compare(key, endKey) >= 0)
                    return false;
            }
            return true;
        }

        /**
         * @see com.ibm.ws.objectManager.SortedMap#comparator()
         */
        public Comparator comparator() {
            return backingMap.comparator();
        }

        public boolean containsKey(Object key) throws ObjectManagerException {
            if (checkRange(key, hasStart, hasEnd))
                return backingMap.containsKey(key);
            return false;
        }

        /**
         * @see com.ibm.ws.objectManager.AbstractMapView#entrySet()
         */
        public Set entrySet() throws ObjectManagerException {
            return new SubMapSet(hasStart, startKey, backingMap, hasEnd,
                                 endKey, new AbstractMapEntry.Type() {
                                     public Object get(AbstractMapEntry entry) {
                                         return entry;
                                     }
                                 }) {
                public boolean contains(Object object) throws ObjectManagerException {
                    if (object instanceof Map.Entry) {
                        Map.Entry entry = (Map.Entry) object;
                        Object v1 = get(entry.getKey()), v2 = entry.getValue();
                        return v1 == null ? v2 == null : v1.equals(v2);
                    }
                    return false;
                }
            };
        }

        public Object firstKey() throws ObjectManagerException {
            if (!hasStart)
                return backingMap.firstKey();
            AbstractTreeMap.Entry node = backingMap.findAfter(startKey);
            if (node != null && checkRange(node.key, false, hasEnd))
                return node.key;
            throw new NoSuchElementException();
        }

        public Object get(Object key) throws ObjectManagerException {
            if (checkRange(key, hasStart, hasEnd))
                return backingMap.get(key);
            return null;
        }

        /**
         * @see com.ibm.ws.objectManager.SortedMap#headMap(java.lang.Object)
         */
        public SortedMap headMap(Object endKey) {
            checkRange(endKey);
            if (hasStart)
                return makeSubMap(startKey, endKey);
            return makeSubMap(null, endKey);
        }

        public boolean isEmpty() throws ObjectManagerException {
            if (hasStart) {
                AbstractTreeMap.Entry node = backingMap.findAfter(startKey);
                return node == null || !checkRange(node.key, false, hasEnd);
            }
            return backingMap.findBefore(endKey) == null;
        }

        public Collection keyCollection() {
            if (keyCollection == null) {
                keyCollection = new SubMapSet(hasStart, startKey, backingMap, hasEnd, endKey,
                                              new AbstractMapEntry.Type() {
                                                  public Object get(AbstractMapEntry entry) {
                                                      return entry.key;
                                                  }
                                              }) {
                    public boolean contains(Object object) throws ObjectManagerException {
                        return containsKey(object);
                    }
                };
            }
            return keyCollection;
        }

        public Object lastKey() throws ObjectManagerException {
            if (!hasEnd)
                return backingMap.lastKey();
            AbstractTreeMap.Entry node = backingMap.findBefore(endKey);
            if (node != null && checkRange(node.key, hasStart, false))
                return node.key;
            throw new NoSuchElementException();
        }

        public Object put(Object key, Token value) throws ObjectManagerException {
            if (checkRange(key, hasStart, hasEnd))
                return backingMap.put(key, value);
            throw new IllegalArgumentException();
        }

        public Object remove(Object key) throws ObjectManagerException {
            if (checkRange(key, hasStart, hasEnd))
                return backingMap.remove(key);
            return null;
        }

        public SortedMap subMap(Object startKey, Object endKey) {
            checkRange(startKey);
            checkRange(endKey);
            Comparator c = backingMap.comparator();
            if (c == null) {
                if (((Comparable) startKey).compareTo(endKey) <= 0)
                    return makeSubMap(startKey, endKey);
            } else {
                if (c.compare(startKey, endKey) <= 0)
                    return makeSubMap(startKey, endKey);
            }
            throw new IllegalArgumentException();
        }

        /**
         * @see com.ibm.ws.objectManager.SortedMap#tailMap(java.lang.Object)
         */
        public SortedMap tailMap(Object startKey) {
            checkRange(startKey);
            return makeSubMap(startKey, endKey);
        }

        /**
         * @see com.ibm.ws.objectManager.AbstractMapView#values()
         */
        public Collection values() {
            return new SubMapSet(hasStart, startKey, backingMap, hasEnd,
                                 endKey, new AbstractMapEntry.Type() {
                                     public Object get(AbstractMapEntry entry) {
                                         return entry.value;
                                     }
                                 });
        }
    }

    /**
     * Contructs a new empty instance of TreeMap.
     */
    public AbstractTreeMap() {
        super();
    }

    /**
     * Contructs a new empty instance of TreeMap which uses the specified
     * Comparator.
     * 
     * @param comparator the Comparator
     */
    public AbstractTreeMap(Comparator comparator) {
        this.comparator = comparator;
    }

    /**
     * Constructs a new instance of TreeMap containing the mappings from the
     * specified Map and using the natural ordering.
     * 
     * @param map the mappings to add
     * 
     * @exception ClassCastException
     *                when a key in the Map does not implement the Comparable
     *                interface, or they keys in the Map cannot be compared
     * @throws ObjectManagerException
     */
    public AbstractTreeMap(Map map) throws ObjectManagerException {
        this();
        putAll(map);
    }

    /**
     * Constructs a new instance of TreeMap containing the mappings from the
     * specified SortedMap and using the same Comparator.
     * 
     * @param map the mappings to add
     * @throws ObjectManagerException
     */
    public AbstractTreeMap(SortedMap map) throws ObjectManagerException {
        this(map.comparator());
        Iterator it = map.entrySet().iterator();
        if (it.hasNext()) {
            Entry entry = (Entry) it.next();
            Entry last = makeEntry(entry.getKey(), entry.getValue());
            setRoot(last);
            size = 1;
            while (it.hasNext()) {
                entry = (Entry) it.next();
                Entry x = makeEntry(entry.getKey(), entry.getValue());
                x.setParent(last);
                last.setRight(x);
                size++;
                balance(x);
                last = x;
            }
        }
    }

    Entry getRoot() throws ObjectManagerException {
        return root == null ? null : (Entry) root.getManagedObject();
    }

    void setRoot(Entry root) throws ObjectManagerException {
        this.root = root == null ? null : root.getToken();
    }

    abstract Entry makeEntry(Object key) throws ObjectManagerException;

    abstract Entry makeEntry(Object key, Token value) throws ObjectManagerException;

    abstract Iterator makeTreeMapIterator(AbstractMapEntry.Type value)
                    throws ObjectManagerException;

    abstract Iterator makeTreeMapIterator(AbstractMapEntry.Type value,
                                          AbstractTreeMap.Entry startNode, boolean checkEnd, Object end)
                    throws ObjectManagerException;

    abstract AbstractTreeMap.SubMap makeSubMap(Object start, Object end);

    void balance(Entry x) throws ObjectManagerException {
        Entry y;
        x.setColor(true);
        while (x != getRoot() && x.getParent().getColor()) {
            if (x.getParent() == x.getParent().getParent().getLeft()) {
                y = x.getParent().getParent().getRight();
                if (y != null && y.getColor()) {
                    x.getParent().setColor(false);
                    y.setColor(false);
                    x.getParent().getParent().setColor(true);
                    x = x.getParent().getParent();
                } else {
                    if (x == x.getParent().getRight()) {
                        x = x.getParent();
                        leftRotate(x);
                    }
                    x.getParent().setColor(false);
                    x.getParent().getParent().setColor(true);
                    rightRotate(x.getParent().getParent());
                }
            } else {
                y = x.getParent().getParent().getLeft();
                if (y != null && y.getColor()) {
                    x.getParent().setColor(false);
                    y.setColor(false);
                    x.getParent().getParent().setColor(true);
                    x = x.getParent().getParent();
                } else {
                    if (x == x.getParent().getLeft()) {
                        x = x.getParent();
                        rightRotate(x);
                    }
                    x.getParent().setColor(false);
                    x.getParent().getParent().setColor(true);
                    leftRotate(x.getParent().getParent());
                }
            }
        }
        getRoot().setColor(false);
    }

    /**
     * Removes all mappings from this TreeMap, leaving it empty.
     * 
     * @see Map#isEmpty
     * @see #size
     */
    public void clear() {
        root = null;
        size = 0;
        modCount++;
    }

    /**
     * Answers a new TreeMap with the same mappings, size and comparator as this
     * TreeMap.
     * 
     * @return a shallow copy of this TreeMap
     * 
     * @see java.lang.Cloneable
     */
    public Object clone() {
        try {
            AbstractTreeMap clone = (AbstractTreeMap) super.clone();
// FIXME            if (getRoot() != null) clone.setRoot(getRoot().clone(null));
//          was:  if (root != null) clone.root = root.clone(null);

            return clone;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    /**
     * Answers the Comparator used to compare elements in this TreeMap.
     * 
     * @return a Comparator or null if the natural ordering is used
     */
    public Comparator comparator() {
        return comparator;
    }

    /**
     * Searches this TreeMap for the specified key.
     * 
     * @param key the object to search for
     * 
     * @return true if <code>key</code> is a key of this TreeMap, false
     *         otherwise
     * 
     * @exception ClassCastException
     *                when the key cannot be compared with the keys in this
     *                TreeMap
     * @exception NullPointerException
     *                when the key is null and the comparator cannot handle null
     */
    public boolean containsKey(Object key) throws ObjectManagerException {
        return find(key) != null;
    }

    /**
     * Searches this TreeMap for the specified value.
     * 
     * @param value the object to search for
     * @return true if <code>value</code> is a value of this TreeMap, false
     *         otherwise
     */
    public boolean containsValue(Object value) throws ObjectManagerException {
        if (getRoot() != null)
            return containsValue(getRoot(), value);
        return false;
    }

    private boolean containsValue(Entry node, Object value) throws ObjectManagerException {
        if (value == null ? node.value == null : value.equals(node.value))
            return true;
        if (node.getLeft() != null)
            if (containsValue(node.getLeft(), value))
                return true;
        if (node.getRight() != null)
            if (containsValue(node.getRight(), value))
                return true;
        return false;
    }

    /**
     * Answers a Set of the mappings contained in this TreeMap. Each element in
     * the set is a Map.Entry. The set is backed by this TreeMap so changes to
     * one are relected by the other. The set does not support adding.
     * 
     * @return a Set of the mappings
     */
    public Set entrySet() throws ObjectManagerException {
        return new AbstractSetView() {
            public long size() {
                return size;
            }

            public boolean contains(Object object) throws ObjectManagerException {
                if (object instanceof Map.Entry) {
                    Map.Entry entry = (Map.Entry) object;
                    Object v1 = get(entry.getKey()), v2 = entry.getValue();
                    return v1 == null ? v2 == null : v1.equals(v2);
                }
                return false;
            }

            public Iterator iterator() throws ObjectManagerException {
                return makeTreeMapIterator(new AbstractMapEntry.Type() {
                    public Object get(AbstractMapEntry entry) {
                        return entry;
                    }
                });
            }
        };
    }

    Entry find(Object key) throws ObjectManagerException {
        int result;
        Comparable object = null;
        if (comparator == null)
            object = (Comparable) key;
        Entry x = getRoot();
        while (x != null) {
            result = object != null ? object.compareTo(x.key) : comparator.compare(key, x.key);
            if (result == 0)
                return x;
            x = result < 0 ? x.getLeft() : x.getRight();
        }
        return null;
    }

    Entry findAfter(Object key) throws ObjectManagerException {
        int result;
        Comparable object = null;
        if (comparator == null)
            object = (Comparable) key;
        Entry x = getRoot(), last = null;
        while (x != null) {
            result = object != null ? object.compareTo(x.key) : comparator.compare(key, x.key);
            if (result == 0)
                return x;
            if (result < 0) {
                last = x;
                x = x.getLeft();
            } else
                x = x.getRight();
        }
        return last;
    }

    Entry findBefore(Object key) throws ObjectManagerException {
        int result;
        Comparable object = null;
        if (comparator == null)
            object = (Comparable) key;
        Entry x = getRoot(), last = null;
        while (x != null) {
            result = object != null ? object.compareTo(x.key) : comparator.compare(key, x.key);
            if (result <= 0)
                x = x.getLeft();
            else {
                last = x;
                x = x.getRight();
            }
        }
        return last;
    }

    /**
     * Answer the first sorted key in this TreeMap.
     * 
     * @return the first sorted key
     * 
     * @exception NoSuchElementException
     *                when this TreeMap is empty
     */
    public Object firstKey() throws ObjectManagerException {
        if (getRoot() != null)
            return minimum(getRoot()).key;
        throw new NoSuchElementException();
    }

    void fixup(Entry x) throws ObjectManagerException {
        Entry w;
        while (x != getRoot() && !x.getColor()) {
            if (x == x.getParent().getLeft()) {
                w = x.getParent().getRight();
                if (w == null) {
                    x = x.getParent();
                    continue;
                }
                if (w.getColor()) {
                    w.setColor(false);
                    x.getParent().setColor(true);
                    leftRotate(x.getParent());
                    w = x.getParent().getRight();
                    if (w == null) {
                        x = x.getParent();
                        continue;
                    }
                }
                if ((w.getLeft() == null || !w.getLeft().getColor())
                    && (w.getRight() == null || !w.getRight().getColor())) {
                    w.setColor(true);
                    x = x.getParent();
                } else {
                    if (w.getRight() == null || !w.getRight().getColor()) {
                        w.getLeft().setColor(false);
                        w.setColor(true);
                        rightRotate(w);
                        w = x.getParent().getRight();
                    }
                    w.setColor(x.getParent().getColor());
                    x.getParent().setColor(false);
                    w.getRight().setColor(false);
                    leftRotate(x.getParent());
                    x = getRoot();
                }
            } else {
                w = x.getParent().getLeft();
                if (w == null) {
                    x = x.getParent();
                    continue;
                }
                if (w.getColor()) {
                    w.setColor(false);
                    x.getParent().setColor(true);
                    rightRotate(x.getParent());
                    w = x.getParent().getLeft();
                    if (w == null) {
                        x = x.getParent();
                        continue;
                    }
                }
                if ((w.getLeft() == null || !w.getLeft().getColor())
                    && (w.getRight() == null || !w.getRight().getColor())) {
                    w.setColor(true);
                    x = x.getParent();
                } else {
                    if (w.getLeft() == null || !w.getLeft().getColor()) {
                        w.getRight().setColor(false);
                        w.setColor(true);
                        leftRotate(w);
                        w = x.getParent().getLeft();
                    }
                    w.setColor(x.getParent().getColor());
                    x.getParent().setColor(false);
                    w.getLeft().setColor(false);
                    rightRotate(x.getParent());
                    x = getRoot();
                }
            }
        }
        x.setColor(false);
    }

    /**
     * Answers the value of the mapping with the specified key.
     * 
     * @param key the key
     * 
     * @return the value of the mapping with the specified key
     * 
     * @exception ClassCastException
     *                when the key cannot be compared with the keys in this
     *                TreeMap
     * @exception NullPointerException
     *                when the key is null and the comparator cannot handle null
     */
    public Object get(Object key) throws ObjectManagerException {
        Entry node = find(key);
        if (node != null)
            return node.value;
        return null;
    }

    /**
     * Answers a SortedMap of the specified portion of this TreeMap which
     * contains keys less than the end key. The returned SortedMap is backed by
     * this TreeMap so changes to one are reflected by the other.
     * 
     * @param endKey the end key
     * 
     * @return a submap where the keys are less than <code>endKey</code>
     * 
     * @exception ClassCastException
     *                when the end key cannot be compared with the keys in this
     *                TreeMap
     * @exception NullPointerException
     *                when the end key is null and the comparator cannot handle
     *                null
     */
    public SortedMap headMap(Object endKey) {
        // Check for errors
        if (comparator == null)
            ((Comparable) endKey).compareTo(endKey);
        else
            comparator.compare(endKey, endKey);
        return makeSubMap(null, endKey);
    }

    /**
     * Answers a Collection of the keys contained in this TreeMap. The set is backed by
     * this TreeMap so changes to one are relected by the other. The Collection does
     * not support adding.
     * 
     * @return a Collection of the keys
     */
    public Collection keyCollection() {
        if (keyCollection == null) {
            keyCollection = new AbstractCollectionView() {
                public long size() throws ObjectManagerException {
                    return AbstractTreeMap.this.size();
                }

                public Iterator iterator() throws ObjectManagerException {
                    return makeTreeMapIterator(
                    new AbstractMapEntry.Type() {
                        public Object get(AbstractMapEntry entry) {
                            return entry.key;
                        }
                    });
                }
            };
        }
        return keyCollection;
    }

    /**
     * Answer the last sorted key in this TreeMap.
     * 
     * @return the last sorted key
     * 
     * @exception NoSuchElementException
     *                when this TreeMap is empty
     */
    public Object lastKey() throws ObjectManagerException {
        if (getRoot() != null)
            return maximum(getRoot()).key;
        throw new NoSuchElementException();
    }

    private void leftRotate(Entry x) throws ObjectManagerException {
        Entry y = x.getRight();
        x.setRight(y.getLeft());
        if (y.getLeft() != null)
            y.getLeft().setParent(x);
        y.setParent(x.getParent());
        if (x.getParent() == null) {
            setRoot(y);
        } else {
            if (x == x.getParent().getLeft())
                x.getParent().setLeft(y);
            else
                x.getParent().setRight(y);
        }
        y.setLeft(x);
        x.setParent(y);
    }

    static Entry maximum(Entry x) throws ObjectManagerException {
        while (x.getRight() != null)
            x = x.getRight();
        return x;
    }

    static Entry minimum(Entry x) throws ObjectManagerException {
        while (x.getLeft() != null)
            x = x.getLeft();
        return x;
    }

    static Entry predecessor(Entry x) throws ObjectManagerException {
        if (x.getLeft() != null)
            return maximum(x.getLeft());
        Entry y = x.getParent();
        while (y != null && x == y.getLeft()) {
            x = y;
            y = y.getParent();
        }
        return y;
    }

    /**
     * Maps the specified key to the specified value.
     * 
     * @param key the key
     * @param value the value
     * 
     * @return the value of any previous mapping with the specified key or null
     *         if there was no mapping
     * 
     * @exception ClassCastException
     *                when the key cannot be compared with the keys in this
     *                TreeMap
     * @exception NullPointerException
     *                when the key is null and the comparator cannot handle null
     * @throws ObjectManagerException
     */
    public Object put(Object key, Token value) throws ObjectManagerException {
        AbstractMapEntry entry = rbInsert(key);
        Object result = entry.value;
        entry.value = value;
        return result;
    }

    /**
     * Copies every mapping in the specified Map to this TreeMap.
     * 
     * @param map the Map to copy mappings from
     * 
     * @exception ClassCastException
     *                when a key in the Map cannot be compared with the keys in
     *                this TreeMap
     * @exception NullPointerException
     *                when a key in the Map is null and the comparator cannot
     *                handle null
     */
    public void putAll(Map map) throws ObjectManagerException {
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    void rbDelete(Entry z) throws ObjectManagerException {
        Entry y = z.getLeft() == null || z.getRight() == null ? z : successor(z);
        Entry x = y.getLeft() != null ? y.getLeft() : y.getRight();
        if (x != null)
            x.setParent(y.getParent());
        if (y.getParent() == null)
            setRoot(x);
        else if (y == y.getParent().getLeft())
            y.getParent().setLeft(x);
        else
            y.getParent().setRight(x);
        modCount++;
        if (y != z) {
            z.key = y.key;
            z.value = y.value;
        }
        if (!y.getColor() && getRoot() != null) {
            if (x == null)
                fixup(y.getParent());
            else
                fixup(x);
        }
        size--;
    }

    private Entry rbInsert(Object object) throws ObjectManagerException {
        int result = 0;
        Comparable key = null;
        if (comparator == null)
            key = (Comparable) object;
        Entry y = null, x = getRoot();
        while (x != null) {
            y = x;
            result = key != null ? key.compareTo(x.key) : comparator.compare(object, x.key);
            if (result == 0)
                return x;
            x = result < 0 ? x.getLeft() : x.getRight();
        }

        size++;
        modCount++;
        Entry z = makeEntry(object);
        if (y == null) {
            setRoot(z);
            return z;
        }
        z.setParent(y);
        if (result < 0)
            y.setLeft(z);
        else
            y.setRight(z);
        balance(z);
        return z;
    }

    /**
     * Removes a mapping with the specified key from this TreeMap.
     * 
     * @param key the key of the mapping to remove
     * 
     * @return the value of the removed mapping or null if key is not a key in
     *         this TreeMap
     * 
     * @exception ClassCastException
     *                when the key cannot be compared with the keys in this
     *                TreeMap
     * @exception NullPointerException
     *                when the key is null and the comparator cannot handle null
     * @throws ObjectManagerException
     */
    public Object remove(Object key) throws ObjectManagerException {
        Entry node = find(key);
        if (node == null)
            return null;
        Object result = node.value;
        rbDelete(node);
        return result;
    }

    private void rightRotate(Entry x) throws ObjectManagerException {
        Entry y = x.getLeft();
        x.setLeft(y.getRight());
        if (y.getRight() != null)
            y.getRight().setParent(x);
        y.setParent(x.getParent());
        if (x.getParent() == null) {
            setRoot(y);
        } else {
            if (x == x.getParent().getRight())
                x.getParent().setRight(y);
            else
                x.getParent().setLeft(y);
        }
        y.setRight(x);
        x.setParent(y);
    }

    /**
     * Answers the number of mappings in this TreeMap.
     * 
     * @return the number of mappings in this TreeMap
     */
    public long size() {
        return size;
    }

    /**
     * Answers a SortedMap of the specified portion of this TreeMap which
     * contains keys greater or equal to the start key but less than the end
     * key. The returned SortedMap is backed by this TreeMap so changes to one
     * are reflected by the other.
     * 
     * @param startKey the start key
     * @param endKey the end key
     * 
     * @return a submap where the keys are greater or equal to
     *         <code>startKey</code> and less than <code>endKey</code>
     * 
     * @exception ClassCastException
     *                when the start or end key cannot be compared with the keys
     *                in this TreeMap
     * @exception NullPointerException
     *                when the start or end key is null and the comparator
     *                cannot handle null
     */
    public SortedMap subMap(Object startKey, Object endKey) {
        if (comparator == null) {
            if (((Comparable) startKey).compareTo(endKey) <= 0)
                return makeSubMap(startKey, endKey);
        } else {
            if (comparator.compare(startKey, endKey) <= 0)
                return makeSubMap(startKey, endKey);
        }
        throw new IllegalArgumentException();
    }

    static Entry successor(Entry x) throws ObjectManagerException {
        if (x.getRight() != null)
            return minimum(x.getRight());
        Entry y = x.getParent();
        while (y != null && x == y.getRight()) {
            x = y;
            y = y.getParent();
        }
        return y;
    }

    /**
     * Answers a SortedMap of the specified portion of this TreeMap which
     * contains keys greater or equal to the start key. The returned SortedMap
     * is backed by this TreeMap so changes to one are reflected by the other.
     * 
     * @param startKey the start key
     * 
     * @return a submap where the keys are greater or equal to
     *         <code>startKey</code>
     * 
     * @exception ClassCastException
     *                when the start key cannot be compared with the keys in
     *                this TreeMap
     * @exception NullPointerException
     *                when the start key is null and the comparator cannot
     *                handle null
     */
    public SortedMap tailMap(Object startKey) {
        // Check for errors
        if (comparator == null)
            ((Comparable) startKey).compareTo(startKey);
        else
            comparator.compare(startKey, startKey);
        return makeSubMap(startKey, null);
    }

    /**
     * Answers a Collection of the values contained in this TreeMap. The
     * collection is backed by this TreeMap so changes to one are relected by
     * the other. The collection does not support adding.
     * 
     * @return a Collection of the values
     */
    public Collection values() {
        if (values == null) {
            values = new AbstractCollectionView() {
                public long size() throws ObjectManagerException {
                    return AbstractTreeMap.this.size();
                }

                public Iterator iterator() throws ObjectManagerException {
                    return makeTreeMapIterator(new AbstractMapEntry.Type() {
                        public Object get(AbstractMapEntry entry) {
                            return entry.getValue();
                        }
                    });
                }
            };
        }
        return values;
    }
}