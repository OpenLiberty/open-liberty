/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.internal.asset;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.InstallLicense;
import com.ibm.ws.install.internal.ExceptionUtils;
import com.ibm.ws.install.internal.InstallLicenseImpl;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.repository.FeatureAsset;
import com.ibm.ws.install.repository.FeatureCollectionAsset;
import com.ibm.ws.kernel.feature.internal.cmdline.ThirdPartyLicenseProvider;
import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.resources.AttachmentResource;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;

import wlp.lib.extract.LicenseProvider;
import wlp.lib.extract.ZipLicenseProvider;

public class ESAAsset extends ESABaseAsset implements FeatureAsset, FeatureCollectionAsset {

    private EsaResource esaResource = null;

    public ESAAsset(String id, String featureName, String repoType, File assetFile, boolean isTemporary) throws ZipException, IOException {
        super(id, featureName, repoType, assetFile, isTemporary);
    }

    public ESAAsset(File assetFile, String repoType, boolean isTemporary) throws ZipException, IOException {
        super(assetFile, repoType, isTemporary);
    }

    public ESAAsset(String id, String featureName, String repoType, EsaResource esaResource) throws ZipException, IOException {
        super(id, featureName, repoType, null, true);
        this.esaResource = esaResource;
        this.featureName = esaResource.getProvideFeature();
        this.shortName = esaResource.getShortName();
    }

    @Override
    protected ResourceBundle getResourceBundle(Locale locale) {
        try {
            ZipEntry entry = getEntry("OSGI-INF/l10n/subsystem_" + locale.getLanguage() + ".properties");
            if (entry == null) {
                entry = getEntry("OSGI-INF/l10n/subsystem.properties");
            }
            if (entry != null)
                return new PropertyResourceBundle(getInputStream(entry));
        } catch (IOException e) {
            //TODO: log the exception
        }
        return null;
    }

    @Override
    protected LicenseProvider createLicenseProvider(String licenseAgreementPrefix, String licenseInformationPrefix,
                                                    String subsystemLicenseType) {
        if (licenseInformationPrefix == null) {
            LicenseProvider lp = ZipLicenseProvider.createInstance(zip, licenseAgreementPrefix);
            if (lp != null)
                return lp;
        } else {
            wlp.lib.extract.ReturnCode licenseReturnCode = ZipLicenseProvider.buildInstance(zip, licenseAgreementPrefix,
                                                                                            licenseInformationPrefix);
            if (licenseReturnCode == wlp.lib.extract.ReturnCode.OK) {
                // Everything is set up ok so carry on
                return ZipLicenseProvider.getInstance();
            }
        }
        // An error indicates that the IBM licenses could not be found so we can use the subsystem header
        if (subsystemLicenseType != null && subsystemLicenseType.length() > 0) {
            return new ThirdPartyLicenseProvider(featureDefinition.getFeatureName(), subsystemLicenseType);
        }
        return null;
    }

    public ZipFile getZip() { //only for ESAAsset
        return zip;
    }

    public Enumeration<? extends ZipEntry> getZipEntries() { //only for ESAAsset
        return zip.entries();
    }

    @Override
    public long size() {
        if (esaResource == null)
            return (null != getAsset()) ? getAsset().length() : super.size();
        return esaResource.getMainAttachmentSize();
    }

    @Override
    public String getDisplayName(Locale locale) {
        if (esaResource == null)
            return super.getDisplayName(locale);
        return esaResource.getName();
    }

    @Override
    public String getDisplayName() {
        if (esaResource == null)
            return super.getDisplayName();
        return esaResource.getName();
    }

    @Override
    public String getDescription(Locale locale) {
        if (esaResource == null)
            return super.getDescription(locale);
        return esaResource.getShortDescription();
    }

    @Override
    public String getDescription() {
        if (esaResource == null)
            return super.getDescription();
        return esaResource.getShortDescription();
    }

    @Override
    public InstallLicense getLicense(Locale locale) {
        if (esaResource == null)
            return super.getLicense(locale);

        if (licenseMap == null) {
            licenseMap = new HashMap<Locale, InstallLicense>();
        }
        if (licenseMap.containsKey(locale))
            return licenseMap.get(locale);

        try {
            AttachmentResource la = esaResource.getLicenseAgreement(locale);
            AttachmentResource li = esaResource.getLicenseInformation(locale);
            AttachmentResource enLi = locale.getLanguage().equalsIgnoreCase("en") ? null : esaResource.getLicenseInformation(Locale.ENGLISH);
            String licenseId = esaResource.getLicenseId();
            if (licenseId != null && !licenseId.isEmpty()) {
                InstallLicenseImpl ili = new InstallLicenseImpl(licenseId, esaResource.getLicenseType(), la, li, enLi);
                ili.addFeature(esaResource.getProvideFeature());
                licenseMap.put(locale, ili);
                return ili;
            }
        } catch (RepositoryException e) {
        }
        return null;
    }

    @Override
    public InstallLicense getLicense() {
        return getLicense(Locale.ENGLISH);
    }

    @Override
    public String getProvideFeature() {
        if (esaResource == null)
            return super.getProvideFeature();
        return esaResource.getProvideFeature();
    }

    @Override
    public Collection<String> getRequireFeature() {
        if (esaResource == null)
            return super.getRequireFeature();
        return esaResource.getRequireFeature();
    }

    @Override
    public boolean isPublic() {
        if (esaResource == null)
            return super.isPublic();
        return esaResource.getVisibility() == Visibility.PUBLIC ||
               esaResource.getVisibility() == Visibility.INSTALL;
    }

    @Override
    public boolean isAddon() {
        if (esaResource == null)
            return super.isAddon();
        return esaResource.getVisibility() == Visibility.INSTALL;
    }

    @Override
    public RepositoryResource getRepositoryResource() {
        return this.esaResource;
    }

    @Override
    public void download(File installTempDir) throws InstallException {
        if (esaResource == null)
            return;

        asset = download(installTempDir, esaResource);
        if (asset != null) {
            if (esaResource.getType().equals(ResourceType.FEATURE)) {
                Visibility v = esaResource.getVisibility();
                if (v.equals(Visibility.PUBLIC) || v.equals(Visibility.INSTALL)) {
                    logger.log(Level.FINE, Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("MSG_DOWNLOAD_SUCCESS", esaResource.getName()));
                }
            }
            try {
                zip = new ZipFile(asset);
                this.subsystemEntry = new SubsytemEntry(zip);
                ZipEntry subsystemEntry = this.subsystemEntry.getSubsystemEntry();
                if (subsystemEntry != null) {
                    featureDefinition = new SubsystemFeatureDefinitionImpl(getRepoType(repoType), zip.getInputStream(subsystemEntry));
                }
            } catch (Exception e) {
                throw ExceptionUtils.createFailedToDownload(esaResource, e, installTempDir);
            }
        }
    }

    @Override
    public void cleanup() {
        if (esaResource == null)
            return;
        delete();
        zip = null;
        asset = null;
        subsystemEntry = null;
        featureDefinition = null;
    }
}
