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
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.logging.Introspector;

/**
 * This component tracks {@link Singleton} services as they arrive and depart.
 * It also dumps the current state of expected and realised {@link Singleton}s when a server dump is taken.
 * 
 * @see SingletonsReady
 */
@Component(immediate = true, configurationPolicy = REQUIRE)
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
        declaredSingletons = unmodifiableSet(Stream.of(pids).collect(toSet()));
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, String.format("%s%nKnown configured messaging singleton pids: %s%nids: ", this, Arrays.toString(pids), declaredSingletons));
    }

    @Deactivate
    public void deactivate() {
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, this + " deactivate");
    }

    @Trivial
    private String servicePidToConfigId(String servicePid) {
        Configuration[] configs;
        try {
            configs = this.configurationAdmin.listConfigurations("(" + SERVICE_PID + "=" + servicePid + ")");

            if (configs == null) {
                // This is not strictly an error given that we may be processing a pid from a deleted application
                Tr.debug(tc, "No configs found matching servicePid=" + servicePid);
                return "PID not found: " + servicePid;
            }
            if (configs.length > 1)
                throw new IllegalStateException("Non unique servicePid=" + servicePid + " matched configs=" + Arrays.toString(configs));

        } catch (IOException | InvalidSyntaxException | IllegalStateException e) {
            FFDCFilter.processException(e, "com.ibm.ws.messaging.lifecycle.SingletonMonitor.servicePidToConfigId", "98");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            return "Error listing singletons:" + sw;
        }

        String id = (String) configs[0].getProperties().get("id");
        return id;
    }

    @Reference(cardinality = MULTIPLE, policy = DYNAMIC, policyOption = GREEDY, target = "(id=unbound)" /* config will supply the correct target filter */)
    synchronized void addSingleton(Singleton singleton, Map<String, Object> props) {
	realizedSingletons.add(getServicePid(props));
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, String.format(" Singleton added: %s%nConfigured: %s%n:  Realized: %s", singleton.getClass(), declaredSingletons, realizedSingletons));
    }

    synchronized void removeSingleton(Singleton singleton, Map<String, Object> props) {
        realizedSingletons.remove(getServicePid(props));
        if (isAnyTracingEnabled() && tc.isDebugEnabled()) debug(tc, String.format("%s singleton removed: %s%nConfigured: %s%n  Realized: %s", this, singleton.getClass(), declaredSingletons, realizedSingletons));
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
        return String.format("List the declared and the realized messaging singletons.%n" +
                             "Messaging cannot start until all the declared singletons become available (i.e. are realized).");
    }

    @Override
    public void introspect(PrintWriter out) throws Exception {
        out.printf("Declared singletons: %s%nRealized singletons: %s", declaredSingletons, realizedSingletons);
    }

    private static String getServicePid(Map<String, Object> props) {
        return (String)props.getOrDefault(SERVICE_PID, "no service pid found in props: " + props);
    }
}
