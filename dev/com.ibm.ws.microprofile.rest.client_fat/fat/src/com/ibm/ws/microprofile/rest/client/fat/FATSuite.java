/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
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
package com.ibm.ws.microprofile.rest.client.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                AsyncMethodTest.class,
                BasicTest.class,
                BasicCdiTest.class,
                BasicEJBTest.class,
                CdiPropsAndProvidersTest.class,
                CollectionsTest.class,
                HandleResponsesTest.class,
                HeaderPropagationTest.class,
                HeaderPropagation12Test.class,
                HostnameVerifierTest.class,
                HostnameVerifierProxyTest.class,
                JsonbContextTest.class,
                MultiClientCdiTest.class,
                ProduceConsumeTest.class,
                PropsTest.class,
                SseTest.class
})
public class FATSuite {
}