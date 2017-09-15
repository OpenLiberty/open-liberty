/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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
import java.io.InvalidObjectException;

import javax.ejb.EJBException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.Timer;
import javax.ejb.TimerHandle;

import com.ibm.ejs.container.passivator.PassivatorSerializableHandle;
import com.ibm.ejs.container.util.EJSPlatformHelper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejb.portable.Constants;
import com.ibm.ws.ejbcontainer.runtime.EJBRuntime;

/**
 * Provides an implementation of the javax.ejb.TimerHandle interface,
 * and provides a serializable reference to an EJBTimer from a managed object.
 **/
public final class PersistentTimerHandle implements TimerHandle, PassivatorSerializableHandle
{
    private static final TraceComponent tc = Tr.register(PersistentTimerHandle.class, "EJBContainer", "com.ibm.ejs.container.container");

    private static final long serialVersionUID = -7720620493313660153L;

    // Although this class is not required to interoperate with other
    // application servers, the serialized version may be persisted, and thus
    // the implementation may need to be portable across the different
    // WebSphere platforms, and the ability to version the implementation is
    // required. The desire is that the container should own the process of
    // marshalling and demarshalling these classes. Therefore, the following
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

    // header information for interop and serialization
    private static final byte[] EYECATCHER = Constants.TIMER_HANDLE_EYE_CATCHER;
    private static final short PLATFORM = Constants.PLATFORM_DISTRIBUTED;
    private static final short VERSION_ID = Constants.TIMER_HANDLE_V1;

    /** Uniquely identifies the task (i.e. primary key of row in the database) **/
    protected transient Long taskId;

    /**
     * Set to true if this object represents a serialized Timer.
     **/
    protected transient boolean isTimer;

    /**
     * Constructs a PersistentTimerHandle with the Task Id that uniquely identifies
     * the Timer. <p>
     *
     * @param taskId unique identity of the represented Timer
     * @param timer true if this TimerHandle is being used by
     *            Stateful passivation to represent a Timer.
     **/
    protected PersistentTimerHandle(long taskId, boolean timer) {

        this.taskId = taskId;
        this.isTimer = timer;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, this.toString());
    }

    // --------------------------------------------------------------------------
    //
    // Methods from TimerHandle interface
    //
    // --------------------------------------------------------------------------

    /**
     * Returns a reference to the timer represented by this handle. <p>
     *
     * @throws NoSuchObjectLocalException If invoked on a handle whose
     *             associated timer has expired or has been cancelled. Also thrown
     *             if running in the adjunct control region.
     * @throws EJBException If this method could not complete due to a
     *             system-level failure.
     **/
    @Override
    public Timer getTimer() throws IllegalStateException, NoSuchObjectLocalException, EJBException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getTimer: " + this);

        // Throw NoSuchObjectLocalException if invoked when running in the adjunct control region on z/OS
        if (EJSPlatformHelper.isZOSCRA()) {
            NoSuchObjectLocalException nsoe = new NoSuchObjectLocalException(this.toString() + " -- called from the adjunct control region.");
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "getTimer: " + nsoe);
            throw nsoe;
        }

        // Determine if the calling bean is in a state that allows timer
        // method access - throws IllegalStateException if not allowed.
        checkTimerAccess();

        EJBRuntime ejbRuntime = EJSContainer.getDefaultContainer().getEJBRuntime();
        Timer timer = ejbRuntime.getPersistentTimerFromStore(taskId);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getTimer: " + timer);

        return timer;
    }

    // --------------------------------------------------------------------------
    //
    // Methods for Serializable interface / Serialization
    //
    // --------------------------------------------------------------------------

    /**
     * Write this object to the ObjectOutputStream.
     * Note, this is overriding the default Serialize interface
     * implementation.
     *
     * @see java.io.Serializable
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "writeObject: " + this);

        out.defaultWriteObject();

        // Write out header information first.
        out.write(EYECATCHER);
        out.writeShort(PLATFORM);
        out.writeShort(VERSION_ID);

        // Write out the instance data.
        out.writeBoolean(isTimer);
        out.writeLong(taskId);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "writeObject");
    }

    /**
     * Read this object from the ObjectInputStream.
     * Note, this is overriding the default Serialize interface
     * implementation.
     *
     * @see java.io.Serializable
     */
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

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
        short incoming_platform = in.readShort();
        short incoming_vid = in.readShort();

        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "readObject: platform = " + incoming_platform + ", version = " + incoming_vid);

        // Verify the version is supported by this version of code.
        if (incoming_vid != VERSION_ID) {
            throw new InvalidObjectException("EJB TimerHandle data stream is not of the correct version, this client should be updated.");
        }

        // Read in the instance data.
        isTimer = in.readBoolean();
        taskId = in.readLong();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "readObject: " + this);
    }

    // --------------------------------------------------------------------------
    //
    // Methods from PassivatorSerializableHandle interface
    //
    // --------------------------------------------------------------------------

    /**
     * Returns the object that this TimerHandle was used to represent during
     * serialization; either a reference to the Timer represented by this handle,
     * or the TimerHandle itself. <p>
     *
     * This method is intended for use by the Stateful passivation code, when
     * a Stateful EJB is being passivated, and contains a Timer (not a
     * Serializable object) or a TimerHandle. <p>
     *
     * This method differs from {@link #getTimer} in that it performs none
     * of the checking required by the EJB Specification, such as if the Timer
     * is still valid. When activating a Stateful EJB, none of this checking
     * should be performed. <p>
     *
     * If this TimerHandle was 'marked' during serialization as representing
     * a Timer, then a Timer will be returned. Otherwise, it is assumed to
     * just represent itself, a TimerHandle that has been serialized.
     * See {@link com.ibm.ejs.container.passivator.NewOutputStream} and {@link com.ibm.ejs.container.passivator.NewInputStream}. <p>
     *
     * @return a reference to the Timer represented by this handle,
     *         or the TimerHandle itself.
     **/
    @Override
    public Object getSerializedObject() {

        if (isTimer) {
            EJBRuntime ejbRuntime = EJSContainer.getDefaultContainer().getEJBRuntime();
            return ejbRuntime.getPersistentTimer(taskId);
        }

        return this;
    }

    // --------------------------------------------------------------------------
    //
    // Non-Interface / Internal Implementation Methods
    //
    // --------------------------------------------------------------------------

    /**
     * Determines if Timer methods are allowed based on the current state
     * of bean instance associated with the current transaction. This includes
     * the methods on the javax.ejb.Timer interface. <p>
     *
     * Must be called by all Timer methods to insure EJB Specification
     * compliance. <p>
     *
     * Note: This method does not apply to the EJBContext.getTimerService()
     * method, as getTimerService may be called for more bean states.
     * getTimerServcie() must provide its own checking. <p>
     *
     * @throws IllegalStateException If this instance is in a state that does
     *             not allow timer service method operations.
     **/
    private void checkTimerAccess() throws IllegalStateException {

        BeanO beanO = EJSContainer.getCallbackBeanO();
        if (beanO != null) {
            beanO.checkTimerServiceAccess();
        } else if (EJSContainer.getDefaultContainer().allowTimerAccessOutsideBean) {
            // Beginning with EJB 3.2, the specification was updated to allow Timer
            // and TimerHandle access outside of an EJB. Although it would seem to
            // make sense that a Timer could also be accessed from an EJB in any
            // state, that part of the specification was not updated, so the above
            // checking is still performed when there is a callback BeanO.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkTimerAccess: TimerHandle access permitted outside of bean");
        } else {
            // EJB 3.1 and earlier restricted access to the Timer API to beans only
            IllegalStateException ise = new IllegalStateException("TimerHandle methods not allowed - no active EJB");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "checkTimerAccess: " + ise);
            throw ise;
        }
    }

    /**
     * Overridden to provide state based equality.
     *
     * This override of the default Object.equals is required, even though
     * there are type specific overloads, in case the caller does not have
     * (or know) the parameter as the specific type. <p>
     **/
    @Override
    public boolean equals(Object obj) {
        // Note : Keep in synch with TimerImpl.equals().
        if (obj instanceof PersistentTimerHandle) {
            PersistentTimerHandle timerHandle = (PersistentTimerHandle) obj;
            return taskId.equals(timerHandle.taskId);
        }
        return false;
    }

    /**
     * Overridden to provide state based hashcode.
     **/
    @Override
    public int hashCode() {
        // Note : Keep in synch with PersistentTimer.hashCode().
        return (int) (taskId % Integer.MAX_VALUE);
    }

    /**
     * Overridden to improve trace.
     **/
    @Override
    public String toString() {
        return ("PersistentTimerHandle(" + taskId + ")");
    }
}
