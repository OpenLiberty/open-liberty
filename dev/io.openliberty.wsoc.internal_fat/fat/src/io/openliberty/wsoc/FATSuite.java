/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2012, 2020 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.fat.util.FatLogHandler;

import io.openliberty.wsoc.tests.WebSocket11Test;
import io.openliberty.wsoc.tests.BasicTest;

/**
 * Collection of all example tests
 */
@RunWith(Suite.class)
/*
 * The classes specified in the @SuiteClasses annotation
 * below should represent all of the test cases for this FAT.
 */
@SuiteClasses({ BasicTest.class, 
                WebSocket11Test.class
})
public class FATSuite {
    private static final Class<?> c = FATSuite.class;

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
