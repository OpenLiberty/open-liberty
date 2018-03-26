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

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.ws.kernel.feature.internal.cmdline.ThirdPartyLicenseProvider;

import wlp.lib.extract.LicenseProvider;
import wlp.lib.extract.ZipLicenseProvider;

/**
 *
 */
public class ESAVirtualAsset extends ESABaseAsset {

    public ESAVirtualAsset(ZipFile indexZip, String featureName, String repoType, boolean isTemporary) throws IOException {
        super(indexZip, featureName, repoType, isTemporary);
    }

    // will need to override
    @Override
    protected LicenseProvider createLicenseProvider(String licenseAgreementPrefix, String licenseInformationPrefix,
                                                    String subsystemLicenseType) {
        String featureLicenseAgreementPrefix = this.featureName + "/" + licenseAgreementPrefix;
        String featureLicenseInformationPrefix = licenseInformationPrefix == null ? null : this.featureName + "/" + licenseInformationPrefix;
        if (featureLicenseInformationPrefix == null) {
            LicenseProvider lp = ZipLicenseProvider.createInstance(zip, featureLicenseAgreementPrefix);
            if (lp != null)
                return lp;
        } else {
            wlp.lib.extract.ReturnCode licenseReturnCode = ZipLicenseProvider.buildInstance(zip, featureLicenseAgreementPrefix,
                                                                                            featureLicenseInformationPrefix);
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

    @Override
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

    @Override
    protected ResourceBundle getResourceBundle(Locale locale) {
        try {
            ZipEntry entry = getEntry(this.featureName + "/OSGI-INF/l10n/subsystem_" + locale.getLanguage() + ".properties");
            if (entry == null) {
                entry = getEntry(this.featureName + "/OSGI-INF/l10n/subsystem.properties");
            }
            if (entry != null)
                return new PropertyResourceBundle(getInputStream(entry));
        } catch (IOException e) {
            //TODO: log the exception
        }
        return null;
    }

    @Override
    public long size() {
        try {
            ZipEntry entry = getEntry("feature.properties");
            if (entry != null) {
                Properties p = new Properties();
                InputStream inStream = getInputStream(entry);
                p.load(inStream);
                String size = p.getProperty(this.featureName + ".size");
                return Long.valueOf(size);
            }
        } catch (IOException e) {

        }
        return 0;
    }
}
