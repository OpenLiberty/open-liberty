/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testtooling.vehicle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.UserTransaction;

import org.junit.Assert;

import com.ibm.ws.testtooling.msgcli.MessagingClient;
import com.ibm.ws.testtooling.msgcli.jms.JMSClientFactory;
import com.ibm.ws.testtooling.msgcli.smc.StatefulMessengerClient;
import com.ibm.ws.testtooling.testinfo.JMSClientContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.MessagingClientContext;
import com.ibm.ws.testtooling.testinfo.MessagingClientContext.MessagingClientType;
import com.ibm.ws.testtooling.testinfo.StatefulMessengerClientContext;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.tranjacket.EMTransactionJacket;
import com.ibm.ws.testtooling.tranjacket.JTATransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/**
 * Helper tool for processing TextExecutionContexts on the Application Server side.
 *
 */
public class JEEExecutionContextHelper {
    /**
     * Prints test information to standard out, to define test execution boundaries in server logs.
     *
     * @param testExecCtx
     */
    public static void printBeginTestInfo(TestExecutionContext testExecCtx) {
        StringBuffer sb = new StringBuffer();
        sb.append("\n** TEST BEGIN ********************************************************************\n");
        sb.append("Test Name: ").append(testExecCtx.getName()).append("\n");
        sb.append("  Test Logic Class: ").append(testExecCtx.getTestLogicClassName()).append("\n");
        sb.append("  Test Logic Method: ").append(testExecCtx.getTestLogicMethod()).append("\n");
        sb.append("  Test Session Sig: ").append(testExecCtx.getTestSessionSig()).append("\n");

        sb.append("  Test Properties:\n");
        HashMap props = testExecCtx.getProperties();
        int propCount = 1;
        for (Object key : props.keySet()) {
            sb.append("     ").append(propCount++).append(") ").append(key).append(": ").append(props.get(key)).append("\n");
        }

        sb.append("  JPA Resources:\n");
        HashMap jpaRes = testExecCtx.getJpaPCInfoMap();
        int jpaResCount = 1;
        for (Object key : jpaRes.keySet()) {
            sb.append("     ").append(jpaResCount++).append(") ").append(key).append(": ").append(jpaRes.get(key)).append("\n");
        }

        sb.append("  Messenger Resources:\n");
        HashMap msgRes = testExecCtx.getMsgClientMap();
        int msgResCount = 1;
        for (Object key : msgRes.keySet()) {
            sb.append("     ").append(msgResCount++).append(") ").append(key).append(": ").append(msgRes.get(key)).append("\n");
        }
        sb.append("\n**********************************************************************\n");

        System.out.println(sb);
    }

    public static void printEndTestInfo(TestExecutionContext testExecCtx) {
        StringBuffer sb = new StringBuffer();
        sb.append("\n** TEST END **********************************************************************\n");
        sb.append("Test Name: ").append(testExecCtx.getName()).append("\n");
        sb.append("  Test Logic Class: ").append(testExecCtx.getTestLogicClassName()).append("\n");
        sb.append("  Test Logic Method: ").append(testExecCtx.getTestLogicMethod()).append("\n");
        sb.append("  Test Session Sig: ").append(testExecCtx.getTestSessionSig()).append("\n");

        sb.append("\n**********************************************************************\n");
        System.out.println(sb);
    }

    public static TestExecutionResources processTestExecutionResources(
                                                                       TestExecutionContext testExecCtx,
                                                                       Object managedComponentObject,
                                                                       UserTransaction tx) {
        TestExecutionResources testExecResources = new TestExecutionResources();

        System.out.println("JEEExecutionContextHelper: Processing TestExecutionContext: " + testExecCtx.getName() + " ...");

        // Process JPA Resources
        Map<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        for (String jpaCtxInfoKey : jpaPCInfoMap.keySet()) {
            JPAPersistenceContext jpaCtxInfo = jpaPCInfoMap.get(jpaCtxInfoKey);
            JPAResource jpaResource = processJPAPersistenceContextInfo(jpaCtxInfo, managedComponentObject, tx);
            if (jpaResource != null) {
                testExecResources.getJpaResourceMap().put(jpaCtxInfoKey, jpaResource);
            }
        }

        // Process Messaging Resources
        Map<String, MessagingClientContext> msgClientMap = testExecCtx.getMsgClientMap();
        for (String msgCtxInfoKey : msgClientMap.keySet()) {
            MessagingClientContext msgCtxInfo = msgClientMap.get(msgCtxInfoKey);
            MessagingClient msgClient = processMessagingClientContext(msgCtxInfo, managedComponentObject);
            if (msgClient != null) {
                testExecResources.getMsgCliResourceMap().put(msgCtxInfoKey, msgClient);
            }

        }

        return testExecResources;
    }

    public static void destroyExecutionResources(TestExecutionResources testExecResources) {
        // Destroy JPA Resources
        if (testExecResources == null) {
            return;
        }

        Map<String, JPAResource> jpaResourceMap = testExecResources.getJpaResourceMap();
        for (String jpaResourceKey : jpaResourceMap.keySet()) {
            JPAResource jpaResource = jpaResourceMap.get(jpaResourceKey);
            if (jpaResource.getTj() != null && jpaResource.getTj().isTransactionActive()) {
                try {
                    jpaResource.getTj().rollbackTransaction();
                } catch (Throwable t) {
                    Assert.fail("Failed to close active transaction: " + t);
                }
            }
            try {
                JPAPersistenceContext jpaPCtx = jpaResource.getPcCtxInfo();
                switch (jpaPCtx.getPcType()) {
                    case JSE:
                    case APPLICATION_MANAGED_RL:
                    case APPLICATION_MANAGED_JTA:
                        jpaResource.close();
                        break;
                    case CONTAINER_MANAGED_TS:
                        break;
                    case CONTAINER_MANAGED_ES:
                        jpaResource.getEm().clear();
                    default:
                        break;
                }
            } catch (Throwable t) {
                Assert.fail("Failed to close JPA Resource " + jpaResource.toString() + " : " + t);
            }
        }

        HashMap<String, MessagingClient> msgClientMap = testExecResources.getMsgCliResourceMap();
        for (String msgClientKey : msgClientMap.keySet()) {
            MessagingClient msgClient = msgClientMap.get(msgClientKey);
            msgClient.close();
        }
    }

    public static JPAResource processJPAPersistenceContextInfo(
                                                               final JPAPersistenceContext pcCtxInfo,
                                                               final Object managedComponentObject,
                                                               final UserTransaction tx) {
        if (pcCtxInfo == null || managedComponentObject == null) {
            return null;
        }

        JPAResource jpaResource = null;
        System.out.println("JEEExecutionContextHelper: Processing JPA CtxInfo: " + pcCtxInfo.getName() + " ...");
        System.out.println("JPA CtxInfo:" + pcCtxInfo.toString());
        System.out.println("Managed Component: " + managedComponentObject.toString());

        try {
            Object obj = null;

            // Acquire Injected Resource
            switch (pcCtxInfo.getInjectionType()) {
                case JNDI:
                    InitialContext ic = null;
                    try {
                        ic = new InitialContext();
                        StringBuffer sb = new StringBuffer(); // ("java:comp/env/");
                        sb.append(pcCtxInfo.getResource());
                        System.out.println("Performing JNDI lookup for JPA Resource at " + sb.toString() + "...");

                        obj = ic.lookup(sb.toString());
                        System.out.println("JNDI lookup returned object " + obj);
                    } finally {
                        if (ic != null) {
                            ic.close();
                        }
                    }

                    break;
                case FIELD:

                    obj = AccessController.doPrivileged(new PrivilegedAction() {
                        @Override
                        public Object run() {
                            Class managedObjectClass = managedComponentObject.getClass();
                            Field jpaResourceField = null;

                            do {
                                if (java.lang.Object.class.equals(managedObjectClass)) {
                                    // Ran up to java.lang.Object, so this field does not exist.
                                    Assert.fail("Unable to resolve field \"" + pcCtxInfo.getResource() +
                                                "\" with managed component \"" +
                                                managedComponentObject.getClass().getName() + "\".");

                                    return null;
                                }

                                try {
                                    jpaResourceField = managedObjectClass.getDeclaredField(pcCtxInfo.getResource());

                                    // Temporally set the field's accessibility to true so we can access the field,
                                    // and then extract the JPA resource
                                    boolean accessible = new Boolean(jpaResourceField.isAccessible());
                                    jpaResourceField.setAccessible(true);
                                    Object retObj = jpaResourceField.get(managedComponentObject);
                                    jpaResourceField.setAccessible(accessible);

                                    return retObj;
//                                break;
                                } catch (NoSuchFieldException nsfe) {
                                    // Try the class higher up the inheritance hierarchy, until we hit Object (failure).
                                    managedObjectClass = managedObjectClass.getSuperclass();
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                    return null;
                                }
                            } while (true);
                        }
                    });

                    break;
                case METHOD:
                    // TODO: implement method injection
                default:
                    // Unsupported Injection Type
                    Assert.fail("Unsupported Injection Type: " + pcCtxInfo.getInjectionType().toString());
                    return null;
            }

            // Process the Injected Resource that was acquired.
            EntityManagerFactory emf = null;
            EntityManager em = null;

            switch (pcCtxInfo.getPcType()) {
                case APPLICATION_MANAGED_RL:
                case APPLICATION_MANAGED_JTA:
                    // Injected resource should be an EntityManagerFactory
                    if (!(obj instanceof EntityManagerFactory)) {
                        Assert.fail("Expecting EntityManagerFactory, received: " + obj.getClass().toString());
                        return null;
                    }

                    emf = (EntityManagerFactory) obj;
                    em = null;

                    if (pcCtxInfo.getEmMap() == null || pcCtxInfo.getEmMap().isEmpty()) {
                        em = emf.createEntityManager();
                    } else {
                        emf.createEntityManager(pcCtxInfo.getEmMap());
                    }

                    if (pcCtxInfo.getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA) {
                        // AM-JTA users JTA Transactions
                        jpaResource = new JPAResource(pcCtxInfo, emf, em, ((tx != null) ? new JTATransactionJacket(tx, true) : null));
                    } else {
                        // AM-RL uses EntityManager Transactions
                        jpaResource = new JPAResource(pcCtxInfo, emf, em, new EMTransactionJacket(em.getTransaction()));
                    }

                    break;
                case CONTAINER_MANAGED_TS:
                case CONTAINER_MANAGED_ES:
                    // Injected resource should be an EntityManager
                    if (!(obj instanceof EntityManager)) {
                        Assert.fail("Expecting EntityManager, received: " + obj.getClass().toString());
                        return null;
                    }
                    em = (EntityManager) obj;

                    jpaResource = new JPAResource(pcCtxInfo, null, em, ((tx != null) ? new JTATransactionJacket(tx, true) : null));
                    break;
                default:
                    // Unsupported Persistence Context Type
                    Assert.fail("Unsupported Persistence Context Type: " + pcCtxInfo.getPcType().toString());
                    return null;
            }
        } catch (Throwable t) {
            // Comprehensive Failure Recovery Catch-Block
            Assert.fail("JEEExecutionContextHelper: Unexpected failure processing JPA Persistence Context: " + t);
            return null;
        } finally {
            System.out.println("JEEExecutionContextHelper: Processing for JPA CtxInfo: " + pcCtxInfo.getName() + " complete.");
            if (jpaResource != null && jpaResource.getEm() != null && jpaResource.getEm().getDelegate() != null) {
                System.out.println("Delegate: " + jpaResource.getEm().getDelegate().getClass().toString());
            }
        }

        return jpaResource;
    }

    public static MessagingClient processMessagingClientContext(
                                                                MessagingClientContext msgCtxInfo,
                                                                Object managedComponentObject) {
        if (msgCtxInfo == null || managedComponentObject == null) {
            return null;
        }

        MessagingClient messageClient = null;

        System.out.println("JEEExecutionContextHelper: Processing Messaging CtxInfo: " + msgCtxInfo.getName() + " ...");
        System.out.println("Messaging CtxInfo:" + msgCtxInfo.toString());
        System.out.println("Managed Component: " + managedComponentObject.toString());

        InitialContext ic = null;
        try {
            ic = new InitialContext();

            if (msgCtxInfo.getMessagingClientType() == MessagingClientType.StatefulMessengerClient) {
                StatefulMessengerClientContext smCtxInfo = (StatefulMessengerClientContext) msgCtxInfo;

                StatefulMessengerClient smc = (StatefulMessengerClient) ic.lookup(smCtxInfo.getBeanName());
                if (smc != null) {
                    if (smCtxInfo.isFullDuplexMode()) {
                        smc.initialize(smCtxInfo.getName(), smCtxInfo.getTransmitterClient());
                    } else {
                        smc.initialize(smCtxInfo.getName(), smCtxInfo.getReceiverClient(), smCtxInfo.getTransmitterClient());
                    }

                    messageClient = smc;
                }
            } else if (msgCtxInfo.getMessagingClientType() == MessagingClientType.JMSClient) {
                JMSClientContext jmsCliCtxInfo = (JMSClientContext) msgCtxInfo;

                messageClient = JMSClientFactory.createJMSMessagingClient(jmsCliCtxInfo.getName(), jmsCliCtxInfo.getJmsClientCfg());
            }
        } catch (Throwable t) {
            // Comprehensive Failure Recovery Catch-Block
            Assert.fail("JEEExecutionContextHelper: Unexpected failure processing Messaging Context: " + t);
            return null;
        } finally {
            System.out.println("JEEExecutionContextHelper: Processing for Messaging CtxInfo: " + msgCtxInfo.getName() + " complete.");

            if (ic != null) {
                try {
                    ic.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }
        }

        return messageClient;
    }

    public static void executeTestLogic(
                                        TestExecutionContext testExecCtx,
                                        TestExecutionResources testExecResources,
                                        Object managedComponentObject) {
        // Before Test Logic Execution
        // ... (good place to fire messages to MDBs that simulate concurrent clients)

        // Execute the Test Logic
        // Test Logic Methods must always take the form of:
        // public void testLogicMethodName(TestExecutionContext testExecCtx,
        //                                 TestExecutionResources testExecResources,
        //                                                         Object managedComponentObject,
        //                                                         TestRecord tr)
        // The test logic method should not throw any Exceptions (RuntimeExceptions will
        // be caught by executeTestLogic()).
        try {
            System.out.println("JEEExecutionContextHelper: Executing Test Logic: " +
                               testExecCtx.getTestLogicClassName() + "." + testExecCtx.getTestLogicMethod() + "()...");

            // TODO: Review need for doPriv blocks, and perhaps use the Thread Context Classloader
            ClassLoader ctxClassLoader = Thread.currentThread().getContextClassLoader();

            Class<?> testLogicClass = Class.forName(testExecCtx.getTestLogicClassName(), true, ctxClassLoader);
            Constructor<?> constructor = testLogicClass.getConstructor(new Class[] {});
            Object testTestLogicObject = constructor.newInstance(new Object[] {});

            Class<?>[] testMethodParms = new Class<?>[] { TestExecutionContext.class, TestExecutionResources.class,
                                                          Object.class };
            Object[] testMethodParmsVals = new Object[] { testExecCtx, testExecResources, managedComponentObject };
            Method testStrategyMethodObj = testLogicClass.getMethod(testExecCtx.getTestLogicMethod(), testMethodParms);

            testStrategyMethodObj.invoke(testTestLogicObject, testMethodParmsVals);
        } catch (InvocationTargetException ete) {
            final Throwable cause = ete.getCause();
            if (cause instanceof java.lang.AssertionError) {
                System.out.println("Assertion Failed: " + cause);
                throw (java.lang.AssertionError) cause;
            } else {
                ete.printStackTrace();
                Assert.fail("JEEExecutionContextHelper: Unexpected Exception caught while executing test: " +
                            testExecCtx.getTestLogicClassName() + "." + testExecCtx.getTestLogicMethod() + " " + cause);
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Comprehensive Failure Recovery Catch-Block for RuntimeExceptions
            Assert.fail("JEEExecutionContextHelper: Unexpected Exception caught while executing test: " +
                        testExecCtx.getTestLogicClassName() + "." + testExecCtx.getTestLogicMethod() + " " + t);
        } finally {
            System.out.println("JEEExecutionContextHelper: Completed Execution of Test Logic: " +
                               testExecCtx.getTestLogicClassName() + "." + testExecCtx.getTestLogicMethod() + "().");
        }

        // After Test Logic Execution
        // ... (good place to fire messages to post-exec MDBs and to collect messages from these MDBs)
    }
}