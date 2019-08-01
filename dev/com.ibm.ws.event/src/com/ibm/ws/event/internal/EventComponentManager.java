package com.ibm.ws.event.internal;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

import com.ibm.websphere.event.EventEngine;
import com.ibm.websphere.event.ExecutorServiceFactory;
import com.ibm.ws.org.apache.felix.scr.Parameters;
import com.ibm.ws.org.apache.felix.scr.ReturnValue;
import com.ibm.ws.org.apache.felix.scr.StaticBundleComponentFactory;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;

public class EventComponentManager implements StaticComponentManager, StaticBundleComponentFactory {

    @Override
    public ReturnValue activate(Object instance, ComponentContext componentContext) {
        if (instance instanceof EventEngineImpl) {
            ((EventEngineImpl) instance).activate(componentContext, (Map<String, Object>) componentContext.getProperties());
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue deactivate(Object instance, ComponentContext componentContext, int reason) {
        if (instance instanceof EventEngineImpl) {
            ((EventEngineImpl) instance).deactivate(componentContext);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue modified(Object instance, ComponentContext componentContext) {
        if (instance instanceof EventEngineImpl) {
            ((EventEngineImpl) instance).modified((Map<String, Object>) componentContext.getProperties());
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue bind(Object instance, String name, Parameters parameters) {
        if ("setExecutorServiceFactory".equals(name)) {
            Object[] params = parameters.getParameters(ExecutorServiceFactory.class);
            ((EventEngineImpl) instance).setExecutorServiceFactory((ExecutorServiceFactory) params[0]);
        } else if ("setLogService".equals(name)) {
            Object[] params = parameters.getParameters(LogService.class);
            ((EventEngineImpl) instance).setLogService((LogService) params[0]);
        } else if ("setOsgiEventHandler".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((EventEngineImpl) instance).setOsgiEventHandler((ServiceReference<org.osgi.service.event.EventHandler>) params[0]);
        } else if ("setWsEventHandler".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((EventEngineImpl) instance).setWsEventHandler((ServiceReference<com.ibm.websphere.event.EventHandler>) params[0]);
        } else if ("setEventEngine".equals(name)) {
            Object[] params = parameters.getParameters(EventEngine.class);
            ((ScheduledEventServiceImpl) instance).setEventEngine((EventEngine) params[0]);
        } else if ("setScheduledExecutor".equals(name)) {
            Object[] params = parameters.getParameters(ScheduledExecutorService.class);
            ((ScheduledEventServiceImpl) instance).setScheduledExecutor((ScheduledExecutorService) params[0]);
        } else if ("setExecutorService".equals(name)) {
            Object[] params = parameters.getParameters(ExecutorService.class);
            ((WorkStageExecutorServiceFactory) instance).setExecutorService((ExecutorService) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue unbind(Object instance, String name, Parameters parameters) {
        if ("unsetExecutorServiceFactory".equals(name)) {
            Object[] params = parameters.getParameters(ExecutorServiceFactory.class);
            ((EventEngineImpl) instance).unsetExecutorServiceFactory((ExecutorServiceFactory) params[0]);
        } else if ("unsetLogService".equals(name)) {
            Object[] params = parameters.getParameters(LogService.class);
            ((EventEngineImpl) instance).unsetLogService((LogService) params[0]);
        } else if ("unsetOsgiEventHandler".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((EventEngineImpl) instance).unsetOsgiEventHandler((ServiceReference<org.osgi.service.event.EventHandler>) params[0]);
        } else if ("unsetWsEventHandler".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((EventEngineImpl) instance).unsetWsEventHandler((ServiceReference<com.ibm.websphere.event.EventHandler>) params[0]);
        } else if ("unsetEventEngine".equals(name)) {
            Object[] params = parameters.getParameters(EventEngine.class);
            ((ScheduledEventServiceImpl) instance).unsetEventEngine((EventEngine) params[0]);
        } else if ("unsetScheduledExecutor".equals(name)) {
            Object[] params = parameters.getParameters(ScheduledExecutorService.class);
            ((ScheduledEventServiceImpl) instance).unsetScheduledExecutor((ScheduledExecutorService) params[0]);
        } else if ("unsetExecutorService".equals(name)) {
            Object[] params = parameters.getParameters(ExecutorService.class);
            ((WorkStageExecutorServiceFactory) instance).unsetExecutorService((ExecutorService) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public ReturnValue updated(Object instance, String name, Parameters parameters) {
        if ("updatedOsgiEventHandler".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((EventEngineImpl) instance).updatedOsgiEventHandler((ServiceReference<org.osgi.service.event.EventHandler>) params[0]);
        } else if ("updatedWsEventHandler".equals(name)) {
            Object[] params = parameters.getParameters(ServiceReference.class);
            ((EventEngineImpl) instance).updatedWsEventHandler((ServiceReference<com.ibm.websphere.event.EventHandler>) params[0]);
        }
        return ReturnValue.VOID;
    }

    @Override
    public boolean init(Object instance, String name) {
        return true;
    }

    @Override
    public StaticComponentManager createStaticComponentManager(String componentName) {
        if (EventEngineImpl.class.getName().equals(componentName) ||
            ScheduledEventServiceImpl.class.getName().equals(componentName) ||
            WorkStageExecutorServiceFactory.class.getName().equals(componentName)) {
            return this;
        }
        return null;
    }
}
