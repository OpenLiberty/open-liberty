package com.ibm.ws.sip.util;

import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;

import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

@Component(service = FeatureUtil.class, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class FeatureUtil {

    private static boolean nettyEnabled = false;

    private final String FEATUREPROVISIONER_REFERENCE_NAME = "featureProvisioner";

    private final AtomicServiceReference<FeatureProvisioner> _featureProvisioner = new AtomicServiceReference<FeatureProvisioner>(
            FEATUREPROVISIONER_REFERENCE_NAME);

    @Activate
    protected void activate(ComponentContext cc) {
        _featureProvisioner.activate(cc);
        setNettyEnabled();
    }
    
    @Modified
    protected void modified(ComponentContext cc) {
        setNettyEnabled();
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        _featureProvisioner.deactivate(cc);
    }

    @Reference(name = FEATUREPROVISIONER_REFERENCE_NAME, service = FeatureProvisioner.class, cardinality = ReferenceCardinality.MANDATORY)
    protected void setFeatureProvisioner(ServiceReference<FeatureProvisioner> ref) {
        _featureProvisioner.setReference(ref);
    }

    protected void unsetFeatureProvisioner(FeatureProvisioner featureProvisioner) {
    }

    private void setNettyEnabled() {
        Set<String> currentFeatureSet = _featureProvisioner.getService().getInstalledFeatures();
        nettyEnabled = currentFeatureSet.contains("netty-1.0") ? true : false;
    }

    /**
     * Check to see if netty-1.0 is currently enabled
     * @return true if netty-1.0 is enabled
     */
    public static boolean isNettyEnabled() {
        System.out.println("isNettyEnabled: " + nettyEnabled);
        return nettyEnabled;
    }
}
