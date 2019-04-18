/*******************************************************************************
 * Copyright (c) 2017-2019 IBM Corporation and others.
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
                AsyncMethodTest.class,
                BasicTest.class,
                BasicCdiTest.class,
                BasicCdiInEE8Test.class,
                BasicEJBTest.class,
                CdiPropsAndProvidersTest.class,
                CollectionsTest.class,
                HandleResponsesTest.class,
                HeaderPropagationTest.class,
                HeaderPropagation12Test.class,
                MultiClientCdiTest.class,
                ProduceConsumeTest.class,
                PropsTest.class
})
public class FATSuite {}
