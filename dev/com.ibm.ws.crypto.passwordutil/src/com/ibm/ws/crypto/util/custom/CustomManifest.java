/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.util.custom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.crypto.util.MessageUtils;
import com.ibm.ws.kernel.provisioning.ProductExtension;
import com.ibm.ws.kernel.provisioning.ProductExtensionInfo;

/**
 * A class to store the information of the custom encryption class.
 * The class is instantiated by using the extension manifest file.
 * The design document can be found at RTC WI 179580. Please review the page 9 to 12 of the WAD document.
 * The URL for the WI is as follows:
 * https://wasrtc.hursley.ibm.com:9443/jazz/resource/itemName/com.ibm.team.workitem.WorkItem/179580
 *
 * The extension manifest file can be located underneath wlp/bin/tools/extensions/ws-customPasswordEncryption, or 
 * The underneath bin/tools/extensions/ws-customPasswordEncryption directory of any product extension directories.
 * An example location of the extension manifest file is:
 *    C:/liberty/wlp/bin/tools/extensions/ws-customPasswordEncryption/customEncryption.jar
 * An example contents of the extension manifest file:
 * -------------------------
 *  Require-Bundle: com.ibm.websphere.crypto.sample.customencryption; version="[1,1.0.100)"; location="usr/extension/lib"
 *  IBM-ImplementationClass: com.ibm.websphere.crypto.sample.customencryption.CustomEncryptionImpl
 * -------------------------
 *
 * An example of feature manifest file:
 * -------------------------           
 *  Manifest-Version: 1.0
 *  IBM-Feature-Version: 2
 *  IBM-ShortName: customEncryption-1.0
 *  Subsystem-SymbolicName: customEncryption-1.0;visibility:=public
 *  Subsystem-Content: 
 *   com.ibm.websphere.crypto.sample.customencryption; version="[1,1.0.100)"; start-phase:="SERVICE_EARLY",
 *   customEncryption.jar; location:="bin/tools/extensions/ws-customPasswordEncryption/customEncryption.jar"; type=file
 *  Subsystem-Description: %description
 * -------------------------
 *
 * The location of the extension manifest file (in the above example, bin/tools/extensions/ws-customPasswordEncryption/customEncryption.jar)
 * should match the location of the extension manifest file.
 * To find out the corresponding feature manifest from the extension manifest, the following is carried out:
 * 1. enumerate all of feature manifest files underneath user/extension/lib/features, lib/features directoty of the any product extension directory.
 * 2. read Subsystem-Content.
 * 3. find out a file of which type is file, then find the location of the file (if type is file, the location element should exist).
 * 4. if there is a file, compare the canonical path of the location of the extension manifest file.
 * 5. if they match, then this feature manifest file is the feature for the custom password encryption.
 *
 */
public class CustomManifest {
    private static final Class<?> CLASS_NAME = CustomManifest.class;
    private static final String RB = "com.ibm.ws.crypto.util.internal.resources.Messages";
    private final static Logger logger = Logger.getLogger(CLASS_NAME.getCanonicalName(), RB);

    private final String extensionJarName; // manifest file name (i.e., customEncryption.jar)
    private final String extensionJarLocation; // extension manifest jar location. this is the canonical path. (i.e., /user/local/wlp/bin/tools/extensions/ws-customPasswordEncryption/customEncryption.jar)
    private final String implClassName; // implementation class name
    private final String algorithmName; // algorithm name (i.e., customAES)
    private final String featureDescription; // description in the feature manifest file.
    private final String featureName; // feature name. if short name is available, short name. otherwise symbolic name.
    private final String featureSymbolicName; // feature symbolic name.
    private final String featureId; // feature id name (i.e., usr or myExt)
    private static final String DEFAULT_ALGORITHM = "custom";
    private static final String DEFAULT_DESC = "Custom password encryption";
    private static final String MANIFEST_EXT = ".mf";
    private static final String HDR_IMPL_CLASS = "IBM-ImplementationClass";
//    private static final String HDR_REQ_BUNDLE = "Require-Bundle";
    // not used. private static final String HDR_CUSTOM_NAME = "IBM-CustomPasswordEncryptionName";
    private static final String HDR_SUB_CONTENT = "Subsystem-Content";
    private static final String HDR_SUB_SYM_NAME = "Subsystem-SymbolicName";
    private static final String HDR_SUB_SHORT_NAME = "IBM-ShortName";
    private static final String HDR_SUB_DESC = "Subsystem-Description";
    private static final String HDR_ATTR_LOCATION = "location";
    private static final String HDR_ATTR_TYPE = "type";
    private static final String HDR_ATTR_VALUE_FILE = "file";
    private static final String FEATURE_DIR = "lib/features";

    // this is for unittest
    protected CustomManifest() {
        extensionJarName = null;
        extensionJarLocation = null;
        algorithmName = null;
        implClassName = null;
        featureDescription = null;
        featureName = null;
        featureSymbolicName = null;
        featureId = null;
    }

    /**
     * constructor.
     * 
     * @param file the location of the extension manifest file.
     * @throws IOException if a file operation is failed.
     * @throws IllegalArgumentException when the required attribute is not found.
     */
    public CustomManifest(final File file) throws IOException, IllegalArgumentException {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("extension manifest file : " + file);
        }
        Attributes attrs = loadExtensionJarFile(file);
        implClassName = attrs.getValue(HDR_IMPL_CLASS);
        if (implClassName == null) {
            throw new IllegalArgumentException(MessageUtils.getMessage("PASSWORDUTIL_ERROR_MISSING_HEADER", HDR_IMPL_CLASS, file));
        }
        extensionJarName = file.getName();
        extensionJarLocation = CustomUtils.getCanonicalPath(file);
        // 2Q1016 release only supports one custom password encryption. therefore the algorithm name is always custom.
        // in order to support any name, need to figure out how to get the value under the runtime environment. maybe
        // properties in the user feature needs to carry the data.
        algorithmName = DEFAULT_ALGORITHM;
        File featureManifest = findFeatureManifest();

        if (featureManifest != null) {
            Attributes fattrs = getAttributes(featureManifest);
            featureSymbolicName = getFeatureSymbolicName(fattrs);
            featureName = getFeatureName(fattrs);
            String desc = getLocalizedString(featureManifest, fattrs.getValue(HDR_SUB_DESC));
            // this is optional, if it's not set, use default value.
            if (desc == null) {
                desc = DEFAULT_DESC;
            }
            featureDescription = desc;
            featureId = getFeatureId(featureManifest);
        } else {
            throw new IllegalArgumentException(MessageUtils.getMessage("PASSWORDUTIL_ERROR_NO_FEATURE_MANIFEST", extensionJarLocation));
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(toString());
        }
    }

    public String getName() {
        return extensionJarName;
    }

    public String getLocation() {
        return extensionJarLocation;
    }

    public String getImplClass() {
        return implClassName;
    }

    public String getAlgorithm() {
        return algorithmName;
    }

    public String getFeatureName() {
        return featureName;
    }

    public String getFeatureId() {
        return featureId;
    }

    public String getDescription() {
        return featureDescription;
    }

    /**
     * find the feature manifest file which contains the location of the extension manifest
     * file as Subsystem-Content.
     * the directories which are searched are: wlp/user/extensions/lib/features and all
     * of the product extension directories.
     * 
     * @throws IOException
     */
    protected File findFeatureManifest() throws IOException {
        for (File root : CustomUtils.listExtensionDirectories()) {
            File[] files = CustomUtils.listFiles(new File(root, FEATURE_DIR));
            if (files != null) {
                for (File file : files) {
                    if (CustomUtils.isFile(file) && file.getName().toLowerCase().endsWith(MANIFEST_EXT)) {
                        List<String> fileLocations = getFileLocationsFromSubsystemContent(getSubsystemContent(file));
                        for (String fileLocation : fileLocations) {
                            for (File jarRoot : CustomUtils.listRootAndExtensionDirectories()) {
                                if (getCanonicalPath(jarRoot, fileLocation).equals(extensionJarLocation)) {
                                    return file;
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * returns the list of location attribute of which type is file in the given text of Subsystem-Content.
     * the location may or may not be an absolute path. If there is not a mathing data, returns empty List.
     */
    protected List<String> getFileLocationsFromSubsystemContent(String subsystemContent) {
        String sc = subsystemContent + ",";
        List<String> files = new ArrayList<String>();
        boolean isFile = false;
        String location = null;
        int strLen = sc.length();
        boolean quoted = false;
        for (int i = 0, pos = 0; i < strLen; i++) {
            char c = sc.charAt(i);
            if (!quoted && (c == ';' || c == ',')) {
                String str = sc.substring(pos, i);
                pos = i + 1;
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("element : " + str);
                }
                if (str.contains(":=")) {
                    if (getKey(str).equals(HDR_ATTR_LOCATION)) {
                        location = getValue(str);
                    }
                } else if (str.contains("=")) {
                    if (getKey(str).equals(HDR_ATTR_TYPE) && getValue(str).equals(HDR_ATTR_VALUE_FILE)) {
                        isFile = true;
                    }
                }
                if (c == ',') { // the delimiter of each subsystem content.
                    if (isFile && location != null) {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("location : " + location);
                        }
                        files.add(location);
                    }
                    location = null;
                    isFile = false;
                }
            } else if (c == '\"') {
                quoted = !quoted;
            }
        }
        return files;
    }

    protected String getSubsystemContent(File file) throws IOException {
        return getAttributes(file).getValue(HDR_SUB_CONTENT);
    }

    /**
     * returns the contents of manifest file (.mf) as the Attribute object.
     */
    protected Attributes getAttributes(final File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = AccessController.doPrivileged(new PrivilegedExceptionAction<FileInputStream>() {
                @Override
                public FileInputStream run() throws IOException {
                    return new FileInputStream(file);
                }
            });
        } catch (PrivilegedActionException pae) {
            throw (IOException) pae.getException();
        }
        return new Manifest(fis).getMainAttributes();
    }

    /**
     * Find corresponding feature id (i.e., user or myExt1 from the location of feature manifest file.
     * The logic is that since the location of the feature manifest file is .../lib/features/<feature manifest>
     * find the parent of lib directory, and compare the location information of the ProductionExtension
     * objects, and if they match, get the corresponding feature id from the ProductExtension object
     * and return it. If there is no matching id, return null.
     * 
     * @throws IOException
     */
    protected static String getFeatureId(File featureManifest) throws IOException {
        String rootDir = CustomUtils.getCanonicalPath(featureManifest.getParentFile().getParentFile().getParentFile()); // go up two directories.
        String output = null;
        // check with user feature dir.
        if (rootDir.equals(CustomUtils.getCanonicalPath(new File(CustomUtils.getInstallRoot(), CustomUtils.USER_FEATURE_DIR)))) {
            output = "usr";
        } else {
            for (ProductExtensionInfo info : ProductExtension.getProductExtensions()) {
                File extensionDir = new File(info.getLocation());
                if (!CustomUtils.isAbsolute(extensionDir)) {
                    File parentDir = new File(CustomUtils.getInstallRoot()).getParentFile();
                    extensionDir = new File(parentDir, info.getLocation());
                }
                if (rootDir.equals(CustomUtils.getCanonicalPath(extensionDir))) {
                    output = info.getProductID();
                    break;
                }
            }
        }
        return output;
    }

    /**
     * returns the key value of key = value, or key := value pair.
     */
    protected String getKey(String str) {
        int index = str.indexOf('=');
        String key = null;
        if (index > 0) {
            key = str.substring(0, index).trim();
            if (key.charAt(key.length() - 1) == ':') {
                key = key.substring(0, key.length() - 1).trim();
            }
        }
        return key;
    }

    /**
     * returns the value of key = value pair.
     * if the value is quoted, the quotation characters are stripped.
     */
    protected String getValue(String str) {
        int index = str.indexOf('=');
        String value = null;
        if (index > 0) {
            value = str.substring(index + 1).trim();
            if (value.charAt(0) == '\"') {
                value = value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    /**
     * returns the feature symbolic name from Attribute object.
     */
    protected String getFeatureSymbolicName(Attributes attrs) {
        String output = null;
        if (attrs != null) {
            // get Subsystem-SymbolicName
            String value = attrs.getValue(HDR_SUB_SYM_NAME);
            if (value != null) {
                String[] parts = value.split(";");
                if (parts.length > 0) {
                    output = parts[0].trim();
                }
            }
        }
        return output;
    }

    /**
     * returns the feature name from Attribute object.
     * if IBM-ShortName exists, use this value, otherwise use the symbolic name.
     */
    protected String getFeatureName(Attributes attrs) {
        String output = null;
        if (attrs != null) {
            // get IBM-ShortName first,
            String value = attrs.getValue(HDR_SUB_SHORT_NAME);
            if (value != null) {
                output = value.trim();
            } else {
                // get Subsystem-SymbolicName
                output = getFeatureSymbolicName(attrs);
            }
        }
        return output;
    }

    /**
     * returns the localized string of specified value.
     * the localization file supposes to be located the l10n directory of the location
     * where the feature manifest file exists, and the name of the resource file is
     * feature symbolic name + _ + locale + .properties.
     * prior to call this method, featureSymbolicName field needs to be set.
     */
    protected String getLocalizedString(File featureManifest, String value) {
        if (value != null && value.startsWith("%")) {
            ResourceBundle res = CustomUtils.getResourceBundle(new File(featureManifest.getParentFile(), "l10n"), featureSymbolicName, Locale.getDefault());
            if (res != null) {
                String loc = res.getString(value.substring(1));
                if (loc != null) {
                    value = loc;
                }
            }
        }
        return value;
    }

    protected String getCanonicalPath(File root, String fileName) throws IOException {
        File file = new File(fileName);
        if (!CustomUtils.isAbsolute(file)) {
            file = new File(root, fileName);
        }
        return CustomUtils.getCanonicalPath(file);
    }

    private Attributes loadExtensionJarFile(final File file) throws IOException {
        JarFile jar = null;
        try {
            jar = AccessController.doPrivileged(new PrivilegedExceptionAction<JarFile>() {
                @Override
                public JarFile run() throws IOException {
                    return new JarFile(file);
                }
            });
        } catch (PrivilegedActionException pae) {
            throw (IOException) pae.getException();
        }
        Attributes attrs = jar.getManifest().getMainAttributes();
        jar.close();
        return attrs;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(" extensionJarName: ").append(extensionJarName);
        sb.append(" extensionJarLocation: ").append(extensionJarLocation);
        sb.append(" implClassName: ").append(implClassName);
        sb.append(" algorithmName: ").append(algorithmName);
        sb.append(" featureDescription: ").append(featureDescription);
        sb.append(" featureSymbolicName: ").append(featureSymbolicName);
        sb.append(" featureName: ").append(featureName);
        sb.append(" featureId: ").append(featureId);
        return sb.toString();
    }
}