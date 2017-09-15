/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ras.instrument.internal.bci;

import static com.ibm.ws.ras.instrument.internal.bci.WebSphereTrTracingClassAdapter.TRACE_COMPONENT_TYPE;
import static com.ibm.ws.ras.instrument.internal.bci.WebSphereTrTracingClassAdapter.TR_TYPE;

import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.ibm.ws.ras.instrument.internal.model.TraceOptionsData;

public class WebSphereTrTracingMethodAdapter extends AbstractRasMethodAdapter<AbstractTracingRasClassAdapter> {

    public WebSphereTrTracingMethodAdapter(AbstractTracingRasClassAdapter classAdapter, MethodVisitor visitor, int access, String methodName, String descriptor, String signature,
                                           String[] exceptions) {
        super(classAdapter, true, visitor, access, methodName, descriptor, signature, exceptions);
    }

    @Override
    public boolean onMethodEntry() {
        if (isTrivial() || isAlreadyTraced()) {
            return false;
        }
        if (isStaticInitializer()) {
            return false;
        }

        Label skipTraceLabel = new Label();
        visitInvokeTraceGuardMethod("isEntryEnabled", skipTraceLabel);

        visitGetTraceObjectField();
        visitLoadMethodName();
        createTraceArrayForParameters();
        visitMethodInsn(
                        INVOKESTATIC,
                        TR_TYPE.getInternalName(),
                        "entry",
                        Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] {
                                                                             TRACE_COMPONENT_TYPE,
                                                                             Type.getType(String.class),
                                                                             Type.getType(Object.class) }));

        visitLabel(skipTraceLabel);
        return true;
    }

    @Override
    public boolean onMethodReturn() {
        if (isTrivial() || isAlreadyTraced()) {
            return false;
        }
        if (isStaticInitializer()) {
            return false;
        }

        Label skipTraceLabel = new Label();
        visitInvokeTraceGuardMethod("isEntryEnabled", skipTraceLabel);

        boolean traceValueOnStack = setupReturnObjectValueForExitTrace();
        if (traceValueOnStack) {
            visitGetTraceObjectField();
            visitInsn(SWAP);
            visitLoadMethodName();
            visitInsn(SWAP);
            visitMethodInsn(
                            INVOKESTATIC,
                            TR_TYPE.getInternalName(),
                            "exit",
                            Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] {
                                                                                 TRACE_COMPONENT_TYPE,
                                                                                 Type.getType(String.class),
                                                                                 Type.getType(Object.class) }));
        } else {
            visitGetTraceObjectField();
            visitLoadMethodName();
            visitMethodInsn(
                            INVOKESTATIC,
                            TR_TYPE.getInternalName(),
                            "exit",
                            Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] {
                                                                                 TRACE_COMPONENT_TYPE,
                                                                                 Type.getType(String.class) }));
        }

        visitLabel(skipTraceLabel);
        return true;
    }

    @Override
    public boolean onThrowInstruction() {
        if (!getClassAdapter().isTraceExceptionOnThrow() || isAlreadyTraced()) {
            return false;
        }

        Label skipTraceLabel = new Label();
        visitInvokeTraceGuardMethod("isDebugEnabled", skipTraceLabel);

        // The trace will eat the exception the top of the stack so we'll need
        // to duplicate the reference to be sure it still exists after the trace.
        visitInsn(DUP);

        visitGetTraceObjectField();
        visitInsn(SWAP);
        visitLdcInsn(getMethodName() + " is rasing exception");
        visitInsn(SWAP);
        visitMethodInsn(
                        INVOKESTATIC,
                        TR_TYPE.getInternalName(),
                        "debug",
                        Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] {
                                                                             TRACE_COMPONENT_TYPE,
                                                                             Type.getType(String.class),
                                                                             Type.getType(Object.class) }));

        visitLabel(skipTraceLabel);
        return true;
    }

    @Override
    public boolean onExceptionHandlerEntry(Type exception, int var) {
        if (!getClassAdapter().isTraceExceptionOnHandling() || isAlreadyTraced()) {
            return false;
        }

        Label skipTraceLabel = new Label();
        visitInvokeTraceGuardMethod("isDebugEnabled", skipTraceLabel);

        if (var == -1) {
            // The trace will eat the exception the top of the stack so we'll need
            // to duplicate the reference to be sure it still exists after the trace.
            visitInsn(DUP);
        } else {
            visitVarInsn(ALOAD, var);
        }

        visitGetTraceObjectField();
        visitInsn(SWAP);
        visitLdcInsn(getMethodName() + " is handling exception");
        visitInsn(SWAP);
        visitMethodInsn(
                        INVOKESTATIC,
                        TR_TYPE.getInternalName(),
                        "debug",
                        Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] {
                                                                             TRACE_COMPONENT_TYPE,
                                                                             Type.getType(String.class),
                                                                             Type.getType(Object.class) }));

        visitLabel(skipTraceLabel);
        return true;
    }

    @Override
    public void initializeTraceObjectField() {
        if (!getClassAdapter().isTraceObjectFieldInitializationRequired() || isAlreadyTraced()) {
            return;
        }

        TraceOptionsData traceOptionsData = getClassAdapter().getTraceOptionsData();
        visitGetClassForType(Type.getObjectType(getClassAdapter().getClassInternalName()));

        List<String> traceGroups = traceOptionsData.getTraceGroups();
        String traceGroupName = traceGroups.isEmpty() ? null : traceGroups.get(0);
        if (traceGroupName != null) {
            visitLdcInsn(traceGroupName);
        } else {
            visitInsn(ACONST_NULL);
        }

        String messageBundle = traceOptionsData.getMessageBundle();
        if (messageBundle != null) {
            visitLdcInsn(messageBundle);
        } else {
            visitInsn(ACONST_NULL);
        }
        visitMethodInsn(
                        INVOKESTATIC,
                        TR_TYPE.getInternalName(),
                        "register",
                        Type.getMethodDescriptor(TRACE_COMPONENT_TYPE, new Type[] {
                                                                                   Type.getType(Class.class),
                                                                                   Type.getType(String.class),
                                                                                   Type.getType(String.class) }));

        visitSetTraceObjectField();
    }

    private void visitInvokeTraceGuardMethod(String guardMethodName, Label skipTraceLabel) {
        visitMethodInsn(INVOKESTATIC, TRACE_COMPONENT_TYPE.getInternalName(), "isAnyTracingEnabled", "()Z");
        visitJumpInsn(IFEQ, skipTraceLabel);

        visitGetTraceObjectField();
        visitJumpInsn(IFNULL, skipTraceLabel);

        visitGetTraceObjectField();
        visitMethodInsn(INVOKEVIRTUAL, TRACE_COMPONENT_TYPE.getInternalName(), guardMethodName, "()Z");
        visitJumpInsn(IFEQ, skipTraceLabel);
    }
}
