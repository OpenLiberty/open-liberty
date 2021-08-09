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

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.exceptions.RepositoryResourceCreationException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.exceptions.RepositoryResourceNoConnectionException;
import com.ibm.ws.repository.exceptions.RepositoryResourceUpdateException;
import com.ibm.ws.repository.exceptions.RepositoryResourceValidationException;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl.AttachmentResourceImpl;

/**
 * This strategy will overwrite an existing resource if it finds a matching one. This strategy uses the default mechanism
 * for setting the state of the overwritten resource, which is: If there is a matching resource and a
 * desiredStateIfMatchingFound state has been set then use it. If a desiredStateIfMatchingFound has not been set (its
 * null) then use the matching resource's state
 */
public class UpdateInPlaceStrategy extends BaseStrategy {

    private boolean _forceReplace;
    private List<RepositoryResourceImpl> _matchingResources = null;

    /**
     * Delegate so super class for states
     */
    public UpdateInPlaceStrategy() {}

    /**
     * Sets the desired state of the asset after uploading it
     *
     * @param desiredStateIfMatchingFound Set the resource to this state if a matching resource was found. If this
     *            is set to null then it will set the state to whatever state the matching resource is set to.
     * @param desiredStateIfNoMatchingFound If no matching resource is found then set the state to this value
     * @param forceReplace Set to true if you wish to perform a replace even if a matching resource was found.
     */
    public UpdateInPlaceStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound, boolean forceReplace) {
        super(desiredStateIfMatchingFound, desiredStateIfNoMatchingFound);
        _forceReplace = forceReplace;
    }

    /**
     * Sets the desired state of the asset after uploading it
     *
     * @param desiredStateIfMatchingFound Set the resource to this state if a matching resource was found. If this
     *            is set to null then it will set the state to whatever state the matching resource is set to.
     * @param desiredStateIfNoMatchingFound If no matching resource is found then set the state to this value
     * @param forceReplace Set to true if you wish to perform a replace even if a matching resource was found.
     * @param matchingResource Set this if you wish to specify the resource to be replaced rather than letting
     *            the resource try and find a matching. This is of use in scenarios where there may be more than one matching
     *            resource and the caller can decide which one to use, as the resource logic will select the first matching one
     *            it finds
     */
    public UpdateInPlaceStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound, boolean forceReplace,
                                 RepositoryResourceImpl matchingResource) {
        super(desiredStateIfMatchingFound, desiredStateIfNoMatchingFound);
        _forceReplace = forceReplace;
        if (matchingResource != null) {
            _matchingResources = new ArrayList<RepositoryResourceImpl>();
            _matchingResources.add(matchingResource);
        }
    }

    @Override
    public List<RepositoryResourceImpl> findMatchingResources(RepositoryResourceImpl resource) throws RepositoryResourceValidationException, RepositoryBackendException, RepositoryBadDataException, RepositoryResourceNoConnectionException {
        if (_matchingResources != null) {
            return _matchingResources;
        } else {
            return resource.findMatchingResource();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     */
    @Override
    public void uploadAsset(RepositoryResourceImpl resource, List<RepositoryResourceImpl> matchingResources) throws RepositoryBackendException, RepositoryResourceException {
        RepositoryResourceImpl firstMatch = (matchingResources == null || matchingResources.isEmpty()) ? null : matchingResources.get(0);
        State targetState = calculateTargetState(firstMatch);

        // We need to unpublish in order to overwrite
        if (firstMatch != null && firstMatch.getState() == State.PUBLISHED) {
            firstMatch.unpublish();
        }

        // Is this an update or an add?
        switch (resource.updateRequired(firstMatch)) {
            case ADD:
                resource.addAsset();
                break;
            case NOTHING:
                if (!_forceReplace) {
                    // Use the asset from massive, although it's identical as far as we are concerned it
                    // also has the fields massive sets onto an asset when it gets uploaded, including
                    // the id itself.
                    resource.copyAsset(firstMatch);
                    break;
                }
                // If force replace is true then drop through to the update logic.
            case UPDATE:
                // Partial updates don't work so copy our data into the one
                // we found in massive and then set our asset to point to that
                // merged asset
                resource.overWriteAssetData(firstMatch, true);
                resource.updateAsset();
                break;
        }

        // Now iterate over the attachments
        for (AttachmentResourceImpl attachment : resource.getAttachmentImpls()) {
            uploadAttachment(resource, attachment, firstMatch);
        }

        // Read back from massive to get the fields massive generated into our resource
        resource.refreshFromMassive();

        resource.moveToState(targetState);
    }

    /**
     * Goes through each attachment and adds/updates the attachment as needed.
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws RepositoryResourceUpdateException
     * @throws RepositoryBadDataException
     * @throws RepositoryResourceCreationException
     */
    public void uploadAttachment(RepositoryResourceImpl resource, AttachmentResourceImpl attachment,
                                 RepositoryResourceImpl matchingResource) throws RepositoryResourceCreationException, RepositoryBadDataException, RepositoryResourceUpdateException, RepositoryBackendException, RepositoryResourceException {
        switch (attachment.updateRequired(matchingResource)) {
            case ADD:
                resource.addAttachment(attachment);
                break;
            case UPDATE:
                resource.updateAttachment(attachment);
                break;
            case NOTHING:
                // Nothing to do but have to include this to stop findbugs crying
                break;
        }
    }
}
