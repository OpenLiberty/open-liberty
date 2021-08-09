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
package com.ibm.ws.javaee.ddmodel.commonext;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Component(configurationPid = "com.ibm.ws.javaee.dd.commonext.LocalTransaction",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class LocalTransactionComponentImpl implements com.ibm.ws.javaee.dd.commonext.LocalTransaction {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.commonext.LocalTransaction delegate;
     protected com.ibm.ws.javaee.dd.commonext.LocalTransaction.ResolverEnum resolver;
     protected com.ibm.ws.javaee.dd.commonext.LocalTransaction.UnresolvedActionEnum unresolved_action;
     protected Boolean shareable;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          shareable = (Boolean) config.get("shareable");
          if (config.get("resolver") != null)
               resolver = com.ibm.ws.javaee.dd.commonext.LocalTransaction.ResolverEnum.valueOf((String) config.get("resolver"));
          if (config.get("unresolved-action") != null)
               unresolved_action = com.ibm.ws.javaee.dd.commonext.LocalTransaction.UnresolvedActionEnum.valueOf((String) config.get("unresolved-action"));
     }

     @Override
     public boolean isSetBoundary() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? false : delegate.isSetBoundary();
     }

     @Override
     public com.ibm.ws.javaee.dd.commonext.LocalTransaction.BoundaryEnum getBoundary() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? null : delegate.getBoundary();
     }

     @Override
     public boolean isSetResolver() {
          return (resolver!= null);
     }

     @Override
     public com.ibm.ws.javaee.dd.commonext.LocalTransaction.ResolverEnum getResolver() {
          if (delegate == null) {
               return resolver == null ? null : resolver;
          } else {
               return resolver == null ? delegate.getResolver() : resolver;
          }
     }

     @Override
     public boolean isSetUnresolvedAction() {
          return (unresolved_action!= null);
     }

     @Override
     public com.ibm.ws.javaee.dd.commonext.LocalTransaction.UnresolvedActionEnum getUnresolvedAction() {
          if (delegate == null) {
               return unresolved_action == null ? null : unresolved_action;
          } else {
               return unresolved_action == null ? delegate.getUnresolvedAction() : unresolved_action;
          }
     }

     @Override
     public boolean isSetShareable() {
          return (shareable!= null);
     }

     @Override
     public boolean isShareable() {
          if (delegate == null) {
               return shareable == null ? false : shareable;
          } else {
               return shareable == null ? delegate.isShareable() : shareable;
          }
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.commonext.LocalTransaction delegate) {
          this.delegate = delegate;
     }
}
