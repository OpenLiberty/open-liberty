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
package com.ibm.ws.jaxws.support;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.metadata.JaxWsClientMetaData;
import com.ibm.ws.jaxws.metadata.JaxWsModuleMetaData;
import com.ibm.ws.jaxws.metadata.JaxWsServerMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Manager to manage the JaxWsModuleMetaData in the jaxwsModuleSlot of a ModuleMetaData.
 */
public class JaxWsMetaDataManager {

    private static final TraceComponent tc = Tr.register(JaxWsMetaDataManager.class);

    static MetaDataSlot jaxwsApplicationSlot = null;
    static MetaDataSlot jaxwsModuleSlot = null;
    static MetaDataSlot jaxwsComponentSlot = null;

    public static void setJaxWsModuleMetaData(ModuleMetaData mmd, JaxWsModuleMetaData moduleMetaData) {
        if (mmd != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting ModuleMetaData on ModuleMetaData instance: " + mmd);
            }
            mmd.setMetaData(jaxwsModuleSlot, moduleMetaData);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not set ModuleMetaData because ModuleMetaData " + "is null");
            }
        }

    }

    public static JaxWsModuleMetaData getJaxWsModuleMetaData(ModuleMetaData mmd) {
        if (mmd != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Getting JaxWs module metadata from module metadata instance: " + mmd);
            }

            return (JaxWsModuleMetaData) mmd.getMetaData(jaxwsModuleSlot);
        }
        return null;
    }

    /**
     * Get the JaxWsModuleMetaData from the current associated moduleMetaData
     * 
     * @return
     */
    public static JaxWsModuleMetaData getJaxWsModuleMetaData() {
        return getJaxWsModuleMetaData(getModuleMetaData());
    }

    /**
     * This method will be called to retrieve client metadata from the module
     * metadata slot for a given WAR module.
     * 
     */
    public static JaxWsClientMetaData getJaxWsClientMetaData(ModuleMetaData mmd) {

        JaxWsClientMetaData clientMetaData = null;
        if (mmd != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Getting client metadata from module metadata instance: " + mmd);
            }

            JaxWsModuleMetaData moduleMetaData = (JaxWsModuleMetaData) mmd.getMetaData(jaxwsModuleSlot);
            if (moduleMetaData != null) {
                clientMetaData = moduleMetaData.getClientMetaData();
            }
        }

        return clientMetaData;
    }

    /**
     * Method to retrieve the ServerMetaData based on ModuleMetaData. This
     * method retrieves the module name from the ModuleMetaData and delegates to
     * the (String, ApplicationMetaData) version of this method below.
     */
    public static JaxWsServerMetaData getJaxWsServerMetaData(ModuleMetaData mmd) {

        JaxWsServerMetaData serverMetaData = null;
        if (mmd != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Getting client metadata from module metadata instance: " + mmd);
            }

            JaxWsModuleMetaData moduleMetaData = (JaxWsModuleMetaData) mmd.getMetaData(jaxwsModuleSlot);
            if (moduleMetaData != null) {
                serverMetaData = moduleMetaData.getServerMetaData();
            }
        }

        return serverMetaData;
    }

    /**
     * This method will be called to retrieve client metadata from the module
     * metadata slot for a given WAR module.
     * 
     */
    public static JaxWsClientMetaData getJaxWsClientMetaData() {
        ModuleMetaData mmd = getModuleMetaData();
        //146981
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "mmd: " + mmd);
        return getJaxWsClientMetaData(mmd);

    }

    /**
     * Gets the metadata for the module
     */
    public static ModuleMetaData getModuleMetaData() {
        ComponentMetaData cmd = getComponentMetaData();
        ModuleMetaData mmd = null;

        if (cmd != null) {
            mmd = cmd.getModuleMetaData();
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "ModuleMetaData object is " + (mmd != null ? mmd.toString() : "null!"));
        }

        return mmd;
    }

    /**
     * Gets the metadata object for the application
     */
    public static ComponentMetaData getComponentMetaData() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        return cmd;
    }

}
