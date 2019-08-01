package com.ibm.ws.diagnostics;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.diagnostics.osgi.BlueprintIntrospector;
import com.ibm.ws.diagnostics.osgi.BlueprintIntrospectorComponentManager;
import com.ibm.ws.diagnostics.osgi.BundleStateIntrospection;
import com.ibm.ws.diagnostics.osgi.BundleStateIntrospectionComponentManager;
import com.ibm.ws.diagnostics.osgi.BundleWiringIntrospection;
import com.ibm.ws.diagnostics.osgi.BundleWiringIntrospectionComponentManager;
import com.ibm.ws.diagnostics.osgi.ComponentInfoIntrospection;
import com.ibm.ws.diagnostics.osgi.ComponentInfoIntrospectionComponentManager;
import com.ibm.ws.diagnostics.osgi.ConfigAdminIntrospection;
import com.ibm.ws.diagnostics.osgi.ConfigAdminIntrospectionComponentManager;
import com.ibm.ws.diagnostics.osgi.ConfigVariableIntrospection;
import com.ibm.ws.diagnostics.osgi.ConfigVariableIntrospectionComponentManager;
import com.ibm.ws.diagnostics.osgi.RegionIntrospection;
import com.ibm.ws.diagnostics.osgi.RegionIntrospectionComponentManager;
import com.ibm.ws.diagnostics.osgi.ServiceIntrospection;
import com.ibm.ws.diagnostics.osgi.ServiceIntrospectionComponentManager;
import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticBundleComponentFactory;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;

public class BundleHelper implements StaticBundleComponentFactory, StaticComponentManager {

    @Override
    public StaticComponentManager createStaticComponentManager(String componentName) {
        if (componentName.startsWith("com.ibm.ws.diagnostics.java")) {
            return this;
        }
        if (BlueprintIntrospector.class.getName().equals(componentName)) {
            return new BlueprintIntrospectorComponentManager();
        }
        if (BundleStateIntrospection.class.getName().equals(componentName)) {
            return new BundleStateIntrospectionComponentManager();
        }
        if (BundleWiringIntrospection.class.getName().equals(componentName)) {
            return new BundleWiringIntrospectionComponentManager();
        }
        if (ComponentInfoIntrospection.class.getName().equals(componentName)) {
            return new ComponentInfoIntrospectionComponentManager();
        }
        if (ConfigAdminIntrospection.class.getName().equals(componentName)) {
            return new ConfigAdminIntrospectionComponentManager();
        }
        if (ConfigVariableIntrospection.class.getName().equals(componentName)) {
            return new ConfigVariableIntrospectionComponentManager();
        }
        if (RegionIntrospection.class.getName().equals(componentName)) {
            return new RegionIntrospectionComponentManager();
        }
        if (ServiceIntrospection.class.getName().equals(componentName)) {
            return new ServiceIntrospectionComponentManager();
        }
        return null;
    }

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue updated(Object instance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public boolean init(Object instance, String name) {
        return true;
    }
}