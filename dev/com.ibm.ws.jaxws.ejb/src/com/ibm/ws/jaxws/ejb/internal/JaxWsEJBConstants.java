/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejb.internal;

import java.util.ResourceBundle;

public class JaxWsEJBConstants {
    public static final String TR_GROUP = "JaxWsEJB";

    public static final String TR_RESOURCE_BUNDLE = "com.ibm.ws.jaxws.ejb.internal.resources.JaxWsEJBMessages";

    public static final ResourceBundle messages = ResourceBundle.getBundle(TR_RESOURCE_BUNDLE);

    /**
     * This constant represents the parameter name used to store EJB instances on an Exchange
     */
    public static final String EJB_INSTANCE = "com.ibm.ws.jaxws.EXCHANGE_EJBINSTANCE";

    /**
     * This constant represents the parameter name used to store WSEJBEndpointManager instances on an Exchange
     */
    public static final String WS_EJB_ENDPOINT_MANAGER = "com.ibm.ws.jaxws.EXCHANGE_WSEJBENDPOINTMANAGER";

}
