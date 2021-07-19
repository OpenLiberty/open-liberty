/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bytebuffer.internal;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This is a non-pooled buffer that keeps track of owner ref counts like a
 * pooled buffer would.
 */
public class RefCountWsByteBufferImpl extends WsByteBufferImpl {

    private static final long serialVersionUID = -7843989267561719849L;
    private static final TraceComponent tc = Tr.register(RefCountWsByteBufferImpl.class,
                                                         MessageConstants.WSBB_TRACE_NAME,
                                                         MessageConstants.WSBB_BUNDLE);

    /** number of references to this pool entry */
    transient public int intReferenceCount = 1;

    /**
     * Constructor.
     */
    public RefCountWsByteBufferImpl() {
        super();
        this.wsBBRefRoot = this;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Created RefCountWsByteBufferImpl");
        }
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append(getClass().getSimpleName());
        sb.append('@');
        sb.append(Integer.toHexString(hashCode()));
        sb.append('/').append(this.intReferenceCount);
        sb.append(' ').append(super.toString());
        return sb.toString();
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#writeExternal(java.io.ObjectOutput)
     */
    @Override
    public void writeExternal(ObjectOutput s) throws IOException {
        super.writeExternal(s);
    }

    /*
     * @see com.ibm.ws.bytebuffer.internal.WsByteBufferImpl#readExternal(java.io.ObjectInput)
     */
    @Override
    public void readExternal(ObjectInput s) throws IOException, ClassNotFoundException {
        super.readExternal(s);
    }
}
