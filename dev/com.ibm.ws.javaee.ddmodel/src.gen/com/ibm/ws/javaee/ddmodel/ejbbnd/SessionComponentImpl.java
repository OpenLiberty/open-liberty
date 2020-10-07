/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// NOTE: This is a generated file. Do not edit it directly.
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.ws.javaee.dd.ejbbnd.Interface;

@Component(configurationPid = "com.ibm.ws.javaee.dd.ejbbnd.Session",
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           immediate = true,
           property = "service.vendor = IBM")
public class SessionComponentImpl extends com.ibm.ws.javaee.ddmodel.ejbbnd.EnterpriseBeanType implements com.ibm.ws.javaee.dd.ejbbnd.Session {
    private Map<String, Object> configAdminProperties;
    private com.ibm.ws.javaee.dd.ejbbnd.Session delegate;
    protected java.lang.String simple_binding_name;
    protected java.lang.String component_id;
    protected java.lang.String remote_home_binding_name;
    protected java.lang.String local_home_binding_name;
    protected java.lang.String name;

    protected volatile List<com.ibm.ws.javaee.dd.commonbnd.EJBRef> ejb_ref = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.EJBRef>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "ejb-ref", target = "(id=unbound)")
    protected void setEjb_ref(com.ibm.ws.javaee.dd.commonbnd.EJBRef value) {
        this.ejb_ref.add(value);
    }

    protected void unsetEjb_ref(com.ibm.ws.javaee.dd.commonbnd.EJBRef value) {
        this.ejb_ref.remove(value);
    }

    protected volatile List<Interface> interfaces = new ArrayList<Interface>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "interface", target = "(id=unbound)")
    protected void setInterface(Interface value) {
        this.interfaces.add(value);
    }

    protected void unsetInterface(Interface value) {
        this.interfaces.remove(value);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "resource-ref", target = "(id=unbound)")
    protected void setResource_ref(com.ibm.ws.javaee.dd.commonbnd.ResourceRef value) {
        this.resource_ref.add(value);
    }

    protected void unsetResource_ref(com.ibm.ws.javaee.dd.commonbnd.ResourceRef value) {
        this.resource_ref.remove(value);
    }

    protected volatile List<com.ibm.ws.javaee.dd.commonbnd.ResourceRef> resource_ref = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.ResourceRef>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "resource-env-ref", target = "(id=unbound)")
    protected void setResource_env_ref(com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef value) {
        this.resource_env_ref.add(value);
    }

    protected void unsetResource_env_ref(com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef value) {
        this.resource_env_ref.remove(value);
    }

    protected volatile List<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef> resource_env_ref = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "message-destination-ref", target = "(id=unbound)")
    protected void setMessage_destination_ref(com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef value) {
        this.message_destination_ref.add(value);
    }

    protected void unsetMessage_destination_ref(com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef value) {
        this.message_destination_ref.remove(value);
    }

    protected volatile List<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef> message_destination_ref = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "data-source", target = "(id=unbound)")
    protected void setData_source(com.ibm.ws.javaee.dd.commonbnd.DataSource value) {
        this.data_source.add(value);
    }

    protected void unsetData_source(com.ibm.ws.javaee.dd.commonbnd.DataSource value) {
        this.data_source.remove(value);
    }

    protected volatile List<com.ibm.ws.javaee.dd.commonbnd.DataSource> data_source = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.DataSource>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, name = "env-entry", target = "(id=unbound)")
    protected void setEnv_entry(com.ibm.ws.javaee.dd.commonbnd.EnvEntry value) {
        this.env_entry.add(value);
    }

    protected void unsetEnv_entry(com.ibm.ws.javaee.dd.commonbnd.EnvEntry value) {
        this.env_entry.remove(value);
    }

    protected volatile List<com.ibm.ws.javaee.dd.commonbnd.EnvEntry> env_entry = new ArrayList<com.ibm.ws.javaee.dd.commonbnd.EnvEntry>();

    @Activate
    protected void activate(Map<String, Object> config) {
        this.configAdminProperties = config;
        component_id = (java.lang.String) config.get("component-id");
        name = (java.lang.String) config.get("name");
        local_home_binding_name = (java.lang.String) config.get("local-home-binding-name");
        remote_home_binding_name = (java.lang.String) config.get("remote-home-binding-name");
        simple_binding_name = (java.lang.String) config.get("simple-binding-name");
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.ejbbnd.Interface> getInterfaces() {
        java.util.List<com.ibm.ws.javaee.dd.ejbbnd.Interface> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.ejbbnd.Interface>() : new ArrayList<com.ibm.ws.javaee.dd.ejbbnd.Interface>(delegate.getInterfaces());
        returnValue.addAll(interfaces);
        return returnValue;

    }

    @Override
    public java.lang.String getSimpleBindingName() {
        if (delegate == null) {
            return simple_binding_name == null ? null : simple_binding_name;
        } else {
            return simple_binding_name == null ? delegate.getSimpleBindingName() : simple_binding_name;
        }
    }

    @Override
    public java.lang.String getComponentID() {
        if (delegate == null) {
            return component_id == null ? null : component_id;
        } else {
            return component_id == null ? delegate.getComponentID() : component_id;
        }
    }

    @Override
    public java.lang.String getRemoteHomeBindingName() {
        if (delegate == null) {
            return remote_home_binding_name == null ? null : remote_home_binding_name;
        } else {
            return remote_home_binding_name == null ? delegate.getRemoteHomeBindingName() : remote_home_binding_name;
        }
    }

    @Override
    public java.lang.String getLocalHomeBindingName() {
        if (delegate == null) {
            return local_home_binding_name == null ? null : local_home_binding_name;
        } else {
            return local_home_binding_name == null ? delegate.getLocalHomeBindingName() : local_home_binding_name;
        }
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
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.EJBRef> getEJBRefs() {
        java.util.List<com.ibm.ws.javaee.dd.commonbnd.EJBRef> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.EJBRef>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.EJBRef>(delegate.getEJBRefs());
        returnValue.addAll(ejb_ref);
        return returnValue;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.ResourceRef> getResourceRefs() {
        java.util.List<com.ibm.ws.javaee.dd.commonbnd.ResourceRef> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.ResourceRef>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.ResourceRef>(delegate.getResourceRefs());
        returnValue.addAll(resource_ref);
        return returnValue;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef> getResourceEnvRefs() {
        java.util.List<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef>(delegate.getResourceEnvRefs());
        returnValue.addAll(resource_env_ref);
        return returnValue;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef> getMessageDestinationRefs() {
        java.util.List<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef>(delegate.getMessageDestinationRefs());
        returnValue.addAll(message_destination_ref);
        return returnValue;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.DataSource> getDataSources() {
        java.util.List<com.ibm.ws.javaee.dd.commonbnd.DataSource> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.DataSource>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.DataSource>(delegate.getDataSources());
        returnValue.addAll(data_source);
        return returnValue;
    }

    @Override
    public java.util.List<com.ibm.ws.javaee.dd.commonbnd.EnvEntry> getEnvEntries() {
        java.util.List<com.ibm.ws.javaee.dd.commonbnd.EnvEntry> returnValue = delegate == null ? new ArrayList<com.ibm.ws.javaee.dd.commonbnd.EnvEntry>() : new ArrayList<com.ibm.ws.javaee.dd.commonbnd.EnvEntry>(delegate.getEnvEntries());
        returnValue.addAll(env_entry);
        return returnValue;
    }

    public Map<String, Object> getConfigAdminProperties() {
        return this.configAdminProperties;
    }

    public void setDelegate(com.ibm.ws.javaee.dd.ejbbnd.Session delegate) {
        this.delegate = delegate;
    }
}
