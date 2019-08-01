package com.ibm.ws.kernel.server.internal;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.kernel.launch.service.PauseableComponentController;
import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticBundleComponentFactory;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

public class ServerMBeanComponentManager implements StaticComponentManager, StaticBundleComponentFactory {

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
        if ("setPauseableComponentController".equals(name)) {
            Object[] params = parameters.getParameters(PauseableComponentController.class);
            ((ServerEndpointControlMBeanImpl) instance).setPauseableComponentController((PauseableComponentController) params[0]);
        } else if ("setVariableRegistry".equals(name)) {
            Object[] params = parameters.getParameters(VariableRegistry.class);
            ((ServerInfoMBeanImpl) instance).setVariableRegistry((VariableRegistry) params[0]);
        } else if ("setWsLocationAdmin".equals(name)) {
            Object[] params = parameters.getParameters(WsLocationAdmin.class);
            ((ServerInfoMBeanImpl) instance).setWsLocationAdmin((WsLocationAdmin) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetPauseableComponentController".equals(name)) {
            Object[] params = parameters.getParameters(PauseableComponentController.class);
            ((ServerEndpointControlMBeanImpl) instance).unsetPauseableComponentController((PauseableComponentController) params[0]);
        } else if ("unsetVariableRegistry".equals(name)) {
            Object[] params = parameters.getParameters(VariableRegistry.class);
            ((ServerInfoMBeanImpl) instance).unsetVariableRegistry((VariableRegistry) params[0]);
        } else if ("unsetWsLocationAdmin".equals(name)) {
            Object[] params = parameters.getParameters(WsLocationAdmin.class);
            ((ServerInfoMBeanImpl) instance).unsetWsLocationAdmin((WsLocationAdmin) params[0]);
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
        if (ServerEndpointControlMBeanImpl.class.getName().equals(componentName) || ServerInfoMBeanImpl.class.getName().equals(componentName)) {
            return this;
        }
        return null;
    }
}
