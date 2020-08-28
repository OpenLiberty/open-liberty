/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi.stackjoiner.bci;

import java.io.PrintStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Injects all Throwable.printStackTrace(PrintStream) calls with a decision to override
 * itself to squash stack traces into one line.
 */
class ThrowableMethodAdapter extends MethodVisitor implements Opcodes {

    private final String name;
    private final String desc;
    private final String signature;

    public ThrowableMethodAdapter(MethodVisitor mv, String name, String desc, String signature) {
        super(ASM8, mv);
        this.name = name;
        this.desc = desc;
        this.signature = signature;
    }
    
    @Override
    public void visitCode() {
    	if (name.equals("printStackTrace") && desc.equals("(Ljava/io/PrintStream;)V")) {
            mv.visitVarInsn(ALOAD, 0); // Loads 'this' (Throwable) onto the stack.
            mv.visitVarInsn(ALOAD, 1); // Loads the first param (PrintStream) onto the stack.
            // Call ThrowableProxy.fireMethod(Throwable, PrintStream) which invokes
            // BaseTraceService.printStackTraceOverride(Throwable, PrintStream) via reflection.
            // This method call consumes/pops the two objects off of the stack.
            mv.visitMethodInsn(
                               INVOKESTATIC,
                               "com/ibm/ws/boot/delegated/logging/ThrowableProxy",
                               "fireMethod",
                               Type.getMethodDescriptor(
                                                        Type.BOOLEAN_TYPE,
                                                        new Type[] { Type.getType(Throwable.class), Type.getType(PrintStream.class) }),
                               false);  // Pushes the Boolean value returned by BaseTraceService.printStackTraceOverride(...) onto the stack
            Label l0 = new Label();     // Generate a reference to Label L0.
            mv.visitJumpInsn(IFEQ, l0); // POP top of the stack, IF value on the stack is 0 (false), JUMP to Label L0.
            mv.visitInsn(RETURN);       // ELSE trigger a RETURN.
            mv.visitLabel(l0);          // Start of Label L0.
        }
    }
} 
