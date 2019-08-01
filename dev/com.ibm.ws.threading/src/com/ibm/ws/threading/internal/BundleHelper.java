package com.ibm.ws.threading.internal;

import com.ibm.ws.org.apache.felix.scr.StaticBundleComponentFactory;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;
import com.ibm.ws.threading.PolicyExecutorComponentManager;
import com.ibm.ws.threading.PolicyExecutorProvider;

public class BundleHelper implements StaticBundleComponentFactory {

    @Override
    public StaticComponentManager createStaticComponentManager(String componentName) {
        if (PolicyExecutorProvider.class.getName().equals(componentName)) {
            return new PolicyExecutorComponentManager();
        }
        if (DeferrableScheduledExecutorImpl.class.getName().equals(componentName)) {
            return new DeferrableExecutorComponentManager();
        }
        if (FutureMonitorImpl.class.getName().equals(componentName)) {
            return new FutureMonitorComponentManager();
        }
        if (ScheduledExecutorImpl.class.getName().equals(componentName)) {
            return new ScheduledExecutorComponentManager();
        }
        if (ThreadingIntrospector.class.getName().equals(componentName)) {
            return new ThreadingIntrospectorComponentManager();
        }
        if (ExecutorServiceImpl.class.getName().equals(componentName)) {
            return new ExecutorServiceComponentManager();
        }
        return null;
    }
}