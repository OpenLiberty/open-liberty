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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;

/**
 * Immediate component to track prereqs as they appear.
 */
@Component(immediate = true,
           configurationPid = "com.ibm.ws.app.prereqmonitor",
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ApplicationPrereqMonitor implements ConfigurationListener {
    public static final TraceComponent tc = Tr.register(ApplicationPrereqMonitor.class);
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final int version = counter.incrementAndGet();
    private final ConfigurationAdmin configurationAdmin;
    private final Set<String> declaredPrereqs;
    private final Set<String> realisedPrereqs = new TreeSet<>();

    private boolean configModified = false;

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
            Tr.debug(tc, this
                         + "\nKnown configured application Prereq Pids:" + Arrays.toString(pids)
                         + "\nids:" + declaredPrereqs);
        }
    }

    @Deactivate
    void deactivate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, this + " deactivate");
        }
    }

    @Trivial
    private String servicePidToConfigId(String servicePid) {
        Configuration[] configs;
        try {
            configs = this.configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + servicePid + ")");

            if (configs == null) {
                // This is not strictly an error given that we may be processing a pid from a deleted application
                Tr.debug(tc, "No configs found matching servicePid=" + servicePid);
                return "PID not found: " + servicePid;
            }
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
            Tr.debug(tc, this + " Prereq added:" + applicationPrereq.getClass().getName()
                         + "\nConfigured:" + declaredPrereqs
                         + "\n  Realised:" + realisedPrereqs);
        }

        // If the configuration has been modified we cannot reliably detect surplus Prereqs.
        if (configModified)
            return;

        // Check for unexpected Prereqs.
        // If there are no unexpected Prereqs, we don't raise an error.
        if (declaredPrereqs.containsAll(realisedPrereqs))
            return;

        // The configuration has not been modified but we have at least one prereq which we did not expect.
        // Caused by implementing ApplicationPrereq without,
        // Either adding application prereq in DefaultInstances,
        // Or add ibm:objectClass="com.ibm.wsspi.application.lifecycle.ApplicationPrereq" to the OCD in metatype.xml.
        Set<String> surplusPrereqs = new TreeSet<>(realisedPrereqs);
        surplusPrereqs.removeAll(declaredPrereqs);
        try {
            throw new IllegalStateException("Undeclared Prereqs:" + surplusPrereqs);
        } catch (IllegalStateException illegalStateException) {
            // AutoFFDC.
            throw illegalStateException;
        }

    }

    synchronized void unsetApplicationPrereq(ApplicationPrereq applicationPrereq) {
        realisedPrereqs.remove(applicationPrereq.getApplicationPrereqID());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, this + " Prereq removed:" + applicationPrereq.getClass().getName()
                         + "\nConfigured:" + declaredPrereqs
                         + "\n  Realised:" + realisedPrereqs);
        }
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, this + " Configuration changed, stop reporting invalid prereqs");
        }

        // It looks like we don't have permission to introspect all configuration events.
        // Eg. [WARNING ] CWWKG0101W: The configuration with the persisted identity (PID) com.ibm.ws.app.prereqmonitor is bound to the bundle com.ibm.ws.app.manager. The configuration is not visible to other bundles.
        // So we have to assume that this.declaredPrereqs is no longer current.

        configModified = true;
    }

    @Override
    public String toString() {
        return ApplicationPrereqMonitor.class.getName() + "#" + version;
    }
}
