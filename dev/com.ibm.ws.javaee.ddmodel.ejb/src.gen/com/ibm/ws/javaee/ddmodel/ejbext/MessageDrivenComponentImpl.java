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
package com.ibm.ws.javaee.ddmodel.ejbext;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Component(configurationPid = "com.ibm.ws.javaee.dd.ejbext.MessageDriven",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class MessageDrivenComponentImpl extends com.ibm.ws.javaee.ddmodel.ejbext.EnterpriseBeanType implements com.ibm.ws.javaee.dd.ejbext.MessageDriven {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.ejbext.MessageDriven delegate;

     @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, name = "bean-cache", target = "(id=unbound)")
     protected volatile com.ibm.ws.javaee.dd.ejbext.BeanCache bean_cache;

     @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, name = "local-transaction", target = "(id=unbound)")
     protected volatile com.ibm.ws.javaee.dd.commonext.LocalTransaction local_transaction;

     @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, name = "global-transaction", target = "(id=unbound)")
     protected volatile com.ibm.ws.javaee.dd.commonext.GlobalTransaction global_transaction;

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "resource-ref", target = "(id=unbound)")
     protected void setResource_ref(com.ibm.ws.javaee.dd.commonext.ResourceRef value) {
          this.resource_ref.add(value);
     }

     protected void unsetResource_ref(com.ibm.ws.javaee.dd.commonext.ResourceRef value) {
          this.resource_ref.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.commonext.ResourceRef> resource_ref = new ArrayList<com.ibm.ws.javaee.dd.commonext.ResourceRef>();

     @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, name = "start-at-app-start", target = "(id=unbound)")
     protected volatile com.ibm.ws.javaee.dd.ejbext.StartAtAppStart start_at_app_start;
     protected java.lang.String name;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          name = (java.lang.String) config.get("name");
     }

     @Override
     public com.ibm.ws.javaee.dd.ejbext.BeanCache getBeanCache() {
          if (delegate == null) {
               return bean_cache == null ? null : bean_cache;
          } else {
               return bean_cache == null ? delegate.getBeanCache() : bean_cache;
          }
     }

     @Override
     public com.ibm.ws.javaee.dd.commonext.LocalTransaction getLocalTransaction() {
          if (delegate == null) {
               return local_transaction == null ? null : local_transaction;
          } else {
               return local_transaction == null ? delegate.getLocalTransaction() : local_transaction;
          }
     }

     @Override
     public com.ibm.ws.javaee.dd.commonext.GlobalTransaction getGlobalTransaction() {
          if (delegate == null) {
               return global_transaction == null ? null : global_transaction;
          } else {
               return global_transaction == null ? delegate.getGlobalTransaction() : global_transaction;
          }
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.commonext.ResourceRef> getResourceRefs() {
          java.util.List<com.ibm.ws.javaee.dd.commonext.ResourceRef> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonext.ResourceRef>() : new ArrayList<com.ibm.ws.javaee.dd.commonext.ResourceRef>(delegate.getResourceRefs());
          returnValue.addAll(resource_ref);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.ejbext.RunAsMode> getRunAsModes() {
          // Not Used In Liberty -- returning default value or app configuration
          java.util.List<com.ibm.ws.javaee.dd.ejbext.RunAsMode> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.ejbext.RunAsMode>() : new ArrayList<com.ibm.ws.javaee.dd.ejbext.RunAsMode>(delegate.getRunAsModes());
          return returnValue;
     }

     @Override
     public com.ibm.ws.javaee.dd.ejbext.StartAtAppStart getStartAtAppStart() {
          if (delegate == null) {
               return start_at_app_start == null ? null : start_at_app_start;
          } else {
               return start_at_app_start == null ? delegate.getStartAtAppStart() : start_at_app_start;
          }
     }

     @Override
     public java.lang.String getName() {
          if (delegate == null) {
               return name == null ? null : name;
          } else {
               return name == null ? delegate.getName() : name;
          }
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.ejbext.MessageDriven delegate) {
          this.delegate = delegate;
     }
}
