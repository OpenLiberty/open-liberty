/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.ibm.ejs.container.util.MethodAttribUtils;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Metadata obtained by scanning the bytecode of an EJB implementation class.
 */
public class ByteCodeMetaData extends ClassVisitor {
    private static final String CLASS_NAME = ByteCodeMetaData.class.getName();
    static final TraceComponent tc = Tr.register(ByteCodeMetaData.class, "EJBContainer", "com.ibm.ejs.container.container");
    static final TraceComponent tcMetaData = Tr.register(ByteCodeMetaData.class, "MetaData", "com.ibm.ejs.container.container");

    private static ClassLoader bootClassLoader;

    /**
     * Return a non-null class loader that can be used to access classes and
     * resources from the boot class loader.
     */
    // d742751
    private static synchronized ClassLoader getBootClassLoader() {
        if (bootClassLoader == null) {
            bootClassLoader = new ClassLoader(null) {
                // Nothing.  We just need a class loader with the null (boot)
                // class loader as the parent for delegation.
            };
        }
        return bootClassLoader;
    }

    /**
     * The implementation class for which this object contains metadata.
     */
    private final Class<?> ivClass;

    /**
     * All public methods in the class hierarchy of {@link #ivClass}.
     */
    private final Method[] ivPublicMethods;

    /**
     * Lazily initialized map of {@link #getNonPrivateMethodKey} for all methods
     * in {@link #ivPublicMethods}.
     */
    private Map<String, Method> ivPublicMethodMap;

    /**
     * True if the class hierarchy has already been scanned.
     */
    private boolean ivScanned;

    /**
     * The exception that occurred during scanning, or null.
     */
    private Throwable ivScanException;

    /**
     * The class currently being scanned.
     */
    private String ivCurrentClassName;

    /**
     * Map of method name to MethodMetaData for private methods in the class
     * currently being scanned.
     */
    private Map<String, MethodMetaData> ivCurrentPrivateMethodMetaData;

    /**
     * The method name currently being scanned.
     */
    private String ivCurrentMethodName;

    /**
     * The method description currently being scanned.
     */
    private String ivCurrentMethodDesc;

    /**
     * Map of getNonPrivateMethodKey to non-bridge non-private method metadata.
     */
    private final Map<String, MethodMetaData> ivNonPrivateMethodMetaData = new HashMap<String, MethodMetaData>();

    /**
     * Map of superclass to Map of method names to private methods defined in
     * that superclass.
     */
    private final Map<String, Map<String, MethodMetaData>> ivPrivateMethodMetaData = new HashMap<String, Map<String, MethodMetaData>>();

    /**
     * Lazily initialized map of getNonPrivateMethodKey to bridge method
     * metadata.
     */
    private Map<String, BridgeMethodMetaData> ivBridgeMethodMetaData;

    /**
     * @param implClass the implementation class
     * @param publicMethods all public methods on the implementation class
     */
    ByteCodeMetaData(Class<?> implClass, Method[] publicMethods) {
        super(Opcodes.ASM8);
        ivClass = implClass;
        ivPublicMethods = publicMethods;
    }

    /**
     * Scan the bytecode of all classes in the hierarchy unless already done.
     */
    private void scan() {
        if (ivScanned) {
            return;
        }

        ivScanned = true;

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        for (Class<?> klass = ivClass; klass != null && klass != Object.class; klass = klass.getSuperclass()) {
            if (isTraceOn && (tc.isDebugEnabled() || tcMetaData.isDebugEnabled()))
                Tr.debug(tc.isDebugEnabled() ? tc : tcMetaData,
                         "scanning " + klass.getName());

            ivCurrentClassName = klass.getName();
            ivCurrentPrivateMethodMetaData = null;

            ClassLoader classLoader = klass.getClassLoader();
            if (classLoader == null) {
                classLoader = getBootClassLoader(); // d742751
            }

            String resourceName = klass.getName().replace('.', '/') + ".class";
            InputStream input = classLoader.getResourceAsStream(resourceName);

            if (input == null) // d728537
            {
                if (isTraceOn && (tc.isDebugEnabled() || tcMetaData.isDebugEnabled()))
                    Tr.debug(tc.isDebugEnabled() ? tc : tcMetaData,
                             "failed to find " + resourceName + " from " + classLoader);

                ivScanException = new FileNotFoundException(resourceName);
                return;
            }

            try {
                ClassReader classReader = new ClassReader(input);
                classReader.accept(this, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }
            // If the class is malformed, ASM might throw any exception.    d728537
            catch (Throwable t) {
                FFDCFilter.processException(t, CLASS_NAME + ".scan", "168", this,
                                            new Object[] { resourceName, klass, classLoader });

                if (isTraceOn && (tc.isDebugEnabled() || tcMetaData.isDebugEnabled()))
                    Tr.debug(tc.isDebugEnabled() ? tc : tcMetaData,
                             "scan exception", t);

                ivScanException = t;
                return;
            } finally {
                try {
                    input.close();
                } catch (IOException ex) {
                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "error closing input stream", ex);
                }
            }
        }
    }

    /**
     * @return the exception that occurred during scanning, or null
     */
    public Throwable getScanException() {
        return ivScanException;
    }

    /**
     * Get the target method of a bridge method, or null if not found
     *
     * @param method the bridge method (Modifiers.isBridge returns true)
     */
    public Method getBridgeMethodTarget(Method method) {
        scan();

        if (ivBridgeMethodMetaData == null) {
            return null;
        }

        BridgeMethodMetaData md = ivBridgeMethodMetaData.get(getNonPrivateMethodKey(method));
        return md == null ? null : md.ivTarget;
    }

    /**
     * @return the metadata for a non-bridge method.
     */
    public MethodMetaData getByteCodeMethodMetaData(Method method) {
        scan();

        if (Modifier.isPrivate(method.getModifiers())) {
            Map<String, MethodMetaData> map = ivPrivateMethodMetaData.get(method.getDeclaringClass().getName());
            return map == null ? null : map.get(method.getName());
        }

        return ivNonPrivateMethodMetaData.get(getNonPrivateMethodKey(method));
    }

    /**
     * @return the key for {@link #ivNonPrivateMethodMetaData}
     */
    private String getNonPrivateMethodKey(Method method) {
        return getNonPrivateMethodKey(method.getName(), MethodAttribUtils.jdiMethodSignature(method));
    }

    /**
     * @param methodName the method name
     * @param desc the method descriptor in JVM format; e.g.: (La/b/c;)V
     * @return the key for {@link #ivNonPrivateMethodMetaData}
     */
    private String getNonPrivateMethodKey(String methodName, String desc) {
        return methodName + desc;
    }

    /**
     * @param key the key returned by one of the getNonPrivateMethodKey methods
     * @return the public method from the implementation class
     */
    Method getPublicMethod(String key) {
        if (ivPublicMethodMap == null) {
            ivPublicMethodMap = new HashMap<String, Method>();
            for (Method method : ivPublicMethods) {
                ivPublicMethodMap.put(getNonPrivateMethodKey(method), method);
            }
        }

        return ivPublicMethodMap.get(key);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        ivCurrentMethodName = name;
        ivCurrentMethodDesc = desc;

        // Scan bridge methods to find their targets.
        if ((access & Opcodes.ACC_BRIDGE) != 0) {
            int flags = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;
            if ((access & flags) != flags) {
                if (TraceComponent.isAnyTracingEnabled() && tcMetaData.isDebugEnabled())
                    Tr.debug(tcMetaData, "visitMethod: " + name + desc + ": non-public/synthetic bridge method");
                return null;
            }

            String key = getNonPrivateMethodKey(name, desc);
            if (ivBridgeMethodMetaData == null) {
                ivBridgeMethodMetaData = new HashMap<String, BridgeMethodMetaData>();
            } else if (ivBridgeMethodMetaData.containsKey(key)) {
                if (TraceComponent.isAnyTracingEnabled() && tcMetaData.isDebugEnabled())
                    Tr.debug(tcMetaData, "visitMethod: " + name + desc + ": overridden bridge method");
                return null;
            }

            BridgeMethodMetaData md = new BridgeMethodMetaData();
            ivBridgeMethodMetaData.put(key, md);

            if (TraceComponent.isAnyTracingEnabled() && tcMetaData.isDebugEnabled())
                Tr.debug(tcMetaData, "visitMethod: " + name + desc + ": bridge method");
            return md;
        }

        // Non-public methods cannot be overridden, so process them separately.
        if ((access & Opcodes.ACC_PUBLIC) == 0) {
            if (ivCurrentPrivateMethodMetaData == null) {
                ivCurrentPrivateMethodMetaData = new HashMap<String, MethodMetaData>();
                ivPrivateMethodMetaData.put(ivCurrentClassName, ivCurrentPrivateMethodMetaData);
            }

            Map<String, MethodMetaData> map = ivPrivateMethodMetaData.get(ivCurrentClassName);
            if (map == null) {
                map = new HashMap<String, MethodMetaData>();
                ivPrivateMethodMetaData.put(ivCurrentClassName, map);
            }

            MethodMetaData md = new MethodMetaData();
            map.put(name, md);

            if (TraceComponent.isAnyTracingEnabled() && tcMetaData.isDebugEnabled())
                Tr.debug(tcMetaData, "visitMethod: " + name + desc + ": private");
            return md;
        }

        String key = getNonPrivateMethodKey(name, desc);
        if (ivNonPrivateMethodMetaData.containsKey(key)) {
            if (TraceComponent.isAnyTracingEnabled() && tcMetaData.isDebugEnabled())
                Tr.debug(tcMetaData, "visitMethod: " + name + desc + ": overridden");
            return null;
        }

        MethodMetaData md = new MethodMetaData();
        ivNonPrivateMethodMetaData.put(key, md);

        if (TraceComponent.isAnyTracingEnabled() && tcMetaData.isDebugEnabled())
            Tr.debug(tcMetaData, "visitMethod: " + name + desc + ": non-private");
        return md;
    }

    private abstract class AbstractMethodVisitor extends MethodVisitor {
        public AbstractMethodVisitor() {
            super(Opcodes.ASM8);
        }

        @Override
        public abstract void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf);
    }

    class MethodMetaData extends AbstractMethodVisitor {
        /**
         * True if the method does not contain any method calls.
         */
        boolean ivTrivial = true;

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (TraceComponent.isAnyTracingEnabled() && tcMetaData.isDebugEnabled() && ivTrivial) {
                String opcodeName;
                if (opcode == Opcodes.INVOKEINTERFACE) {
                    opcodeName = "interface";
                } else {
                    opcodeName = opcode == Opcodes.INVOKESPECIAL ? "special" : opcode == Opcodes.INVOKESTATIC ? "static" : opcode == Opcodes.INVOKEVIRTUAL ? "virtual" : Integer.toString(opcode);
                    if (itf) {
                        opcodeName += " (interface)";
                    }
                }

                Tr.debug(tcMetaData, "non-trivial method " + ivCurrentMethodName + ivCurrentMethodDesc + ": " +
                                     opcodeName + ' ' + owner + '.' + name + desc);
            }

            ivTrivial = false;
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
            if (TraceComponent.isAnyTracingEnabled() && tcMetaData.isDebugEnabled() && ivTrivial)
                Tr.debug(tcMetaData, "non-trivial method " + ivCurrentMethodName + ivCurrentMethodDesc + ": " +
                                     "dynamic [" + bsm.getOwner() + '.' + bsm.getName() + bsm.getDesc() + "] " + name + desc);

            ivTrivial = false;
        }

        @Override
        public void visitEnd() {
            if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled() || tcMetaData.isDebugEnabled()) && ivTrivial)
                Tr.debug(tc.isDebugEnabled() ? tc : tcMetaData,
                         "trivial method " + ivCurrentMethodName + ivCurrentMethodDesc);
        }
    }

    private class BridgeMethodMetaData extends AbstractMethodVisitor {
        Method ivTarget;

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            ivTarget = getPublicMethod(getNonPrivateMethodKey(name, desc));
        }

        @Override
        public void visitEnd() {
            if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled() || tcMetaData.isDebugEnabled()) && ivTarget != null)
                Tr.debug(tc.isDebugEnabled() ? tc : tcMetaData,
                         "bridge method " + ivCurrentMethodName + ivCurrentMethodDesc +
                                                                ", target " + ivTarget);
        }
    }
}
