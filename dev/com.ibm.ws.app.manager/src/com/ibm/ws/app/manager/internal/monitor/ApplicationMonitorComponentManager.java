package com.ibm.ws.app.manager.internal.monitor;

import java.util.Map;

import org.osgi.service.component.ComponentContext;

import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;

public class ApplicationMonitorComponentManager implements StaticComponentManager {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        ((ApplicationMonitor) instance).activate(componentContext, (Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        ((ApplicationMonitor) instance).deactivate(componentContext, reason);
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        ((ApplicationMonitor) instance).modified((Map<String, Object>) componentContext.getProperties());
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object componentInstance, String name, Parameters parameters) {
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object componentInstance, String name, Parameters parameters) {
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
}