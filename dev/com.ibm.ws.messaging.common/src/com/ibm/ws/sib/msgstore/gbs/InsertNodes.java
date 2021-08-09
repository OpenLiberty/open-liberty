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
 * This defines the nodes into which an insert will be done.
 *
 * <p>There are two positions defined here.  One is called the position
 * point and the other is called the insert point - each represented by
 * a node and an index within the node.  The insert point always exists
 * and it identifies the position in a leaf node into which a key will
 * be inserted.  The position point only exists if the place into which
 * the new key is to be inserted resides in an inner node.  In these
 * cases the point where the new key is to be placed is called the
 * position point.  The key in the position point is moved from the
 * position point to the insert point to make room for the new key at
 * the position point.  This is how physical insertions always take
 * place within a leaf node.</p>
 *
 * @author Stewart L. Palmer
 */

class InsertNodes
{

  public String toString()
  {
    String rvalue = "????";
    if (_directionSet)
      {
        rvalue = "false";
        if (_rightInsert)
          rvalue = "true";
      }

    String x;
    x = "insertNode = "    + insertNode()    + "\n" +
        "insertIndex = "   + insertIndex( )  + ", " +
        "positionNode = "  + positionNode()  + "\n" +
        "positionIndex = " + positionIndex() + "\n" +
        "rightSide = " + rvalue + ".";
    return x;
  }

  GBSNode insertNode()
  { return _insertNode; }

  int insertIndex()
  { return _insertIndex; }

  GBSNode positionNode()
  { return _positionNode; }

  int positionIndex()
  { return _positionIndex; }

  boolean isDuplicate()
  { return _isDuplicate; }

  void setInsert(
    GBSNode      insertNode,
    int          insertIndex)
  {
    _insertNode  = insertNode;
    _insertIndex = insertIndex;
    _positionNode = null;
    _isDuplicate = false;
  }

  void setInsert(
    GBSNode          insertNode,
    NodeInsertPoint  ip)
  {
    _insertNode  = insertNode;
    _isDuplicate = ip.isDuplicate();
    _insertIndex = ip.insertPoint();
    _positionNode = null;
  }

  void setInsertAndPosition(
    GBSNode      insertNode,
    int          insertIndex,
    GBSNode      positionNode,
    int          positionIndex)
  {
    _insertNode  = insertNode;
    _insertIndex = insertIndex;
    _positionNode  = positionNode;
    _positionIndex = positionIndex;
    _isDuplicate = false;
  }

  void setRight()
  {
    _directionSet = true;
    _rightInsert = true;
  }

  void setLeft()
  {
    _directionSet = true;
    _rightInsert = false;
  }

  boolean rightSide()
  {
    if ( !_directionSet )
      throw new RuntimeException("Direction never set before queried.");
    return _rightInsert;
  }

  private GBSNode      _insertNode;
  private int          _insertIndex;
  private GBSNode      _positionNode;
  private int          _positionIndex;
  private boolean      _isDuplicate;
  private boolean      _rightInsert;
  private boolean      _directionSet;

}
