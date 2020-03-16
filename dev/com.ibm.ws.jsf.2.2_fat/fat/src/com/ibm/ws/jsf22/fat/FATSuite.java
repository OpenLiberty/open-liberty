/*
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.jsf22.fat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.jsf22.fat.tests.CDIConfigByACPTests;
import com.ibm.ws.jsf22.fat.tests.CDIFacesInMetaInfTests;
import com.ibm.ws.jsf22.fat.tests.CDIFacesInWebXMLTests;
import com.ibm.ws.jsf22.fat.tests.CDIFlowsTests;
import com.ibm.ws.jsf22.fat.tests.CDITests;
import com.ibm.ws.jsf22.fat.tests.JSF22AparTests;
import com.ibm.ws.jsf22.fat.tests.JSF22AppConfigPopTests;
import com.ibm.ws.jsf22.fat.tests.JSF22BeanValidationTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ClientWindowTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ComponentRendererTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ComponentTesterTests;
import com.ibm.ws.jsf22.fat.tests.JSF22FlashEventsTests;
import com.ibm.ws.jsf22.fat.tests.JSF22FlowsTests;
import com.ibm.ws.jsf22.fat.tests.JSF22IncludeTest;
import com.ibm.ws.jsf22.fat.tests.JSF22InputFileTests;
import com.ibm.ws.jsf22.fat.tests.JSF22LocalizationTesterTests;
import com.ibm.ws.jsf22.fat.tests.JSF22MiscLifecycleTests;
import com.ibm.ws.jsf22.fat.tests.JSF22MiscellaneousTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ResetValuesAndAjaxDelayTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ResourceLibraryContractHtmlUnit;
import com.ibm.ws.jsf22.fat.tests.JSF22StatelessViewTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ThirdPartyApiTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ViewActionAndPhaseIdTests;
import com.ibm.ws.jsf22.fat.tests.JSF22ViewPoolingTests;
import com.ibm.ws.jsf22.fat.tests.JSFCompELTests;
import com.ibm.ws.jsf22.fat.tests.JSFHtml5Tests;
import com.ibm.ws.jsf22.fat.tests.JSFHtmlUnit;
import com.ibm.ws.jsf22.fat.tests.JSFServerTest;
import com.ibm.ws.jsf22.fat.tests.JSFSimpleHtmlUnit;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * JSF 2.2 Tests
 *
 * Make sure to add any new test classes to the @SuiteClasses
 * annotation.
 *
 * By default only lite mode tests are run.
 *
 * Add "-Dfat.test.mode=full" to the end of your command, to run
 * the bucket in full mode.
 *
 * Tests will also run with JSF 2.3 feature due to @ClassRule RepeatTests
 */
@RunWith(Suite.class)
@SuiteClasses({
                JSFServerTest.class,
                JSFHtmlUnit.class,
                JSFSimpleHtmlUnit.class,
                JSF22StatelessViewTests.class,
                JSFHtml5Tests.class,
                JSF22ResourceLibraryContractHtmlUnit.class,
                JSF22ComponentTesterTests.class,
                JSF22ClientWindowTests.class,
                JSF22ComponentRendererTests.class,
                JSFCompELTests.class,
                JSF22FlowsTests.class,
                CDIFlowsTests.class,
                JSF22MiscellaneousTests.class,
                JSF22ViewActionAndPhaseIdTests.class,
                JSF22ResetValuesAndAjaxDelayTests.class,
                JSF22MiscLifecycleTests.class,
                JSF22AppConfigPopTests.class,
                JSF22FlashEventsTests.class,
                CDIConfigByACPTests.class,
                CDIFacesInMetaInfTests.class,
                CDIFacesInWebXMLTests.class,
                CDITests.class,
                JSF22BeanValidationTests.class,
                JSF22ViewPoolingTests.class,
                JSF22IncludeTest.class,
                JSF22InputFileTests.class,
                JSF22LocalizationTesterTests.class,
                JSF22AparTests.class,
                JSF22ThirdPartyApiTests.class
})
public class FATSuite {

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

    static Set<String> removeFeatures = new HashSet<>(Arrays.asList("jsf-2.2", "cdi-1.2", "beanValidation-1.1"));
    static Set<String> addFeatures = new HashSet<>(Arrays.asList("jsf-2.3", "cdi-2.0", "beanValidation-2.0"));

    /**
     * Run the tests again with the jsf-2.3 feature. Tests should be skipped where appropriate
     * using @SkipForRepeat("JSF-2.3").
     */
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(new FeatureReplacementAction(removeFeatures, addFeatures)
                                    .withID("JSF-2.3")
                                    .forceAddFeatures(false)
                                    .withMinJavaLevel(8));
}
