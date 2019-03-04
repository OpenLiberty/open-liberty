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
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.managedbean;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Component(configurationPid = "com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class ManagedBeanBndComponentImpl implements com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd delegate;

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "interceptor", target = "(id=unbound)")
     protected void setInterceptor(com.ibm.ws.javaee.dd.commonbnd.Interceptor value) {
          this.interceptor.add(value);
     }

     protected void unsetInterceptor(com.ibm.ws.javaee.dd.commonbnd.Interceptor value) {
          this.interceptor.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.commonbnd.Interceptor> interceptor = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.Interceptor>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "managed-bean", target = "(id=unbound)")
     protected void setManaged_bean(com.ibm.ws.javaee.dd.managedbean.ManagedBean value) {
          this.managed_bean.add(value);
     }

     protected void unsetManaged_bean(com.ibm.ws.javaee.dd.managedbean.ManagedBean value) {
          this.managed_bean.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.managedbean.ManagedBean> managed_bean = new ArrayList<com.ibm.ws.javaee.dd.managedbean.ManagedBean>();

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
     }

     @Override
     public java.lang.String getVersion() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? null : delegate.getVersion();
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.commonbnd.Interceptor> getInterceptors() {
          java.util.List<com.ibm.ws.javaee.dd.commonbnd.Interceptor> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.Interceptor>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.Interceptor>(delegate.getInterceptors());
          returnValue.addAll(interceptor);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.managedbean.ManagedBean> getManagedBeans() {
          java.util.List<com.ibm.ws.javaee.dd.managedbean.ManagedBean> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.managedbean.ManagedBean>() : new ArrayList<com.ibm.ws.javaee.dd.managedbean.ManagedBean>(delegate.getManagedBeans());
          returnValue.addAll(managed_bean);
          return returnValue;
     }

// Methods required to implement DeploymentDescriptor -- Not used in Liberty
    @Override
    public String getDeploymentDescriptorPath() {
        return null;
    }

    @Override
    public Object getComponentForId(String id) {
        return null;
    }

    @Override
    public String getIdForComponent(Object ddComponent) {
        return null;
    }
// End of DeploymentDescriptor Methods -- Not used in Liberty
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.managedbean.ManagedBeanBnd delegate) {
          this.delegate = delegate;
     }
}
