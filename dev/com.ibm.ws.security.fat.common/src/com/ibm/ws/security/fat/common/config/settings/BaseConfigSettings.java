/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.config.settings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonIOTools;

public abstract class BaseConfigSettings {

    private final static Class<?> thisClass = BaseConfigSettings.class;

    protected static boolean debug = false;

    public static final String BASE_CONFIG_SUFFIX = ".base";

    public static final String VAR_INCLUDE_FILE = "${include.file}";
    public static final String INCLUDE_ELEMENT = "<include location=\"" + VAR_INCLUDE_FILE + "\"/>";

    protected boolean isIncludeFile = false;
    protected String includeFileLocation = null;

    protected String configElementName = null;

    protected Map<String, BaseConfigSettings> childConfigSettings = new HashMap<String, BaseConfigSettings>();

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Creates a shallow copy of this object. The copy is shallow because the new object does not necessarily have the
     * same values for the member variables defined in BaseConfigSettings as the source object. If wishing to create an
     * exact duplicate of this settings class, use {@code  BaseConfigSettings.copyConfigSettings(...)}.
     * 
     * @return
     */
    abstract public BaseConfigSettings createShallowCopy();

    abstract public BaseConfigSettings copyConfigSettings();

    /**
     * Returns a map of variables to be replaced within a configuration file and the values to replace them. For instance,
     * a map might be returned with a "${test}" key mapped to "This is a test value." That map indicates that any "${test}"
     * variables within a configuration file should be replaced with the string "This is a test value."
     * 
     * @return
     */
    public Map<String, String> getConfigSettingsVariablesMap() {
        return null;
    }

    /**
     * Returns a map of configuration element attributes and their values for this particular configuration element.
     * 
     * @return
     */
    public Map<String, String> getConfigAttributesMap() {
        return null;
    }

    /**
     * Returns a deep copy of the provided settings. If settings is null, null is returned.
     * 
     * @param settings
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends BaseConfigSettings> T copyConfigSettings(T settings) {
        String method = "copyConfigSettings";
        if (debug) {
            Log.info(thisClass, method, "Copying config settings of class type: " + settings.getClass());
        }
        T copySettings = null;
        if (settings != null) {
            // Create new instance of the same sub-class that was passed in
            copySettings = (T) settings.createShallowCopy();

            // Set the settings that are common to all classes that extend BaseConfigSettings
            copySettings.includeFileLocation = settings.includeFileLocation;
            copySettings.isIncludeFile = settings.isIncludeFile();
            copySettings.configElementName = settings.configElementName;

            if (settings.hasChildConfigSettings()) {
                if (debug) {
                    Log.info(thisClass, method, "Copying " + settings.childConfigSettings.size() + " child config settings");
                }
                // Copy all child configuration element settings
                for (String key : settings.childConfigSettings.keySet()) {
                    BaseConfigSettings childSettings = settings.childConfigSettings.get(key);
                    if (debug) {
                        Log.info(thisClass, method, "Adding child element: " + key);
                    }
                    copySettings.childConfigSettings.put(key, copyConfigSettings(childSettings));
                }
            }

        }
        if (debug) {
            Log.info(thisClass, method, "Done copying settings for class type: " + settings.getClass());
        }
        return copySettings;
    }

    /**
     * Gets the only settings included in the provided map. If no, or more than 1, sets of settings are being
     * tracked, this will throw an exception.
     * 
     * @return
     * @throws Exception
     */
    public static <T extends BaseConfigSettings> T getDefaultSettings(Map<String, T> settings) throws Exception {
        if (settings == null || settings.size() != 1) {
            String reason = "no sets of settings were found";
            if (settings != null && settings.size() != 1) {
                reason = "an unexpected number of settings were found: " + settings.size();
            }
            throw new Exception("Expected to get default settings, but " + reason);
        }
        T defaultSettings = null;
        for (String id : settings.keySet()) {
            defaultSettings = settings.get(id);
        }
        return defaultSettings;
    }

    public void setIsIncludedFile(boolean isIncludeFile) {
        this.isIncludeFile = isIncludeFile;
    }

    /**
     * Sets the location of the include file to use for these settings. If null, the isIncludeFile variable will be set to
     * false to denote that an include file is not specified for these configuration settings. Otherwise, the isIncludeFile
     * variable is set to true.
     * 
     * @param fileLocation
     */
    public void setIncludeFileLocation(String fileLocation) {
        includeFileLocation = fileLocation;
        this.isIncludeFile = (fileLocation == null) ? false : true;
    }

    /**
     * Returns whether the configuration settings represented by this class are contained within a separate XML file meant
     * to be included in a server configuration. If true, a file location for that XML should be tracked by this class. If
     * false, the settings tracked by this class are meant to be output to, or consumed by, a server configuration file.
     * 
     * @return
     */
    public boolean isIncludeFile() {
        return this.isIncludeFile;
    }

    /**
     * Creates a key for the childConfigSettings map that uniquely identifies these particular settings. This key prevents
     * child elements of a particular config element from having the same id as other child elements of the same type (this
     * should not be allowed in Liberty configs anyway). However child elements of different types may have the same id
     * attribute value.
     * <br>
     * Example of a configuration that this supports:
     * <parentElement id="parent" ... >
     * <childElement id="myElement" ... />
     * <otherElement id="myElement" ... /> <!-- Same id as element above, but element is a different type -->
     * </parentElement>
     * 
     * Example of a configuration that is NOT supported:
     * <parentElement id="parent" ... >
     * <childElement id="myElement" ... />
     * <childElement id="myElement" ... /> <!-- Same id and element type as element above -->
     * </parentElement>
     * 
     * @param uniqueId id attribute of the config element
     * @param settings
     * @return
     */
    public static String buildChildElementKey(String uniqueId, BaseConfigSettings settings) {
        return settings.configElementName + "-" + uniqueId;
    }

    public void putChildConfigSettings(String configElementId, BaseConfigSettings settings) {
        String uniqueKey = buildChildElementKey(configElementId, settings);
        if (debug) {
            Log.info(thisClass, "putChildConfigSettings", "Adding child config settings: " + uniqueKey);
        }
        childConfigSettings.put(uniqueKey, settings);
    }

    public void removeChildConfigSettings(String configElementId, BaseConfigSettings settings) {
        String uniqueKey = buildChildElementKey(configElementId, settings);
        if (debug) {
            Log.info(thisClass, "putChildConfigSettings", "Removing child config settings: " + uniqueKey);
        }
        childConfigSettings.remove(uniqueKey);
    }

    /**
     * Returns whether there are child elements of these config settings.
     * 
     * @return
     */
    public boolean hasChildConfigSettings() {
        return (childConfigSettings != null && !childConfigSettings.isEmpty());
    }

    /**
     * Builds and returns a server configuration-consumable string of all of the configuration properties tracked by this
     * class. The appropriately named configuration element, attributes, and any sub-elements are created and returned in
     * the string, which can subsequently be placed into a server configuration file without modification.
     * 
     * @return
     */
    public String buildConfigOutput() {
        String method = "buildConfigOutput";
        if (debug) {
            Log.info(thisClass, method, "Building config output for config element: " + configElementName);
        }

        StringBuilder output = new StringBuilder();

        if (isIncludeFile()) {
            // Settings are contained in external file; build <include> element pointing to that file
            if (debug) {
                Log.info(thisClass, method, "Using include file: " + includeFileLocation);
            }
            output.append(buildIncludeElement(includeFileLocation));
        } else {
            output.append("<" + configElementName + " ");

            Map<String, String> attributes = getConfigAttributesMap();
            if (attributes != null) {
                for (String attribute : attributes.keySet()) {
                    String value = attributes.get(attribute);
                    if (value != null) {
                        output.append(attribute + "=\"" + attributes.get(attribute) + "\" ");
                    }
                }
            }

            if (!childConfigSettings.isEmpty()) {
                // Create sub-elements inside this config element for all of the child config settings 
                output.append(">" + CommonIOTools.NEW_LINE);

                for (String key : childConfigSettings.keySet()) {
                    if (debug) {
                        Log.info(thisClass, method, "Building output for child element: " + key);
                    }
                    BaseConfigSettings settings = childConfigSettings.get(key);
                    output.append(settings.buildConfigOutput());
                }

                output.append("</" + configElementName + ">" + CommonIOTools.NEW_LINE);
            } else {
                output.append("/>" + CommonIOTools.NEW_LINE);
            }

        }
        if (debug) {
            Log.info(thisClass, method, "Config output: " + CommonIOTools.NEW_LINE + output.toString());
        }
        return output.toString();
    }

    /**
     * Builds an include element for server configuration files with the location attribute set to {@code value}.
     * 
     * @param value
     * @return
     */
    public static String buildIncludeElement(String value) {
        String method = "buildIncludeElement";
        if (value == null || value.isEmpty()) {
            if (debug) {
                Log.info(thisClass, method, "No value provided; returning empty string");
            }
            return "";
        }
        if (debug) {
            Log.info(thisClass, method, "Building include element with value: " + value);
        }

        String includeElement = INCLUDE_ELEMENT;
        includeElement = includeElement.replace(VAR_INCLUDE_FILE, value);

        if (debug) {
            Log.info(thisClass, method, "Include element: " + includeElement);
        }
        return includeElement;
    }

    /**
     * Builds a string of new-line-separated include elements using the values provided in {@code values} to set each
     * respective element's location attribute.
     * 
     * @param values
     * @return
     */
    public static String buildIncludeElements(List<String> values) {
        String method = "buildIncludeElements";
        if (debug) {
            Log.info(thisClass, method, "Building a list of include elements");
        }

        StringBuilder elements = new StringBuilder();
        for (String value : values) {
            String includeElement = buildIncludeElement(value);
            elements.append(includeElement + CommonIOTools.NEW_LINE);
        }

        return elements.toString();
    }

    public void printConfigSettings() {
        String method = "printConfigSettings";
        if (isIncludeFile()) {
            String indent = "  ";
            Log.info(thisClass, method, indent + "isIncludeFile: " + isIncludeFile);
            Log.info(thisClass, method, indent + "includeFileLocation: " + includeFileLocation);
        }
    }

}
