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
import static com.ibm.websphere.ras.Tr.entry;
import static com.ibm.websphere.ras.Tr.exit;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Stream.concat;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.application.lifecycle.ApplicationPrereq;
import com.ibm.wsspi.application.lifecycle.ApplicationStartBarrier;
import com.ibm.wsspi.logging.Introspector;

/**
 * Immediate component to track prereqs as they appear.
 */
@Component(
           immediate = true,
           configurationPid = "com.ibm.ws.app.prereqs",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = "service.vendor=IBM")
public class ApplicationPrereqMonitor implements Introspector {
    public static final TraceComponent tc = Tr.register(ApplicationPrereqMonitor.class);
    private static final String PREREQ_ID_PROP = "application.prereq.id";
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final int version = counter.incrementAndGet();
    private final ConfigurationAdmin configAdmin;
    private volatile Set<String> declaredPrereqs;
    // OSGi allows unbind calls to happen after logically subsequent bind calls, so
    // Set<> semantics will not suffice.
    // List<> allows duplicates, so the eventual result of out-of-order operations
    // will be correct.
    private final List<String> errors = new ArrayList<>();
    private final List<String> pendingPrereqs = new ArrayList<>();
    private final List<String> realizedPrereqs = new ArrayList<>();
    private int appStartBarrierBindCount, appStartBarrierUnbindCount;

    /**
     * Use constructor parameter references to ensure that mandatory references are
     * supplied first.
     */
    @Activate
    public ApplicationPrereqMonitor(@Reference ConfigurationAdmin configAdmin, Map<String, Object> properties) {
        this.configAdmin = configAdmin;
        this.declaredPrereqs = findDeclarations(properties);
    }

    @Modified
    synchronized void modified(Map<String, Object> properties) {
        Set<String> oldSet = declaredPrereqs;
        Set<String> newSet = findDeclarations(properties);
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
            if (unmodified.size() > 0) debug(this, tc, "Prereq declarations unmodified:", unmodified);
            if (removed.size() > 0) debug(this, tc, "Prereq declarations removed:", removed);
            if (added.size() > 0) debug(this, tc, "Prereq declarations added:", added);
        }
    }

    @Deactivate
    void deactivate() {
        // instrumentation will trace entry and exit
    }

    private Set<String> findDeclarations(Map<String, Object> properties) {
        final Set<String> decls = Optional.of(properties)
                .map(props -> props.get("applicationPrereqDeclarations"))
                .map(String[].class::cast)
                .map(Stream::of)
                .orElse(Stream.empty())
                .map(this::servicePidToPrereqId)
                .filter(Objects::nonNull)
                .collect(TreeSet::new, Set::add,Set::addAll);
        return unmodifiableSet(decls);
    }

    /**
     * Convert the provided service pid to a configuration id. This assumes that the
     * service pid is valid at the point this is called. If not, a string indicating
     * an error will be returned, and a suitable FFDC will be generated.
     */
    @Trivial
    @FFDCIgnore(Exception.class)
    private String servicePidToPrereqId(String servicePid) {
        try {
            Configuration[] configs = this.configAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + servicePid + ")");

            // We may be processing a pid from a deleted application
            if (configs == null) throw new IllegalStateException("No configs found matching servicePid=" + servicePid);
            if (configs.length > 1) throw new IllegalStateException("Non unique servicePid=" + servicePid + " matched configs=" + Arrays.toString(configs));

            // even if we get to here, the config we successfully retrieved may have been
            // deleted, which will result in an IllegalStateException
            return requireNonNull((String) configs[0].getProperties().get(PREREQ_ID_PROP));
        } catch (Exception e) {
            // Do not AutoFFDC this (see the @FFDCIgnore above)
            errors.add("" + e);
            return null;
        }
    }

    /**
     * The Service Component Registry can bind a ServiceReference before it is
     * realised, so this represents an intermediate stage between declaration and
     * realisation.
     */
    @Reference(cardinality = MULTIPLE, policy = DYNAMIC, policyOption = GREEDY)
    synchronized void addPendingPrereq(ServiceReference<ApplicationPrereq> ref, Map<String, Object> props) {
        pendingPrereqs.add(getPrereqId(props));
    }

    synchronized void removePendingPrereq(ServiceReference<ApplicationPrereq> ref, Map<String, Object> props) {
        pendingPrereqs.remove(getPrereqId(props));
    }

    @Reference(cardinality = MULTIPLE, policy = DYNAMIC, policyOption = GREEDY)
    synchronized void addRealizedPrereq(ApplicationPrereq applicationPrereq, Map<String, Object> props) {
        realizedPrereqs.add(getPrereqId(props));
    }

    synchronized void removeRealizedPrereq(ApplicationPrereq applicationPrereq, Map<String, Object> props) {
        realizedPrereqs.remove(getPrereqId(props));
    }
    
    private String getPrereqId(Map<String, Object> props) {
        return requireNonNull((String) props.get(PREREQ_ID_PROP));
    }

    @Reference(cardinality = MULTIPLE, policy = DYNAMIC, policyOption = GREEDY)
    synchronized void addStartBarrier(ApplicationStartBarrier barrier, Map<String, Object> properties) {
        // ApplicationPrereqMonitor may not YET have received notification of the config update that allowed ApplicationStartBarrier to be created.
        // As a workaround, call modified() now before processing the rest of the bind logic.
        // Pass in the properties for ApplicationStartBarrier which are from the same config element as ApplicationPrereqMonitor.
        // Any trailing call to modified() from OSGi declarative services for this same config change should observe zero change and do nothing.
        modified(properties);

        // Process the call to bind: increment the bind count, log some debug trace, and check for errors
        appStartBarrierBindCount++;
        Set<String> pending = new TreeSet<>(pendingPrereqs), realized = new TreeSet<>(realizedPrereqs);
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) {
            debug(this, tc, "Declared prereqs: " + declaredPrereqs);
            debug(this, tc, "Pending prereqs:  " + pending);
            debug(this, tc, "Realized prereqs: " + realized);
        }
        if (declaredPrereqs.equals(realized)) return;
        if (appStartBarrierBindCount > appStartBarrierUnbindCount + 1) {
            // This bind call has arrived out of order with respect to a logically preceding unbind call.
            // Clear the errors now, and not when the unbind happens.
            errors.clear();
        } else {
            Set<String> missing = new TreeSet<>(declaredPrereqs), blocked = pending, extra = realized;
            blocked.removeAll(realized);
            missing.removeAll(realized);
            extra.removeAll(declaredPrereqs);
            errors.add("addStartBarrier: prereq mismatch detected:"
                    + "\n\tmissing: " + missing
                    + "\n\tblocked: " + blocked
                    + "\n\textra:   " + extra);
        }
    }

    synchronized void removeStartBarrier(ApplicationStartBarrier ready) {
        appStartBarrierUnbindCount++;
        // StartBarrier is being removed, so either the server is shutting down or being reconfigured.
        // If the unbind arrived in sequence, clear the error history
        if (appStartBarrierBindCount == appStartBarrierUnbindCount) errors.clear(); 
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
        return getIntrospectorName().replaceAll(".", "=") + "\n"
               + "List the declared (D), pending (P), and realized (R) application prereqs.\n"
               + "Applications cannot start until all the declared prereqs become available (i.e. are realized).";
    }

    @Override
    public synchronized void introspect(PrintWriter out) throws Exception {
        out.println();
        out.println("=Detected Application Prereqs=");
        concat(concat(declaredPrereqs.stream(), realizedPrereqs.stream()), pendingPrereqs.stream())
                .filter(Objects::nonNull)
                .sorted()
                .distinct()
                .sequential()
                .peek(s -> out.print('['))
                .peek(s -> out.print(declaredPrereqs.contains(s) ? 'D' : ' '))
                .peek(s -> out.print(realizedPrereqs.contains(s) ? 'R' : pendingPrereqs.contains(s) ? 'P' : ' '))
                .peek(s -> out.print("] "))
                .forEach(out::println);
        out.println();
        out.println("=App Start Barrier tracking=");
        out.println("Bind calls:   " + appStartBarrierBindCount);
        out.println("Unbind calls: " + appStartBarrierUnbindCount);
        out.println();
        out.println("=Errors=");
        errors.forEach(out::println);
        if (errors.isEmpty())
            out.println("No errors recorded.");
    }
}
