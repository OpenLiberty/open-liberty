/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                BasicTest.class,
                BasicCdiTest.class,
                BasicCdiInEE8Test.class,
                HeaderPropagationTest.class,
                MultiClientCdiTest.class,
                AsyncMethodTest.class,
                ProduceConsumeTest.class,
                PropsTest.class,
                CdiPropsAndProvidersTest.class
})
public class FATSuite {}
