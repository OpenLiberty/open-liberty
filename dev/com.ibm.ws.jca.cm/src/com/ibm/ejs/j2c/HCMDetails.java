/*******************************************************************************
 * Copyright (c) 1997,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.resource.spi.ConnectionRequestInfo;
import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.cm.handle.HandleListInterface;

public class HCMDetails implements HandleListInterface.HandleDetails, PrivilegedAction<StackTraceElement[]> {
    private static final TraceComponent tc = Tr.register(HCMDetails.class, J2CConstants.traceSpec, J2CConstants.NLS_FILE);

    /**
     * Indicates if a connection leak has ever been observed.
     */
    private static final AtomicBoolean connectionLeakOccurred = new AtomicBoolean();

    /**
     * Indicates if the stack of a connection leak has been reported to the user.
     */
    private static final AtomicBoolean connectionLeakStackReported = new AtomicBoolean();

    public final ConnectionManager _cm;
    public final Object _handle;
    public MCWrapper _mcWrapper;
    public final Subject _subject;
    public final ConnectionRequestInfo _cRequestInfo;
    private StackTraceElement[] stackOfAllocateConnection;

    HCMDetails(ConnectionManager cm,
               java.lang.Object handle,
               MCWrapper mcWrapper,
               javax.security.auth.Subject subject,
               ConnectionRequestInfo cRequestInfo) {
        _cm = cm;
        _handle = handle;
        _mcWrapper = mcWrapper;
        _subject = subject;
        _cRequestInfo = cRequestInfo;

        // Capture thread stacks after the first connection leak is observed, up until a connection leak stack gets reported.
        if (connectionLeakOccurred.get() && !connectionLeakStackReported.get()) {
            stackOfAllocateConnection = AccessController.doPrivileged(this);
        }
    }

    @Override
    public void close(boolean leaked) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "close", leaked ? ("leaked " + _handle) : _handle);

        try {
            if (leaked
                && !connectionLeakOccurred.compareAndSet(false, true) // it's not the first time a connection leak occurred
                && stackOfAllocateConnection != null) { // and we have the stack for it

                // Report the first captured connection leak stack to the user, and any others to trace
                if (connectionLeakStackReported.compareAndSet(false, true)) {
                    Tr.info(tc, "CONNECTION_LEAK_DETECTED_J2CA8070", _cm.gConfigProps.cfName, printableStackOfAllocate());
                } else if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Connection leak detected for " + _cm.gConfigProps.cfName + ". Stack of allocate:",
                             printableStackOfAllocate());
                }
            }

            if (_handle instanceof java.sql.Connection)
                ((java.sql.Connection) _handle).close();
            else if (_handle instanceof javax.resource.cci.Connection)
                ((javax.resource.cci.Connection) _handle).close();
            else
                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                    @Override
                    public Void run() throws Exception {
                        Method m = _handle.getClass().getMethod("close");
                        m.invoke(_handle);
                        return null;
                    }
                });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "close");
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "close", e);
        }
    }

    @Override
    public boolean forHandle(Object h) {
        boolean isMyHandle = _handle.equals(h);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "forHandle " + h + "? " + isMyHandle);
        return isMyHandle;
    }

    @Override
    public void park() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "park", _handle);

        try {
            _cm.parkHandle(this);
        } catch (javax.resource.spi.IllegalStateException e) {
            // Absorbing this exception. Could be caused by Transaction timeout having forced a mc.cleanup
            //  which invalidated the handle for this parkHandle call.
            // The RRA throws this exception for the above case.  Other RA's may throw different exceptions
            //  which will fall into the follow on catch clauses which will rethrow the exception.

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "error is expected if it follows a transaction timeout", e);
        } catch (Exception e) {
            // This could be caused by Transaction timeout having forced a mc.cleanup
            //  which invalidated the handle for this parkHandle call.

            FFDCFilter.processException(e, getClass().getName(), "373", this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "park", e);

            RuntimeException re = new RuntimeException("parkHandle call Failed");
            re.initCause(e);
            throw re;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "park");
    }

    /**
     * Format the connection allocation stack trace with one StackTraceElement on each line and some indentation.
     *
     * @return stack trace formatted as printable text.
     */
    private String printableStackOfAllocate() {
        final String EOLN = String.format("%n");
        StringBuilder s = new StringBuilder();
        boolean include = false;
        for (StackTraceElement line : stackOfAllocateConnection)
            if (include)
                s.append(EOLN).append("    ").append(line);
            else // skip the portion of the stack above the constructor of this class
                include = "<init>".equals(line.getMethodName());
        return s.toString();
    }

    @Override
    public void reassociate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "reassociate", _handle);

        try {
            _cm.reAssociate(this);
        } catch (javax.resource.spi.IllegalStateException e) {
            // Absorbing this exception. Could be caused by Transaction timeout having forced a mc.cleanup
            //  which invalidated the handle for this reAssociate call.
            // The RRA throws this exception for the above case.  Other RA's may throw different exceptions
            //  which will fall into the followon catch clauses which will rethrow the exception.

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "error is expected if it follows a transaction timeout", e);
        } catch (Exception e) {
            // This could be caused by Transaction timeout having forced a mc.cleanup
            // which invalidated the handle for this parkHandle call.

            // reAssociate failed for some unknown reason

            FFDCFilter.processException(e, getClass().getName(), "297", this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(this, tc, "reassociate", e);

            RuntimeException re = new RuntimeException("Reassociate call Failed");
            re.initCause(e);
            throw re;
        } // end catch Exception

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "reassociate");
    }

    /**
     * Privileged action to obtain the current stack.
     *
     * @return the current thread stack.
     */
    @Override
    public StackTraceElement[] run() {
        return Thread.currentThread().getStackTrace();
    }
}