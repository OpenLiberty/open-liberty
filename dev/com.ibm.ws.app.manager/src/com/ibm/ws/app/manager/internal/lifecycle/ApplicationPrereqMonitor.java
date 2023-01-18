/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;
import com.ibm.wsspi.logging.Introspector;

/**
 * Immediate component to track prereqs as they appear.
 */
@Component(immediate = true,
           configurationPid = "com.ibm.ws.app.prereqmonitor",
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ApplicationPrereqMonitor implements ConfigurationListener, Introspector {
    private static class ConfigurationOutOfDateException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ConfigurationOutOfDateException(Throwable cause) {
            super(cause);
        }
    }

    public static final TraceComponent tc = Tr.register(ApplicationPrereqMonitor.class);
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final int version = counter.incrementAndGet();
    private final ConfigurationAdmin configurationAdmin;
    private final Set<String> declaredPrereqs;
    private final Set<String> realizedPrereqs = new TreeSet<>();

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

    /**
     * Convert the provided service pid to a configuration id.
     * This assumes that the service pid is valid at the point this is called.
     * If not, a string indicating an error will be returned, and a suitable
     * FFDC will be generated.
     */
    @Trivial
    @FFDCIgnore(IllegalStateException.class)
    private String servicePidToConfigId(String servicePid) {

        try {
            Configuration[] configs = this.configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + servicePid + ")");

            if (configs == null)
                // We may be processing a pid from a deleted application
                throw new IllegalStateException("No configs found matching servicePid=" + servicePid);
            if (configs.length > 1)
                throw new IllegalStateException("Non unique servicePid=" + servicePid + " matched configs=" + Arrays.toString(configs));

            // even if we get to here, the config we successfully retrieved may have been deleted
            // which will result in an IllegalStateException
            String id = (String) configs[0].getProperties().get("id");
            return id;

        } catch (IOException | InvalidSyntaxException | IllegalStateException e) {
            // Do not AutoFFDC this (see the @FFDCIgnore above)
            try {
                throw new ConfigurationOutOfDateException(e);
            } catch (ConfigurationOutOfDateException cod) {
                // AutoFFDC this instead
            }
            // The config received is not reliable,
            // so don't use it for error detection.
            configModified = true;
            return "Error converting servicePid: " + servicePid;
        }

    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    synchronized void setApplicationPrereq(ApplicationPrereq applicationPrereq) {
        realizedPrereqs.add(applicationPrereq.getApplicationPrereqID());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, this + " Prereq added:" + applicationPrereq.getClass().getName()
                         + "\nConfigured:" + declaredPrereqs
                         + "\n  Realized:" + realizedPrereqs);
        }

        // If the configuration has been modified we cannot reliably detect surplus Prereqs.
        if (configModified)
            return;

        // Check for unexpected Prereqs.
        // If there are no unexpected Prereqs, we don't raise an error.
        if (declaredPrereqs.containsAll(realizedPrereqs))
            return;

        // The configuration has not been modified but we have at least one prereq which we did not expect.
        // Caused by implementing ApplicationPrereq without,
        // Either adding application prereq in DefaultInstances,
        // Or add ibm:objectClass="com.ibm.wsspi.application.lifecycle.ApplicationPrereq" to the OCD in metatype.xml.
        Set<String> surplusPrereqs = new TreeSet<>(realizedPrereqs);
        surplusPrereqs.removeAll(declaredPrereqs);
        try {
            throw new IllegalStateException("Undeclared Prereqs:" + surplusPrereqs);
        } catch (IllegalStateException illegalStateException) {
            // AutoFFDC.
            throw illegalStateException;
        }

    }

    synchronized void unsetApplicationPrereq(ApplicationPrereq applicationPrereq) {
        realizedPrereqs.remove(applicationPrereq.getApplicationPrereqID());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, this + " Prereq removed:" + applicationPrereq.getClass().getName()
                         + "\nConfigured:" + declaredPrereqs
                         + "\n  Realized:" + realizedPrereqs);
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

    @Override
    public String getIntrospectorName() {
        return getClass().getSimpleName();
    }

    @Override
    public String getIntrospectorDescription() {
        return String.format("List the declared (D) and the realized (R) application prereqs.%n" +
                             "Application Handlers cannot start until all the declared prereqs become available (i.e. are realized).");
    }

    @Override
    public void introspect(PrintWriter out) throws Exception {
        Stream.concat(declaredPrereqs.stream(),
                      realizedPrereqs.stream()).sorted().distinct().sequential().peek(s -> out.print('[')).peek(s -> out.print(declaredPrereqs.contains(s) ? 'D' : ' ')).peek(s -> out.print(realizedPrereqs.contains(s) ? 'R' : ' ')).peek(s -> out.print("] ")).forEach(out::println);

    }
}
