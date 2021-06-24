/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd;
import com.ibm.ws.javaee.dd.ejbbnd.EnterpriseBean;
import com.ibm.ws.javaee.dd.ejbbnd.Interface;
import com.ibm.ws.javaee.dd.ejbbnd.Session;

@RunWith(Parameterized.class)
public class EJBJarBndInterfaceTest extends EJBJarBndTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }

    public EJBJarBndInterfaceTest(boolean ejbInWar) {
        super(ejbInWar);
    }

    protected static final String interfaceXML1 =
            "<session name=\"SessionBean1\">\n" +
                "<interface class=\"com.ibm.Class1\" binding-name=\"interfaceBindingName1\"/>\n" +
            "</session>";

    @Test
    public void testInterfaceAttributeBindingName() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd11(interfaceXML1));
        Assert.assertEquals("interfaceBindingName1", ((Session) ejbJarBnd.getEnterpriseBeans().get(0)).getInterfaces().get(0).getBindingName());
    }

    @Test
    public void testInterfaceAttributeClass() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd11(interfaceXML1));
        Assert.assertEquals("com.ibm.Class1", ((Session) ejbJarBnd.getEnterpriseBeans().get(0)).getInterfaces().get(0).getClassName());
    }

    protected static final String interfaceXML2 =
            "<session name=\"SessionBean2\">\n" +
                "<interface binding-name=\"interfaceBinding2\"\n" +
                "class=\"com.ibm.test.SessionBean2\"/>\n" +
            "</session>";

    @Test
    public void testInterfaceAttributesSeparateLine() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd10(interfaceXML2));
        Assert.assertEquals("interfaceBinding2", ((Session) ejbJarBnd.getEnterpriseBeans().get(0)).getInterfaces().get(0).getBindingName());
        Assert.assertEquals("com.ibm.test.SessionBean2", ((Session) ejbJarBnd.getEnterpriseBeans().get(0)).getInterfaces().get(0).getClassName());
    }

    protected static final String interfaceXML3 =
            "<session name=\"SessionBean3\">" +
                "<interface class=\"com.ibm.test.SessionBean3a\" binding-name=\"interfaceBindingName3a\"/>" +
                "<interface class=\"com.ibm.test.SessionBean3b\" binding-name=\"interfaceBindingName3b\"/>" +
                "<interface class=\"com.ibm.test.SessionBean3c\" binding-name=\"interfaceBindingName3c\"/>" +
            "</session>";

    @Test
    public void testInterfaceMultiple() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd11(interfaceXML3));

        List<EnterpriseBean> sessionBeans = ejbJarBnd.getEnterpriseBeans();
        Session bean0 = (Session) sessionBeans.get(0);
        List<Interface> interfaces = bean0.getInterfaces();
        Assert.assertEquals(3, interfaces.size());

        Assert.assertEquals("com.ibm.test.SessionBean3a", interfaces.get(0).getClassName());
        Assert.assertEquals("interfaceBindingName3a", interfaces.get(0).getBindingName());
        Assert.assertEquals("com.ibm.test.SessionBean3b", interfaces.get(1).getClassName());
        Assert.assertEquals("interfaceBindingName3b", interfaces.get(1).getBindingName());
        Assert.assertEquals("com.ibm.test.SessionBean3c", interfaces.get(2).getClassName());
        Assert.assertEquals("interfaceBindingName3c", interfaces.get(2).getBindingName());

        Assert.assertEquals(3, interfaces.size());
    }
}
