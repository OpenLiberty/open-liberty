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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

public class JSR47TracingMethodAdapter extends AbstractRasMethodAdapter<AbstractTracingRasClassAdapter> {

    public JSR47TracingMethodAdapter(AbstractTracingRasClassAdapter classAdapter, MethodVisitor visitor, int access, String methodName, String descriptor, String signature,
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
        visitInvokeTraceGuardMethod("isLoggable", "FINER", skipTraceLabel);

        visitGetTraceObjectField();
        visitLoadClassName();
        visitLoadMethodName();
        createTraceArrayForParameters();
        visitMethodInsn(
                        INVOKEVIRTUAL,
                        "java/util/logging/Logger",
                        "entering",
                        "(Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V");

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
        visitInvokeTraceGuardMethod("isLoggable", "FINER", skipTraceLabel);

        boolean traceValueOnStack = setupReturnObjectValueForExitTrace();
        if (traceValueOnStack) {
            visitGetTraceObjectField();
            visitInsn(SWAP);
            visitLoadClassName();
            visitInsn(SWAP);
            visitLoadMethodName();
            visitInsn(SWAP);
            visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/util/logging/Logger",
                            "exiting",
                            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V");
        } else {
            visitGetTraceObjectField();
            visitLoadClassName();
            visitLoadMethodName();
            visitMethodInsn(
                            INVOKEVIRTUAL,
                            "java/util/logging/Logger",
                            "exiting",
                            "(Ljava/lang/String;Ljava/lang/String;)V");
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
        visitInvokeTraceGuardMethod("isLoggable", "FINER", skipTraceLabel);

        // The trace will eat the exception the top of the stack so we'll need
        // to duplicate the reference to be sure it still exists after the trace.
        visitInsn(DUP);

        visitGetTraceObjectField();
        visitInsn(SWAP);
        visitLoadClassName();
        visitInsn(SWAP);
        visitLoadMethodName();
        visitInsn(SWAP);
        visitMethodInsn(
                        INVOKEVIRTUAL,
                        "java/util/logging/Logger",
                        "throwing",
                        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V");

        visitLabel(skipTraceLabel);
        return true;
    }

    @Override
    public boolean onExceptionHandlerEntry(Type exception, int var) {
        if (!getClassAdapter().isTraceExceptionOnHandling() || isAlreadyTraced()) {
            return false;
        }

        Label skipTraceLabel = new Label();
        visitInvokeTraceGuardMethod("isLoggable", "FINER", skipTraceLabel);

        if (var == -1) {
            // The trace will eat the exception the top of the stack so we'll need
            // to duplicate the reference to be sure it still exists after the trace.
            visitInsn(DUP);
        } else {
            visitVarInsn(ALOAD, var);
        }

        visitGetTraceObjectField();
        visitInsn(SWAP);
        visitGetLoggingLevel("FINER");
        visitInsn(SWAP);
        visitLoadClassName();
        visitInsn(SWAP);
        visitLoadMethodName();
        visitInsn(SWAP);
        visitLdcInsn("HANDLING");
        visitInsn(SWAP);
        visitMethodInsn(
                        INVOKEVIRTUAL,
                        "java/util/logging/Logger",
                        "logp",
                        "(Ljava/util/logging/Level;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V");

        visitLabel(skipTraceLabel);
        return true;
    }

    @Override
    public void initializeTraceObjectField() {
        if (!getClassAdapter().isTraceObjectFieldInitializationRequired() || isAlreadyTraced()) {
            return;
        }

        visitLoadClassName();
        visitMethodInsn(
                        INVOKESTATIC,
                        "java/util/logging/Logger",
                        "getLogger",
                        "(Ljava/lang/String;)Ljava/util/logging/Logger;");
        visitSetTraceObjectField();

        // Liberty doesn't currently have the WsLogger or LoggerHelper implementations
        // List<String> traceGroups = getClassAdapter().getTraceOptionsData().getTraceGroups();
        // String traceGroupName = traceGroups.isEmpty() ? null : traceGroups.get(0);
        // if (traceGroupName != null) {
        // // WebSphere extensions may blow up in pure J2SE environment so
        // // setup a try/catch block around our attempt to register with a
        // // trace group
        // Label tryStartLabel = new Label();
        // Label tryEndLabel = new Label();
        // Label handlerLabel = new Label();
        // Label postHandlerLabel = new Label();
        // visitTryCatchBlock(tryStartLabel, tryEndLabel, handlerLabel, "java/lang/Throwable");
        // visitLabel(tryStartLabel);
        //
        // // Logger must be a WsLogger to add to a trace group.
        // visitGetTraceObjectField();
        // visitTypeInsn(INSTANCEOF, "com/ibm/ws/logging/WsLogger");
        // visitJumpInsn(IFEQ, postHandlerLabel);
        //
        // visitGetTraceObjectField();
        // visitLdcInsn(traceGroupName);
        // visitMethodInsn(
        // INVOKESTATIC,
        // "com/ibm/ws/logging/LoggerHelper",
        // "addLoggerToGroup",
        // "(Ljava/util/logging/Logger;Ljava/lang/String;)V");
        // visitLabel(tryEndLabel);
        // visitJumpInsn(GOTO, postHandlerLabel);
        //
        // visitLabel(handlerLabel);
        // visitFrame(F_SAME1, 0, null, 1, new Object[] { "java/lang/Throwable" });
        // visitInsn(POP);
        //
        // visitLabel(postHandlerLabel);
        // visitFrame(F_SAME, 0, null, 0, null);
        // }
    }

    private void visitGetLoggingLevel(String levelName) {
        visitFieldInsn(GETSTATIC, "java/util/logging/Level", levelName, "Ljava/util/logging/Level;");
    }

    private void visitInvokeTraceGuardMethod(String guardMethodName, String levelName, Label skipTraceLabel) {
        visitGetTraceObjectField();
        visitJumpInsn(IFNULL, skipTraceLabel);

        visitGetTraceObjectField();
        visitGetLoggingLevel(levelName);
        visitMethodInsn(INVOKEVIRTUAL, "java/util/logging/Logger", "isLoggable", "(Ljava/util/logging/Level;)Z");
        visitJumpInsn(IFEQ, skipTraceLabel);
    }
}
