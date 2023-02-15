/*******************************************************************************
 * Copyright (c) 2017, 2023 IBM Corporation and others.
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
package app.injection;

import static org.junit.Assert.assertTrue;

import java.util.Properties;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import app.injection.Injectables.NonBatchArtifact;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE9Action;
import fat.util.JobWaiter;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/BatchInjectionServlet")
public class BatchInjectionServlet extends FATServlet {

    public static Logger logger = Logger.getLogger("test");

    /**
     * Test that, inside the context of a running batch job, (on the same thread as an executing batch job), the batch context
     * gets injected into other CDI managed beans.
     *
     * Lump this all into one test. Test that all injections are handled by executing the job and looking for COMPLETED status.
     *
     * Because app uses injection which will inject all beans satisfying injection condition, it is
     * easier to have one big test which verifies all the correct injections do (or don't) happen,
     * than it is to have a more fine-grained set of test methods saying which injections do or don't work.
     *
     * To find logic validating injection,
     *
     * @see app.injection.Injectables
     */
    @Test
    @Mode(TestMode.LITE)
    public void testInjectionWithinBatchJob() throws Exception {
        logger.fine("Running test = testInjectionWithinBatchJob");
        new JobWaiter().completeNewJob("Injection", null);
    }

    /**
     * Test that non-String BatchProperty values can be injected via CDI. Only applicable to EE 10 features (batch-2.1).
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    @SkipForRepeat({ EmptyAction.ID, EE7FeatureReplacementAction.ID, EE8FeatureReplacementAction.ID, JakartaEE9Action.ID })
    public void testInjectionNonStringProperties() throws Exception {
        logger.fine("Running test = testInjectionNonStringProperties");
        Properties jobProps = new Properties();
        jobProps.put("color", "blue");
        jobProps.put("quantity", "4");
        jobProps.put("shortProp", "13");
        jobProps.put("longProp", "2048000");
        jobProps.put("floatProp", "60.305");
        jobProps.put("doubleProp", "120.61");
        jobProps.put("boolProp", "true");

        new JobWaiter().completeNewJob("InjectionNonStringProps", jobProps);
    }

    /**
     * Test the failure condition when a BatchProperty cannot be converted to its declared type.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.LITE)
    @SkipForRepeat({ EmptyAction.ID, EE7FeatureReplacementAction.ID, EE8FeatureReplacementAction.ID, JakartaEE9Action.ID })
    @ExpectedFFDC({ "java.lang.NumberFormatException", "java.lang.RuntimeException" })
    public void testInjectionNonStringPropertiesBadType() throws Exception {
        logger.fine("Running test = testInjectionNonStringProperties");
        Properties jobProps = new Properties();
        jobProps.put("color", "blue");
        jobProps.put("quantity", "circle"); // Not an int
        jobProps.put("shortProp", "13");
        jobProps.put("longProp", "2048000");
        jobProps.put("floatProp", "60.305");
        jobProps.put("doubleProp", "120.61");
        jobProps.put("boolProp", "true");

        boolean jobFailed = false;
        try {
            new JobWaiter().completeNewJob("InjectionNonStringProps", jobProps);
        } catch (IllegalStateException e) {
            jobFailed = true;
        }
        assertTrue("Injection job did not fail when using an incorrect property type", jobFailed);
    }

    @Inject
    NonBatchArtifact nonArtifact;

    /**
     * Test that, outside the context of a running batch job, the batch context does NOT get injected into
     * CDI managed beans.
     */
    @Test
    @Mode(TestMode.LITE)
    public void testNullInjectionOutsideOfBatchJob() throws Exception {
        logger.fine("Running test = testNullInjectionOutsideOfBatchJob");
        nonArtifact.assertBatchInjectionsNull();
    }

}
