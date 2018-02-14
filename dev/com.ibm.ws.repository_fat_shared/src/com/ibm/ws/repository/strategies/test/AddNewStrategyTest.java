/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.strategies.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;
import com.ibm.ws.repository.strategies.writeable.AddNewStrategy;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;

public class AddNewStrategyTest extends StrategyTestBaseClass {

    @Test
    public void testAddingToRepoUsingAddNewStrategyStrategy() throws RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResourceImpl readBack = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertTrue(
                   "The read back resource should be equivalent to the one we put in",
                   readBack.equivalent(_testRes));

        // Make sure there is a new asset uploaded (this strategy will
        // mean an upload will always create a new asset)
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResourceImpl readBack2 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertTrue("The new asset should be equivalent to the one already added", readBack2.equivalent(readBack));
        assertFalse("The IDs of the assets should be different", readBack2.getId() == readBack.getId());
        assertEquals("There should now be two resource in the repo", 2, repoConnection.getAllResourcesWithDupes().size());

        _testRes.setFeaturedWeight("5");
        _testRes.uploadToMassive(new AddNewStrategy());
        SampleResourceImpl readBack3 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertFalse("The new asset should not be equivalent to the one already added", readBack3.equivalent(readBack2));
        assertFalse("The IDs of the assets should be different", readBack3.getId() == readBack2.getId());
        assertEquals("There should now be three resource in the repo", 3, repoConnection.getAllResourcesWithDupes().size());
    }

    @Override
    protected UploadStrategy createStrategy(State ifMatching, State ifNoMatching) {
        return new AddNewStrategy(ifMatching, ifNoMatching);
    }

}
