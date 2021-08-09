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
package com.ibm.ws.repository.resources;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.DownloadPolicy;
import com.ibm.ws.repository.common.enums.LicenseType;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.transport.model.WlpInformation;

/**
 * Represents a resource in a repository.
 * <p>
 * This interface allows read access to the fields which are common to all repository resources. Sub-interfaces contain more specialized getter methods for fields which are
 * specific to certain types of resource.
 */
public interface RepositoryResource {

    /**
     * Gets the repository connection which this resource is associated with
     * <p>
     * This will only be null if the resource was not retrieved from a repository.
     *
     * @return the associated repository connection, or null if the resource is not associated with a repository
     */
    public RepositoryConnection getRepositoryConnection();

    /**
     * Gets the resource ID
     * <p>
     * This is an arbitrary string which uniquely identifies the resource within the repository. Resource IDs may change if the resource is updated in the repository.
     *
     * @return the resource ID, or null if the resource was not retrieved from a repository
     */
    public String getId();

    /**
     * Gets the human readable name of the resource
     *
     * @return the human readable name of the resource, or null if it is not set
     */
    public String getName();

    /**
     * Gets the provider name
     *
     * @return the provider name, or null if it is not set
     */
    public String getProviderName();

    /**
     * Gets the provider URL
     *
     * @return the provider URL
     */
    public String getProviderUrl();

    /**
     * Gets the type of this resource (Feature, Product etc.)
     *
     * @return the resource type, or null if it is not set
     */
    public ResourceType getType();

    /**
     * Gets the long description for the resource.
     * <p>
     * This is used as the body text on the wasdev website and is in HTML format.
     *
     * @return a long HTML description of the resource, or null if it is not set
     */
    public String getDescription();

    /**
     * Gets the version of the resource
     *
     * @return the version, or null if it is not set
     */
    public String getVersion();

    /**
     * Gets the size of the main attachment of the resource, if it has a main attachment.
     *
     * @return the size of the main attachment in bytes, or zero if there is no main attachment
     */
    public long getMainAttachmentSize();

    /**
     * Gets the SHA256 hash of the main attachment of the resource, if it has a main attachment.
     *
     * @return the SHA256 hash of the main attachment, or null if there is no main attachment
     */
    public String getMainAttachmentSHA256();

    /**
     * Gets the download policy of the resource.
     *
     * @return the download policy of the resource
     */
    public DownloadPolicy getDownloadPolicy();

    /**
     * Gets the featured weight. This is a String value although it will be a number from 1 to 9, with 9 being the highest weight.
     * Not having a value set means it is lower in priority than any that have a weight.
     *
     * @return the featuredWeight, or null if it has not been set
     */
    public String getFeaturedWeight();

    /**
     * Gets the version from the applies to field for the applies to entry that matches the supplied product definition.
     * Note that version information in the ProductDefinition is ignored, we match against the productId, InstallType
     * and ensure the edition supplied is one in the editions list in the applies to.
     *
     * @param def The product definition to look for in the appliesTo field.
     * @return The version, as read from the appliesTo field
     */
    public String getAppliesToVersions(Collection<ProductDefinition> definitions);

    /**
     * Gets the "main" attachment for this resource. It will cycle through the assets asking the
     * resource if the attachment is the "main" one, it will stop once it finds the first
     * attachment that satisfies that criteria.
     *
     * @return the first attachment considered to be a "main" attachment, or null if there is no "main" attachment
     */
    public AttachmentResource getMainAttachment() throws RepositoryBackendException, RepositoryResourceException;

    /**
     * Get an {@link AttachmentResource} for the specified attachment name
     *
     * @param attachmentName The name of the attachment to look for
     * @return An {@link AttachmentResource} object that matches the supplied name
     */
    public AttachmentResource getAttachment(String attachmentName) throws RepositoryBackendException, RepositoryResourceException;

    /**
     * This returns a a {@link Collection} of {@link AttachmentResource} objects associated with the asset. We cache
     * the list of attachments by obtaining a new asset if the current asset doesn't have any. To force
     * the asset to be refreshed from massive call {@link #refreshFromMassive()} method.
     *
     * @return a {@link List} of {@link AttachmentResource} objects. This collection is unmodifiable, however
     *         methods like delete may be called on the AttachmentResources within the collection
     * @throws RepositoryException
     */
    public Collection<AttachmentResource> getAttachments() throws RepositoryBackendException, RepositoryResourceException;

    /**
     * Returns the combined License Agreement and License Information HTML attachment that best matches the given locale.
     * <p>
     * If no LA+LI attachment matches the given locale, the English attachment will be returned. If there is no English attachment then null will be returned.
     *
     * @param loc the locale to match against
     * @return the LA+LI HTML attachment that best matches the given locale, or null if one could not be found
     */
    public AttachmentResource getLicense(Locale loc) throws RepositoryBackendException, RepositoryResourceException;

    /**
     * Returns the License Agreement attachment that best matches the given locale.
     * <p>
     * If no LA attachment matches the given locale, the English attachment will be returned. If there is no English attachment then null will be returned.
     *
     * @param loc the locale to match against
     * @return the LA attachment that best matches the given locale, or null if one could not be found
     */
    public AttachmentResource getLicenseAgreement(Locale loc) throws RepositoryBackendException, RepositoryResourceException;

    /**
     * Returns the License Information attachment that best matches the given locale.
     * <p>
     * If no LI attachment matches the given locale, the English attachment will be returned. If there is no English attachment then null will be returned.
     *
     * @param loc the locale to match against
     * @return the LI attachment that best matches the given locale, or null if one could not be found
     */
    public AttachmentResource getLicenseInformation(Locale loc) throws RepositoryBackendException, RepositoryResourceException;

    /**
     * Gets the license type of the resource
     *
     * @return the license type of the resource, or null if it has not been set
     */
    public LicenseType getLicenseType();

    /**
     * Gets the value of the LicenseId.
     * For features, this is contained in the Subsystem-License header
     *
     * @return the license id, or null if it has not been set
     */
    public String getLicenseId();

    /**
     * Get the short description of the asset
     *
     * @return The short description of the asset, or null if it has not been set
     */
    public String getShortDescription();

    /**
     * Get the {@link DisplayPolicy}
     *
     * @return {@link DisplayPolicy} in use, or null if it has not been set
     */
    public DisplayPolicy getDisplayPolicy();

    /**
     * Get the {@link WlpInformation} version of the asset
     *
     * @return The {@link WlpInformation} version of the asset
     */
    public String getWlpInformationVersion();

    /**
     * Get the maven coordinates for the asset
     * <p>
     * This is only set for resources which are stored in a maven repository.
     * <p>
     * Maven coordinates are returned as a string in the format {@code groupId:assetId:version}.
     *
     * @return the maven coordinates for the resource, or {@code null} if they have not been set
     */
    public String getMavenCoordinates();

    /**
     * Writes the directory based repository formatted JSON to the supplied output stream
     *
     * @param writeJsonTo The output stream the JSON should be written to
     */
    public void writeDiskRepoJSONToStream(final OutputStream writeJsonTo) throws RepositoryResourceException;

    /**
     * This method dumps a formatted JSON string to the supplied out stream
     *
     * @param os The output stream the JSON should be dumped too.
     */
    public void dump(OutputStream os);

}