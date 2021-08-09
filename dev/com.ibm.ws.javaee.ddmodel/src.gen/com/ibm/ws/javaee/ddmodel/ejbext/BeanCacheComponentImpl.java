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

@Component(configurationPid = "com.ibm.ws.javaee.dd.ejbext.BeanCache",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class BeanCacheComponentImpl implements com.ibm.ws.javaee.dd.ejbext.BeanCache {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.ejbext.BeanCache delegate;
     protected com.ibm.ws.javaee.dd.ejbext.BeanCache.ActivationPolicyTypeEnum activation_policy;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          if (config.get("activation-policy") != null)
               activation_policy = com.ibm.ws.javaee.dd.ejbext.BeanCache.ActivationPolicyTypeEnum.valueOf((String) config.get("activation-policy"));
     }

     @Override
     public com.ibm.ws.javaee.dd.ejbext.BeanCache.ActivationPolicyTypeEnum getActivationPolicy() {
          if (delegate == null) {
               return activation_policy == null ? null : activation_policy;
          } else {
               return activation_policy == null ? delegate.getActivationPolicy() : activation_policy;
          }
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.ejbext.BeanCache delegate) {
          this.delegate = delegate;
     }
}
