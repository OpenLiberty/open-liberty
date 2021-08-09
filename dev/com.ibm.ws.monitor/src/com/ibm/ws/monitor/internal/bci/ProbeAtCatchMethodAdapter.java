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
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2_X1;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.SWAP;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import com.ibm.websphere.monitor.annotation.ProbeAtCatch;
import com.ibm.ws.monitor.internal.ListenerConfiguration;
import com.ibm.ws.monitor.internal.ProbeImpl;
import com.ibm.ws.monitor.internal.ProbeListener;
import com.ibm.ws.monitor.internal.ProbeFilter;

/**
 * Method adapter that injects code to fire probes when an exception handler
 * is entered.
 */
public class ProbeAtCatchMethodAdapter extends ProbeMethodAdapter {

    /**
     * Key to use when caching the exception filter.
     */
    private final static String EXCEPTION_FILTER_KEY = "ProbeAtCatch:Filter";

    /**
     * Map of exception handler {@code Label}s to their exception type.
     */
    private Map<Label, Type> handlers = new HashMap<Label, Type>();

    /**
     * Set of active listeners that are interested in an exception catch
     * at this probe site.
     */
    private Set<ProbeListener> enabledListeners = new HashSet<ProbeListener>();

    /**
     * The {@code Label} of exception handler we're visiting but haven't seen
     * an instruction for yet. Within an exception handler we must wait to
     * inject our probes until we see the first opcode to avoid issues with
     * stack map verification.
     */
    private Label handlerPendingInstruction;

    /**
     * Flag that indicates this adapter has work to do.
     */
    private final boolean enabled;

    /**
     * Create a new method adapter that injects a probe at entry to an
     * exception handler.
     * 
     * @param probeMethodAdapter the chained adapter
     */
    protected ProbeAtCatchMethodAdapter(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
        super(probeMethodAdapter, methodInfo);

        for (ProbeListener listener : interested) {
            ListenerConfiguration config = listener.getListenerConfiguration();
            ProbeAtCatch probeAtCatch = config.getProbeAtCatch();
            if (probeAtCatch != null) {
                ProbeFilter filter = config.getTransformerData(EXCEPTION_FILTER_KEY);
                if (filter == null) {
                    filter = new ProbeFilter(probeAtCatch.value(), null, null, null, null);
                    config.setTransformerData(EXCEPTION_FILTER_KEY, filter);
                }
                enabledListeners.add(listener);
            }
        }

        enabled = !enabledListeners.isEmpty();
    }

    /**
     * Callback used to determine the {@code Label}s that correspond to
     * exception handlers.
     */
    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String exceptionType) {
        if (enabled && exceptionType != null) {
            handlers.put(handler, Type.getObjectType(exceptionType));
        }
        super.visitTryCatchBlock(start, end, handler, exceptionType);
    }

    /**
     * Visit a {@code Label}. We use this to keep track of when an exception
     * handler is entered.
     */
    @Override
    public void visitLabel(Label label) {
        if (enabled && handlers.containsKey(label)) {
            handlerPendingInstruction = label;
        }
        super.visitLabel(label);
    }

    /**
     * Generate the code required to fire a probe out of an exception
     * handler.
     */
    private void onHandlerEntry() {
        if (enabled) {
            Type exceptionType = handlers.get(handlerPendingInstruction);

            // Clear the pending instruction flag
            handlerPendingInstruction = null;

            // Filter the interested down to this exception
            Set<ProbeListener> filtered = new HashSet<ProbeListener>();
            for (ProbeListener listener : enabledListeners) {
                ListenerConfiguration config = listener.getListenerConfiguration();
                ProbeFilter filter = config.getTransformerData(EXCEPTION_FILTER_KEY);
                if (filter.isBasicFilter() && filter.basicClassNameMatches(exceptionType.getClassName())) {
                    filtered.add(listener);
                } else if (filter.isAdvancedFilter()) {
                    Class<?> clazz = getOwningClass(exceptionType.getInternalName());
                    if (clazz != null && filter.matches(clazz)) {
                        filtered.add(listener);
                    }
                }
            }

            if (filtered.isEmpty()) {
                return;
            }

            String key = createKey(exceptionType);
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

            setProbeListeners(probe, filtered);
        }
    }

    private String createKey(Type exceptionType) {
        StringBuilder sb = new StringBuilder("EXCEPTION_CAUGHT: ");
        sb.append(getMethodName());
        sb.append(getDescriptor());
        sb.append(": ").append(exceptionType.getInternalName());
        return sb.toString();
    }

    //-------------------------------------------------------------------------
    // The methods below are all visitXInsn wrapper methods that are used to
    // detect the first instruction in a handler.  We do this to help ensure
    // that all of the appropriate metadata associated with the code has
    // been properly handled before our code is injected.
    //-------------------------------------------------------------------------

    @Override
    public void visitInsn(int opcode) {
        if (handlerPendingInstruction != null) {
            onHandlerEntry();
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (handlerPendingInstruction != null) {
            onHandlerEntry();
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        if (handlerPendingInstruction != null) {
            onHandlerEntry();
        }
        super.visitIincInsn(var, increment);
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        if (handlerPendingInstruction != null) {
            onHandlerEntry();
        }
        super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        if (handlerPendingInstruction != null) {
            onHandlerEntry();
        }
        super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(Object constant) {
        if (handlerPendingInstruction != null) {
            onHandlerEntry();
        }
        super.visitLdcInsn(constant);
    }

    @Override
    public void visitLookupSwitchInsn(Label defaultTarget, int[] keys, Label[] labels) {
        if (handlerPendingInstruction != null) {
            onHandlerEntry();
        }
        super.visitLookupSwitchInsn(defaultTarget, keys, labels);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (handlerPendingInstruction != null) {
            onHandlerEntry();
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        if (handlerPendingInstruction != null) {
            onHandlerEntry();
        }
        super.visitMultiANewArrayInsn(desc, dims);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label defaultLabel, Label[] labels) {
        if (handlerPendingInstruction != null) {
            onHandlerEntry();
        }
        super.visitTableSwitchInsn(min, max, defaultLabel, labels);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        if (handlerPendingInstruction != null) {
            onHandlerEntry();
        }
        super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if (handlerPendingInstruction != null) {
            onHandlerEntry();
        }
        super.visitVarInsn(opcode, var);
    }
}
