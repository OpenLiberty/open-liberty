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
 * Fringe balance (on insert) of a GBS tree.
 *
 * <p>This is a singleton class (only one instance ever exists) with no
 * state of its own.  It rebalances a fringe after a delete operation.
 * It is packaged as a separate class only to keep the size of the main
 * file (GBSTree.java) a manageable size.</p>
 */
class GBSInsertFringe
{

  /**
   * Fetch the singleton instance of this class.
   *
   * @return The singleton instance of the class.
   */
  static GBSInsertFringe singleInstance()
  {
    if (_singleton == null)
      _singleton = new GBSInsertFringe();
    return _singleton;
  }

  /**
   * Reference to the singleton instance of the class.
   */
  private static GBSInsertFringe  _singleton;

  /**
   * Balance a fringe following the addition of its final node.
   *
   * @param kFactor The K factor for the tree.
   * @param stack The stack of nodes through which the insert operation
   *              passed.
   * @param fpoint The fringe balance point (the top of the fringe).
   * @param fpidx The index within the stack of fpoint
   * @param maxBal Maximum allowed fringe imbalance.  This is derived
   *               from the K factor but has to be computed by the
   *               caller anyway so it is passed along here.
   */
  void balance(
    int           kFactor,
    NodeStack     stack,
    GBSNode       fpoint,
    int           fpidx,
    int           maxBal)
  {
    /* Get parent of balance point           */
    GBSNode bparent = stack.node(fpidx - 1);

    switch (kFactor)
    {
      case 2:
        balance2(stack, bparent, fpoint, fpidx, maxBal);
        break;
      case 4:
        balance4(stack, bparent, fpoint, fpidx, maxBal);
        break;
      case 6:
        balance6(stack, bparent, fpoint, fpidx, maxBal);
        break;
      case 8:
        balance8(stack, bparent, fpoint, fpidx, maxBal);
        break;
      case 12:
        balance12(stack, bparent, fpoint, fpidx, maxBal);
        break;
      case 16:
        balance16(stack, bparent, fpoint, fpidx, maxBal);
        break;
      case 24:
        balance24(stack, bparent, fpoint, fpidx, maxBal);
        break;
      case 32:
        balance32(stack, bparent, fpoint, fpidx, maxBal);
        break;
      default:
        String x =
         "Unknown K factor in fringe balance: " + kFactor;
        error(x);
        break;
    }
  }

  /**
   * Balance a fringe with a K factor of two.
   *
   * @param stack The stack of nodes through which the insert operation
   *              passed.
   * @param bparent The parent of the fringe balance point.
   * @param fpoint The fringe balance point (the top of the fringe).
   * @param fpidx The index within the stack of fpoint
   * @param maxBal Maximum allowed fringe imbalance.
   */
  private void balance2(
    NodeStack     stack,
    GBSNode       bparent,         /* Parent of fringe balance point        */
    GBSNode       fpoint,          /* Fringe balance point                  */
    int           fpidx,           /* Index within stack of fpoint          */
    int           maxBal)          /* Maximum allowed fringe imbalance      */
  {
        /*
              k = 2,    2k-1 = 5,    k-1 = 1

                          [A-B-C]                     [3 children]

                          becomes:

                        *----B----*
                        |         |
                        A         C

             -------------------------------

                        *----B----*
                        |         |
                        A         C - D - E           [3 children]


                          becomes:

                        *----B----*
                        |         |
                        A      *--D--*
                               |     |
                               C     E
                                                                      */


    GBSNode a = fpoint;
    GBSNode b = a.rightChild();
    if (bparent.rightChild() == a)
      bparent.setRightChild(b);
    else
      bparent.setLeftChild(b);
    b.setLeftChild(a);
    a.clearRightChild();
    if ((fpidx > 1) && (stack.balancePointIndex() > -1))
      {
        GBSNode qpoint = b;
        stack.setNode(fpidx, qpoint);
        GBSInsertHeight.singleInstance().balance(stack, qpoint);
      }
  }

  /**
   * Balance a fringe with a K factor of four.
   *
   * @param stack The stack of nodes through which the insert operation
   *              passed.
   * @param bparent The parent of the fringe balance point.
   * @param fpoint The fringe balance point (the top of the fringe).
   * @param fpidx The index within the stack of fpoint
   * @param maxBal Maximum allowed fringe imbalance.
   */
  private void balance4(
    NodeStack     stack,
    GBSNode       bparent,         /* Parent of fringe balance point        */
    GBSNode       fpoint,          /* Fringe balance point                  */
    int           fpidx,           /* Index within stack of fpoint          */
    int           maxBal)          /* Maximum allowed fringe imbalance      */
  {
     /*
              k = 4,    2k-1 = 7,    k-1 = 3


                          [A-B-C]                     [3 children]

                          becomes:

                        *----B----*
                        |         |
                        A         C

                -------------------------------

                        *----B----*
                        |         |
                        A         C - D - E - F - G        [5 children]


                          becomes:


                      *------D------*
                      |             |
                   *--B--*       *--F--*
                   |     |       |     |
                   A     C       E     G                              */

    if (maxBal == 3)
      {
        GBSNode a = fpoint;
        GBSNode b = a.rightChild();
        if (bparent.rightChild() == a)
          bparent.setRightChild(b);
        else
          bparent.setLeftChild(b);
        b.setLeftChild(a);
        a.clearRightChild();
      }
    else
      {
        if (maxBal != 5)
          error("fringeBalance4: maxBal != 5, maxBal = " + maxBal);
        GBSNode t0_top = stack.node(fpidx-2);
        GBSNode c = fpoint;
        GBSNode d = c.rightChild();
        GBSNode e = d.rightChild();
        GBSNode f = e.rightChild();
        d.setChildren(bparent, f);
        c.clearRightChild();
        f.setLeftChild(e);
        e.clearRightChild();
        if (t0_top.rightChild() == bparent)
          t0_top.setRightChild(d);
        else
          t0_top.setLeftChild(d);
        if ((fpidx > 2) && (stack.balancePointIndex() > -1))
          {
            GBSNode qpoint = d;
            stack.setNode(fpidx-1, qpoint);
            GBSInsertHeight.singleInstance().balance(stack, qpoint);
          }
      }
  }

  /**
   * Balance a fringe with a K factor of six.
   *
   * @param stack The stack of nodes through which the insert operation
   *              passed.
   * @param bparent The parent of the fringe balance point.
   * @param fpoint The fringe balance point (the top of the fringe).
   * @param fpidx The index within the stack of fpoint
   * @param maxBal Maximum allowed fringe imbalance.
   */
  private void balance6(
    NodeStack     stack,
    GBSNode       bparent,         /* Parent of fringe balance point        */
    GBSNode       fpoint,          /* Fringe balance point                  */
    int           fpidx,           /* Index within stack of fpoint          */
    int           maxBal)          /* Maximum allowed fringe imbalance      */
  {
    /*
               k = 6,  2k-1 = 11,  k-1 = 5

                       [A-B-C-D-E]                   [5 children]

                         becomes:

                         *--C--*
                         |     |
                       *-B     D-*
                       |         |
                       A         E

               -------------------------------

                         *--C--*
                         |     |                     [8 children]
                       *-B     D-*
                       |         |
                       A         E - F - G - H - I - J - K


                         becomes:


                    *-------F-------*
                    |               |
                 *--C--*         *--I--*
                 |     |         |     |
               *-B     D-*     *-H     J-*
               |         |     |         |
               A         E     G         K                            */

    if (maxBal == 5)
      {
        GBSNode a = fpoint;
        GBSNode b = a.rightChild();
        GBSNode c = b.rightChild();
        if (bparent.rightChild() == a)
          bparent.setRightChild(c);
        else
          bparent.setLeftChild(c);
        c.setLeftChild(b);
        b.setChildren(a, null);
        a.clearRightChild();
      }
    else
      {
        if (maxBal != 8)
          error("fringeBalance6: maxBal != 8, maxBal = " + maxBal);
        GBSNode t0_top = stack.node(fpidx-2);
        GBSNode c = bparent;
        GBSNode d = fpoint;
        GBSNode e = d.rightChild();
        GBSNode f = e.rightChild();
        GBSNode g = f.rightChild();
        GBSNode h = g.rightChild();
        GBSNode i = h.rightChild();
        f.setChildren(c, i);
        i.setLeftChild(h);
        h.setChildren(g, null);
        g.clearRightChild();
        e.clearRightChild();
        if (t0_top.rightChild() == c)
          t0_top.setRightChild(f);
        else
          t0_top.setLeftChild(f);
        if ((fpidx > 2) && (stack.balancePointIndex() > -1))
          {
            GBSNode qpoint = f;
            stack.setNode(fpidx-1, qpoint);
            GBSInsertHeight.singleInstance().balance(stack, qpoint);
          }
      }
  }

  /**
   * Balance a fringe with a K factor of eight.
   *
   * @param stack The stack of nodes through which the insert operation
   *              passed.
   * @param bparent The parent of the fringe balance point.
   * @param fpoint The fringe balance point (the top of the fringe).
   * @param fpidx The index within the stack of fpoint
   * @param maxBal Maximum allowed fringe imbalance.
   */
  private void balance8(
    NodeStack     stack,
    GBSNode       bparent,         /* Parent of fringe balance point        */
    GBSNode       fpoint,          /* Fringe balance point                  */
    int           fpidx,           /* Index within stack of fpoint          */
    int           maxBal)          /* Maximum allowed fringe imbalance      */
  {
    /*
          k = 8,  2k-1 = 15,  k-1 = 7

                [A-B-C-D-E-F-G]                [7 children]

                    becomes:


                *------D------*
                |             |
             *--B--*       *--F--*
             |     |       |     |
             A     C       E     G

     -------------------------------------------

                *------D------*
                |             |
             *--B--*       *--F--*             [9 children]
             |     |       |     |
             A     C       E     G - H - I - J - K - L - M - N - O


                    becomes:


               *--------H--------*
               |                 |
           *---D---*         *---L---*
           |       |         |       |
         *-B-*   *-F-*     *-J-*   *-N-*
         |   |   |   |     |   |   |   |
         A   C   E   G     I   K   M   O                              */

    if (maxBal == 7)
      {
        GBSNode a = fpoint;
        GBSNode b = a.rightChild();
        GBSNode c = b.rightChild();
        GBSNode d = c.rightChild();
        GBSNode e = d.rightChild();
        GBSNode f = e.rightChild();
        d.setLeftChild(b);
        d.setRightChild(f);
        b.setLeftChild(a);
        a.clearRightChild();
        c.clearRightChild();
        f.setLeftChild(e);
        e.clearRightChild();
        if (bparent.rightChild() == a)
          bparent.setRightChild(d);
        else
          bparent.setLeftChild(d);
      }
    else
      {
        if (maxBal != 9)
          error("fringeBalance8: maxBal != 9, maxBal = " + maxBal);
        GBSNode t0_top = stack.node(fpidx-3);
        GBSNode d = stack.node(fpidx-2);
        GBSNode g = fpoint;
        GBSNode h = g.rightChild();
        GBSNode i = h.rightChild();
        GBSNode j = i.rightChild();
        GBSNode k = j.rightChild();
        GBSNode l = k.rightChild();
        GBSNode m = l.rightChild();
        GBSNode n = m.rightChild();
        h.setLeftChild(d);
        h.setRightChild(l);
        l.setLeftChild(j);
        l.setRightChild(n);
        j.setLeftChild(i);
        i.clearRightChild();
        k.clearRightChild();
        n.setLeftChild(m);
        m.clearRightChild();
        g.clearRightChild();
        if (t0_top.rightChild() == d)
          t0_top.setRightChild(h);
        else
          t0_top.setLeftChild(h);
        if ((fpidx > 3) && (stack.balancePointIndex() > -1))
          {
            GBSNode qpoint = h;
            stack.setNode(fpidx-2, qpoint);
            GBSInsertHeight.singleInstance().balance(stack, qpoint);
          }
      }

  }

  /**
   * Balance a fringe with a K factor of twelve.
   *
   * @param stack The stack of nodes through which the insert operation
   *              passed.
   * @param bparent The parent of the fringe balance point.
   * @param fpoint The fringe balance point (the top of the fringe).
   * @param fpidx The index within the stack of fpoint
   * @param maxBal Maximum allowed fringe imbalance.
   */
  private void balance12(
    NodeStack     stack,
    GBSNode       bparent,         /* Parent of fringe balance point        */
    GBSNode       fpoint,          /* Fringe balance point                  */
    int           fpidx,           /* Index within stack of fpoint          */
    int           maxBal)          /* Maximum allowed fringe imbalance      */
  {
    /*
        k = 12,     2k-1 = 23,     k-1 = 11

                [A-B-C-D-E-F-G-H-I-J-K]              [11 children]

                       becomes:

                *-------F-------*
                |               |
             *--C--*         *--I--*
             |     |         |     |
           *-B     D-*     *-H     J-*
           |         |     |         |
           A         E     G         K

  --------------------------------------------------------

                *-------F-------*
                |               |
             *--C--*         *--I--*
             |     |         |     |
           *-B     D-*     *-H     J-*          [14 children]
           |         |     |         |
           A         E     G         K - L-M-N-O-P-Q-R-S-T-U-V-W


                       becomes:


              *-----------L-----------*
              |                       |
        *-----F-----*           *-----R-----*
        |           |           |           |
      *-C-*       *-I-*       *-O-*       *-U-*
      |   |       |   |       |   |       |   |
    *-B   D-*   *-H   J-*   *-N   P*-   *-T   V-*
    |       |   |       |   |       |   |       |
    A       E   G       K   M       Q   S       W                     */

    if (maxBal == 11)
      {
        GBSNode a = fpoint;
        GBSNode b = a.rightChild();
        GBSNode c = b.rightChild();
        GBSNode d = c.rightChild();
        GBSNode e = d.rightChild();
        GBSNode f = e.rightChild();
        GBSNode g = f.rightChild();
        GBSNode h = g.rightChild();
        GBSNode i = h.rightChild();
        f.setLeftChild(c);
        f.setRightChild(i);
        c.setLeftChild(b);
        b.setLeftChild(a);
        b.clearRightChild();
        a.clearRightChild();
        e.clearRightChild();
        i.setLeftChild(h);
        h.setLeftChild(g);
        h.clearRightChild();
        g.clearRightChild();
        if (bparent.rightChild() == a)
          bparent.setRightChild(f);
        else
          bparent.setLeftChild(f);
      }
    else
      {
        if (maxBal != 14)
          error("fringeBalance12: maxBal != 14, maxBal = " + maxBal);
        GBSNode t0_top = stack.node(fpidx-3);
        GBSNode f = stack.node(fpidx-2);
        GBSNode j = fpoint;
        GBSNode k = j.rightChild();
        GBSNode l = k.rightChild();
        GBSNode m = l.rightChild();
        GBSNode n = m.rightChild();
        GBSNode o = n.rightChild();
        GBSNode p = o.rightChild();
        GBSNode q = p.rightChild();
        GBSNode r = q.rightChild();
        GBSNode s = r.rightChild();
        GBSNode t = s.rightChild();
        GBSNode u = t.rightChild();
        if (t0_top.rightChild() == f)
          t0_top.setRightChild(l);
        else
          t0_top.setLeftChild(l);
        l.setLeftChild(f);
        l.setRightChild(r);
        r.setLeftChild(o);
        r.setRightChild(u);
        o.setLeftChild(n);
        n.setLeftChild(m);
        n.clearRightChild();
        m.clearRightChild();
        q.clearRightChild();
        u.setLeftChild(t);
        t.setLeftChild(s);
        t.clearRightChild();
        s.clearRightChild();
        k.clearRightChild();
        if ((fpidx > 3) && (stack.balancePointIndex() > -1))
          {
            GBSNode qpoint = l;
            stack.setNode(fpidx-2, qpoint);
            GBSInsertHeight.singleInstance().balance(stack, qpoint);
          }
      }
  }

  /**
   * Balance a fringe with a K factor of sixteen.
   *
   * @param stack The stack of nodes through which the insert operation
   *              passed.
   * @param bparent The parent of the fringe balance point.
   * @param fpoint The fringe balance point (the top of the fringe).
   * @param fpidx The index within the stack of fpoint
   * @param maxBal Maximum allowed fringe imbalance.
   */
  private void balance16(
    NodeStack     stack,
    GBSNode       bparent,         /* Parent of fringe balance point        */
    GBSNode       fpoint,          /* Fringe balance point                  */
    int           fpidx,           /* Index within stack of fpoint          */
    int           maxBal)          /* Maximum allowed fringe imbalance      */
  {
    /*
            k = 16,    2k-1 = 31,    k-1 = 15

     [A-B-C-D-E-F-G-H-I-J-K-L-M-N-O]             [15 children]

                becomes:


           *--------H--------*
           |                 |
       *---D---*         *---L---*
       |       |         |       |
     *-B-*   *-F-*     *-J-*   *-N-*
     |   |   |   |     |   |   |   |
     A   C   E   G     I   K   M   O

  -----------------------------------------------

           *--------H--------*
           |                 |
       *---D---*         *---L---*
       |       |         |       |
     *-B-*   *-F-*     *-J-*   *-N-*           [17 children]
     |   |   |   |     |   |   |   |
     A   C   E   G     I   K   M   O - P-Q-R-S-T-U-V-W-X-Y-Z-0-1-2-3-4


                  becomes:

                 *-----------------P-----------------*
                 |                                   |
        *--------H--------*                 *--------X--------*
        |                 |                 |                 |
    *---D---*         *---L---*         *---T---*         *---1---*
    |       |         |       |         |       |         |       |
  *-B-*   *-F-*     *-J-*   *-N-*     *-R-*   *-V-*     *-Z-*   *-3-*
  |   |   |   |     |   |   |   |     |   |   |   |     |   |   |   |
  A   C   E   G     I   K   M   O     Q   S   U   W     Y   0   2   4


   ----------------------------------------------

           *--------H--------*
           |                 |
       *---D---*         *---L---*
       |       |         |       |
     *-B-*   *-F-*     *-J-*   *-N-*           [17 children]
     |   |   |   |     |   |   |   |
     A   C   E   G     I   K   M   O - A-B-C-D-E-F-G-H-I-J-K-L-M-N-O-P


                  becomes:

                 *-----------------A-----------------*
                 |                                   |
        *--------H--------*                 *--------I--------*
        |                 |                 |                 |
    *---D---*         *---L---*         *---E---*         *---M---*
    |       |         |       |         |       |         |       |
  *-B-*   *-F-*     *-J-*   *-N-*     *-C-*   *-G-*     *-K-*   *-O-*
  |   |   |   |     |   |   |   |     |   |   |   |     |   |   |   |
  A   C   E   G     I   K   M   O     B   D   F   H     J   L   N   P

                                                                      */
    if (maxBal == 15)
      {
        GBSNode a = fpoint;
        GBSNode b = a.rightChild();
        GBSNode c = b.rightChild();
        GBSNode d = c.rightChild();
        GBSNode e = d.rightChild();
        GBSNode f = e.rightChild();
        GBSNode g = f.rightChild();
        GBSNode h = g.rightChild();
        GBSNode i = h.rightChild();
        GBSNode j = i.rightChild();
        GBSNode k = j.rightChild();
        GBSNode l = k.rightChild();
        GBSNode m = l.rightChild();
        GBSNode n = m.rightChild();
        h.setLeftChild(d);
        h.setRightChild(l);
        d.setLeftChild(b);
        d.setRightChild(f);
        b.setLeftChild(a);
        a.clearRightChild();
        c.clearRightChild();
        f.setLeftChild(e);
        e.clearRightChild();
        g.clearRightChild();
        l.setLeftChild(j);
        l.setRightChild(n);
        j.setLeftChild(i);
        i.clearRightChild();
        k.clearRightChild();
        n.setLeftChild(m);
        m.clearRightChild();
        if (bparent.rightChild() == a)
          bparent.setRightChild(h);
        else
          bparent.setLeftChild(h);
      }
    else
      {
        if (maxBal != 17)
          error("fringeBalance16: maxBal != 17, maxBal = " + maxBal);
        GBSNode t0_top = stack.node(fpidx-4);
        GBSNode h1 = stack.node(fpidx-3);
        GBSNode a = fpoint.rightChild();
        GBSNode b = a.rightChild();
        GBSNode c = b.rightChild();
        GBSNode d = c.rightChild();
        GBSNode e = d.rightChild();
        GBSNode f = e.rightChild();
        GBSNode g = f.rightChild();
        GBSNode h = g.rightChild();
        GBSNode i = h.rightChild();
        GBSNode j = i.rightChild();
        GBSNode k = j.rightChild();
        GBSNode l = k.rightChild();
        GBSNode m = l.rightChild();
        GBSNode n = m.rightChild();
        GBSNode o = n.rightChild();
        if (t0_top.rightChild() == h1)
          t0_top.setRightChild(a);
        else
          t0_top.setLeftChild(a);
        a.setLeftChild(h1);
        a.setRightChild(i);
        i.setLeftChild(e);
        i.setRightChild(m);
        e.setLeftChild(c);
        e.setRightChild(g);
        c.setLeftChild(b);
        b.clearRightChild();
        d.clearRightChild();
        g.setLeftChild(f);
        f.clearRightChild();
        h.clearRightChild();
        m.setLeftChild(k);
        m.setRightChild(o);
        k.setLeftChild(j);
        j.clearRightChild();
        l.clearRightChild();
        o.setLeftChild(n);
        n.clearRightChild();
        fpoint.clearRightChild();
        if ((fpidx > 4) && (stack.balancePointIndex() > -1))
          {
            GBSNode qpoint = a;
            stack.setNode(fpidx-3, qpoint);
            GBSInsertHeight.singleInstance().balance(stack, qpoint);
          }
      }
  }

  /**
   * Balance a fringe with a K factor of twenty-four.
   *
   * @param stack The stack of nodes through which the insert operation
   *              passed.
   * @param bparent The parent of the fringe balance point.
   * @param fpoint The fringe balance point (the top of the fringe).
   * @param fpidx The index within the stack of fpoint
   * @param maxBal Maximum allowed fringe imbalance.
   */
  private void balance24(
    NodeStack     stack,
    GBSNode       bparent,         /* Parent of fringe balance point        */
    GBSNode       fpoint,          /* Fringe balance point                  */
    int           fpidx,           /* Index within stack of fpoint          */
    int           maxBal)          /* Maximum allowed fringe imbalance      */
  {
    /*

        k = 24,     2k-1 = 47,     k-1 = 23

    [A-B-C-D-E-F-G-H-I-J-K-L-M-N-O-P-Q-R-S-T-U-V-W]      [23 children]

                       becomes:


              *-----------L-----------*
              |                       |
        *-----F-----*           *-----R-----*
        |           |           |           |
      *-C-*       *-I-*       *-O-*       *-U-*
      |   |       |   |       |   |       |   |
    *-B   D-*   *-H   J-*   *-N   P*-   *-T   V-*
    |       |   |       |   |       |   |       |
    A       E   G       K   M       Q   S       W

 ----------------------------------------------------


              *-----------L-----------*
              |                       |
        *-----F-----*           *-----R-----*
        |           |           |           |
      *-C-*       *-I-*       *-O-*       *-U-*
      |   |       |   |       |   |       |   |
    *-B   D-*   *-H   J-*   *-N   P*-   *-T   V-*                  [26 children]
    |       |   |       |   |       |   |       |
    A       E   G       K   M       Q   S       W - A-B-C-D-E-F-G-H-I-J-K-L-M-N-O-P-Q-R-S-T-U-V-W-X


                       becomes:



                          *------------------------A------------------------*
                          |                                                 |
              *-----------L-----------*                         *-----------M-----------*
              |                       |                         |                       |
        *-----F-----*           *-----R-----*             *-----G-----*           *-----S-----*
        |           |           |           |             |           |           |           |
      *-C-*       *-I-*       *-O-*       *-U-*         *-D-*       *-J-*       *-P-*       *-V-*
      |   |       |   |       |   |       |   |         |   |       |   |       |   |       |   |
    *-B   D-*   *-H   J-*   *-N   P*-   *-T   V-*     *-C   E-*   *-I   K-*   *-O   Q*-   *-U   W-*
    |       |   |       |   |       |   |       |     |       |   |       |   |       |   |       |
    A       E   G       K   M       Q   S       W     B       F   H       L   N       R   T       X

                                                                      */
    if (maxBal == 23)
      {
        GBSNode b = fpoint.rightChild();
        GBSNode c = b.rightChild();
        GBSNode d = c.rightChild();
        GBSNode e = d.rightChild();
        GBSNode f = e.rightChild();
        GBSNode g = f.rightChild();
        GBSNode h = g.rightChild();
        GBSNode i = h.rightChild();
        GBSNode j = i.rightChild();
        GBSNode k = j.rightChild();
        GBSNode l = k.rightChild();
        GBSNode m = l.rightChild();
        GBSNode n = m.rightChild();
        GBSNode o = n.rightChild();
        GBSNode p = o.rightChild();
        GBSNode q = p.rightChild();
        GBSNode r = q.rightChild();
        GBSNode s = r.rightChild();
        GBSNode t = s.rightChild();
        GBSNode u = t.rightChild();
        GBSNode v = u.rightChild();
        l.setLeftChild(f);
        l.setRightChild(r);
        f.setLeftChild(c);
        f.setRightChild(i);
        c.setLeftChild(b);
        b.setLeftChild(fpoint);
        b.clearRightChild();
        fpoint.clearRightChild();
        e.clearRightChild();
        i.setLeftChild(h);
        h.setLeftChild(g);
        h.clearRightChild();
        g.clearRightChild();
        k.clearRightChild();
        r.setLeftChild(o);
        r.setRightChild(u);
        o.setLeftChild(n);
        o.setRightChild(p);
        n.setLeftChild(m);
        n.clearRightChild();
        m.clearRightChild();
        q.clearRightChild();
        u.setLeftChild(t);
        u.setRightChild(v);
        t.setLeftChild(s);
        t.clearRightChild();
        s.clearRightChild();
        if (bparent.rightChild() == fpoint)
          bparent.setRightChild(l);
        else
          bparent.setLeftChild(l);
      }
    else
      {
        if (maxBal != 26)
          error("fringeBalance24: maxBal != 26, maxBal = " + maxBal);
        GBSNode t0_top = stack.node(fpidx-4);
        GBSNode l1 = stack.node(fpidx-3);
        GBSNode w1 = fpoint.rightChild();
        GBSNode a = w1.rightChild();
        GBSNode b = a.rightChild();
        GBSNode c = b.rightChild();
        GBSNode d = c.rightChild();
        GBSNode e = d.rightChild();
        GBSNode f = e.rightChild();
        GBSNode g = f.rightChild();
        GBSNode h = g.rightChild();
        GBSNode i = h.rightChild();
        GBSNode j = i.rightChild();
        GBSNode k = j.rightChild();
        GBSNode l = k.rightChild();
        GBSNode m = l.rightChild();
        GBSNode n = m.rightChild();
        GBSNode o = n.rightChild();
        GBSNode p = o.rightChild();
        GBSNode q = p.rightChild();
        GBSNode r = q.rightChild();
        GBSNode s = r.rightChild();
        GBSNode t = s.rightChild();
        GBSNode u = t.rightChild();
        GBSNode v = u.rightChild();
        if (t0_top.rightChild() == l1)
          t0_top.setRightChild(a);
        else
          t0_top.setLeftChild(a);
        a.setLeftChild(l1);
        a.setRightChild(m);
        m.setLeftChild(g);
        m.setRightChild(s);
        g.setLeftChild(d);
        g.setRightChild(j);
        d.setLeftChild(c);
        c.setLeftChild(b);
        c.clearRightChild();
        b.clearRightChild();
        f.clearRightChild();
        j.setLeftChild(i);
        j.setRightChild(k);
        i.setLeftChild(h);
        i.clearRightChild();
        h.clearRightChild();
        l.clearRightChild();
        s.setLeftChild(p);
        s.setRightChild(v);
        p.setLeftChild(o);
        o.setLeftChild(n);
        o.clearRightChild();
        n.clearRightChild();
        r.clearRightChild();
        v.setLeftChild(u);
        u.setLeftChild(t);
        u.clearRightChild();
        t.clearRightChild();
        w1.clearRightChild();
        if ((fpidx > 4) && (stack.balancePointIndex() > -1))
          {
            GBSNode qpoint = a;
            stack.setNode(fpidx-3, qpoint);
            GBSInsertHeight.singleInstance().balance(stack, qpoint);
          }
      }
  }

  /**
   * Balance a fringe with a K factor of thirty-two.
   *
   * @param stack The stack of nodes through which the insert operation
   *              passed.
   * @param bparent The parent of the fringe balance point.
   * @param fpoint The fringe balance point (the top of the fringe).
   * @param fpidx The index within the stack of fpoint
   * @param maxBal Maximum allowed fringe imbalance.
   */
  private void balance32(
    NodeStack     stack,
    GBSNode       bparent,         /* Parent of fringe balance point        */
    GBSNode       fpoint,          /* Fringe balance point                  */
    int           fpidx,           /* Index within stack of fpoint          */
    int           maxBal)          /* Maximum allowed fringe imbalance      */
  {
     /*
        k = 32,     2k-1 = 63,     k-1 = 31

 [A-B-C-D-E-F-G-H-I-J-K-L-M-N-O-P-Q-R-S-T-U-V-W-X-Y-Z-0-1-2-3-4]    [31 children]

                       becomes:


                    *-----------------P-----------------*
                    |                                   |
           *--------H--------*                 *--------X--------*
           |                 |                 |                 |
       *---D---*         *---L---*         *---T---*         *---1---*
       |       |         |       |         |       |         |       |
     *-B-*   *-F-*     *-J-*   *-N-*     *-R-*   *-V-*     *-Z-*   *-3-*
     |   |   |   |     |   |   |   |     |   |   |   |     |   |   |   |
     A   C   E   G     I   K   M   O     Q   S   U   W     Y   0   2   4


   ---------------------------------------------------------------------



                    *-----------------P-----------------*
                    |                                   |
           *--------H--------*                 *--------X--------*
           |                 |                 |                 |
       *---D---*         *---L---*         *---T---*         *---1---*
       |       |         |       |         |       |         |       |
     *-B-*   *-F-*     *-J-*   *-N-*     *-R-*   *-V-*     *-Z-*   *-3-*                   [33 children]
     |   |   |   |     |   |   |   |     |   |   |   |     |   |   |   |
     A   C   E   G     I   K   M   O     Q   S   U   W     Y   0   2   4 - A-B-C-D-E-F-G-H-I-J-K-L-M-N-O-
                                                                           P-Q-R-S-T-U-V-W-X-Y-Z-0-1-2-3-4-5


                       becomes:


                              *-------------------------------A-------------------------------*
                              |                                                               |
              *---------------P---------------*                               *---------------Q---------------*
              |                               |                               |                               |
      *-------H-------*               *-------X-------*               *-------I-------*               *-------Y-------*
      |               |               |               |               |               |               |               |
  *---D---*       *---L---*       *---T---*       *---1---*       *---E---*       *---M---*       *---U---*       *---2---*
  |       |       |       |       |       |       |       |       |       |       |       |       |       |       |       |
*-B-*   *-F-*   *-J-*   *-N-*   *-R-*   *-V-*   *-Z-*   *-3-*   *-C-*   *-G-*   *-K-*   *-O-*   *-S-*   *-W-*   *-0-*   *-4-*
|   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
A   C   E   G   I   K   M   O   Q   S   U   W   Y   0   2   4   B   D   F   H   J   L   N   P   R   T   V   X   Z   1   3   5





---------------A-------------------------------*
                                               |
                               *---------------Q---------------*
                               |                               |
-------*               *-------I-------*               *-------Y-------*
       |               |               |               |               |
   *---1---*       *---E---*       *---M---*       *---U---*       *---2---*
   |       |       |       |       |       |       |       |       |       |
 *-Z-*   *-3-*   *-C-*   *-G-*   *-K-*   *-O-*   *-S-*   *-W-*   *-0-*   *-4-*
 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
 Y   0   2   4   B   D   F   H   J   L   N   P   R   T   V   X   Z   1   3   5





               *---------------A---------------*
               |                               |
       *-------P-------*               *-------Q-------*
       |               |               |               |
   *---H---*       *---X---*       *---I---*       *---Y---*
   |       |       |       |       |       |       |       |
 *-D-*   *-L-*   *-T-*   *-1-*   *-E-*   *-M-*   *-U-*   *-2-*
 |   |   |   |   |   |   |   |   |   |   |   |   |   |   |   |
*B* *F* *J* *N* *R* *V* *Z* *3* *C* *G* *K* *O* *S* *W* *0* *4*
| | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | |
A C E G I K M O Q S U W Y 0 2 4 B D F H J L N P R T V X Z 1 3 5

                                                                              */

    if (maxBal == 31)
      {
        GBSNode b = fpoint.rightChild();
        GBSNode c = b.rightChild();
        GBSNode d = c.rightChild();
        GBSNode e = d.rightChild();
        GBSNode f = e.rightChild();
        GBSNode g = f.rightChild();
        GBSNode h = g.rightChild();
        GBSNode i = h.rightChild();
        GBSNode j = i.rightChild();
        GBSNode k = j.rightChild();
        GBSNode l = k.rightChild();
        GBSNode m = l.rightChild();
        GBSNode n = m.rightChild();
        GBSNode o = n.rightChild();
        GBSNode p = o.rightChild();
        GBSNode q = p.rightChild();
        GBSNode r = q.rightChild();
        GBSNode s = r.rightChild();
        GBSNode t = s.rightChild();
        GBSNode u = t.rightChild();
        GBSNode v = u.rightChild();
        GBSNode w = v.rightChild();
        GBSNode x = w.rightChild();
        GBSNode y = x.rightChild();
        GBSNode z = y.rightChild();
        GBSNode p0 = z.rightChild();
        GBSNode p1 = p0.rightChild();
        GBSNode p2 = p1.rightChild();
        GBSNode p3 = p2.rightChild();
        GBSNode p4 = p3.rightChild();
        p.setLeftChild(h);
        p.setRightChild(x);
        h.setLeftChild(d);
        h.setRightChild(l);
        d.setLeftChild(b);
        d.setRightChild(f);
        b.setLeftChild(fpoint);
        b.setRightChild(c);
        fpoint.clearRightChild();
        c.clearRightChild();
        f.setLeftChild(e);
        f.setRightChild(g);
        e.clearRightChild();
        g.clearRightChild();
        l.setLeftChild(j);
        l.setRightChild(n);
        j.setLeftChild(i);
        j.setRightChild(k);
        i.clearRightChild();
        k.clearRightChild();
        n.setLeftChild(m);
        n.setRightChild(o);
        m.clearRightChild();
        o.clearRightChild();
        x.setLeftChild(t);
        x.setRightChild(p1);
        t.setLeftChild(r);
        t.setRightChild(v);
        r.setLeftChild(q);
        r.setRightChild(s);
        q.clearRightChild();
        s.clearRightChild();
        v.setLeftChild(u);
        v.setRightChild(w);
        u.clearRightChild();
        w.clearRightChild();
        p1.setLeftChild(z);
        p1.setRightChild(p3);
        z.setLeftChild(y);
        z.setRightChild(p0);
        y.clearRightChild();
        p0.clearRightChild();
        p3.setLeftChild(p2);
        p3.setRightChild(p4);
        p2.clearRightChild();
        if (bparent.rightChild() == fpoint)
          bparent.setRightChild(p);
        else
          bparent.setLeftChild(p);
      }
    else
      {
        if (maxBal != 33)
          error("fringeBalance32: maxBal != 33, maxBal = " + maxBal);
        GBSNode t0_top = stack.node(fpidx-5);
        GBSNode xp1 = stack.node(fpidx-4);
        GBSNode a = fpoint.rightChild();
        GBSNode b = a.rightChild();
        GBSNode c = b.rightChild();
        GBSNode d = c.rightChild();
        GBSNode e = d.rightChild();
        GBSNode f = e.rightChild();
        GBSNode g = f.rightChild();
        GBSNode h = g.rightChild();
        GBSNode i = h.rightChild();
        GBSNode j = i.rightChild();
        GBSNode k = j.rightChild();
        GBSNode l = k.rightChild();
        GBSNode m = l.rightChild();
        GBSNode n = m.rightChild();
        GBSNode o = n.rightChild();
        GBSNode p = o.rightChild();
        GBSNode q = p.rightChild();
        GBSNode r = q.rightChild();
        GBSNode s = r.rightChild();
        GBSNode t = s.rightChild();
        GBSNode u = t.rightChild();
        GBSNode v = u.rightChild();
        GBSNode w = v.rightChild();
        GBSNode x = w.rightChild();
        GBSNode y = x.rightChild();
        GBSNode z = y.rightChild();
        GBSNode p0 = z.rightChild();
        GBSNode p1 = p0.rightChild();
        GBSNode p2 = p1.rightChild();
        GBSNode p3 = p2.rightChild();
        GBSNode p4 = p3.rightChild();
        GBSNode p5 = p4.rightChild();
        a.setChildren(xp1, q);

        q.setChildren(i, y);

        i.setChildren(e, m);

        e.setChildren(c, g);

        c.setChildren(b, d);

        b.clearRightChild();
        d.clearRightChild();
        g.setChildren(f, h);

        f.clearRightChild();
        h.clearRightChild();
        m.setChildren(k, o);

        k.setChildren(j, l);

        j.clearRightChild();
        l.clearRightChild();
        o.setChildren(n, p);

        n.clearRightChild();
        p.clearRightChild();
        y.setChildren(u, p2);

        u.setChildren(s, w);

        s.setChildren(r, t);

        r.clearRightChild();
        t.clearRightChild();
        w.setChildren(v, x);

        v.clearRightChild();
        x.clearRightChild();
        p2.setChildren(p0, p4);

        p0.setChildren(z, p1);

        z.clearRightChild();
        p1.clearRightChild();
        p4.setChildren(p3, p5);

        p3.clearRightChild();
        fpoint.clearRightChild();
        if (t0_top.rightChild() == xp1)
          t0_top.setRightChild(a);
        else
          t0_top.setLeftChild(a);
        if ((fpidx > 5) && (stack.balancePointIndex() > -1))
          {
            GBSNode qpoint = a;
            stack.setNode(fpidx-4, qpoint);
            GBSInsertHeight.singleInstance().balance(stack, qpoint);
          }
      }
  }

  /**
   *
   */
  private void error(
    String      reason)
  {
    String x = "Error in Ixgibf: " + reason;
    throw new RuntimeException(x);
  }

}
