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
import static org.objectweb.asm.Opcodes.DUP2_X2;
import static org.objectweb.asm.Opcodes.DUP_X2;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.POP2;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Type;

import com.ibm.websphere.monitor.annotation.ProbeFieldGet;
import com.ibm.ws.monitor.internal.ListenerConfiguration;
import com.ibm.ws.monitor.internal.ProbeImpl;
import com.ibm.ws.monitor.internal.ProbeListener;
import com.ibm.ws.monitor.internal.ProbeFilter;

public class ProbeFieldGetMethodAdapter extends ProbeMethodAdapter {

    /**
     * Key used to reference cached field filter.
     */
    private final static String FIELD_FILTER_KEY = ProbeFieldGet.class.getSimpleName() + ":FieldFilter";

    /**
     * Key used to reference cached field type filter.
     */
    private final static String FIELD_TYPE_FILTER_KEY = ProbeFieldGet.class.getSimpleName() + ":FieldTypeFilter";

    /**
     * Map of enabled listener to its associated field filter.
     */
    private Map<ProbeListener, ProbeFilter> fieldFilters = new HashMap<ProbeListener, ProbeFilter>();

    /**
     * Map of enabled listener to its associated field type filter.
     */
    private Map<ProbeListener, ProbeFilter> fieldTypeFilters = new HashMap<ProbeListener, ProbeFilter>();

    /**
     */
    ProbeFieldGetMethodAdapter(ProbeMethodAdapter probeMethodAdapter, MethodInfo methodInfo, Set<ProbeListener> interested) {
        super(probeMethodAdapter, methodInfo);

        for (ProbeListener listener : interested) {
            ListenerConfiguration config = listener.getListenerConfiguration();
            ProbeFieldGet probeFieldGet = config.getProbeFieldGet();
            if (probeFieldGet != null) {
                ProbeFilter fieldFilter = config.getTransformerData(FIELD_FILTER_KEY);
                if (fieldFilter == null) {
                    String className = probeFieldGet.clazz();
                    if (className.isEmpty()) {
                        className = probeMethodAdapter.getProbedClass().getName();
                    }
                    fieldFilter = new ProbeFilter(className, probeFieldGet.field(), null, null, null);
                    config.setTransformerData(FIELD_FILTER_KEY, fieldFilter);
                }

                ProbeFilter fieldTypeFilter = config.getTransformerData(FIELD_TYPE_FILTER_KEY);
                if (fieldTypeFilter == null) {
                    fieldTypeFilter = new ProbeFilter(probeFieldGet.type(), null, null, null, null);
                    config.setTransformerData(FIELD_TYPE_FILTER_KEY, fieldTypeFilter);
                }

                fieldFilters.put(listener, fieldFilter);
                fieldTypeFilters.put(listener, fieldTypeFilter);
            }
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (fieldFilters.isEmpty() || (opcode != GETSTATIC && opcode != GETFIELD)) {
            super.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        Class<?> clazz = null;
        Field field = null;

        Set<ProbeListener> matchingListeners = new HashSet<ProbeListener>();
        Type fieldType = Type.getType(desc);
        for (ProbeListener listener : fieldFilters.keySet()) {
            ProbeFilter fieldFilter = fieldFilters.get(listener);
            ProbeFilter fieldTypeFilter = fieldTypeFilters.get(listener);
            if (!fieldFilter.basicMemberNameMatches(name)) {
                continue;
            }
            if (!fieldTypeFilter.basicClassNameMatches(fieldType.getClassName())) {
                continue;
            }
            if (clazz == null) {
                clazz = getOwningClass(owner);
                field = getTargetField(clazz, name, desc);
            }
            if (fieldFilter.matches(field) && fieldTypeFilter.matches(field.getType())) {
                matchingListeners.add(listener);
            }
        }

        if (!matchingListeners.isEmpty()) {
            String key = createKey(owner, name, desc);
            ProbeImpl probe = getProbe(key);
            long probeId = probe.getIdentifier();

            setProbeInProgress(true);
            if (opcode == GETSTATIC) {
                visitInsn(ACONST_NULL); // (target) target
            } else {
                visitInsn(DUP); // (target) target
                visitInsn(DUP); // (target) target target
            }

            // Call get field to get our disposable value
            super.visitFieldInsn(opcode, owner, name, desc);

            box(fieldType); // (target) target boxed
            visitLdcInsn(Long.valueOf(probeId)); // (target) target boxed long1 long2
            visitInsn(DUP2_X2); // (target) long1 long2 target boxed long1 long2
            visitInsn(POP2); // (target) long1 long2 target boxed
            if (isStatic()) {
                visitInsn(ACONST_NULL); // (target) long1 long2 target boxed this
            } else {
                visitVarInsn(ALOAD, 0); // (target) long1 long2 target boxed this
            }
            visitInsn(DUP_X2); // (target) long1 long2 this target boxed this
            visitInsn(POP); // (target) long1 long2 this target boxed
            visitFireProbeInvocation(); // (target)
            setProbeInProgress(false);

            setProbeListeners(probe, matchingListeners);
        }

        super.visitFieldInsn(opcode, owner, name, desc);
    }

    String createKey(String owner, String fieldName, String desc) {
        StringBuilder sb = new StringBuilder("FIELD_GET: ");
        sb.append(getMethodName());
        sb.append(getDescriptor());
        sb.append(" gets ");
        sb.append(owner).append(".").append(fieldName);
        sb.append(" (").append(desc).append(")");
        return sb.toString();
    }
}
