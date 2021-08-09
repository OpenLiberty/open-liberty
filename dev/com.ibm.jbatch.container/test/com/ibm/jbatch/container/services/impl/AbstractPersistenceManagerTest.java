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

        assertTrue(AbstractPersistenceManager.isFinalBatchStatus(BatchStatus.ABANDONED));
        assertTrue(AbstractPersistenceManager.isFinalBatchStatus(BatchStatus.COMPLETED));
        assertTrue(AbstractPersistenceManager.isFinalBatchStatus(BatchStatus.STOPPED));
        assertTrue(AbstractPersistenceManager.isFinalBatchStatus(BatchStatus.FAILED));
        assertFalse(AbstractPersistenceManager.isFinalBatchStatus(BatchStatus.STARTING));
        assertFalse(AbstractPersistenceManager.isFinalBatchStatus(BatchStatus.STARTED));
        assertFalse(AbstractPersistenceManager.isFinalBatchStatus(BatchStatus.STOPPING));
    }

}
