/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;

public class KeystoreSettings extends BaseConfigSettings {

    private static final Class<?> thisClass = KeystoreSettings.class;

    public static final String CONFIG_ELEMENT_NAME = "keyStore";

    public static final String ATTR_KEYSTORE_ID = "id";
    public static final String ATTR_KEYSTORE_PASSWORD = "password";
    public static final String ATTR_KEYSTORE_LOCATION = "location";
    public static final String ATTR_KEYSTORE_TYPE = "type";
    public static final String ATTR_KEYSTORE_FILE_BASED = "fileBased";
    public static final String ATTR_KEYSTORE_READ_ONLY = "readOnly";
    public static final String ATTR_KEYSTORE_POLLING_RATE = "pollingRate";
    public static final String ATTR_KEYSTORE_UPDATE_TRIGGER = "updateTrigger";
    public static final String ATTR_KEYSTORE_KEY_ENTR = "keyEntr";

    private String id = null;
    private String password = null;
    private String location = null;
    private String type = null;
    private String fileBased = null;
    private String readOnly = null;
    private String pollingRate = null;
    private String updateTrigger = null;
    private String keyEntr = null;

    public KeystoreSettings() {
        configElementName = CONFIG_ELEMENT_NAME;
    }

    public KeystoreSettings(String id, String password, String location, String type, String fileBased, String readOnly, String pollingRate,
                            String updateTrigger, String keyEntr) {

        configElementName = CONFIG_ELEMENT_NAME;

        this.id = id;
        this.password = password;
        this.location = location;
        this.type = type;
        this.fileBased = fileBased;
        this.readOnly = readOnly;
        this.pollingRate = pollingRate;
        this.updateTrigger = updateTrigger;
        this.keyEntr = keyEntr;
    }

    @Override
    public KeystoreSettings createShallowCopy() {
        return new KeystoreSettings(id, password, location, type, fileBased, readOnly, pollingRate, updateTrigger, keyEntr);
    }

    @Override
    public KeystoreSettings copyConfigSettings() {
        return copyConfigSettings(this);
    }

    @Override
    public Map<String, String> getConfigAttributesMap() {
        String method = "getConfigAttributesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of keystore attributes");
        }
        Map<String, String> attributes = new HashMap<String, String>();

        attributes.put(ATTR_KEYSTORE_ID, getId());
        attributes.put(ATTR_KEYSTORE_PASSWORD, getPassword());
        attributes.put(ATTR_KEYSTORE_LOCATION, getLocation());
        attributes.put(ATTR_KEYSTORE_TYPE, getType());
        attributes.put(ATTR_KEYSTORE_FILE_BASED, getFileBased());
        attributes.put(ATTR_KEYSTORE_READ_ONLY, getReadOnly());
        attributes.put(ATTR_KEYSTORE_POLLING_RATE, getPollingRate());
        attributes.put(ATTR_KEYSTORE_UPDATE_TRIGGER, getUpdateTrigger());
        attributes.put(ATTR_KEYSTORE_KEY_ENTR, getKeyEntr());

        return attributes;
    }

    /********************************* Private member getters and setters *********************************/

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return this.password;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return this.location;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public void setFileBased(String fileBased) {
        this.fileBased = fileBased;
    }

    public String getFileBased() {
        return this.fileBased;
    }

    public void setReadOnly(String readOnly) {
        this.readOnly = readOnly;
    }

    public String getReadOnly() {
        return this.readOnly;
    }

    public void setPollingRate(String pollingRate) {
        this.pollingRate = pollingRate;
    }

    public String getPollingRate() {
        return this.pollingRate;
    }

    public void setUpdateTrigger(String updateTrigger) {
        this.updateTrigger = updateTrigger;
    }

    public String getUpdateTrigger() {
        return this.updateTrigger;
    }

    public void setKeyEntr(String keyEntr) {
        this.keyEntr = keyEntr;
    }

    public String getKeyEntr() {
        return this.keyEntr;
    }

    @Override
    public void printConfigSettings() {
        String thisMethod = "printConfigSettings";

        String indent = "  ";
        Log.info(thisClass, thisMethod, "Keystore config settings:");
        Log.info(thisClass, thisMethod, indent + ATTR_KEYSTORE_ID + ": " + id);
        Log.info(thisClass, thisMethod, indent + ATTR_KEYSTORE_PASSWORD + ": " + password);
        Log.info(thisClass, thisMethod, indent + ATTR_KEYSTORE_LOCATION + ": " + location);
        Log.info(thisClass, thisMethod, indent + ATTR_KEYSTORE_TYPE + ": " + type);
        Log.info(thisClass, thisMethod, indent + ATTR_KEYSTORE_FILE_BASED + ": " + fileBased);
        Log.info(thisClass, thisMethod, indent + ATTR_KEYSTORE_READ_ONLY + ": " + readOnly);
        Log.info(thisClass, thisMethod, indent + ATTR_KEYSTORE_POLLING_RATE + ": " + pollingRate);
        Log.info(thisClass, thisMethod, indent + ATTR_KEYSTORE_UPDATE_TRIGGER + ": " + updateTrigger);
        Log.info(thisClass, thisMethod, indent + ATTR_KEYSTORE_KEY_ENTR + ": " + keyEntr);
    }

}
