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
package com.ibm.ws.sib.processor.utils.am;

import com.ibm.ws.sib.processor.utils.am.BatchedTimeoutManager.LinkedListEntry;

/**
 * The entry provided to the BatchedTimeoutManager. The get and set methods are used
 * by the BatchedTimeoutManager to store data in this entry, for efficient removal.
 */
public interface BatchedTimeoutEntry
{
  public LinkedListEntry getEntry();
  public void setEntry(LinkedListEntry entry);
  public void cancel();
}
