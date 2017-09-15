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

import com.ibm.ws.objectManager.utils.Printable;
import com.ibm.ws.objectManager.utils.Trace;
import com.ibm.ws.objectManager.utils.Tracing;

/**
 * 
 * Classic BTree implementation.
 * 
 * Definition of a B-tree taken from Corme Leiserson and Rivest.
 * Reference: Introduction to Algorithms,
 * by Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest
 * 1990.
 * <P>
 * A B-tree is made of nodes each of which is a sub Tree descending from a root.
 * Each node consists of a non decreasing ordered set of Entries, one Entry of each key
 * value pair present in the node. Each node also has references to child
 * sub trees for the Entry keys occupying the sequence between the Entry keys in the node.
 * There is a defined minimum number of subTrees in each node, called minimumNodeSize,
 * except the root node which can have zero sub trees.
 * Cosequently each sub Tree can have a minimum of minimumNodeSize-1 Entries
 * except the root node which can have zero Entries.
 * A sub tree can have a maximum of minimumNodeSize*2 sub trees and cosequently
 * minimumNodeSize*2 -1 Entries.
 * <P>
 * This implementation supports duplicate keys, duplicate keys are stored in the order they
 * are added, youngest first.
 * <P>
 * Example tree where minimumNodeSize is 2.
 * <blockquote><pre>
 * root
 * \
 * Node -> Entry( ,M) -> Entry ( ,S) -> Entry ( , )
 * \ \ \
 * Node -> Entry( ,A)-> Entry( ,B) Node -> Entry( ,N) Node -> Entry( ,T)
 * </blockquote></pre>
 * Nodes contain a chain of Entrys.
 * Each entry contains a refrerence to a child node containing keys less than then one in the Entry.
 * Each entry also contains a reference to the next entry.
 * The final entry in the chain of an ointermediate node contains no key and a child that contains
 * entrys with keys greater than all other Entrys in the Node.
 * 
 * @author Andrew_Banks
 */
public class BTree
                extends AbstractMapView
                implements Printable
//       implements SortedMap, Cloneable, java.io.Serializable
{

    private static final Class cclass = BTree.class;
    private static Trace trace = ObjectManager.traceFactory.getTrace(BTree.class,
                                                                     ObjectManagerConstants.MSG_GROUP_MAPS);

    // The anchor of the tree.
    Node root;

    // The minimum number of subnodes a node can have, also the minimum number of key a node can have is 
    // minimumNodeSize - 1. The maximum nomber of sub nodes a node may hold is 2*minimumNodeSize, consequently
    // the maximum number of key in a node is 2*minimumNoideSize-1.
    // This value is sometimes called the minimum degree or order of the btree. 
    private int minimumNodeSize;

    // The Comparator used to establish order of keys. or
    // null if this Map uses the elements natural ordering.
    private java.util.Comparator comparator = null;

    /**
     * Constructor contains the root of the btree.
     * 
     * @param minimumNodeSize the minimum number of child nodes allowd in any node except the root.
     *            This must be greater than or equal to 2.
     * @throws ObjectManagerException
     */
    public BTree(int minimumNodeSize)
        throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "<init>"
                        , new Object[] { new Integer(minimumNodeSize) });
        // Check munimumNodeSize >= 2.
        // TODO Should be ObjectManagerException.
        if (minimumNodeSize < 2)
            throw new IllegalArgumentException("Illegal minimum Node Size (less than 2): " +
                                               minimumNodeSize);

        this.minimumNodeSize = minimumNodeSize;
        root = makeNode(null);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "<init>");
    } // BTree().

    /**
     * @return int the minimumNodeSize.
     */
    public int getMinimumNodeSize()
    {
        return minimumNodeSize;
    } // getMinimumNodeSize().

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.AbstractMap#size()
     */
    public synchronized long size()
                    throws ObjectManagerException
    {
        final String methodName = "size";
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        methodName);

        long size = root.size();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       methodName,
                       new Object[] { new Long(size) });
        return size;
    } // size().

    /**
     * Insert an object with the given key into the tree.
     * 
     * @param key for any retrieval.
     * @param value to be inserted.
     * @return Object already occupying the key or null if there is none.
     * @throws ObjectManagerException
     */
    public synchronized Object put(Object key,
                                   Object value)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "put",
                        new Object[] { key });

        Entry newEntry = makeEntry(key, value);
        Entry existingEntry = putEntry(newEntry);

        Object returnValue = null;
        if (existingEntry != null)
            returnValue = existingEntry.getValue();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "put",
                       new Object[] { returnValue });
        return returnValue;
    } // put().

    /**
     * Insert a object with the given key into the tree.
     * 
     * @param newEntry to be inserted into the tree structure.
     * @return an Entry using the new key or null if there is none.
     * @throws ObjectManagerException
     */
    public synchronized Entry putEntry(Entry newEntry)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "putEntry",
                        new Object[] { newEntry });

        // If the root is full then split it, increasing the height of the tree by one.
        if (root.numberOfKeys == minimumNodeSize * 2 - 1) {
            Node newRoot = makeNode(null);
            newRoot.isLeaf = false;
            root.parent = newRoot;
            newRoot.numberOfKeys++;
            newRoot.first = root.split();
            newRoot.first.next = makeEntry(null, null);
            newRoot.first.next.setChild(root);
            root = newRoot;
        } // if (root.numberOfKeys == minimumNodeSize * 2 - 1).

        Entry returnEntry = root.insert(newEntry);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "putEntry",
                       new Object[] { returnEntry });
        return returnEntry;
    } // putEntry().

    /**
     * @param key for the search.
     * @return Object value which is the oldest one that matches the key, or null if there is none.
     * @throws ObjectManagerException
     */
    public synchronized Object get(Object key)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "get",
                        new Object[] { key });

        Object returnValue = null;
        Entry entry = root.get(key);
        if (entry != null)
            returnValue = entry.getValue();

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "get",
                       new Object[] { returnValue });
        return returnValue;
    }

    /*
     * @return Entry which is the oldest one that matches the key, or null if there is none.
     */
    public synchronized Entry getEntry(Object key)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "get",
                        new Object[] { key });

        Entry entry = root.get(key);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "get",
                       new Object[] { entry });
        return entry;
    }

    /*
     * Find and delete an object n the tree.
     * 
     * @return Object value which is the oldest one that matches the key, or null if there is none.
     */
    public synchronized Object remove(Object key)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "remove",
                        new Object[] { key });

        Object returnValue = null;
        // Optionally, see if the key exists in this tree. If it does not exist
        // we could avoid trying to delete it, thus avoiding any unnecessary 
        // rearranging of the tree as we progress the deletion.
        // Find the Entyry we want to delete.
        Entry entry = root.get(key);
        if (entry != null) {
            // Start the search for the deleted object at the root. Since the root
            // may contain less than minimumNodeSize-1 entries we don't check that it contains
            // sufficient Entries before entering it.
            entry = root.delete(key);
            returnValue = entry.getValue();
        } // if (entry != null).  

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "remove",
                       new Object[] { returnValue });
        return returnValue;
    }

    /**
     * Retireve and remove the first Entry in the Map that matches the key.
     * 
     * @param key of the Object to be removed from the map.
     * @return Entry matching the key, or null if there is none.
     * @throws ObjectManagerException
     */
    public synchronized Entry removeEntry(Object key)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "removeEntry",
                        new Object[] { key });

        Entry returnEntry = null;

        // Optionally, see if the key exists in this tree. If it does not exist
        // we could avoid trying to delete it, thus avoiding any unnecessary 
        // rearranging of the tree as we progress the deletion.
        // Find the Entry we want to delete.
        Entry entry = root.get(key);

        if (entry != null) {
            // Start the search for the deleted object at the root. Since the root
            // may contain less than minimumNodeSize-1 entries we don't check that it contains
            // sufficient Entries before entering it.
            returnEntry = root.delete(key);
        }

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "removeEntry",
                       new Object[] { returnEntry });
        return returnEntry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.AbstractMap#clear()
     */
    public void clear()
                    throws ObjectManagerException
    {
        root.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Collection#iterator()
     */
    public Iterator iterator()
                    throws ObjectManagerException
    {
        return values().iterator();
    } // iterator().

    /**
     * Create a new root Node.
     * 
     * @param newRoot to replace the existing one.
     * @throws ObjectManagerException
     */
    void setRoot(Node newRoot)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "setRoot",
                        new Object[] { newRoot });

        root = newRoot;

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "setRoot");
    } // setRoot().

    /**
     * Create a new Entry, can be overriden by a subClass to provide its own type
     * of Entry.
     * 
     * @param key establishing order within the map, must be either comparable or
     *            supported by the comparator provided on the BTree constructor.
     * @param value assoiciated with the key.
     * @return Entry that was created.
     * @throws ObjectManagerException
     * 
     */
    Entry makeEntry(Object key, Object value)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "makeEntry",
                        new Object[] { key, value });

        Entry returnEntry = new Entry(key, value);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "makeEntry",
                       new Object[] { returnEntry });
        return returnEntry;
    } // makeEntry().

    /**
     * Create a new leaf Node, can be overriden by a subClass to provide
     * the subClass' own Node.
     * 
     * @param parent of the created Node or null if it is to be the root.
     * @return Node that was created.
     * @throws ObjectManagerException
     */
    Node makeNode(Node parent)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "makeNode",
                        new Object[] { parent });

        Node returnNode = new Node(parent);

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "makeNode",
                       new Object[] { returnNode });
        return returnNode;
    } // makeEntry().

    /**
     * Compares two keys using the comparison method for this Map.
     * 
     * @param key1 the firsy key.
     * @param key2 the second key.
     * 
     * @return int a negative integer if the first argument is less than the second.
     *         int 0 if the keys are equal.
     *         int a positive integer if the first argument is greater than the second.
     */
    private int compare(Object key1,
                        Object key2)
    {
        return (comparator == null ? ((Comparable) key1).compareTo(key2) : comparator.compare(key1,
                                                                                              key2));
    }

    /**
     * Print a dump of the Map.
     * 
     * @param printWriter to be written to.
     */
    public synchronized void print(java.io.PrintWriter printWriter)
    {
        printWriter.println("Dump of BTree minimumNodeSize=" + minimumNodeSize + "(int)");

        try {
            for (Node.EntryIterator iterator = root.entryIterator(); iterator.hasNext();) {
                Entry entry = iterator.nextEntry();
                int[] index = iterator.getIndex();
                String indexLabel = "";
                for (int i = 0; i < index.length - 1; i++)
                    indexLabel = indexLabel + index[i] + ",";
                indexLabel = indexLabel + index[index.length - 1];
                printWriter.println((indexLabel + "                    ").substring(0, 20)
                                    + " Key=" + entry.getKey() + " Value=" + entry.getValue());
            } // for(iterator... 
        } catch (ObjectManagerException objectManagerException) {
            // No FFDC code needed.
            printWriter.println("Caught objectManagerException=" + objectManagerException);
            objectManagerException.printStackTrace(printWriter);
        } // try...

    } // print().

    /*
     * Validates that the BTree is:
     * 1) In the correct sort order.
     * 2) Has all of the leaf nodes at the same height from the root.
     * 
     * @return boolean true if the tree is not corrupt.
     */
    public synchronized boolean validate(java.io.PrintStream printStream)
                    throws ObjectManagerException
    {
        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.entry(this,
                        cclass,
                        "validate",
                        new Object[] { printStream });

        boolean valid = true; // Until proven otherwise.   
        int leafDepth = 0;

        Node.EntryIterator iterator = root.entryIterator();
        Entry previous = null;
        if (iterator.hasNext())
            previous = iterator.nextEntry();
        while (iterator.hasNext()) {
            Entry entry = iterator.nextEntry();

            // Check keys are returned in the sort order.
            if (compare(entry.getKey(), previous.getKey()) < 0) {
                valid = false;
                printStream.println("key=" + previous.getKey() + " < following key=" + entry.getKey());
            }
            previous = entry;

            Node child = entry.getChild();
            // Check tree is balanced, all leaf nodes must be the same distance from the root. 
            if (child == null) {
                int[] index = iterator.getIndex();
                if (leafDepth == 0)
                    leafDepth = index.length;
                if (leafDepth != index.length) {
                    valid = false;
                    String indexLabel = "";
                    for (int i = 0; i < index.length - 1; i++)
                        indexLabel = indexLabel + index[i] + ",";
                    indexLabel = indexLabel + index[index.length - 1];
                    printStream.println((indexLabel + "                    ").substring(0, 20)
                                        + " Key=" + entry.getKey() + " Value=" + entry.getValue()
                                        + " Leaf not at depth=" + leafDepth);
                }
            } else {
                // Check that leaf nodes have no children.
                if (child.isLeaf) {
                    for (Entry e = child.first; e != null; e = e.next) {
                        if (e.getChild() != null) {
                            valid = false;
                            printStream.println("Leaf Node " + child + " has a child=" + e.getChild() + " in entry=" + e);
                        }
                    } // for.
                } // if (child.isLeaf).
            } // if (entry.getChild() == null)
        } // while... 

        if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
            trace.exit(this,
                       cclass,
                       "validate",
                       new Object[] { new Boolean(valid) });
        return valid;
    } // method validate().

    // --------------------------------------------------------------------------
    // extends AbstractMap.
    // -------------------------------------------------------------------------

    // The Entry view of this Map, created the first time it is needed.
    private transient Set entrySet = null;

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.objectManager.Map#entrySet()
     */
    public Set entrySet()
    {
        if (entrySet == null) {
            entrySet = new AbstractSetView() {
                public Iterator iterator()
                                throws ObjectManagerException
                {
                    return root.entryIterator();
                }

                public long size(Transaction transaction)
                                throws ObjectManagerException
                {
                    throw new UnsupportedOperationException();
                }

                public long size()
                                throws ObjectManagerException
                {
                    return BTree.this.size();
                }
            };
        }
        return entrySet;
    } // entrySet().

    //----------------------------------------------------------------------------------------------
    // extends Object.
    // ----------------------------------------------------------------------------------------------

    /**
     * Short description of the object.
     * Overrides toString in AbstractMap, which contains the whole map.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return new String(cclass.getName()
                          + "/" + Integer.toHexString(System.identityHashCode(this)));
    } // toString().

    //----------------------------------------------------------------------------------------------
    // Inner classes
    // ----------------------------------------------------------------------------------------------

    /**
     * @author Andrew_Banks
     * 
     *         A node in the tree containing the sub tree below it.
     *         Each node has a chain of Entries which contain the keys and child nodes for the sub
     *         tree containing keys less than or younger than the key here. The final Entry
     *         contains they child node with keys greater or older than this one but no key or value.
     */
    class Node
    {
        private final Class cclass = BTree.Node.class;

        // The Node for which this Node is a child.
        // TODO Looks like parent is unnecessary.
        Node parent;
        // Number of Entrys stored at this node not icluding the final Entry in an intermediate node
        // which contains no key of value. 
        // Except in the root Node there must be at least minimumNodeSize -1,
        // and not greater than 2*minimumNodeSize - 1.
        int numberOfKeys = 0;
        // The Entrys held in this node sorted into non decreasing order.
        Entry first = null;
        // Entry's stored at this Node.
        // True if this Node has no child Nodes.
        boolean isLeaf;

        /**
         * This constructor creates an empty leaf node.
         * 
         * @param parent of this Node.
         */
        Node(Node parent)
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "<init>",
                            new Object[] { parent });

            this.parent = parent; // TODO We don't appear to need a parent!
            isLeaf = true;

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "<init>"
                                );
        } // Node().

        /**
         * @return long the number of Keys in this node and its descendants.
         * @throws ObjectManagerException
         */
        public long size()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "size");

            long size;
            Entry entry = first;

            if (isLeaf) {
                size = numberOfKeys;

            } else {
                // Allow for the final entry having no key value pair, just a child. 
                size = -1;
                while (entry != null) {
                    size = size + entry.getChild().size();
                    size = size + 1;
                    entry = entry.next;
                } // while... 
            }

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "size",
                           new Object[] { new Long(size) });
            return size;
        }

        /*
         * @return Entry oldest containing the key, or null if there is no match.
         */
        Entry get(Object key)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "get",
                            new Object[] { key });

            Entry returnEntry = null;
            Entry previous = null;
            Entry entry = first;
            while (entry != null) {
                if (entry.key == null) {
                    // Try the final greater than node in an internal Node. 
                    returnEntry = entry.getChild().get(key);
                    break;
                }

                int comparison = compare(key,
                                         entry.getKey());
                if (comparison == 0) {
                    returnEntry = entry;
                    break;

                } else if (comparison < 0) {
                    // entry is greater than the key, but the previous child might contain the key.
                    if (!isLeaf)
                        returnEntry = entry.getChild().get(key);
                    break;
                }

                previous = entry;
                entry = entry.next;
            } // while...

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "get",
                           new Object[] { returnEntry });
            return returnEntry;
        } // get().

        /**
         * Splits a full node into two nodes about its middle key. The greater than half remains here.
         * 
         * @return Entry in the the middle, chained to its child, the less than Node.
         * @throws ObjectManagerException
         */
        Entry split()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "split");

            // Find the last entry that will remain in this Node.
            Entry last = getEntry(minimumNodeSize - 2);
            Entry middle = last.next;

            // Cut out the less than half of this node.
            Node left = makeNode(parent);
            left.first = first;
            left.numberOfKeys = minimumNodeSize - 1;
            if (isLeaf) {
                last.next = null;
            } else {
                left.isLeaf = false;
                last.next = makeEntry(null, null);
                last.next.setChild(middle.getChild());
            }

            // What ramains here is the parent's right Node.
            numberOfKeys = minimumNodeSize - 1;
            first = middle.next;

            // Promote the mid entry into the parent, carrying the less than child.
            middle.setChild(left);
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "split",
                           new Object[] { middle });
            return middle;
        } // split().

        /**
         * Insert an Entry into the first position of this node.
         * 
         * @param entry to be insterted
         */
        void insertFirst(Entry entry)
        {
            entry.next = first;
            first = entry;
            numberOfKeys++;

        } // insertFirst().

        /**
         * Insert an Entry into the last position of this node.
         * 
         * @param entry to be insterted.
         * @param rightNode The rightmost Node, this will become the child of the new Entry
         * @throws ObjectManagerException
         */
        void insertLast(Entry entry, Node rightNode)
                        throws ObjectManagerException
        {

            // Find the entry before the insert point.
            Entry previous = first;
            for (int i = 0; i < numberOfKeys - 1; i++) {
                previous = previous.next;
            }

            if (isLeaf) {
                entry.setChild(null);
                entry.next = null;
            } else {
                // Take the following final greater than child as the entry's less than child.
                // rightNode becomes the final greater than child.
                entry.next = previous.next;
                entry.setChild(previous.next.getChild());
                entry.next.setChild(rightNode);
            }
            previous.next = entry;

            numberOfKeys++;

        } // insert().

        /**
         * Insert a new Entry into this Node, according to the key in the Entry.
         * The caller ensures that this node is not full when this method is called.
         * 
         * @param newEntry to be inserted.
         * @return Entry an existing Entry for the key contained in the new Entry, or null.
         * @throws ObjectManagerException
         */
        Entry insert(Entry newEntry)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "insert",
                            new Object[] { newEntry });

            // try{BTree.this.print(new java.io.PrintWriter(System.out,true));}catch(Exception e){};

            Entry replacedEntry = null;

            // Search for the Entry in this node which contains the sub Tree with keys greater than 
            // the newEntry.
            Entry previous = null;
            Entry entry = first;
            int comparison = 1;

            if (isLeaf) {
                // We are a leaf, possibly also the root.
                while (entry != null) {
                    comparison = compare(newEntry.getKey(),
                                         entry.getKey());
                    if (comparison <= 0)
                        break;
                    previous = entry;
                    entry = entry.next;
                } // while...
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this,
                                cclass,
                                "insert",
                                new Object[] { "isLeaf", new Integer(comparison) });

                if (previous == null) {
                    if (comparison == 0) {
                        replacedEntry = first;
                        newEntry.next = first.next;
                        first = newEntry;

                    } else {
                        newEntry.next = first;
                        first = newEntry;
                        numberOfKeys++;
                    }

                } else {
                    if (comparison == 0) {
                        replacedEntry = entry;
                        if (entry != null)
                            newEntry.next = entry.next;
                    } else {
                        newEntry.next = entry;
                        numberOfKeys++;
                    }

                    previous.next = newEntry;
                }

            } else {
                // This is an internal node with child nodes.
                while (entry.key != null) {
                    comparison = compare(newEntry.getKey(),
                                         entry.getKey());
                    if (comparison <= 0)
                        break;
                    previous = entry;
                    entry = entry.next;
                }
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this,
                                cclass,
                                "delete",
                                new Object[] { "!isLeaf", new Integer(comparison) });

                if (previous == null) {

                    if (comparison == 0) {
                        replacedEntry = first;
                        newEntry.next = first.next;
                        newEntry.setChild(first.getChild());
                        first = newEntry;
                        replacedEntry.next = null;
                        replacedEntry.setChild(null);

                    } else {
                        // Insert into child, less than node.
                        Node node = entry.getChild();
                        if (node.numberOfKeys == minimumNodeSize * 2 - 1) {
                            Entry promoted = node.split();
                            promoted.next = entry;
                            first = promoted;
                            numberOfKeys++;

                            // Decide whether to insert into the left right nodes or replace the Entry we just promoted.
                            comparison = compare(newEntry.getKey(),
                                                 promoted.getKey());
                            if (comparison < 0)
                                replacedEntry = first.getChild().insert(newEntry);

                            else if (comparison == 0) {
                                replacedEntry = promoted;
                                newEntry.next = entry;
                                first = newEntry;
                                newEntry.setChild(promoted.getChild());
                                replacedEntry.next = null;
                                replacedEntry.setChild(null);

                            } else
                                replacedEntry = promoted.next.getChild().insert(newEntry);

                        } else {
                            replacedEntry = node.insert(newEntry);
                        }
                    }

                } else {
                    // Not the first node.
                    if (comparison == 0) {
                        // Replacement of an internal node, not the first node.
                        replacedEntry = entry;

                        // TODO How can entry be null? Remove this test??
                        if (entry != null) {
                            newEntry.next = entry.next;
                            newEntry.setChild(entry.getChild());
                        }

                        previous.next = newEntry;
                        replacedEntry.next = null;
                        replacedEntry.setChild(null);

                    } else {
                        Node node = entry.getChild();

                        if (node.numberOfKeys == minimumNodeSize * 2 - 1) {
                            // Insufficient room in the node below for an insert, split the child Node.
                            Entry promoted = node.split();
                            promoted.next = entry;
                            previous.next = promoted;
                            numberOfKeys++;

                            // Decide whether to insert into the left right nodes or replace the Entry we just promoted.
                            comparison = compare(newEntry.getKey(),
                                                 promoted.getKey());
                            if (comparison < 0)
                                replacedEntry = promoted.getChild().insert(newEntry);

                            else if (comparison == 0) {
                                replacedEntry = promoted;
                                newEntry.next = entry;
                                previous.next = newEntry;
                                newEntry.setChild(promoted.getChild());
                                replacedEntry.next = null;
                                replacedEntry.setChild(null);

                            } else
                                replacedEntry = promoted.next.getChild().insert(newEntry);

                        } else {
                            // Sufficient room in the node below for an insert.
                            replacedEntry = node.insert(newEntry);
                        }
                    }
                } // if (previous == null). 

            } // (isLeaf).   

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "insert",
                           new Object[] { replacedEntry });
            return replacedEntry;

        } // insert().

        /**
         * Remove a Entry from this node or its descendants.
         * 
         * This descends into the map recursively searching for and acting on
         * nodes that potentially contain the given key. We must ensure that a Node has
         * more than minimumNumberOfKeys-1 before descending into it so that the child can
         * always sustain a deletion. Similrarly the caller must ensure we have sufficient keys to
         * sustain deletion before invoking this method.
         * 
         * @param key to be deleted.
         * @return Entry which was deleted or null if there is no Entry matching the key.
         * @throws ObjectManagerException
         */
        Entry delete(Object key)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "delete",
                            new Object[] { key, new Boolean(isLeaf) });

            // try{BTree.this.print(new java.io.PrintWriter(System.out,true));}catch(Exception e){};

            // The Entry we have found and removed.
            Entry returnEntry;

            // Find the Entry we want to delete, or the Node where the Entry might be found.
            Entry previousPrevious = null;
            Entry previous = null;
            Entry entry = first;
            int comparison = 1;

            if (isLeaf) {
                // Find the Entry that contains the key, if any.
                while (entry != null) {
                    comparison = compare(key, entry.getKey());
                    if (comparison <= 0)
                        break;
                    previous = entry;
                    entry = entry.next;
                } // while (entry != null ).
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this,
                                cclass,
                                "delete",
                                new Object[] { "isLeaf", new Integer(comparison) });

                if (comparison == 0) {
                    returnEntry = entry;

                    // Reduce the size of the leaf node, the higher Nodes will have ensured that we
                    // have more than minimumNodeSize - 1 keys.        
                    if (previous == null)
                        first = entry.next;
                    else {
                        previous.next = entry.next;
                    }
                    entry.next = null;
                    numberOfKeys--;
                } else {
                    // Key not found in this node. 
                    // We have searched all the way up the tree and found nothing.
                    returnEntry = null;
                }

            } else {
                // Internal Node.
                while (entry.key != null) {
                    comparison = compare(key, entry.getKey());
                    if (comparison <= 0)
                        break;
                    previousPrevious = previous;
                    previous = entry;
                    entry = entry.next;
                } // while (entry != null ).
                if (Tracing.isAnyTracingEnabled() && trace.isDebugEnabled())
                    trace.debug(this,
                                cclass,
                                "delete",
                                new Object[] { "!isLeaf", new Integer(comparison) });

                // Does this node contain the key?
                if (comparison == 0) {
                    returnEntry = entry;
                    // Key found in this internal non leaf Node.
                    Node leftNode = entry.getChild();

                    if (leftNode.numberOfKeys > minimumNodeSize - 1) {
                        // The less than child node has enough keys to sustain a deletion.
                        // Swap entry with the predecessor Entry and delete the key there.
                        Node preNode = leftNode;
                        while (!preNode.isLeaf) {
                            preNode = preNode.getEntry(preNode.numberOfKeys).getChild();
                        }
                        Entry preEntry = preNode.swapLast(returnEntry);
                        if (previous == null) {
                            first = preEntry;
                        } else {
                            previous.next = preEntry;
                        }

                        leftNode.delete(key);

                    } else if (entry.next.getChild().numberOfKeys > minimumNodeSize - 1) {
                        // The greater than child node has enough keys to sustain a deletion.
                        // Swap entry with the successor Entry and delete the key there.
                        Node rightNode = entry.next.getChild();
                        Node successorNode = entry.next.getChild();
                        while (!successorNode.isLeaf) {
                            successorNode = successorNode.first.getChild();
                        }
                        Entry successorEntry = successorNode.swapFirst(returnEntry);
                        if (previous == null) {
                            first = successorEntry;
                        } else {
                            previous.next = successorEntry;
                        }

                        rightNode.delete(key);

                    } else {
                        // Neither the greater than or less than child nodes have enough keys.
                        // Merge them together and also give them the node we want to delete.

                        // Drop the entry we will give to the less than node and also drop 
                        // the greater than node.
                        Entry nextEntry = entry.next; // TODO We dont appear to use this?
                        Node deleteNode = entry.getChild();
                        if (previous == null) {
                            first = entry.next;
                        } else {
                            previous.next = entry.next;
                        }
                        entry.mergeChildWithGreaterThanChild();
                        numberOfKeys--;

                        // Only the root may have less than minimumNumberOfKeys, it has now dopped to zero
                        // so promote the merged node to be the new root. 
                        if (numberOfKeys == 0)
                            root = deleteNode;
                        // deleteNode is now the child of entry.next.
                        deleteNode.delete(key);
                    }

                } else {
                    // Key not found in this node. 
                    // The entry.getChild() is the sub tree that contains the Entry we are searching for.
                    // Try further into the tree, assume that the key will be found, if it is not in the 
                    // tree we may perform some unnecessary rebalancing of nodes to ensure no nodes have 
                    // sufficient keys to sustain the delete.
                    Node deleteNode = entry.getChild();
                    Node leftNode = null;
                    if (previous != null)
                        leftNode = previous.getChild();

                    if (entry.getChild().numberOfKeys == minimumNodeSize - 1) {
                        // The child containing the key does not have enough keys to sustain a deletion.

                        if ((previous != null)
                            && (previous.getChild().numberOfKeys > minimumNodeSize - 1)) {
                            // Less than sibling node exists and has surplus Entries.
                            // Take an Entry from the less than Node and give our entry to our child so that it can 
                            // sustain deletion.
                            Entry fromLeft = previous.getChild().removeLastEntryAndFollowingNode();
                            Node leftChild;
                            if (previous.getChild().isLeaf)
                                leftChild = null;
                            else
                                leftChild = fromLeft.next.getChild();
                            fromLeft.next = entry;
                            fromLeft.setChild(previous.getChild());
                            if (previousPrevious == null)
                                first = fromLeft;
                            else
                                previousPrevious.next = fromLeft;

                            previous.setChild(leftChild);
                            entry.getChild().insertFirst(previous);

                            returnEntry = entry.getChild().delete(key);

                        } else if ((entry.next != null)
                                   && (entry.next.getChild().numberOfKeys > minimumNodeSize - 1)) {
                            // The greater than sibling node exists and it has surplus Entries.
                            // Take the first Entry from the greater than Node and give ours to the deleteNode.
                            Entry fromRight = entry.next.getChild().removeFirstEntryAndPrecedingNode();
                            fromRight.next = entry.next;
                            // Descend the entry to the previous Node. 
                            deleteNode.insertLast(entry, fromRight.getChild());
                            fromRight.setChild(deleteNode);
                            if (previous == null)
                                first = fromRight;
                            else
                                previous.next = fromRight;

                            returnEntry = deleteNode.delete(key);

                        } else if (entry.next != null) {
                            // No sibling Nodes of deleteNode have surplus Entries, the greater than
                            // sibling node exists so merge the deleteNode with that, 
                            // reducing the keys in this node by one.

                            // Drop the entry we gave to the less than node and also drop the greater than node.
                            Entry nextEntry = entry.next; // TODO We dont appear to use this?
                            if (previous == null)
                                first = entry.next;
                            else
                                previous.next = entry.next;
                            entry.mergeChildWithGreaterThanChild();
                            numberOfKeys--;

                            // Only the root may have less than minimumNumberOfKeys. If we are it and it has now 
                            // dopped to zero promote the merged node to be the new root. 
                            if (numberOfKeys == 0)
                                root = deleteNode;
                            // deleteNode is now the child of entry.next.
                            returnEntry = deleteNode.delete(key);

                        } else {
                            // No sibling Nodes of deleteNode have surplus Entries, merge the deleteNode
                            // with its less than sibling, which must exist since the greater than Node does not.
                            // Reducing the keys in this node by one.
                            // Drop the entry we gave to the less than node and also the delete Node, which follows it.
                            if (previousPrevious == null) {
                                first = entry;
                            } else {
                                previousPrevious.next = entry;
                            }
                            previous.mergeChildWithGreaterThanChild();
                            numberOfKeys--;

                            // Only the root may have less than minimumNumberOfKeys. If we are it and it has now 
                            // dopped to zero promote the merged node to be the new root. 
                            if (numberOfKeys == 0)
                                setRoot(entry.getChild());
                            returnEntry = entry.getChild().delete(key);

                        } // if rebalancing attempts. 

                    } else {
                        // Enough Entrys in the deleteNode to simply delete from there.
                        returnEntry = deleteNode.delete(key);
                    } // if (deleteNode.numberOfKeys == minimumNodeSize - 1).                   

                } // if (found).
            } // if (isLeaf).

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "delete",
                           new Object[] { returnEntry });
            return returnEntry;
        } // delete().

        /**
         * Remove the last Entry and the greater than Node following it. A new trailing Entry
         * is created to hold the child of the entry that is returned.
         * 
         * @return Entry that is removed.
         * @throws ObjectManagerException
         */
        Entry removeLastEntryAndFollowingNode()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "removeLastEntryAndFollowingNode");

            Entry entry = first;
            Entry previous = null;
            for (int i = 0; i < numberOfKeys - 1; i++) {
                previous = entry;
                entry = entry.next;
            }

            if (isLeaf) {
                previous.next = null;
            } else {
                previous.next = makeEntry(null, null);
                previous.next.setChild(entry.getChild());
                entry.setChild(null);
            }

            numberOfKeys--;

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "removeLastEntryAndFollowingNode",
                           new Object[] { entry, new Integer(numberOfKeys) });
            return entry;

        } // removeLastEntryAndFollowingNode().

        /**
         * Remove the first Entry and the less than Node it contains.
         * 
         * @return Entry that was removed.
         */
        Entry removeFirstEntryAndPrecedingNode()
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "removeFirstEntryAndPrecedingNode");

            Entry entry = first;
            first = first.next;
            numberOfKeys--;

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "removeFirstEntryAndPrecedingNode",
                           new Object[] { entry, new Integer(numberOfKeys) });
            return entry;
        } // removeFirstEntryAndPrecedingNode().

        /**
         * @param index of the Entry required.
         * @return Entry at the index.
         */
        Entry getEntry(int index)
        {
            Entry entry = first;
            for (int i = 0; i < index; i++)
                entry = entry.next;

            return entry;
        }

        /**
         * Place the Entry at the start of the list of Entry's and return the one currently there.
         * The next and child values are also swapped.
         * 
         * @param newEntry
         * @return Entry that is currently the first one in the node.
         * @throws ObjectManagerException
         */
        Entry swapFirst(Entry newEntry)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "swapFirst",
                            new Object[] { newEntry });

            Entry existing = first;
            Entry firstNext = newEntry.next;
            Node firstChild = newEntry.getChild();

            newEntry.next = first.next;
            newEntry.setChild(first.getChild());
            first = newEntry;

            existing.next = firstNext;
            existing.setChild(firstChild);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "swapFirst",
                           new Object[] { existing });
            return existing;
        } // swapFirst().

        /**
         * Place the Entry at the end of the list of Entry's and return the one currently there.
         * The next and child values are also swapped.
         * 
         * @param newEntry to be made the last (rightmost) Entry in this node.
         * @return the current last Entry.
         * @throws ObjectManagerException
         */
        Entry swapLast(Entry newEntry)
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "swapLast",
                            new Object[] { newEntry });

            Entry previous = null;
            Entry entry = first;
            while (entry.next != null) {
                previous = entry;
                entry = entry.next;
            }

            Entry next = newEntry.next;
            Node child = newEntry.getChild();

            newEntry.next = entry.next;
            newEntry.setChild(entry.getChild());
            if (previous == null) {
                first = newEntry;
            } else {
                previous.next = newEntry;
            }

            entry.next = next;
            entry.setChild(child);

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "swapLast",
                           new Object[] { entry });
            return entry;
        } // swapLast().

        final Node.EntryIterator entryIterator()
                        throws ObjectManagerException
        {
            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.entry(this,
                            cclass,
                            "entryIterator");

            Node.EntryIterator entryIterator = new EntryIterator();

            if (Tracing.isAnyTracingEnabled() && trace.isEntryEnabled())
                trace.exit(this,
                           cclass,
                           "entryIterator",
                           entryIterator);
            return entryIterator;
        } // entryIterator().

        /**
         * Clear this node and its descendants. Free all storage associated with nodes but not
         * the Entries themselves.
         * 
         * @throws ObjectManagerException
         */
        public void clear()
                        throws ObjectManagerException
        {
            if (isLeaf) {

            } else {
                // Allow for the final entry having no key value pair, just a child.
                for (Entry entry = first; entry != null; entry = entry.next) {
                    entry.getChild().clear();
                } // for... 
            }
            first = null;
            numberOfKeys = 0;
            isLeaf = true;
        } // clear().

        // ----------------------------------------------------------------------------------------------
        // Inner classes
        // ----------------------------------------------------------------------------------------------

        /**
         * Gives up Entries in the stored order for this node and its sub nodes.
         */
        private class EntryIterator
                        implements Iterator
        {

            // The Entry to return after we have exhausted the current sub tree.
            Entry current;
            // The entry previously return from next();
            Entry previous = null;
            // True if the previous Entry was found in this node. 
            boolean foundHere;
            // The subNode we are returning iteration Entries from.
            Node.EntryIterator currentChildNodeIterator;
            int index = 0;

            EntryIterator()
                throws ObjectManagerException
            {
                current = first;
                if (isLeaf) {
                    foundHere = true;
                    currentChildNodeIterator = null;
                } else {
                    foundHere = false;
                    currentChildNodeIterator = first.getChild().entryIterator();
                }

            } // EntryIterator().

            // Used by SubMapEntryIterator     
            //      EntryIterator(Entry first) {
            //        next = first;
            //      }

            public boolean hasNext()
                            throws ObjectManagerException
            {
                if (current == null
                    || (current.key == null && !currentChildNodeIterator.hasNext())) {
                    return false;

                } else {
                    return true;
                }
            }

            public boolean hasNext(Transaction transaction)
            {
                throw new UnsupportedOperationException();
            }

            final Entry nextEntry()
                            throws ObjectManagerException
            {

                //   if (modCount != expectedModCount)
                //      throw new ConcurrentModificationException();

                if (isLeaf) {
                    // We are a leaf.
                    if (current == null)
                        throw new java.util.NoSuchElementException();

                    previous = current;
                    current = current.next;
                    index++;

                } else {
                    // We are an internal node.
                    if (currentChildNodeIterator.hasNext()) {
                        previous = currentChildNodeIterator.nextEntry();
                        foundHere = false;

                    } else {
                        if (current == null)
                            throw new java.util.ConcurrentModificationException();
                        previous = current;
                        foundHere = true;
                        current = current.next;
                        if (current == null)
                            throw new java.util.NoSuchElementException();
                        else
                            currentChildNodeIterator = current.getChild().entryIterator();
                        index++;
                    }
                }

                return previous;
            } // nextEntry().

            public Object next()
                            throws ObjectManagerException
            {
                return nextEntry();
            }

            public Object next(Transaction transaction)
            {
                throw new UnsupportedOperationException();
            }

            public void remove()
                            throws ObjectManagerException
            {
                if (previous == null)
                    throw new IllegalStateException();
                // if (modCount != expectedModCount)
                //    throw new ConcurrentModificationException();
                // Check that it is still in the tree.
// TODO       if (lastReturned.left != null && lastReturned.right != null) 
//          next = lastReturned;
                // TODO Would be better to pass the entry to be removed rather than just the key.
                BTree.this.remove(previous.key);
                // expectedModCount++;
                previous = null;
            }

            public Object remove(Transaction transaction)
            {
                throw new UnsupportedOperationException();
            }

            /**
             * @return int[] the index into each node for the Entry containing the key. int[0] is the
             *         index in the root, int[int.length] is the index in the leaf.
             */
            protected int[] getIndex() {
                if (foundHere)
                    return new int[] { index - 1 };
                else {
                    int[] childIndex = currentChildNodeIterator.getIndex();
                    int[] returnIndex = new int[childIndex.length + 1];
                    returnIndex[0] = index;
                    for (int i = 0; i < childIndex.length; i++)
                        returnIndex[i + 1] = childIndex[i];

                    return returnIndex;
                }
            }
        }

    } // class Node.

    /**
     * A Container were an object with a given key is stored.
     * 
     * TODO Should extend Map.Entry not java.util.Map.Entry
     */
    class Entry
                    implements java.util.Map.Entry
    {
        private final Class cclass = BTree.Entry.class;

        private Object key;
        // The payload data associated with the key.
        private Object value;
        protected Entry next = null;
        // The node containing Entries with keys greater than or older than the key contained here.
        Node child = null;

        /**
         * Constructor.
         * 
         * @param key establishing order within the map, must be either comparable or
         *            supported by the comparator provided on the BTree constructor.
         * @param value associated with the key.
         */
        Entry(Object key,
              Object value)
        {
            this.key = key;
            this.value = value;
        } // Entry().

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Map.Entry#getKey()
         */
        public Object getKey()
        {
            return key;
        }

        public Object getValue()
        {
            return value;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.util.Map.Entry#setValue(java.lang.Object)
         */
        public Object setValue(Object value)
        {
            Object existingValue = this.value;
            this.value = value;
            return existingValue;
        }

        /**
         * @return Node the child Node to the right of this Entry holding greater than keys.
         * @exception ObjectManagerException
         */
        Node getChild()
                        throws ObjectManagerException
        {
            return child;
        } // getChild().

        /**
         * Set the child Node to the right of the Entry holding greater than keys.
         * 
         * @param child Node to set.
         */
        void setChild(Node child)
        {
            this.child = child;
        }

        /**
         * Merge our child with the child Node of its greater than neighbour.
         * This entry becomes the intervening Entry joining the two Nodes together.
         * The merged Node is created by adding this Entry and the folowing node on to this one, it
         * tis then attached the child of the following Entry, replacing its original node.
         * 
         * Used when both this node and the greater than Node the minimum minimumNodeSize - 1 keys.
         * After the merge this child Node has minimuNodeSize*2 -1 keys
         * and the child from the creater than node is dropped.
         * 
         * @throws ObjectManagerException
         */
        void mergeChildWithGreaterThanChild()
                        throws ObjectManagerException
        {
            Entry rightEntry = next;
            Node rightNode = next.getChild();
            Node mergeNode = getChild();
            Entry last = mergeNode.first;
            while (last.next != null
                   && last.next.key != null) {
                last = last.next;
            }

            next = rightNode.first;
            if (last.next != null) {
                // Take the child from the keyless Entry in the less than child. 
                setChild(last.next.getChild());
            } else {
                // Now in a Leaf Node, so clear the child.
                setChild(null);
            } // if (last.next != null ).  
            last.next = this;

            // Move the merged node to the child of the greater than node.
            rightEntry.setChild(mergeNode);
            mergeNode.numberOfKeys = minimumNodeSize * 2 - 1;

        } // mergeWithGreaterThanNode().

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        public String toString()
        {
            return new String("BTree.Entry"
                              + "(key=" + key + " value=" + value + ")"
                              + "/hashCode=" + Integer.toHexString(hashCode()));
        } // toString().
    } // Entry.
} // class BTree.
