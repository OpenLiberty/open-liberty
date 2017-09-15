/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.osgi.framework.BundleContext;

import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.kernel.launch.service.ServerFeatures;

/**
 *
 */
public class ServerFeaturesHelper {

    private final BundleContext bundleContext;

    static boolean isServerFeaturesRequest(BundleContext bc) {
        String featuresRequest = bc.getProperty(ServerFeatures.REQUEST_SERVER_FEATURES_PROPERTY);

        //check the property is set, and has the expected content..
        //otherwise this is a no-op.
        if (featuresRequest == null || !featuresRequest.equals("1.0.0")) {
            return false;
        }

        return true;
    }

    ServerFeaturesHelper(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Builds a list of required server features and publish it as a service to let the launcher obtain them..
     * <p>
     * <em>ONLY</em> takes effect if the Launcher has set the magic server content property
     */
    void processServerFeaturesRequest(Result featureResolveResult) {

        final Set<String> serverFeatureNames = new HashSet<String>();
        serverFeatureNames.addAll(featureResolveResult.getResolvedFeatures());
        serverFeatureNames.addAll(featureResolveResult.getMissing());

        //publish the service that will be used by the launcher to read back the data.
        final Dictionary<String, Object> d = new Hashtable<String, Object>();

        bundleContext.registerService(ServerFeatures.class, new ServerFeatures() {
            @Override
            public String[] getServerFeatureNames() {
                //return newly filtered data
                return serverFeatureNames.toArray(new String[serverFeatureNames.size()]);
            }
        }, d);

        //we're done here.. let liberty proceed to declare itself 'started' =)
    }
}
