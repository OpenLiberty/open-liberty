/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.api.jms.impl;

import java.lang.reflect.Constructor;
import java.util.Vector;

import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.api.jms.ApiJmsConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.api.jms.ute.UTEHelperFactory;
import com.ibm.ws.sib.utils.ras.SibTr;

public class JmsErrorUtils {

    // ************************** TRACE INITIALISATION ***************************
    private static TraceComponent tc = SibTr.register(JmsErrorUtils.class, ApiJmsConstants.MSG_GROUP_EXT, ApiJmsConstants.MSG_BUNDLE_EXT);
    private static TraceNLS nls = TraceNLS.getTraceNLS(ApiJmsConstants.MSG_BUNDLE_EXT);

    /**
     * This variable determines whether the stack of a throwable object should be
     * filtered to remove all reference to this class. By default this should be
     * set to true. (see filterThrowable for more info).
     */
    private final static boolean filterStack = true;

    /**
     * This variable is used to hold the name of this class (ie "JmsErrorUtils").
     */
    private final static String className = JmsErrorUtils.class.getName();

    // *************************** INTERFACE METHODS *****************************

    /**
     * Used by the JMS API layer to create new <code>Throwable</code>'s, usually
     * <code>Exceptions</code> (both <code>JMSException</code>s and other Java
     * exception types). If a previous exception has not been caught, an
     * overloaded version of this method which does not specify the caught
     * exception may be called.
     * <p>
     * 
     * The method provides a single place where the error handling and creation
     * occurs, ensuring that these actions are always performed completely and
     * correctly.
     * <p>
     * 
     * The exact actions performed by this method depend on whether a previously
     * caught <code>Throwable</code> is supplied as a parameter to this method,
     * and whether a <code>JMSException</code> or other type of exception is
     * being constructed (note that <code>JMSException</code> subclasses behave
     * similarly to <code>JMSException</code>s).
     * <p>
     * 
     * The newly created <code>Throwable</code> is created with its reason value
     * set to the nls message corresponding to the message key passed to this
     * method. The message key must end in "SIAPnnnn" (where n's are digits), as
     * an error code may be derived from the key. This is done if the newly
     * created <code>Throwable</code> is a <code>JMSException</code>, in which
     * case it is created using the constructor that sets the error code.
     * <p>
     * 
     * Finally, if a trace component is supplied and is debug-enabled, the newly
     * created <code>Throwable</code> is traced.
     * <p>
     * 
     * If the probeId is non-null an FFDC will be generated. This will contain
     * the caughtThrowable if non-null, otherwise the newly generated exception
     * will be FFDC'd.
     * <p>
     * 
     * No errors are expected to occur in this method, but if exceptions are
     * thrown by methods invoked here, a <code>RuntimeException</code> is thrown.
     * <p>
     * 
     * @param throwableClass
     *            the type of <code>Throwable</code> to construct.
     * 
     * @param messageKey
     *            index into the message catalog (must end with "SIAPnnnn").
     * 
     * @param messageInserts
     *            inserts for the message.
     * 
     * @param caughtThrowable
     *            non-null if the new <code>Throwable</code> is being constructed
     *            from within a <code>catch</code> block.
     * 
     * @param probeId
     *            uniquely identifies the code which caught/generated caughtThrowable
     *            (must have the format "className.methodName#n").
     *            If probeId == null, no FFDC is generated.
     *            New - if probeId is non-null an FFDC will be generated, even if caughtThrowable is null
     * 
     * @param caller
     *            non-null if non-static code caught <code>caughtThrowable</code>.
     * 
     * @param traceComponent
     *            if this is set to a non-null value, and is debug-enabled, the newly
     *            created <code>Throwable</code> will be traced using this component.
     */
    static Throwable newThrowable(Class throwableClass
                                  , String messageKey
                                  , Object[] messageInserts
                                  , Throwable caughtThrowable
                                  , String probeId
                                  , Object caller
                                  , TraceComponent traceComponent
                    ) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "newThrowable",
                        new Object[] { throwableClass
                                      , messageKey
                                      , caughtThrowable
                                      , probeId
                                      , caller, });
        Throwable t = null;

        try {

            // If test environment is enabled then take this opportunity to check
            // the the message exists in the file. Note that this is not called in
            // the mainline product codepath.
            if (UTEHelperFactory.jmsTestEnvironmentEnabled) {
                String defaultIfMissing = "XXX_MISSING_XXX";
                String msg = nls.getFormattedMessage(messageKey, messageInserts, defaultIfMissing);

                if (defaultIfMissing.equals(msg)) {
                    // NB. NLS translation is not required since this only occurs in the test environment.
                    throw new IllegalArgumentException("The message key " + messageKey + " does not exist.");
                }

                // Now check that the correct number of inserts have been provided.
                // This call gets the number of inserts in the message.
                int numInserts = getNumberOfInserts(messageKey);
                int gotInserts = 0;
                if (messageInserts != null) {
                    gotInserts = messageInserts.length;
                }
                if (numInserts != gotInserts) {
                    //NB. NLS translation is not required since this only occurs in the test environment.
                    throw new IllegalArgumentException("Expected " + numInserts + " for " + messageKey + " but got " + gotInserts);
                }
            }

            // If an invalid throwable class was supplied throw a runtime exception.
            if (!Throwable.class.isAssignableFrom(throwableClass)) {
                RuntimeException re = new RuntimeException("JmsErrorUtils.newThrowable#1");
                FFDCFilter.processException(re, "JmsErrorUtils.newThrowable", "JmsErrorUtils.newThrowable#1");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "exception : ", re);
                throw re;
            }

            // First note if the throwable to be constructed is a JMSException
            // or subclass of JMSException.
            boolean wantJMSExceptionOrSubclass = JMSException.class.isAssignableFrom(throwableClass);

            // If a JMSException constructor is required, get the two String arg
            // constructor, else get a one String arg constructor.
            Constructor throwableConstructor = null;
            try {
                if (wantJMSExceptionOrSubclass) {
                    throwableConstructor = throwableClass.getConstructor(new Class[] { String.class, String.class });
                }
                else {
                    throwableConstructor = throwableClass.getConstructor(new Class[] { String.class });
                }
            } catch (Exception e) {
                // No FFDC code needed
                RuntimeException re = new RuntimeException("JmsErrorUtils.newThrowable#2");
                re.initCause(e);
                FFDCFilter.processException(re, "JmsErrorUtils.newThrowable", "JmsErrorUtils.newThrowable#2");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "exception : ", re);
                throw re;
            }

            // Get the nls text for the message. 'messageInserts' may be null.
            String reason = nls.getFormattedMessage(messageKey, messageInserts, null);

            // Get the error code for the throwable, 'messageKey' is assumed to have
            // the form "...CWSIAxxxx".
            String errorCode = null;
            if (messageKey.length() >= 9) {
                errorCode = messageKey.substring(messageKey.length() - 9);
            }

            // Construct the throwable.
            try {
                if (wantJMSExceptionOrSubclass) {
                    t = (Throwable) throwableConstructor.newInstance(new Object[] { reason, errorCode });
                }
                else {
                    t = (Throwable) throwableConstructor.newInstance(new Object[] { reason });
                }
            } catch (Exception e) {
                // No FFDC code needed
                RuntimeException re = new RuntimeException("JmsErrorUtils.newThrowable#3");
                re.initCause(e);
                FFDCFilter.processException(re, "JmsErrorUtils.newThrowable", "JmsErrorUtils.newThrowable#3");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "exception : ", re);
                throw re;
            }

            // If 'caughtThrowable' refers to an exception, and if a JMSException is
            // being constructed, set the linked exception in the new JMSException to
            // refer to the previously caught exception.
            if ((caughtThrowable instanceof Exception) && wantJMSExceptionOrSubclass) {
                Exception caughtException = (Exception) caughtThrowable;
                ((JMSException) t).setLinkedException(caughtException);
            }

            // If 'caughtThrowable' is non-null, set the cause in the Throwable
            // being constructed.
            if (caughtThrowable != null) {
                t.initCause(caughtThrowable);
            }

            // This method removes all trace of the newThrowable method and
            // associated reflection calls.
            if (filterStack) {
                filterThrowable(t);
            }

            // If a trace component was supplied and is debug enabled, trace
            // the newly constructed throwable.
            if ((traceComponent != null) && TraceComponent.isAnyTracingEnabled() && (traceComponent.isDebugEnabled())) {
                SibTr.debug(traceComponent, "throwable : ", t);
            }

            // If probeId is non-null, generate an FFDC
            if (probeId != null) {
                // 'probeId' is assumed to
                // have the form "className.methodName#...", usually with a number after
                // the '#'. If the invoking method is static, 'caller' will be null.
                // d238447 FFDC review. A null probeId is used to indicate that FFDC is not required.
                String sourceId = "";

                int index = probeId.indexOf('#');
                if (index == -1) {
                    // no seperator, so we can't make any assumptions about the format
                    sourceId = probeId;
                }
                else {
                    sourceId = probeId.substring(0, index);
                }

                // If caughtThrowable is non-null we FFDC that. Otherwise we FFDC the newly generated exception.
                Throwable foo;
                if (caughtThrowable != null) {
                    foo = caughtThrowable;
                }
                else {
                    foo = t;
                }

                if (caller != null) {
                    FFDCFilter.processException(foo, sourceId, probeId, caller);
                }
                else {
                    FFDCFilter.processException(foo, sourceId, probeId);
                }
            }

            // Return the newly created exception.

        } catch (RuntimeException re) {
            // No FFDC code needed
            // d238447 FFDC review. This is an internal error, we should FFDC the RE.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "caught : ", re);
            FFDCFilter.processException(re, "JmsErrorUtils.newThrowable", "newThrowable#4");
            throw re;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "newThrowable", t);
        return t;
    }

    /**
     * Obtains the number of inserts in a given message by looking
     * for increasing instances of {i} for i=0,1,2,...,20
     * 
     */
    private static int getNumberOfInserts(String messageKey) {

        String unInsertedMessage = nls.getString(messageKey);
        int numInserts = 0;
        // Not much point in going any further than 20 inserts!
        for (int i = 0; i < 20; i++) {
            if (unInsertedMessage.indexOf("{" + i + "}") != -1) {
                numInserts++;
            }
            else {
                // This message insert was not found
                break;
            }
        }

        return numInserts;
    }

    /**
     * Used by the JMS API layer to create new <code>Throwable</code>'s, usually
     * <code>Exceptions</code> (both <code>JMSException</code>s and other Java
     * exception types). If a previous exception has been caught, an overloaded
     * version of this method which specifies the caught exception may be called.
     * <p>
     * 
     * The method provides a single place where the error handling and creation
     * occurs, ensuring that these actions are always performed completely and
     * correctly.
     * <p>
     * 
     * The new <code>Throwable</code> is created with its reason value set to the
     * nls message corresponding to the message key passed to this method. The
     * message key must end in "SIAPnnnn" (where n's are digits), as an error
     * code may be derived from the key. This is done if the newly created
     * <code>Throwable</code> is a <code>JMSException</code>, in which case it is
     * created using the constructor that sets the error code.
     * <p>
     * 
     * If a trace component is supplied and is debug-enabled, the newly created
     * <code>Throwable</code> is traced.
     * <p>
     * 
     * No errors are expected to occur in this method, but if exceptions are
     * thrown by methods invoked here, a <code>RuntimeException</code> is thrown.
     * <p>
     * 
     * @param throwableClass
     *            the type of <code>Throwable</code> to construct.
     * 
     * @param messageKey
     *            index into the message catalog (must end with "SIAPnnnn").
     * 
     * @param messageInserts
     *            inserts for the message.
     * 
     * @param traceComponent
     *            if this is set to a non-null value, and is debug-enabled, the newly
     *            created <code>Throwable</code> will be traced using this component.
     */
    static Throwable newThrowable(Class throwableClass
                                  , String messageKey
                                  , Object[] messageInserts
                                  , TraceComponent traceComponent
                    ) {
        return newThrowable(throwableClass
                            , messageKey
                            , messageInserts
                            , null
                            , null
                            , null
                            , traceComponent);
    }

    /**
     * Investigates the current call stack when the method is called to return
     * a string describing the line of customer application code that was responsible
     * for calling into the JMS component package (...sib.api.jms.impl)
     * 
     * This method is used for providing assistance to the user in tracking down
     * badly behaved code. Note that this method should only be used in error cases
     * due to the performance implications of creating a new exception each time it
     * is called.
     * 
     * @return String describing the relevant line of application code.
     */
    protected static String getFirstApplicationStackString() {
        // Don't do trace in this method as it would only confuse the issue
        // to see a full stack trace.
        String val = null;

        Exception e = new Exception();
        StackTraceElement[] ste = e.getStackTrace();

        String jmsImplPackageName = JmsErrorUtils.class.getPackage().getName();

        // The elements of the stack start at 0 with an entry for this method, and
        // get closer to the application as the indices increase. We are looking for
        // the first entry that isn't in the jms.impl package.

        // Start at index 1, because sometimes the Throwable constructor gets into
        // the stack trace too. We know that there is at least two lines in the stack
        // trace because this method has been called by someone else in the JMS component.
        for (int i = 1; i < ste.length; i++) {

            // Class name includes the package name.
            String recordClassName = ste[i].getClassName();
            if (!recordClassName.startsWith(jmsImplPackageName)) {
                // We have found the first non-JMS entry.
                val = ste[i].toString();
                // Leave the loop immediately.
                break;
            }
        }

        return val;
    }

    // ************************* IMPLEMENTATION METHODS **************************

    /**
     * This method filters the stack trace of the parameter object to remove all
     * trace of the newThrowable and reflection calls that have been made.
     * 
     * @param t
     */
    private static void filterThrowable(Throwable t) {

        // Now we want to remove the superfluous lines of stack trace from the
        // exception (those that have been inserted as a result of the way we
        // created the exception.
        StackTraceElement[] stackLines = t.getStackTrace();

        // If there is definitely something to work with.
        if ((stackLines != null) && (stackLines.length > 1)) {
            Vector v = new Vector();

            // Look at each stack line in turn until we reach the JmsErrorUtils
            // class stuff, then copy them over starting with the first line after
            // JmsErrorUtils finishes.
            boolean euStarted = false;

            // First read up to the point where the JmsErrorUtils entries start.
            int index = 0;
            for (index = 0; index < stackLines.length; index++) {
                StackTraceElement ste = stackLines[index];

                // See if this is the errorUtils
                if (className.equals(ste.getClassName())) {
                    // We have found an error utils line
                    euStarted = true;
                }
                else {
                    if (euStarted) {
                        // This indicates that we have now left the ErrorUtils lines,
                        // and everything left in the stack should be copied across.
                        v.add(ste);
                    }
                }
            }

            // Now we take the filtered stack trace and set it back into the
            // exception we originally got it from.
            if (v.size() != 0) {
                StackTraceElement[] filteredSTE = new StackTraceElement[v.size()];
                for (int i = 0; i < filteredSTE.length; i++) {
                    filteredSTE[i] = (StackTraceElement) v.elementAt(i);
                }
                t.setStackTrace(filteredSTE);
            }
        }
    }

    /**
     * This is a generic function to convert all the JMS 1.1 exceptions to JMS 2.0 exception.
     * It uses reflection to create the desired JMS2.0 exception
     * 
     * @param jmse The JMSException
     * @param exceptionToBeThrown The exception which has to be returned
     * @return Throwable It can be of type IllegalStateRuntimeException, InvalidClientIDRuntimeException, InvalidDestinationRuntimeException, InvalidSelectorRuntimeException,
     *         JMSSecurityRuntimeException, MessageFormatRuntimeException, MessageNotWriteableRuntimeException, ResourceAllocationRuntimeException,
     *         TransactionInProgressRuntimeException, TransactionRolledBackRuntimeException
     */
    public static Throwable getJMS2Exception(JMSException jmse, Class exceptionToBeThrown) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getJMS2Exceptions",
                        new Object[] { jmse, exceptionToBeThrown });

        JMSRuntimeException jmsre = null;
        try {
            jmsre = (JMSRuntimeException) exceptionToBeThrown.getConstructor(new Class[] { String.class, String.class, Throwable.class }).
                            newInstance(new Object[] { jmse.getMessage(), jmse.getErrorCode(), jmse.getLinkedException() });
        } catch (Exception e) {

            RuntimeException re = new RuntimeException("JmsErrorUtils.newThrowable#5", e);
            re.initCause(jmse);
            FFDCFilter.processException(re, "JmsErrorUtils.newThrowable", "JmsErrorUtils.newThrowable#5");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "exception : ", re);
            throw re;
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getJMS2Exceptions", jmsre);
        }

        return jmsre;

    }

}
