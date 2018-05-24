/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.tx.statlesmixsec.web;

import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.EJBTransactionRequiredException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.tx.statlesmixsec.ejb.AsmDescTxAttrLocal;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * This test class was created to verify the fix for APAR PK93643. Please refer
 * to the APAR for a more detailed description of the problem. Here is a brief
 * synopsis of what the APAR was opened for:
 *
 * In WebSphere ApplicationServer version 7.0.0.3 test server within Rational
 * Application Developer version 7.5.3 environment, Security Roles that are
 * defined within the Assembly Descriptor section of the ejb-jar.xml Deployment
 * Descriptor file where the corresponding EJB are defined via Java Annotations
 * are not being processed correctly. Transaction attributes defined in the same
 * way are being ignored.
 *
 */
@SuppressWarnings("serial")
@WebServlet("/AsmDescTranAttrServlet")
public class AsmDescTranAttrServlet extends FATServlet {
    private static final String CLASS_NAME = AsmDescTranAttrServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB
    AsmDescTxAttrLocal bean;

    /**
     * Test to verify that if a bean is only defined via annotations (i.e. no
     * enterprise-beans defined in XML) and a trans-attribute is defined in the
     * assembly-descriptor section of the ejb-jar.xml for a specific method, the
     * trans-attribute is picked up AND overrides the TransactionAtrribute
     * defined via annotations on the method.
     *
     * The method has a TransactionAtrribute of REQUIRES_NEW defined via
     * annotations, but has a trans-attribute of Mandatory defined in XML. The
     * method should use the Mandatory transaction attribute. Since the method
     * is being called w/o an active user transaction the method call should
     * result in a javax.ejb.EJBTransactionRequiredException exception being
     * thrown.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRequiredException" })
    public void testAsmDescTxAttrOverride() throws Exception {
        try {
            String result = bean.xmlOverridesAnnTxAttr();
            fail("--> " + result);
        } catch (EJBTransactionRequiredException ex) {
            svLogger.info("--> Test caught the expected exception: " + ex);
        }
    }

    /**
     * Test to verify that if a bean is only defined via annotations (i.e. no
     * enterprise-beans defined in XML) and a trans-attribute is defined in the
     * assembly-descriptor section of the ejb-jar.xml for a specific method, the
     * trans-attribute is picked up.
     *
     * The method has a trans-attribute of Mandatory defined in XML. The method
     * should use the Mandatory transaction attribute. Since the method is being
     * called w/o an active user transaction the method call should result in a
     * javax.ejb.EJBTransactionRequiredException exception being thrown.
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRequiredException" })
    public void testAsmDescTxAttr() throws Exception {
        try {
            String result = bean.xmlOnlyTxAttr();
            fail("--> " + result);
        } catch (EJBTransactionRequiredException ex) {
            svLogger.info("--> Test caught the expected exception: " + ex);
        }
    }
}