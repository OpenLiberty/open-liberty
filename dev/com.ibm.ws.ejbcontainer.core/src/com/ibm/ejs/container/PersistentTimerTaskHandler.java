/*******************************************************************************
 * Copyright (c) 2003, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Date;

import javax.ejb.EJBException;

import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.ejs.util.Util;
import com.ibm.ejs.util.dopriv.GetContextClassLoaderPrivileged;
import com.ibm.websphere.csi.J2EEName;
import com.ibm.websphere.csi.TransactionAttribute;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejb.portable.Constants;
import com.ibm.ws.ejbcontainer.runtime.AbstractEJBRuntime;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntime;
import com.ibm.ws.ejbcontainer.util.ParsedScheduleExpression;
import com.ibm.ws.ejbcontainer.util.ScheduleExpressionParser;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Provides a mechanism to persist EJB timer specific data, including the
 * information to calculate the next fire time (trigger), and perform the
 * work associated with the corresponding scheduled task / EJB Timer. <p>
 *
 * When a task / EJB timer expires, this object will execute the
 * timeout method on the corresponding timer bean. <p>
 *
 * Any data required to execute the timeout method associated with
 * an EJB timer must be part of the serializable state of this object. <p>
 *
 * Note: PersistentExecutor has been optimized to detect when the same object
 * implements both Runnable and Trigger and only persist the object once.
 **/
public abstract class PersistentTimerTaskHandler implements Runnable, Serializable {

    private static final TraceComponent tc = Tr.register(PersistentTimerTaskHandler.class, "EJBContainer", "com.ibm.ejs.container.container");
    private static final String CLASS_NAME = PersistentTimerTaskHandler.class.getName();

    private static final long serialVersionUID = -8200752857441853748L;

    // Although this class is not required to interoperate with other
    // application servers, the serialized version will be persisted, and thus
    // the implementation may need to be portable across the different
    // WebSphere platforms, and the ability to version the implementation is
    // required. The desire is that the container should own the process of
    // marshaling and demarshalling these classes. Therefore, the following
    // buffer contents have been agreed upon between AE WebSphere container
    // and WebSphere390 container:
    //
    // |------- Header Information -----------||-------- Object Contents --------|
    // [ eyecatcher ][ platform ][ version id ]
    //     byte[4]       short       short               instance fields
    //
    // This class overrides the default implementation of the Serializable
    // methods 'writeObject' and 'readObject'. The implementations of these
    // methods read and write the buffer contents as mapped above.

    // header information for interoperability and serialization
    private static final byte[] EYECATCHER = Constants.TIMER_TASK_EYE_CATCHER;
    private static final short PLATFORM = Constants.PLATFORM_DISTRIBUTED;

    /**
     * Uniquely identifies the EJB.
     */
    protected transient J2EEName j2eeName;

    /**
     * The timeout callback method ID. This field can only be set to a non-zero
     * value for automatic timers.
     *
     * @see BeanMetaData.timedMethodInfos
     */
    protected transient int methodId;

    /**
     * The method name corresponding to methodId. This field is only set for
     * automatic timers, and it is only used to detect incompatible changes to
     * the application, or for findEJBTimers command output.
     */
    private transient String automaticMethodName;

    /**
     * The class name corresponding to methodId. This field is only set for
     * automatic timers specified via annotations, and it is only used to detect
     * incompatible changes to the application.
     */
    private transient String automaticClassName;

    /**
     * Serialized form of user provided info object. Since a copy must be returned
     * each time it is requested, just the serialized bytes are stored and then
     * deserialized each time it is accessed, performing better than a full copy.
     */
    private transient byte[] userInfoBytes;

    /**
     * Initial expiration for single action and interval timers.
     */
    protected transient long expiration;

    /**
     * Interval in milliseconds for interval timers.
     */
    protected transient long interval;

    /**
     * ScheduleExpression for calendar-based timers.
     */
    protected transient ParsedScheduleExpression parsedSchedule;

    /**
     * Common constructor for all persistent timer types.
     *
     * @param j2eeName identity of the timer bean that is the target of the associated task.
     * @param info     the user data associated with this timer
     *
     * @throws IOException if the serializable user object cannot be serialized.
     **/
    private PersistentTimerTaskHandler(J2EEName j2eeName, Serializable info) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>: " + j2eeName + ", " + Util.identity(info));

        this.j2eeName = j2eeName;
        this.userInfoBytes = serializeObject(info);
    }

    /**
     * Constructor for expiration based persistent timers.
     *
     * @param j2eeName   identity of the Timer bean that is the target of the associated task.
     * @param info       the user data associated with this timer
     * @param expiration The point in time at which the timer must expire.
     * @param interval   The number of milliseconds that must elapse between timer expiration notifications.
     *                       A negative value indicates this is a single-action timer.
     *
     * @throws IOException if the serializable user object cannot be serialized.
     **/
    protected PersistentTimerTaskHandler(J2EEName j2eeName, Serializable info,
                                         Date expiration, long interval) {
        this(j2eeName, info);

        this.expiration = expiration.getTime();
        this.interval = interval;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>: " + this);
    }

    /**
     * Constructor for calendar based persistent timers (not automatic).
     *
     * @param j2eeName       identity of the Timer bean that is the target of the associated task.
     * @param info           the user data associated with this timer; may be null
     * @param parsedSchedule the parsed schedule expression for calendar-based timers; must be non-null
     *
     * @throws IOException if the serializable user object cannot be serialized.
     **/
    protected PersistentTimerTaskHandler(J2EEName j2eeName, Serializable info,
                                         ParsedScheduleExpression parsedSchedule) {
        this(j2eeName, info);

        this.parsedSchedule = parsedSchedule;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>: " + this);
    }

    /**
     * Constructor for automatic calendar based persistent timers.
     *
     * @param j2eeName       identity of the Timer bean that is the target of the associated task.
     * @param info           the user data associated with this timer; may be null
     * @param parsedSchedule the parsed schedule expression for calendar-based timers; must be non-null
     * @param methodId       timeout callback method identifier; must be a non-zero value
     * @param methodame      timeout callback method name; used for validation
     * @param className      timeout callback class name; used for validation (null if defined in XML)
     *
     * @throws IOException if the serializable user object cannot be serialized.
     **/
    protected PersistentTimerTaskHandler(J2EEName j2eeName, Serializable info,
                                         ParsedScheduleExpression parsedSchedule,
                                         int methodId,
                                         String methodName,
                                         String className) {
        this(j2eeName, info);

        this.parsedSchedule = parsedSchedule;

        // For automatic timers, save the method ID for execution, and the method name and class name for validation.
        this.methodId = methodId;
        this.automaticMethodName = methodName;
        this.automaticClassName = className;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>: " + this);
    }

    /**
     * Performs the work associated with the scheduled task / EJB timer. <p>
     *
     * When the EJB timer expires, this method will be called. A wrapper for the
     * target timer bean will be obtained, and the timeout method will be invoked. <p>
     *
     * The wrapper is similar to a generated Local wrapper, and will activate the
     * bean as needed. It will also perform exception handling / mapping as
     * required by the EJB Specification.
     **/
    @Override
    public void run() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "run: " + this);

        EJSHome home;
        TimedObjectWrapper timedObject;

        try {
            // Get the currently installed and active home for this timer. It
            // is possible the home is not currently running, so an
            // EJBNotFoundException may occur.
            home = EJSContainer.getDefaultContainer().getInstalledHome(j2eeName);

            // Verify that the bean is still a Timer bean, in case the
            // application has been modified since the timer was created.
            if (home.beanMetaData.timedMethodInfos == null) {
                Tr.warning(tc, "HOME_NOT_FOUND_CNTR0092W", j2eeName);
                throw new EJBNotFoundException("Incompatible Application Change: " + j2eeName + " no longer supports timers.");
            }
        } catch (EJBNotFoundException ejbnfex) {
            FFDCFilter.processException(ejbnfex, CLASS_NAME + ".run", "305", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "run: Failed locating timer bean " + j2eeName + " : " + ejbnfex);
            throw new TimerServiceException("Failed locating timer bean " + j2eeName, ejbnfex);
        }

        if (automaticMethodName != null && !validateAutomaticTimer(home.beanMetaData)) {
            // Log a message that tells the user that they will
            // need to manually remove the timers from the database.
            Tr.error(tc, "AUTOMATIC_TIMER_VALIDATION_FAILED_CNTR0301E",
                     new Object[] { j2eeName.getComponent(),
                                    j2eeName.getModule(),
                                    j2eeName.getApplication(),
                                    automaticMethodName });
            throw new EJBException("CNTR0220I: The " + j2eeName.getComponent() +
                                   " enterprise bean in the " + j2eeName.getModule() +
                                   " module of the " + j2eeName.getApplication() +
                                   " application has an automatic timer for the " + automaticMethodName +
                                   " method, but an incompatible change was made to the application since the server created the timer.");
        }

        AbstractEJBRuntime ejbRuntime = (AbstractEJBRuntime) home.container.getEJBRuntime();

        // Create a Timer associated with this running Task, so it may be
        // passed on the ejbTimeout invocation. Will cache this taskHandler on the timer.
        PersistentTimer timer = createTimer(ejbRuntime);

        // Get the TimedObjectWrapper from the pool to execute the method.
        timedObject = home.getTimedObjectWrapper(home.ivStatelessId); // set for singleton, stateless, and mdb

        // Don't bother running the timer if the server is stopping; ensure attempt to run fails
        if (ejbRuntime.isStopping()) {
            EJBMethodInfoImpl methodInfo = timedObject.methodInfos[methodId];
            EJBException failure = new EJBException("Timeout method " + methodInfo.getBeanClassName() + "." + methodInfo.getMethodName()
                                                    + " will not be invoked because server is stopping");
            // Don't log error or ffdc since this is normal; but throw exception to ensure db not updated with success
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "run: " + failure);
            throw failure;
        }

        // Invoke ejbTimeout on the wrapper.  No need to handle exceptions,
        // as the wrapper will have already handled/mapped any exceptions
        // as expected.
        try {
            // We are about to call the Timer's ejbTimeout method so set the Timer's instance variable which indicates this.
            timer.isTimeoutCallback = true;

            boolean runInGlobalTx = runInGlobalTransaction(timedObject.methodInfos[methodId]);

            timedObject.invokeCallback(timer, methodId, runInGlobalTx);
        } finally {
            timer.isTimeoutCallback = false;
            home.putTimedObjectWrapper(timedObject);

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "run: " + this);
        }
    }

    /**
     * Creates the runtime environment specific Timer implementation corresponding to
     * this TimerTaskHandler. The returned time should have this taskHandler set as the
     * cachedTaskHandler field.
     */
    protected abstract PersistentTimer createTimer(EJBRuntime ejbRuntime);

    /**
     * Validate that the method corresponding to the method ID stored in the
     * database matches the method that was used when the automatic timer was
     * created. For example, this validation will fail if the application is
     * changed to remove an automatic timer without clearing the timers from
     * the database. As a prerequisite to calling this method, this object
     * must be an automatic timer.
     *
     * @param bmd the bean metadata
     * @return true if this automatic timer is valid, or false if not
     */
    private boolean validateAutomaticTimer(BeanMetaData bmd) {
        if (bmd.timedMethodInfos == null || methodId > bmd.timedMethodInfos.length) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "validateAutomaticTimer: ivMethodId=" + methodId
                             + " > " + Arrays.toString(bmd.timedMethodInfos));
            return false;
        }

        Method method = bmd.timedMethodInfos[methodId].ivMethod;
        if (!method.getName().equals(automaticMethodName)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "validateAutomaticTimer: ivAutomaticMethodName=" + automaticMethodName
                             + " != " + method.getName());
            return false;
        }

        if (automaticClassName != null &&
            !automaticClassName.equals(method.getDeclaringClass().getName())) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "validateAutomaticTimer: ivAutomaticClassName=" + automaticClassName
                             + " != " + method.getDeclaringClass().getName());
            return false;
        }

        return true;
    }

    /**
     * Invoked upon timer expiration prior to the Executor service locking the
     * timer row in the database. This allows the EJB Container to ensure the
     * lock on singleton beans is obtained before the database lock to match
     * the order of locking that occurs when a method is called on a singleton
     * that then performs timer operations; like getTimers. <p>
     *
     * A method context ({@link EJSDeployedSupport}) is returned when a lock
     * has been successfully obtained, and must be provided when calling
     * {@link #unlockSingleton()}. <p>
     *
     * If the bean is not a singleton, does not use container managed concurrency,
     * or the timeout method will not run in a global transaction, then nothing
     * occurs and null is returned; {@link #unlockSingleton()} should not be
     * called. <p>
     *
     * If the bean is no longer installed, not a singleton, or not configured
     * to run in a global transaction, then nothing occurs and null is returned. <p>
     *
     * If the bean is no longer installed or any failure occurs, the EJB
     * specification defined exception for a local method call will be propagated
     * back to the caller; the caller will not be expected to call
     * {@link #unlockSingleton()} to unlock the bean. <p>
     *
     * @return the method context used to lock the bean; or null if locking not required.
     */
    protected EJSDeployedSupport lockSingleton() {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "lockSingleton: " + this);

        EJSHome home;
        EJSDeployedSupport s = null;

        try {
            // Get the currently installed and active home for this timer.
            home = EJSContainer.getDefaultContainer().getInstalledHome(j2eeName);
        } catch (EJBNotFoundException ejbnfex) {
            FFDCFilter.processException(ejbnfex, CLASS_NAME + ".lockSingleton", "305", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "lockSingleton: Failed locating timer bean " + j2eeName + " : " + ejbnfex);
            throw new TimerServiceException("Failed locating timer bean " + j2eeName, ejbnfex);
        }

        // Calling BeanO.preInvoke to obtain a lock is only needed for singleton beans with
        // container managed concurrency that have been configured to run in a global transaction.

        if (home.ivSingletonSessionHome && !home.beanMetaData.ivSingletonUsesBeanManagedConcurrency) {
            EJBMethodInfoImpl[] timerMethodInfos = home.beanMetaData.timedMethodInfos;
            if (timerMethodInfos != null && methodId < timerMethodInfos.length) {
                if (runInGlobalTransaction(timerMethodInfos[methodId])) {
                    s = new EJSDeployedSupport();
                    s.methodId = methodId;
                    s.methodInfo = timerMethodInfos[methodId];
                    s.beanO = home.createSingletonBeanO();
                    try {
                        s.beanO.preInvoke(s, null);
                    } catch (Throwable ex) {
                        FFDCFilter.processException(ex, CLASS_NAME + ".lockSingleton", "403", this);
                        if (isTraceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "Unexpected failure calling beanO.preInvoke: " + ex);
                        try {
                            s.beanO.postInvoke(methodId, s);
                        } catch (Throwable exPost) {
                            FFDCFilter.processException(exPost, CLASS_NAME + ".lockSingleton", "409", this);
                            if (isTraceOn && tc.isDebugEnabled())
                                Tr.debug(tc, "Unexpected failure calling beanO.postInvoke: " + exPost);
                        }
                        if (isTraceOn && tc.isEntryEnabled())
                            Tr.exit(tc, "lockSingleton: Failure acquiring lock for bean " + j2eeName + " : " + ex);
                        throw ExceptionUtil.EJBException("Failure acquiring lock for bean " + j2eeName, ex);
                    }
                }
            } else {
                // The run() method will report this as an error later
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Timer method no longer present on bean??");
            }
        } else {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "not a container managed concurrency singleton bean");
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "lockSingleton: " + s);
        return s;
    }

    /**
     * Invoked after {@link #lockSingleton()} and timer expiration completion.
     * This allows the EJB Container to release any singleton bean locks that were
     * obtained prior to executing the timeout callback method. <p>
     *
     * This method must be called if {@link #lockSingleton()} has been called
     * and returned a non-null value. <p>
     *
     * @param methodContext the method context returned by {@link #lockSingleton()}.
     */
    protected void unlockSingleton(EJSDeployedSupport s) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "unlockSingleton: " + this + ", " + s);

        if (s != null && s.beanO != null) {
            try {
                s.beanO.postInvoke(s.methodId, s);
            } catch (Throwable ex) {
                FFDCFilter.processException(ex, CLASS_NAME + ".unlockSingleton", "305", this);
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "Ignoring unexpected failure calling beanO.postInvoke: " + ex);
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "unlockSingleton");
    }

    /**
     * Returns true if this timer is an automatic timer.
     */
    protected boolean isAutomaticTimer() {
        return automaticMethodName != null;
    }

    public String getAutomaticTimerMethodName() {
        return automaticMethodName;
    }

    /**
     * Retrieves the application specified information that is to be
     * delivered along with the Timer task expiration. A new copy of
     * the serializable object is returned every time. <p>
     *
     * Note: the info object may be returned (or null) even if the application
     * has been uninstalled and the TimerTaskHandler object is not usable.
     * Generally, this method will only fail if the info object itself
     * could not be deserialized. <p>
     *
     * @return the application specified information.
     *
     * @throws TimerServiceException if the application specified information
     *                                   object could not be deserialized.
     */
    public Serializable getUserInfo() throws TimerServiceException {
        if (userInfoBytes == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getUserInfo: null");
            return null;
        }

        Serializable userInfo = null;

        try {
            // Use timer application ClassLoader if installed; otherwise use current thread context ClassLoader
            ClassLoader loader = EJSContainer.getClassLoader(j2eeName);
            if (loader == null) {
                loader = AccessController.doPrivileged(new GetContextClassLoaderPrivileged());
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(userInfoBytes);
            EJBRuntime ejbRuntime = EJSContainer.getDefaultContainer().getEJBRuntime();
            final ObjectInputStream objIstream = ejbRuntime.createObjectInputStream(bais, loader);

            userInfo = (Serializable) AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws ClassNotFoundException, IOException {
                    return objIstream.readObject();
                }
            });
        } catch (PrivilegedActionException paex) {
            // Most likely the application is no longer installed, and so Info class
            // could not be found, or there is a bug in the user info serialization code.
            // Let the caller decide whether to FFDC this scenario, since our MBean supports
            // accessing timers when the corresponding application is not installed.
            TimerServiceException ex = new TimerServiceException("Failure deserializing timer info object.", paex.getException());
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getUserInfo: " + ex);
            throw ex;
        } catch (IOException ioex) {
            FFDCFilter.processException(ioex, CLASS_NAME + ".getUserInfo", "406");
            TimerServiceException ex = new TimerServiceException("Failure deserializing timer info object.", ioex);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getUserInfo: " + ex);
            throw ex;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getUserInfo: " + Util.identity(userInfo));

        return userInfo;
    }

    /**
     * Get the point in time at which the next timer expiration is scheduled
     * to occur after the specified last execution time. <p>
     *
     * @param lastExecution last time the timeout callback was scheduled to expire
     * @param the           date/time in which the timer task was created.
     */
    public abstract Date getNextTimeout(Date lastExecution, Date timerCreationTime);

    /**
     * Retrieves the parsed schedule expression associated with this timer if
     * it is calendar-based.
     *
     * @return the parsed schedule expression, or null if this is not a calendar-based timer
     */
    public ParsedScheduleExpression getParsedSchedule() {
        return parsedSchedule;
    }

    /**
     * Returns the unique Java EE name of the Timer bean that is the target
     * of the corresponding scheduled task (EJB Timer). <p>
     *
     * @return the unique J2EE name of the Timer bean.
     **/
    public J2EEName getJ2EEName() {
        return j2eeName;
    }

    // --------------------------------------------------------------------------
    //
    // Methods for Serializable interface / Serialization
    //
    // --------------------------------------------------------------------------

    /**
     * Write this object to the ObjectOutputStream.
     *
     * Note, this is overriding the default Serialize interface implementation.
     *
     * @see java.io.Serializable
     */
    private void writeObject(ObjectOutputStream out) throws IOException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "writeObject: " + this);

        // Use v1 unless features are present that require v2.
        int version = Constants.TIMER_TASK_V1;
        if (parsedSchedule != null) {
            version = Constants.TIMER_TASK_V2;
        }

        out.defaultWriteObject();

        // Write out header information first.
        out.write(EYECATCHER);
        out.writeShort(PLATFORM);
        out.writeShort(version);

        // Write out the instance data.
        out.writeObject(j2eeName.getBytes());
        out.writeObject(userInfoBytes);

        switch (version) {

            case Constants.TIMER_TASK_V1:
                out.writeLong(expiration);
                out.writeLong(interval);
                break;

            case Constants.TIMER_TASK_V2:
                out.writeObject(parsedSchedule);
                out.writeInt(methodId);
                out.writeObject(automaticMethodName);
                out.writeObject(automaticClassName);
                break;

            default:
                // cannot occur since initialize above
                break;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "writeObject");
    }

    /**
     * Read this object from the ObjectInputStream.
     *
     * Note, this is overriding the default Serialize interface implementation.
     *
     * @see java.io.Serializable
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "readObject");

        in.defaultReadObject();

        // Read in eye catcher.
        byte[] eyeCatcher = new byte[EYECATCHER.length];

        // Insure that all of the bytes have been read.
        int bytesRead = 0;
        for (int offset = 0; offset < EYECATCHER.length; offset += bytesRead) {
            bytesRead = in.read(eyeCatcher, offset, EYECATCHER.length - offset);
            if (bytesRead == -1) {
                throw new IOException("end of input stream while reading eye catcher");
            }
        }

        // Validate that the eyecatcher matches
        for (int i = 0; i < EYECATCHER.length; i++) {
            if (EYECATCHER[i] != eyeCatcher[i]) {
                String eyeCatcherString = new String(eyeCatcher);
                throw new IOException("Invalid eye catcher '" + eyeCatcherString + "' in TimerHandle input stream");
            }
        }

        // Read in the rest of the header.
        @SuppressWarnings("unused")
        short incoming_platform = in.readShort();
        short incoming_vid = in.readShort();

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "version = " + incoming_vid);

        // Verify the version is supported by this version of code.
        if (incoming_vid != Constants.TIMER_TASK_V1 &&
            incoming_vid != Constants.TIMER_TASK_V2) {
            throw new InvalidObjectException("EJB TimerTaskHandler data stream is not of the correct version, this client should be updated.");
        }

        // Read in the instance data.

        byte[] j2eeNameBytes = (byte[]) in.readObject();
        j2eeName = EJSContainer.j2eeNameFactory.create(j2eeNameBytes);
        userInfoBytes = (byte[]) in.readObject();

        switch (incoming_vid) {

            case Constants.TIMER_TASK_V1:
                expiration = in.readLong();
                interval = in.readLong();
                break;

            case Constants.TIMER_TASK_V2:
                parsedSchedule = (ParsedScheduleExpression) in.readObject();
                methodId = in.readInt();
                automaticMethodName = (String) in.readObject();
                automaticClassName = (String) in.readObject();
                break;

            default:
                // cannot occur since unsupported version detected above
                break;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "readObject: " + this);
    }

    // --------------------------------------------------------------------------
    //
    // Non-Interface / Internal Implementation Methods
    //
    // --------------------------------------------------------------------------

    /**
     * Returns true if the specified timeout callback method will run in
     * the scope of a Scheduler provided global transaction.
     */
    protected static boolean runInGlobalTransaction(EJBMethodInfoImpl methodInfo) {
        // The transaction attribute will be one of:
        // - TX_BEAN_MANAGED for BMT
        // - TX_NOT_SUPPORTED if NotSupported was explicitly specified
        // - TX_REQUIRED if unspecified or if Required or RequiresNew was
        //   explicitly specified
        // - TX_REQUIRES_NEW if TimerQOSAtLeastOnceForRequired was specified, and
        //   the transaction attribute was unspecified or Required.
        // We want scheduler to run the task in a global transaction only in the
        // TX_REQUIRED case.
        if (methodInfo.getTransactionAttribute() == TransactionAttribute.TX_REQUIRED) {
            return true;
        }
        return false;
    }

    /**
     * Gets BeanMetaData through EJSHome lookup
     */
    protected BeanMetaData getBeanMetaData() {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getBeanMetaData: " + this);

        EJSHome home;

        try {
            // Get the currently installed and active home for this timer. It
            // is possible the home is not currently running, so an
            // EJBNotFoundException may occur.
            home = EJSContainer.getDefaultContainer().getInstalledHome(j2eeName);

            // Verify that the bean is still a Timer bean, in case the
            // application has been modified since the timer was created.
            if ((home.beanMetaData.timedMethodInfos) == null) {
                Tr.warning(tc, "HOME_NOT_FOUND_CNTR0092W", j2eeName);
                throw new EJBNotFoundException("Incompatible Application Change: " + j2eeName + " no longer supports timers.");
            }
        } catch (EJBNotFoundException ejbnfex) {
            FFDCFilter.processException(ejbnfex, CLASS_NAME + ".getBeanMetaData", "635", this);
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getBeanMetaData: Failed locating timer bean " + j2eeName + " : " + ejbnfex);
            throw new TimerServiceException("Failed locating timer bean " + j2eeName, ejbnfex);
        }

        BeanMetaData bmd = home.beanMetaData;

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getBeanMetaData: " + bmd);

        return bmd;
    }

    /**
     * Internal convenience method for serializing the user info object to a byte array.
     **/
    private static byte[] serializeObject(Object obj) {

        if (obj == null) {
            return null;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(obj);
            out.flush();
        } catch (IOException ioex) {
            throw new EJBException("Timer info object failed to serialize.", ioex);
        }

        return baos.toByteArray();
    }

    /**
     * Overridden to improve trace.
     **/
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName()).append("(");
        builder.append(j2eeName).append(", ");
        if (parsedSchedule == null) {
            builder.append(new Date(expiration)).append(", ").append(interval);
        } else {
            builder.append(methodId).append(", ");
            builder.append(ScheduleExpressionParser.toString(parsedSchedule.getSchedule()));
        }
        builder.append(")");
        return builder.toString();
    }
}
