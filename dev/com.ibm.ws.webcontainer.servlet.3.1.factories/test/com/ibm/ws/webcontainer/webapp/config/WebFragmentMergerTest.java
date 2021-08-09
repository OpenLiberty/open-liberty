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
package com.ibm.ws.webcontainer.webapp.config;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.ibm.ws.webcontainer.filter.FilterMapping;
import com.ibm.ws.webcontainer.osgi.webapp.WebAppConfiguration;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

/**
 *
 */
public class WebFragmentMergerTest {

    @Test
    public void testDataSourceOverrideWebXmlMerge() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "DataSourceOverrideWebxmlMerge");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testDataSourceNotMerge() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "DataSourceNotMerge");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test(expected = UnableToAdaptException.class)
    public void testDataSourceOverrideWebxmlConflict() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "DataSourceOverrideWebxmlConflict");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetServiceRef30() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "InjectionTargetServiceRef");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetServiceRef31() throws Exception {
        List<String> errors = testXMLMergeHelper(31, "InjectionTargetServiceRef");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetResRef30() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "InjectionTargetResRef");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetResRef31() throws Exception {
        List<String> errors = testXMLMergeHelper(31, "InjectionTargetResRef");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetResEnvRef30() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "InjectionTargetResEnvRef");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetResEnvRef31() throws Exception {
        List<String> errors = testXMLMergeHelper(31, "InjectionTargetResEnvRef");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetEjbref30() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "InjectionTargetEjbref");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetEjbref31() throws Exception {
        List<String> errors = testXMLMergeHelper(31, "InjectionTargetEjbref");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetEjblocal30() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "InjectionTargetEjblocal");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetEjblocal31() throws Exception {
        List<String> errors = testXMLMergeHelper(31, "InjectionTargetEjblocal");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetEnvEntry30() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "InjectionTargetEnvEntry");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetEnvEntry31() throws Exception {
        List<String> errors = testXMLMergeHelper(31, "InjectionTargetEnvEntry");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetMsgRef30() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "InjectionTargetMsgRef");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetMsgRef31() throws Exception {
        List<String> errors = testXMLMergeHelper(31, "InjectionTargetMsgRef");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetPersConRef30() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "InjectionTargetPersConRef");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetPersConRef31() throws Exception {
        List<String> errors = testXMLMergeHelper(31, "InjectionTargetPersConRef");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetPersUnitRef30() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "InjectionTargetPersUnitRef");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testInjectionTargetPersUnitRef31() throws Exception {
        List<String> errors = testXMLMergeHelper(31, "InjectionTargetPersUnitRef");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testResourceRefDifferentWebOverride() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "AllRefDifferentWebOverride");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testResourceRefIdenticalRefsFragmentsOnly() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "AllRefIdenticalRefsFragmentsOnly");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test(expected = UnableToAdaptException.class)
    public void testResourceRefDifferentRefsFragmentsOnly() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "ResourceRefDifferentRefsFragmentsOnly");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test(expected = UnableToAdaptException.class)
    public void testEjbrefDifferentRefsFragmentsOnly() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "EjbrefDifferentRefsFragmentsOnly");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test(expected = UnableToAdaptException.class)
    public void testEjblocalDifferentRefsFragmentsOnly() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "EjblocalDifferentRefsFragmentsOnly");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test(expected = UnableToAdaptException.class)
    public void testEnvEntryDifferentRefsFragmentsOnly() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "EnvEntryDifferentRefsFragmentsOnly");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test(expected = UnableToAdaptException.class)
    public void testMsgRefDifferentRefsFragmentsOnly() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "MsgRefDifferentRefsFragmentsOnly");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test(expected = UnableToAdaptException.class)
    public void testPersConRefDifferentRefsFragmentsOnly() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "PersConRefDifferentRefsFragmentsOnly");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test(expected = UnableToAdaptException.class)
    public void testResourceEnvRefDifferentRefsFragmentsOnly() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "ResourceEnvRefDifferentRefsFragmentsOnly");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test(expected = UnableToAdaptException.class)
    public void testServiceRefDifferentRefsFragmentsOnly() throws Exception {
        List<String> errors = testXMLMergeHelper(30, "ServiceRefDifferentRefsFragmentsOnly");
        assertTrue(errors.toString(), errors.isEmpty());
    }

    @Test
    public void testFilterMappingAddFromFragments() throws Exception {

        String testFolder = "FilterMappingAddFromFragments/";

        String webXmlPath = "test-resources/mergeTestXmls/" + testFolder + "web.xml";
        String fragment1Path = "test-resources/mergeTestXmls/" + testFolder + "web-fragment1.xml";
        String fragment2Path = "test-resources/mergeTestXmls/" + testFolder + "web-fragment2.xml";

        WebAppConfiguration configUnderTest = ServletConfigMock.mergeTestXmls(30, webXmlPath, fragment1Path, fragment2Path);

        List filterMappings = configUnderTest.getFilterMappings();
        boolean passed = true;
        for (Object mapping : filterMappings) {
            FilterMapping typedMapping = (FilterMapping) mapping;

            if (typedMapping.getFilterConfig().getFilterName().equals("WSCUrlFilter")) {
                if (typedMapping.getFilterConfig().getServletNameMappings().contains("Fragment1Servlet") &&
                    typedMapping.getFilterConfig().getServletNameMappings().contains("Fragment2Servlet")) {
                    passed = passed && true;
                } else {
                    passed = false;
                }
            }

            if (typedMapping.getFilterConfig().getFilterName().equals("SecondFilter")) {
                if (typedMapping.getFilterConfig().getServletNameMappings().contains("Fragment3Servlet")) {
                    passed = passed && true;
                } else {
                    passed = false;
                }
            }

        }

        assertTrue("Did not find all filter mappings", passed);
    }

    @Test
    public void testFilterMappingOverrideFromFragments() throws Exception {

        String testFolder = "FilterMappingOverrideFromFragments/";

        String webXmlPath = "test-resources/mergeTestXmls/" + testFolder + "web.xml";
        String fragment1Path = "test-resources/mergeTestXmls/" + testFolder + "web-fragment1.xml";
        String fragment2Path = "test-resources/mergeTestXmls/" + testFolder + "web-fragment2.xml";

        WebAppConfiguration configUnderTest = ServletConfigMock.mergeTestXmls(30, webXmlPath, fragment1Path, fragment2Path);

        List filterMappings = configUnderTest.getFilterMappings();
        boolean passed = true;
        for (Object mapping : filterMappings) {
            FilterMapping typedMapping = (FilterMapping) mapping;

            if (typedMapping.getFilterConfig().getFilterName().equals("WSCUrlFilter")) {
                if (typedMapping.getFilterConfig().getServletNameMappings().contains("Override1Servlet")) {
                    passed = passed && true;
                } else {
                    passed = false;
                }
            }

            if (typedMapping.getFilterConfig().getFilterName().equals("SecondFilter")) {
                if (typedMapping.getFilterConfig().getServletNameMappings().contains("Override2Servlet")) {
                    passed = passed && true;
                } else {
                    passed = false;
                }
            }

        }

        assertTrue("Did not find all filter mappings", passed);
    }

    @Test
    public void testServletMappingAddFromFragments() throws Exception {

        String testFolder = "ServletMappingAddFromFragments/";

        String webXmlPath = "test-resources/mergeTestXmls/" + testFolder + "web.xml";
        String fragment1Path = "test-resources/mergeTestXmls/" + testFolder + "web-fragment1.xml";
        String fragment2Path = "test-resources/mergeTestXmls/" + testFolder + "web-fragment2.xml";

        WebAppConfiguration configUnderTest = ServletConfigMock.mergeTestXmls(30, webXmlPath, fragment1Path, fragment2Path);

        List<String> servletMappings = configUnderTest.getServletMappings("SimpleServlet");
        servletMappings.addAll(configUnderTest.getServletMappings("SimpleServlet2"));
        boolean passed = true;

        if (servletMappings.contains("/simple1")) {
            passed = passed && true;
        } else
            passed = false;
        if (servletMappings.contains("/simple2")) {
            passed = passed && true;
        } else
            passed = false;
        if (servletMappings.contains("/simple3")) {
            passed = passed && true;
        } else
            passed = false;

        assertTrue("Did not find all servlet mappings", passed);
    }

    @Test
    public void testServletMappingOverrideFromFragments() throws Exception {

        String testFolder = "ServletMappingOverrideFromFragments/";

        String webXmlPath = "test-resources/mergeTestXmls/" + testFolder + "web.xml";
        String fragment1Path = "test-resources/mergeTestXmls/" + testFolder + "web-fragment1.xml";
        String fragment2Path = "test-resources/mergeTestXmls/" + testFolder + "web-fragment2.xml";

        WebAppConfiguration configUnderTest = ServletConfigMock.mergeTestXmls(30, webXmlPath, fragment1Path, fragment2Path);

        List<String> servletMappings = configUnderTest.getServletMappings("SimpleServlet");
        servletMappings.addAll(configUnderTest.getServletMappings("SimpleServlet2"));
        boolean passed = true;

        if (servletMappings.contains("/simpleOverride1")) {
            passed = passed && true;
        } else
            passed = false;
        if (servletMappings.contains("/simpleOverride3")) {
            passed = passed && true;
        } else
            passed = false;

        assertTrue("Did not find all servlet mappings", passed);
    }

    private List<String> testXMLMergeHelper(int runtimeVersion, String folderToTest) throws Exception {

        String testFolder = folderToTest + "/";
        String expectedXML = "webExpected.xml";

        if (runtimeVersion == 30)
            expectedXML = "webExpected30.xml";
        else if (runtimeVersion == 31)
            expectedXML = "webExpected31.xml";

        String webXmlPath = "test-resources/mergeTestXmls/" + testFolder + "web.xml";
        String webXmlExpectedPath = "test-resources/mergeTestXmls/" + testFolder + expectedXML;
        String fragment1Path = "test-resources/mergeTestXmls/" + testFolder + "web-fragment1.xml";
        String fragment2Path = "test-resources/mergeTestXmls/" + testFolder + "web-fragment2.xml";

        WebAppConfiguration configUnderTest = ServletConfigMock.mergeTestXmls(runtimeVersion, webXmlPath, fragment1Path, fragment2Path);
        WebAppConfiguration expectedConfig = ServletConfigMock.mergeTestXmls(runtimeVersion, webXmlExpectedPath, new String[] {});
        List<String> errors = WebAppComparators.compareWebAppConfiguration(expectedConfig, configUnderTest);

        return errors;

    }
}
