/*******************************************************************************
 * Copyright (c) 2014, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.api.jms.service;

import static java.security.AccessController.doPrivileged;
import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import java.security.PrivilegedAction;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.ws.jca.rar.ResourceAdapterBundleService;
import com.ibm.ws.messaging.lifecycle.SingletonsReady;
import com.ibm.ws.sib.common.service.CommonServiceFacade;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;

/**
 * This component is the point of entry for JCA into SIB messaging.
 * Any dependencies that need to be in place for JCA to work should be expressed here.
 */
@Component (configurationPolicy = IGNORE, immediate = true, property= {"type=wasJms","service.vendor=IBM"})
public class JmsServiceFacade implements ResourceAdapterBundleService {

    private static final TraceComponent tc = SibTr.register(JmsServiceFacade.class, TraceGroups.TRGRP_RA,
                                                            "com.ibm.ws.sib.ra.CWSIVMessages");
    
    /**
     * @param singletonsReady force this component to wait for SingletonsReady
     */
    @Activate
    public JmsServiceFacade(@Reference(name = "singletonsReady") SingletonsReady singletonsReady) {
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "<init>",singletonsReady);
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }
    
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
