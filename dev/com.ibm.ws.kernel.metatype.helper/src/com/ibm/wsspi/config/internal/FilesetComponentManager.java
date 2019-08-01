package com.ibm.wsspi.config.internal;

import java.util.Map;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticBundleComponentFactory;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;
import com.ibm.wsspi.config.FilesetChangeListener;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

public class FilesetComponentManager implements StaticComponentManager, StaticBundleComponentFactory {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((FilesetImpl) instance).activate(componentContext, (Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((FilesetImpl) instance).deactivate(componentContext);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        ((FilesetImpl) instance).modified((Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setListener".equals(name)) {
            Object[] params = parameters.getParameters(FilesetChangeListener.class);
            ((FilesetImpl) instance).setListener((FilesetChangeListener) params[0]);
        } else if ("setLocationAdmin".equals(name)) {
            Object[] params = parameters.getParameters(WsLocationAdmin.class);
            ((FilesetImpl) instance).setLocationAdmin((WsLocationAdmin) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetListener".equals(name)) {
            Object[] params = parameters.getParameters(FilesetChangeListener.class);
            ((FilesetImpl) instance).unsetListener((FilesetChangeListener) params[0]);
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
        if (FilesetImpl.class.getName().equals(componentName)) {
            return this;
        }
        return null;
    }
}
