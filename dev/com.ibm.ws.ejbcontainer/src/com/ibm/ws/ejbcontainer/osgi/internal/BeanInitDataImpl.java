/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.Remove;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.ejbcontainer.EJBEndpoint;
import com.ibm.ws.ejbcontainer.EJBReferenceFactory;
import com.ibm.ws.ejbcontainer.EJBType;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.ejbcontainer.ManagedBeanEndpoint;
import com.ibm.ws.ejbcontainer.internal.EJBReferenceFactoryImpl;
import com.ibm.ws.ejbcontainer.osgi.BeanRuntime;
import com.ibm.ws.ejbcontainer.osgi.internal.metadata.OSGiBeanMetaData;
import com.ibm.ws.javaee.dd.ejb.NamedMethod;
import com.ibm.ws.javaee.dd.ejb.RemoveMethod;
import com.ibm.ws.javaee.dd.ejb.Session;
import com.ibm.ws.javaee.util.DDUtil;
import com.ibm.ws.metadata.ejb.BeanInitData;
import com.ibm.ws.metadata.ejb.ModuleInitData;

public class BeanInitDataImpl extends BeanInitData implements EJBEndpoint, ManagedBeanEndpoint {

    public com.ibm.ws.javaee.dd.ejbext.EnterpriseBean enterpriseBeanExt;
    public com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup beanBnd;
    public String systemHomeBindingName;

    public BeanMetaData beanMetaData;
    public BeanRuntime beanRuntime;
    private EJBReferenceFactory referenceFactory;

    public BeanInitDataImpl(String name, ModuleInitData mid) {
        super(name, mid);
    }

    @Override
    public String getName() {
        return (ivName.startsWith("$") ? null : ivName);
    }

    @Override
    public EJBType getEJBType() {
        return EJBType.forValue(ivType);
    }

    @Override
    public String getClassName() {
        return ivClassName;
    }

    @Override
    public boolean isLocalBean() {
        return ivLocalBean;
    }

    @Override
    public String getHomeInterfaceName() {
        return ivRemoteHomeInterfaceName;
    }

    @Override
    public String getRemoteInterfaceName() {
        return ivRemoteInterfaceName;
    }

    @Override
    public List<String> getLocalBusinessInterfaceNames() {
        return ivLocalBusinessInterfaceNames == null ? Collections.<String> emptyList() : Arrays.asList(ivLocalBusinessInterfaceNames);
    }

    @Override
    public List<String> getRemoteBusinessInterfaceNames() {
        return ivRemoteBusinessInterfaceNames == null ? Collections.<String> emptyList() : Arrays.asList(ivRemoteBusinessInterfaceNames);
    }

    @Override
    public boolean isWebService() {
        return ivWebServiceEndpoint || ivWebServiceEndpointInterfaceName != null;
    }

    @Override
    public J2EEName getJ2EEName() {
        return ivJ2EEName;
    }

    @Override
    public BeanMetaData createBeanMetaData() {
        return new OSGiBeanMetaData(0, beanRuntime, systemHomeBindingName);
    }

    @Override
    public List<Method> getStatefulRemoveMethods() throws EJBConfigurationException {
        if (ivType != InternalConstants.TYPE_STATEFUL_SESSION) {
            return Collections.emptyList();
        }

        // See EJBMDOrchestrator.flagSFSBRemoveMethods.  This algorithm will run
        // again before the EJB can actually be used, so it only needs to be
        // good enough to conservatively identify any method that could be a
        // remove method.  We do not need to do full error checking, and we do
        // not need to identify to which interface a method belongs.

        Set<Method> results = new LinkedHashSet<Method>();
        try {
            Class<?> klass = ivModuleInitData.ivClassLoader.loadClass(ivClassName);
            Method[] allMethods = klass.getMethods();

            if (ivEnterpriseBean != null) {
                Session session = (Session) ivEnterpriseBean;

                for (RemoveMethod removeMethod : session.getRemoveMethod()) {
                    NamedMethod namedMethod = removeMethod.getBeanMethod();
                    String namedMethodName = namedMethod.getMethodName();

                    if ("*".equals(namedMethodName)) {
                        for (Method method : allMethods) {
                            if (method.getDeclaringClass() != Object.class) {
                                results.add(method);
                            }
                        }
                    } else if (namedMethod.getMethodParamList() == null) {
                        for (Method method : allMethods) {
                            if (method.getDeclaringClass() != Object.class && method.getName().equals(namedMethodName)) {
                                results.add(method);
                            }
                        }
                    } else {
                        Method method = DDUtil.findMethod(namedMethod, allMethods);
                        if (method != null) {
                            results.add(method);
                        }
                    }
                }
            }

            if (!ivModuleInitData.ivMetadataComplete) {
                for (Method method : allMethods) {
                    if (method.isAnnotationPresent(Remove.class)) {
                        results.add(method);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new EJBConfigurationException(e);
        }

        return new ArrayList<Method>(results);
    }

    public ModuleInitDataImpl getModuleInitData() {
        return (ModuleInitDataImpl) ivModuleInitData;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.ejbcontainer.osgi.EJBEndpoint#isPassivationCapable()
     */
    @Override
    public boolean isPassivationCapable() {
        return ivPassivationCapable;
    }

    @Override
    public synchronized EJBReferenceFactory getReferenceFactory() {
        if (ivType != InternalConstants.TYPE_STATELESS_SESSION &&
            ivType != InternalConstants.TYPE_STATEFUL_SESSION &&
            ivType != InternalConstants.TYPE_SINGLETON_SESSION) {
            throw new IllegalStateException("EJB is not a session bean.");
        }

        if (referenceFactory == null) {
            referenceFactory = new EJBReferenceFactoryImpl(ivJ2EEName);
        }
        return referenceFactory;
    }
}
