/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.io.InvalidObjectException;
import java.io.IOException;
import java.io.Serializable;

import com.ibm.ejs.container.passivator.PassivatorSerializableHandle;
import com.ibm.ejs.container.util.ByteArray;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import com.ibm.ws.ejb.portable.Constants;

/**
 * The TimerNpHandleImpl exists to allow SFSB passivation. Unlike TimerImpl,
 * it does not need to implement TimerHandle because Timer.getHandle must
 * always throw IllegalStateException for non-persistent timers.
 */
public final class TimerNpHandleImpl
                implements Serializable, PassivatorSerializableHandle
{
    private static final TraceComponent tc =
                    Tr.register(TimerNpHandleImpl.class, "EJBContainer",
                                "com.ibm.ejs.container.container");

    private static final long serialVersionUID = 1137182555465371492L;

    // Although this class is not required to interoperate with other
    // application servers, the serialized version may be persisted, and thus
    // the implementation may need to be portable across the different
    // WebSphere platforms, and the ability to version the implementation is
    // required. The desire is that the container should own the process of
    // marshaling and demarshaling these classes. Therefore, the following
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

    /** Uniquely identifies the timer **/
    private transient String ivTaskId;

    /** Uniquely identifies the EJB, and provides access to the bean home. **/
    private transient BeanId ivBeanId;

    /**
     * Constructs a TimerNpHandleImpl with the Task Id which uniquely identifies
     * the Timer, and the Bean Id which uniquely identifies the EJB. <p>
     * 
     * @param beanId identity of the EJB for the Timer callback
     * @param taskId unique identity of the represented Timer
     **/
    TimerNpHandleImpl(BeanId beanId,
                      String taskId) // F743-425.CodRev
    {
        ivBeanId = beanId;
        ivTaskId = taskId;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, this.toString());
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
    private void writeObject(java.io.ObjectOutputStream out)
                    throws IOException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "writeObject: " + this);

        out.defaultWriteObject();

        // Write out header information first.
        out.write(EYECATCHER);
        out.writeShort(PLATFORM);
        out.writeShort(VERSION_ID);

        // Write out the instance data.
        out.writeUTF(ivTaskId);
        out.writeObject(ivBeanId.getByteArrayBytes());

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
    private void readObject(java.io.ObjectInputStream in)
                    throws IOException, ClassNotFoundException
    {
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
                throw new IOException("Invalid eye catcher '" + eyeCatcherString +
                                      "' in TimerHandle input stream");
            }

        }

        // Read in the rest of the header.
        @SuppressWarnings("unused")
        short incoming_platform = in.readShort();
        short incoming_vid = in.readShort();

        // Verify the version is supported by this version of code.
        if (incoming_vid != VERSION_ID) {
            throw new InvalidObjectException("EJB TimerHandle data stream is not of the correct version, " +
                                             "this client should be updated.");
        }

        // Read in the instance data.
        ivTaskId = in.readUTF();

        byte[] bytes = (byte[]) in.readObject();
        ByteArray byteArray = new ByteArray(bytes);
        EJSContainer container = EJSContainer.getDefaultContainer();
        ivBeanId = BeanId.getBeanId(byteArray, container);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "readObject: " + this);
    }

    /**
     * Returns the object that this TimerHandle was used to represent during
     * serialization; a reference to the Timer represented by this handle. <p>
     * 
     * This method is intended for use by the Stateful passivation code, when
     * a Stateful EJB is being passivated, and contains a Timer (not a
     * Serializable object). <p>
     * 
     * @return a reference to the Timer represented by this handle.
     **/
    public Object getSerializedObject()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getSerializedObject : " + ivBeanId + ", " + ivTaskId);

        Object timer = TimerNpImpl.getDeserializedTimer(ivBeanId, ivTaskId); // RTC107334

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getSerializedObject : " + timer);

        return timer;
    }
}
