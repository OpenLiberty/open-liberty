/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.web.impl;

import java.security.Permissions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authorization.jacc.common.PolicyConfigurationManager;
import com.ibm.ws.security.authorization.jacc.web.WebSecurityPropagator;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.metadata.WebResourceCollection;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

public class WebSecurityPropagatorImpl implements WebSecurityPropagator {
    private static final TraceComponent tc = Tr.register(WebSecurityPropagatorImpl.class);

    private static final int EXTENSION_PATTERN = 0;
    private static final int PATHPREFIX_PATTERN = 1;
    private static final int EXACT_PATTERN = 2;
    private static final int DEFAULT_PATTERN = 3;
    private static final String STARSTAR = "**";
    private static final ActionString ALLMETHOD = new ActionString(":NONE");

    public WebSecurityPropagatorImpl() {}

    /** {@inheritDoc} */
    @Override
    public void propagateWebConstraints(PolicyConfigurationFactory pcf,
                                        String contextId,
                                        Object webAppConfigObject) {
        WebAppConfig webAppConfig = null;
        if (webAppConfigObject != null) {
            try {
                webAppConfig = (WebAppConfig) webAppConfigObject;
            } catch (ClassCastException cce) {
                Tr.error(tc, "JACC_WEB_SPI_PARAMETER_ERROR", new Object[] { webAppConfigObject.getClass().getName(), "propagateWebConstraints", "WebAppConfig" });
                return;
            }
        } else {
            // if null, do nothing.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Nothing to propagate due to null webAppConfig object.");
            return;
        }

        // if there is the same contextId in the map, delete it. This is for preventing to link PolicyConfiguration incorrectly.
        String appName = webAppConfig.getApplicationName();
        PolicyConfigurationManager.removeModule(appName, contextId);

        PolicyConfiguration webPC = null;
        try {
            // for web, there is no scenario to update (add) the Permissions,
            // therefore, the existing permissions are deleted upon invoking getPolicyConfiguration.
            webPC = pcf.getPolicyConfiguration(contextId, true);
        } catch (PolicyContextException pce) {
            Tr.error(tc, "JACC_WEB_GET_POLICYCONFIGURATION_FAILURE", new Object[] { contextId, pce });
            return;
        }
        SecurityConstraintCollection scc = getSecurityMetadata(webAppConfig).getSecurityConstraintCollection();
        try {
            processRole(webPC, webAppConfig);
            if (scc != null) {
                List<SecurityConstraint> scList = scc.getSecurityConstraints();
                Map<String, URLMap> allURLMap = convertURLMap(scList);
                processUrlMap(webPC, allURLMap, isDenyUncoveredHttpMethods(scList));
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "ContextId: " + contextId + " has no security constraints.");
            }
            PolicyConfigurationManager.linkConfiguration(appName, webPC);
            PolicyConfigurationManager.addModule(appName, contextId);
            // commit will be invoked in PolicyCOnfigurationManager class.
        } catch (PolicyContextException e) {
            Tr.error(tc, "JACC_WEB_PERMISSION_PROPAGATION_FAILURE", new Object[] { contextId, e });
        }

        return;
    }

    private void processRole(PolicyConfiguration webPC, WebAppConfig webAppConfig) throws PolicyContextException {
        SecurityMetadata smd = getSecurityMetadata(webAppConfig);
        List<String> roles = smd.getRoles();
        // loop all servlets
        Iterator<?> servletNames = webAppConfig.getServletNames();
        while (servletNames.hasNext()) {
            String servletName = (String) servletNames.next();
            boolean starStarFound = false;

            // handle roleref
            Map<String, String> roleRefs = smd.getRoleRefs(servletName);
            if (roleRefs != null && !roleRefs.isEmpty()) {
                for (Entry<String, String> roleRef : roleRefs.entrySet()) {
                    String refName = roleRef.getKey();
                    String name = roleRef.getValue();
                    WebRoleRefPermission wrrp = new WebRoleRefPermission(servletName, refName);
                    if (!starStarFound && (STARSTAR.equals(refName))) {
                        starStarFound = true;
                    }
                    webPC.addToRole(name, wrrp);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "addToRole(roleRef) role : " + name + " permission : " + wrrp);
                }
            }
            // per 1.5 spec section 3.1.3.3, if there is no "**" role in the roleref, it needs to add it.
            if (!starStarFound) {
                WebRoleRefPermission wrrp = new WebRoleRefPermission(servletName, STARSTAR);
                webPC.addToRole(STARSTAR, wrrp);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "addToRole(roleRef) role : ** permission : " + wrrp);
            }
            // handle role
            for (String roleName : roles) {
                WebRoleRefPermission wrrp = new WebRoleRefPermission(servletName, roleName);
                webPC.addToRole(roleName, wrrp);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "addToRole(role) role : " + roleName + " permission : " + wrrp);
            }
        }

        for (String roleName : roles) {
            // As per the spec, create WebRoleRefPermissions for every role
            // with an empty servletName to handle JSP's/Servlets not listed
            WebRoleRefPermission wrrp = new WebRoleRefPermission("", roleName);
            webPC.addToRole(roleName, wrrp);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addToRole(every role) role : " + roleName + " permission : " + wrrp);
        }
        return;
    }

    private Map<String, URLMap> convertURLMap(List<SecurityConstraint> scList) {
        Map<String, URLMap> allURLMap = new HashMap<String, URLMap>();
        URLMap rootURL = new URLMap("/");
        rootURL.setUncheckedSet(null);
        allURLMap.put("/", rootURL);

        for (SecurityConstraint sc : scList) {
            List<WebResourceCollection> wrcList = sc.getWebResourceCollections();

            List<String> roles = sc.getRoles();

            String userDataConstraint = "NONE";
            if (sc.isSSLRequired()) {
                userDataConstraint = "CONFIDENTIAL";
            }
            boolean accessPrecluded = sc.isAccessPrecluded();

            for (WebResourceCollection wrc : wrcList) {
                List<String> urls = wrc.getUrlPatterns();
                List<String> methods = wrc.getHttpMethods();
                List<String> omissionMethods = wrc.getOmissionMethods();
                for (String url : urls) {
                    List<String> selected = null;
                    URLMap urlMap = allURLMap.get(url);
                    if (urlMap == null) {
                        urlMap = getNewURLMap(url, allURLMap);
                    }
                    boolean omission = false;
                    if (methods == null || methods.size() == 0) {
                        if (omissionMethods != null && omissionMethods.size() > 0) {
                            omission = true;
                            selected = omissionMethods;
                        } else {
                            // All methods implied
                            if ((tc.isDebugEnabled()))
                                Tr.debug(tc, "All Methods are set since HTTP Method isn't defined.");
                        }
                    } else {
                        selected = methods;
                    }
                    if (accessPrecluded) {
                        // Excluded methods
                        urlMap.setExcludedSet(selected, omission);
                    } else if (roles == null) {
                        urlMap.setUncheckedSet(selected, omission);
                    } else {
                        for (String role : roles) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Setting role map for role = " + role);
                            urlMap.setRoleMap(role, selected, omission);
                        }
                    }
                    urlMap.setUserDataMap(userDataConstraint, selected, omission);
                }
            }
        }
        return allURLMap;
    }

    private URLMap getNewURLMap(String url, Map<String, URLMap> urlMaps) {
        URLMap urlMap = new URLMap(url);
        for (Entry<String, URLMap> e : urlMaps.entrySet()) {
            String prevURL = e.getKey();
            URLMap prevURLMap = e.getValue();
            int prevURLType = urlType(prevURL);

            switch (urlType(url)) {

                case EXTENSION_PATTERN: // Extension (*.html) pattern (0)
                    // If the pattern is an extension pattern, it must be qualified by every path-prefix pattern (1) appearing in the deployment descriptor and every exact pattern (2) in the deployment descriptor that is matched by the pattern being qualified.

                    if (prevURLType == PATHPREFIX_PATTERN || prevURLType == EXACT_PATTERN && urlPatternMatch(url, prevURL)) {
                        urlMap.appendURLPattern(prevURL);
                        break;
                    }
                    // If the pattern is the default pattern, "/" (3), it must be qualified by every other pattern except the default pattern appearing in the deployment descriptor.
                    if (prevURLType == DEFAULT_PATTERN) {
                        prevURLMap.appendURLPattern(url);
                    }
                    break;

                case PATHPREFIX_PATTERN: // Path Prefix (/acme/widget/*) pattern (1)
                    // If the pattern is a path prefix pattern (1), it must be qualified by every path-prefix pattern in the deployment descriptor matched by and different from the pattern being qualified. The pattern must also be qualified by every exact (2) pattern appearing in the deployment descriptor that is matched by the pattern being qualified.

                    if ((prevURLType == PATHPREFIX_PATTERN || prevURLType == EXACT_PATTERN) && urlPatternMatch(url, prevURL)) {
                        urlMap.appendURLPattern(prevURL);
                        break;
                    }
                    if (prevURLType == PATHPREFIX_PATTERN && urlPatternMatch(prevURL, url)) {
                        prevURLMap.appendURLPattern(url);
                        break;
                    }
                    // See the commetnts for extension and default
                    if (prevURLType == EXTENSION_PATTERN || prevURLType == DEFAULT_PATTERN) {
                        prevURLMap.appendURLPattern(url);
                    }
                    break;

                case EXACT_PATTERN: // Exact (/acme/widget/hammer) pattern (2)
                    // If the pattern is an exact pattern, its qualified form must not contain any qualifying patterns.

                    // See the extension(0) and path prefix(1) pattern comments
                    if ((prevURLType == EXTENSION_PATTERN || prevURLType == PATHPREFIX_PATTERN) && urlPatternMatch(prevURL, url)) {
                        prevURLMap.appendURLPattern(url);
                        break;
                    }
                    // See the default(3) pattern comments.
                    if (prevURLType == DEFAULT_PATTERN) {
                        prevURLMap.appendURLPattern(url);
                    }
                    break;

                case DEFAULT_PATTERN: // Default (/) pattern (3)
                    // If the pattern is the default pattern, "/", it must be qualified by every other pattern except the default pattern appearing in the deployment descriptor.
                    if (prevURLType != DEFAULT_PATTERN) {
                        urlMap.appendURLPattern(prevURL);
                    }
                    break;

                default:
                    break;
            }

        }
        if (tc.isDebugEnabled())
            Tr.debug(tc, "full urlPattern created is: " + urlMap.getURLPattern());
        urlMaps.put(url, urlMap);
        return urlMap;
    }

    private void processUrlMap(PolicyConfiguration webPC, Map<String, URLMap> allURLMap, boolean isDenyUncoveredHttpMethodsSet) throws PolicyContextException {
        Permissions webUncheckedPerms = new Permissions();
        Permissions webExcludedPerms = new Permissions();
        boolean uncheckedAdded = false;
        boolean excludedAdded = false;

        for (Entry<String, URLMap> e : allURLMap.entrySet()) {
            String url = e.getKey();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "url is: " + url);
            URLMap newMap = e.getValue();

            String urlPatternName = newMap.getURLPattern();
            if (isUnqualified(url, urlPatternName)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "url: " + url + " is unqualified");
                continue;
            }

            if (tc.isDebugEnabled())
                Tr.debug(tc, "urlPatternName: " + urlPatternName);

            boolean excludedSet = false;
            ActionString excludedString = newMap.getExcludedString();
            boolean done = false;
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Method string for Excluded Permission: " + excludedString);
            if (excludedString != null) {
                String actionString = excludedString.getActions();
                webExcludedPerms.add(new WebResourcePermission(urlPatternName, actionString));
                webExcludedPerms.add(new WebUserDataPermission(urlPatternName, actionString));
                excludedSet = true;
                excludedAdded = true;
                if (actionString == null) {
                    // this means all methods.
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "all methods is set for excluded");
                    done = true;
                }
            }
            Map<String, String> rMap = null;
            if (!done) {
                rMap = newMap.getRoleMap(); //get role here.

                ActionString uString = newMap.getUncheckedString();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Method string for Unchecked Permission: " + uString);
                if (uString != null) {
                    // if output exists, or nothing is found in unchecked, excluded or role, then set uncheck for all methods.
                    // also if isDenyUncoveredHttpMethodsSet is true, the value needs to set as Excluded list per 1.5 spec on page 27.
                    WebResourcePermission wrp = new WebResourcePermission(urlPatternName, uString.getActions());
                    if (!isDenyUncoveredHttpMethodsSet) {
                        webUncheckedPerms.add(wrp);
                    } else {
                        webExcludedPerms.add(wrp);
                        excludedAdded = true;
                    }
                } else if ((!excludedSet) && (rMap == null)) {
                    // if there is no role or excluded list, set unchecked to all methods.
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "set unchecked for all methods");
                    webUncheckedPerms.add(new WebResourcePermission(urlPatternName, (String) null));
                    uncheckedAdded = true;
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "unchecked list is null");
                }
                ActionString userDataConfidential = null;
                // In this release, both CONFIDENTIAL and INTEGRAL are treated as CONFIDENTIAL.
                // Because both end up using SSL connection and there is a code to check the string
                // "CONFIDENTIAL" to determine whether SSL is required in some class.
                userDataConfidential = newMap.getUserDataString("CONFIDENTIAL_OR_INTEGRAL");
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "\nUserData - Confidential: " + userDataConfidential); //current
                if (userDataConfidential != null) {
                    String actions = userDataConfidential.getActions();
                    addUserData(webUncheckedPerms, urlPatternName, actions);
                    uncheckedAdded = true;
                    if (actions != null && actions.startsWith(":")) {
                        // all methods with CONFIDENTIAL
                        done = true;
                    }
                }
                if (!done) {
                    ActionString userDataRest = null;
                    if (userDataConfidential == null && (!excludedSet)) {
                        // if nothing has set yet, set all method.
                        userDataRest = ALLMETHOD; // all methods
                    } else {
                        userDataRest = newMap.getUserDataString("REST");
                        if (userDataRest == null && userDataConfidential == null) {
                            // no output, either all methods or none. if there is confidential, then it would be none.
                            userDataRest = ALLMETHOD; // all methods
                        }
                    }
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "UserData - Rest: " + userDataRest);
                    if (userDataRest != null) {
                        if (!isDenyUncoveredHttpMethodsSet || uString == null) {
                            addUserData(webUncheckedPerms, urlPatternName, userDataRest.getActions());
                            uncheckedAdded = true;
                        } else {
                            if (ALLMETHOD.equals(userDataRest)) {
                                String action = uString.getActions();
                                addUserData(webExcludedPerms, urlPatternName, action);
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "UserData - setExcluded: " + action);
                                excludedAdded = true;
                                action = uString.getReverseActions();
                                addUserData(webUncheckedPerms, urlPatternName, action);
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "UserData - setUnchecked: " + action);
                                uncheckedAdded = true;
                            } else {
                                addUserData(webExcludedPerms, urlPatternName, userDataRest.getActions());
                                excludedAdded = true;
                            }
                        }
                    }
                }
                if (rMap != null) {
                    // if role is available.
                    for (Entry<String, String> en : rMap.entrySet()) {
                        String role = en.getKey();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "role is " + role);
                        String methodString = en.getValue();
                        if (methodString == null || methodString.length() == 0) {
                            // All method
                            WebResourcePermission wrp = new WebResourcePermission(urlPatternName, (String) null);
                            webPC.addToRole(role, wrp);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "addToRole(all methods) role : " + role + " permission : " + wrp);
                        } else {
                            // selected methods
                            WebResourcePermission wrp = new WebResourcePermission(urlPatternName, methodString);
                            webPC.addToRole(role, wrp);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "addToRole(specific methods) role : " + role + " permission : " + wrp);
                        }
                    }
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "No role map. URL: " + urlPatternName);
                }
            }
        }
        //add the permissions to pc
        if (excludedAdded) {
            webPC.addToExcludedPolicy(webExcludedPerms);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addToExcludedPolicy permission : " + webExcludedPerms);
        }
        if (uncheckedAdded) {
            webPC.addToUncheckedPolicy(webUncheckedPerms);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "addToUncheckedPolicy permission : " + webUncheckedPerms);
        }
        return;
    }

    /*************************************************************************
     * Any pattern, qualified by a pattern that matches it, is overridden and
     * made irrelevant (in the translation) by the qualifying pattern.
     * Specifically, all extension patterns and the default pattern are made
     * irrelevant by the presence of the path prefix pattern "/*" in a
     * deployment descriptor.
     **************************************************************************/
    private boolean isUnqualified(String url, String urlPattern) {
        boolean unqualified = false;
        if (urlPattern.indexOf(":") != -1) {
            int idx = urlPattern.indexOf(":");
            StringTokenizer st = new StringTokenizer(urlPattern.substring(idx + 1), ":");
            while (st.hasMoreTokens()) {
                if (urlPatternMatch(st.nextToken(), url)) {
                    unqualified = true;
                    break;
                }
            }
        }
        return unqualified;
    }

    private void addUserData(Permissions permissions, String url, String userdata) {
        if (userdata != null && userdata.startsWith(":")) {
            String transport = userdata.substring(1);
            // all methods
            permissions.add(new WebUserDataPermission(url, null, transport));
        } else {
            permissions.add(new WebUserDataPermission(url, userdata));
        }
    }

    /**********************************************************************
     * pattern type example
     * ------------ -------
     * extension *.html
     * path prefix /acme/widget/*
     * exact /acme/widget/hammer
     * default /
     **********************************************************************/

    private int urlType(String urlPattern) {
        String pattern = urlPattern.toString();
        if (pattern.startsWith("*.")) { //extension
            return EXTENSION_PATTERN; //0
        } else if (pattern.startsWith("/") && pattern.endsWith("/*")) { // path prefix
            return PATHPREFIX_PATTERN; //1
        } else if (pattern.equals("/")) {
            return DEFAULT_PATTERN; //3
        }
        return EXACT_PATTERN; //2
    }

    /*************************************************************************
     * The pattern match rules are as follows.
     * 1) their pattern values are String equivalent, or
     * 2) this pattern is the path-prefix pattern "/*", or
     * 3) this pattern is a path-prefix pattern (that is, it starts with
     * "/" and ends with "/*") and the other pattern starts with the
     * substring of this pattern, minus its last 2 characters, and the
     * next character of the other pattern, if there is one, is "/", or
     * 4) this pattern is an extension pattern (that is, it starts with "*.")
     * and the other pattern ends with this pattern, or
     * 5) this pattern is the special default pattern, "/", which matches all
     * other patterns.
     **************************************************************************/
    protected boolean urlPatternMatch(String pattern1, String pattern2) {
        if (pattern1.equals(pattern2)) {
            return true;
        }
        if (pattern1.equals("/*")) {
            return true;
        }
        if (pattern1.startsWith("/") && pattern1.endsWith("/*")) {
            String subPattern = pattern1.substring(0, pattern1.length() - 2);
            int pLength = subPattern.length();
            if (pattern2.startsWith(subPattern)) {
                if (pattern2.length() == pLength || pattern2.charAt(pLength) == '/') {
                    return true;
                }
            }
        }

        if (pattern1.startsWith("*.")) {
            if (pattern2.endsWith(pattern1.substring(1))) {
                return true;
            }
        }

        if (pattern1.equals("/")) {
            return true;
        }
        return false;
    }

    /**
     * Gets the security metadata from the web app config
     *
     * @param webAppConfig the webAppConfig representing the deployed module
     * @return the security metadata
     */
    private SecurityMetadata getSecurityMetadata(WebAppConfig webAppConfig) {
        WebModuleMetaData wmmd = ((WebAppConfigExtended) webAppConfig).getMetaData();
        return (SecurityMetadata) wmmd.getSecurityMetaData();
    }

    /**
     * Returns whether deny-uncovered-http-methods attribute is set.
     * In order to check this value, entire WebResourceCollection objects need to be examined,
     * since it only set properly when web.xml is processed.
     *
     * @param scList the List of SecurityConstraint objects.
     * @return true if deny-uncovered-http-methods attribute is set, false otherwise.
     */
    private boolean isDenyUncoveredHttpMethods(List<SecurityConstraint> scList) {
        for (SecurityConstraint sc : scList) {
            List<WebResourceCollection> wrcList = sc.getWebResourceCollections();
            for (WebResourceCollection wrc : wrcList) {
                if (wrc.getDenyUncoveredHttpMethods()) {
                    return true;
                }
            }
        }
        return false;
    }

}
