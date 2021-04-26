/**
 *
 */
package com.ibm.ws.jpa.fvt.injection.tests;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;

import com.ibm.ws.jpa.fvt.injection.mdb.TestCoordinator;
import com.ibm.ws.jpa.fvt.injection.mdb.TestExecutionResult;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

import junit.framework.Assert;

/**
 *
 */
public abstract class AbstractMDBTestServlet extends JPATestServlet {
    /**  */
    private static final long serialVersionUID = -4510787067548883519L;

    @Resource(lookup = "jms/JPAConnectionFactory")
    private QueueConnectionFactory connectionFactory;

    private javax.jms.QueueConnection conn = null;
    private QueueSession queueSession = null;
    private QueueSender queueSender = null;
    private boolean jmsReady = false;

    protected final String testLogicClassName = "com.ibm.ws.jpa.fvt.injection.testlogic.JPAInjectionTestLogic";

    protected final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("cleanup",
                       new JPAPersistenceContext("cleanup", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/cleanup"));

        try {
            conn = connectionFactory.createQueueConnection();
            queueSession = conn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            queueSender = queueSession.createSender(getQueue());
            jmsReady = true;
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    protected abstract Queue getQueue();

    @Override
    protected void executeTestVehicle(TestExecutionContext ctx) {
        if (!jmsReady) {
            Assert.fail("JMS had failed to initialize.  Cannot run the test.");
        }

        TestCoordinator.initalize();
        TestExecutionResult tre = null;

        try {
            ObjectMessage objectMessage = queueSession.createObjectMessage(ctx);
            queueSender.send(objectMessage);
            tre = TestCoordinator.blockForMDBExecutionCompletion();
            if (tre == null) {
                Assert.fail("Did not receive TestExecutionResult.");
            } else if (tre.getException() != null) {
                throw tre.getException();
            }
        } catch (InterruptedException e) {
            // MDB did not respond or complete its work in a timely fashion.
            e.printStackTrace();
            Assert.fail("MDB did not respond or complete its work before timeout.");
        } catch (Throwable t) {
            logException(t, ctx);
        } finally {

        }
    }

}
