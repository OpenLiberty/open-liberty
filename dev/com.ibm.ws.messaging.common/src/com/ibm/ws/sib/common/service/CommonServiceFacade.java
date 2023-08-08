/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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

@Component(configurationPolicy = IGNORE, property = {"type=com.ibm.ws.sib.common.service"})
public class CommonServiceFacade implements Singleton {
    private static final TraceComponent tc = SibTr.register(CommonServiceFacade.class, MSG_GROUP, MSG_BUNDLE);
    private static final String CLASS_NAME = "com.ibm.ws.sib.common.service.CommonServiceFacade";

    private final CommsClientServiceFacadeInterface commsClient;

    @Activate
    public CommonServiceFacade(
            @Reference(name = "commsClient")
            CommsClientServiceFacadeInterface commsClient
            ) {
        final String methodName = "<init>";
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, methodName, commsClient);
        this.commsClient = commsClient;
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

    public TrmMessageFactory getTrmMessageFactory() {
        return TrmMessageFactory.getInstance();
    }

    public static JsDestinationAddressFactory getJsDestinationAddressFactory() {
        return (JsDestinationAddressFactory) SIDestinationAddressFactory.getInstance();
    }

    public static SelectionCriteriaFactory getSelectionCriteriaFactory() {
        return SelectionCriteriaFactory.getInstance();
    }
}
