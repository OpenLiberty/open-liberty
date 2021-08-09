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
package com.ibm.ws.repository.resources.writeable;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;
import java.util.Locale;

import com.ibm.ws.repository.common.enums.AttachmentLinkType;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.DownloadPolicy;
import com.ibm.ws.repository.common.enums.LicenseType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryResourceCreationException;
import com.ibm.ws.repository.exceptions.RepositoryResourceDeletionException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;
import com.ibm.ws.repository.transport.model.WlpInformation;

/**
 * Represents a resource which could be written to a repository.
 * <p>
 * This interface allows write access to the fields which are common to all repository resources. Sub-interfaces contain more specialized methods for fields which are specific to
 * certain types of resource.
 */
public interface RepositoryResourceWritable extends RepositoryResource {

    /**
     * By default this class uses a cached version of the resource which may have
     * stale data (including attachments), calling this method forces the class to grab a
     * new version of the resource from massive.
     *
     * @throws RepositoryBackendException
     */
    public void refreshFromMassive() throws RepositoryBackendException, RepositoryResourceException;

    public void setRepositoryConnection(RepositoryConnection repoConnection);

    /**
     * Unsets the resource ID
     */
    public void resetId();

    /**
     * Sets the human readable name of the resource
     *
     * @param name the human readable name of the resource
     */
    public void setName(String name);

    /**
     * Sets the provider name
     *
     * @param providerName the provider name
     */
    public void setProviderName(String providerName);

    /**
     * Sets the provider URL
     *
     * @param providerUrl
     */
    public void setProviderUrl(String providerUrl);

    /**
     * Sets the long description for the resource
     * <p>
     * This is used as the body text on the wasdev website and is in HTML format.
     *
     * @param desc a long HTML description of the resource
     */
    public void setDescription(String desc);

    /**
     * Sets the short description of the resource
     *
     * @param shortDescription The short description to use for the resource
     */
    public void setShortDescription(String shortDescription);

    /**
     * Gets the {@link State} of the resource.
     * <p>
     * The state cannot be set by other fields but may be updated by the upload strategy when calling {@link #uploadToMassive(UploadStrategy)}.
     *
     * @return the State of the resource
     */
    public State getState();

    /**
     * Sets the version of the resource.
     *
     * @param version the version of the resource
     */
    public void setVersion(String version);

    /**
     * Sets the download policy of the resource
     *
     * @param policy the download policy of the resource
     */
    public void setDownloadPolicy(DownloadPolicy policy);

    /**
     * Sets the {@link DisplayPolicy} to use
     *
     * @param policy the display policy
     */
    public void setDisplayPolicy(DisplayPolicy policy);

    /**
     * Sets the relative URL where this resource should appear if a website is used to display resources in the repository.
     * <p>
     * Generally this value is an easier to remember identifier for the resource that is more SEO friendly.
     *
     * @param vanityUrl a relative URL for this resource
     */
    public void setVanityURL(String vanityUrl);

    /**
     * Gets the relative URL where this resource should appear if a website is used to display resources in the repository.
     *
     * @return a relative URL for this resource
     */
    public String getVanityURL();

    /**
     * Sets the featured weight. This is a String value although it will be a number from 1 to 9, with 9 being the highest weight.
     * Not having a value set means it is lower in priority than any that have a weight.
     *
     * @param featuredWeight
     */
    public void setFeaturedWeight(String featuredWeight);

    /**
     * Returns a collection of all of the minimum versions from this resource's applies to String.
     *
     * @return a collection of versions which may be empty but won't be <code>null</code>
     */
    public Collection<String> getAppliesToMinimumVersions();

    /**
     * (Re)calculate any fields that may be based on information within this resource.
     *
     * @param performEditionChecking whether to validate the product editions in the appliesTo field
     * @throws RepositoryResourceCreationException if the resource applies to an invalid edition and {@code performEditionChecking} is {@code true}
     */
    public void updateGeneratedFields(boolean performEditionChecking) throws RepositoryResourceCreationException;

    /**
     * Adds a main, or "content" attachment to the resource which will be stored in the repository. The filename is used as the name of the attachment.
     * <p>
     * This is equivalent to calling {@link #addContent(File, String) addContent(file, file.getName())}
     *
     * @param file the file to be added as the main attachment
     * @return the AttachmentResource representation of the supplied file
     * @throws RepositoryException if there is a problem adding the attachment
     */
    public AttachmentResourceWritable addContent(File file) throws RepositoryException;

    /**
     * Adds a main, or "content" attachment to the resource which will be stored in the repository.
     * <p>
     * There can be only one attachment of AttachmentType.CONTENT.
     * <p>
     * When the resource is uploaded, the attachment file will also be uploaded to the repository.
     *
     * @param file the file to be added as the main attachment
     * @param name the name of the attachment
     * @return the AttachmentResource representation of the supplied file
     * @throws RepositoryException if there is a problem adding the attachment
     */
    public AttachmentResourceWritable addContent(File file, String name) throws RepositoryException;

    /**
     * Adds a main, or "content" attachment to the resource which will be stored on a separate web server.
     * <p>
     * There can be only one attachment of AttachmentType.CONTENT.
     * <p>
     * The supplied file will be read to determine the attachment metadata. However, when the resource is uploaded, the supplied file will not be uploaded to the repository.
     * Instead
     * the attachment record will contain the supplied URL which points to the location where the attachment is stored.
     * <p>
     * It is the callers responsibility to ensure that the supplied file is available at the supplied URL.
     * <p>
     * Note that the attachment metadata includes the CRC and SHA256 hash of the attachment file, so if the file on the separate web server is changed, the resource must also be
     * updated with the new file.
     * <p>
     * The linkType describes how the file can be retrieved. For features, this should always be {@link AttachmentLinkType#DIRECT} so that the feature file can be downloaded by the
     * installer.
     *
     * @param file the file to be added as the main attachment. This file will be read but not uploaded
     * @param name the name of the attachment
     * @param url the URL where the attachment file can be downloaded
     * @param linkType whether the URL points directly to the file, or to a webpage about the file
     * @return the AttachmentResource representation of the supplied file
     * @throws RepositoryException if there is a problem adding the attachment
     */
    public AttachmentResourceWritable addContent(File file, String name, String url, AttachmentLinkType linkType) throws RepositoryException;

    /**
     * Add an attachment of the given type to the resource. The file will be uploaded to the repository when the resource is uploaded.
     * <p>
     * This is equivalent to {@link #addAttachment(File, AttachmentType, String) addAttachment(file, attachmentType, file.getName()} <p>
     * <p>
     * In most cases, a more specific method should be used to add attachments (e.g. {@link #addContent(File)}, {@link #addLicense(File, Locale)}).
     *
     * @param file the file to add as an attachment
     * @param type the type of the attachment
     * @return the AttachmentResource representation of the supplied file
     * @throws RepositoryException if there is a problem adding the attachment.
     */
    public AttachmentResourceWritable addAttachment(File file, AttachmentType type) throws RepositoryException;

    /**
     * Adds the supplied attachment to the resource under the supplied name. The file will be uploaded to the repository when the resource is uploaded.
     * <p>
     * In most cases, a more specific method should be used to add attachments (e.g. {@link #addContent(File, String)}, {@link #addLicense(File, Locale)}).
     *
     * @param file The attachment to be added
     * @param name The name to use for the attachment
     *            a new attachment to be added
     * @return Returns the AttachmentResource representation of the supplied file
     * @throws RepositoryException
     */
    public AttachmentResourceWritable addAttachment(File file, AttachmentType type, String name) throws RepositoryException;

    /**
     * Adds the supplied attachment to the resource. A URL to the file will be stored in the repository but the file itself will never be uploaded.
     * <p>
     * Adds an attachment of the given type. The attachment is not stored in the repository as described for {@link #addContent(File, String, String, AttachmentLinkType)}.
     *
     * @param file the attachment file to add
     * @param type the type of the attachment
     * @param name the name of the attachment
     * @param url the URL where the attachment is stored
     * @param linkType whether the given URL links directly to the file or to a web page
     * @return the AttachmentResource representation of the supplied file
     * @throws RepositoryException if there is a problem adding the attachment
     * @see #addAttachment(File, AttachmentType, String, String, AttachmentLinkType)
     */
    public AttachmentResourceWritable addAttachment(File file, AttachmentType type, String name, String url, AttachmentLinkType linkType) throws RepositoryException;

    /**
     * Adds a complete license file attachment to the resource
     *
     * @param license the license file
     * @param loc the locale of the license text
     * @throws RepositoryException if there is a problem adding the license
     */
    public void addLicense(File license, Locale loc) throws RepositoryException;

    /**
     * Adds a license agreement file attachment to the resource
     *
     * @param license the license agreement file
     * @param loc the locale of the license agreement text
     * @throws RepositoryException if there is a problem adding the license agreement
     */
    public void addLicenseAgreement(File license, Locale loc) throws RepositoryException;

    /**
     * Adds a license information file attachment to the resource
     *
     * @param license the license information file
     * @param loc the locale of the license information text
     * @throws RepositoryException if there is a problem adding the license information
     */
    public void addLicenseInformation(File license, Locale loc) throws RepositoryException;

    /**
     * Sets the license type of the resource
     *
     * @param lt the license type
     */
    public void setLicenseType(LicenseType lt);

    /**
     * Set the LicenseId.
     * <p>
     * Currently this is only set for features based on the Subsystem-License header
     *
     * @param lic the license id
     */
    public void setLicenseId(String lic);

    /**
     * Set the maven coordinates
     * <p>
     * This must only be set for resources which are stored in a maven repository.
     * <p>
     * Maven coordinates must be given as a string in the format {@code groupId:assetId:version}.
     */
    public void setMavenCoordinates(String mavenCoordinates);

    /**
     * Sets the {@link WlpInformation} version of the asset
     *
     * @param wlpInformationVersion The {@link WlpInformation} version to use for the asset
     */
    public void setWlpInformationVersion(String wlpInformationVersion);

    /**
     * Uploads the resource to the repository using the supplied strategy
     *
     * @param strategy the upload strategy to use
     * @throws RepositoryBackendException if there is a problem communicating with the remote repository
     * @throws RepositoryResourceException if there is another problem uploading the resource
     */
    public void uploadToMassive(UploadStrategy strategy) throws RepositoryBackendException, RepositoryResourceException;

    /**
     * Deletes the resource and all the attachments
     * <p>
     * The caller should not make any changes to the object after calling this method
     *
     * @throws RepositoryResourceDeletionException if there is a problem deleting the resource
     */
    public void delete() throws RepositoryBackendException, RepositoryResourceDeletionException;

    /**
     * Returns a URL which represent the URL that can be used to view the asset in Massive. This is more
     * for testing purposes, the assets can be access programatically via various methods on this class.
     * This is only used by the uploaders so is only for massive based repos
     *
     * @return String - the asset URL
     */
    public String getAssetURL();

    /**
     * Returns the date and time when the asset was last updated.
     *
     */
    public Calendar getLastUpdatedOn();

}