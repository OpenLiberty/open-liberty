/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.factory;

import static com.ibm.ws.ejbcontainer.injection.factory.EJBLinkInfoRefAddr.ADDR_TYPE;

import java.rmi.Remote;
import java.util.Hashtable;

import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBNotFoundException;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.container.EJSWrapper;
import com.ibm.ejs.container.HomeOfHomes;
import com.ibm.ejs.container.HomeRecord;
import com.ibm.websphere.ejbcontainer.AmbiguousEJBReferenceException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntime;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * EJB-Link Resolver Factory; including auto-link resolution. <p>
 *
 * This class is used as an object factory that returns an EJB component home
 * or business interface wrapper (local) or stub (remote). <p>
 *
 * This factory is used when injection occurs or when a component performs
 * a lookup in the java:comp name space for EJB references with no binding
 * override.
 */
public class EJBLinkObjectFactory implements ObjectFactory
{
    private static final TraceComponent tc = Tr.register(EJBLinkObjectFactory.class, "EJBContainer", "com.ibm.ejs.container.container");

    @Override
    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment)
                    throws Exception
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance : " + obj);

        Object retObj = null;

        // -----------------------------------------------------------------------
        // Is obj a Reference?
        // -----------------------------------------------------------------------
        if (!(obj instanceof Reference))
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : null (non-Reference)");
            return null;
        }

        Reference ref = (Reference) obj;

        // -----------------------------------------------------------------------
        // Is the right factory for this reference?
        // -----------------------------------------------------------------------
        if (!getClass().getName().equals(ref.getFactoryClassName())) // F93680
        {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : null (wrong factory class: " + ref.getFactoryClassName() + ")");
            return null;
        }

        // -----------------------------------------------------------------------
        // Is address null?
        // -----------------------------------------------------------------------
        RefAddr addr = ref.get(ADDR_TYPE);
        if (addr == null)
        {
            NamingException nex = new NamingException("The address for this Reference is empty (null)");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance : " + nex);
            throw nex;
        }

        // Reference has the right factory and non empty address,
        // so it is OK to generate the object now
        EJBLinkInfo info = (EJBLinkInfo) addr.getContent();

        try
        {
            retObj = getObjectInstance(info, info.ivBeanName);
        } catch (Exception ex)
        {
            // d655264.1 - Include the ref, component, module, and app names
            // in the exception message text.
            String component = "";
            if (info.ivComponent != null)
            {
                component = "in the " + info.ivComponent + " component ";
            }

            String message = "The EJB reference " + component +
                             "in the " + info.ivModule +
                             " module of the " + info.ivApplication +
                             " application could not be resolved";
            if (ex instanceof AmbiguousEJBReferenceException)
            {
                // d725628 - Don't nest the cause, just include the message.
                throw new AmbiguousEJBReferenceException(message + ": " + ex.getMessage());
            }
            throw new EJBException(message, ex);

        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance : " + retObj.getClass().getName());

        return retObj;
    }

    private Object getObjectInstance(EJBLinkInfo info, String beanName) // d654352
    throws Exception
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getObjectInstance : " + beanName);

        // -----------------------------------------------------------------------
        // First - find the bean home (EJSHome) based on what is known
        // -----------------------------------------------------------------------
        // Has the home been resolved already, and cached in the binding info?
        EJSHome home = info.ivHome;

        // If the home has not been resolved previously, then the home must
        // be located via the ejb-link or auto-link information
        if (home == null)
        {
            HomeOfHomes homeOfHomes = EJSContainer.homeOfHomes;

            // Make sure a hOh is available (container is started) before attempting to use it.
            if (homeOfHomes == null)
            {
                InjectionException inex = new InjectionException("EJB Container is not started");
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "getObjectInstance : " + inex);
                throw inex;
            }

            EJBNotFoundException ejbNotFound = null;
            // If the 'beanName' or 'ejb-link' parameter was specified in
            // the annotation or ejb-ref, then finding the home is just
            // a matter of looking up that bean.
            if (beanName != null)
            {
                try
                {
                    // F7434950.CodRev - Use resolveEJBLink.
                    HomeRecord hr = homeOfHomes.resolveEJBLink(info.ivApplication,
                                                               info.ivModule,
                                                               beanName);
                    home = hr.getHomeAndInitialize(); // d648522
                } catch (EJBNotFoundException e)
                {
                    // d654352 - If we failed to find the EJB using beanName, then ignore
                    // it if the client is a WAR
                    //
                    // Prior to 8.0, AMM would merge beanName only if it found a
                    // target; otherwise, beanName was not merged.  Because web
                    // container passes merged view references to injection engine,
                    // this allowed invalid beanNames to be accepted by the system.
                    // As of 8.0, AMM unconditionally merges beanNames, which is
                    // desirable but causes failures for working applications.
                    if (!info.ivModule.endsWith(".war"))
                    {
                        throw e;
                    }

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Discarding EJBNotFoundException encountered in WAR.", e);
                    ejbNotFound = e;
                }

            }

            if (home == null)
            {
                // Look for the home interface, if specified, otherwise
                // use the business interface.                               d432816
                String beanInterface = (info.ivHomeInterface != null) ? info.ivHomeInterface
                                : info.ivBeanInterface;

                if (beanInterface != null)
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "bean name not specified, auto-link on " +
                                     beanInterface);

                    try
                    {
                        // A specific bean was not identified in config, so attempt
                        // 'auto-link', where a bean home is located based on
                        // whether the home or bean implements the interface
                        // being injected.                                      d429866.1
                        home = homeOfHomes.getHomeByInterface(info.ivApplication,
                                                              info.ivModule, // d446993
                                                              beanInterface);
                    } catch (EJBNotFoundException e)
                    {
                        if (ejbNotFound == null)
                        {
                            throw e;
                        }
                        throw new EJBNotFoundException(ejbNotFound.getMessage() + " " + e.getMessage(), e);
                    } catch (AmbiguousEJBReferenceException e) {
                        if (ejbNotFound == null) {
                            throw e;
                        }
                        throw new EJBNotFoundException(ejbNotFound.getMessage() + " " + e.getMessage(), e);
                    }
                }
            }

            if (home == null)
            {
                if (ejbNotFound != null)
                {
                    throw ejbNotFound;
                }

                InjectionException inex = new InjectionException("EJB Reference could not be resolved");
                if (isTraceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "getObjectInstance : " + inex);
                throw inex;
            }

            info.ivHome = home;
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "home = " + home.getJ2EEName());

        // -----------------------------------------------------------------------
        // Second - determine the interface type, and create the return object
        // -----------------------------------------------------------------------
        Object retObj;

        // If the home interface has been provided, then this reference must
        // be either an ejb-ref or ejb-local-ref from xml, as it is not possible
        // to determine that a home has been specified via annotation.
        if (info.ivHomeInterface != null)
        {
            checkHomeSupported(home, info.ivHomeInterface);

            // F743-28601
            // Prior to this track, a reference that came from XML and was passed
            // into the injection engine via either a EjbRef or EjbLocalRef always
            // had either its 'localRef' or 'remoteRef' flag set.
            //
            // An @EJB annotation in a web component gets written out to XML and
            // passed into the injection engine via an EjbRef or EjbLocalRef.
            // In the case where the referenced bean is from a different
            // application, its not possible to determine whether the reference
            // should be local or remote, and so we are forced to guess which
            // type of reference (local or remote) to write out to XML.  This is
            // called an indeterminate reference.  (We don't have this scenario
            // for an @EJB annotation in a bean component, because those don't
            // get translated into XML thats passed into the injection engine,
            // since EJB components don't send the merged view XML into the
            // injection engine.)
            //
            // As of this track, an indeterminate EJB reference no longer has its
            // 'localRef' or 'remoteRef' flag set.
            //
            // However, this does not cause a problem for this logic, because if
            // the homeInterface is non-null, then we know the reference must have
            // come from an explicitly specified entry in XML,
            // and so we know it did not come from an annotation, and thus can not
            // be indeterminate, and thus either the 'localRef' or 'remoteRef' flags
            // will be set.

            if (info.ivIsLocalRef)
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "configured for local home");

                // TODO : verify local home and local interface classes match
                retObj = home.getWrapper().getLocalObject();
            }
            else if (info.ivIsRemoteRef)
            {
                EJBRuntime runtime = home.getContainer().getEJBRuntime();
                runtime.checkRemoteSupported(home, info.ivHomeInterface);

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "configured for remote home");

                // TODO : verify remote home and remote interface classes match
                EJSWrapper wrapper = home.getWrapper().getRemoteWrapper();
                retObj = runtime.getRemoteReference(wrapper);
            }
            else
            {
                throw new InjectionException("Invalid configuration - home interface from annotation?");
            }
        }
        else
        {
            // TODO : consider catching exception and providing meaningful info
            // TODO : consider caching / or using from annotation

            String businessInterfaceName = info.ivBeanInterface;

            // For ejb-link, the injection type might be a super class of a
            // business interface... so find the correct business interface
            // before creating the object. Do this by setting this boolean
            // and pass it into create*BusinessObject.  It will then use this
            // information to decide whether the injection type is a super class
            // of a business interface                                           d449434
            boolean useSupporting = beanName != null;

            // Handle a local business interface from xml
            if (info.ivIsLocalRef)
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "configured for local business interface");

                retObj = home.createLocalBusinessObject(businessInterfaceName, useSupporting);

                if (retObj instanceof Remote)
                {
                    throw new InjectionException("Use of ejb-local-ref for remote");
                }
            }

            // Handle a remote business interface from xml
            else if (info.ivIsRemoteRef)
            {
                home.getContainer().getEJBRuntime().checkRemoteSupported(home, businessInterfaceName);

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "configured for remote business interface");

                retObj = home.createRemoteBusinessObject(businessInterfaceName, useSupporting);
            }

            // Otherwise could be home or business / local or remote
            // as annotations don't provide that level of detail
            else
            {
                BeanMetaData bmd = home.getBeanMetaData();

                // First, check against the home interfaces
                if (businessInterfaceName.equals(bmd.localHomeInterfaceClassName))
                {
                    checkHomeSupported(home, businessInterfaceName);

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "match on local home");

                    retObj = home.getWrapper().getLocalObject();
                }
                else if (businessInterfaceName.equals(bmd.homeInterfaceClassName))
                {
                    checkHomeSupported(home, businessInterfaceName);

                    EJBRuntime runtime = home.getContainer().getEJBRuntime();
                    runtime.checkRemoteSupported(home, businessInterfaceName);

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "match on remote home");

                    EJSWrapper wrapper = home.getWrapper().getRemoteWrapper();
                    retObj = runtime.getRemoteReference(wrapper);
                }
                else
                {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "looking for match on business interface");

                    // The only thing left are the business interfaces
                    retObj = home.createBusinessObject(businessInterfaceName, useSupporting);

                } // End of non-home annotation processing
            } // End of annotation processing
        } // End of non-home xml or annotation processing

        // Careful not to call toString or it may invoke the customer bean!
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance : " + retObj.getClass().getName());

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
