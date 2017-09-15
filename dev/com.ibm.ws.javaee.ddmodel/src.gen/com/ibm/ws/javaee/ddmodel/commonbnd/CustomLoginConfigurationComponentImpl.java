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

@Component(configurationPid = "com.ibm.ws.javaee.dd.commonbnd.CustomLoginConfiguration",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class CustomLoginConfigurationComponentImpl implements com.ibm.ws.javaee.dd.commonbnd.CustomLoginConfiguration {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.commonbnd.CustomLoginConfiguration delegate;

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "property", target = "(id=unbound)")
     protected void setProperty(com.ibm.ws.javaee.dd.commonbnd.Property value) {
          this.property.add(value);
     }

     protected void unsetProperty(com.ibm.ws.javaee.dd.commonbnd.Property value) {
          this.property.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.commonbnd.Property> property = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.Property>();
     protected java.lang.String name;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          name = (java.lang.String) config.get("name");
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
     public java.util.List<com.ibm.ws.javaee.dd.commonbnd.Property> getProperties() {
          java.util.List<com.ibm.ws.javaee.dd.commonbnd.Property> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.Property>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.Property>(delegate.getProperties());
          returnValue.addAll(property);
          return returnValue;
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.commonbnd.CustomLoginConfiguration delegate) {
          this.delegate = delegate;
     }
}
