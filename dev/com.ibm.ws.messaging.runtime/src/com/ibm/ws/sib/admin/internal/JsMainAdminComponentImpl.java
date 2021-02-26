/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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
import com.ibm.ws.messaging.security.RuntimeSecurityService;
import com.ibm.ws.messaging.service.JsMainAdminComponent;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMainAdminService;
import com.ibm.ws.sib.admin.internal.JsAdminConstants.ME_STATE;
import com.ibm.ws.sib.common.service.CommonServiceFacade;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;

/**
 * The JsMainAdminComponent requires the JsMainAdminService and its dependencies, to be started.
 */
@Component(configurationPid = "com.ibm.ws.messaging.runtime",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class JsMainAdminComponentImpl implements JsMainAdminComponent, ApplicationPrereq {

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
    private final JsMainAdminService service;

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
    public static final AtomicServiceReference<JsAdminService> jsAdminServiceref = new AtomicServiceReference<JsAdminService>(KEY_JS_ADMIN_SERVICE);
    public static final AtomicServiceReference<RuntimeSecurityService> runtimeSecurityServiceRef = new AtomicServiceReference<RuntimeSecurityService>(
                    "runtimeSecurityService");
    
    final String jsAdminComponentId;
    
    @Activate
    public JsMainAdminComponentImpl(Map<String, Object> props, @Reference JsMainAdminService service) {
        final String methodName = "JsMainAdminComponentImpl";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, methodName, new Object[] { this, service });
        
        this.service = service;
        jsAdminComponentId = (String) props.getOrDefault("id", "ERROR: No id in the properties for "+CLASS_NAME);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, methodName);
    }

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
            configAdminRef.activate(context);
            messageStoreRef.activate(context);
            destinationAddressFactoryRef.activate(context);
            jsAdminServiceref.activate(context);
            runtimeSecurityServiceRef.activate(context);
            service.start(properties);

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
    //TODO Consider disallowing modification of this service, remove modified() and force creation of a new JsMainAdmin.
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
                service.start(properties);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.debug(tc, "Modifying the configuration", service
                                    .getMeState());
                service.modify(properties);
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
            service.stop();

            configAdminRef.deactivate(context);
            messageStoreRef.deactivate(context);
            destinationAddressFactoryRef.deactivate(context);
            jsAdminServiceref.deactivate(context);
            runtimeSecurityServiceRef.deactivate(context);

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
     * Declarative Services method for setting the RuntimeSecurityService reference.
     * 
     * @param ref reference to the service
     */
    @Reference
    protected void setRuntimeSecurityService(ServiceReference<RuntimeSecurityService> ref) {
        final String methodName = "setRuntimeSecurityService";
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, methodName, new Object[] {this, ref});
        
        runtimeSecurityServiceRef.setReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, methodName);
    }

    /**
     * Declarative Services method for unsetting the RuntimeSecurityService reference.
     * 
     * @param ref reference to the service
     */
    protected void unsetRuntimeSecurityService(ServiceReference<RuntimeSecurityService> ref) {
        final String methodName = "unsetRuntimeSecurityService";
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, methodName, new Object[] {this, ref});
        // TODO Change all to unsetReference();
        runtimeSecurityServiceRef.unsetReference(ref);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, methodName);
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

    @Override
    public String getApplicationPrereqID() {
        return jsAdminComponentId;
    }
}
