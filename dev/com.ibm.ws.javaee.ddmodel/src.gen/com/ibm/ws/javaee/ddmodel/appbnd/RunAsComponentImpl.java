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
package com.ibm.ws.javaee.ddmodel.appbnd;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Component(configurationPid = "com.ibm.ws.javaee.dd.appbnd.RunAs",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class RunAsComponentImpl implements com.ibm.ws.javaee.dd.appbnd.RunAs {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.appbnd.RunAs delegate;
     protected java.lang.String userid;
     protected com.ibm.wsspi.kernel.service.utils.SerializableProtectedString password;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          userid = (java.lang.String) config.get("userid");
          password = (com.ibm.wsspi.kernel.service.utils.SerializableProtectedString) config.get("password");
     }

     @Override
     public java.lang.String getUserid() {
          if (delegate == null) {
               return userid == null ? null : userid;
          } else {
               return userid == null ? delegate.getUserid() : userid;
          }
     }

     @Override
     @com.ibm.websphere.ras.annotation.Sensitive
     public java.lang.String getPassword() {
          if (delegate == null) {
               return password == null ? null : new String(password.getChars());
          } else {
               return password == null ? delegate.getPassword() : new String(password.getChars());
          }
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.appbnd.RunAs delegate) {
          this.delegate = delegate;
     }
}
