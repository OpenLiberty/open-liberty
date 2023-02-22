/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
package com.ibm.ws.app.manager.internal.lifecycle;

import static com.ibm.websphere.ras.Tr.debug;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;

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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
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
           configurationPid = "com.ibm.ws.app.prereqs",
           configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ApplicationPrereqMonitor implements Introspector {
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
    private Set<String> declaredPrereqs = new TreeSet<>();
    private final Set<String> realizedPrereqs = new TreeSet<>();

    private boolean configInconsistent = false;

    /**
     * Use constructor parameter references to ensure that mandatory references are supplied first.
     *
     * @param configurationAdmin must be supplied before any calls to setApplicationPrereq
     */
    @Activate
    public ApplicationPrereqMonitor(@Reference ConfigurationAdmin configurationAdmin, Map<String, Object> properties) {
        this.configurationAdmin = configurationAdmin;

        String[] pids = (String[]) properties.get("applicationPrereqDeclarations");
        Stream.of(pids).map(this::servicePidToConfigId).forEach(declaredPrereqs::add);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, this
                         + "\nKnown configured application Prereq Pids:" + Arrays.toString(pids)
                         + "\nids:" + declaredPrereqs);
        }
    }

    @Modified
    synchronized void modified(Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "modified");
        }

        // Assume we now have a new consistent configuration, until proven otherwise.
        configInconsistent = false;

        String[] pids = (String[]) properties.get("applicationPrereqDeclarations");
        Set<String> oldSet = declaredPrereqs;
        Set<String> newSet = Stream.of(pids).map(this::servicePidToConfigId).collect(TreeSet::new, Set::add, Set::addAll);
        if (newSet.equals(oldSet))
            return;
        // Now we know there are some changes.
        this.declaredPrereqs = newSet;
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) {
            // calculate the diffs
            Set<String> removed = new TreeSet<>(oldSet), added = new TreeSet<>(newSet), unmodified = new TreeSet<>(oldSet);
            removed.removeAll(newSet);
            added.removeAll(oldSet);
            unmodified.removeAll(removed);
            // trace the diffs
            if (unmodified.size() > 0)
                debug(this, tc, "Prereq declarations unmodified:", unmodified);
            if (removed.size() > 0)
                debug(this, tc, "Prereq declarations removed:", removed);
            if (added.size() > 0)
                debug(this, tc, "Prereq declarations added:", added);
        }
        if (isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.exit(this, tc, "modified");
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
            // The config received is not reliable, so don't use it for error detection.
            configInconsistent = true;
            return "Error converting servicePid: " + servicePid;
        }

    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
               // The name must differ from the name in the metatype, so that cardinality.minimum does not apply
               name = "candidateApplicationPrereq",
               unbind = "unsetApplicationPrereq",
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    synchronized void setApplicationPrereq(ApplicationPrereq applicationPrereq) {
        realizedPrereqs.add(applicationPrereq.getApplicationPrereqID());
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, this + " Prereq added:" + applicationPrereq.getClass().getName()
                         + "\nConfigured:" + declaredPrereqs
                         + "\n  Realized:" + realizedPrereqs);
        }

        // If the configuration update processing failed we cannot reliably detect surplus Prereqs.
        if (configInconsistent)
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
    public synchronized void introspect(PrintWriter out) throws Exception {
        Stream.concat(declaredPrereqs.stream(),
                      realizedPrereqs.stream()).sorted().distinct().sequential().peek(s -> out.print('[')).peek(s -> out.print(declaredPrereqs.contains(s) ? 'D' : ' ')).peek(s -> out.print(realizedPrereqs.contains(s) ? 'R' : ' ')).peek(s -> out.print("] ")).forEach(out::println);

    }
}
