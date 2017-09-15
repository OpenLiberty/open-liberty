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
 * A stack of remembered nodes.
 *
 * <p>Every operation that might modify the tree (insert, delete) uses
 * this stack (or a derived class of this stack) to remember the nodes
 * that were traversed during the search phase of the operation.  Should
 * the tree require any rebalancing this stack of nodes is used then for
 * such operations as finding the parent of the current fringe, walking
 * through the fringe, and finding the most recently passed point of
 * height imbalance in the tree.</p>
 *
 * <p>Binary tree walking is most naturally implemented as a recursive
 * function as follows:</p>
 *<pre>
 *      void visit(GBSNode p)
 *      {
 *        if (p.leftChild() != null)         There is a left node
 *          visit(p.leftChild())             Visit left node
 *
 *        <Process current node>
 *
 *        if (p.rightChild() != null)        There is a right node
 *          visit(p.rightChild())            Visit right node
 *      }
 *</pre>
 *
 * <p>The tree walking done here represents a linear unrolling of the
 * recursive algorithm with a stack of nodes to remember the path
 * traversed through the tree and an associated state variable with each
 * node indicating whether the next action is to visit the left child,
 * visit the right child, process the current node, or terminate.</p>
 */
public abstract class NodeStack
{
  public NodeStack(
    GBSTree       tree)
  {
    _tree = tree;
    _node = new GBSNode[nmax];
    _state = new int[nmax];
    nodeStackReset();
  }

  public void reset()
  {
    nodeStackReset();
  }

  private void nodeStackReset()
  {
    _idx = -1;
    _cidx = -1;
    _bpidx = -1;
    _topIndex = -222;
    _endp = null;
    _endIndex = -111;
    _maxIdx = -1;
  }

  /**
   * Visit left sub-tree
   */
  static final int VISIT_LEFT       = 1;

  /**
   * Process current node
   */
  static final int PROCESS_CURRENT  = 2;

  /**
   * Visit right sub-tree
   */
  static final int VISIT_RIGHT      = 3;

  /**
   * Restore state after visits
   */
  static final int DONE_VISITS      = 4;

  /**
   * Start the stack with the root of the tree.
   */
  private void stackStart(
    GBSNode     node)
  {
    _idx = 0;
    _cidx = 0;
    _state[_cidx] = 0;
    _node[_cidx]  = node;
    _bpidx = _cidx + 1;
    _maxIdx = _cidx;
  }

  /**
   * Start the stack with the root of the tree.
   */
  protected void stackStart(
    GBSNode     node,
    String      starter)
  {
    _starter = starter;
    _lastStacker = null;
    stackStart(node);
  }

  void setStacker(
    String     stacker)
  { _lastStacker = stacker; }

  /**
   * Push a node and associated state onto the stack.
   */
  public void push(
    int         state,
    GBSNode     node,
    String      pusher)
  {
    _lastStacker = pusher;
    push(state, node);
  }

  /**
   * Push a node and associated state onto the stack.
   */
  public void push(
    int         state,
    GBSNode     node)
  {
      _cidx++; _idx = _cidx;
      if (_cidx > GBSTree.maxDepth)
        throw new OptimisticDepthException(
          "maxDepth (" + GBSTree.maxDepth +
                   ") exceeded.  Depth is " + _cidx + ".");
//    _idx++;
//    _cidx = _idx;
//    _endIndex = _idx;
    _state[_cidx] = state;
    _node[_cidx]  = node;
    if (index() > _maxIdx)
      _maxIdx = index();
  }

  /**
   * Push a node onto the stack.
   */
  void push(
    GBSNode     node)
  {
    push(0, node);
  }

  void printSummary()
  {
    System.out.println("balancePointIndex = " + balancePointIndex());
    for (int i = 0; i <= _cidx; i++)
    {
      System.out.println(i + ": " + node(i));
    }
  }

  void reset(
    int     idx)
  {
    if (idx < 0)
      throw new RuntimeException("idx < 0, idx = " + idx + ".");
    _idx = idx;
    _cidx = idx;
  }

  /**
   * Push a node and associated state onto the stack, remembering
   * a height imbalance if there is one.
   */
  void balancedPush(
    int         state,
    GBSNode     node)
  {
    push(state, node);
    if (node.balance() != 0)
      _bpidx = _cidx;
  }

  void pop()
  {
    _cidx--;  _idx = _cidx;
//    _cidx = _idx;
//    _idx--;
//    _endIndex = _idx;
  }

  public int index()
  { return _cidx; }

  void resetBalancePointIndex()
  { _bpidx = -2; }

  int balancePointIndex()
  { return _bpidx; }

  /**
   * @return the node in the stack at index x.
   */
  GBSNode node(
    int         x)
  {
    return _node[x];
  }

  /**
   * @return the current node in the stack.
   */
  GBSNode node()
  { return _node[_cidx]; }

  /**
   * @return the parent of the current node.  Null if it has no parent.
   */
  public GBSNode parent()
  {
    GBSNode p = null;
    if (_cidx > 0)
      p = _node[_cidx-1];
    return p;
  }

  /**
   * @return the state of the current node in the stack.
   */
  int state()
  { return _state[_cidx]; }

  void setNode(
    int         x,
    GBSNode     node)
  { _node[x] = node; }

  /**
   * Walk through a fringe of the tree.
   *
   * <p>We are positioned at a node that is within a fringe of a tree.
   * This method will go through every node in the fringe, invoking
   * processNode on every node in the tree.  processNode is an abstract
   * method that is implemented by all subclasses.</p>
   *
   * @param p The node at which to start.
   *
   */
  void processSubFringe(
    GBSNode      p)
  {
                                   /* Depth of a balanced T-zero tree for   */
                                   /*  this tree's K factor                 */
    int t0_depth = _tree.tZeroDepth();

    /* Determine top-most node we must visit in the tree walk               */
    int ntop = 0;                  /* This is the index within the          */
                                   /*  NodeStack that represents the top    */
                                   /*  of the sub-tree.  We start by        */
                                   /*  assuming we have to walk the whole   */
                                   /*  tree.                                */
    int xChild = 0;                /* Assume current node has no children   */
    if (p.hasChild())
      xChild = 1;                  /* Allow for terminal child              */
    if (index() > t0_depth)        /* Have at least a whole t0 sub-tree     */
      ntop = index() - (t0_depth - xChild);
    innerProcessSubTree(p, NodeStack.VISIT_RIGHT, ntop);
  }

  /**
   * Walk through an entire sub-tree, invoking processNode on each node.
   *
   * @param p The node at which to start.
   */
  public void processSubTree(
    GBSNode      p)
  {
    stackStart(p, "NodeStack.processSubTree");
    innerProcessSubTree(p, NodeStack.VISIT_LEFT, 0);
  }

  /**
   * Walk through an entire sub-tree, invoking processNode on each node.
   *
   * <p>Walk through a sub-tree, invoking processNode on each node.
   * processNode is an abstract megthod that is implemented by
   * subclasses.</p>
   *
   * @param p The node at which to start.
   * @param initialState The initial state of the walk, which is one of
   *                     VISIT_LEFT or VISIT_RIGHT.  VISIT_LEFT is the
   *                     initial state for visiting a whole sub-tree.
   *                     VISIT_RIGHT is the initial state for visiting a
   *                     sub-fringe.
   * @param topIndex The index within the NodeStack that is considered
   *                 to be the top of the tree.
   */
  private void innerProcessSubTree(
    GBSNode      p,
    int          initialState,
    int          topIndex)
  {
    boolean done = false;
    _topIndex = topIndex;
    _endp = p;
    _endIndex = _idx;
    GBSNode q;                     /* Used for tree walking                 */
    int s = initialState;

    while ( !done )
    {
      switch(s)
      {
        case NodeStack.VISIT_LEFT:
          s = NodeStack.PROCESS_CURRENT;
          q = p.leftChild();
          while (q != null)
          {
            push(s, p);
            p = q;
            q = p.leftChild();
          }
          break;
        case NodeStack.PROCESS_CURRENT:
          s = NodeStack.VISIT_RIGHT;
          done = processNode(p);
          _endp = p;               /* Last node processed                   */
          _endIndex = _idx;        /* Index to parent of last node processed*/
          break;
        case NodeStack.VISIT_RIGHT:
          s = NodeStack.DONE_VISITS;
          q = p.rightChild();
          if (q != null)
            {
              push(s, p);
              s = NodeStack.VISIT_LEFT;
              p = p.rightChild();
            }
          break;
        case NodeStack.DONE_VISITS:
          if (_idx == topIndex)    /* Have finally hit end of sub-tree      */
            done = true;
          else
            {
              s = _state[_cidx];
              p = _node[_cidx];
              pop();
            }
          break;
        default:
          throw new RuntimeException("Help!, s = " + s + ".");
//        break;
      }                            /* switch(s)                             */

    }                              /* while ( !done )                       */
  }

  /**
   * The methods herein that walk
   */
  public abstract boolean processNode(
    GBSNode       p);

  GBSNode lastNode()
  { return _endp; }

  int lastIndex()
  { return _endIndex; }

  int topIndex()
  { return _topIndex; }

  protected GBSTree tree()
  { return _tree; }

  /**
   * Return the name of a node state
   */
  public String stateName(
    int      state)
  {
    String name = "Unknown state = " + state;
    switch(state)
    {
      case NodeStack.VISIT_LEFT:
        name = "VISIT_LEFT";
        break;
      case NodeStack.PROCESS_CURRENT:
        name = "PROCESS_CURRENT";
        break;
      case NodeStack.VISIT_RIGHT:
        name = "VISIT_RIGHT";
        break;
      case NodeStack.DONE_VISITS:
        name = "DONE_VISITS";
        break;
    }
    return name;
  }

  /**
   * Return a String represenation of the NodeStack
   */
  public String toString()
  {
    StringBuffer buf = new StringBuffer();
    buf.append("NodeStack, _idx = " + _idx + ", _cidx = " + _cidx +
               ", maxDepth() = " +
                maxDepth() + ", lastIndex() = " + lastIndex() + "\n" +
                ", starter = " + _starter +
                ", lastStacker = " + _lastStacker + "\n");
    if (maxDepth() >= 0)
      {
        for (int i = 0; i <= maxDepth(); i++)
        {
          GBSNode p = node(i);
          if (p == null)
            buf.append(i + ": " +     "???"       + "\n");
          else
            buf.append(i + ": " + p.lmkString() +
                       ", s = " + stateName(_state[i]) + "\n");
        }
      }
    return buf.toString();
  }

  private GBSTree _tree;
  private GBSNode _endp;
  private int     _endIndex;
  private int     _topIndex;

   /**
   * Maximum possible depth of a T0 sub_tree.
   *
   * Depth must allow for the sum of the following:
   *
   *   1. Longest path to lowest internal node,
   *
   *   2. Height of a t0 sub-tree (including its root) for the largest
   *      K factor, and
   *
   *   3. Maximum depth to which a sub-tree can grow before fringe
   *      rebalance takes place.
   */
  private static final int nmax = GBSTree.maxDepth + 7 + 128;

  /**
   * Stack of nodes through which a tree traversal has walked.
   */
  protected GBSNode[]  _node;

  /**
   * The state of the tree traversal at each node
   */
  protected int[]    _state;

  /**
   * Array index into _node[] and _state[] that constitute the
   * tree traversal stack.
   */
  protected   int      _idx;

  /**
   * This index points to the current item, whether it was made current
   * by push() or by pop().  _idx points to the current item only if
   * it became the current item by push.
   */
  private   int      _cidx;

  /**
   * The value of _cidx the last time a tree height imbalance was observed.
   */
  private int        _bpidx;

  public int maxDepth()
  { return _maxIdx; }

  /**
   * Maximum value remembered for index().
   */
  private int      _maxIdx;

  private String   _starter;
  private String   _lastStacker;

}
