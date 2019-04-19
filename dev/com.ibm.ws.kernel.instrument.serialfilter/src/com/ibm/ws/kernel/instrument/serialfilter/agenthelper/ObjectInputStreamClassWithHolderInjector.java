/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.kernel.instrument.serialfilter.agenthelper;

import org.objectweb.asm.*;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import static org.objectweb.asm.Opcodes.*;

import com.ibm.ws.kernel.instrument.serialfilter.store.Holder;

/**
 * This class modifies the bytes for the ObjectInputStream class
 * to add a ClassValidator field, assign an instance to that field,
 * and invoke the validate() method on that field with the result of
 * any call to resolveClass().
 */
public class ObjectInputStreamClassWithHolderInjector extends ClassVisitor {

    ObjectInputStreamClassWithHolderInjector(ClassVisitor cv) {
        super(Opcodes.ASM7, cv);
    }
    public static Map<?, ?> getConfigMapFromHolder() {
        final boolean debugEnabled = PreMainUtil.isDebugEnabled();
        try {
            // use forName in order to load the class by using bootstrap classloader.
            // Otherwise, the context classloader will load the class which is different
            // object than the one which was loaded by the bootstrap classloader.
            final Class<?> holder = Class.forName(Holder.CLASS_PATH_NAME, true, null);
            return AccessController.doPrivileged(new PrivilegedAction<Map<?, ?>>() {
                @Override
                public Map<?, ?> run() {
                    try {
                        Field factoryField = holder.getDeclaredField(Holder.FACTORY_FIELD);
                        factoryField.setAccessible(true);
                        return (Map<?, ?>) factoryField.get(null);
                    } catch (NoSuchFieldException e) {
                        if (debugEnabled) {
                            System.out.println(Holder.FACTORY_FIELD + " does not exist.");
                        }
                        throw new Error("FieldsHolder class has not been initialized as expected.", e);
                    } catch (IllegalAccessException e) {
                        if (debugEnabled) {
                            System.out.println("Could not access " + Holder.FACTORY_FIELD);
                        }
                        throw new Error(e);
                    } catch (SecurityException e) {
                        if (debugEnabled) {
                            System.out.println("Could not access field " + Holder.FACTORY_FIELD +". Ensure the code is suitably privileged.");
                        }
                        throw new Error(e);
                    }
                }
            });
        } catch (ClassNotFoundException cnfe) {
            if (debugEnabled) {
                System.out.println("Holder class is not found.");
            }
            throw new Error("Holder class is not found.", cnfe);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // visitMethod() should be called immediately after all the visitField() calls
        // Add the field the first time around.
        if(PreMainUtil.isDebugEnabled()) {
            System.out.println("Entering Retransform visitMethod : " + name);
        }

        // create the default MethodVisitor
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if ("<init>".equals(name))
            return new MakeConstructorAssignFieldBeforeReturning(mv);
        if (access == ACC_PUBLIC
                && "()Ljava/lang/Object;".equals(desc)
                && ("readObject".equals(name) || "readUnshared".equals(name))) {
            return new MakeMethodCallValidatorReset(mv);
        }
        return new MakeMethodValidateResultOfResolveClass(mv);
    }

    private final class MakeConstructorAssignFieldBeforeReturning extends MethodVisitor {
        MakeConstructorAssignFieldBeforeReturning(MethodVisitor mv) {
            super(ASM7, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if (RETURN == opcode) {
                // inject code equivalent to this:
                //     Object validators = FieldsHolder.serializationValidatorFactory.get(this);
                //     FieldsHolder.serializationValidator.put(this, validators);

                // stack = []
                mv.visitFieldInsn(GETSTATIC, Holder.CLASS_NAME, Holder.VALIDATOR_FIELD, Holder.VALIDATOR_DESC);
                // stack = [validatorMap]
                mv.visitVarInsn(ALOAD, 0);
                // stack = [validatorMap, this]                
                mv.visitFieldInsn(GETSTATIC, Holder.CLASS_NAME, Holder.FACTORY_FIELD, Holder.FACTORY_DESC);
                // stack = [validatorMap, this, factory]
                mv.visitVarInsn(ALOAD, 0);
                // stack = [validatorMap, this, factory, this]
                // INVOKE factory.get(this), POP 2 args and PUSH result
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                // stack = [validatorMap, this, validators]
                // cast to Map, stack unchanged
                mv.visitTypeInsn(CHECKCAST, "java/util/Map");                
                // validatorMap.put(this, validators), POP 2 args and PUSH result
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                // stack = [result]
                // POP
                mv.visitInsn(POP);
                // stack = []
            }
            super.visitInsn(opcode);
        }
    }

    private class MakeMethodValidateResultOfResolveClass extends MethodVisitor {
        MakeMethodValidateResultOfResolveClass(MethodVisitor mv) {
            super(ASM7, mv);
        }

        @Override
        public final void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (INVOKEVIRTUAL == opcode) {
                // inject code equivalent to this:
                //     Object validators = FieldsHolder.serializationValidator.get(this);
                //     invoke validators

                if ("java/io/ObjectInputStream".equals(owner)) {
                    if ("resolveClass".equals(name)) {
                        // stack = [..., clazz]
                        mv.visitFieldInsn(GETSTATIC, Holder.CLASS_NAME, Holder.VALIDATOR_FIELD, Holder.VALIDATOR_DESC);
                        // stack = [..., clazz, validatorMap]
                        mv.visitVarInsn(ALOAD, 0);
                        // stack = [..., clazz, validatorMap, this]
                        // INVOKE validatorMap.get(this), POP 2 args and PUSH result
                        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                        // stack = [..., clazz, validators]
                        // cast to Map, stack unchanged
                        mv.visitTypeInsn(CHECKCAST, "java/util/Map");                                        
                        mv.visitInsn(SWAP);
                        // stack = [..., validators, clazz]
                        // INVOKE validators.get(clazz), POP 2 args, PUSH result (also a class)
                        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                        // stack = [..., (Object)clazz]
                        // cast to class explicitly, or we get a stack shape inconsistency
                        mv.visitTypeInsn(CHECKCAST, "java/lang/Class");
                        // stack = [..., (Class)clazz]
                    }
                }
            }
        }
    }

    private final class MakeMethodCallValidatorReset extends MakeMethodValidateResultOfResolveClass {
        MakeMethodCallValidatorReset(MethodVisitor mv) { super(mv); }

        public final void visitCode() {
            super.visitCode();
            invokeResetOnValidator();
        }

        @Override
        public final void visitInsn(int opcode) {
            if (ARETURN == opcode) {
                invokeResetOnValidator();
            }
            super.visitInsn(opcode);
        }

        private void invokeResetOnValidator() {
            // stack = []
            mv.visitFieldInsn(GETSTATIC, Holder.CLASS_NAME, Holder.VALIDATOR_FIELD, Holder.VALIDATOR_DESC);
            // stack = [validatorMap]
            mv.visitVarInsn(ALOAD, 0);
            // stack = [validatorMap, this]
            // INVOKE validatorMap.remove(this), POP 2 args and PUSH result
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
            // stack = [validators]
            // cast to Map, stack unchanged
            mv.visitTypeInsn(CHECKCAST, "java/util/Map");                
            // clear validators object.
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "clear", "()V", true);
            // stack = []
        }
    }
}