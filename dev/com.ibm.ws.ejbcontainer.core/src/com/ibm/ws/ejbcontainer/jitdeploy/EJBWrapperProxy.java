/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.INDENT;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.convertClassName;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.getTypes;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.writeToClassFile;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_2;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.ibm.ejs.container.BusinessLocalWrapperProxy;
import com.ibm.ejs.container.EJSLocalHomeWrapperProxy;
import com.ibm.ejs.container.EJSLocalWrapperProxy;
import com.ibm.ejs.container.LocalBeanWrapperProxy;
import com.ibm.ejs.container.WrapperProxyState;
import com.ibm.ejs.container.util.DeploymentUtil;
import com.ibm.ejs.container.util.NameUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Provides Just In Time deployment of EJB Wrapper Proxy classes. The wrapper
 * proxy class uses the EJSContainer.resolveWrapperProxy methods to obtain a
 * wrapper instance, and then forward the method call. The properties of the
 * wrapper proxy class vary depending on the interface type:
 *
 * <ul>
 * <li>EJBLocalObject - extends EJSLocalWrapperProxy
 * <li>EJBLocalHome - extends EJSLocalHomeWrapperProxy
 * <li>local business interface - extends BusinessLocalWrapperProxy
 * <li>no-interface - extends bean class, implements LocalBeanWrapperProxy,
 * contains EJSWrapperBaseProxy field
 * </ul>
 */
public final class EJBWrapperProxy
{
    private static final TraceComponent tc = Tr.register
                    (EJBWrapperProxy.class,
                     JITUtils.JIT_TRACE_GROUP,
                     JITUtils.JIT_RSRC_BUNDLE);

    private static final String WRAPPER_PROXY_STATE_INTERNAL_NAME = convertClassName(WrapperProxyState.class.getName());
    private static final String WRAPPER_PROXY_STATE_TYPE_NAME = 'L' + WRAPPER_PROXY_STATE_INTERNAL_NAME + ';';

    private static final String LOCAL_WRAPPER_PROXY_INTERNAL_NAME = convertClassName(EJSLocalWrapperProxy.class.getName());
    private static final String LOCAL_HOME_WRAPPER_PROXY_INTERNAL_NAME = convertClassName(EJSLocalHomeWrapperProxy.class.getName());
    private static final String BUSINESS_LOCAL_WRAPPER_PROXY_INTERNAL_NAME = convertClassName(BusinessLocalWrapperProxy.class.getName());
    private static final String LOCAL_BEAN_WRAPPER_PROXY_INTERNAL_NAME = convertClassName(LocalBeanWrapperProxy.class.getName());

    public static final String LOCAL_BEAN_PROXY_FIELD = "ivProxy";
    private static final String LOCAL_BEAN_PROXY_FIELD_TYPE_NAME = 'L' + BUSINESS_LOCAL_WRAPPER_PROXY_INTERNAL_NAME + ";";

    public static String getProxyClassName(Class<?> intf)
    {
        String intfName = intf.getName();

        if (intfName.startsWith("java."))
        {
            // We cannot define classes in the "java." package.
            return NameUtil.deployPackagePrefix + intfName;
        }

        StringBuilder stubBuilder = new StringBuilder(intfName);
        int packageOffset = Math.max(intfName.lastIndexOf('.') + 1,
                                     intfName.lastIndexOf('$') + 1);
        stubBuilder.insert(packageOffset, "EJSProxy$$");

        return stubBuilder.toString();
    }

    public static byte[] generateClassBytes(String proxyClassName,
                                            Class<?>[] intfs,
                                            Method[] methods)
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        String internalClassName = convertClassName(proxyClassName);
        String internalSuperName;
        String[] internalInterfaceNames = new String[intfs.length];
        String resolveDesc = "(L" + BUSINESS_LOCAL_WRAPPER_PROXY_INTERNAL_NAME + ";)Ljava/lang/Object;";

        for (int i = 0; i < internalInterfaceNames.length; i++)
        {
            internalInterfaceNames[i] = convertClassName(intfs[i].getName());
        }

        boolean isClassProxy = !intfs[0].isInterface();
        if (isClassProxy)
        {
            internalSuperName = convertClassName(internalInterfaceNames[0]);
            internalInterfaceNames[0] = LOCAL_BEAN_WRAPPER_PROXY_INTERNAL_NAME;
        }
        else
        {
            boolean isEJBLocalHome = false;
            boolean isEJBLocalObject = false;

            for (Class<?> intf : intfs)
            {
                isEJBLocalHome |= EJBLocalHome.class.isAssignableFrom(intf);
                isEJBLocalObject |= EJBLocalObject.class.isAssignableFrom(intf);
            }

            internalSuperName = isEJBLocalHome ? LOCAL_HOME_WRAPPER_PROXY_INTERNAL_NAME :
                            isEJBLocalObject ? LOCAL_WRAPPER_PROXY_INTERNAL_NAME :
                                            BUSINESS_LOCAL_WRAPPER_PROXY_INTERNAL_NAME;
            if (isEJBLocalHome || isEJBLocalObject)
            {
                resolveDesc = "(L" + LOCAL_WRAPPER_PROXY_INTERNAL_NAME + ";)Ljava/lang/Object;";
            }
        }

        if (isTraceOn)
        {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "generateClassBytes");
            if (tc.isDebugEnabled())
            {
                Tr.debug(tc, INDENT + "className = " + internalClassName);
                Tr.debug(tc, INDENT + "interface = " + Arrays.toString(internalInterfaceNames));
                Tr.debug(tc, INDENT + "super     = " + internalSuperName);
            }
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER,
                 internalClassName,
                 null,
                 internalSuperName,
                 internalInterfaceNames);

        addFields(cw, isClassProxy);
        addCtor(cw, internalClassName, isClassProxy, internalSuperName);

        if (isClassProxy)
        {
            addClassProxyEqualsMethod(cw, internalClassName);
            addClassProxyHashCodeMethod(cw, internalClassName);
        }

        for (Method method : methods)
        {
            addMethod(cw, internalClassName, isClassProxy, resolveDesc, method);
        }

        if (isClassProxy)
        {
            ArrayList<Method> nonPublicMethods = DeploymentUtil.getNonPublicMethods(intfs[0], methods);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, INDENT + "Non-public methods : " +
                             nonPublicMethods.size());

            for (Method method : nonPublicMethods)
            {
                addMethod(cw, internalClassName, true, resolveDesc, method);
            }
        }

        cw.visitEnd();
        byte[] classBytes = cw.toByteArray();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            writeToClassFile(internalClassName, classBytes);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "generateClassBytes: " + classBytes.length + " bytes");

        return classBytes;
    }

    private static void addFields(ClassWriter cw, boolean isClassProxy)
    {
        if (isClassProxy)
        {
            final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, INDENT + "adding field : " +
                             LOCAL_BEAN_PROXY_FIELD + " " +
                             LOCAL_BEAN_PROXY_FIELD_TYPE_NAME);

            // -----------------------------------------------------------------------
            // private BusinessLocalWrapperProxy ivProxy;
            // -----------------------------------------------------------------------
            cw.visitField(ACC_PRIVATE | ACC_FINAL, LOCAL_BEAN_PROXY_FIELD,
                          LOCAL_BEAN_PROXY_FIELD_TYPE_NAME, null, null);
        }
    }

    private static void addCtor(ClassWriter cw,
                                String internalClassName,
                                boolean isClassProxy,
                                String internalSuperName)
    {
        MethodVisitor mv;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : <init> ()V");

        // -----------------------------------------------------------------------
        // public <Class Name>(WrapperProxyState state)
        // {

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(" + WRAPPER_PROXY_STATE_TYPE_NAME + ")V", null, null);
        mv.visitCode();

        if (isClassProxy)
        {
            // -----------------------------------------------------------------------
            //    super();
            // -----------------------------------------------------------------------
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, internalSuperName, "<init>", "()V");

            // -----------------------------------------------------------------------
            //    this.ivProxy = new BusinessLocalWrapperProxy(state);
            // -----------------------------------------------------------------------
            mv.visitVarInsn(ALOAD, 0);
            mv.visitTypeInsn(NEW, BUSINESS_LOCAL_WRAPPER_PROXY_INTERNAL_NAME);
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, BUSINESS_LOCAL_WRAPPER_PROXY_INTERNAL_NAME,
                               "<init>", "(" + WRAPPER_PROXY_STATE_TYPE_NAME + ")V");
            mv.visitFieldInsn(PUTFIELD, internalClassName,
                              LOCAL_BEAN_PROXY_FIELD, LOCAL_BEAN_PROXY_FIELD_TYPE_NAME);
        }
        else
        {
            // -----------------------------------------------------------------------
            //    super(state);
            // -----------------------------------------------------------------------
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, internalSuperName,
                               "<init>", "(" + WRAPPER_PROXY_STATE_TYPE_NAME + ")V");
        }

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    /**
     * Adds a definition for Object.equals method for the No-Interface view
     * (LocalBean).
     *
     * @param cw ASM ClassWriter to add the method to.
     * @param implClassName name of the wrapper class being generated.
     */
    private static void addClassProxyEqualsMethod(ClassWriter cw,
                                                  String implClassName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : equals (Ljava/lang/Object;)Z");

        // -----------------------------------------------------------------------
        // public boolean equals(Object other)
        // {
        // -----------------------------------------------------------------------
        final String desc = "(Ljava/lang/Object;)Z";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "equals", desc, null, null);
        mv.visitCode();

        // -----------------------------------------------------------------------
        //    if (other instanceof type)
        //    {
        // -----------------------------------------------------------------------
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(INSTANCEOF, implClassName);
        Label if_instanceofType_End = new Label();
        mv.visitJumpInsn(IFEQ, if_instanceofType_End);

        // -----------------------------------------------------------------------
        //       return this.ivProxy.equals(((type)other).ivProxy)
        // -----------------------------------------------------------------------
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, implClassName,
                          LOCAL_BEAN_PROXY_FIELD, LOCAL_BEAN_PROXY_FIELD_TYPE_NAME);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, implClassName);
        mv.visitFieldInsn(GETFIELD, implClassName,
                          LOCAL_BEAN_PROXY_FIELD, LOCAL_BEAN_PROXY_FIELD_TYPE_NAME);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object",
                           "equals", desc);
        mv.visitInsn(IRETURN);

        // -----------------------------------------------------------------------
        //    }
        // -----------------------------------------------------------------------
        mv.visitLabel(if_instanceofType_End);

        // -----------------------------------------------------------------------
        //    return false;
        // -----------------------------------------------------------------------
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    /**
     * Adds a definition for Object.hashCode method for the No-Interface view
     * (LocalBean).
     *
     * @param cw ASM ClassWriter to add the method to.
     * @param implClassName name of the wrapper class being generated.
     */
    private static void addClassProxyHashCodeMethod(ClassWriter cw,
                                                    String implClassName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : hashCode ()I");

        // -----------------------------------------------------------------------
        // public boolean equals(Object other)
        // {
        // -----------------------------------------------------------------------
        final String desc = "()I";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "hashCode", desc, null, null);
        mv.visitCode();

        // -----------------------------------------------------------------------
        //    return this.ivProxy.hashCode();
        // -----------------------------------------------------------------------
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, implClassName,
                          LOCAL_BEAN_PROXY_FIELD, LOCAL_BEAN_PROXY_FIELD_TYPE_NAME);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object",
                           "hashCode", desc);
        mv.visitInsn(IRETURN);

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mv.visitMaxs(1, 2);
        mv.visitEnd();
    }

    private static void addMethod(ClassWriter cw,
                                  String internalClassName,
                                  boolean isClassProxy,
                                  String resolveDesc,
                                  Method method)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : " + method);

        String methodName = method.getName();
        Type[] paramTypes = getTypes(method.getParameterTypes());

        org.objectweb.asm.commons.Method m =
                        new org.objectweb.asm.commons.Method(methodName,
                                        Type.getType(method.getReturnType()),
                                        paramTypes);

        Type[] exceptionTypes = getTypes(method.getExceptionTypes());

        GeneratorAdapter mg = new GeneratorAdapter(ACC_PUBLIC, m, null, exceptionTypes, cw);
        mg.visitCode();

        if (isClassProxy)
        {
            // -----------------------------------------------------------------------
            //    this.ivProxy ...
            // -----------------------------------------------------------------------
            mg.visitVarInsn(ALOAD, 0);
            mg.visitFieldInsn(GETFIELD, internalClassName,
                              LOCAL_BEAN_PROXY_FIELD, LOCAL_BEAN_PROXY_FIELD_TYPE_NAME);
        }
        else
        {
            // -----------------------------------------------------------------------
            //    this ...
            // -----------------------------------------------------------------------
            mg.visitVarInsn(ALOAD, 0);
        }

        Class<?> declaringClass = method.getDeclaringClass();
        String internalDeclaringClassName = convertClassName(declaringClass.getName());

        // -----------------------------------------------------------------------
        //    <return> ((<interface>)EJSContainer.resolveWrapperProxy(<wrapperProxy>)).<method>(<args...>)
        // -----------------------------------------------------------------------
        mg.visitMethodInsn(INVOKESTATIC,
                           "com/ibm/ejs/container/EJSContainer",
                           "resolveWrapperProxy",
                           resolveDesc);
        mg.visitTypeInsn(CHECKCAST, internalDeclaringClassName);

        for (int i = 0; i < paramTypes.length; i++)
        {
            mg.loadArg(i);
        }

        mg.visitMethodInsn(declaringClass.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL,
                           internalDeclaringClassName,
                           method.getName(),
                           m.getDescriptor());

        mg.returnValue();

        mg.endMethod();
    }
}
