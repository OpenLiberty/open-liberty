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

@Component(configurationPid = "com.ibm.ws.javaee.dd.commonext.ResourceRef",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class ResourceRefComponentImpl implements com.ibm.ws.javaee.dd.commonext.ResourceRef {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.commonext.ResourceRef delegate;
     protected java.lang.String name;
     protected com.ibm.ws.javaee.dd.commonext.ResourceRef.IsolationLevelEnum isolation_level;
     protected Integer commit_priority;
     protected com.ibm.ws.javaee.dd.commonext.ResourceRef.BranchCouplingEnum branch_coupling;

     @Activate
     protected void activate(Map<String, Object> config) {
          this.configAdminProperties = config;
          name = (java.lang.String) config.get("name");
          if (config.get("isolation-level") != null)
               isolation_level = com.ibm.ws.javaee.dd.commonext.ResourceRef.IsolationLevelEnum.valueOf((String) config.get("isolation-level"));
          if (config.get("branch-coupling") != null)
               branch_coupling = com.ibm.ws.javaee.dd.commonext.ResourceRef.BranchCouplingEnum.valueOf((String) config.get("branch-coupling"));
          commit_priority = (Integer) config.get("commit-priority");
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
     public boolean isSetIsolationLevel() {
          return (isolation_level!= null);
     }

     @Override
     public com.ibm.ws.javaee.dd.commonext.ResourceRef.IsolationLevelEnum getIsolationLevel() {
          if (delegate == null) {
               return isolation_level == null ? null : isolation_level;
          } else {
               return isolation_level == null ? delegate.getIsolationLevel() : isolation_level;
          }
     }

     @Override
     public boolean isSetConnectionManagementPolicy() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? false : delegate.isSetConnectionManagementPolicy();
     }

     @Override
     public com.ibm.ws.javaee.dd.commonext.ResourceRef.ConnectionManagementPolicyEnum getConnectionManagementPolicy() {
          // Not Used In Liberty -- returning default value or app configuration
          return delegate == null ? null : delegate.getConnectionManagementPolicy();
     }

     @Override
     public boolean isSetCommitPriority() {
          return (commit_priority!= null);
     }

     @Override
     public int getCommitPriority() {
          if (delegate == null) {
               return commit_priority == null ? 0 : commit_priority;
          } else {
               return commit_priority == null ? delegate.getCommitPriority() : commit_priority;
          }
     }

     @Override
     public boolean isSetBranchCoupling() {
          return (branch_coupling!= null);
     }

     @Override
     public com.ibm.ws.javaee.dd.commonext.ResourceRef.BranchCouplingEnum getBranchCoupling() {
          if (delegate == null) {
               return branch_coupling == null ? null : branch_coupling;
          } else {
               return branch_coupling == null ? delegate.getBranchCoupling() : branch_coupling;
          }
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.commonext.ResourceRef delegate) {
          this.delegate = delegate;
     }
}
