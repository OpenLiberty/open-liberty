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
package com.ibm.ws.javaee.ddmodel.appext;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Component(configurationPid = "com.ibm.ws.javaee.dd.appext.ApplicationExt",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class ApplicationExtComponentImpl implements com.ibm.ws.javaee.dd.appext.ApplicationExt {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.appext.ApplicationExt delegate;
     protected Boolean shared_session_context_value;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          shared_session_context_value = (Boolean) config.get("shared-session-context");
     }

     @Override
     public java.lang.String getVersion() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? null : delegate.getVersion();
     }

     @Override
     public boolean isSetClientMode() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? false : delegate.isSetClientMode();
     }

     @Override
     public com.ibm.ws.javaee.dd.appext.ApplicationExt.ClientModeEnum getClientMode() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? null : delegate.getClientMode();
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.appext.ModuleExtension> getModuleExtensions() {
          // Not Used In Liberty -- returning default value or app configuration
          java.util.List<com.ibm.ws.javaee.dd.appext.ModuleExtension> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.appext.ModuleExtension>() : new ArrayList<com.ibm.ws.javaee.dd.appext.ModuleExtension>(delegate.getModuleExtensions());
          return returnValue;
     }

     @Override
     public boolean isSetReloadInterval() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? false : delegate.isSetReloadInterval();
     }

     @Override
     public long getReloadInterval() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? 0 : delegate.getReloadInterval();
     }

     @Override
     public boolean isSetSharedSessionContext() {
          return (shared_session_context_value!= null);
     }

     @Override
     public boolean isSharedSessionContext() {
          if (delegate == null) {
               return shared_session_context_value == null ? false : shared_session_context_value;
          } else {
               return shared_session_context_value == null ? delegate.isSharedSessionContext() : shared_session_context_value;
          }
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

     public void setDelegate(com.ibm.ws.javaee.dd.appext.ApplicationExt delegate) {
          this.delegate = delegate;
     }
}
