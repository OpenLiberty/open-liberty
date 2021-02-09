/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.custom.junit.runner;

import java.util.Locale;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Util;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Test filter which compares the current test mode against the
 * test mode which is set for a specific test method.
 * 
 * Test must have a test mode which is less than or equal to the
 * specified test mode.  For example, LITE is less than FULL, which
 * is less than QUARANTINE, which is less than EXPERIMENTAL.
 * 
 * When the test mode is set to LITE, only LITE mode tests are run.
 * When the test mode is set to FULL, only LITE and FULL mode tests
 * are run.
 * 
 */
public class TestModeFilter extends Filter {
    private static final Class<? extends TestModeFilter> c = TestModeFilter.class;

    /** The property which is used to specify the current test mode. */
    public static final String FAT_MODE_PROPERTY_NAME = "fat.test.mode";

    /** The default test mode for the framework. */
    public static TestMode DEFAULT_FRAMEWORK_MODE = TestMode.LITE;
    
    /** The default test mode for individual tests. */
    public static TestMode DEFAULT_TEST_MODE = TestMode.LITE;
    
    public static final TestMode FRAMEWORK_TEST_MODE;

    static {
        String modeProperty = System.getProperty(FAT_MODE_PROPERTY_NAME);
        
        FRAMEWORK_TEST_MODE = ((modeProperty != null) ? TestMode.valueOf(modeProperty.toUpperCase(Locale.ROOT)) : DEFAULT_FRAMEWORK_MODE );

        Log.info(c, "<clinit>",
            "FAT framework test mode (" + FAT_MODE_PROPERTY_NAME + ") (" + modeProperty + "): " + FRAMEWORK_TEST_MODE);
    }

    //

    /**
     * Compare a specified test mode with the current test mode,
     * telling if a test having the specified test mode should be run.
     * 
     * The test should be run if the specified test mode is less than
     * or equal to the current test mode.
     *
     * @param testMode The test mode which is to be compared.
     *
     * @return True or false telling if the specified test mode
     *     should be run.
     */
    public static boolean shouldRun(TestMode testMode) {
        return ( FRAMEWORK_TEST_MODE.compareTo(testMode) >= 0 );
    }

    //

    /**
     * Answer a print string for this filter.
     * 
     * @return A print string for this filter.
     */
    @Override
    public String describe() {        
        return "TestModeFilter(" + FRAMEWORK_TEST_MODE + ")";
    }

    /**
     * Tell if a test method should be run based on it's test mode.
     * 
     * Obtain the mode of the test method as a method annotation, then
     * as a class annotation.  If neither annotation is present, use
     * the default test mode {@link #DEFAULT_TEST_MODE}.  (The default
     * is set so that tests with no annotation are always run.)
     * 
     * @param desc The description of the test method.
     *
     * @return True or false telling if the test method is to be run.
     */
    @Override
    public boolean shouldRun(Description desc) {
        Mode mode = Util.getAnnotation(Mode.class, desc);

        TestMode testMode;
        if ( mode != null ) {
            testMode = mode.value();
        } else {
            testMode = DEFAULT_TEST_MODE;
        }

        boolean run = shouldRun(testMode);
        if ( !run ) {
            Log.info(c, "shouldRun",
                "Skipping test " + desc.getMethodName() +
                "; test mode " + testMode + " is inactive for framework mode " + FRAMEWORK_TEST_MODE);
            return false;
        } else {
            return true;
        }
    }
}
