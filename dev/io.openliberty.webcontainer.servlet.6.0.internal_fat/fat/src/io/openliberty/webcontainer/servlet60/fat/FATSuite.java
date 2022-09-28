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
package io.openliberty.webcontainer.servlet60.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60CookieSetAttributeTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60GetRealPathTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60RequestConnectionTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60SessionCookieConfigSCITest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60SessionCookieConfigXMLTest;
import io.openliberty.webcontainer.servlet60.fat.tests.Servlet60XPoweredByHeaderTest;

@RunWith(Suite.class)
@SuiteClasses({
                Servlet60XPoweredByHeaderTest.class,
                Servlet60GetRealPathTest.class,
                Servlet60CookieSetAttributeTest.class,
                Servlet60RequestConnectionTest.class,
                Servlet60SessionCookieConfigXMLTest.class,
                Servlet60SessionCookieConfigSCITest.class
})
public class FATSuite {

}
