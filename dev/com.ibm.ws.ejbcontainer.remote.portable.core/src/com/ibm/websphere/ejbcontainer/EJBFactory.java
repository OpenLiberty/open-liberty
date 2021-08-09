/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ejbcontainer;

import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.ejb.EJBException;

/**
 * <code>EJBFactory</code> defines the methods that allow a remote client to
 * create or find remote EJB objects. The factory is used to support
 * EJB-Link and Auto-Link in the Client Container. <p>
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
 **/
public interface EJBFactory extends Remote
{
    /**
     * Returns a reference to the EJB object specified. <p>
     * 
     * The combination of application name and beanName or ejb-link
     * including module information uniquely identify any EJB object. <p>
     * 
     * All parameters must be non-null. <p>
     * 
     * @param application name of the application containing the EJB.
     * @param beanName beanName or ejb-link, including module information.
     * @param interfaceName component home or business interface of EJB.
     * 
     * @return a reference to the EJB object specified.
     * 
     * @exception EJBException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    public Object create(String application,
                         String beanName,
                         String interfaceName)
                    throws EJBException,
                    RemoteException;

    /**
     * Returns a reference to the EJB object specified. <p>
     * 
     * The combination of application name, module name, and bean name
     * uniquely identify any EJB object. <p>
     * 
     * All parameters must be non-null. <p>
     * 
     * @param application name of the application containing the EJB.
     * @param module name of the module containing the EJB.
     * @param beanName name of the specific EJB.
     * @param interfaceName component home or business interface of EJB.
     * 
     * @return a reference to the EJB object specified.
     * 
     * @exception EJBException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    public Object create(String application,
                         String module,
                         String beanName,
                         String interfaceName)
                    throws EJBException,
                    RemoteException;

    /**
     * Returns a reference to the EJB object specified. <p>
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
     * @param application name of the application containing the EJB.
     * @param beanName name of the specific EJB.
     * @param interfaceName component home or business interface of EJB.
     * 
     * @return a reference to the EJB object specified.
     * 
     * @exception EJBException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    public Object findByBeanName(String application,
                                 String beanName,
                                 String interfaceName)
                    throws EJBException,
                    RemoteException;

    /**
     * Returns a reference to the EJB object specified. <p>
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
     * @param application name of the application containing the EJB.
     * @param interfaceName component home or business interface of EJB.
     * 
     * @return a reference to the EJB object specified.
     * 
     * @exception EJBException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    public Object findByInterface(String application,
                                  String interfaceName)
                    throws EJBException,
                    RemoteException;
}
