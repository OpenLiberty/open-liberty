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

@Component(configurationPid = "com.ibm.ws.javaee.dd.ejbbnd.JCAAdapter",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class JCAAdapterComponentImpl implements com.ibm.ws.javaee.dd.ejbbnd.JCAAdapter {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.ejbbnd.JCAAdapter delegate;
     protected java.lang.String activation_spec_binding_name;
     protected java.lang.String activation_spec_auth_alias;
     protected java.lang.String destination_binding_name;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          destination_binding_name = (java.lang.String) config.get("destination-binding-name");
          activation_spec_auth_alias = (java.lang.String) config.get("activation-spec-auth-alias");
          activation_spec_binding_name = (java.lang.String) config.get("activation-spec-binding-name");
     }

     @Override
     public java.lang.String getActivationSpecBindingName() {
          if (delegate == null) {
               return activation_spec_binding_name == null ? null : activation_spec_binding_name;
          } else {
               return activation_spec_binding_name == null ? delegate.getActivationSpecBindingName() : activation_spec_binding_name;
          }
     }

     @Override
     public java.lang.String getActivationSpecAuthAlias() {
          if (delegate == null) {
               return activation_spec_auth_alias == null ? null : activation_spec_auth_alias;
          } else {
               return activation_spec_auth_alias == null ? delegate.getActivationSpecAuthAlias() : activation_spec_auth_alias;
          }
     }

     @Override
     public java.lang.String getDestinationBindingName() {
          if (delegate == null) {
               return destination_binding_name == null ? null : destination_binding_name;
          } else {
               return destination_binding_name == null ? delegate.getDestinationBindingName() : destination_binding_name;
          }
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.ejbbnd.JCAAdapter delegate) {
          this.delegate = delegate;
     }
}
