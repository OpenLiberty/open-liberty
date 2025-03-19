/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.HttpConstraintElement;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.security.metadata.MatchResponse;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraint;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollectionImpl;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.metadata.WebResourceCollection;
import com.ibm.ws.webcontainer.webapp.WebAppConfigExtended;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInitializationCollaborator;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

/**
 * This class provides a plug point into the web container at a certain state of the servlets.
 * We are specifically interested in the state when the servlets have finished registering and are started.
 * At this point, the web container has processed all the security configuration, and thus we can update
 * our security metadata. We will get the runAs role and security constraints defined via static annotations
 * and programmatically (aka dynamic annotations).
 */

public class ServletStartedListener implements WebAppInitializationCollaborator {
    private static final TraceComponent tc = Tr.register(ServletStartedListener.class);

    private static final String[] STANDARD_METHODS = { "GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE" };
    protected static final String KEY_WEB_JACC_SERVICE = "webJaccService";
    private final AtomicServiceReference<WebJaccService> webJaccService = new AtomicServiceReference<WebJaccService>(KEY_WEB_JACC_SERVICE);

    protected void setWebJaccService(ServiceReference<WebJaccService> reference) {
        webJaccService.setReference(reference);
    }

    protected void unsetWebJaccService(ServiceReference<WebJaccService> reference) {
        webJaccService.unsetReference(reference);
    }

    protected void activate(ComponentContext cc) {
        webJaccService.activate(cc);
    }

    protected void deactivate(ComponentContext cc) {
        webJaccService.deactivate(cc);
    }

    /** {@inheritDoc} */
    @Override
    public void starting(Container moduleContainer) {
        // Do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void started(Container moduleContainer) {
        try {

            WebAppConfig webAppConfig = moduleContainer.adapt(WebAppConfig.class);
            SecurityMetadata securityMetadataFromDD = getSecurityMetadata(webAppConfig);
            updateSecurityMetadata(securityMetadataFromDD, webAppConfig);
            setModuleSecurityMetaData(moduleContainer, securityMetadataFromDD);
            if (com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31) {
                notifyDeployOfUncoveredMethods(webAppConfig);
            }
            if (checkDynamicAnnotation(webAppConfig)) {
                WebJaccService js = webJaccService.getService();
                if (js != null) {
                    js.propagateWebConstraints(webAppConfig.getApplicationName(), webAppConfig.getModuleName(), webAppConfig);
                }
            }
        } catch (UnableToAdaptException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was a problem setting the security meta data.", e);
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public void stopping(Container moduleContainer) {
        // Do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void stopped(Container moduleContainer) {
        // Do nothing
    }

    /**
     *
     */

    public void notifyDeployOfUncoveredMethods(WebAppConfig webAppConfig) {
        MatchResponse matchResponse = MatchResponse.NO_MATCH_RESPONSE;
        boolean isDenyUncoveredHttpMethodsSet = false;

        SecurityMetadata secMetaData = getSecurityMetadata(webAppConfig);

        if (secMetaData != null) {
            SecurityConstraintCollection secConstraintCollection = secMetaData.getSecurityConstraintCollection();
            if (secConstraintCollection != null) {
                List<SecurityConstraint> secConstraints = secConstraintCollection.getSecurityConstraints();

                List<String> aggregatedUrlPatterns = new ArrayList<String>();

                for (SecurityConstraint sc : secConstraints) {

                    List<WebResourceCollection> webResCollection = sc.getWebResourceCollections();
                    for (WebResourceCollection wrc : webResCollection) {
                        if (isDenyUncoveredHttpMethodsSet == false && wrc.getDenyUncoveredHttpMethods()) {
                            isDenyUncoveredHttpMethodsSet = true;
                        }
                        List<String> urlPatterns = wrc.getUrlPatterns();
                        for (String uri : urlPatterns) {
                            if (!aggregatedUrlPatterns.contains(uri)) {
                                aggregatedUrlPatterns.add(uri);
                            }
                        }
                    }
                }

                for (String uriName : aggregatedUrlPatterns) {
                    List<String> uncoveredMethodsWithDeny = new ArrayList<String>();
                    List<String> uncoveredMethodsWithoutDeny = new ArrayList<String>();
                    for (String methodName : STANDARD_METHODS) {
                        matchResponse = secConstraintCollection.getMatchResponse(uriName, methodName);

                        if (MatchResponse.DENY_MATCH_RESPONSE.equals(matchResponse)) {
                            uncoveredMethodsWithDeny.add(methodName);
                        }
                        if (MatchResponse.NO_MATCH_RESPONSE.equals(matchResponse)) {
                            uncoveredMethodsWithoutDeny.add(methodName);
                        }

                    }

                    String listUncoveredMethodsWithDeny = "";
                    boolean uncoveredMethodsFoundWithDenySet = false;
                    for (String ucm : uncoveredMethodsWithDeny) {
                        listUncoveredMethodsWithDeny = listUncoveredMethodsWithDeny.concat(ucm).concat(" ");
                        uncoveredMethodsFoundWithDenySet = true;
                    }

                    String listUncoveredMethodsWithoutDeny = "";
                    boolean uncoveredMethodsFoundWithoutDenySet = false;
                    for (String ucm : uncoveredMethodsWithoutDeny) {
                        listUncoveredMethodsWithoutDeny = listUncoveredMethodsWithoutDeny.concat(ucm).concat(" ");
                        uncoveredMethodsFoundWithoutDenySet = true;
                    }

                    if (com.ibm.ws.webcontainer.osgi.WebContainer.getServletContainerSpecLevel() >= 31) {
                        if (isDenyUncoveredHttpMethodsSet) {
                            if (uncoveredMethodsFoundWithDenySet) {
                                Tr.info(tc, "UNCOVERED_HTTP_METHODS_FOUND", uriName, webAppConfig.getApplicationName(), listUncoveredMethodsWithDeny);
                            }
                        } else {
                            if (uncoveredMethodsFoundWithoutDenySet) {
                                Tr.info(tc, "UNCOVERED_HTTP_METHODS_FOUND_AND_UNPROTECTED", uriName, webAppConfig.getApplicationName(), listUncoveredMethodsWithoutDeny);
                            }

                        }

                    }
                }
            }
        }

    }

    /**
     * Updates the security metadata object (which at this time only has the deployment descriptor info)
     * with the webAppConfig information comprising all sources.
     *
     * @param securityMetadataFromDD the security metadata processed from the deployment descriptor
     * @param webAppConfig the web app configuration provided by the web container
     */
    private void updateSecurityMetadata(SecurityMetadata securityMetadataFromDD, WebAppConfig webAppConfig) {
        for (Iterator<IServletConfig> it = webAppConfig.getServletInfos(); it.hasNext();) {
            IServletConfig servletConfig = it.next();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Updating servlet: " + servletConfig.getServletName());
            }
            updateSecurityMetadataWithRunAs(securityMetadataFromDD, servletConfig);
            updateSecurityMetadataWithSecurityConstraints(securityMetadataFromDD, servletConfig);
        }
    }

    /**
     * Updates the security metadata object (which at this time only has the deployment descriptor info)
     * with the runAs roles defined in the servlet. The sources are the web.xml, static annotations,
     * and dynamic annotations.
     *
     * @param securityMetadataFromDD the security metadata processed from the deployment descriptor
     * @param servletConfig the configuration of the servlet
     */
    public void updateSecurityMetadataWithRunAs(SecurityMetadata securityMetadataFromDD, IServletConfig servletConfig) {
        String runAs = servletConfig.getRunAsRole();
        if (runAs != null) {
            String servletName = servletConfig.getServletName();
            //only add if there is no run-as entry in web.xml
            Map<String, String> servletNameToRunAsRole = securityMetadataFromDD.getRunAsMap();
            if (servletNameToRunAsRole.get(servletName) == null) {
                servletNameToRunAsRole.put(servletName, runAs);
                List<String> allRoles = securityMetadataFromDD.getRoles();
                if (!allRoles.contains(runAs)) {
                    allRoles.add(runAs);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Added runAs role: " + runAs);
                }
            }
        }
    }

    /**
     * Updates the security constraints in the security metadata object (which at this time only has the deployment descriptor info)
     * with the servletSecurity element defined in the servlet.
     * A servletSecurity element only exists for servlets that have static or dynamic annotations.
     *
     * @param securityMetadataFromDD the security metadata processed from the deployment descriptor
     * @param servletConfig the configuration of the servlet
     */
    public void updateSecurityMetadataWithSecurityConstraints(SecurityMetadata securityMetadataFromDD, IServletConfig servletConfig) {
        ServletSecurityElement servletSecurity = servletConfig.getServletSecurity();
        if (servletSecurity != null) {
            Collection<String> urlPatterns = servletConfig.getMappings();
            if (urlPatterns != null) {
                List<SecurityConstraint> securityConstraintsInDD = null;
                SecurityConstraintCollection securityConstraintCollectionInDD = securityMetadataFromDD.getSecurityConstraintCollection();
                // accumulate the list of urlPatternsInDD, patterns for which we have some
                //  security constraint in the DD that apply to this servlet.
                Set<String> urlPatternsInDD = new HashSet<String>();
                if (securityConstraintCollectionInDD != null) {
                    securityConstraintsInDD = securityConstraintCollectionInDD.getSecurityConstraints();
                    for (SecurityConstraint secConstraint : securityConstraintCollectionInDD.getSecurityConstraints()) {
                        for (WebResourceCollection webRescColl : secConstraint.getWebResourceCollections()) {
                            urlPatternsInDD.addAll(webRescColl.getUrlPatterns());
                        }
                    }
                }
                List<String> urlPatternsForAnnotations = new ArrayList<String>(urlPatterns);
                // per spec, constraints from annotations that would match patterns for constraints defined in DD
                //  should have no effect for those patterns, so remove the patterns.
                urlPatternsForAnnotations.removeAll(urlPatternsInDD);
                List<SecurityConstraint> securityConstraints = createSecurityConstraints(securityMetadataFromDD, servletSecurity, urlPatternsForAnnotations);
                if (securityConstraintsInDD == null) {
                    securityConstraintsInDD = new ArrayList<SecurityConstraint>();
                }
                securityConstraintsInDD.addAll(securityConstraints);

                if (securityConstraintCollectionInDD == null) {
                    securityConstraintCollectionInDD = new SecurityConstraintCollectionImpl(securityConstraintsInDD);
                    securityConstraintCollectionInDD.addSecurityConstraints(securityConstraintsInDD);
                    securityMetadataFromDD.setSecurityConstraintCollection(securityConstraintCollectionInDD);
                }
            }
        }
    }

    /**
     * Constructs a list of SecurityConstraint objects from the given ServletSecurityElement and list of URL patterns.
     *
     * @param securityMetadataFromDD the security metadata processed from the deployment descriptor, for updating the roles
     * @param servletSecurity the ServletSecurityElement that represents the information parsed from the @ServletSecurity annotation
     * @param urlPatterns the list of URL patterns defined in the @WebServlet annotation
     * @return a list of SecurityConstraint objects
     */
    private List<SecurityConstraint> createSecurityConstraints(SecurityMetadata securityMetadataFromDD, ServletSecurityElement servletSecurity, Collection<String> urlPatterns) {
        List<SecurityConstraint> securityConstraints = new ArrayList<SecurityConstraint>();
        securityConstraints.add(getConstraintFromHttpElement(securityMetadataFromDD, urlPatterns, servletSecurity));
        securityConstraints.addAll(getConstraintsFromHttpMethodElement(securityMetadataFromDD, urlPatterns, servletSecurity));
        return securityConstraints;
    }

    /**
     * Gets the security constraint from the HttpConstraint element defined in the given ServletSecurityElement
     * with the given list of url patterns.
     *
     * This constraint applies to all methods that are not explicitly overridden by the HttpMethodConstraint element. The method
     * constraints are defined as omission methods in this security constraint.
     *
     * @param securityMetadataFromDD the security metadata processed from the deployment descriptor, for updating the roles
     * @param urlPatterns the list of URL patterns defined in the @WebServlet annotation
     * @param servletSecurity the ServletSecurityElement that represents the information parsed from the @ServletSecurity annotation
     * @return the security constraint defined by the @HttpConstraint annotation
     */
    private SecurityConstraint getConstraintFromHttpElement(SecurityMetadata securityMetadataFromDD, Collection<String> urlPatterns, ServletSecurityElement servletSecurity) {
        List<String> omissionMethods = new ArrayList<String>();
        if (!servletSecurity.getMethodNames().isEmpty()) {
            omissionMethods.addAll(servletSecurity.getMethodNames()); //list of methods named by @HttpMethodConstraint
        }

        WebResourceCollection webResourceCollection = new WebResourceCollection((List<String>) urlPatterns, new ArrayList<String>(), omissionMethods, securityMetadataFromDD.isDenyUncoveredHttpMethods());
        List<WebResourceCollection> webResourceCollections = new ArrayList<WebResourceCollection>();
        webResourceCollections.add(webResourceCollection);
        return createSecurityConstraint(securityMetadataFromDD, webResourceCollections, servletSecurity, true);
    }

    /**
     * Gets the security constraints from the HttpMethodConstraint elements defined in the given ServletSecurityElement
     * with the given list of url patterns.
     *
     * @param securityMetadataFromDD the security metadata processed from the deployment descriptor, for updating the roles
     * @param urlPatterns the list of URL patterns defined in the @WebServlet annotation
     * @param servletSecurity the ServletSecurityElement that represents the information parsed from the @ServletSecurity annotation
     * @return a list of security constraints defined by the @HttpMethodConstraint annotations
     */
    private List<SecurityConstraint> getConstraintsFromHttpMethodElement(SecurityMetadata securityMetadataFromDD, Collection<String> urlPatterns,
                                                                         ServletSecurityElement servletSecurity) {
        List<SecurityConstraint> securityConstraints = new ArrayList<SecurityConstraint>();
        Collection<HttpMethodConstraintElement> httpMethodConstraints = servletSecurity.getHttpMethodConstraints();
        for (HttpMethodConstraintElement httpMethodConstraint : httpMethodConstraints) {
            String method = httpMethodConstraint.getMethodName();
            List<String> methods = new ArrayList<String>();
            methods.add(method);
            WebResourceCollection webResourceCollection = new WebResourceCollection((List<String>) urlPatterns, methods, new ArrayList<String>(), securityMetadataFromDD.isDenyUncoveredHttpMethods());
            List<WebResourceCollection> webResourceCollections = new ArrayList<WebResourceCollection>();
            webResourceCollections.add(webResourceCollection);
            securityConstraints.add(createSecurityConstraint(securityMetadataFromDD, webResourceCollections, httpMethodConstraint, false));

        }
        return securityConstraints;
    }

    /**
     * Creates a security constraint from the given web resource collections, url patterns and HttpConstraint element.
     *
     * @param securityMetadataFromDD the security metadata processed from the deployment descriptor, for updating the roles
     * @param webResourceCollections a list of web resource collections
     * @param httpConstraint the element representing the information in the @HttpConstraint annotation
     * @return the security constraint
     */
    private SecurityConstraint createSecurityConstraint(SecurityMetadata securityMetadataFromDD, List<WebResourceCollection> webResourceCollections,
                                                        HttpConstraintElement httpConstraint, boolean fromHttpConstraint) {
        List<String> roles = createRoles(httpConstraint);
        List<String> allRoles = securityMetadataFromDD.getRoles();
        for (String role : roles) {
            if (!allRoles.contains(role)) {
                allRoles.add(role);
            }
        }
        boolean sslRequired = isSSLRequired(httpConstraint);
        boolean accessPrecluded = isAccessPrecluded(httpConstraint);
        boolean accessUncovered = isAccessUncovered(httpConstraint);
        return new SecurityConstraint(webResourceCollections, roles, sslRequired, accessPrecluded, fromHttpConstraint, accessUncovered);
    }

    /**
     * Gets a list of roles from the rolesAllowed element in the @HttpConstraint annotation
     *
     * @param httpConstraint the element representing the information in the @HttpConstraint annotation
     * @return a list of allowed roles defined in the annotation's security constraint
     */
    private List<String> createRoles(HttpConstraintElement httpConstraint) {
        String[] rolesFromAnno = httpConstraint.getRolesAllowed();
        List<String> roles = new ArrayList<String>();
        for (int i = 0; i < rolesFromAnno.length; i++) {
            roles.add(rolesFromAnno[i]);
        }
        return roles;
    }

    /**
     * Determines if SSL is required for the given HTTP constraint.
     *
     * SSL is required if the transport guarantee is any value other than NONE.
     *
     * @param httpConstraint the element representing the information in the @HttpConstraint annotation
     * @return true if SSL is required, otherwise false
     */
    private boolean isSSLRequired(HttpConstraintElement httpConstraint) {
        boolean sslRequired = false;
        TransportGuarantee transportGuarantee = httpConstraint.getTransportGuarantee();
        if (transportGuarantee != TransportGuarantee.NONE) {
            sslRequired = true;
        }
        return sslRequired;
    }

    /**
     * Determines if access is precluded for the given HTTP constraint.
     *
     * Access is precluded when there are no roles, and the emptyRoleSemantic
     * defined in the annotation is DENY.
     *
     * @param httpConstraint the element representing the information in the @HttpConstraint annotation
     * @return true if access is precluded, otherwise false
     */
    private boolean isAccessPrecluded(HttpConstraintElement httpConstraint) {
        boolean accessPrecluded = false;
        String[] roles = httpConstraint.getRolesAllowed();
        if (roles == null || roles.length == 0) {
            if (EmptyRoleSemantic.DENY == httpConstraint.getEmptyRoleSemantic())
                accessPrecluded = true;
        }
        return accessPrecluded;
    }

    /**
     * Determines if access is uncovered for the given HTTP constraint.
     *
     * Access is uncovered when there are no roles, and the emptyRoleSemantic
     * defined in the annotation is PERMIT.
     *
     * @param httpConstraint the element representing the information in the @HttpConstraint annotation
     * @return true if access is precluded, otherwise false
     */
    private boolean isAccessUncovered(HttpConstraintElement httpConstraint) {

        boolean accessUncovered = false;
        String[] roles = httpConstraint.getRolesAllowed();
        if (roles == null || roles.length == 0) {
            if (EmptyRoleSemantic.PERMIT == httpConstraint.getEmptyRoleSemantic())
                accessUncovered = true;
        }
        return accessUncovered;
    }

    /**
     * Sets the given security metadata on the deployed module's web module metadata for retrieval later.
     *
     * @param deployedModule the deployed module to get the web module metadata
     * @param securityMetadataFromDD the security metadata processed from the deployment descriptor
     */
    private void setModuleSecurityMetaData(Container moduleContainer, SecurityMetadata securityMetadataFromDD) {
        try {
            WebModuleMetaData wmmd = moduleContainer.adapt(WebModuleMetaData.class);
            wmmd.setSecurityMetaData(securityMetadataFromDD);
        } catch (UnableToAdaptException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "There was a problem setting the security meta data.", e);
            }
        }
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

    protected boolean checkDynamicAnnotation(WebAppConfig webAppConfig) {
        boolean result = false;
        for (Iterator<IServletConfig> it = webAppConfig.getServletInfos(); it.hasNext();) {
            IServletConfig servletConfig = it.next();
            List<String> mappings = servletConfig.getMappings();
            ServletSecurityElement servletSecurity = servletConfig.getServletSecurity();
            if (servletSecurity != null && mappings != null && !mappings.isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "*** Found dynamic security constraints for servlet: " + servletConfig.getServletName());
                }
                result = true;
                break;
            }
        }
        return result;
    }

}
