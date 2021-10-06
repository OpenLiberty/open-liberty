/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.ejbcontainer.test.tools;

import static java.util.logging.Level.WARNING;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import junit.framework.AssertionFailedError;

/**
 * FATHelper class provides helper methods that do not use
 * or require websphere application server classes.
 */
public abstract class FATHelper {
    private final static String CLASS_NAME = FATHelper.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    /**
     * The accuracy of the time returned by System.currentTimeMillis/nanoTime on
     * some platforms (e.g., Windows) is lower than the accuracy used for
     * Thread.sleep/Object.wait/etc. See
     * <tt>http://blogs.sun.com/dholmes/entry/inside_the_hotspot_vm_clocks</tt>.
     * The website above suggests that 15ms is a good upper bound for the number
     * of milliseconds that a single reading can be off, and empirical evidence
     * supports this.
     */
    public static final long TIME_FUDGE_FACTOR = 15;

    /**
     * When comparing two time values, the start and end times can both be off
     * by up to {@link #TIME_FUDGE_FACTOR}, so this value should be used to
     * adjust the subtraction of two times.
     */
    public static final long DURATION_FUDGE_FACTOR = TIME_FUDGE_FACTOR * 2;

    /**
     * The minimum reliably noticeable time delay for the systems that typically
     * run the FAT bucket. This value has been found empirically.
     */
    public static final long MINIMUM_DELAY_TIME = 400;

    // The time for the container to run post invoke processing for @Timeout.
    // Should be used after a Timer has triggered a CountDownLatch to insure
    // the @Timeout method, including the transaction, has completed and thus
    // updated (or even removed) the timer.
    public static final long POST_INVOKE_DELAY = 200;

    /**
     * A more accurate version of Thread.sleep() that utilizes System.nanoTime().
     *
     * Thread.sleep() is repeated until System.nanoTime() shows that the thread
     * has waited at least the specified time in milliseconds.
     *
     * @param time time to sleep in milliseconds
     */
    public static void sleep(long time) {
        long remainingTime;
        long startTime = System.nanoTime();
        while ((remainingTime = time - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)) > 0) {
            svLogger.info("Sleeping for " + remainingTime + "ms (of total = " + time + "ms)");
            try {
                Thread.sleep(remainingTime);
            } catch (InterruptedException e) {
                svLogger.info("Unexpected exception during Thread.sleep() : " + e);
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * Checks the passed-in throwable to see if it is or if it wrappers an
     * AssertionFailedError (JUnit's means of determining whether a test case
     * failed). If an AssertionFailedError exists in the cause chain, then it
     * will be thrown. If not, this method will throw a RuntimeException that
     * wraps the original.
     *
     * This is the same as calling <code>checkForAssertion(throwable, true)</code>.
     *
     * @param throwable - source java.lang.Throwable which may be or contain an
     *            AssertionFailedError
     * @throws AssertionFailedError - only if throwable is or wrappers an
     *             AssertionFailedError
     */
    public static void checkForAssertion(Throwable throwable) throws AssertionFailedError {
        checkForAssertion(throwable, true); //d584580 default to true
    }

    /**
     * Checks the passed-in throwable to see if it is or if it wrappers an
     * AssertionFailedError (JUnit's means of determining whether a test case
     * failed). If an AssertionFailedError exists in the cause chain, then it
     * will be thrown.
     * <br/>
     * If not, this method will either return or will throw an exception based
     * on the value of throwIfNotAssertFailed. If true, this method will
     * attempt to throw the throwable itself, but only if it is an instance of
     * a java.lang.RuntimeException or a java.lang.Error. If it is not an
     * instance of either of these, a new RuntimeException will be created
     * which wraps throwable. If throwIfNotAssertFailed is false, then this
     * method will return without throwing any exceptions.
     *
     * @param throwable - source java.lang.Throwable which may be or contain an
     *            AssertionFailedError
     * @param throwIfNotAssertFailed - determines whether to throw an Error or
     *            RuntimeException if throwable is not (or does not contain) an instance
     *            of AssertionFailedError
     * @throws AssertionFailedError - only if throwable is or wrappers an
     *             AssertionFailedError
     */
    public static void checkForAssertion(Throwable throwable, boolean throwIfNotAssertFailed) throws AssertionFailedError {

        if (throwable == null) {
            throw new IllegalArgumentException("throwable cannot be null");
        }
        Throwable t = throwable, prevT = null;

        //check for AssertionFailedError:
        while (t != null && t != prevT) {
            if (t instanceof AssertionFailedError) {
                throw (AssertionFailedError) t;
            }
            prevT = t;
            t = t.getCause();
        }

        //determine what should throwable should be thrown
        if (throwIfNotAssertFailed) {
            svLogger.logp(WARNING, CLASS_NAME, "checkForAssertion", "Non-failure exception: ", throwable);
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException) throwable;
            } else if (throwable instanceof Error) {
                throw (Error) throwable;
            } else {
                //we must wrap the throwable in a RuntimeException since we
                // do not have a114125ny throwable types declared on this method's
                // throws clause.
                RuntimeException wrappedEx = new RuntimeException("checkForAssertion", throwable);
                throw wrappedEx;
            }
        }
    }

    protected static Context getContext() throws NamingException {
        return new InitialContext();
    }

    private static Context getLocalContext() throws NamingException {
        return (Context) new InitialContext().lookup("local:ejb");
    }

    private static Context getEJBLocalContext() throws NamingException {
        return (Context) new InitialContext().lookup("ejblocal:");
    }

    /**
     * Lookup in the EJB 3 local namespace (e.g. ejblocal: namespace) a specified
     * EJB 3 local interface name (either home or business interface) for a specified
     * J2EEName of a EJB 3 bean using the default binding generated by EJB container.
     * This method is useful when no xml file is provided for bindings of interface
     * to JNDI names (e.g. when module contains no ejb-jar.xml file).
     *
     * @param interfaceName
     *            the fully qualified name of the local home or business interface
     *            name ( e.g. suite.r70.base.ejb3.sla.BasicCMTStatelessLocal ).
     * @param application
     *            the application name of installed application (e.g EJB3SLABeanApp).
     * @param module
     *            the module name that contains the bean (e.g EJB3SLABean.jar).
     * @param bean
     *            the EJB 3 bean name in specified application and module (e.g. AdvBasicCMTStatelessLocal).
     *
     * @return the object returned by the name lookup in ejblocal: namespace.
     *
     * @throws NamingException
     */
    public static Object lookupDefaultBindingEJBLocalInterface(String interfaceName, String application, String module, String bean) throws NamingException {
        String jndiName = application + "/" + module + "/" + bean + "#" + interfaceName;
        return FATHelper.lookupLocalBinding(jndiName);
    }

    /**
     * Lookup in the EJB java: global namespace
     *
     * @param interfaceName
     *            the fully qualified name of the local home or business interface
     *            name ( e.g. suite.r70.base.ejb3.sla.BasicCMTStatelessLocal ).
     * @param application
     *            the application name of installed application (e.g EJB3SLABeanApp).
     * @param module
     *            the module name that contains the bean (e.g EJB3SLABean.jar).
     * @param bean
     *            the EJB 3 bean name in specified application and module (e.g. AdvBasicCMTStatelessLocal).
     *
     * @return the object returned by the name lookup in ejblocal: namespace.
     *
     * @throws NamingException
     */
    public static Object lookupDefaultBindingEJBJavaGlobal(String interfaceName, String application, String module, String bean) throws NamingException {
        String jndiName = "java:global/" + application + "/" + module + "/" + bean + "!" + interfaceName;
        return FATHelper.lookupJavaBinding(jndiName);
    }

    /**
     * Lookup in the EJB java:app namespace
     *
     * @param interfaceName
     *            the fully qualified name of the local home or business interface
     *            name ( e.g. suite.r70.base.ejb3.sla.BasicCMTStatelessLocal ).
     * @param module
     *            the module name that contains the bean (e.g EJB3SLABean.jar).
     * @param bean
     *            the EJB 3 bean name in specified application and module (e.g. AdvBasicCMTStatelessLocal).
     *
     * @return the object returned by the name lookup in ejblocal: namespace.
     *
     * @throws NamingException
     */
    public static Object lookupDefaultBindingEJBJavaApp(String interfaceName, String module, String bean) throws NamingException {
        String jndiName = "java:app/" + module + "/" + bean + "!" + interfaceName;
        return FATHelper.lookupJavaBinding(jndiName);
    }

    /**
     * Returns the local home or business interface from JNDI lookup to "jndiName".
     * This method will prefix local lookups with ejblocal:
     *
     * @param jndiName home JNDI name to lookup.
     * @return Local interface object found.
     */
    public static Object lookupLocalBinding(String jndiName) throws NamingException {
        svLogger.info("lookupLocalBinding : jndi name = ejblocal:" + jndiName);
        Object o = getEJBLocalContext().lookup(jndiName);
        svLogger.info("lookupLocalBinding : returning : " + ((o == null) ? o : o.getClass().getName()));
        return o;
    }

    /**
     * Returns the local home or business interface from JNDI lookup to "jndiName".
     *
     * @param jndiName home JNDI name to lookup.
     * @return Local interface object found.
     */
    public static Object lookupJavaBinding(String jndiName) throws NamingException {
        svLogger.info("lookupJavaBinding : jndi name = " + jndiName);
        Object o = getContext().lookup(jndiName);
        svLogger.info("lookupJavaBinding : returning : " + ((o == null) ? o : o.getClass().getName()));
        return o;
    }

    /**
     * Returns the local home interface from JNDI lookup to "jndiName".
     * This method will prefix local lookups with local:ejb
     *
     * @param jndiName home JNDI name to lookup.
     * @return Local interface object found.
     */
    public static Object lookupLocalHome(String jndiName) throws NamingException {
        svLogger.info("lookupLocalHome : jndi name = local:ejb/" + jndiName);
        Object o = getLocalContext().lookup(jndiName);
        svLogger.info("lookupLocalBinding : returning : " + ((o == null) ? o : o.getClass().getName()));
        return o;
    }

    /**
     * Returns the remote business interface for EJB3 bindings from JNDI lookup.
     *
     * @param interfaceName
     *            the fully qualified name of the local home or business interface
     *            name ( e.g. suite.r70.base.ejb3.sla.BasicCMTStatelessLocal ).
     * @param application
     *            the application name of installed application (e.g EJB3SLABeanApp).
     * @param module
     *            the module name that contains the bean (e.g EJB3SLABean.jar).
     * @param bean
     *            the EJB 3 bean name in specified application and module (e.g. AdvBasicCMTStatelessLocal).
     *
     * @return Remote Business Interface object found.
     *
     * @throws NamingException
     */
    public static Object lookupDefaultEJBLegacyBindingsEJBRemoteInterface(String interfaceName, String application, String module, String bean) throws NamingException {
        Class<?> interfaceClass;
        String jndiName = "ejb/" + application + "/" + module + "/" + bean + "#" + interfaceName;
        try {
            // must narrow to the remote business interface before returning.
            interfaceClass = Thread.currentThread().getContextClassLoader().loadClass(interfaceName);
        } catch (ClassNotFoundException e) {
            throw new NamingException(e.getMessage());
        }

        return lookupRemoteBinding(jndiName, interfaceClass);
    }

    /**
     * Returns the remote business interface from JNDI lookup.
     *
     * @param interfaceName
     *            the fully qualified name of the local home or business interface
     *            name ( e.g. suite.r70.base.ejb3.sla.BasicCMTStatelessLocal ).
     * @param application
     *            the application name of installed application (e.g EJB3SLABeanApp).
     * @param module
     *            the module name that contains the bean, without extension (e.g EJB3SLABean).
     * @param bean
     *            the EJB 3 bean name in specified application and module (e.g. AdvBasicCMTStatelessLocal).
     *
     * @return Remote Business Interface object found.
     *
     * @throws NamingException
     */
    public static Object lookupDefaultBindingsEJBRemoteInterface(String interfaceName, String application, String module, String bean) throws NamingException {
        Class<?> interfaceClass;
        String jndiName = "java:global/" + application + "/" + module + "/" + bean + "!" + interfaceName;

        try {
            // must narrow to the remote business interface before returning.
            interfaceClass = Thread.currentThread().getContextClassLoader().loadClass(interfaceName);
        } catch (ClassNotFoundException e) {
            throw new NamingException(e.getMessage());
        }

        return lookupRemoteBinding(jndiName, interfaceClass);
    }

    /**
     * Returns the remote home or business interface from JNDI lookup to "jndiName".
     * This method is intended for "short name" default bindings. For example:
     * <tt> suite.r70.base.ejb3.defbnd.RemoteBusiness</tt>
     *
     * @param jndiName home JNDI name to lookup.
     * @return Remote interface object found.
     */
    public static Object lookupRemoteShortBinding(String jndiName) throws NamingException {
        // The remote short default is unique in that the JNDI name is also the
        // remote interface class name.  Load the class, then perform the lookup.
        Class<?> interfaceClass;
        try {
            // must narrow to the remote business interface before returning
            interfaceClass = Thread.currentThread().getContextClassLoader().loadClass(jndiName);
        } catch (ClassNotFoundException ex) {
            throw new NamingException(ex.getMessage());
        }

        return lookupRemoteBinding(jndiName, interfaceClass);
    }

    /**
     * Returns the remote home or business interface from JNDI lookup
     * of "jndiName". This method is intended for non default remote bindings.
     *
     * @param jndiName JNDI name to lookup.
     * @return Remote interface object found.
     */ // F896-23013
    public static <T> T lookupRemoteBinding(String jndiName,
                                            Class<T> interfaceClass) throws NamingException {
        svLogger.info("Lookup Remote JNDI : " + jndiName +
                      ", interface = " + interfaceClass.getName());

        Object o = getContext().lookup(jndiName);

        svLogger.info("Found : " + ((o == null) ? "null" : o.getClass().getName()));

        return interfaceClass.cast(PortableRemoteObject.narrow(o, interfaceClass));
    }

    /**
     * Returns the remote home from JNDI lookup
     * of "jndiName". This method is intended for non default remote bindings.
     *
     * DOES NOT PRO.NARROW()
     *
     * @param jndiName JNDI name to lookup.
     * @return Remote interface object found.
     */
    public static <T> T lookupRemoteHomeBinding(String jndiName,
                                                Class<T> interfaceClass) throws NamingException {
        svLogger.info("Lookup Remote JNDI : " + jndiName +
                      ", interface = " + interfaceClass.getName());

        Object o = getContext().lookup(jndiName);

        svLogger.info("Found : " + ((o == null) ? "null" : o.getClass().getName()));

        return interfaceClass.cast(o);
    }

    public static UserTransaction lookupUserTransaction() throws NamingException {
        // For J2EE 1.3 and later jar files the UserTransaction cannot be looked
        // up in the global name space from within the server process, only the
        // java:comp name space.
        String userTranLookupString = isServer() ? "java:comp/UserTransaction" : "jta/usertransaction";
        svLogger.info("jndi lookup: " + userTranLookupString);
        Object o = getContext().lookup(userTranLookupString); // d157927
        svLogger.info("UserTransaction class :" + o.getClass().getName());

        return ((UserTransaction) o);
    }

    /**
     * UserTransaction cleanup method for use in finally blocks. <p>
     *
     * If a test scenario fails, leaving a UserTransaction in an active state,
     * this method may be used to rolled back the UserTransaction. <p>
     *
     * @param userTran the UserTransaction that may require cleanup; will be
     *            rolled back if still active. Null ignored.
     */
    public static void cleanupUserTransaction(UserTransaction userTran) {
        if (userTran == null) {
            return;
        }

        try {
            int status = userTran.getStatus();
            if (status != Status.STATUS_NO_TRANSACTION && status != Status.STATUS_COMMITTED) {
                svLogger.info("---> Rolling back user transaction");
                userTran.rollback();
            }
        } catch (Throwable ex) {
            svLogger.info("---> Failed to rollback an active UserTransaction : " + ex.getClass().getName());
            ex.printStackTrace(System.out);
        }
    }

    /**
     * Returns true when running in a WebSphere server process.
     */
    public static boolean isServer() {
        String testLauncher = System.getProperty("testLauncher");
        return testLauncher == null;
    }

    /**
     * Fail method to be used to wrap an error inside an assertion failure.
     *
     * @param th Throwable to be wrapped inside the Assertion Failure.
     * @param messString message for Assertion Failure.
     */
    public static void fail(Throwable th, String messString) {
        AssertionFailedError error = new AssertionFailedError(messString);
        error.initCause(th);
        throw error;
    }

    /**
     * Returns the remote business interface from JNDI lookup.
     *
     * @param interfaceClass
     *            the remote interface class.
     * @param application
     *            the application name of installed application (e.g EJB3SLABeanApp).
     * @param module
     *            the module name that contains the bean, without extension (e.g EJB3SLABean).
     * @param bean
     *            the EJB 3 bean name in specified application and module (e.g. AdvBasicCMTStatelessLocal).
     *
     * @return Remote Business Interface object found.
     *
     * @throws NamingException
     */
    public static Object lookupDefaultBindingsEJBRemoteInterface(Class<?> interfaceClass, String application, String module, String bean) throws NamingException {
        String jndiName = "java:global/" + application + "/" + module + "/" + bean + "!" + interfaceClass.getName();
        return lookupRemoteBinding(jndiName, interfaceClass);
    }

    public static final boolean isZOS() {
        String osName = System.getProperty("os.name");
        if (osName.contains("OS/390") || osName.contains("z/OS") || osName.contains("zOS")) {
            return true;
        }
        return false;
    }
}
