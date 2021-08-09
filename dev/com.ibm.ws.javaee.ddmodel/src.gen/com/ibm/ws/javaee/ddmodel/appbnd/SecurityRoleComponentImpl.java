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

@Component(configurationPid = "com.ibm.ws.javaee.dd.appbnd.SecurityRole",
     configurationPolicy = ConfigurationPolicy.REQUIRE,
     immediate=true,
     property = "service.vendor = IBM")
public class SecurityRoleComponentImpl implements com.ibm.ws.javaee.dd.appbnd.SecurityRole {
private Map<String,Object> configAdminProperties;
private com.ibm.ws.javaee.dd.appbnd.SecurityRole delegate;

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "user", target = "(id=unbound)")
     protected void setUser(com.ibm.ws.javaee.dd.appbnd.User value) {
          this.user.add(value);
     }

     protected void unsetUser(com.ibm.ws.javaee.dd.appbnd.User value) {
          this.user.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.appbnd.User> user = new ArrayList<com.ibm.ws.javaee.dd.appbnd.User>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "group", target = "(id=unbound)")
     protected void setGroup(com.ibm.ws.javaee.dd.appbnd.Group value) {
          this.group.add(value);
     }

     protected void unsetGroup(com.ibm.ws.javaee.dd.appbnd.Group value) {
          this.group.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.appbnd.Group> group = new ArrayList<com.ibm.ws.javaee.dd.appbnd.Group>();

     @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "special-subject", target = "(id=unbound)")
     protected void setSpecial_subject(com.ibm.ws.javaee.dd.appbnd.SpecialSubject value) {
          this.special_subject.add(value);
     }

     protected void unsetSpecial_subject(com.ibm.ws.javaee.dd.appbnd.SpecialSubject value) {
          this.special_subject.remove(value);
     }

     protected volatile List<com.ibm.ws.javaee.dd.appbnd.SpecialSubject> special_subject = new ArrayList<com.ibm.ws.javaee.dd.appbnd.SpecialSubject>();

     @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, name = "run-as", target = "(id=unbound)")
     protected volatile com.ibm.ws.javaee.dd.appbnd.RunAs run_as;
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
     public java.util.List<com.ibm.ws.javaee.dd.appbnd.User> getUsers() {
          java.util.List<com.ibm.ws.javaee.dd.appbnd.User> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.appbnd.User>() : new ArrayList<com.ibm.ws.javaee.dd.appbnd.User>(delegate.getUsers());
          returnValue.addAll(user);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.appbnd.Group> getGroups() {
          java.util.List<com.ibm.ws.javaee.dd.appbnd.Group> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.appbnd.Group>() : new ArrayList<com.ibm.ws.javaee.dd.appbnd.Group>(delegate.getGroups());
          returnValue.addAll(group);
          return returnValue;
     }

     @Override
     public java.util.List<com.ibm.ws.javaee.dd.appbnd.SpecialSubject> getSpecialSubjects() {
          java.util.List<com.ibm.ws.javaee.dd.appbnd.SpecialSubject> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.appbnd.SpecialSubject>() : new ArrayList<com.ibm.ws.javaee.dd.appbnd.SpecialSubject>(delegate.getSpecialSubjects());
          returnValue.addAll(special_subject);
          return returnValue;
     }

     @Override
     public com.ibm.ws.javaee.dd.appbnd.RunAs getRunAs() {
          if (delegate == null) {
               return run_as == null ? null : run_as;
          } else {
               return run_as == null ? delegate.getRunAs() : run_as;
          }
     }
     public Map<String,Object> getConfigAdminProperties() {
          return this.configAdminProperties;
     }

     public void setDelegate(com.ibm.ws.javaee.dd.appbnd.SecurityRole delegate) {
          this.delegate = delegate;
     }
}
