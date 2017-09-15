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

@Component(configurationPid = "com.ibm.ws.javaee.dd.appbnd.SpecialSubject",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class SpecialSubjectComponentImpl implements com.ibm.ws.javaee.dd.appbnd.SpecialSubject {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.appbnd.SpecialSubject delegate;
     protected com.ibm.ws.javaee.dd.appbnd.SpecialSubject.Type type;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          if (config.get("type") != null)
               type = com.ibm.ws.javaee.dd.appbnd.SpecialSubject.Type.valueOf((String) config.get("type"));
     }

     @Override
     public com.ibm.ws.javaee.dd.appbnd.SpecialSubject.Type getType() {
          if (delegate == null) {
               return type == null ? null : type;
          } else {
               return type == null ? delegate.getType() : type;
          }
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.appbnd.SpecialSubject delegate) {
          this.delegate = delegate;
     }
}
