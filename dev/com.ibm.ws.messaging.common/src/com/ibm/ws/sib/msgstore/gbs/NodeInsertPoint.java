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
 * This is the point within a node AFTER which
 * a new key is to be inserted.
 *
 * @author Stewart L. Palmer
 */

public class NodeInsertPoint
{
  boolean isDuplicate()
  { return _isDuplicate; }

  int insertPoint()
  { return _insertPoint; }

  void markDuplicate(
    int         ipt)
  {
    setInsertPoint(ipt);
    _isDuplicate = true;
  }

  void setInsertPoint(
    int         ipt)
  {
    _insertPoint = (short) ipt;
    if (ipt != (int) _insertPoint)
      {
        String x =
        "Insert point won't fit in a short.  Supplied insert point = " + ipt;
        throw new IllegalArgumentException(x);
      }
    _isDuplicate = false;
  }

  public String toString()
  {
    String x;
    if (isDuplicate())
      x = "*duplicate* at " + insertPoint();
    else
      x = insertPoint() + "";
    return x;
  }

  private boolean _isDuplicate;
  private short   _insertPoint = -100;
}
