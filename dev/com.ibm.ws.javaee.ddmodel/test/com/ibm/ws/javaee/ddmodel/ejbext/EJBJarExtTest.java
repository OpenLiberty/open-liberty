/*******************************************************************************
 * Copyright (c) 2012,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ejbext;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class EJBJarExtTest extends EJBJarExtTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }
    
    public EJBJarExtTest(boolean ejbInWar) {
        super(ejbInWar);
    }

    @Test
    public void testGetVersion() throws Exception {
        Assert.assertEquals("XMI",
                parseEJBJarExtXMI(ejbJarExtXMI(), getEJBJar21())
                    .getVersion());

        Assert.assertEquals("Version should be 1.0",
                "1.0", parseEJBJarExtXML(ejbJarExt10()).getVersion());
        Assert.assertEquals("Version should be 1.1",
                "1.1", parseEJBJarExtXML(ejbJarExt11()).getVersion());
    }

    @Test
    public void testGetEnterpriseBeans() throws Exception {
        Assert.assertEquals("Should have one EJB",
                1,
                parseEJBJarExtXML(ejbJarExt11()).getEnterpriseBeans().size());
    }

    @Test
    public void testGetEnterpriseBeansXMI() throws Exception {
        Assert.assertEquals("Should have one EJB",
                1,
                parseEJBJarExtXMI(ejbJarExtXMI(), getEJBJar21()).getEnterpriseBeans().size());
    }
}
