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

import static javax.ejb.TransactionManagementType.BEAN;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.interceptor.ExcludeClassInterceptors;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.interceptor.Interceptors;

import com.ibm.websphere.ejbcontainer.test.tools.FATMDBHelper;

/**
 * Class for testing injection into fields of an interceptor class.
 * Only the StatefulInterceptorInjectionLocal interface method has
 * an implementation. The MixedSFLocal and MixedSFRemote have empty
 * implementation in this class. This is done to ensure auto-link is
 * not used when a @EJB or <ejb-ref> is used to refer to either the
 * MixedSFLocal or MixedSFRemote business interfaces that is implemented
 * by the MixedSFInterceptorBean class. This allows us to force the
 * explicit binding file to be used when resolving EJB references.
 */
@Stateful
@Local({ MixedSFLocal.class, StatefulInterceptorInjectionLocal.class })
@Remote(MixedSFRemote.class)
@ExcludeDefaultInterceptors
@TransactionManagement(BEAN)
@Interceptors({ AnnotationInjectionInterceptor.class, AnnotationInjectionInterceptor2.class, XMLInjectionInterceptor.class, XMLInjectionInterceptor2.class,
                XMLInjectionInterceptor3.class, XMLInjectionInterceptor4.class })
public class StatefulInterceptorInjectionBean implements StatefulInterceptorInjectionLocal, MixedSFLocal, MixedSFRemote {
    private static final String CLASS_NAME = StatefulInterceptorInjectionBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    /**
     * This method is used when testing injection into a field
     * of an interceptor class when the @EJB or @Resource annotation is used
     * to annotate a field in this interceptor class and the
     * bindings for each of these reference types is inside of a
     * <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Override
    @ExcludeClassInterceptors
    @Interceptors({ AnnotationInjectionInterceptor.class })
    public String getAnnotationInterceptorResults() {
        String qcfJndiName = "java:comp/env/AnnotationInjectionInterceptor/jms/WSTestQCF";
        String qJndiName = "java:comp/env/AnnotationInjectionInterceptor/jms/RequestQueue";
        svLogger.info("Waiting for message on: " + qJndiName);

        try {
            String results = (String) FATMDBHelper.getQueueMessage(qcfJndiName, qJndiName);
            assertNotNull("No results returned from queue", results);
            return results;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            svLogger.info("Unable to getQueueMessage : " +
                          ex.getClass().getName() + " : " + ex.getMessage());
        }
        return "FAILED : " + CLASS_NAME + ".getAnnotationInterceptorResults";
    }

    /**
     * This method is used when testing injection into a field
     * of an interceptor class when the @EJB or @Resource annotation is used
     * to annotate a field in this interceptor class and the bindings for
     * each of these reference types is inside of the <session> stanza
     * in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Override
    @ExcludeClassInterceptors
    @Interceptors({ AnnotationInjectionInterceptor2.class })
    public String getAnnotationInterceptor2Results() {
        String qcfJndiName = "java:comp/env/AnnotationInjectionInterceptor2/jms/WSTestQCF";
        String qJndiName = "java:comp/env/AnnotationInjectionInterceptor2/jms/RequestQueue";
        svLogger.info("Waiting for message on: " + qJndiName);

        try {
            String results = (String) FATMDBHelper.getQueueMessage(qcfJndiName, qJndiName);
            assertNotNull("No results returned from queue", results);
            return results;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            svLogger.info("Unable to getQueueMessage : " +
                          ex.getClass().getName() + " : " + ex.getMessage());
        }
        return "FAILED : " + CLASS_NAME + ".getAnnotationInterceptor2Results";
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
    @Interceptors({ XMLInjectionInterceptor.class })
    public String getXMLInterceptorResults() {
        String qcfJndiName = "java:comp/env/XMLInjectionInterceptor/jms/WSTestQCF";
        String qJndiName = "java:comp/env/XMLInjectionInterceptor/jms/RequestQueue";
        svLogger.info("Waiting for message on: " + qJndiName);

        try {
            String results = (String) FATMDBHelper.getQueueMessage(qcfJndiName, qJndiName);
            assertNotNull("No results returned from queue", results);
            return results;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            svLogger.info("Unable to getQueueMessage : " +
                          ex.getClass().getName() + " : " + ex.getMessage());
        }
        return "FAILED : " + CLASS_NAME + ".getXMLInterceptorResults";
    }

    /**
     * This method is used when testing injection into a field
     * of an interceptor class when <injection-target> is used inside of a
     * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
     * and <message-destination-ref> stanza that is within a <interceptor>
     * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
     * Also, the bindings for each of these reference type is inside of a
     * a <session> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Override
    @ExcludeClassInterceptors
    @Interceptors({ XMLInjectionInterceptor2.class })
    public String getXMLInterceptor2Results() {
        String qcfJndiName = "java:comp/env/XMLInjectionInterceptor2/jms/WSTestQCF";
        String qJndiName = "java:comp/env/XMLInjectionInterceptor2/jms/RequestQueue";
        svLogger.info("Waiting for message on: " + qJndiName);

        try {
            String results = (String) FATMDBHelper.getQueueMessage(qcfJndiName, qJndiName);
            assertNotNull("No results returned from queue", results);
            return results;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            svLogger.info("Unable to getQueueMessage : " +
                          ex.getClass().getName() + " : " + ex.getMessage());
        }
        return "FAILED : " + CLASS_NAME + ".getXMLInterceptor2Results";
    }

    /**
     * This method is used when testing injection into a field
     * of an interceptor class when <injection-target> is used inside of a
     * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
     * and <message-destination-ref> stanza that is within a <session>
     * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
     * Also, the bindings for each of these reference type is inside of a
     * <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Override
    @ExcludeClassInterceptors
    @Interceptors({ XMLInjectionInterceptor3.class })
    public String getXMLInterceptor3Results() {
        String qcfJndiName = "java:comp/env/XMLInjectionInterceptor3/jms/WSTestQCF";
        String qJndiName = "java:comp/env/XMLInjectionInterceptor3/jms/RequestQueue";
        svLogger.info("Waiting for message on: " + qJndiName);

        try {
            String results = (String) FATMDBHelper.getQueueMessage(qcfJndiName, qJndiName);
            assertNotNull("No results returned from queue", results);
            return results;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            svLogger.info("Unable to getQueueMessage : " +
                          ex.getClass().getName() + " : " + ex.getMessage());
        }
        return "FAILED : " + CLASS_NAME + ".getXMLInterceptor3Results";
    }

    /**
     * This method is used when testing injection into a field
     * of an interceptor class when <injection-target> is used inside of a
     * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>,
     * and <message-destination-ref> stanza that is within a <session>
     * stanza that appears in the ejb-jar.xml file of the EJB 3 module.
     * Also, the bindings for each of these reference type is inside of a
     * <session> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Override
    @ExcludeClassInterceptors
    @Interceptors({ XMLInjectionInterceptor4.class })
    public String getXMLInterceptor4Results() {
        String qcfJndiName = "java:comp/env/XMLInjectionInterceptor4/jms/WSTestQCF";
        String qJndiName = "java:comp/env/XMLInjectionInterceptor4/jms/RequestQueue";
        svLogger.info("Waiting for message on: " + qJndiName);

        try {
            String results = (String) FATMDBHelper.getQueueMessage(qcfJndiName, qJndiName);
            assertNotNull("No results returned from queue", results);
            return results;
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            svLogger.info("Unable to getQueueMessage : " +
                          ex.getClass().getName() + " : " + ex.getMessage());
        }
        return "FAILED : " + CLASS_NAME + ".getXMLInterceptor4Results";
    }

    @Override
    @Remove
    public void finish() {
        // intentional empty method stub
    }

    @Override
    @ExcludeClassInterceptors
    public void destroy() {
        // intentional empty method stub
    }

    @Override
    @ExcludeClassInterceptors
    public String getString() {
        //     intentional empty method stub
        return null;
    }

    @Override
    @ExcludeClassInterceptors
    public void setString(String str) {
        //     intentional empty method stub
    }

}
