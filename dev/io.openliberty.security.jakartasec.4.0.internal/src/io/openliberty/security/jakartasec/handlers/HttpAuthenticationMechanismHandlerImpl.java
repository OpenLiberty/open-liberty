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
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.util.WebConfigUtils;

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
     * Checks if global authentication override is active.
     * Returns true if overrideHttpAuthMethod is set in server.xml.
     */
    protected boolean isGlobalAuthOverrideActive() {
        try {
            WebAppSecurityConfig webAppSecConfig = WebConfigUtils.getWebAppSecurityConfig();
            if (webAppSecConfig == null) {
                return false;
            }
            String overrideMethod = webAppSecConfig.getOverrideHttpAuthMethod();
            if (overrideMethod != null && !overrideMethod.isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Global auth override detected: " + overrideMethod);
                }
                return true;
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception checking global auth override: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Determines if application-defined HttpAuthenticationMechanisms should be filtered out
     * in favor of Liberty's traditional authentication mechanisms.
     *
     * <p>Returns true when global authentication override is active with BasicAuth failover,
     * which means Liberty's traditional BasicAuth should be used instead of app-defined HAMs.
     *
     * <p>Note: Form failover is handled differently via cross-module HAM logic in
     * {@link #scanAuthenticationMechanisms(Set)} rather than complete filtering.
     *
     * <p>Returns false for APP_DEFINED failover (uses app-defined HAMs).
     *
     * @return true if app-defined HAMs should be filtered out (BasicAuth failover only), false otherwise
     */
    protected boolean shouldFilterAppDefinedHAMs() {
        if (!isGlobalAuthOverrideActive()) {
            return false;
        }

        try {
            WebAppSecurityConfig webAppSecConfig = WebConfigUtils.getWebAppSecurityConfig();
            if (webAppSecConfig == null) {
                return false;
            }

            // Filter for BasicAuth failover (uses Liberty's traditional BasicAuth)
            if (webAppSecConfig.getAllowFailOverToBasicAuth()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Filtering app-defined HAMs: allowFailOverToBasicAuth=true");
                }
                return true;
            }

        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception checking failover config: " + e.getMessage());
            }
        }

        return false;
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
     * Processes a single HttpAuthenticationMechanism and determines if it should be added to the collection.
     *
     * Handles filtering logic for:
     *
     * BasicAuth failover - filters all app-defined HAMs
     * Form failover - filters CustomFormAuthenticationMechanism, allows FormAuthenticationMechanism cross-module
     * Module-scoping - only includes HAMs from current module (unless global Form failover)
     *
     *
     * @param httpAuthenticationMechanismInstance the HAM instance to process
     * @param multiHttpAuthenticationMechanisms   the collection to add the HAM to if it passes filtering
     * @param applicationName                     the current application name
     * @param moduleName                          the current module name
     * @param filterAppHAMs                       true if app-defined HAMs should be filtered (BasicAuth failover)
     * @param isGlobalAuthOverride                true if global auth override is active
     * @param allowFailOverToFormLogin            true if Form failover is configured
     * @param allowFailOverToBasicAuth            true if BasicAuth failover is configured
     * @param allowFailOverToAppDefined           true if APP_DEFINED failover is configured
     * @param overrideHttpAuthMethod              the override HTTP auth method (for debug output)
     */
    private void processHttpAuthenticationMechanism(HttpAuthenticationMechanism httpAuthenticationMechanismInstance,
                                                    Set<MultiHttpAuthenticationMechanism> multiHttpAuthenticationMechanisms,
                                                    String applicationName,
                                                    String moduleName,
                                                    boolean filterAppHAMs,
                                                    boolean isGlobalAuthOverride,
                                                    boolean allowFailOverToFormLogin,
                                                    boolean allowFailOverToBasicAuth,
                                                    boolean allowFailOverToAppDefined,
                                                    String overrideHttpAuthMethod) {
        Class<?> hamClass = httpAuthenticationMechanismInstance.getClass();
        String hamClassName = hamClass.getSimpleName();
        String hamModuleName = HAMModuleRegistry.getModuleName(applicationName, hamClassName, moduleName);

        // Handle empty string module name: treat as null for FormHAM (backward compat),
        // but keep as-is for CustomFormHAM (module-specific, should be filtered)
        boolean isFormHAM = hamClassName.startsWith("FormAuthenticationMechanism");
        boolean isCustomFormHAM = hamClassName.startsWith("CustomFormAuthenticationMechanism");

        if (hamModuleName != null && hamModuleName.isEmpty() && isFormHAM && !isCustomFormHAM) {
            // FormAuthenticationMechanism with empty module name should be treated as unregistered (null)
            // This allows it to be available globally for backward compatibility
            hamModuleName = null;
        }
        // CustomFormAuthenticationMechanism with empty module name stays as-is (empty string)
        // This ensures it gets filtered out when ModMatch=false (module-specific behavior)

        // Debug output for complex server.xml config scenarios
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Found HAM: " + hamClassName +
                         ", registered module: " + hamModuleName +
                         ", current module: " + moduleName +
                         ", filterAppHAMs: " + filterAppHAMs);

            // Single line output for ease of debug analysis
            boolean moduleMatch = (hamModuleName != null && hamModuleName.equals(moduleName));
            boolean inRegistry = (hamModuleName != null);
            boolean globalFormFailover = (isGlobalAuthOverride && allowFailOverToFormLogin && !allowFailOverToAppDefined);

            Tr.debug(tc, "FORM_FAILOVER_DATA: App=" + applicationName +
                         " | CurMod=" + moduleName +
                         " | HAM=" + hamClassName +
                         " | RegMod=" + hamModuleName +
                         " | ModMatch=" + moduleMatch +
                         " | InReg=" + inRegistry +
                         " | IsForm=" + isFormHAM +
                         " | IsCustomForm=" + isCustomFormHAM +
                         " | GlobalAuthOvr=" + isGlobalAuthOverride +
                         " | OvrMethod=" + overrideHttpAuthMethod +
                         " | AllowFormFO=" + allowFailOverToFormLogin +
                         " | AllowBasicFO=" + allowFailOverToBasicAuth +
                         " | AllowAppDefFO=" + allowFailOverToAppDefined +
                         " | FilterAppHAMs=" + filterAppHAMs +
                         " | GlobalFormFO=" + globalFormFailover);
        }

        // Global override filtering: when BasicAuth failover is active, only use system-generated HAMs
        if (filterAppHAMs) {
            if (hamModuleName == null) {
                // System-generated HAM (not in registry)
                multiHttpAuthenticationMechanisms.add(new MultiHttpAuthenticationMechanism(httpAuthenticationMechanismInstance));
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added system-generated HAM: " + hamClassName);
                }
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Filtered out app-defined HAM due to BasicAuth failover: " + hamClassName);
                }
            }
        } else {
            // Check for global Form failover scenario
            boolean globalFormFailover = isGlobalAuthOverride && allowFailOverToFormLogin && !allowFailOverToAppDefined;

            // Global Form failover: filter CustomFormAuthenticationMechanism, allow FormAuthenticationMechanism cross-module
            // FormHAM will have been updated with global login metadata (like globalLogin.jsp or similar)
            if (globalFormFailover && isCustomFormHAM) {
                // Filter out CustomFormAuthenticationMechanism when global Form login is configured
                // This is the same as EE9/EE10 behaviour where CustomFormAuthenticationMechanism is vetoed
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Filtered out CustomFormAuthenticationMechanism due to global Form failover: " + hamClassName);
                }
            } else if (globalFormFailover && isFormHAM) {
                // Add FormAuthenticationMechanism to cross module boundaries for global Form login
                multiHttpAuthenticationMechanisms.add(new MultiHttpAuthenticationMechanism(httpAuthenticationMechanismInstance));
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added FormAuthenticationMechanism for global Form failover (cross-module): " + hamClassName);
                }
            }
            // Standard module-scoping logic
            else if (hamModuleName != null && hamModuleName.equals(moduleName)) {
                multiHttpAuthenticationMechanisms.add(new MultiHttpAuthenticationMechanism(httpAuthenticationMechanismInstance));
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added HAM to current module: " + hamClassName);
                }
            } else if (hamModuleName == null) {
                // HAM not in registry (backward compatibility or system-generated)
                multiHttpAuthenticationMechanisms.add(new MultiHttpAuthenticationMechanism(httpAuthenticationMechanismInstance));
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added unregistered HAM (backward compatibility): " + hamClassName);
                }
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Filtered out HAM from different module: " + hamClassName);
                }
            }
        }
    }

    /**
     * Scans for and collects HttpAuthenticationMechanism instances from the current module.
     *
     * This method handles:
     *
     * Global authentication override with BasicAuth failover - filters out app-defined HAMs</li>
     * Global authentication override with Form failover - filters CustomFormAuthenticationMechanism,
     * allows FormAuthenticationMechanism to cross module boundaries</li>
     * Module-scoping - only includes HAMs registered to the current module</li>
     * Backward compatibility - includes unregistered HAMs</li>
     *
     *
     * The method scans two sources:
     *
     * CDI.select(HttpAuthenticationMechanism.class) - all HAMs visible to CDI</li>
     * CDIHelper.getBeansFromCurrentModule() - HAMs from the module's BeanManager</li>
     *
     * @param multiHttpAuthenticationMechanisms the set to populate with discovered HAMs
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void scanAuthenticationMechanisms(Set<MultiHttpAuthenticationMechanism> multiHttpAuthenticationMechanisms) {

        Instance<HttpAuthenticationMechanism> httpAuthenticationMechanismInstances = null;
        CDI cdi = getCDI();

        if (cdi != null) {
            httpAuthenticationMechanismInstances = cdi.select(HttpAuthenticationMechanism.class);
        }

        String moduleName = getModuleName();
        String applicationName = getApplicationName();

        boolean filterAppHAMs = shouldFilterAppDefinedHAMs();

        // we need metadata for failover analysis and HAM filtering
        //   this mimics what is done in pre JS 4.0 extension processing, but couldn't
        //   be done in JS 4.0 extension processing - complexities with multiple hams and qualifiers
        boolean isGlobalAuthOverride = isGlobalAuthOverrideActive();
        boolean allowFailOverToFormLogin = false;
        boolean allowFailOverToBasicAuth = false;
        boolean allowFailOverToAppDefined = false;
        String overrideHttpAuthMethod = null;

        try {
            WebAppSecurityConfig webAppSecConfig = WebConfigUtils.getWebAppSecurityConfig();
            if (webAppSecConfig != null) {
                allowFailOverToFormLogin = webAppSecConfig.getAllowFailOverToFormLogin();
                allowFailOverToBasicAuth = webAppSecConfig.getAllowFailOverToBasicAuth();
                allowFailOverToAppDefined = webAppSecConfig.getAllowFailOverToAppDefined();
                overrideHttpAuthMethod = webAppSecConfig.getOverrideHttpAuthMethod();
                
                // Implicitly enable failover flags when overrideHttpAuthMethod is set
                // This maintains backward compatibility with EE10 behavior where overrideHttpAuthMethod
                // alone was sufficient to enable global failover
                if ("FORM".equalsIgnoreCase(overrideHttpAuthMethod) && !allowFailOverToFormLogin) {
                    allowFailOverToFormLogin = true;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Implicitly enabled allowFailOverToFormLogin due to overrideHttpAuthMethod=FORM");
                    }
                } else if ("BASIC".equalsIgnoreCase(overrideHttpAuthMethod) && !allowFailOverToBasicAuth) {
                    allowFailOverToBasicAuth = true;
                    // Recalculate filterAppHAMs since we just enabled BasicAuth failover
                    filterAppHAMs = isGlobalAuthOverride && allowFailOverToBasicAuth && !allowFailOverToAppDefined;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Implicitly enabled allowFailOverToBasicAuth due to overrideHttpAuthMethod=BASIC, filterAppHAMs=" + filterAppHAMs);
                    }
                }
            }
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception fetching WebAppSecurityConfig: " + e.getMessage());
            }
        }

        // scan HAMs from CDI.select() - includes all HAMs visible to CDI
        if (httpAuthenticationMechanismInstances != null) {
            for (HttpAuthenticationMechanism httpAuthenticationMechanismInstance : httpAuthenticationMechanismInstances) {
                processHttpAuthenticationMechanism(httpAuthenticationMechanismInstance, multiHttpAuthenticationMechanisms,
                                                   applicationName, moduleName, filterAppHAMs,
                                                   isGlobalAuthOverride, allowFailOverToFormLogin,
                                                   allowFailOverToBasicAuth, allowFailOverToAppDefined,
                                                   overrideHttpAuthMethod);
            }
        }

        // scan HAMs from module's BeanManager - already filtered to current module by CDIHelper
        if (cdi != null && cdi.getBeanManager() != null && !cdi.getBeanManager().equals(CDIHelper.getBeanManager())) {
            for (HttpAuthenticationMechanism mechanism : CDIHelper.getBeansFromCurrentModule(HttpAuthenticationMechanism.class)) {
                processHttpAuthenticationMechanism(mechanism, multiHttpAuthenticationMechanisms,
                                                   applicationName, moduleName, filterAppHAMs,
                                                   isGlobalAuthOverride, allowFailOverToFormLogin,
                                                   allowFailOverToBasicAuth, allowFailOverToAppDefined,
                                                   overrideHttpAuthMethod);
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
