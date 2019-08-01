package com.ibm.ws.kernel.filemonitor.internal;

import com.ibm.ws.kernel.filemonitor.internal.scan.ScanningComponentManager;
import com.ibm.ws.kernel.filemonitor.internal.scan.ScanningCoreServiceImpl;
import com.ibm.ws.org.apache.felix.scr.StaticBundleComponentFactory;
import com.ibm.ws.org.apache.felix.scr.StaticComponentManager;

public class BundleHelper implements StaticBundleComponentFactory {

    @Override
    public StaticComponentManager createStaticComponentManager(String componentName) {
        if (FileNotificationImpl.class.getName().equals(componentName)) {
            return new FileNotificationComponentManager();
        }
        if (ScanningCoreServiceImpl.class.getName().equals(componentName)) {
            return new ScanningComponentManager();
        }
        return null;
    }
}