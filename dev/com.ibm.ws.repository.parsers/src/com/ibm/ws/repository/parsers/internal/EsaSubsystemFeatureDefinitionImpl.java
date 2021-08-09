/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.parsers.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;

/**
 * This extension to {@link SubsystemFeatureDefinitionImpl} will work with a
 * feature contained within an ESA and provide translated header information
 * when requested (although {@link SubsystemFeatureDefinitionImpl} can load
 * features from an ESA it will not show translated messages).
 */
public class EsaSubsystemFeatureDefinitionImpl extends SubsystemFeatureDefinitionImpl {

    /**
     * Create a new instance of this class for the supplied ESA file.
     *
     * @param esa The ESA to load
     * @return The {@link EsaSubsystemFeatureDefinitionImpl} for working with the properties of the ESA
     * @throws ZipException
     * @throws IOException
     */
    public static EsaSubsystemFeatureDefinitionImpl constructInstance(File esa) throws ZipException, IOException {
        // Find the manifest - case isn't guaranteed so do a search
        ZipFile zip = new ZipFile(esa);
        Enumeration<? extends ZipEntry> zipEntries = zip.entries();
        ZipEntry subsystemEntry = null;
        while (zipEntries.hasMoreElements()) {
            ZipEntry nextEntry = zipEntries.nextElement();
            if ("OSGI-INF/SUBSYSTEM.MF".equalsIgnoreCase(nextEntry.getName())) {
                subsystemEntry = nextEntry;
            }
        }
        return new EsaSubsystemFeatureDefinitionImpl(zip.getInputStream(subsystemEntry), zip);
    }

    private final ZipFile esa;

    private EsaSubsystemFeatureDefinitionImpl(InputStream manifestInputStream,
                                              ZipFile esa) throws IOException {
        // Pass input stream to supertype, it isn't installed in a repo yet
        // (it's an ESA file) so supply null for the repo type
        super(null, manifestInputStream);
        this.esa = esa;
    }

    @Override
    protected ResourceBundle getResourceBundle(Locale locale) {
        // Look at where to searh for localization files
        String localizationLocation = this.getHeader("Subsystem-Localization");
        if (localizationLocation == null) {
            return null;
        }

        ZipEntry[] entries = new ZipEntry[] {
                                              this.esa.getEntry(localizationLocation + "_"
                                                                + locale.toString() + ".properties"),
                                              this.esa.getEntry(localizationLocation + "_"
                                                                + locale.getLanguage() + ".properties"),
                                              this.esa.getEntry(localizationLocation + ".properties") };

        for (ZipEntry entry : entries) {
            if (entry != null) {
                try {
                    return new PropertyResourceBundle(new InputStreamReader(this.esa.getInputStream(entry)));
                } catch (IOException e) {
                }
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getIbmShortName() {
        /*
         * The runtime only sets the short name for public features, which we can't really change for the runtime but an ESA might contain a short name that we do want to use (i.e.
         * INSTALL ESAs)
         */
        String shortName = super.getIbmShortName();
        if (shortName == null) {
            shortName = getHeader("IBM-ShortName");
        }
        return shortName;
    }

}
