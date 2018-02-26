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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;
import com.ibm.ws.repository.strategies.writeable.UpdateInPlaceStrategy;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;

public class UpdateInPlaceStrategyTest extends StrategyTestBaseClass {

    @Before
    public void requireUpdateCapability() {
        assumeThat(fixture.isUpdateSupported(), is(true));
    }

    @Test
    public void testAddingToRepoUsingUpdateInPlaceStrategy() throws RepositoryBackendException, RepositoryResourceException {
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        SampleResourceImpl readBack = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Perform an update but make sure there are no changes
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        SampleResourceImpl readBack2 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        checkSame(readBack, readBack2, _testRes, true, true);

        _testRes.setFeaturedWeight("5");
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        SampleResourceImpl readBack3 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        checkUpdated(readBack2, readBack3, _testRes, true);
    }

    @Test
    public void testAddingToRepoUsingUpdateInPlaceStrategyWithForceReplace() throws RepositoryBackendException, RepositoryResourceException {
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        SampleResourceImpl readBack = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        assertTrue("The read back resource should be equivalent to the one we put in", readBack.equivalent(_testRes));

        // Perform an update but make sure there are no changes
        _testRes.uploadToMassive(new UpdateInPlaceStrategy(State.DRAFT, State.DRAFT, true));
        SampleResourceImpl readBack2 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        checkSame(readBack, readBack2, _testRes, true, false);

        _testRes.setFeaturedWeight("5");
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        SampleResourceImpl readBack3 = (SampleResourceImpl) repoConnection.getResource(_testRes.getId());
        checkUpdated(readBack2, readBack3, _testRes, true);
    }

    @Override
    protected UploadStrategy createStrategy(State ifMatching, State ifNoMatching) {
        return new UpdateInPlaceStrategy(ifMatching, ifNoMatching, false);
    }

}
