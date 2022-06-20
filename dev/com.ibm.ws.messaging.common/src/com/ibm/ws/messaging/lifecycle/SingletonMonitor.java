/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.lifecycle;

import static com.ibm.websphere.ras.Tr.debug;
import static com.ibm.websphere.ras.TraceComponent.isAnyTracingEnabled;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.logging.Introspector;

/**
 * This component tracks {@link Singleton} services as they arrive and depart.
 * It also dumps the current state of expected and realised {@link Singleton}s when a server dump is taken.
 * 
 * @see SingletonsReady
 */
@Component(
        immediate = true, 
        configurationPolicy = REQUIRE,
        property = {
                "osgi.command.scope=sib",
                "osgi.command.function=singletons",
                "service.vendor=IBM"
        })
public class SingletonMonitor implements Introspector {
    public static final TraceComponent tc = Tr.register(SingletonMonitor.class);
    private static final AtomicInteger counter = new AtomicInteger(0);
    private final int version = counter.incrementAndGet();
    private final ConfigurationAdmin configurationAdmin;
    private final Set<String> declaredSingletons;
    private final Set<String> realizedSingletons = new TreeSet<>();

    /**
     * Use constructor parameter references to ensure that mandatory references are supplied first.
     *
     * @param configurationAdmin must be supplied before any calls to addSingleton
     */
    @Activate
    public SingletonMonitor(@Reference ConfigurationAdmin configurationAdmin, Map<String, Object> properties) {
        properties.entrySet().forEach(e -> debug(tc, "### SingletonMonitor property " + e.getKey() + " = " + (e.getValue() instanceof String[] ? Arrays.toString((Object[]) e.getValue()) : e.getValue())));
        this.configurationAdmin = configurationAdmin;
        String[] pids = (String[]) properties.get("singletonDeclarations");
        Objects.requireNonNull(pids);
        declaredSingletons = unmodifiableSet(Stream.of(pids).map(this::servicePidToSingletonType).collect(toSet()));
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, String.format("Known configured messaging singleton types: %s%nagent pids: %s", declaredSingletons, Arrays.toString(pids)));
    }

    @Deactivate
    public void deactivate() {
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, this + " deactivate");
    }

    private String servicePidToSingletonType(String servicePid) {
        try {
            Configuration[] configs = this.configurationAdmin.listConfigurations("(" + SERVICE_PID + "=" + servicePid + ")");
            if (configs == null) {
                Tr.debug(tc, "No configs found matching servicePid: " + servicePid);
                return "PID not found: " + servicePid;
            }
            if (configs.length > 1) throw new IllegalStateException("Non unique servicePid=" + servicePid + " matched configs=" + Arrays.toString(configs));

            String type = (String) configs[0].getProperties().get("type");
            if (isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.debug(this, tc, servicePid + " -> " + type);
            return type;
        } catch (IOException | InvalidSyntaxException | IllegalStateException e) {
            FFDCFilter.processException(e, "com.ibm.ws.messaging.lifecycle.SingletonMonitor.servicePidToConfigId", "98");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return "Error listing singletons:" + sw;
        }
    }

    @Reference(cardinality = MULTIPLE, policy = DYNAMIC, policyOption = GREEDY, target = "(id=unbound)" /* config will supply the correct target filter */)
    synchronized void addSingleton(SingletonAgent agent) {
	realizedSingletons.add(agent.getSingletonType());
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, String.format("Singleton added: %s%nConfigured: %s%n:  Realized: %s", agent, declaredSingletons, realizedSingletons));
    }

    synchronized void removeSingleton(SingletonAgent agent) {
        realizedSingletons.remove(agent.getSingletonType());
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(this, tc, String.format("Singleton removed: %s%nConfigured: %s%n  Realized: %s", agent, declaredSingletons, realizedSingletons));
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
        return String.format("List the declared (D) and the realized (R) messaging singletons.%n" +
                             "Messaging cannot start until all the declared singletons become available (i.e. are realized).");
    }

    @Override
    public void introspect(PrintWriter out) throws Exception {
        Stream.concat(declaredSingletons.stream(), realizedSingletons.stream())
            .sorted()
            .distinct()
            .sequential()
            .peek(s -> out.print('['))
            .peek(s -> out.print(declaredSingletons.contains(s) ? 'D' : ' '))
            .peek(s -> out.print(realizedSingletons.contains(s) ? 'R' : ' '))
            .peek(s -> out.print("] "))
            .forEach(out::println);
    }
    
    public void singletons() throws Exception {
        System.out.println(getIntrospectorName());
        System.out.println(getIntrospectorDescription());
        try (PrintWriter pw = new PrintWriter(System.out)) {
            introspect(pw);
        }
    }
}
