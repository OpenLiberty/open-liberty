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
package com.ibm.ws.repository.transport.client;

import java.io.IOException;

import com.ibm.ws.repository.common.enums.StateAction;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.ClientFailureException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentSummary;

/**
 *
 */
public interface RepositoryWriteableClient extends RepositoryClient {

    /**
     * This method will update the state of an object by taking the supplied
     * action.
     *
     * @param assetId
     *            The ID of the asset to update
     * @param action
     *            The action to take to modify the state
     * @return <code>true</code> if the update was successful (currently always
     *         returns <code>true</code> from Massive)
     * @throws IOException
     * @throws RequestFailureException
     */
    public void updateState(final String assetId, final StateAction action) throws IOException, RequestFailureException;

    /**
     * Adds an asset into Massive. Note that Massive will set some fields (such
     * as ID) so it is important to switch to the returned object after calling
     * this method.
     *
     * @param asset
     *            The asset to add, it will not be modified by this method
     * @return The asset with information added by Massive
     * @throws IOException
     */
    public Asset addAsset(final Asset asset) throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException;

    /**
     * Updates an asset in Massive. The {@link Asset#get_id()} must return the
     * correct ID for this asset. Note that Massive will set some fields (such
     * as last update date) so it is important to switch to the returned object
     * after calling this method.
     *
     * @param asset
     *            The asset to add, it will not be modified by this method
     * @return The asset with information added by Massive
     * @throws IOException
     * @throws RequestFailureException
     */
    public Asset updateAsset(final Asset asset) throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException;

    /**
     * @param assetId The id of the asset where the attachment should be added
     * @param attSummary Infomation about the attachment to be added
     */
    public Attachment addAttachment(final String assetId, final AttachmentSummary attSummary) throws IOException, BadVersionException, RequestFailureException, SecurityException;

    /**
     * This method will update an existing attachment on an asset. Note that
     * Massive currently doesn't support update attachment so this will do a
     * delete and an add.
     *
     * @param assetId
     *            The ID of the asset that the attachment is attached to
     * @param name
     *            The name of the attachment to update
     * @param file
     *            The file to attach
     * @param attach
     *            Attachment metadata
     * @return
     * @throws IOException
     * @throws RequestFailureException
     */
    public Attachment updateAttachment(final String assetId, final AttachmentSummary summary) throws IOException, BadVersionException, RequestFailureException, SecurityException;

    /**
     * @param assetId The id of the asset which contains the attachment
     * @param attachmentId The id of the attachment within the asset
     */
    public void deleteAttachment(final String assetId, final String attachmentId) throws IOException, RequestFailureException;

    /**
     * @param assetId The id of the asset to be deleted. All attachments attached to this asset will be deleted as well
     */
    public void deleteAssetAndAttachments(final String assetId) throws IOException, RequestFailureException;

}
