/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.cmdline;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.FeatureResolverImpl;
import com.ibm.ws.kernel.feature.internal.generator.ManifestFileProcessor;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureRepository;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.wsspi.kernel.service.utils.ParserUtils;

public class ClasspathAction implements ActionHandler {
    /**
     * Information about an installed product.
     */
    private static class ProductInfo {
        final ContentBasedLocalBundleRepository repository;
        final Map<String, FeatureInfo> featuresByShortName = new TreeMap<String, FeatureInfo>(String.CASE_INSENSITIVE_ORDER);

        ProductInfo(ContentBasedLocalBundleRepository repository) {
            this.repository = repository;
        }
    }

    /**
     * Information about a feature in a product.
     */
    private static class FeatureInfo {
        final ProductInfo productInfo;
        final ProvisioningFeatureDefinition feature;

        FeatureInfo(ProductInfo productInfo, ProvisioningFeatureDefinition feature) {
            this.productInfo = productInfo;
            this.feature = feature;
        }

        @Override
        public String toString() {
            return feature.toString();
        }
    }

    @Override
    public ExitCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
        String output = args.getPositionalArguments().get(0);

        String featuresString = args.getOption("features");
        String[] features = featuresString == null || featuresString.trim().isEmpty() ? null : featuresString.split("[\\s,]+");
        if (features == null || features.length == 0) {
            stderr.println(NLS.getMessage("missing.option", args.getAction(), "--features"));
            return ReturnCode.BAD_ARGUMENT;
        }

        // Collect information about all installed features.
        Map<String, ProductInfo> productInfos = new HashMap<String, ProductInfo>();
        Map<String, FeatureInfo> featuresBySymbolicName = new HashMap<String, FeatureInfo>();
        collectFeatureInfos(productInfos, featuresBySymbolicName);

        // Look up the features specified by the user.
        List<FeatureInfo> featureInfos = new ArrayList<FeatureInfo>();
        for (String name : features) {
            FeatureInfo featureInfo = getFeatureInfo(name, productInfos, featuresBySymbolicName);
            if (featureInfo == null) {
                stderr.println(NLS.getMessage("classpath.feature.not.found", name));
                return ReturnCode.BAD_ARGUMENT;
            }

            if (featureInfo.feature.getVisibility() != Visibility.PUBLIC) {
                stderr.println(NLS.getMessage("classpath.feature.not.public", name));
                return ReturnCode.BAD_ARGUMENT;
            }

            featureInfos.add(featureInfo);
        }

        FeatureRepository repo = new FeatureRepository();
        repo.init();
        FeatureResolver resolver = new FeatureResolverImpl();
        Result r = resolver.resolveFeatures(repo, Collections.<ProvisioningFeatureDefinition> emptySet(), Arrays.asList(features), Collections.<String> emptySet(), false);

        Map<String, FeatureInfo> featuresToCheck = new HashMap<String, FeatureInfo>();
        for (String name : r.getResolvedFeatures()) {
            featuresToCheck.put(name, getFeatureInfo(name, productInfos, featuresBySymbolicName));
        }
        // Collect the API JARs from the user-specified features.
        Set<File> apiJars = new LinkedHashSet<File>();
        for (FeatureInfo featureInfo : featureInfos) {
            collectAPIJars(featureInfo, featuresToCheck, apiJars);
        }

        File outputFile = new File(output);

        // Build the relative classpaths from the output JAR to the API JARs.
        StringBuilder jarClasspath = new StringBuilder();
        for (File apiJar : apiJars) {
            String relativePath = getRelativePath(outputFile, apiJar);
            if (relativePath == null) {
                stderr.println(NLS.getMessage("classpath.wrong.drive", outputFile.getAbsolutePath(), apiJar.getAbsolutePath()));
                return ReturnCode.BAD_ARGUMENT;
            }

            // http://docs.oracle.com/javase/7/docs/technotes/guides/jar/jar.html
            // "The value of [the Class-Path] attribute specifies the relative
            // URLs of the extensions or libraries that this application or
            // extension needs. URLs are separated by one or more spaces."
            if (jarClasspath.length() != 0) {
                jarClasspath.append(' ');
            }
            jarClasspath.append(ParserUtils.encode(relativePath));
        }

        // Create the output JAR.
        try {
            createClasspathJar(outputFile, jarClasspath.toString());
        } catch (IOException e) {
            stderr.println(NLS.getMessage("classpath.create.fail", outputFile.getAbsolutePath(), e.getLocalizedMessage()));
            return ReturnCode.IO_FAILURE;
        }

        return ReturnCode.OK;
    }

    /**
     * Collect information about all installed products and their features.
     *
     * @param productInfos result parameter of product name (prefix) to info
     * @param featuresBySymbolicName result parameter of symbolic name to info
     */
    private void collectFeatureInfos(Map<String, ProductInfo> productInfos,
                                     Map<String, FeatureInfo> featuresBySymbolicName) {
        ManifestFileProcessor manifestFileProcessor = new ManifestFileProcessor();
        for (Map.Entry<String, Map<String, ProvisioningFeatureDefinition>> productEntry : manifestFileProcessor.getFeatureDefinitionsByProduct().entrySet()) {
            String productName = productEntry.getKey();
            Map<String, ProvisioningFeatureDefinition> features = productEntry.getValue();

            ContentBasedLocalBundleRepository repository;
            if (productName.equals(ManifestFileProcessor.CORE_PRODUCT_NAME)) {
                repository = BundleRepositoryRegistry.getInstallBundleRepository();
            } else if (productName.equals(ManifestFileProcessor.USR_PRODUCT_EXT_NAME)) {
                repository = BundleRepositoryRegistry.getUsrInstallBundleRepository();
            } else {
                repository = manifestFileProcessor.getBundleRepository(productName, null);
            }

            ProductInfo productInfo = new ProductInfo(repository);
            productInfos.put(productName, productInfo);

            for (Map.Entry<String, ProvisioningFeatureDefinition> featureEntry : features.entrySet()) {
                String featureSymbolicName = featureEntry.getKey();
                ProvisioningFeatureDefinition feature = featureEntry.getValue();

                FeatureInfo featureInfo = new FeatureInfo(productInfo, feature);
                featuresBySymbolicName.put(featureSymbolicName, featureInfo);

                String shortName = feature.getIbmShortName();
                if (shortName != null) {
                    productInfo.featuresByShortName.put(shortName, featureInfo);
                }
            }
        }
    }

    /**
     * Gets a feature by its name using the server.xml featureManager algorithm.
     *
     * @param name the user-specified feature name
     * @param productInfos product name (prefix) to info
     * @param featuresBySymbolicName symbolic name to info
     * @return
     */
    private FeatureInfo getFeatureInfo(String name,
                                       Map<String, ProductInfo> productInfos,
                                       Map<String, FeatureInfo> featuresBySymbolicName) {
        String productName, featureName;
        int index = name.indexOf(':');
        if (index == -1) {
            FeatureInfo featureInfo = featuresBySymbolicName.get(name);
            if (featureInfo != null) {
                return featureInfo;
            }

            productName = ManifestFileProcessor.CORE_PRODUCT_NAME;
            featureName = name;
        } else {
            productName = name.substring(0, index);
            featureName = name.substring(index + 1);
        }

        ProductInfo product = productInfos.get(productName);
        return product == null ? null : product.featuresByShortName.get(featureName);
    }

    /**
     * The feature subsystem content types that can contain API JARs.
     */
    private static final SubsystemContentType[] JAR_CONTENT_TYPES = { SubsystemContentType.BUNDLE_TYPE, SubsystemContentType.JAR_TYPE };

    /**
     * Collect API JARs from a feature and its recursive dependencies.
     *
     * @param featureInfo the feature to process
     * @param visitedFeaturesBySymbolicName result set of visited features
     * @param apiJars result set of API JAR files
     * @param featuresBySymbolicName symbolic name to info
     */
    private void collectAPIJars(FeatureInfo featureInfo,
                                Map<String, FeatureInfo> allowedFeatures,
                                Set<File> apiJars) {
        for (SubsystemContentType contentType : JAR_CONTENT_TYPES) {
            for (FeatureResource resource : featureInfo.feature.getConstituents(contentType)) {
                if (APIType.getAPIType(resource) == APIType.API) {
                    File file = featureInfo.productInfo.repository.selectBundle(resource.getLocation(), resource.getSymbolicName(), resource.getVersionRange());
                    if (file != null) {
                        apiJars.add(file);
                    }
                }
            }
        }

        for (FeatureResource resource : featureInfo.feature.getConstituents(SubsystemContentType.FEATURE_TYPE)) {
            String name = resource.getSymbolicName();
            FeatureInfo childFeatureInfo = allowedFeatures.get(name);
            if (childFeatureInfo != null && APIType.API.matches(resource)) {
                allowedFeatures.remove(name);
                collectAPIJars(childFeatureInfo, allowedFeatures, apiJars);
            }
        }
    }

    /**
     * Return a relative path from the directory of the source file to the
     * target file. If the target file is on a different drive (for example,
     * "C:" vs "D:" on Windows), then there is no relative path, and null will
     * be returned.
     */
    private static String getRelativePath(File src, File target) {
        String[] srcComponents = getCanonicalPath(src).replace(File.separatorChar, '/').split("/");
        String[] targetComponents = getCanonicalPath(target).replace(File.separatorChar, '/').split("/");

        if (srcComponents.length > 0 && targetComponents.length > 0 && !targetComponents[0].equals(srcComponents[0])) {
            return null;
        }

        int common = 0;
        for (int minLength = Math.min(srcComponents.length, targetComponents.length); common < minLength; common++) {
            if (!srcComponents[common].equals(targetComponents[common])) {
                break;
            }
        }

        StringBuilder result = new StringBuilder();

        // Paths in a file are relative to the file's directory, so subtract one
        // to skip the final filename component.
        for (int i = common; i < srcComponents.length - 1; i++) {
            if (result.length() != 0) {
                result.append('/');
            }
            result.append("..");
        }

        for (int i = common; i < targetComponents.length; i++) {
            if (result.length() != 0) {
                result.append('/');
            }
            result.append(targetComponents[i]);
        }

        return result.toString();
    }

    private static String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            e.getClass(); // findbugs
            return file.getAbsolutePath();
        }
    }

    /**
     * Writes a JAR with a MANIFEST.MF containing the Class-Path string.
     *
     * @param outputFile the output file to create
     * @param classpath the Class-Path string
     * @throws IOException if an error occurs creating the file
     */
    private void createClasspathJar(File outputFile, String classpath) throws IOException {
        FileOutputStream out = new FileOutputStream(outputFile);
        try {
            Manifest manifest = new Manifest();
            Attributes attrs = manifest.getMainAttributes();
            attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            attrs.put(Attributes.Name.CLASS_PATH, classpath);
            new JarOutputStream(out, manifest).close();
        } finally {
            out.close();
        }
    }
}
