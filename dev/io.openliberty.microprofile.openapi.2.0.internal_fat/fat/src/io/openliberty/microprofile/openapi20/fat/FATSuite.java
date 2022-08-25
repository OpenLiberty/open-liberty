/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.openliberty.microprofile.openapi20.fat.cache.CacheTest;
import io.openliberty.microprofile.openapi20.fat.deployments.DeploymentTest;
import io.openliberty.microprofile.openapi20.fat.deployments.MergeConfigTest;
import io.openliberty.microprofile.openapi20.fat.deployments.MergeTest;
import io.openliberty.microprofile.openapi20.fat.deployments.MergeWithServletTest;
import io.openliberty.microprofile.openapi20.fat.shutdown.ShutdownTest;

@RunWith(Suite.class)
@SuiteClasses({
    ApplicationProcessorTest.class,
    CacheTest.class,
    DeploymentTest.class,
    MergeConfigTest.class,
    MergeTest.class,
    MergeWithServletTest.class,
    ShutdownTest.class
})
public class FATSuite {}