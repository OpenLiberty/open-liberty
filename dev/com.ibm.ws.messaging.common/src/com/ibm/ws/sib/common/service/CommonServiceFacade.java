/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.common.service;

import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.sib.admin.JsConstants.MSG_BUNDLE;
import static com.ibm.ws.sib.admin.JsConstants.MSG_GROUP;
import static com.ibm.ws.sib.utils.ras.SibTr.entry;
import static com.ibm.ws.sib.utils.ras.SibTr.exit;
import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.ws.messaging.lifecycle.Singleton;
import com.ibm.ws.messaging.lifecycle.SingletonsReady;
import com.ibm.ws.messaging.security.RuntimeSecurityService;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.comms.ClientConnectionFactory;
import com.ibm.ws.sib.comms.CommsClientServiceFacadeInterface;
import com.ibm.ws.sib.mfp.JsDestinationAddressFactory;
import com.ibm.ws.sib.mfp.trm.TrmMessageFactory;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;

@Component(configurationPolicy = IGNORE, property = {"type=com.ibm.ws.sib.common.service", "service.vendor=IBM"})
public class CommonServiceFacade implements Singleton {
    private static final TraceComponent tc = SibTr.register(CommonServiceFacade.class, MSG_GROUP, MSG_BUNDLE);
    private static final String CLASS_NAME = "com.ibm.ws.sib.common.service.CommonServiceFacade";

    private final CommsClientServiceFacadeInterface commsClient;
    private final RuntimeSecurityService runtimeSecurity;

    @Activate
    public CommonServiceFacade(
            @Reference(name = "commsClient")
            CommsClientServiceFacadeInterface commsClient,
            @Reference(name = "runtimeSecurity")
            RuntimeSecurityService runtimeSecurity
            ) {
        final String methodName = "<init>";
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, methodName, new Object[]{commsClient, runtimeSecurity});
        this.commsClient = commsClient;
        this.runtimeSecurity = runtimeSecurity;
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, methodName);
    }

    public static ClientConnectionFactory getClientConnectionFactory() {
        return SingletonsReady.findService(CommonServiceFacade.class)
                .map(f -> f.commsClient.getClientConnectionFactory())
                .orElse(null);
    }

    public static JsAdminService getJsAdminService() {
        return SingletonsReady.findService(JsAdminService.class)
                .orElse(null);
    }

    public static RuntimeSecurityService getRuntimeSecurityService() {
        return SingletonsReady.findService(CommonServiceFacade.class)
                .map(f -> f.runtimeSecurity)
                .orElse(null);
    }

    public TrmMessageFactory getTrmMessageFactory() {
        return TrmMessageFactory.getInstance();
    }

    public JsDestinationAddressFactory getJsDestinationAddressFactory() {
        return (JsDestinationAddressFactory) SIDestinationAddressFactory.getInstance();
    }

    public SelectionCriteriaFactory getSelectionCriteriaFactory() {
        return SelectionCriteriaFactory.getInstance();
    }
}
