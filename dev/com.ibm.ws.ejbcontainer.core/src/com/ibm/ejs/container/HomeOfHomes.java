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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ejb.Handle;

import com.ibm.ejs.container.BeanOFactory.BeanOFactoryType;
import com.ibm.ejs.container.activator.ActivationStrategy;
import com.ibm.ejs.container.activator.Activator;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.ejs.util.FastHashtable;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.csi.EJBModuleMetaData;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.websphere.ejbcontainer.AmbiguousEJBReferenceException;
import com.ibm.websphere.ejbcontainer.EJBStoppedException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.diagnostics.IncidentStreamWriter;
import com.ibm.ws.ejbcontainer.diagnostics.IntrospectionWriter;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntime;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.IncidentStream;

/**
 * The <code>HomeOfHomes</code> provides the container-internal EJB home
 * for the homes of all the EJBs installed in a container. <p>
 */
public final class HomeOfHomes implements HomeInternal
{
    // --------------------------------------------------------------------------
    //
    // Constants
    //
    // --------------------------------------------------------------------------
    private static final String CLASS_NAME = "com.ibm.ejs.container.HomeOfHomes";

    public static final String HOME_OF_HOMES = "__homeOfHomes";

    private static final String EJB_FACTORY = "__EJBFactory";

    private final J2EENameFactory j2eeNameFactory;

    // --------------------------------------------------------------------------
    //
    // Construction
    //
    // --------------------------------------------------------------------------
    public HomeOfHomes(EJSContainer container, Activator activator)
    {
        this.container = container;
        this.activator = activator;
        beanOFactory = container.getEJBRuntime().getBeanOFactory(BeanOFactoryType.CM_STATELESS_BEANO_FACTORY, null);

        j2eeNameFactory = container.getJ2EENameFactory();

        homeOfHomesJ2EEName = j2eeNameFactory.create(HOME_OF_HOMES,
                                                     HOME_OF_HOMES,
                                                     HOME_OF_HOMES);
        J2EEName ejbFactoryJ2EEName = j2eeNameFactory.create(HOME_OF_HOMES,
                                                             HOME_OF_HOMES,
                                                             EJB_FACTORY);
        ivEJBFactoryHome = new EJBFactoryHome(container,
                                              this,
                                              ejbFactoryJ2EEName,
                                              j2eeNameFactory);
    }

    // --------------------------------------------------------------------------
    //
    // Operations
    //
    // --------------------------------------------------------------------------

    /**
     * Create a new EJSHome instance.
     */
    public EJSHome create(BeanMetaData beanMetaData)
                    throws RemoteException
    {
        J2EEName name = beanMetaData.j2eeName;
        HomeRecord hr = beanMetaData.homeRecord;

        StatelessBeanO homeBeanO = null;
        EJSHome result = null;
        try {
            result = (EJSHome) beanMetaData.homeBeanClass.newInstance();

            homeBeanO = (StatelessBeanO) beanOFactory.create(container, null, false);
            homeBeanO.setEnterpriseBean(result);
        } catch (Exception ex) {
            FFDCFilter.processException(ex, CLASS_NAME + ".create", "90", this);
            throw new InvalidEJBClassNameException("", ex);
        }

        homeBeanO.reentrant = true;
        hr.beanO = homeBeanO; //LIDB859-4

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "created new home bean", name);

        return result;
    } // create

    /**
     * Adds the bean/home to the HomeOfHomes and returns a new HomeRecord
     * instance. <p>
     *
     * This method is where the home is placed in the HomesByName hashtable,
     * as well as the ejb-link and auto-link tables. <p>
     */
    // LIDB859-4 d429866.2
    public void addHome(BeanMetaData bmd) // F743-26072
    throws RemoteException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "addHome : " + bmd.j2eeName);

        if (homesByName.get(bmd.j2eeName) != null) {
            throw new DuplicateHomeNameException(bmd.j2eeName.toString());
        }

        homesByName.put(bmd.j2eeName, bmd.homeRecord); // d366845.3

        J2EEName j2eeName = bmd.j2eeName;
        String application = j2eeName.getApplication();
        AppLinkData linkData;

        // F743-26072 - Use AppLinkData
        synchronized (ivAppLinkData)
        {
            linkData = ivAppLinkData.get(application);
            if (linkData == null)
            {
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.debug(tc, "adding application link data for " + application);

                linkData = new AppLinkData();
                ivAppLinkData.put(application, linkData);
            }
        }

        updateAppLinkData(linkData, true, j2eeName, bmd);

        // For version cable modules, the serialized BeanId will contain the
        // unversioned (or base) name, so a map is created from unversioned to
        // active to enable properly routing incoming requests.             F54184
        if (bmd._moduleMetaData.isVersionedModule())
        {
            EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;
            bmd.ivUnversionedJ2eeName = j2eeNameFactory.create(mmd.ivVersionedAppBaseName,
                                                               mmd.ivVersionedModuleBaseName,
                                                               j2eeName.getComponent());
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Versioned Mapping Added : " + bmd.ivUnversionedJ2eeName + " -> " + j2eeName);
            J2EEName dupBaseName = ivVersionedModuleNames.put(bmd.ivUnversionedJ2eeName, j2eeName);
            if (dupBaseName != null) // F54184.2
            {
                ivVersionedModuleNames.put(bmd.ivUnversionedJ2eeName, dupBaseName);
                throw new DuplicateHomeNameException("Base Name : " + bmd.ivUnversionedJ2eeName);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "addHome");
    }

    void setActivationStrategy(EJSHome result,
                               BeanMetaData beanMetaData)
                    throws RemoteException
    {
        int strategy;
        EJBRuntime runtime = container.getEJBRuntime();

        switch (beanMetaData.type) {

            case InternalConstants.TYPE_MESSAGE_DRIVEN: // 126512
                if (beanMetaData.usesBeanManagedTx) {
                    result.beanOFactory = runtime.getBeanOFactory(BeanOFactoryType.BM_MESSAGEDRIVEN_BEANO_FACTORY, beanMetaData);
                } else {
                    result.beanOFactory = runtime.getBeanOFactory(BeanOFactoryType.CM_MESSAGEDRIVEN_BEANO_FACTORY, beanMetaData);
                }
                strategy = Activator.UNCACHED_ACTIVATION_STRATEGY;
                break;

            case InternalConstants.TYPE_STATELESS_SESSION: // 126512
                if (beanMetaData.usesBeanManagedTx) {
                    result.beanOFactory = runtime.getBeanOFactory(BeanOFactoryType.BM_STATELESS_BEANO_FACTORY, beanMetaData);
                } else {
                    result.beanOFactory = runtime.getBeanOFactory(BeanOFactoryType.CM_STATELESS_BEANO_FACTORY, beanMetaData);
                }
                strategy = Activator.UNCACHED_ACTIVATION_STRATEGY;
                break;

            case InternalConstants.TYPE_STATEFUL_SESSION: // 126512
                if (beanMetaData.usesBeanManagedTx) {
                    result.beanOFactory = runtime.getBeanOFactory(BeanOFactoryType.BM_STATEFUL_BEANO_FACTORY, beanMetaData);
                } else {
                    result.beanOFactory = runtime.getBeanOFactory(BeanOFactoryType.CM_STATEFUL_BEANO_FACTORY, beanMetaData);
                }
                if (beanMetaData.sessionActivateTran) {
                    strategy = Activator.STATEFUL_ACTIVATE_TRAN_ACTIVATION_STRATEGY;
                } else if (beanMetaData.sessionActivateSession) { // LIDB441.5
                    strategy = Activator.STATEFUL_ACTIVATE_SESSION_ACTIVATION_STRATEGY;
                } else {
                    strategy = Activator.STATEFUL_ACTIVATE_ONCE_ACTIVATION_STRATEGY;
                }
                break;

            case InternalConstants.TYPE_BEAN_MANAGED_ENTITY: // 126512
                result.beanOFactory = runtime.getBeanOFactory(BeanOFactoryType.BEAN_MANAGED_BEANO_FACTORY, beanMetaData);
                if (beanMetaData.optionACommitOption) {
                    strategy = Activator.OPTA_ENTITY_ACTIVATION_STRATEGY;
                } else if (beanMetaData.optionBCommitOption) {
                    strategy = Activator.OPTB_ENTITY_ACTIVATION_STRATEGY;
                } else if (beanMetaData.entitySessionalTranOption) {
                    strategy = Activator.ENTITY_SESSIONAL_TRAN_ACTIVATION_STRATEGY; // LIDB441.5
                } else {
                    strategy = Activator.OPTC_ENTITY_ACTIVATION_STRATEGY;
                }
                break;

            case InternalConstants.TYPE_CONTAINER_MANAGED_ENTITY: // 126512
                // Select the correct beanO factory based on the CMP Version.
                // EJB 1.x uses persisters and EJB 2.x uses concrete beans. f110762.1
                if (beanMetaData.cmpVersion == 2)
                    result.beanOFactory = runtime.getBeanOFactory(BeanOFactoryType.CONTAINER_MANAGED_2_0_BEANO_FACTORY, beanMetaData);
                else
                    result.beanOFactory = runtime.getBeanOFactory(BeanOFactoryType.CONTAINER_MANAGED_BEANO_FACTORY, beanMetaData);

                if (beanMetaData.optionACommitOption) {
                    strategy = Activator.OPTA_ENTITY_ACTIVATION_STRATEGY;
                } else if (beanMetaData.optionBCommitOption) {
                    strategy = Activator.OPTB_ENTITY_ACTIVATION_STRATEGY;
                } else if (beanMetaData.entitySessionalTranOption) {
                    strategy = Activator.ENTITY_SESSIONAL_TRAN_ACTIVATION_STRATEGY; // LIDB441.5
                } else if (beanMetaData.ivReadOnlyCommitOption) { // LI3408
                    strategy = Activator.READONLY_ENTITY_ACTIVATION_STRATEGY;
                } else {
                    strategy = Activator.OPTC_ENTITY_ACTIVATION_STRATEGY;
                }
                break;

            case InternalConstants.TYPE_SINGLETON_SESSION: // F743-508 d565527
                result.beanOFactory = runtime.getBeanOFactory(BeanOFactoryType.SINGLETON_BEANO_FACTORY, beanMetaData);
                strategy = Activator.UNCACHED_ACTIVATION_STRATEGY;
                break;

            case InternalConstants.TYPE_MANAGED_BEAN: // F743-34301
                result.beanOFactory = runtime.getBeanOFactory(BeanOFactoryType.MANAGED_BEANO_FACTORY, beanMetaData);
                strategy = Activator.UNCACHED_ACTIVATION_STRATEGY;
                break;

            default:
                throw new ContainerInternalError();

        }

        // Ask the Activator for the appropriate activation strategy, and
        // set it in the home. If configured, the activator may have created a
        // separate EJB Cache for the home and return an activation strategy
        // corresponding to that EJB Cache.                              d129562
        result.activationStrategy = activator.getActivationStrategy(result, strategy);
    }

    public EJSHome removeHome(BeanMetaData bmd)
    {
        J2EEName name = bmd.getJ2EEName();
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "removeHome : " + name);

        if (bmd._moduleMetaData.isVersionedModule()) { // F54184
            ivVersionedModuleNames.remove(bmd.ivUnversionedJ2eeName);
        }

        boolean added = homesByName.remove(name) != null;
        if (added) // RTC100347
        {
            // F743-26072 - Use AppLinkData.
            synchronized (ivAppLinkData)
            {
                String application = name.getApplication();
                AppLinkData linkData = ivAppLinkData.get(application);

                updateAppLinkData(linkData, false, name, bmd);
                if (linkData.ivNumBeans == 0)
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "removing application link data for " + application);
                    ivAppLinkData.remove(application);
                }
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "removeHome");
        return (EJSHome) bmd.homeRecord.homeInternal; // RTC100347
    }

    /**
     * Return the Home associated with the j2eeName. <p>
     */
    public HomeInternal getHome(J2EEName name) //197121
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "getHome : " + name);

        HomeInternal result = null;

        // Name is the HomeOfHomes special name when referencing an
        // EJBHome; return the HomeOfHomes
        if (name.equals(homeOfHomesJ2EEName))
        {
            result = this;
        }

        // Name is the EJBFactory special name when referencing the
        // HomeOfHomes for EJB-Link or Auto-Link; return the
        // EJBFactoryHome.                                                 d440604
        else if (name.equals(ivEJBFactoryHome.ivJ2eeName))
        {
            result = ivEJBFactoryHome;
        }

        // Otherwise, the J2EEName is for a reference to an EJB
        // instance; return the EJBHome.
        else
        {
            HomeRecord hr = homesByName.get(name); // d366845.3
            if (hr != null) {
                result = hr.homeInternal;
                if (result == null) {
                    if (hr.bmd.ivDeferEJBInitialization) {
                        result = hr.getHomeAndInitialize(); // d648522
                    } else {
                        // Request for a non-deferred bean that hasn't quite finished
                        // initializing; return null since bean doesn't exist just yet.
                        // Caller will throw appropriate exception.            PM98090
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Non-deferred EJB not yet available : " + name);
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "getHome : " + result);
        return result;

    }

    /**
     * Returns the J2EEName for the specified bean name defined within the
     * specified application. The bean may be defined within any of the
     * application modules. <p>
     *
     * An EJBNotFoundException will be thrown if either a bean does not
     * exist by the requested name, or more than one bean exists by
     * the requested name. <p>
     *
     * The caller must be holding the monitor lock on <tt>linkData</tt>. <p>
     *
     * @param linkData the application link data.
     * @param application the name of the application.
     * @param beanName the name of the EJB, from xml or annotation.
     * @return the bean name, or null if the application or bean does not exist
     * @throws AmbiguousEJBReferenceException if the bean name is not unique
     *             within the application
     */
    private J2EEName findApplicationBean(AppLinkData linkData, String application, String beanName)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "findApplicationBean : " + application + ", " + beanName);

        J2EEName j2eeName;

        Set<J2EEName> beans = linkData.ivBeansByName.get(beanName);
        if (beans != null && !beans.isEmpty())
        {
            if (beans.size() != 1)
            {
                AmbiguousEJBReferenceException ex = new AmbiguousEJBReferenceException
                                ("The reference to bean " + beanName +
                                 " is ambiguous. Application " + application +
                                 " contains multiple beans with same name.");
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "findApplicationBean : " + ex);
                throw ex;
            }

            j2eeName = beans.iterator().next();
        }
        else
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.debug(tc, "No beans found");

            j2eeName = null;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "findApplicationBean : " + j2eeName);
        return j2eeName;
    }

    /**
     * Returns the Home associated with the specified bean name defined
     * within the specified application. The bean may be defined within
     * any of the application modules. <p>
     *
     * An EJBNotFoundException will be thrown if either a bean does not
     * exist by the requested name, or more than one bean exists by
     * the requested name. <p>
     *
     * @param application the name of the application.
     * @param beanName the name of the EJB, from xml or annotation.
     **/
    // d429866.1
    public EJSHome getHomeByName(String application, String beanName)
                    throws EJBNotFoundException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getHomeByName : " + application + ", " + beanName);

        AppLinkData linkData = getAppLinkData(application); // F743-26072
        if (linkData == null)
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "application " + application + " not found");
        }
        else
        {
            J2EEName j2eeName;
            synchronized (linkData)
            {
                j2eeName = findApplicationBean(linkData, application, beanName);
            }

            if (j2eeName != null)
            {
                EJSHome retHome = (EJSHome) getHome(j2eeName);

                if (retHome != null)
                {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "getHomeByName : " + retHome.getJ2EEName());
                    return retHome;
                }
            }
            else
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "EJB " + beanName + " not found");
            }
        }

        EJBNotFoundException ex =
                        new EJBNotFoundException("EJB named " + beanName +
                                                 " not present in application " +
                                                 application + ".");
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getHomeByName : " + ex);
        throw ex;
    }

    /**
     * Resolves an EJB link within the specified application and module. An EJB
     * link is specified by the JEE specification as follows:
     *
     * <p>The ejb-linkType is used by ejb-link elements in the ejb-ref or
     * ejb-local-ref elements to specify that an EJB reference is linked to
     * enterprise bean.
     *
     * <p>The value of the ejb-link element must be the ejb-name of an
     * enterprise bean in the same ejb-jar file or in another ejb-jar file in
     * the same Java EE application unit.
     *
     * <p>Alternatively, the name in the ejb-link element may be composed of a
     * path name specifying the ejb-jar containing the referenced enterprise
     * bean with the ejb-name of the target bean appended and separated from the
     * path name by "#". The path name is relative to the Deployment File
     * containing Deployment Component that is referencing the enterprise bean.
     * This allows multiple enterprise beans with the same ejb-name to be
     * uniquely identified.
     *
     * <p>Alternatively, the name in the ejb-link element may be composed of a
     * logical module name separated from the bean name by the "/".
     *
     * <p>Examples:
     *
     * <pre>
     * &lt;ejb-link&gt;EmployeeRecord&lt;/ejb-link&gt;
     * </pre>
     *
     * <pre>
     * &lt;ejb-link&gt;../products/product.jar#ProductEJB&lt;/ejb-link&gt;
     * </pre>
     *
     * <pre>
     * &lt;ejb-link&gt;Product#ProductEJB&lt;/ejb-link&gt;
     * </pre>
     *
     * @param application the application for the EJB link
     * @param module the module for the EJB link (module defining the ref),
     *            may be null for the client module
     * @param link the EJB link
     * @return the home record for the bean
     * @throws EJBNotFoundException if the bean is not found
     * @throws AmbiguousEJBReferenceException if the bean name is not unique
     *             within the application
     */
    public HomeRecord resolveEJBLink(String application,
                                     String module,
                                     String link)
                    throws EJBNotFoundException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resolveEJBLink: " + application + "#" + module + ", " + link);

        AppLinkData linkData = null;
        String origModule = module;
        String bean;

        // F743-25385
        // There are three styles that can be used when specifying the ejb-link data.
        //       Style 1: just the bean-component-name itself
        //                      For example:  TestBeanA
        //
        //       Style 2: the <relative physical path of the module containing the bean>#<bean-component-name>
        //                      For example: <blah>/<blah>/myModule.jar#TestBeanA
        //
        //       Style 3: the <logical name of the module containing the bean>/<bean-component-name>
        //                      For example: testModule/TestBeanA
        //
        // The point here is to correctly extract the moduleName and beanName from
        // the specified ejb-link data.  Once we have determined which of the three
        // styles the data is using, we then extract the moduleName/beanName using
        // rules specific to that style.

        int bidx = link.indexOf('#');
        if (bidx > -1)
        {
            // F743-25385
            // There is a '#' character, so this means we are dealing with style #2.
            //
            // For style #2, we have these rules:
            //       a) moduleName is the part between the last '/' character
            //          and the '#' character
            //
            //       b) beanName is everything after the '#' character
            int midx = link.lastIndexOf('/');
            if (midx > -1 && midx < bidx)
            {
                module = link.substring(midx + 1, bidx);
            }
            else
            {
                // If no path was specified, then the referenced module
                // is in the same location.                            d455591
                module = link.substring(0, bidx);
            }

            bean = link.substring(bidx + 1);

            // Verify that the physical module file specified using the style #2 syntax
            // is actually a valid location for a bean.
            if (!module.endsWith(".jar") && !module.endsWith(".war") && !module.equals(origModule))
            {
                throw new EJBNotFoundException("Incorrect usage of beanName/ejb-link syntax. " +
                                               "The syntax used requires the physical module file " +
                                               "to be specified, and the module must end in .jar " +
                                               "or .war to be a valid location for a bean. " +
                                               "The beanName/ejb-link data specified bean " + bean +
                                               " and module " + module + ".");
            }

            if (isTraceOn && tc.isDebugEnabled())
            {
                Tr.debug(tc, "Used ejb-link style 2 to get module name " + module + " and beanName " + bean);
            }
        }
        else
        {
            // F743-25385
            // There is no '#' character, so we are not dealing with style #2.
            // Determine if we are dealing with style #1 or #3.
            //
            // We do a 'lastIndexOf' instead of just an 'indexOf' to account for
            // the potential case where the user has a '/' character in the logical
            // name of the module.  (If thats not possible, then we should probably
            // verify they only have one '/' character, and error if there are
            // multiple).
            int forwardSlashIndex = link.lastIndexOf('/');
            if (forwardSlashIndex > -1)
            {
                // There is a forward slash, so that means we have style #3.
                //
                // For style #3, we have these rules:
                //      a) moduleName is the everything before the last '/' character
                //
                //      b) beanName is everything after the last '/' character
                String logicalModule = link.substring(0, forwardSlashIndex);
                bean = link.substring(forwardSlashIndex + 1);

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Used ejb-link style 3 to get module name " + logicalModule + " and beanName " + bean);

                // We are using style #3 syntax, and so the user specified the logical
                // name of the module file (eg, MyModule).  However, the bean is
                // associated with a J2EEName that was created using the physical module
                // name, and so we need to map the logical module that was specified
                // to its physical module, so we can then use the physical (and real)
                // J2EEName to locate the bean.
                //
                // To support the scenario where we have two bean components that have
                // the same beanName, and one is packaged in an ejbjar and the other
                // is packaged in a war, and these two physical modules are assigned
                // the same logical name, we split the logical-to-physical mappings
                // into a 'ejbjar' and 'war' groups, and we search the 'ejbjar' group
                // first, and only search the 'war' group if needed.

                // F743-26072 - Use AppLinkData to map logical-to-physical.
                linkData = getAppLinkData(application);
                module = null;

                if (linkData != null)
                {
                    synchronized (linkData)
                    {
                        Set<String> modules = linkData.ivModulesByLogicalName.get(logicalModule);
                        if (modules != null)
                        {
                            if (modules.size() == 1)
                            {
                                module = modules.iterator().next();
                            }
                            else if (!modules.isEmpty())
                            {
                                throw new AmbiguousEJBReferenceException("The reference to bean " + bean +
                                                                         " in the " + logicalModule +
                                                                         " logical module is ambiguous. Application " + application +
                                                                         " contains multiple modules with same logical name.");
                            }
                        }
                    }
                }

                if (module == null)
                {
                    throw new EJBNotFoundException("The reference to the " + bean +
                                                   " bean in the " + logicalModule +
                                                   " logical module in the " + application +
                                                   " application did not map to any bean component.");
                }
            }
            else
            {
                // There is no forward slash either, so we must be dealing with style #1.
                //
                // For style #1, we have these rules:
                //      a) trust the passed in module name
                //      b) use the passed in 'link' value as the beanName
                bean = link;

                if (isTraceOn && tc.isDebugEnabled())
                {
                    Tr.debug(tc, "Used ejb-link style 1 to get module name " + module + " and beanName " + bean);
                }
            }
        }

        J2EEName j2eeName = null;
        HomeRecord hr = null;

        // The module name will be set if the ejb-ref was defined in an EJB or
        // WAR module (not client), or a module name was specified as part of
        // the beanName/ejb-link. If a module name is available, first attempt
        // to locate the bean directly using the J2EEName.                 d657052

        if (module != null)
        {
            j2eeName = j2eeNameFactory.create(application, module, bean);
            hr = getHomeRecord(j2eeName);
        }

        if (hr == null)
        {
            // If the bean was not found in the same module, and a module
            // was not explicitly specified, then look for it in the other
            // modules within the same application.                    d429866.1
            if (module == origModule)
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "looking for home in other modules...");

                // F743-26072 - Use AppLinkData to find the bean.
                if (linkData == null)
                {
                    linkData = getAppLinkData(application);
                }

                if (linkData != null)
                {
                    synchronized (linkData)
                    {
                        j2eeName = findApplicationBean(linkData, application, bean);
                    }
                }

                if (j2eeName != null)
                {
                    hr = getHomeRecord(j2eeName);
                }
                else
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "EJB " + bean + " not found");
                }
            }

            if (hr == null)
            {
                EJBNotFoundException ex =
                                new EJBNotFoundException("EJB named " + bean +
                                                         " not present in application " +
                                                         application + ".");
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "resolveEJBLink: " + ex);
                throw ex;
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resolveEJBLink: " + hr);
        return hr;
    }

    /**
     * Returns the Home for the EJB that implements the specified component
     * home or business interface defined within the specified application.
     * The bean may be defined within any of the application modules, but
     * if a module name is provided, that module will be searched first. <p>
     *
     * An EJBNotFoundException will be thrown if either a bean does not
     * exist that implements the requested interface, or more than one bean
     * exists with the specified interface. <p>
     *
     * @param application the name of the application.
     * @parm module the module of the referencing bean (EJB module);
     *       null if client container or web container.
     * @param beanInterface the component or business interface of the EJB.
     **/
    // d429866.1
    public EJSHome getHomeByInterface(String application,
                                      String module,
                                      String beanInterface)
                    throws EJBNotFoundException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getHomeByInterface : " + application + ", " +
                         module + ", " + beanInterface);

        J2EEName j2eeName = null;
        AppLinkData linkData = getAppLinkData(application); // F743-26072
        if (linkData != null)
        {
            // If a module was specified, then look for a bean that implements
            // the interface in the module table first, as there is a better
            // chance of finding a single match, and this is consistent with
            // ejb-link behavior.                                              d446993
            if (module != null)
            {
                synchronized (linkData)
                {
                    Map<String, Set<J2EEName>> moduleTable = linkData.ivBeansByModuleByType.get(module);

                    if (moduleTable != null)
                    {
                        Set<J2EEName> beans = moduleTable.get(beanInterface);

                        if (beans != null && !beans.isEmpty())
                        {
                            if (beans.size() != 1)
                            {
                                AmbiguousEJBReferenceException ex = new AmbiguousEJBReferenceException
                                                ("The reference to bean interface " + beanInterface +
                                                 " is ambiguous. Module " + application +
                                                 "/" + module +
                                                 " contains multiple beans with the same interface.");
                                if (isTraceOn && tc.isEntryEnabled())
                                    Tr.exit(tc, "getHomeByInterface : " + ex);
                                throw ex;
                            }

                            j2eeName = beans.iterator().next();

                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "Mapped beanInterface " + beanInterface + " to j2eeName " + j2eeName + " in current module.");
                        }
                        else
                        {
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "EJB with interface " + beanInterface +
                                             " not found in module " + module);
                        }
                    }
                }

                // If a bean was found, attempt to get the home outside of the linkData
                // sync block to avoid a deadlock if the bean hasn't completed starting.
                if (j2eeName != null)
                {
                    EJSHome retHome = (EJSHome) getHome(j2eeName);

                    if (retHome != null)
                    {
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(tc, "getHomeByInterface : " + retHome.getJ2EEName());
                        return retHome;
                    }
                    j2eeName = null;
                }
            }

            synchronized (linkData)
            {
                Set<J2EEName> beans = linkData.ivBeansByType.get(beanInterface);
                if (beans != null && !beans.isEmpty())
                {
                    if (beans.size() != 1)
                    {
                        AmbiguousEJBReferenceException ex = new AmbiguousEJBReferenceException
                                        ("The reference to bean interface " + beanInterface +
                                         " is ambiguous. Application " + application +
                                         " contains multiple beans with the same interface.");
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(tc, "getHomeByInterface : " + ex);
                        throw ex;
                    }

                    j2eeName = beans.iterator().next();
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Mapped beanInterface " + beanInterface + " to j2eeName " + j2eeName + " in another module in the app.");
                }
                else
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "EJB with interface " + beanInterface + " not found");
                }
            }

            // If a bean was found, attempt to get the home outside of the linkData
            // sync block to avoid a deadlock if the bean hasn't completed starting.
            if (j2eeName != null)
            {
                EJSHome retHome = (EJSHome) getHome(j2eeName);

                if (retHome != null)
                {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "getHomeByInterface : " + retHome.getJ2EEName());
                    return retHome;
                }
            }
        }
        else
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Applicaton " + application + " not found");
        }

        EJBNotFoundException ex =
                        new EJBNotFoundException("EJB with interface " + beanInterface +
                                                 " not present in application " +
                                                 application + ".");
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getHomeByInterface : " + ex);
        throw ex;
    }

    /**
     * Updates the EJB-link and auto-link data for a bean.
     *
     * @param linkData
     * @param add <tt>true</tt> if data for the bean should be added, or
     *            <tt>false</tt> if data should be removed
     * @param bmd
     */
    private void updateAppLinkData(AppLinkData linkData, boolean add, J2EEName j2eeName, BeanMetaData bmd) // F743-26072
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "updateAppLinkData: " + j2eeName + ", add=" + add);

        int numBeans;
        synchronized (linkData)
        {
            linkData.ivNumBeans += add ? 1 : -1;
            numBeans = linkData.ivNumBeans;

            // Update the table of bean names for this application, in support
            // of ejb-link.                                                  d429866.1
            updateAppLinkDataTable(linkData.ivBeansByName, add, j2eeName.getComponent(), j2eeName, "ivBeansByName");

            // Update the table of bean interfaces for this application, in support
            // of auto-link.                                                 d429866.2
            updateAutoLink(linkData, add, j2eeName, bmd);

            // F743-25385CodRv d648723
            // Update the table of logical-to-physical module names, in support of
            // ejb-link.
            // F743-26072 - This will redundantly add the same entry for every
            // bean in the module, but it will remove the entry for the first bean
            // to be removed.  In other words, we do not support removing a single
            // bean from a module.
            updateAppLinkDataTable(linkData.ivModulesByLogicalName, add,
                                   bmd._moduleMetaData.ivLogicalName, j2eeName.getModule(), "ivModulesByLogicalName");
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "updateAppLinkData: " + j2eeName + ", add=" + add + ", numBeans=" + numBeans);
    }

    /**
     * Gets the EJB-link and auto-link data for the specified application. The
     * caller must synchronize on the result object before attempting to use any
     * data it contains.
     *
     * @param application the application name
     * @return the link data, or <tt>null</tt> if none exists for the
     *         specified application
     */
    private AppLinkData getAppLinkData(String application)
    {
        synchronized (ivAppLinkData)
        {
            return ivAppLinkData.get(application);
        }
    }

    /**
     * Updates a map from name to set of values.
     *
     * @param table the table to update
     * @param add <tt>true</tt> if data should be added, or <tt>false</tt> if
     *            data should be removed
     * @param key the key to add or remove
     * @param value the value to add
     */
    private static <T> void updateAppLinkDataTable(Map<String, Set<T>> table,
                                                   boolean add,
                                                   String key,
                                                   T value,
                                                   String tracePrefix) // F743-26072
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        Set<T> values = table.get(key);
        if (add)
        {
            if (isTraceOn && tc.isDebugEnabled() && tracePrefix != null)
                Tr.debug(tc, tracePrefix + ": adding " + key + " = " + value);

            if (values == null)
            {
                values = new LinkedHashSet<T>();
                table.put(key, values);
            }

            values.add(value);
        }
        else
        {
            if (isTraceOn && tc.isDebugEnabled() && tracePrefix != null)
                Tr.debug(tc, tracePrefix + ": removing " + key + " = " + value);

            if (values != null) // d657052
            {
                values.remove(value);
                if (values.size() == 0) // d657052
                {
                    if (isTraceOn && tc.isDebugEnabled() && tracePrefix != null)
                        Tr.debug(tc, tracePrefix + ": removing " + key);
                    table.remove(key);
                }
            }
            else
            {
                if (isTraceOn && tc.isDebugEnabled() && tracePrefix != null)
                    Tr.debug(tc, tracePrefix + ": key not found: " + key);
            }
        }
    }

    /**
     * Internal method that has the logic necessary to maintain a
     * hashtable of all bean interfaces in an application to J2EEName
     * and for each module within an application for auto-link resolution. <p>
     *
     * @param linkData the application link data
     * @param add <tt>true</tt> if data for the bean should be added, or
     *            <tt>false</tt> if data should be removed
     * @param bmd BeanMetaData for the bean to be added to the table.
     **/
    // d429866.2
    private void updateAutoLink(AppLinkData linkData, boolean add, J2EEName j2eeName, BeanMetaData bmd)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "updateAutoLink");

        String module = j2eeName.getModule();
        String beanInterface = null;

        // -----------------------------------------------------------------------
        // Locate the table of interfaces for the module and application   d446993
        // -----------------------------------------------------------------------

        Map<String, Set<J2EEName>> appTable = linkData.ivBeansByType;
        Map<String, Set<J2EEName>> moduleTable = linkData.ivBeansByModuleByType.get(module);

        if (moduleTable == null)
        {
            moduleTable = new HashMap<String, Set<J2EEName>>(7);
            linkData.ivBeansByModuleByType.put(module, moduleTable);
        }

        // -----------------------------------------------------------------------
        // Add all of the interfaces of the EJB which may be injected/looked up
        // -----------------------------------------------------------------------

        beanInterface = bmd.homeInterfaceClassName;
        if (beanInterface != null)
        {
            updateAppLinkDataTable(appTable, add, beanInterface, j2eeName, "ivBeansByType");
            updateAppLinkDataTable(moduleTable, add, beanInterface, j2eeName, null);
        }

        beanInterface = bmd.localHomeInterfaceClassName;
        if (beanInterface != null)
        {
            updateAppLinkDataTable(appTable, add, beanInterface, j2eeName, "ivBeansByType");
            updateAppLinkDataTable(moduleTable, add, beanInterface, j2eeName, null);
        }

        String[] businessRemoteInterfaceName = bmd.ivBusinessRemoteInterfaceClassNames;
        if (businessRemoteInterfaceName != null)
        {
            for (String remoteInterface : businessRemoteInterfaceName)
            {
                updateAppLinkDataTable(appTable, add, remoteInterface, j2eeName, "ivBeansByType");
                updateAppLinkDataTable(moduleTable, add, remoteInterface, j2eeName, null);
            }
        }

        String[] businessLocalInterfaceName = bmd.ivBusinessLocalInterfaceClassNames;
        if (businessLocalInterfaceName != null)
        {
            for (String localInterface : businessLocalInterfaceName)
            {
                updateAppLinkDataTable(appTable, add, localInterface, j2eeName, "ivBeansByType");
                updateAppLinkDataTable(moduleTable, add, localInterface, j2eeName, null);
            }
        }

        // F743-1756 - Also add No-Interface View to maps for injection/lookup
        if (bmd.ivLocalBean)
        {
            beanInterface = bmd.enterpriseBeanClassName;
            updateAppLinkDataTable(appTable, add, beanInterface, j2eeName, "ivBeansByType");
            updateAppLinkDataTable(moduleTable, add, beanInterface, j2eeName, null);
        }

        // F743-26072 - If this was the last bean in the module type table, then
        // remove the module type table.
        if (moduleTable.isEmpty())
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "ivBeansByModuleByType: removing " + module);
            linkData.ivBeansByModuleByType.remove(module);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "updateAutoLink");
    }

    /**
     * Get the Java EE name of the EJBFactory. <p>
     **/
    // d440604
    public J2EEName getEJBFactoryJ2EEName()
    {
        return ivEJBFactoryHome.ivJ2eeName;
    }

    /**
     * Returns the active versioned name for the specified unversioned
     * name. <p>
     *
     * @param unversionedName bean name with the unversioned (or base)
     *            module name.
     * @return the currently active bean name with the versioned (or actual)
     *         module name.
     */
    // F54184
    J2EEName getVersionedJ2EEName(J2EEName unversionedName)
    {
        // Note: Return the unversioned name if not found in the map; this
        //       supports the module no longer being versioned.
        J2EEName versionedName = ivVersionedModuleNames.get(unversionedName);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Versioned J2EEName : " + unversionedName + " -> " + versionedName);
        return (versionedName != null) ? versionedName : unversionedName;
    }

    // --------------------------------------------------------------------------
    //
    // HomeInternal interface implementation
    //
    // --------------------------------------------------------------------------

    /**
     * Get the JNDI name of this home. <p>
     */
    @Override
    public String getJNDIName(Object homeKey)
    {
        HomeRecord hr = homesByName.get(homeKey); // d366845.3
        return hr.homeInternal.getJNDIName(homeKey);
    }

    /**
     * Get the Java EE name of this home. <p>
     */
    @Override
    public J2EEName getJ2EEName()
    {
        return homeOfHomesJ2EEName;
    }

    /**
     * Get the id of the home bean. <p>
     */
    @Override
    public BeanId getId()
    {
        Tr.error(tc, "UNEXPECTED_METHOD_CALL_CNTR0074E", "HomeOfHomes.getId()"); // 135803
        return null;
    }

    @Override
    public final EJSWrapperCommon getWrapper() // f111627
    {
        Tr.error(tc, "UNEXPECTED_METHOD_CALL_CNTR0074E",
                 "HomeOfHomes.getWrapper()"); // 135803
        return null;
    }

    /**
     * Get a wrapper for the given BeanId <p>
     */
    @Override
    public final EJSWrapperCommon getWrapper(BeanId id) // f111627
    throws CSIException, RemoteException
    {
        return container.getWrapper(id);
    }

    /**
     * This method creates and returns a new <code>BeanO</code> instance
     * appropriate for this home. <p>
     *
     * The returned <code>BeanO</code> has a newly created enterprise
     * bean instance associated with it, and the enterprise bean instance
     * has had its set...Context() method called on it to set its context
     * to the returned <code>BeanO</code>. <p>
     *
     * This method must only be called when a new <code>BeanO</code>
     * instance is needed. It always creates a new <code>BeanO</code>
     * instance and a new instance of the associated enterprise bean. <p>
     *
     * @param threadData the <code>EJBThreadData</code> associated with the
     *            currently running thread <p>
     * @param tx the <code>ContainerTx</code> to associate with the newly
     *            created <code>BeanO</code> <p>
     * @param id the <code>BeanId</code> to associate with the newly
     *            created <code>BeanO</code> <p>
     *
     * @return newly created <code>BeanO</code> associated with a newly
     *         created bean instance of type of beans managed by this
     *         home <p>
     */
    // Added ContainerTx d168509
    @Override
    public BeanO createBeanO(EJBThreadData threadData, ContainerTx tx, BeanId id)
                    throws RemoteException
    {
        J2EEName homeKey = id.getJ2EEName(); // d366845.3
        HomeRecord hr = homesByName.get(homeKey); // d366845.3

        BeanO result = null; // d199071
        if (hr != null) { // d199071
            result = hr.beanO; // d199071
        }

        if (result == null)
        {
            // This will only ever occur when an attempt is being made to
            // invoke a method on a home wrapper, where the home has been
            // uninstalled since the wrapper was obtained. Doing nothing
            // would result in an NPE, so instead, a meaningful exception
            // should be reported.                                          d547849
            // If HR exists, but no beanO, then the application was started
            // again, but the bean hasn't started yet, still an error.      d716824
            String msgTxt =
                            "The referenced version of the " + homeKey.getComponent() +
                                            " bean in the " + homeKey.getApplication() +
                                            " application has been stopped and may no longer be used. " +
                                            "If the " + homeKey.getApplication() +
                                            " application has been started again, a new reference for " +
                                            "the new image of the " + homeKey.getComponent() +
                                            " bean must be obtained. Local references to a bean or home " +
                                            "are no longer valid once the application has been stopped.";

            throw new EJBStoppedException(msgTxt);
        }

        return result;
    }

    /**
     * Create wrapper instance of the type of wrappers managed by this
     * home. <p>
     *
     * This method provides a wrapper factory capability.
     *
     * @param id The <code>BeanId</code> to associate with the wrapper
     *
     * @return <code>EJSWrapper</code> instance whose most specific
     *         type is the type of wrappers managed by this home <p>
     */
    @Override
    public EJSWrapperCommon internalCreateWrapper(BeanId id) // f111627
    throws javax.ejb.CreateException, RemoteException, CSIException
    {
        J2EEName homeKey = (J2EEName) id.getPrimaryKey();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Retrieving home record : " + homeKey);
        }

        EJSHome home = (EJSHome) getHome(homeKey); // d648522

        // If the home was not found, then the application has either not
        // been installed or started, or possibly failed to start.
        // Log a meaningful warning, and throw a meaningful exception.   d356676.1
        if (home == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Unknown home", id);
            Tr.warning(tc, "HOME_NOT_FOUND_CNTR0092W", homeKey.toString());
            Exception ex = new EJBNotFoundException(homeKey.toString() + ".");
            throw new InvalidBeanIdException(ex);
        }

        EJSWrapperCommon wrappers; // d243336

        // For EJSHomes (Stateless Session beans), the wrappers for home
        // instances are singletons, and so that singleton is cached on the
        // home, to avoid Wrapper Cache lookups.  However, the wrappers are
        // still held in the wrapper cache to insure the remote wrapper is
        // registered with the ORB properly. If the wrappers are ever
        // evicted from the Wrapper Cache, then this method will be called to
        // fault it back in, and may just return that singleton.         d196581.1
        if (home.ivHomeWrappers != null) // d243336
        {
            wrappers = home.ivHomeWrappers; // d243336
        }
        else
        {
            BeanMetaData bmd = home.beanMetaData; // d648522
            wrappers = new EJSWrapperCommon
                            (bmd.homeRemoteImplClass, // f111627
                            bmd.homeLocalImplClass, // f111627
                            id, // f111627
                            bmd, // f111627
                            home.pmiBean, // d140003.33
                            container, // f111627
                            container.getWrapperManager(), // f111627
                            true); // f111627
        }

        return wrappers;
    }

    /**
     * Return true if this home contains singleton session bean. <p>
     */
    @Override
    public boolean isSingletonSessionHome() //d565527
    {
        return false;
    }

    /**
     * Return true if this home contains stateless session beans. <p>
     */
    @Override
    public final boolean isStatelessSessionHome()
    {
        return true;
    }

    /**
     * Return true if this home contains Message Driven beans. <p>
     */
    @Override
    public final boolean isMessageDrivenHome() // d121554
    {
        return false;
    }

    /**
     * Return true if this home contains stateful session beans. <p>
     */
    @Override
    public final boolean isStatefulSessionHome()
    {
        return false;
    }

    /**
     * Return the BeanMetaData object for beans associated with this
     * home. <p>
     */
    @Override
    public BeanMetaData getBeanMetaData(Object homeKey)
    {
        HomeRecord hr = homesByName.get(homeKey); // d366845.3
        return hr.bmd;
    }

    /**
     * Return the ClassLoader associated with this home.<p>
     */
    @Override
    public ClassLoader getClassLoader()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Return the ActivationStrategy for beans associated with this
     * home. <p>
     */
    @Override
    public final ActivationStrategy getActivationStrategy()
    {
        return activationStrategy;
    }

    /**
     * Return the method name for the given method id.
     */
    @Override
    public final String getMethodName(Object homeKey, int id, boolean isHome)
    {
        HomeRecord hr = homesByName.get(homeKey); // d366845.3
        return hr.homeInternal.getMethodName(homeKey, id, true);
    }

    /**
     * Return the name of the class that implements the bean's owned
     * by the given home.
     */
    @Override
    public String getEnterpriseBeanClassName(Object homeKey)
    {
        HomeRecord hr = homesByName.get(homeKey); // d366845.3
        return hr.homeInternal.getEnterpriseBeanClassName(homeKey);
    } // getEnterpriseBeanClassName

    /**
     * Create a Handle for the given BeanId
     */
    @Override
    public final Handle createHandle(BeanId id)
    {
        // Moscone draft 3 introduces the concept of a HomeHandle.
        // Currently, we do not support this, so there should be
        // no attempts to obtain a handle for a bean implementing
        // a home

        throw new UnsupportedOperationException();
    }

    /**
     * Set the activation strategy for the home of homes. <p>
     */
    public void setActivationStrategy(ActivationStrategy a)
    {
        if (activationStrategy != null) {
            throw new IllegalStateException();
        }
        activationStrategy = a;
    } // setActivationStrategy

    /**
     * Returns the HomeRecord associated with the J2EEName. <p>
     *
     * @param name The <code>J2EEName</code> to associate with the HomeRecord.
     *
     * @return <code>HomeRecord</code> instance associated with the J2EEName.
     *
     */
    public HomeRecord getHomeRecord(J2EEName name) { //LIDB859-4
        HomeRecord hr = homesByName.get(name); // d366845.3
        return hr;
    }

    /**
     * Returns a list of all HomeRecords currently registered with HomeOfHomes.
     */
    public List<HomeRecord> getAllHomeRecords() {
        return Collections.list(homesByName.elements());
    }

    public void ffdcDump(IncidentStream is) // d726844.1
    {
        introspect(new IncidentStreamWriter(is));
    }

    /**
     * Writes the important state data of this class, in a readable format,
     * to the specified output writer. <p>
     *
     * @param writer output resource for the introspection data
     */
    // F86406
    public void introspect(IntrospectionWriter writer)
    {
        writer.begin("HomeOfHomes Dump ---> " + this.toString());

        writer.begin("homesByName keys : ");
        List<String> j2eeNameStrings = new ArrayList<String>(); // 619922.1
        for (Enumeration<HomeRecord> en = homesByName.elements(); en.hasMoreElements();)
        {
            HomeRecord hr = en.nextElement();
            j2eeNameStrings.add(hr.getJ2EEName().toString());
        }

        Collections.sort(j2eeNameStrings);
        for (String j2eeNameString : j2eeNameStrings)
        {
            writer.println(j2eeNameString);
        }
        writer.end();

        if (ivVersionedModuleNames.size() > 0)
        {
            writer.begin("versioned module names : ");
            for (Map.Entry<J2EEName, J2EEName> entry : ivVersionedModuleNames.entrySet())
            {
                writer.println(entry.getKey() + " --> " + entry.getValue());
            }
            writer.end();
        }
    }

    // --------------------------------------------------------------------------
    //
    // Data
    //
    // --------------------------------------------------------------------------

    /**
     * The container for which we are the home of homes
     */
    private final EJSContainer container;
    private final Activator activator;
    private final J2EEName homeOfHomesJ2EEName;
    EJBFactoryHome ivEJBFactoryHome; // Home of HomeOfHomes     d440604, d639148
    private static int fastHashTableSize = 2053;//d140003.7

    /**
     * The home table maps the Java EE name of the bean installed
     * in this container to its corresponding HomeRecord (see below).
     */
    private final FastHashtable<J2EEName, HomeRecord> homesByName = new FastHashtable<J2EEName, HomeRecord>(fastHashTableSize);//d140003.7 // d366845.3

    /**
     * A map of application name to the EJB-link and auto-link data for that
     * application. Access to this table must be synchronized by holding the
     * monitor lock on this object.
     */
    private final Map<String, AppLinkData> ivAppLinkData = new HashMap<String, AppLinkData>(); // F743-26072

    /**
     * This structure holds data to perform EJB-link and auto-link for an
     * application. Access to this data must be synchronized by holding the
     * monitor lock on the lock.
     */
    static class AppLinkData // F743-26072
    {
        /**
         * The number of beans in this application data.
         */
        int ivNumBeans;

        /**
         * The bean name table used to resolve ejb-links. Map of bean name to
         * beans in all modules in the application.
         */
        // d429866.1
        Map<String, Set<J2EEName>> ivBeansByName = new HashMap<String, Set<J2EEName>>();

        /**
         * The bean interface table used to resolve auto-links. Map of interface
         * class names to the set of beans in all modules in the application that
         * support that home or business interface.
         */
        // d429866.2
        Map<String, Set<J2EEName>> ivBeansByType = new HashMap<String, Set<J2EEName>>();

        /**
         * The bean interface table per module used to resolve auto-links. Map
         * of module name to a map of interface class names to the set of beans
         * in that module that support that home or business interface.
         */
        // d446993
        Map<String, Map<String, Set<J2EEName>>> ivBeansByModuleByType =
                        new HashMap<String, Map<String, Set<J2EEName>>>();

        // F743-25385CodRv
        /**
         * Maps logical module names to physical module names.
         *
         * This mapping is needed because a module has both a physical name
         * (ie, A.jar or B.war) and a logical name (which is optionally specified via
         * the <module-name> element in the web or ejb deployment descriptor file,
         * or defaulted to the physical name of the module minus the extension.)
         * For example, a physical module name might be 'test.jar'. The default
         * module name would be 'test', but the user might override it via the
         * deployment descriptor to be 'MyModule'.
         *
         * The syntax for ejb-link supports specifying both a physical module name
         * (eg, test.jar) or the logical module name (eg, MyModule), and we have to
         * make both of these formats ultimately resolve to the J2EEName that is
         * associated with the HomeRecord for the desired bean component. The
         * J2EEName associated with the HomeRecord is always built using the
         * physical module name.
         *
         * When the user specifies the physical module name, then we don't need
         * to do any mapping, because the module name already matches the one
         * used in the actual J2EEName.
         *
         * However, when the user specifies the logical module, then we need
         * to do a mapping, because the logical module name will almost never
         * match the physical module name, and thus it won't match the module
         * name portion of the actual J2EEName. So, we need to map the
         * module name to the physical/real name, and then we can use that to
         * locate the bean.
         */
        Map<String, Set<String>> ivModulesByLogicalName = new HashMap<String, Set<String>>();
    }

    /**
     * The beanOFactory for home beanOs.
     */
    private final BeanOFactory beanOFactory;

    /**
     * Trace context
     */
    //d121558
    private final static TraceComponent tc =
                    Tr.register(HomeOfHomes.class, "EJBContainer",
                                "com.ibm.ejs.container.container");

    /**
     * Activation strategy for this home, must always be appropriate for
     * stateless session beans.
     */
    private ActivationStrategy activationStrategy;

    /**
     * Map of a beans unversioned (or base) name to the active version name. <p>
     *
     * An entry will exist for every bean in a version capable module.
     * The unversioned name uses the base module name specified by {@link EJBModuleMetaData#setVersionedModuleBaseName}.
     */
    // F54184
    private final Map<J2EEName, J2EEName> ivVersionedModuleNames = new ConcurrentHashMap<J2EEName, J2EEName>();
}
