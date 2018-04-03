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

@RunWith(Suite.class)
@SuiteClasses({
                CommonWebServerTests15.class,
                CommonWebServerTests15Servlet40.class,
                CommonWebServerTests20.class,
                CommonWebServerTests20Servlet40.class,
                ConfigDropinRootTests.class,
                ConfigSpringBootAppTagTests.class,
                PreThinnedSpringBootTests.class,
                WarmStartTests.class,
                SSLTests15.class,
                SSLTests20.class,
                SSLMutualAuthTests15.class,
                SSLMutualAuthTests20.class,
                JSPTests15.class
})

public class FATSuite {

}
