/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
package com.ibm.ws.sib.admin.internal;

import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static com.ibm.ws.sib.utils.ras.SibTr.debug;
import static com.ibm.ws.sib.utils.ras.SibTr.entry;
import static com.ibm.ws.sib.utils.ras.SibTr.exception;
import static com.ibm.ws.sib.utils.ras.SibTr.exit;
import static com.ibm.ws.sib.utils.ras.SibTr.info;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.Map;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIDestinationAddressFactory;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.messaging.lifecycle.SingletonsReady;
import com.ibm.ws.messaging.security.RuntimeSecurityService;
import com.ibm.ws.messaging.service.JsMainAdminComponent;
import com.ibm.ws.sib.admin.JsAdminService;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMainAdminService;
import com.ibm.ws.sib.admin.internal.JsAdminConstants.ME_STATE;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;
import com.ibm.wsspi.sib.core.SelectionCriteriaFactory;

/**
 * The JsMainAdminComponent requires the JsMainAdminService and its dependencies, to be started.
 */
@Component(configurationPid = "com.ibm.ws.messaging.runtime",
           configurationPolicy = REQUIRE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class JsMainAdminComponentImpl implements JsMainAdminComponent, ApplicationPrereq {
    private static final TraceComponent tc = SibTr.register(JsMainAdminComponentImpl.class, JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);
    private static final String CLASS_NAME = "com.ibm.ws.sib.admin.internal.JsMainAdminComponentImpl";
    private final JsMainAdminService service;
    public final SIDestinationAddressFactory destinationAddressFactory;

    final String jsAdminComponentId;

    @Activate
    public JsMainAdminComponentImpl(Map<String, Object> properties,
            @Reference JsMainAdminService service,
            @Reference SingletonsReady singletonsReady) {
        final String methodName = "JsMainAdminComponentImpl";
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(tc, methodName, new Object[] { this, properties, service, singletonsReady });

        jsAdminComponentId = (String) properties.getOrDefault("id", "ERROR: No id in the properties for "+CLASS_NAME);
        this.service = service;
        this.destinationAddressFactory = SIDestinationAddressFactory.getInstance();
        service.start(properties);

        if (isAnyTracingEnabled() && tc.isEntryEnabled())
            exit(tc, methodName);
    }


    @Modified
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(tc, CLASS_NAME + "modified", new Object[] { context, properties });

        try {

            // If ME is stopped we start it again.This happens when ME might have
            // not started during activate() and user changes the server.xml, we
            // attempt to start it again(thinking user have reactified any
            // server.xml issue if any )
            if (service.getMeState().equals(ME_STATE.STOPPED.toString())) {
                if (isAnyTracingEnabled() && tc.isEntryEnabled()) debug(tc, "Starting ME", service.getMeState());
                info(tc, "RESTART_ME_SIAS0106");
                service.start(properties);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) debug(tc, "Modifying the configuration", service.getMeState());
                service.modify(properties);
            }

        } catch (Exception e) {
            exception(tc, e);
            FFDCFilter.processException(e, this.getClass().getName(), "187", this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())exit(tc, CLASS_NAME + "modified");

    }

    @Deactivate
    protected void deactivate(ComponentContext context, Map<String, Object> properties) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(tc, CLASS_NAME + "deactivate", new Object[] { context, properties });

        service.stop();

        if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, CLASS_NAME + "deactivate");

    }


    public static SelectionCriteriaFactory getSelectionCriteriaFactory() {
        return SelectionCriteriaFactory.getInstance();
    }

    public static JsAdminService getJsAdminService() {
        return SingletonsReady.findService(JsAdminService.class)
                .orElse(null);
    }

    @Override
    public String getApplicationPrereqID() {
        return jsAdminComponentId;
    }
}
