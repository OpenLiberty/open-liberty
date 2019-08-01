package com.ibm.ws.collector.manager.internal;

import com.ibm.ws.logging.ffdc.source.FFDCComponentManager;
import com.ibm.ws.logging.ffdc.source.FFDCSource;
import com.ibm.ws.org.apache.felix.scr.StaticBundleComponentFactory;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;

public class BundleHelper implements StaticBundleComponentFactory {

    @Override
    public StaticComponentManager createStaticComponentManager(String componentName) {
        if (CollectorManagerImpl.class.getName().equals(componentName)) {
            return new CollectorComponentManager();
        }
        if (FFDCSource.class.getName().equals(componentName)) {
            return new FFDCComponentManager();
        }
        return null;
    }
}