/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package test.iiop.interceptor;

import java.util.Map;

import org.apache.yoko.osgi.locator.BundleProviderLoader;
import org.apache.yoko.osgi.locator.Register;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

@Component(service = SubsystemFactory.class)
public class TestSubsystemFactory extends SubsystemFactory {
    private static final String INITIALIZER_CLASS_NAME = ORBInitializerImpl.class.getName();
    private Register providerRegistry;
    private BundleProviderLoader initializerClassLoader;

    @Reference
    protected void setRegister(Register providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Activate
    protected void activate(BundleContext bundleContext) {
        Bundle bundle = bundleContext.getBundle();
        initializerClassLoader = new BundleProviderLoader(INITIALIZER_CLASS_NAME, INITIALIZER_CLASS_NAME, bundle, 1);
        providerRegistry.registerProvider(initializerClassLoader);
    }

    @Deactivate
    protected void deactivate() {
        providerRegistry.unregisterProvider(initializerClassLoader);
    }

    @Override
    public Policy getTargetPolicy(ORB orb, Map<String, Object> properties, Map<String, Object> extraConfig) throws Exception {
        return null;
    }

    @Override
    public Policy getClientPolicy(ORB orb, Map<String, Object> properties) throws Exception {
        return null;
    }

    @Override
    public String getInitializerClassName(boolean endpoint) {
        return INITIALIZER_CLASS_NAME;
    }
}
