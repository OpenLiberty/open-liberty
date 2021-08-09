/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.test.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.ForeignBusDefinition;
import com.ibm.ws.sib.admin.JsBus;
import com.ibm.ws.sib.admin.JsEObject;
import com.ibm.ws.sib.admin.JsMEConfig;
import com.ibm.ws.sib.admin.JsPermittedChainUsage;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
import com.ibm.ws.sib.processor.test.SIMPJsStandaloneEngineImpl;
import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * class that provides a mock-up local bus object
 * 
 * @author millwood
 */
public class MPBus implements JsBus, JsEObject {
    HashMap<String, ForeignBusDefinition> foreignBuses = new HashMap<String, ForeignBusDefinition>();
    HashMap<String, VirtualLinkDefinition> links = new HashMap<String, VirtualLinkDefinition>();
    SIBUuid8 uuid = new SIBUuid8();
    String name = "TEST_BUS";
    private final SIMPJsStandaloneEngineImpl engine;
    public boolean reReadProperties = true;

    public MPBus(SIMPJsStandaloneEngineImpl engine) {
        this.engine = engine;
    }

    public Object getEObject() {
        return null;
    }

    public String[] getAttributeNames() {
        return null;
    }

    public String getAttribute(String name) {
        return "false";
    }

    public Map getChildren() {
        return null;
    }

    public JsEObject getParent() {
        return null;
    }

    public String getCustomProperty(String name) {
        return null;
    }

    public boolean getBoolean(String name, boolean defaultValue) {
        return false;
    }

    public List getBooleanList(String name) {
        return new ArrayList();
    }

    public int getInt(String name, int defaultValue) {
        return 0;
    }

    public List getIntList(String name) {
        return new ArrayList();
    }

    public long getLong(String name, long defaultValue) {
        return 0;
    }

    public List getLongList(String name) {
        return new ArrayList();
    }

    public float getFloat(String name, float defaultValue) {
        return 0;
    }

    public List getFloatList(String name) {
        return new ArrayList();
    }

    public String getString(String name, String defaultValue) {
        return "";
    }

    public List getStringList(String name) {
        return new ArrayList();
    }

    /**
     * Clear out the hash maps
     */
    public synchronized void clear() {
        foreignBuses = new HashMap<String, ForeignBusDefinition>();
        links = new HashMap<String, VirtualLinkDefinition>();
        uuid = new SIBUuid8();
    }

    /**
     * Is the bus configured as secure?
     * 
     * @return
     */
    public boolean isSecure() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsBus#getName()
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsBus#getUuid()
     */
    public SIBUuid8 getUuid() {
        return uuid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsBus#getForeignBus(java.lang.String)
     */
    public ForeignBusDefinition getForeignBus(String busName) {
        if (reReadProperties)
            engine.readPropertiesFile(); // we sometimes need this info before
        // loadLocalisations is called
        ForeignBusDefinition def = null;
        synchronized (this) {
            def = foreignBuses.get(busName);
        }
        return def;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsBus#getForeignBusForLink(java.lang.String)
     */
    public synchronized ForeignBusDefinition getForeignBusForLink(String uuid) {
        VirtualLinkDefinition virtualLinkDefinition = links.get(uuid);
        if (virtualLinkDefinition == null) {
            return null; // Should throw SIBExceptionNoLinkExists("no link")
        }
        return virtualLinkDefinition.getForeignBus();
    }

    public synchronized void setForeignBus(ForeignBusDefinition foreignBusDefinition) {
        foreignBuses.put(foreignBusDefinition.getName(), foreignBusDefinition);
    }

    public synchronized void setLink(VirtualLinkDefinition virtualLinkDefinition) {
        links
                        .put(virtualLinkDefinition.getUuid().toString(), virtualLinkDefinition);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsBus#getSIBDestination(java.lang.String,
     * java.lang.String)
     */
    public BaseDestinationDefinition getSIBDestination(String arg0, String arg1) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsBus#getSIBDestination(java.lang.String,
     * java.lang.String, com.ibm.ws.sib.admin.DestinationDefinition)
     */
    public void getSIBDestination(String arg0, String arg1,
                                  DestinationDefinition arg2) {}

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsBus#getSIBDestinationLocalitySet(java.lang.String,
     * java.lang.String)
     */
    public Set getSIBDestinationLocalitySet(String arg0, String arg1) {
        return null;
    }

    public Set getPermittedChains() {
        return Collections.EMPTY_SET;
    }

    public JsPermittedChainUsage getPermittedChainUsage() {
        return JsPermittedChainUsage.ALL;
    }

    public boolean isBootstrapAllowed() {
        return true;
    }

    public boolean isBusAuditAllowed() {
        return true;
    }

    public boolean useAdminDomain() {
        return false;
    }

    public JsMEConfig getLWMMEConfig() {
        return null;
    }

}
