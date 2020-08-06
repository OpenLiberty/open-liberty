/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.grpc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                HelloWorldCDITests.class,
                HelloWorldTest.class,
                ServiceSupportTests.class,
                ServiceConfigTests.class,
                ServiceInterceptorTests.class,
                StoreServicesTests.class,
                // leave out for now, to avoid intermittent build breaks StreamingTests.class,
                ClientInterceptorTests.class,

})

public class FATSuite {

    private static final Class<?> c = FATSuite.class;

}
