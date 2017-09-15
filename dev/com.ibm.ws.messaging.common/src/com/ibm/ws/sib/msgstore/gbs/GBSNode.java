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
 * A GBS Tree Node
 */
public class GBSNode
{
  /**
   * Default construction not allowed
   */
  private GBSNode()
  { }

  /**
   * Standard constructor.  Allocates an array to hold the keys.
   * Must have first key set by reset() before first use or reuse.
   */
  public GBSNode(
    GBSTree       index)
  {
    _index = index;
    _nodeKey = new Object[width()];
    _population = 0;
    _balance = 0;
  }

  /**
   * Standard constructor.  Allocates an array to hold the keys.
   * Stores the first key.
   */
  public GBSNode(
    GBSTree       index,
    Object        key)
  {
    _index = index;
    _nodeKey = new Object[width()];
    reset(key);
  }

  /**
   * Constructor used only to create the dummy top node in the
   * index descriptor.  Avoids allocating the array to hold the keys.
   */
  public GBSNode(
    GBSTree       index,
    int           x)
  {
    _index = index;
  }

  /**
   * Return the node to its post-construction state.
   *
   * @param key The key that would have been passed in to the constructor
   *            for the newly allocated node.
   */
  void reset(
    Object        key)
  {
    _nodeKey[0] = key;
    _nodeKey[midPoint()] = key;
    _population = 1;
    _rightChild = null;
    _leftChild = null;
    _balance = 0;
  }

  /**
   * Return a reference to the containing tree.
   */
  GBSTree index()
  { return _index; }

  /**
   * @return K factor for this tree
   */
  public int kFactor()
  { return index().kFactor(); }

  /**
   * Return the left child of the node.
   */
  public GBSNode  leftChild()
  { return _leftChild; }

  /**
   * Return the right child of the node.
   */
  public GBSNode  rightChild()
  { return _rightChild; }

  /**
   * Return the key in the specified slot in the node.
   *
   * @param x The index of the slot whose key is to be returned.
   */
  public Object key(
    int       x)
  { return _nodeKey[x]; }

  /**
   * Return a String representation of the key in the specified slot.
   *
   * <p>If this node is the dummy top node (which has no key array)
   * return "---" as the printable key value.</p>
   *
   * @param x The index of the slot whose key is to be returned.
   */
  String keyString(
    int       x)
  {
    if (isDummy())
      return "---";
    else
      return key(x).toString();
  }

  /**
   * Return the key in the specified slot in the node or null if this
   * node is the dummy top node which has no array of keys.
   *
   * @param x The index of the slot whose key is to be returned.
   */
  public Object testKey(
    int       x)
  {
    if (isDummy())
      return null;
    else
      return key(x);
  }

  /**
   * Overlay the value of a key in a given slot.
   *
   * <p>This is used only be test code.</p>
   *
   * @param x The index to be overlayed.
   * @param q The key to use for the overlay.
   */
  public void testSetKey(
    int       x,
    Object    q)
  {
    _nodeKey[x] = q;
  }

  /**
   * Increment the node population.
   *
   * <p>Used only by test code.</p>
   */
  public void testIncrementPopulation()
  {
    _population++;
  }

  /**
   * Return true if the node has either a right child or a left child.
   */
  boolean hasChild()
  {
    boolean has = false;
    if ( (leftChild() != null) || (rightChild() != null) )
      has = true;
    return has;
  }

  /**
   * Return true if the node is the dummy top node.
   */
  private boolean isDummy()
  {
    boolean x = false;
    if (_nodeKey == null)
      x = true;
    return x;
  }

  /**
   * Return the balance factor for the node.
   */
  public short balance()
  {
    if ( (_balance == -1) || (_balance == 0) || (_balance == 1) )
      return _balance;
    else
     {
       String x = "Found invalid balance factor: " + _balance;
       throw new RuntimeException(x);
     }
  }

  /**
   * Set the node's balance factor to zero.
   */
  void clearBalance()
  { _balance = 0; }

  /**
   * Set the node's balance factor to a new value.
   *
   * @param b The value to set, which must be 0, -1, or +1.
   */
  void setBalance(
    int       b)
  {
    if ( (b == -1) || (b == 0) || (b == 1) )
      _balance = (short) b;
    else
     {
       String x = "Attempt to set invalid balance factor: " + b;
       throw new IllegalArgumentException(x);
     }
  }

  /**
   * Return the node population (The number of keys held).
   */
  public int population()
  { return _population; }

  /**
   * Decrement the node's population by one.
   */
  void decrementPopulation()
  {
    if (_population <= 0)
      {
        String x = "Attempt to make node population negative.\n" +
         "Current population = " + _population + ".";
        throw new IllegalArgumentException(x);
      }
    _population--;
  }

  /**
   * Return true if the node is full (can hold no more keys).
   */
  public boolean isFull()
  {
    boolean full = false;
    if (population() == width())
      full = true;
    return full;
  }

  /**
   * Return true if the node is an inner node (not a leaf node).
   */
  public boolean isInnerNode()
  {
    boolean inner = true;
    if ( (_leftChild  == null) ||
         (_rightChild == null) )
      inner = false;
    return inner;
  }

  /**
   * Return true if the node is a leaf node.
   */
  public boolean isLeafNode()
  {
    boolean leaf = false;
    if ( (_leftChild  == null) &&
         (_rightChild == null) )
      leaf = true;
    return leaf;
  }

  /**
   * @return Node width (max population count).
   */
  public int width()
  { return index().nodeWidth(); }

  /**
   * @return Node median compare point for the node.
   */
  int midPoint()
  { return index().nodeMidPoint(); }

  /**
   * @return The key that represents the median value in the node.
   */
  public Object middleKey()
  {  return _nodeKey[midPoint()]; }

  /**
   * @return The key that represents the lowest value in the node.
   */
  public Object leftMostKey()
  {  return _nodeKey[0]; }

  /**
   * @return The String value of the key that represents the
   *         lowest value in the node.
   */
  public String lmkString()
  {
    if (isDummy())
      return "---";
    else
      return _nodeKey[0].toString();
  }

  /**
   * @return the end point in the node for a search from the left.
   */
  private int endPoint()
  {
    int x = midPoint();            /* Assume node at least half full        */
    int r = rightMostIndex();
    if (r < midPoint())            /* Less than half full                   */
    x = r;                         /* Just search what is there             */
    return x;
  }

  /**
   * Find the insert point in the left half of the node for a new key.
   *
   * @param new1  New Object to be inserted
   * @param point Found insertion point
   */
  void findInsertPointInLeft(
    Object           new1,
    NodeInsertPoint  point)
  {
    int endp = endPoint();
    findIndex(0, endp, new1, point);
  }

  /**
   * Find the insert point in the right half of the node for a new key.
   * NB: If the node is less than half full then the new key is to
   *     become the highest key in the node.
   *
   * @param new1  New Object to be inserted
   * @param point Found insertion point
   */
  void findInsertPointInRight(
    Object           new1,
    NodeInsertPoint  point)
  {
    int r = rightMostIndex();
    int m = midPoint();
    if (r <= m)
      {
        String x =
         "Erroneous call to findInsertPointInRight(). "  +
         "rightMostIndex() = " + r +
         ", midPoint() = "     + m + ".";
        throw new OptimisticDepthException(x);
      }
    findIndex(m+1 , r, new1, point);
  }

  /**
   * Set the node's right child pointer.
   */
  public void setRightChild(
    GBSNode        x)
  { _rightChild = x; }

  /**
   * Clear the node's right child pointer.
   */
  void clearRightChild()
  { _rightChild = null; }

  /**
   * Set the node's left child pointer.
   */
  public void setLeftChild(
    GBSNode        x)
  { _leftChild = x; }

  /**
   * Clear the node's left child pointer.
   */
  void clearLeftChild()
  { _leftChild = null; }

  /**
   * Set both of the node's child pointers (left and right).
   */
  public void setChildren(
    GBSNode       left,
    GBSNode       right)
  {
    _leftChild = left;
    _rightChild = right;
  }

  /**
   * Search the left half of the node.
   *
   * <p>Look in the left half of the node to find an index entry that is
   * one of
   * <ol>
   * <li>an exact match for the supplied key,
   * <li>greater than the supplied key, or
   * <li>greater than or equal to the supplied key.
   * </ol>
   * The type of the supplied SearchComparator indicate which of the
   * above three conditions apply</p>.
   *
   * @param comp The SearchComparator to use for the key comparisons.
   * @param searchKey The key to find.
   *
   * @return Index of key within node.  -1 if key does not exist.
   */
  public int searchLeft(
    SearchComparator  comp,
    Object            searchKey)
  {
    int idx = -1;
    int top = middleIndex();
    if (comp.type() == SearchComparator.EQ)
      idx = findEqual(comp, 0, top, searchKey);
    else
      idx = findGreater(comp, 0, top, searchKey);

    return idx;
  }

  /**
   * Search the right half of the node.
   *
   * <p>Look in the right half of the node to find an index entry that is
   * one of
   * <ol>
   * <li>an exact match for the supplied key,
   * <li>greater than the supplied key, or
   * <li>greater than or equal to the supplied key.
   * </ol>
   * The type of the supplied SearchComparator indicate which of the
   * above three conditions apply</p>.
   *
   * @param comp The SearchComparator to use for the key comparisons.
   * @param searchKey The key to find.
   *
   * @return Index of key within node.  -1 if key does not exist.
   */
  public int searchRight(
    SearchComparator  comp,
    Object            searchKey)
  {
    int idx = -1;
    int bot = middleIndex();
    int right = rightMostIndex();
    if (bot > right)
      {
        String x =
         "bot = " + bot + ", right = " + right;
        throw new OptimisticDepthException(x);
      }
    if (comp.type() == SearchComparator.EQ)
      idx = findEqual(comp, bot, right, searchKey);
    else
      idx = findGreater(comp, bot, right, searchKey);

    return idx;
  }

  /**
   * Search the whole node.
   *
   * <p>Look in the whole node to find an index entry that is one of</p>
   * <ol>
   * <li>an exact match for the supplied key,
   * <li>greater than the supplied key, or
   * <li>greater than or equal to the supplied key.
   * </ol>
   * <p>The type of the supplied SearchComparator indicate which of the
   * above three conditions apply</p>.
   *
   * @param comp The SearchComparator to use for the key comparisons.
   * @param searchKey The key to find.
   *
   * @return Index of key within node.  -1 if key does not exist.
   */
  int searchAll(
    SearchComparator  comp,
    Object            searchKey)
  {
    int idx = -1;
    if (comp.type() == SearchComparator.EQ)
      idx = findEqual(comp, 0, rightMostIndex(), searchKey);
    else
      idx = findGreater(comp, 0, rightMostIndex(), searchKey);

    return idx;
  }

  /**
   * Find an index entry that is equal to the supplied key.
   *
   * @param searchKey The key to find.
   *
   * @return Index of key within node.  -1 if key does not exist.
   */
  private int findEqual(
    SearchComparator  comp,
    int               lower,
    int               upper,
    Object            searchKey)
  {
    int nkeys = numKeys(lower, upper);
    int idx = -1;
    if (nkeys < 4)
      idx = sequentialSearchEqual(comp, lower, upper, searchKey);
    else
      idx = binarySearchEqual(comp, lower, upper, searchKey);
    return idx;
  }

  /**
   * Use sequential search to find a matched key.
   */
  private int sequentialSearchEqual(
    SearchComparator  comp,
    int               lower,
    int               upper,
    Object            searchKey)
  {
    int xcc;                       /* Current compare result                */
    int idx = -1;                  /* The returned index (-1 if not found)  */
    for (int i = lower; i <= upper; i++)
    {
      xcc = comp.compare(searchKey, _nodeKey[i]);
      if (xcc == 0)                /* Found a match                         */
        {
          idx = i;                 /* Remember current point                */
          break;
        }
    }
    return idx;
  }

  /**
   * Use binary search to find the a matched key.
   */
  private int binarySearchEqual(
    SearchComparator  comp,
    int               lower,
    int               upper,
    Object            searchKey)
  {
    int xcc;                       /* Current compare result                */
    int i;
    int idx = -1;                  /* Returned index (-1 if not found)      */
    while (lower <= upper)         /* Until they cross                      */
    {
      i = (lower + upper) >>> 1;   /* Mid-point of current range            */
      xcc = comp.compare(searchKey, _nodeKey[i]);
      if (xcc < 0)                 /* SEARCH_KEY < NODE_KEY                 */
        upper = i - 1;             /* Discard upper half of range           */
      else                         /* SEARCH_KEY >= NODE_KEY                */
        {
          if (xcc > 0)             /* SEARCH_KEY > NODE_KEY                 */
            lower = i + 1;         /* Discard lower half of range           */
          else                     /* SEARCH_KEY FOUND                      */
            {
              idx = i;             /* Remember the index                    */
              break;               /* We're done                            */
            }
        }
    }
    return idx;
  }

  /**
   * Find an index entry that is either greater than or greater
   * than or equal to the supplied key.
   *
   * @param searchKey The key to find.
   *
   * @return Index of key within node.  -1 if key does not exist.
   */
  private int findGreater(
    SearchComparator  comp,
    int               lower,
    int               upper,
    Object            searchKey)
  {
    int nkeys = numKeys(lower, upper);
    int idx = -1;
    if (nkeys < 4)
      idx = sequentialSearchGreater(comp, lower, upper, searchKey);
    else
      idx = binarySearchGreater(comp, lower, upper, searchKey);
    return idx;
  }

  /**
   * Use sequential search to find an index entry that is either
   * greater than or greater than or equal to the supplied key.
   */
  private int sequentialSearchGreater(
    SearchComparator  comp,
    int               lower,
    int               upper,
    Object            searchKey)
  {
    int xcc;                       /* Current compare result                */
    int idx = -1;                  /* The returned index (-1 if not found)  */
    for (int i = lower; i <= upper; i++)
    {
      xcc = comp.compare(searchKey, _nodeKey[i]);
      if ( !(xcc > 0) )            /* Key <= or key < index entry           */
        {
          idx = i;                 /* Remember current point                */
          break;
        }
    }
    return idx;
  }

  /**
   * Use binary search to find an index entry that is either
   * greater than or greater than or equal to the supplied key.
   */
  private int binarySearchGreater(
    SearchComparator  comp,
    int               lower,
    int               upper,
    Object            searchKey)
  {
    int xcc;                       /* Current compare result                */
    int i;
    int idx = -1;                  /* Returned index (-1 if not found)      */
    while (lower <= upper)         /* Until they cross                      */
    {
      i = (lower + upper) >>> 1;   /* Mid-point of current range            */
      xcc = comp.compare(searchKey, _nodeKey[i]);
      if (xcc < 0)                 /* SEARCH_KEY < NODE_KEY                 */
        {
          upper = i - 1;           /* Discard upper half of range           */
          idx = i;                 /* Remember last upper bound             */
        }
      else                         /* SEARCH_KEY >= NODE_KEY                */
        {
          if (xcc > 0)             /* SEARCH_KEY > NODE_KEY                 */
            lower = i + 1;         /* Discard lower half of range           */
        }
    }
    return idx;
  }


  /**
   * Find the insert point for a new key.
   *
   * <p>Find the insert point for a new key.  This method finds the point
   * AFTER which the new key should be inserted.  The key does not
   * need to be bounded by the node value and duplicates are allowed.
   * If the new key is less than the lowest value already in the node,
   * -1 is returned as the insert point.</p>
   *
   * <p>If the node is full, the PRE-insert point returned may be the
   * right-most slot in the node.  In that case, the new key REPLACES
   * the maximum value in the node.</p>
   *
   * @param lower Lower bound for search
   * @param upper Upper bound for search
   * @param new1  New Object to be inserted
   * @param point Found insertion point
   */
  private void findIndex(
    int              lower,
    int              upper,
    Object           new1,
    NodeInsertPoint  point)
  {
    int nkeys = numKeys(lower, upper);
    if (nkeys < 4)
      sequentialFindIndex(lower, upper, new1, point);
    else
      binaryFindIndex(lower, upper, new1, point);
  }

  /**
   * Use sequential search to find the insert point.
   */
  private void sequentialFindIndex(
    int              lower,
    int              upper,
    Object           new1,
    NodeInsertPoint  point)
  {
    java.util.Comparator comp = insertComparator();
    int xcc;                       /* Current compare result                */
    int lxcc = +1;                 /* Remembered compare result             */
    int idx = rightMostIndex() + 1;/* Assume greatest value                 */
    for (int i = lower; i <= upper; i++)
    {
      xcc = comp.compare(new1, _nodeKey[i]);
      if ( !(xcc > 0) )            /* New one not greater, want previous pt.*/
        {
          idx = i;                 /* Remember current point                */
          lxcc = xcc;              /* Remember last compare result          */
          break;
        }
    }
    if (lxcc != 0)                 /* Never had an equal compare            */
      point.setInsertPoint(idx-1); /* Desired point is previous one         */
    else                           /* Had an equal compare                  */
      point.markDuplicate(idx);    /* Key is a duplicate at point "idx"     */
  }

  /**
   * Use binary search to find the insert point.
   */
  private void binaryFindIndex(
    int              lower,
    int              upper,
    Object           new1,
    NodeInsertPoint  point)
  {
    java.util.Comparator comp = insertComparator();
    int xcc;                       /* Current compare result                */
    int lxcc = +1;                 /* Remembered compare result             */
    int i;
    int idx = upper + 1;           /* Found index plus one                  */
    while (lower <= upper)         /* Until they cross                      */
    {
      i = (lower + upper) >>> 1;   /* Mid-point of current range            */
      xcc = comp.compare(new1, _nodeKey[i]);
      if ( !(xcc > 0) )            /* SEARCH_KEY <= NODE_KEY                */
        {
          upper = i - 1;           /* Discard upper half of range           */
          idx = i;                 /* Remember last upper bound             */
          lxcc = xcc;              /* Remember last upper compare result    */
        }
      else                         /* SEARCH_KEY >= NODE_KEY                */
        lower = i + 1;             /* Discard lower half of range           */
    }
    if (lxcc != 0)                 /* Never had an equal compare            */
      point.setInsertPoint(idx-1); /* Desired point is previous one         */
    else                           /* Had an equal compare                  */
      point.markDuplicate(idx);    /* Key is a duplicate at point "idx"     */
  }

  /**
   * Find the delete key in the right half of the node.
   *
   * <p>The key to be found is known to be greater than the key
   * at the mid point.  If the key at the mid point is the last
   * key in the node then the delete key is not here.</p>
   */
  int findDeleteInRight(
    Object           delKey)
  {
    int idx = -1;
    int r = rightMostIndex();
    int m = midPoint();
    if (r > m)
      idx = findDelete(m, r, delKey);
    return idx;
  }

  /**
   * Find the delete key in the left half of the node.
   */
  int findDeleteInLeft(
    Object           delKey)
  {
    int endp = endPoint();
    return findDelete(0, endp, delKey);
  }

  /**
   * Find the index of the key to delete.
   *
   * @param delKey The key to delete.
   *
   * @return Index of key within node.  -1 if key does not exist.
   */
  int findDelete(
    int              lower,
    int              upper,
    Object           delKey)
  {
    int nkeys = numKeys(lower, upper);
    int idx = -1;
    if (nkeys < 4)
      idx = sequentialFindDelete(lower, upper, delKey);
    else
      idx = binaryFindDelete(lower, upper, delKey);
    return idx;
  }

  /**
   * Use sequential search to find the delete key.
   */
  private int sequentialFindDelete(
    int              lower,
    int              upper,
    Object           delKey)
  {
    java.util.Comparator comp = deleteComparator();
    int xcc;                       /* Current compare result                */
    int idx = -1;                  /* The returned index (-1 if not found)  */
    for (int i = lower; i <= upper; i++)
    {
      xcc = comp.compare(delKey, _nodeKey[i]);
      if (xcc == 0)                /* Found a match                         */
        {
          idx = i;                 /* Remember current point                */
          break;
        }
    }
    return idx;
  }

  /**
   * Use binary search to find the delete key.
   */
  private int binaryFindDelete(
    int              lower,
    int              upper,
    Object           delKey)
  {
    java.util.Comparator comp = insertComparator();
    int xcc;                       /* Current compare result                */
    int i;
    int idx = -1;                  /* Returned index (-1 if not found)      */
    while (lower <= upper)         /* Until they cross                      */
    {
      i = (lower + upper) >>> 1;   /* Mid-point of current range            */
      xcc = comp.compare(delKey, _nodeKey[i]);
      if (xcc < 0)                 /* SEARCH_KEY < NODE_KEY                 */
        upper = i - 1;             /* Discard upper half of range           */
      else                         /* SEARCH_KEY >= NODE_KEY                */
        {
          if (xcc > 0)             /* SEARCH_KEY > NODE_KEY                 */
            lower = i + 1;         /* Discard lower half of range           */
          else                     /* SEARCH_KEY FOUND                      */
            {
              idx = i;             /* Remember the index                    */
              break;               /* We're done                            */
            }
        }
    }
    return idx;
  }

  /**
   * @return the Comparator used for insert operations.
   */
  public java.util.Comparator insertComparator()
  {
    return index().insertComparator();
  }

  /**
   * @return the Comparator used for delete operations.
   */
  java.util.Comparator deleteComparator()
  {
    return index().deleteComparator();
  }

  /**
   * @return the number of key values between an upper and lower bound.
   */
  private static int numKeys(
    int    lower,
    int    upper)
  {
    if (lower > upper)
      {
        String x =
         "Lower bound greater than upper bound.  " +
         "lower = " + lower + ", upper = " + upper + ".";
        throw new IllegalArgumentException(x);
      }
    return upper - lower + 1;
  }

  /**
   * Returns index of right most key in the node.
   *
   * <p>Any method that will not modify the node may be called in the
   * optimistic search phase of a tree modification.  If such a method
   * uses the value of rightMostIndex() more than once, it must call
   * rightMlostIndex() exactly once and cache the result in a local
   * variable.  This is because the value may be constantly changing
   * during the optimistic, unprotected read phase and the calling
   * method must always use the same value for rightMostIndex() even if
   * it is made inaccurate by concurrent modifications.</p>
   */
  public int rightMostIndex()
  {
    int x = population() - 1;
    if (x == -1)
      throw new OptimisticDepthException("Empty Node.");
    return x;
  }

  /**
   * Returns index of right most key in the node if the node
   * were to be full.
   */
  public int topMostIndex()
  { return width()-1; }

  /**
   * Return the index to the key at the median position in the node.
   */
  public int middleIndex()
  {
    int x = midPoint();
    int r = rightMostIndex();
    if (r < midPoint())
      x = r;
    return x;
  }


  /**
   * Return true if the node is less than half full.
   */
  boolean lessThanHalfFull()
  {
    boolean result = false;
    if (rightMostIndex() <= midPoint())
      result = true;
    return result;
  }

  /**
   * Returns right most key in the node.
   */
  public Object rightMostKey()
  { return _nodeKey[rightMostIndex()]; }

  /**
   * Add a right child to the node placing in the new right child
   * the supplied key.
   */
  void addRightLeaf(
    Object            new1)
  {
    GBSNode  p = _index.getNode(new1);
    if (rightChild() != null)
      throw new RuntimeException("Help!");
    setRightChild(p);
  }

  /**
   * Insert a new key by overlaying the left-most key, shifting
   * other keys left and inserting the new key at the appropriate
   * insert point.
   *
   * @param ix The insert point within the node.
   * @param new1 The new key to be inserted.
   * @return the key that used to be the left-most key in the node.
   */
  Object insertByLeftShift(
    int               ix,
    Object            new1)
  {
    Object old1 = leftMostKey();
    leftShift(ix);
    _nodeKey[ix] = new1;
    if (midPoint() > rightMostIndex())
      _nodeKey[midPoint()] = rightMostKey();
    return old1;
  }

  /**
   * Delete a key by moving following keys to the left to overlay the
   * deleted key.
   *
   * @param ix The delete point within the node.
   */
  void deleteByLeftShift(
    int               ix)
  {
    overlayLeftShift(ix);
    _population--;
  }

  /**
   * Open up a slot in a node by shifting the keys left.
   *
   * <p>We are inserting a new key into a node that is not currently
   * full.  There is at least one vacant slot.  We open up the correct
   * slot for the new key by shifting all other keys left.</p>
   */
  private void leftShift(
    int           ix)
  {
    for (int j = 0; j < ix; j++)
      _nodeKey[j] = _nodeKey[j+1];
  }

  /**
   * Insert a new key by shifting keys right.
   *
   * <p>Insert a new key by overlaying the right-most key, shifting
   * other keys right and inserting the new key at the appropriate
   * insert point.</p>
   *
   * @param ix The insert point within the node.
   * @param new1 The new key to be inserted.
   * @return the key that used to be the right-most key in the node.
   */
  Object insertByRightShift(
    int               ix,
    Object            new1)
  {
    Object old1 = null;
    if (isFull())
      {
        old1 = rightMostKey();
        rightMove(ix+1);
        _nodeKey[ix+1] = new1;
      }
    else
      {
        rightShift(ix+1);
        _nodeKey[ix+1] = new1;
        _population++;
        if (midPoint() > rightMostIndex())
          _nodeKey[midPoint()] = rightMostKey();
      }
    return old1;
  }

  /**
   * Add a left-most key to the node.
   *
   * <p>Insert a new key in the left-most slot in the node by shifting
   * all other keys right and inserting the new key at the left-most
   * slot in the node.  If this causes the right-most key to be overlayed
   * it is returned to the caller.  Otherwise null is returned to the
   * caller.</p>
   *
   * @param new1 The new key to be inserted.
   * @return the key that used to be the right-most key in the node
   *         iff it was overlayed in the process.
   */
  public Object addLeftMostKey(
    Object            new1)
  {
    Object old1 = null;

    if (isFull())                  /* Node is currently full                */
      {
        old1 = rightMostKey();     /* We will overlay right-most key        */
        rightMove(0);
        _nodeKey[0] = new1;
      }
    else                           /* Node not full, must adjust population */
      {                            /*  and, perhaps, median key             */
        rightShift(0);
        _population++;
        _nodeKey[0] = new1;
        if (midPoint() > rightMostIndex())
          _nodeKey[midPoint()] = rightMostKey();
      }

    return old1;
  }

  /**
   * Add the right-most key to the node.
   *
   * <p>Insert a new key in the right-most slot in the node.
   * If this causes the right-most key to be overlayed
   * it is returned to the caller.  Otherwise null is returned to the
   * caller.</p>
   *
   * @param new1 The new key to be inserted.
   * @return the key that used to be the right-most key in the node
   *         iff it was overlayed in the process.
   */
  Object addRightMostKey(
    Object            new1)
  {
    Object old1 = null;

    if (isFull())                  /* Node is currently full                */
      {                            /* We will overlay right-most key        */
        old1 = rightMostKey();
        _nodeKey[rightMostIndex()] = new1;
      }
    else
      {
        _population++;
        _nodeKey[rightMostIndex()] = new1;
        if (midPoint() > rightMostIndex())
          _nodeKey[midPoint()] = rightMostKey();
      }

    return old1;
  }

  /**
   * Add the left-most key to the node, shifting keys right to make
   * room by overlaying one of the keys.
   *
   * <p>Insert a new key in the left-most slot in the node by shifting
   * all other keys right and inserting the new key at the left-most
   * slot in the node.  This will overlay the key whose position is
   * passed in as a parameter.</p>
   *
   * @param ix The slot containing the key to be overlayed.
   * @param new1 The new key to be inserted.
   */
  void addLeftMostKeyByDelete(
    int               ix,
    Object            new1)
  {
    overlayRightShift(ix);
    overlayLeftMostKey(new1);
  }

  /**
   * Add the left-most key in the node, shifting all other keys left
   * to make room by overlaying one of the keys.
   *
   * <p>Insert a new key in the right-most slot in the node by shifting
   * all other keys left and inserting the new key at the right-most
   * slot in the node.  This will overlay the key whose position is
   * passed in as a parameter.</p>
   *
   * @param ix The slot containing the key to be overlayed.
   * @param new1 The new key to be inserted.
   */
  void addRightMostKeyByDelete(
    int               ix,
    Object            new1)
  {
    overlayLeftShift(ix);
    overlayRightMostKey(new1);
  }

  /**
   * Replace the left-most key in the node with the supplied one.
   */
  void overlayLeftMostKey(
    Object           new1)
  {
    _nodeKey[0] = new1;
  }

  /**
   * Overlay the right-most key in the node.
   */
  void overlayRightMostKey(
    Object           new1)
  {
    _nodeKey[rightMostIndex()] = new1;
  }

  /**
   * Fill a node with new keys from the right side.
   *
   * <p>A node that is not completely full has acquired a right child.
   * Use the left keys of the right child to fill the node as much
   * as possible, limited by the free space in the current node and
   * the population of the right child.</p>
   */
  void fillFromRight()
  {
    int gapWid = width() - population();
    int gidx = population();
    GBSNode p = rightChild();
    for (int j = 0; j < gapWid; j++)
      _nodeKey[j+gidx] = p._nodeKey[j];
    int delta = gapWid;
    if (p._population < delta)
      delta = p._population;
    _population   += delta;
    p._population -= delta;
    for (int j = 0; j < gidx; j++)
      p._nodeKey[j] = p._nodeKey[j+gapWid];
  }

  /**
   * Set the value of the median key, which will be the right-most
   * key if the node is less than half full.
   */
  void adjustMedian()
  {
    if (midPoint() > rightMostIndex())
      _nodeKey[midPoint()] = rightMostKey();
  }

  /**
   * Find the right-most child of a node by following the paths of
   * all of the right-most children all the way to the bottom of
   * the tree.
   */
  GBSNode rightMostChild()
  {
    GBSNode q = this;
    GBSNode p = rightChild();
    while (p != null)
    {
      q = p;
      p = p.rightChild();
    }
    return q;
  }

  /**
   * Open up a slot for a new key by shifting keys right to make room.
   *
   * <p>We are inserting a new key into a node that is not currently
   * full.  There is at least one vacant slot.  We open up the correct
   * slot for the new key by shifting all other keys right.</p>
   */
  private void rightShift(
    int           ix)
  {
    for (int j = rightMostIndex(); j >= ix; j--)
      _nodeKey[j+1] = _nodeKey[j];
  }

  /**
   * Open up a slot for a new key by shifting keys right to make room
   * and overlaying the right-most key.
   *
   * <p>We are inserting a new key into a node that is currently full.
   * We open up the correct slot for the new key by shifting all other
   * keys right, overlaying the right-most key.</p>
   */
  private void rightMove(
    int           ix)
  {
    for (int j = rightMostIndex()-1; j >= ix; j--)
      _nodeKey[j+1] = _nodeKey[j];
  }

  /**
   * Overlay a key to be deleted by moving keys left.
   *
   * <p>We are removing a key from a node.  We shift all of the keys
   * left to overlay the slot of the key to be deleted and open up the
   * slot occupied by the right-most key in the node.</p>
   */
  void overlayLeftShift(
    int           ix)
  {
    for (int j = ix; j < rightMostIndex(); j++)
      _nodeKey[j] = _nodeKey[j+1];
  }

  /**
   * Overlay a key to be deleted by moving keys right.
   *
   * <p>We are removing a key from a node.  We shift all of the keys
   * right to overlay the slot of the key to be deleted and open up the
   * slot occupied by the left most key in the node.</p>
   */
  private void overlayRightShift(
    int           ix)
  {
    for (int j = ix; j > 0; j--)
      _nodeKey[j] = _nodeKey[j-1];
  }

  /**
   *  Find the lower predecessor of this node.
   *
   * <p>The lower predecessor is the right-most child of the left child.
   * This is the prior node in order that is also below this node.</p>
   *
   * @param stack The stack that is used to remember the traversal path.
   *
   * @return The lower predecessor node.
   */
  GBSNode lowerPredecessor(
    NodeStack     stack)
  {
    GBSNode  r     = leftChild();
    GBSNode  lastr = r;
    if (r != null)
      stack.push(NodeStack.PROCESS_CURRENT, this);
    while (r != null)              /* Find right-most child of left child   */
    {
      if (r.rightChild() != null)
        stack.push(NodeStack.DONE_VISITS, r);
      lastr = r;
      r = r.rightChild();
    }
    return lastr;
  }


  /**
   *  Find the lower successor of this node.
   *
   * <p>The lower successor is the leftt-most child of the right child.
   * This is the next node in order that is also below this node.</p>
   *
   * @param stack The stack that is used to remember the traversal path.
   *
   * @return The lower successor node.
   */
  GBSNode lowerSuccessor(
    NodeStack     stack)
  {
    GBSNode  r     = rightChild();
    GBSNode  lastr = r;
    if (r != null)
      stack.push(NodeStack.DONE_VISITS, this);
    while (r != null)              /* Find left-most child of right child   */
    {
      if (r.leftChild() != null)
        stack.push(NodeStack.PROCESS_CURRENT, r);
      lastr = r;
      r = r.leftChild();
    }
    return lastr;
  }

  /**
   * Find the left-most child of a node by following all of the
   * left children down to the bottom of the tree.
   *
   * @param stack The InsertStack that is used to record the traversal.
   *
   * @return The left most child at the bottom of the tree.
   */
  GBSNode leftMostChild(
    InsertStack    stack)
  {
    stack.start(this, "GBSNode.leftMostChild");
    GBSNode l = leftChild();
    GBSNode lastl = this;
    while (l != null)
    {
      stack.push(0, l);
      lastl = l;
      l = l.leftChild();
    }
    return lastl;
  }

  /**
   * Used by test code to create an exact copy of an existing node.
   */
  public GBSNode testClone(
    GBSTree     target)
  {
    GBSNode p = new GBSNode(target, leftMostKey());
    for (int j = 1; j < population(); j++)
      p._nodeKey[j] = _nodeKey[j];
    p._population = _population;
    p.adjustMedian();
    return p;
  }

  /**
   * Used by test code to make sure that all of the keys within
   * a node are in the correct collating sequence.
   */
  public boolean validate()
  {
    boolean correct = true;
    if (population() > 1)
    {
      java.util.Comparator comp = index().insertComparator();
      int xcc = 0;
      for (int i = 0; i < population()-1; i++)
      {
        xcc = comp.compare(_nodeKey[i], _nodeKey[i+1]);
        if ( !(xcc < 0) )
          {
            System.out.println("Entry " + i + " not less than entry " + i+1);
            correct = false;
          }
      }
    }
    return correct;
  }

  /**
   * Return a String representation of the node.
   */
  public String toString()
  {
    StringBuffer buf = new StringBuffer();
    String x =
     "width = "          + width()          + ", " +
     "midPoint = "       + midPoint()       + ", " +
     "population = "     + population()     + ", " +
     "rightMostIndex = " + rightMostIndex() + ", " +
     "balance = "        + balance();
    buf.append(x);
    if (_nodeKey != null)
      {
        buf.append("\n ");
        buf.append("middleKey = "      + middleKey());
        if (population() != 0)
          {
            buf.append("\n");
            buf.append("{");
            for (int i = 0; i <= rightMostIndex(); i++)
            {
              buf.append("[" + i + "]: " + _nodeKey[i]);
              if (i != rightMostIndex())
                buf.append(", ");
            }
            buf.append("}\n ");
          }
        if (leftChild() != null)
          buf.append("Has left child.");
        else
          buf.append("Left child null.");
        if (rightChild() != null)
          buf.append("  Has right child.");
        else
          buf.append("  Right child null.");
      }
    String y = buf.toString();
    return y;
  }


  private GBSNode   _leftChild;
  private GBSNode   _rightChild;
  private short     _balance;
  private short     _population;
  private GBSTree   _index;

  private Object[]  _nodeKey;

}
