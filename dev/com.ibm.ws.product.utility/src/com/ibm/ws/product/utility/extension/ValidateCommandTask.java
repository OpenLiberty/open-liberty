/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.generator.ManifestFileProcessor;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;
import com.ibm.ws.product.utility.BaseCommandTask;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.CommandConstants;
import com.ibm.ws.product.utility.ExecutionContext;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.product.utility.extension.ifix.xml.UpdatedFile;

/**
 * Product installation validation tool.
 */
public class ValidateCommandTask extends BaseCommandTask {

    public static final String VALIDATE_TASK_NAME = "validate";

    private File wlpInstallRoot;
    private CommandConsole commandConsole;
    private boolean printErrorOnly = false;
    private boolean printStartMessage = false;
    private int overallFailed = 0;

    /** {@inheritDoc} */
    @Override
    public Set<String> getSupportedOptions() {
        return new HashSet<String>();
    }

    @Override
    public String getTaskName() {
        return VALIDATE_TASK_NAME;
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskDescription() {
        return getOption("validate.desc");
    }

    @Override
    public String getTaskHelp() {
        return super.getTaskHelp("validate.desc", "validate.usage.options", "validate.option-key.", "validate.option-desc.", null);
    }

    @Override
    public void doExecute(ExecutionContext context) {
        commandConsole = context.getCommandConsole();
        wlpInstallRoot = context.getAttribute(CommandConstants.WLP_INSTALLATION_LOCATION, File.class);

        try {
            //start validate all the platform blst and features
            if (!!!printErrorOnly || printStartMessage)
                commandConsole.printlnInfoMessage(getMessage("info.validate.start"));

            ManifestFileProcessor mfp = new ManifestFileProcessor();
            Map<String, ProvisioningFeatureDefinition> features = mfp.getFeatureDefinitions();

            for (ProvisioningFeatureDefinition pfd : features.values()) {
                StringBuilder result = new StringBuilder();
                Formatter formatter = new Formatter(result);

                String featureName = pfd.getFeatureName();

                File csFile = pfd.getFeatureChecksumFile();

                if (!csFile.exists()) {
                    // If the cs file doesn't exist we just skip since the file is optional.
                    continue;
                }

                if ("true".equalsIgnoreCase(pfd.getHeader("IBM-Test-Feature")))
                    continue;

                if (!"os/400".equalsIgnoreCase(System.getProperty("os.name")) &&
                    "com.ibm.websphere.appserver.os400.extensions-1.0".equalsIgnoreCase(pfd.getSymbolicName())) {
                    // Special case for the OS/400 platform manifest (wlp/lib/platform/os400Extensions.mf)
                    // which will stick around after a minify but its referenced files will not
                    continue;
                }

                /**
                 * cs file is valid, then check its content
                 */
                //load the checksums file
                InputStream csis = null;
                Properties csprops = new Properties();
                try {
                    csis = new FileInputStream(csFile);
                    csprops.load(csis);
                } catch (IOException e) {
                    commandConsole.printInfoMessage(getMessage("info.validate.validating.feature", featureName));
                    commandConsole.printlnErrorMessage(getMessage("ERROR_CHECKSUMS_FILE_NOT_LOADED", csFile.getAbsoluteFile()));
                    commandConsole.printlnInfoMessage(getMessage("info.validate.exception", e.getMessage()));
                    return;
                } finally {
                    FileUtils.tryToClose(csis);
                }

                //start validate all the jars of the feature
                boolean resultFlag = true;
                for (Iterator<Object> it = csprops.keySet().iterator(); it.hasNext();) {
                    String jarPath = (String) it.next();
                    String jarChecksum = csprops.getProperty(jarPath);

                    /*
                     * check the file exists. The ProvisioningFeatureDefinition does not provide the install location for this feature but it could be in the standard WLP location
                     * (wlp/lib) or it might be a usr feature in wlp/usr/extensions/lib or it could even be a product extension that might be anywhere on disk! What we do know is
                     * that the checksum file always goes into lib/features/checksums or lib/platform/checksums dir and entries are relative to the parent of the lib directory so
                     * we just walk up to find the install root for this feature.
                     */
                    File installRoot = csFile.getParentFile().getParentFile().getParentFile().getParentFile();
                    File jarFile = new File(installRoot, jarPath);
                    if (!jarFile.exists()) {
                        formatter.format("  %1$s %2$s%n", getMessage("info.validate.validating.result.error"), getMessage("info.validate.content.file.not.exist", jarPath));
                        overallFailed++;
                        resultFlag = false;
                        continue;
                    }

                    //validate jar file
                    if (!validate(jarFile, jarChecksum, jarPath)) {
                        //jar file is broken
                        formatter.format("  %1$s %2$s%n", getMessage("info.validate.validating.result.error"), getMessage("info.validate.content.file.broken", jarPath));
                        overallFailed++;
                        resultFlag = false;
                        continue;
                    }

                }
                if (!!!resultFlag || pfd.getVisibility() == Visibility.PUBLIC) {
                    printResult(featureName, resultFlag, result.toString());
                }
            }

            // If everything has been validated successfully, run the check for any Ifixes that need reapplying to the runtime.
            if (overallFailed == 0) {
                Set<String> fixesToReApply = getFixesToReapply(mfp, commandConsole);
                if (!fixesToReApply.isEmpty())
                    commandConsole.printlnInfoMessage(getMessage("info.validate.fixes.need.reapplying", fixesToReApply));

                // Finally issue the success message.
                commandConsole.printlnInfoMessage(getMessage("info.validate.success"));
            } else {
                commandConsole.printlnInfoMessage(getMessage("info.validate.fail", overallFailed));
            }

        } catch (ValidateException e) {
            commandConsole.printlnInfoMessage(getMessage("info.validate.exception", e.getMessage()));
            return;
        }
    }

    private boolean validate(File targetFile, String expectedChecksum, String jarPath) throws ValidateException {
        String realchecksum;
        try {
            realchecksum = MD5Utils.getFileMD5String(targetFile);
        } catch (IOException e) {
            commandConsole.printlnErrorMessage(getMessage("ERROR_UNABLE_READ_FILE", targetFile.getAbsoluteFile(), e.getMessage()));
            throw new ValidateException();
        }
        boolean validated = realchecksum.equals(expectedChecksum);
        if (!validated) {
            //the file didn't match the checksum, has it been ifixed?
            Map<String, IFixInfo> latestFixInfos = IFixUtils.getLatestFixInfos(wlpInstallRoot, commandConsole);
            //get the most recent fix info for this file
            IFixInfo latestFixInfo = latestFixInfos.get(jarPath);
            if (latestFixInfo != null) {
                //awkwardly get the UpdatedFile object for this file from the fix info
                //then we can get the latest hash for validation
                for (UpdatedFile updatedFile : latestFixInfo.getUpdates().getFiles()) {
                    if (updatedFile.getId().equals(jarPath)) {
                        String ifixedHashCode = updatedFile.getHash();
                        validated = realchecksum.equals(ifixedHashCode);
                    }
                }
            }
        }
        return validated;
    }

    private void printResult(String featureName, boolean flag, String result) {

        if (flag) {
            if (!!!printErrorOnly) {
                commandConsole.printInfoMessage(getMessage("info.validate.validating.feature", featureName));
                commandConsole.printlnInfoMessage(getMessage("info.validate.validating.result.pass"));
            }
        } else {
            commandConsole.printInfoMessage(getMessage("info.validate.validating.feature", featureName));
            commandConsole.printlnInfoMessage(getMessage("info.validate.validating.result.fail"));
            commandConsole.printInfoMessage(result);
        }
    }

    private static class ValidateException extends Exception {}

    public static Set<String> getFixesToReapply(ManifestFileProcessor mfp, CommandConsole commandConsole) {
        Set<String> fixesToReApply = new HashSet<String>();
        // Get all the product feature manifests, and process each one individually.
        for (Map.Entry<String, Map<String, ProvisioningFeatureDefinition>> entry : mfp.getFeatureDefinitionsByProduct().entrySet()) {
            String productExtensionName = entry.getKey();
            Map<String, ProvisioningFeatureDefinition> productFeatures = entry.getValue();

            ContentBasedLocalBundleRepository bundleRepo = null;
            File installLocation = new File(mfp.getProdFeatureLocation(productExtensionName));
            // For some reason the core feature name doesn't match the core Bundle Repo, so we have to special case this.
            // Otherwise get the bundle repo that matches the product feature id.
            if (productExtensionName == ManifestFileProcessor.CORE_PRODUCT_NAME) {
                bundleRepo = mfp.getBundleRepository(ExtensionConstants.CORE_EXTENSION, null);
            } else {
                bundleRepo = mfp.getBundleRepository(productExtensionName, null);
            }

            if (installLocation != null)
                fixesToReApply.addAll(IFixUtils.getIFixesThatMustBeReapplied(installLocation, productFeatures, bundleRepo, commandConsole));
        }
        return fixesToReApply;
    }

    public void setPrintErrorOnly(boolean printErrorOnly) {
        this.printErrorOnly = printErrorOnly;
    }

    public void setPrintStartMessage(boolean printStartMessage) {
        this.printStartMessage = printStartMessage;
    }

    public boolean isSuccessful() {
        return overallFailed == 0;
    }
}
