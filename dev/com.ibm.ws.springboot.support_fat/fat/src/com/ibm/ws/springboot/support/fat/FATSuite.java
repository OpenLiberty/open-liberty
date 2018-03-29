/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.springboot.support.fat.utility.SpringBootUtilityThinTest;

@RunWith(Suite.class)
@SuiteClasses({
                CommonWebServerTests15.class,
                CommonWebServerTests15Servlet40.class,
                CommonWebServerTests20.class,
                CommonWebServerTests20Servlet40.class,
                ConfigDropinRootTests.class,
                ConfigSpringBootApplicationTagTests.class,
                PreThinnedSpringBootTests.class,
                WarmStartTests.class,
                SSLTests15.class,
                SSLTests20.class,
                SSLMutualAuthTests15.class,
                SSLMutualAuthTests20.class,
                JSPTests15.class,
                MissingServletTests15.class,
                MissingServletTests20.class,
                NeedSpringBoot15FeatureTests.class,
                NeedSpringBoot20FeatureTests.class,
                NoServletRequiredAppTests15.class,
                SpringBootUtilityThinTest.class
})

public class FATSuite {

}
