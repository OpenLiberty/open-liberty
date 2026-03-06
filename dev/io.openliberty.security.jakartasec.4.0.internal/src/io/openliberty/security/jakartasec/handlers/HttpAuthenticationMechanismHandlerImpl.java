/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

package io.openliberty.security.jakartasec.handlers;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.javaeesec.CDIHelper;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesUtils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.AmbiguousResolutionException;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanismHandler;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Implementation of the HttpAuthenticationMechanismHandler interface.
 * This class selects the highest priority HttpAuthenticationMechanism
 * and delegates authentication operations to it.
 */
@Default
@ApplicationScoped
public class HttpAuthenticationMechanismHandlerImpl implements HttpAuthenticationMechanismHandler {

    private static final TraceComponent tc = Tr.register(HttpAuthenticationMechanismHandlerImpl.class);

    // Priority constants for built-in HAMs (lower values = lower priority)
    private static final int PRIORITY_BASIC = Integer.MIN_VALUE + 1;
    private static final int PRIORITY_FORM = Integer.MIN_VALUE + 2;
    private static final int PRIORITY_CUSTOM_FORM = Integer.MIN_VALUE + 3;
    private static final int PRIORITY_OIDC = Integer.MIN_VALUE + 4;

    protected static Map<String, Integer> hamClassPriorities;
    {
        Map<String, Integer> temp = new HashMap<String, Integer>();
        temp.put("BasicHttpAuthenticationMechanism", PRIORITY_BASIC);
        temp.put("FormAuthenticationMechanism", PRIORITY_FORM);
        temp.put("CustomFormAuthenticationMechanism", PRIORITY_CUSTOM_FORM);
        temp.put("OidcHttpAuthenticationMechanism", PRIORITY_OIDC);
        hamClassPriorities = Collections.unmodifiableMap(temp);
    }

    /**
     * Map of module names to sets of HttpAuthenticationMechanism instances sorted by priority.
     */
    private final ConcurrentHashMap<String, Set<MultiHttpAuthenticationMechanism>> authMechanismMap = new ConcurrentHashMap<>();

    // cache the last, highest priority HAM name to avoid duplicate logging
    private static volatile String lastLoggedHAMName = null;

    /**
     * Default constructor
     */
    public HttpAuthenticationMechanismHandlerImpl() {
        // empty
    }

    protected ModulePropertiesUtils getModulePropertiesUtils() {
        return ModulePropertiesUtils.getInstance();
    }

    /**
     * Logs the HAM name **to the debug output** if it's different from the last logged one.
     *
     * This handles application reloads where the HAM may change.
     *
     * @param httpAuthenticationMechanism is the HAM to log, cannot be null.
     */
    private static void logHAMToDebugIfChanged(MultiHttpAuthenticationMechanism httpAuthenticationMechanism) {
        if (tc.isDebugEnabled()) {
            String hamName = httpAuthenticationMechanism.getSimpleName();
            // only output if HAM name has changed
            if (!hamName.equals(lastLoggedHAMName)) {
                Tr.debug(tc, "The (highest priority) HttpAuthenticationMechanism being used is: " + hamName);
                lastLoggedHAMName = hamName;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {

        MultiHttpAuthenticationMechanism multiHttpAuthenticationMechanism = getHighestPriorityHttpAuthenticationMechanism();
        if (multiHttpAuthenticationMechanism == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Selecting the highest priority HttpAuthenticationMechanism but none returned.");
            }
            return AuthenticationStatus.NOT_DONE;
        }
        logHAMToDebugIfChanged(multiHttpAuthenticationMechanism);

        return multiHttpAuthenticationMechanism.validateRequest(request, response, httpMessageContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AuthenticationStatus secureResponse(HttpServletRequest request,
                                               HttpServletResponse response,
                                               HttpMessageContext httpMessageContext) throws AuthenticationException {

        MultiHttpAuthenticationMechanism multiHttpAuthenticationMechanism = getHighestPriorityHttpAuthenticationMechanism();
        if (multiHttpAuthenticationMechanism == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Selecting the highest priority HttpAuthenticationMechanism but none returned.");
            }
            return AuthenticationStatus.NOT_DONE;
        }
        logHAMToDebugIfChanged(multiHttpAuthenticationMechanism);

        return multiHttpAuthenticationMechanism.secureResponse(request, response, httpMessageContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanSubject(HttpServletRequest request, HttpServletResponse response, HttpMessageContext httpMessageContext) {

        MultiHttpAuthenticationMechanism multiHttpAuthenticationMechanism = getHighestPriorityHttpAuthenticationMechanism();

        if (multiHttpAuthenticationMechanism == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Selecting the highest priority HttpAuthenticationMechanism but none returned.");
            }
            return;
        }
        logHAMToDebugIfChanged(multiHttpAuthenticationMechanism);

        multiHttpAuthenticationMechanism.cleanSubject(request, response, httpMessageContext);
    }

    /**
     * Gets the highest priority HttpAuthenticationMechanism.
     *
     * @return The highest priority HttpAuthenticationMechanism, or
     *         null if none are available
     */
    protected MultiHttpAuthenticationMechanism getHighestPriorityHttpAuthenticationMechanism() {
        String moduleName = getModuleName();

        Set<MultiHttpAuthenticationMechanism> multiHttpAuthenticationMechanisms = authMechanismMap.get(moduleName);

        if (multiHttpAuthenticationMechanisms == null) {
            // first time fetching the auth mechanisms
            multiHttpAuthenticationMechanisms = new TreeSet<>(priorityComparator);
            scanAuthenticationMechanisms(multiHttpAuthenticationMechanisms);
            authMechanismMap.put(moduleName, multiHttpAuthenticationMechanisms);
            // output hams every time the authMechanismMap is rebuilt
            logHttpAuthenticationMechanisms(multiHttpAuthenticationMechanisms);
        } else if (multiHttpAuthenticationMechanisms.size() > 1) {
            // Re-sort since priority can change due to deferred EL expressions
            Set<MultiHttpAuthenticationMechanism> oldMultiHttpAuthenticationMechanisms = multiHttpAuthenticationMechanisms;
            multiHttpAuthenticationMechanisms = new TreeSet<>(priorityComparator);
            multiHttpAuthenticationMechanisms.addAll(oldMultiHttpAuthenticationMechanisms);
        }

        if (multiHttpAuthenticationMechanisms.isEmpty()) {
            Tr.error(tc, "JAKARTASEC_ERROR_NO_HAM", getModuleName(), getApplicationName());
            return null;
        }

        checkUniquePriorityFound(multiHttpAuthenticationMechanisms);

        // Return the highest priority mechanism (first in the sorted set)
        return multiHttpAuthenticationMechanisms.iterator().next();
    }

    /**
     * Ensure a unique HAM is found and throw an exception if one cannot be found.
     *
     * All in-built HAMs will have a unique, ordered priority, so this only applies
     * if there are multiple application HAMs with the same highest priority,
     * or if there are duplicate in-built HAMs of the same type.
     *
     * @param multiHttpAuthenticationMechanisms is a list of current HAMs.
     */

    private void checkUniquePriorityFound(Set<MultiHttpAuthenticationMechanism> multiHttpAuthenticationMechanisms) {
        // if we have fewer than 2 mechanisms, there's no ambiguity
        if (multiHttpAuthenticationMechanisms.size() < 2) {
            return;
        }

        // get the first two mechanisms from the set ...
        Iterator<MultiHttpAuthenticationMechanism> iterator = multiHttpAuthenticationMechanisms.iterator();
        MultiHttpAuthenticationMechanism first = iterator.next();
        MultiHttpAuthenticationMechanism second = iterator.next();

        // ... and compare their priorities
        if (first.getPriority() == second.getPriority()) {
            // get the top priority value as this is the ambiguous case
            int topPriority = first.getPriority();

            // create a comma-separated list of only the MultiHttpAuthenticationMechanism
            //   instances with the top priority
            StringBuilder mechanismList = new StringBuilder();
            boolean isFirst = true;

            // reset the iterator to include all mechanisms
            iterator = multiHttpAuthenticationMechanisms.iterator();
            while (iterator.hasNext()) {
                MultiHttpAuthenticationMechanism mechanism = iterator.next();

                // so build a list of HAMs which have the same priority as the
                //   top one, as it could be more than just the two
                if (mechanism.getPriority() == topPriority) {
                    if (!isFirst) {
                        mechanismList.append(", ");
                    }

                    // output name only if in hamClassPriorities map, otherwise include priority
                    String simpleName = mechanism.getSimpleName();
                    if (hamClassPriorities.containsKey(simpleName)) {
                        mechanismList.append(simpleName);
                    } else {
                        mechanismList.append(simpleName).append(" Priority = ").append(mechanism.getPriority());
                    }
                    isFirst = false;
                } else {
                    // can break as set is ordered by priority
                    break;
                }
            }

            Tr.error(tc, "JAKARTASEC_ERROR_AMBIGUOUS_RESOLUTION", mechanismList.toString());

            throw new AmbiguousResolutionException(Tr.formatMessage(tc, "JAKARTASEC_ERROR_AMBIGUOUS_RESOLUTION", mechanismList.toString()));
        }
    }

    /**
     * Output a message showing the ordering of the discovered hams.
     *
     * @param multiHttpAuthenticationMechanisms is a list of hams.
     */
    private void logHttpAuthenticationMechanisms(Set<MultiHttpAuthenticationMechanism> multiHttpAuthenticationMechanisms) {
        // only log the output if debug tracing turned on
        if (tc.isDebugEnabled() == false) {
            return;
        }
        StringBuilder msg = new StringBuilder("Order of HttpAuthenticationMechanisms found (the first one will be used if its prioritization is unique - @Priority for application HAMs and HAM type - Oidc/CustomForm/Form/Basic - for in-built HAMs): ");
        final String prioritySeparator = ", ";
        for (MultiHttpAuthenticationMechanism httpAuthenticationMechanism : multiHttpAuthenticationMechanisms) {
            String simpleName = httpAuthenticationMechanism.getSimpleName();
            msg = msg.append(simpleName);
            // don't output priority for in-built hams
            if (hamClassPriorities.containsKey(simpleName) == false) {
                msg = msg.append(" Priority = " + httpAuthenticationMechanism.getPriority());
            }
            msg = msg.append(prioritySeparator);
        }
        // remove trailing ", "
        msg.setLength(msg.length() - prioritySeparator.length());
        Tr.debug(tc, getApplicationName(), msg.toString());
    }

    /**
     * Scans for all available HttpAuthenticationMechanism implementations.
     *
     * @param multiHttpAuthenticationMechanisms The set to populate with found mechanisms
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void scanAuthenticationMechanisms(Set<MultiHttpAuthenticationMechanism> multiHttpAuthenticationMechanisms) {

        Instance<HttpAuthenticationMechanism> httpAuthenticationMechanismInstances = null;
        CDI cdi = getCDI();

        if (cdi != null) {
            httpAuthenticationMechanismInstances = cdi.select(HttpAuthenticationMechanism.class);
        }

        if (httpAuthenticationMechanismInstances != null) {
            for (HttpAuthenticationMechanism httpAuthenticationMechanismInstance : httpAuthenticationMechanismInstances) {
                MultiHttpAuthenticationMechanism multiHttpAuthenticationMechanism = new MultiHttpAuthenticationMechanism(httpAuthenticationMechanismInstance);
                multiHttpAuthenticationMechanisms.add(multiHttpAuthenticationMechanism);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found an HttpAuthenticationMechanism from CDI: "
                                 + multiHttpAuthenticationMechanism.getSimpleName());
                }
            }
        }

        // If the mechanism is from the extension, then check the app's bean manager too
        if (cdi != null && cdi.getBeanManager() != null && !cdi.getBeanManager().equals(CDIHelper.getBeanManager())) {
            for (HttpAuthenticationMechanism mechanism : CDIHelper.getBeansFromCurrentModule(HttpAuthenticationMechanism.class)) {
                MultiHttpAuthenticationMechanism multiHttpAuthenticationMechanism = new MultiHttpAuthenticationMechanism(mechanism);
                multiHttpAuthenticationMechanisms.add(multiHttpAuthenticationMechanism);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found an HttpAuthenticationMechanism from module BeanManager: "
                                 + multiHttpAuthenticationMechanism.getSimpleName());
                }
            }
        }
    }

    /**
     * Comparator for ordering HttpAuthenticationMechanism instances by priority.
     * Highest priority values come first.
     */
    private final Comparator<MultiHttpAuthenticationMechanism> priorityComparator = new Comparator<MultiHttpAuthenticationMechanism>() {
        @Override
        public int compare(MultiHttpAuthenticationMechanism o1, MultiHttpAuthenticationMechanism o2) {

            int result = -1;
            if (o1.equals(o2)) {
                result = 0;
            } else {
                int p1 = o1.getPriority();
                int p2 = o2.getPriority();
                if (p1 < p2) {
                    result = 1;
                }
            }
            return result;
        }
    };

    /**
     * Gets the current module name.
     *
     * @return The module name
     */
    protected String getModuleName() {
        return ModulePropertiesUtils.getInstance().getJ2EEModuleName();
    }

    /**
     * Gets the current application name.
     *
     * @return The module name
     */
    protected String getApplicationName() {
        return ModulePropertiesUtils.getInstance().getJ2EEApplicationName();
    }

    /**
     * Clears the authentication mechanism map.
     * Used primarily for testing.
     */
    protected void clearAuthMechanismMap() {
        authMechanismMap.clear();
    }

    /**
     * Gets the CDI instance.
     * This method can be overridden for testing.
     *
     * @return The CDI instance
     */
    protected CDI<?> getCDI() {
        return CDI.current();
    }
}
