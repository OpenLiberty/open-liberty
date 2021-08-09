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

package com.ibm.ws.sib.msgstore.test.persistence.dispatcher;
/*
 * Change activity:
 *
 * Reason     Date        Origin     Description
 * ---------- ----------- --------   ------------------------------------------
 * 184390.1.1 26-Jan-04   schofiel   Revised Reliability Qualities of Service - MS - Tests for spill
 * 184390.1.3 27-Feb-04   schofiel   Revised Reliability Qualities of Service - MS - PersistentDispatcher
 * ============================================================================
 */

import com.ibm.ws.sib.msgstore.persistence.Persistable;

public interface PersistableEventDispatchListener
{
    public void eventDispatchBegun(Persistable p);

    public void eventDispatchCancelled(Persistable p);

    public void eventDispatchCompleted(Persistable p);
    
    public void eventExecuteBatch(int bcIdentity, int batchNumber, int numTasksInBatch);
}
