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

import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_5;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.ibm.ws.monitor.internal.MonitoringProxyActivator;

/**
 * Class adapter to hook the class static initializer to call the monitoring
 * subsystem with a reference to the initialized {@code Class} instance.
 */
public class ClassAvailableHookClassAdapter extends ClassVisitor {

    Type classType;
    boolean supportsClassLiterals;
    boolean isStaticClass;
    boolean isInterface;
    boolean hookedStaticInit = false;

    /**
     * Create a new class adapter to hook the static initializer.
     * 
     * @param delegate the class adapter to delegate to
     */
    public ClassAvailableHookClassAdapter(ClassVisitor delegate) {
        super(Opcodes.ASM8, delegate);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.classType = Type.getObjectType(name);
        this.supportsClassLiterals = (version & 0xFFFF) >= (V1_5 & 0xFFFF);
        this.isStaticClass = (access & ACC_STATIC) != 0;
        this.isInterface = (access & ACC_INTERFACE) != 0;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        boolean isStaticMethod = (access & ACC_STATIC) != 0;
        if (!isStaticClass && !isInterface && isStaticMethod && "<clinit>".equals(name)) {
            hookedStaticInit = true;
            mv = new ProcessCandidateHookMethodAdapter(mv, classType, supportsClassLiterals);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        if (!isStaticClass && !isInterface && !hookedStaticInit) {
            MethodVisitor mv = visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
        }
        super.visitEnd();
    }
}

/**
 * Method adapter that injects code to call the monitoring system with
 * the intialized class instance.
 */
class ProcessCandidateHookMethodAdapter extends MethodVisitor {

    Type classType;
    boolean supportsClassLiterals;

    protected ProcessCandidateHookMethodAdapter(MethodVisitor visitor, Type classType, boolean supportsClassLiterals) {
        super(Opcodes.ASM8, visitor);
        this.classType = classType;
        this.supportsClassLiterals = supportsClassLiterals;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        if (supportsClassLiterals) {
            visitLdcInsn(classType);
        } else {
            mv.visitLdcInsn(classType.getClassName());
            mv.visitMethodInsn(
                               INVOKESTATIC,
                               Type.getInternalName(Class.class),
                               "forName",
                               Type.getMethodDescriptor(Type.getType(Class.class), new Type[] { Type.getType(String.class) }), false);
        }

        mv.visitMethodInsn(
                           INVOKESTATIC,
                           MonitoringProxyActivator.CLASS_AVAILABLE_PROXY_CLASS_INTERNAL_NAME,
                           "classAvailable",
                           Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { Type.getType(Class.class) }), false);
    }
}
