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
 * This defines the node from which a delete will be done.
 *
 * <p>There are two positions defined here.  One is called the target
 * point and the other is called the delete point - each represented
 * by a node and an index within the node.  The delete point always
 * exists and it identifies the key in a leaf node that is to be
 * physically removed from the index.  The target point only exists if
 * the key to be deleted from the index is not contained within a
 * leaf.  If it is not contained within a leaf then it is called the
 * target point and a suitable key (the delete point) is migrated up
 * from a leaf to overlay the key in the target point.  This is how
 * physical deletions always take place from a leaf.</p>
 *
 * @author Stewart L. Palmer
 */

class DeleteNode
{

  /**
   * The following define the actions to be taken with respect to the
   * target node of the delete operation.
   */
  static final int NONE           = 0;
  static final int ADD_LEFT       = 1;
  static final int ADD_RIGHT      = 2;
  static final int OVERLAY_RIGHT  = 3;
  static final int OVERLAY_LEFT   = 4;

  DeleteNode()
  {
    reset();
  }

  public String toString()
  {
    String x;
    x = "deleteNode = "    + deleteNode()    + "\n" +
        "deleteIndex = "   + deleteIndex()   + "\n" +
        "targetNode = "    + targetNode()    + "\n" +
        "targetIndex = "   + targetIndex();
    return x;
  }

  /**
   * Return the node from which the delete will be done.
   */
  GBSNode deleteNode()
  { return _deleteNode; }

  /**
   * Return the index to the item to be deleted within the node.
   */
  int deleteIndex()
  { return _deleteIndex; }

  /**
   * Return the node into which a key will be moved in order to force
   * the physical deletion to take place in a leaf node.
   */
  GBSNode targetNode()
  { return _targetNode; }

  /**
   * Return the index to the item to be overlayed within the node.
   */
  int targetIndex()
  { return _targetIndex; }

  /**
   * Return the type indicator which defines the manner in which the
   * overlay of the target is to take place (if any).
   */
  int targetType()
  { return _type; }

  /**
   * Return true if the item to be deleted was found and this object
   * contains the information about the object to be deleted.
   */
  boolean wasFound()
  { return (_notFound == false); }

  /**
   * Remember the information about the delete point.
   *
   * @param deleteNode The node in which the object resides.
   * @param deleteIndex The index within the node to the entry to
   *                    be deleted.
   */
  void setDelete(
    GBSNode      deleteNode,
    int          deleteIndex)
  {
    _deleteNode  = deleteNode;
    _deleteIndex = deleteIndex;
    _notFound = false;
  }

  /**
   * Remember the information about the target point.
   * 
   * @param targetNode The node in which the target object resides.
   * @param targetIndex
   *                   The index within the node to the entry to
   *                   be overlayed by the key in the delete point.
   * @param type       Defines the manner in which the overlay is to be done.
   */
  void setTarget(
    GBSNode      targetNode,
    int          targetIndex,
    int          type)
  {
    _targetNode  = targetNode;
    _targetIndex = targetIndex;
    _type = type;
  }

  private GBSNode      _deleteNode;
  private int          _deleteIndex;
  private GBSNode      _targetNode;
  private int          _targetIndex;
  private boolean      _notFound;
  private int          _type;

  /**
   * Return this object to its original post-construction state.
   */
  void reset()
  {
    _deleteNode = null;
    _deleteIndex = 0;
    _targetNode = null;
    _targetIndex = 0;
    _type = NONE;
    _notFound = true;
  }

}
