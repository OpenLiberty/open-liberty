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
 * This defines the point at which a key was found.
 *
 *
 * @author Stewart L. Palmer
 */

class SearchNode
{
  SearchNode()
  {
    reset();
  }

  public String toString()
  {
    String x;
    x = "foundNode = "    + foundNode()    + "\n" +
        "foundIndex = "   + foundIndex();
    return x;
  }

  GBSNode foundNode()
  { return _foundNode; }

  int foundIndex()
  { return _foundIndex; }

  boolean wasFound()
  { return (_notFound == false); }

  /**
   * Return the reference to the contained Object if any.
   */
  Object key()
  { return _obj; }

  void setFound(
    GBSNode      foundNode,
    int          foundIndex)
  {
    _foundNode  = foundNode;
    _foundIndex = foundIndex;
    _notFound = false;
  }

  /**
   * Remember the location of a found key.
   *
   * @param obj The object that was found in the index.
   */
   void setLocation(
    Object      obj)
  {
    _obj = obj;
  }

  private Object       _obj;
  private GBSNode      _foundNode;
  private int          _foundIndex;
  private boolean      _notFound;

  void reset()
  {
    _obj = null;
    _foundNode = null;
    _foundIndex = 0;
    _notFound = true;
  }

}
