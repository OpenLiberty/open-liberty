/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Holder for fields that will be used for interactions with a feature.</p>
 * 
 * <p>This class only relies on the IBM-Shortname, i.e. the name as used in server.xml, and all other items
 * are related to this value. To set up another feature in this way you need to adhere to the following:</p>
 * <ul>
 * <li>set the Subsystem-SymbolicName to test.icons.<i>shortName</i></li>
 * <li>any ESA file relating to the feature must match the Subsystem-SymbolicName, and be in the
 * testFeatures folder</li>
 * <li>the feature should contain a test wab whose name must match the Subsystem-SymbolicName, In addition,
 * the test wab must be available on the URL <i>shortName</i>/test, and should return the
 * <i>shortName</i> from that URL</li>
 * </ul>
 */
public class IconFeature {

    private final String shortName;

    List<String> expectedIconFiles = new ArrayList<String>();
    List<String> unexpectedIconFiles = new ArrayList<String>();

    private String namespaceQualifier = "";
    private String namespaceRoot = "";

    /**
     * Initialise an IconFeature
     * 
     * @param shortName the value of IBM-Shortname. For a user feature, this should not include the
     *            <i>usr:</i> prefix, but you should call setUserFeature(true) on the returned object.
     */
    public IconFeature(String shortName) {
        this.shortName = shortName;
    }

    /**
     * @return the Subsystem-SymbolicName, which will effectively be test.icons.<i>shortName</i>
     */
    public String getSymbolicName() {
        return "test.icons." + shortName;
    }

    /**
     * @return the list of expected Icons. The will never be null but may be empty
     */
    public List<String> getExpectedIcons() {
        return expectedIconFiles;
    }

    /**
     * @return the list of unexpected Icons (i.e. Icon Files that should not appear in the image being tested). The will never be null but may be empty
     */
    public List<String> getUnexpectedIcons() {
        return unexpectedIconFiles;
    }

    /**
     * @return the location in the autoFVT folder where the esa file will be located
     */
    public String getAutoFVTLocation() {
        return "testFeatures/" + getEsaFile();
    }

    /**
     * @return the name of the ESA file, which should be the value of Subsystem-SymbolicName with the .esa suffix
     */
    public String getEsaFile() {
        return getSymbolicName() + ".esa";
    }

    /**
     * @return the URL of the features WAB that can be accessed to verify the feature installed ok.
     */
    public String getURL() {
        return "/" + getShortName() + "/test";
    }

    /**
     * Gets the IBM-Shortname value. This won't handle the namespacing if this is a user feature, e.g. for
     * adding into server.xml. For that, use getNamespacedShortname().
     * 
     * @return the shortName of the feature, as provided to the class. This should match the feature's
     *         IBM-Shortname
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Add a file that will be expected to be included in the image to be tested.
     * 
     * @param filePath the path as specified in the feature manifest
     */
    public void addExpectedIcon(String filePath) {
        expectedIconFiles.add(getIconFolder() + "/" + filePath);
    }

    /**
     * @return the folder inside the image to be tested where all icons are expected to go
     */
    public String getIconFolder() {
        return namespaceRoot + "lib/features/icons/" + getSymbolicName();
    }

    /**
     * Add a file that will be expected not to be included in the image to be tested.
     * 
     * @param filePath the path as specified in the feature manifest
     */
    public void addUnexpectedIcon(String filePath) {
        unexpectedIconFiles.add(getIconFolder() + "/" + filePath);
    }

    /**
     * @return the location of the feature manifest inside the image. the manifest should be named the
     *         same as the IBM-Shortname
     */
    public String getFeatureManifest() {
        return namespaceRoot + "lib/features/" + getShortName() + ".mf";
    }

    /**
     * @return the location of the feature's test bundle within the image to test. The bundle should be a
     *         jar named after the Subsystem-Symbolicname of the feature.
     */
    public String getFeatureBundle() {
        return namespaceRoot + "lib/" + getSymbolicName() + "_1.0.0.jar";
    }

    /**
     * Specify that this is a user feature. Once called, files will be assumed to go into the
     * usr/extension folder, and the feature name for the server.xml will be prefixed with usr:
     */
    public void markAsUserFeature() {
        namespaceRoot = "usr/extension/";
        namespaceQualifier = "usr:";
    }

    /**
     * @return the feature name, plus any namespace prefix. Use this to specify the feature name in
     *         server.xml
     */
    public String getNamespacedShortName() {
        return namespaceQualifier + getShortName();
    }

}
