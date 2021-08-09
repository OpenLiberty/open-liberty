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

package com.ibm.ws.sib.admin.internal;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationAliasDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.MQLinkDefinition;
import com.ibm.ws.sib.admin.MQLocalizationDefinition;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;

//import com.ibm.wsspi.runtime.config.ConfigObject;

/**
 * A singleton JsAdminFactory is created during static initialization and is
 * used for the creation of certain runtime objects formed from configuration
 * data.
 */
public abstract class JsAdminFactory {

    private static final String CLASS_NAME = "com.ibm.ws.sib.admin.internal.JsAdminFactory";

    private static final TraceComponent tc = SibTr.register(JsAdminFactory.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);

    private static JsAdminFactory _instance = null;

    private static Exception createException = null;

    static {

        // Create the singleton
        try {
            createInstance();
        } catch (Exception e) {
            // No FFDC code needed
            createException = e;
        }
    }

    /**
     * Create the singleton instance of this factory class
     * 
     * @throws Exception
     */
    private static void createInstance() throws Exception {
        if (tc.isEntryEnabled())
            SibTr.entry(tc, "createInstance", null);

        try {
            Class cls = Class.forName(JsConstants.JS_ADMIN_FACTORY_CLASS);
            _instance = (JsAdminFactory) cls.newInstance();
        } catch (Exception e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME + ".<clinit>", JsConstants.PROBE_10);
            SibTr.error(tc, "EXCP_DURING_INIT_SIEG0001", e);
            // TODO: HIGH: Is this the correct error?
            throw e;
        }

        if (tc.isEntryEnabled())
            SibTr.exit(tc, "createInstance");
    }

    /**
     * Get the singleton instance of the JsAdminfactory
     * 
     * @return JsAdminFactory The instance of this factory class
     * @throws Exception
     */
    public static JsAdminFactory getInstance() throws Exception {

        // If no instance then then throw the exception which occurred.
        if (_instance == null) {
            throw createException;
        }

        // Return the instance
        return _instance;
    }

    //----------------------------------------------------------------------------
    // BaseDestinationDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a BaseDestinationDefinition instance.
     * 
     * @param type
     * @param name
     * 
     * @return The new BaseDestinationDefinition instance.
     */
    public abstract BaseDestinationDefinition createBaseDestinationDefinition(DestinationType type, String name);

    /**
     * Create a BaseDestinationDefinition.
     * 
     * @param d
     * 
     * @return The new BaseDestinationDefinition instance.
     */
    public abstract BaseDestinationDefinition createBaseDestinationDefinition(LWMConfig d);

    //----------------------------------------------------------------------------
    // DestinationDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a DestinationDefinition.
     * 
     * @param type
     * @param name
     * 
     * @return The new DestinationDefinition instance.
     */
    public abstract DestinationDefinition createDestinationDefinition(DestinationType type, String name);

    /**
     * Create a DestinationDefinition.
     * 
     * @param d
     * 
     * @return The new DestinationDefinition instance.
     */
    public abstract DestinationDefinition createDestinationDefinition(LWMConfig d);

    //----------------------------------------------------------------------------
    // DestinationAliasDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a DestinationAliasDefinition.
     * 
     * @param type
     * @param name
     * 
     * @return The new DestinationAliasDefinition instance.
     */
    public abstract DestinationAliasDefinition createDestinationAliasDefinition(DestinationType type, String name);

    /**
     * Create a DestinationAliasDefinition.
     * 
     * @param d
     * 
     * @return The new DestinationAliasDefinition instance.
     */
    public abstract DestinationAliasDefinition createDestinationAliasDefinition(LWMConfig d);

    //----------------------------------------------------------------------------
    // DestinationForeignDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a DestinationForeignDefinition.
     * 
     * @param type
     * @param name
     * 
     * @return The new DestinationForeignDefinition instance.
     */
//    public abstract DestinationForeignDefinition createDestinationForeignDefinition(DestinationType type, String name);

    /**
     * Create a DestinationForeignDefinition.
     * 
     * @param d
     * 
     * @return The new DestinationForeignDefinition instance.
     */
//    public abstract DestinationForeignDefinition createDestinationForeignDefinition(ConfigObject d);

    //----------------------------------------------------------------------------
    // LocalizationDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a LocalizationDefinition.
     * 
     * @param name
     * 
     * @return The new LocalizationDefinition instance.
     */
    public abstract LocalizationDefinition createLocalizationDefinition(String name);

    /**
     * Create a LocalizationDefinition.
     * 
     * @param lp
     * 
     * @return The new LocalizationDefinition instance.
     */
    public abstract LocalizationDefinition createLocalizationDefinition(LWMConfig lp);

    //----------------------------------------------------------------------------
    // MQLocalizationDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a MQLocalizationDefinition.
     * 
     * @param name
     * 
     * @return The new MQLocalizationDefinition instance.
     */
    public abstract MQLocalizationDefinition createMQLocalizationDefinition(String name);

    /**
     * Create a MQLocalizationDefinition.
     * 
     * @param mqs
     * @param bm
     * @param lpp
     * 
     * @return The new MQLocalizationDefinition instance.
     */
    public abstract MQLocalizationDefinition createMQLocalizationDefinition(LWMConfig mqs, LWMConfig bm, LWMConfig lpp);

    //----------------------------------------------------------------------------
    // MediationLocalizationDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a MediationLocalizationDefinition.
     * 
     * @param name
     * 
     * @return The new MediationLocalizationDefinition instance.
     */
//    public abstract MediationLocalizationDefinition createMediationLocalizationDefinition(String name);

    /**
     * Create a MediationLocalizationDefinition.
     * 
     * @param lp
     * 
     * @return The new MediationLocalizationDefinition instance.
     */
//    public abstract MediationLocalizationDefinition createMediationLocalizationDefinition(ConfigObject lp);

    //----------------------------------------------------------------------------
    // MQMediationLocalizationDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a MQMediationLocalizationDefinition.
     * 
     * @param name
     * 
     * @return The new MQMediationLocalizationDefinition instance.
     */
//    public abstract MQMediationLocalizationDefinition createMQMediationLocalizationDefinition(String name);

    /**
     * Create a MQMediationLocalizationDefinition.
     * 
     * @param mqs
     * @param bm
     * @param lpp
     * 
     * @return The new MQMediationLocalizationDefinition instance.
     */
//    public abstract MQMediationLocalizationDefinition createMQMediationLocalizationDefinition(ConfigObject mqs, ConfigObject bm, ConfigObject lpp);

    //----------------------------------------------------------------------------
    // MediationExecutionPointDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a MediationExecutionPointDefinition.
     * 
     * @param name
     * 
     * @return The new MediationExecutionPointDefinition instance.
     */
//    public abstract MediationExecutionPointDefinition createMediationExecutionPointDefinition(String name);

    /**
     * Create a MediationExecutionPointDefinition.
     * 
     * @param lp
     * @param isMep is it a SIBMediationExecutionPoint or a
     *            SIBLocalizationPoint
     * @return The new MediationExecutionPointDefinition instance.
     */
//    public abstract MediationExecutionPointDefinition createMediationExecutionPointDefinition(ConfigObject lp, boolean isMep);

    //----------------------------------------------------------------------------
    // MediationDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a MediationDefinition.
     * 
     * @return The new MediationDefinition instance.
     */
//    public abstract MediationDefinition createMediationDefinition();

    /**
     * Create a MediationDefinition.
     * 
     * @param m
     * 
     * @return The new MediationDefinition instance.
     */
//    public abstract MediationDefinition createMediationDefinition(ConfigObject m);

    //----------------------------------------------------------------------------
    // ForeignBusDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a ForeignBusDefinition.
     * 
     * @param name
     * @param vld
     * @param fdp
     * 
     * @return The new ForeignBusDefinition instance.
     */
//    public abstract ForeignBusDefinition createForeignBusDefinition(String name, VirtualLinkDefinition vld, ForeignDestinationDefault fdp);

    /**
     * Create a ForeignBusDefinition.
     * 
     * @param name
     * @param fbd
     * @param fdp
     * 
     * @return The new ForeignBusDefinition instance.
     */
//    public abstract ForeignBusDefinition createForeignBusDefinition(String name, ForeignBusDefinition fbd, ForeignDestinationDefault fdp);

    /**
     * Create a ForeignBusDefinition.
     * 
     * @param fb
     * 
     * @return The new ForeignBusDefinition instance.
     */
//    public abstract ForeignBusDefinition createForeignBusDefinition(ConfigObject fb);

    //----------------------------------------------------------------------------
    // ForeignDestinationDefault
    //----------------------------------------------------------------------------

    /**
     * Create a ForeignDestinationDefault.
     * 
     * @return The new ForeignDestinationDefault instance.
     */
//    public abstract ForeignDestinationDefault createForeignDestinationDefault();

    /**
     * Create a ForeignDestinationDefault.
     * 
     * @param fdd
     * 
     * @return The new ForeignDestinationDefault instance.
     */
//    public abstract ForeignDestinationDefault createForeignDestinationDefault(ConfigObject fdd);

    //----------------------------------------------------------------------------
    // VirtualLinkDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a VirtualLinkDefinition.
     * 
     * @param name
     * 
     * @return The new xxx VirtualLinkDefinition.
     */
//    public abstract VirtualLinkDefinition createVirtualLinkDefinition(String name);

    /**
     * Create a VirtualLinkDefinition.
     * 
     * @param vl
     * @param fb
     * 
     * @return The new VirtualLinkDefinition instance.
     */
//    public abstract VirtualLinkDefinition createVirtualLinkDefinition(ConfigObject vl, ForeignBusDefinition fb);

    //----------------------------------------------------------------------------
    // MQLinkDefinition
    //----------------------------------------------------------------------------

    /**
     * Create a MQLinkDefinition.
     * 
     * @param uuid
     * 
     * @return The new xxx MQLinkDefinition.
     */
    public abstract MQLinkDefinition createMQLinkDefinition(String uuid);
}
