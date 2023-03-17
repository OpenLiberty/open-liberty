/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.featureUtility.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.log.Log;

public class HelpActionTest extends  FeatureUtilityToolTest {
    private static final Class<?> c = HelpActionTest.class;


    @BeforeClass
    public static void beforeClassSetup() throws Exception {
        final String methodName = "beforeClassSetup";
	replaceWlpProperties(libertyVersion);
        Log.exiting(c, methodName);
    }

    @AfterClass
    public static void cleanUp() throws Exception {
        resetOriginalWlpProps();
    }

    @Test
    public void testHelp() throws Exception {
        final String METHOD_NAME = "testHelp";
        Log.entering(c, METHOD_NAME);

        // run the command
        String[] param1s = { "help"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();

        // check for feature utility
        assertTrue("Should contain featureUtility", output.contains("featureUtility"));
        // check for Actions
        assertTrue("Should contain options", output.contains("Actions"));

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testHelpInstallFeature() throws Exception {
        final String METHOD_NAME = "testHelpInstallFeature";
        Log.entering(c, METHOD_NAME);

        // run the command
        String[] param1s = { "help", "installFeature"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();

        assertTrue("Should contain featureUtility installFeature", output.contains("featureUtility installFeature"));

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testHelpInstallServerFeatures() throws Exception {
        final String METHOD_NAME = "testHelpInstallServerFeatures";
        Log.entering(c, METHOD_NAME);

        // run the command
        String[] param1s = { "help", "installServerFeatures"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();

        assertTrue("Should contain featureUtility installServerFeatures", output.contains("featureUtility installServerFeatures"));

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testHelpViewSettings() throws Exception {
        final String METHOD_NAME = "testHelpViewSettings";
        Log.entering(c, METHOD_NAME);

        // run the command
        String[] param1s = { "help", "viewSettings"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();

        assertTrue("Should contain featureUtility viewSettings", output.contains("featureUtility viewSettings"));

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    public void testHelpFind() throws Exception {
        final String METHOD_NAME = "testHelpFind";
        Log.entering(c, METHOD_NAME);

        // run the command
        String[] param1s = { "help", "find"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();

        assertTrue("Should contain featureUtility find", output.contains("featureUtility find"));

        Log.exiting(c, METHOD_NAME);
    }
    @Test
    public void testHelpHelp() throws Exception {
        final String METHOD_NAME = "testHelpHelp";
        Log.entering(c, METHOD_NAME);

        // run the command
        String[] param1s = { "help", "help"};
        ProgramOutput po = runFeatureUtility(METHOD_NAME, param1s);
        assertEquals("Exit code should be 0",0, po.getReturnCode());
        String output = po.getStdout();

        assertTrue("Should contain featureUtility help", output.contains("featureUtility help"));

        Log.exiting(c, METHOD_NAME);
    }
    
	@Test
	public void testUnknownAction() throws Exception {
		final String METHOD_NAME = "testUnknownAction";
		Log.entering(c, METHOD_NAME);

		// Run the command.
		String[] parms = { "invalidAction" }; // purposefully broken action
		ProgramOutput po = runFeatureUtility(METHOD_NAME, parms);

		// Validation.
		String validationMessage = "Unknown action: " + parms[0];
		assertEquals("Exit code should be 20", 20, po.getReturnCode());
		assertTrue("Should contain '" + validationMessage + "'", po.getStdout().contains(validationMessage));
	}


}
