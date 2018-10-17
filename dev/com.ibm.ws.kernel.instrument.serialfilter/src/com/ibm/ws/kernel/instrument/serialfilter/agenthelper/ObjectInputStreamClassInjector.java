/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.objectweb.asm.Opcodes.*;

/**
 * This class modifies the bytes for the ObjectInputStream class
 * to add a ClassValidator field, assign an instance to that field,
 * and invoke the validate() method on that field with the result of
 * any call to resolveClass().
 */
public class ObjectInputStreamClassInjector extends ClassVisitor {
    /**
     * This field name corresponds to a field already provided in recent IBM Java class libraries.
     * If it is changed, the logic for discovering the pre-enabled ObjectInputStream classes in those
     * libraries will fail, and additional, unnecessary class transformation will happen.
     * On IBM Java 6 and 7 these transformations will fail.
     */
    private static final String FACTORY_FIELD = "serializationValidatorFactory";
    private static final String FACTORY_DESC = "Ljava/util/Map;";
    private static final String FACTORY_SIG = "Ljava/util/Map<Ljava/io/ObjectInputStream;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/Object;>;>;";
    private static final int FACTORY_ACCESS = ACC_PUBLIC + ACC_FINAL + ACC_STATIC;

    /**
     * This field name corresponds to a field already provided in recent IBM Java class libraries.
     * @see #FACTORY_FIELD for more details
     */
    private static final String VALIDATOR_FIELD = "serializationValidator";
    private static final String VALIDATOR_DESC = "Ljava/util/Map;";
    private static final String VALIDATOR_SIG = "Ljava/util/Map<Ljava/lang/Class<*>;Ljava/lang/Class<*>;>;";
    private static final int VALIDATOR_ACCESS = ACC_PRIVATE + ACC_VOLATILE;

    private boolean validatorFieldAdded = false;
    private String currentClassName;

    public static Map<?, ?> getConfigMap(){
        final Class<?> oisc = ObjectInputStream.class;
        return AccessController.doPrivileged(new PrivilegedAction<Map<?, ?>>() {
            @Override
            public Map<?, ?> run() {
                try {
                    Field factoryField = oisc.getDeclaredField(FACTORY_FIELD);
                    factoryField.setAccessible(true);
                    return (Map<?, ?>) factoryField.get(null);
                } catch (NoSuchFieldException e) {
                    if (PreMainUtil.isDebugEnabled()) {
                        System.out.println("Unable to locate field " + oisc.getName() + "." + FACTORY_FIELD + ".");
                    }
                    throw new Error("ObjectInputStream class has not been modified as expected.", e);
                } catch (IllegalAccessException e) {
                    if (PreMainUtil.isDebugEnabled()) {
                        System.out.println("Could not access field " + oisc.getName() + "." + FACTORY_FIELD + ".");
                    }
                    throw new Error(e);
                } catch (SecurityException e) {
                    if (PreMainUtil.isDebugEnabled()) {
                        System.out.println("Could not access field " + oisc.getName() + "." + FACTORY_FIELD + ". Ensure the code is suitably privileged.");
                    }
                    throw new Error(e);
                }
            }
        });
    }

    /**
     * Some IBM JVMs already support this feature. This method will check for that support
     * <em>without loading ObjectInputStream.class</em>.
     * @return true iff the support was detected
     */
    public static boolean injectionNeeded() {
        // first check whether we need to modify ObjectInputStream
        boolean debugEnabled = PreMainUtil.isDebugEnabled();
        InputStream is = String.class.getResourceAsStream("/java/io/ObjectInputStream.class");
        if (is == null) {
            if (debugEnabled) {
                System.out.println("Could not locate /java/io/ObjectInputStream.class as a resource");
            }
        } else {
            try {
                ClassReader cr = new ClassReader(is);
                final Set<String> fieldsToLookFor = new HashSet<String>(asList(FACTORY_FIELD, VALIDATOR_FIELD));
                if (debugEnabled) {
                    System.out.println("Searching ObjectInputStream.class bytes for fields: " + fieldsToLookFor);
                }
                cr.accept(new ClassVisitor(Opcodes.ASM7) {
                    @Override
                    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
                        if (PreMainUtil.isDebugEnabled()) {
                            System.out.println("Found field '" + name + "' with description '" + desc + "'");
                        }
                        fieldsToLookFor.remove(name);
                        return null;
                    }
                }, 0);
                if (fieldsToLookFor.isEmpty()) {
                    if (debugEnabled) {
                        System.out.println("Found all fields already in ObjectInputStream.class");
                    }
                    return false;
                }
            } catch (IOException e) {
                if (debugEnabled) {
                    System.out.println("Could not read /java/io/ObjectInputStream.class as a resource");
                }
            }
        }
        return true;
    }

    ObjectInputStreamClassInjector(ClassVisitor cv) {
        super(Opcodes.ASM7, cv);
    }

    private void addFieldsOnlyOnce() {
        if (validatorFieldAdded) return;
        // this should really be a final field,
        // but the once-only initialization is too hard to ensure
        // so make it volatile instead to ensure thread propagation
        // and initialize it from every constructor
        cv.visitField(FACTORY_ACCESS, FACTORY_FIELD, FACTORY_DESC, FACTORY_SIG, null).visitEnd();
        cv.visitField(VALIDATOR_ACCESS, VALIDATOR_FIELD, VALIDATOR_DESC, VALIDATOR_SIG, null).visitEnd();
        validatorFieldAdded = true;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.currentClassName = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // visitMethod() should be called immediately after all the visitField() calls
        // Add the field the first time around.
        addFieldsOnlyOnce();

        // create the default MethodVisitor
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

        if ("<clinit>".equals(name))
            return new MakeStaticInitializerAssignField(mv);
        if ("<init>".equals(name))
            return new MakeConstructorAssignFieldBeforeReturning(mv);
        if (access == ACC_PUBLIC
                && "()Ljava/lang/Object;".equals(desc)
                && ("readObject".equals(name) || "readUnshared".equals(name))) {
            return new MakeMethodCallValidatorReset(mv);
        }
        return new MakeMethodValidateResultOfResolveClass(mv);
    }

    public static boolean hasModified(Class<?> clazz) {
        try {
            return clazz.getDeclaredField(VALIDATOR_FIELD) != null;
        } catch (NoSuchFieldException e) {
            return false;
        }

    }

    private final class MakeStaticInitializerAssignField extends MethodVisitor {
        MakeStaticInitializerAssignField(MethodVisitor mv) {
            super(ASM7, mv);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            /*
             * Note: this invocation of System.getProperties() would normally require a doPriv block.
             * However, since this agent forces the initialization of ObjectInputStream directly from
             * the agent's pre-main, no application code will be on the call stack, and no security
             * manager will be installed yet anyway.
             */
            // stack = []
            // INVOKE System.getProperties(), PUSH result
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperties", "()Ljava/util/Properties;", false);
            // stack = [properties]
            mv.visitLdcInsn(PreMainUtil.FACTORY_INIT_PROPERTY);
            // stack = [properties, propname]
            // INVOKE Properties.get(propname), POP 2 args, PUSH result
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/Properties", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
            // stack = [factory]
            mv.visitTypeInsn(CHECKCAST, "java/util/Map");
            // stack = [factory]
            // PUT STATIC field, POP 1 arg
            mv.visitFieldInsn(PUTSTATIC, currentClassName, FACTORY_FIELD, FACTORY_DESC);
            // stack = []
        }
    }

    private final class MakeConstructorAssignFieldBeforeReturning extends MethodVisitor {
        MakeConstructorAssignFieldBeforeReturning(MethodVisitor mv) {
            super(ASM7, mv);
        }

        @Override
        public void visitInsn(int opcode) {
            if (RETURN == opcode) {
                // inject code equivalent to this:
                //     this.validators = (Map)ObjectInputStream.factory.get(this);

                // stack = []
                mv.visitVarInsn(ALOAD, 0);
                // stack = [this]
                mv.visitFieldInsn(GETSTATIC, currentClassName, FACTORY_FIELD, FACTORY_DESC);
                // stack = [this, factory]
                mv.visitVarInsn(ALOAD, 0);
                // stack = [this, factory, this]
                // INVOKE factory.get(this), POP 2 args and PUSH result
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                // stack = [this, validators]

                // cast to Map, stack unchanged
                mv.visitTypeInsn(CHECKCAST, "java/util/Map");
                // stack = [this, validators]
                // this.validators = validators, POP 2 args
                mv.visitFieldInsn(PUTFIELD, currentClassName, VALIDATOR_FIELD, VALIDATOR_DESC);
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
                if ("java/io/ObjectInputStream".equals(owner)) {
                    if ("resolveClass".equals(name)) {
                        // stack = [..., clazz]
                        mv.visitVarInsn(ALOAD, 0);
                        // stack = [..., clazz, this]
                        mv.visitFieldInsn(GETFIELD, currentClassName, VALIDATOR_FIELD, VALIDATOR_DESC);
                        // stack = [..., clazz, validators]
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
            mv.visitVarInsn(ALOAD, 0);
            // stack = [this]
            mv.visitFieldInsn(GETFIELD, currentClassName, VALIDATOR_FIELD, VALIDATOR_DESC);
            // stack = [validators]
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "clear", "()V", true);
            // stack = []
        }
    }
}
