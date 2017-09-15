/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;

import com.ibm.ejs.container.EJBNotFoundException;
import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.ws.ejbcontainer.EJBEndpoint;
import com.ibm.ws.ejbcontainer.EJBEndpoints;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.ManagedBeanEndpoint;
import com.ibm.ws.ejbcontainer.ManagedBeanEndpoints;
import com.ibm.ws.ejbcontainer.osgi.MDBRuntime;
import com.ibm.ws.ejbcontainer.osgi.ManagedBeanRuntime;
import com.ibm.ws.ejbcontainer.osgi.SessionBeanRuntime;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.OSGiEJBModuleMetaDataImpl;
import com.ibm.ws.javaee.dd.commonbnd.Interceptor;
import com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd;
import com.ibm.ws.javaee.dd.ejbext.EJBJarExt;
import com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd;
import com.ibm.ws.metadata.ejb.BeanInitData;
import com.ibm.ws.metadata.ejb.ModuleInitData;
import com.ibm.wsspi.adaptable.module.Container;

public class ModuleInitDataImpl extends ModuleInitData implements EJBEndpoints, ManagedBeanEndpoints {
    private final int moduleVersion;
    public final SessionBeanRuntime sessionBeanRuntime;
    public final MDBRuntime mdbRuntime;
    public final ManagedBeanRuntime managedBeanRuntime;

    public boolean systemModule;
    public Container container;

    public EJBJarExt ejbJarExtension;
    public EJBJarBnd ejbJarBinding;
    public Map<String, Interceptor> ejbJarInterceptorBindings;

    public ManagedBeanBnd managedBeanBinding;
    public Map<String, Interceptor> managedBeanInterceptorBindings;

    /**
     * The interceptors for EJBs in the module.
     */
    public Set<String> ivEJBInterceptorClassNames;

    /**
     * The interceptors for ManagedBeans in the module.
     */
    public Set<String> ivMBInterceptorClassNames;

    // Subset of ivBeans containing only EJBs (not ManagedBeans); not set when only EJBs are present.
    private List<BeanInitData> ejbs;

    public ModuleInitDataImpl(String name,
                              String appName,
                              int moduleVersion,
                              SessionBeanRuntime sessionBeanRuntime,
                              MDBRuntime mdbRuntime,
                              ManagedBeanRuntime managedBeanRuntime) {
        super(name, appName);
        this.moduleVersion = moduleVersion;
        this.sessionBeanRuntime = sessionBeanRuntime;
        this.mdbRuntime = mdbRuntime;
        this.managedBeanRuntime = managedBeanRuntime;
    }

    @Override
    public int getModuleVersion() {
        return moduleVersion;
    }

    public void addEJBInterceptor(String interceptorClassName) {
        if (ivEJBInterceptorClassNames == null) {
            ivEJBInterceptorClassNames = new HashSet<String>();
        }
        ivEJBInterceptorClassNames.add(interceptorClassName);
    }

    public void addMBInterceptor(String interceptorClassName) {
        if (ivMBInterceptorClassNames == null) {
            ivMBInterceptorClassNames = new HashSet<String>();
        }
        ivMBInterceptorClassNames.add(interceptorClassName);
    }

    @Override
    public void addBean(BeanInitData bid) {
        if (bid.ivType == InternalConstants.TYPE_MANAGED_BEAN) {
            if (ejbs == null) {
                // Add all prior beans; none could be managed
                ejbs = new ArrayList<BeanInitData>(ivBeans);
            }
        } else if (ejbs != null) {
            ejbs.add(bid);
        }

        ivBeans.add(bid);
    }

    public boolean containsManagedBeansOnly() {
        return ejbs != null && ejbs.size() == 0;
    }

    @Override
    public Set<String> getEJBInterceptorClassNames() {
        return (ivEJBInterceptorClassNames != null) ? Collections.unmodifiableSet(ivEJBInterceptorClassNames) : Collections.<String> emptySet();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<EJBEndpoint> getEJBEndpoints() {
        List<BeanInitData> beans = ejbs != null ? ejbs : ivBeans;
        return Collections.unmodifiableList((List) beans);
    }

    @Override
    public EJBModuleMetaDataImpl createModuleMetaData(EJBApplicationMetaData ejbAMD) {
        return new OSGiEJBModuleMetaDataImpl(0, ejbAMD, systemModule);
    }

    @Override
    public EJBEndpoint findEJBEndpoint(EJB annotation, Class<?> injectionType, String application, String module) throws ClassCastException, EJBNotFoundException {

        // First, the "lookup" attribute overrides all other attributes
        // of the @EJB annotation, so look for a match based on that.

        String lookupName = annotation.lookup();
        if (lookupName != null && !"".equals(lookupName)) {
            String[] names = lookupName.split("/");
            if ("java:global".equals(names[0])) {

                String lookupApp = names.length == 4 ? names[1] : null;
                if (lookupApp != null && !lookupApp.equals(ivAppName)) {
                    // not in this application
                    return null;
                }
                String lookupMod = names.length == 4 ? names[2] : names[1];
                if (!lookupMod.equals(ivLogicalName)) {
                    // not in this module
                    return null;
                }
                String beanName = names.length == 4 ? names[3] : names[2];
                return findEJBEndpoint(beanName, injectionType);

            } else if ("java:app".equals(names[0])) {

                if (ivAppName != null && !ivAppName.equals(application)) {
                    // not in this application
                    return null;
                }

                if (!names[1].equals(ivLogicalName)) {
                    // not in this module
                    return null;
                }
                return findEJBEndpoint(names[2], injectionType);

            } else if ("java:module".equals(names[0])) {

                if (ivAppName != null && !ivAppName.equals(application)) {
                    // not in this application
                    return null;
                }

                if (!ivLogicalName.equals(module)) {
                    // not in this module
                    return null;
                }
                return findEJBEndpoint(names[1], injectionType);

            }
            // Not located in this module
            return null;
        }

        // Without a lookup name, the annotation must be in the same application
        if (!ivAppName.equals(application)) {
            return null;
        }

        // Second, the "beanName" attribute has the next highest priority,
        // so look for a bean in this module with a matching bean name.

        String beanName = annotation.beanName();
        if (beanName != null && !"".equals(beanName)) {
            int forwardSlashIndex = beanName.lastIndexOf('/');
            if (forwardSlashIndex > -1) {
                // There is a forward slash:
                //      a) moduleName is the everything before the last '/' character
                //      b) beanName is everything after the last '/' character
                String logicalModule = beanName.substring(0, forwardSlashIndex);
                if (!logicalModule.equals(this.ivLogicalName)) {
                    // The bean is not in this module
                    return null;
                }
                beanName = beanName.substring(forwardSlashIndex + 1);
            }

            List<BeanInitData> beans = ejbs != null ? ejbs : ivBeans;
            for (BeanInitData bean : beans) {
                if (beanName.equals(bean.ivName)) {
                    return (EJBEndpoint) bean;
                }
            }
            return null;
        }

        // Finally, the "beanInterface" and injection type have the lowest
        // priority. And, when "beanInterface" is specified, it must be
        // type compatible with the injection type or it is just plain
        // not going to work, even if not a match in this module.

        Class<?> beanInterface = annotation.beanInterface();
        if (beanInterface != null && !beanInterface.isAssignableFrom(injectionType)) {
            throw new ClassCastException("The " + beanInterface.getName() +
                                         " EJB interface is not compatble with the " +
                                         injectionType.getName() + " injection type.");
        }

        String beanInterfaceName = (beanInterface != null) ? beanInterface.getName() : injectionType.getName();

        List<BeanInitData> beans = ejbs != null ? ejbs : ivBeans;
        for (BeanInitData bean : beans) {
            if (bean.supportsInterface(beanInterfaceName)) {
                return (EJBEndpoint) bean;
            }
        }
        return null;
    }

    private EJBEndpoint findEJBEndpoint(String beanNameAndInterface, Class<?> injectionType) throws ClassCastException, EJBNotFoundException {
        String[] beanNames = beanNameAndInterface.split("!");
        if (beanNames.length == 2 && !injectionType.getName().equals(beanNames[1])) {
            throw new ClassCastException("The " + beanNames[1] +
                                         " EJB interface is not compatble with the " +
                                         injectionType.getName() + " injection type.");
        }
        BeanInitData beanData = findBean(beanNames[0]);
        if (beanData == null) {
            throw new EJBNotFoundException("The " + beanNames[0] + " bean does not exist in the " + ivLogicalName + " module.");
        }
        if (!beanData.supportsInterface(injectionType.getName())) {
            throw new ClassCastException("The " + beanNames[0] +
                                         " EJB does not implement the " +
                                         injectionType.getName() + " injection type.");
        }
        return (EJBEndpoint) beanData;
    }

    private BeanInitData findBean(String beanName) throws ClassCastException {
        List<BeanInitData> beans = ejbs != null ? ejbs : ivBeans;
        for (BeanInitData bean : beans) {
            if (beanName.equals(bean.ivName)) {
                return bean;
            }
        }
        return null;
    }

    @Override
    public Set<String> getManagedBeanInterceptorClassNames() {
        return (ivMBInterceptorClassNames != null) ? Collections.unmodifiableSet(ivMBInterceptorClassNames) : Collections.<String> emptySet();
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<ManagedBeanEndpoint> getManagedBeanEndpoints() {
        List<BeanInitData> beans = new ArrayList<BeanInitData>();
        if (ejbs != null) { // only set if there are managed beans
            for (BeanInitData bid : ivBeans) {
                if (bid.ivType == InternalConstants.TYPE_MANAGED_BEAN) {
                    beans.add(bid);
                }
            }
        }
        return Collections.unmodifiableList((List) beans);
    }

}
