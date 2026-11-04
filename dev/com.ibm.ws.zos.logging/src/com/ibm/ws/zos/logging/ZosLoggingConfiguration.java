/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging;

import java.util.Hashtable;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.logging.osgi.MessageRouterConfigListener;
import com.ibm.ws.zos.logging.internal.ZosLoggingConfigListener;

/**
 * Service that represents the <zosLogging> configuration element.
 * Updates to the config element are pushed to ZosLoggingConfigListener
 * and MessageRouterConfigListener services.
 */
@Component(name = "com.ibm.ws.zos.logging.config",
           service = ZosLoggingConfiguration.class,
           immediate = true,
           property = { "service.vendor = IBM" },
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ZosLoggingConfiguration {

    /**
     * The latest values of the <zosLogging> config parameters.
     */
    private volatile Hashtable<String, Object> zosLoggingConfig = null;

    /**
     * Config listener used to pass updates to ZosLoggingBundleActivator.
     */
    private ZosLoggingConfigListener zosLoggingConfigListener;

    /**
     * Config listener used to pass updates to MessageRouterConfigurator.
     */
    private MessageRouterConfigListener msgRouterConfigListener;

    /**
     * Method used to register the ZosLoggingConfigListener.
     *
     * @param listener
     */
    @Reference(policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL)
    protected void setZosLoggingConfigListener(ZosLoggingConfigListener listener) {
        this.zosLoggingConfigListener = listener;
        updateListeners();
    }

    /**
     * Method used to register the MessageRouterConfigListener.
     *
     * @param listener
     */
    @Reference(policyOption = ReferencePolicyOption.GREEDY,
               cardinality = ReferenceCardinality.OPTIONAL)
    protected void setMessageRouterConfigListener(MessageRouterConfigListener listener) {
        this.msgRouterConfigListener = listener;
        updateListeners();
    }

    /**
     * Activate and store initial config values, then update listener services.
     *
     * @param config
     */
    protected void activate(Map<String, Object> config) {
        zosLoggingConfig = new Hashtable<String, Object>(config);
        updateListeners();
    }

    /**
     * Deactivate by clearing saved config and listener references.
     *
     * @param config
     * @param reason
     */
    protected void deactivate(Map<String, Object> config, int reason) {
        zosLoggingConfig = null;
        msgRouterConfigListener = null;
        zosLoggingConfigListener = null;
    }

    /**
     * Store new config values and update listener services.
     *
     * @param config
     */
    protected void modified(Map<String, Object> config) {
        zosLoggingConfig = new Hashtable<String, Object>(config);
        updateListeners();
    }

    /**
     * Push the current config values to all listeners we have references to.
     */
    private void updateListeners() {
        // Avoid registering the log handlers until we've read our config and we can
        // tell base logging services which messages we care about.
        if ((zosLoggingConfig != null) && (zosLoggingConfigListener != null) && (msgRouterConfigListener != null)) {

            // Initialize enableLogToMVS to its default, false, because
            // autounboxing causes NPEs later down the line if it's not set.
            // It might not be set in cases where we tear down the server
            // early, for example, when an angel is required, but not connected.
            Object tmpLogMVS = zosLoggingConfig.get("enableLogToMVS");
            if (null == tmpLogMVS) {
                zosLoggingConfig.put("enableLogToMVS", false);
            }

            Object tmpDisableWtoMessages = zosLoggingConfig.get("disableWtoMessages");
            if (null == tmpDisableWtoMessages) {
                zosLoggingConfig.put("disableWtoMessages", false);
            }

            Object tmpDisableHardcopyMessages = zosLoggingConfig.get("disableHardcopyMessages");
            if (null == tmpDisableHardcopyMessages) {
                zosLoggingConfig.put("disableHardcopyMessages", false);
            }

            // Tell base logging services which messages the WTO log handler cares about,
            // in addition to any messages referenced in MessageRouter.properties
            String wtoMessages = (String) zosLoggingConfig.get("wtoMessage");
            if (wtoMessages != null) {
                msgRouterConfigListener.updateMessageListForHandler(wtoMessages, "WTO");
            }

            // Tell base logging services which messages the HARDCOPY log handler cares about,
            // in addition to any messages referenced in MessageRouter.properties
            String hardcopyMessages = (String) zosLoggingConfig.get("hardCopyMessage");
            if (hardcopyMessages != null) {
                msgRouterConfigListener.updateMessageListForHandler(hardcopyMessages, "HARDCOPY");
            }

            // Now that logging knows what to send us, go register the log handlers.
            zosLoggingConfigListener.updated(zosLoggingConfig);
        }
    }

}
