/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.jbatch.container.services.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.batch.runtime.BatchStatus;

import org.junit.Test;

public class AbstractPersistenceManagerTest {

    @Test
    public void testIsDone() {
        
        
        assertTrue( AbstractPersistenceManager.FINAL_STATUS_SET.contains(BatchStatus.ABANDONED));
        assertTrue( AbstractPersistenceManager.FINAL_STATUS_SET.contains(BatchStatus.COMPLETED));
        assertTrue( AbstractPersistenceManager.FINAL_STATUS_SET.contains(BatchStatus.STOPPED));
        assertTrue( AbstractPersistenceManager.FINAL_STATUS_SET.contains(BatchStatus.FAILED));
        assertFalse( AbstractPersistenceManager.FINAL_STATUS_SET.contains(BatchStatus.STARTING));
        assertFalse( AbstractPersistenceManager.FINAL_STATUS_SET.contains(BatchStatus.STARTED));
        assertFalse( AbstractPersistenceManager.FINAL_STATUS_SET.contains(BatchStatus.STOPPING));
    }
    
}
