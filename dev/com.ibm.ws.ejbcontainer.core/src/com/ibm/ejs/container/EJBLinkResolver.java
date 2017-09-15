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

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;

import javax.ejb.EJBException;

import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ejbcontainer.EJBFactory;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntime;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * <code>EJBLinkResolver</code> provides the implementation of the
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
 **/
public class EJBLinkResolver implements EJBFactory
{
    private static final String CLASS_NAME = EJBLinkResolver.class.getName();

    private static final TraceComponent tc = Tr.register(EJBLinkResolver.class, "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    private HomeOfHomes homeOfHomes;
    private J2EENameFactory j2eeNameFactory;

    /**
     * Initialize an EJBLinkResolver with a reference to the HomeOfHomes and
     * J2EENameFactory. <p>
     *
     * @param homeOfHomes the singleton instance of the HomeOfHomes.
     * @param j2eeNameFactory the J2EEName factory
     **/
    public EJBLinkResolver initialize(HomeOfHomes homeOfHomes, J2EENameFactory j2eeNameFactory)
    {
        this.homeOfHomes = homeOfHomes;
        this.j2eeNameFactory = j2eeNameFactory;
        return this;
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
                    throws EJBException, RemoteException
    {
        if (application == null)
            throw new IllegalArgumentException("Application name not specified");
        if (beanName == null)
            throw new IllegalArgumentException("Bean name not specified");
        if (interfaceName == null)
            throw new IllegalArgumentException("Interface name not specified");

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "create : application = " + application +
                         ", beanName = " + beanName +
                         ", interface = " + interfaceName);

        // -----------------------------------------------------------------------
        // First - find the bean home (EJSHome) based on what is known
        // -----------------------------------------------------------------------

        EJSHome home = null;
        try
        {
            HomeRecord hr = homeOfHomes.resolveEJBLink(application, null, beanName);
            home = hr.getHomeAndInitialize();
        } catch (Throwable ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".create",
                                        "186", this);
            EJBException ejbex = ExceptionUtil.EJBException
                            ("Failure locating " + beanName, ex);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "create: " + ejbex);

            throw ejbex;
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "home = " + home.getJ2EEName());

        // -----------------------------------------------------------------------
        // Second - determine the interface type, and create the return object
        // -----------------------------------------------------------------------

        Object retObj = create(home, interfaceName);

        // Careful not to call toString or it may invoke the customer bean!
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "create : " + retObj.getClass().getName());

        return retObj;
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
                    throws EJBException, RemoteException
    {
        if (application == null)
            throw new IllegalArgumentException("Application name not specified");
        if (module == null)
            throw new IllegalArgumentException("Module name not specified");
        if (!module.endsWith(".jar") && !module.endsWith(".war"))
            throw new IllegalArgumentException("Module must be a .jar or .war file");
        if (beanName == null)
            throw new IllegalArgumentException("Bean name not specified");
        if (interfaceName == null)
            throw new IllegalArgumentException("Interface name not specified");

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "create : application = " + application +
                         ", module = " + module +
                         ", bean = " + beanName +
                         ", interface = " + interfaceName);

        Object retObj = null;
        EJSHome home = null;
        J2EEName j2eeName = null;

        // -----------------------------------------------------------------------
        // First - find the bean home (EJSHome) based on what is known
        // -----------------------------------------------------------------------

        try
        {
            // This is ejb-link with a specified jar file, so look in the
            // specified module only.
            j2eeName = j2eeNameFactory.create(application, module, beanName);
            home = (EJSHome) homeOfHomes.getHome(j2eeName);

            if (home == null)
            {
                // Either the ejb ref was configured wrong, and points to a
                // bean that does not exist, or the bean has been uninstalled.
                // To be consistent with the other HomeOfHome methods, throw
                // an EJBNotFoundException, and wrap in EJBException.
                throw new EJBNotFoundException("EJB named " + beanName +
                                               " not present in module " +
                                               module + " of application " +
                                               application);
            }
        } catch (Throwable ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".create",
                                        "182", this);
            EJBException ejbex = ExceptionUtil.EJBException
                            ("Failure locating " + j2eeName, ex);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "create: " + ejbex);
            throw ejbex;
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "home = " + home.getJ2EEName());

        // -----------------------------------------------------------------------
        // Second - determine the interface type, and create the return object
        // -----------------------------------------------------------------------

        retObj = create(home, interfaceName);

        // Careful not to call toString or it may invoke the customer bean!
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "create : " + retObj.getClass().getName());

        return retObj;
    }

    /**
     * Determine the interface type, and create the return object using
     * the specified home. <p>
     *
     * @param home home for bean type identified by beanName/ejb-link.
     * @param interfaceName component home or business interface of EJB.
     *
     * @return a reference to the EJB object specified.
     *
     * @exception EJBException is thrown when the specified EJB cannot
     *                be found or a failure occurs creating an instance.
     */
    private Object create(EJSHome home,
                          String interfaceName)
                    throws EJBException, RemoteException
    {
        BeanMetaData bmd = home.getBeanMetaData();

        // First, check against the home interfaces
        if (interfaceName.equals(bmd.localHomeInterfaceClassName))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "match on local home");

            return home.getWrapper().getLocalObject();
        }
        else if (interfaceName.equals(bmd.homeInterfaceClassName))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "match on remote home");

            return getRemoteHomeReference(home, interfaceName);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "looking for match on business interface");

            // The only thing left are the business interfaces
            return createBusinessObject(home, bmd, interfaceName, true);
        } // End of non-home annotation processing
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
                    throws EJBException, RemoteException
    {
        if (application == null)
            throw new IllegalArgumentException("Application name not specified");
        if (beanName == null)
            throw new IllegalArgumentException("Bean name not specified");
        if (interfaceName == null)
            throw new IllegalArgumentException("Interface name not specified");

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "findByBeanName : App = " + application +
                         ", bean = " + beanName + ", interface = " + interfaceName);

        Object retObj = null;
        EJSHome home = null;

        // -----------------------------------------------------------------------
        // First - find the bean home (EJSHome) based on what is known
        // -----------------------------------------------------------------------

        try
        {
            // A module was not explicitly specified, so look for the home in all
            // of the modules within the same application.
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "looking for home in all app modules...");

            home = homeOfHomes.getHomeByName(application, beanName);
        } catch (Throwable ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".findByBeanName",
                                        "182", this);
            EJBException ejbex = ExceptionUtil.EJBException
                            ("Failure locating bean " + beanName +
                             " in application " + application, ex);
            // TODO : Tr.error
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "findByBeanName: " + ejbex);

            throw ejbex;
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "home = " + home.getJ2EEName());

        // -----------------------------------------------------------------------
        // Second - determine the interface type, and create the return object
        // -----------------------------------------------------------------------

        BeanMetaData bmd = home.getBeanMetaData();

        // First, check against the home interfaces
        if (interfaceName.equals(bmd.localHomeInterfaceClassName))
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "match on local home");

            retObj = home.getWrapper().getLocalObject();
        }
        else if (interfaceName.equals(bmd.homeInterfaceClassName))
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "match on remote home");

            retObj = getRemoteHomeReference(home, interfaceName);
        }
        else
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "looking for match on business interface");

            // The only thing left are the business interfaces
            retObj = createBusinessObject(home, bmd, interfaceName, true);
        } // End of non-home annotation processing

        // Careful not to call toString or it may invoke the customer bean!
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "findByBeanName : " + retObj.getClass().getName());

        return retObj;
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
                    throws EJBException, RemoteException
    {
        if (application == null)
            throw new IllegalArgumentException("Application name not specified");
        if (interfaceName == null)
            throw new IllegalArgumentException("Interface name not specified");

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "findByInterface : App = " + application +
                         ", interface = " + interfaceName);

        Object retObj = null;
        EJSHome home = null;

        // -----------------------------------------------------------------------
        // First - find the bean home (EJSHome) based on what is known
        // -----------------------------------------------------------------------

        try
        {
            // A module was not explicitly specified, nor the bean name, so look
            // for the home in all of the modules within the same application.
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "bean name not specified, auto-link on " +
                             interfaceName);

            // A specific bean was not identified in config, so attempt
            // 'auto-link', where a bean home is located based on
            // whether the home or bean implements the interface
            // being injected.
            home = homeOfHomes.getHomeByInterface(application, null, interfaceName);
        } catch (Throwable ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".findByInterface",
                                        "182", this);
            EJBException ejbex = ExceptionUtil.EJBException
                            ("Failure locating interface " + interfaceName +
                             " in application " + application, ex);
            // TODO : Tr.error
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "findByInterface: " + ejbex);

            throw ejbex;
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "home = " + home.getJ2EEName());

        // -----------------------------------------------------------------------
        // Second - determine the interface type, and create the return object
        // -----------------------------------------------------------------------

        BeanMetaData bmd = home.getBeanMetaData();

        // First, check against the home interfaces
        if (interfaceName.equals(bmd.localHomeInterfaceClassName))
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "match on local home");

            retObj = home.getWrapper().getLocalObject();
        }
        else if (interfaceName.equals(bmd.homeInterfaceClassName))
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "match on remote home");

            retObj = getRemoteHomeReference(home, interfaceName);
        }
        else
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "looking for match on business interface");

            // The only thing left are the business interfaces
            retObj = createBusinessObject(home, bmd, interfaceName, false);
        } // End of non-home annotation processing

        // Careful not to call toString or it may invoke the customer bean!
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "findByInterface : " + retObj.getClass().getName());

        return retObj;
    }

    // --------------------------------------------------------------------------
    //
    // Internal implementation methods
    //
    // --------------------------------------------------------------------------

    /**
     * Encapsulates the common code and exception handling for creating an
     * instance of a Session bean for the specified business interface,
     * for lookup or injection. <p>
     *
     * An EJSWrapper instance will be returned for both local and remote
     * interfaces. A stub is returned for remote interfaces. <p>
     *
     * @param home the internal home object of the referenced ejb.
     * @param businessInterface the local or remote business interface
     *            to be injected.
     * @param ejblink true if ejb-link/beanName; false for auto-link
     *
     * @return the wrapper representing either the local or remote business
     *         interface to be injected.
     **/
    private Object createBusinessObject(EJSHome home,
                                        BeanMetaData bmd,
                                        String interfaceName,
                                        boolean ejblink)
                    throws EJBException, NoSuchObjectException
    {
        Object retObj = null;

        try
        {
            retObj = home.createBusinessObject(interfaceName, ejblink);
        } catch (Throwable ex)
        {
            // ClassNotFoundException, CreateException, and NoSuchObjectException
            // must be caught here... but basically all failures that may occur
            // while creating the bean instance must cause the injection to fail.
            FFDCFilter.processException(ex, CLASS_NAME + ".createBusinessObject",
                                        "281", this);
            EJBException ejbex = ExceptionUtil.EJBException
                            ("Failure creating instance of " + home.getJ2EEName() +
                             " of type " + interfaceName, ex);
            // TODO : Tr.error
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "createBusinessObject: " + ejbex);

            throw ejbex;
        }

        // This should never occur, but does cover the following 2 possibilites:
        // 1) one of the above factory calls returns null (unlikely)
        // 2) an attempt is being made to inject an MDB or 2.1 Entity (not supported)
        if (retObj == null)
        {
            EJBException ejbex = new EJBException
                            ("Unable to create instance of " + home.getJ2EEName() +
                             " of type " + interfaceName);
            // TODO : Tr.error
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "createBusinessObject: " + ejbex);

            throw ejbex;
        }

        return retObj;
    }

    /**
     * Encapsulates the common code and exception handling for obtaining
     * a remote home reference (Stub) for the specified home interface,
     * for lookup or injection. <p>
     *
     * @param home the internal home object of the referenced ejb.
     * @param interfaceName the remote home interface to be injected.
     **/
    private Object getRemoteHomeReference(EJSHome home, String interfaceName)
                    throws EJBException, NoSuchObjectException
    {
        Object retObj = null;

        try
        {
            checkHomeSupported(home, interfaceName);

            EJBRuntime runtime = home.getContainer().getEJBRuntime();
            runtime.checkRemoteSupported(home, interfaceName);

            EJSWrapper wrapper = home.getWrapper().getRemoteWrapper();
            retObj = runtime.getRemoteReference(wrapper);
        } catch (Throwable ex)
        {
            // ClassNotFoundException, CreateException, and NoSuchObjectException
            // must be caught here... but basically all failures that may occur
            // while creating the bean instance must cause the injection to fail.
            FFDCFilter.processException(ex, CLASS_NAME + ".getRemoteHomeReference",
                                        "281", this);
            EJBException ejbex = ExceptionUtil.EJBException
                            ("Failure obtaining remote home reference of " + home.getJ2EEName() +
                             " of type " + interfaceName, ex);
            // TODO : Tr.error
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getRemoteHomeReference: " + ejbex);

            throw ejbex;
        }

        // This should never occur, but does cover the following 2 possibilites:
        // 1) one of the above factory calls returns null (unlikely)
        // 2) an attempt is being made to inject an MDB or 2.1 Entity (not supported)
        if (retObj == null)
        {
            EJBException ejbex = new EJBException
                            ("Unable to obtain remote reference of " + home.getJ2EEName() +
                             " of type " + interfaceName);
            // TODO : Tr.error
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getRemoteHomeReference: " + ejbex);

            throw ejbex;
        }

        return retObj;
    }

    /**
     * Used to determine if looking up / injecting a home is a supported operation.
     *
     * @throws EJBNotFoundException thrown if looking up or injecting a home is unsupported
     */
    protected void checkHomeSupported(EJSHome home, String homeInterface)
                    throws EJBNotFoundException
    {
        // NO-OP - home access is supported by default
    }

}
