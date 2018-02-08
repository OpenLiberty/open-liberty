/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.internal.asset;

import java.util.Locale;

import com.ibm.ws.install.InstalledFeature;
import com.ibm.ws.install.InstalledFeatureCollection;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

/**
 *
 */
public class InstalledAssetImpl implements InstalledFeature, InstalledFeatureCollection {

    private ProvisioningFeatureDefinition pfd = null;

    public InstalledAssetImpl(ProvisioningFeatureDefinition pfd) {
        this.pfd = pfd;
    }

    @Override
    public String getDisplayName(Locale locale) {
        String displayName = pfd.getHeader("Subsystem-Name", locale);
        if (displayName == null) {
            displayName = pfd.getHeader("Subsystem-Name", new Locale(locale.getLanguage()));
            if (displayName == null)
                displayName = pfd.getHeader("Subsystem-Name", Locale.ENGLISH);
        }
        return displayName;
    }

    @Override
    public String getDisplayName() {
        return getDisplayName(Locale.ENGLISH);
    }

    @Override
    public String getShortDescription() {
        return getShortDescription(Locale.ENGLISH);
    }

    @Override
    public String getShortDescription(Locale locale) {
        String shortDescription = pfd.getHeader("Subsystem-Description", locale);
        if (shortDescription == null) {
            shortDescription = pfd.getHeader("Subsystem-Description", new Locale(locale.getLanguage()));
            if (shortDescription == null)
                shortDescription = pfd.getHeader("Subsystem-Description", Locale.ENGLISH);
        }
        return shortDescription;
    }

    @Override
    public boolean isPublic() {
        Visibility v = pfd.getVisibility();
        return v == Visibility.PUBLIC || v == Visibility.INSTALL;
    }

    @Override
    public String getProductId() {
        return pfd.getHeader("IBM-ProductID");
    }
}
