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
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.exceptions.RepositoryResourceNoConnectionException;
import com.ibm.ws.repository.exceptions.RepositoryResourceValidationException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl.AttachmentResourceImpl;

/**
 * This strategy will check if there is a matching resource.<br>
 * If the resource is equivalent, it will not upload anything.<br>
 * If the resource is different then a new resource is added to the repo and the original resource is deleted<br>
 * If there is no matching resource then the resource is added.<br>
 * <br>
 * The resource is then moved to the desired state based on the values passed to the constructor and whether a matching
 * resource was found. If there is no matching resource use desiredStateIfNoMatchingFound. If there is a matching
 * resource and a desiredStateIfMatchingFound state has been set then use it. If a desiredStateIfMatchingFound has not
 * been set (it's null) then set the new resource's state to match the state of the matching resource found in the
 * repo.
 */
public class AddThenDeleteStrategy extends AddNewStrategy {

    private boolean _forceReplace;
    private List<RepositoryResourceImpl> _matchingResources = null;
    private List<RepositoryResource> _deletedResources = null;

    /**
     * Delegate to super class for states
     */
    public AddThenDeleteStrategy() {}

    /**
     * Sets the desired state of the asset after uploading it
     *
     * @param desiredStateIfMatchingFound Set the resource to this state if a matching resource was found. If this
     *            is set to null then it will set the state to whatever state the matching resource is set to.
     * @param desiredStateIfNoMatchingFound If no matching resource is found then set the state to this value
     * @param forceReplace Set to true if you wish to perform a replace even if a matching resource was found.
     */
    public AddThenDeleteStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound, boolean forceReplace) {
        this(desiredStateIfMatchingFound, desiredStateIfNoMatchingFound, forceReplace, null);
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
     * @param deletedResources Set this if you wish to track which resources are deleted by this strategy. Deleted resources will
     *            be added to this list. If the list is not thread-safe then this strategy will not be thread-safe.
     */
    public AddThenDeleteStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound, boolean forceReplace,
                                 RepositoryResourceImpl matchingResource) {
        this(desiredStateIfMatchingFound, desiredStateIfNoMatchingFound, forceReplace, matchingResource, null);
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
     * @param deletedResources Set this if you wish to track which resources are deleted by this strategy. Deleted resources will
     *            be added to this list. If the list is not thread-safe then this strategy will not be thread-safe.
     */
    public AddThenDeleteStrategy(State desiredStateIfMatchingFound, State desiredStateIfNoMatchingFound, boolean forceReplace,
                                 RepositoryResourceImpl matchingResource, List<RepositoryResource> deletedResources) {
        super(desiredStateIfMatchingFound, desiredStateIfNoMatchingFound);
        _forceReplace = forceReplace;
        if (matchingResource != null) {
            _matchingResources = new ArrayList<RepositoryResourceImpl>();
            _matchingResources.add(matchingResource);
        }
        _deletedResources = deletedResources;
    }

    /**
     * {@inheritDoc}
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     */
    @Override
    public void uploadAsset(RepositoryResourceImpl resource, List<RepositoryResourceImpl> matchingResources) throws RepositoryBackendException, RepositoryResourceException {
        boolean doUpdate = false;
        boolean deleteOriginal = false;
        RepositoryResourceImpl firstMatch = (matchingResources == null || matchingResources.isEmpty()) ? null : matchingResources.get(0);

        // Check if we need to do an update
        if (_forceReplace) {
            doUpdate = true;
            deleteOriginal = firstMatch == null ? false : true;
        } else {
            // First check assert itself for changes
            switch (resource.updateRequired(firstMatch)) {
                // This strategy will add a new asset instead of overwriting an existing one
                // Update should fall through to add
                case UPDATE:
                    deleteOriginal = true;
                case ADD:
                    doUpdate = true;
                    break;
                default:
                    // Nothing to do but have to include this to stop findbugs crying
            }

            // If the asset is the same, check attachments just in case they have changed
            if (!doUpdate) {
                // Then check each attachment
                loop: for (AttachmentResourceImpl attachment : resource.getAttachmentImpls()) {
                    switch (attachment.updateRequired(firstMatch)) {
                        case UPDATE:
                        case ADD:
                            deleteOriginal = true;
                            doUpdate = true;
                            break loop;
                        default:
                            // Nothing to do but have to include this to stop findbugs crying
                    }
                }
            }
        }

        // Only do an update if needed
        if (doUpdate) {

            super.uploadAsset(resource, matchingResources);

            // If the action was an update to an existing resource then delete the original now
            if (deleteOriginal) {
                for (RepositoryResourceImpl massiveResource : matchingResources) {
                    massiveResource.delete();
                    if (_deletedResources != null) {
                        _deletedResources.add(massiveResource);
                    }
                }
            }

        } else {
            // Use the asset from massive, although it's identical as far as we are concerned it
            // also has the fields massive sets onto an asset when it gets uploaded, including
            // the id itself.
            resource.copyAsset(firstMatch);

            // Also remove duplicates so there is only one asset left.  NOTE starting at 1, don't delete the first one as this is what we are using
            if (matchingResources.size() > 1) {
                for (int i = 1; i < matchingResources.size(); i++) {
                    RepositoryResourceImpl res = matchingResources.get(i);
                    res.delete();
                    if (_deletedResources != null) {
                        _deletedResources.add(res);
                    }
                }
            }

            // Finally, make sure the existing asset is in the desired state
            resource.moveToState(getTargetState(firstMatch));
        }
        resource.refreshFromMassive();
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
     * The target state is dependent on if a matching resource is found. See {@link AddThenDeleteStrategy} class doc
     */
    @Override
    protected State getTargetState(RepositoryResourceImpl matchingResource) {
        return calculateTargetState(matchingResource);
    }
}
