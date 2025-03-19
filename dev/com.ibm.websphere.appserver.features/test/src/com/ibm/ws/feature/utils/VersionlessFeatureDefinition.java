package com.ibm.ws.feature.utils;

import java.util.ArrayList;

public class VersionlessFeatureDefinition {

    private final String featureName;
    private final String subsystemName;
    private final ArrayList<String[]> featuresAndPlatformAndKind;
    private String alsoKnownAs;
    private String akaFutureFeature;
    private final String edition;

    public VersionlessFeatureDefinition(String featureName, String subsystemName, ArrayList<String[]> featuresAndPlatformAndKind, String editon) {
        this.featureName = featureName;
        this.subsystemName = subsystemName;
        this.featuresAndPlatformAndKind = featuresAndPlatformAndKind;
        this.edition = editon;
    }

    public VersionlessFeatureDefinition(String featureName, String subsystemName, String[] featureAndPlatformAndKind, String edition) {
        this.featureName = featureName;
        this.subsystemName = subsystemName;
        this.featuresAndPlatformAndKind = new ArrayList<String[]>();
        featuresAndPlatformAndKind.add(featureAndPlatformAndKind);
        this.edition = edition;
    }

    /**
     * Get the Feature Name for this feature.
     *
     * @return
     */
    public String getFeatureName() {
        return this.featureName;
    }

    public String getEdition() {
        return this.edition;
    }

    /**
     * Get the Feature Name for this feature.
     *
     * @return
     */
    public String getSubsystemName() {
        return this.subsystemName;
    }

    public String getAlsoKnownAs() {
        return alsoKnownAs;
    }

    public void setAlsoKnownAs(String alsoKnownAs) {
        this.alsoKnownAs = alsoKnownAs;
    }

    public String getAKAFutureFeature() {
        return akaFutureFeature;
    }

    public void setAKAFutureFeature(String futureFeature) {
        this.akaFutureFeature = futureFeature;
    }

    /**
     * Get the features mapped to their platform dependency
     * EX:
     * jpa-2.2, jakartaPlatform-8.0, ga
     * persistence-3.0, JakartaPlatform-9.1, ga
     *
     * @return
     */
    public ArrayList<String[]> getFeaturesAndPlatformAndKind() {
        return this.featuresAndPlatformAndKind;
    }

    public void addFeaturePlatformAndKind(String[] featurePlatformAndKind) {
        featuresAndPlatformAndKind.add(featurePlatformAndKind);
    }

    public void addFeaturePlatformAndKind(String featureBaseName, String platform, String featureNameWithVersion, String kind) {
        featuresAndPlatformAndKind.add(new String[] { featureBaseName, platform, featureNameWithVersion, kind });
    }

    /**
     * Gets all versions of this feature so the 'ibm.tolerates:="2.0,2.1,2.2,3.0,3.1,3.2"' can be created
     *
     * @return
     */
    public String[] getPreferredAndTolerates() {
        ArrayList<String> versions = new ArrayList<String>();

        for (String[] featAndPlat : featuresAndPlatformAndKind) {
            if (!versions.contains(featAndPlat[0].split("-")[1])) {
                versions.add(featAndPlat[0].split("-")[1]);
            }
        }
        return getPreferredAndTolerates(versions);
    }

    public String[] getPreferredAndTolerates(ArrayList<String> versions) {
        String tolerates = "";
        String first = "";
        versions.sort(VersionlessFeatureDefinition::compareVersions);
        for (String version : versions) {
            if (first.equals("")) {
                first = version;
            } else {
                tolerates += version + ",";
            }
        }

        if (tolerates.equals("")) {
            return new String[] { first };
        }

        tolerates = tolerates.substring(0, tolerates.length() - 1);

        return new String[] { first, tolerates };
    }

    public ArrayList<String> getAllVersions() {
        ArrayList<String> versions = new ArrayList<String>();

        for (String[] featAndPlat : featuresAndPlatformAndKind) {
            if (!versions.contains(featAndPlat[0].split("-")[1])) {
                versions.add(featAndPlat[0].split("-")[1]);
            }
        }

        return versions;
    }

    /**
     * Gets every version of a dependency that a versioned feature uses.
     * Ex. Servlet-4.0 can be used on mp1.0, mp-1.1, mp1.2, mp1.3 and so on.
     * This gets the lowest version of the dependency at index 0. For the above example it would be "1.0"
     * This gathers all versions, except the lowest, of the dependency at index 1.
     * For the above example it would be "1.1,1.2,1.3"
     *
     * @return
     */
    public String[] getAllDependencyVersions(String versionedFeature, String dependency) {
        String result = "";
        String currentLow = "" + Double.MAX_VALUE;

        for (int i = 0; i < featuresAndPlatformAndKind.size(); i++) {
            String[] fnp = featuresAndPlatformAndKind.get(i);
            if (fnp[0].equals(versionedFeature)) {
                if (fnp[1].contains(dependency + "-")) {
                    //Should leave you with "(version number).feature" ex. "1.2"
                    String temp = fnp[1].split("-")[1];
                    int compare = compareVersions(currentLow, temp);
                    if (compare == 1) {
                        if (!currentLow.equals("" + Double.MAX_VALUE)) {
                            result += currentLow + ",";
                        }
                        currentLow = temp;
                    } else if (compare == -1) {
                        result += temp + ",";
                    }
                    //We ignore any instance where the versions are the same, as this means its a repeat
                }
            }
        }
        if (result.length() > 0) {
            result = result.substring(0, result.length() - 1);
        }
        return new String[] { currentLow, result };
    }

    /**
     * Compares the two versions. If version 1 is higher it will return true.
     * Otherwise it will return false
     *
     * @param version1
     * @param version2
     * @return
     */
    public static int compareVersions(String version1, String version2) {
        if (Double.parseDouble(version1) > Double.parseDouble(version2)) {
            return 1;
        } else if (Double.parseDouble(version1) == Double.parseDouble(version2)) {
            return 0;
        }

        return -1;
    }

    /**
     * @return the kind
     */
    public String getKind() {
        String kind = "noship";
        for (int i = 0; i < featuresAndPlatformAndKind.size(); i++) {
            if ("ga".equals(kind)) {
                break;
            }
            String[] fnp = featuresAndPlatformAndKind.get(i);
            String versionKind = fnp[3];
            // If current kind is noship, then just set it to what versionKind is.  It is going to either be noship, beta or ga
            // If version kind is ga, then set kind to version kind.  Otherwise leave it be beta or ga because it wouldn't change
            if ("noship".equals(kind) || "ga".equals(versionKind)) {
                kind = versionKind;
            }
        }
        return kind;
    }
}