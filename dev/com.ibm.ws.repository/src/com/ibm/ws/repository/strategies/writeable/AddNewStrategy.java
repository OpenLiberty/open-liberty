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
package com.ibm.ws.repository.strategies.writeable;

import java.util.List;

import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.exceptions.RepositoryResourceNoConnectionException;
import com.ibm.ws.repository.exceptions.RepositoryResourceValidationException;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl.AttachmentResourceImpl;

/**
 * This strategy will add a new resource to the repository. It does not care if an equivalent resource exists, it will
 * always create a new (potentially duplicate) resource.
 */
public class AddNewStrategy extends BaseStrategy {

    /**
     * Delegate to super class for states
     */
    public AddNewStrategy() {}

    /**
     * Sets the desired state of the asset after uploading it
     *
     * @param desiredStateIfMatchingFound This is not used by this strategy but can be used by derived strategies
     * @param desiredStateIfNoMatchingFound Set the resource to this state after uploading. This behaviour can
     *            be changed by derived classes
     */
    public AddNewStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound) {
        super(desiredStateIfMatchingFound, desiredStateIfNoMatchingFound);
    }

    /** {@inheritDoc} */
    @Override
    public void uploadAsset(RepositoryResourceImpl resource, List<RepositoryResourceImpl> matchingResources) throws RepositoryBackendException, RepositoryResourceException {
        // Add the asset
        resource.addAsset();

        // ... and the attachments
        for (AttachmentResourceImpl attachment : resource.getAttachmentImpls()) {
            resource.addAttachment(attachment);
        }

        // read back any fields massive added during upload
        resource.refreshFromMassive();

        // Move the resource to the desired state, use the noMatching state as matching will be null by default
        RepositoryResourceImpl firstMatch = (matchingResources == null || matchingResources.isEmpty()) ? null : matchingResources.get(0);
        resource.moveToState(getTargetState(firstMatch));
    }

    /**
     * Get the state that the resource being added should be set to once it's been uploaded
     *
     * @param matchingResource
     * @return
     */
    protected State getTargetState(RepositoryResourceImpl matchingResource) {
        return _desiredStateIfNoMatchingFound;
    }

    @Override
    public List<RepositoryResourceImpl> findMatchingResources(RepositoryResourceImpl resource) throws RepositoryResourceValidationException, RepositoryBackendException, RepositoryBadDataException, RepositoryResourceNoConnectionException {
        return null;
    }
}
