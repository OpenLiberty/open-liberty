/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxrs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.ws.rs.Path;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.UserResource;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.AbstractServiceFactoryBean;
import org.apache.cxf.service.factory.FactoryBeanListener.Event;
import org.apache.cxf.service.invoker.Invoker;


/**
 * Builds a JAX-RS service model from resource classes.
 */
public class JAXRSServiceFactoryBean extends AbstractServiceFactoryBean {
    
    protected List<ClassResourceInfo> classResourceInfos = 
        new ArrayList<ClassResourceInfo>();
    
    private Invoker invoker;
    private Executor executor;
    private boolean enableStatic;
    private QName serviceName;

    private Class<?> defaultModelClass;
    
    public JAXRSServiceFactoryBean() {
    }

    public Bus getBus() {
        Bus bus = super.getBus();
        return bus == null ? BusFactory.getThreadDefaultBus() : bus;
    }

    public void setBus(Bus bus) {
        if (super.getBus() == null) {
            super.setBus(bus);
        }
    }
    
    public void setServiceName(QName name) {
        this.serviceName = name;
    }
    
    public QName getServiceName() {
        return serviceName;
    }
    
    public void setEnableStaticResolution(boolean staticResolution) {
        this.enableStatic = staticResolution;
    }
    
    @Override
    public Service create() {
        sendEvent(Event.START_CREATE);

        initializeServiceModel();

        initializeDefaultInterceptors();

        if (invoker != null) {
            getService().setInvoker(getInvoker());
        } else {
            getService().setInvoker(createInvoker());
        }

        if (getExecutor() != null) {
            getService().setExecutor(getExecutor());
        }
        if (getDataBinding() != null) {
            getService().setDataBinding(getDataBinding());
        }

        Service serv = getService();
        sendEvent(Event.END_CREATE, serv);
        return serv;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }

    public List<Class<?>> getResourceClasses() {
        List<Class<?>> resourceClasses = new ArrayList<Class<?>>();
        for (ClassResourceInfo cri : classResourceInfos) {
            resourceClasses.add(cri.getResourceClass());
        }
        return resourceClasses;
    }

    public List<ClassResourceInfo> getClassResourceInfo() {
        return Collections.unmodifiableList(classResourceInfos);
    }
    
    List<ClassResourceInfo> getRealClassResourceInfo() {
        return classResourceInfos;
    }
    
    public void setResourceClass(Class<?> cls) {
        if (getCreatedFromModel(cls) == null) {
            classResourceInfos.clear();
            boolean isRoot = AnnotationUtils.getClassAnnotation(cls, Path.class) != null;
            createResourceInfo(cls, isRoot);
        }
    }
    
    public void setResourceClasses(List<Class<?>> classes) {
        for (Class<?> resourceClass : classes) {
            
            ClassResourceInfo cri = getCreatedFromModel(resourceClass);
            if (cri != null) {
                if (!InjectionUtils.isConcreteClass(cri.getServiceClass())) {
                    cri = new ClassResourceInfo(cri);
                    cri.setResourceClass(resourceClass);
                    classResourceInfos.add(cri);
                }
                continue;
            }
            
            createResourceInfo(resourceClass, true);
        }
    }
    
    public void setUserResources(List<UserResource> resources) {
        Map<String, UserResource> map = userResourcesAsMap(resources);
        for (UserResource ur : resources) {
            if (ur.getPath() != null) {
                ClassResourceInfo cri = ResourceUtils.createClassResourceInfo(map, ur, 
                                                                              defaultModelClass, 
                                                                              true, 
                                                                              enableStatic,
                                                                              getBus());
                if (cri != null) {
                    classResourceInfos.add(cri);
                }
            }
        }
    }
    
    public void setUserResourcesWithServiceClass(List<UserResource> resources, Class<?> ...sClasses) {
        Map<String, UserResource> map = userResourcesAsMap(resources);
        for (Class<?> sClass : sClasses) {
            ClassResourceInfo cri = ResourceUtils.createServiceClassResourceInfo(
                map, map.get(sClass.getName()), sClass, true, enableStatic, getBus());
            if (cri != null) {
                classResourceInfos.add(cri);
            }
        }
    }
    
    private Map<String, UserResource> userResourcesAsMap(List<UserResource> resources) {
        Map<String, UserResource> map = new HashMap<String, UserResource>();
        for (UserResource ur : resources) {
            map.put(ur.getName(), ur);
        }
        return map;
    }
    
    protected ClassResourceInfo createResourceInfo(Class<?> cls, boolean isRoot) {
        ClassResourceInfo classResourceInfo = 
            ResourceUtils.createClassResourceInfo(cls, cls, isRoot, enableStatic, getBus());
        if (classResourceInfo != null) {
            classResourceInfos.add(classResourceInfo);
        }
        return classResourceInfo;
    }

    public void setResourceClasses(Class<?>... classes) {
        setResourceClasses(Arrays.asList(classes));
    }
    
    public void setResourceClassesFromBeans(List<Object> beans) {
        for (Object bean : beans) {
            
            Class<?> realClass = ClassHelper.getRealClass(getBus(), bean);
            
            ClassResourceInfo cri = getCreatedFromModel(realClass);
            if (cri != null) {
                if (!InjectionUtils.isConcreteClass(cri.getServiceClass())) {
                    cri = new ClassResourceInfo(cri);
                    classResourceInfos.add(cri);
                }
                cri.setResourceClass(bean.getClass());
                cri.setResourceProvider(new SingletonResourceProvider(bean));
                continue;
            }
            
            cri = ResourceUtils.createClassResourceInfo(bean.getClass(), realClass, true, enableStatic,
                                                        getBus());
            if (cri != null) {
                classResourceInfos.add(cri);
                cri.setResourceProvider(
                                   new SingletonResourceProvider(bean));
            }
        }
    }
    
    private ClassResourceInfo getCreatedFromModel(Class<?> realClass) {
        sendEvent(Event.CREATE_FROM_CLASS, realClass);

        for (ClassResourceInfo cri : classResourceInfos) {
            if (cri.isCreatedFromModel() 
                && cri.isRoot() && cri.getServiceClass().isAssignableFrom(realClass)) {
                return cri;
            }
        }
        return null;
    }
    
    protected void initializeServiceModel() {
        
        JAXRSServiceImpl service = new JAXRSServiceImpl(classResourceInfos, serviceName);

        setService(service);
        sendEvent(Event.SERVICE_SET, service);
    }

    protected Invoker createInvoker() {
        return new JAXRSInvoker();
    }

    public void setService(Service service) {
        super.setService(service);
    }

    public Class<?> getDefaultModelClass() {
        return defaultModelClass;
    }

    public void setDefaultModelClass(Class<?> defaultModelClass) {
        this.defaultModelClass = defaultModelClass;
    }
}
