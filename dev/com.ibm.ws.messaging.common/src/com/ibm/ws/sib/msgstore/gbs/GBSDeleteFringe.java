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
 * Fringe balance (on delete) of a GBS tree.
 *
 * <p>This is a singleton class (only one instance ever exists) with no
 * state of its own.  It rebalances a fringe after a delete operation.
 * It is packaged as a separate class only to keep the size of the main
 * file (GBSTree.java) a manageable size.</p>
 */
class GBSDeleteFringe
{

  /**
   * Fetch the singleton instance of this class.
   *
   * @return The singleton instance of the class.
   */
  static GBSDeleteFringe singleInstance()
  {
    if (_singleton == null) {
      _singleton = new GBSDeleteFringe();
    }
    return _singleton;
  }

  /**
   * Reference to the singleton instance of the class.
   */
  private static GBSDeleteFringe  _singleton;

  /**
   * Balance a fringe following delete.
   */
  void balance(
    int           t0_depth,
    DeleteStack   stack)

  {
    int ntop = stack.topIndex();
    InsertStack istack = stack.insertStack();
    DeleteStack.Linearizer lxx = stack.linearizer();
    DeleteStack.FringeNote  xx = stack.fringeNote();
    xx.depthDecrease = false;
    xx.conditionalDecrease = false;
    GBSNode f = stack.node(ntop);  /* Father of the t0 fringe               */
    if (ntop == 0)                 /* Final t0 fringe is to go away         */
      {                            /* Make final sub-tree a list            */
        f.setRightChild(linearize(lxx, f.rightChild()));
        return;                    /* Nothing left to do                    */
      }
    GBSNode g = stack.node(ntop-1);/* Grandparent of the fringe             */
    boolean gLeft = true;
    if (g.rightChild() == f) {
      gLeft = false;
    }

    if (f.leftChild() == stack.node(ntop+1)) { /* Deleted from left         */
      leftFringe(lxx, xx, istack, f, g, gLeft, t0_depth);
    }
    else {                                     /* Deleted from right        */
      rightFringe(lxx, xx, istack, f, g, gLeft);
    }
    lastFringe(xx, g, gLeft, stack, ntop, istack);
  }


  /**
   * Handle the case where we deleted from a left fringe.
   */
  private void leftFringe(
    DeleteStack.Linearizer       lxx,
    DeleteStack.FringeNote      xx,
    InsertStack   istack,
    GBSNode       f,
    GBSNode       g,
    boolean       gLeft,
    int           t0_depth)
  {

    GBSNode w = f.rightChild();    /* The right brother of delete fringe    */
    GBSNode s = f.leftChild();     /* t0 fringe from which we deleted       */
    GBSNode bfather = g;
    GBSNode sh = null;             /* The linearized s                      */
    GBSNode p = null;
    int fbalance = f.balance();
    f.clearBalance();
    if (w == null)                 /* There is no right brother             */
      {
        sh = linearize(lxx, s);
        f.setRightChild(null);
        combine(lastInList(sh), f);/* [s, f]                                */
        p = sh;
        xx.depthDecrease = true;
      }
    else                           /* There is a right brother              */
      {
        p = linearize(lxx, s);
        f.setChildren(null, null);
        combine(lastInList(p), f); /* [s, f]                                */
        sh = p;                    /* Assume linear list or single t0       */
        GBSNode wh = w.leftMostChild(istack);
        int zpoints = istack.index();
        if (zpoints > 0)           /* Is a t0 fringe (possibly more         */
          {                        /*  than one level down)                 */
            zpoints--;
            int ztop = 0;          /* Assume top of entire tree             */
            if (zpoints >= t0_depth)/* Have more than a whole t0 fringe     */
              {
                ztop = zpoints - t0_depth + 1;
                sh = w;            /* We will elevate w                     */
                bfather = istack.node(ztop-1);
                bfather.setLeftChild(p);/* Father of left-most fringe       */
                                   /*        points to linear list          */
              }
                                   /* Linearize left-most fringe            */
          wh = linearize(lxx, istack.node(ztop));
          }
        combine(lastInList(p), wh);/* Add w onto f  (now [s, f, w])         */
        leftDepth(xx, fbalance);   /* Check for depth change                */
      }
    if (gLeft) {
      g.setLeftChild(sh);
    }
    else {
      g.setRightChild(sh);
    }
    boolean bLeft = true;
    if (bfather.rightChild() == p) {
      bLeft = false;
    }
    int maxBal = g.kFactor() - 1;
    if (maxBal < 3) {
      maxBal = 3;
    }
    istack.start(bfather, "GBSDeleteFringe.leftFringe");
    istack.setNode(1, null);
    istack.resetBalancePointIndex();
    int fpidx = 1;
    if (g.kFactor() > 2) {
      GBSInsertFringe.singleInstance().balance(
                      g.kFactor(), istack, p, fpidx, maxBal);
    }
    xx.newg = bfather;
    if (bLeft) {
      xx.newf = bfather.leftChild();
    }
    else {
      xx.newf = bfather.rightChild();
    }
  }

  /**
   * Given the original balance factor from the original parent of
   * the t0 sub-tree from which we deleted from the left side,
   * determine the possible new balance factor and depth decrease
   * indicator.
   */
  private void leftDepth(
    DeleteStack.FringeNote      xx,
    int           fbalance)
  {
    switch (fbalance)              /* Original father balance factor        */
    {
      case -1:                     /* Have deleted from left of left-heavy  */
                                   /*  Is now balanced but depth has        */
        xx.depthDecrease = true;   /*  decreased.                           */
        break;
      case  0:                     /* Both sides are t0                     */
        /* G has lost one from whichever side.                              */
        /*   UNLESS lastFringe does an insert fringe re-balancing.          */
        /* In which case it remains balanced and no height change           */
        xx.conditionalDecrease = true;
        xx.conditionalBalance = 0;
        break;
      case  1:                     /* Right side has more than one t0       */
        /* Is now balanced.                                                 */
        /* G has lost one from whichever side.                              */
        /*   UNLESS lastFringe does an insert fringe re-balancing.          */
        /* If true, stack.node(ntop).balance = -1,  G has not lost anything */
        xx.conditionalDecrease = true;
        xx.conditionalBalance = -1;
        break;
      default:
        throw new RuntimeException("Help!  fbalance = " + fbalance);
    }
  }

  /**
   * Handle the case where we deleted from a right fringe.
   */
  private void rightFringe(
    DeleteStack.Linearizer       lxx,
    DeleteStack.FringeNote      xx,
    InsertStack   istack,
    GBSNode       f,
    GBSNode       g,
    boolean       gLeft)
  {

    GBSNode w = f.leftChild();     /* The left brother of delete fringe     */
    GBSNode s = f.rightChild();    /* t0 fringe from which we deleted       */
    int fbalance = f.balance();
    f.clearBalance();
    if (w == null)                 /* There is no left brother              */
      {
        boolean bLeft = true;
        if (g.rightChild() == f) {
          bLeft = false;
        }
        f.setRightChild(linearize(lxx, s));
        int maxBal = f.kFactor() - 1;
        if (maxBal < 3) {
          maxBal = 3;
        }
        istack.start(g, "GBSDeleteFringe.rightFringe");
        istack.setNode(1, null);
        istack.resetBalancePointIndex();
        int fpidx = 1;
        GBSInsertFringe.singleInstance().balance(
                      g.kFactor(), istack, f, fpidx, maxBal);
        xx.newg = g;
        if (bLeft) {
          xx.newf = g.leftChild();
        }
        else {
          xx.newf = g.rightChild();
        }
        xx.depthDecrease = true;
      }
    else                           /* There is a left brother               */
      {                            /* Elevate the left brother to the       */
        xx.newg = g;               /*  position of the current father.      */
        if (g.leftChild() == f) {  /*  This is an LL rotation to make up    */
          g.setLeftChild(w);       /*  for the disappearance of the         */
        }                          /*  right brother.                       */
        else {
          g.setRightChild(w);
        }
        f.setLeftChild(null);
        GBSNode p = w.rightMostChild();
        f.setRightChild(linearize(lxx, s));/* [f, s]                        */
        combine(p, f);             /* Now [w, f, s]. lastFringe cleans up   */
        xx.newf = w;               /* Remember new father                   */
        rightDepth(xx, fbalance);  /* Check for depth decrease              */
      }
  }

  /**
   * Given the original balance factor from the original parent of
   * the t0 sub-tree from which we deleted from the right side,
   * determine the possible new balance factor and depth decrease
   * indicator.
   */
  private void rightDepth(
    DeleteStack.FringeNote      xx,
    int           fbalance)
  {
    switch (fbalance)              /* Original father balance factor        */
    {
      case  1:                     /* Have deleted from right of right-heavy*/
                                   /*  Is now balanced but depth has        */
        xx.depthDecrease = true;   /*  decreased.                           */
        break;
      case  0:                     /* Both sides are t0                     */
        /* G has lost one from whichever side.                              */
        /*   UNLESS lastFringe does an insert fringe re-balancing.          */
        /* In which case it remains balanced and no height change           */
        xx.conditionalDecrease = true;
        xx.conditionalBalance = 0;
        break;
      case -1:                     /* Left side has more than one t0        */
        /* Is now balanced.                                                 */
        /* G has lost one from whichever side.                              */
        /*   UNLESS lastFringe does an insert fringe re-balancing.          */
        /* If true, stack.node(ntop).balance = +1,  G has not lost anything */
        xx.conditionalDecrease = true;
        xx.conditionalBalance = 1;
        break;
      default:
        throw new RuntimeException("Help!  fbalance = " + fbalance);
    }
  }

  /**
   * Examine the final fringe and re-balance if necessary.
   *
   * <p>Examine the fringe that was produced by either rightFringe or
   * lastFringe.  As this fringe may have resulted from the combination
   * of one or more partial fringes it may now require input fringe
   * rebalancing.</p>
   *
   * @param g grandparent
   * @param gLeft If true, f was left son of g
   * @param stack The NodeStack for delete
   * @param ntop Index within stack of t0 parent
   * @param istack A scratch InsertStack
   * @param newg New t0 grandparent
   * @param newf New t0 parent
   */
  private void lastFringe(
    DeleteStack.FringeNote      xx,
    GBSNode       g,
    boolean       gLeft,
    DeleteStack   stack,
    int           ntop,
    InsertStack   istack)
  {
    if (gLeft) {                    /* Original father was left child        */
      stack.setNode(ntop, g.leftChild());/* Put new left parent on stack    */
    }
    else {                           /* Original parent was right child       */
      stack.setNode(ntop, g.rightChild());/* Put new right parent on stack  */
    }
    GBSNode p = xx.newf;
    GBSNode q = null;
    istack.start(xx.newg, "GBSDeleteFringe.lastFringe");
    istack.resetBalancePointIndex();
    GBSNode fpoint = null;
    int fdepth = 0;
    int fpidx = 0;
    while (p != null)
    {
      fdepth++;
      istack.push(p);
      if (fpoint == null)
      {
        if (p.leftChild() == null) /* First right-heavy half leaf           */
        {
          fdepth = 1;              /* Reset fringe depth                    */
          fpoint = p;              /* Remember fringe balance point         */
          fpidx = istack.index();  /* Remember index to fringe balance pt.  */
        }
      }
      q = p;
      p = p.rightChild();
    }

    int maxBal = g.kFactor() + 1;
    if ((g.kFactor() % 3) == 0) {
      maxBal = g.kFactor() + 2;
    }
    if ( (fdepth > maxBal) ||
         ((fdepth >= maxBal) &&
          (q.isFull())) )
    {
      GBSInsertFringe.singleInstance().balance(
                    g.kFactor(), istack, fpoint, fpidx, maxBal);
      if (xx.conditionalDecrease) {
        stack.node(ntop).setBalance(xx.conditionalBalance);
      }

      xx.conditionalDecrease = false;
    }

    if (xx.conditionalDecrease) {
      xx.depthDecrease = true;
    }

    if (xx.depthDecrease) {
      GBSDeleteHeight.singleInstance().balance(stack, ntop);
    }
  }

  /**
   * Return the last node in a linear list of nodes linked together
   * by their right child pointer.
   */
  private GBSNode lastInList(
    GBSNode       p)
  {
    GBSNode q = p;
    p = p.rightChild();
    while (p != null)
    {
      q = p;
      p = p.rightChild();
    }
    return q;
  }

  /**
   * Turn a sub-fringe into a linear list.
   *
   * <p>If you look at the diagrams of insert fringe balancing in
   * GBSInsertFringe you will observe that as a fringe grows on insert
   * it starts out as a linear list and, when it reaches the right
   * length, suddenly becomes a tree.  When a full fringe sub-tree has a
   * node deleted, it reverts once more to being a linear list.</p>
   */
  private GBSNode linearize(
    DeleteStack.Linearizer       xx,
    GBSNode       p)
  {
    xx.headp = null;
    xx.lastp = null;
    innerLinearize(xx, p);
    return xx.headp;
  }

  /**
   * This is the recursive part of linearize.
   */
  private void innerLinearize(
    DeleteStack.Linearizer       xx,
    GBSNode       p)
  {
    xx.depth++;
    if (xx.depth > GBSTree.maxDepth)
      throw new OptimisticDepthException(
        "maxDepth (" + GBSTree.maxDepth +
                 ") exceeded in GBSDeleteFringe.innerLinearize().");

    if (p.leftChild() != null) {
      innerLinearize(xx, p.leftChild());
    }
    if (xx.lastp == null) {
      xx.headp = p;
    }
    else {
      xx.lastp.setChildren(null, p);
    }
    xx.lastp = p;

    if (p.rightChild() != null) {
      innerLinearize(xx, p.rightChild());
    }
    xx.depth--;
  }

  /**
   * Append a linear list (formerly a fringe) to a sub-tree and fill the
   * last node in the sub-tree from the linear list if the last node
   * in the sub-tree is not already full.
   *
   * <p>A sub-tree is to have a former sub-tree (now a linear list)
   * appended to it.  Both the sub-tree and the list will probably have
   * terminal nodes that are partially full.  If the sub-tree has a
   * terminal node that is not full, then all keys in the list must be
   * moved left in order to fill up the sub-tree.</p>
   *
   * @param p Tail of sub-tree.
   * @param f Head of list to add.
   */
  private void combine(
    GBSNode       p,
    GBSNode       f)
  {
                                   /* Size of gap in tail of sub-tree       */
    int gapWid = p.width() - p.population();
    GBSNode q = p;                 /* Last node visited                     */
    p.setRightChild(f);            /* Make sub-tree point to list           */
    p = f;                         /* Next node becomes current             */
    if (gapWid != 0)               /* Keys must be adjusted                 */
      {
        GBSNode lq = null;         /* Parent of last node visited           */
        while(p != null)           /* While q has a right child             */
        {
          q.fillFromRight();
          lq = q;
          q = p;
          p = p.rightChild();
        }
        if (q.population() != 0) {  /* Final node is non-empty              */
          q.adjustMedian();         /* Adjust its middle key                */
        }
        else {                      /* Final node is now empty              */
          lq.clearRightChild();     /* And can be got rid of                */
        }
      }
  }

}
