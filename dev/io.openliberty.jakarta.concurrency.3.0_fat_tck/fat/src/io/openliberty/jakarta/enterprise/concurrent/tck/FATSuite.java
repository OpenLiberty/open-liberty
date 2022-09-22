/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jakarta.enterprise.concurrent.tck;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testng.xml.XmlPackage;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlSuite.FailurePolicy;
import org.testng.xml.XmlTest;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.TestModeFilter;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, //Need to have a passing test while normal tests are disabled
                ConcurrentTckLauncherFull.class,
                ConcurrentTckLauncherWeb.class
})
public class FATSuite {

    static enum PROFILE {
        FULL("eefull"),
        WEB("eeweb");

        String value;

        PROFILE(String value) {
            this.value = value;
        }
    }

    /**
     * Programmatically create a tck-suite-programmatic.xml file based on environment variables
     *
     * @return String suite file name
     * @throws IOException if we are unable to write contents of tck-suite-programmatic.xml to filesystem
     */
    public static String createSuiteXML(PROFILE profile) throws IOException {
        XmlSuite suite = new XmlSuite();
        suite.setFileName("tck-suite-programmatic.xml");
        suite.setName("jakarta-concurrency");
        suite.setVerbose(2);
        suite.setConfigFailurePolicy(FailurePolicy.CONTINUE);

        XmlTest test = new XmlTest(suite);
        test.setName("jakarta-concurrency-programmatic");

        XmlPackage apiPackage = new XmlPackage();
        XmlPackage specPackage = new XmlPackage();

        apiPackage.setName("ee.jakarta.tck.concurrent.api.*");
        specPackage.setName("ee.jakarta.tck.concurrent.spec.*");

        Set<String> apiExcludes = new HashSet<>();
        Set<String> specExcludes = new HashSet<>();

        /**
         * Exclude certain tests when running in lite mode
         */
        if (TestModeFilter.FRAMEWORK_TEST_MODE != Mode.TestMode.FULL) {
            Log.info(ConcurrentTckLauncherFull.class, "createSuiteXML", "Modifying API and Spec packages to exclude specific tests for lite mode.");
            apiExcludes.add("ee.jakarta.tck.concurrent.api.Trigger");
            specExcludes.addAll(Arrays.asList("ee.jakarta.tck.concurrent.spec.ManagedScheduledExecutorService.inheritedapi",
                                              "ee.jakarta.tck.concurrent.spec.ManagedScheduledExecutorService.inheritedapi_servlet"));
        }

        /**
         * Skip signature testing on Windows
         * So far as I can tell the signature test plugin is not supported on windows
         * Opened an issue against jsonb tck https://github.com/eclipse-ee4j/jsonb-api/issues/327
         */
        if (System.getProperty("os.name").contains("Windows")) {
            Log.info(ConcurrentTckLauncherFull.class, "createSuiteXML", "Skipping Signature Tests on Windows");
            specExcludes.add("ee.jakarta.tck.concurrent.spec.signature");
        }

        /**
         * If JDK is not in the supported list for signature testing, skip it.
         */
        int javaSpecVersion = Integer.parseInt(System.getProperty("java.specification.version"));
        if (!(javaSpecVersion == 11 || javaSpecVersion == 17)) {
            Log.info(ConcurrentTckLauncherFull.class, "createSuiteXML", "Skipping Signature Tests on unsupported JDK");
            specExcludes.add("ee.jakarta.tck.concurrent.spec.signature");
        }

        apiPackage.setExclude(new ArrayList<String>(apiExcludes));
        specPackage.setExclude(new ArrayList<String>(specExcludes));

        test.setPackages(Arrays.asList(apiPackage, specPackage));

        if (profile == PROFILE.FULL)
            test.setExcludedGroups(Arrays.asList(PROFILE.WEB.value));
        else
            test.setExcludedGroups(Arrays.asList(PROFILE.FULL.value));

        suite.setTests(Arrays.asList(test));

        Log.info(ConcurrentTckLauncherFull.class, "createSuiteXML", suite.toXml());

        //When this code runs it is running as part of an ant task already in the autoFVT directory.
        //Therefore, use a relative path to this file.
        String suiteXmlFileLocation = "publish/tckRunner/tck/" + suite.getFileName();
        try (FileWriter suiteXmlWriter = new FileWriter(suiteXmlFileLocation);) {
            suiteXmlWriter.write(suite.toXml());
            Log.info(ConcurrentTckLauncherFull.class, "createSuiteXML", "Wrote to " + suiteXmlFileLocation);
        }

        return suite.getFileName();
    }

}
