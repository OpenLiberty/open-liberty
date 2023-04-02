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
package com.ibm.ws.cloudant.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.containers.SimpleLogConsumer;
import componenttest.containers.TestContainerSuite;

@RunWith(Suite.class)
@SuiteClasses({
                CloudantDemoTest.class,
                CloudantTest.class,
                CloudantTestOutboundSSL.class,
                CloudantModifyConfigTest.class
})
public class FATSuite extends TestContainerSuite {

    @ClassRule
    public static CouchDBContainer cloudant = new CouchDBContainer("kyleaure/couchdb-ssl:1.0")
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "cloudant"));

}