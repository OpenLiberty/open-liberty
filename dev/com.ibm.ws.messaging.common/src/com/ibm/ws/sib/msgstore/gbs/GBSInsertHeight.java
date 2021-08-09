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
package com.ibm.ws.sib.msgstore.gbs;

/**
 * Height balance (on insert) of a GBS tree.
 *
 * <p>This is a singleton class (only one instance ever exists) with no
 * state of its own.  It rebalances a fringe after a delete operation.
 * It is packaged as a separate class only to keep the size of the main
 * file (GBSTree.java) a manageable size.</p>
 *
 * <p>INSERT starts by searching down the tree for the place where they
 * key value would belong were it already in the tree.  When this
 * position is found, there will be a vacant space for the new leaf node
 * that will be added.  Adding the node could result in an imbalance
 * which would require adjustments to restore balance to the tree.
 * These adjustments are completely symmetrical.  To simplify the
 * discussion, we assume that all imbalances are caused by making the
 * tree right-heavy.  Symmetrical operations will restore the balance in
 * an identical manner to left-heavy trees.</p>
 *
 * <p>As an example, assume we are starting with the tree immediately
 * below and adding a new key value of 907.</p>
 *
 * <pre>
 *              *--------------163--------------*
 *              |               +               |
 *              |                               |
 *              |                               |
 *             137                      *------568------*
 *              0                       |       0       |
 *                                      |               |
 *                                      |               |
 *                                     218             808
 *                                      0               0
 * </pre>
 *
 * <p>The tree is already right-heavy and 907 > 163, so we are adding a
 * right node to a right-heavy tree.  Node 163 is the balance point (the
 * point around which the re-balancing will occur) because it is the
 * first node above the added node that has a nonzero balance factor.
 * After adding the new node, we adjust all of the balance factors
 * between the balance point and the new node as follows:</p>
 *
 * <pre>
 *              *--------------163--------------*
 *              |               +               |
 *              |                               |
 *              |                               |
 *             137                      *------568------*
 *              0                       |       +       |
 *                                      |               |
 *                                      |               |
 *                                     218             808--*
 *                                      0               +   |
 *                                                          |
 *                                                          |
 *                                                         907
 *                                                          0
 * </pre>
 *
 * <p>At this point, the balance point (163) has a right sub-tree whose
 * height is greater than its left by two.  We need to do an RR rotation
 * to restore the balance.  It is called an RR rotation because the
 * imbalance has been caused by the Right son (R) acquiring a Right
 * child (R).  To restore the balance in this case we elevate the right
 * child to the position of the balance point.  This drops the balance
 * point down to the former position of its left child.  The left child
 * of the newly elevated son becomes the right child of the newly
 * lowered balance point.  At this point balance has been restored and
 * the sub-tree rooted at the balance point has also been restored to
 * its original height.  Thus no further rotations farther up the tree
 * are required.</p>
 *
 * <pre>
 *              *--------------568--------------*
 *              |               0               |
 *              |                               |
 *              |                               |
 *      *------163------*                      808------*
 *      |       0       |                       +       |
 *      |               |                               |
 *      |               |                               |
 *     137             218                             907
 *      0               0                               0
 * </pre>
 *
 * <p>An imbalance could also have been caused by adding a key value of
 * 175 to the same tree.</p>
 *
 * <pre>
 *              *--------------163--------------*
 *              |               +               |
 *              |                               |
 *              |                               |
 *             137                      *------568------*
 *              0                       |       0       |
 *                                      |               |
 *                                      |               |
 *                                     218             808
 *                                      0               0
 * </pre>
 *
 * <p>The tree is already right-heavy and 175 > 163, so we are adding a
 * right node to a right-heavy tree.  Node 163 is the balance point (the
 * point around which the re-balancing will occur because it is the
 * first node above the added node that has a nonzero balance factor.
 * After adding the new node, we adjust all of the balance factors
 * between the balance point and the new node as follows:</p>
 *
 * <pre>
 *              *--------------163--------------*
 *              |               +               |
 *              |                               |
 *              |                               |
 *             137                      *------568------*
 *              0                       |       -       |
 *                                      |               |
 *                                      |               |
 *                                  *--218             808
 *                                  |   -               0
 *                                  |
 *                                  |
 *                                 175
 *                                  0
 * </pre>
 *
 * <p>Once again the right sub-tree of the balance point is two higher
 * than the left sub-tree.  But this time the imbalance has been caused
 * by the Right son (R) acquiring a Left son (L).  We need to do an RL
 * Rotation.</p>
 *
 * <pre>
 *                      *--------------218--------------*
 *                      |               0               |
 *                      |                               |
 *                      |                               |
 *              *------163------*                      568------*
 *              |       0       |                       +       |
 *              |               |                               |
 *              |               |                               |
 *             137             175                             808
 *              0               0                               0
 * </pre>
 *
 * <p>The parent of the new node is elevated to the position of .  the
 * new balance point.  This has the effect of forcing the new node to
 * the left of the balance point to restore the balance.  The former
 * balance point becomes the left child of the elevated node.  The new
 * node becomes the right child of the former balance point.  As in RR
 * rotation, the final depth of both sub-trees below the balance point
 * is the same as the depth before the insertion.  No further rotations
 * are required in order to restore the balance.</p>
 *
 * @author Stewart L. Palmer
 */

class GBSInsertHeight
{
  /**
   * <p>The two arrays newBalance1 and newBalance2 are used to derive
   * the new balance factors for the two children of the elevated node
   * based on the balance factor fo the elevated node.  This
   * relationship may be derived from the fact that the two new children
   * of the elevated node inherit the children.</p>
   */
  private static final int
                newBalance1[]
                  = {
                     0,            /* -1                                    */
                     0,            /*  0                                    */
                    -1};           /* +1                                    */
  private static final int
                newBalance2[]
                  = {
                     1,            /* -1                                    */
                     0,            /*  0                                    */
                     0};           /* +1                                    */

  /**
   * Fetch the singleton instance of this class.
   *
   * @return The singleton instance of the class.
   */
  static GBSInsertHeight singleInstance()
  {
    if (_singleton == null)
      _singleton = new GBSInsertHeight();
    return _singleton;
  }

  /**
   * Reference to the singleton instance of the class.
   */
  private static GBSInsertHeight  _singleton;

  /**
   * Restore the height balance of a tree following an insert.
   *
   * @param stack The NodeStack used to do the insert.
   * @param q The root of the newly added fringe.
   */
  void balance(
    NodeStack     stack,
    GBSNode       q)
  {
    GBSNode p;
    int bpidx = stack.balancePointIndex();
    int x = bpidx;
    GBSNode bpoint = stack.node(x);
    GBSNode bfather = stack.node(x-1);

    /* Adjust balance factors in intervening nodes                          */
    if (bpoint.leftChild() == stack.node(x+1))
      p = bpoint.leftChild();
    else
      p = bpoint.rightChild();
    x++;
    while (p != q)
    {
      if (p.leftChild() == stack.node(x+1))
        {                          /* We followed and added to left path    */
          p.setBalance(-1);        /* It is now left heavy                  */
          p = p.leftChild();
        }
      else                         /* We followed and added to right path   */
        {
          p.setBalance(1);         /* It is now right heavy                 */
          p = p.rightChild();
        }
      x++;
    }

    /* Adjust the balance factor at the balance point.                      */
    /* Re-balance if necessary.                                             */
  if (bpoint.leftChild() == stack.node(bpidx+1))
    {                              /* Added to left side                    */
      int bpb = bpoint.balance();
      switch (bpb)
      {
        case 0:
          bpoint.setBalance(-1);
          break;
        case 1:
          bpoint.clearBalance();
          break;
        case -1:
          rotateLeft(bfather, bpoint);
          break;
        default:
          String zzz1 = "Help1 !, bpb = " + bpb;
          throw new RuntimeException(zzz1);
      }
    }
  else                             /* Added to right side                   */
    {
      int bpb = bpoint.balance();
      switch (bpb)
      {
        case 0:
          bpoint.setBalance(1);
          break;
        case -1:
          bpoint.clearBalance();
          break;
        case 1:
          rotateRight(bfather, bpoint);
          break;
        default:
          String zzz2 = "Help2 !, bpb = " + bpb;
          throw new RuntimeException(zzz2);
      }
    }
  }

  /**
   * Do an LL or LR rotation.
   *
   * @param bfather The father of the balance point.
   * @param bpoint  The balance point.
   */
  private void rotateLeft(
    GBSNode       bfather,
    GBSNode       bpoint)
  {
    GBSNode bson = bpoint.leftChild();
    if (bson.balance() == -1)      /* Single LL rotation                    */
      {
        bpoint.setLeftChild(bson.rightChild());
        bson.setRightChild(bpoint);
        if (bfather.rightChild() == bpoint)
          bfather.setRightChild(bson);
        else
          bfather.setLeftChild(bson);
        bpoint.clearBalance();
        bson.clearBalance();
      }
    else                           /* Double LR rotation                    */
      {
        GBSNode blift = bson.rightChild();
        bson.setRightChild(blift.leftChild());
        blift.setLeftChild(bson);
        bpoint.setLeftChild(blift.rightChild());
        blift.setRightChild(bpoint);
        if (bfather.rightChild() == bpoint)
          bfather.setRightChild(blift);
        else
          bfather.setLeftChild(blift);
        bpoint.setBalance(newBalance2[blift.balance()+1]);
        bson.setBalance(  newBalance1[blift.balance()+1]);
        blift.clearBalance();
      }
  }

  /**
   * Do an RR or RL rotation.
   *
   * @param bfather The father of the balance point.
   * @param bpoint  The balance point.
   */
  private void rotateRight(
    GBSNode       bfather,
    GBSNode       bpoint)
  {
                                   /* Right child of balance point          */
    GBSNode bson = bpoint.rightChild();
    if (bson.balance() == 1)       /* Single RR rotation                    */
      {
        bpoint.setRightChild(bson.leftChild());
        bson.setLeftChild(bpoint);
        if (bfather.rightChild() == bpoint)
          bfather.setRightChild(bson);
        else
          bfather.setLeftChild(bson);
        bpoint.clearBalance();
        bson.clearBalance();
      }
    else                           /* Double RL rotation                    */
      {
        GBSNode blift = bson.leftChild();
        bson.setLeftChild(blift.rightChild());
        blift.setRightChild(bson);
        bpoint.setRightChild(blift.leftChild());
        blift.setLeftChild(bpoint);
        if (bfather.rightChild() == bpoint)
          bfather.setRightChild(blift);
        else
          bfather.setLeftChild(blift);
        bpoint.setBalance(newBalance1[blift.balance()+1]);
        bson.setBalance(  newBalance2[blift.balance()+1]);
        blift.clearBalance();
      }
  }

}
