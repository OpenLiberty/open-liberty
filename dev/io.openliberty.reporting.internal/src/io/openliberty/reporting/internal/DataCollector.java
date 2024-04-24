/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.reporting.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.websphere.kernel.server.ServerInfoMBean;
import com.ibm.ws.kernel.feature.FeatureDefinition;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.ws.kernel.feature.FixManager;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.productinfo.DuplicateProductInfoException;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.productinfo.ProductInfoParseException;
import com.ibm.ws.kernel.productinfo.ProductInfoReplaceException;

import io.openliberty.reporting.internal.utils.HashUtils;

/**
 * <p>
 * Collects the required data from the kernel to pass to the cloud service to
 * check if there is any CVE's that could have impact.
 * </p>
 */
public class DataCollector {

    /**
     *
     * @return data Map<String, String>
     */
    public Map<String, String> getData() {
        Map<String, String> data = new HashMap<>();

        data.put("id", uniqueID);
        data.put("productEdition", productEdition);
        data.put("productVersion", productVersion);
        data.put("features", String.join(",", installedFeatures));
        data.put("javaVendor", javaVendor);
        data.put("javaVersion", javaRuntimeInfo);
        data.put("iFixes", String.join(",", iFixSet));
        data.put("osArch", osArch);
        data.put("os", os);

        return data;
    }

    private final String uniqueID;

    private String javaVendor = "";
    private final String javaRuntimeInfo;

    private final Set<String> installedFeatures = new HashSet<>();

    private final String productVersion;
    private final String productEdition;

    private final Set<String> iFixSet = new HashSet<>();

    private final String os;

    private final String osArch;

    /**
     * <p>
     * Collects all the required data.
     * </p>
     *
     * @param featureProvisioner
     * @param FixManager
     * @param serverInfo
     * @throws IOException
     * @throws DataCollectorException
     */
    public DataCollector(FeatureProvisioner featureProvisioner, FixManager FixManager, ServerInfoMBean serverInfo) throws IOException, DataCollectorException {

        Map<String, ? extends ProductInfo> allProductInfo;
        try {
            allProductInfo = ProductInfo.getAllProductInfo();

            uniqueID = HashUtils.hashString(serverInfo.getInstallDirectory()); // placeholder.

            javaRuntimeInfo = serverInfo.getJavaRuntimeVersion();

            installedFeatures.addAll(getPublicFeatures(featureProvisioner));

            String javaVM = System.getProperty("java.vendor").toLowerCase();

            if (javaVM == null) {
                javaVM = System.getProperty("java.vm.name", "unknown").toLowerCase();
            }

            javaVendor = javaVM;

            iFixSet.addAll(FixManager.getIFixes());

            os = System.getProperty("os.name");

            osArch = System.getProperty("os.arch");

            // the key is the productId
            if (allProductInfo.containsKey("com.ibm.websphere.appserver")) {
                productVersion = allProductInfo.get("com.ibm.websphere.appserver").getVersion();
                productEdition = allProductInfo.get("com.ibm.websphere.appserver").getEdition();
            } else {
                productVersion = allProductInfo.get("io.openliberty").getVersion();
                productEdition = allProductInfo.get("io.openliberty").getEdition();
            }

        } catch (ProductInfoParseException | DuplicateProductInfoException | ProductInfoReplaceException e) {
            throw new DataCollectorException("Unable to parse Product Info", e);
        }
    }

    /**
     *
     * @param featureProvisioner
     * @return Set<String>
     */
    private Set<String> getPublicFeatures(FeatureProvisioner featureProvisioner) {
        Set<String> publicFeatures = new TreeSet<>();
        Iterator<String> it = featureProvisioner.getInstalledFeatures().iterator();
        while (it.hasNext()) {
            String feature = it.next();
            FeatureDefinition fd = featureProvisioner.getFeatureDefinition(feature);

            if (fd != null && fd.getVisibility() == Visibility.PUBLIC) {
                if (fd.getSymbolicName().contains("versionless")) {
                    continue;
                }
                // get name from feature definition.
                // input ones come from the cache which is lower case.
                // If we don't want to include auto features, then check each feature before
                // adding it.
                if (!fd.getFeatureName().contains(":")) {
                    publicFeatures.add(fd.getFeatureName());
                }
            }
        }
        return publicFeatures;
    }

}
