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
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2_X1;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.SWAP;

import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.monitor.annotation.ProbeAtThrow;
import com.ibm.ws.monitor.internal.ListenerConfiguration;
import com.ibm.ws.monitor.internal.ProbeImpl;
import com.ibm.ws.monitor.internal.ProbeListener;

/**
 * Method adapter that injects code to fire a probe before an exception
 * is thrown.
 */
public class ProbeAtThrowMethodAdapter extends ProbeMethodAdapter {

    /**
     * Set of active listeners that are interested in an exception throw
     * at this probe site.
     */
    private Set<ProbeListener> enabledListeners = new HashSet<ProbeListener>();

    /**
     * Create a new method adapter that injects a probe prior to throwing an
     * exception.
     * 
     * @param probeMethodAdapter the chained adapter
     */
    protected ProbeAtThrowMethodAdapter(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
        super(probeMethodAdapter, methodInfo);

        for (ProbeListener listener : interested) {
            ListenerConfiguration config = listener.getListenerConfiguration();
            ProbeAtThrow probeAtThrow = config.getProbeAtThrow();
            if (probeAtThrow != null) {
                enabledListeners.add(listener);
            }
        }
    }

    /**
     * Inject code to fire a probe before any throw instruction.
     */
    @Override
    public void visitInsn(int opcode) {
        if (opcode == ATHROW && !enabledListeners.isEmpty()) {
            String key = createKey();
            ProbeImpl probe = getProbe(key);
            long probeId = probe.getIdentifier();

            setProbeInProgress(true);
            visitInsn(DUP); // throwable throwable
            visitLdcInsn(Long.valueOf(probeId)); // throwable throwable long1 long2
            visitInsn(DUP2_X1); // throwable long1 long2 throwable long1 long2
            visitInsn(POP2); // throwable long1 long2 throwable
            if (isStatic()) {
                visitInsn(ACONST_NULL); // throwable long1 long2 throwable this
            } else {
                visitVarInsn(ALOAD, 0); // throwable long1 long2 throwable this
            }
            visitInsn(SWAP); // throwable long1 long2 this throwable
            visitInsn(ACONST_NULL); // throwable long1 long2 this throwable null
            visitInsn(SWAP); // throwable long1 long2 this null throwable
            visitFireProbeInvocation(); // throwable
            setProbeInProgress(false);

            setProbeListeners(probe, enabledListeners);
        }

        super.visitInsn(opcode);
    }

    private String createKey() {
        StringBuilder sb = new StringBuilder("EXCEPTION_THROWN: ");
        sb.append(getMethodName());
        sb.append(getDescriptor());
        return sb.toString();
    }

}
