/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.managedbeans.component;

import java.lang.reflect.Method;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.Application;
import javax.annotation.ManagedBean;

import org.apache.cxf.message.Message;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.api.JaxRsFactoryBeanCustomizer;
import com.ibm.ws.jaxrs20.metadata.EndpointInfo;
import com.ibm.ws.jaxrs20.metadata.JaxRsModuleMetaData;
import com.ibm.ws.jaxrs20.metadata.ProviderResourceInfo;
import com.ibm.ws.jaxrs20.metadata.ProviderResourceInfo.RuntimeType;


@Component(name = "com.ibm.ws.jaxrs20.managedbeans.component.JaxRsFactoryBeanManagedBeansCustomizer", immediate = true, property = { "service.vendor=IBM" })
public class JaxRsFactoryBeanManagedBeansCustomizer implements JaxRsFactoryBeanCustomizer  {

    private final TraceComponent tc = Tr
            .register(JaxRsFactoryBeanManagedBeansCustomizer.class);
	
	@Override
	public Priority getPriority() {
		// TODO Auto-generated method stub
		return Priority.Low;
	}

	@Override
	public Application onApplicationInit(Application app, JaxRsModuleMetaData metaData) {
		// TODO Auto-generated method stub
		Application newApp = null;
		try{
			newApp = (Application) getManagedBean(app.getClass());
		}catch(Throwable e){
			
			return app;
		}
		return newApp;
	}

	@Override
	public void onPrepareProviderResource(BeanCustomizerContext context) {
		// TODO Auto-generated method stub
		
		 EndpointInfo endpointInfo = context.getEndpointInfo();
	        Set<ProviderResourceInfo> perRequestProviderAndPathInfos = endpointInfo.getPerRequestProviderAndPathInfos();
	        Set<ProviderResourceInfo> singletonProviderAndPathInfos = endpointInfo.getSingletonProviderAndPathInfos();
	        Map<Class<?>, Object> resourcesManagedbyManagedBean = new HashMap<Class<?>, Object>();

	        for (ProviderResourceInfo p : perRequestProviderAndPathInfos)
	        {
	        	
	            if (p.getRuntimeType() != RuntimeType.POJO)
	                continue;

	            Object managedBeanObject = getManagedBean(p.getProviderResourceClass());
	            if (managedBeanObject != null)
	            {

	                p.setRuntimeType(RuntimeType.MANAGEDBEAN);
	                resourcesManagedbyManagedBean.put(p.getProviderResourceClass(), managedBeanObject);
	            }

	        }
	        for (ProviderResourceInfo o : singletonProviderAndPathInfos)
	        {

	            if (o.getRuntimeType() != RuntimeType.POJO)
	                continue;

	            Object managedBeanObject = getManagedBean(o.getProviderResourceClass());
	            if (managedBeanObject != null)
	            {

	                o.setRuntimeType(RuntimeType.MANAGEDBEAN);
	                resourcesManagedbyManagedBean.put(o.getProviderResourceClass(), managedBeanObject);

	            }

	        }

	        context.setContextObject(resourcesManagedbyManagedBean);
	}

	private Object getManagedBean(Class<?> providerResourceClass) {
		// TODO Auto-generated method stub
		//Per spec, managedBean can be accessed by JNDI lookup
		String managedBeanName = "";
		String jndiName = "java:module/";
		if(providerResourceClass == null){
			 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
	                Tr.debug(
	                         tc,
	                         "Couldn't get managedBean instance`, will use JAX-RS instance.");
			return null;
		}
		
		try{
			ManagedBean managedBean = providerResourceClass.getAnnotation(ManagedBean.class);
			
			if(managedBean == null){
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
	                Tr.debug(
	                         tc,
	                             "Class " + providerResourceClass.getName() + "does not contain any ManagedBean annotation. " );
				return null;
			}
			if(managedBeanName != null){
				managedBeanName = managedBean.value();
			}
//			String jndiName = "java:app/ManagedBeanSampleWithName/JaxrsManagedBean";

			jndiName = jndiName + managedBeanName;
			Object managedbeanServiceObject = new InitialContext().lookup(jndiName);
			if(null!= managedbeanServiceObject){
				return managedbeanServiceObject;
			}
        } catch (NamingException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "Couldn't get managedBean instance for "
                                         + providerResourceClass.getName()
                                         + " through JNDI: " + jndiName
                                         + ", will use JAX-RS instance.");
            return null;
        } catch (Throwable e){
        	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(
                         tc,
                         "Couldn't get managedBean instance for "
                                         + providerResourceClass.getName() + "for exception " + e );
            return null;
        }
		
		return null;
	}

	@Override
	public boolean isCustomizableBean(Class<?> clazz, Object contextObject) {
		// TODO Auto-generated method stub
		if (contextObject == null) {
            return false;
        }
        @SuppressWarnings("unchecked")
        Map<Class<?>, Object> newContext = (Map<Class<?>, Object>) (contextObject);
        if (newContext.isEmpty())
        {
            return false;
        }
        if (newContext.containsKey(clazz))
        {
            return true;
        }
        return false;
	}

	@Override
	public <T> T onSingletonProviderInit(T provider, Object contextObject, Message m) {
		// TODO Auto-generated method stub
        if (contextObject == null)
        {
            return null;
        }
        Map<Class<?>, Object> newContext = (Map<Class<?>, Object>) (contextObject);
        if (newContext.isEmpty())
        {
            return null;
        }
        
        T newProvider = null;
        newProvider = (T) getManagedBean(provider.getClass());
        if (newProvider != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Provider: get Provider from ManagedBean " + provider.getClass().getName());
            }
            return newProvider;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Provider: Provider is null from managedBean , use Provider from rs for " + provider.getClass().getName());
        }

        return null;
	}

	@Override
	public <T> T onSingletonServiceInit(T service, Object contextObject) {
		// TODO Auto-generated method stub
		 if (contextObject == null)
	        {
	            return null;
	        }
	        Map<Class<?>, Object> newContext = (Map<Class<?>, Object>) (contextObject);
	        if (newContext.isEmpty())
	        {
	            return null;
	        }
	        
	        T newService = null;
	        newService = (T) getManagedBean(service.getClass());
	        if (newService != null) {
	            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	                Tr.debug(tc, "Provider: get Service from ManagedBean " + service.getClass().getName());
	            }
	            return newService;
	        }

	        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	            Tr.debug(tc, "Provider: Service is null from managedBean , use Provider from rs for " + service.getClass().getName());
	        }

	        return null;
	}

	@Override
	public <T> T beforeServiceInvoke(T serviceObject, boolean isSingleton,
			Object contextObject) {
		// TODO Auto-generated method stub
		 if (isSingleton || contextObject == null) {
	            return serviceObject;
	        }
		 Map<Class<?>, Object> newContext = (Map<Class<?>, Object>) (contextObject);
	        
		 if (newContext.isEmpty())
		 {
	            return serviceObject;
	     }

		 Object newServiceObject = null;
	        
		 Class<?> clazz = serviceObject.getClass();

		 newServiceObject = getManagedBean(clazz);
	        
		 if (newServiceObject != null) {
	
			 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
	         
				 Tr.debug(tc, "Resource: get Per Request Resource from managedBean " + clazz.getName());           
			 }
	         
			 return (T) newServiceObject;
	       
		 }

	        
		 if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			 Tr.debug(tc, "Resource: Per Request Resource is null from managedBean , use Resource from rs for " + clazz.getName());
	        
		 }
	        
		 return serviceObject;

	}

	@Override
	public Object serviceInvoke(Object serviceObject, Method m,
			Object[] params, boolean isSingleton, Object contextObject, Message msg)
			throws Exception {
		 return m.invoke(serviceObject, params);    
		
	}

	@Override
	public void afterServiceInvoke(Object serviceObject, boolean isSingleton,
			Object contextObject) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> T onSetupProviderProxy(T provider, Object contextObject) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public void destroyApplicationScopeResources(
			JaxRsModuleMetaData jaxRsModuleMetaData) {
		// TODO Auto-generated method stub
		
	}

	

}
