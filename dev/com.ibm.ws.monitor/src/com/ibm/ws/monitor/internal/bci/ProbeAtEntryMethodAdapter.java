/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal.bci;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;

import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.monitor.annotation.ProbeAtEntry;
import com.ibm.ws.monitor.internal.ListenerConfiguration;
import com.ibm.ws.monitor.internal.ProbeImpl;
import com.ibm.ws.monitor.internal.ProbeListener;

/**
 * Method adapter that injects code to fire probes at method entry.
 * This adapter does not support constructors or static initializers.
 */
public class ProbeAtEntryMethodAdapter extends ProbeMethodAdapter {

    /**
     * Set of active listeners that are interested in method entry at this
     * probe site.
     */
    private Set<ProbeListener> enabledListeners = new HashSet<ProbeListener>();

    /**
     * Create a new method adapter that injects a probe at method entry.
     * 
     * @param probeMethodAdapter the chained adapter
     */
    protected ProbeAtEntryMethodAdapter(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
        super(probeMethodAdapter, methodInfo);

        for (ProbeListener listener : interested) {
            ListenerConfiguration config = listener.getListenerConfiguration();
            ProbeAtEntry probeAtEntry = config.getProbeAtEntry();
            if (probeAtEntry != null) {
                enabledListeners.add(listener);
            }
        }
    }

    /**
     * Inject the byte code required to fire a method entry probe.
     */
    @Override
    protected void onMethodEntry() {
        if (enabledListeners.isEmpty())
            return;

        String probeKey = createKey();
        ProbeImpl probe = getProbe(probeKey);
        long probeId = probe.getIdentifier();

        setProbeInProgress(true);
        visitLdcInsn(Long.valueOf(probeId)); // long1 long2
        if (isStatic() || isConstructor()) {
            visitInsn(ACONST_NULL); // long1 long2 this
        } else {
            visitVarInsn(ALOAD, 0); // long1 long2 this
        }
        visitInsn(ACONST_NULL); // long1 long2 this that
        createParameterArray(); // long1 long2 this that args
        visitFireProbeInvocation();
        setProbeInProgress(false);

        setProbeListeners(probe, enabledListeners);
    }

    private String createKey() {
        StringBuilder sb = new StringBuilder("METHOD_ENTRY: ");
        sb.append(getMethodName());
        sb.append(getDescriptor());
        return sb.toString();
    }

}
