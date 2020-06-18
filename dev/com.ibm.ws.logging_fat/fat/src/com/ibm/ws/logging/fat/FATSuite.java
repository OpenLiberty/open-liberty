/*************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 *
 */
@RunWith(Suite.class)

@SuiteClasses({
                StackTraceFilteringForLoggedExceptionParametersTest.class, StackTraceFilteringForLoggedExceptionWithACauseParametersTest.class,
                StackTraceFilteringForPrintedExceptionTest.class, StackTraceFilteringForPrintedExceptionWithIBMCodeAtTopTest.class,
                StackTraceFilteringForNoClassDefFoundErrorTest.class, StackTraceFilteringForBadlyWrittenThrowableTest.class,
                StackTraceFilteringForIBMFeatureExceptionTest.class, StackTraceFilteringForUserFeatureExceptionTest.class,
                StackTraceFilteringForSpecificationClassesExceptionTest.class,
                InvalidTraceSpecificationTest.class,
                HealthCenterTest.class,
                TestHideMessages.class,
                TestHideMsgDefinedBootstrap.class,
                IsoDateFormatTest.class,
                HandlerTest.class,
                HeaderFormatTest.class,
                LogServiceTest.class,
                RealFlushTest.class,
                JSONFieldsTest.class,
                ConsoleFormatTest.class,
                CustomAccessLogFieldsTest.class
})

public class FATSuite {

}
