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

@Component(configurationPid = "com.ibm.ws.javaee.dd.commonbnd.ResourceRef",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class ResourceRefComponentImpl implements com.ibm.ws.javaee.dd.commonbnd.ResourceRef {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.commonbnd.ResourceRef delegate;

     @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, name = "authentication-alias", target = "(id=unbound)")
     protected volatile com.ibm.ws.javaee.dd.commonbnd.AuthenticationAlias authentication_alias;

     @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, name = "custom-login-configuration", target = "(id=unbound)")
     protected volatile com.ibm.ws.javaee.dd.commonbnd.CustomLoginConfiguration custom_login_configuration;
     protected java.lang.String name;
     protected java.lang.String binding_name;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          name = (java.lang.String) config.get("name");
          binding_name = (java.lang.String) config.get("binding-name");
     }

     @Override
     public java.lang.String getName() {
          if (delegate == null) {
               return name == null ? null : name;
          } else {
               return name == null ? delegate.getName() : name;
          }
     }

     @Override
     public java.lang.String getBindingName() {
          if (delegate == null) {
               return binding_name == null ? null : binding_name;
          } else {
               return binding_name == null ? delegate.getBindingName() : binding_name;
          }
     }

     @Override
     public com.ibm.ws.javaee.dd.commonbnd.AuthenticationAlias getAuthenticationAlias() {
          if (delegate == null) {
               return authentication_alias == null ? null : authentication_alias;
          } else {
               return authentication_alias == null ? delegate.getAuthenticationAlias() : authentication_alias;
          }
     }

     @Override
     public com.ibm.ws.javaee.dd.commonbnd.CustomLoginConfiguration getCustomLoginConfiguration() {
          if (delegate == null) {
               return custom_login_configuration == null ? null : custom_login_configuration;
          } else {
               return custom_login_configuration == null ? delegate.getCustomLoginConfiguration() : custom_login_configuration;
          }
     }

     @Override
     public java.lang.String getDefaultAuthUserid() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? null : delegate.getDefaultAuthUserid();
     }

     @Override
     public java.lang.String getDefaultAuthPassword() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? null : delegate.getDefaultAuthPassword();
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.commonbnd.ResourceRef delegate) {
          this.delegate = delegate;
     }
}
