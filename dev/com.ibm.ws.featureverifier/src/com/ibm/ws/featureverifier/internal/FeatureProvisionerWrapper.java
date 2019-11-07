/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.featureverifier.internal;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * A small wrapper class to make it a little simpler for us to reflectively invoke the 'getInstalledFeatures' method.
 */
public class FeatureProvisionerWrapper {

    private final Object o;
    private final static String KNOWN_IMPL = "com.ibm.ws.kernel.feature.internal.FeatureManager";

    private FeatureProvisionerWrapper(Object o) {
        if (!KNOWN_IMPL.equals(o.getClass().getName())) {
            throw new RuntimeException("Error cannot wrap unexpected impl " + o.getClass().getName() + " of feature provisioner, wanted " + KNOWN_IMPL);
        }
        this.o = o;
    }

    private String getInstalledFeatures() {
        try {
            //Thankfully the return type is parsable as a String, which is more than enough for our purposes.
            //else we'd need to reflectively handle the return type etc.. which is a little more complex.
            Method m = o.getClass().getMethod("getInstalledFeatures");
            return m.invoke(o).toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read installed features using reflection ", e);
        }
    }

    /**
     * Small utility method that encapsulates the instantiation of this class,
     * and hides the parsing of the returned string.
     * 
     * If the FeatureProvisioner changes in the runtime, changes should be limited to this class.
     * 
     * @return Set of Strings representing the shortName or symbolicName of the features active in the runtime.
     */
    public static Set<String> obtainInstalledFeatures(BundleContext context) {
        ServiceReference<?> sr = context.getServiceReference("com.ibm.ws.kernel.feature.FeatureProvisioner");
        if (sr != null) {
            Object fp = context.getService(sr);
            if (fp != null) {
                FeatureProvisionerWrapper fpw = new FeatureProvisionerWrapper(fp);
                String installedFeaturesString = fpw.getInstalledFeatures();
                if (!installedFeaturesString.startsWith("[") || !installedFeaturesString.endsWith("]")) {
                    throw new IllegalStateException("ERROR: Unexpected format of installedFeatures string, expected [ feature (,feature) ]");
                }
                installedFeaturesString = installedFeaturesString.substring(1, installedFeaturesString.length() - 1);
                String[] featuresArray = installedFeaturesString.split(",");
                Set<String> featureSet = new HashSet<String>();
                for (String feature : featuresArray) {
                    featureSet.add(feature.trim());
                }
                return featureSet;
            } else {
                throw new IllegalStateException("Unable to obtain feature provisioner service from osgi");
            }

        } else {
            throw new IllegalStateException("Unable to obtain service reference for feature provisioner service from osgi");
        }
    }

}
