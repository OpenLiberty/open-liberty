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
 * A GBS Tree Iterator
 *
 */
public class GBSIterator implements GBSTree.Iterator
{
    /**
     * Used to remember the location of a position in the index.
     */
    private static class Locator
    {
        /**
         * Reset to post-construction state.
         */
        private void reset()
        {
            _node = null;
            _index = -6;
            _obj = null;
            _vno = 0;
            _xno = 0;
        }

        /**
         * Remember the location of a found key along with the index version
         * numbers associated with this find operation.
         *
         * @param obj The object that was found in the index.
         * @param vno The value of vno at the time the object was found.
         * @param xno The value of xno at the time the object was found.
         */
        private void setLocation(
                                Object      obj,
                                int         vno,
                                int         xno)
        {
            _obj = obj;
            _vno = vno;
            _xno = xno;
        }

        /**
         * Remember the node and index of a found key.
         *
         * @param node The node that contains the found key.
         * @param index The index within the node of the found key.
         */
        private void setLocation(
                                GBSNode     node,
                                int         index)
        {
            _node = node;
            _index = index;
        }

        /**
         * Remember the location of a found key along with the index version
         * numbers associated with this find operation.
         *
         * @param loc The Locator that defines the found key.
         */
        private void setLocation(
                                Locator     loc)
        {
            _node  = loc._node;
            _index = loc._index;
            _obj   = loc._obj;
            _vno   = loc._vno;
            _xno   = loc._xno;
        }

        /**
         * Save the version number
         */
        private void setVersion(
                               int         vno)
        {
            _vno = vno;
        }

        /**
         * Return the reference to the contained Object if any.
         */
        private Object key()
        {
            return _obj;
        }

        /**
         * Return the contained node (if any).
         */
        private GBSNode node()
        {
            return _node;
        }

        /**
         * Return the index within the contained node.
         */
        private int index()
        {
            return _index;
        }

        public String toString()
        {
            String x =
            "node = " + _node + "\n" +
            "  " +
            "index = " + _index + ", " +
            "vno = "   + _vno   + ", " +
            "xno = "   + _xno;
            return x;
        }

        private GBSNode  _node;
        private int      _index;
        private Object   _obj;
        private int      _vno;
        private int      _xno;
    }


    /**
     * Default construction not allowed
     */
    private GBSIterator()
    {
    }

    /**
     * Standard constructor is package private.
     */
    GBSIterator(
               GBSTree       index)
    {
        _index    = index;
        _dstack   = new DeleteStack(index);
        _current1 = new Locator();
        _last1    = new Locator();
    }

    /**
     * Reset to post construction state.
     */
    public void reset()
    {
        _dstack.reset();
        _current1.reset();
        _last1.reset();
        _eof = false;
        _s = 0;
        _p = null;
    }

    /**
     * Find the first key in the index.
     *
     * <p>Start by using a non-locking, optimistic search.  If this
     * doesn't work then switch to a pessimistic search which locks the
     * index.</p>
     *
     * @param stack The stack to use to record the traversal.
     *
     * @return The first entry in the index.
     */
    private void findFirst(
                          DeleteStack    stack)
    {
        boolean x = optimisticFindFirst(stack);
        if (x == pessimisticNeeded)
            pessimisticFindFirst(stack);
    }


    /**
     * Find first key in index using optimistic locking
     *
     * @param stack The stack to use to record the traversal
     */
    private boolean optimisticFindFirst(
                                       DeleteStack    stack)
    {
        Object q = null;

        int v1 = _index.vno();
        int x1 = _index.xno();

        if ((v1&1) != 0)
            return pessimisticNeeded;
        synchronized(this)
        {
        }

        try
        {
            q = getFirst(stack);
        }
        catch (NullPointerException npe)
        {
            //No FFDC Code Needed.
            _nullPointerExceptions++;
            return GBSTree.checkForPossibleIndexChange(v1, _index.vno(), npe, "optimisticFindFirst");
        }
        catch (OptimisticDepthException ode)
        {
            //No FFDC Code Needed.
            _optimisticDepthExceptions++;
            return GBSTree.checkForPossibleIndexChange(v1, _index.vno(), ode, "optimisticFindFirst");
        }

        if (v1 != _index.vno())
            return pessimisticNeeded;

        _current1.setLocation(q, v1, x1);
        _optimisticFindFirsts++;

        return optimisticWorked;
    }

    /**
     * Find first key in index using pessimistic locking.
     *
     * @param stack The stack to use to record the traversal
     */
    private void pessimisticFindFirst(
                                     DeleteStack    stack)
    {
        Object q = null;
        int v1 = 0;
        int x1 = 0;

        synchronized(_index)
        {
            q = getFirst(stack);
            v1 = _index.vno();
            x1 = _index.xno();
            _pessimisticFindFirsts++;
        }

        _current1.setLocation(q, v1, x1);
    }

    /**
     * Return first key in the index in key order, if any.
     *
     * <p>This also sets the node and index in _current1 if a key
     * is found.</p>
     *
     * @param stack The stack to use to record the traversal.
     *
     * @return The first key in the index in key order or null
     *         if the index is empty.
     */
    private Object getFirst(
                           DeleteStack    stack)
    {
        Object q = null;
        GBSNode n = leftMostChild(stack);
        if (n != null)
        {
            q = n.leftMostKey();
            _current1.setLocation(n, 0);
        }
        return q;
    }

    /**
     * Find the left-most child of the tree by following all of the
     * left children down to the bottom of the tree.
     *
     * @param stack The DeleteStack that is used to record the traversal.
     *
     * @return The left most child at the bottom of the tree or null
     *         if the tree is empty.
     *
     * @exception OptimisticDepthException if the depth of the traversal
     *            exceeds GBSTree.maxDepth.
     */
    private GBSNode leftMostChild(
                                 DeleteStack    stack)
    {
        GBSNode       p;
        p = _index.root();             /* Root of tree                          */
        GBSNode lastl = null;          /* Will point to left-most child         */
        if (p != null)                 /* Root is not null, we have a tree      */
        {
            /* Remember father of root               */
            stack.start(_index.dummyTopNode(), "GBSIterator.leftMostChild");
            lastl = leftMostChild(stack, p);
        }

        return lastl;
    }

    /**
     * Find the left-most child of a node by following all of the
     * left children down to the bottom of the tree.
     *
     * @param stack The DeleteStack that is used to record the traversal.
     * @param p The node at which to start the traversal.
     *
     * @return The left most child at the bottom of the tree or null
     *         if the tree is empty.
     *
     * @exception OptimisticDepthException if the depth of the traversal
     *            exceeds GBSTree.maxDepth.
     */
    private GBSNode leftMostChild(
                                 DeleteStack    stack,
                                 GBSNode        p)
    {
        GBSNode q = p.leftChild();
        while (q != null)
        {
            if (stack.index() > GBSTree.maxDepth)
                throw new OptimisticDepthException(
                                                  "maxDepth (" + GBSTree.maxDepth +
                                                  ") exceeded in GBSIterator.leftMostChild().");
            stack.push(NodeStack.PROCESS_CURRENT, p, "GBSIterator.leftMostChild");
            p = q;
            q = p.leftChild();
        }

        _p = p;
        _s = NodeStack.VISIT_RIGHT;

        return _p;
    }

    /**
     * Find the next key in the index.
     *
     * <p>If _eof is false the iterator has not reached the end of the
     * index and we can move forward in the normal way.  But if _eof is
     * true then the iterator has hit the end of the index and has lost
     * its position.  In this case we have to search the index for a key
     * that is greater than the last one returned iff the index has
     * changed since the last call to next().  If the index has not
     * changed then no keys have been added and the iterator is still
     * stuck at the end of the index.</p>
     *
     * <p>On return the information about the found entry (if any) is
     * stored in _current1.</p>
     *
     * @param stack The stack to use to record the traversal.
     *
     */
    private void findNext(
                         DeleteStack    stack)
    {
        if (_last1.key() == null)
            throw new RuntimeException("Help!  In findNext(), _last1.key() == null");

        if ( !_eof )
            findNextBeforeEof(stack);
        else
            findNextAfterEof(stack);
    }

    /**
     * Find the next key in the index.
     *
     * <p>Start by using a non-locking, optimistic search.  If this
     * doesn't work then switch to a pessimistic search which locks the
     * index.  The optimistic search used depends on whether or not vno
     * has changed since the last key was given out.  If it has not
     * changed use optimisticGetNext1().  Otherwise use
     * optimistGetNext2().</p>
     *
     * <p>On return the next entry in the index (if any) is stored
     * in _current1.<p>
     *
     * @param stack The stack to use to record the traversal.
     */
    private void findNextBeforeEof(
                                  DeleteStack    stack)
    {
        boolean state = pessimisticNeeded;

        if (_last1._vno == _index.vno())
            state = optimisticGetNext1(stack);
//    else // vno (and perhaps xno) changed
//      state = optimisticGetNext2(stack);
        if (state == pessimisticNeeded)
            pessimisticGetNext(stack);
    }

    /**
     * Find the next key in the index after an eof condition.
     *
     * <p>The vno from the time of the eof condition is stored in
     * _current1.  If the vno in the index has not changed then the
     * iterator is still stuck at the end and there is nothing to do.  If
     * the vno in the index has changed then we do an optimistic search to
     * re-establish position followed by a pessimistic search if the
     * optimistic search failed.</p>
     */
    private void findNextAfterEof(
                                 DeleteStack    stack)
    {
        if ( !_eof )
            throw new RuntimeException("findNextAfterEof called when _eof false.");

        if (_current1._vno != _index.vno())
        {
            boolean state = pessimisticNeeded;
            state = optimisticSearchNext(stack);
            if (state == pessimisticNeeded)
                pessimisticSearchNext(stack);
        }
    }


    /**
     * Optimistically find the next key in the index after an eof condition.
     */
    private boolean optimisticSearchNext(
                                        DeleteStack    stack)
    {
        int v1 = _index.vno();
        int x1 = _index.xno();

        if ((v1&1) != 0)
            return pessimisticNeeded;
        synchronized(this)
        {
        }

        try
        {
            internalSearchNext(stack, v1, x1);
        }
        catch (NullPointerException npe)
        {
            //No FFDC Code Needed.
            _nullPointerExceptions++;
            return GBSTree.checkForPossibleIndexChange(v1, _index.vno(), npe, "optimisticSearchNext");
        }
        catch (OptimisticDepthException ode)
        {
            //No FFDC Code Needed.
            _optimisticDepthExceptions++;
            return GBSTree.checkForPossibleIndexChange(v1, _index.vno(), ode, "optimisticSearchNext");
        }

        if (v1 != _index.vno())
        {
            _current1.setVersion(1);
            return pessimisticNeeded;
        }

        _optimisticSearchNexts++;
        return optimisticWorked;
    }

    /**
     * Pessimistically find the next key in the index after an eof condition.
     */
    private void pessimisticSearchNext(
                                      DeleteStack    stack)
    {
        synchronized(_index)
        {
            internalSearchNext(stack, _index.vno(), _index.xno());
            _pessimisticSearchNexts++;
        }
    }


    /**
     * Search to re-establish current iterator position.
     *
     * <p>This is called when the iterator can find no more entries and is
     * either known to have hit eof or is just about to confirm that fact.
     * It sets _eof true and saves the current index vno in _current1.  If
     * the search succeeds it sets _eof false.</p>
     */
    private void internalSearchNext(
                                   DeleteStack    stack,
                                   int            v1,
                                   int            x1)
    {
        SearchComparator comp = searchComparator(SearchComparator.GT);
        _s = 0;
        _p = null;
        _eof = true;
        _current1.setVersion(v1);
        SearchNode sn = searchNode();
        Object q = _index.iteratorFind(_dstack, comp, _last1.key(), sn);
        if (q != null)
        {
            _current1.setLocation(sn.foundNode(), sn.foundIndex());
            _current1.setLocation(sn.key(), v1, x1);
            _s = NodeStack.VISIT_RIGHT;
            _p = sn.foundNode();
            _eof = false;
        }
    }


    /**
     * Get the next key optimistically in the case that vno has not
     * changed since the last call to next().
     *
     * <p>This is called if vno has not changed since the last key was
     * fetched.  This means that nothing in the index has changed at all
     * and we can simply move forward to the next key.  If the optimistic
     * get fails (returning pessimisticNeeded) we can't simply redo the
     * same algorithm because this algorithm is only valid when vno is
     * unchanged and a return of pessimisticNeeded means that vno has
     * changed.</p>
     *
     * @param stack The stack to use to record the traversal.
     *
     * @return pessimistNeeded if the optimistic search failed and must
     *         be retried pesimistically.
     */
    private boolean optimisticGetNext1(
                                      DeleteStack      stack)
    {
        Object q = null;

        int v1 = _index.vno();
        int x1 = _index.xno();

        if ((v1&1) != 0)
            return pessimisticNeeded;
        synchronized(this)
        {
        }

        int idx = _last1.index() + 1;
        GBSNode p = _last1.node();
        if (idx < p.population())      /* Have more in current node             */
        {
            /* Get next one in current node          */
            _current1.setLocation(p, idx);
            q = p.key(idx);
        }
        else                           /* Have gotten all in current node       */
        {
            /* Move to next node (if any)            */
            if (1 == 1)
                return pessimisticNeeded;
            try
            {
                p = nextNode(stack);
                _current1.setLocation(p, 0);
                if (p != null)
                    q = p.leftMostKey();
            }
            catch (NullPointerException npe)
            {
                //No FFDC Code Needed.
                _nullPointerExceptions++;
                return GBSTree.checkForPossibleIndexChange(v1, _index.vno(), npe, "optimisticGetNext1");
            }
            catch (OptimisticDepthException ode)
            {
                //No FFDC Code Needed.
                _optimisticDepthExceptions++;
                return GBSTree.checkForPossibleIndexChange(v1, _index.vno(), ode, "optimisticGetNext1");
            }
        }

        if (v1 != _index.vno())
            return pessimisticNeeded;

        _current1.setLocation(q, v1, x1);
        _optimisticGetNext1s++;

        return optimisticWorked;
    }

    /**
     * Get the next key pessimistically.
     *
     * <p>Lock the whole index and call internalGetNext().</p>
     *
     * @param stack The stack used to record the traversal.
     */
    private void pessimisticGetNext(
                                   DeleteStack    stack)
    {
        synchronized(_index)
        {
            internalGetNext(stack, _index.vno(), _index.xno());
            _pessimisticGetNexts++;
        }
    }

    /**
     * Get the next key when vno (and perhaps xno) has changed.
     *
     * <p>vno (and perhaps xno) has changed.  If xno is unchanged then we
     * have to assume that the keys in the current node have changed.
     * They may not have changed but since we do not have a separate vno
     * per node we can't tell.  So search the node for the last key given
     * out in order to re-establish the former position.  If the last key
     * given out no longer exists in the current node then we move to the
     * next node and if there is a next node return the left-most key
     * in the next node.</p>
     *
     * <p>If this yields no result or xno has changed then we have to
     * search the tree for the next key that is greater than the last one
     * given out.<p>
     *
     * @param stack The stack used to record the traversal.
     * @param v1 A local copy of _index.vno().
     * @param x1 A local copy of _index.xno().
     */
    private void internalGetNext(DeleteStack    stack,
                                int            v1,
                                int            x1)
    {
        if (_last1._xno == x1)
        {
            SearchComparator comp = searchComparator(SearchComparator.EQ);
            GBSNode p = _last1.node();
            int x = p.searchAll(comp, _last1.key());
            if (x >= 0)
            {
                if ((x+1) < p.population())
                {
                    _current1.setLocation(p, x+1);
                    _current1.setLocation(p.key(x+1), v1, x1);
                }
                else
                {
                    p = nextNode(stack);
                    _current1.setLocation(p, 0);
                    if (p != null)
                    {
                        Object q = p.leftMostKey();
                        _current1.setLocation(q, v1, x1);
                    }
                }
            }
        }

        if (_current1.key() == null)
            internalSearchNext(stack, v1, x1);
    }

    private boolean   _eof;
    private int       _s;
    private GBSNode   _p;

    /**
     * Find the next node in the tree in key order.
     *
     * @param stack The stack being used for traversal.
     *
     * @return the next node in key order.  null if we have reached the end.
     */
    private GBSNode nextNode(
                            DeleteStack    stack)
    {
        if (_eof)
            throw new RuntimeException("_eof is set on entry to nextNode()");

        boolean done = false;
        GBSNode q = null;
        GBSNode nextp = null;
        while ( !done )
        {
            if (stack.index() > GBSTree.maxDepth)
                throw new OptimisticDepthException(
                                                  "maxDepth (" + GBSTree.maxDepth +
                                                  ") exceeded in GBSIterator.nextNode().");
            switch (_s)
            {
            case NodeStack.VISIT_LEFT:
                _s = NodeStack.PROCESS_CURRENT;
                q = _p.leftChild();
                while (q != null)
                {
                    stack.push(_s, _p, "GBSIterator.nextNode:VISIT_LEFT");
                    _p = q;
                    q = _p.leftChild();
                }
                break;
            case NodeStack.PROCESS_CURRENT:
                _s = NodeStack.VISIT_RIGHT;
                done = true;
                nextp = _p;              /* Next node to visit                    */
                break;
            case NodeStack.VISIT_RIGHT:
                _s = NodeStack.DONE_VISITS;
                q = _p.rightChild();
                if (q != null)
                {
                    stack.push(_s, _p, "GBSIterator.nextNode:VISIT_RIGHT");
                    _s = NodeStack.VISIT_LEFT;
                    _p = _p.rightChild();
                }
                break;
            case NodeStack.DONE_VISITS:
                if (stack.index() <= 0)  /* Have finally hit end of sub-tree      */
                    done = true;
                else
                {
                    _s = stack.state();
                    _p = stack.node();
                    stack.pop();
                }
                break;
            default:
                throw new RuntimeException("Help!, _s = " + _s + ", _p = " + _p + ".");
            }                            /* switch(_s)                            */
        }                              /* while ( !done )                       */
        return nextp;
    }

    /**
     * Return the next element in the collection.
     */
    public Object next()
    {
        _current1.reset();
        if (_last1.key() == null)
            findFirst(_dstack);
        else
        {
            findNext(_dstack);
            if (_current1.key() == null)
                _eof = true;
        }

        if (_current1.key() != null)
            _last1.setLocation(_current1);

        return _current1.key();
    }

    /**
     * Remove from the collection the last key returned by next().
     *
     * <p>Since the collection is allowed to change in arbitrary ways
     * there is no assumption that the last thing given out still exists.
     * If vno has not changed since the last call to next we are
     * positioned on the last thing given out.  If vno has changed but xno
     * has not we have to assume the current node has changed and we can
     * try a search of the node to re-esbablish position within it.</p>
     *
     * <p>If we have position within a node then we can check to see if
     * direct removal from the node is possible in order to by pass the
     * call to delete.  Direct removal is possible if the node is a leaf
     * with more than one key and is also not full.  Removal of the last
     * key would require removal of the node which might require a tree
     * re-balance.  Removal of a key from a full node might be the removal
     * of the last key in a T0 fringe which would require a fringe
     * re-balance.</p>
     *
     * <p>Since removal of the current key is expected to be frequent in
     * the case of expiry there is a chance that we can actually establish
     * the same position established by GBSTree.findDelete and then call
     * GBSTree to do the second half (the chaper part) of the delete
     * operation in all cases.  This should be considered when add
     * parallelization at a later time.</p>
     *
     * @return true if the key was there to be removed.
     */
    private boolean internalRemove()
    {
        boolean result = false;
        boolean standardDelete = true;
        GBSNode p = _last1.node();
        int lastx = _last1.index();
        synchronized(_index)
        {
            if (_last1._xno != _index.xno())
                standardDelete = true;
            else // _xno has not changed
            {
                p = _last1.node();
                if ( !p.isLeafNode() )
                    standardDelete = true;
                else // Current node is a leaf
                {
                    if ( (p.population() < 2) || (p.isFull()) )
                        standardDelete = true;
                    else // population > 1
                    {
                        if (_last1._vno == _index.vno())
                            standardDelete = false;
                        else // positioned on leaf with more than one key, can delete
                        {
                            SearchComparator comp = searchComparator(SearchComparator.EQ);
                            lastx = p.searchAll(comp, _last1.key());
                            if (lastx < 0)
                                standardDelete = true;
                            else // positioned on leaf with more than one key, can delete
                                standardDelete = false;
                        }
                    } // population > 1
                } // Current node is a leaf
            } // xno has not changed
            if ( !standardDelete )
                _index.iteratorSpecialDelete(this, p, lastx);
        }

        _deleteCount++;

        if (standardDelete)
            result = _index.delete(_last1.key());
        else
        {
            _specialCount++;
            result = true;
        }

        return result;
    }

    /**
     * Remove from the collection the last thing returned by next().
     *
     * @exception NoSuchElementException if next() has never been called.
     *
     * @return true if the element was there to be removed.
     */
    public boolean remove()
    {
        if (_last1.key() == null)
            throw new java.util.NoSuchElementException(
                                                      "remove() without calling next()");

        boolean result = internalRemove();

        return result;
    }

    public String toString()
    {
        String x = "Delete Count = " + _deleteCount + ", " +
                   "Special Count = " + _specialCount;
        return x;
    }

    private int           _specialCount;
    private int           _deleteCount;
    private GBSTree       _index;
    private DeleteStack   _dstack;

    /**
     * This identifies the current thing we are working on trying to find
     * for a next() operation.  If the next() operation succeeds then
     * _current1 is copied on top of _last1.
     */
    private Locator       _current1;

    /**
     * This identifies the last thing successfully returned by next().
     * _last1 is only modified when next() successfully returns something.
     * If _last1 contains no key then next() has never successfully
     * returned anything and each call to next() should start again
     * with the first key in the index.
     */
    private Locator       _last1;

    private static final boolean pessimisticNeeded = false;
    private static final boolean optimisticWorked  = true;

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
        return localComparator().getSingleton(type);
    }

    /**
     * Return the local comparator for this Iterator.
     *
     * <p>If the index never changes during the iteration the comparator
     * is never needed.  So this method delays creation of the comparator
     * until it is needed, which in some cases is never.</p>
     */
    private SearchComparator localComparator()
    {
        if (_localComparator == null)
            _localComparator = new SearchComparator(_index.insertComparator());
        return _localComparator;
    }

    private SearchComparator      _localComparator;

    /**
     * Return the SearchNode for this Iterator.
     *
     * <p>If the index never changes during the iteration the SearchNode
     * is never needed.  So this method delays creation of the SearchNode
     * until it is needed, which in some cases is never.</p>
     */
    private SearchNode searchNode()
    {
        if (_localSearchNode == null)
            _localSearchNode = new SearchNode();
        else
            _localSearchNode.reset();
        return _localSearchNode;
    }

    private SearchNode      _localSearchNode;

    private volatile int    _optimisticFindFirsts;
    private volatile int    _optimisticGetNext1s;
    private volatile int    _optimisticSearchNexts;
    private volatile int    _nullPointerExceptions;
    private volatile int    _optimisticDepthExceptions;
    private          int    _pessimisticFindFirsts;
    private          int    _pessimisticSearchNexts;
    private          int    _pessimisticGetNexts;


}
