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
package com.ibm.ws.jaxrs20.support;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.metadata.JaxRsClientMetaData;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;
import com.ibm.ws.jaxrs20.metadata.JaxRsServerMetaData;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.runtime.metadata.MetaDataSlot;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Manager to manage the JaxWsModuleMetaData in the jaxwsModuleSlot of a ModuleMetaData.
 */
public class JaxRsMetaDataManager {

    private static final TraceComponent tc = Tr.register(JaxRsMetaDataManager.class);

    public static MetaDataSlot jaxrsApplicationSlot = null;
    public static MetaDataSlot jaxrsModuleSlot = null;
    public static MetaDataSlot jaxrsComponentSlot = null;

    public static void setJaxRsModuleMetaData(ModuleMetaData mmd, JaxRsModuleMetaData moduleMetaData) {
        if (mmd != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Setting ModuleMetaData on ModuleMetaData instance: " + mmd);
            }
            mmd.setMetaData(jaxrsModuleSlot, moduleMetaData);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Could not set ModuleMetaData because ModuleMetaData " + "is null");
            }
        }

    }

    public static JaxRsModuleMetaData getJaxRsModuleMetaData(ModuleMetaData mmd) {
        if (mmd != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Getting JaxWs module metadata from module metadata instance: " + mmd);
            }

            return (JaxRsModuleMetaData) mmd.getMetaData(jaxrsModuleSlot);
        }
        return null;
    }

    /**
     * Get the JaxWsModuleMetaData from the current associated moduleMetaData
     * 
     * @return
     */
    public static JaxRsModuleMetaData getJaxRsModuleMetaData() {
        return getJaxRsModuleMetaData(getModuleMetaData());
    }

    /**
     * This method will be called to retrieve client metadata from the module
     * metadata slot for a given WAR module.
     * 
     */
    public static JaxRsClientMetaData getJaxWsClientMetaData(ModuleMetaData mmd) {

        JaxRsClientMetaData clientMetaData = null;
        if (mmd != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Getting client metadata from module metadata instance: " + mmd);
            }

            JaxRsModuleMetaData moduleMetaData = (JaxRsModuleMetaData) mmd.getMetaData(jaxrsModuleSlot);
            if (moduleMetaData != null) {
                //clientMetaData = moduleMetaData.getClientMetaData();
            }
        }

        return clientMetaData;
    }

    /**
     * Method to retrieve the ServerMetaData based on ModuleMetaData. This
     * method retrieves the module name from the ModuleMetaData and delegates to
     * the (String, ApplicationMetaData) version of this method below.
     */
    public static JaxRsServerMetaData getJaxWsServerMetaData(ModuleMetaData mmd) {

        JaxRsServerMetaData serverMetaData = null;
        if (mmd != null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Getting client metadata from module metadata instance: " + mmd);
            }

            JaxRsModuleMetaData moduleMetaData = (JaxRsModuleMetaData) mmd.getMetaData(jaxrsModuleSlot);
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
    public static JaxRsClientMetaData getJaxWsClientMetaData() {
        ModuleMetaData mmd = getModuleMetaData();
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
