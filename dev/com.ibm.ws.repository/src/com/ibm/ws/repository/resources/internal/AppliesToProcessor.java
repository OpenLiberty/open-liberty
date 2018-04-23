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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.repository.exceptions.RepositoryResourceCreationException;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.FilterVersion;

/**
 * This class parses the appliesTo header, the only documentation
 * for this header is the existing parse logic within
 *
 * wlp.lib.ProductMatch
 *
 * which is sadly too tied to its existing usage to be reusable here.
 * This class MUST be kept in sync with that logic.
 */
public class AppliesToProcessor {

    private final static List<String> allEditions = Arrays.asList("Liberty Core", "Base", "Express", "Developers", "ND", "z/OS");
    private final static List<String> betaEditions = Arrays.asList("Beta");

    private final static Map<String, String> editionsMap = new HashMap<String, String>();
    public final static String VERSION_ATTRIB_NAME = "productVersion";
    public final static String EDITION_ATTRIB_NAME = "productEdition";
    public final static String INSTALL_TYPE_ATTRIB_NAME = "productInstallType";
    private final static String EARLY_ACCESS_LABEL = "Beta";

    /** Use this constant for an edition that should not be added to the editions list at all (eg BASE_ILAN) */
    private final static String EDITION_UNMAPPED = "EDITION_UNMAPPED";

    /**
     * This constant represents an edition that does not exist and that we don't know about
     * ie it represents an error and we want the upload to fail if we hit this
     */
    private final static String EDITION_UNKNOWN = "EDITION_UNKNOWN";

    static {
        editionsMap.put("Core", "Liberty Core");
        editionsMap.put("CORE", "Liberty Core");
        editionsMap.put("LIBERTY_CORE", "Liberty Core");
        editionsMap.put("BASE", "Base");
        editionsMap.put("DEVELOPERS", "Developers");
        editionsMap.put("EXPRESS", "Express");
        editionsMap.put("EARLY_ACCESS", "Beta");
        editionsMap.put("zOS", "z/OS");
        editionsMap.put("ND", "ND");
        editionsMap.put("BASE_ILAN", EDITION_UNMAPPED);
    }
    public final static String BETA_REGEX = "[2-9][0-9][0-9][0-9][.].*";

    public static String getValue(String substring) {
        int index = substring.indexOf('=');
        substring = substring.substring(index + 1).trim();
        if (substring.charAt(0) == '"') {
            return substring.substring(1, substring.length() - 1);
        } else {
            return substring;
        }
    }

    private static void add(AppliesToFilterInfo atfi, String substring, Map<String, String> helperMap) {
        substring = substring.trim();
        if (atfi.getProductId() == null) {
            atfi.setProductId(substring);
        } else if (substring.startsWith(VERSION_ATTRIB_NAME)) {
            String version = getValue(substring);
            boolean unbounded = version.endsWith("+");
            if (unbounded) {
                version = version.substring(0, version.length() - 1);
            }
            //version+ == unbounded
            //version  == exact
            //year.month.day == Early Access

            //is this an early access?
            String label = null;
            String compatibilityLabel = null;
            if (version.matches("^" + BETA_REGEX)) {
                label = EARLY_ACCESS_LABEL;
                compatibilityLabel = EARLY_ACCESS_LABEL;
            } else {
                compatibilityLabel = version;
                //select first 3 dot components of string.
                int cutpoint = 0;
                int count = 0;
                while (cutpoint < version.length() && count < 3) {
                    if (version.charAt(cutpoint++) == '.')
                        count++;
                }
                //if there were less, just use the entire string.
                if (cutpoint == version.length() && count != 3) {
                    label = version;
                } else {
                    label = version.substring(0, cutpoint - 1);
                }
            }
            FilterVersion minVersion = new FilterVersion();
            minVersion.setLabel(label);
            minVersion.setInclusive(true);
            minVersion.setValue(version);
            minVersion.setCompatibilityLabel(compatibilityLabel);
            atfi.setMinVersion(minVersion);
            if (!unbounded) {
                //use a new instance of filterversion, just incase anyone decides to edit it later
                FilterVersion maxVersion = new FilterVersion();
                maxVersion.setLabel(label);
                maxVersion.setInclusive(true);
                maxVersion.setValue(version);
                maxVersion.setCompatibilityLabel(compatibilityLabel);
                atfi.setMaxVersion(maxVersion);
                atfi.setHasMaxVersion(Boolean.toString(true));
            }
        } else if (substring.startsWith(EDITION_ATTRIB_NAME)) {
            String editionStr = getValue(substring);
            Set<String> editions = new LinkedHashSet<String>();
            List<String> rawEditions = new ArrayList<String>();
            atfi.setRawEditions(rawEditions);
            for (int startIndex = 0, endIndex = editionStr.indexOf(',');; startIndex = endIndex, endIndex = editionStr.indexOf(',', ++startIndex)) {
                String edition = editionStr.substring(startIndex, endIndex == -1 ? editionStr.length() : endIndex);

                // Store original edition in rawEditions
                rawEditions.add(edition);

                // Map entries to happier names.
                String mappedEdition = mapRawEditionToHumanReadable(edition, helperMap);
                if (EDITION_UNMAPPED.equals(mappedEdition)) {
                    // In this case, we don't map the edition to anything
                } else if (EDITION_UNKNOWN.equals(mappedEdition)) {
                    // In this case, we don't know about the edition, so map
                    // it to itself in order to retain the same behaviour as
                    // the previous version of the code
                    editions.add(edition);
                } else {
                    // Any other possibility means that this edition maps to something
                    // good so use that
                    editions.add(mappedEdition);
                }

                if (endIndex == -1) {
                    break;
                }
            }
            atfi.setEditions(new ArrayList<String>(editions));
        } else if (substring.startsWith(INSTALL_TYPE_ATTRIB_NAME)) {
            String installTypeString = getValue(substring);
            atfi.setInstallType(installTypeString);
        }
    }

    public static List<AppliesToFilterInfo> parseAppliesToHeader(String appliesTo) {
        List<AppliesToFilterInfo> result = new ArrayList<AppliesToFilterInfo>();

        Map<String, String> editionNameMap = editionsMap;

        boolean quoted = false;
        int index = 0;
        AppliesToFilterInfo match = new AppliesToFilterInfo();
        for (int i = 0; i < appliesTo.length(); i++) {
            char c = appliesTo.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            }
            if (!quoted) {
                if (c == ',') {
                    add(match, appliesTo.substring(index, i), editionNameMap);
                    index = i + 1;
                    addEditions(match);
                    result.add(match);
                    match = new AppliesToFilterInfo();
                } else if (c == ';') {
                    add(match, appliesTo.substring(index, i), editionNameMap);
                    index = i + 1;
                }
            }
        }
        add(match, appliesTo.substring(index), editionNameMap);
        addEditions(match);
        result.add(match);

        return result;
    }

    public static void addEditions(AppliesToFilterInfo match) {
        List<String> editionsList = allEditions;

        // No editions in the appliesTo? That means *all* editions. Note that we need to
        // test rawEditions here, not editions, because not all rawEditions may actually
        // be mapped into an edition in editions.
        List<String> rawEditions = match.getRawEditions();
        if (rawEditions == null || rawEditions.isEmpty()) {
            List<String> editions = new ArrayList<String>();

            if ((match.getMinVersion() != null) && (match.getMinVersion().getCompatibilityLabel() != null) &&
                (match.getMinVersion().getCompatibilityLabel().equals(EARLY_ACCESS_LABEL))) {
                editions.addAll(betaEditions);
            } else {
                editions.addAll(editionsList);
            }

            match.setEditions(editions);
        }
    }

    /**
     * Maps the raw (ie non-human-readable) name of an edition to the human readable name, or EDITION_UNMAPPED
     * if the edition is known but should not be mapped to anything or "" if the edition is now known (ie an
     * error case).
     */
    private static String mapRawEditionToHumanReadable(String rawEdition, Map<String, String> map) {
        String result = map.get(rawEdition);
        return result != null ? result : EDITION_UNKNOWN;
    }

    /**
     * Validates the list of rawEditions and throws RepositoryResourceCreationException if any of
     * them are unknown.
     *
     * Clients of this class should call this method once the AppliesToFilterInfo object has been
     * created in order to check whether it is valid.
     */
    public static void validateEditions(AppliesToFilterInfo info, String appliesToHeader) throws RepositoryResourceCreationException {
        if (info.getRawEditions() != null) {
            for (String rawEdition : info.getRawEditions()) {
                if (EDITION_UNKNOWN.equals(mapRawEditionToHumanReadable(rawEdition, editionsMap))) {
                    throw new RepositoryResourceCreationException("Resource applies to at least one unknown edition: " + rawEdition +
                                                                  "; appliesTo= " + appliesToHeader, null);
                }
            }
        }
    }

    /**
     * Similiar logic to parseAppliesToHeader above, duplicate logic, be nice to find time to factor this into
     * a common routine
     *
     * @param appliesTo
     * @return
     */
    public static List<AppliesToEntry> parseAppliesToEntries(String appliesTo) {
        List<AppliesToEntry> result = new ArrayList<AppliesToEntry>();

        boolean quoted = false;
        int index = 0;

        AppliesToEntry entry = new AppliesToEntry();
        for (int i = 0; i < appliesTo.length(); i++) {
            char c = appliesTo.charAt(i);
            if (c == '"') {
                quoted = !quoted;
            }
            if (!quoted) {
                if (c == ',') {
                    entry.add(appliesTo.substring(index, i));
                    // We've read one applied to entry in. Make sure its ok
                    index = i + 1;
                    result.add(entry);
                    entry = new AppliesToEntry();
                } else if (c == ';') {
                    entry.add(appliesTo.substring(index, i));
                    index = i + 1;
                }
            }
        }
        entry.add(appliesTo.substring(index));
        result.add(entry);

        return result;
    }

    public static class AppliesToEntry {

        private String _productId;
        private String _editions;
        private String _version;
        private String _installType;

        private void add(String substring) {
            substring = substring.trim();
            if (_productId == null) {
                _productId = substring;
            } else if (substring.startsWith(VERSION_ATTRIB_NAME)) {
                _version = AppliesToProcessor.getValue(substring);
            } else if (substring.startsWith(EDITION_ATTRIB_NAME)) {
                _editions = AppliesToProcessor.getValue(substring);
            } else if (substring.startsWith(INSTALL_TYPE_ATTRIB_NAME)) {
                _installType = AppliesToProcessor.getValue(substring);
            }
        }

        /**
         * @return the _productId
         */
        public String getProductId() {
            return _productId;
        }

        /**
         * @return the _edition
         */
        public String getEditions() {
            return _editions;
        }

        /**
         * @return the _version
         */
        public String getVersion() {
            return _version;
        }

        /**
         * @return the _installType
         */
        public String getInstallType() {
            return _installType;
        }

        @Override
        public String toString() {
            return "[AppliesToEntry <productId=" + _productId + "> <editions=" + _editions + "> <version=" +
                   _version + "> <installType=" + _installType + ">";
        }
    };
}