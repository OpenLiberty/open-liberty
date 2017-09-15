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
 * A stack of remembered nodes for insert operations.
 *
 * @author Stewart L. Palmer
 */

class InsertStack extends NodeStack
{
  InsertStack(
    GBSTree       tree)
  {
    super(tree);
    _insertPoint = new NodeInsertPoint();
    _insertNodes = new InsertNodes();
  }

  /**
   * Start the stack with the root of the tree.
   */
  void start(
    GBSNode     node,
    String      starter)
  {
    super.stackStart(node, starter);
  }

  /**
   * <p>This method is called once for each node during fringe
   * migration.  If the node is not full we have found a home for the
   * migrating key which will now hold the key.  If the node is full we
   * put the migrating key in its left-most slot and the new migrating
   * key is the right-most one in the node which will migrate to the
   * next node.</p>
   *
   * @param p The node to inspect.
   *
   * @return true if the subtree walk is to stop.
   */
  public boolean processNode(
    GBSNode       p)
  {
    boolean done = false;
    if ( !p.isFull() )             /* Node is not full                      */
      {                            /* We have a home for the migrating key  */
        _migrating = false;
        done = true;
        p.addLeftMostKey(_mkey);
      }
    else                           /* Node is full.  Insert new key at left */
      {                            /*  and migrate the old right-most key   */
        Object migrateKey = p.addLeftMostKey(_mkey);
        _mkey = migrateKey;
      }

    return done;
  }

  void setMigratingKey(
    Object          mkey)
  {
    _mkey = mkey;
    _migrating = true;
    if (_mkey == null)
      _migrating = false;
  }

  Object migratingKey()
  { return _mkey; }

  boolean migrating()
  { return _migrating; }

  void markDuplicate()
  { _isDuplicate = true; }

  boolean isDuplicate()
  { return _isDuplicate; }

  /**
   * @return the NodeInsertPoint associated with this stack.
   */
  NodeInsertPoint nodeInsertPoint()
  { return _insertPoint; }

  /**
   * @return the InsertNodes associated with this stack.
   */
  InsertNodes insertNodes()
  { return _insertNodes; }

  /**
   * Reset to post-construction state.
   */
  public void reset()
  {
    _migrating = false;
    _mkey = null;
    _isDuplicate = false;
  }

  private boolean          _migrating;
  private Object           _mkey;
  private NodeInsertPoint  _insertPoint;
  private InsertNodes      _insertNodes;
  private boolean          _isDuplicate;
}
