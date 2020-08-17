package test.compatibility;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        Bundle b = context.getBundle();
        System.out.println("Start bundle " + b.getSymbolicName());
    }

    @Override
    public void stop(BundleContext context) throws Exception {}

}