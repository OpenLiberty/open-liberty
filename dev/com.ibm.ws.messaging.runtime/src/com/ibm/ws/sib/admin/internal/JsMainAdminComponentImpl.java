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

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.messaging.service.JsMainAdminServiceImpl;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMainAdminService;
import com.ibm.ws.sib.admin.internal.JsAdminConstants.ME_STATE;
import com.ibm.ws.sib.common.service.CommonServiceFacade;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;

/**
 * A declarative service implementor
 */
@Component(configurationPid = "com.ibm.ws.messaging.runtime",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class JsMainAdminComponentImpl implements ConfigurationListener {

    /**  */
    private static final String KEY_JS_ADMIN_SERVICE = "jsAdminService";
    /**  */
    private static final String KEY_DESTINATION_ADDRESS_FACTORY = "destinationAddressFactory";
    /**  */
    private static final String KEY_MESSAGE_STORE = "messageStore";
    /**  */
    private static final String KEY_CONFIG_ADMIN = "configAdmin";
    /** RAS trace variable */
    private static final TraceComponent tc = SibTr.register(
                                                            JsMainAdminComponentImpl.class, JsConstants.TRGRP_AS,
                                                            JsConstants.MSG_BUNDLE);
    private static final String CLASS_NAME = "com.ibm.ws.sib.admin.internal.JsMainAdminComponentImpl";
    private JsMainAdminService service;

    /**
     * ConfigAdmin service.
     */
    public static final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>(
                    KEY_CONFIG_ADMIN);

    /**
     * MessageStore service
     */
    public static final AtomicServiceReference<MessageStore> messageStoreRef = new AtomicServiceReference<MessageStore>(
                    KEY_MESSAGE_STORE);

    /**
     * DestinationAddressFactory service
     */
    public static final AtomicServiceReference<SIDestinationAddressFactory> destinationAddressFactoryRef = new AtomicServiceReference<SIDestinationAddressFactory>(
                    KEY_DESTINATION_ADDRESS_FACTORY);

    /**
     * JsAdminService service
     */
    public static final AtomicServiceReference<JsAdminService> jsAdminServiceref = new AtomicServiceReference<JsAdminService>(
                    KEY_JS_ADMIN_SERVICE);

    /**
     * This method is call by the declarative service when the feature is
     * activated
     */
    @Activate
    protected void activate(ComponentContext context,
                            Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "activate", new Object[] { context,
                                                                   properties });
        }

        try {
            service = new JsMainAdminServiceImpl();

            configAdminRef.activate(context);
            messageStoreRef.activate(context);
            destinationAddressFactoryRef.activate(context);
            jsAdminServiceref.activate(context);
            service.activate(context, properties, configAdminRef.getService());

        } catch (Exception e) {
            SibTr.exception(tc, e);
            FFDCFilter.processException(e, this.getClass().getName(), "133", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "activate");
        }
    }

    /**
     * This method is call by the declarative service when there is
     * configuration change
     */
    @Modified
    protected void modified(ComponentContext context,
                            Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "modified", new Object[] { context,
                                                                   properties });
        }

        try {

            // If ME is stopped we start it again.This happens when ME might have
            // not started during activate() and user changes the server.xml, we
            // attempt to start it again(thinking user have reactified any
            // server.xml issue if any )
            if (service.getMeState().equals(ME_STATE.STOPPED.toString())) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.debug(tc, "Starting ME", service.getMeState());
                SibTr.info(tc, "RESTART_ME_SIAS0106");
                service.activate(context, properties, configAdminRef.getService());
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.debug(tc, "Modifying the configuration", service
                                    .getMeState());
                service.modified(context, properties, configAdminRef.getService());
            }

        } catch (Exception e) {
            SibTr.exception(tc, e);
            FFDCFilter.processException(e, this.getClass().getName(), "187", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "modified");
        }

    }

    /**
     * This method is call by the declarative service when the feature is
     * removed
     */
    @Deactivate
    protected void deactivate(ComponentContext context,
                              Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "deactivate", new Object[] { context,
                                                                     properties });
        }

        try {
            service.deactivate(context, properties);
            // Once service.deactivate() is completed destroy the service instance
            service = null;

            configAdminRef.deactivate(context);
            messageStoreRef.deactivate(context);
            destinationAddressFactoryRef.deactivate(context);
            jsAdminServiceref.deactivate(context);

        } catch (Exception e) {
            SibTr.exception(tc, e);
            FFDCFilter.processException(e, this.getClass().getName(), "227", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "deactivate");
        }

    }

    /**
     * Declarative Services method for setting the ConfigurationAdmin service
     * reference.
     * 
     * @param ref
     *            reference to the service
     */
    @Reference(name = KEY_CONFIG_ADMIN, service = ConfigurationAdmin.class)
    protected void setConfigAdmin(ServiceReference<ConfigurationAdmin> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "setConfigAdmin", ref);
        configAdminRef.setReference(ref);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "setConfigAdmin");
    }

    /**
     * Declarative Services method for unsetting the ConfigurationAdmin service
     * reference.
     * 
     * @param ref
     *            reference to the service
     */
    protected void unsetConfigAdmin(ServiceReference<ConfigurationAdmin> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "unsetConfigAdmin", ref);
        configAdminRef.unsetReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "unsetConfigAdmin");
    }

    /**
     * Declarative Services method for setting the MessageStore service
     * reference.
     * 
     * @param ref
     *            reference to the service
     */
    @Reference(name = KEY_MESSAGE_STORE, service = MessageStore.class)
    protected void setMessageStore(ServiceReference<MessageStore> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "setMessageStore", ref);
        messageStoreRef.setReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "setMessageStore");
    }

    /**
     * Declarative Services method for unsetting the MessageStore service
     * reference.
     * 
     * @param ref
     *            reference to the service
     */
    protected void unsetMessageStore(ServiceReference<MessageStore> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "unsetMessageStore", ref);
        messageStoreRef.unsetReference(ref);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "unsetMessageStore");

    }

    /**
     * Declarative Services method for setting the DestinationAddressFactory service
     * reference.
     * 
     * @param ref
     *            reference to the service
     */
    @Reference(name = KEY_DESTINATION_ADDRESS_FACTORY, service = SIDestinationAddressFactory.class)
    protected void setDestinationAddressFactory(ServiceReference<SIDestinationAddressFactory> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "setDestinationAddressFactory", ref);
        destinationAddressFactoryRef.setReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "setDestinationAddressFactory");
    }

    /**
     * Declarative Services method for unsetting the DestinationAddressFactory service
     * reference.
     * 
     * @param ref
     *            reference to the service
     */
    protected void unsetDestinationAddressFactory(ServiceReference<SIDestinationAddressFactory> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "unsetDestinationAddressFactory", ref);
        destinationAddressFactoryRef.setReference(ref);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "unsetDestinationAddressFactory");

    }

    /**
     * Declarative Services method for setting the JsAdminService service
     * reference.
     * 
     * @param ref
     *            reference to the service
     */
    @Reference(name = KEY_JS_ADMIN_SERVICE, service = JsAdminService.class)
    protected void setJsAdminService(ServiceReference<JsAdminService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "setJsAdminService", ref);
        jsAdminServiceref.setReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "setJsAdminService");
    }

    /**
     * Declarative Services method for unsetting the JsAdminService service
     * reference.
     * 
     * @param ref
     *            reference to the service
     */
    protected void unsetJsAdminService(ServiceReference<JsAdminService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "unsetJsAdminService", ref);
        jsAdminServiceref.setReference(ref);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "unsetJsAdminService");

    }

    /**
     * static method for getting the SIDestinationAddressFactory instance
     * 
     * @return SIDestinationAddressFactory
     */
    public static SIDestinationAddressFactory getSIDestinationAddressFactory()
    {
        return destinationAddressFactoryRef.getService();
    }

    /**
     * static method for getting the SelectionCriteriaFactory instance
     * 
     * @return SelectionCriteriaFactory
     */
    public static SelectionCriteriaFactory getSelectionCriteriaFactory()
    {
        return CommonServiceFacade.getSelectionCriteriaFactory();
    }

    /**
     * static method for getting the JsAdminService instance
     * 
     * @return JsAdminService
     */
    public static JsAdminService getJsAdminService()
    {
        return jsAdminServiceref.getService();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.ConfigurationListener#configurationEvent(org.osgi.service.cm.ConfigurationEvent)
     */
    @Override
    public void configurationEvent(ConfigurationEvent event) {
        service.configurationEvent(event, configAdminRef.getService());
    }
}
