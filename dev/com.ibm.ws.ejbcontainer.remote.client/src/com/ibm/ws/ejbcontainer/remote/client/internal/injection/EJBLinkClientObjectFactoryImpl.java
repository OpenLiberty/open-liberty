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
package com.ibm.ws.ejbcontainer.remote.client.internal.injection;

import java.security.AccessController;
import java.util.Hashtable;

import javax.ejb.EJBException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.rmi.PortableRemoteObject;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

import com.ibm.ejs.util.Util;
import com.ibm.ejs.util.dopriv.GetContextClassLoaderPrivileged;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.clientcontainer.remote.common.ClientSupport;
import com.ibm.ws.clientcontainer.remote.common.ClientSupportFactory;
import com.ibm.ws.container.service.naming.RemoteObjectInstance;
import com.ibm.ws.kernel.LibertyProcess;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;

@Component(service = { ObjectFactory.class, EJBLinkClientObjectFactoryImpl.class })
public class EJBLinkClientObjectFactoryImpl implements ObjectFactory {
    private static final TraceComponent tc = Tr.register(EJBLinkClientObjectFactoryImpl.class);
    private static final TraceComponent tcInjection = Tr.register(EJBLinkClientObjectFactoryImpl.class,
                                                                  "Injection", "com.ibm.wsspi.injectionengine.injection");

    private ClientSupportFactory clientSupportFactory;

    @org.osgi.service.component.annotations.Reference(service = LibertyProcess.class, target = "(wlp.process.type=client)")
    protected void setLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    protected void unsetLibertyProcess(ServiceReference<LibertyProcess> reference) {}

    @org.osgi.service.component.annotations.Reference
    protected void setClientSupportFactory(ClientSupportFactory clientSupportFactory) {
        this.clientSupportFactory = clientSupportFactory;
    }

    protected void unsetClientSupportFactory(ClientSupportFactory clientSupportFactory) {
        this.clientSupportFactory = null;
    }

    @Override
    @Trivial
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
        if (!(obj instanceof Reference)) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance", " : null (non-Reference)");
            return null;
        }

        Reference ref = (Reference) obj;

        // -----------------------------------------------------------------------
        // Is the right factory for this reference?
        // -----------------------------------------------------------------------
        if (!getClass().getName().equals(ref.getFactoryClassName())) {
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance", " : null (wrong factory class: " + ref.getFactoryClassName() + ")");
            return null;
        }

        // -----------------------------------------------------------------------
        // Is address null?
        // -----------------------------------------------------------------------
        RefAddr addr = ref.get(EJBLinkClientInfoRefAddr.ADDR_TYPE);
        if (addr == null) {
            NamingException nex = new NamingException("The address for this Reference is empty (null)");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getObjectInstance", " : " + nex);
            throw nex;
        }

        // The three above tests passed -- so it is OK to generate the Info now
        EJBLinkClientInfo info = (EJBLinkClientInfo) addr.getContent();

        // -----------------------------------------------------------------------
        //
        // First - Determine if the module was specified in ejb-link/beanName
        //
        // -----------------------------------------------------------------------
        String module = null;
        String logicalModule = null;
        String bean = info.ivBeanName;

        // If the 'beanName' or 'ejb-link' parameter was specified in the
        // annotation or ejb-ref, then a module may have been specified, in
        // which case the module specific factory will be used, and a 'create'
        // will be used rather than a 'find'. This logic may be repeated in the
        // server, but is done here in hopes of finding a module specific EJBFactory.
        if (bean != null) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "bean name specified (" + bean +
                             "), checking for module information...");

            // There are three styles that can be used when specifying the ejb-link data.
            //       Style 1: just the bean-component-name itself
            //                      For example:  TestBeanA
            //
            //       Style 2: the <relative physical path of the module containing the bean>#<bean-component-name>
            //                      For example: <blah>/<blah>/myModule.jar#TestBeanA
            //
            //       Style 3: the <logical name of the module containing the bean>/<bean-component-name>
            //                      For example: testModule/TestBeanA

            // All modules will be checked unless it was specified as
            // part of the bean name, like "../other.jar#beanName".
            int bidx = bean.indexOf('#');
            if (bidx > -1) {
                // There is a '#' character, so this means we are dealing with style #2.
                //
                // For style #2, we have these rules:
                //       a) moduleName is the part between the last '/' character
                //          and the '#' character
                //       b) beanName is everything after the '#' character
                int midx = bean.lastIndexOf('/');
                if (midx > -1 && midx < bidx) {
                    module = bean.substring(midx + 1, bidx);
                } else {
                    module = bean.substring(0, bidx);
                }

                bean = bean.substring(bidx + 1);

                if (!module.endsWith(".jar") && !module.endsWith(".war")) {
                    Tr.error(tcInjection, "EJB_MODULE_MUST_END_WITH_JAR_CWNEN0034E", bean, module);
                    throw new InjectionConfigurationException("The ejb-link/beanName is specified incorrectly. The " + bean +
                                                              " bean : " + module + " module name must end with .jar or .war.");
                }

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Used ejb-link style 2 to get module name " + module + " and beanName " + bean);
            } else {
                // There is no '#' character, so we are not dealing with style #2.
                // Determine if we are dealing with style #1 or #3.
                //
                int forwardSlashIndex = bean.lastIndexOf('/');
                if (forwardSlashIndex > -1) {
                    // There is a forward slash, so that means we have style #3.
                    //
                    // For style #3, we have these rules:
                    //      a) moduleName is the everything before the last '/' character
                    //      b) beanName is everything after the last '/' character
                    logicalModule = bean.substring(0, forwardSlashIndex);
                    bean = bean.substring(forwardSlashIndex + 1);

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "Used ejb-link style 3 to get module name " + logicalModule + " and beanName " + bean);
                }
            }
        }

        // -----------------------------------------------------------------------
        //
        // Third - Use the EJBFactory to create an instance of the interface
        //
        // -----------------------------------------------------------------------
        ClientSupport ejbFactory = clientSupportFactory.getRemoteClientSupport();

        String beanInterface = (info.ivHomeInterface != null) ? info.ivHomeInterface
                        : info.ivBeanInterface;

        try {
            RemoteObjectInstance roi = null;
            if (bean != null) {
                if (module != null) {
                    roi = ejbFactory.createEJB(info.ivApplication,
                                               module,
                                               bean,
                                               beanInterface);
                } else if (logicalModule != null) {
                    roi = ejbFactory.createEJB(info.ivApplication,
                                               info.ivBeanName,
                                               beanInterface);
                } else {
                    roi = ejbFactory.findEJBByBeanName(info.ivApplication,
                                                       bean,
                                                       beanInterface);
                }
            } else {
                roi = ejbFactory.findEJBByInterface(info.ivApplication,
                                                    beanInterface);
            }
            retObj = roi.getObject();
        } catch (EJBException exception) {
            if (bean == null) {
                Tr.error(tcInjection, "ENTERPRISE_BEAN_INTERFACE_NOT_FOUND_ON_NODE_CWNEN0068E",
                         info.ivRefName, info.ivModule, beanInterface);
                throw new InjectionException("The " + info.ivRefName +
                                             " EJB reference in the " + info.ivModule +
                                             " module to the " + beanInterface +
                                             " interface of an enterprise bean cannot be resolved on this node.");
            }

            Tr.error(tcInjection, "ENTERPRISE_BEAN_NOT_FOUND_ON_NODE_CWNEN0041E",
                     info.ivRefName, info.ivModule, beanInterface, bean);
            throw new InjectionException("The " + info.ivRefName +
                                         " EJB reference in the " + info.ivModule +
                                         " module to the " + beanInterface +
                                         " interface of the " + bean +
                                         " enterprise bean cannot be resolved on this node.");
        }

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "narrowing " + Util.identity(retObj));

        ClassLoader loader = AccessController.doPrivileged(new GetContextClassLoaderPrivileged());
        Class<?> beanInterfaceClass = loader.loadClass(beanInterface);
        retObj = PortableRemoteObject.narrow(retObj, beanInterfaceClass);

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "narrowed " + Util.identity(retObj));

        // Careful not to call toString or it may invoke the customer bean!
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getObjectInstance", " : " + retObj.getClass().getName());

        return retObj;
    }
}
