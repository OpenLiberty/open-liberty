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

import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;
import javax.jms.Message;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.ws.ejbcontainer.fat.rar.message.MessageListener;

/**
 * Class for testing injection into fields of an interceptor class
 * when bound to a message-driven bean.
 */
@MessageDriven(activationConfig = {
                                    @ActivationConfigProperty(propertyName = "name", propertyValue = "MessageDrivenInjectionBean")
},
               name = "MessageDrivenInjectionBean")
@ExcludeDefaultInterceptors
@Interceptors({ AnnotationInjectionInterceptor.class, AnnotationInjectionInterceptor2.class, XMLInjectionInterceptor.class, XMLInjectionInterceptor2.class,
                XMLInjectionInterceptor3.class, XMLInjectionInterceptor4.class })
public class MessageDrivenInjectionBean implements MessageListener {
    private static final String CLASS_NAME = MessageDrivenInjectionBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);
    public static String svResults = null;

    /**
     * This method is used when testing injection into a field
     * of an interceptor class when the @EJB or @Resource annotation is used
     * to annotate a field in this interceptor class and the
     * bindings for each of these reference types is inside of a
     * <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Override
    @ExcludeClassInterceptors
    @Interceptors(AnnotationInjectionInterceptor.class)
    public void onStringMessage(String arg0) {
        svLogger.info("onStringMessage: " + arg0);

        try {
            InitialContext ic = new InitialContext();
            String jndiName = "java:comp/env/AnnotationInjectionInterceptor/jms/WSTestQCF";
            Object obj = ic.lookup(jndiName);
            assertNotNull("MessageDriveInjectionBean.onStringMessage jms/WSTestQCF lookup in java:comp success", obj);
            obj = ic.lookup("java:comp/env/AnnotationInjectionInterceptor/jms/RequestQueue");
            assertNotNull("MessageDriveInjectionBean.onStringMessage jms/RequestQueue lookup in java:comp success", obj);
            setResults("Passed : onStringMessage");
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            svLogger.info("onStringMessage : lookup failed : " +
                          ex.getClass().getName() + " : " + ex.getMessage());
            setResults("Failed : onStringMessage : " +
                       ex.getClass().getName() + ":" + ex.getMessage());
        }

        svLogger.info("onStringMessage results: " + svResults);
    }

    /**
     * This method is used when testing injection into a field
     * of an interceptor class when the @EJB or @Resource annotation is used
     * to annotate a field in this interceptor class and the bindings for
     * each of these reference types is inside of the <message-driven> stanza
     * in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Override
    @ExcludeClassInterceptors
    @Interceptors(AnnotationInjectionInterceptor2.class)
    public void onMessage(Message arg0) {
        svLogger.info("onMessage: " + arg0);

        try {
            InitialContext ic = new InitialContext();
            String jndiName = "java:comp/env/AnnotationInjectionInterceptor2/jms/WSTestQCF";
            Object obj = ic.lookup(jndiName);
            assertNotNull("MessageDriveInjectionBean.onMessage jms/WSTestQCF lookup in java:comp success", obj);
            obj = ic.lookup("java:comp/env/AnnotationInjectionInterceptor2/jms/RequestQueue");
            assertNotNull("MessageDriveInjectionBean.onMessage jms/RequestQueue lookup in java:comp success", obj);
            setResults("Passed : onMessage");
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            svLogger.info("onMessage : lookup failed : " +
                          ex.getClass().getName() + " : " + ex.getMessage());
            setResults("Failed : onMessage : " +
                       ex.getClass().getName() + ":" + ex.getMessage());
        }

        svLogger.info("onMessage results: " + svResults);
    }

    /**
     * This method is used when testing injection into a field
     * of an interceptor class when <injection-target> is used inside of a
     * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
     * and <message-destination-ref> stanza that is within a <interceptor>
     * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
     * The bindings for each of these reference type is inside of a
     * <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Override
    @ExcludeClassInterceptors
    @Interceptors(XMLInjectionInterceptor.class)
    public void onIntegerMessage(Integer arg0) {
        svLogger.info("onIntegerMessage: " + arg0);

        try {
            InitialContext ic = new InitialContext();
            String jndiName = "java:comp/env/XMLInjectionInterceptor/jms/WSTestQCF";
            Object obj = ic.lookup(jndiName);
            assertNotNull("MessageDriveInjectionBean.onIntegerMessage jms/WSTestQCF lookup in java:comp success", obj);
            obj = ic.lookup("java:comp/env/XMLInjectionInterceptor/jms/RequestQueue");
            assertNotNull("MessageDriveInjectionBean.onIntegerMessage jms/RequestQueue lookup in java:comp success", obj);
            setResults("Passed : onIntegerMessage");
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            svLogger.info("onIntegerMessage : lookup failed : " +
                          ex.getClass().getName() + " : " + ex.getMessage());
            setResults("Failed : onIntegerMessage : " +
                       ex.getClass().getName() + ":" + ex.getMessage());
        }

        svLogger.info("onIntegerMessage results: " + svResults);
    }

    /**
     * This method is used when testing injection into a field
     * of an interceptor class when <injection-target> is used inside of a
     * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
     * and <message-destination-ref> stanza that is within a <interceptor>
     * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
     * Also, the bindings for each of these reference type is inside of a
     * the <message-driven> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Override
    @Interceptors(XMLInjectionInterceptor2.class)
    public void onGetTimestamp(String arg0) {
        svLogger.info("onGetTimeStamp: " + arg0);

        try {
            InitialContext ic = new InitialContext();
            String jndiName = "java:comp/env/XMLInjectionInterceptor2/jms/WSTestQCF";
            Object obj = ic.lookup(jndiName);
            assertNotNull("MessageDriveInjectionBean.onGetTimeStamp jms/WSTestQCF lookup in java:comp success", obj);
            obj = ic.lookup("java:comp/env/XMLInjectionInterceptor2/jms/RequestQueue");
            assertNotNull("MessageDriveInjectionBean.onGetTimeStamp jms/RequestQueue lookup in java:comp success", obj);
            setResults("Passed : onGetTimestamp");
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            svLogger.info("onGetTimestamp : lookup failed : " +
                          ex.getClass().getName() + " : " + ex.getMessage());
            setResults("Failed : onGetTimestamp : " +
                       ex.getClass().getName() + ":" + ex.getMessage());
        }

        svLogger.info("onGetTimeStamp results: " + svResults);
    }

    /**
     * This method is used when testing injection into a field
     * of an interceptor class when <injection-target> is used inside of a
     * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
     * and <message-destination-ref> stanza that is within a <message-driven>
     * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
     * Also, the bindings for each of these reference type is inside of a
     * <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Override
    @Interceptors(XMLInjectionInterceptor3.class)
    public void onCreateDBEntryNikki(String arg0) {
        svLogger.info("onCreateDBEntryNikki: " + arg0);

        try {
            InitialContext ic = new InitialContext();
            String jndiName = "java:comp/env/XMLInjectionInterceptor3/jms/WSTestQCF";
            Object obj = ic.lookup(jndiName);
            assertNotNull("MessageDriveInjectionBean.onCreateDBEntryNikki jms/WSTestQCF lookup in java:comp success", obj);
            obj = ic.lookup("java:comp/env/XMLInjectionInterceptor3/jms/RequestQueue");
            assertNotNull("MessageDriveInjectionBean.onCreateDBEntryNikki jms/RequestQueue lookup in java:comp success", obj);
            setResults("Passed : onCreateDBEntryNikki");
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            svLogger.info("onCreateDBEntryNikki : lookup failed : " +
                          ex.getClass().getName() + " : " + ex.getMessage());
            setResults("Failed : onCreateDBEntryNikki : " +
                       ex.getClass().getName() + ":" + ex.getMessage());
        }

        svLogger.info("onCreateDBEntryNikki results: " + svResults);
    }

    /**
     * This method is used when testing injection into a field
     * of an interceptor class when <injection-target> is used inside of a
     * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
     * and <message-destination-ref> stanza that is within a <message-driven>
     * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
     * Also, the bindings for each of these reference type is inside of a
     * <message-driven> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Override
    @Interceptors(XMLInjectionInterceptor4.class)
    public void onCreateDBEntryZiyad(String arg0) {
        svLogger.info("onCreateDBEntryZiyad: " + arg0);

        try {
            InitialContext ic = new InitialContext();
            String jndiName = "java:comp/env/XMLInjectionInterceptor4/jms/WSTestQCF";
            Object obj = ic.lookup(jndiName);
            assertNotNull("MessageDriveInjectionBean.onCreateDBEntryZiyad jms/WSTestQCF lookup in java:comp success", obj);
            obj = ic.lookup("java:comp/env/XMLInjectionInterceptor4/jms/RequestQueue");
            assertNotNull("MessageDriveInjectionBean.onCreateDBEntryZiyad jms/RequestQueue lookup in java:comp success", obj);
            setResults("Passed : onCreateDBEntryZiyad");
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            svLogger.info("onCreateDBEntryZiyad : lookup failed : " +
                          ex.getClass().getName() + " : " + ex.getMessage());
            setResults("Failed : onCreateDBEntryZiyad : " +
                       ex.getClass().getName() + ":" + ex.getMessage());
        }

        svLogger.info("onCreateDBEntryZiyad results: " + svResults);
    }

    @Override
    public void onThrowEJBException(String arg0) {
        // intentionally left empty
    }

    @Override
    public void onWait(String arg0) {
        //  intentionally left empty
    }

    public void setResults(String results) {
        if (svResults == null) {
            svResults = results;
        } else {
            svResults += ";" + results;
        }
    }

}
