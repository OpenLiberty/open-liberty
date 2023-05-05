/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.checkpoint.internal;

import static io.openliberty.checkpoint.internal.CheckpointImpl.debug;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.asm.ASMHelper;
import io.openliberty.checkpoint.internal.criu.DeployCheckpoint;

/**
 *
 */
public class CheckpointTransformer implements ClassFileTransformer {
    private static final TraceComponent tc = Tr.register(CheckpointTransformer.class);
    private static final String CLASS_DEPLOY_CHECKPOINT_NAME = DeployCheckpoint.class.getName();
    private static final String CLASS_DEPLOY_CHECKPOINT_PATH = CLASS_DEPLOY_CHECKPOINT_NAME.replace('.', '/');

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        debug(tc, () -> "transforming class" + className);
        if (noClassFound(loader)) {
            // cannot load the DeployCheckpoint class; do no transformation
            return null;
        }
        ClassReader cr = new ClassReader(classfileBuffer);
        ClassWriter cw = new ClassWriter(cr, ASMHelper.getCurrentASM());
        ClassVisitor cv = new ClassVisitor(ASMHelper.getCurrentASM(), cw) {
            boolean visitedStaticBlock = false;
            private boolean isStaticClass;
            private boolean isInterface;

            class StaticBlockMethodVisitor extends MethodVisitor {
                StaticBlockMethodVisitor(MethodVisitor mv) {
                    super(ASMHelper.getCurrentASM(), mv);
                }

                @Override
                public void visitCode() {
                    super.visitCode();
                    mv.visitMethodInsn(
                                       INVOKESTATIC,
                                       CLASS_DEPLOY_CHECKPOINT_PATH,
                                       "checkpoint",
                                       Type.getMethodDescriptor(Type.VOID_TYPE), false);

                }

            }

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                this.isStaticClass = (access & ACC_STATIC) != 0;
                this.isInterface = (access & ACC_INTERFACE) != 0;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                boolean isStaticMethod = (access & ACC_STATIC) != 0;
                if (!isStaticClass && !isInterface && isStaticMethod && "<clinit>".equals(name)) {
                    visitedStaticBlock = true;
                    mv = new StaticBlockMethodVisitor(mv);
                }
                return mv;
            }

            @Override
            public void visitEnd() {
                if (!isStaticClass && !isInterface && !visitedStaticBlock) {
                    MethodVisitor mv = visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
                    mv.visitCode();
                    mv.visitInsn(RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }
                super.visitEnd();
            }
        };

        cr.accept(cv, 0);

        return cw.toByteArray();
    }

    @FFDCIgnore(ClassNotFoundException.class)
    private boolean noClassFound(ClassLoader loader) {
        try {
            loader.loadClass(CLASS_DEPLOY_CHECKPOINT_NAME);
        } catch (ClassNotFoundException e) {
            return true;
        }
        return false;
    }
}
