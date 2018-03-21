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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.ibm.ws.install.InstallLicense;
import com.ibm.ws.install.internal.InstallLicenseImpl;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.install.repository.FeatureAsset;
import com.ibm.ws.install.repository.FeatureCollectionAsset;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.cmdline.ThirdPartyLicenseProvider;
import com.ibm.ws.kernel.feature.internal.generator.ManifestFileProcessor;
import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;

import wlp.lib.extract.LicenseProvider;
import wlp.lib.extract.ZipLicenseProvider;

/**
 *
 */
public class ESABaseAsset extends InstallAsset implements FeatureAsset, FeatureCollectionAsset {

    protected String featureName;
    protected String shortName;
    protected String repoType;
    protected SubsytemEntry subsystemEntry = null;
    protected ProvisioningFeatureDefinition featureDefinition = null;
    protected LicenseProvider licenseProvider;
    protected Map<Locale, InstallLicense> licenseMap;

    protected ZipFile zip;

    public ESABaseAsset(String id, String featureName, String repoType, File assetFile, boolean isTemporary) throws ZipException, IOException {
        super(id, assetFile, isTemporary);
        this.featureName = featureName;
        this.repoType = repoType;
        if (assetFile != null) {
            zip = new ZipFile(assetFile);
            this.subsystemEntry = new SubsytemEntry(zip);
            ZipEntry subsystemEntry = this.subsystemEntry.getSubsystemEntry();
            if (subsystemEntry != null) {
                featureDefinition = new SubsystemFeatureDefinitionImpl(getRepoType(repoType), zip.getInputStream(subsystemEntry));
                this.shortName = InstallUtils.getShortName(featureDefinition);
            }
        }
    }

    public ESABaseAsset(File assetFile, String repoType, boolean temporary) throws ZipException, IOException {
        super(assetFile, temporary);
        zip = new ZipFile(assetFile);
        this.repoType = repoType;
        this.subsystemEntry = new SubsytemEntry(zip);
        ZipEntry subsystemEntry = this.subsystemEntry.getSubsystemEntry();
        if (subsystemEntry != null) {
            featureDefinition = new SubsystemFeatureDefinitionImpl(getRepoType(repoType), zip.getInputStream(subsystemEntry));
            this.featureName = featureDefinition.getSymbolicName();
            this.shortName = InstallUtils.getShortName(featureDefinition);
            setName(this.shortName == null ? this.featureName : this.shortName);
        } else {
            this.featureName = null;
            this.shortName = null;
        }
    }

    public ESABaseAsset(ZipFile indexZip, String featureName, String repoType, boolean temporary) throws IOException {
        super(temporary);
        zip = indexZip;
        this.repoType = repoType;
        this.subsystemEntry = new SubsytemEntry(zip, featureName);
        ZipEntry subsystemEntry = this.subsystemEntry.getSubsystemEntry();
        if (subsystemEntry != null) {
            featureDefinition = new SubsystemFeatureDefinitionImpl(getRepoType(repoType), zip.getInputStream(subsystemEntry));
            this.featureName = featureDefinition.getSymbolicName();
            this.shortName = InstallUtils.getShortName(featureDefinition);
            setName(this.shortName == null ? this.featureName : this.shortName);
        } else {
            this.featureName = null;
            this.shortName = null;
        }
    }

    /**********************************************************************************************
     * protected methods
     **********************************************************************************************/
    protected String getRepoType(String toVal) {
        if (toVal == null || "usr".equals(toVal)) {
            return ExtensionConstants.USER_EXTENSION;
        } else if (ManifestFileProcessor.CORE_PRODUCT_NAME.equals(toVal)) {
            return ExtensionConstants.CORE_EXTENSION;
        } else {
            return toVal;
        }
    }

    // will need to override
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

    protected String getHeader(String header, Locale locale) {
        // Get the value for the header....
        String value = featureDefinition.getHeader(header);

        // if null, empty, or no target locale, just return it
        if (value == null || value.isEmpty() || locale == null)
            return value;

        // If this is a localizable header that indicates it wants to be localized...
        if (value.charAt(0) == '%') {
            // Find the resource bundle...
            ResourceBundle rb = getResourceBundle(locale);
            if (rb != null) {
                // Find the new value in the resource bundle
                value = rb.getString(value.substring(1));
            }
        }

        return value;
    }

    // will need to override
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

    /**********************************************************************************************
     * public methods
     **********************************************************************************************/

    public boolean isAutoFeature() {
        return featureDefinition != null & featureDefinition.isAutoFeature();
    }

    public boolean isPublic() {
        return featureDefinition.getVisibility() == Visibility.PUBLIC ||
               featureDefinition.getVisibility() == Visibility.INSTALL;
    }

    @Override
    public boolean isAddon() {
        return featureDefinition.getVisibility() == Visibility.INSTALL;
    }

    public boolean installWhenSatisfied() {
        String installPolicy = featureDefinition.getHeader("IBM-Install-Policy");
        return installPolicy != null && "when-satisfied".equals(installPolicy);
    }

    public String getFeatureName() {
        return featureName;
    }

    public String getRepoType() {
        return repoType;
    }

    public void setRepoType(String repoType) throws IOException {
        if (this.repoType == null) {
            if (repoType == null)
                return;
        } else {
            if (this.repoType.equalsIgnoreCase(repoType))
                return;
        }
        this.repoType = repoType;
        featureDefinition = new SubsystemFeatureDefinitionImpl(getRepoType(repoType), zip.getInputStream(this.subsystemEntry.getSubsystemEntry()));
    }

    public ProvisioningFeatureDefinition getProvisioningFeatureDefinition() {
        return featureDefinition;
    }

    public ZipEntry getEntry(String name) {
        return zip.getEntry(name);
    }

    public InputStream getInputStream(ZipEntry entry) throws IOException {
        return zip.getInputStream(entry);
    }

    public ZipEntry getSubsystemEntry() {
        return this.subsystemEntry != null ? this.subsystemEntry.getSubsystemEntry() : null;
    }

    public String getSubsystemEntryName() {
        return this.subsystemEntry != null ? this.subsystemEntry.getSubsystemEntryName() : null;
    }

    public String getLicenseId() {
        return featureDefinition.getHeader("Subsystem-License");
    }

    public boolean matchFeature(String feature) {
        return (this.featureName != null && this.featureName.equals(feature)) ||
               (getName() != null && getName().equalsIgnoreCase(feature));
    }

    public LicenseProvider getLicenseProvider(Locale locale) {
        if (licenseProvider == null) {
            String licenseAgreementPrefix = featureDefinition.getHeader("IBM-License-Agreement");
            String licenseInformationPrefix = featureDefinition.getHeader("IBM-License-Information");
            licenseProvider = createLicenseProvider(licenseAgreementPrefix, licenseInformationPrefix, getLicenseId());
        }
        return licenseProvider;
    }

    @Override
    public boolean isFeature() {
        return true;
    }

    @Override
    public String installedLogMsg() {
        return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_INSTALLED_FEATURE", getDisplayName()).replaceAll("CWWKF1304I:", "").trim();
    }

    /**********************************************************************************************
     * interface methods
     **********************************************************************************************/

    @Override
    public String getShortName() {
        return this.shortName;
    }

    @Override
    public String getProvideFeature() {
        String manifestSymbolicName = featureDefinition.getHeader("Subsystem-SymbolicName");
        int i = manifestSymbolicName.indexOf(";");
        if (i >= 0)
            manifestSymbolicName = manifestSymbolicName.substring(0, i);
        return manifestSymbolicName.trim();
    }

    @Override
    public Collection<String> getRequireFeature() {
        Collection<String> requireFeatures = new ArrayList<String>();
        for (FeatureResource fr : featureDefinition.getConstituents(null)) {
            SubsystemContentType type = fr.getType();
            if (SubsystemContentType.FEATURE_TYPE == type) {
                requireFeatures.add(fr.getSymbolicName());
            }
        }
        return requireFeatures;
    }

    @Override
    public String getDisplayName(Locale locale) {
        String displayName = getHeader("Subsystem-Name", locale);
        if (displayName == null) {
            displayName = getHeader("Subsystem-Name", new Locale(locale.getLanguage()));
            if (displayName == null)
                displayName = getHeader("Subsystem-Name", Locale.ENGLISH);
        }

        if (displayName != null && !displayName.isEmpty() && !displayName.equalsIgnoreCase("%name"))
            return displayName;

        displayName = getShortName();
        if (displayName != null && !displayName.isEmpty())
            return displayName;

        return featureName;
    }

    @Override
    public String getDisplayName() {
        return getDisplayName(Locale.ENGLISH);
    }

    @Override
    public String getId() {
        return this.featureName;
    }

    @Override
    public String getDescription(Locale locale) {
        String description = getHeader("Subsystem-Description", locale);
        if (description == null) {
            description = getHeader("Subsystem-Description", new Locale(locale.getLanguage()));
            if (description == null)
                description = getHeader("Subsystem-Description", Locale.ENGLISH);
        }
        return description;
    }

    @Override
    public String getDescription() {
        return getDescription(Locale.ENGLISH);
    }

    @Override
    public InstallLicense getLicense(Locale locale) {
        if (licenseMap == null) {
            licenseMap = new HashMap<Locale, InstallLicense>();
        }
        if (licenseMap.containsKey(locale))
            return licenseMap.get(locale);

        String licenseId = getLicenseId();
        if (licenseId != null && !licenseId.isEmpty()) {
            InstallLicenseImpl ili = new InstallLicenseImpl(licenseId, null, getLicenseProvider(locale), false);
            ili.addFeature(getProvideFeature());
            licenseMap.put(locale, ili);
            return ili;
        }
        return null;
    }

    @Override
    public InstallLicense getLicense() {
        return getLicense(Locale.ENGLISH);
    }

    /** {@inheritDoc} */
    @Override
    public long size() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public void delete() {
        InstallUtils.close(zip);
        super.delete();
    }
}
