/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.xml.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;
import com.ibm.ws.ejbcontainer.injection.xml.ejb.SFMsgDestinationLocalBiz;
import com.ibm.ws.ejbcontainer.injection.xml.ejb.SLMsgDestinationLocalBiz;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> MsgDestinationRefTest .
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests usage of message destination ref from stateful and stateless session beans
 * and injection of message destination refs. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSLMsgDestRef - Test message-destination-ref of SL Session Bean
 * <li>testSFMsgDestRef - Test message-destination-ref of SF Session Bean
 * <li>testSLMsgDestRefMthdInjection - Test message-destination-ref with
 * SLSB and method level injection
 * <li>testSFMsgDestRefFldInjection - Test message-destination-ref with SFSB
 * and field level injection
 * <li>testSLMsgDestRefAnnFldInjection - Test message-destination-ref with SLSB
 * and annotation based field level injection
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/MsgDestinationRefServlet")
public class MsgDestinationRefServlet extends FATServlet {
    private static final String CLASS_NAME = MsgDestinationRefServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Names of application and module... for lookup.
    private static final String Application = "EJB3INJSXTestApp";
    private static final String Module = "EJB3INJSXBean.jar";

    // Names of the beans used for the test... for lookup.
    private static final String SLMsgDestinationTestBeanName = "SLMsgDestinationTestBean";
    private static final String SFMsgDestinationTestBeanName = "SFMsgDestinationTestBean";
    private static final String SLMsgDestinationMthdInjTestBeanName = "SLMsgDestinationMthdInjTestBean";
    private static final String SFMsgDestinationFldInjTestBeanName = "SFMsgDestinationFldInjTestBean";
    private static final String SLMsgDestinationAnnotInjTestBeanName = "SLMsgDestinationAnnotInjTestBean";

    // Names of the interfaces used for the test
    private static final String SLMsgDestinationLocalBizInt = SLMsgDestinationLocalBiz.class.getName();
    private static final String SFMsgDestinationLocalBizInt = SFMsgDestinationLocalBiz.class.getName();

    private static final String CF_NAME = "Jetstream/jms/WSTestQCF";
    private static final String RESPONSE_QUEUE = "Jetstream/jms/ResponseQueue";

    /**
     * Test use of a message destination reference for a Stateless Session Bean
     * defined in xml. <p>
     */
    @Test
    public void testSLMsgDestRef() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SLMsgDestinationLocalBiz bean = (SLMsgDestinationLocalBiz) FATHelper.lookupDefaultBindingEJBLocalInterface(SLMsgDestinationLocalBizInt, Application, Module,
                                                                                                                   SLMsgDestinationTestBeanName);
        assertNotNull("1 ---> SLLSB was not accessed successfully.", bean);

        bean.putQueueMessage("MsgDestinationRefTest");
        String message = bean.getQueueMessage();
        svLogger.info("Queue had message: " + message);
        assertEquals("2 ---> Unexpected returned message: ",
                     "SLMsgDestinationTestBean:MsgDestinationRefTest", message);
        message = (String) FATMDBHelper.getQueueMessage(CF_NAME, RESPONSE_QUEUE);
        assertEquals("3 ---> Unexpected message on response queue: ",
                     "SLMsgDestinationTestBean:MsgDestinationRefTest", message);
    }

    /**
     * Test use of a message destination reference for a Stateful Session Bean
     * defined in xml. <p>
     */
    @Test
    public void testSFMsgDestRef() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SFMsgDestinationLocalBiz bean = (SFMsgDestinationLocalBiz) FATHelper.lookupDefaultBindingEJBLocalInterface(SFMsgDestinationLocalBizInt, Application, Module,
                                                                                                                   SFMsgDestinationTestBeanName);
        assertNotNull("1 ---> SLLSB was not accessed successfully.", bean);

        bean.setQueueMessage("MsgDestinationRefTest2");
        bean.putQueueMessage();
        String message = bean.getQueueMessage();
        svLogger.info("Queue had message: " + message);
        assertEquals("2 ---> Unexpected returned message: ",
                     "SFMsgDestinationTestBean:MsgDestinationRefTest2", message);
        message = (String) FATMDBHelper.getQueueMessage(CF_NAME, RESPONSE_QUEUE);
        assertEquals("3 ---> Unexpected message on response queue: ",
                     "SFMsgDestinationTestBean:MsgDestinationRefTest2", message);
    }

    /**
     * Test method level injection of a message destination reference and use
     * for a Stateless Session Bean defined in xml. <p>
     */
    @Test
    public void testSLMsgDestRefMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SLMsgDestinationLocalBiz bean = (SLMsgDestinationLocalBiz) FATHelper.lookupDefaultBindingEJBLocalInterface(SLMsgDestinationLocalBizInt, Application, Module,
                                                                                                                   SLMsgDestinationMthdInjTestBeanName);
        assertNotNull("1 ---> SLSB was not accessed successfully.", bean);

        bean.putQueueMessage("MsgDestinationRefTest3");
        String message = bean.getQueueMessage();
        svLogger.info("Queue had message: " + message);
        assertEquals("2 ---> Unexpected returned message: ",
                     "SLMsgDestinationMthdInjTestBean:MsgDestinationRefTest3", message);
        message = (String) FATMDBHelper.getQueueMessage(CF_NAME, RESPONSE_QUEUE);
        assertEquals("3 ---> Unexpected message on response queue: ",
                     "SLMsgDestinationMthdInjTestBean:MsgDestinationRefTest3", message);
    }

    /**
     * Test field level injection of a message destination reference and use
     * for a Stateful Session Bean defined in xml. <p>
     */
    @Test
    public void testSFMsgDestRefFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SFMsgDestinationLocalBiz bean = (SFMsgDestinationLocalBiz) FATHelper.lookupDefaultBindingEJBLocalInterface(SFMsgDestinationLocalBizInt, Application, Module,
                                                                                                                   SFMsgDestinationFldInjTestBeanName);
        assertNotNull("1 ---> SFSB was not accessed successfully.", bean);

        bean.setQueueMessage("MsgDestinationRefTest4");
        bean.putQueueMessage();
        String message = bean.getQueueMessage();
        svLogger.info("Queue had message: " + message);
        assertEquals("2 ---> Unexpected returned message: ",
                     "SFMsgDestinationFldInjTestBean:MsgDestinationRefTest4", message);
        message = (String) FATMDBHelper.getQueueMessage(CF_NAME, RESPONSE_QUEUE);
        assertEquals("3 ---> Unexpected message on response queue: ",
                     "SFMsgDestinationFldInjTestBean:MsgDestinationRefTest4", message);
    }

    /**
     * Test field level injection of a message destination reference and use
     * for a Stateless Session Bean defined via annotations. <p>
     */
    @Test
    public void testSLMsgDestRefAnnFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SLMsgDestinationLocalBiz bean = (SLMsgDestinationLocalBiz) FATHelper.lookupDefaultBindingEJBLocalInterface(SLMsgDestinationLocalBizInt, Application, Module,
                                                                                                                   SLMsgDestinationAnnotInjTestBeanName);
        assertNotNull("1 ---> SLSB was not accessed successfully.", bean);

        bean.putQueueMessage("MsgDestinationRefTest5");
        String message = bean.getQueueMessage();
        svLogger.info("Queue had message: " + message);
        assertEquals("2 ---> Unexpected returned message: ",
                     "SLMsgDestinationAnnotInjTestBean:MsgDestinationRefTest5", message);
        message = (String) FATMDBHelper.getQueueMessage(CF_NAME, RESPONSE_QUEUE);
        assertEquals("3 ---> Unexpected message on response queue: ",
                     "SLMsgDestinationAnnotInjTestBean:MsgDestinationRefTest5", message);
    }

}
