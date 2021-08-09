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

package com.ibm.ws.sib.mfp.util;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/**
 * A LiteIterator is a lightweight direct implementation of an Iterator over an Object
 * array.  Its remove method should not be called.
 */

public final class LiteIterator implements Iterator {
  private static TraceComponent tc = SibTr.register(LiteIterator.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);

  private int index = 0;
  private int length = 0;
  private Object array;

  public LiteIterator(Object array) {
    this.array = array;
    this.length = Array.getLength(array);
  }

  public boolean hasNext() {
    return index < length;
  }

  public Object next() throws NoSuchElementException {
    try {
      return Array.get(array, index++);
    }
    catch (ArrayIndexOutOfBoundsException e) {
      // No FFDC code needed
      throw new NoSuchElementException(e.getMessage());
    }
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }
}
