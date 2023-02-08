/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Component services that declare a mandatory, static reference to {@link SingletonsReady}
 * will not be activated until all the messaging {@link Singleton}s are ready.
 *
 * Subordinate code invoked, directly or indirectly, by those services may then rely on
 * the {@link Singleton}s being retrievable via {@link #requireService(Class)}.
 * {@link ApplicationPrereq}
 */
@Component(
        service = SingletonsReady.class,
        // ensure the singletons can't be satisfied
        // until config supplies a suitable target LDAP filter
        // and the actual cardinality.minimum value
        configurationPolicy = REQUIRE,
     	configurationPid = "com.ibm.ws.messaging.lifecycle.singletons",
        property = {
                "service.vendor=IBM",
                "singletons.cardinality.minimum=100000",
                "singletons.target=(id=unbound)",
                "allSingletons.cardinality.minimum=100000"})
public final class SingletonsReady {
    private static final TraceComponent tc = Tr.register(SingletonsReady.class);

    private enum AMBIGUOUS_ENTRY implements Singleton {INSTANCE};

    private static final AtomicReference<SingletonsReady> CURRENT = new AtomicReference<>();

    private final Map<Class<?>, Singleton> singletonMap;

    @Activate
    public SingletonsReady(
            // This @Reference is filtered and will only match configured singletons
            // (e.g. in server.xml or a defaultInstances.xml snippet)
            @Reference(name = "singletons") List<SingletonAgent> singletonAgents,
            // This @Reference is unfiltered and will receive every
            // available singleton whether it matches anything or not
            @Reference(name = "allSingletons") List<Singleton> allSingletons, 
            Map<String, Object> properties) {

        if (isAnyTracingEnabled() && tc.isEntryEnabled()) entry(this, tc, "<init>", singletonAgents, allSingletons);
        properties.entrySet().forEach(e -> debug(tc, "### SingletonsReady property " + e.getKey() + " = " + (e.getValue() instanceof String[] ? Arrays.toString((Object[]) e.getValue()) : e.getValue())));

        List<Singleton> singletons = singletonAgents.stream().map(SingletonAgent::getSingleton).collect(toList()); 
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) debug(this, tc, "singletons, allSingletons", singletons, allSingletons);

        {
            final List<String> unexpected = allSingletons
                // start with all known singletons
                .stream()
                // remove the expected ones
                .filter(not(singletons::contains))
                // convert those to strings
                .map(Object::toString)
                // trace them
                .peek(s -> debug(this, tc, "Unexpected singleton: ", s))
                // collect them into a list
                .collect(Collectors.toList());

            if (unexpected.size() > 0)
                throw new LifecycleError(
                    "Found unexpected Singleton services that have been "
                    + "declared but not configured correctly: " + unexpected);
        }

        // create a map from known Singleton types to singleton instances

        final Map<Class<?>, Singleton> map = new LinkedHashMap<>();
        // populate the map with each known singleton as values, and types as keys
        for (Singleton s: singletons) {
            for (Class<?> cls: singletonClassesOf(s)) {
                Singleton oldValue = map.putIfAbsent(cls, s);
                if (null == oldValue) continue; // put succeeded, so carry on
                // If we get here, there was a clash.
                // Make sure requests for this type fail
                map.replace(cls, oldValue, AMBIGUOUS_ENTRY.INSTANCE);
            }
        }
        singletonMap = unmodifiableMap(map);


        // Check every singleton is accessible by a unique class name
        Set<Singleton> distinct = new HashSet<Singleton>(map.values());
        Set<Singleton> ambiguous = new HashSet<>(singletons);
        ambiguous.removeAll(distinct);
        if (ambiguous.size() > 0)
            throw new LifecycleError(String.format("Some singletons share implementation classes: %n\tdistinct  = %s%n\tambiguous = %s", distinct, ambiguous));

        // Now make this instance available via the static accessors (with apologies to purists)
        boolean applied = CURRENT.compareAndSet(null, this);
        if (!applied) throw new LifecycleError("Could not set " + SingletonsReady.class.getSimpleName() + " instance as current");

        if (isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Set<String> impls = distinct.stream()
                    .map(Object::getClass)
                    .map(Class::getSimpleName)
                    .collect(TreeSet::new, Set::add, Set::addAll);
            Set<String> intfs = map.keySet().stream()
                    .map(Class::getSimpleName)
                    .filter(not(impls::contains))
                    .collect(TreeSet::new, Set::add, Set::addAll);
            debug(this, tc, "Messaging singletons are now available: ", impls);
            debug(this, tc, "Messaging singletons also available as: ", intfs);
        }
        if (isAnyTracingEnabled() && tc.isEntryEnabled()) exit(this, tc, "<init>");
    }

    @Deactivate
    private void destroy() {
        boolean result = CURRENT.compareAndSet(this, null);
        debug(this, tc, "Messaging singletons are no longer available: ", singletonMap.keySet(), result ? "unset succeeded" : "unset failed");
    }

    /**
     * Retrieve a service that should be present.
     * @param <T> the service class
     * @param type the service class
     * @return the singleton instance of the specified class
     * @throws LifecycleError if zero or multiple instances are available
     */
    public static <T extends Singleton> T requireService(Class<T> type) {
        SingletonsReady current = CURRENT.get();
        if (current == null)
            throw new LifecycleError("Singletons are not yet ready. Examine the call stack for a service component where a dependency on " + SingletonsReady.class.getSimpleName() + " can be declared to resolve this error.");
        Object value = current.singletonMap.get(type);
        if (value == null)
            throw new LifecycleError("No singleton found of type: " + type);
        if (value == AMBIGUOUS_ENTRY.INSTANCE)
            throw new LifecycleError("More than one singleton found of type: " + type);
        return type.cast(value);
    }

    /**
     * Retrieve a service that should be present.
     * If no instance is available, the error is captured using FFDC and
     * {@link Optional#empty()} is returned.
     * @param <T> the service class
     * @param type the service class
     * @return an Optional containing the singleton instance of the specified class, if available.
     */
    public static <T extends Singleton> Optional<T> findService(Class<T> type) {
        try {
            return Optional.of(requireService(type));
        } catch (LifecycleError e) {
            FFDCFilter.processException(e, SingletonsReady.class.getName(), "findService-LifecycleError", new Object[] {type.getCanonicalName(), CURRENT.get()});
            return Optional.empty();
        }
    }

    private static Set<Class<?>> singletonClassesOf(Singleton singleton) {
        // find all the parent classes
        return allClassesOf(singleton.getClass())
                // consider only the ones that inherit from Singleton
                .filter(Singleton.class::isAssignableFrom)
                // but not Singleton itself
                .filter(not(Singleton.class::equals))
                .collect(Collectors.toSet());
    }

    private static Stream<Class<?>> allClassesOf(Class<?> c) {
        if (c == null)
            return Stream.empty();
        Stream<Class<?>> interfaces = Stream.of(c.getInterfaces()).flatMap(SingletonsReady::allClassesOf);
        Stream<Class<?>> supers = allClassesOf(c.getSuperclass());
        Stream<Class<?>> ancestors = Stream.concat(interfaces, supers);
        return Stream.concat(Stream.of(c), ancestors);
    }

    private static <T> Predicate<T> not(Predicate<T> negand) { return negand.negate(); }
}
