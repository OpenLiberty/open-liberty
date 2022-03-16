/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.cdi40.internal.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.annotation.MinimumJavaLevel;
import io.openliberty.cdi40.internal.fat.bce.BuildCompatibleExtensionsErrorTest;
import io.openliberty.cdi40.internal.fat.bce.BuildCompatibleExtensionsTest;
import io.openliberty.cdi40.internal.fat.config.BeansXMLTest;
import io.openliberty.cdi40.internal.fat.config.LegacyConfigTest;

@MinimumJavaLevel(javaLevel = 11)
@RunWith(Suite.class)
@SuiteClasses({
                BuildCompatibleExtensionsTest.class,
                BuildCompatibleExtensionsErrorTest.class,
                LegacyConfigTest.class,
                BeansXMLTest.class
})
public class FATSuite {}
