/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.common.service;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.comms.ClientConnectionFactory;
import com.ibm.ws.sib.comms.CommsClientServiceFacadeInterface;
import com.ibm.ws.sib.mfp.JsDestinationAddressFactory;
import com.ibm.ws.sib.mfp.trm.TrmMessageFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = "service.vendor=IBM")
public class CommonServiceFacade {

    /** RAS trace variable */
    private static final TraceComponent tc = SibTr.register(
                                                            CommonServiceFacade.class, JsConstants.MSG_GROUP,
                                                            JsConstants.MSG_BUNDLE);
    private static final String CLASS_NAME = "com.ibm.ws.sib.common.service.CommonServiceFacade";

    /**
     * JsAdminService service
     */
    private static final AtomicServiceReference<JsAdminService> jsAdminServiceref = new AtomicServiceReference<JsAdminService>(
                    "JsAdminService");

    //obtain CommsClientServiceFacade reference
    //TODO: CommsClientServiceFacade has to be loaded on demand.
    private static final AtomicServiceReference<CommsClientServiceFacadeInterface> _commsClientServiceFacaderef = new AtomicServiceReference<CommsClientServiceFacadeInterface>(
                    "CommsClientServiceFacadeInterface");

    @Activate
    protected void activate(ComponentContext context,
                            Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "activate", new Object[] { context,
                                                                   properties });
        }

        try {
            jsAdminServiceref.activate(context);
            _commsClientServiceFacaderef.activate(context);
        } catch (Exception e) {
            SibTr.exception(tc, e);
            FFDCFilter.processException(e, this.getClass().getName(), "133",
                                        this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "activate");
        }
    }

    @Modified
    protected void modified(ComponentContext context,
                            Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "modified", new Object[] { context,
                                                                   properties });
        }

        try {
            jsAdminServiceref.activate(context);
            _commsClientServiceFacaderef.activate(context);
        } catch (Exception e) {
            SibTr.exception(tc, e);
            FFDCFilter.processException(e, this.getClass().getName(), "187",
                                        this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "modified");
        }

    }

    @Deactivate
    protected void deactivate(ComponentContext context,
                              Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "deactivate", new Object[] { context,
                                                                     properties });
        }
        try {

            jsAdminServiceref.deactivate(context);
            _commsClientServiceFacaderef.deactivate(context);
        } catch (Exception e) {
            SibTr.exception(tc, e);
            FFDCFilter.processException(e, this.getClass().getName(), "227",
                                        this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "deactivate");
        }

    }

    /**
     * @param ref
     *            reference to the service
     */
    @Reference(service = JsAdminService.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setJsAdminService(ServiceReference<JsAdminService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "setJsAdminService", ref);
        jsAdminServiceref.setReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "setJsAdminService");
    }

    /**
     * @param ref
     *            reference to the service
     */
    protected void unsetJsAdminService(ServiceReference<JsAdminService> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "unsetJsAdminService", ref);
        jsAdminServiceref.unsetReference(ref);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "unsetJsAdminService");

    }

    /**
     * @param ref
     *            reference to the service
     */
    @Reference(service = CommsClientServiceFacadeInterface.class, cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setCommsClientServiceFacadeInterface(ServiceReference<CommsClientServiceFacadeInterface> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "setJsAdminService", ref);
        _commsClientServiceFacaderef.setReference(ref);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "setJsAdminService");
    }

    /**
     * @param ref
     *            reference to the service
     */
    protected void unsetCommsClientServiceFacadeInterface(ServiceReference<CommsClientServiceFacadeInterface> ref) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.entry(tc, "unsetJsAdminService", ref);
        _commsClientServiceFacaderef.unsetReference(ref);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.exit(tc, "unsetJsAdminService");

    }

    // obtain ClientConnectionFactory through CommsClientServiceFacade
    public static ClientConnectionFactory getClientConnectionFactory() {
        return _commsClientServiceFacaderef.getService().getClientConnectionFactory();
    }

    /**
     * static method for getting the JsAdminService instance
     * 
     * @return JsAdminService
     */
    public static JsAdminService getJsAdminService() {
        return jsAdminServiceref.getService();
    }

    //Provide implemenations thru factory interfaces, so that other OSGI bundles
    // can consume them.

    // passing TrmMessageFactory implementation thru MFP 
    public static TrmMessageFactory getTrmMessageFactory() {
        // This should have been returning from some MFP Facade.
        // For now returning directly as MFP is part of common now.
        return TrmMessageFactory.getInstance();
    }

    // passing JsDestinationAddressFactory implementation thru MFP
    public static JsDestinationAddressFactory getJsDestinationAddressFactory() {
        // This should have been returning from some MFP Facade.
        // For now returning directly as MFP is part of common now.		
        return (JsDestinationAddressFactory) SIDestinationAddressFactory.getInstance();
    }

    // passing SelectionCriteriaFactory implementation thru core
    public static SelectionCriteriaFactory getSelectionCriteriaFactory() {
        // This should have been returning from some core Facade.
        // For now returning directly as core is part of common now.		
        return SelectionCriteriaFactory.getInstance();
    }

}
