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
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.DUP2_X1;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.SWAP;
import static org.objectweb.asm.Type.VOID;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Type;

import com.ibm.websphere.monitor.annotation.ProbeAtReturn;
import com.ibm.ws.monitor.internal.ListenerConfiguration;
import com.ibm.ws.monitor.internal.ProbeImpl;
import com.ibm.ws.monitor.internal.ProbeListener;

/**
 * Method adapter that injects code to fire probes at normal method
 * return. This adapter does not support constructors or static
 * initializers.
 */
public class ProbeAtReturnMethodAdapter extends ProbeMethodAdapter {

    /**
     * Set of active listeners that are interested in a method return
     * at this probe site.
     */
    private Set<ProbeListener> enabledListeners = new HashSet<ProbeListener>();

    /**
     * Create a new method adapter that injects a probe at method entry.
     * 
     * @param probeMethodAdapter the chained adapter
     */
    protected ProbeAtReturnMethodAdapter(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
        super(probeMethodAdapter, methodInfo);

        for (ProbeListener listener : interested) {
            ListenerConfiguration config = listener.getListenerConfiguration();
            ProbeAtReturn probeAtReturn = config.getProbeAtReturn();
            if (probeAtReturn != null) {
                enabledListeners.add(listener);
            }
        }
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (enabledListeners.isEmpty() || opcode == ATHROW)
            return;

        String probeKey = createKey();
        ProbeImpl probe = getProbe(probeKey);
        long probeId = probe.getIdentifier();
        Type returnType = getReturnType();

        setProbeInProgress(true);

        if (returnType.getSort() == VOID) {
            visitLdcInsn(Long.valueOf(probeId)); // long1 long2
            if (isStatic()) {
                visitInsn(ACONST_NULL); // long1 long2 this
            } else {
                visitVarInsn(ALOAD, 0); // long1 long2 this
            }
            visitInsn(ACONST_NULL); // long1 long2 this that
            visitInsn(ACONST_NULL); // long1 long2 this that null
        } else {
            if (returnType.getSize() == 2) { // retval
                visitInsn(DUP2); // retval retval
            } else {
                visitInsn(DUP); // retval retval
            }
            box(returnType); // retval boxed
            visitLdcInsn(Long.valueOf(probeId)); // retval boxed long1 long2
            visitInsn(DUP2_X1); // retval long1 long2 boxed long1 long2
            visitInsn(POP2); // retval long1 long2 boxed
            if (isStatic()) {
                visitInsn(ACONST_NULL); // retval long1 long2 boxed this
            } else {
                visitVarInsn(ALOAD, 0); // retval long1 long2 boxed this
            }
            visitInsn(SWAP); // retval long1 long2 this boxed
            visitInsn(ACONST_NULL); // retval long1 long2 this boxed that
            visitInsn(SWAP); // retval long1 long2 this that boxed
        }

        // Fire the probe
        visitFireProbeInvocation();
        setProbeInProgress(false);

        setProbeListeners(probe, enabledListeners);
    }

    private String createKey() {
        StringBuilder sb = new StringBuilder("METHOD_RETURN: ");
        sb.append(getMethodName());
        sb.append(getDescriptor());
        return sb.toString();
    }

}
