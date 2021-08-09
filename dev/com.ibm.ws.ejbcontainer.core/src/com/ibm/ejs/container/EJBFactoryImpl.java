/*******************************************************************************
 * Copyright (c) 1998, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.RemoteException;

import javax.ejb.EJBException;

import com.ibm.websphere.ejbcontainer.EJBFactory;

/**
 * <code>EJBFactoryImpl</code> provides a 'wrapper' implementation of the
 * EJBFactory remote interface, to provide access to the HomeOfHomes,
 * allowing a remote client to create or find remote EJB objects.
 * The factory is used to support EJB-Link and Auto-Link in the
 * Client Container. <p>
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
 *
 * An instance of EJBFactoryImpl will be bound into the namespace for every
 * application and module, which the client container may then lookup when
 * EJB-Link or Auto-Link resolution is required. This may occur during
 * injection processing or a lookup of a reference in the component
 * namespace. Although HomeOfHomes is a singleton in the server process,
 * a separate instance is required to insure the request is routed to
 * the correct server process and cluster. <p>
 **/
public class EJBFactoryImpl extends EJSRemoteWrapper implements EJBFactory
{
    private final EJBLinkResolver ejbLinkResolver;

    /**
     * Construct an EJBFactoryImpl with a reference to an EJBLinkResolver. <p>
     *
     * After creating an instance with this constructor, the instance will
     * not be usable as a 'wrapper' until the inherited fields from
     * EJSWrapperBase and EJSRemoteWrapper have been set. See the
     * corresponding EJSWrapperCommon constructor. <p>
     *
     * An EJBFactoryImpl instance serves as a 'wrapper' for the
     * HomeOfHomes, providing EJB-Link and Auto-Link support for the
     * Client Container. <p>
     *
     * Although the HomeOfHomes is a singleton, a different instance of
     * this 'wrapper' must be created per application and/or module
     * to provide a unique servant key and correct cluster association. <p>
     *
     * @param EJBLinkResolver EJB-Link/Auto-Linker helper for the HomeOfHomes.
     **/
    public EJBFactoryImpl(EJBLinkResolver ejbLinkResolver)
    {
        this.ejbLinkResolver = ejbLinkResolver;
    }

    // --------------------------------------------------------------------------
    //
    // EJBFactory interface methods
    //
    // --------------------------------------------------------------------------

    /**
     * Returns a reference to the EJB object specified. <p>
     *
     * The combination of application name and beanName or ejb-link
     * including module information uniquely identify any EJB object. <p>
     *
     * All parameters must be non-null. <p>
     *
     * @param application name of the application containing the EJB.
     * @param beanName beanName or ejb-link that includes module information.
     * @param interfaceName component home or business interface of EJB.
     *
     * @return a reference to the EJB object specified.
     *
     * @exception EJBException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     **/
    @Override
    public Object create(String application,
                         String beanName,
                         String interfaceName)
                    throws EJBException,
                    RemoteException
    {
        return ejbLinkResolver.create(application, beanName, interfaceName);
    }

    /**
     * Returns a reference to the EJB object specified. <p>
     *
     * The combination of application name, module name, and bean name
     * uniquely identify any EJB object. <p>
     *
     * All parameters must be non-null. <p>
     *
     * Note: This method must be maintained to support backward compatibility
     * with the client container prior to WAS 8.0. <p>
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
    @Override
    public Object create(String application,
                         String module,
                         String beanName,
                         String interfaceName)
                    throws EJBException,
                    RemoteException
    {
        return ejbLinkResolver.create(application, module, beanName, interfaceName);
    }

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
    @Override
    public Object findByBeanName(String application,
                                 String beanName,
                                 String interfaceName)
                    throws EJBException,
                    RemoteException
    {
        return ejbLinkResolver.findByBeanName(application, beanName, interfaceName);
    }

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
    @Override
    public Object findByInterface(String application,
                                  String interfaceName)
                    throws EJBException,
                    RemoteException
    {
        return ejbLinkResolver.findByInterface(application, interfaceName);
    }
}
