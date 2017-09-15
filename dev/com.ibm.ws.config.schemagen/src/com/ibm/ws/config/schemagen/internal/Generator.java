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
package com.ibm.ws.config.schemagen.internal;

import java.io.File;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.ibm.ws.config.xml.internal.XMLConfigConstants;
import com.ibm.ws.config.xml.internal.schema.MetaTypeInformationSpecification;
import com.ibm.ws.config.xml.internal.schema.SchemaMetaTypeParser;
import com.ibm.ws.kernel.feature.internal.generator.ManifestFileProcessor;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry.BundleRepositoryHolder;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;

/**
 *
 */
public class Generator {

    public static final ResourceBundle messages = ResourceBundle.getBundle(XMLConfigConstants.NLS_PROPS);
    public static final ResourceBundle options = ResourceBundle.getBundle(XMLConfigConstants.NLS_OPTIONS);

    private static final String JAR_NAME = "ws-schemagen.jar";

    /**
     * Pick and use a consistent set of return codes across all
     * platforms. Most common range is 0 to 256.
     */
    public enum ReturnCode {
        OK(0),
        // Jump a few numbers for error return codes
        BAD_ARGUMENT(20),
        RUNTIME_EXCEPTION(21),

        // All "actions" should be < 0, these are not returned externally
        HELP_ACTION(-1),
        GENERATE_ACTION(-2);

        final int val;

        ReturnCode(int val) {
            this.val = val;
        }

        public int getValue() {
            return val;
        }
    }

    /**
     * The runtime main. String arguments are treated and parsed as
     * command line parameters.
     * 
     * @param args
     *            Command line arguments.
     */
    public static void main(String[] args) {
        Generator schemaGen = new Generator();
        System.exit(schemaGen.createSchema(args));
    }

    public static final String CORE_PRODUCT_NAME = "core";
    public static final String USR_PRODUCT_EXT_NAME = "usr";
    
    private final GeneratorOptions generatorOptions = new GeneratorOptions();

    public Generator() { }

    /**
     * @param args
     * @return
     */
    int createSchema(String[] args) {
        ReturnCode rc = ReturnCode.OK;

        try {
            rc = generatorOptions.processArgs(args);
            // Now perform more general processing based on the current return code
            switch (rc) {
                case GENERATE_ACTION:
                    rc = ReturnCode.OK;

                    ManifestFileProcessor mfp = new ManifestFileProcessor();
                    Map<String, List<File>> bundlesByProductMap = new HashMap<String, List<File>>();
                    Map<String, Map<String, ProvisioningFeatureDefinition>> fdsByProd = mfp.getFeatureDefinitionsByProduct();
                    ContentBasedLocalBundleRepository lbr = null;

                    for (Map.Entry<String, Map<String, ProvisioningFeatureDefinition>> entry : fdsByProd.entrySet()) {
                        String productName = entry.getKey();
                        Map<String, ProvisioningFeatureDefinition> fds = entry.getValue();
                        
                        if (productName.equals(CORE_PRODUCT_NAME)) {
                            lbr = BundleRepositoryRegistry.getInstallBundleRepository();
                        } else if (productName.equals(USR_PRODUCT_EXT_NAME)) {
                            lbr = BundleRepositoryRegistry.getUsrInstallBundleRepository();
                        } else {
                            BundleRepositoryHolder brh = BundleRepositoryRegistry.getRepositoryHolder(productName);
                            lbr = (brh != null) ? brh.getBundleRepository() : null;
                        }
                        List<File> bundles = new ArrayList<File>();
                        for (ProvisioningFeatureDefinition def : fds.values()) {
                            for (FeatureResource bundle : def.getConstituents(SubsystemContentType.BUNDLE_TYPE)) {
                                if (lbr != null) {
                                    File f = lbr.selectBundle(bundle.getLocation(), bundle.getSymbolicName(), bundle.getVersionRange());
                                    if (f != null) {
                                        bundles.add(f);
                                    }
                                }                               
                            }
                        }

                        bundlesByProductMap.put(productName, bundles);
                    }

                    SchemaMetaTypeParser smtp = new SchemaMetaTypeParser(generatorOptions.getLocale(), bundlesByProductMap);
                    generate(smtp.getMetatypeInformation());
                    break;
                case HELP_ACTION:
                    // Only show command-line-style brief usage -help or --help invoked from command line
                    System.out.println(MessageFormat.format(options.getString("briefUsage"), JAR_NAME));
                    System.out.println();
                    showUsageInfo();

                    rc = ReturnCode.OK;
                    break;
                default:
                    rc = ReturnCode.BAD_ARGUMENT;
                    break;
            }
        // These exceptions relate to error flows where we just output the message
        } catch (SchemaGeneratorException e) {
          System.out.println(e.getMessage());
          rc = ReturnCode.RUNTIME_EXCEPTION;
        // These exceptions are ones we didn't expect during development so we need the stack trace for service purposes.
        } catch (RuntimeException e) {
            System.out.println(MessageFormat.format(messages.getString("error.schemaGenException"), e.getMessage()));
            e.printStackTrace();
            rc = ReturnCode.RUNTIME_EXCEPTION;
        }

        return rc.getValue();
    }

    /**
     * Pass the constructed metatypinformation to SchemaWriter for the actual generation
     * 
     * @param metatype
     * @param outputFile
     */
    private void generate(List<MetaTypeInformationSpecification> metatype) {
        XMLOutputFactory factory = XMLOutputFactory.newInstance();
        try {
            PrintWriter writer = new PrintWriter(generatorOptions.getOutputFile(), generatorOptions.getEncoding());
            XMLStreamWriter xmlWriter = null;
            if (generatorOptions.getCompactOutput()) {
            	 xmlWriter = new CompactOutputXMLStreamWriter(factory.createXMLStreamWriter(writer));
            } else {
            	 xmlWriter = new IndentingXMLStreamWriter(factory.createXMLStreamWriter(writer), writer);
            }
            SchemaWriter schemaWriter = new SchemaWriter(xmlWriter);
            schemaWriter.setIgnoredPids(generatorOptions.getIgnoredPids());
            schemaWriter.setGenerateDocumentation(true);
            schemaWriter.setEncoding(generatorOptions.getEncoding());
            schemaWriter.setLocale(generatorOptions.getLocale());
            schemaWriter.setSchemaVersion(generatorOptions.getSchemaVersion());
            schemaWriter.setOutputVersion(generatorOptions.getOutputVersion());

            for (MetaTypeInformationSpecification item : metatype) {
                schemaWriter.add(item);
            }
            schemaWriter.generate(true);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param b
     */
    private void showUsageInfo() {
        final String okpfx = "option-key.";
        final String odpfx = "option-desc.";

        // Kernel feature list tools and schema tools for some reason share the same configuration options file.
        // Create an exclusion set to prevent the schema generator tool help from displaying undesired information.
        Set<String> exclusionSet = new HashSet<String>();
        exclusionSet.add("option-key.productExtension");
        
        Enumeration<String> keys = options.getKeys();
        Set<String> optionKeys = new TreeSet<String>();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (key.startsWith(okpfx) && !exclusionSet.contains(key)) {
                optionKeys.add(key);
            }
        }

        if (optionKeys.size() > 0) {
            System.out.println(options.getString("use.options"));
            System.out.println();

            // Print each option and it's associated descriptive text
            for (String optionKey : optionKeys) {
                String option = optionKey.substring(okpfx.length());
                System.out.println(options.getString(optionKey));
                System.out.println(options.getString(odpfx + option));
                System.out.println();
            }
        }
    }
}
