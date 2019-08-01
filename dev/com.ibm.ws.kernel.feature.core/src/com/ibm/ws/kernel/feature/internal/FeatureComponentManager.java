package com.ibm.ws.kernel.feature.internal;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;

import com.ibm.ws.kernel.provisioning.LibertyBootRuntime;
import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticBundleComponentFactory;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

public class FeatureComponentManager implements StaticComponentManager, StaticBundleComponentFactory {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((FeatureManager) instance).activate(componentContext, (Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((FeatureManager) instance).deactivate(reason);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setRuntimeUpdateManager".equals(name)) {
            Object[] params = parameters.getParameters(RuntimeUpdateManager.class);
            ((FeatureManager) instance).setRuntimeUpdateManager((RuntimeUpdateManager) params[0]);
        } else if ("setVariableRegistry".equals(name)) {
            Object[] params = parameters.getParameters(VariableRegistry.class);
            ((FeatureManager) instance).setVariableRegistry((VariableRegistry) params[0]);
        } else if ("setLocationService".equals(name)) {
            Object[] params = parameters.getParameters(WsLocationAdmin.class);
            ((FeatureManager) instance).setLocationService((WsLocationAdmin) params[0]);
        } else if ("setLibertyBoot".equals(name)) {
            Object[] params = parameters.getParameters(LibertyBootRuntime.class);
            ((FeatureManager) instance).setLibertyBoot((LibertyBootRuntime) params[0]);
        } else if ("setEventAdminService".equals(name)) {
            Object[] params = parameters.getParameters(EventAdmin.class);
            ((FeatureManager) instance).setEventAdminService((EventAdmin) params[0]);
        } else if ("setDigraph".equals(name)) {
            Object[] params = parameters.getParameters(RegionDigraph.class);
            ((FeatureManager) instance).setDigraph((RegionDigraph) params[0]);
        } else if ("setExecutorService".equals(name)) {
            Object[] params = parameters.getParameters(ExecutorService.class);
            ((FeatureManager) instance).setExecutorService((ExecutorService) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetRuntimeUpdateManager".equals(name)) {
            Object[] params = parameters.getParameters(RuntimeUpdateManager.class);
            ((FeatureManager) instance).unsetRuntimeUpdateManager((RuntimeUpdateManager) params[0]);
        } else if ("unsetVariableRegistry".equals(name)) {
            Object[] params = parameters.getParameters(VariableRegistry.class);
            ((FeatureManager) instance).unsetVariableRegistry((VariableRegistry) params[0]);
        } else if ("unsetLocationService".equals(name)) {
            Object[] params = parameters.getParameters(WsLocationAdmin.class);
            ((FeatureManager) instance).unsetLocationService((WsLocationAdmin) params[0]);
        } else if ("unsetLibertyBoot".equals(name)) {
            Object[] params = parameters.getParameters(LibertyBootRuntime.class);
            ((FeatureManager) instance).unsetLibertyBoot((LibertyBootRuntime) params[0]);
        } else if ("unsetEventAdminService".equals(name)) {
            Object[] params = parameters.getParameters(EventAdmin.class);
            ((FeatureManager) instance).unsetEventAdminService((EventAdmin) params[0]);
        } else if ("unsetDigraph".equals(name)) {
            Object[] params = parameters.getParameters(RegionDigraph.class);
            ((FeatureManager) instance).unsetDigraph((RegionDigraph) params[0]);
        } else if ("unsetExecutorService".equals(name)) {
            Object[] params = parameters.getParameters(ExecutorService.class);
            ((FeatureManager) instance).unsetExecutorService((ExecutorService) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue updated(Object componentInstance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public boolean init(Object instance, String name) {
        return true;
    }

    @Override
    public StaticComponentManager createStaticComponentManager(String componentName) {
        if (FeatureManager.class.getName().equals(componentName)) {
            return this;
        }
        return null;
    }
}
