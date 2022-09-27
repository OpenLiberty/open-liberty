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

import static com.ibm.websphere.ras.Tr.debug;
import static com.ibm.websphere.ras.Tr.entry;
import static com.ibm.websphere.ras.Tr.exit;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.messaging.lifecycle.SingletonsReady.requireService;
import static java.security.AccessController.doPrivileged;
import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.rar.ResourceAdapterBundleService;
import com.ibm.ws.messaging.lifecycle.Singleton;
import com.ibm.ws.sib.common.service.CommonServiceFacade;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;

@Component (configurationPolicy = IGNORE, immediate = true, property= {"type=wasJms","service.vendor=IBM"})
public class JmsServiceFacade implements ResourceAdapterBundleService, Singleton {

    private static final TraceComponent tc = SibTr.register(JmsServiceFacade.class, TraceGroups.TRGRP_RA,
                                                            "com.ibm.ws.sib.ra.CWSIVMessages");
    
 
    public static final SIDestinationAddressFactory getSIDestinationAddressFactory() {      
		return CommonServiceFacade.getJsDestinationAddressFactory();
    }

    public static final SelectionCriteriaFactory getSelectionCriteriaFactory() {     
		return CommonServiceFacade.getSelectionCriteriaFactory();
    }

    @Override
    public void setClassLoaderID(ClassLoaderIdentity classloaderId) {
        // Nothing to do for this ResourceAdapterBundleService method
    }

    /*
     * Returns true if environment is client container.
     * This has to be only used by JMS 20 module (so it is safe .. as no chance of NPE..i.e bundleContext is always initialized properly)
     */
    public static boolean isClientContainer() {
    	Bundle bundle = FrameworkUtil.getBundle(JmsServiceFacade.class);
        String processType = doPrivileged((PrivilegedAction<String>)() -> bundle.getBundleContext().getProperty(WsLocationConstants.LOC_PROCESS_TYPE));
        return WsLocationConstants.LOC_PROCESS_TYPE_CLIENT.equals(processType);
    }
}
