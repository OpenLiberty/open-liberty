/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.generator;

import java.text.MessageFormat;
import java.util.Map;

import com.ibm.ws.kernel.feature.internal.generator.FeatureListOptions.ReturnCode;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

/**
 *
 */
public class FeatureListGenerator {

    private final FeatureListOptions options;

    /**
     * @param options
     */
    public FeatureListGenerator(FeatureListOptions options) {
        this.options = options;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            ArgumentProcessor ap = new ArgumentProcessor(args);
            FeatureListGenerator flg = new FeatureListGenerator(ap.getOptions());
            System.exit(flg.generate().getValue());
        } catch (FeatureListException fle) {
            System.out.println(fle.getMessage());
            System.exit(ReturnCode.RUNTIME_EXCEPTION.getValue());
        }
    }

    /**
     * @return
     */
    private ReturnCode generate() {
        if (options.getReturnCode().getValue() > 0) {
            return options.getReturnCode();
        } else if (options.getReturnCode() == ReturnCode.GENERATE_ACTION) {
            String productName = options.getProductName();
            boolean coreProduct = productName.equals(ManifestFileProcessor.CORE_PRODUCT_NAME);
            ManifestFileProcessor mfp = new ManifestFileProcessor();
            Map<String, ProvisioningFeatureDefinition> features = mfp.getFeatureDefinitions(productName);
            Map<String, ProvisioningFeatureDefinition> coreFeatures =
                            coreProduct ? null : mfp.getFeatureDefinitions(ManifestFileProcessor.CORE_PRODUCT_NAME);
            String location = mfp.getProdFeatureLocation(productName);
            String productId = mfp.getProdFeatureId(productName);

            // Check for issues retrieving product extension feature definitions. If getFeatureDefinitions(...)
            // returns null, the product extension was not found. If it returns an empty map, the feature manifest
            // files were not found.
            if (!coreProduct) {
                if (features == null) {
                    if (location != null) {
                        System.out.println(MessageFormat.format(ArgumentProcessor.messages.getString("error.prod.ext.not.found"), productName, location));
                        return ReturnCode.PRODUCT_EXT_NOT_FOUND;
                    } else {
                        System.out.println(MessageFormat.format(ArgumentProcessor.messages.getString("error.prod.ext.not.defined"), productName));
                        return ReturnCode.PRODUCT_EXT_NOT_DEFINED;
                    }
                } else if (features.isEmpty()) {
                    System.out.println(MessageFormat.format(ArgumentProcessor.messages.getString("error.prod.ext.features.not.found"), productName));
                    return ReturnCode.PRODUCT_EXT_NO_FEATURES_FOUND;
                }
            }

            FeatureListUtils utils = new FeatureListUtils(options);

            utils.writeStartDocument();
            utils.startFeatureInfo(options.getProductName(), location, productId);

            FeatureList fl = new FeatureList(options, features, coreFeatures, utils);
            // pass mfp to writeFeatureList so can get stuff to create ContentBasedLocalBundleRepository for product features
            fl.writeFeatureList(mfp);

            DefaultConfigurationList dcl = new DefaultConfigurationList(options, features, utils);
            dcl.writeDefaultConfiguration(mfp);

            utils.endFeatureInfo();
            utils.writeEndDocument();
            if (options.getReturnCode() == ReturnCode.GENERATE_ACTION)
                return ReturnCode.OK;
        }

        return options.getReturnCode();
    }

}
