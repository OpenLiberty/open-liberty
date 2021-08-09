/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                DSDTest.class,
                EnvEntryTest.class,
                JPATest.class,
                RepeatableDSDTest.class,
                RepeatableEnvEntryTest.class,
                RepeatableTranTest.class,
                ResRefTest.class,
                ServiceLookupTest.class,
                TranTest.class
})
public class FATSuite {}
