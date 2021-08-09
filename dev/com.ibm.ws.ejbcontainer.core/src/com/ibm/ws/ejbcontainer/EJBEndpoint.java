/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer;

import java.lang.reflect.Method;
import java.util.List;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.csi.J2EEName;

/**
 * Basic information about an EJB.
 */
public interface EJBEndpoint {

    /**
     * @return the Java EE name for the EJB; unique within the server
     */
    J2EEName getJ2EEName();

    /**
     * @return the EJB name; unique within the module
     */
    String getName();

    /**
     * @return the EJB type
     */
    EJBType getEJBType();

    /**
     * @return the class name configured for this EJB
     */
    String getClassName();

    /**
     * @return {@code true} if the EJB has a no-interface view
     */
    boolean isLocalBean();

    /**
     * @return the remote home interface name, or null if unspecified
     */
    String getHomeInterfaceName();

    /**
     * @return the remote component interface name, or null if unspecified
     */
    String getRemoteInterfaceName();

    /**
     * @return the local business interface names as a read-only list
     */
    List<String> getLocalBusinessInterfaceNames();

    /**
     * @return the remote business interface names as a read-only list
     */
    List<String> getRemoteBusinessInterfaceNames();

    /**
     * @return {@code true} if the EJB has any kind of webservice interface including
     *         service-endpoint, {@code @WebService} and {@code @WebServiceProvider}
     */
    boolean isWebService();

    /**
     * @return methods on the EJB implementation class that are declared as stateful remove methods as a read-only list
     * @throws EJBConfigurationException if the bean implementation class cannot be loaded
     */
    List<Method> getStatefulRemoveMethods() throws EJBConfigurationException;

    /**
     * @return {@code true} if the EJB is a Stateful Session Bean and is passivation capable
     */
    boolean isPassivationCapable();

    /**
     * @return a reference factory for this session bean
     * @throws IllegalStateException if reference factories are not supported for this EJB type
     */
    EJBReferenceFactory getReferenceFactory();
}
