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
import com.ibm.ws.javaee.dd.ejbbnd.MessageDriven;

@RunWith(Parameterized.class)
public class EJBJarBndListenerPortTest extends EJBJarBndTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }
    
    public EJBJarBndListenerPortTest(boolean ejbInWar) {
        super(ejbInWar);
    }

    protected static final String listenerPortXML =
            "<message-driven name=\"MessageDrivenBean1\">\n" +
                "<listener-port name=\"lpName1\"/>\n" +
            "</message-driven>";

    @Test
    public void testListenerPortAttributeName() throws Exception {
        EJBJarBnd ejbJarBnd = parseEJBJarBndXML(ejbJarBnd11(listenerPortXML));

        List<EnterpriseBean> mdBeans = ejbJarBnd.getEnterpriseBeans();
        Assert.assertEquals("Only expected 1 message driven bean", 1, mdBeans.size());
        MessageDriven bean0 = (MessageDriven) mdBeans.get(0);
        Assert.assertEquals(bean0.getName(), "MessageDrivenBean1", bean0.getName());
        Assert.assertNotNull("Listener port should not be null", bean0.getListenerPort());
        Assert.assertEquals("Incorrect listener port name: " + bean0.getListenerPort().getName(), "lpName1", bean0.getListenerPort().getName());
    }
}
