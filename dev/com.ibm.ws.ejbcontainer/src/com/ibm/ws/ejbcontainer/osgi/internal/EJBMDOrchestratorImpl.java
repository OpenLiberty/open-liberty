/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.ejb.EJBException;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.ContainerException;
import com.ibm.ejs.container.ContainerProperties;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.interceptors.InterceptorMetaData;
import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.cpi.Persister;
import com.ibm.websphere.csi.EJBModuleConfigData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.EJBMethodInterface;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.failover.SfFailoverCache;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.OSGiEJBApplicationMetaData;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.OSGiEJBMethodMetaDataImpl;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.OSGiEJBModuleMetaDataImpl;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.WCCMMetaDataImpl;
import com.ibm.ws.ejbcontainer.runtime.AbstractEJBRuntime;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.injectionengine.osgi.util.Link;
import com.ibm.ws.injectionengine.osgi.util.OSGiJNDIEnvironmentRefBindingHelper;
import com.ibm.ws.javaee.dd.commonbnd.MessageDestination;
import com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup;
import com.ibm.ws.javaee.dd.commonext.Method;
import com.ibm.ws.javaee.dd.ejb.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd;
import com.ibm.ws.javaee.dd.ejbbnd.Interface;
import com.ibm.ws.javaee.dd.ejbbnd.MessageDriven;
import com.ibm.ws.javaee.dd.ejbext.BeanCache;
import com.ibm.ws.javaee.dd.ejbext.RunAsMode;
import com.ibm.ws.javaee.dd.ejbext.Session;
import com.ibm.ws.managedobject.ManagedObjectService;
import com.ibm.ws.metadata.ejb.EJBMDOrchestrator;
import com.ibm.ws.metadata.ejb.ModuleInitData;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.wsspi.ejbcontainer.WSEJBHandlerResolver;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

public class EJBMDOrchestratorImpl extends EJBMDOrchestrator {

    private static final TraceComponent tc = Tr.register(EJBMDOrchestratorImpl.class);
    private static final TraceComponent tcContainer = Tr.register(EJBMDOrchestratorImpl.class.getName(),
                                                                  EJBMDOrchestratorImpl.class,
                                                                  "EJBContainer",
                                                                  "com.ibm.ejs.container.container");
    private final AtomicServiceReference<ManagedObjectService> managedObjectServiceRef;

    public EJBMDOrchestratorImpl(AtomicServiceReference<ManagedObjectService> managedObjectServiceRef) {
        this.managedObjectServiceRef = managedObjectServiceRef;
    }

    @Override
    public void processEJBJarBindings(ModuleInitData mid, EJBModuleMetaDataImpl mmd) throws EJBConfigurationException {

        EJBJarBnd ejbJarBinding = getModuleInitDataImpl(mid).ejbJarBinding;
        if (ejbJarBinding != null) // d683087.1
        {
            processEJBBindings(mmd, ejbJarBinding);
            processMessageDestinationBindings(mmd, ejbJarBinding);
        }
        processMessageDestinationBindingDefault(mid, mmd);
        processActivationSpecBindingDefault(mmd);
    }

    private ModuleInitDataImpl getModuleInitDataImpl(ModuleInitData mid) {
        return (ModuleInitDataImpl) mid;
    }

    private void processEJBBindings(EJBModuleMetaDataImpl mmd, EJBJarBnd ejbJarBinding) throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        // Load the binding names for configuration files (ie. non-container default bindings
        // names.   First, we may have a simple binding name.  For pre-EJB3, this was the home's
        // JNDI name (ie. both local and remote after a little tweaking).  Also the home's JNDI
        // name either came directly from the binding xmi file, or was constructed from the home's
        // class name if missing in the bindings file.  For EJB3, the simple binding name comes
        // from the new bindings xml file.   In EJB3 it cam apply to both home and business
        // business interfaces, depending on bean type.  In EJB3 if a simple binding name is not
        // provided the customer may have provided specific JNDI binding names for the local home,
        // remote home, or business interface(s) of the bean.   Finally, for EJB3, if neither a
        // simple binding name, or specific JNDI binding names are provide, the container will
        // provide a default binding name instead.

        Set<String> bindingsUsed = ContainerProperties.IgnoreDuplicateEJBBindings ? new HashSet<String>() : null; // PM51230

        for (com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean ejbBinding : ejbJarBinding.getEnterpriseBeans()) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "processing EJB binding", new Object[] { ejbBinding });

            String ejbName = ejbBinding.getName();
            if (ejbName == null) {
                Tr.warning(tcContainer, "INCOMPLETE_EJB_BINDING_CNTR0142W", "" /* session.getJndiName() */);
            } else if (bindingsUsed == null || bindingsUsed.add(ejbName)) { // PM51230
                BeanMetaData bmd = mmd.ivBeanMetaDatas.get(ejbName);

                if (ejbBinding instanceof com.ibm.ws.javaee.dd.ejbbnd.Session) {
                    if (bmd == null || !bmd.isSessionBean()) {
                        Tr.warning(tcContainer, "ORPHAN_BINDING_ENTRY_CNTR0169E",
                                   new Object[] { ejbName, "session", mmd.ivName });
                    } else {
                        processSessionBeanBinding(bmd, (com.ibm.ws.javaee.dd.ejbbnd.Session) ejbBinding);
                    }
                } else if (ejbBinding instanceof com.ibm.ws.javaee.dd.ejbbnd.MessageDriven) {
                    if (bmd == null || !bmd.isMessageDrivenBean()) {
                        Tr.warning(tcContainer, "ORPHAN_BINDING_ENTRY_CNTR0169E",
                                   new Object[] { ejbName, "message-driven", mmd.ivName });
                    } else {
                        processMessageDrivenBeanBinding(bmd, (com.ibm.ws.javaee.dd.ejbbnd.MessageDriven) ejbBinding);
                    }
                } else {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "unknown binding: ", new Object[] { ejbBinding, ejbBinding });
                }
            }
        }
    }

    private void processMessageDestinationBindingDefault(ModuleInitData mid, EJBModuleMetaDataImpl mmd) throws EJBException {
        if (mid.ivEJBJar != null) {
            for (EnterpriseBean eb : mid.ivEJBJar.getEnterpriseBeans()) {
                if (eb.getKindValue() == EnterpriseBean.KIND_MESSAGE_DRIVEN) {
                    BeanMetaData bmd = mmd.ivBeanMetaDatas.get(eb.getName());

                    // If the message destination jndi name has not already been set from jca-adapter bindings,
                    // try to set a default for this bean
                    if (bmd.ivMessageDestinationJndiName == null) {
                        String bindingName = null;
                        com.ibm.ws.javaee.dd.ejb.MessageDriven messageDriven = (com.ibm.ws.javaee.dd.ejb.MessageDriven) eb;
                        String msgDrivenLink = messageDriven.getLink();
                        if (msgDrivenLink != null) {
                            Link parsedLink = Link.parseMessageDestinationLink(mmd.getName(), msgDrivenLink);

                            if (parsedLink.moduleURI != null) {
                                // retrieve the module meta data for the linked module name.
                                OSGiEJBApplicationMetaData ejbAMD = getOSGiApplicationMetaData(mmd);
                                EJBModuleMetaDataImpl linkedMMD = ejbAMD.getModuleMetaData(parsedLink.moduleURI);
                                if (linkedMMD != null) {
                                    bindingName = linkedMMD.ivMessageDestinationBindingMap.get(parsedLink.name);
                                }
                            } else {
                                bindingName = mmd.ivMessageDestinationBindingMap.get(parsedLink.name);
                            }

                            if (bindingName == null) {
                                bindingName = parsedLink.name;
                            }
                        }

                        if (bindingName != null) {
                            bmd.ivMessageDestinationJndiName = bindingName;
                        }
                    }
                }
            }
        }
    }

    private void processActivationSpecBindingDefault(EJBModuleMetaDataImpl mmd) {
        for (BeanMetaData bmd : mmd.ivBeanMetaDatas.values()) {
            if (bmd.type == InternalConstants.TYPE_MESSAGE_DRIVEN) {
                // Message listener ports are not supported in Liberty, so regardless whether
                // one of those has been provided, make sure an activation spec name is set.
                if (bmd.ivActivationSpecJndiName == null) {
                    bmd.ivActivationSpecJndiName = getDefaultActivationSpecJndiName(bmd);
                }
            }
        }
    }

    /**
     * Cast an EJBApplicationMetaData to a OSGiApplicationMetaData.
     *
     * @return OSGiEJBApplicationMetaData
     */
    private OSGiEJBApplicationMetaData getOSGiApplicationMetaData(EJBModuleMetaDataImpl mmd) {
        return (OSGiEJBApplicationMetaData) mmd.getEJBApplicationMetaData();
    }

    private OSGiEJBModuleMetaDataImpl getOSGiEJBModuleMetaDataImpl(EJBModuleMetaDataImpl mmd) {
        return (OSGiEJBModuleMetaDataImpl) mmd;
    }

    private void processMessageDrivenBeanBinding(BeanMetaData bmd, MessageDriven mdbBinding) // F743-36290.1
    {
        if (mdbBinding.getJCAAdapter() != null) {
            bmd.ivActivationSpecJndiName = nullIfEmpty(mdbBinding.getJCAAdapter().getActivationSpecBindingName());
            bmd.ivActivationSpecAuthAlias = nullIfEmpty(mdbBinding.getJCAAdapter().getActivationSpecAuthAlias());
            bmd.ivMessageDestinationJndiName = nullIfEmpty(mdbBinding.getJCAAdapter().getDestinationBindingName());
        }

        if (mdbBinding.getListenerPort() != null) {
            bmd.ivMessageListenerPortName = mdbBinding.getListenerPort().getName();
        }
    }

    /**
     * @param String name
     * @return turns an empty string to a null.
     */
    private String nullIfEmpty(String bindingName) {
        if (bindingName == null || bindingName.trim().isEmpty()) {
            return null;
        }
        return bindingName;
    }

    /**
     * Return the default activation specification jndi name for the provided
     * message-driven bean. The default name follows the same convention
     * as is defined in the EJB specification for business interfaces bound
     * in the java:global name context (excluding the interface name). <p>
     *
     * [<app-name>]/<module-name>/<bean-name>
     *
     * <app-name> only applies if the bean is packaged within an .ear file.
     * It defaults to the base name of the .ear file with no filename extension,
     * unless specified by the application.xml deployment descriptor.
     *
     * <module-name> is the name of the module in which the bean is packaged.
     * In a stand-alone ejb-jar file or .war file, the <module-name> defaults
     * to the base name of the module with any filename extension removed.
     * In an ear file, the <module-name> defaults to the pathname of the module
     * with any filename extension removed, but with any directory names included.
     * The default <module-name> can be overridden using the module-name element
     * of ejb-jar.xml (for ejb-jar files) or web.xml (for .war files).
     *
     * <bean-name> is the ejb-name of the enterprise bean. For enterprise beans
     * defined via annotation, it defaults to the unqualified name of the session
     * bean class, unless specified in the contents of the MessageDriven annotation
     * name() attribute. For enterprise beans defined via ejb-jar.xml, itâ€™s
     * specified in the <ejb-name> deployment descriptor element.
     *
     * In full profile, the activation specification jndi name is required in
     * the binding file, however, in the Liberty profile a default is provided
     * so that a customer can avoid including an ibm-ejb-jar-bnd.xml file.
     */
    private String getDefaultActivationSpecJndiName(BeanMetaData bmd) {
        EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;
        EJBApplicationMetaData amd = mmd.getEJBApplicationMetaData();

        String appName = amd.getLogicalName();
        StringBuilder sb = new StringBuilder();

        if (appName != null) {
            sb.append(appName).append('/');
        }
        sb.append(mmd.ivLogicalName).append('/');
        sb.append(bmd.enterpriseBeanName);

        return sb.toString();
    }

    private void processSessionBeanBinding(BeanMetaData bmd, com.ibm.ws.javaee.dd.ejbbnd.Session sessionBinding) throws EJBException, EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        String ejbName = bmd.enterpriseBeanName;
        EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;

        // Set the component-id override for default binding names.
        String componentID = sessionBinding.getComponentID();
        if (componentID != null) {
            if (componentID.trim().length() == 0) {
                bmd.ivComponent_Id = null;
                OnError onError = ContainerProperties.customBindingsOnErr;
                switch (onError) {
                    case WARN:
                        Tr.warning(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                   new Object[] { ejbName, mmd.ivName });
                        break;
                    case FAIL:
                        Tr.error(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                 new Object[] { ejbName, mmd.ivName });
                        throw new EJBException("The " + ejbName +
                                               " bean or home in the " + mmd.ivName +
                                               " module contains a blank string value for the" +
                                               " Java Naming and Directory Interface (JNDI) binding name.");
                    case IGNORE:
                    default:
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tcContainer, "Ignoring custom binding configuration for" + ejbName + "in " + mmd.ivName + ". blank component-id binding.");
                        break;
                }
            } else {
                bmd.ivComponent_Id = componentID.trim();
            }
        }

        String localHomeBindingName = sessionBinding.getLocalHomeBindingName();
        if (localHomeBindingName != null) {
            bmd.localHomeJndiBindingName = localHomeBindingName.trim();

            if (localHomeBindingName.trim().length() == 0) {
                bmd.localHomeJndiBindingName = null;
                OnError onError = ContainerProperties.customBindingsOnErr;
                switch (onError) {
                    case WARN:
                        Tr.warning(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                   new Object[] { ejbName, mmd.ivName });
                        break;
                    case FAIL:
                        Tr.error(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                 new Object[] { ejbName, mmd.ivName });
                        throw new EJBException("The " + ejbName +
                                               " bean or home in the " + mmd.ivName +
                                               " module contains a blank string value for the" +
                                               " Java Naming and Directory Interface (JNDI) binding name.");
                    case IGNORE:
                    default:
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tcContainer, "Ignoring custom binding configuration for" + ejbName + "in " + mmd.ivName + ". blank local-home binding.");
                        break;
                }
            }
        }

        String remoteHomeBindingName = sessionBinding.getRemoteHomeBindingName();
        if (remoteHomeBindingName != null) {
            bmd.remoteHomeJndiBindingName = remoteHomeBindingName.trim();
            if (remoteHomeBindingName.trim().length() == 0) {
                bmd.remoteHomeJndiBindingName = null;
                OnError onError = ContainerProperties.customBindingsOnErr;
                switch (onError) {
                    case WARN:
                        Tr.warning(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                   new Object[] { ejbName, mmd.ivName });
                        break;
                    case FAIL:
                        Tr.error(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                 new Object[] { ejbName, mmd.ivName });
                        throw new EJBException("The " + ejbName +
                                               " bean or home in the " + mmd.ivName +
                                               " module contains a blank string value for the" +
                                               " Java Naming and Directory Interface (JNDI) binding name.");
                    case IGNORE:
                    default:
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tcContainer, "Ignoring custom binding configuration for" + ejbName + "in " + mmd.ivName + ". blank remote-home binding.");
                        break;
                }
            }
        }

        // If there are business interface bindings we need to save them,
        // and also check for duplicates. We do not allow multiple
        // bindings to be supplied for a single business interface. We
        // also need to check that the binding name is not an empty
        // string.
        List<Interface> interfaceBindings = sessionBinding.getInterfaces();
        if (!interfaceBindings.isEmpty()) {
            bmd.businessInterfaceJndiBindingNames = new HashMap<String, String>();
            for (Interface interfaceBinding : interfaceBindings) {
                String interfaceBindingName = interfaceBinding.getBindingName();

                boolean isntBlank = true;
                if (interfaceBindingName.trim().length() == 0) {
                    isntBlank = false;
                    OnError onError = ContainerProperties.customBindingsOnErr;
                    switch (onError) {
                        case WARN:
                            Tr.warning(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                       new Object[] { ejbName, mmd.ivName });
                            break;
                        case FAIL:
                            Tr.error(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                     new Object[] { ejbName, mmd.ivName });
                            throw new EJBException("The " + ejbName +
                                                   " bean or home in the " + mmd.ivName +
                                                   " module contains a blank string value for the" +
                                                   " Java Naming and Directory Interface (JNDI) binding name.");
                        case IGNORE:
                        default:
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tcContainer, "Ignoring custom binding configuration for" + ejbName + "in " + mmd.ivName + ". blank business interface binding.");
                            break;
                    }
                }

                String interfaceClassName = interfaceBinding.getClassName();

                // The binding for the No-Interface view may specify either the
                // bean class or a blank (class=""). If a blank was specified,
                // convert it to the bean class. Blank is the value that will be
                // generated by the tooling, but if the customer enters the
                // class manually, that is also supported.                 F743-1756.3
                if (interfaceClassName.equals("")) {
                    interfaceClassName = bmd.enterpriseBeanClassName;
                }

                String oldInterfaceBindingName = isntBlank ? bmd.businessInterfaceJndiBindingNames.put(interfaceClassName, interfaceBindingName) : null;
                if (oldInterfaceBindingName != null) {
                    Tr.error(tcContainer, "MULTIPLE_JNDI_BINDING_NAMES_CNTR0139E",
                             new Object[] { ejbName, mmd.ivName, interfaceClassName });
                    throw new EJBException("The " + ejbName +
                                           " bean in the " + mmd.ivName +
                                           " module has multiple Java Naming and Directory Interface (JNDI)" +
                                           " binding names specified for the " + interfaceClassName +
                                           " business interface.");
                }
            }
        }

        boolean hasSpecificBinding = bmd.localHomeJndiBindingName != null ||
                                     bmd.remoteHomeJndiBindingName != null ||
                                     bmd.businessInterfaceJndiBindingNames != null;
        processSimpleBindingName(bmd, sessionBinding, hasSpecificBinding);

        validateEJBBindings(bmd);

    }

    private void processSimpleBindingName(BeanMetaData bmd,
                                          com.ibm.ws.javaee.dd.ejbbnd.Session ejbBinding,
                                          boolean hasSpecificBinding) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        String ejbName = bmd.enterpriseBeanName;
        EJBModuleMetaDataImpl mmd = bmd._moduleMetaData;

        // Process simple binding name.
        String simpleJndiBindingName = ejbBinding.getSimpleBindingName();
        if (simpleJndiBindingName != null) {
            if (hasSpecificBinding) {
                Tr.error(tcContainer, "INVALID_JNDI_BINDING_COMBINATION_CNTR0130E",
                         new Object[] { ejbName, mmd.ivName });
                throw new EJBException("When a simple Java Naming and Directory Interface (JNDI)" +
                                       " binding name is specified for a bean or home," +
                                       " specific JNDI bindings cannot be specified." +
                                       " The " + ejbName +
                                       " bean in the " + mmd.ivName +
                                       " module either must use a simple JNDI binding name" +
                                       " or specific JNDI bindings, but not use both options.");
            }
            bmd.simpleJndiBindingName = simpleJndiBindingName.trim();
            if (simpleJndiBindingName.trim().length() == 0) {
                bmd.simpleJndiBindingName = null;
                OnError onError = ContainerProperties.customBindingsOnErr;
                switch (onError) {
                    case WARN:
                        Tr.warning(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                   new Object[] { ejbName, mmd.ivName });
                        break;
                    case FAIL:
                        Tr.error(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                 new Object[] { ejbName, mmd.ivName });
                        throw new EJBException("The " + ejbName +
                                               " bean or home in the " + mmd.ivName +
                                               " module contains a blank string value for the" +
                                               " Java Naming and Directory Interface (JNDI) binding name.");
                    case IGNORE:
                    default:
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tcContainer, "Ignoring custom binding configuration for" + ejbName + "in " + mmd.ivName + ". blank simple binding.");
                        break;
                }
            } else if (simpleJndiBindingName.contains(":")) {
                bmd.simpleJndiBindingName = null;
                OnError onError = ContainerProperties.customBindingsOnErr;
                switch (onError) {
                    case WARN:
                        Tr.warning(tcContainer, "NAMESPACE_IN_JNDI_BINDING_NAME_CNTR0339W",
                                   new Object[] { ejbName, mmd.ivName, simpleJndiBindingName });
                        break;
                    case FAIL:
                        Tr.error(tcContainer, "NAMESPACE_IN_JNDI_BINDING_NAME_CNTR0339W",
                                 new Object[] { ejbName, mmd.ivName, simpleJndiBindingName });
                        throw new EJBException("The " + ejbName +
                                               " bean or home in the " + mmd.ivName +
                                               " module contains a namespace in the string value for the" +
                                               " Java Naming and Directory Interface (JNDI) binding name.");
                    case IGNORE:
                    default:
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tcContainer, "Ignoring custom binding configuration for" + ejbName + "in " + mmd.ivName + ". namespace in simple binding.");
                        break;
                }
            }
        }
    }

    private static final String EJBLOCAL_BINDING_PREFIX = "ejblocal:"; // d456907

    private void validateEJBBindings(BeanMetaData bmd) // F743-36290.1
                    throws EJBConfigurationException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        // Check out the binding(s) that have been specified for this bean and/or it's home.
        //
        // When business interface bindings are supplied there needs to be a matching business interface
        // class for each of the bindings.   Also, local buisiness interface bindgins must begin with
        // "ejblocal:" and remote business interface bindings must NOT begin with "ejblocal:".
        //
        // If a home binding has been specified, there needs to be a matcthing home interface.  Also,
        // local home bindings must begin with "ejblocal:", and remote home bindings must NOT begin with "ejblocal:".
        //
        // Besides local home bindings beginning with "ejblocal:" we do not allow any other namespaces (nested or otherwise)
        //
        // Since all of the checks in this section only apply to EJB3 beans so we will throw an exception
        // and fail application start if any of the listed configuration errors are detected.

        if (bmd.ivComponent_Id != null) {
            if (bmd.ivComponent_Id.contains(":")) {
                String providedBinding = bmd.ivComponent_Id;
                bmd.ivComponent_Id = null;
                OnError onError = ContainerProperties.customBindingsOnErr;
                switch (onError) {
                    case WARN:
                        Tr.warning(tcContainer, "NAMESPACE_IN_JNDI_BINDING_NAME_CNTR0339W",
                                   new Object[] { bmd.enterpriseBeanName,
                                                  bmd._moduleMetaData.ivName,
                                                  providedBinding });
                        break;
                    case FAIL:
                        Tr.error(tcContainer, "NAMESPACE_IN_JNDI_BINDING_NAME_CNTR0339W",
                                 new Object[] { bmd.enterpriseBeanName,
                                                bmd._moduleMetaData.ivName,
                                                providedBinding });
                        throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                            " bean or home in the " + bmd._moduleMetaData.ivName +
                                                            " module contains a namespace in the string value for the" +
                                                            " Java Naming and Directory Interface (JNDI) binding name.");
                    case IGNORE:
                    default:
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tcContainer, "Ignoring custom binding configuration for" + bmd.enterpriseBeanName + "in " + bmd._moduleMetaData.ivName
                                                  + ". namespace in component-id binding.");
                        break;
                }
            }
        }

        if (bmd.businessInterfaceJndiBindingNames != null) {
            for (Map.Entry<String, String> entry : bmd.businessInterfaceJndiBindingNames.entrySet()) {
                String bindingInterface = entry.getKey();
                String bindingValue = entry.getValue();
                boolean matchFound = false;

                String[] localBusinessInterfaces = bmd.ivBusinessLocalInterfaceClassNames;
                if (localBusinessInterfaces != null) {
                    for (String localBusinessInterface : localBusinessInterfaces) {
                        if (bindingInterface.equals(localBusinessInterface)) {
                            matchFound = true;

                            // Also, make sure binding for this local business interface begins with ejblocal:
                            if (!bindingValue.startsWith(EJBLOCAL_BINDING_PREFIX)) {
                                Tr.error(tcContainer, "IMPROPER_LOCAL_JNDI_BINDING_PREFIX_CNTR0136E",
                                         new Object[] { bmd.enterpriseBeanName,
                                                        bmd._moduleMetaData.ivName,
                                                        bindingValue });
                                throw new EJBConfigurationException("The specific Java Naming and Directory Interface (JNDI)" +
                                                                    " binding name provided for a local bean does not begin with" +
                                                                    " ejblocal:. The " + bindingValue +
                                                                    " binding name that is specified for the " + bmd.enterpriseBeanName +
                                                                    " bean in the " + bmd._moduleMetaData.ivName +
                                                                    " module does not begin with ejblocal:.");
                            }
                            String trimmedBindingValue = bindingValue.substring(EJBLOCAL_BINDING_PREFIX.length());
                            if (trimmedBindingValue.trim().length() == 0) {
                                bmd.businessInterfaceJndiBindingNames.remove(bindingInterface);
                                OnError onError = ContainerProperties.customBindingsOnErr;
                                switch (onError) {
                                    case WARN:
                                        Tr.warning(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                                   new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                                        break;
                                    case FAIL:
                                        Tr.error(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                                 new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                                        throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                                            " bean or home in the " + bmd._moduleMetaData.ivName +
                                                                            " module contains a blank string value for the" +
                                                                            " Java Naming and Directory Interface (JNDI) binding name.");
                                    case IGNORE:
                                    default:
                                        if (isTraceOn && tc.isDebugEnabled())
                                            Tr.debug(tcContainer, "Ignoring custom binding configuration for" + bmd.enterpriseBeanName + "in " + bmd._moduleMetaData.ivName
                                                                  + ". blank local business interface binding.");
                                        break;
                                }
                            } else if (trimmedBindingValue.contains(":")) {
                                bmd.businessInterfaceJndiBindingNames.remove(bindingInterface);
                                OnError onError = ContainerProperties.customBindingsOnErr;
                                switch (onError) {
                                    case WARN:
                                        Tr.warning(tcContainer, "NAMESPACE_IN_LOCAL_JNDI_BINDING_NAME_CNTR0340W",
                                                   new Object[] { bmd.enterpriseBeanName,
                                                                  bmd._moduleMetaData.ivName,
                                                                  bindingValue });
                                        break;
                                    case FAIL:
                                        Tr.error(tcContainer, "NAMESPACE_IN_LOCAL_JNDI_BINDING_NAME_CNTR0340W",
                                                 new Object[] { bmd.enterpriseBeanName,
                                                                bmd._moduleMetaData.ivName,
                                                                bindingValue });
                                        throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                                            " bean or home in the " + bmd._moduleMetaData.ivName +
                                                                            " module contains a namespace in the string value for the" +
                                                                            " Java Naming and Directory Interface (JNDI) binding name.");
                                    case IGNORE:
                                    default:
                                        if (isTraceOn && tc.isDebugEnabled())
                                            Tr.debug(tcContainer, "Ignoring custom binding configuration for" + bmd.enterpriseBeanName + "in " + bmd._moduleMetaData.ivName
                                                                  + ". namespace in local business interface binding.");
                                        break;
                                }
                            }
                        }
                    }
                }

                String[] remoteBusinessInterfaces = bmd.ivBusinessRemoteInterfaceClassNames;
                if (remoteBusinessInterfaces != null) {
                    for (String remoteBusinessInterface : remoteBusinessInterfaces) {
                        if (bindingInterface.equals(remoteBusinessInterface)) {
                            matchFound = true;

                            // Also, make sure binding for this remote business interface does NOT begin with ejblocal:
                            if (bindingValue.startsWith(EJBLOCAL_BINDING_PREFIX)) {
                                Tr.error(tcContainer, "IMPROPER_REMOTE_JNDI_BINDING_PREFIX_CNTR0137E",
                                         new Object[] { bmd.enterpriseBeanName,
                                                        bmd._moduleMetaData.ivName,
                                                        bindingValue });
                                throw new EJBConfigurationException("The specific Java Naming and Directory Interface (JNDI)" +
                                                                    " binding name that is provided for a remote" +
                                                                    " bean begins with ejblocal:.  The " + bindingValue +
                                                                    " remote binding name that is specified for the " + bmd.enterpriseBeanName +
                                                                    " bean in the " + bmd._moduleMetaData.ivName +
                                                                    " module cannot begin with ejblocal:.");
                            }
                            if (bindingValue.trim().length() == 0) {
                                bmd.businessInterfaceJndiBindingNames.remove(bindingInterface);
                                OnError onError = ContainerProperties.customBindingsOnErr;
                                switch (onError) {
                                    case WARN:
                                        Tr.warning(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                                   new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                                        break;
                                    case FAIL:
                                        Tr.error(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                                 new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                                        throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                                            " bean or home in the " + bmd._moduleMetaData.ivName +
                                                                            " module contains a blank string value for the" +
                                                                            " Java Naming and Directory Interface (JNDI) binding name.");
                                    case IGNORE:
                                    default:
                                        if (isTraceOn && tc.isDebugEnabled())
                                            Tr.debug(tcContainer, "Ignoring custom binding configuration for" + bmd.enterpriseBeanName + "in " + bmd._moduleMetaData.ivName
                                                                  + ". blank remote business interface binding.");
                                        break;
                                }
                            } else if (bindingValue.contains(":")) {
                                bmd.businessInterfaceJndiBindingNames.remove(bindingInterface);
                                OnError onError = ContainerProperties.customBindingsOnErr;
                                switch (onError) {
                                    case WARN:
                                        Tr.warning(tcContainer, "NAMESPACE_IN_JNDI_BINDING_NAME_CNTR0339W",
                                                   new Object[] { bmd.enterpriseBeanName,
                                                                  bmd._moduleMetaData.ivName,
                                                                  bindingValue });
                                        break;
                                    case FAIL:
                                        Tr.error(tcContainer, "NAMESPACE_IN_JNDI_BINDING_NAME_CNTR0339W",
                                                 new Object[] { bmd.enterpriseBeanName,
                                                                bmd._moduleMetaData.ivName,
                                                                bindingValue });
                                        throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                                            " bean or home in the " + bmd._moduleMetaData.ivName +
                                                                            " module contains a namespace in the string value for the" +
                                                                            " Java Naming and Directory Interface (JNDI) binding name.");
                                    case IGNORE:
                                    default:
                                        if (isTraceOn && tc.isDebugEnabled())
                                            Tr.debug(tcContainer, "Ignoring custom binding configuration for" + bmd.enterpriseBeanName + "in " + bmd._moduleMetaData.ivName
                                                                  + ". namespace in remote business interface binding.");
                                        break;
                                }
                            }
                        }
                    }
                }

                if (bmd.ivLocalBean) // d617690
                {
                    if (bindingInterface.equals(bmd.enterpriseBeanClassName)) // F743-1756.3
                    {
                        matchFound = true;

                        // Also, make sure binding for this local interface begins with ejblocal:
                        if (!bindingValue.startsWith(EJBLOCAL_BINDING_PREFIX)) {
                            Tr.error(tcContainer, "IMPROPER_LOCAL_JNDI_BINDING_PREFIX_CNTR0136E",
                                     new Object[] { bmd.enterpriseBeanName,
                                                    bmd._moduleMetaData.ivName,
                                                    bindingValue });
                            throw new EJBConfigurationException("The specific Java Naming and Directory Interface (JNDI)" +
                                                                " binding name provided for a local bean does not begin with" +
                                                                " ejblocal:. The " + bindingValue +
                                                                " binding name that is specified for the " + bmd.enterpriseBeanName +
                                                                " bean in the " + bmd._moduleMetaData.ivName +
                                                                " module does not begin with ejblocal:.");
                        }
                        String trimmedBindingValue = bindingValue.substring(EJBLOCAL_BINDING_PREFIX.length());
                        if (trimmedBindingValue.trim().length() == 0) {
                            bmd.businessInterfaceJndiBindingNames.remove(bindingInterface);
                            OnError onError = ContainerProperties.customBindingsOnErr;
                            switch (onError) {
                                case WARN:
                                    Tr.warning(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                               new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                                    break;
                                case FAIL:
                                    Tr.error(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                             new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                                    throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                                        " bean or home in the " + bmd._moduleMetaData.ivName +
                                                                        " module contains a blank string value for the" +
                                                                        " Java Naming and Directory Interface (JNDI) binding name.");
                                case IGNORE:
                                default:
                                    if (isTraceOn && tc.isDebugEnabled())
                                        Tr.debug(tcContainer, "Ignoring custom binding configuration for" + bmd.enterpriseBeanName + "in " + bmd._moduleMetaData.ivName
                                                              + ". blank in local interface binding.");
                                    break;
                            }
                            Tr.error(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                     new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                            throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                                " bean or home in the " + bmd._moduleMetaData.ivName +
                                                                " module contains a blank string value for the" +
                                                                " Java Naming and Directory Interface (JNDI) binding name.");
                        } else if (trimmedBindingValue.contains(":")) {
                            bmd.businessInterfaceJndiBindingNames.remove(bindingInterface);
                            OnError onError = ContainerProperties.customBindingsOnErr;
                            switch (onError) {
                                case WARN:
                                    Tr.warning(tcContainer, "NAMESPACE_IN_LOCAL_JNDI_BINDING_NAME_CNTR0340W",
                                               new Object[] { bmd.enterpriseBeanName,
                                                              bmd._moduleMetaData.ivName,
                                                              bindingValue });
                                    break;
                                case FAIL:
                                    Tr.error(tcContainer, "NAMESPACE_IN_LOCAL_JNDI_BINDING_NAME_CNTR0340W",
                                             new Object[] { bmd.enterpriseBeanName,
                                                            bmd._moduleMetaData.ivName,
                                                            bindingValue });
                                    throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                                        " bean or home in the " + bmd._moduleMetaData.ivName +
                                                                        " module contains a namespace in the string value for the" +
                                                                        " Java Naming and Directory Interface (JNDI) binding name.");
                                case IGNORE:
                                default:
                                    if (isTraceOn && tc.isDebugEnabled())
                                        Tr.debug(tcContainer, "Ignoring custom binding configuration for" + bmd.enterpriseBeanName + "in " + bmd._moduleMetaData.ivName
                                                              + ". namespace in local interface binding.");
                                    break;
                            }
                        }
                    }
                }

                if (!matchFound) {
                    Tr.error(tcContainer, "JNDI_BINDING_HAS_NO_CORRESPONDING_BUSINESS_INTERFACE_CLASS_CNTR0140E",
                             new Object[] { bmd.enterpriseBeanName,
                                            bmd._moduleMetaData.ivName,
                                            bindingInterface });
                    throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                        " bean in the " + bmd._moduleMetaData.ivName +
                                                        " module has specified the [" + bindingInterface +
                                                        "] business interface, which does not exist" +
                                                        " for a business interface Java Naming and Directory Interface (JNDI) binding.");
                }
            }
        }

        if (bmd.localHomeJndiBindingName != null) {
            if (bmd.localHomeInterfaceClassName == null) {
                Tr.error(tcContainer, "JNDI_BINDING_HAS_NO_CORRESPONDING_HOME_INTERFACE_CLASS_CNTR0141E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                    " bean in the " + bmd._moduleMetaData.ivName +
                                                    " module has specified a home Java Naming and Directory Interface (JNDI)" +
                                                    " binding. The binding does not have a matching home interface class.");
            }

            if (!bmd.localHomeJndiBindingName.startsWith(EJBLOCAL_BINDING_PREFIX)) {
                Tr.error(tcContainer, "IMPROPER_LOCAL_JNDI_BINDING_PREFIX_CNTR0136E",
                         new Object[] { bmd.enterpriseBeanName,
                                        bmd._moduleMetaData.ivName,
                                        bmd.localHomeJndiBindingName });
                throw new EJBConfigurationException("The specific Java Naming and Directory Interface (JNDI)" +
                                                    " binding name provided for a local home does not begin with" +
                                                    " ejblocal:. The " + bmd.localHomeJndiBindingName +
                                                    " binding name that is specified for the home of " + bmd.enterpriseBeanName +
                                                    " bean in the " + bmd._moduleMetaData.ivName +
                                                    " module does not begin with ejblocal:.");
            }
            String trimmedBindingValue = bmd.localHomeJndiBindingName.substring(EJBLOCAL_BINDING_PREFIX.length());
            if (trimmedBindingValue.trim().length() == 0) {
                bmd.localHomeJndiBindingName = null;
                OnError onError = ContainerProperties.customBindingsOnErr;
                switch (onError) {
                    case WARN:
                        Tr.warning(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                   new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                        break;
                    case FAIL:
                        Tr.error(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                 new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                        throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                            " bean or home in the " + bmd._moduleMetaData.ivName +
                                                            " module contains a blank string value for the" +
                                                            " Java Naming and Directory Interface (JNDI) binding name.");
                    case IGNORE:
                    default:
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tcContainer,
                                     "Ignoring custom binding configuration for" + bmd.enterpriseBeanName + "in " + bmd._moduleMetaData.ivName + ". blank in local-home binding.");
                        break;
                }
            } else if (trimmedBindingValue.contains(":")) {
                String providedBinding = bmd.localHomeJndiBindingName;
                bmd.localHomeJndiBindingName = null;
                OnError onError = ContainerProperties.customBindingsOnErr;
                switch (onError) {
                    case WARN:
                        Tr.warning(tcContainer, "NAMESPACE_IN_LOCAL_JNDI_BINDING_NAME_CNTR0340W",
                                   new Object[] { bmd.enterpriseBeanName,
                                                  bmd._moduleMetaData.ivName,
                                                  providedBinding });
                        break;
                    case FAIL:
                        Tr.error(tcContainer, "NAMESPACE_IN_LOCAL_JNDI_BINDING_NAME_CNTR0340W",
                                 new Object[] { bmd.enterpriseBeanName,
                                                bmd._moduleMetaData.ivName,
                                                providedBinding });
                        throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                            " bean or home in the " + bmd._moduleMetaData.ivName +
                                                            " module contains a namespace in the string value for the" +
                                                            " Java Naming and Directory Interface (JNDI) binding name.");
                    case IGNORE:
                    default:
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tcContainer, "Ignoring custom binding configuration for" + bmd.enterpriseBeanName + "in " + bmd._moduleMetaData.ivName
                                                  + ". namespace in local-home binding.");
                        break;
                }
            }
        }

        if (bmd.remoteHomeJndiBindingName != null) {
            if (bmd.homeInterfaceClassName == null) {
                Tr.error(tcContainer, "JNDI_BINDING_HAS_NO_CORRESPONDING_HOME_INTERFACE_CLASS_CNTR0141E",
                         new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                    " bean in the " + bmd._moduleMetaData.ivName +
                                                    " module has specified a home Java Naming and Directory Interface (JNDI)" +
                                                    " binding. The binding does not have a matching home interface class.");
            }

            if (bmd.remoteHomeJndiBindingName.startsWith(EJBLOCAL_BINDING_PREFIX)) {
                Tr.error(tcContainer, "IMPROPER_REMOTE_JNDI_BINDING_PREFIX_CNTR0137E",
                         new Object[] { bmd.enterpriseBeanName,
                                        bmd._moduleMetaData.ivName,
                                        bmd.remoteHomeJndiBindingName });
                throw new EJBConfigurationException("The specific Java Naming and Directory Interface (JNDI)" +
                                                    " binding name that is provided for a remote" +
                                                    " home begins with ejblocal:.  The " + bmd.remoteHomeJndiBindingName +
                                                    " remote binding name that is specified for the home of " + bmd.enterpriseBeanName +
                                                    " bean in the " + bmd._moduleMetaData.ivName +
                                                    " module cannot begin with ejblocal:.");
            }
            if (bmd.remoteHomeJndiBindingName.trim().length() == 0) {
                bmd.remoteHomeJndiBindingName = null;
                OnError onError = ContainerProperties.customBindingsOnErr;
                switch (onError) {
                    case WARN:
                        Tr.warning(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                   new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                        break;
                    case FAIL:
                        Tr.error(tcContainer, "BLANK_JNDI_BINDING_NAME_CNTR0138E",
                                 new Object[] { bmd.enterpriseBeanName, bmd._moduleMetaData.ivName });
                        throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                            " bean or home in the " + bmd._moduleMetaData.ivName +
                                                            " module contains a blank string value for the" +
                                                            " Java Naming and Directory Interface (JNDI) binding name.");
                    case IGNORE:
                    default:
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tcContainer,
                                     "Ignoring custom binding configuration for" + bmd.enterpriseBeanName + "in " + bmd._moduleMetaData.ivName + ". blank in remote-home binding.");
                        break;
                }
            } else if (bmd.remoteHomeJndiBindingName.contains(":")) {
                String providedBinding = bmd.remoteHomeJndiBindingName;
                bmd.remoteHomeJndiBindingName = null;
                OnError onError = ContainerProperties.customBindingsOnErr;
                switch (onError) {
                    case WARN:
                        Tr.warning(tcContainer, "NAMESPACE_IN_JNDI_BINDING_NAME_CNTR0339W",
                                   new Object[] { bmd.enterpriseBeanName,
                                                  bmd._moduleMetaData.ivName,
                                                  providedBinding });
                        break;
                    case FAIL:
                        Tr.error(tcContainer, "NAMESPACE_IN_JNDI_BINDING_NAME_CNTR0339W",
                                 new Object[] { bmd.enterpriseBeanName,
                                                bmd._moduleMetaData.ivName,
                                                providedBinding });
                        throw new EJBConfigurationException("The " + bmd.enterpriseBeanName +
                                                            " bean or home in the " + bmd._moduleMetaData.ivName +
                                                            " module contains a namespace in the string value for the" +
                                                            " Java Naming and Directory Interface (JNDI) binding name.");
                    case IGNORE:
                    default:
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tcContainer, "Ignoring custom binding configuration for" + bmd.enterpriseBeanName + "in " + bmd._moduleMetaData.ivName
                                                  + ". namespace in remote-home binding.");
                        break;
                }
            }
        }
    }

    private void processMessageDestinationBindings(EJBModuleMetaDataImpl mmd, EJBJarBnd ejbJarBinding) // F743-36290.1
    {
        List<MessageDestination> mdBindings = ejbJarBinding.getMessageDestinations();

        if (!mdBindings.isEmpty()) {
            for (MessageDestination mdBinding : mdBindings) {
                String name = mdBinding.getName();
                String bindingName = mdBinding.getBindingName();
                mmd.ivMessageDestinationBindingMap.put(name, bindingName);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "message destination bindings = " + mmd.ivMessageDestinationBindingMap);
        }
    }

    @Override
    protected void setActivationLoadPolicy(BeanMetaData bmd) {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        WCCMMetaDataImpl wccm = getWASWCCMMetaData(bmd);

        if (wccm.enterpriseBeanExtension != null) {
            BeanCache beanCache = wccm.enterpriseBeanExtension.getBeanCache();
            if (wccm.enterpriseBeanExtension instanceof Session) {
                if (beanCache != null) {
                    BeanCache.ActivationPolicyTypeEnum activationPolicy = beanCache.getActivationPolicy();
                    if (!bmd.passivationCapable && activationPolicy != BeanCache.ActivationPolicyTypeEnum.ONCE) {
                        Tr.warning(tcContainer, "ACTIVATION_POLICY_IGNORED_NOT_PASSIVATION_CAPABLE_CNTR0332W",
                                   new Object[] { activationPolicy,
                                                  bmd.enterpriseBeanName,
                                                  bmd._moduleMetaData.ivName,
                                                  bmd._moduleMetaData.ivAppName });

                        activationPolicy = BeanCache.ActivationPolicyTypeEnum.ONCE;
                    }

                    switch (activationPolicy) {
                        case TRANSACTION:
                            if (isTraceOn && tc.isDebugEnabled()) {
                                Tr.debug(tc, "SessionExtension.getActivateAt is TRANSACTION");
                            }
                            bmd.sessionActivateTran = true;
                            bmd.sessionActivateSession = false;
                            bmd.activationPolicy = BeanMetaData.ACTIVATION_POLICY_TRANSACTION;
                            break;

                        case ACTIVITY_SESSION:
                            if (isTraceOn && tc.isDebugEnabled()) {
                                Tr.debug(tc, "SessionExtension.getActivateAt is ACTIVITY_SESSION");
                            }
                            bmd.sessionActivateTran = false;
                            bmd.sessionActivateSession = true;
                            bmd.activationPolicy = BeanMetaData.ACTIVATION_POLICY_ACTIVITY_SESSION;
                            break;

                        case ONCE:
                            if (bmd.ivSFSBFailover) {
                                if (isTraceOn && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "SFSB failover enabled, overriding to TRANSACTION");
                                }
                                bmd.sessionActivateTran = true;
                                bmd.sessionActivateSession = false;
                                bmd.activationPolicy = BeanMetaData.ACTIVATION_POLICY_TRANSACTION;
                            } else {
                                if (isTraceOn && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "SLSB or sfsb failover disabled, using ONCE");
                                }
                                bmd.sessionActivateTran = false;
                                bmd.sessionActivateSession = false;
                                bmd.activationPolicy = BeanMetaData.ACTIVATION_POLICY_ONCE;
                            }

                    } //end switch
                } else { //enforce default of ONCE values for session beans d164025.1
                    // LIDB2018-1 force activate at TRAN if SFSB failover
                    // is enabled for this SessionBean.
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Session.BeanCache is not set, so EJB container is using its default value.");
                    }
                    if (bmd.ivSFSBFailover) {
                        bmd.sessionActivateTran = true;
                        bmd.sessionActivateSession = false;
                        bmd.activationPolicy = BeanMetaData.ACTIVATION_POLICY_TRANSACTION;
                    } else {
                        bmd.sessionActivateTran = false;
                        bmd.sessionActivateSession = false;
                        bmd.activationPolicy = BeanMetaData.ACTIVATION_POLICY_ONCE;
                    }
                }
            }
        } else {
            // Otherwise, we may have a bean with no extensions defined
            // Assign good defaults.
            if (isTraceOn && tc.isDebugEnabled()) {
                Tr.debug(tc, "bmd.wccm.enterpriseBeanExtension is null, assigning default for activation policy");
            }
            if (bmd.type == InternalConstants.TYPE_SINGLETON_SESSION ||
                bmd.type == InternalConstants.TYPE_STATELESS_SESSION ||
                bmd.type == InternalConstants.TYPE_MESSAGE_DRIVEN ||
                bmd.type == InternalConstants.TYPE_MANAGED_BEAN) {
                bmd.sessionActivateTran = false;
                bmd.sessionActivateSession = false;
                bmd.activationPolicy = BeanMetaData.ACTIVATION_POLICY_ONCE;
                if (isTraceOn && tc.isDebugEnabled()) {
                    Tr.debug(tc, "defaulting SGLSB SLSB MB or MDB to ActivationPolicyTypeEnum.ONCE");
                }

            } else if (bmd.type == InternalConstants.TYPE_STATEFUL_SESSION) {

                if (bmd.ivSFSBFailover) {
                    bmd.sessionActivateTran = true;
                    bmd.sessionActivateSession = false;
                    bmd.activationPolicy = BeanMetaData.ACTIVATION_POLICY_TRANSACTION;
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "defaulting SFSB to ActivationPolicyTypeEnum.TRANSACTION since SFSB failover enabled.");
                    }
                } else {
                    bmd.sessionActivateTran = false;
                    bmd.sessionActivateSession = false;
                    bmd.activationPolicy = BeanMetaData.ACTIVATION_POLICY_ONCE;
                    if (isTraceOn && tc.isDebugEnabled()) {
                        Tr.debug(tc, "defaulting SFSB to ActivationPolicyTypeEnum.ONCE");
                    }
                }
            }

        } // no bean extensions defined in jar's extensions xml file
    }

    @Override
    protected void setConcurrencyControl(BeanMetaData bmd) {
        // Entity not supported.  Ignore silently.
    }

    @Override
    protected void checkPinPolicy(BeanMetaData bmd) {
        // Pin policy not supported.  Ignore silently.
    }

    @Override
    protected void getIsolationLevels(int[] isoLevels,
                                      int type,
                                      String[] methodNames,
                                      Class<?>[][] methodParamTypes,
                                      List<?> isoLevelList,
                                      EnterpriseBean enterpriseBean) {
        // Entity not supported.  Ignore silently.
    }

    @Override
    protected void getReadOnlyAttributes(boolean[] readOnlyAttrs,
                                         int type,
                                         String[] methodNames,
                                         Class<?>[][] methodParamTypes,
                                         List<?> accessIntentList,
                                         EnterpriseBean enterpriseBean) {
        // Entity not supported.  Ignore silently.
    }

    @Override
    public void processGeneralizations(EJBModuleConfigData moduleConfig, EJBModuleMetaDataImpl mmd) {
        // Entity not supported.  Ignore silently.
    }

    @Override
    protected String getFailoverInstanceId(EJBModuleMetaDataImpl mmd, SfFailoverCache sfDRSCache) {
        throw new UnsupportedOperationException("stateful failover not supported");
    }

    @Override
    protected boolean getSFSBFailover(EJBModuleMetaDataImpl mmd, EJSContainer container) {
        throw new UnsupportedOperationException("stateful failover not supported");
    }

    @Override
    protected void processEJBExtensionsMetadata(BeanMetaData bmd) {
        // Entity not supported.  Ignore silently.
    }

    private ComponentNameSpaceConfiguration finishBMDInitForReferenceContextPrivileged(final BeanMetaData bmd,
                                                                                       final String defaultCnrJndiName,
                                                                                       final WSEJBHandlerResolver wsHandlerResolver) throws ContainerException, EJBConfigurationException {
        return super.finishBMDInitForReferenceContext(bmd, defaultCnrJndiName, wsHandlerResolver);
    }

    @Override
    @FFDCIgnore(PrivilegedActionException.class)
    public ComponentNameSpaceConfiguration finishBMDInitForReferenceContext(final BeanMetaData bmd,
                                                                            final String defaultCnrJndiName,
                                                                            final WSEJBHandlerResolver wsHandlerResolver) throws ContainerException, EJBConfigurationException {
        ComponentNameSpaceConfiguration cnsc;
        try {
            cnsc = AccessController.doPrivileged(new PrivilegedExceptionAction<ComponentNameSpaceConfiguration>() {
                @Override
                public ComponentNameSpaceConfiguration run() throws ContainerException, EJBConfigurationException {
                    return finishBMDInitForReferenceContextPrivileged(bmd, defaultCnrJndiName, wsHandlerResolver);
                }
            });
        } catch (PrivilegedActionException paex) {
            Throwable cause = paex.getCause();
            if (cause instanceof ContainerException) {
                throw (ContainerException) cause;
            } else if (cause instanceof EJBConfigurationException) {
                throw (EJBConfigurationException) cause;
            }
            throw new Error(cause);
        }

        // handle extension processing for RunAs.
        WCCMMetaDataImpl wccm = getWASWCCMMetaData(bmd);
        if (wccm != null && wccm.enterpriseBeanExtension != null) {
            List<RunAsMode> runAsModes = wccm.enterpriseBeanExtension.getRunAsModes();
            if (!runAsModes.isEmpty()) {
                processRunAsExtension(bmd, runAsModes);
            }
        }
        return cnsc;
    }

    @Override
    protected void processReferenceContext(BeanMetaData bmd) throws InjectionException, EJBConfigurationException {
        if (getOSGiEJBModuleMetaDataImpl(bmd._moduleMetaData).isSystemModule()) {
            // Without proper MetaData, injection processing does not work.
            // However, we still need to do all the metadata processing that
            // leads up to a ReferenceContext being create.
            AbstractEJBRuntime runtime = (AbstractEJBRuntime) bmd.container.getEJBRuntime();
            try {
                runtime.finishBMDInitForReferenceContext(bmd);
            } catch (ContainerException e) {
                throw new InjectionException(e);
            }
        } else {
            super.processReferenceContext(bmd);
        }
    }

    private void processRunAsExtension(BeanMetaData bmd, List<RunAsMode> runAsModes) {
        EJBMethodInterface[] mtypes = EJBMethodInterface.values();
        for (int j = 0; j < mtypes.length; ++j) {
            List<EJBMethodMetaData> ejbMMDs = bmd.getEJBMethodMetaData(mtypes[j]);
            if (ejbMMDs != null && ejbMMDs.size() > 0) {
                int[] highestStyleOnMethod = new int[ejbMMDs.size()];
                for (RunAsMode ram : runAsModes) {
                    for (Method ramMeth : ram.getMethods()) {

                        String meName = ramMeth.getName().trim();
                        Method.MethodTypeEnum meType = ramMeth.getType();

                        if (meName.equals("*")) {
                            if (meType == Method.MethodTypeEnum.UNSPECIFIED) {
                                for (int i = 0; i < ejbMMDs.size(); i++) {
                                    OSGiEJBMethodMetaDataImpl methodMD = (OSGiEJBMethodMetaDataImpl) ejbMMDs.get(i);
                                    if (highestStyleOnMethod[i] <= 1) {
                                        setRunAsExtension(methodMD, ram);
                                        highestStyleOnMethod[i] = 1;
                                    }
                                }
                            } else if (methodTypeMatches(meType, mtypes[j])) {
                                for (int i = 0; i < ejbMMDs.size(); i++) {
                                    OSGiEJBMethodMetaDataImpl methodMD = (OSGiEJBMethodMetaDataImpl) ejbMMDs.get(i);
                                    if (highestStyleOnMethod[i] <= 2) {
                                        setRunAsExtension(methodMD, ram);
                                        highestStyleOnMethod[i] = 2;
                                    }
                                }
                            }
                        } else if (meType == Method.MethodTypeEnum.UNSPECIFIED || methodTypeMatches(meType, mtypes[j])) {
                            for (int i = 0; i < ejbMMDs.size(); i++) {
                                OSGiEJBMethodMetaDataImpl methodMD = (OSGiEJBMethodMetaDataImpl) ejbMMDs.get(i);
                                if (methodMD.getMethodName().equals(meName)) {
                                    String meParams = ramMeth.getParams();
                                    if (meParams == null) { // we must treat meParams.isEmpty() as a valid signature.
                                        if (highestStyleOnMethod[i] <= 3) {
                                            setRunAsExtension(methodMD, ram);
                                            highestStyleOnMethod[i] = 3;
                                        }
                                    } else if (methodSignatureMatches(meParams, methodMD)) {
                                        if (highestStyleOnMethod[i] <= 4) {
                                            setRunAsExtension(methodMD, ram);
                                            highestStyleOnMethod[i] = 4;
                                        }
                                    }
                                }
                            }
                        }
                    } // end for RunAsMode Methods
                } // end for RunAsModes
            } // end if there are methods of type interface
        } // end Method Interface for loop
    }

    private boolean methodSignatureMatches(String runAsParams, OSGiEJBMethodMetaDataImpl methodMD) {
        if (methodMD.getMethod() == null)
            return false;

        Class<?> ejbParams[] = methodMD.getMethod().getParameterTypes();
        // Use StringTokenizer that splits on default whitespace and comma, as in tWAS MethodElementImpl.getMethodParams
        StringTokenizer c = new StringTokenizer(runAsParams, " ,\n\r\t\f");

        if (ejbParams.length != c.countTokens())
            return false;

        for (int i = 0; i < ejbParams.length; i++) {
            if (!ejbParams[i].getName().equals(c.nextToken()))
                return false;
        }

        return true;
    }

    private void setRunAsExtension(OSGiEJBMethodMetaDataImpl methodMD, RunAsMode ram) {
        // Setting one type will automatically unset the other two.
        switch (ram.getModeType()) {
            case SPECIFIED_IDENTITY:
                String role = null; // null is a valid role - it causes the Bean Level RunAsID to be used.
                if (ram.getSpecifiedIdentity() != null) {
                    role = ram.getSpecifiedIdentity().getRole();
                    if (role.trim().isEmpty()) {
                        role = null;
                    }
                }
                methodMD.setRunAs(role);
                return;
            case CALLER_IDENTITY:
                methodMD.setUseCallerPrincipal();
                return;
            case SYSTEM_IDENTITY:
                methodMD.setUseSystemPrincipal();
                return;
            default:
                throw new IllegalArgumentException(ram.getModeType().toString());
        }
    }

    private boolean methodTypeMatches(Method.MethodTypeEnum extType, EJBMethodInterface intfType) {
        switch (extType) {
            case REMOTE:
                return intfType == EJBMethodInterface.REMOTE;
            case HOME:
                return intfType == EJBMethodInterface.HOME;
            case LOCAL:
                return intfType == EJBMethodInterface.LOCAL || intfType == EJBMethodInterface.MESSAGE_ENDPOINT;
            case LOCAL_HOME:
                return intfType == EJBMethodInterface.LOCAL_HOME;
            case SERVICE_ENDPOINT:
                return intfType == EJBMethodInterface.SERVICE_ENDPOINT;
            default:
                throw new IllegalArgumentException(extType.toString());
        }
    }

    @Override
    protected boolean processSessionExtensionTimeout(BeanMetaData bmd) {
        WCCMMetaDataImpl wccm = getWASWCCMMetaData(bmd);
        if (wccm.enterpriseBeanExtension instanceof Session) {
            Session sessExt = (Session) wccm.enterpriseBeanExtension;
            if (sessExt.getTimeOut() != null) {
                bmd.sessionTimeout = sessExt.getTimeOut().getValue() * 1000;
                return true;
            }
        }
        return false;
    }

    @Override
    protected void processSessionExtension(BeanMetaData bmd) {
        // Entity not supported.  Ignore silently.
        // NOTE: lightweight not supported
    }

    @Override
    protected void processEntityExtension(BeanMetaData bmd) {
        // Entity not supported.  Ignore silently.
    }

    @Override
    protected void processZOSMetadata(BeanMetaData bmd) {
        // Ignore.
    }

    @Override
    protected ManagedObjectService getManagedObjectService() throws EJBConfigurationException {
        try {
            return managedObjectServiceRef.getServiceWithException();
        } catch (IllegalStateException e) {
            throw new EJBConfigurationException(e);
        }
    }

    @Override
    protected void populateBindings(BeanMetaData bmd,
                                    Map<JNDIEnvironmentRefType, Map<String, String>> allBindings,
                                    Map<String, String> envEntryValues,
                                    ResourceRefConfigList resRefList) throws EJBConfigurationException {
        WCCMMetaDataImpl wccm = getWASWCCMMetaData(bmd);

        RefBindingsGroup beanBinding = wccm.refBindingsGroup;
        com.ibm.ws.javaee.dd.ejbext.EnterpriseBean ejbExtension = wccm.enterpriseBeanExtension;
        List<com.ibm.ws.javaee.dd.commonext.ResourceRef> resRefExts = ejbExtension == null ? null : ejbExtension.getResourceRefs();
        Map<String, com.ibm.ws.javaee.dd.commonbnd.Interceptor> interceptorBindings = wccm.interceptorBindings;
        OSGiJNDIEnvironmentRefBindingHelper.processBndAndExt(allBindings, envEntryValues, resRefList, beanBinding, resRefExts);

        // ----------------------------------------------------------------------
        // Do additional interceptor work
        //
        // Determine if an InterceptorMetaData object was created for this EJB.
        // If so, then we need to update bindings maps for each interceptor class.
        // ----------------------------------------------------------------------

        InterceptorMetaData imd = bmd.ivInterceptorMetaData;

        if (imd != null) {
            // There are interceptor methods, which might be just in the EJB itself.
            // So check if there are any interceptor classes.
            Class<?> interceptorClasses[] = imd.ivInterceptorClasses;
            if (interceptorClasses != null) {
                if (interceptorBindings != null) {
                    for (Class<?> interceptorClass : interceptorClasses) {
                        String interceptorClassName = interceptorClass.getName();
                        com.ibm.ws.javaee.dd.commonbnd.Interceptor interceptorBnd = interceptorBindings.get(interceptorClassName);

                        if (interceptorBnd != null) {
                            OSGiJNDIEnvironmentRefBindingHelper.processBndAndExt(allBindings, envEntryValues, resRefList, interceptorBnd, null);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Persister createCMP11Persister(BeanMetaData bmd, String defaultDataSourceJNDIName) throws ContainerException {
        // Not supported.
        return null;
    }

    private WCCMMetaDataImpl getWASWCCMMetaData(BeanMetaData bmd) {
        return (WCCMMetaDataImpl) bmd.wccm;
    }
}
