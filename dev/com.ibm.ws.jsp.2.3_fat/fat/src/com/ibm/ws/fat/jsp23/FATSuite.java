/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.jsp23;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.jsp23.tests.JSP23JSP22ServerTest;
import com.ibm.ws.fat.jsp23.tests.JSPDummyTest;
import com.ibm.ws.fat.jsp23.tests.JSPJava8Test;
import com.ibm.ws.fat.jsp23.tests.JSPServerHttpUnit;
import com.ibm.ws.fat.jsp23.tests.JSPServerTest;
import com.ibm.ws.fat.util.FatLogHandler;

/**
 * JSP 2.3 Tests
 *
 * The tests for both features should be included in this test component.
 */
@RunWith(Suite.class)
@SuiteClasses({
                JSPDummyTest.class,
                JSPServerHttpUnit.class,
                JSPServerTest.class,
                JSPJava8Test.class,
                JSP23JSP22ServerTest.class
})
public class FATSuite {

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }
}
