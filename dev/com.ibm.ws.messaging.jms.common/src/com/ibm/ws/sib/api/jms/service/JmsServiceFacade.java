/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jms.service;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.rar.ResourceAdapterBundleService;
import com.ibm.ws.sib.common.service.CommonServiceFacade;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;

public class JmsServiceFacade implements ResourceAdapterBundleService {

    /** RAS trace variable */
    private static final TraceComponent tc = SibTr.register(
                                                            JmsServiceFacade.class, TraceGroups.TRGRP_RA,
                                                            "com.ibm.ws.sib.ra.CWSIVMessages");
    private static final String CLASS_NAME = "com.ibm.ws.sib.api.jms.JmsServiceFacade";

    //Liberty COMMS change
    //Obtain required factories through CommonServiceFacade
    private static final AtomicServiceReference<CommonServiceFacade> _commonServiceFacadeRef = new AtomicServiceReference<CommonServiceFacade>("commonServiceFacade");

    private static BundleContext bundleContext;

    public void activate(ComponentContext context,
                         Map<String, Object> properties, Map<String, Object> serviceList) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "activate", new Object[] { context,
                                                                   properties });
        }

        bundleContext = context.getBundleContext();
        try {

            _commonServiceFacadeRef.activate(context);

        } catch (Exception e) {

            SibTr.exception(tc, e);
            FFDCFilter.processException(e, CLASS_NAME, "1", this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "activate");
        }
    }

    protected void modified(ComponentContext context,
                            Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "modified", new Object[] { context,
                                                                   properties });
        }

        try {

            _commonServiceFacadeRef.activate(context);
        } catch (Exception e) {
            SibTr.exception(tc, e);
            FFDCFilter
                            .processException(e, this.getClass().getName(), "2", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "modified");
        }

    }

    protected void deactivate(ComponentContext context,
                              Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, CLASS_NAME + "deactivate", new Object[] { context,
                                                                     properties });
        }

        try {
            _commonServiceFacadeRef.deactivate(context);
        } catch (Exception e) {
            SibTr.exception(tc, e);
            FFDCFilter.processException(e, CLASS_NAME, "3", this);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, CLASS_NAME + "deactivate");
        }
    }

    //Liberty COMMS change
    //Obtain required factories through CommonServiceFacade
    protected void setCommonServiceFacade(ServiceReference<CommonServiceFacade> ref) {
        _commonServiceFacadeRef.setReference(ref);
    }

    protected void unsetCommonServiceFacade(ServiceReference<CommonServiceFacade> ref) {
        _commonServiceFacadeRef.unsetReference(ref);
    }

    public static SIDestinationAddressFactory getSIDestinationAddressFactory()
    {
        return _commonServiceFacadeRef.getService().getJsDestinationAddressFactory();
    }

    public static SelectionCriteriaFactory getSelectionCriteriaFactory()
    {
        return _commonServiceFacadeRef.getService().getSelectionCriteriaFactory();
    }

    @Override
    public void setClassLoaderID(ClassLoaderIdentity classloaderId) {
        // Nothing to do for this ResourceAdapterBundleService method
    }

    /*
     * Returns true if environment is client container.
     * This has to be only used by JMS 20 module (so it is safe .. as no chance of NPE..i.e bundleContext is always initialized properly)
     */
    public static boolean isClientContainer()
    {
        String processType = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return bundleContext.getProperty(WsLocationConstants.LOC_PROCESS_TYPE);
            }
        });
        return Boolean.valueOf(WsLocationConstants.LOC_PROCESS_TYPE_CLIENT.equals(processType));
    }
}
