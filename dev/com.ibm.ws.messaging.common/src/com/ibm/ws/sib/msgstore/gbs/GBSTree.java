package com.ibm.ws.sib.msgstore.gbs;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * A GBS Tree Descriptor and code to manipulate and search the tree.
 */
public class GBSTree
{

    /**
     * An Iterator that can be used to walk through elements in the tree.
     *
     * <p>If concurrent modifications take place while the iterator is
     * open they will be seen if the ordering permits.  For example,
     * assume all keys are integers.  If the Iterator is positioned on a
     * 50, an insert or delete of a 40 will not be visible.  An insert of
     * a 70 will add an extra future element to the scan.  A delete of a
     * 70 will remove a future element from the scan.</p>
     *
     * <p>The current implementation requires that the key of any object
     * on which the Iterator is positioned remain immutable.  In order to
     * find the next object in the index, the Iterator must know where
     * that object is located and also the traversal path to that object.
     * If an insert or delete causes the index to be rebalanced the
     * traversal path is potentially changed and position is
     * re-established by doing a full or partial search to find the next
     * key greater than the key on which the Iterator is currently
     * positioned.</p>
     *
     * <p>For example, an index contains {50, 60, 70, 80}.  The Iterator
     * is positioned on the 50, the 50 is removed, its key changed to 75,
     * and reinserted.  If these operations caused a structural
     * re-balancing of the index, a subsequent next() operation will cause
     * a full or partial search of the index in order to re-establish the
     * traversal path to the object on which the iterator is currently
     * positioned.  This search will use the key of the object on which
     * the Iterator is positioned.  If the key has been changed to 75, the
     * next value returned may be 60 (correct) or may be 80 (not correct)
     * depending on the extent of the search required to re-establish the
     * traversal path to the object on which the Iterator is currently
     * positioned.</p>
     */
    public interface Iterator
    {

        /**
         * Advances to the next element in the collection and returns it.
         *
         * @return the next element in the collection or null if there are
         *         no more elements.
         */
        public Object next();

        /**
         * Remove from the collection the last element returned by next().
         *
         * <p>If remove() is called before any call to next() then the
         * element to be deleted is undefined and a NoSuchElementException
         * is thrown.  If remove() is called after any call to next() then
         * the element to be deleted is well defined but may no longer exist
         * due to other modifications to the index.  If the element is still
         * there and is deleted, remove() returns true.  If the element is
         * no longer there to be deleted, remove() returns false.</p>
         *
         * @return true if the element was there to be deleted.
         *
         * @exception NoSuchElementException if next() was never called and
         *            the element to be deleted is therefore undefined.
         */
        public boolean remove();

        /**
         * Reset the Iterator to the beginning of the index.
         */
        public void reset();
    }

    /**
     * Construct.
     *
     * @param kFactor The GBS Tree k factor, which defines the maximum fringe
     *                imbalance allowed.  Valid values are 2, 4, 6, 8, 12, 16
     *                24, and 32.
     * @param nodeWidth The maximum number of keys that can be stored in one node.
     *                  Values must be between 3 and 2000, inclusive.  The algorithms
     *                  fail if the width is less than 3.  2000 is a somewhat
     *                  arbitrary upper bound.
     * @param insertComparator The comparator used to compare keys for insert
     *                         operations.  Duplicate keys are not allowed.  But
     *                         duplicate key values may be supported by supplying
     *                         an insert comparator that takes into account duplicate
     *                         keys and makes them appear to be unique by including
     *                         an extra field in the key value.
     * @param keyComparator The comparator used to compare key values only.  This
     *                      comparator should return 0 when the two key values
     *                      are equal.
     */
    public GBSTree(
                  int                   kFactor,
                  int                   nodeWidth,
                  java.util.Comparator  insertComparator,
                  java.util.Comparator  keyComparator)
    {
        _tZeroDepth = calcTZeroDepth(kFactor);
        _kFactor = kFactor;
        _nodeWidth = nodeWidth;
        _nodeMidPoint = (nodeWidth-1) / 2;
        _insertComparator = insertComparator;
        _deleteComparator = insertComparator;
        if (insertComparator == null)
            throw new IllegalArgumentException("insertComparator is null.");
        if (keyComparator == null)
            throw new IllegalArgumentException("keyComparator is null.");
        _localComparator = new SearchComparator(keyComparator);
        _dummyTopNode = new GBSNode(this, 0);
        if ( (nodeWidth < 3) || (nodeWidth > 2000) )
        {
            String x =
            "Invalid node width (" + nodeWidth + ").\n" +
            "Minimum required is 3.\n"                  +
            "Maximum (arbitrary) limit is 2000.";
            throw new IllegalArgumentException(x);
        }
    }

    /**
     * Tree types
     */
    public static final int GBS_TREE   = 12;
    public static final int T_TREE     = 15;

    /**
     * @return the tree type in case we ever decide to implement more than
     *         one type.
     */
    public int treeType()
    {
        return GBS_TREE;
    }

    /**
     * Return number of keys stored in a node in this index.
     */
    public int nodeWidth()
    {
        return _nodeWidth;
    }

    /**
     * Return number of items stored in this index.
     */
    public int size()
    {
        return _population;
    }

    public int optimisticFinds()
    {
        return _optimisticFinds;
    }

    public int optimisticInserts()
    {
        return _optimisticInserts;
    }

    public int optimisticInsertSurprises()
    {
        return _optimisticInsertSurprises;
    }

    public int optimisticDeletes()
    {
        return _optimisticDeletes;
    }

    public int optimisticDeleteSurprises()
    {
        return _optimisticDeleteSurprises;
    }

    public int pessimisticFinds()
    {
        return _pessimisticFinds;
    }

    public int pessimisticInserts()
    {
        return _pessimisticInserts;
    }

    public int pessimisticDeletes()
    {
        return _pessimisticDeletes;
    }

    public int nullPointerExceptions()
    {
        return _nullPointerExceptions;
    }

    public int optimisticDepthExceptions()
    {
        return _optimisticDepthExceptions;
    }

    /**
     * @return Node median compare point for any node in this index.
     */
    int nodeMidPoint()
    {
        return _nodeMidPoint;
    }

    /**
     * Return K factor for this tree
     */
    public int kFactor()
    {
        return _kFactor;
    }

    /**
     * Return the root node of the tree.
     */
    public GBSNode  root()
    {
        return dummyTopNode().rightChild();
    }

    /**
     * Return the dummy top node which is the parent of the root.
     */
    GBSNode  dummyTopNode()
    {
        return _dummyTopNode;
    }

    /**
     * Set the root of the tree.
     *
     * @param x The node which is to become the root.
     */
    private void setRoot(
                        GBSNode       x)
    {
        dummyTopNode().setRightChild(x);
    }

    /**
     * Used by test code to explicitly set a root of a tree.
     *
     * <p>Should not be used by normal code at all.</p>
     *
     * @param x The node which is to become the root.
     */
    public void testSetRoot(
                           GBSNode       x)
    {
        setRoot(x);
    }

    /**
     * Add the first node to the tree on insert.
     *
     * @param new1 The Object that constitutes the first key in the new node.
     */
    private void addFirstNode(
                             Object            new1)
    {
        GBSNode  p = getNode(new1);
        setRoot(p);
    }

    /**
     * Return the Comparator used for insert operations.
     */
    public java.util.Comparator insertComparator()
    {
        return _insertComparator;
    }

    /**
     * Return the Comparator used for delete operations.
     */
    public java.util.Comparator deleteComparator()
    {
        return _deleteComparator;
    }

    private java.util.Comparator _insertComparator;
    private java.util.Comparator _deleteComparator;

    /**
     * Find the search comparator needed for a search.
     *
     * @param type is the type of comparator desired, which must be one
     *             of SearchComparator.EQ, SearchComparator.GT, or
     *             SearchComparator.GE.
     *
     * @return The appropriate type of search comparator.
     */
    private SearchComparator searchComparator(
                                             int             type)
    {
        return _localComparator.getSingleton(type);
    }

    private SearchComparator      _localComparator;

    /**
    * The right child of this node points to the actual head of the tree
    * This is done to ensure that every node in the tree (including the
    * root) has a parent.  Makes height rebalancing simpler as it never
    * has to treat the root as a special case of a node with no parent.
    */
    private GBSNode  _dummyTopNode;

    private GBSNode  _nodePool;

    void releaseNode(
                    GBSNode       p)
    {
        p.setRightChild(_nodePool);
        _nodePool = p;
    }

    /**
     * Allocate a new node for the tree.
     * 
     * @param newKey The initial key for the new node.
     * 
     * @return The new node.
     */
    GBSNode getNode(Object newKey)
    {
        GBSNode   p;
        if (_nodePool == null)
            p = new GBSNode(this, newKey);
        else
        {
            p = _nodePool;
            _nodePool = p.rightChild();
            p.reset(newKey);
        }
        return p;
    }

    /**
     * Add some number of free nodes to the node pool for testing.
     *
     * @param x The number of nodes to add.
     */
    public void prePopulate(int x)
    {
        for (int i = 0; i < x; i++)
        {
            GBSNode p = new GBSNode(this);
            releaseNode(p);
        }
    }

    /**
     * Total index population
     */
    private int _population;

    /**
     * Version number used for optimistic locking.
     */
    private volatile int _vno;

    public int vno()
    {
        return _vno;
    }

    /**
     * This version number is incremented whenever a structural change
     * (fringe or height balance) is made to the index.  This is used
     * by GBSIterator to see if it safe to walk the tree to the next
     * key.
     */
    private volatile int _xno;

    public int xno()
    {
        return _xno;
    }

    private volatile int   _optimisticFinds;
    private          int   _pessimisticFinds;
    private volatile int   _optimisticInserts;
    private volatile int   _optimisticInsertSurprises;
    private          int   _pessimisticInserts;
    private volatile int   _optimisticDeletes;
    private volatile int   _optimisticDeleteSurprises;
    private          int   _pessimisticDeletes;
    private volatile int   _nullPointerExceptions;
    private volatile int   _optimisticDepthExceptions;

    /**
    * T factor (node width) is set by the constructor
    */
    private final int _nodeWidth;

    /**
    * Mid-point within node is set by the constructor
    */
    private final int _nodeMidPoint;

    /**
    * K factor (maximum fringe imbalance) is set by the constructor
    */
    private final int _kFactor;

    /**
     * Depth of a T0 (T-Zero) subtree is set by the constructor.
     */
    private final int _tZeroDepth;

    private static final boolean pessimisticNeeded = false;
    private static final boolean optimisticWorked  = true;

    /**
    * The maximum depth of a height balanced binary tree (as in AVL tree)
    * is
    *
    *                 1.4404 lg(N+2) - 0.328
    *
    * Where lg() represents log base 2 and N is the number of internal
    * nodes in the tree.  (See Knuth, Vol. 3, Section 6.2.3).
    *
    * We choose as an upper limit 2 giganodes (2,147,483,648) for N.
    * Thus, lg(N+2) = 32.  (1.4404 * 32) - 0.328 = 45.7648.  This gives a
    * maximum depth for any tree of 46.  Each tree also has a parent node
    * which gives us a total maximum depth of 47.
    */

    static final int maxDepth = 47;


    /* Here for historical reference only.  Fringe re-balance only            */
    /* supports K factors up to 32.                                           */
/*
  private static final int
                 _t0_d[] =         /. T0 Tree depths as a function          ./
                       {-1,-1, 0,  /.   2         of K factor               ./
                           -1, 1,  /.   4                                   ./
                           -1, 2,  /.   6                                   ./
                           -1, 2,  /.   8                                   ./
                     -1,-1,-1, 3,  /.  12                                   ./
                     -1,-1,-1, 3,  /.  16                                   ./
         -1,-1,-1,-1,-1,-1,-1, 4,  /.  24                                   ./
         -1,-1,-1,-1,-1,-1,-1, 4,  /.  32                                   ./
         -1,-1,-1,-1,-1,-1,-1,-1,  /.                                       ./
         -1,-1,-1,-1,-1,-1,-1, 5,  /.  48                                   ./
         -1,-1,-1,-1,-1,-1,-1,-1,  /.                                       ./
         -1,-1,-1,-1,-1,-1,-1, 5,  /.  64                                   ./
         -1,-1,-1,-1,-1,-1,-1,-1,  /.                                       ./
         -1,-1,-1,-1,-1,-1,-1,-1,  /.                                       ./
         -1,-1,-1,-1,-1,-1,-1,-1,  /.                                       ./
         -1,-1,-1,-1,-1,-1,-1, 6,  /.  96                                   ./
         -1,-1,-1,-1,-1,-1,-1,-1,  /.                                       ./
         -1,-1,-1,-1,-1,-1,-1,-1,  /.                                       ./
         -1,-1,-1,-1,-1,-1,-1,-1,  /.                                       ./
         -1,-1,-1,-1,-1,-1,-1, 6}; /. 128                                   ./
*/

    private static final int
    _t0_d[] =         /* T0 Tree depths as a function          */
    {-1,-1, 0,  /*   2         of K factor               */
        -1, 1,  /*   4                                   */
        -1, 2,  /*   6                                   */
        -1, 2,  /*   8                                   */
        -1,-1,-1, 3,  /*  12                                   */
        -1,-1,-1, 3,  /*  16                                   */
        -1,-1,-1,-1,-1,-1,-1, 4,  /*  24                                   */
        -1,-1,-1,-1,-1,-1,-1, 4}; /*  32                                   */


    /**
     * Return the depth of a T0 sub-tree, same as the depth of a fringe.
     */
    int tZeroDepth()
    {
        return _tZeroDepth;
    }

    /**
     * Return the maximum imbalance allowed for a fringe.
     *
     * <p>This depends on the K Factor and on whether or not the tree is
     * a special case of a single fringe.</p>
     */
    public int maximumFringeImbalance()
    {
        int maxBal;
        GBSNode q = root();
        if (q.leftChild() == null)
        {
            maxBal = kFactor() - 1;
            if (maxBal < 3)
                maxBal = 3;
        }
        else
        {
            if ((kFactor() % 3) == 0)
                maxBal = kFactor() + 2;
            else
                maxBal = kFactor() + 1;
        }
        return maxBal;
    }

    /**
     * Set the depth of a T0 sub-tree.
     */
    private int calcTZeroDepth(int proposedK)
    {
        int d = -1;
        if ( (proposedK >= 0) && (proposedK < _t0_d.length) )
            d = _t0_d[proposedK];
        if (d < 0)
        {
            String x =
            "K Factor (" + proposedK + ") is invalid.\n" +
            "Valid K factors are: " + kFactorString() + ".";
            throw new IllegalArgumentException(x);
        }
        return d;
    }

    /**
     * @return A String containing all legal K factors
     */
    private String kFactorString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("{");
        boolean first1 = true;
        for (int i = 0; i < _t0_d.length; i++)
        {
            if ( !(_t0_d[i] < 0) )
            {
                if (!first1)
                    buf.append(", ");
                buf.append(i + "");
                first1 = false;
            }
        }
        buf.append("}");
        return buf.toString();
    }

    /**
    * Process an exception that may have occurred while the index was
    * changing.
    *
    * <p>An optimistic search hit an exception that may have been caused by
    * a change in the index in mid-search.  The two parameters v1 and v2
    * represent the index vno at the time the search started and at the
    * time the exception was caught.  If the two are different then the
    * index changed in mid-search and we assume that was the reason for the
    * exception.  We return "pessimisticNeeded" which indicates that a
    * pessimistic search is now required.  If the index structure is,
    * indeed, damaged then the pessimistic search will encounter the same
    * exception.  If the two version numbers are the same then the index
    * did not change in mid-search and the exception is an indication of
    * either a programming error or a fatal corruption of the index.</p>
 
    * @param v1  The version number at the time the search started.
    * @param v2  The version number at the time the exception was caught.
    * @param exc The Exception that was caught.
    * @param msg A message to store with the new exception.
    *
    * @return pessimisticNeeded if the version numbers have changed and
    *         a pessimistic search is required.
    */
    static boolean checkForPossibleIndexChange(
                                              int             v1,
                                              int             v2,
                                              Throwable       exc,
                                              String          msg)
    {
        if (v1 != v2)
            return pessimisticNeeded;
        else
        {
            GBSTreeException x = new GBSTreeException(
                                                     msg + ", v1 = " + v1, exc);
            throw x;
        }
    }

    /**
     * Create and return an Iterator positioned on the beginning of the tree.
     *
     * <p>The Iterator is not actually positioned on the beginning of the
     * tree.  It has no position at all.  The first time Iterator.next() is
     * invoked it will position itself on the beginning of the tree.</p>
     */
    public Iterator iterator()
    {
        GBSIterator x = new GBSIterator(this);
        Iterator q = (Iterator) x;
        return q;
    }

    /**
     * Find the first key in the index that is equal to the given key.
     *
     * @param searchKey The key to be found.
     *
     * @return The first key in the index that is equal to the supplied key
     *         (searchKey).
     */
    public Object searchEqual(
                             Object            searchKey)
    {
        SearchComparator comp = searchComparator(SearchComparator.EQ);
        Object p = find(comp, searchKey);
        return p;
    }

    /**
     * Find the first key in the index that is greater than the given key.
     *
     * @param searchKey The key to be found.
     *
     * @return The first key in the index that is strictly greater than
     *         the supplied key (searchKey).
     */
    public Object searchGreater(
                               Object            searchKey)
    {
        SearchComparator comp = searchComparator(SearchComparator.GT);
        Object p = find(comp, searchKey);
        return p;
    }

    /**
     * Find the first key in the index that is greater than or equal to
     * the given key.
     *
     * @param searchKey The key to be found.
     *
     * @return The first key in the index that is greater than or equal to
     *         the supplied key (searchKey).
     */
    public Object searchGreaterOrEqual(
                                      Object            searchKey)
    {
        SearchComparator comp = searchComparator(SearchComparator.GE);
        Object p = find(comp, searchKey);
        return p;
    }


    private static ThreadLocal _searchNode = new ThreadLocal();

    /**
     * Find a SearchNode for use by the current thread.
     *
     * <p>Allocation of a SearchNode is more expensive than serial
     * reuse.  This is a very cheap form of pooling done by attaching a
     * SearchNode to each thread that calls find.</p>
     */
    private SearchNode getSearchNode()
    {
        Object x = _searchNode.get();
        SearchNode g = null;
        if (x != null)
        {
            g = (SearchNode) x;
            g.reset();
        }
        else
        {
            g = new SearchNode();
            x = (Object) g;
            _searchNode.set(x);
        }
        return g;
    }

    /**
     * Search the index.
     *
     * <p>Search the index to find one of the following:</p>
     * <ol>
     * <li>An index entry that is exactly equal to the supplied key,
     * <li>An index entry that is strictly greater than the supplied key, or
     * <li>An index entry that is greater than or equal to the supplied key.
     * </ol>
     *
     */
    private Object find(
                       SearchComparator  comp,
                       Object            searchKey)
    {
        SearchNode point = getSearchNode();
        Object ret = null;

        boolean gotit = optimisticFind(comp, searchKey, point);
        if (gotit == pessimisticNeeded)
            pessimisticFind(comp, searchKey, point);

        if (point.wasFound())
            ret = point.foundNode().key(point.foundIndex());

        return ret;
    }

    /**
     * Optimistic find in a GBS Tree
     *
     */
    private boolean optimisticFind(
                                  SearchComparator  comp,
                                  Object            searchKey,
                                  SearchNode        point)
    {
        point.reset();
        int v1 = _vno;
        if (root() != null)
        {
            if ((v1&1) != 0)
                return pessimisticNeeded;
            synchronized(point)
            {
            }

            try
            {
                internalFind(comp, searchKey, point);
            }
            catch (NullPointerException npe)
            {
                //No FFDC Code Needed.
                _nullPointerExceptions++;
                return checkForPossibleIndexChange(v1, _vno, npe, "optimisticInsert");
            }
            catch (OptimisticDepthException ode)
            {
                //No FFDC Code Needed.
                _optimisticDepthExceptions++;
                return checkForPossibleIndexChange(v1, _vno, ode, "optimisticInsert");
            }
            if (v1 != _vno)
                return pessimisticNeeded;
        }

        _optimisticFinds++;
        return optimisticWorked;
    }

    /**
     * Pessimistic find in a GBS Tree
     *
     */
    private void pessimisticFind(
                                SearchComparator  comp,
                                Object            searchKey,
                                SearchNode        point)
    {
        point.reset();

        synchronized(this)
        {
            internalFind(comp, searchKey, point);
            _pessimisticFinds++;
        }
    }


    /**
     * Search the index.
     *
     * <p>Search the index to find one of the following:</p>
     * <ol>
     * <li>An index entry that is exactly equal to the supplied key,
     * <li>An index entry that is strictly greater than the supplied key, or
     * <li>An index entry that is greater than or equal to the supplied key.
     * </ol>
     *
     */
    private void internalFind(
                             SearchComparator  comp,
                             Object            searchKey,
                             SearchNode        point)
    {
        GBSNode p = root();
        GBSNode l = null;
        GBSNode r = null;

        while (p != null)
        {
            int xcc = comp.compare(searchKey, p.middleKey());
            if (xcc == 0)
            {
                point.setFound(p, p.middleIndex());
                p = null;
            }
            else if (xcc < 0)
            {
                if (p.leftChild() != null)
                {
                    l = p;
                    p = p.leftChild();
                }
                else
                {
                    leftSearch(comp, p, r, searchKey, point);
                    p = null;
                }
            }
            else // (xcc > 0)
            {
                if (p.rightChild() != null)
                {
                    r = p;
                    p = p.rightChild();
                }
                else
                {
                    rightSearch(comp, p, l, searchKey, point);
                    p = null;
                }
            }
        }
    }


    /**
     * Search the index from an Iterator.
     *
     * <p>Search the index to find one of the following:</p>
     * <ol>
     * <li>An index entry that is exactly equal to the supplied key,
     * <li>An index entry that is strictly greater than the supplied key, or
     * <li>An index entry that is greater than or equal to the supplied key.
     * </ol>
     *
     * <p>This is called by the Iterator when the tree has had structural
     * modifications between calls to next().  The only safe way to
     * re-establish the position of the Iterator is to re-search the
     * tree.  If this is a next() operation then the Iterator is searching
     * for the key that is greater than the last key given out.</p>
     */
    synchronized Object iteratorFind(
                                    DeleteStack       stack,
                                    SearchComparator  comp,
                                    Object            searchKey,
                                    SearchNode        point)
    {
        point.reset();
        GBSNode p = root();
        GBSNode l = null;
        GBSNode r = null;
        int lx = 0;
        int rx = 0;
        Object ret = null;
        stack.start(dummyTopNode(), "GBSTree.iteratorFind");

        while (p != null)
        {
            int xcc = comp.compare(searchKey, p.middleKey());
            if (xcc == 0)
            {
                point.setFound(p, p.middleIndex());
                p = null;
            }
            else if (xcc < 0)
            {
                if (p.leftChild() != null)
                {
                    stack.push(NodeStack.PROCESS_CURRENT, p, "GBSTree.iteratorFind(1)");
                    l = p;
                    lx = stack.index();
                    p = p.leftChild();
                }
                else
                {
                    leftSearch(comp, p, r, searchKey, point);
                    if (point.wasFound())
                    {
                        if (point.foundNode() == r)
                            stack.reset(rx - 1);
                    }
                    p = null;
                }
            }
            else // (xcc > 0)
            {
                if (p.rightChild() != null)
                {
                    stack.push(NodeStack.DONE_VISITS, p, "GBSTree.iteratorFind(2)");
                    r = p;
                    rx = stack.index();
                    p = p.rightChild();
                }
                else
                {
                    rightSearch(comp, p, l, searchKey, point);
                    if (point.wasFound())
                    {
                        if (point.foundNode() == l)
                            stack.reset(lx - 1);
                    }
                    p = null;
                }
            }
        }

        if (point.wasFound())
        {
            ret = point.foundNode().key(point.foundIndex());
            point.setLocation(ret);
        }

        return ret;
    }

    /**
     * Search after falling off a left edge.
     *
     * <p>We have fallen off of the left edge because the search key is
     * less than the median of the current node.  In the example below if
     * the current node is GHI then the previous node is DEF and the
     * desired key is greater than E and less than H.  If the curent node
     * is MNO the previous node is JKL and the desired key is greater than
     * K and less than N.  If the current node is ABC there is no previous
     * node and the desired key is less than B.</p>
     *
     *<pre>
     *              *------------------J.K.L------------------*
     *              |                                         |
     *              |                                         |
     *              |                                         |
     *    *-------D.E.F-------*                     *-------P.Q.R-------*
     *    |                   |                     |                   |
     *    |                   |                     |                   |
     *    |                   |                     |                   |
     *  A.B.C               G.H.I                 M.N.O               S.T.U
     *</pre>
     *
     * <p>We now have two choices:</p>
     * <ol>
     * <li>Check to see if the search key is greater than the right-most
     *     key in the predecessor node.
     * <li>Check to see if the search key is greater than or equal to the
     *     left-most key in the current node.
     * </ol>
     * <p>If either of the above is true we need to search the left half
     * of the current node.  Otherwise we need to search the right half of
     * the predecessor node.  But this only is true if we are looking for
     * an exact match.  With an exact match it doesn't matter whether we
     * select the node to search by checking right-most key in predecessor
     * or left-most key in current.  But if we are looking for the first
     * index entry that is greater than the given key we must first check
     * the right-most key in the upper predecessor (if there is one) to
     * take care of the case that the key value lies between right-most in
     * upper predecessor and left-most in current.  For in this case we
     * must search left-most of current.</p>
     *
     * @param comp The index comparator used by the current search.
     * @param p The current node.
     * @param r The logical predecessor node, which is the one from which
     *          we last turned right.
     * @param searchKey The key we are trying to find.
     * @param point will contain the node and index of the found key
     *              (if any).
     *
     */
    private void leftSearch(
                           SearchComparator  comp,
                           GBSNode           p,
                           GBSNode           r,
                           Object            searchKey,
                           SearchNode        point)
    {
        if (r == null)               /* There is no upper predecessor         */
        {
            int idx = p.searchLeft(comp, searchKey);
            if ( !(idx < 0) )
                point.setFound(p, idx);
        }
        else                         /* There is an upper predecessor         */
        {
            int xcc = comp.compare(searchKey, r.rightMostKey());
            if (xcc == 0)            /* Target is right-most in upper pred.   */
                point.setFound(r, r.rightMostIndex());
            else if (xcc > 0)      /* Target (if it exists) is in left      */
            {
                /*  half of current node                 */
                int idx = p.searchLeft(comp, searchKey);
                if ( !(idx < 0) )
                    point.setFound(p, idx);
            }
            else                   /* Target (if it exists) is in right     */
            {
                /*  half of predecessor (if it exists)   */
                int idx = r.searchRight(comp, searchKey);
                if ( !(idx < 0) )
                    point.setFound(r, idx);
            }
        }
    }

    /**
     * Search after falling off a right edge.
     *
     * <p>We have fallen off of the right edge because the search key is
     * greater than the median of the current node.  In the example below
     * if the current node is GHI then the successor node is JKL and the
     * desired key is greater than H and less than K.  If the curent node
     * is MNO the successor node is PQR and the desired key is greater
     * than N and less than Q.  If the current node is STU there is no
     * successor node and the desired key is greater than T.</p>
     *
     *<pre>
     *              *------------------J.K.L------------------*
     *              |                                         |
     *              |                                         |
     *              |                                         |
     *    *-------D.E.F-------*                     *-------P.Q.R-------*
     *    |                   |                     |                   |
     *    |                   |                     |                   |
     *    |                   |                     |                   |
     *  A.B.C               G.H.I                 M.N.O               S.T.U
     *</pre>
     *
     * <p>We now have two choices:
     * <ol>
     * <li>Check to see if the search key is less than the left-most
     *     key in the successor node.
     * <li>Check to see if the search key is less than or equal to the
     *     right-most key in the current node.
     * </ol>
     * If either of the above is true we need to search the right half of
     * the current node.  Otherwise we need to search the left half of
     * the successor node.</p>
     *
     * @param comp The index comparator used by the current search.
     * @param p The current node.
     * @param l The logical successor node, which is the one from which
     *          we last turned left.
     * @param searchKey The key we are trying to find.
     * @param point will contain the node and index of the found key
     *              (if any).
     *
     */
    private void rightSearch(
                            SearchComparator  comp,
                            GBSNode           p,
                            GBSNode           l,
                            Object            searchKey,
                            SearchNode        point)
    {
        int xcc = comp.compare(searchKey, p.rightMostKey());
        if (xcc == 0)                /* Target is right-most key in current   */
            point.setFound(p, 0);
        else if (xcc < 0)            /* Target (if it exists) is in right     */
        {
            /*  half of current node                 */
            int idx = p.searchRight(comp, searchKey);
            if ( !(idx < 0) )
                point.setFound(p, idx);
        }
        else                         /* Target (if it exists) is in left      */
        {
            /*  half of successor (if it exists)     */
            if (l != null)
            {
                int idx = l.searchLeft(comp, searchKey);
                if ( !(idx < 0) )
                    point.setFound(l, idx);
            }
        }
    }

    private static ThreadLocal _insertStack = new ThreadLocal();

    /**
     * Find an InsertStack for use by the current thread.
     *
     * <p>Allocation of an InsertStack is more expensive than serial
     * reuse.  This is a very cheap form of pooling done by attaching an
     * InsertStack to each thread that calls insert.</p>
     */
    private InsertStack getInsertStack()
    {
        Object x = _insertStack.get();
        InsertStack g = null;
        if (x != null)
        {
            g = (InsertStack) x;
            g.reset();
        }
        else
        {
            g = new InsertStack(this);
            x = (Object) g;
            _insertStack.set(x);
        }
        return g;
    }

    /**
     * Insert into a GBS Tree.
     *
     * @param new1 The Object to be inserted.
     *
     * @return true if the Object was inserted, false if the Object was
     *         a duplicate of one already in the tree.
     */
    public boolean insert(
                         Object            new1)
    {
        boolean result;
        InsertStack stack = getInsertStack();
        if (root() == null)
            pessimisticInsert(stack, new1);
        else
        {
            boolean didit = optimisticInsert(stack, new1);
            if (didit == pessimisticNeeded)
                pessimisticInsert(stack, new1);
        }
        if (stack.isDuplicate())
            result = false;
        else
            result = true;

        return result;
    }


    /**
     * Optimistic insert into a GBS Tree.
     *
     * @param stack The InsertStack used to do the insert.
     * @param new1 The Object to be inserted.
     *
     * @return optimisticWorked if the insert worked, pessimisticNeeded if
     *         the insert failed due to interference from another thread.
     */
    private boolean optimisticInsert(
                                    InsertStack       stack,
                                    Object            new1)
    {
        InsertNodes  point = stack.insertNodes();
        int v1 = _vno;
        if (root() == null)
            return pessimisticNeeded;

        if ((v1&1) != 0)
            return pessimisticNeeded;
        synchronized(stack)
        {
        }

        try
        {
            findInsert(
                      point,
                      stack,
                      new1);
        }
        catch (NullPointerException npe)
        {
            //No FFDC Code Needed.
            _nullPointerExceptions++;
            return checkForPossibleIndexChange(v1, _vno, npe, "optimisticInsert");
        }
        catch (OptimisticDepthException ode)
        {
            //No FFDC Code Needed.
            _optimisticDepthExceptions++;
            return checkForPossibleIndexChange(v1, _vno, ode, "optimisticInsert");
        }
        synchronized(this)
        {
            if (v1 != _vno)
            {
                _optimisticInsertSurprises++;
                return pessimisticNeeded;
            }

            _optimisticInserts++;
            if (point.isDuplicate())
                stack.markDuplicate();
            else
            {
                _vno++;
                if ((_vno&1) == 1)
                {
                    finishInsert(point, stack, new1);
                    _population++;
                }
                synchronized(stack)
                {
                }
                _vno++;
            }
        } // synchronized(this)

        return optimisticWorked;
    }


    /**
     * Pessimistic insert into a GBS Tree.
     *
     * @param stack The InsertStack used to do the insert.
     * @param new1 The Object to be inserted.
     *
     * @return optimisticWorked as this version always works.
     */
    private synchronized boolean pessimisticInsert(
                                                  InsertStack       stack,
                                                  Object            new1)
    {
        _vno++;
        if ((_vno&1) == 1)
        {
            if (root() == null)
            {
                addFirstNode(new1);
                _population++;
            }
            else
            {
                InsertNodes  point = stack.insertNodes();
                findInsert(
                          point,
                          stack,
                          new1);
                if (point.isDuplicate())
                    stack.markDuplicate();
                else
                {
                    finishInsert(point, stack, new1);
                    _population++;
                }
            }
        }
        synchronized(stack)
        {
        }
        _vno++;
        _pessimisticInserts++;

        return optimisticWorked;
    }

    /**
     * Find the insert point within the tree.
     *
     * @param point Returned insert points.
     * @param stack The InsertStack of nodes traversed.
     * @param new1 The Object to be inserted.
     */
    private void findInsert(
                           InsertNodes       point,
                           InsertStack       stack,
                           Object            new1)
    {
        java.util.Comparator comp = insertComparator();
        NodeInsertPoint ip = stack.nodeInsertPoint();
        GBSNode       p;               /* P is used to march down the tree      */
        int           xcc;             /* Compare result                        */
        GBSNode       l_last = null;   /* Node from which we last departed by   */
                                       /*  turning left (logical successor)     */
        GBSNode       r_last = null;   /* Node from which we last departed by   */
                                       /*  turning right (logical predecessor)  */
        p = root();                    /* Root of tree                          */
                                       /* Remember father of root               */
        stack.start(dummyTopNode(), "GBSTree.findInsert");

        for (;;)                        /* Through the whole tree                */
        {
            xcc = comp.compare(new1, p.middleKey());
            if ( !(xcc > 0) )              /* New key <= node key                   */
            {
                GBSNode leftChild = p.leftChild();
                if (leftChild != null)     /* Have a left child                     */
                {
                    l_last = p;            /* Remember node from which we           */
                                           /*  last moved left (successor)          */
                    stack.balancedPush(NodeStack.PROCESS_CURRENT, p);
                    p = leftChild;         /* Move left                             */
                }
                else                       /* Have no left child                    */
                {
                    leftAdd(               /* Add a left child                      */
                                           p,             /* Current node                          */
                                           r_last,        /* Node from which we last turned right  */
                                           new1,          /* Key to add                            */
                                           ip,            /* NodeInsertPoint                       */
                                           point);        /* Returned InsertNodes                  */
                    break;
                }
            }
            else                           /* New key > node key                    */
            {
                GBSNode rightChild = p.rightChild();
                if (rightChild != null)    /* There is a right child                */
                {
                    r_last = p;            /* Remember node from which we           */
                                           /*  last moved right (predecessor)       */
                    stack.balancedPush(NodeStack.DONE_VISITS, p);
                    p = rightChild;        /* Move right                            */
                }
                else                       /* There is no right child               */
                {
                    rightAdd(              /* Add a right child                     */
                                           p,           /* Current node                          */
                                           l_last,      /* Node from which we last turned left   */
                                           new1,        /* Key to add                            */
                                           ip,          /* NodeInsertPoint                       */
                                           point);      /* Returned InsertNodes                  */
                    break;
                }
            }
        } // for
    }

    /**
     * Add to predecssor or current.
     *
     *<ol>
     *<li>There is no left child.
     *
     *<li>There might or might not be a right child
     *
     *<li>INSERT_KEY  <  MEDIAN of current node
     *
     *<li>INSERT_KEY  >  MEDIAN of predecessor (if there is one)
     *</ol>
     * <p>In the example below, if the current node is GHI, then the insert
     * key is greater than E and less than H. If the current node is MNO,
     * then the insert key is greater than K and less than N. If the
     * current node is ABC, the insert key is less than B and there is no
     * predecessor because there is no node from which we moved right.</p>
     *<pre>
     *
     *              *------------------J.K.L------------------*
     *              |                                         |
     *              |                                         |
     *              |                                         |
     *    *-------D.E.F-------*                     *-------P.Q.R-------*
     *    |                   |                     |                   |
     *    |                   |                     |                   |
     *    |                   |                     |                   |
     *  A.B.C               G.H.I                 M.N.O               S.T.U
     *
     *</pre>
     *
     * <p>We tried to move left and fell off the end.  If the key value is
     * greater than the high key of the predecessor or if there is no
     * predecessor, the insert point is in the left half of the current
     * node.  Otherwise, the insert point is in the right half of the
     * predecessor node.</p>
     *
     * @param p Current node from which we tried to move left
     * @param r Last node from which we actually moved right (logical predecessor)
     * @param new1 Key being inserted
     * @param ip Scratch NodeInsertPoint to avoid allocating a new one
     * @param point Returned insert points
     */
    private void leftAdd(
                        GBSNode           p,
                        GBSNode           r,
                        Object            new1,
                        NodeInsertPoint   ip,
                        InsertNodes       point)
    {
        if (r == null)                 /* There is no upper predecessor         */
            leftAddNoPredecessor(p, new1, ip, point);
        else                           /* There is an upper predecessor         */
            leftAddWithPredecessor(p, r, new1, ip, point);
    }

    /**
     * Add to left side with no predecessor present.
     *
     * <p>There is no upper predecessor.  If key exists it is in left part
     * of current node.</p>
     *
     * @param p Current node from which we tried to move left
     * @param new1 Key being inserted
     * @param ip Scratch NodeInsertPoint to avoid allocating a new one
     * @param point Returned insert points
     */
    private void leftAddNoPredecessor(
                                     GBSNode           p,
                                     Object            new1,
                                     NodeInsertPoint   ip,
                                     InsertNodes       point)
    {
        p.findInsertPointInLeft(new1, ip);
        point.setInsert(p, ip);
    }


    /**
     * Add to left side with an upper predecessor present.
     *
     * <p>There is an upper predecessor.  If the current key is greater
     * than the right-most key of the upper predecessor then the insert
     * point is in the left half of the current node.</p>
     * <p>Otherwise it is in the right part of the upper predecessor.  If
     * it is not a duplicate of some key in the upper predecessor we
     * insert the new key in the upper predecessor and migrate down the
     * right-most key in the upper predecessor.</p>
     *
     * @param p Current node from which we tried to move left
     * @param r Last node from which we actually moved right (logical predecessor)
     * @param new1 Key being inserted
     * @param ip Scratch NodeInsertPoint to avoid allocating a new one
     * @param point Returned insert points
     */
    private void leftAddWithPredecessor(
                                       GBSNode           p,
                                       GBSNode           r,
                                       Object            new1,
                                       NodeInsertPoint   ip,
                                       InsertNodes       point)
    {
        java.util.Comparator comp = r.insertComparator();
        int xcc = comp.compare(new1, r.rightMostKey());
        if (xcc > 0)                   /* If key exists it is in left part      */
        {
            /*  of current node                      */
            p.findInsertPointInLeft(new1, ip);
            point.setInsert(p, ip);
        }
        else                           /* If key exists it is in right part     */
        {
            /*  of predecessor node                  */
            r.findInsertPointInRight(new1, ip);
            if (ip.isDuplicate())      /* Key is a duplicate                    */
                point.setInsert(r, ip);
            else                       /* Key is not a duplicate                */
            {
                point.setInsertAndPosition(
                                          p,
                                          -1,
                                          r,
                                          ip.insertPoint());
                point.setRight();
            }

        }

    }

    /**
     * Add to current or successor.
     *
     *<ol>
     *<li>There is no right child
     *
     *<li>There may or may not be a left child.
     *
     *<li>INSERT_KEY  >  MEDIAN of current node.
     *
     *<li>INSERT_KEY  <  MEDIAN of successor (if there is one)
     *</ol>
     * <p>In the example below, if the current node is GHI, then the insert
     * key is greater than H and less than K. If the current node is
     * MNO, then the insert key is greater than N and less than Q. If
     * the current node is STU, the insert key is greater than T and
     * there is no successor.</p>
     *
     *<pre>
     *              *------------------J.K.L------------------*
     *              |                                         |
     *              |                                         |
     *              |                                         |
     *    *-------D.E.F-------*                     *-------P.Q.R-------*
     *    |                   |                     |                   |
     *    |                   |                     |                   |
     *    |                   |                     |                   |
     *  A.B.C               G.H.I                 M.N.O               S.T.U
     *</pre>
     *
     * <p>We tried to move right and fell off the end.  If the key value is
     * less than the low key of the successor or if there is no
     * successor, the insert point is in the right half of the current
     * node.  Otherwise, the insert point is in the left half of the
     * successor node.</p>
     *
     * @param p Current node from which we tried to move left
     * @param l Last node from which we actually moved left (logical successor)
     * @param new1 Key being inserted
     * @param ip Scratch NodeInsertPoint to avoid allocating a new one
     * @param point Returned insert points
     */
    private void rightAdd(
                         GBSNode           p,
                         GBSNode           l,
                         Object            new1,
                         NodeInsertPoint   ip,
                         InsertNodes       point)
    {
        if (l == null)                 /* There is no upper successor           */
            rightAddNoSuccessor(p, new1, ip, point);
        else                           /* There is an upper successor           */
            rightAddWithSuccessor(p, l, new1, ip, point);
    }

    /**
     * Add to right side with no upper successor present.
     *
     * <p>There is no upper successor.  If key exists it is in right part
     * of current node.  If curent node is less than half full there can
     * be no duplicate because the last compare that got us here was with
     * the right-most key in the current node.</p>
     *
     * @param p Current node from which we tried to move left
     * @param new1 Key being inserted
     * @param ip Scratch NodeInsertPoint to avoid allocating a new one
     * @param point Returned insert points
     */
    private void rightAddNoSuccessor(
                                    GBSNode           p,
                                    Object            new1,
                                    NodeInsertPoint   ip,
                                    InsertNodes       point)
    {
        if (p.lessThanHalfFull())      /* Node not even half full so the insert */
                                       /*  point now is high key of current     */
            point.setInsert(p, p.rightMostIndex());
        else                           /* Keys exist beyond the median          */
        {
            /* Must look for insert point            */
            p.findInsertPointInRight(new1, ip);
            point.setInsert(p, ip);
        }
    }

    /**
     * Add to right side with an upper successor present.
     *
     * <p>There is an upper successor.  If the current key is less than
     * the left-most key of the upper successor then the insert point is
     * in the right half of the current node.</p>
     * <p>Otherwise it is in the left part of the upper successor.  If it
     * is not a duplicate of some key in the upper successor we insert the
     * new key in the upper successor and migrate down the left-most key
     * in the upper successor.</p>
     *
     * @param p Current node from which we tried to move left
     * @param l Last node from which we actually moved left (logical successor)
     * @param new1 Key being inserted
     * @param ip Scratch NodeInsertPoint to avoid allocating a new one
     * @param point Returned insert points
     */
    private void rightAddWithSuccessor(
                                      GBSNode           p,
                                      GBSNode           l,
                                      Object            new1,
                                      NodeInsertPoint   ip,
                                      InsertNodes       point)
    {
        java.util.Comparator comp = l.insertComparator();
        int xcc = comp.compare(new1, l.leftMostKey());
        if (xcc < 0)                   /* If key exists it is in right part     */
        {
            /*  of current node                      */
            if (p.lessThanHalfFull())  /* Node not even half full so the insert */
                                       /*  point now is high key of current     */
                point.setInsert(p, p.rightMostIndex());
            else                       /* Keys exist beyond the median          */
            {
                /* Must look for insert point            */
                p.findInsertPointInRight(new1, ip);
                point.setInsert(p, ip);
            }
        }
        else                           /* If key exists it is in left part      */
        {
            /*  of upper successor                   */
            l.findInsertPointInLeft(new1, ip);
            if (ip.isDuplicate())      /* Key is a duplicate                    */
                point.setInsert(l, ip);
            else                       /* Key is not a duplicate                */
            {
                point.setInsertAndPosition(
                                          p,
                                          p.rightMostIndex(),
                                          l,
                                          ip.insertPoint());
                point.setLeft();
            }
        }
    }

    /**
     * Do the write phase of the insert.
     *
     * <p>findInsert does all of the reading needed to find the insert point.
     * finishInsert does all of the writing required to actually do the insert.
     * It is called by either optimisticInsert or pessimisticInsert as the
     * writing phase is the same regardless of the locking strategy.</p>
     *
     * @param point The insert point and position point.
     * @param stack The InsertStack of nodes traversed.
     * @param new1 The Object to be inserted.
     *
     */
    private void finishInsert(
                             InsertNodes       point,
                             InsertStack       stack,
                             Object            new1)
    {
        Object insertKey = new1;
        if (point.positionNode() != null)
        {
            GBSNode p = point.positionNode();
            int     ix = point.positionIndex();
            if (point.rightSide())
                insertKey = p.insertByRightShift(ix, insertKey);
            else
                insertKey = p.insertByLeftShift(ix, insertKey);
        }

        /* If the insert point is at the right-most slot in a full node         */
        /* then we migrate the new key into the fringe.                         */
        Object migrateKey = null;
        if (point.insertIndex() == point.insertNode().topMostIndex())
            migrateKey = insertKey;
        else                           /* Insert within existing node           */
            migrateKey =                 /* Which may migrate a key out of it     */
                                         point.insertNode().insertByRightShift(
                                                                              point.insertIndex(), insertKey);
        insertFringeMigrate(stack, point.insertNode(), migrateKey);
    }

    /**
     * Migrate a key from a partial leaf through to the proper place
     * in its fringe, adding a node to the right side of the fringe
     * if necessary.
     *
     * @param stack The stack that has been used to walk the tree
     * @param p     Node from which we are migrating
     * @param mkey  The migrating key.  This may be null in which case
     *              there is no key to migrate and we have nothing to do.
     */
    private void insertFringeMigrate(
                                    InsertStack       stack,
                                    GBSNode           p,
                                    Object            mkey)
    {
        GBSNode endp = p;
        int endIndex = stack.index();
        /* Maximum number of right child nodes   */
        /*  before the fringe must be rebalanced */
        int maxBal = maximumFringeImbalance();

        stack.setMigratingKey(mkey);
        if (mkey != null)              /* Have a key to migrate                 */
        {
            stack.processSubFringe(p); /* See InsertStack.processNode           */
            endp = stack.lastNode();
            endIndex = stack.lastIndex();
        }
        if (stack.migrating())         /* Need a right child for migrating key  */
        {
            _xno++;
            endp.addRightLeaf(stack.migratingKey());
        }

        insertCheckFringeBalance(
                                stack,
                                endp,
                                endIndex,
                                maxBal);

    }

    /**
     * Check to see if fringe balancing is necessary and do it if needed.
     *
     * @param stack    The stack that has been used to walk the tree
     * @param endp     Last node visited during fringe migration
     * @param endIndex The index within stack of endp
     * @param maxBal   Maximum fringe imbalance allowed for the tree
     */
    private void insertCheckFringeBalance(
                                         InsertStack       stack,
                                         GBSNode           endp,
                                         int               endIndex,
                                         int               maxBal)
    {
        GBSNode       fpoint = null;   /* This points to the first half leaf we */
                                       /*  find that has a right child but no   */
                                       /*  left child.  If any rebalancing is   */
                                       /*  to be done to a fringe, this will be */
                                       /*  the fringe balance point.            */

        int           fDepth = 0;      /* Fringe balance depth                  */

        int           fpidx = 0;       /* Index into NodeStack where fpoint was */
                                       /*  last saved                           */

        /* Check to see if we have filled out a final node, which may have      */
        /* filled out a fringe to the re-balance point.  The absence of a       */
        /* left child means it is part of a fringe.  If this is the case we     */
        /* walk backward through the tree looking for the top of the fringe.    */
        if ( (endp.isFull()) &&
             (endp.leftChild() == null) )
        {
            fDepth = 1;                /* Last node counts as one               */
            for (int j = endIndex; j > 0; j--)
            {
                GBSNode q = stack.node(j);
                if (q.leftChild() != null)/* Found the top of the fringe          */
                    break;
                else                     /* Not yet the top of the fringe         */
                {
                    fDepth++;            /* Bump fringe depth                     */
                    fpoint = q;          /* Remember possible fringe balance point*/
                    fpidx = j;           /*  and its index in the stack           */
                }
            }

            if (fDepth >= maxBal)
            {
                _xno++;
                GBSInsertFringe.singleInstance().balance(
                                                        kFactor(), stack, fpoint, fpidx, maxBal);
            }
        }
    }

    /**
     * Delete an Object from the tree and return true if the Object
     * was found.
     *
     * @param deleteKey Object to be deleted.
     *
     * @return true if the object was found.
     */
    public boolean delete(
                         Object            deleteKey)
    {
        return internalDelete(deleteKey);
    }

    private static ThreadLocal _deleteStack = new ThreadLocal();

    /**
     * Find a DeleteStack for use by the current thread.
     *
     * <p>Allocation of a DeleteStack is more expensive than serial reuse.
     * This is a very cheap form of pooling done by attaching a
     * DeleteStack to each thread that calls delete.</p>
     */
    private DeleteStack getDeleteStack()
    {
        Object x = _deleteStack.get();
        DeleteStack g = null;
        if (x != null)
        {
            g = (DeleteStack) x;
            g.reset();
        }
        else
        {
            g = new DeleteStack(this);
            x = (Object) g;
            _deleteStack.set(x);
        }
        return g;
    }

    /**
     * Deletes an Object from the tree and returns true if the Object
     * was found.
     *
     * <p>First tries optimisticDelete.  If this fails due to
     * concurrency, then does it with pessimisticDelete.</p>
     *
     * @param deleteKey Object to be deleted.
     *
     * @return true if the object was found.
     */
    private boolean internalDelete(
                                  Object            deleteKey)
    {
        boolean deleted = false;

        if (root() != null)
        {
            DeleteStack stack = getDeleteStack();

            boolean didit = optimisticDelete(stack, deleteKey);
            if (didit == pessimisticNeeded)
                pessimisticDelete(stack, deleteKey);

            DeleteNode point = stack.deleteNode();
            if (point.wasFound())
                deleted = true;
        }
        return deleted;
    }


    /**
     * Optimistic delete from a GBS Tree.
     *
     * @param stack The DeleteStack used to do the delete.
     * @param deleteKey The Object to be deleteed.
     *
     * @return optimisticWorked if the delete worked, pessimisticNeeded if
     *         the delete failed due to interference from another thread.
     */
    private boolean optimisticDelete(
                                    DeleteStack       stack,
                                    Object            deleteKey)
    {
        DeleteNode point = stack.deleteNode();
        int v1 = _vno;

        if ((v1&1) != 0)
            return pessimisticNeeded;

        synchronized(stack)
        {
        }

        try
        {
            findDelete(point, stack, deleteKey);
        }
        catch (NullPointerException npe)
        {
            //No FFDC Code Needed.
            _nullPointerExceptions++;
            return checkForPossibleIndexChange(v1, _vno, npe, "optimisticDelete");
        }
        catch (OptimisticDepthException ode)
        {
            //No FFDC Code Needed.
            _optimisticDepthExceptions++;
            return checkForPossibleIndexChange(v1, _vno, ode, "optimisticDelete");
        }
        synchronized(this)
        {
            if (v1 != _vno)
            {
                _optimisticDeleteSurprises++;
                return pessimisticNeeded;
            }

            _optimisticDeletes++;
            if (point.wasFound())
            {
                _vno++;
                if ((_vno&1) == 1)
                {
                    finishDelete(point, stack, deleteKey);
                    if (_population <= 0)
                        throw new GBSTreeException("_population = " + _population);
                    _population--;
                }
                synchronized(stack)
                {
                }
                _vno++;
            }
        }
        return optimisticWorked;
    }


    /**
     * Deletes an Object from the tree and returns true if the Object
     * was found.
     *
     * @param deleteKey Object to be deleted.
     *
     * @return true if the object was found.
     */
    private synchronized boolean pessimisticDelete(
                                                  DeleteStack       stack,
                                                  Object            deleteKey)
    {
        _pessimisticDeletes++;
        if (root() != null)
        {
            stack.reset();
            DeleteNode point = stack.deleteNode();

            _vno++;
            if ((_vno&1) == 1)
            {
                findDelete(point, stack, deleteKey);
                if (point.wasFound())
                {
                    finishDelete(point, stack, deleteKey);
                    if (_population <= 0)
                        throw new GBSTreeException("_population = " + _population);
                    _population--;
                }

                synchronized(stack)
                {
                }
                _vno++;
            }
        }

        return optimisticWorked;
    }

    /**
     * Allow an Iterator to delete an item by direct removal from a single
     * node.
     *
     * <p>The Iterator has determined that the item to be deleted resides
     * in a leaf node with more than one item in the node.  Removal of the
     * item from the leaf can be done without further modification to the
     * tree.  The Iterator has already synchronized on the index.</p>
     *
     * @param iter The Iterator itself which is used as the
     *             synchronization target for a void synchronization
     *             block.
     * @param node The node from which to delete.
     * @param index The index within the node of the item to delete.
     */
    void iteratorSpecialDelete(
                              Iterator       iter,
                              GBSNode        node,
                              int            index)
    {
        _vno++;
        if ((_vno&1) == 1)
        {
            node.deleteByLeftShift(index);
            node.adjustMedian();
            _population--;
        }
        synchronized(iter)
        {
        }
        _vno++;
    }

    /**
     * Do the write phase of the delete.
     *
     * <p>findDelete does all of the reading needed to find the delete point.
     * finishDelete does all of the writing required to actually do the delete.
     * It is called by either optimisticDelete or pessimisticDelete as the
     * writing phase is the same regardless of the locking strategy.</p>
     *
     * @param point The delete point and target point.
     * @param stack The Delete stack of nodes traversed.
     * @param deleteKey The Object to be deleted.
     *
     */
    private void finishDelete(
                             DeleteNode        point,
                             DeleteStack       stack,
                             Object            deleteKey)
    {
        adjustTarget(point);
        point.deleteNode().deleteByLeftShift(point.deleteIndex());

        deleteFringeMigrate(stack, point.deleteNode());
    }

    /**
     * Force the delete point to be in a leaf by migrating a leaf key
     * (the delete point) on top of the key to be deleted (the target
     * point).
     *
     * <p>If the key to be deleted is already in a leaf there is nothing
     * to do.</p>
     *
     * @param point The definition of the delete point and target point.
     */
    private void adjustTarget(
                             DeleteNode    point)
    {
        int s = point.targetType();
        GBSNode t = point.targetNode();
        GBSNode d = point.deleteNode();
        int    tx;

        switch (s)
        {
        case DeleteNode.NONE:
            /* Nothing to do.  Delete point is in a leaf.                       */
            break;
        case DeleteNode.ADD_LEFT:
            /* Migrate up high key of a leaf into its upper successor node.     */
            tx = point.targetIndex();
            t.addLeftMostKeyByDelete(tx, d.rightMostKey());
            break;
        case DeleteNode.ADD_RIGHT:
            /* Migrate up low key of a leaf into its upper predecessor node.    */
            tx = point.targetIndex();
            t.addRightMostKeyByDelete(tx, d.leftMostKey());
            break;
        case DeleteNode.OVERLAY_RIGHT:
            /* Migrate up low key of a leaf into high key of upper predecessor. */
            t.overlayRightMostKey(d.leftMostKey());
            break;
        case DeleteNode.OVERLAY_LEFT:
            /* Migrate up high key of a leaf into low key of upper successor.   */
            t.overlayLeftMostKey(d.rightMostKey());
            break;
        default:
            throw new RuntimeException("s = " + s);
        }
    }

    /**
     * Find the delete point within the tree.
     *
     * @param point The DeleteNode that will identify the delete point.
     * @param stack The DeleteStack that records nodes traversed.
     * @param deleteKey Object to be deleted.
     */
    private void findDelete(
                           DeleteNode        point,
                           DeleteStack       stack,
                           Object            deleteKey)
    {
        java.util.Comparator comp = deleteComparator();
        GBSNode       p;               /* P is used to march down the tree      */
        int           xcc;             /* Compare result                        */
        GBSNode       l_last = null;   /* Node from which we last departed by   */
                                       /*  turning left (logical successor)     */
        GBSNode       r_last = null;   /* Node from which we last departed by   */
                                       /*  turning right (logical predecessor)  */
        p = root();                    /* Root of tree                          */
                                       /* Remember father of root               */
        stack.start(dummyTopNode(), "GBSTree.findDelete");

        while (p != null)
        {
            xcc = comp.compare(deleteKey, p.middleKey());
            if (xcc == 0)                /* Delete key = median key               */
            {
                midDelete(stack, p, point);
                p = null;
            }
            else if (xcc < 0)            /* Delete key < median key               */
            {
                if (p.leftChild() != null)
                {
                    l_last = p;          /* Remember node from which we           */
                                         /*  last moved left (successor)          */
                    stack.push(NodeStack.PROCESS_CURRENT, p, "GBSTree.findDelete(1)");
                    p = p.leftChild();
                }
                else                     /* There is no left child                */
                {
                    /* Delete on left side                   */
                    leftDelete(p, r_last, deleteKey, point);
                    p = null;
                }
            }
            else                         /* Delete key > median key               */
            {
                if (p.rightChild() != null)
                {
                    r_last = p;          /* Remember node from which we           */
                                         /*  last moved right (predecessor)       */
                    stack.push(NodeStack.DONE_VISITS, p, "GBSTree.findDelete(2)");
                    p = p.rightChild();
                }
                else                     /* There is no right child               */
                {
                    rightDelete(p, l_last, deleteKey, point);
                    p = null;
                }
            }
        }
    }

    /**
     * Delete a key with an equal match.
     *
     * <p>During the search, we happened upon an equal match.  If there is
     * a predecessor at a lower level, we bring it up to fill the hole and
     * make it the delete key.  Otherwise, the one we have found is the
     * delete key already.</p>
     *
     */
    private void midDelete(
                          DeleteStack       stack,
                          GBSNode           p,
                          DeleteNode        point)
    {
        /* Assume no lower predecessor/successor */
        point.setDelete(p, p.middleIndex());
        GBSNode q = p.lowerPredecessor(stack);
        if (q != null)                 /* Have a lower predecessor              */
        {
            /* Elevate right-most of lower pred.     */
            point.setDelete(q, q.rightMostIndex());
            point.setTarget(p, p.middleIndex(), DeleteNode.ADD_LEFT);
        }
        else                           /* There is no lower predecessor         */
        {
            /* Check for lower successor             */
            q = p.lowerSuccessor(stack);
            if (q != null)             /* Have a lower successor                */
            {
                /* Elevate left-most of lower successor  */
                point.setDelete(q, 0);
                point.setTarget(p, p.middleIndex(), DeleteNode.ADD_RIGHT);
            }
        }
    }

    /**
     * Delete from current or predecessor.
     *
     *<ol>
     * <li>There is no left child.
     *
     * <li> There might or might not be a right child
     *
     * <li>DELETE_KEY  <  MEDIAN of current node
     *
     * <li>DELETE_KEY  >  MEDIAN of predecessor (if there is one)
     *</ol>
     *
     * <p>In the example below, if the current node is GHI, then the delete
     * key is greater than E and less than H. If the current node is MNO,
     * then the delete key is greater than K and less than N. If the
     * current node is ABC, the delete key is less than B and there is no
     * predecessor because there is no node from which we moved right.</p>
     *<pre>
     *              *------------------J.K.L------------------*
     *              |                                         |
     *              |                                         |
     *              |                                         |
     *    *-------D.E.F-------*                     *-------P.Q.R-------*
     *    |                   |                     |                   |
     *    |                   |                     |                   |
     *    |                   |                     |                   |
     *  A.B.C               G.H.I                 M.N.O               S.T.U
     *</pre>
     *
     * <p>We tried to move left and fell off the end.  If the key value is
     * greater than the high key of the predecessor or if there is no
     * predecessor, the delete key is in the left half of the current
     * node.  Otherwise, the delete key is in the right half of the
     * predecessor node.</p>
     *
     * @param p Current node from which we tried to move left
     * @param r Last node from which we actually moved right (logical predecessor)
     * @param deleteKey Key being deleted
     * @param point Returned delete point
     */
    private void leftDelete(
                           GBSNode           p,
                           GBSNode           r,
                           Object            deleteKey,
                           DeleteNode        point)
    {
        if (r == null)                 /* There is no upper predecessor         */
            leftDeleteNoPredecessor(p, deleteKey, point);
        else                           /* There is an upper predecessor         */
            leftDeleteWithPredecessor(p, r, deleteKey, point);
    }

    /**
     * Delete from left side with no upper predecessor.
     */
    private void leftDeleteNoPredecessor(
                                        GBSNode           p,
                                        Object            deleteKey,
                                        DeleteNode        point)
    {
        int idx = p.findDeleteInLeft(deleteKey);
        if (idx >= 0)
            point.setDelete(p, idx);
    }

    /**
     * Delete from left side with an upper predecessor present.
     */
    private void leftDeleteWithPredecessor(
                                          GBSNode           p,
                                          GBSNode           r,
                                          Object            deleteKey,
                                          DeleteNode        point)
    {
        java.util.Comparator comp = p.deleteComparator();
        int xcc = comp.compare(deleteKey, r.rightMostKey());
        if (xcc == 0)                  /* Target key IS predecessor high key    */
        {
            /* Migrate up low key of current (delete key)
               into high key of predecessor (target key)                        */
            point.setDelete(p, 0);
            point.setTarget(r, r.rightMostIndex(), DeleteNode.OVERLAY_RIGHT);
        }
        else if (xcc > 0)              /* Target key > predecessor high key     */
        {
            /* Target is in left part of current     */
            int ix = p.findDeleteInLeft(deleteKey);
            if ( !(ix < 0) )
                point.setDelete(p, ix);
        }
        else                           /* Target key < predecessor high         */
        {
            /* It is in right half of predecessor    */
            int ix = r.findDeleteInRight(deleteKey);
            if ( !(ix < 0) )
            {
                point.setTarget(r, ix, DeleteNode.ADD_RIGHT);
                point.setDelete(p, 0);
            }
        }
    }


    /**
     * Delete from current or successor.
     *
     *<ol>
     * <li>There is no right child
     *
     * <li>There may or may not be a left child.
     *
     * <li>DELETE_KEY  >  MEDIAN of current node.
     *
     * <li>DELETE_KEY  <  MEDIAN of successor (if there is one)
     *</ol>
     * <p>In the example below, if the current node is GHI, then the delete
     * key is greater than H and less than K. If the current node is
     * MNO, then the delete key is greater than N and less than Q. If
     * the current node is STU, the insert key is greater than T and
     * there is no successor.</p>
     *
     *<pre>
     *              *------------------J.K.L------------------*
     *              |                                         |
     *              |                                         |
     *              |                                         |
     *    *-------D.E.F-------*                     *-------P.Q.R-------*
     *    |                   |                     |                   |
     *    |                   |                     |                   |
     *    |                   |                     |                   |
     *  A.B.C               G.H.I                 M.N.O               S.T.U
     *</pre>
     * <p>We tried to move right and fell off the end.  If the key value is
     * less than the low key of the successor or if there is no successor,
     * the delete key (if any) is in the right half of the current node.
     * Otherwise, the delete key (if any) is in the left half of the
     * successor node.</p>
     *
     * @param p Current node from which we tried to move left
     * @param l Last node from which we actually moved left (logical successor)
     * @param deleteKey Key being deleted
     * @param point Returned delete point
     */
    private void rightDelete(
                            GBSNode           p,
                            GBSNode           l,
                            Object            deleteKey,
                            DeleteNode        point)
    {
        if (l == null)                 /* There is no upper successor           */
            rightDeleteNoSuccessor(p, deleteKey, point);
        else                           /* There is an upper successor           */
            rightDeleteWithSuccessor(p, l, deleteKey, point);
    }

    /**
     * Delete from right side with no successor.
     */
    private void rightDeleteNoSuccessor(
                                       GBSNode           p,
                                       Object            deleteKey,
                                       DeleteNode        point)
    {
        int idx = p.findDeleteInRight(deleteKey);
        if (idx >= 0)
            point.setDelete(p, idx);
    }

    /**
     * Delete from right side with successor node present.
     */
    private void rightDeleteWithSuccessor(
                                         GBSNode           p,
                                         GBSNode           l,
                                         Object            deleteKey,
                                         DeleteNode        point)
    {
        java.util.Comparator comp = p.deleteComparator();
        int xcc = comp.compare(deleteKey, l.leftMostKey());

        if (xcc == 0)                /* Target key IS low key of successor    */
        {
            /* Migrate up high key of current (delete key)
                into low key of successor (target key)                  */
            point.setDelete(p, p.rightMostIndex());
            point.setTarget(l, 0, DeleteNode.OVERLAY_LEFT);
        }
        else if (xcc < 0)            /* Less than left most key of successor  */
        {
            /* Target is in right of current         */
            int ix = p.findDeleteInRight(deleteKey);
            if (ix >= 0)
                point.setDelete(p, ix);
        }
        else                        /* Greater than left most of successor   */
        {
            /* Target is in left of successor        */
            int ix = l.findDeleteInLeft(deleteKey);
            if (ix >= 0)
            {
                /* Migrate up high key of current (delete key) into
                    low key of successor (hole left after keys moved)           */
                point.setDelete(p, p.rightMostIndex());
                point.setTarget(l, ix, DeleteNode.ADD_LEFT);
            }
        }
    }

    /**
     * Migrate a hole from a partial leaf through to the proper place
     * in its fringe, deleting a node from the right side of the fringe
     * if necessary.
     *
     * <p>This may cause a fringe t0 tree to degenerate to a linear list
     * which, in turn, will trigger a height rebalancing.</p>
     *
     * @param tree  The index we are using
     * @param stack The stack that has been used to walk the tree
     * @param p     Node from which we are migrating.  Its right-most
     *              slot is the hole being migrated.
     */
    private void deleteFringeMigrate(
                                    DeleteStack   stack,
                                    GBSNode       p)
    {
        GBSNode endp = p;              /* Last node processed                   */
        int endIndex = stack.index();  /* Index to last parent                  */
        stack.add(0, p);               /* Put last node in stack for possible   */
                                       /*  use by rebalance                     */
                                       /* Maximum number of right child nodes   */
                                       /*  before the fringe must be rebalanced */
        int maxBal = maximumFringeImbalance();

        stack.processSubFringe(p);     /* See DeleteStack.processNode           */

        endp = stack.lastNode();
        endIndex = stack.lastIndex();
        deleteCheckFringeBalance(
                                stack,
                                endp,
                                endIndex,
                                maxBal);

    }

    /**
     * Check to see if fringe balancing is required following a delete
     * and do the fringe rebalancing if needed.
     */
    private void deleteCheckFringeBalance(
                                         DeleteStack   stack,
                                         GBSNode       endp,
                                         int           endIndex,
                                         int           maxBal)
    {

        int           fDepth = 0;      /* Fringe balance depth                  */

        fDepth = 0;
        if (endp.leftChild() == null)
        {
            fDepth = 1;                /* Last node counts as one               */
            for (int j = endIndex; j > 0; j--)
            {
                GBSNode q = stack.node(j);
                if (q.leftChild() == null)/* Have not found end yet               */
                    fDepth++;
                else                     /* Found the top of the fringe           */
                    break;
            }
        }

        int t0_depth = tZeroDepth();
        int t = t0_depth;
        if (t < 1)
            t = 1;

        if ( (stack.maxDepth() >= t) &&/* Deep enough for a t0 and              */
             (endp.population() == (endp.width()-1))  )/* Last node was full   */
        {
            /* Check for a possible t0 tree          */
            int j = 1;
            if ((kFactor() % 3) == 0)
                j = 2;
            if (fDepth == j)
            {
                _xno++;
                GBSDeleteFringe.singleInstance().balance(
                                                        tZeroDepth(), stack);
            }
        }
        else                          /* Not a t0 fringe                       */
        {
            /* Check for empty node                  */
            if (endp.population() != 0)
                endp.adjustMedian();
            else
            {
                _xno++;
                GBSNode p = stack.node(endIndex);
                if (p.leftChild() == endp)
                    p.setLeftChild(null);
                else
                    p.setRightChild(null);
            }
        }
    }

}
