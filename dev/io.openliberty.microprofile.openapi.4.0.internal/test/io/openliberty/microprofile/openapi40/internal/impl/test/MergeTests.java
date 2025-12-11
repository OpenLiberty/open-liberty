/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.impl.test;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.openliberty.microprofile.openapi20.test.merge.MergeProcessorTest;
import io.openliberty.microprofile.openapi20.test.merge.parts.ComponentsMergeTest;
import io.openliberty.microprofile.openapi20.test.merge.parts.ExtensionsMergeTest;
import io.openliberty.microprofile.openapi20.test.merge.parts.OperationIDMergeTest;
import io.openliberty.microprofile.openapi20.test.merge.parts.PathsMergeTest;
import io.openliberty.microprofile.openapi20.test.merge.parts.SecurityMergeTest;
import io.openliberty.microprofile.openapi20.test.merge.parts.ServersMergeTest;
import io.openliberty.microprofile.openapi20.test.merge.parts.TagsMergeTest;
import io.openliberty.microprofile.openapi20.test.merge.parts.TestUtils;

@RunWith(Suite.class)
@SuiteClasses({
                MergeProcessorTest.class,
                ComponentsMergeTest.class,
                ExtensionsMergeTest.class,
                OperationIDMergeTest.class,
                PathsMergeTest.class,
                SecurityMergeTest.class,
                ServersMergeTest.class,
                TagsMergeTest.class
})
public class MergeTests {

    @BeforeClass
    public static void setup() {
        TestUtils.setCurrent(new TestUtil40Impl());
    }
}
