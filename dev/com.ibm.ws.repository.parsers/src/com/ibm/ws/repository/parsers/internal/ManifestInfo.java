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
package com.ibm.ws.repository.parsers.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveEntryNotFoundException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveIOException;
import com.ibm.ws.repository.parsers.exceptions.RepositoryArchiveInvalidEntryException;

/**
 * class encapsulating return from parsing a manifest file
 */
public class ManifestInfo {

    /**
     * Fields to search for inside manifest
     */
    private static final String APPLIES_TO = "Applies-To";
    private static final String REQUIRE_FEATURE = "Require-Feature";
    private static final String PROVIDER = "Bundle-Vendor";
    private static final String SAMPLE_TYPE = "Sample-Type";
    private static final String SAMPLE_TYPE_PRODUCT = "product";
    private static final String SAMPLE_TYPE_OPENSOURCE = "thirdParty";

    private final String _prov;
    private final String _appliesTo;
    private final ResourceType _type;
    private final List<String> _requiresList;
    private final String _archiveRoot;
    private final Manifest _manifest;

    /**
     * Constructor
     *
     * @param wlpInfo
     * @param prov
     */
    private ManifestInfo(String prov, String appliesTo, ResourceType type, List<String> requiresList, String archiveRoot, Manifest manifest) {
        _prov = prov;
        _appliesTo = appliesTo;
        _type = type;
        _requiresList = requiresList;
        _archiveRoot = archiveRoot;
        _manifest = manifest;

    }

    /**
     * Get the provider from the manifest
     *
     * @return _prov
     */
    public String getProviderName() {
        return _prov;
    }

    /**
     * Get the applies to field from the manifest
     *
     * @return _appliesTo
     */
    public String getAppliesTo() {
        return _appliesTo;
    }

    /**
     * Get the type field from the manifest
     *
     * @return
     */
    public ResourceType getType() {
        return _type;
    }

    /**
     * Get the required features list from the manifest
     *
     * @return
     */
    public List<String> getRequiredFeature() {
        return _requiresList;
    }

    /**
     * @return
     */
    public String getArchiveRoot() {
        return _archiveRoot;
    }

    /**
     * @return the _manifest
     */
    public Manifest getManifest() {
        return _manifest;
    }

    /**
     * Extracts information from the manifest in the supplied jar file and
     * populates a newly created WlpInformation object with the extracted
     * information as well as putting information into the asset itself.
     *
     * @param jar
     *            The jar file to parse
     * @param ass
     *            The asset associated with the jar file, the provider field is
     *            set by reading it from the manifest.
     * @return A newly created WlpInformation object with data populated from
     *         the manifest
     * @throws MassiveArchiveException
     * @throws IOException
     */
    public static ManifestInfo parseManifest(JarFile jar) throws RepositoryArchiveException, RepositoryArchiveIOException {

        String prov = null;

        // Create the WLPInformation and populate it
        Manifest mf = null;
        try {
            mf = jar.getManifest();
        } catch (IOException e) {
            throw new RepositoryArchiveIOException("Unable to access manifest in sample", new File(jar.getName()), e);
        } finally {
            try {
                jar.close();
            } catch (IOException e) {
                throw new RepositoryArchiveIOException("Unable to access manifest in sample", new File(jar.getName()), e);
            }
        }

        if (null == mf) {
            throw new RepositoryArchiveEntryNotFoundException("No manifest file found in sample", new File(jar.getName()), "/META-INF/MANIFEST.MF");
        }

        String appliesTo = null;
        ResourceType type = null;
        List<String> requiresList = new ArrayList<String>();

        Attributes mainattrs = mf.getMainAttributes();

        // Iterate over the main attributes in the manifest and look for the ones we are interested in.
        for (Object at : mainattrs.keySet()) {
            String attribName = ((Attributes.Name) at).toString();
            String attribValue = (String) mainattrs.get(at);

            if (APPLIES_TO.equals(attribName)) {
                appliesTo = attribValue;
            } else if (SAMPLE_TYPE.equals(attribName)) {
                String typeString = (String) mainattrs.get(at);
                if (SAMPLE_TYPE_OPENSOURCE.equals(typeString)) {
                    type = ResourceType.OPENSOURCE;
                } else if (SAMPLE_TYPE_PRODUCT.equals(typeString)) {
                    type = ResourceType.PRODUCTSAMPLE;
                } else {
                    throw new IllegalArgumentException("The following jar file is not a known sample type " + jar.getName());
                }
            } else if (REQUIRE_FEATURE.equals(attribName)) {

                // We need to split the required features, from a String of comma seperated
                // features into a List of String, where each String is a feature name.
                StringTokenizer featuresTokenizer = new StringTokenizer(attribValue, ",");
                while (featuresTokenizer.hasMoreElements()) {
                    String nextFeature = (String) featuresTokenizer.nextElement();
                    requiresList.add(nextFeature);
                }
            } else if (PROVIDER.equals(attribName)) {
                prov = attribValue;
            }
        }

        if (null == prov) {
            throw new RepositoryArchiveInvalidEntryException("No Bundle-Vendor specified in the sample's manifest", new File(jar.getName()), "/META-INF/MANIFEST.MF");
        }
        if (null == type) {
            throw new RepositoryArchiveInvalidEntryException("No Sample-Type specified in the sample's manifest", new File(jar.getName()), "/META-INF/MANIFEST.MF");
        }

        String archiveRoot = mainattrs.getValue("Archive-Root");
        archiveRoot = archiveRoot != null ? archiveRoot : "";

        ManifestInfo mi = new ManifestInfo(prov, appliesTo, type, requiresList, archiveRoot, mf);

        return mi;
    }

}
