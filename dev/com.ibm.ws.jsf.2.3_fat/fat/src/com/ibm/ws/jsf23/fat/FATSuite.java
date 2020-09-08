/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;
import com.ibm.ws.jsf23.fat.tests.CDIConfigByACPTests;
import com.ibm.ws.jsf23.fat.tests.CDIFacesInMetaInfTests;
import com.ibm.ws.jsf23.fat.tests.CDIFacesInWebXMLTests;
import com.ibm.ws.jsf23.fat.tests.CDIInjectionTests;
import com.ibm.ws.jsf23.fat.tests.JSF23CDIGeneralTests;
import com.ibm.ws.jsf23.fat.tests.JSF23ClassLevelBeanValidationTests;
import com.ibm.ws.jsf23.fat.tests.JSF23CommandScriptTests;
import com.ibm.ws.jsf23.fat.tests.JSF23ComponentSearchTests;
import com.ibm.ws.jsf23.fat.tests.JSF23EvalScriptsTests;
import com.ibm.ws.jsf23.fat.tests.JSF23ExternalContextStartupShutdownTests;
import com.ibm.ws.jsf23.fat.tests.JSF23FaceletVDLTests;
import com.ibm.ws.jsf23.fat.tests.JSF23FacesDataModelTests;
import com.ibm.ws.jsf23.fat.tests.JSF23GeneralTests;
import com.ibm.ws.jsf23.fat.tests.JSF23IterableSupportTests;
import com.ibm.ws.jsf23.fat.tests.JSF23JPA22Test;
import com.ibm.ws.jsf23.fat.tests.JSF23JSF22SingletonFeatureTest;
import com.ibm.ws.jsf23.fat.tests.JSF23MapSupportTests;
import com.ibm.ws.jsf23.fat.tests.JSF23SelectOneRadioGroupTests;
import com.ibm.ws.jsf23.fat.tests.JSF23UIRepeatConditionTests;
import com.ibm.ws.jsf23.fat.tests.JSF23UISelectManyTests;
import com.ibm.ws.jsf23.fat.tests.JSF23ViewParametersTests;
import com.ibm.ws.jsf23.fat.tests.JSF23ViewResourceTests;
import com.ibm.ws.jsf23.fat.tests.JSF23WebSocketTests;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * JSF 2.3 Tests
 *
 * Make sure to add any new test classes to the @SuiteClasses
 * annotation.
 *
 * Make sure to distinguish FULL mode tests using
 * <code>@Mode(TestMode.FULL)</code>. Tests default to
 * use LITE mode (<code>@Mode(TestMode.LITE)</code>).
 *
 * By default only LITE mode tests are run. To also run
 * full mode tests a property must be specified:
 *
 * -Dfat.test.mode=FULL.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                JSF23FaceletVDLTests.class,
                JSF23CDIGeneralTests.class,
                JSF23GeneralTests.class,
                JSF23WebSocketTests.class,
                JSF23MapSupportTests.class,
                JSF23IterableSupportTests.class,
                JSF23ComponentSearchTests.class,
                JSF23UIRepeatConditionTests.class,
                JSF23FacesDataModelTests.class,
                JSF23ClassLevelBeanValidationTests.class,
                JSF23ExternalContextStartupShutdownTests.class,
                JSF23JSF22SingletonFeatureTest.class,
                JSF23CommandScriptTests.class,
                JSF23SelectOneRadioGroupTests.class,
                JSF23JPA22Test.class,
                JSF23EvalScriptsTests.class,
                JSF23ViewParametersTests.class,
                JSF23UISelectManyTests.class,
                JSF23ViewResourceTests.class,
                CDIInjectionTests.class,
                CDIFacesInMetaInfTests.class,
                CDIFacesInWebXMLTests.class,
                CDIConfigByACPTests.class
})
public class FATSuite {

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());
}
