/*******************************************************************************
 * Copyright (c) 2014, 2024 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.v32.fat.statefullifecycletx;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.wsspi.uow.UOWManager;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@WebServlet("/*")
@SuppressWarnings("serial")
public class StatefulLifecycleTxTestServlet extends FATServlet {
    private static final Logger logger = Logger.getLogger(StatefulLifecycleTxTestServlet.class.getName());

    @Resource
    UserTransaction userTran;

    @Resource(lookup = "java:comp/websphere/UOWManager")
    UOWManager uowManager;

    private void testInGlobalTx(Class<?> klass) throws Exception {
        userTran.begin();
        AbstractStatefulLifecycleTxBean.test(klass.getSimpleName(), uowManager, true);
        userTran.commit();
    }

    private void testInLTC(String beanName) throws Exception {
        AbstractStatefulLifecycleTxBean.test(beanName, uowManager, false);
    }

    private void testInLTC(Class<?> klass) throws Exception {
        testInLTC(klass.getSimpleName());
    }

    @Test
    public void testDefaultInLTC() throws Exception {
        testInLTC(StatefulLifecycleTxDefaultBean.class);
    }

    @Test
    public void testDefault2xInLTC() throws Exception {
        testInLTC(StatefulLifecycleTxDefault2xBean.class);
    }

    @Test
    public void testDefaultInGlobalTx() throws Exception {
        testInGlobalTx(StatefulLifecycleTxDefaultBean.class);
    }

    @Test
    public void testDefault2xInGlobalTx() throws Exception {
        testInGlobalTx(StatefulLifecycleTxDefault2xBean.class);
    }

    @Test
    public void testNotSupportedClassInLTC() throws Exception {
        testInLTC(StatefulLifecycleTxNotSupportedClassBean.class);
    }

    @Test
    public void testNotSupportedClass2xInLTC() throws Exception {
        testInLTC(StatefulLifecycleTxNotSupportedClass2xBean.class);
    }

    @Test
    public void testNotSupportedClassInGlobalTx() throws Exception {
        testInGlobalTx(StatefulLifecycleTxNotSupportedClassBean.class);
    }

    @Test
    public void testNotSupportedClass2xInGlobalTx() throws Exception {
        testInGlobalTx(StatefulLifecycleTxNotSupportedClass2xBean.class);
    }

    @Test
    public void testNotSupportedMethodInLTC() throws Exception {
        testInLTC(StatefulLifecycleTxNotSupportedMethodBean.class);
    }

    @Test
    public void testNotSupportedMethod2xInLTC() throws Exception {
        testInLTC(StatefulLifecycleTxNotSupportedMethod2xBean.class);
    }

    @Test
    public void testNotSupportedMethodInGlobalTx() throws Exception {
        testInGlobalTx(StatefulLifecycleTxNotSupportedMethodBean.class);
    }

    @Test
    public void testNotSupportedMethod2xInGlobalTx() throws Exception {
        testInGlobalTx(StatefulLifecycleTxNotSupportedMethod2xBean.class);
    }

    @Test
    public void testNotSupportedXMLStyle1() throws Exception {
        testInLTC("StatefulLifecycleTxNotSupportedXMLStyle1Bean");
    }

    @Test
    public void testNotSupportedXMLStyle1MI() throws Exception {
        testInLTC("StatefulLifecycleTxNotSupportedXMLStyle1MIBean");
    }

    @Test
    public void testNotSupportedXMLStyle2() throws Exception {
        testInLTC("StatefulLifecycleTxNotSupportedXMLStyle2Bean");
    }

    @Test
    public void testNotSupportedXMLStyle2Ambig() throws Exception {
        testInLTC("StatefulLifecycleTxNotSupportedXMLStyle2AmbigBean");
    }

    @Test
    public void testNotSupportedXMLStyle2MI() throws Exception {
        testInLTC("StatefulLifecycleTxNotSupportedXMLStyle2MIBean");
    }

    @Test
    public void testNotSupportedXMLStyle3() throws Exception {
        testInLTC("StatefulLifecycleTxNotSupportedXMLStyle3Bean");
    }

    @Test
    public void testNotSupportedXMLStyle3Ambig() throws Exception {
        testInLTC("StatefulLifecycleTxNotSupportedXMLStyle3AmbigBean");
    }

    @Test
    public void testNotSupportedXMLStyle3MI() throws Exception {
        testInLTC("StatefulLifecycleTxNotSupportedXMLStyle3MIBean");
    }

    @Test
    public void testRequiresNewClassInLTC() throws Exception {
        testInLTC(StatefulLifecycleTxRequiresNewClassBean.class);
    }

    @Test
    public void testRequiresNewClass2xInLTC() throws Exception {
        testInLTC(StatefulLifecycleTxRequiresNewClass2xBean.class);
    }

    @Test
    public void testRequiresNewClassInGlobalTx() throws Exception {
        testInGlobalTx(StatefulLifecycleTxRequiresNewClassBean.class);
    }

    @Test
    public void testRequiresNewClass2xInGlobalTx() throws Exception {
        testInGlobalTx(StatefulLifecycleTxRequiresNewClass2xBean.class);
    }

    @Test
    public void testRequiresNewMethodInLTC() throws Exception {
        testInLTC(StatefulLifecycleTxRequiresNewMethodBean.class);
    }

    @Test
    public void testRequiresNewMethod2xInLTC() throws Exception {
        testInLTC(StatefulLifecycleTxRequiresNewMethod2xBean.class);
    }

    @Test
    public void testRequiresNewMethodInGlobalTx() throws Exception {
        testInGlobalTx(StatefulLifecycleTxRequiresNewMethodBean.class);
    }

    @Test
    public void testRequiresNewMethod2xInGlobalTx() throws Exception {
        testInGlobalTx(StatefulLifecycleTxRequiresNewMethod2xBean.class);
    }

    @Test
    public void testRequiresNewXMLStyle1() throws Exception {
        testInLTC("StatefulLifecycleTxRequiresNewXMLStyle1Bean");
    }

    @Test
    public void testRequiresNewXMLStyle1MI() throws Exception {
        testInLTC("StatefulLifecycleTxRequiresNewXMLStyle1MIBean");
    }

    @Test
    public void testRequiresNewXMLStyle2() throws Exception {
        testInLTC("StatefulLifecycleTxRequiresNewXMLStyle2Bean");
    }

    @Test
    public void testRequiresNewXMLStyle2Ambig() throws Exception {
        testInLTC("StatefulLifecycleTxRequiresNewXMLStyle2AmbigBean");
    }

    @Test
    public void testRequiresNewXMLStyle2MI() throws Exception {
        testInLTC("StatefulLifecycleTxRequiresNewXMLStyle2MIBean");
    }

    @Test
    public void testRequiresNewXMLStyle3() throws Exception {
        testInLTC("StatefulLifecycleTxRequiresNewXMLStyle3Bean");
    }

    @Test
    public void testRequiresNewXMLStyle3Ambig() throws Exception {
        testInLTC("StatefulLifecycleTxRequiresNewXMLStyle3AmbigBean");
    }

    @Test
    public void testRequiresNewXMLStyle3MI() throws Exception {
        testInLTC("StatefulLifecycleTxRequiresNewXMLStyle3MIBean");
    }

    private void testError(String beanName) throws Exception {
        try {
            new InitialContext().lookup("java:module/" + beanName);
            throw new IllegalStateException("expected lookup failure");
        } catch (NameNotFoundException e) {
            throw e;
        } catch (NamingException e) {
            logger.info("caught expected " + e);
        }
    }

    private void testError(Class<?> klass) throws Exception {
        testError(klass.getSimpleName());
    }

    @Test
    public void testMandatoryClassIgnored() throws Exception {
        testInLTC(StatefulLifecycleTxMandatoryClassIgnoredBean.class);
    }

    @Test
    @AllowedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testMandatoryMethodError() throws Exception {
        testError(StatefulLifecycleTxMandatoryMethodErrorBean.class);
    }

    @Test
    @AllowedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testMandatoryXMLError() throws Exception {
        testError("StatefulLifecycleTxMandatoryXMLErrorBean");
    }

    @Test
    public void testNeverClassIgnored() throws Exception {
        testInLTC(StatefulLifecycleTxNeverClassIgnoredBean.class);
    }

    @Test
    @AllowedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testNeverMethodError() throws Exception {
        testError(StatefulLifecycleTxNeverMethodErrorBean.class);
    }

    @Test
    @AllowedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testNeverXMLError() throws Exception {
        testError("StatefulLifecycleTxNeverXMLErrorBean");
    }

    @Test
    public void testRequiredClassIgnored() throws Exception {
        testInLTC(StatefulLifecycleTxRequiredClassIgnoredBean.class);
    }

    @Test
    public void testRequiredClass2xIgnored() throws Exception {
        testInLTC(StatefulLifecycleTxRequiredClass2xIgnoredBean.class);
    }

    @Test
    @AllowedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testRequiredMethodError() throws Exception {
        testError(StatefulLifecycleTxRequiredMethodErrorBean.class);
        // For one test variation, we ensure that an unexpected error does not
        // occur when attempting to re-access an EJB with an error.
        testError(StatefulLifecycleTxRequiredMethodErrorBean.class);
    }

    @Test
    @AllowedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testRequiredMethod2xError() throws Exception {
        testError(StatefulLifecycleTxRequiredMethod2xErrorBean.class);
    }

    @Test
    @AllowedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testRequiredXMLError() throws Exception {
        testError("StatefulLifecycleTxRequiredXMLErrorBean");
    }

    @Test
    public void testSupportsClassIgnored() throws Exception {
        testInLTC(StatefulLifecycleTxSupportsClassIgnoredBean.class);
    }

    @Test
    @AllowedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testSupportsMethodError() throws Exception {
        testError(StatefulLifecycleTxSupportsMethodErrorBean.class);
    }

    @Test
    @AllowedFFDC("com.ibm.ejs.container.EJBConfigurationException")
    public void testSupportsXMLError() throws Exception {
        testError("StatefulLifecycleTxSupportsXMLErrorBean");
    }
}
