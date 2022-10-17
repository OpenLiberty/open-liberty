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
package io.openliberty.org.apache.myfaces40.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.openliberty.org.apache.myfaces40.fat.tests.ExtensionlessMappingTest;
import io.openliberty.org.apache.myfaces40.fat.tests.ExternalContextAddResponseCookieTest;
import io.openliberty.org.apache.myfaces40.fat.tests.FacesConfigTest;
import io.openliberty.org.apache.myfaces40.fat.tests.FacesContextGetLifecycleTest;
import io.openliberty.org.apache.myfaces40.fat.tests.InputTextTypeTest;
import io.openliberty.org.apache.myfaces40.fat.tests.SelectItemTests;
import io.openliberty.org.apache.myfaces40.fat.tests.SimpleTest;

@RunWith(Suite.class)
@SuiteClasses({
                SimpleTest.class,
                ExternalContextAddResponseCookieTest.class,
                FacesConfigTest.class,
                InputTextTypeTest.class,
                ExtensionlessMappingTest.class,
                SelectItemTests.class,
                FacesContextGetLifecycleTest.class
})
public class FATSuite {

}
