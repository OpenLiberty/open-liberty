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
package com.ibm.ws.javaee.ddmodel.commonbnd;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Component(configurationPid = "com.ibm.ws.javaee.dd.commonbnd.Interceptor",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class InterceptorComponentImpl extends com.ibm.ws.javaee.ddmodel.commonbnd.RefBindingsGroupType implements com.ibm.ws.javaee.dd.commonbnd.Interceptor {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.commonbnd.Interceptor delegate;
     protected java.lang.String class_;

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "ejb-ref", target = "(id=unbound)")
     protected void setEjb_ref(com.ibm.ws.javaee.dd.commonbnd.EJBRef value) {
          this.ejb_ref.add(value);
     }

     protected void unsetEjb_ref(com.ibm.ws.javaee.dd.commonbnd.EJBRef value) {
          this.ejb_ref.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.commonbnd.EJBRef> ejb_ref = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.EJBRef>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "resource-ref", target = "(id=unbound)")
     protected void setResource_ref(com.ibm.ws.javaee.dd.commonbnd.ResourceRef value) {
          this.resource_ref.add(value);
     }

     protected void unsetResource_ref(com.ibm.ws.javaee.dd.commonbnd.ResourceRef value) {
          this.resource_ref.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.commonbnd.ResourceRef> resource_ref = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.ResourceRef>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "resource-env-ref", target = "(id=unbound)")
     protected void setResource_env_ref(com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef value) {
          this.resource_env_ref.add(value);
     }

     protected void unsetResource_env_ref(com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef value) {
          this.resource_env_ref.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef> resource_env_ref = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "message-destination-ref", target = "(id=unbound)")
     protected void setMessage_destination_ref(com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef value) {
          this.message_destination_ref.add(value);
     }

     protected void unsetMessage_destination_ref(com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef value) {
          this.message_destination_ref.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef> message_destination_ref = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "data-source", target = "(id=unbound)")
     protected void setData_source(com.ibm.ws.javaee.dd.commonbnd.DataSource value) {
          this.data_source.add(value);
     }

     protected void unsetData_source(com.ibm.ws.javaee.dd.commonbnd.DataSource value) {
          this.data_source.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.commonbnd.DataSource> data_source = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.DataSource>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "env-entry", target = "(id=unbound)")
     protected void setEnv_entry(com.ibm.ws.javaee.dd.commonbnd.EnvEntry value) {
          this.env_entry.add(value);
     }

     protected void unsetEnv_entry(com.ibm.ws.javaee.dd.commonbnd.EnvEntry value) {
          this.env_entry.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.commonbnd.EnvEntry> env_entry = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.EnvEntry>();

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          class_ = (java.lang.String) config.get("class");
     }

     @Override
     public java.lang.String getClassName() {
          if (delegate == null) {
               return class_ == null ? null : class_;
          } else {
               return class_ == null ? delegate.getClassName() : class_;
          }
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.commonbnd.EJBRef> getEJBRefs() {
          java.util.List<com.ibm.ws.javaee.dd.commonbnd.EJBRef> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.EJBRef>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.EJBRef>(delegate.getEJBRefs());
          returnValue.addAll(ejb_ref);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.commonbnd.ResourceRef> getResourceRefs() {
          java.util.List<com.ibm.ws.javaee.dd.commonbnd.ResourceRef> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.ResourceRef>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.ResourceRef>(delegate.getResourceRefs());
          returnValue.addAll(resource_ref);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef> getResourceEnvRefs() {
          java.util.List<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef>(delegate.getResourceEnvRefs());
          returnValue.addAll(resource_env_ref);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef> getMessageDestinationRefs() {
          java.util.List<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef>(delegate.getMessageDestinationRefs());
          returnValue.addAll(message_destination_ref);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.commonbnd.DataSource> getDataSources() {
          java.util.List<com.ibm.ws.javaee.dd.commonbnd.DataSource> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.DataSource>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.DataSource>(delegate.getDataSources());
          returnValue.addAll(data_source);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.commonbnd.EnvEntry> getEnvEntries() {
          java.util.List<com.ibm.ws.javaee.dd.commonbnd.EnvEntry> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.EnvEntry>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.EnvEntry>(delegate.getEnvEntries());
          returnValue.addAll(env_entry);
          return returnValue;
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.commonbnd.Interceptor delegate) {
          this.delegate = delegate;
     }
}
