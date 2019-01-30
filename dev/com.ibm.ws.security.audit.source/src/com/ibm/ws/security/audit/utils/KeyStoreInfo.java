/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.utils;

/**
 *
 */
import java.io.Serializable;

import javax.management.ObjectName;

public class KeyStoreInfo implements Serializable {

    public KeyStoreInfo() {
        name = null;
        location = null;
        password = null;

        //Need to do a lookup to see if FIPS is enabled
        provider = "IBMJCE";
        type = "JKS";
        fileBased = Boolean.TRUE;
        hostList = null;
        scopeName = null;
        scopeNameString = null;
        readOnly = Boolean.FALSE;
        initializeAtStartup = Boolean.FALSE;
        stashFile = Boolean.FALSE;
        customProvider = null;
        slot = null;
        accelerator = Boolean.FALSE;
        customProps = null;
        description = null;
        usage = null;
    }

    public KeyStoreInfo(String _name, String _location, String _password,
                        String _provider, String _type, Boolean _fileBased,
                        String _hostList, String _scopeNameString,
                        ObjectName _scopeName, Boolean _readOnly,
                        Boolean _initializeAtStartup, Boolean _stashFile,
                        String _customProvider, Integer _slot,
                        Boolean _accelerator, java.util.List _customProps,
                        String _description) {
        name = _name;
        location = _location;
        password = _password;
        provider = (_provider == null) ? "IBMJCE" : _provider;
        type = (_type == null) ? "JKS" : _type;
        fileBased = _fileBased;
        hostList = _hostList;
        scopeName = _scopeName;
        scopeNameString = _scopeNameString;
        readOnly = _readOnly;
        initializeAtStartup = _initializeAtStartup;
        stashFile = _stashFile;
        customProvider = _customProvider;
        slot = _slot;
        accelerator = _accelerator;
        customProps = _customProps;
        description = _description;
    }

    public void setName(String _name) {
        name = _name;
    }

    public void setLocation(String _location) {
        location = _location;
    }

    public void setPassword(String _password) {
        password = _password;
    }

    public void setType(String _type) {
        type = _type;
    }

    public void setProvider(String _provider) {
        provider = _provider;
    }

    public void setFileBased(Boolean _fileBased) {
        fileBased = _fileBased;
    }

    public void setHostList(String _hostList) {
        hostList = _hostList;
    }

    public void setScopeName(ObjectName _scopeName) {
        scopeName = _scopeName;
    }

    public void setScopeNameString(String _scopeNameString) {
        scopeNameString = _scopeNameString;
    }

    public void setReadOnly(Boolean _readOnly) {
        readOnly = _readOnly;
    }

    public void setInitializeAtStartup(Boolean _initializeAtStartup) {
        initializeAtStartup = _initializeAtStartup;
    }

    public void setStashFile(Boolean _stashFile) {
        stashFile = _stashFile;
    }

    public void setCustomProvider(String _customProvider) {
        customProvider = _customProvider;
    }

    public void setSlot(Integer _slot) {
        slot = _slot;
    }

    public void setAccelerator(Boolean _accelerator) {
        accelerator = _accelerator;
    }

    public void setCustomProps(java.util.List _customProps) {
        customProps = _customProps;
    }

    public void setDescription(String _description) {
        description = _description;
    }

    public void setUsage(String _usage) {
        usage = _usage;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getPassword() {
        return password;
    }

    public String getProvider() {
        return provider;
    }

    public String getType() {
        return type;
    }

    public Boolean getFileBased() {
        return fileBased;
    }

    public String getHostList() {
        return hostList;
    }

    public ObjectName getScopeName() {
        return scopeName;
    }

    public String getScopeNameString() {
        return scopeNameString;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public Boolean getInitializeAtStartup() {
        return initializeAtStartup;
    }

    public Boolean getStashFile() {
        return stashFile;
    }

    public String getCustomProvider() {
        return customProvider;
    }

    public java.util.List getCustomProps() {
        return customProps;
    }

    public Boolean getAccelerator() {
        return accelerator;
    }

    public Integer getSlot() {
        return slot;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        StringBuffer passwordMask = new StringBuffer();

        sb.append("\tKeyStoreInfo:");
        sb.append("\n\t").append("name = ").append(name);
        sb.append("\n\t").append("location = ").append(location);
        if (password != null) {
            for (int i = 0; i < password.length(); i++) {
                passwordMask.append("*");
            }
        } else {
            passwordMask.append("<null>");
        }
        sb.append("\n\t").append("password = ").append(passwordMask);
        sb.append("\n\t").append("provider = ").append(provider);
        sb.append("\n\t").append("type = ").append(type);

        String value = null;
        if (fileBased == null) {
            value = "<null>";
        } else if (fileBased.booleanValue()) {
            value = "yes";
        } else {
            value = "no";
        }
        sb.append("\n\t").append("fileBased = ").append(value);
        sb.append("\n\t").append("hostList = ").append(hostList);
        sb.append("\n\t").append("scopeName = ").append(scopeNameString);

        if (readOnly == null) {
            value = "<null>";
        } else if (readOnly.booleanValue()) {
            value = "yes";
        } else {
            value = "no";
        }
        sb.append("\n\t").append("readOnly = ").append(value);
        if (initializeAtStartup == null) {
            value = "<null>";
        } else if (initializeAtStartup.booleanValue()) {
            value = "yes";
        } else {
            value = "no";
        }
        sb.append("\n\t").append("initializeAtStartup = ").append(value);
        if (stashFile == null) {
            value = "<null>";
        } else if (stashFile.booleanValue()) {
            value = "yes";
        } else {
            value = "no";
        }
        sb.append("\n\t").append("stashFile = ").append(value);
        sb.append("\n\t").append("customProvider = ").append(customProvider);
        if (slot != null) {
            value = slot.toString();
        } else {
            value = "<null>";
        }
        sb.append("\n\t").append("slot = ").append(value);
        if (customProps != null) {
            for (int j = 0; j < customProps.size(); j++) {
                sb.append("\n\t").append("customProps=").append(customProps.get(j));
            }
        } else {
            sb.append("\n\t").append("customProps=<null>");
        }
        sb.append("\n\t").append("description = ").append(description);
        sb.append("\n\t").append("usage = ").append(usage);

        return sb.toString();
    }

    private String name;
    private String location;
    private String password;
    private String provider;
    private String type;
    private Boolean fileBased;
    private String hostList;
    private ObjectName scopeName;
    private String scopeNameString;
    private Boolean readOnly;
    private Boolean initializeAtStartup;
    private Boolean stashFile;
    private String customProvider;
    private Integer slot;
    private Boolean accelerator;
    private java.util.List customProps;
    private String description;
    private String usage;
}