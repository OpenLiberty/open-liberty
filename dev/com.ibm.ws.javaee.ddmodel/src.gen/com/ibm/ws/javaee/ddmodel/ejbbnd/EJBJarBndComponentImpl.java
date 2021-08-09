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
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Component(configurationPid = "com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class EJBJarBndComponentImpl implements com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd delegate;

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "session", target = "(id=unbound)")
     protected void setSession(com.ibm.ws.javaee.dd.ejbbnd.Session value) {
          this.session.add(value);
     }

     protected void unsetSession(com.ibm.ws.javaee.dd.ejbbnd.Session value) {
          this.session.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.ejbbnd.Session> session = new ArrayList<com.ibm.ws.javaee.dd.ejbbnd.Session>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "message-driven", target = "(id=unbound)")
     protected void setMessage_driven(com.ibm.ws.javaee.dd.ejbbnd.MessageDriven value) {
          this.message_driven.add(value);
     }

     protected void unsetMessage_driven(com.ibm.ws.javaee.dd.ejbbnd.MessageDriven value) {
          this.message_driven.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.ejbbnd.MessageDriven> message_driven = new ArrayList<com.ibm.ws.javaee.dd.ejbbnd.MessageDriven>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "interceptor", target = "(id=unbound)")
     protected void setInterceptor(com.ibm.ws.javaee.dd.commonbnd.Interceptor value) {
          this.interceptor.add(value);
     }

     protected void unsetInterceptor(com.ibm.ws.javaee.dd.commonbnd.Interceptor value) {
          this.interceptor.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.commonbnd.Interceptor> interceptor = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.Interceptor>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "message-destination", target = "(id=unbound)")
     protected void setMessage_destination(com.ibm.ws.javaee.dd.commonbnd.MessageDestination value) {
          this.message_destination.add(value);
     }

     protected void unsetMessage_destination(com.ibm.ws.javaee.dd.commonbnd.MessageDestination value) {
          this.message_destination.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.commonbnd.MessageDestination> message_destination = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.MessageDestination>();

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
     public java.util.List<com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean> getEnterpriseBeans() {
          java.util.List<com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean>() : new ArrayList<com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean>(delegate.getEnterpriseBeans());
          returnValue.addAll(message_driven);
          returnValue.addAll(session);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.commonbnd.Interceptor> getInterceptors() {
          java.util.List<com.ibm.ws.javaee.dd.commonbnd.Interceptor> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.Interceptor>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.Interceptor>(delegate.getInterceptors());
          returnValue.addAll(interceptor);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.commonbnd.MessageDestination> getMessageDestinations() {
          java.util.List<com.ibm.ws.javaee.dd.commonbnd.MessageDestination> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.MessageDestination>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.MessageDestination>(delegate.getMessageDestinations());
          returnValue.addAll(message_destination);
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

     public void setDelegate(com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd delegate) {
          this.delegate = delegate;
     }
}
