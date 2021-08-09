/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.UnexpectedException;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.rmi.PortableRemoteObject;
import javax.rmi.CORBA.Stub;
import javax.rmi.CORBA.Util;

import org.omg.CORBA.NO_RESPONSE;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.portable.ApplicationException;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CORBA.portable.RemarshalException;
import org.omg.CORBA.portable.ServantObject;

import com.ibm.ws.ejb.portable.Constants;
import com.ibm.ws.ejb.portable.LoggerHelper;

/**
 * A <code>ClientAsyncResult</code> object packages the results of an
 * EJB asynchronous method call. This is the client-side result object.
 * It is meant to be returned to remote clients.
 */
public class ClientAsyncResult implements Future, Serializable {
    private static final long serialVersionUID = -2495785869614768234L;

    private static final String CLASS_NAME = ClientAsyncResult.class.getName();
    private static final Logger svLogger = LoggerHelper.getLogger(CLASS_NAME, "EJBContainer");

    public static final String disableAsyncResultRetries = "com.ibm.websphere.ejbcontainer.disableAsyncResultRetries"; // F16043
    public static final boolean DisableAsyncResultRetries = Boolean.getBoolean(disableAsyncResultRetries); // F16043

    public static final String asyncResultNoResponseBackoff = "com.ibm.websphere.ejbcontainer.asyncResultNoResponseBackoff"; // F16043
    public static final long AsyncResultNoResponseBackoff = TimeUnit.SECONDS.toMillis(Integer.getInteger(asyncResultNoResponseBackoff, 30).intValue()); // F16043
    private static final long MIN_ASYNC_RESULT_NO_RESPONSE_WAIT_TIME = TimeUnit.SECONDS.toMillis(1);

    /**
     * Stub reference to retrieve server results.
     * This is set to null once results are retrieved.
     */
    private transient RemoteAsyncResult ivServer = null;

    /**
     * True if {@link #ivServer} supports {@link RemoteAsyncResultExtended}.
     * This non-transient field is initialized to true in the server and written
     * using defaultWriteObject. If the server class has the field but the
     * client class does not, then defaultReadObject will ignore the extra data,
     * and the client use the non-extended interface. If the client class has
     * the field but the server class does not, then defaultReadObject will
     * leave this field set to false (constructors are not called, which means
     * the following default initialization is not performed), and the client
     * will again use the non-extended interface.
     */
    private final boolean ivServerExtended; // F16043

    /**
     * True if {@link #ivServer} should be used as {@link RemoteAsyncResultExtended}. This field will be false if {@link #ivServerExtended} is false, but it can be false for other
     * reasons.
     */
    private transient boolean ivUseServerExtended; // F16043

    /**
     * The result object when the asynchronous method call ends without exception.
     * This field is only non-null after get() has already been called to server.
     */
    private transient Object ivResult = null;

    /**
     * True if the method was canceled prior to being executed or if the method
     * finished executing (with or without an exception), which means that
     * ivCancellationException, ivExecutionException, or ivResult must be set.
     */
    private transient boolean ivDone = false;

    /**
     * The exception object if the method was canceled prior to being executed.
     */
    private transient CancellationException ivCancellationException = null;

    /**
     * The exception object when the asynchronous method call ends with exception.
     */
    private transient ExecutionException ivExecutionException = null;

    /**
     * True if the bean implements Business RMI Remote.
     */
    private transient boolean ivBusinessRmiRemote = false;

    private EJBException initCause(EJBException ex) { // F16043
        if (ex.getCause() == null) {
            ex.initCause(ex.getCausedByException());
        }
        return ex;
    }

    /**
     * Construct the ClientAsyncResult to return to the client.
     * Set the reference to the RemoteAsyncResultImpl so it can be accessed via the stub.
     * Set if RMI remote business interface is implemented by bean.
     */
    public ClientAsyncResult(RemoteAsyncResult serverImpl, boolean businessRmiRemote) { // d614994
        this.ivServer = serverImpl;
        this.ivBusinessRmiRemote = businessRmiRemote; // d614994
        this.ivServerExtended = serverImpl instanceof RemoteAsyncResultExtended;
    }

    public String toString() { // F16043
        return super.toString() + "[rmi=" + ivBusinessRmiRemote +
               ", done=" + ivDone +
               ", exception=" + (ivExecutionException != null) +
               ", cancelled=" + (ivCancellationException != null) +
               ", extended=" + ivUseServerExtended +
               ']';
    }

    /**
     * Cancel the asynchronous method if possible. If the method is still waiting
     * on a work manager queue and has not been dispatched, we can cancel it.
     * Otherwise, it is not likely that the method will be able to stop
     * execution.<p>
     *
     * @param mayInterruptIfRunning
     *
     * @return - true if the method was successfully canceled. Otherwise, false.
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        final boolean isTraceOn = svLogger.isLoggable(Level.FINER);
        if (isTraceOn)
            svLogger.entering(CLASS_NAME, "cancel", new Object[] { toString(), Boolean.valueOf(mayInterruptIfRunning) });

        boolean cancelled = false;

        try {
            cancelled = (ivServer != null ? ivServer.cancel(mayInterruptIfRunning) : false);
            if (cancelled) {
                ivCancellationException = new CancellationException(); // d614994
                ivDone = true;
                ivServer = null;
            }
        } catch (RemoteException e) {
            // Throw an EJBException to the client.
            EJBException ejbEx = initCause(new EJBException(e));
            if (isTraceOn)
                svLogger.exiting(CLASS_NAME, "cancel", ejbEx);
            throw ejbEx;
        }

        if (isTraceOn)
            svLogger.exiting(CLASS_NAME, "cancel", Boolean.valueOf(cancelled));
        return cancelled;
    } // end cancel()

    /**
     * This method allows clients to check the Future object to see if the
     * asynchronous method was canceled before it got a chance to execute.
     */
    public boolean isCancelled() {
        if (svLogger.isLoggable(Level.FINER))
            svLogger.logp(Level.FINER, CLASS_NAME, "isCancelled", toString());
        return (ivCancellationException != null); // d614969
    } // end isCancelled()

    /**
     * This get method returns the result of the asynchronous method call if it is
     * available. Otherwise, it blocks until the result is available. It is
     * unblocked when the Work object that runs the asynchronous method on a work
     * manager finishes (ie. with either good results or an exception), and
     * sets results on this instance.<p>
     *
     * @return - the result object
     *
     * @throws CancellationException - if the asynchronous method was canceled successfully
     * @throws ExecutionException - if the asynchronous method ended with an exception
     * @throws InterruptedException - if the thread is interrupted while waiting
     */
    public Object get() throws ExecutionException, InterruptedException {
        final boolean isTraceOn = svLogger.isLoggable(Level.FINER);
        if (isTraceOn)
            svLogger.entering(CLASS_NAME, "get", toString());

        if (ivCancellationException != null) {
            if (isTraceOn)
                svLogger.exiting(CLASS_NAME, "get", ivCancellationException);
            throw ivCancellationException; // d614994
        }

        // If exception was caught on previous get() call, return the exception again.
        if (ivExecutionException != null) {
            if (isTraceOn)
                svLogger.exiting(CLASS_NAME, "get", ivExecutionException);
            throw ivExecutionException; // d614994
        }

        try {
            if ((ivResult == null) && (ivServer != null)) {
                if (ivUseServerExtended) { // F16043
                    try {
                        ivResult = waitForResult(0);
                    } catch (TimeoutException ex) {
                        // Should not happen for infinite timeout.
                        IllegalStateException ise = new IllegalStateException(ex);
                        if (isTraceOn)
                            svLogger.exiting(CLASS_NAME, "get", ise);
                        throw ise;
                    }
                } else {
                    if (isTraceOn)
                        svLogger.logp(Level.FINER, CLASS_NAME, "get", "calling stub.get()");
                    ivResult = ivServer.get();
                }
                ivServer = null;
            }
        } catch (ExecutionException ee) {
            ivExecutionException = ee; // d614994
            ivServer = null; // d614994
            if (isTraceOn)
                svLogger.exiting(CLASS_NAME, "get", ee);
            throw ee; // d614994
        } catch (RemoteException e) {
            if (isTraceOn)
                svLogger.logp(Level.FINER, CLASS_NAME, "get", "caught RemoteException", e);
            ivServer = null; // d614994
            if (ivBusinessRmiRemote) {
                ivExecutionException = new ExecutionException(e);
            } else {
                // Should be ok to use getCause() on the RemoteException here, the exception mapping
                // on the server should insure we have an exception not an error in the RemoteException
                Throwable cause = e.getCause();
                EJBException ejbEx = initCause(new EJBException(cause instanceof Exception ? (Exception) cause : e));
                ivExecutionException = new ExecutionException(ejbEx);
            }
            if (isTraceOn)
                svLogger.exiting(CLASS_NAME, "get", ivExecutionException);
            // Throw an ExecutionException to the client.
            throw ivExecutionException;
        }

        if (isTraceOn)
            svLogger.exiting(CLASS_NAME, "get", "result");
        return ivResult;
    } // end get()

    /**
     * This get method returns the result of the asynchronous method call if it is
     * available. Otherwise, it blocks until the result is available, or the
     * timeout expires. It is unblocked when the Work object that runs the asynchronous
     * method on a work manager finishes and sets results on this instance, or
     * when the timeout expires.<p>
     *
     * @param timeout - the timeout value
     * @param unit - the time unit for the timeout value (e.g. milliseconds, seconds, etc.)
     *
     * @return - the result object
     *
     * @throws CancellationException - if the asynchronous method was canceled successfully
     * @throws ExecutionException - if the asynchronous method ended with an exception
     * @throws InterruptedException - if the thread is interrupted while waiting
     * @throws TimeoutException - if the timeout period expires before the asynchronous method completes
     */
    public Object get(long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        final boolean isTraceOn = svLogger.isLoggable(Level.FINER);
        if (isTraceOn)
            svLogger.entering(CLASS_NAME, "get", new Object[] { toString(), Long.valueOf(timeout), unit });

        if (ivCancellationException != null) {
            if (isTraceOn)
                svLogger.exiting(CLASS_NAME, "get", ivCancellationException);
            throw ivCancellationException; // d614994
        }

        // If exception was caught on previous get() call, return the exception again.
        if (ivExecutionException != null) {
            if (isTraceOn)
                svLogger.exiting(CLASS_NAME, "get", ivExecutionException);
            throw ivExecutionException; // d614994
        }

        try {
            if ((ivResult == null) && (ivServer != null)) {
                long millis = unit.toMillis(timeout);
                if (ivUseServerExtended && millis > 0) { // F16043
                    ivResult = waitForResult(millis);
                } else {
                    if (isTraceOn)
                        svLogger.logp(Level.FINER, CLASS_NAME, "get", "calling stub.get(long, String)");
                    ivResult = ivServer.get(timeout, unit.toString());
                }
                ivServer = null;
            }
        } catch (ExecutionException ee) {
            ivExecutionException = ee; // d614994
            ivServer = null; // d614994
            if (isTraceOn)
                svLogger.exiting(CLASS_NAME, "get", ee);
            throw ee; // d614994
        } catch (RemoteException e) {
            if (isTraceOn)
                svLogger.logp(Level.FINER, CLASS_NAME, "get", "caught RemoteException", e);
            ivServer = null; // d614994
            if (ivBusinessRmiRemote) {
                ivExecutionException = new ExecutionException(e);
            } else {
                // Should be ok to use getCause() on the RemoteException here, the exception mapping
                // on the server should insure we have an exception not an error in the RemoteException
                Throwable cause = e.getCause();
                EJBException ejbEx = initCause(new EJBException(cause instanceof Exception ? (Exception) cause : e));
                ivExecutionException = new ExecutionException(ejbEx);
            }
            // Throw an ExecutionException to the client.
            if (isTraceOn)
                svLogger.exiting(CLASS_NAME, "get", ivExecutionException);
            throw ivExecutionException;
        }

        if (isTraceOn)
            svLogger.exiting(CLASS_NAME, "get", "result");
        return ivResult;
    } // end get(timeout, unit)

    /**
     * Waits for a result using RemoteAsyncResultExtended.
     *
     * @param timeoutMillis the positive number of milliseconds to wait, or 0 to
     *            wait forever
     * @see RemoteAsyncResultExtended#waitForResult
     */
    private Object waitForResult(long timeoutMillis)
                    throws ExecutionException, InterruptedException, TimeoutException, RemoteException { // F16043
        final boolean isTraceOn = svLogger.isLoggable(Level.FINER);
        if (isTraceOn)
            svLogger.entering(CLASS_NAME, "waitForResult", Long.valueOf(timeoutMillis));

        long begin = System.nanoTime();
        // The nanoTime updated before every call to
        // RemoteAsyncResultExtended.waitForResult.
        long beginLastAttempt = begin;
        // The nanoTime when we should stop waiting altogether.  This value is
        // only used if timeout is non-zero.
        long end = begin + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);

        // True if the client ORB has ever thrown a NO_RESPONSE.
        boolean noResponse = false;
        // The waitTime argument for the next call to
        // RemoteAsyncResultExtended.waitForResult.
        long waitTimeMillis = timeoutMillis;
        // The maximum value for waitTimeMillis.  This value is lazily
        // initialized when a NO_RESPONSE occurs.
        long maxWaitTimeMillis = -1;

        for (;;) {
            if (isTraceOn)
                svLogger.entering(CLASS_NAME, "waitForResult", "waiting " + waitTimeMillis);

            Object[] resultArray;
            try {
                resultArray = stubWaitForResult((Stub) ivServer, waitTimeMillis);
            } catch (RemoteException ex) {
                Throwable cause = ex.getCause();

                // If this is the first NO_RESPONSE, then try again with a shorter
                // server-side wait time.  For subsequent NO_RESPONSE, assume there
                // is a connection issue and rethrow the exception.
                if (cause instanceof NO_RESPONSE && !noResponse) {
                    if (isTraceOn)
                        svLogger.logp(Level.FINER, CLASS_NAME, "waitForResult", "retrying", cause);
                    resultArray = null;
                    noResponse = true;
                } else {
                    if (isTraceOn)
                        svLogger.exiting(CLASS_NAME, "waitForResult", ex);
                    throw ex;
                }
            }

            if (resultArray != null) {
                if (isTraceOn)
                    svLogger.exiting(CLASS_NAME, "waitForResult", "result");
                return resultArray[0];
            }

            // Either the client ORB or the server timed us out.  Choose a lower
            // waitTimeMillis and then try again.

            long now = System.nanoTime();

            if (timeoutMillis > 0) {
                long remainingMillis = TimeUnit.NANOSECONDS.toMillis(end - now);
                if (remainingMillis <= 0) {
                    if (isTraceOn)
                        svLogger.exiting(CLASS_NAME, "waitForResult", "timeout");
                    throw new TimeoutException();
                }

                waitTimeMillis = remainingMillis;
            }

            if (noResponse) {
                if (maxWaitTimeMillis == -1) {
                    // For the first NO_RESPONSE, set maximum server wait time for
                    // all subsequent requests based on how long the ORB actually
                    // waited before timing out this request.  Back off a little bit
                    // to give time for the server to respond.
                    long actualWaitTimeMillis = TimeUnit.NANOSECONDS.toMillis(now - beginLastAttempt);
                    maxWaitTimeMillis = Math.max(MIN_ASYNC_RESULT_NO_RESPONSE_WAIT_TIME,
                                                 actualWaitTimeMillis - AsyncResultNoResponseBackoff);
                }

                if (timeoutMillis == 0) {
                    waitTimeMillis = maxWaitTimeMillis;
                } else {
                    waitTimeMillis = Math.min(waitTimeMillis, maxWaitTimeMillis);
                }
            }

            beginLastAttempt = now;
        }
    }

    /**
     * Invoke {@link RemoteAsyncResultExtended#waitForResult}. The
     * implementation of this method was copied from IBM rmic -keep. We do this
     * so that we don't need to narrow the RemoteAsyncResult stub, which would
     * require a round-trip is_a check.
     *
     * @param stub the stub
     * @param waitTime the method argument
     * @return the method result
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws RemoteException
     */
    private Object[] stubWaitForResult(Stub stub, long waitTime)
                    throws ExecutionException, InterruptedException, RemoteException { // F16043
        while (true) {
            if (!Util.isLocal(stub)) {
                org.omg.CORBA_2_3.portable.InputStream in = null;
                try {
                    try {
                        OutputStream out = stub._request("waitForResult", true);
                        out.write_longlong(waitTime);
                        in = (org.omg.CORBA_2_3.portable.InputStream) stub._invoke(out);
                        return (Object[]) in.read_value(Object[].class);
                    } catch (ApplicationException ex) {
                        in = (org.omg.CORBA_2_3.portable.InputStream) ex.getInputStream();
                        String id = in.read_string();
                        if (id.equals("IDL:java/util/concurrent/ExecutionEx:1.0")) {
                            throw (ExecutionException) in.read_value(ExecutionException.class);
                        }
                        if (id.equals("IDL:java/lang/InterruptedEx:1.0")) {
                            throw (InterruptedException) in.read_value(InterruptedException.class);
                        }
                        throw new UnexpectedException(id);
                    } catch (RemarshalException ex) {
                        continue;
                    }
                } catch (SystemException ex) {
                    throw Util.mapSystemException(ex);
                } finally {
                    stub._releaseReply(in);
                }
            }

            ServantObject so = stub._servant_preinvoke("waitForResult", RemoteAsyncResultExtended.class);
            if (so == null) {
                continue;
            }

            try {
                Object[] result = ((RemoteAsyncResultExtended) so.servant).waitForResult(waitTime);
                return (Object[]) Util.copyObject(result, stub._orb());
            } catch (Throwable ex) {
                Throwable exCopy = (Throwable) Util.copyObject(ex, stub._orb());
                if (exCopy instanceof ExecutionException) {
                    throw (ExecutionException) exCopy;
                }
                if (exCopy instanceof InterruptedException) {
                    throw (InterruptedException) exCopy;
                }
                throw Util.wrapException(exCopy);
            } finally {
                stub._servant_postinvoke(so);
            }
        }
    }

    /**
     * This method allows clients to poll the Future object and only get results
     * once the asynchronous method has finished (ie. either with good results or an
     * exception).
     */
    public boolean isDone() {
        final boolean isTraceOn = svLogger.isLoggable(Level.FINER);
        if (isTraceOn)
            svLogger.entering(CLASS_NAME, "isDone", toString());

        try {
            if (!ivDone) {
                ivDone = (ivServer != null ? ivServer.isDone() : true);
            }
        } catch (RemoteException e) {
            // Throw an EJBException to the client.
            throw initCause(new EJBException(e));
        }

        if (isTraceOn)
            svLogger.exiting(CLASS_NAME, "isDone", Boolean.valueOf(ivDone));
        return ivDone;
    } // end isDone()

    /*
     * readObject and writeObject implementations
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        if (svLogger.isLoggable(Level.FINER))
            svLogger.logp(Level.FINER, CLASS_NAME, "writeObject", toString());

        // if ivServer object is null throw and exception because there is no server side
        // Future object to communicate with.
        if (ivServer == null) {
            throw new EJBException("No Server side Future object exists.");
        }
        out.defaultWriteObject();
        // write out the header information
        out.write(Constants.CLIENT_ASYNC_RESULT_EYE_CATCHER);
        out.writeShort(Constants.PLATFORM_DISTRIBUTED);
        out.writeShort(Constants.CLIENT_ASYNC_RESULT_V1);

        // write out the data
        out.writeObject(ivServer);
        out.writeBoolean(ivBusinessRmiRemote);
    } // end writeObject()

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        final boolean isTraceOn = svLogger.isLoggable(Level.FINER);
        if (isTraceOn)
            svLogger.entering(CLASS_NAME, "readObject");

        in.defaultReadObject();
        byte[] ec = new byte[Constants.EYE_CATCHER_LENGTH];
        byte[] eyecatcher = Constants.CLIENT_ASYNC_RESULT_EYE_CATCHER;
        short incoming_platform;
        short incoming_vid;

        int bytesRead = 0;
        for (int offset = 0; offset < Constants.EYE_CATCHER_LENGTH; offset += bytesRead) {
            bytesRead = in.read(ec, offset, Constants.EYE_CATCHER_LENGTH - offset);
            if (bytesRead == -1) {
                throw new IOException("end of input stream while reading eye catcher");
            }
        }

        // validate that the eyecatcher matches
        for (int i = 0; i < eyecatcher.length; i++) {
            if (eyecatcher[i] != ec[i]) {
                String eyeCatcherString = Arrays.toString(ec);
                throw new IOException("Invalid eye catcher '" + eyeCatcherString + "' in ClientAsyncResult input stream");
            }
        }
        incoming_platform = in.readShort();
        incoming_vid = in.readShort();

        // The server uses a custom _RemoteAsyncResult_Tie to ensure that
        // _RemoteAsyncResult_Stub is sent instead of
        // _RemoteAsyncResultExtended_Stub, which won't exist on old clients.
        ivServer = (RemoteAsyncResult) PortableRemoteObject.narrow(in.readObject(), RemoteAsyncResult.class);
        ivBusinessRmiRemote = in.readBoolean();

        // F16043 - If the client has disabled retries, then don't use
        // RemoteAsyncResultExtended.  Also, there is no reason to use retries
        // for colocated stubs.
        ivUseServerExtended = ivServerExtended &&
                              !DisableAsyncResultRetries &&
                              !Util.isLocal((Stub) ivServer);

        if (isTraceOn)
            svLogger.exiting(CLASS_NAME, "readObject",
                             toString() +
                                             ", platform=" + incoming_platform +
                                             ", version=" + incoming_vid +
                                             ", extended=" + ivServerExtended +
                                             ", server=" + ivServer);
    } // end readObject()

} // ClientAsyncResult
