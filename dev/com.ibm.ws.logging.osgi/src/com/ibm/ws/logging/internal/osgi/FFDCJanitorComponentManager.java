package com.ibm.ws.logging.internal.osgi;

import java.util.concurrent.ScheduledExecutorService;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticBundleComponentFactory;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;

public class FFDCJanitorComponentManager implements StaticComponentManager, StaticBundleComponentFactory {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((FFDCJanitor) instance).activate();
        return ReturnValue.VOID;
    }

    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((FFDCJanitor) instance).deactivate(reason);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setScheduler".equals(name)) {
            Object[] params = parameters.getParameters(ScheduledExecutorService.class);
            ((FFDCJanitor) instance).setScheduler((ScheduledExecutorService) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetScheduler".equals(name)) {
            Object[] params = parameters.getParameters(ScheduledExecutorService.class);
            ((FFDCJanitor) instance).unsetScheduler((ScheduledExecutorService) params[0]);
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
        if (FFDCJanitor.class.getName().equals(componentName)) {
            return this;
        }
        return null;
    }
}
