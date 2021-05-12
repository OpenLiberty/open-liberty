/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
                AppAndResourceTest.class,
                AppAndResourceCDIBeanDiscoveryModeDisabledTest.class,
                CDIInjectIntoAppTest.class,
                ExceptionTest.class,
                InjectAppTest.class,
                JsonbTest.class,
                ManagedBeansTest.class,
                ValidatorTest.class,
                WebXmlAppTest.class,
                WebXmlNoAppTest.class,
                XmlWithJaxbTest.class,
                XmlWithoutJaxbTest.class
})
public class FATSuite {

}
