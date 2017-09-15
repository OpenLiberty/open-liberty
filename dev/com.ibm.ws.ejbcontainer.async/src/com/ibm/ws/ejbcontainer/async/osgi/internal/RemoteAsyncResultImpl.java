/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.async.osgi.internal;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Observer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.rmi.CORBA.Tie;

import org.omg.PortableServer.Servant;

import com.ibm.ejs.container.RemoteAsyncResult;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ejbcontainer.EJBPMICollaborator;
import com.ibm.ws.ejbcontainer.jitdeploy.ClassDefiner;
import com.ibm.ws.ejbcontainer.jitdeploy.JITDeploy;
import com.ibm.ws.ejbcontainer.osgi.EJBRemoteRuntime;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * A <code>RemoteAsyncResultImpl</code> object packages the results of an
 * EJB asynchronous method call. This is the server-side result object that
 * is meant to be returned to remote clients.
 */
@Trivial
// NOTE: We do not support RemoteAsyncResultExtended as in traditional WAS.
public final class RemoteAsyncResultImpl extends ServerAsyncResultImpl implements RemoteAsyncResult {
    private static final String CLASS_NAME = RemoteAsyncResultImpl.class.getName();
    private static final TraceComponent tc = Tr.register(RemoteAsyncResultImpl.class, "EJBContainer", "com.ibm.ejs.container.container");

    private static Class<? extends Tie> remoteResultTieClass;

    /**
     * The remote runtime.
     */
    private final EJBRemoteRuntime ivRemoteRuntime;

    /**
     * RemoteAsyncResultReaper is the service which will cleanup server-side Future
     * objects as their timeouts expire to avoid memory leaks if the client does not
     * or can not retrieve the results within the timeout.
     */
    private final RemoteAsyncResultReaper ivRemoteAsyncResultReaper; // F743-15582

    /**
     * The remote object ID if this object has been activated.
     *
     * <p>Access to this field is protected by synchronizing on {@link #ivRemoteAsyncResultReaper}.
     */
    private byte[] ivObjectID;

    /**
     * {@code true} if this result has been added to the reaper. The object must
     * be activated before this field will be set to {@code true}. If a
     * client thread is waiting or otherwise manages to cause {@link #cleanup} to
     * be called before {@link #done}, then this result object will never be
     * added to the reaper.
     *
     * <p>Access to this field is protected by synchronizing on {@link #ivRemoteAsyncResultReaper}.
     */
    private boolean ivAddedToReaper; // d690014

    /**
     * The time according to {@link System#currentTimeMillis} that the results
     * for this object become available.
     *
     * <p>Access to this field is protected by synchronizing on {@link #ivRemoteAsyncResultReaper}.
     *
     * @see #done
     */
    private long ivTimeoutStartTime;

    /**
     * The observer to be notified when results are available.
     *
     * <p>Modifications to this variable are protected by synchronizing this
     * object.
     */
    private Observer ivObserver; // F16043

    public RemoteAsyncResultImpl(EJBRemoteRuntime remoteRuntime, RemoteAsyncResultReaper reaper, EJBPMICollaborator pmiBean) {
        super(pmiBean);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "<init>: " + this);

        ivRemoteRuntime = remoteRuntime;
        ivRemoteAsyncResultReaper = reaper;
    }

    @Override
    public String toString() {
        return super.toString() +
               "[activated=" + isActivated() +
               ", timeout=" + (ivTimeoutStartTime == 0 ? null : new Date(ivTimeoutStartTime)) +
               ']';
    }

    private boolean isActivated() {
        return ivObjectID != null;
    }

    /**
     * Sets the current observer and updates the pending one. If the result is
     * already available, the existing observer if any will be updated and
     * passed the new observer as data. Otherwise, the observer will be updated
     * when the result is available and will be passed null as data.
     *
     * @param observer the new observer
     * @return true if the observer was set, or false if the result is available
     */
    boolean setObserver(Observer observer) { // F16043
        boolean success;
        Observer oldObserver;

        synchronized (this) {
            oldObserver = ivObserver;
            success = oldObserver != null || !isDone();
            ivObserver = success ? observer : null;
        }

        if (oldObserver != null) {
            oldObserver.update(null, observer);
        }
        return success;
    }

    @Override
    protected void done() { // F743-15582
        Observer oldObserver;
        synchronized (this) {
            super.done();
            oldObserver = ivObserver;
            ivObserver = null;
        }

        if (oldObserver != null) { // F16043
            oldObserver.update(null, null);
        }

        synchronized (ivRemoteAsyncResultReaper) {
            // d690014 - Don't add to the reaper (from this worker thread) if the
            // client woke up due to the super.done() call and released resources
            // before we got here.
            if (isActivated()) {
                ivTimeoutStartTime = System.currentTimeMillis();

                this.ivRemoteAsyncResultReaper.add(this);
                ivAddedToReaper = true;
            }
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);
        if (cancelled) {
            // ClientAsyncResult will remember that cancel(true) was called
            // successfully and will throw CancellationException locally.
            cleanup();
        }
        return cancelled;
    }

    @Override
    public Object get() throws CancellationException, ExecutionException, InterruptedException {
        Object result;
        try {
            result = super.get();
            cleanup();
        } catch (ExecutionException ee) { // F743-15582
            cleanup();
            throw ee;
        }
        // We do not need to handle CancellationException since that implies the
        // client has already successfully called cancel(true).
        return result;
    }

    @Override
    public Object get(long timeout, String unit) throws CancellationException, ExecutionException, InterruptedException, TimeoutException {
        Object result;
        try {
            result = super.get(timeout, TimeUnit.valueOf(unit));
            cleanup();
        } catch (ExecutionException ee) { // F743-15582
            cleanup();
            throw ee;
        }
        // We do not need to handle CancellationException since that implies the
        // client has already successfully called cancel(true).
        return result;
    }

    public long getTimeoutStartTime() {
        return ivTimeoutStartTime;
    }

    /**
     * Clean up all resources associated with this object.
     */
    private void cleanup() { // d623593
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "cleanup");

        synchronized (ivRemoteAsyncResultReaper) {
            if (ivAddedToReaper) { // d690014
                // d690014.3 - The call to remove will remove the result from the
                // reaper and then call unexportObject itself.
                this.ivRemoteAsyncResultReaper.remove(this);
            } else {
                unexportObject();
            }
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "cleanup");
    }

    /**
     * Export this object so that it is remotely accessible.
     *
     * <p>This method must be called before the async work is scheduled.
     */
    RemoteAsyncResult exportObject() throws RemoteException {
        ivObjectID = ivRemoteRuntime.activateAsyncResult((Servant) createTie());
        return ivRemoteRuntime.getAsyncResultReference(ivObjectID);
    }

    private Tie createTie() {
        Tie tie;
        try {
            tie = getTieClass().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        tie.setTarget(this);
        return tie;
    }

    private static synchronized Class<? extends Tie> getTieClass() {
        Class<? extends Tie> tieClass = remoteResultTieClass;
        if (tieClass == null) {
            try {
                tieClass = JITDeploy.generate_Tie(RemoteAsyncResultImpl.class.getClassLoader(),
                                                  RemoteAsyncResultImpl.class.getName(),
                                                  RemoteAsyncResult.class,
                                                  null,
                                                  new ClassDefiner(),
                                                  0, true).asSubclass(Tie.class);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }

            remoteResultTieClass = tieClass;
        }

        return tieClass;
    }

    /**
     * Unexport this object so that it is not remotely accessible.
     *
     * <p>The caller must be holding the monitor lock on the
     * RemoteAsyncResultReaper prior to calling this method if the async work
     * was successfully scheduled.
     */
    protected void unexportObject() { // d623593
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "unexportObject: " + this);

        if (ivObjectID != null) {
            // Either the allowed timeout occurred or a method was called and we
            // know that the client no longer needs this server resource.
            try {
                ivRemoteRuntime.deactivateAsyncResult(ivObjectID);
            } catch (Throwable e) {
                // We failed to unexport the object.  This should never happen, but
                // it's not fatal.
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "unexportObject exception", e);
                FFDCFilter.processException(e, CLASS_NAME + ".unexportObject", "237", this);
            }

            this.ivObjectID = null;
        }
    }
}
