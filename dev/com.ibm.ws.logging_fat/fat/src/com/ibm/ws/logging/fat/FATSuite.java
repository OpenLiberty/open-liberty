/*************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
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
                StackJoinerTest.class,
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
                CustomAccessLogFieldsTest.class,
                TraceInjectionTest.class,
                TestSuppressSensitiveTraceBootstrap.class
})

public class FATSuite {

}
