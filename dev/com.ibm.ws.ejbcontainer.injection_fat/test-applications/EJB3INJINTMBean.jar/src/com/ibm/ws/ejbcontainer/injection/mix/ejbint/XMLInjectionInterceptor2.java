/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.interceptor.InvocationContext;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

/**
 * This interceptor class is used when testing injection into a field
 * of an interceptor class when <injection-target> is used inside of a
 * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
 * and <message-destination-ref> stanza that is within a <interceptor>
 * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
 * Also, the bindings for each of these reference type is inside of a
 * <session> or <message-driven> stanza in the ibm-ejb-jar-bnd.xml binding file.
 */
public class XMLInjectionInterceptor2 {
    private static final String CLASS_NAME = XMLInjectionInterceptor2.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String PASSED = "XII2_PASSED";

    private static final String CF_NAME = "java:comp/env/XMLInjectionInterceptor2/jms/WSTestQCF";
    private static final String REQUEST_QUEUE = "java:comp/env/XMLInjectionInterceptor2/jms/RequestQueue";

    //  @EJB(name="XMLInjectionInterceptor2/ejbLocalRef",beanName="MixedSFInterceptorBean")
    MixedSFLocal ejbLocalRef; // EJB Local ref

    //  @EJB(name="XMLInjectionInterceptor2/ejbRemoteRef",beanName="MixedSFInterceptorBean")
    MixedSFRemote ejbRemoteRef; // EJB Remote ref

    //   @Resource (name="XMLInjectionInterceptor2/jms/WSTestQCF", authenticationType=APPLICATION, shareable=true, description="Queue conn factory")
    public QueueConnectionFactory qcf;

    // @Resource(name="XMLInjectionInterceptor2/jms/RequestQueue")
    public Queue reqQueue;

    // @Resource(name="XMLInjectionInterceptor2/jms/ResponseQueue")
    public Queue resQueue;

    //  @Resource(name="XMLInjectionInterceptor2/StringVal")
    public String envEntry;

    @SuppressWarnings("unused")
    //@AroundInvoke
    private Object aroundInvoke(InvocationContext inv) throws Exception {
        Method m = inv.getMethod();
        String methodName = m.getName();
        svLogger.info(CLASS_NAME + ".aroundInvoke: " + methodName);
        if (methodName.equals("getXMLInterceptor2Results") || methodName.equals("onGetTimestamp")) {
            FATMDBHelper.emptyQueue(CF_NAME, REQUEST_QUEUE);

            svLogger.info("XMLInjectionInterceptor2 qcf = " + qcf);
            svLogger.info("XMLInjectionInterceptor2 reqQueue = " + reqQueue);
            svLogger.info("XMLInjectionInterceptor2 resQueue = " + resQueue);

            assertNotNull("Checking for non-null ejbLocalRef", ejbLocalRef);
            ejbLocalRef.setString("XMLInjectionInterceptor2");
            assertEquals("Checking ejb local ref set/getString",
                         "XMLInjectionInterceptor2", ejbLocalRef.getString());

            assertNotNull("Checking for non-null ejbRemoteRef", ejbRemoteRef);
            ejbRemoteRef.setString("XMLInjectionInterceptor2");
            assertEquals("Checking ejb remote ref set/getString",
                         "XMLInjectionInterceptor2", ejbRemoteRef.getString());

            assertNotNull("Checking for non-null env entry ref", envEntry);
            assertEquals("Checking value of env entry",
                         "Hello XMLInjectionInterceptor2!", envEntry);

            assertNotNull("Checking for non-null Resource ref", qcf);
            assertNotNull("Checking for non-null message destination ref(reqQueue)", reqQueue);
            assertNotNull("Checking for non-null resource env ref(resQueue)", resQueue);

            svLogger.info("XMLInjectionInterceptor2 pre-send results: " + PASSED);
            if (methodName.equals("onGetTimestamp")) {
                MessageDrivenInjectionBean mdb = (MessageDrivenInjectionBean) inv.getTarget();
                mdb.setResults(PASSED);
            } else {
                if (qcf != null && reqQueue != null) {
                    FATMDBHelper.putQueueMessage(PASSED, qcf, reqQueue);
                } else {
                    svLogger.info("Could not use qcf or queue, cannot post results!!");
                }
            }
        }

        Object rv = inv.proceed();
        return rv;
    }

    @SuppressWarnings("unused")
    //@PreDestroy
    private void preDestroy(InvocationContext inv) {
        svLogger.info(CLASS_NAME + ".preDestroy");
        if (ejbLocalRef != null) {
            ejbLocalRef.destroy();
            ejbLocalRef = null;
        }
        if (ejbRemoteRef != null) {
            ejbRemoteRef.destroy();
            ejbRemoteRef = null;
        }
        try {
            inv.proceed();
        } catch (Exception e) {
            throw new EJBException("unexpected Exception", e);
        }
    }
}