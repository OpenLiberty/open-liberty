package com.ibm.ws.kernel.instrument.serialfilter.serverconfig;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticBundleComponentFactory;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;

public class FilterConfigComponentManager implements StaticComponentManager, StaticBundleComponentFactory {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((FilterConfigFactory) instance).activate(componentContext, (Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((FilterConfigFactory) instance).deactivate(componentContext, reason);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        ((FilterConfigFactory) instance).modified((Map<String, Object>) componentContext.getProperties());
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
    public ReturnValue updated(Object componentInstance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public boolean init(Object instance, String name) {
        return true;
    }

    @Override
    public StaticComponentManager createStaticComponentManager(String componentName) {
        if (FilterConfigFactory.class.getName().equals(componentName)) {
            return this;
        }
        return null;
    }
}
