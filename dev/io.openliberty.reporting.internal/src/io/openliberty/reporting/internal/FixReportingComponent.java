/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.reporting.internal;

import static org.osgi.service.condition.Condition.CONDITION_ID;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.SatisfyingConditionTarget;

import com.ibm.websphere.kernel.server.ServerInfoMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.ws.kernel.feature.FixManager;
import com.ibm.ws.kernel.feature.ServerStartedPhase2;

import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * <p>
 * The main entry point into CVE Reporting service
 * </p>
 */
@Component(immediate = true, configurationPid = "io.openliberty.reporting.CVEReportingComponent", configurationPolicy = ConfigurationPolicy.OPTIONAL)
@SatisfyingConditionTarget("(" + CONDITION_ID + "=" + CheckpointPhase.CONDITION_PROCESS_RUNNING_ID + ")")
public class FixReportingComponent {

    ReporterTask reporterTask;

    ScheduledFuture<?> future;

    ScheduledExecutorService scheduledExecutor;

    private static final TraceComponent tc = Tr.register(FixReportingComponent.class);

    /**
     * <p>
     * CVE Reporting will run by default unless disabled in server.xml.
     * </p>
     *
     * @param ignored
     * @param properties
     * @param justAMarker
     * @param featureProvisioner
     * @param fixManager
     * @param serverInfo
     * @param scheduledExecutor
     */
    @Activate
    public FixReportingComponent(ComponentContext ignored, Map<String, Object> properties,
                                 @Reference ServerStartedPhase2 justAMarker, @Reference final FeatureProvisioner featureProvisioner,
                                 @Reference final FixManager fixManager, @Reference final ServerInfoMBean serverInfo,
                                 @Reference(target = "(deferrable=true)") final ScheduledExecutorService scheduledExecutor) {

        this.scheduledExecutor = scheduledExecutor;

        reporterTask = new ReporterTask(featureProvisioner, fixManager, serverInfo, properties);

        if (isEnabled(properties)) {
            Tr.info(tc, "CWWKF1700.reporting.is.enabled");
            future = scheduledExecutor.scheduleAtFixedRate(reporterTask, 0, 1, TimeUnit.DAYS);
        } else {
            Tr.info(tc, "CWWKF1701.reporting.is.disabled");
        }

    }

    /**
     *
     * @param properties
     */
    @Modified
    protected void modified(Map<String, Object> properties) {
        if (isEnabled(properties)) {
            if (future == null || future.isDone()) {
                Tr.info(tc, "CWWKF1700.reporting.is.enabled");
                future = scheduledExecutor.scheduleAtFixedRate(reporterTask, 0, 1, TimeUnit.DAYS);
            }
        } else {
            if (future != null && !future.isDone()) {
                Tr.info(tc, "CWWKF1701.reporting.is.disabled");
                future.cancel(false);
                future = null;
            }
        }

    }

    @Deactivate
    protected void deactivate() {
        if (future != null && !future.isDone()) {
            future.cancel(false);
            future = null;
        }
    }

    /**
     * <p>
     * Check server.xml to see if any are explicitly disabling CVE Reporting.
     * </p>
     *
     * @param properties Map<String,String>
     * @return enabled boolean
     */
    private static boolean isEnabled(Map<String, Object> properties) {

        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                Boolean enabled = false;
                enabled = Boolean.getBoolean("cve.insight.enabled");
                // MUST BE ENABLED BEFORE BETA
                // if (ProductInfo.getBetaEdition()) {
                // check config
                if (enabled) {
                    return !Boolean.FALSE.equals(properties.get("enabled"));
                }
                // }
                return false;
            }
        });

    }
}
