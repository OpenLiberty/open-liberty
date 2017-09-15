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
package com.ibm.ws.sib.mfp.impl;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.ws.sib.mfp.MfpConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.websphere.ras.TraceComponent;

/*
 * This class provides a wrapper around a JMF list to provide a List that can
 * be freely modified by the caller, but that can be easily converted back
 * to a List again when the data needs to be written back to JMF.
 */

// This class extends AbstractList and implements by delgation to a 'backingList'.
// This backingList is initially the unerlying JMFList which continue to be used as
// long as no changes need to be made that would alter the size of the list.  If
// such a change is attempted we must first change the backingList to an ArrayList
// copy of the original.  The caller is then responsible to writing this new list
// back to JMF when required.

public class JsMsgList extends AbstractList<Object> {
  private static TraceComponent tc = SibTr.register(JsMsgList.class, MfpConstants.MSG_GROUP, MfpConstants.MSG_BUNDLE);
 
  JsMsgList(List<Object> originalList) {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", originalList);
    // We start out with the backing list set to the original JMF list.
    if (originalList != null) {
      backingList = originalList;
      changed = false;
    } else {
      backingList = new ArrayList<Object>();
      changed = true;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
  }

  private List<Object> backingList;
  private boolean changed;

  public Collection getList() {
    return backingList;
  }

  public boolean isChanged() {
    return changed;
  }

  private List<Object> copyList() {
    if (!changed) {
      backingList = new ArrayList<Object>(backingList);
      changed = true;
    }
    return backingList;
  }

  /*
   * Methods needed to implement a modifiable AbstractList.  We only need to
   * make a copy if the list is to be changed in size.
   */

  public int size() {
    return backingList.size();
  }

  public Object get(int index) {
    return backingList.get(index);
  }

  public Object set(int index, Object value) {
    return backingList.set(index, value);
  }

  public void add(int index, Object value) {
    copyList().add(index, value);
  }

  public Object remove(int index) {
    return copyList().remove(index);
  }
}
