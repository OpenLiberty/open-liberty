/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jms.service;

import static com.ibm.wsspi.kernel.service.location.WsLocationConstants.LOC_PROCESS_TYPE_CLIENT;
import static com.ibm.wsspi.kernel.service.location.WsLocationConstants.SYMBOL_PROCESS_TYPE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.ws.jca.rar.ResourceAdapterBundleService;
import com.ibm.ws.messaging.lifecycle.Singleton;
import com.ibm.ws.messaging.lifecycle.SingletonsReady;
import com.ibm.ws.sib.common.service.CommonServiceFacade;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;

@Component(name="com.ibm.ws.messaging.jms", configurationPolicy = REQUIRE, property = {"service.vendor=IBM","type=wasJms"})
public class JmsServiceFacade implements ResourceAdapterBundleService, Singleton {

    private static final TraceComponent tc = SibTr.register(JmsServiceFacade.class, TraceGroups.TRGRP_RA,
                                                            "com.ibm.ws.sib.ra.CWSIVMessages");
    private static final String CLASS_NAME = "com.ibm.ws.sib.api.jms.JmsServiceFacade";

    private final boolean isClient;
   
    @Activate
    public JmsServiceFacade(@Reference CommonServiceFacade commonServiceFacade, // Required but not used.
                            @Reference WsLocationAdmin wsLocationAdmin) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "<init>", new Object[] {commonServiceFacade, wsLocationAdmin});
        }

        this.isClient = LOC_PROCESS_TYPE_CLIENT.equals(wsLocationAdmin.resolveString(SYMBOL_PROCESS_TYPE));
       
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "<init>", new Object[] {isClient});
        }
    }

    public static SIDestinationAddressFactory getSIDestinationAddressFactory()
    {
        // Fail-fast if invoked out of sequence.
        SingletonsReady.requireService(JmsServiceFacade.class);
        return CommonServiceFacade.getJsDestinationAddressFactory();
    }

    public static SelectionCriteriaFactory getSelectionCriteriaFactory()
    {
        // Fail-fast if invoked out of sequence.
        SingletonsReady.requireService(JmsServiceFacade.class);
        return CommonServiceFacade.getSelectionCriteriaFactory();
    }

    @Override
    public void setClassLoaderID(ClassLoaderIdentity classloaderId) {
        // Nothing to do for this ResourceAdapterBundleService method
    }

    /*
     * Returns true if environment is client container.
     * Fail-fast if invoked out of sequence.
     */
    public static boolean isClientContainer() { return SingletonsReady.requireService(JmsServiceFacade.class).isClient; }
}
