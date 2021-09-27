/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.timer.np.v32.web;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.timer.np.v32.shared.TestBean;
import com.ibm.ws.ejbcontainer.timer.np.v32.shared.TimerTestBean;

import componenttest.custom.junit.runner.Mode;

/**
 * Test the behavior of the TimerService.getTimers() API for non-persistent
 * timers. <p>
 *
 * The following scenarios are covered for each session bean type:
 *
 * <ul>
 * <li>Automatic timers declared in multiple modules, including a war module.
 * <li>Transactional nature of timer creation for programmatic timers created
 * in multiple modules, including a war module.
 * <li>Transactional nature of timer cancellation, both automatic and programmatic
 * in multiple modules, including a war module.
 * <li>SingleAction timers that expire are no longer returned by getAllTimers(),
 * across multiple modules, including a war module.
 * <li>Interval timers that expire will be returned by getAllTimers() until cancelled,
 * across multiple modules, including a war module.
 * </ul>
 */
@WebServlet("/NpTimerV32ApiServlet")
@SuppressWarnings("serial")
public class NpTimerV32ApiServlet extends AbstractServlet {
    private static final Logger logger = Logger.getLogger(NpTimerV32ApiServlet.class.getName());

    private static final String EJB_MODULE = "NpTimerV32ApiEJB";
    private static final String OTHER_EJB_MODULE = "NpTimerV32ApiOtherEJB";
    private static final String WAR_MODULE = "NpTimerV32ApiWeb";

    private static final String SINGLETON = "SingletonTimerBean";
    private static final String STATEFUL = "StatefulTestBean";
    private static final String STATELESS = "StatelessTimerBean";

    private static final long MAX_WAIT_TIME = 3 * 60 * 1000;

    @Resource
    UserTransaction userTran;

    private <T> T lookupBean(Class<T> intf, String moduleName, String beanName) throws NamingException {
        return intf.cast(new InitialContext().lookup("java:app/" + moduleName + "/" + beanName));
    }

    private <T extends TestBean> void testGetAllTimersAutomaticMultipleModules(Class<T> intf, String beanName) throws Exception {

        logger.info("   --> verifying getAllTimers for module " + EJB_MODULE + " and bean " + beanName);
        T bean = lookupBean(intf, EJB_MODULE, beanName);
        bean.verifyGetAllTimers(bean.getAllExpectedAutomaticTimerCount());

        logger.info("   --> verifying getAllTimers for module " + OTHER_EJB_MODULE + " and bean " + beanName);
        bean = lookupBean(intf, OTHER_EJB_MODULE, beanName);
        bean.verifyGetAllTimers(bean.getAllExpectedAutomaticTimerCount());

        logger.info("   --> verifying getAllTimers for module " + WAR_MODULE + " and bean " + beanName);
        bean = lookupBean(intf, WAR_MODULE, beanName);
        bean.verifyGetAllTimers(bean.getAllExpectedAutomaticTimerCount());
    }

    /**
     * Verify that TimerService.getAllTimers() on a Singleton bean will return all
     * Automatic timers for a module and will not include timers that exist for
     * other modules (including a war module).
     */
    @Test
    public void testGetAllTimersAutomaticMultipleModulesSingleton() throws Exception {
        testGetAllTimersAutomaticMultipleModules(TimerTestBean.class, SINGLETON);
    }

    /**
     * Verify that TimerService.getAllTimers() on a Stateful bean will return all
     * Automatic timers for a module and will not include timers that exist for
     * other modules (including a war module).
     */
    @Test
    public void testGetAllTimersAutomaticMultipleModulesStateful() throws Exception {
        testGetAllTimersAutomaticMultipleModules(TestBean.class, STATEFUL);
    }

    /**
     * Verify that TimerService.getAllTimers() on a Stateless bean will return all
     * Automatic timers for a module and will not include timers that exist for
     * other modules (including a war module).
     */
    @Test
    public void testGetAllTimersAutomaticMultipleModulesStateless() throws Exception {
        testGetAllTimersAutomaticMultipleModules(TimerTestBean.class, STATELESS);
    }

    private void testGetAllTimersProgrammaticTransactionalCreate(String beanName, String info) throws Exception {

        TimerTestBean bean = lookupBean(TimerTestBean.class, EJB_MODULE, beanName);
        TimerTestBean otherbean = lookupBean(TimerTestBean.class, OTHER_EJB_MODULE, beanName);
        TimerTestBean warbean = lookupBean(TimerTestBean.class, WAR_MODULE, beanName);

        int expectedAutomatic = bean.getAllExpectedAutomaticTimerCount();
        int otherExpectedAutomatic = otherbean.getAllExpectedAutomaticTimerCount();
        int warExpectedAutomatic = warbean.getAllExpectedAutomaticTimerCount();

        logger.info("   --> creating timers under new UserTransaction for beans : " + beanName);
        userTran.begin();
        bean.createTimers(5, info + "-rollback");
        otherbean.createTimers(4, info + "-rollback");
        warbean.createTimers(3, info + "-rollback");
        bean.verifyGetAllTimers(expectedAutomatic + 5);
        otherbean.verifyGetAllTimers(otherExpectedAutomatic + 4);
        warbean.verifyGetAllTimers(warExpectedAutomatic + 3);
        logger.info("   --> Rolling back UserTransaction : " + beanName);
        userTran.rollback();
        bean.verifyGetAllTimers(expectedAutomatic);
        otherbean.verifyGetAllTimers(otherExpectedAutomatic);
        warbean.verifyGetAllTimers(warExpectedAutomatic);

        logger.info("   --> creating timers under new UserTransaction for beans : " + beanName);
        userTran.begin();
        bean.createTimers(5, info + "-commit");
        otherbean.createTimers(4, info + "-commit");
        warbean.createTimers(3, info + "-commit");
        bean.verifyGetAllTimers(expectedAutomatic + 5);
        otherbean.verifyGetAllTimers(otherExpectedAutomatic + 4);
        warbean.verifyGetAllTimers(warExpectedAutomatic + 3);
        logger.info("   --> Committing UserTransaction : " + beanName);
        userTran.commit();
        bean.verifyGetAllTimers(expectedAutomatic + 5);
        otherbean.verifyGetAllTimers(otherExpectedAutomatic + 4);
        warbean.verifyGetAllTimers(warExpectedAutomatic + 3);
    }

    /**
     * For Singleton beans, verify that TimerService.getAllTimers() works properly
     * with the transactional nature of timer creation for programmatic timers
     * created in multiple modules, including a war module. <p>
     *
     * The expected behavior is:
     *
     * <ul>
     * <li> Timers created, but not yet committed will be returned by getAllTimers()
     * when called from within the same transaction.
     * <li> If the timer creation transaction rolls back, getAllTimers() will not
     * return the timers.
     * <li> Timers created and committed will be returned by getAllTimers().
     * </ul>
     */
    @Test
    public void testGetAllTimersProgrammaticTransactionalCreateSingleton() throws Exception {
        testGetAllTimersProgrammaticTransactionalCreate(SINGLETON, "testGetAllTimersProgrammaticTransactionalCreateSingleton");
    }

    /**
     * For Stateless beans, verify that TimerService.getAllTimers() works properly
     * with the transactional nature of timer creation for programmatic timers
     * created in multiple modules, including a war module. <p>
     *
     * The expected behavior is:
     *
     * <ul>
     * <li> Timers created, but not yet committed will be returned by getAllTimers()
     * when called from within the same transaction.
     * <li> If the timer creation transaction rolls back, getAllTimers() will not
     * return the timers.
     * <li> Timers created and committed will be returned by getAllTimers().
     * </ul>
     */
    @Test
    public void testGetAllTimersProgrammaticTransactionalCreateStateless() throws Exception {
        testGetAllTimersProgrammaticTransactionalCreate(STATELESS, "testGetAllTimersProgrammaticTransactionalCreateStateless");
    }

    private void testGetAllTimersTransactionalCancel(String beanName, String info) throws Exception {

        TimerTestBean bean = lookupBean(TimerTestBean.class, EJB_MODULE, beanName);
        TimerTestBean otherbean = lookupBean(TimerTestBean.class, OTHER_EJB_MODULE, beanName);
        TimerTestBean warbean = lookupBean(TimerTestBean.class, WAR_MODULE, beanName);

        int expectedAutomatic = bean.getAllExpectedAutomaticTimerCount();
        int otherExpectedAutomatic = otherbean.getAllExpectedAutomaticTimerCount();
        int warExpectedAutomatic = warbean.getAllExpectedAutomaticTimerCount();

        logger.info("   --> creating timers for beans : " + beanName);
        bean.createTimers(5, info);
        otherbean.createTimers(4, info);
        warbean.createTimers(3, info);

        logger.info("   --> cancelling timers under new UserTransaction for beans : " + beanName);
        userTran.begin();
        bean.cancelTwoTimers();
        otherbean.cancelTwoTimers();
        warbean.cancelTwoTimers();
        bean.verifyGetAllTimers(expectedAutomatic + 3);
        otherbean.verifyGetAllTimers(otherExpectedAutomatic + 2);
        warbean.verifyGetAllTimers(warExpectedAutomatic + 1);
        logger.info("   --> Rolling back UserTransaction : " + beanName);
        userTran.rollback();
        bean.verifyGetAllTimers(expectedAutomatic + 5);
        otherbean.verifyGetAllTimers(otherExpectedAutomatic + 4);
        warbean.verifyGetAllTimers(warExpectedAutomatic + 3);

        logger.info("   --> cancelling timers under new UserTransaction for beans : " + beanName);
        userTran.begin();
        bean.cancelTwoTimers();
        otherbean.cancelTwoTimers();
        warbean.cancelTwoTimers();
        bean.verifyGetAllTimers(expectedAutomatic + 3);
        otherbean.verifyGetAllTimers(otherExpectedAutomatic + 2);
        warbean.verifyGetAllTimers(warExpectedAutomatic + 1);
        logger.info("   --> Committing UserTransaction : " + beanName);
        userTran.commit();
        bean.verifyGetAllTimers(expectedAutomatic + 3);
        otherbean.verifyGetAllTimers(otherExpectedAutomatic + 2);
        warbean.verifyGetAllTimers(warExpectedAutomatic + 1);
    }

    /**
     * For Singleton beans, verify that TimerService.getAllTimers() works properly
     * with the transactional nature of timer cancellation for automatic and
     * programmatic timers created in multiple modules, including a war module. <p>
     *
     * The expected behavior is:
     *
     * <ul>
     * <li> Timers cancelled, but not yet committed will not returned by getAllTimers()
     * when called from within the same transaction.
     * <li> If the timer cancellation transaction rolls back, getAllTimers() will
     * return the timers.
     * <li> Timers cancelled and committed will not be returned by getAllTimers().
     * </ul>
     */
    @Test
    public void testGetAllTimersTransactionalCancelSingleton() throws Exception {
        testGetAllTimersTransactionalCancel(SINGLETON, "testGetAllTimersTransactionalCancelSingleton");
    }

    /**
     * For Stateless beans, verify that TimerService.getAllTimers() works properly
     * with the transactional nature of timer cancellation for automatic and
     * programmatic timers created in multiple modules, including a war module. <p>
     *
     * The expected behavior is:
     *
     * <ul>
     * <li> Timers cancelled, but not yet committed will not returned by getAllTimers()
     * when called from within the same transaction.
     * <li> If the timer cancellation transaction rolls back, getAllTimers() will
     * return the timers.
     * <li> Timers cancelled and committed will not be returned by getAllTimers().
     * </ul>
     */
    @Test
    public void testGetAllTimersTransactionalCancelStateless() throws Exception {
        testGetAllTimersTransactionalCancel(STATELESS, "testGetAllTimersTransactionalCancelStateless");
    }

    private void testGetAllTimersWithExpiredSingleActionTimer(String moduleName, String beanName, String info) throws Exception {

        TimerTestBean bean = lookupBean(TimerTestBean.class, moduleName, beanName);
        int expectedAutomatic = bean.getAllExpectedAutomaticTimerCount();

        logger.info("   --> creating single action timer for module : " + moduleName + ", and for bean : " + beanName);
        CountDownLatch timerLatch = bean.createSingleActionTimer(info);
        bean.verifyGetAllTimers(expectedAutomatic + 1);

        logger.info("   --> waiting for single action timer to complete");
        timerLatch.await(MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
        FATHelper.sleep(FATHelper.POST_INVOKE_DELAY * 2);
        bean.verifyGetAllTimers(expectedAutomatic);
    }

    /**
     * For Singleton beans in an EJB module, verify that a SingleAction timer
     * will no longer be returned by TimerService.getAllTimers() once the
     * timer has expired.
     */
    @Test
    public void testGetAllTimersWithExpiredSingleActionTimerEjbModuleSingleton() throws Exception {
        testGetAllTimersWithExpiredSingleActionTimer(EJB_MODULE, SINGLETON, "testGetAllTimersWithExpiredSingleActionTimerEjbModuleSingleton");
    }

    /**
     * For Stateless beans in an EJB module, verify that a SingleAction timer
     * will no longer be returned by TimerService.getAllTimers() once the
     * timer has expired.
     */
    @Test
    @Mode(Mode.TestMode.FULL) // Singleton version is LITE
    public void testGetAllTimersWithExpiredSingleActionTimerEjbModuleSateless() throws Exception {
        testGetAllTimersWithExpiredSingleActionTimer(EJB_MODULE, STATELESS, "testGetAllTimersWithExpiredSingleActionTimerEjbModuleStateless");
    }

    /**
     * For Singleton beans in a WAR module, verify that a SingleAction timer
     * will no longer be returned by TimerService.getAllTimers() once the
     * timer has expired.
     */
    @Test
    @Mode(Mode.TestMode.FULL) // Stateless version is LITE
    public void testGetAllTimersWithExpiredSingleActionTimerWarModuleSingleton() throws Exception {
        testGetAllTimersWithExpiredSingleActionTimer(WAR_MODULE, SINGLETON, "testGetAllTimersWithExpiredSingleActionTimerWarModuleSingleton");
    }

    /**
     * For Stateless beans in a WAR module, verify that a SingleAction timer
     * will no longer be returned by TimerService.getAllTimers() once the
     * timer has expired.
     */
    @Test
    public void testGetAllTimersWithExpiredSingleActionTimerWarModuleSateless() throws Exception {
        testGetAllTimersWithExpiredSingleActionTimer(WAR_MODULE, STATELESS, "testGetAllTimersWithExpiredSingleActionTimerWarModuleStateless");
    }

    private void testGetAllTimersWithExpiredIntervalTimer(String moduleName, String beanName, String info) throws Exception {

        TimerTestBean bean = lookupBean(TimerTestBean.class, moduleName, beanName);
        int expectedAutomatic = bean.getAllExpectedAutomaticTimerCount();

        logger.info("   --> creating interval timer for module : " + moduleName + ", and for bean : " + beanName);
        CountDownLatch[] timerLatches = bean.createIntervalTimer(info);
        bean.verifyGetAllTimers(expectedAutomatic + 1);

        logger.info("   --> waiting for interval timer to complete first time");
        timerLatches[0].await(MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
        FATHelper.sleep(FATHelper.POST_INVOKE_DELAY);
        bean.verifyGetAllTimers(expectedAutomatic + 1);
        timerLatches[1].countDown();

        logger.info("   --> waiting for interval timer to complete second time and cancel itself");
        timerLatches[2].await(MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
        FATHelper.sleep(FATHelper.POST_INVOKE_DELAY);
        bean.verifyGetAllTimers(expectedAutomatic);
    }

    /**
     * For Singleton beans in an EJB module, verify that an Interval timer
     * will continue to be returned by TimerService.getAllTimers(), even after
     * expiration, until the timer has been cancelled.
     */
    @Test
    @Mode(Mode.TestMode.FULL) // Stateless version is LITE
    public void testGetAllTimersWithExpiredIntervalTimerEjbModuleSingleton() throws Exception {
        testGetAllTimersWithExpiredIntervalTimer(EJB_MODULE, SINGLETON, "testGetAllTimersWithExpiredIntervalTimerEjbModuleSingleton");
    }

    /**
     * For Stateless beans in an EJB module, verify that an Interval timer
     * will continue to be returned by TimerService.getAllTimers(), even after
     * expiration, until the timer has been cancelled.
     */
    @Test
    public void testGetAllTimersWithExpiredIntervalTimerEjbModuleSateless() throws Exception {
        testGetAllTimersWithExpiredIntervalTimer(EJB_MODULE, STATELESS, "testGetAllTimersWithExpiredIntervalTimerEjbModuleStateless");
    }

    /**
     * For Singleton beans in a WAR module, verify that an Interval timer
     * will continue to be returned by TimerService.getAllTimers(), even after
     * expiration, until the timer has been cancelled.
     */
    @Test
    public void testGetAllTimersWithExpiredIntervalTimerWarModuleSingleton() throws Exception {
        testGetAllTimersWithExpiredIntervalTimer(WAR_MODULE, SINGLETON, "testGetAllTimersWithExpiredIntervalTimerWarModuleSingleton");
    }

    /**
     * For Stateless beans in a WAR module, verify that an Interval timer
     * will continue to be returned by TimerService.getAllTimers(), even after
     * expiration, until the timer has been cancelled.
     */
    @Test
    @Mode(Mode.TestMode.FULL) // Singleton version is LITE
    public void testGetAllTimersWithExpiredIntervalTimerWarModuleSateless() throws Exception {
        testGetAllTimersWithExpiredIntervalTimer(WAR_MODULE, STATELESS, "testGetAllTimersWithExpiredIntervalTimerWarModuleStateless");
    }

    @Override
    protected void clearAllProgrammaticTimers() {
        try {
            logger.info("   --> clearAllProgrammaticTimers : " + EJB_MODULE);
            TimerTestBean bean = lookupBean(TimerTestBean.class, EJB_MODULE, SINGLETON);
            bean.clearAllProgrammaticTimers();
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
        }
        try {
            logger.info("   --> clearAllProgrammaticTimers : " + OTHER_EJB_MODULE);
            TestBean bean = lookupBean(TestBean.class, OTHER_EJB_MODULE, STATEFUL);
            bean.clearAllProgrammaticTimers();
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
        }
        try {
            logger.info("   --> clearAllProgrammaticTimers : " + WAR_MODULE);
            TimerTestBean bean = lookupBean(TimerTestBean.class, WAR_MODULE, STATELESS);
            bean.clearAllProgrammaticTimers();
        } catch (Throwable ex) {
            ex.printStackTrace(System.out);
        }
    }
}
