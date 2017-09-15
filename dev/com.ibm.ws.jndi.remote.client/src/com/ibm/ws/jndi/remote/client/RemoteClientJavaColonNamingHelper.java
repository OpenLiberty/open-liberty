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
package com.ibm.ws.jndi.remote.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.RemoteException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.clientcontainer.remote.common.ClientSupport;
import com.ibm.ws.clientcontainer.remote.common.ClientSupportFactory;
import com.ibm.ws.container.service.naming.JavaColonNamingHelper;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.container.service.naming.RemoteObjectInstance;
import com.ibm.ws.container.service.naming.RemoteObjectInstanceException;
import com.ibm.ws.container.service.naming.RemoteReferenceObjectInstanceImpl;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.serialization.DeserializationContext;
import com.ibm.ws.serialization.SerializationService;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.wsspi.injectionengine.ObjectFactoryInfo;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 *
 */
@Component(property = { "service.ranking:Integer=-1000" })
public class RemoteClientJavaColonNamingHelper implements JavaColonNamingHelper {

    private final static String REFERENCE_OBJECT_FACTORIES = "objectFactories";
    private SerializationService serializationService;
    private ClientSupportFactory clientSupportFactory;

    private final Map<String, ObjectFactoryInfo> objectFactoryInfos = new ConcurrentHashMap<String, ObjectFactoryInfo>();
    private final ConcurrentServiceReferenceMap<String, ObjectFactory> objectFactories =
                    new ConcurrentServiceReferenceMap<String, ObjectFactory>(REFERENCE_OBJECT_FACTORIES);

    @Activate
    protected void activate(ComponentContext cc) throws Exception {
        objectFactories.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        objectFactories.deactivate(cc);
    }

    @Reference
    protected void setSerializationService(SerializationService ss) {
        serializationService = ss;
    }

    protected void unsetSerializationService(SerializationService ss) {
        if (serializationService == ss)
            serializationService = null;
    }

    @Reference
    protected void setClientSupportFactory(ClientSupportFactory clientSupportFactory) {
        this.clientSupportFactory = clientSupportFactory;
    }

    protected void unsetClientSupportFactory(ClientSupportFactory clientSupportFactory) {
        this.clientSupportFactory = null;
    }

    @Reference(name = REFERENCE_OBJECT_FACTORIES, service = ObjectFactory.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addObjectFactory(ServiceReference<ObjectFactory> factorySR) {
        Object o = factorySR.getProperty(Constants.OBJECTCLASS);
        String[] implNames;
        if (o instanceof String) {
            implNames = new String[] { (String) o };
        } else {
            implNames = (String[]) o;
        }

        for (String implName : implNames) {
            objectFactories.putReference(implName, factorySR);
        }

    }

    protected void removeObjectFactory(ServiceReference<ObjectFactory> factorySR) {
        Object o = factorySR.getProperty(Constants.OBJECTCLASS);
        String[] implNames;
        if (o instanceof String) {
            implNames = new String[] { (String) o };
        } else {
            implNames = (String[]) o;
        }

        for (String implName : implNames) {
            objectFactories.removeReference(implName, factorySR);
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addObjectFactoryInfo(ObjectFactoryInfo factoryInfo) {
        String factoryClassName = factoryInfo.getObjectFactoryClass().getName();
        objectFactoryInfos.put(factoryClassName, factoryInfo);
    }

    protected void removeObjectFactoryInfo(ObjectFactoryInfo factoryInfo) {
        String factoryClassName = factoryInfo.getObjectFactoryClass().getName();
        objectFactoryInfos.remove(factoryClassName);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.naming.JavaColonNamingHelper#getObjectInstance(com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace, java.lang.String)
     */
    @Override
    public Object getObjectInstance(JavaColonNamespace namespace, String name) throws NamingException {

        Object objectInstance = null;

        // java:app and java:global are the only namespaces that we could expect to find when
        // checking the server, so we'll only check those:
        if (JavaColonNamespace.APP.equals(namespace) || JavaColonNamespace.GLOBAL.equals(namespace)) {
            // ok, so either we have never looked it up before, or if we did it wasn't cacheable
            // so we must do the lookup on the server
            ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
            J2EEName j2eeName = cmd.getJ2EEName();
            RemoteObjectInstance roi = null;
            try {
                ClientSupport jndiHelper = clientSupportFactory.getRemoteClientSupport();
                roi = jndiHelper.getRemoteObjectInstance(j2eeName.getApplication(), j2eeName.getModule(), j2eeName.getComponent(), namespace.toString(), name);
            } catch (RemoteException ex) {
                //ffdc
                throw new NamingException("Failed remote lookup of " + namespace.prefix() + name);
            }

            if (roi != null) {
                try {
                    objectInstance = roi.getObject();
                    if (roi instanceof RemoteReferenceObjectInstanceImpl) {
                        // need to deserialize remote object byte array into javax.naming.Reference and re-construct
                        ObjectInputStream ois = null;
                        try {
                            byte[] bytes = (byte[]) objectInstance;
                            ClassLoader classLoader = AccessController.doPrivileged(new GetCL("this"));
                            DeserializationContext dc = serializationService.createDeserializationContext();
                            ois = dc.createObjectInputStream(new ByteArrayInputStream(bytes), classLoader);
                            objectInstance = ois.readObject();

                            if (objectInstance == null || !(objectInstance instanceof javax.naming.Reference)) {
                                throw new NamingException("Invalid reference returned from server");
                            }
                            javax.naming.Reference ref = (javax.naming.Reference) objectInstance;
                            String factoryClassName = ref.getFactoryClassName();
                            ObjectFactory objectFactory = null;
                            ObjectFactoryInfo info = objectFactoryInfos.get(factoryClassName);
                            if (info != null) {
                                objectFactory = info.getObjectFactoryClass().newInstance();
                            } else {
                                objectFactory = objectFactories.getServiceWithException(factoryClassName);
                            }
                            objectInstance = objectFactory.getObjectInstance(ref, null, null, null);
                            if (objectInstance == null) {
                                // should never happen
                                throw new NamingException("Could not object instance from object factory " + objectFactory);
                            }

                        } catch (Exception ex) {
                            NamingException ne = new NamingException("Failure occurred while reconstructing remote reference object");
                            ne.initCause(ex);
                            throw ne;
                        } finally {
                            if (ois != null)
                                try {
                                    ois.close();
                                } catch (IOException ex) {
                                }
                        }

                    }

                } catch (RemoteObjectInstanceException ex) {
                    //ffdc
                    NamingException ne = new NamingException();
                    ne.initCause(ex);
                    throw ne;
                }
            }
        }
        return objectInstance;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.naming.JavaColonNamingHelper#hasObjectWithPrefix(com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace, java.lang.String)
     */
    @Override
    public boolean hasObjectWithPrefix(JavaColonNamespace namespace, String name) throws NamingException {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        J2EEName j2eeName = cmd.getJ2EEName();

        boolean b;
        try {
            b = (JavaColonNamespace.APP.equals(namespace) || JavaColonNamespace.GLOBAL.equals(namespace)) &&
                clientSupportFactory.getRemoteClientSupport().hasRemoteObjectWithPrefix(j2eeName.getApplication(), j2eeName.getModule(), j2eeName.getComponent(), namespace.toString(), name);
        } catch (RemoteException ex) {
            //ffdc
            b = false;
        }
        return b;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.container.service.naming.JavaColonNamingHelper#listInstances(com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace, java.lang.String)
     */
    @Override
    public Collection<? extends NameClassPair> listInstances(JavaColonNamespace namespace, String nameInContext) throws NamingException {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        J2EEName j2eeName = cmd.getJ2EEName();
        Collection<? extends NameClassPair> instances;
        if (JavaColonNamespace.APP.equals(namespace) || JavaColonNamespace.GLOBAL.equals(namespace)) {
            try {
                ClientSupport jndiHelper = clientSupportFactory.getRemoteClientSupport();
                instances = jndiHelper.listRemoteInstances(j2eeName.getApplication(), j2eeName.getModule(), j2eeName.getComponent(), namespace.toString(), nameInContext);
            } catch (RemoteException ex) {
                //ffdc
                instances = Collections.emptyList();
            }
        } else {
            instances = Collections.emptyList();
        }
        return instances;
    }

    private static class GetCL implements PrivilegedAction<ClassLoader> {
        String whichLoader;

        GetCL(String which) {
            whichLoader = which;
        }

        @Override
        public ClassLoader run() {
            if ("this".equals(whichLoader)) {
                return RemoteClientJavaColonNamingHelper.class.getClassLoader();
            }
            return Thread.currentThread().getContextClassLoader();
        }
    }
}
