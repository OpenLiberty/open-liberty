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
import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.common.RunAs;
import com.ibm.ws.javaee.dd.common.SecurityRoleRef;

/**
 *
 */
public interface Servlet
                extends DescriptionGroup {

    /**
     * @return &lt;servlet-name>
     */
    String getServletName();

    /**
     * @return &lt;servlet-class>, or null if unspecified
     */
    String getServletClass();

    /**
     * @return &lt;jsp-file>, or null if unspecified
     */
    String getJSPFile();

    /**
     * @return &lt;init-param> as a read-only list
     */
    List<ParamValue> getInitParams();

    /**
     * @return true if &lt;load-on-startup> is specified
     * @see #isNullLoadOnStartup
     * @see #getLoadOnStartup
     */
    boolean isSetLoadOnStartup();

    /**
     * @return true if &lt;load-on-startup> is specified and was null
     * @see #isSetLoadOnStartup
     * @see #getLoadOnStartup
     */
    boolean isNullLoadOnStartup();

    /**
     * @return &lt;load-on-startup> if specified and not null
     * @see #isSetLoadOnStartup
     * @see #isNullLoadOnStartup
     */
    int getLoadOnStartup();

    /**
     * @return true if &lt;enabled> is specified
     * @see #isEnabled
     */
    boolean isSetEnabled();

    /**
     * @return &lt;enabled> if specified
     * @see #isSetEnabled
     */
    boolean isEnabled();

    /**
     * @return true if &lt;async-supported> is specified
     * @see #isAsyncSupported
     */
    boolean isSetAsyncSupported();

    /**
     * @return &lt;async-supported> if specified
     * @see #isSetAsyncSupported
     */
    boolean isAsyncSupported();

    /**
     * @return &lt;run-as>, or null if unspecified
     */
    RunAs getRunAs();

    /**
     * @return &lt;security-role-ref> as a read-only list
     */
    List<SecurityRoleRef> getSecurityRoleRefs();

    /**
     * @return &lt;multipart-config>, or null if unspecified
     */
    MultipartConfig getMultipartConfig();

}
