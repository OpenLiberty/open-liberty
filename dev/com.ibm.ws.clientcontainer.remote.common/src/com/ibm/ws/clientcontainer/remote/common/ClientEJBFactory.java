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
package com.ibm.ws.clientcontainer.remote.common;

import java.rmi.RemoteException;
import java.util.Set;

import com.ibm.ws.container.service.naming.RemoteObjectInstance;

/**
 * Defines the methods that allow a remote client to create or find remote
 * EJB objects. The factory is used to support EJB-Link and Auto-Link in
 * the Client Container. <p>
 *
 * When an application defines an EJB reference, but does not provide any
 * binding information for that reference, this is considered either an
 * EJB-Link or Auto-Link scenario. EJB-Link refers to a configuration where
 * the 'ejb-ref' has an 'ejb-link' (@EJB with 'beanName' parameter).
 * And, Auto-Link refers to a configuration where the 'ejb-ref' does not
 * contain an 'ejb-link', and thus only the reference interface is known. <p>
 *
 * For both the EJB-Link and Auto-Link scenarios, component name space and
 * injection processing will use the methods on the EJBFactory to resolve
 * the reference. <p>
 */
public interface ClientEJBFactory {

    /**
     * Returns a set of Remote EJB interface classes for which the dynamically
     * generated stub classes need to be compatible with RMIC generated stubs
     * and ties when accessed by the client application. <p>
     *
     * In full profile, pre-EJB 3 modules are processed by ejbdeploy, and rmic
     * is used to generate stubs for remote home and interface classes. These
     * stubs need to exist so that we do not dynamically generate stubs that
     * use the "WAS EJB 3" marshalling rules. <p>
     *
     * In Liberty profile, there is no separate deploy step, so we need to
     * ensure that stubs for pre-EJB 3 modules are generated with as much
     * compatibility with RMIC as we can. <p>
     *
     * @param appName name of the client application.
     *
     * @return Set of EJB interface class names that require compatibility with RMIC.
     */
    Set<String> getRmicCompatibleClasses(String appName) throws RemoteException;

    /**
     * Returns a RemoteObjectInstance containing a reference to the
     * EJB object specified. <p>
     *
     * The combination of application name and beanName or ejb-link
     * including module information uniquely identify any EJB object. <p>
     *
     * All parameters must be non-null. <p>
     *
     * @param appName name of the application containing the EJB.
     * @param beanName beanName or ejb-link, including module information.
     * @param interfaceName component home or business interface of EJB.
     *
     * @return a reference to the EJB object specified.
     *
     * @exception RemoteException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    RemoteObjectInstance create(String appName, String beanName, String interfaceName) throws RemoteException;

    /**
     * Returns a RemoteObjectInstance containing a reference to the
     * EJB object specified. <p>
     *
     * The combination of application name, module name, and bean name
     * uniquely identify any EJB object. <p>
     *
     * All parameters must be non-null. <p>
     *
     * @param appName name of the application containing the EJB.
     * @param moduleName name of the module containing the EJB.
     * @param beanName name of the specific EJB.
     * @param interfaceName component home or business interface of EJB.
     *
     * @return a reference to the EJB object specified.
     *
     * @exception RemoteException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    RemoteObjectInstance create(String appName, String moduleName, String beanName, String interfaceName) throws RemoteException;

    /**
     * Returns a RemoteObjectInstance containing a reference to the
     * EJB object specified. <p>
     *
     * The combination of application name, module name, and bean name
     * uniquely identify any EJB object. <p>
     *
     * This method if intended to be used when the specific module name
     * is not known. All modules in the application will be searched
     * for an EJB with the specified name. If multiple modules in the
     * application contain an EJB with the specified name, this is
     * considered an ambiguous reference, and an EJBException will
     * be thrown. <p>
     *
     * @param appName name of the application containing the EJB.
     * @param beanName name of the specific EJB.
     * @param interfaceName component home or business interface of EJB.
     *
     * @return a reference to the EJB object specified.
     *
     * @exception RemoteException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    RemoteObjectInstance findByBeanName(String appName, String beanName, String interfaceName) throws RemoteException;

    /**
     * Returns a RemoteObjectInstance containing a reference to the
     * EJB object specified. <p>
     *
     * The combination of application name, module name, and bean name
     * uniquely identify any EJB object. <p>
     *
     * This method if intended to be used when neither the specific
     * module name nor bean name are known. All modules in the application
     * will be searched for an EJB that implements the specified interface.
     * If multiple EJB objects in the application implement the specified
     * interface, this is considered an ambiguous reference, and an
     * EJBException will be thrown. <p>
     *
     * @param appName name of the application containing the EJB.
     * @param interfaceName component home or business interface of EJB.
     *
     * @return a reference to the EJB object specified.
     *
     * @exception RemoteException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    RemoteObjectInstance findByInterface(String appName, String interfaceName) throws RemoteException;
}
