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
import java.security.PrivilegedExceptionAction;

import javax.resource.spi.ConnectionRequestInfo;
import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jca.cm.handle.HandleListInterface;

public class HCMDetails implements HandleListInterface.Handle {
    private static final TraceComponent tc = Tr.register(HCMDetails.class, J2CConstants.traceSpec, J2CConstants.NLS_FILE);

    public final ConnectionManager _cm;
    public final Object _handle;
    public MCWrapper _mcWrapper;
    public final Subject _subject;
    public final ConnectionRequestInfo _cRequestInfo;

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
    }

    @Override
    public void close() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "close", _handle);

        try {
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

            Object[] parms = new Object[] { "parkHandle", "parkHandle", _handle, e };
            Tr.warning(tc, "PARK_OR_REASSOCIATE_FAILED_W_J2CA0083", parms); // TODO message not added to Liberty
        } catch (Exception e) {
            // This could be caused by Transaction timeout having forced a mc.cleanup
            //  which invalidated the handle for this parkHandle call.

            Object[] parms = new Object[] { "parkHandle", "parkHandle", _handle, e };
            Tr.warning(tc, "PARK_OR_REASSOCIATE_FAILED_W_J2CA0083", parms); // TODO message not added to Liberty
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

            Object[] parms = new Object[] { "reAssociate", "reAssociate", _handle, e };
            Tr.warning(tc, "PARK_OR_REASSOCIATE_FAILED_W_J2CA0083", parms); // TODO message not added to Liberty
        } catch (Exception e) {
            // This could be caused by Transaction timeout having forced a mc.cleanup
            // which invalidated the handle for this parkHandle call.

            // reAssociate failed for some unknown reason

            Object[] parms = new Object[] { "reAssociate", "reAssociate", _handle, e };
            Tr.warning(tc, "PARK_OR_REASSOCIATE_FAILED_W_J2CA0083", parms); // TODO message not added to Liberty
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
}