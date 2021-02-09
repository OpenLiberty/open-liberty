/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.serverinfo.impl;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

import componenttest.serverinfo.FATServerInfoMBean;

@Component(service = { FATServerInfoMBean.class, DynamicMBean.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM",
                        "jmx.objectname=" + FATServerInfoMBean.OBJECT_NAME })
public class FATServerInfoMBeanImpl extends StandardMBean implements FATServerInfoMBean {
    private static final String REFERENCE_JPA_RUNTIME = "jpaRuntime";

    private final AtomicServiceReference<FeatureProvisioner> featureProvisioner = new AtomicServiceReference<FeatureProvisioner>(REFERENCE_JPA_RUNTIME);

    private transient ComponentContext context;
    private transient Dictionary<String, Object> props;

    public FATServerInfoMBeanImpl() throws NotCompliantMBeanException {
        super(FATServerInfoMBean.class);
    }

    @Activate
    protected void activate(ComponentContext cc) {
        props = cc.getProperties();
        featureProvisioner.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        featureProvisioner.deactivate(cc);
    }

    @Reference(name = REFERENCE_JPA_RUNTIME, service = FeatureProvisioner.class)
    protected void setfeatureProvisioner(ServiceReference<FeatureProvisioner> ref) {
        featureProvisioner.setReference(ref);
    }

    protected void unsetfeatureProvisioner(ServiceReference<FeatureProvisioner> ref) {
        featureProvisioner.unsetReference(ref);
    }

    @Override
    public Set<String> getInstalledFeatures() {
        HashSet<String> retVal = new HashSet<String>();

        FeatureProvisioner fp = featureProvisioner.getService();
        if (fp != null) {
            Set<String> fpFeatureSet = fp.getInstalledFeatures();
            if (fpFeatureSet != null) {
                retVal.addAll(fpFeatureSet);
            }
        }

        return retVal;
    }
}
