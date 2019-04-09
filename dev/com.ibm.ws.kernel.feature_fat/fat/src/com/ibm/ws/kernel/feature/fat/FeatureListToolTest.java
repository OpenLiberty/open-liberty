/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.kernel.feature.fat;

import org.junit.BeforeClass;
import org.junit.Test;

import componenttest.topology.impl.LibertyServerFactory;

public class FeatureListToolTest extends FeatureToolTestCommon {

    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        setupEnv(LibertyServerFactory.getLibertyServer("com.ibm.ws.kernel.feature.fat.tool"));
        setupProductExtensions(SETUP_ALL_PROD_EXTS);

    }

    /**
     * Tests that the featurelist's tool help displays the --productExtension option.
     * 
     * @throws Exception
     */
    @Test
    public void testFeatureListToolProductExtensionParmHelpDisplay() throws Exception {
        testFeatureToolProductExtensionParmHelpDisplay(javaExc, new String[] { "-jar", installRoot + "/bin/tools/ws-featurelist.jar", "--help" }, installRoot);
    }

    /**
     * Test ws-featurelist.jar with --productExtension=testproduct where the features folder for the product extension is empty.
     * The request is expected to fail and issue message: CWWKG0078E.
     * 
     * @throws Exception
     */
    @Test
    public void testFeatureListToolUsingProductWithNoFeatureMfs() throws Exception {
        testFeatureToolUsingProductWithNoFeatureMfs(javaExc,
                                                    new String[] { "-jar", installRoot + "/bin/tools/ws-featurelist.jar", "--productExtension=testproduct",
                                                                  installRoot + "/tool.output.dir/prodExtFeaturelistNoFeatures.xml" },
                                                    installRoot,
                                                    "CWWKG0078E");
    }

    /**
     * Test ws-featurelist.jar with a product extension name argument pointing to a product that does not exist: --productExtension=testproductbadName.
     * The request is expected to fail and issue message: CWWKG0080E.
     * 
     * @throws Exception
     */
    @Test
    public void testFeatureListToolUsingBadProductExtNameArgument() throws Exception {
        testFeatureToolUsingBadProductExtNameArgument(javaExc,
                                                      new String[] { "-jar", installRoot + "/bin/tools/ws-featurelist.jar",
                                                                    "--productExtension=testproductbadName", installRoot + "/tool.output.dir/prodExtFeaturelistBadNameArg.xml" },
                                                      installRoot,
                                                      "CWWKG0080E");
    }

    /**
     * Test ws-featurelist.jar with --productExtension=testproduct where the com.ibm.websphere.productInstall in the
     * product extension's properties file points to "".
     * The request is expected to fail and issue message: CWWKG0079E.
     * 
     * @throws Exception
     */
    @Test
    public void testFeatureListToolUsingEmptyInstallLocationInProdExtPropsFile() throws Exception {
        testFeatureToolUsingEmptyInstallLocationInProdExtPropsFile(javaExc,
                                                                   new String[] { "-jar", installRoot + "/bin/tools/ws-featurelist.jar",
                                                                                 "--productExtension=testproduct",
                                                                                 installRoot + "/tool.output.dir/prodExtFeaturelistWithInvalidInstLocInPropsFile.xml" },
                                                                   installRoot,
                                                                   "CWWKG0079E");
    }

    /**
     * Test ws-featurelist.jar without the --productExtension argument.
     * Only Core features expected in the output list.
     * 
     * @throws Exception
     */
    @Test
    public void testFeatureListToolWithNoProdExtArgument() throws Exception {
        testFeatureToolWithNoProdExtArgument(javaExc,
                                             new String[] { "-jar", installRoot + "/bin/tools/ws-featurelist.jar",
                                                           installRoot + "/tool.output.dir/coreFeaturelist.xml" },
                                             installRoot);
    }

    /**
     * Test ws-featurelist.jar with --productExtension=usr argument.
     * Only features in the default user product extension location are expected in the output list.
     * 
     * @throws Exception
     */
    @Test
    public void testFeatureListToolWithUsrProdExtArgument() throws Exception {
        testFeatureToolWithUsrProdExtArgument(javaExc,
                                              new String[] { "-jar", installRoot + "/bin/tools/ws-featurelist.jar",
                                                            "--productExtension=usr", installRoot + "/tool.output.dir/usrFeaturelist.xml" },
                                              installRoot);
    }

    /**
     * Test ws-featurelist.jar with the --productExtension=testproduct argument.
     * Only features in the default user product extension testproduct are expected in the output list.
     * 
     * @throws Exception
     */
    @Test
    public void testFeatureListToolWithProdExtArgument() throws Exception {
        testFeatureToolWithProdExtArgument(javaExc, new String[] { "-jar", installRoot + "/bin/tools/ws-featurelist.jar",
                                                                  "--productExtension=testproduct", installRoot + "/tool.output.dir/prodExtFeaturelist.xml" },
                                           installRoot);
    }
}