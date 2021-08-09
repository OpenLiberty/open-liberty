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

package com.ibm.ws.repository.resources.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import com.ibm.ws.repository.common.enums.AttachmentLinkType;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.DownloadPolicy;
import com.ibm.ws.repository.common.enums.LicenseType;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.ResourceTypeLabel;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.common.enums.StateAction;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.common.utils.internal.HashUtils;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.connections.internal.AbstractRepositoryConnection;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBackendIOException;
import com.ibm.ws.repository.exceptions.RepositoryBackendRequestFailureException;
import com.ibm.ws.repository.exceptions.RepositoryBadDataException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryOperationNotSupportedException;
import com.ibm.ws.repository.exceptions.RepositoryResourceCreationException;
import com.ibm.ws.repository.exceptions.RepositoryResourceDeletionException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.exceptions.RepositoryResourceLifecycleException;
import com.ibm.ws.repository.exceptions.RepositoryResourceNoConnectionException;
import com.ibm.ws.repository.exceptions.RepositoryResourceUpdateException;
import com.ibm.ws.repository.exceptions.RepositoryResourceValidationException;
import com.ibm.ws.repository.resources.AttachmentResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.AppliesToProcessor.AppliesToEntry;
import com.ibm.ws.repository.resources.writeable.AttachmentResourceWritable;
import com.ibm.ws.repository.resources.writeable.RepositoryResourceWritable;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.ClientFailureException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentSummary;
import com.ibm.ws.repository.transport.model.FilterVersion;
import com.ibm.ws.repository.transport.model.ImageDetails;
import com.ibm.ws.repository.transport.model.Provider;
import com.ibm.ws.repository.transport.model.WlpInformation;

/**
 * This class is used as the base class to provide an API for java applications to interface with
 * assets stored within massive. The data structure used to store assets in massive is kept separate
 * from the representation of a resource to protect calling applications from underlying changes to
 * massive or the data structure used.
 *
 * If this is class is used to read data from Massive, bear in mind that the asset information obtained
 * from massive is cached when first read. If you need to ensure that the data contained is up to date
 * please call the "refreshFromMassive" method which will reload the asset from massive.
 */
public abstract class RepositoryResourceImpl implements RepositoryResourceWritable {

    private static final int SERVER_ERROR = 500;

    // The backing asset for this resource
    protected Asset _asset;

    protected RepositoryReadableClient _client;

    /**
     * A flag to indicate whether the 'content' attachment has been attached.
     * Currently we only allow one such attachment.
     */
    private boolean _contentAttached = false;

    /**
     * IMPORTANT: Do not use this field directly, instead use getAttachments. This
     * field is updated lazily, it is not populated when the resource is created, but
     * instead upon the first call to getAttachments
     * Use a hashmap so we can use the string equals when checking if this resource already
     * has an attachment with that name
     */
    private HashMap<String, AttachmentResourceImpl> _attachments = new HashMap<String, AttachmentResourceImpl>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    private RepositoryConnection _repoConnection;

    /**
     * A match result enum used for checking if a resource matches a product definition.
     * Values are <br>
     * <I>MATCHED</I> : The resource matches<br>
     * <I>NOT_APPLICABLE</I> : The resource doesn't apply to the specified product or the resource
     * has no applies to information.<br>
     * <I>INVALID_VERSION</I>: The resource does not apply to the specified version<br>
     * <I>INVALID_EDITION</I>: The resource does not apply to the specified edition<br>
     * <I>INVALID_INSTALL_TYPE</I>: The resource does not apply to the specified install type.
     */
    public enum MatchResult {
        MATCHED, NOT_APPLICABLE, INVALID_VERSION, INVALID_EDITION, INVALID_INSTALL_TYPE;
    }

    /*
     * ------------------------------------------------------------------------------------------------
     * Constructor Code
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * The first logInfoResource from the collection is used. Deprecated in favor of using the constructor that takes
     * one RepositoryConnectionListResource.
     *
     * @param entry
     */
    protected static RepositoryReadableClient createClient(RepositoryConnection repoConnection) {
        return ((AbstractRepositoryConnection) repoConnection).createClient();
    }

    private RepositoryWriteableClient getWritableClient() throws RepositoryOperationNotSupportedException {
        if (_client instanceof RepositoryWriteableClient) {
            return (RepositoryWriteableClient) _client;
        } else {
            throw new RepositoryOperationNotSupportedException("The backend does not support write operations or no connection has been specified", getRepositoryConnection());
        }
    }

    /**
     *
     * @param repoConnection This can be set to null if the resource is being created. The connection can be set later by the {@link #setRepositoryConnection}
     *            method. Note that if no connection has been set and an operation is attempted that requires a connection a {@link RepositoryResourceNoConnectionException}
     *            will be thrown
     */
    public RepositoryResourceImpl(RepositoryConnection repoConnection) {
        this(repoConnection, null);
    }

    /**
     * Constructor, requires logon information to massive
     *
     * @param userId user id to log on to massive with
     * @param password password to log on to massive with
     * @param apiKey the apikey for the marketplace
     */
    public RepositoryResourceImpl(RepositoryConnection repoConnection, Asset ass) {
        _repoConnection = repoConnection;

        if (ass == null) {
            _asset = new Asset();
            _asset.setWlpInformation(new WlpInformation());

            // By default all assets are downloadable
            setDownloadPolicy(DownloadPolicy.ALL);
        } else {
            _asset = ass;
        }

        if (repoConnection != null) {
            _client = createClient(repoConnection);
        }
    }

    /**
     * Creates a new resource using the same logon infomation as this resource
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    protected <T extends RepositoryResourceImpl> T createNewResource() {

        T result;
        if (null == getType()) {
            result = (T) createTestResource(getRepositoryConnection());
        } else {
            result = ResourceFactory.getInstance().createResource(getType(), getRepositoryConnection(), null);
        }
        return result;
    }

    private static RepositoryResourceImpl createTestResource(RepositoryConnection repoConnection) {
        return new RepositoryResourceImpl(repoConnection, null) {};
    }

    /** {@inheritDoc} */
    @Override
    public void refreshFromMassive() throws RepositoryBackendException, RepositoryResourceException {
        try {
            _asset = _client.getAsset(_asset.get_id());
            parseAttachmentsInAsset();
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Unable to obtain asset from massive " + _asset.get_id(), ioe, getRepositoryConnection());
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("Bad version in asset ", _asset.get_id(), bvx);
        } catch (RequestFailureException bfe) {
            throw new RepositoryBackendRequestFailureException(bfe, getRepositoryConnection());
        }
    }

    /**
     * Check if this resources matches the supplied product definition
     *
     * @param def The product definition to check against
     * @return A {@link MatchResult} that says whether the resource matches the product definition, and if
     *         not the reason why the match failed.
     */
    public MatchResult matches(ProductDefinition def) {
        Collection<AppliesToFilterInfo> atfiList = _asset.getWlpInformation().getAppliesToFilterInfo();
        if (atfiList == null || atfiList.isEmpty()) {
            return MatchResult.NOT_APPLICABLE;
        }
        MatchResult matchResult = MatchResult.MATCHED;
        for (AppliesToFilterInfo atfi : atfiList) {
            if (!!!atfi.getProductId().equals(def.getId())) {
                // This one isn't applicable, maybe the next one is
                matchResult = MatchResult.NOT_APPLICABLE;
                continue;
            } else {
                if (def.getVersion() != null && !def.getVersion().isEmpty()) {

                    Version checkVersion = new Version(def.getVersion());
                    VersionRange vr = FilterVersion.getFilterRange(atfi.getMinVersion(), atfi.getMaxVersion());

                    if (!vr.includes(checkVersion)) {
                        return MatchResult.INVALID_VERSION;
                    }
                }

                if (atfi.getRawEditions() != null && !!!atfi.getRawEditions().isEmpty() && !!!atfi.getRawEditions().contains(def.getEdition())) {
                    return MatchResult.INVALID_EDITION;
                }

                if (atfi.getInstallType() != null && !!!atfi.getInstallType().equals(def.getInstallType())) {
                    return MatchResult.INVALID_INSTALL_TYPE;
                }

                // Got here so this must have been a match, only need one of the array to match, not all
                return MatchResult.MATCHED;
            }
        }
        return matchResult;
    }

    /**
     * This method reads the attachments from massive, effectively syncing up with massive.
     *
     * @throws RepositoryBackendException
     */
    public synchronized void parseAttachmentsInAsset() throws RepositoryBackendException {
        readAttachmentsFromAsset(_asset);
    }

    /**
     * Read the attachments from the supplied asset and create an AttachmentResource to represent them
     * and then store them in our AttachmentResource list
     *
     * @param ass
     * @throws RepositoryBackenAttachment
     */
    private synchronized void readAttachmentsFromAsset(Asset ass) {
        Collection<Attachment> attachments = ass.getAttachments();

        _attachments = new HashMap<String, AttachmentResourceImpl>();
        if (attachments != null) {
            for (Attachment at : attachments) {
                _attachments.put(at.getName(), new AttachmentResourceImpl(at));

                if (at.getType() == AttachmentType.CONTENT) {
                    _contentAttached = true;
                }
            }
        }

    }

    /**
     * ------------------------------------------------------------------------------------------------
     * Standard Asset Fields
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * Gets the asset associated with this resource
     *
     * @return the asset associated with this resource
     */
    protected Asset getAsset() {
        return _asset;
    }

    /** {@inheritDoc} */
    @Override
    public void setRepositoryConnection(RepositoryConnection repoConnection) {
        _repoConnection = repoConnection;
        _client = createClient(repoConnection);
        // Using a new repo - we should blow away the id.
        resetId();
    }

    /** {@inheritDoc} */
    @Override
    public RepositoryConnection getRepositoryConnection() {
        return _repoConnection;
    }

    /**
     * Gets the connection associated with the connection and throws a RepositoryResourceNoConnectionException
     * if no connection has been specified
     *
     * @return The connection associated with this resource
     * @throws RepositoryResourceNoConnectionException If no connection has been specified
     */
    public RepositoryConnection getAndCheckRepositoryConnection() throws RepositoryResourceNoConnectionException {
        if (_repoConnection == null) {
            // In order to have an ID then we should have already been stored so we should have a connection but just in case...
            throw new RepositoryResourceNoConnectionException("No connection has been specified for the resource when attemping to communicte with the repository", getId());
        }
        return _repoConnection;
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return _asset.get_id();
    }

    /** {@inheritDoc} */
    @Override
    public void resetId() {
        _asset.set_id(null);
    }

    /** {@inheritDoc} */
    @Override
    public void setName(String name) {
        _asset.setName(name);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return _asset.getName();
    }

    /** {@inheritDoc} */
    @Override
    public void setProviderName(String providerName) {
        Provider prov = _asset.getProvider();
        if (prov == null) {
            prov = new Provider();
            _asset.setProvider(prov);
        }
        prov.setName(providerName);
    }

    /** {@inheritDoc} */
    @Override
    public String getProviderName() {
        return _asset.getProvider() == null ? null : _asset.getProvider().getName();
    }

    /** {@inheritDoc} */
    @Override
    public void setProviderUrl(String providerUrl) {
        Provider prov = _asset.getProvider();
        if (prov == null) {
            prov = new Provider();
            _asset.setProvider(prov);
        }
        prov.setUrl(providerUrl);
    }

    /** {@inheritDoc} */
    @Override
    public String getProviderUrl() {
        return _asset.getProvider() == null ? null : _asset.getProvider().getUrl();
    }

    /** {@inheritDoc} */
    protected void setType(ResourceType type) {
        if (type == null) {
            _asset.setType(null);
            _asset.getWlpInformation().setTypeLabel(null);
        } else {
            _asset.setType(type);
            _asset.getWlpInformation().setTypeLabel(type.getTypeLabel());
        }
    }

    /** {@inheritDoc} */
    @Override
    public ResourceType getType() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return _asset.getType();
    }

    public ResourceTypeLabel getTypeLabel() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return _asset.getWlpInformation().getTypeLabel();
    }

    /** {@inheritDoc} */
    @Override
    public void setDescription(String desc) {
        _asset.setDescription(desc);
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return _asset.getDescription();
    }

    /** {@inheritDoc} */
    @Override
    public State getState() {
        return _asset.getState();
    }

    public void setState(State state) {
        _asset.setState(state);
    }

    /** {@inheritDoc} */
    @Override
    public void setVersion(String version) {
        _asset.setVersion(version);
    }

    /** {@inheritDoc} */
    @Override
    public String getVersion() {
        return _asset.getVersion();
    }

    private void setMainAttachmentSize(long size) {
        _asset.getWlpInformation().setMainAttachmentSize(size);
    }

    /** {@inheritDoc} */
    @Override
    public long getMainAttachmentSize() {
        return _asset.getWlpInformation().getMainAttachmentSize();
    }

    /** {@inheritDoc} */
    @Override
    public String getMainAttachmentSHA256() {
        return _asset.getWlpInformation().getMainAttachmentSHA256();
    }

    private void setMainAttachmentSHA256(String mainAttachmentSHA256) {
        _asset.getWlpInformation().setMainAttachmentSHA256(mainAttachmentSHA256);
    }

    /** {@inheritDoc} */
    @Override
    public void setDownloadPolicy(DownloadPolicy policy) {
        _asset.getWlpInformation().setDownloadPolicy(policy);
    }

    /** {@inheritDoc} */
    @Override
    public void setVanityURL(String vanityUrl) {
        _asset.getWlpInformation().setVanityRelativeURL(vanityUrl);
    }

    /** {@inheritDoc} */
    @Override
    public DownloadPolicy getDownloadPolicy() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return _asset.getWlpInformation().getDownloadPolicy();
    }

    /** {@inheritDoc} */
    @Override
    public String getVanityURL() {
        return _asset.getWlpInformation().getVanityRelativeURL();
    }

    /** {@inheritDoc} */
    @Override
    public void setFeaturedWeight(String featuredWeight) {
        _asset.getWlpInformation().setFeaturedWeight(featuredWeight);
    }

    /** {@inheritDoc} */
    @Override
    public String getFeaturedWeight() {
        return _asset.getWlpInformation().getFeaturedWeight();
    }

    /** {@inheritDoc} */
    @Override
    public String getAppliesToVersions(Collection<ProductDefinition> definitions) {
        String appliesTo = _asset.getWlpInformation().getAppliesTo();
        List<AppliesToEntry> entries = AppliesToProcessor.parseAppliesToEntries(appliesTo);
        // Check each entry in the applies to
        for (AppliesToEntry entry : entries) {
            // Now check each supplied product definition
            for (ProductDefinition def : definitions) {
                if (!!!entry.getProductId().equals(def.getId())) {
                    continue;
                } else {
                    if (entry.getEditions() != null && !!!entry.getEditions().contains(def.getEdition())) {
                        continue;
                    }
                    if (entry.getInstallType() != null && !!!entry.getInstallType().equals(def.getInstallType())) {
                        continue;
                    }
                    return entry.getVersion();
                }
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getAppliesToMinimumVersions() {
        Collection<String> versions = new HashSet<String>();
        try {
            List<AppliesToFilterInfo> entries = generateAppliesToFilterInfoList(false);
            if (entries != null) {
                for (AppliesToFilterInfo appliesToFilterInfo : entries) {
                    FilterVersion minVersion = appliesToFilterInfo.getMinVersion();
                    if (minVersion != null) {
                        versions.add(minVersion.toString());
                    }
                }
            }
        } catch (RepositoryResourceCreationException e) {
            // Impossible as we don't validate the applies to
        }
        return versions;
    }

    @Override
    public Calendar getLastUpdatedOn() {
        return _asset.getLastUpdatedOn();
    }

    /**
     * ------------------------------------------------------------------------------------------------
     * Attachment Methods
     * ------------------------------------------------------------------------------------------------
     */

    /** {@inheritDoc} */
    @Override
    public AttachmentResourceWritable addContent(File file) throws RepositoryException {
        return addContent(file, file.getName());
    }

    /** {@inheritDoc} */
    @Override
    public AttachmentResourceWritable addContent(File file, String name) throws RepositoryException {
        if (_contentAttached) {
            throw new RepositoryResourceValidationException("addContent(" + file.getAbsolutePath()
                                                            + ") called for resource " + getName() + " which all ready has a CONTENT attachment", getId());
        }
        _contentAttached = true;
        AttachmentResourceWritable at = addAttachment(file, AttachmentType.CONTENT, name);
        setMainAttachmentSize(at.getSize());
        try {
            setMainAttachmentSHA256((file == null) ? null : HashUtils.getFileSHA256String(file));
        } catch (IOException ioe) {
            throw new RepositoryException(ioe);
        }
        return at;
    }

    /** {@inheritDoc} */
    @Override
    public AttachmentResourceWritable addContent(File file, String name, String url, AttachmentLinkType linkType) throws RepositoryException {
        if (_contentAttached) {
            throw new RepositoryResourceValidationException("addContent(" + file.getAbsolutePath()
                                                            + ") called for resource " + getName() + " which all ready has a CONTENT attachment", getId());
        }
        _contentAttached = true;
        AttachmentResourceWritable at = addAttachment(file, AttachmentType.CONTENT, name, url, linkType);
        setMainAttachmentSize(at.getSize());
        try {
            setMainAttachmentSHA256((file == null) ? null : HashUtils.getFileSHA256String(file));
        } catch (IOException ioe) {
            throw new RepositoryException(ioe);
        }
        return at;
    }

    /** {@inheritDoc} */
    @Override
    public AttachmentResourceWritable addAttachment(File file, AttachmentType type) throws RepositoryException {
        return addAttachment(file, type, file.getName());
    }

    /** {@inheritDoc} */
    @Override
    public synchronized AttachmentResourceWritable addAttachment(File file, AttachmentType type, String name) throws RepositoryException {
        AttachmentResourceImpl at = new AttachmentResourceImpl(file, name);
        at.setType(type);
        // Massive can set this, but we need to know it before then, as we may want to compare this attachment
        // size with the size of an attachment already uploaded.
        at.setFileProps();
        _attachments.put(name, at);
        return at;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized AttachmentResourceWritable addAttachment(File file, AttachmentType type, String name, String url, AttachmentLinkType linkType) throws RepositoryException {
        AttachmentResourceImpl at = new AttachmentResourceImpl(file, name, url, linkType);
        at.setType(type);
        // Since we are storing the attachment outside of massive we have to work out the size ourselves
        at.setFileProps();
        _attachments.put(name, at);
        return at;
    }

    /**
     * This method checks the supplied attachment to see if its the "main" one. The default behaviour is to
     * check if the attachment is a registered as type=content, if it is then it assumes it is the "main" attachment.
     *
     * @param at The attachment to check
     * @return True if this is considered the "main" attachment for the asset
     */
    boolean isMainAttachment(AttachmentResource at) {
        return (AttachmentType.CONTENT == at.getType());
    }

    /** {@inheritDoc} */
    @Override
    public AttachmentResource getMainAttachment() throws RepositoryBackendException, RepositoryResourceException {
        Collection<AttachmentResource> attachments = getAttachments();

        for (AttachmentResource at : attachments) {
            if (isMainAttachment(at)) {
                return at;
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public AttachmentResource getAttachment(String attachmentName) throws RepositoryBackendException, RepositoryResourceException {
        for (AttachmentResource at : getAttachments()) {
            if (at.getName().equals(attachmentName)) {
                return at;
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Collection<AttachmentResource> getAttachments() throws RepositoryBackendException, RepositoryResourceException {
        return Collections.<AttachmentResource> unmodifiableCollection(getAttachmentImpls());
    }

    public synchronized Collection<AttachmentResourceImpl> getAttachmentImpls() throws RepositoryBackendException, RepositoryResourceException {
        if (_attachments == null || _attachments.isEmpty()) {
            // Read the resource back from massive, we don't call refresh from massive as it will
            // read all the resource info back and we just want the attachments
            // Might just be an asset that hasn't been uploaded yet and has no attachments
            if (getId() != null) {
                RepositoryResourceImpl mr = (RepositoryResourceImpl) getAndCheckRepositoryConnection().getResource(getId());
                readAttachmentsFromAsset(mr._asset);

                // Update our backing asset with the attachments read in
                _asset.setAttachments(mr._asset.getAttachments());
            } else {
                if (_attachments == null) {
                    _attachments = new HashMap<String, AttachmentResourceImpl>();
                }
            }
        }
        return _attachments.values();
    }

    /**
     * Gets the number of attachments associated with this resource
     *
     * @return The number of attachments associated with this resource
     * @throws RepositoryBackendException
     */
    public int getAttachmentCount() throws RepositoryBackendException, RepositoryResourceException {
        Collection<AttachmentResource> attachments = getAttachments();
        return attachments.size();
    }

    /*
     * ------------------------------------------------------------------------------------------------
     * LICENSE METHODS
     * ------------------------------------------------------------------------------------------------
     */

    /** {@inheritDoc} */
    @Override
    public void addLicense(File license, Locale loc) throws RepositoryException {
        AttachmentResourceImpl res = (AttachmentResourceImpl) addAttachment(license, AttachmentType.LICENSE, license.getName());
        res.setLocale(loc);
    }

    /** {@inheritDoc} */
    @Override
    public void addLicenseAgreement(File license, Locale loc) throws RepositoryException {
        AttachmentResourceImpl res = (AttachmentResourceImpl) addAttachment(license, AttachmentType.LICENSE_AGREEMENT, license.getName());
        res.setLocale(loc);
    }

    /** {@inheritDoc} */
    @Override
    public void addLicenseInformation(File license, Locale loc) throws RepositoryException {
        AttachmentResourceImpl res = (AttachmentResourceImpl) addAttachment(license, AttachmentType.LICENSE_INFORMATION, license.getName());
        res.setLocale(loc);
    }

    /** {@inheritDoc} */
    @Override
    public AttachmentResource getLicense(Locale loc) throws RepositoryBackendException, RepositoryResourceException {
        AttachmentSummary s = matchByLocale(getAttachmentImpls(), AttachmentType.LICENSE, loc);
        AttachmentResource result = null;
        if (s instanceof AttachmentResource)
            result = (AttachmentResource) s;
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public AttachmentResource getLicenseAgreement(Locale loc) throws RepositoryBackendException, RepositoryResourceException {
        // Get the attachment resource which has Type.LICENSE and Locale loc
        AttachmentSummary s = matchByLocale(getAttachmentImpls(), AttachmentType.LICENSE_AGREEMENT, loc);
        AttachmentResource result = null;
        if (s instanceof AttachmentResource)
            result = (AttachmentResource) s;
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public AttachmentResource getLicenseInformation(Locale loc) throws RepositoryBackendException, RepositoryResourceException {
        // Get the attachment resource which has Type.LICENSE and Locale loc
        AttachmentSummary s = matchByLocale(getAttachmentImpls(), AttachmentType.LICENSE_INFORMATION, loc);
        AttachmentResource result = null;
        if (s instanceof AttachmentResource)
            result = (AttachmentResource) s;
        return result;
    }

    /*
     * Find an AttachmentResource in a given set which either has the desired Locale
     * or failing that, the same language as the desired locale.
     */
    public static AttachmentSummary matchByLocale(Collection<? extends AttachmentSummary> attachments,
                                                  AttachmentType desiredType, Locale desiredLocale) {
        Collection<AttachmentSummary> possibleMatches = new ArrayList<AttachmentSummary>();
        for (AttachmentSummary s : attachments) {
            AttachmentType attType = s.getAttachment().getType();

            if (attType.equals(desiredType)) {
                // This is either an exact match, or a candidate for a match-by-language
                if (s.getLocale().equals(desiredLocale)) {
                    return s;
                } else {
                    possibleMatches.add(s);
                }
            }
        }

        Locale baseDesiredLocale = new Locale(desiredLocale.getLanguage());
        for (AttachmentSummary s : possibleMatches) {
            if (s.getLocale().equals(baseDesiredLocale)) {
                return s;
            }
        }

        // If we've found no better match, can we at least provide English?
        for (AttachmentSummary s : possibleMatches) {
            if (s.getLocale().equals(Locale.ENGLISH)) {
                return s;
            }
        }

        // Getting desperate: do we have anything that's language is English?
        for (AttachmentSummary s : possibleMatches) {
            if (new Locale(s.getLocale().getLanguage()).equals(Locale.ENGLISH)) {
                return s;
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setLicenseType(LicenseType lt) {
        _asset.setLicenseType(lt);
    }

    /** {@inheritDoc} */
    @Override
    public LicenseType getLicenseType() {
        return _asset.getLicenseType();
    }

    /** {@inheritDoc} */
    @Override
    public void setLicenseId(String lic) {
        _asset.setLicenseId(lic);
    }

    /** {@inheritDoc} */
    @Override
    public String getLicenseId() {
        return _asset.getLicenseId();
    }

    /** {@inheritDoc} */
    @Override
    public void setShortDescription(String shortDescription) {
        _asset.setShortDescription(shortDescription);
    }

    /** {@inheritDoc} */
    @Override
    public String getShortDescription() {
        return _asset.getShortDescription();
    }

    /** {@inheritDoc} */
    @Override
    public void setWlpInformationVersion(String wlpInformationVersion) {
        _asset.getWlpInformation().setWlpInformationVersion(wlpInformationVersion);
    }

    /** {@inheritDoc} */
    @Override
    public String getWlpInformationVersion() {
        return _asset.getWlpInformation().getWlpInformationVersion();
    }

    /** {@inheritDoc} */
    @Override
    public void setDisplayPolicy(DisplayPolicy policy) {
        _asset.getWlpInformation().setDisplayPolicy(policy);
    }

    /** {@inheritDoc} */
    @Override
    public DisplayPolicy getDisplayPolicy() {
        if (_asset.getWlpInformation() == null) {
            return null;
        }
        return _asset.getWlpInformation().getDisplayPolicy();
    }

    @Override
    public String getMavenCoordinates() {
        return _asset.getWlpInformation().getMavenCoordinates();
    }

    @Override
    public void setMavenCoordinates(String mavenCoordinates) {
        _asset.getWlpInformation().setMavenCoordinates(mavenCoordinates);
    }

    /*
     * ------------------------------------------------------------------------------------------------
     * HELPER METHODS
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * Decide whether an attachment needs updating.
     *
     * @return boolean - whether the attachment needs updating
     */
    public UpdateType updateRequired(RepositoryResourceImpl matching) {

        if (null == matching) {
            // No matching asset found
            return UpdateType.ADD;
        }

        if (equivalentWithoutAttachments(matching)) {
            return UpdateType.NOTHING;
        } else {
            // As we are doing an update set our id to be the one we found in massive
            // Not needed now, we are merging both assets
            _asset.set_id(matching.getId());
            return UpdateType.UPDATE;
        }
    }

    /**
     * Creates an object which can be used to compare with another resource's to determine if
     * they represent the same asset.
     *
     * @return
     */
    public RepositoryResourceMatchingData createMatchingData() {
        RepositoryResourceMatchingData matchingData = new RepositoryResourceMatchingData();
        matchingData.setName(getName());
        matchingData.setProviderName(getProviderName());
        matchingData.setType(getType());
        return matchingData;
    }

    protected List<RepositoryResourceImpl> performMatching() throws BadVersionException, RequestFailureException, RepositoryBadDataException, RepositoryBackendException, RepositoryResourceNoConnectionException {
        List<RepositoryResourceImpl> matching = new ArrayList<RepositoryResourceImpl>();

        @SuppressWarnings("unchecked")
        Collection<RepositoryResourceImpl> resources = (Collection<RepositoryResourceImpl>) getPotentiallyMatchingResources();

        RepositoryResourceImpl resource = null;
        for (RepositoryResourceImpl res : resources) {
            if (createMatchingData().equals(res.createMatchingData())) {
                // found an asset on massive - get the full asset.
                resource = (RepositoryResourceImpl) getAndCheckRepositoryConnection().getResource(res.getId());
                matching.add(resource);
            }
        }

        return matching;
    }

    /**
     * Returns a superset of resources to those returned by {@link #performMatching()}
     *
     * This allows us to do some type-specific pre-filtering of the resources on the server.
     *
     * @throws RepositoryResourceNoConnectionException If no connection has been specified for the resource
     * @throws RepositoryBackendException If there was a problem talking to the backend
     */
    protected Collection<? extends RepositoryResource> getPotentiallyMatchingResources() throws RepositoryBackendException, RepositoryResourceNoConnectionException {
        return getAndCheckRepositoryConnection().getAllResourcesWithDupes(getType());
    }

    /**
     * Perform the matching logic for {@link #getResources(Collection, Collection, Visibility, RepositoryConnectionList)} and
     * {@link #findResources(String, Collection, Collection, Visibility, RepositoryConnectionList)} so that this method will return <code>true</code> if the resource has the
     * <code>visibility</code> supplied and matches one of the products in <code>productDefinitions</code>.
     *
     * @param resource The resource to test
     * @param productDefinitions The product definitions to match to, maybe <code>null</code> which means that no product matching will take place.
     * @param visibility The visibility to match to, maybe <code>null</code> which means that no visibility matching will take place.
     * @return
     */
    public boolean doesResourceMatch(Collection<ProductDefinition> productDefinitions, Visibility visibility) {
        ResourceType type = getType();
        if (ResourceType.FEATURE == type && visibility != null) {
            EsaResourceImpl esa = (EsaResourceImpl) this;
            Visibility visibilityMatches = esa.getVisibility() == null ? Visibility.PUBLIC : esa.getVisibility();
            if (!visibilityMatches.equals(visibility)) {
                // Visibility is different, no match
                return false;
            }
        }

        // If there is no product definitions defined then say it matches - not being filtered to a specific version
        boolean matches = productDefinitions == null || productDefinitions.isEmpty();
        if (productDefinitions != null) {
            for (ProductDefinition productDefinition : productDefinitions) {
                if (matches(productDefinition) == MatchResult.MATCHED) {
                    matches = true;
                    break;
                }
            }
        }
        return matches;
    }

    /**
     * This method tries to find out if there is a match for "this" resource already in massive.
     *
     * @return A list of resources that were found in massive which has the same name, provider and type as this
     *         resource, an empty list otherwise.
     * @throws RepositoryResourceValidationException If the resource fails a validation check
     * @throws RepositoryBackendException If there was a problem talking to the backend
     * @throws RepositoryResourceNoConnectionException If no connection has been specified
     */
    public List<RepositoryResourceImpl> findMatchingResource() throws RepositoryResourceValidationException, RepositoryBackendException, RepositoryBadDataException, RepositoryResourceNoConnectionException {
        List<RepositoryResourceImpl> matchingRes;
        try {
            matchingRes = performMatching();
            if (matchingRes != null && matchingRes.size() > 1) {
                StringBuilder warningMessage = new StringBuilder("More than one match found for " + getName() + ":");
                for (RepositoryResourceImpl massiveResource : matchingRes) {
                    warningMessage.append("\n\t" + massiveResource.getName() + " (" + massiveResource.getId() + ")");
                }
                logger.warning(warningMessage.toString());
            }
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("BadDataException accessing asset", getId(), bvx);
        } catch (RequestFailureException bfe) {
            throw new RepositoryBackendRequestFailureException(bfe, getRepositoryConnection());
        }
        return matchingRes;
    }

    /**
     * Resources should override this method to copy fields that should be used as part of an
     * update
     *
     * @param fromResource
     */
    protected void copyFieldsFrom(RepositoryResourceImpl fromResource, boolean includeAttachmentInfo) {
        setName(fromResource.getName()); // part of the identification so locked
        setDescription(fromResource.getDescription());
        setShortDescription(fromResource.getShortDescription());
        setProviderName(fromResource.getProviderName()); // part of the identification so locked
        setProviderUrl(fromResource.getProviderUrl());
        setVersion(fromResource.getVersion());
        setDownloadPolicy(fromResource.getDownloadPolicy());
        setLicenseId(fromResource.getLicenseId());
        setLicenseType(fromResource.getLicenseType());
        setMainAttachmentSize(fromResource.getMainAttachmentSize());
        setMainAttachmentSHA256(fromResource.getMainAttachmentSHA256());
        setFeaturedWeight(fromResource.getFeaturedWeight());
        setDisplayPolicy(fromResource.getDisplayPolicy());
        setVanityURL(fromResource.getVanityURL());
        setWlpInformationVersion(fromResource.getWlpInformationVersion());
        setMavenCoordinates(fromResource.getMavenCoordinates());

        if (includeAttachmentInfo) {
            setMainAttachmentSize(fromResource.getMainAttachmentSize());
        }
        _asset.getWlpInformation().setAppliesToFilterInfo(fromResource.getAsset().getWlpInformation().getAppliesToFilterInfo());
    }

    /**
     * This method copies the fields from "this" that we care about to the "fromResource". Then we
     * set our asset to point to the one in "fromResource". In effect this means we get all the details
     * from the "fromResource" and override fields we care about and store the merged result in our asset.
     *
     * This is used when we have read an asset back from massive, the asset read back from massive will have more
     * fields that in it (that massive has set) so this method is used to copy the asset containing those extra
     * fields from the "from" resource to the our resource. Note that the from resource is modified during this
     * process, so do not rely on it's contents not changing. This is only used to copy the contents from a matching
     * resource into a new resource and then the matching resource object is discarded so this is currently safe.
     * TODO: Find a better way of doing this
     *
     * @param fromResource
     * @throws RepositoryResourceValidationException
     */
    public void overWriteAssetData(RepositoryResourceImpl fromResource, boolean includeAttachmentInfo) throws RepositoryResourceValidationException {

        // Make sure we are dealing with the same type....this
        // should never happen
        if (!fromResource.getClass().getName().equals(getClass().getName())) {
            throw new RepositoryResourceValidationException("Expected class of type " + getClass().getName()
                                                            + " but was " + fromResource.getClass().getName(), this.getId());
        }
        // copy the stuff into target
        fromResource.copyFieldsFrom(this, includeAttachmentInfo);

        // Now use target
        _asset = fromResource._asset;
    }

    public void addAsset() throws RepositoryResourceCreationException, RepositoryBadDataException, RepositoryBackendIOException, RepositoryBackendRequestFailureException {
        // ensure the resource does not have an id - if we read a resource back from massive, change it, then re-upload it
        // then we need to remove the id as massive won't allow us to push an asset into massive with an id
        resetId();
        try {
            _asset = getWritableClient().addAsset(_asset);
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to add asset " + getId(), ioe, getRepositoryConnection());
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("Bad version when adding asset", getId(), bvx);
        } catch (RequestFailureException rfe) {
            if (rfe.getResponseCode() == SERVER_ERROR) {
                // Treat this is a backend exception as the caller may want to retry
                throw new RepositoryBackendRequestFailureException(rfe, getRepositoryConnection());
            } else {
                throw new RepositoryResourceCreationException("Failed to add the asset", getId(), rfe);
            }
        } catch (SecurityException se) {
            throw new RepositoryResourceCreationException("Failed to add the asset", getId(), se);
        } catch (RepositoryOperationNotSupportedException rbnse) {
            throw new RepositoryResourceCreationException("Failed to add the asset", getId(), rbnse);
        } catch (ClientFailureException cfe) {
            throw new RepositoryResourceCreationException("Failed to add the asset", getId(), cfe);
        }
    }

    public void updateAsset() throws RepositoryResourceUpdateException, RepositoryResourceValidationException, RepositoryBadDataException, RepositoryBackendIOException, RepositoryBackendRequestFailureException {
        try {
            _asset = getWritableClient().updateAsset(_asset);
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to update asset " + getId(), ioe, getRepositoryConnection());
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("Bad version when updating asset", getId(), bvx);
        } catch (RequestFailureException e) {
            if (e.getResponseCode() == SERVER_ERROR) {
                // Rethrow as a backend exception as the caller may want to retry
                throw new RepositoryBackendRequestFailureException(e, getRepositoryConnection());
            } else {
                throw new RepositoryResourceUpdateException("Failed to update the attachment", getId(), e);
            }
        } catch (SecurityException se) {
            throw new RepositoryResourceUpdateException("Failed to update the asset", getId(), se);
        } catch (RepositoryOperationNotSupportedException rbnse) {
            throw new RepositoryResourceUpdateException("Failed to add the asset", getId(), rbnse);
        } catch (ClientFailureException cfe) {
            throw new RepositoryResourceUpdateException("Failed to add the asset", getId(), cfe);
        }
    }

    /**
     * Sets our asset object to point to the asset inside the "from" resource
     *
     * @param from
     */
    public void copyAsset(RepositoryResourceImpl from) {
        _asset = from._asset;
    }

    public void addAttachment(AttachmentResourceImpl at) throws RepositoryResourceCreationException, RepositoryBadDataException, RepositoryResourceUpdateException, RepositoryBackendIOException, RepositoryBackendRequestFailureException {
        // ensure the attachment does not have an id - if we read a resource back from massive, change it, then re-upload it
        // then we need to remove the id as massive won't allow us to push an asset into massive with an id
        at.resetId();
        try {
            getWritableClient().addAttachment(getId(), at);
        } catch (IOException e) {
            throw new RepositoryBackendIOException("Failed to add the attachment" + getId(), e, getRepositoryConnection());
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("Bad version when adding attachment", getId(), bvx);
        } catch (RequestFailureException rfe) {
            if (rfe.getResponseCode() == SERVER_ERROR) {
                // Re-throw as a backend exception as the caller may want to retry
                throw new RepositoryBackendRequestFailureException(rfe, getRepositoryConnection());
            } else {
                throw new RepositoryResourceCreationException("Failed to add the attachment", getId(), rfe);
            }
        } catch (SecurityException se) {
            throw new RepositoryResourceUpdateException("Failed to add the attachment", getId(), se);
        } catch (RepositoryOperationNotSupportedException rbnse) {
            throw new RepositoryResourceUpdateException("Failed to add the attachment", getId(), rbnse);
        }
    }

    public void updateAttachment(AttachmentResourceImpl at) throws RepositoryResourceUpdateException, RepositoryBadDataException, RepositoryBackendIOException, RepositoryBackendRequestFailureException {
        try {
            getWritableClient().updateAttachment(getId(), at);
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to update the attachment " + this.getId(), ioe, getRepositoryConnection());
        } catch (BadVersionException bvx) {
            throw new RepositoryBadDataException("Bad version when updating attachment", this.getId(), bvx);
        } catch (RequestFailureException e) {
            if (e.getResponseCode() == SERVER_ERROR) {
                // Re-throw as a backend exception as the caller may want to retry
                throw new RepositoryBackendRequestFailureException(e, getRepositoryConnection());
            } else {
                throw new RepositoryResourceUpdateException("Failed to update the attachment", this.getId(), e);
            }
        } catch (SecurityException se) {
            throw new RepositoryResourceUpdateException("Failed to update the attachment", this.getId(), se);
        } catch (RepositoryOperationNotSupportedException rbnse) {
            throw new RepositoryResourceUpdateException("Failed to add the attachment", this.getId(), rbnse);
        }

    }

    /** {@inheritDoc} */
    @Override
    public synchronized void uploadToMassive(UploadStrategy strategy) throws RepositoryBackendException, RepositoryResourceException {
        updateGeneratedFields(strategy.performEditionChecking());
        // If the asset has already been uploaded to massive (i.e. it has an id) then read and copy any
        // attachment stored in massive
        if (getId() != null) {
            copyAttachments();
        }
        List<RepositoryResourceImpl> matching = strategy.findMatchingResources(this);
        strategy.uploadAsset(this, matching);
    }

    /**
     * Does a deep copy of attachments that are stored in the massive backend. Downloads the files to a temp
     * dir (and deletes them on JVM exit), sets the URL to null and associates the downloaded file with the
     * resource. This will cause massive to store the attachment and create a new URL for it.
     *
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    //@SuppressWarnings("rawtypes")
    void copyAttachments() throws RepositoryBackendException, RepositoryResourceException {
        Collection<AttachmentResourceImpl> attachments = getAttachmentImpls();
        for (AttachmentResourceImpl at : attachments) {
            AttachmentLinkType linkType = at.getLinkType();
            if ((null == linkType) && (at.getURL() != null)) {
                final File tempFile = new File(getId() + "_" + at.getName());

                try {
                    // Why do we have to specify a return type for the run method and paramatize
                    // PrivilegedExceptionAction to it, this method should have a void return type ideally.
                    AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                        @Override
                        public Object run() {
                            tempFile.deleteOnExit();
                            return null;
                        }
                    });
                } catch (PrivilegedActionException e) {
                    throw new RepositoryResourceValidationException("Unable to copy attachments", at.getId(), e.getCause());
                }

                try {
                    try {
                        at.downloadToFile(tempFile);
                    } catch (IOException e) {
                        // try once more, if we fail then give up on this asset
                        at.downloadToFile(tempFile);
                    }
                    at.setURL(null);
                    at.setFile(tempFile);
                } catch (IOException e) {
                    // tried twice, give up :(
                    throw new RepositoryBackendIOException("Exception caught while obtaining attachments for resource " + getName(), e, getRepositoryConnection());
                }
            }

            // Reset ID after pulling down (if needed) the attachment
            at.resetId();
        }
    }

    private void performLifeCycle(StateAction action) throws RepositoryBackendException, RepositoryResourceException {
        State s = getState();
        if (s.isStateActionAllowed(action)) {
            try {
                getWritableClient().updateState(_asset.get_id(), action);
            } catch (IOException ioe) {
                throw new RepositoryBackendIOException("IOException on " + action, ioe, null);
            } catch (RequestFailureException cause) {
                throw new RepositoryBackendRequestFailureException(cause, null);
            }
        } else {
            throw new RepositoryResourceLifecycleException(action + " not supported for assets in " + s + " state", _asset.get_id(), s, action);
        }
        refreshFromMassive();
    }

    public void publish() throws RepositoryBackendException, RepositoryResourceException {
        performLifeCycle(StateAction.PUBLISH);
    }

    public void approve() throws RepositoryBackendException, RepositoryResourceException {
        performLifeCycle(StateAction.APPROVE);
    }

    public void cancel() throws RepositoryBackendException, RepositoryResourceException {
        performLifeCycle(StateAction.CANCEL);
    }

    public void need_more_info() throws RepositoryBackendException, RepositoryResourceException {
        performLifeCycle(StateAction.NEED_MORE_INFO);
    }

    public void unpublish() throws RepositoryBackendException, RepositoryResourceException {
        performLifeCycle(StateAction.UNPUBLISH);
    }

    /**
     * Moves the resource to the desired state
     *
     * @param resource
     * @param state
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    public void moveToState(State state) throws RepositoryBackendException, RepositoryResourceException {
        if (getState() == null) {
            return;
        }
        int counter = 0;
        while (getState() != state) {
            counter++;
            StateAction nextAction = getState().getNextAction(state);
            performLifeCycle(nextAction);
            if (counter >= 10) {
                throw new RepositoryResourceLifecycleException("Unable to move to state " + state +
                                                               " after 10 state transistion attempts. Resource left in state " + getState(), getId(), getState(), nextAction);
            }
        }
    }

    @Override
    public void delete() throws RepositoryResourceDeletionException, RepositoryBackendIOException, RepositoryBackendRequestFailureException {
        try {
            getWritableClient().deleteAssetAndAttachments(_asset.get_id());
        } catch (IOException ioe) {
            throw new RepositoryBackendIOException("Failed to delete resource " + this.getId(), ioe, this.getRepositoryConnection());
        } catch (RequestFailureException e) {
            if (e.getResponseCode() == SERVER_ERROR) {
                // Re-throw as a backend exception as the caller may want to retry
                throw new RepositoryBackendRequestFailureException(e, getRepositoryConnection());
            } else {
                throw new RepositoryResourceDeletionException("Failed to delete resource", this.getId(), e);
            }
        } catch (RepositoryOperationNotSupportedException e) {
            throw new RepositoryResourceDeletionException("Failed to delete resource", this.getId(), e);
        }
    }

    @Override
    public String getAssetURL() {
        String url = null;
        if (getRepositoryConnection() instanceof RestRepositoryConnection) {
            RestRepositoryConnection lie = (RestRepositoryConnection) getRepositoryConnection();
            url = lie.getAssetURL(getId());
        }
        return url;
    }

    /** {@inheritDoc} */
    @Override
    public void updateGeneratedFields(boolean performEditionChecking) throws RepositoryResourceCreationException {
        //update the asset filter info.
        updateAssetFilterInfo(performEditionChecking);
        if ((_asset != null) && (_asset.getWlpInformation()) != null &&
            (_asset.getWlpInformation().getVanityRelativeURL()) == null) {
            createVanityURL();
        }
    }

    /**
     * Returns empty string by defualt (no version string used)
     *
     * @return
     */
    protected String getVersionForVanityUrl() {
        return "";
    }

    protected String getNameForVanityUrl() {
        return getName();
    }

    protected void createVanityURL() {
        List<Character> allowable = Arrays.asList(
                                                  'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
                                                  'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                                                  '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
                                                  '.', '-', '_');
        ResourceType type = getType();
        if (type == null) {
            return;
        }
        StringBuffer sb = new StringBuffer(type.getURLForType());
        String version = getVersionForVanityUrl();
        if (version != null && !version.isEmpty()) {
            sb.append("-");
            sb.append(version);
        }
        sb.append("-");
        sb.append(getNameForVanityUrl());

        // Now filter out any non white listed character and replace spaces with underscores
        StringBuffer filtered = new StringBuffer();
        for (Character c : sb.toString().toCharArray()) {
            if (allowable.contains(c)) {
                filtered.append(c);
            } else if (c.equals(' ')) {
                filtered.append('_');
            }
        }
        _asset.getWlpInformation().setVanityRelativeURL(filtered.toString());
    }

    protected List<AppliesToFilterInfo> generateAppliesToFilterInfoList(boolean validateEditions) throws RepositoryResourceCreationException {
        WlpInformation wlp = _asset.getWlpInformation();
        List<AppliesToFilterInfo> atfi = null;
        if (wlp != null) {
            String appliesTo = wlp.getAppliesTo();
            if (appliesTo != null) {
                atfi = AppliesToProcessor.parseAppliesToHeader(appliesTo);
                if (atfi != null && validateEditions) {
                    for (AppliesToFilterInfo info : atfi) {
                        AppliesToProcessor.validateEditions(info, appliesTo);
                    }
                }
            }
        }
        return atfi;
    }

    private void updateAssetFilterInfo(boolean performEditionChecking) throws RepositoryResourceCreationException {
        WlpInformation wlp = _asset.getWlpInformation();
        List<AppliesToFilterInfo> atfi = generateAppliesToFilterInfoList(performEditionChecking);
        if (wlp != null && atfi != null) {
            wlp.setAppliesToFilterInfo(atfi);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void dump(OutputStream os) {
        _asset.dump(os);
    }

    @Override
    public void writeDiskRepoJSONToStream(final OutputStream writeJsonTo) throws RepositoryResourceException {
        try {
            _asset.dumpMinimalAsset(writeJsonTo);
        } catch (IllegalArgumentException e) {
            throw new RepositoryResourceCreationException("Failed to write the resource to disk", getId(), e);
        } catch (IllegalAccessException e) {
            throw new RepositoryResourceCreationException("Failed to write the resource to disk", getId(), e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_asset == null) ? 0 : _asset.hashCode());
        return result;
    }

    /**
     * Checks if the two resources are equivalent by checking if the assets
     * are equivalent.
     *
     * @param obj
     * @return
     */
    public boolean equivalent(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RepositoryResourceImpl other = (RepositoryResourceImpl) obj;
        if (_asset == null) {
            if (other._asset != null)
                return false;
        } else if (!_asset.equivalent(other._asset))
            return false;
        return true;
    }

    /**
     * Checks if the two resources are equivalent by checking if the assets
     * are equivalent.
     *
     * @param obj
     * @return
     */
    public boolean equivalentWithoutAttachments(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RepositoryResourceImpl other = (RepositoryResourceImpl) obj;
        if (_asset == null) {
            if (other._asset != null)
                return false;
        } else if (!_asset.equivalentWithoutAttachments(other._asset))
            return false;
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RepositoryResourceImpl other = (RepositoryResourceImpl) obj;
        if (_asset == null) {
            if (other._asset != null)
                return false;
        } else if (!_asset.equals(other._asset))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "<MassiveResource@" + this.hashCode() + " <Asset=" + _asset + ">>";
    }

    /*
     * ------------------------------------------------------------------------------------------------
     * PRIVATE INTERNAL METHODS
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * Get the CRC of a file from an InputStream
     *
     * @param is The input stream to obtain the CRC from
     * @return a long representing the CRC value of the data read from the supplied input stream
     * @throws IOException
     */
    private static long getCRC(InputStream is) throws IOException {
        CheckedInputStream check = new CheckedInputStream(is, new CRC32());
        BufferedInputStream in = new BufferedInputStream(check);
        while (in.read() != -1) {
            // Read file in completely
        }
        long crc = check.getChecksum().getValue();
        return crc;
    }

    /*
     * ------------------------------------------------------------------------------------------------
     * ATTACHMENT CLASSES
     * ------------------------------------------------------------------------------------------------
     */

    /**
     * This class provides access to details about a specific attachment
     */
    public class AttachmentResourceImpl implements AttachmentSummary, AttachmentResourceWritable {

        private final Attachment _attachment;
        private File _file = null;

        /**
         * Take a local file and store it in Massive. LinkType = null.
         *
         * @param file
         * @param name
         */
        private AttachmentResourceImpl(File file, String name) {
            _attachment = new Attachment();
            _attachment.setName(name);
            _file = file;
        }

        private AttachmentResourceImpl(Attachment at) {
            _attachment = at;
        }

        private AttachmentResourceImpl(File file, String name, String url, AttachmentLinkType linkType) {
            _attachment = new Attachment();
            _attachment.setName(name);
            _attachment.setUrl(url);
            if (linkType == null) {
                linkType = AttachmentLinkType.DIRECT;
            }
            _attachment.setLinkType(linkType);
            _file = file;
        }

        /** {@inheritDoc} */
        @Override
        public String getName() {
            return _attachment.getName();
        }

        /** {@inheritDoc} */
        @Override
        public Locale getLocale() {
            return _attachment.getLocale();
        }

        @Override
        public File getFile() {
            return _file;
        }

        public void setFile(File file) {
            _file = file;
        }

        /** {@inheritDoc} */
        @Override
        public String getURL() {
            return _attachment.getUrl();
        }

        public void setURL(String url) {
            _attachment.setUrl(url);
        }

        @Override
        public Attachment getAttachment() {
            return _attachment;
        }

        /** {@inheritDoc} */
        @Override
        public long getSize() {
            return _attachment.getSize();
        }

        private void setFileProps() throws RepositoryException {
            setSize(getFileLength());
            setCRC(calculateCRC());
        }

        /**
         * Should only be set by us
         *
         * @param size
         */
        private void setSize(long size) {
            _attachment.setSize(size);
        }

        /** {@inheritDoc} */
        @Override
        public void setImageDimensions(int height, int width) {
            ImageDetails details = _attachment.getWlpInformation().getImageDetails();
            if (details == null) {
                details = new ImageDetails();
                _attachment.getWlpInformation().setImageDetails(details);
            }
            details.setHeight(height);
            details.setWidth(width);
        }

        @Override
        public int getImageHeight() {
            ImageDetails details = _attachment.getWlpInformation().getImageDetails();
            if (details == null) {
                return 0;
            } else {
                return details.getHeight();
            }
        }

        @Override
        public int getImageWidth() {
            ImageDetails details = _attachment.getWlpInformation().getImageDetails();
            if (details == null) {
                return 0;
            } else {
                return details.getWidth();
            }
        }

        /**
         * Gets the id of the attachment resource
         *
         * @return the id of the attachment resource
         */
        @Override
        public String getId() {
            return _attachment.get_id();
        }

        public void resetId() {
            _attachment.set_id(null);
        }

        /** {@inheritDoc} */
        @Override
        public AttachmentType getType() {
            return _attachment.getType();
        }

        /**
         * Set the attachment type
         *
         * @param type The type of attachment
         */
        public void setType(AttachmentType type) {
            _attachment.setType(type);
        }

        /**
         * Gets the date the attachment was uploaded on
         *
         * @return The date the attachment was uploaded on
         */
        public Calendar getUploadOn() {
            return _attachment.getUploadOn();
        }

        /** {@inheritDoc} */
        @Override
        public InputStream getInputStream() throws RepositoryBackendException, RepositoryBadDataException {
            try {
                return _client.getAttachment(_asset, _attachment);
            } catch (IOException e) {
                throw new RepositoryBackendIOException("Failed to get read attachment", e, getRepositoryConnection());
            } catch (BadVersionException e) {
                throw new RepositoryBadDataException("BadVersion reading attachment", getId(), e);
            } catch (RequestFailureException e) {
                throw new RepositoryBackendRequestFailureException(e, getRepositoryConnection());
            }
        }

        public void setCRC(long CRC) {
            _attachment.getWlpInformation().setCRC(CRC);
        }

        @Override
        public long getCRC() {
            return _attachment.getWlpInformation().getCRC();
        }

        public void setLocale(Locale locale) {
            _attachment.setLocale(locale);
        }

        /**
         * Deletes this attachment from the repository
         */
        public void deleteNow() throws RepositoryResourceDeletionException, RepositoryBackendIOException, RepositoryBackendRequestFailureException {
            synchronized (RepositoryResourceImpl.this) {
                try {
                    if (getId() != null) {
                        getWritableClient().deleteAttachment(RepositoryResourceImpl.this.getId(), getId());
                    }
                    _attachments.remove(getName());
                    if (_attachment.getType().equals(AttachmentType.CONTENT)) {
                        _contentAttached = false;
                    }
                } catch (IOException e) {
                    throw new RepositoryBackendIOException("Failed to delete the attachment " + getId() + " in asset "
                                                           + RepositoryResourceImpl.this.getId(), e, RepositoryResourceImpl.this.getRepositoryConnection());
                } catch (RequestFailureException e) {
                    if (e.getResponseCode() == SERVER_ERROR) {
                        // re-throw as backend exception as the caller may want to retry
                        throw new RepositoryBackendRequestFailureException(e, getRepositoryConnection());
                    } else {
                        throw new RepositoryResourceDeletionException("Failed to delete the attachment " + getId() + " in asset "
                                                                      + RepositoryResourceImpl.this.getId(), this.getId(), e);
                    }
                } catch (RepositoryOperationNotSupportedException rbnse) {
                    throw new RepositoryResourceDeletionException("Failed to delete the attachment " + getId() + " in asset "
                                                                  + RepositoryResourceImpl.this.getId(), this.getId(), rbnse);
                }
            }
        }

        /**
         * Does this attachment need updating. Is it different from the one in the
         * remote resource
         *
         * @param remoteResource
         * @return
         * @throws RepositoryBackendException
         * @throws RepositoryResourceException
         */
        public UpdateType updateRequired(RepositoryResourceImpl remoteResource) throws RepositoryBackendException, RepositoryResourceException {
            if (null == remoteResource) {
                // No matching asset found
                return UpdateType.ADD;
            }

            Collection<AttachmentResource> remoteAttachments = remoteResource.getAttachments();
            for (AttachmentResource remoteAt : remoteAttachments) {
                if (getName().equals(remoteAt.getName())) {
                    if (equivalent(remoteAt)) {
                        return UpdateType.NOTHING;
                    } else {
                        return UpdateType.UPDATE;
                    }
                }
            }

            // No attachment found
            return UpdateType.ADD;
        }

        /**
         * Checks if the two resources are equivalent by checking if the attachments
         * are equivalent.
         *
         * @param obj
         * @return
         */
        public boolean equivalent(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AttachmentResourceImpl other = (AttachmentResourceImpl) obj;
            if (_attachment == null) {
                if (other._attachment != null)
                    return false;
            } else if (!_attachment.equivalent(other._attachment))
                return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((_file == null) ? 0 : _file.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AttachmentResourceImpl other = (AttachmentResourceImpl) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (_file == null) {
                if (other._file != null)
                    return false;
            } else if (!_file.equals(other._file))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "<AttachmentResource@" + this.hashCode() + " <Attachment=" + _attachment + ">>";
        }

        /** {@inheritDoc} */
        @Override
        public void dump(OutputStream os) {
            _attachment.dump(os);
        }

        /**
         * Downloads the attachment to the specified file
         *
         * @param fileToWriteTo The file to write the attachment to.
         * @throws RepositoryBackendException
         * @throws RepositoryBadDataException
         */
        public void downloadToFile(final File fileToWriteTo) throws RepositoryBackendException, IOException, RepositoryBadDataException {
            FileOutputStream fos = null;
            InputStream is = getInputStream();
            try {
                try {
                    fos = AccessController.doPrivileged(
                                                        new PrivilegedExceptionAction<FileOutputStream>() {
                                                            @Override
                                                            public FileOutputStream run() throws FileNotFoundException {
                                                                return new FileOutputStream(fileToWriteTo);
                                                            }
                                                        });
                } catch (PrivilegedActionException e) {
                    // Creating a FileInputStream can only return a FileNotFoundException
                    throw (FileNotFoundException) e.getCause();
                }

                byte[] buffer = new byte[1024];
                int read = 0;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            } finally {
                if (null != fos) {
                    fos.close();
                }
                if (null != is) {
                    is.close();
                }
            }
        }

        public AttachmentLinkType getLinkType() {
            return _attachment.getLinkType();
        }

        private RepositoryResourceImpl getOuterType() {
            return RepositoryResourceImpl.this;
        }

        /**
         * Gets the CRC value for the this attachment.
         *
         * @return The CRC of the attachment or -1 if it wasn't found.
         * @throws RepositoryException
         * @throws RepositoryBackendException
         */
        private long calculateCRC() throws RepositoryException {
            if (_file == null) {
                return 0l;
            }
            InputStream is = null;
            try {
                is = AccessController.doPrivileged(
                                                   new PrivilegedExceptionAction<FileInputStream>() {
                                                       @Override
                                                       public FileInputStream run() throws FileNotFoundException {
                                                           return new FileInputStream(_file);
                                                       }
                                                   });
                return RepositoryResourceImpl.getCRC(is);
            } catch (PrivilegedActionException e) {
                // Creating a FileInputStream can only return a FileNotFoundException or NPE
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw new RepositoryException(cause);
                } else {
                    throw (RuntimeException) cause;
                }
            } catch (IOException cause) {
                throw new RepositoryException(cause);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // do nothing;
                    }
                }
            }
        }

        /**
         * Return the length of a file using a doPrivileged
         *
         * @param file
         * @return long - length of file
         * @throws RepositoryException
         */
        private long getFileLength() throws RepositoryException {
            if (_file == null) {
                return 0l;
            }
            Long fileSize;
            try {
                fileSize = AccessController.doPrivileged(new PrivilegedExceptionAction<Long>() {
                    @Override
                    public Long run() {
                        return _file.length();
                    }
                });
            } catch (PrivilegedActionException e) {
                throw new RepositoryException(e.getCause());
            }
            return fileSize.longValue();
        }

        /** {@inheritDoc} */
        @Override
        public RepositoryConnection getRepositoryConnection() {
            return getOuterType().getRepositoryConnection();
        }
    }

}
