/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.client.web;

import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_NO_TRANSACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.EJBTransactionRequiredException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;
import org.omg.CORBA.BAD_PARAM;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.websphere.ejbcontainer.test.tools.FATTransactionHelper;
import com.ibm.ws.ejbcontainer.remote.server.shared.TxAttrEJB;
import com.ibm.ws.ejbcontainer.remote.server.shared.TxAttrEJBHome;
import com.ibm.ws.ejbcontainer.remote.server.shared.TxAttrRemote;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;
import test.TestRemoteInterface;

/**
 * Tests variations of looking up remote enterprise beans located on a different server
 * and whether the EJB container performs the correct action for each of the possible TX
 * attribute values that can be assigned to a method of an EJB when making remote EJB
 * calls between two different servers.
 */
@WebServlet("/RemoteTxAttrServlet")
public class RemoteTxAttrServlet extends FATServlet {
    private static final long serialVersionUID = -5671511025293075382L;

    private static final Logger logger = Logger.getLogger(RemoteTxAttrServlet.class.getName());
    private static final Integer IIOPPort = Integer.getInteger("bvt.prop.IIOP");
    private static final Integer IIOPSecurePort = Integer.getInteger("bvt.prop.IIOP.secure");

    private static Context defaultContext;
    private static Context providerContext;
    private static Context remoteContext;

    // Name of module... for lookup.
    private static final String CorbaName = "corbaname::localhost:" + IIOPPort;
    private static final String CorbaNameNS = CorbaName + "/NameService";
    private static final String CorbaNameSecure = "corbaname::localhost:" + IIOPSecurePort;
    private static final String App = "RemoteServerApp";
    private static final String Module = "RemoteServerEJB";
    private static final String TxAttrBean = "TxAttrBean";
    private static final String TxAttrCompBean = "TxAttrCompBean";
    private static final String TestRemoteSingleton = "TestRemoteSingletonBean";
    private static final String TestRemoteStateful = "TestRemoteStatefulBean";
    private static final String TestRemoteStateless = "TestRemoteStatelessBean";

    private static final String TxAttrBeanJndi = "ejb/global/" + App + "/" + Module + "/" + TxAttrBean;
    private static final String TxAttrCompBeanJndi = "ejb/global/" + App + "/" + Module + "/" + TxAttrCompBean;
    private static final String TestRemoteSingletonJndi = "ejb/global/" + App + "/" + Module + "/" + TestRemoteSingleton;
    private static final String TestRemoteStatefulJndi = "ejb/global/" + App + "/" + Module + "/" + TestRemoteStateful;
    private static final String TestRemoteStatelessJndi = "ejb/global/" + App + "/" + Module + "/" + TestRemoteStateless;

    private static final String TxAttrRemote_Escape_5c = "com%5c.ibm%5c.ws%5c.ejbcontainer%5c.remote%5c.server%5c.shared%5c.TxAttrRemote";
    private static final String TxAttrRemote_Escape_5C = "com%5C.ibm%5C.ws%5C.ejbcontainer%5C.remote%5C.server%5C.shared%5C.TxAttrRemote";
    private static final String TxAttrRemote_Escape_Slash = "com\\.ibm\\.ws\\.ejbcontainer\\.remote\\.server\\.shared\\.TxAttrRemote";

    private TxAttrRemote txAttrBean;

    @Resource
    private UserTransaction userTran;

    /**
     * Provides the default InitialContext.
     *
     * @return an InitialContext with no properties
     * @throws NamingException if a NamingException is encountered
     */
    private static Context getDefaultContext() throws NamingException {
        if (defaultContext == null) {
            defaultContext = new InitialContext();
        }
        return defaultContext;
    }

    /**
     * Provides InitialContext created with Context.PROVIDER_URL for the remote system.
     *
     * @return an InitialContext with Context.PROVIDER_URL
     * @throws NamingException if a NamingException is encountered
     */
    private static Context getProviderContext() throws NamingException {
        if (providerContext == null) {
            logger.info("creating provider context with provider.url = corbaloc::127.0.0.1:" + IIOPPort + "/NameService");
            Properties p = new Properties();
            // TODO: remove or enable if testProviderUrlContextLookup is ever supported
            // p.put(Context.INITIAL_CONTEXT_FACTORY, "com.ibm.ws.jndi.iiop.InitialContextFactoryImpl");
            p.put(Context.PROVIDER_URL, "corbaloc::127.0.0.1:" + IIOPPort);
            providerContext = new InitialContext(p);
        }
        return providerContext;
    }

    /**
     * Provides the Context of the remote system NameService
     *
     * @return an InitialContext with Context.PROVIDER_URL
     * @throws NamingException if a NamingException is encountered
     */
    private static Context getRemoteContext() throws NamingException {
        if (remoteContext == null) {
            logger.info("creating remote context with provider.url = corbaloc::127.0.0.1:" + IIOPPort + "/NameService");
            Context context = new InitialContext();
            remoteContext = (Context) context.lookup("corbaname::localhost:" + IIOPPort + "/NameService");
        }
        return remoteContext;
    }

    /**
     * Provides an instance of the TxAttrBean. The first call to this method
     * will perform a lookup using the default InitialContext and full corbaname.
     */
    private TxAttrRemote getTxAttrBean() throws NamingException {
        if (txAttrBean == null) {
            // lookup the bean using default context with corbaname (including NameService)
            String jndiName = CorbaNameNS + "#" + TxAttrBeanJndi + "!" + TxAttrRemote.class.getName();
            txAttrBean = lookupRemoteBean(getDefaultContext(), jndiName, TxAttrRemote.class);
        }
        return txAttrBean;
    }

    public static <T> T lookupRemoteBean(String jndiName, Class<T> interfaceClass) throws NamingException {
        return lookupRemoteBean(getDefaultContext(), jndiName, interfaceClass);
    }

    public static <T> T lookupRemoteBean(Context context, String jndiName, Class<T> interfaceClass) throws NamingException {
        logger.info("lookupRemoteBean: JNDI = " + jndiName + ", interface = " + interfaceClass.getName());
        Object remoteObj = context.lookup(jndiName);
        logger.info("lookupRemoteBean: found = " + ((remoteObj == null) ? "null" : remoteObj.getClass().getName()));
        T remoteBean = interfaceClass.cast(PortableRemoteObject.narrow(remoteObj, interfaceClass));
        logger.info("lookupRemoteBean: returning = " + ((remoteBean == null) ? remoteBean : remoteBean.getClass().getName()));
        return remoteBean;
    }

    /**
     * Test looking up a remote EJB located on a different server using the
     * default InitialContext with the full corbaname (not including NameService).
     *
     * corbaname::localhost:<IIOPPort>#ejb/global/<App>/<Module>/<Bean>!<interface>
     */
    @Test
    public void testDefaultContextLookupWithCorbaname() throws Exception {
        // lookup the bean using default context with corbaname
        String jndiName = CorbaName + "#" + TxAttrBeanJndi + "!" + TxAttrRemote.class.getName();
        TxAttrRemote bean = lookupRemoteBean(getDefaultContext(), jndiName, TxAttrRemote.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works
        boolean global = bean.txRequired();
        assertTrue("Container did not begin global transaction for TX REQUIRED", global);

        // lookup the bean using default context with corbaname using %5c escape
        jndiName = CorbaName + "#" + TxAttrBeanJndi + "!" + TxAttrRemote_Escape_5c;
        bean = lookupRemoteBean(getDefaultContext(), jndiName, TxAttrRemote.class);
        assertNotNull("Remote bean is null", bean);
        assertTrue("Container did not begin global transaction for TX REQUIRED", bean.txRequired());

        // lookup the bean using default context with corbaname using %5C escape
        jndiName = CorbaName + "#" + TxAttrBeanJndi + "!" + TxAttrRemote_Escape_5C;
        bean = lookupRemoteBean(getDefaultContext(), jndiName, TxAttrRemote.class);
        assertNotNull("Remote bean is null", bean);
        assertTrue("Container did not begin global transaction for TX REQUIRED", bean.txRequired());

        // lookup the bean using default context with corbaname using \\ escape
        jndiName = CorbaName + "#" + TxAttrBeanJndi + "!" + TxAttrRemote_Escape_Slash;
        bean = lookupRemoteBean(getDefaultContext(), jndiName, TxAttrRemote.class);
        assertNotNull("Remote bean is null", bean);
        assertTrue("Container did not begin global transaction for TX REQUIRED", bean.txRequired());
    }

    /**
     * Test looking up a remote EJB located on a different server using the
     * default InitialContext with the full corbaname (including NameService).
     *
     * corbaname::localhost:<IIOPPort>/NameService#ejb/global/<App>/<Module>/<Bean>!<interface>
     */
    @Test
    public void testDefaultContextLookupWithCorbanameNameService() throws Exception {
        // lookup the bean using default context with corbaname (including NameService)
        String jndiName = CorbaNameNS + "#" + TxAttrBeanJndi + "!" + TxAttrRemote.class.getName();
        TxAttrRemote bean = lookupRemoteBean(getDefaultContext(), jndiName, TxAttrRemote.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works
        boolean global = bean.txRequired();
        assertTrue("Container did not begin global transaction for TX REQUIRED", global);
    }

    /**
     * Test looking up a remote EJB located on a different server using the
     * default InitialContext with the corbaname, not including NameService or interface.
     *
     * corbaname::localhost:<IIOPPort>#ejb/global/<App>/<Module>/<Bean>
     */
    // @Test - TODO: enable or remove test once determined if this should be supported
    public void testDefaultContextLookupWithCorbanameNoInterface() throws Exception {
        // lookup the bean using default context with corbaname, minus interface
        String jndiName = CorbaName + "#" + TxAttrBeanJndi;
        TxAttrRemote bean = lookupRemoteBean(getDefaultContext(), jndiName, TxAttrRemote.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works
        boolean global = bean.txRequired();
        assertTrue("Container did not begin global transaction for TX REQUIRED", global);
    }

    /**
     * Test looking up a remote EJB located on a different server using an InitialContext
     * with java.naming.provider.url and the lookup name without corbaname prefix.
     *
     * java.naming.provider.url : corbaloc::127.0.0.1:<IIOPPort>
     * ejb/global/<App>/<Module>/<Bean>!<interface>
     */
    // @Test - not supported
    public void testProviderUrlContextLookup() throws Exception {
        // lookup the bean using context with provider URL
        String jndiName = TxAttrBeanJndi + "!" + TxAttrRemote.class.getName();
        TxAttrRemote bean = lookupRemoteBean(getProviderContext(), jndiName, TxAttrRemote.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works
        boolean global = bean.txRequired();
        assertTrue("Container did not begin global transaction for TX REQUIRED", global);
    }

    /**
     * Test looking up a remote EJB located on a different server using the
     * context of the remote server NameService and the lookup name without corbaname prefix.
     *
     * remote NameService : corbaloc::127.0.0.1:<IIOPPort>
     * ejb/global/<App>/<Module>/<Bean>!<interface>
     */
    @Test
    public void testRemoteContextLookup() throws Exception {
        // lookup the bean using context of remote NameService
        String jndiName = TxAttrBeanJndi + "!" + TxAttrRemote.class.getName();
        TxAttrRemote bean = lookupRemoteBean(getRemoteContext(), jndiName, TxAttrRemote.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works
        boolean global = bean.txRequired();
        assertTrue("Container did not begin global transaction for TX REQUIRED", global);
    }

    /**
     * Test looking up a remote EJB located on a different server using the
     * default InitialContext with the full corbaname (including NameService)
     * and escaping the single period in the interface.
     *
     * An escape is required when the interface contains a single period, so also
     * verify the lookup fails without the escape.
     *
     * corbaname::localhost:<IIOPPort>/NameService#ejb/global/<App>/<Module>/<Bean>!<interface>
     */
    @Test
    public void testDefaultContextLookupWithEscape() throws Exception {
        // lookup the bean using default context with corbaname, with escape for period (.)
        String jndiName = CorbaNameNS + "#" + TestRemoteStatelessJndi + "!test%5c.TestRemoteInterface";
        TestRemoteInterface bean = lookupRemoteBean(getDefaultContext(), jndiName, TestRemoteInterface.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works
        assertEquals("Incorrect beanName returned", "TestRemoteStatelessBean", bean.getBeanName());

        // Also confirm that the same lookup without an escape fails
        jndiName = CorbaNameNS + "#" + TestRemoteStatelessJndi + "!test.TestRemoteInterface";
        try {
            bean = lookupRemoteBean(getDefaultContext(), jndiName, TestRemoteInterface.class);
            fail("lookup did not fail : " + bean);
        } catch (BAD_PARAM ex) {
            logger.info("Excpected exception occurred : " + ex);
        }
    }

    /**
     * Test looking up a remote EJB located on a different server using the
     * default InitialContext with the full corbaname (not including NameService)
     * using he secure IIOP port.
     *
     * corbaname::localhost:<IIOPSecurePort>#ejb/global/<App>/<Module>/<Bean>!<interface>
     */
    // @Test - requires additional security configuration
    public void testDefaultContextLookupWithSecurePort() throws Exception {
        // lookup the bean using default context with corbaname on secure IIOP port
        String jndiName = CorbaNameSecure + "#" + TxAttrBeanJndi + "!" + TxAttrRemote.class.getName();
        TxAttrRemote bean = lookupRemoteBean(getDefaultContext(), jndiName, TxAttrRemote.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works
        boolean global = bean.txRequired();
        assertTrue("Container did not begin global transaction for TX REQUIRED", global);
    }

    /**
     * Test looking up a remote EJBHome located on a different server using the
     * default InitialContext with the full corbaname (not including NameService).
     *
     * corbaname::localhost:<IIOPPort>#ejb/global/<App>/<Module>/<Bean>!<interface>
     */
    @Test
    public void testDefaultContextLookupWithEJBHome() throws Exception {
        // lookup the bean using default context with corbaname of EJBHome
        String jndiName = CorbaName + "#" + TxAttrCompBeanJndi + "!" + TxAttrEJBHome.class.getName();
        TxAttrEJBHome home = lookupRemoteBean(getDefaultContext(), jndiName, TxAttrEJBHome.class);
        assertNotNull("Remote home is null", home);

        // Create an instance of the bean
        TxAttrEJB bean = home.create();
        assertNotNull("Remote bean is null", bean);

        // verify the bean works
        boolean global = bean.txRequired();
        assertTrue("Container did not begin global transaction for TX REQUIRED", global);
    }

    /**
     * Test that a remote reference to an EJB may be passed as a parameter to a remote
     * EJB located on a different server.
     */
    @Test
    public void testPassingRemoteInterfaceParameter() throws Exception {
        // lookup the bean using default context with corbaname, with escape for period (.)
        String jndiName = CorbaNameNS + "#" + TestRemoteStatelessJndi + "!test%5c.TestRemoteInterface";
        TestRemoteInterface bean = lookupRemoteBean(getDefaultContext(), jndiName, TestRemoteInterface.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works; remote reference may be passed as parameter
        assertTrue("Remote reference not passed as parameter correctly", bean.verifyRemoteBean(bean));
    }

    /**
     * Test that method calls to a remote Singleton session bean on a different server
     * are routed to the same bean instance (state is preserved).
     */
    @Test
    public void testSingletonBeanState() throws Exception {
        // lookup the bean using default context with corbaname, with escape for period (.)
        String jndiName = CorbaNameNS + "#" + TestRemoteSingletonJndi + "!test%5c.TestRemoteInterface";
        TestRemoteInterface bean = lookupRemoteBean(getDefaultContext(), jndiName, TestRemoteInterface.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works; remote state is preserved
        assertEquals("Remote state is not correct", 8, bean.increment(8));
        assertEquals("Remote state is not correct", (8 + 27), bean.increment(27));
    }

    /**
     * Test that method calls to a remote Stateful session bean on a different server
     * are routed to the same bean instance (state is preserved).
     */
    @Test
    public void testStatefulBeanState() throws Exception {
        // lookup the bean using default context with corbaname, with escape for period (.)
        String jndiName = CorbaNameNS + "#" + TestRemoteStatefulJndi + "!test%5c.TestRemoteInterface";
        TestRemoteInterface bean = lookupRemoteBean(getDefaultContext(), jndiName, TestRemoteInterface.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works; remote state is preserved
        assertEquals("Remote state is not correct", 42, bean.increment(42));
        assertEquals("Remote state is not correct", (42 + 27), bean.increment(27));
    }

    /**
     * Test that method calls to a remote Stateless session bean on a different server
     * are routed to any bean instance (state is not preserved).
     */
    @Test
    public void testStatelessBeanState() throws Exception {
        // lookup the bean using default context with corbaname, with escape for period (.)
        String jndiName = CorbaNameNS + "#" + TestRemoteStatelessJndi + "!test%5c.TestRemoteInterface";
        TestRemoteInterface bean = lookupRemoteBean(getDefaultContext(), jndiName, TestRemoteInterface.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works; remote state is not preserved
        assertEquals("Remote state is not correct", 14, bean.increment(14));
        assertEquals("Remote state is not correct", 35, bean.increment(35));
    }

    /**
     * Test that asynchronous method calls to a remote Singleton session bean on a different
     * server are routed properly and the result may be retrieved.
     */
    @Test
    public void testSingletonAsyncMethod() throws Exception {
        // lookup the bean using default context with corbaname, with escape for period (.)
        String jndiName = CorbaNameNS + "#" + TestRemoteSingletonJndi + "!test%5c.TestRemoteInterface";
        TestRemoteInterface bean = lookupRemoteBean(getDefaultContext(), jndiName, TestRemoteInterface.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works; async result may be retrieved
        Future<String> nameFuture = bean.asynchMethodReturn();
        assertEquals("Asynchronus method value incorrect", TestRemoteSingleton, nameFuture.get(60, TimeUnit.SECONDS));
    }

    /**
     * Test that asynchronous method calls to a remote Stateful session bean on a different
     * server are routed properly and the result may be retrieved.
     */
    @Test
    public void testStatefulAsyncMethod() throws Exception {
        // lookup the bean using default context with corbaname, with escape for period (.)
        String jndiName = CorbaNameNS + "#" + TestRemoteStatefulJndi + "!test%5c.TestRemoteInterface";
        TestRemoteInterface bean = lookupRemoteBean(getDefaultContext(), jndiName, TestRemoteInterface.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works; async result may be retrieved
        Future<String> nameFuture = bean.asynchMethodReturn();
        assertEquals("Asynchronus method value incorrect", TestRemoteStateful, nameFuture.get(60, TimeUnit.SECONDS));
    }

    /**
     * Test that asynchronous method calls to a remote Stateless session bean on a different
     * server are routed properly and the result may be retrieved.
     */
    @Test
    public void testStatelessAsyncMethod() throws Exception {
        // lookup the bean using default context with corbaname, with escape for period (.)
        String jndiName = CorbaNameNS + "#" + TestRemoteStatelessJndi + "!test%5c.TestRemoteInterface";
        TestRemoteInterface bean = lookupRemoteBean(getDefaultContext(), jndiName, TestRemoteInterface.class);
        assertNotNull("Remote bean is null", bean);

        // verify the bean works; async result may be retrieved
        Future<String> nameFuture = bean.asynchMethodReturn();
        assertEquals("Asynchronus method value incorrect", TestRemoteStateless, nameFuture.get(60, TimeUnit.SECONDS));
    }

    /**
     * Test the Required transaction attribute for a remote EJB on a different server
     * when a transaction does not exist on the calling thread.
     *
     * While the thread is currently not associated with a transaction context, call a method
     * that has a transaction attribute of REQUIRED and verify the container began a global
     * transaction. Verify container completed global transaction prior to returning to caller
     * of method.
     */
    @Test
    public void testRequiredAttrib() throws Exception {
        TxAttrRemote bean = getTxAttrBean();
        assertNotNull("Remote bean is null", bean);

        boolean global = bean.txRequired();
        assertTrue("Container did not begin global transaction for TX REQUIRED", global);
        assertFalse("Container did not complete global transaction for TX REQUIRED", FATTransactionHelper.isTransactionGlobal());
    }

    /**
     * Test the Required transaction attribute for a remote EJB on a different server
     * when a transaction does exist on the calling thread.
     *
     * While thread is currently associated with a transaction context, call a remote method
     * that has a transaction attribute of REQUIRED and verify the container throws a
     * EJBTransactionRequiredException indicating the global transaction has not been
     * propagated to the remote server.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRequiredException" })
    public void testRequiredAttribInGlobalTrans() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        TxAttrRemote bean = getTxAttrBean();
        assertNotNull("Remote bean is null", bean);

        try {
            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            logger.info("user global transaction was started");

            // call TX REQUIRED method
            try {
                boolean global = bean.txRequired(tid);
                fail("Expected exception did not occur; Container used caller's global transaction for TX REQUIRED : " + global);
            } catch (EJBTransactionRequiredException ex) {
                logger.info("Expected exception occurred calling REQUIRED method : " + ex);
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX REQUIRED", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            logger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * Test the RequiresNew transaction attribute for a remote EJB on a different server
     * when a transaction does exist on the calling thread.
     *
     * While thread is currently associated with a transaction context, call a method that
     * has a transaction attribute of REQUIRES_NEW and verify the container begins a new global
     * transaction. Verify container completes global transaction prior to returning to caller
     * of method. Verify caller's global transaction is still active when container returns to
     * caller.
     */
    @Test
    public void testRequiresNewAttribIfClientTranExists() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        TxAttrRemote bean = getTxAttrBean();
        assertNotNull("Remote bean is null", bean);

        try {
            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            logger.info("user global transaction was started");

            // call TX REQUIRES NEW method
            boolean global = bean.txRequiresNew(tid);
            assertTrue("Container began new global transaction for TX REQUIRES NEW", global);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX REQUIRES NEW", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            logger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * Test the RequiresNew transaction attribute for a remote EJB on a different server
     * when a transaction does not exist on the calling thread.
     *
     * While thread is currently not associated with a transaction context, call a method that
     * has a transaction attribute of REQUIRES NEW and verify the container began a global
     * transaction. Verify container completed global transaction prior to returning to caller
     * of method.
     */
    @Test
    public void testRequiresNewAttribOnGlobalInt() throws Exception {
        TxAttrRemote bean = getTxAttrBean();
        assertNotNull("Remote bean is null", bean);

        boolean global = bean.txRequiresNew();
        assertTrue("Container did not begin global transaction for TX REQUIRES NEW", global);
        assertFalse("container did not complete global transaction for TX REQUIRES NEW", FATTransactionHelper.isTransactionGlobal());
    }

    /**
     * Test the Mandatory transaction attribute for a remote EJB on a different server
     * when a transaction does not exist on the calling thread.
     *
     * While thread is currently not associated with a transaction context, call a method that
     * has a transaction attribute of Mandatory and verify the container throws a
     * javax.ejb.EJBTransactionRequiredException.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRequiredException" })
    public void testMandatoryAttribThrowsExcp() throws Exception {
        TxAttrRemote bean = getTxAttrBean();
        assertNotNull("Remote bean is null", bean);

        try {
            bean.txMandatory();
            fail("The container did NOT throw the expected EJBTransactionRequiredException for TX Mandatory");
        } catch (EJBTransactionRequiredException ex) {
            logger.info("Container threw expected EJBTransactionRequiredException for TX Mandatory");
        }
    }

    /**
     * Test the Mandatory transaction attribute for a remote EJB on a different server
     * when a transaction does exist on the calling thread.
     *
     * While thread is currently associated with a transaction context, call a method that
     * has a transaction attribute of Mandatory and verify the container throws a
     * javax.ejb.EJBTransactionRequiredException.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRequiredException" })
    public void testMandatoryAttribInGlobalTrans() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        TxAttrRemote bean = getTxAttrBean();
        assertNotNull("Remote bean is null", bean);

        try {
            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            logger.info("user global transaction was started");

            // call TX Mandatory method
            try {
                boolean global = bean.txMandatory(tid);
                fail("Expected exception did not occur; Container used caller's global transaction for TX Mandatory : " + global);
            } catch (EJBTransactionRequiredException ex) {
                logger.info("Expected exception occurred calling MANDATORY method : " + ex);
            }

            // Verify global tran still active.
            assertTrue("Container completed caller's transaction for TX Mandatory", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            logger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * Test the Never transaction attribute for a remote EJB on a different server
     * when a transaction does not exist on the calling thread.
     *
     * While thread is currently not associated with a transaction context, call a method that
     * has a transaction attribute of Never and verify the container begins a local transaction.
     */
    @Test
    public void testNever() throws Exception {
        TxAttrRemote bean = getTxAttrBean();
        assertNotNull("Remote bean is null", bean);

        boolean local = bean.txNever();
        assertTrue("container did not begin a local transaction for TX Never", local);
    }

    /**
     * Test the Never transaction attribute for a remote EJB on a different server
     * when a transaction does exist on the calling thread.
     *
     * Used to verify when a method with a NEVER transaction attribute is called while
     * the thread is currently associated with a global transaction the container throws
     * a javax.ejb.EJBException.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSIException" })
    public void testNeverException() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        TxAttrRemote bean = getTxAttrBean();
        assertNotNull("Remote bean is null", bean);

        try {
            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            logger.info("user global transaction was started");

            // call TX NEVER method
            try {
                bean.txNever(tid);
                fail("Container did not throw a javax.ejb.EJBException as expected for TX Never when transaction already existed.");
            } catch (EJBException ex) {
                logger.info("container threw expected EJBException for TX Never when transaction already existed.");
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX NEVER", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            logger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * Test the NotSupported transaction attribute for a remote EJB on a different server
     * when a transaction does not exist on the calling thread.
     *
     * While thread is currently not associated with a transaction context, call a method that
     * has a transaction attribute of NotSupported and verify the container began a local
     * transaction.
     */
    @Test
    public void testNotSupported() throws Exception {
        TxAttrRemote bean = getTxAttrBean();
        assertNotNull("Remote bean is null", bean);

        boolean local = bean.txNotSupported();
        assertTrue("container did not begin a local transaction for TX NotSupported", local);
    }

    /**
     * Test the NotSupported transaction attribute for a remote EJB on a different server
     * when a transaction does exist on the calling thread.
     *
     * While thread is currently associated with a transaction context, call a method
     * that has a transaction attribute of NotSupported and verify the container began a
     * local transaction.
     */
    @Test
    public void testNotSupportedGlobalTransExists() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        TxAttrRemote bean = getTxAttrBean();
        assertNotNull("Remote bean is null", bean);

        try {
            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            logger.info("user global transaction was started");

            boolean local = bean.txNotSupported();
            assertTrue("container did not begin a local transaction for TX NotSupported", local);

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX NotSupported", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            logger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }

    /**
     * Test the Supports transaction attribute for a remote EJB on a different server
     * when a transaction does not exist on the calling thread.
     *
     * While thread is currently not associated with a transaction context, call a method that
     * has a transaction attribute of Supports and verify the container began a local transaction.
     */
    @Test
    public void testSupportsAttrib() throws Exception {
        TxAttrRemote bean = getTxAttrBean();
        assertNotNull("Remote bean is null", bean);

        boolean local = bean.txSupports();
        assertTrue("container did not begin a local transaction for TX Supports", local);
    }

    /**
     * Test the Supports transaction attribute for a remote EJB on a different server
     * when a transaction does exist on the calling thread.
     *
     * While thread is currently associated with a transaction context, call a method that
     * has a transaction attribute of Supports and verify the container executes in caller's
     * global transaction. Verify container does not complete the caller's global
     * transaction prior to returning to caller of method.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRequiredException" })
    public void testSupportsAttribOnGlobalTrans() throws Exception {
        byte[] tid = null;
        UserTransaction userTran = null;

        TxAttrRemote bean = getTxAttrBean();
        assertNotNull("Remote bean is null", bean);

        try {
            // Begin a global transaction
            userTran = FATHelper.lookupUserTransaction();
            userTran.begin();
            tid = FATTransactionHelper.getTransactionId();
            logger.info("user global transaction was started");

            // call TX Supports method
            try {
                boolean global = bean.txSupports(tid);
                fail("Expected exception did not occur; Container used caller's global transaction for TX Supports : " + global);
            } catch (EJBTransactionRequiredException ex) {
                logger.info("Expected exception occurred calling SUPPORTS method : " + ex);
            }

            // Verify global tran still active.
            assertTrue("container did not complete caller's transaction for TX Supports", FATTransactionHelper.isSameTransactionId(tid));
            userTran.commit();
            tid = null;
            logger.info("user global transaction committed");
        } finally {
            if (tid != null || (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)) {
                userTran.rollback();
            }
        }
    }
}