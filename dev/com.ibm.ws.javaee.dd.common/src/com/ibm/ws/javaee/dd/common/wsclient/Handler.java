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
package com.ibm.ws.javaee.dd.common.wsclient;

import java.util.List;

import com.ibm.ws.javaee.dd.common.DescriptionGroup;
import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.common.QName;

/**
 * Represents &lt;handler>.
 */
public interface Handler
                extends DescriptionGroup
{
    /**
     * @return &lt;handler-name>
     */
    String getHandlerName();

    /**
     * @return &lt;handler-class>
     */
    String getHandlerClassName();

    /**
     * @return &lt;init-param> as a read-only list
     */
    List<ParamValue> getInitParams();

    /**
     * @return &lt;soap-header> as a read-only list
     */
    List<QName> getSoapHeaders();

    /**
     * @return &lt;soap-role> as a read-only list
     */
    List<String> getSoapRoles();

    /**
     * @return &lt;port-name> as a read-only list
     */
    List<String> getPortNames();
}
