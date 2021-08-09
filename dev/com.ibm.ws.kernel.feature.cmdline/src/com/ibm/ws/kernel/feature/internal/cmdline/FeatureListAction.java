/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.cmdline;

import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;

import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.feature.internal.generator.DefaultConfigurationList;
import com.ibm.ws.kernel.feature.internal.generator.FeatureList;
import com.ibm.ws.kernel.feature.internal.generator.FeatureListOptions;
import com.ibm.ws.kernel.feature.internal.generator.FeatureListUtils;
import com.ibm.ws.kernel.feature.internal.generator.ManifestFileProcessor;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

/**
 *
 */
public class FeatureListAction implements ActionHandler {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.kernel.feature.internal.runtimetool.ActionHandler#handleTask(java.io.PrintStream, java.io.PrintStream,
     * com.ibm.ws.kernel.feature.internal.runtimetool.Arguments)
     */
    @Override
    public ReturnCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {

        FeatureListOptions options = new FeatureListOptions();

        // Process locale argument
        String val = args.getOption("locale");
        if (val != null) {
            int index = val.indexOf('_');
            String lang = (index == -1) ? val : val.substring(0, index);
            options.setLocale((index == -1) ? new Locale(lang) : new Locale(lang, val.substring(index + 1)));
        }

        // Process encoding argument.
        val = args.getOption("encoding");
        if (val != null) {
            options.setEncoding(val);
        }

        // Process product extension (name) argument.
        val = args.getOption("productextension");
        if (val != null) {
            options.setProductName(val);
        }

        // Process "include internals" switch argument. Any value but true (case insensitive) will be
        // taken as false.
        val = args.getOption("includeinternals");
        if (val != null) {
            options.setIncludeInternals(Boolean.parseBoolean(val));
        }

        // Process file output name argument.
        options.setOutputFile(args.getPositionalArguments().get(0));

        // Get the feature definitions for the specified or default product.
        ManifestFileProcessor mfp = new ManifestFileProcessor();
        String productName = options.getProductName();
        boolean coreProduct = productName.equals(ManifestFileProcessor.CORE_PRODUCT_NAME);
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
                    System.out.println(MessageFormat.format(NLS.messages.getString("tool.product.ext.not.found"), productName, location));
                    return ReturnCode.PRODUCT_EXT_NOT_FOUND;
                } else {
                    System.out.println(MessageFormat.format(NLS.messages.getString("tool.product.ext.not.defined"), productName));
                    return ReturnCode.PRODUCT_EXT_NOT_DEFINED;
                }
            } else if (features.isEmpty()) {
                System.out.println(MessageFormat.format(NLS.messages.getString("tool.product.ext.features.not.found"), productName));
                return ReturnCode.PRODUCT_EXT_NO_FEATURES_FOUND;
            }
        }

        FeatureListUtils utils = new FeatureListUtils(options);
        FeatureList fl = new FeatureList(options, features, coreFeatures, utils);

        // Begin feature list file
        utils.writeStartDocument();

        // Write top level <featureInfo> element that contains feature list and default configuration information
        utils.startFeatureInfo(options.getProductName(), location, productId);

        // pass mfp to writeFeatureList so can get stuff to create ContentBasedLocalBundleRepository for product features
        fl.writeFeatureList(mfp);

        DefaultConfigurationList dcl = new DefaultConfigurationList(options, features, utils);
        // <defaultConfiguration>
        dcl.writeDefaultConfiguration(mfp);

        // </featureInfo>
        utils.endFeatureInfo();

        // End feature list file
        utils.writeEndDocument();

        com.ibm.ws.kernel.feature.internal.generator.FeatureListOptions.ReturnCode rc = options.getReturnCode();

        if (rc == com.ibm.ws.kernel.feature.internal.generator.FeatureListOptions.ReturnCode.OK) {
            return ReturnCode.OK;
        } else if (rc == com.ibm.ws.kernel.feature.internal.generator.FeatureListOptions.ReturnCode.BAD_ARGUMENT) {
            return ReturnCode.BAD_ARGUMENT;
        } else {
            return ReturnCode.RUNTIME_EXCEPTION;
        }
    }

}
