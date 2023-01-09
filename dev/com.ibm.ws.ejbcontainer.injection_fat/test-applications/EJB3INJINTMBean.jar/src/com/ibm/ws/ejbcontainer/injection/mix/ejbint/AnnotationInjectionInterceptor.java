/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import static javax.annotation.Resource.AuthenticationType.APPLICATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

/**
 * This interceptor class is used when testing injection into a field
 * of an interceptor class when the @EJB or @Resource annotation is used
 * to annotate a field in this interceptor class.
 * The bindings for each of these reference types is inside of a
 * <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file.
 */
public class AnnotationInjectionInterceptor {
    private static final String CLASS_NAME = AnnotationInjectionInterceptor.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String PASSED = "AII_PASSED";

    private static final String CF_NAME = "java:comp/env/AnnotationInjectionInterceptor/jms/WSTestQCF";
    private static final String REQUEST_QUEUE = "java:comp/env/AnnotationInjectionInterceptor/jms/RequestQueue";

    @EJB(name = "AnnotationInjectionInterceptor/ejbLocalRef", beanName = "MixedSFInterceptorBean")
    MixedSFLocal ejbLocalRef; // EJB Local ref

    @EJB(name = "AnnotationInjectionInterceptor/ejbRemoteRef", beanName = "MixedSFInterceptorBean")
    MixedSFRemote ejbRemoteRef; // EJB Remote ref

    @Resource(name = "AnnotationInjectionInterceptor/jms/WSTestQCF", authenticationType = APPLICATION, shareable = true, description = "Queue conn factory")
    public QueueConnectionFactory qcf;

    @Resource(name = "AnnotationInjectionInterceptor/jms/RequestQueue")
    public Queue reqQueue;

    @Resource(name = "AnnotationInjectionInterceptor/jms/ResponseQueue")
    public Queue resQueue;

    @Resource(name = "AnnotationInjectionInterceptor/StringVal")
    public String envEntry;

    @SuppressWarnings("unused")
    @AroundInvoke
    private Object aroundInvoke(InvocationContext inv) throws Exception {
        Method m = inv.getMethod();
        String methodName = m.getName();
        svLogger.info(CLASS_NAME + ".aroundInvoke: " + methodName);
        if (methodName.equals("getAnnotationInterceptorResults") || methodName.equals("onStringMessage")) {
            FATMDBHelper.emptyQueue(CF_NAME, REQUEST_QUEUE);

            assertNotNull("Checking for non-null ejbLocalRef", ejbLocalRef);
            ejbLocalRef.setString("AnnotationInjectionInterceptor");
            assertEquals("Checking ejbLocalRef set/getString",
                         "AnnotationInjectionInterceptor", ejbLocalRef.getString());

            assertNotNull("Checking for non-null ejbRemoteRef", ejbRemoteRef);
            ejbRemoteRef.setString("AnnotationInjectionInterceptor");
            assertEquals("Checking ejbRemoteRef set/getString",
                         "AnnotationInjectionInterceptor", ejbRemoteRef.getString());

            assertNotNull("Checking for non-null envEntry", envEntry);
            assertEquals("Checking value of env entry",
                         "Hello AnnotationInjectionInterceptor!", envEntry);

            // Using the Queue connection factory and queue will prove those injections were successful
            assertNotNull("Checking for non-null Resource ref", qcf);
            assertNotNull("Checking for non-null message destination ref(reqQueue)", reqQueue);
            assertNotNull("Checking for non-null resource env ref(resQueue)", resQueue);
            svLogger.info("AnnotationInjectionInterceptor pre-send results: " + PASSED);
            if (methodName.equals("onStringMessage")) {
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
    @PreDestroy
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
