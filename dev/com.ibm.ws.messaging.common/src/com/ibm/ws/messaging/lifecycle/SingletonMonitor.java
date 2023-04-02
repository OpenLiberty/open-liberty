/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.messaging.lifecycle;

import static com.ibm.websphere.ras.Tr.debug;
import static com.ibm.websphere.ras.Tr.entry;
import static com.ibm.websphere.ras.Tr.exit;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Stream.concat;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.osgi.framework.InvalidSyntaxException;
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
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.logging.Introspector;

/**
 * This component tracks {@link Singleton} services as they arrive and depart.
 * If SingletonsReady does not become available, SingletonMonitor will show the progress so far.
 * It also dumps the current state of expected and realised {@link Singleton}s when a server dump is taken.
 * 
 * @see SingletonsReady
 */
@Component(
        immediate = true, 
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        configurationPid = "com.ibm.ws.messaging.lifecycle.singletons",
        property = {
                "osgi.command.scope=sib",
                "osgi.command.function=singletons",
                "service.vendor=IBM"
        })
public class SingletonMonitor implements Introspector {
    public static final TraceComponent tc = Tr.register(SingletonMonitor.class);
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final int version = counter.incrementAndGet();
    private final ConfigurationAdmin configAdmin;
    private volatile Set<String> declaredSingletons;
    // OSGi allows unbind calls to happen after logically subsequent bind calls, so Set<> semantics will not suffice.
    // List<> allows duplicates, so the eventual result of out-of-order operations will be correct.
    private final List<String> errors = new ArrayList<>();
    private final List<String> realizedSingletons = new ArrayList<>();
    private final List<String> pendingSingletons = new ArrayList<>();
    private int singletonsReadyBindCount, singletonsReadyUnbindCount;

    @Activate
    public SingletonMonitor(@Reference(name="configAdmin") ConfigurationAdmin configAdmin) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(tc, "<init>");
        this.configAdmin = configAdmin;
        this.declaredSingletons = unmodifiableSet(findDeclaredSingletons());
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(tc, "<init>");
    }

    /**
     * Get the current set of ids of declared SingletonAgents.
     */
    private Set<String> findDeclaredSingletons() {
        // The singleton declaration config PIDs could be retrieved from this component's config properties.
        // The PIDs could then be passed to configAdmin.getConfiguration(pid).getProperties().get("id") to retrieve the id.
        // However, those declarations are made in other bundles and ConfigAdmin prohibits this bundle from looking them up using getConfiguration().
        // Therefore the declarations are retrieved using configAdmin.listConfigurations() instead.
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "findDeclaredSingletons");
        Configuration[] configs;
        try {
            configs = configAdmin.listConfigurations("(service.factoryPid=com.ibm.ws.messaging.lifecycle.SingletonAgent)");
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "Singleton configs: ", Arrays.toString(configs));
        } catch (IOException | InvalidSyntaxException e) {
            FFDCFilter.processException(e, "com.ibm.ws.messaging.lifecycle.SingletonMonitor.findDeclaredSingletons", "list configs");
            throw new LifecycleError("Could not list declared singletons", e);
        }
        Set<String> result = Stream.of(configs)
                .map(this::retrieveId)
                .filter(Objects::nonNull)
                .collect(TreeSet::new, Set::add, Set::addAll);
        result = unmodifiableSet(result);
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "findDeclaredSingletons", result);
        return result;
    }

    private String retrieveId(Configuration config) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "retrieveId", config);
        String result = null;
        try {
            Dictionary<String, Object> dict = config.getProperties();
            Object definition = dict.get("id");
            result = (String) definition;
        } catch (Exception e) {
            errors.add("" + e);
            if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, "retrieveId", e);
        }
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "retrieveId", result);
        return result;
    }

    @Modified
    public synchronized void modified() {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "modified");
        Set<String> oldSet = declaredSingletons;
        Set<String> newSet = findDeclaredSingletons();
        if (newSet.equals(oldSet)) return;
        // Now we know there are some changes.
        this.declaredSingletons = newSet;
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) {
            // calculate the diffs
            Set<String> removed = new TreeSet<>(oldSet), added = new TreeSet<>(newSet), unmodified = new TreeSet<>(oldSet);
            removed.removeAll(newSet);
            added.removeAll(oldSet);
            unmodified.removeAll(removed);
            // trace the diffs
            if (unmodified.size() > 0) debug(this, tc, "Singleton declarations unmodified:", unmodified);
            if (removed.size() > 0) debug(this, tc, "Singleton declarations removed:", removed);
            if (added.size() > 0) debug(this, tc, "Singleton declarations added:", added);
        }
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "modified");
    }

    @Deactivate
    public void deactivate() {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) {
            entry(this, tc, "deactivate");
            exit(this, tc, "deactivate");
        }
    }

    /**
     * The Service Component Registry can bind a ServiceReference before it is realised,
     * so this represents an intermediate stage between declaration and realisation.
     */
    @Reference(cardinality = MULTIPLE, policy = DYNAMIC, policyOption = GREEDY)
    synchronized void addPendingSingleton(ServiceReference<Singleton> ref) {
        final String type = (String)ref.getProperty("type");
        pendingSingletons.add(type);
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, String.format("Pending Singleton added: %s", type));
    }

    synchronized void removePendingSingleton(ServiceReference<Singleton> ref) {
        final String type = (String)ref.getProperty("type");
        pendingSingletons.remove(type);
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, String.format("Pending Singleton removed: %s", type));
    }

    @Reference(cardinality = MULTIPLE, policy = DYNAMIC, policyOption = GREEDY)
    synchronized void addRealizedSingleton(Singleton singleton, Map<String, Object> properties) {
        final String type = (String)properties.get("type");
        realizedSingletons.add(type);
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, String.format("Realized Singleton added: %s", type));
    }

    synchronized void removeRealizedSingleton(Singleton singleton, Map<String, Object> properties) {
        final String type = (String)properties.get("type");
        realizedSingletons.remove(type);
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, String.format("Realized Singleton removed: %s", type));
    }

    @Reference(cardinality = MULTIPLE, policy = DYNAMIC, policyOption = GREEDY)
    synchronized void addSingletonsReady(SingletonsReady ready) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "addSingletonsReady", new Object[] {ready});
        // SingletonMonitor may not YET have received notification of the config update that allowed SingletonsReady to be created.
        // As a workaround, call modified() now before processing the rest of the bind logic.
        // Any trailing call to modified() from OSGi declarative services for this same configuration should observe zero change and do nothing.
        modified();

        // Process the call to bind: increment the bind count, log some debug trace, and check for errors
        singletonsReadyBindCount++;
        Set<String> pending = new TreeSet<>(pendingSingletons), realized = new TreeSet<>(realizedSingletons);
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) {
            debug(this, tc, "Declared singletons: " + declaredSingletons);
            debug(this, tc, "Pending singletons:  " + pending);
            debug(this, tc, "Realized singletons: " + realized);
        }
        if (declaredSingletons.equals(realized)) return;
        Set<String> missing = new TreeSet<>(declaredSingletons), blocked = pending, extra = realized;
        blocked.removeAll(realized);
        missing.removeAll(realized);
        extra.removeAll(declaredSingletons);
        Exception e = new IllegalStateException("Singleton mismatch detected:"
                + "\n\tmissing: " + missing
                + "\n\tblocked: " + blocked
                + "\n\textra:   " + extra);
        FFDCFilter.processException(e, SingletonMonitor.class.getName(), "addSingletonsReady");
        if (singletonsReadyBindCount > singletonsReadyUnbindCount + 1) {
            // This bind call has arrived out of order with respect to a logicially preceding unbind call.
            // Clear the errors now, and not when the unbind happens.
            errors.clear();
        }
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "addSingletonsReady");

    }

    synchronized void removeSingletonsReady(SingletonsReady ready) {
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "removeSingletonsReady", new Object[] {ready});
        singletonsReadyUnbindCount++;
        // SingletonsReady is being removed, so either the server is shutting down or being reconfigured.
        if (singletonsReadyBindCount > singletonsReadyUnbindCount) {
            // The unbind call has arrived out of order, so do not reset the error list.
            if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "removeSingletonsReady (out of order)");
            return;
        }
        // Reset the error list so only errors for the current configuration are recorded
        errors.clear();
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "removeSingletonsReady (in order)");
    }

    @Override
    public String toString() {
        return SingletonMonitor.class.getName() + "#" + version;
    }

    @Override
    public String getIntrospectorName() {
        return "Messaging" + getClass().getSimpleName();
    }

    @Override
    public String getIntrospectorDescription() {
        return getIntrospectorName().replaceAll(".", "=") + "\n"
                + "List the declared (D), pending (P), and realized (R) messaging singletons.\n"
                + "Messaging cannot start until all the declared singletons become available (i.e. are realized).";
    }

    @Override
    public synchronized void introspect(PrintWriter out) throws Exception {
        // dump the status of singletons
        out.println();
        out.println("=Detected Singletons=");
        concat(concat(declaredSingletons.stream(), realizedSingletons.stream()), pendingSingletons.stream())
            .sorted()
            .distinct()
            .sequential()
            .peek(s -> out.print('['))
            .peek(s -> out.print(declaredSingletons.contains(s) ? 'D' : ' '))
            .peek(s -> out.print(realizedSingletons.contains(s) ? 'R' : pendingSingletons.contains(s) ? 'P' : ' '))
            .peek(s -> out.print("] "))
            .forEach(out::println);
        out.println();
        out.println("=SingletonsReady tracking=");
        out.println("Bind calls:   " + singletonsReadyBindCount);
        out.println("Unbind calls: " + singletonsReadyUnbindCount);
        out.println();
        out.println("=Errors=");
        errors.forEach(out::println);
        if (errors.isEmpty()) out.println("No errors recorded.");
    }

    public void singletons() throws Exception {
        System.out.println(getIntrospectorName());
        System.out.println(getIntrospectorDescription());
        try (PrintWriter pw = new PrintWriter(System.out)) {
            introspect(pw);
        }
    }
}
