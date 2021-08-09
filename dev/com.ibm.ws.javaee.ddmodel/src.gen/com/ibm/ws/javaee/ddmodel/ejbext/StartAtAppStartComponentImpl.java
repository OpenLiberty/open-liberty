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

@Component(configurationPid = "com.ibm.ws.javaee.dd.ejbext.StartAtAppStart",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class StartAtAppStartComponentImpl implements com.ibm.ws.javaee.dd.ejbext.StartAtAppStart {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.ejbext.StartAtAppStart delegate;
     protected Boolean value;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          value = (Boolean) config.get("value");
     }

     @Override
     public boolean getValue() {
          if (delegate == null) {
               return value == null ? false : value;
          } else {
               return value == null ? delegate.getValue() : value;
          }
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.ejbext.StartAtAppStart delegate) {
          this.delegate = delegate;
     }
}
