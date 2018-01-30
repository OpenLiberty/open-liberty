/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class parses the appliesTo header, the only documentation
 * for this header is currently the logic in this class.
 *
 * Please note if you alter this logic, additional parsers for the
 * appliesTo header can be found in the following class(es).
 *
 * com.ibm.ws.repository.resources.internal.AppliesToProcessor.
 *
 * These parsers must be kept in sync.
 */
public final class ProductMatch {
    private String productId;
    private String version;
    private String installType;
    private final List editions = new ArrayList();
    private String licenseType;
    private static final Pattern validNumericVersionOrRange = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)\\+?$");
    private static final Pattern validBaseVersion = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
    public static final int MATCHED = 0;
    public static final int NOT_APPLICABLE = -1;
    public static final int INVALID_VERSION = -2;
    public static final int INVALID_EDITION = -3;
    public static final int INVALID_INSTALL_TYPE = -4;
    public static final int INVALID_LICENSE = -5;

    /**
     * @param substring
     */
    public void add(String substring) {
        substring = substring.trim();
        if (productId == null) {
            productId = substring;
        } else if (substring.startsWith("productVersion")) {
            version = getValue(substring);
        } else if (substring.startsWith("productEdition")) {
            String editionStr = getValue(substring);
            for (int startIndex = 0, endIndex = editionStr.indexOf(',');; startIndex = endIndex, endIndex = editionStr.indexOf(',', ++startIndex)) {
                editions.add(editionStr.substring(startIndex, endIndex == -1 ? editionStr.length() : endIndex));
                if (endIndex == -1) {
                    break;
                }
            }
        } else if (substring.startsWith("productInstallType")) {
            installType = getValue(substring);
        } else if (substring.startsWith("productLicenseType")) {
            licenseType = getValue(substring);
        }
    }

    public int matches(Properties props) {
        if (productId.equals(props.getProperty("com.ibm.websphere.productId"))) {
            if (version != null) {
                String productVersion = props.getProperty("com.ibm.websphere.productVersion");
                Matcher appliesToMatcher = validNumericVersionOrRange.matcher(version);
                boolean appliesToIsRange = version.endsWith("+");
                Matcher productVersionMatcher = validNumericVersionOrRange.matcher(productVersion);

                if (appliesToMatcher.matches() && appliesToIsRange && productVersionMatcher.matches()) {
                    //Do a range check if the applies to is a numeric range n.n.n.n+, and the target product version is a numeric version n.n.n.n
                    int[] minAppliesToVersion = allMatcherGroupsToIntArray(appliesToMatcher);
                    int[] targetProductVersion = allMatcherGroupsToIntArray(productVersionMatcher);

                    if (!versionSatisfiesMinimum(minAppliesToVersion, targetProductVersion)) {
                        return INVALID_VERSION;
                    }
                } else {
                    // If one of the versions matches the base product version (n.n.n), check
                    // that the other version (m.m.m.m or m.m.m.m+) starts with this base version string.
                    final Matcher baseAppliesToMatcher = validBaseVersion.matcher(version);
                    final Matcher baseProductVersionMatcher = validBaseVersion.matcher(productVersion);
                    final boolean baseAppliesToMatches = baseAppliesToMatcher.matches();
                    final boolean baseProductVersionMatches = baseProductVersionMatcher.matches();
                    if (baseAppliesToMatches ^ baseProductVersionMatches) {
                        if (baseAppliesToMatches) {
                            if (!!!(productVersionMatcher.matches() && productVersion.startsWith(version + '.'))) {
                                return INVALID_VERSION;
                            }
                        } else {
                            if (!!!(appliesToMatcher.matches() && version.startsWith(productVersion + '.'))) {
                                return INVALID_VERSION;
                            }
                        }
                    }
                    // If appliesTo version doesn't end in +, if both versions match base product version,
                    // or target product version is non-numeric, require String.equals()
                    else if (!!!version.equals(productVersion)) {
                        return INVALID_VERSION;
                    }
                }
            }

            if (!!!editions.isEmpty() && !!!editions.contains(props.getProperty("com.ibm.websphere.productEdition"))) {
                return INVALID_EDITION;
            }

            if (licenseType != null && !!!licenseType.equals(props.getProperty("com.ibm.websphere.productLicenseType"))) {
                return INVALID_LICENSE;
            }

            if (installType != null && !!!installType.equals(props.getProperty("com.ibm.websphere.productInstallType"))) {
                return INVALID_INSTALL_TYPE;
            }

            return MATCHED;
        } else {
            return NOT_APPLICABLE;
        }
    }

    /**
     * Takes groups 1 to n of a matcher with groupCount() == n
     * and places them in order into an int[] array of size n.
     *
     * @param matcher
     * @return
     */
    private int[] allMatcherGroupsToIntArray(Matcher matcher) {
        int numGroups = matcher.groupCount();
        int[] digits = new int[numGroups];
        for (int i = 0; i < numGroups; i++) {
            digits[i] = Integer.parseInt(matcher.group(i + 1));
        }
        return digits;
    }

    /**
     * @param substring
     * @return
     */
    private String getValue(String substring) {
        int index = substring.indexOf('=');
        substring = substring.substring(index + 1).trim();
        if (substring.charAt(0) == '"') {
            return substring.substring(1, substring.length() - 1);
        } else {
            return substring;
        }
    }

    /**
     * @return
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return
     */
    public List getEditions() {
        return editions;
    }

    public String getProductId() {
        return productId;
    }

    public String getLicenseType() {
        return licenseType;
    }

    public String getInstallType() {
        return installType;
    }

    /**
     * Evaluate whether queryVersion is considered to be greater or equal than minimumVersion.
     *
     * Expects version numbers in array form, where the elements of the array are the digits of the version
     * number, in order.
     *
     * Returns true if the version represented by queryVersion is greater than or equal to the
     * version represented by minimumVersion.
     *
     * Returns false if the version numbers are not of the same length, or the minimum version is not satisfied.
     *
     * @param minimumVersion
     * @param queryVersion
     * @return
     */
    public static boolean versionSatisfiesMinimum(int[] minimumVersion, int[] queryVersion) {
        if (minimumVersion.length == queryVersion.length) {
            for (int i = 0; i < minimumVersion.length; i++) {
                //Start at most significant digit
                if (queryVersion[i] < minimumVersion[i]) {
                    //This is too small, so fail. We're moving in from the most significant end, so no point in continuing
                    return false;
                } else if (queryVersion[i] == minimumVersion[i]) {
                    //This one is the same, so we must continue and check the other digits.
                    continue;
                } else if (queryVersion[i] > minimumVersion[i]) {
                    //This one is bigger. We're moving in from the most significant end, so the other bits don't matter now.
                    return true;
                }
            }
            //We got to the end without breaking any rules, so it is valid.
            return true;
        }
        return false;
    }

}