/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.featureverifier.migrator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.Manifest;

import org.apache.aries.util.manifest.ManifestHeaderProcessor;
import org.apache.aries.util.manifest.ManifestHeaderProcessor.NameValuePair;
import org.apache.aries.util.manifest.ManifestProcessor;

import com.ibm.ws.featureverifier.internal.FilterUtils;
import com.ibm.ws.featureverifier.internal.FilterUtils.ParseError;

class FeatureInfo implements Comparable<FeatureInfo> {

    final String symbolicName;
    final boolean singleton;
    final String visibility;
    private boolean isAutoFeature;
    private String autoFeatureHeaderContent;

    final Map<String, Set<String>> features;

    boolean isAutoFeature() {
        return this.isAutoFeature;
    }

    Set<Set<String>> getEnablingFeatureNames() throws ParseError {
        if (autoFeatureHeaderContent == null) {
            throw new IllegalStateException("Can only process autofeature info for live features, not ones loaded from framework xml");
        }
        Set<String> filters = new HashSet<String>();
        List<NameValuePair> info = ManifestHeaderProcessor.parseExportString(autoFeatureHeaderContent);
        for (NameValuePair nvp : info) {
            if (nvp.getName().equals("osgi.identity")) {
                Map<String, String> attribs = nvp.getAttributes();
                if (attribs != null) {
                    for (Map.Entry<String, String> attrib : attribs.entrySet()) {
                        if (attrib.getKey().equals("filter:")) {
                            filters.add(attrib.getValue());
                        }
                    }
                }
            }
        }

        Set<Set<String>> features = FilterUtils.parseFilters(filters);

        return features;
    }

    private NameValuePair getDetailsFromManifest(File manifest, Map<String, Set<String>> features) {
        NameValuePair name = null;
        try {
            InputStream is = new FileInputStream(manifest);
            try {
                Manifest m = ManifestProcessor.parseManifest(is);
                String nameStr = m.getMainAttributes().getValue("Subsystem-SymbolicName");
                name = ManifestHeaderProcessor.parseBundleSymbolicName(nameStr);

                String contentString = m.getMainAttributes().getValue("Subsystem-Content");
                Map<String, Map<String, String>> content = ManifestHeaderProcessor.parseImportString(contentString);
                for (Entry<String, Map<String, String>> contentItem : content.entrySet()) {
                    if (contentItem.getValue().get("type") != null && contentItem.getValue().get("type").equals("osgi.subsystem.feature")) {
                        String tolerates = contentItem.getValue().get("ibm.tolerates:");
                        String preferred = contentItem.getKey();
                        Set<String> tolerated = new TreeSet<String>();
                        if (tolerates != null) {
                            String parts[] = tolerates.split(",");
                            String base = preferred.substring(0, preferred.lastIndexOf('-'));
                            for (String part : parts) {
                                tolerated.add(base + "-" + part);
                            }
                        }
                        features.put(preferred, Collections.unmodifiableSet(tolerated));
                    }
                }

                Map<String, String> manifestMap = ManifestProcessor.readManifestIntoMap(m);
                if (manifestMap.containsKey(IgnoreConstants.AUTOFEATURE_HEADER_NAME)) {
                    autoFeatureHeaderContent = manifestMap.get(IgnoreConstants.AUTOFEATURE_HEADER_NAME);
                    isAutoFeature = true;
                }
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        } catch (IOException io) {
            System.err.println("Manifest " + manifest.getAbsolutePath());
            io.printStackTrace();
        }
        return name;
    }

    public FeatureInfo(File baseDir, String source) {

        Map<String, Set<String>> features = new TreeMap<String, Set<String>>();

        NameValuePair parsedName = getDetailsFromManifest(new File(baseDir, source), features);
        symbolicName = parsedName.getName();
        String visibilityStr = null;
        String singletonStr = null;
        if (parsedName.getAttributes() != null) {
            visibilityStr = parsedName.getAttributes().get("visibility:");
            singletonStr = parsedName.getAttributes().get("singleton:");
        }
        visibility = visibilityStr == null ? "private" : visibilityStr;
        singleton = singletonStr == null ? false : Boolean.valueOf(singletonStr);

        if (symbolicName == null) {
            throw new IllegalStateException(source);
        }

        this.features = Collections.unmodifiableMap(features);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                 + ((symbolicName == null) ? 0 : symbolicName.hashCode());
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
        FeatureInfo other = (FeatureInfo) obj;
        if (symbolicName == null) {
            if (other.symbolicName != null)
                return false;
        } else if (!symbolicName.equals(other.symbolicName))
            return false;
        return true;
    }

    @Override
    public int compareTo(FeatureInfo o) {
        if (o == null)
            return -1;
        else
            return symbolicName.compareTo(o.symbolicName);
    }

}