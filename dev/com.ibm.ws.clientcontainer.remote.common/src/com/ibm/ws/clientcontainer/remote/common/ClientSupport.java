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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Set;

import javax.naming.NameClassPair;
import javax.naming.NamingException;

import com.ibm.ws.container.service.naming.RemoteObjectInstance;

/**
 * This is the remote interface to a singleton class bound in CosNaming to allow remote clients (the
 * client container in particular) to access objects bound in the server's namespace. The byte[]
 * is a serialized RemoteObjectInstance (wrapper) which is intended to be serialized/deserialized
 * using the Serialization Service - it will either contain the javax.naming.Reference, the actual
 * object, or data indicating that the value is a remote EJB.
 */
public interface ClientSupport extends Remote {
    public static final String SERVICE_NAME = "ClientSupport";

    RemoteObjectInstance getRemoteObjectInstance(String appName, String moduleName, String compName, String namespaceString, String jndiName) throws NamingException, RemoteException;

    public boolean hasRemoteObjectWithPrefix(String appName, String moduleName, String compName, String namespaceString, String name) throws NamingException, RemoteException;

    public Collection<? extends NameClassPair> listRemoteInstances(String appName, String moduleName, String compName, String namespaceString, String nameInContext) throws NamingException, RemoteException;

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
    public Set<String> getEJBRmicCompatibleClasses(String appName) throws RemoteException;

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
     * @param beanInterface component home or business interface of EJB.
     *
     * @return a RemoteObjectInstance containing a reference to the EJB object specified.
     *
     * @exception RemoteException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    public RemoteObjectInstance createEJB(String appName, String beanName, String beanInterface) throws NamingException, RemoteException;

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
     * @param beanInterface component home or business interface of EJB.
     *
     * @return a RemoteObjectInstance containing a reference to the EJB object specified.
     *
     * @exception RemoteException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    public RemoteObjectInstance createEJB(String appName, String moduleName, String beanName, String beanInterface) throws NamingException, RemoteException;

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
     * considered an ambiguous reference, and a RemoteException will
     * be thrown. <p>
     *
     * @param appName name of the application containing the EJB.
     * @param beanName name of the specific EJB.
     * @param beanInterface component home or business interface of EJB.
     *
     * @return a RemoteObjectInstance containing a reference to the EJB object specified.
     *
     * @exception RemoteException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    public RemoteObjectInstance findEJBByBeanName(String appName, String beanName, String beanInterface) throws NamingException, RemoteException;

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
     * interface, this is considered an ambiguous reference, and a
     * RemoteException will be thrown. <p>
     *
     * @param appName name of the application containing the EJB.
     * @param beanInterface component home or business interface of EJB.
     *
     * @return a RemoteObjectInstance containing a reference to the EJB object specified.
     *
     * @exception RemoteException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    public RemoteObjectInstance findEJBByInterface(String appName, String beanInterface) throws NamingException, RemoteException;
}
