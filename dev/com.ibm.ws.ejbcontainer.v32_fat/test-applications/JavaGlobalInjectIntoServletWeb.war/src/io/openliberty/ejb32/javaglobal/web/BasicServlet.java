/*******************************************************************************
 * Copyright (c) 2016, 2024 IBM Corporation and others.
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
package io.openliberty.ejb32.javaglobal.web;

import static junit.framework.Assert.assertEquals;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.ejb32.javaglobal.ejb.InjectIntoServletLocal;

@WebServlet("/BasicServlet")
@SuppressWarnings("serial")
public class BasicServlet extends FATServlet {
    @Resource(lookup = "java:global/env/AppXmlGlobalString")
    String appXmlGlobalString;

    @Resource(name = "java:app/env/AppXmlAppString")
    String appXmlAppString;

    @Resource(name = "JavaCompEnvFullBinding")
    String javaCompEnvFullBindingString;

    @Resource(name = "java:comp/env/JavaCompEnvBinding")
    String javaCompEnvBindingString;

    @EJB()
    private InjectIntoServletLocal ivBean;

    /**
     * This test ensures that a java:global env-entry defined in application.xml
     * may be injected into a servlet using lookup attribute of @Resource.
     */
    @Test
    public void testJavaGlobalfromAppXml() {
        assertEquals("Incorrect value for java:global/env/AppXmlGlobalString", "AppXmlString: Hello!", appXmlGlobalString);
    }

    /**
     * This test ensures that a java:app env-entry with value defined in
     * application.xml may be injected into a servlet using an @Resource
     * with a matching name.
     */
    @Test
    public void testJavaAppfromAppXml() {
        assertEquals("Incorrect value for java:app/env/AppXmlAppString", "AppXmlString: Howdy!", appXmlAppString);
    }

    /**
     * This test ensures that a java:global env-entry defined in application.xml
     * may be injected into a servlet using a binding for a java:comp @Resource,
     * where the binding contains the full java:comp/env prefix and the @Resource
     * does not.
     */
    @Test
    public void testJavaCompEnvFullBinding() {
        assertEquals("Incorrect value for JavaCompEnvFullBinding", "AppXmlString: Hello!", javaCompEnvFullBindingString);
    }

    /**
     * This test ensures that a java:app env-entry defined in application.xml
     * may be injected into a servlet using a binding for a java:comp @Resource,
     * where the binding does not contain the full java:comp/env prefix and the
     *
     * @Resource does.
     */
    @Test
    public void testJavaCompEnvBinding() {
        assertEquals("Incorrect value for java:app/env/AppXmlAppString", "AppXmlString: Howdy!", javaCompEnvBindingString);
    }

    /**
     * This test ensures that a java:global env-entry defined in application.xml
     * may be injected into a bean using lookup attribute of @Resource and the
     * bean is injected into a servlet.
     */
    @Test
    public void testJavaGlobalfromAppXmlinBean() {
        ivBean.testJavaGlobalfromAppXml();
    }

    /**
     * This test ensures that a java:app env-entry with value defined in
     * application.xml may be injected into a bean using an @Resource
     * with a matching name and the bean is injected into a servlet.
     */
    @Test
    public void testJavaAppfromAppXmlinBean() {
        ivBean.testJavaAppfromAppXml();
    }
}
