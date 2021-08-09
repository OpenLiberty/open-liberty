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
 * Height balance (on delete) of a GBS tree.
 *
 * <p>This is a singleton class (only one instance ever exists) with no
 * state of its own.  It rebalances a fringe after a delete operation.
 * It is packaged as a separate class only to keep the size of the main
 * file (GBSTree.java) a manageable size.</p>
 *
 * <p>We are passed a DeleteStack of node addresses through which the
 * search for the delete node has progressed.  The first entry in the
 * stack is the dummy top node which points to the root.  The second
 * entry is the root itself.  The last entry is the node actually being
 * deleted.</p>
 *<pre>
 *              stack(0)        -> Dummy top node
 *              stack(1)        -> Root
 *                   ...
 *              stack(npoints)  -> Node being deleted
 *</pre>
 *
 * <p>Note that if "npoints" is equal to one, then the node being
 * deleted is the root itself.  In this case, all we have to do is zero
 * the balance factor of the dummy top node and we are done.</p>
 *
 * <p>The following notes pertain to rebalancing an AVL tree following a
 * delete operation.  Since a GBS Tree is just an AVL tree with large
 * nodes, the rebalancing algorithm for a GBS Tree is the same as that
 * for an AVL Tree.  In all of the diagrams that follow, the trees are
 * shown with one key per node.  The GBS Tree has the same structural
 * properties but usually holds about a dozen keys per node.  One
 * important difference is that the unit of height balance for a GBS
 * tree is not a node but is a whole fringe, whose size and depth
 * depends on the K factor for the tree.</p>
 *
 * <p>DELETE is conceptually similar to insert but has a few important
 * differences worthy of note.  For one thing, one cannot directly
 * delete a node with two children.  Where its parent used to have one
 * child, it will suddenly have two and will become a ternary, rather
 * than a binary, node.  If we need to delete a node with two children,
 * we must replace it with its predecessor or successor value and then
 * delete the node that used to hold the predecessor or successor.  Let
 * us say that we wish to delete key value 210 from the following tree.
 * the node 210 has two children.  Since it is already right-heavy, we
 * decide to select the successor node from the right sub-tree, hoping
 * to avoid a re-balancing.  The successor is the left-most node of the
 * right sub-tree (node 226).</p>
 *<pre>
 *               *--------------210--------------*
 *               |               +               |
 *               |                               |
 *               |                               |
 *       *------149------*               *------542------*
 *       |       0       |               |       +       |
 *       |               |               |               |
 *       |               |               |               |
 *      141--*          166--*       *--426          *--758--*
 *       +   |           +   |       |   -           |   0   |
 *           |               |       |               |       |
 *           |               |       |               |       |
 *          144             195     226            *676     877*
 *           0               0       0             | -       + |
 *                                                 |           |
 *                                                 |           |
 *                                                569         935
 *                                                 0           0
 *</pre>
 * <p>We copy its value up to the former 210 node and we then physically
 * delete node 226.  This guarantees that we are always deleting either
 * a leaf or a half-leaf.  This deletion has caused an imbalance,
 * however.  This brings us to our next difference between INSERT and
 * DELETE.  The resulting tree is imbalanced.  Its right sub-tree is two
 * higher than its left.  We will require an RR rotation but with a
 * significant difference from the INSERT case.  The right sub-tree that
 * is triggering the rotation is already balanced.  In the RR case of
 * insertion, the right sub-tree becomes excessively right-heavy as the
 * result of adding a new node to the right side.  Since the addition
 * caused the imbalance, the resulting sub-tree could not be balanced
 * since the creation of a balanced sub-tree causes no change in tree
 * height.  In the case of deletion we are causing a right-heavy
 * sub-tree by deleting a left node.  The resulting right-heavy sub-tree
 * could be balanced.  In the INSERT case, a right-heavy son causes a
 * right imbalance and the RR rotation restores equilibrium to both the
 * son and the balance point.</p>
 *<pre>
 *               *--------------226--------------*
 *               |               +               |
 *               |                               |
 *               |                               |
 *       *------149------*               *------542------*
 *       |       0       |               |       +       |
 *       |               |               |               |
 *       |               |               |               |
 *      141--*          166--*          426          *--758--*
 *       +   |           +   |                       |   0   |
 *           |               |                       |       |
 *           |               |                       |       |
 *          144             195                    *676     877*
 *           0               0                     | -       + |
 *                                                 |           |
 *                                                 |           |
 *                                                569         935
 *                                                 0           0
 *</pre>
 * <p>After the RR rotations is complete, the tree is as shown below.
 * Note that, unlike the INSERT case, this RR Rotation started with a
 * balanced right son and ended with an unbalanced son and balance
 * point.  The INSERT RR Rotation started with an unbalanced right son
 * and ended with a balanced son and balance point.</p>
 *<pre>
 *               *--------------226--------------*
 *               |               +               |
 *               |                               |
 *               |                               |
 *       *------149------*               *------758------*
 *       |       0       |               |       -       |
 *       |               |               |               |
 *       |               |               |               |
 *      141--*          166--*       *--542--*          877--*
 *       +   |           +   |       |   +   |           +   |
 *           |               |       |       |               |
 *           |               |       |       |               |
 *          144             195     426    *676             935
 *           0               0       0     | -               0
 *                                         |
 *                                         |
 *                                        569
 *                                         0
 *</pre>
 * <p>Another consideration that is unique to delete is the potential
 * number of rotations.  In worst case, up to log(n) rotations may be
 * required as opposed to INSERT which has a worst case of one rotation.
 * Note that the rotation operations done for INSERT all have the effect
 * of reducing the height of the rotated sub-tree by one.  The only
 * rotation that does not do so is the RR Rotation that involves a
 * balanced right son.  Since this case cannot happen during an INSERT,
 * all INSERT rotations reduce the height of the rotated sub-tree by
 * one.  If we add a right node below a balanced node, it becomes
 * right-heavy and there is no need for a rotation.  If we add a right
 * node below a right-heavy node, the rotation operation restores the
 * right-heavy node to its original height.  Since its original height
 * has been restored, it cannot trigger further rotations above it.  In
 * the case of DELETE, a rotation may reduce the height of the sub-tree
 * which could cause an imbalance further up the tree which could
 * require another rotation to re-balance.</p>
 *
 * <p>As an example, consider the following tree.  It is a worst case
 * example which happens to be a Fibonacci tree.  It is as left-heavy as
 * one can get and still be an AVL tree.</p>
 *<pre>
 *                    *--------------128--------------*
 *                    |               -               |
 *                    |                               |
 *                    |                               |
 *            *------125------*               *------131------*
 *            |       -       |               |       -       |
 *            |               |               |               |
 *            |               |               |               |
 *        *--123--*       *--127          *--130             132
 *        |   -   |       |   -           |   -               0
 *        |       |       |               |
 *        |       |       |               |
 *      *122     124     126             129
 *      | -       0       0               0
 *      |
 *      |
 *     121
 *      0
 *</pre>
 * <p>If we delete the right-most node (132), we end up with the
 * following tree:</p>
 *<pre>
 *                    *--------------128--------------*
 *                    |               -               |
 *                    |                               |
 *                    |                               |
 *            *------125------*               *------131
 *            |       -       |               |       -
 *            |               |               |
 *            |               |               |
 *        *--123--*       *--127          *--130
 *        |   -   |       |   -           |   -
 *        |       |       |               |
 *        |       |       |               |
 *      *122     124     126             129
 *      | -       0       0               0
 *      |
 *      |
 *     121
 *      0
 *</pre>
 * <p>It is clear that the node at 131 requires an LL Rotation to
 * re-balance it.  After the rotation, we have the following:</p>
 *<pre>
 *
 *                    *--------------128--------------*
 *                    |               -               |
 *                    |                               |
 *                    |                               |
 *            *------125------*               *------130------*
 *            |       -       |               |       0       |
 *            |               |               |               |
 *            |               |               |               |
 *        *--123--*       *--127             129             131
 *        |   -   |       |   -               0               0
 *        |       |       |
 *        |       |       |
 *      *122     124     126
 *      | -       0       0
 *      |
 *      |
 *     121
 *      0
 *</pre>
 * <p>The right sub-tree of node 128 has a depth of two but the left
 * sub-tree of 128 has a depth of four.  A second LL rotation around
 * node 128 is required to restore balance and gives us the following
 * tree:</p>
 *<pre>
 *
 *                    *--------------125--------------*
 *                    |               0               |
 *                    |                               |
 *                    |                               |
 *            *------123------*               *------128------*
 *            |       -       |               |       0       |
 *            |               |               |               |
 *            |               |               |               |
 *        *--122             124          *--127          *--130--*
 *        |   -               0           |   -           |   0   |
 *        |                               |               |       |
 *        |                               |               |       |
 *       121                             126             129     131
 *        0                               0               0       0
 *</pre>
 *
 * @author Stewart L. Palmer
 */


class GBSDeleteHeight
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
  static GBSDeleteHeight singleInstance()
  {
    if (_singleton == null)
      _singleton = new GBSDeleteHeight();
    return _singleton;
  }

  /**
   * Reference to the singleton instance of the class.
   */
  private static GBSDeleteHeight  _singleton;

  /**
   * Restore height balance following a fringe raveling caused by a delete.
   *
   * @param stack The DeleteStack through which the delete path traversed.
   * @param ntop  The index within the DeleteStack of the parent of the
   *              t0 fringe that was raveled.
   */
  void balance(
    DeleteStack   stack,
    int           ntop)
  {
    DeleteStack.HeightNote xx = stack.heightNote();
    ntop--;
    GBSNode bnext = stack.node(ntop + 1);/* Set initial "next" point        */
    for (int i = ntop; i > 0; i--) /* Work UP through the balance points    */
    {
      xx.depthDecrease = false;
      reBalance(xx, stack.node(i-1), stack.node(i), bnext);
      bnext = xx.bnew;
      if ( !xx.depthDecrease )     /* Depth did not change                  */
        break;                     /* So we are re-balanced                 */
    }
  }

  /**
   * Method reBalance.
   *
   * @param xx HeightNote
   * @param bparent Parent of the balance point
   * @param bpoint The balance point
   * @param bnext Next child below balance point
   */
  private void reBalance(
    DeleteStack.HeightNote   xx,
    GBSNode                  bparent,
    GBSNode                  bpoint,
    GBSNode                  bnext)
  {
    xx.bnew = bpoint;              /* Default for next time                 */
    if (bpoint.leftChild() == bnext)/* We deleted from left side            */
      {                            /* Check for RR or RL Rotation           */
        switch (bpoint.balance())  /* Look at ORIGINAL Balance Factor       */
        {
          case 0:                  /* Have deleted a left node of a balanced*/
            bpoint.setBalance(1);  /*  tree.  Now right heavy.              */
            break;
          case 1:                  /* Deleted left node of right heavy      */
            rotateRight(xx, bparent, bpoint);/* Need to re-balance          */
            break;
          case -1:                 /* Deleted left node of left-heavy tree  */
            bpoint.clearBalance(); /* It becomes balanced & depth decreases */
            xx.depthDecrease = true;
            break;
          default:
            throw new RuntimeException("bpoint.balance() = " + bpoint.balance());
        }
      }
    else                           /* We deleted from right side            */
      {                            /* Check for LL or LR Rotation           */
        switch (bpoint.balance())  /* Look at ORIGINAL Balance Factor       */
        {
          case 0:                  /* Deleted a right node of a balanced    */
            bpoint.setBalance(-1); /*  tree.  Now left-heavy                */
            break;
          case -1:                 /* Deleted right node of left-heavy      */
            rotateLeft(xx, bparent, bpoint);/* Need to re-balance           */
            break;
          case 1:                  /* Deleted a right node of a right-heavy */
            bpoint.clearBalance(); /* It become balanced & depth decreases  */
            xx.depthDecrease = true;
            break;
          default:
            throw new RuntimeException("bpoint.balance() = " + bpoint.balance());
        }
      }
  }


  private void rotateLeft(
    DeleteStack.HeightNote  xx,
    GBSNode                 bparent,
    GBSNode                 bpoint)
  {
                                   /* Left child of balance point           */
    GBSNode bson = bpoint.leftChild();
    if (bson.balance() <= 0)       /* Single LL rotation                    */
      {
        bpoint.setLeftChild(bson.rightChild());
        bson.setRightChild(bpoint);
        if (bparent.rightChild() == bpoint)
          bparent.setRightChild(bson);
        else
          bparent.setLeftChild(bson);
        xx.bnew = bson;
        if (bson.balance() == 0)
          {
            bpoint.setBalance(-1);
            bson.setBalance(1);
          }
        else
          {
            bpoint.clearBalance();
            bson.clearBalance();
            xx.depthDecrease = true;
          }
      }
    else                           /* Double LR rotation                    */
      {
        GBSNode blift = bson.rightChild();
        bson.setRightChild(blift.leftChild());
        blift.setLeftChild(bson);
        bpoint.setLeftChild(blift.rightChild());
        blift.setRightChild(bpoint);
        if (bparent.rightChild() == bpoint)
          bparent.setRightChild(blift);
        else
          bparent.setLeftChild(blift);
        xx.bnew = blift;
        bpoint.setBalance(newBalance2[blift.balance()+1]);
        bson.setBalance(  newBalance1[blift.balance()+1]);
        blift.clearBalance();
        xx.depthDecrease = true;
      }
  }

  private void rotateRight(
    DeleteStack.HeightNote  xx,
    GBSNode                 bparent,
    GBSNode                 bpoint)
  {
                                   /* Right child of balance point          */
    GBSNode bson = bpoint.rightChild();
    if (bson.balance() >= 0)       /* Single RR rotation                    */
      {
        bpoint.setRightChild(bson.leftChild());
        bson.setLeftChild(bpoint);
        if (bparent.rightChild() == bpoint)
          bparent.setRightChild(bson);
        else
          bparent.setLeftChild(bson);
        xx.bnew = bson;
        if (bson.balance() == 0)
          {
            bpoint.setBalance(1);
            bson.setBalance(-1);
          }
        else
          {
            bpoint.clearBalance();
            bson.clearBalance();
            xx.depthDecrease = true;
          }
      }
    else                           /* Double RL rotation                    */
      {
        GBSNode blift = bson.leftChild();
        bson.setLeftChild(blift.rightChild());
        blift.setRightChild(bson);
        bpoint.setRightChild(blift.leftChild());
        blift.setLeftChild(bpoint);
        if (bparent.rightChild() == bpoint)
          bparent.setRightChild(blift);
        else
          bparent.setLeftChild(blift);
        xx.bnew = blift;
        bpoint.setBalance(newBalance1[blift.balance()+1]);
        bson.setBalance(  newBalance2[blift.balance()+1]);
        blift.clearBalance();
        xx.depthDecrease = true;
      }

  }

}
