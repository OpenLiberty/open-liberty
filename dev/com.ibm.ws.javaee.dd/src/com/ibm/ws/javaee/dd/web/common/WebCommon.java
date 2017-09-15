/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.web.common;

import java.util.List;

import com.ibm.ws.javaee.dd.common.DescriptionGroup;
import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRefsGroup;
import com.ibm.ws.javaee.dd.common.Listener;
import com.ibm.ws.javaee.dd.common.MessageDestination;
import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.common.SecurityRole;
import com.ibm.ws.javaee.dd.jsp.JSPConfig;

/**
 *
 */
public interface WebCommon
                extends DescriptionGroup, JNDIEnvironmentRefsGroup {

    /**
     * @return true if &lt;distributable> is specified
     */
    boolean isSetDistributable();

    /**
     * @return &lt;context-param> as a read-only list
     */
    List<ParamValue> getContextParams();

    /**
     * @return &lt;filter> as a read-only list
     */
    List<Filter> getFilters();

    /**
     * @return &lt;filter-mapping> as a read-only list
     */
    List<FilterMapping> getFilterMappings();

    /**
     * @return &lt;listener> as a read-only list
     */
    List<Listener> getListeners();

    /**
     * @return &lt;servlet> as a read-only list
     */
    List<Servlet> getServlets();

    /**
     * @return &lt;servlet-mapping> as a read-only list
     */
    List<ServletMapping> getServletMappings();

    /**
     * @return &lt;session-config>, or null if unspecified
     */
    SessionConfig getSessionConfig();

    /**
     * @return &lt;mime-mapping> as a read-only list
     */
    List<MimeMapping> getMimeMappings();

    /**
     * @return &lt;welcome_file_list>, or null if unspecified
     */
    WelcomeFileList getWelcomeFileList();

    /**
     * @return &lt;error_page> as a read-only list
     */
    List<ErrorPage> getErrorPages();

    /**
     * @return &lt;jsp_config>, or null if unspecified
     */
    JSPConfig getJSPConfig();

    /**
     * @return &lt;deny_uncovered_http_methods, or null if unspecified
     */
    //DenyUncoveredHttpMethods getDenyUncoveredHttpMethods();
    /**
     * @return true if &lt;deny_uncovered_http_methods> is specified
     */
    boolean isSetDenyUncoveredHttpMethods();

    /**
     * @return &lt;security_constraint> as a read-only list
     */
    List<SecurityConstraint> getSecurityConstraints();

    /**
     * @return &lt;login_config>, or null if unspecified
     */
    LoginConfig getLoginConfig();

    /**
     * @return &lt;security_role> as a read-only list
     */
    List<SecurityRole> getSecurityRoles();

    /**
     * @return &lt;message-destination> as a read-only list
     */
    List<MessageDestination> getMessageDestinations();

    /**
     * @return &lt;locale-encoding-mapping-list>, or null if unspecified
     */
    LocaleEncodingMappingList getLocaleEncodingMappingList();
}
