/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal.lifecycle;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;

/**
 * Immediate component to track prereqs as they appear.
 */
@Component(immediate = true,
           configurationPid = "com.ibm.ws.app.prereqmonitor",
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ApplicationPrereqMonitor {
    public static final TraceComponent tc = Tr.register(ApplicationPrereqMonitor.class);
    private final ConfigurationAdmin configurationAdmin;
    private final Set<String> declaredPrereqs;
    private final Set<String> realisedPrereqs = new TreeSet<>();

    /**
     * Use constructor parameter references to ensure that mandatory references are supplied first.
     *
     * @param configurationAdmin must be supplied before any calls to setApplicationPrereq
     */
    @Activate
    public ApplicationPrereqMonitor(@Reference ConfigurationAdmin configurationAdmin, Map<String, Object> properties) {
        this.configurationAdmin = configurationAdmin;

        String[] pids = (String[]) properties.get("applicationPrereqDeclarations");
        declaredPrereqs = unmodifiableSet(Stream.of(pids).map(this::servicePidToConfigId).collect(toSet()));

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Known configured application Prereq Pids:" + Arrays.toString(pids)
                         + "\nids:" + declaredPrereqs);
        }
    }

    private String servicePidToConfigId(String servicePid) {
        Configuration[] configs;
        try {
            configs = this.configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + servicePid + ")");

            if (configs == null)
                throw new IllegalStateException("No configs found matching servicePid=" + servicePid);
            if (configs.length > 1)
                throw new IllegalStateException("Non unique servicePid=" + servicePid + " matched configs=" + Arrays.toString(configs));

        } catch (IOException | InvalidSyntaxException | IllegalStateException e) {
            // AutoFFDC.
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return "Error listing prereqs:" + sw;
        }

        String id = (String) configs[0].getProperties().get("id");
        return id;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    synchronized void setApplicationPrereq(ApplicationPrereq applicationPrereq) {
        realisedPrereqs.add(applicationPrereq.getApplicationPrereqID());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Prereq added:" + applicationPrereq.getClass().getName()
                         + "\nConfigured:" + declaredPrereqs
                         + "\n  Realised:" + realisedPrereqs);
        }

        // Check for unexpected Prereqs.
        // If there are insufficient realised Prereqs, we don't raise an error but we will continue waiting until more Prereqs are realised.
        if (!declaredPrereqs.containsAll(realisedPrereqs)) {
            try {
                throw new IllegalStateException("Invalid Prereqs. Configured:" + declaredPrereqs + " Realised:" + realisedPrereqs);
            } catch (IllegalStateException illegalStateException) {
                // AutoFFDC.
                throw illegalStateException;
            }
        }
    }

    synchronized void unsetApplicationPrereq(ApplicationPrereq applicationPrereq) {
        realisedPrereqs.remove(applicationPrereq.getApplicationPrereqID());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Prereq removed:" + applicationPrereq.getClass().getName()
                         + "\nConfigured:" + declaredPrereqs
                         + "\n  Realised:" + realisedPrereqs);
        }
    }
}
