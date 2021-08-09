/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.extension;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.kernel.feature.FeatureDefinition;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.generator.ManifestFileProcessor;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.product.utility.BaseCommandTask;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.ExecutionContext;

public class FeatureInfoCommandTask extends BaseCommandTask {

    public static final String FEATURE_INFO_TASK_NAME = "featureInfo";

    /** {@inheritDoc} */
    @Override
    public Set<String> getSupportedOptions() {
        return new HashSet<String>();
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return FEATURE_INFO_TASK_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskDescription() {
        return getOption("featureInfo.desc");
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return super.getTaskHelp("featureInfo.desc", "featureInfo.usage.options", "featureInfo.option-key.", "featureInfo.option-desc.", null);
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(ExecutionContext context) {
        CommandConsole commandConsole = context.getCommandConsole();

        ManifestFileProcessor processor = new ManifestFileProcessor();

        for (Map.Entry<String, Map<String, ProvisioningFeatureDefinition>> prodFeatureEntries : processor.getFeatureDefinitionsByProduct().entrySet()) {
            String productName = prodFeatureEntries.getKey();
            boolean headingPrinted = false;
            for (Map.Entry<String, ProvisioningFeatureDefinition> entry : prodFeatureEntries.getValue().entrySet()) {
                // entry.getKey() this is the longer Subsystem-SymbolicName
                FeatureDefinition featureDefintion = entry.getValue();
                String featureName = featureDefintion.getFeatureName();

                if (featureDefintion.getVisibility() == Visibility.PUBLIC) {
                    if (productName.equals(ManifestFileProcessor.CORE_PRODUCT_NAME)) {
                        commandConsole.printInfoMessage(featureName);
                    } else {
                        if (headingPrinted == false) {
                            commandConsole.printlnInfoMessage("");
                            commandConsole.printInfoMessage("Product Extension: ");
                            commandConsole.printlnInfoMessage(productName);
                            headingPrinted = true;
                        }
                        int colonIndex = featureName.indexOf(":");
                        commandConsole.printInfoMessage(featureName.substring(colonIndex + 1));
                    }

                    commandConsole.printInfoMessage(" [");
                    commandConsole.printInfoMessage(featureDefintion.getVersion().toString());
                    commandConsole.printlnInfoMessage("]");
                }
            }
        }

    }
}
