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
import static org.objectweb.asm.Opcodes.DOUBLE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2_X1;
import static org.objectweb.asm.Opcodes.FLOAT;
import static org.objectweb.asm.Opcodes.F_FULL;
import static org.objectweb.asm.Opcodes.INTEGER;
import static org.objectweb.asm.Opcodes.LONG;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.SWAP;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.ibm.websphere.monitor.annotation.ProbeAtExceptionExit;
import com.ibm.ws.monitor.internal.ListenerConfiguration;
import com.ibm.ws.monitor.internal.ProbeImpl;
import com.ibm.ws.monitor.internal.ProbeListener;

/**
 * Method adapter that injects code to fire probes when a method
 * exits due to an exception.
 * TODO: Figure out if there's a way to get a try/catch around a super
 * class initializer
 */
public class ProbeAtExceptionExitMethodAdapter extends ProbeMethodAdapter {

    /**
     * The {@link Type} representing a java.lang.Throwable.
     */
    private final static Type THROWABLE_TYPE = Type.getType(Throwable.class);

    /**
     * Set of active listeners that are interested in an exception catch
     * at this probe site.
     */
    private final Set<ProbeListener> enabledListeners = new HashSet<ProbeListener>();

    /**
     * Try/catch block starting label.
     */
    private final Label startLabel = new Label();

    /**
     * Try/catch block end label.
     */
    private final Label endLabel = new Label();

    /**
     * Create a new method adapter that injects a probe where a method exits
     * with an unhandled exception.
     * 
     * @param probeMethodAdapter the chained adapter
     */
    protected ProbeAtExceptionExitMethodAdapter(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
        super(probeMethodAdapter, methodInfo);

        for (ProbeListener listener : interested) {
            ListenerConfiguration config = listener.getListenerConfiguration();
            ProbeAtExceptionExit probeAtExceptionExit = config.getProbeAtExceptionExit();
            if (probeAtExceptionExit != null) {
                enabledListeners.add(listener);
            }
        }
    }

    @Override
    public void onMethodEntry() {
        if (enabledListeners.isEmpty())
            return;

        // Indicate we're injecting probe data
        setProbeInProgress(true);

        // Try/catch block starts before any code
        visitTryCatchBlock(startLabel, endLabel, endLabel, THROWABLE_TYPE.getInternalName());
        visitLabel(startLabel);

        // Indicate that we're done with the probe data sequence
        setProbeInProgress(false);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (enabledListeners.isEmpty()) {
            super.visitMaxs(maxStack, maxLocals);
            return;
        }

        // Indicate that we're injecting probe data
        setProbeInProgress(true);

        // Try/catch block ends after any code
        visitLabel(endLabel);

        List<Object> stackLocals = new ArrayList<Object>(getArgumentTypes().length + 2);
        if (!isStatic()) {
            stackLocals.add(Type.getType(getProbedClass()).getInternalName());
        }
        for (Type type : getArgumentTypes()) {
            switch (type.getSort()) {
                case Type.ARRAY:
                case Type.OBJECT:
                    stackLocals.add(type.getInternalName());
                    break;
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    stackLocals.add(INTEGER);
                    break;
                case Type.LONG:
                    stackLocals.add(LONG);
                    break;
                case Type.FLOAT:
                    stackLocals.add(FLOAT);
                    break;
                case Type.DOUBLE:
                    stackLocals.add(DOUBLE);
                    break;
                default:
                    break;
            }
        }
        visitFrame(F_FULL, stackLocals.size(), stackLocals.toArray(), 1, new Object[] { THROWABLE_TYPE.getInternalName() });

        ProbeImpl probe = getProbe(createKey());
        long probeId = probe.getIdentifier();

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
        visitInsn(SWAP); // throwable long1 logn2 this null throwable
        visitFireProbeInvocation(); // throwable
        visitInsn(ATHROW);

        // Indicate that we're done with the probe data sequence
        setProbeInProgress(false);
        setProbeListeners(probe, enabledListeners);

        super.visitMaxs(maxStack, maxLocals);
    }

    private String createKey() {
        StringBuilder sb = new StringBuilder("METHOD_EXCEPTION_EXIT: ");
        sb.append(getMethodName());
        sb.append(getDescriptor());
        return sb.toString();
    }

}
