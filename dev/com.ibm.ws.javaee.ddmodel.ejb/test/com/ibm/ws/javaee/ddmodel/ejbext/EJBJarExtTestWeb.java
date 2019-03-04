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
package com.ibm.ws.javaee.ddmodel.ejbext;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

/**
 * Repeat a few tests for WEB-INF/ibm-ejb-jar-ext.xml
 */
public class EJBJarExtTestWeb extends EJBJarExtTestBaseWeb {

    @Test
    public void testGetVersion() throws Exception {
        Assert.assertEquals("XMI", parseEJBJarExtension(ejbJarExtension("") + "</ejbext:EJBJarExtension>",
                                                        parseEJBJar(ejbJar21() + "</ejb-jar>")).getVersion());
        Assert.assertEquals("Version should be 1.0", "1.0", parse(ejbJarExt10() + "</ejb-jar-ext>").getVersion());
        Assert.assertEquals("Version should be 1.1", "1.1", parse(ejbJarExt11() + "</ejb-jar-ext>").getVersion());
    }

    @Test
    public void testGetEnterpriseBeans() throws Exception {
        Assert.assertEquals("List size should be zero", 0, parse(ejbJarExt11() + "</ejb-jar-ext>").getEnterpriseBeans().size());
    }

    @Test
    public void testGetEnterpriseBeansXMI() throws Exception {
        Assert.assertEquals(Collections.emptyList(),
                            parseEJBJarExtension(ejbJarExtension("") + "</ejbext:EJBJarExtension>",
                                                 parseEJBJar(ejbJar21() + "</ejb-jar>")).getEnterpriseBeans());
    }
}
