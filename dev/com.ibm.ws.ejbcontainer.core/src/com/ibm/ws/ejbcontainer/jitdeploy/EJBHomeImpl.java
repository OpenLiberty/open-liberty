/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
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
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.methodKey;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.writeToClassFile;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.JSR;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_2;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;

import javax.ejb.CreateException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.ibm.ejs.container.ContainerEJBException;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.util.MethodAttribUtils;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.InternalConstants;

/**
 * Provides Just In Time runtime deployment of EJB Home implementations.
 **/
public final class EJBHomeImpl
{
    private static final TraceComponent tc = Tr.register(EJBHomeImpl.class,
                                                         JITUtils.JIT_TRACE_GROUP,
                                                         JITUtils.JIT_RSRC_BUNDLE);

    /**
     * Returns the local or remote Component interface class that corresponds to
     * the specified local or remote Component Home interface class. <p>
     * 
     * The Component interface class may be derived from the Home interface by
     * examining the return type of one of the 'create' methods. All Homes
     * must have at least one 'create' method, and the return type must be
     * the Component interface, per the EJB Specification. <p>
     * 
     * If the Component interface cannot be found, either because there were
     * no create methods or the create method returned void, then an
     * EJBConfigurationException will be thrown. <p>
     * 
     * Other validation will NOT be performed on the returned interface.
     * The method 'validateInterfaceBasics()' should be called to insure
     * the returned Class is a valid Component interface. <p>
     * 
     * @param homeInterface local or remote EJB home interface class
     * @param beanName name used to identify the bean if an error is logged.
     * 
     * @return the corresponding local or remote Component interface class.
     * @throws EJBConfigurationException when the specified home interface
     *             either has no 'create' methods, or a create method
     *             returns 'void'.
     **/
    // d443878
    static Class<?> getComponentInterface(Class<?> homeInterface,
                                          String beanName)
                    throws EJBConfigurationException
    {
        Class<?> compInterface = null;
        Method[] methods = homeInterface.getMethods();
        for (Method method : methods)
        {
            if (method.getName().startsWith("create"))
            {
                compInterface = method.getReturnType();

                if (compInterface == Void.TYPE)
                {
                    // Log the error and throw meaningful exception.        d457128.2
                    Tr.error(tc, "JIT_VOID_CREATE_RETURN_CNTR5018E",
                             new Object[] { beanName,
                                           homeInterface.getName(),
                                           method.getName() });
                    throw new EJBConfigurationException("EJB home interface " + homeInterface.getName() +
                                                        " 'create' method must return the component interface : " +
                                                        method + " : " + beanName);
                }

                break;
            }
        }

        if (compInterface == null)
        {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_NO_CREATE_METHOD_CNTR5019E",
                     new Object[] { beanName,
                                   homeInterface.getName() });
            throw new EJBConfigurationException("EJB home interface " + homeInterface.getName() +
                                                " must define a 'create' method : " + beanName);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getComponentInterface : " + compInterface);

        return compInterface;
    }

    /**
     * Validate the basic aspects of a local or remote home interface as
     * required by the EJB specification. <p>
     * 
     * An EJBConfigurationException will be thrown if the following rules
     * are violated:
     * 
     * <ul>
     * <li> The home interface must be an interface.
     * <li> The interface must extend javax.ejb.EJBHome or javax.ejb.EJBLocalHome.
     * <li> The return type for a create<METHOD> method must be the session bean's
     * remote/local interface type.
     * <li> The throws clause must include javax.ejb.CreateException.
     * </ul>
     * 
     * And, for Session beans, the following will also be validated:
     * 
     * <ul>
     * <li> At least one 'create' method must be defined.
     * <li> The return type for a create<METHOD> method must be the session bean's
     * remote/local interface type.
     * <li> The throws clause must include javax.ejb.CreateException.
     * </ul>
     * 
     * Additional method level validation will be performed when the
     * 'wrapper' is generated. <p>
     * 
     * @param homeInterface EJB local or remote home interface class to validate.
     * @param componentInterface corresponding local or remote component interface.
     * @param wrapperType the type of wrapper that will be generated.
     * @param beanName name used to identify the bean if an error is logged.
     * @param beanType Type of EJB, using constants defined in
     *            EJBComponentMetaData. Not all bean types are
     *            supported; only Stateless, Stateful, and BMP.
     * 
     * @throws EJBConfigurationException whenever the specified interface does
     *             not conform the the EJB Specification requirements.
     **/
    // d443878 d457128
    static void validateInterfaceBasics(Class<?> homeInterface,
                                        Class<?> componentInterface,
                                        EJBWrapperType wrapperType,
                                        String beanName,
                                        int beanType)
                    throws EJBConfigurationException
    {
        // All home interfaces must be 'interfaces'!
        if (!Modifier.isInterface(homeInterface.getModifiers()))
        {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_INTERFACE_NOT_INTERFACE_CNTR5011E",
                     new Object[] { beanName,
                                   homeInterface.getName() });
            throw new EJBConfigurationException("EJB home interface " + homeInterface.getName() +
                                                " must be an interface : " + beanName);
        }

        // This may appear to be a good place to use a 'switch', but using
        // 'switch' with 'enum's performs very poorly, as it involves inner
        // classes, and repeated validation of the types.

        if (wrapperType == EJBWrapperType.LOCAL_HOME)
        {
            if (!(EJBLocalHome.class).isAssignableFrom(homeInterface))
            {
                // Log the error and throw meaningful exception.           d457128.2
                Tr.error(tc, "JIT_MUST_EXTEND_EJBLOCALHOME_CNTR5016E",
                         new Object[] { beanName,
                                       homeInterface.getName() });
                throw new EJBConfigurationException("EJB local home interface " + homeInterface.getName() +
                                                    " must extend javax.ejb.EJBLocalHome : " + beanName);
            }
        }
        else if (wrapperType == EJBWrapperType.REMOTE_HOME)
        {
            if (!(EJBHome.class).isAssignableFrom(homeInterface))
            {
                // Log the error and throw meaningful exception.           d457128.2
                Tr.error(tc, "JIT_MUST_EXTEND_EJBHOME_CNTR5017E",
                         new Object[] { beanName,
                                       homeInterface.getName() });
                throw new EJBConfigurationException("EJB remote home interface " + homeInterface.getName() +
                                                    " must extend javax.ejb.EJBHome : " + beanName);
            }
        }

        // For Session beans, at least one create method must be defined,
        // and it must return the component interface type.
        // Entity beans have no required home methods, though support
        // 'create', 'find' and 'home' methods, which will be validated
        // when the wrapper is actually generated.                         d461100
        if (beanType == InternalConstants.TYPE_STATELESS_SESSION ||
            beanType == InternalConstants.TYPE_STATEFUL_SESSION)
        {
            Class<?> returnType = null;
            Method[] methods = homeInterface.getMethods();
            for (Method method : methods)
            {
                if (method.getName().startsWith("create"))
                {
                    returnType = method.getReturnType();

                    if (returnType != componentInterface)
                    {
                        // Log the error and throw meaningful exception.     d457128.2
                        Tr.error(tc, "JIT_WRONG_CREATE_RETURN_CNTR5020E",
                                 new Object[] { beanName,
                                               homeInterface.getName(),
                                               method.getName(),
                                               componentInterface.getName() });
                        throw new EJBConfigurationException("EJB home interface " + homeInterface.getName() +
                                                            " 'create' method must return the component interface : " +
                                                            method + " does not return " + componentInterface.getName() +
                                                            " : " + beanName);
                    }

                    boolean throwsCreateException = false;
                    Class<?>[] exceptions = method.getExceptionTypes();

                    for (Class<?> exception : exceptions)
                    {
                        if (CreateException.class == exception)
                        {
                            throwsCreateException = true;
                            break;
                        }
                    }

                    if (!throwsCreateException)
                    {
                        // Log the error and throw meaningful exception.     d457128.2
                        Tr.error(tc, "JIT_MISSING_CREATE_EX_CNTR5021E",
                                 new Object[] { beanName,
                                               homeInterface.getName(),
                                               method.getName() });
                        throw new EJBConfigurationException("EJB home method " + method +
                                                            " must throw javax.ejb.CreateException : " + beanName);
                    }
                } // end startswith "create"
            }

            if (returnType == null)
            {
                // Log the error and throw meaningful exception.           d457128.2
                Tr.error(tc, "JIT_NO_CREATE_METHOD_CNTR5019E",
                         new Object[] { beanName,
                                       homeInterface.getName() });
                throw new EJBConfigurationException("EJB home interface " + homeInterface.getName() +
                                                    " must define a 'create' method : " + beanName);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "validateInterfaceBasics : successful : " +
                         homeInterface.getName());
    }

    /**
     * Core method for generating the EJB Home implementation class bytes.
     * Intended for use by JITDeploy only (should not be called directly). <p>
     * 
     * @param homeClassName name of the home class to be generated.
     * @param remoteHomeInterface EJBHome (remote) interface implemented by the home.
     * @param localHomeInterface EJBLocalHome interface implemented by the home.
     * @param ejbClass EJB implementation class, that the generated home
     *            will route methods to.
     * @param pkeyClass Primary Key class (for BMP Entity), that the
     *            generated home will pass on method calls.
     *            Null should be passed for non-Entity EJBs.
     * @initMethods Map of the home create methods to the corresponding
     *              EJB Init methods that were either annotated
     *              with @Init or specified as <init-method> in xml.
     * @param beanName Name of the EJB (for messages).
     * @param beanType Type of EJB, using constants defined in
     *            EJBComponentMetaData. Not all bean types are
     *            supported; only Stateless, Stateful, and BMP.
     **/
    static byte[] generateClassBytes(String homeClassName,
                                     Class<?> remoteHomeInterface,
                                     Class<?> localHomeInterface,
                                     Class<?> ejbClass,
                                     Class<?> pkeyClass,
                                     HashMap<String, String> initMethods,
                                     String beanName,
                                     int beanType)
                    throws EJBConfigurationException
    {
        Method[] methods = null;
        HashMap<String, Method> homeMethods = new HashMap<String, Method>(); // d490485

        String internalClassName = convertClassName(homeClassName);
        String internalRemoteHomeInterfaceName = (remoteHomeInterface == null)
                        ? null : convertClassName(remoteHomeInterface.getName());
        String internalLocalHomeInterfaceName = (localHomeInterface == null)
                        ? null : convertClassName(localHomeInterface.getName());
        String internalEJBClassName = convertClassName(ejbClass.getName());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "generateEJBHomeImplBytes");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, INDENT + "className = " + internalClassName);
            Tr.debug(tc, INDENT + "remote    = " + internalRemoteHomeInterfaceName);
            Tr.debug(tc, INDENT + "local     = " + internalLocalHomeInterfaceName);
            Tr.debug(tc, INDENT + "ejb       = " + internalEJBClassName);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS); //F743-11995

        // Define the Class object
        cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER,
                 internalClassName,
                 null,
                 "com/ibm/ejs/container/EJSHome",
                 null);

        // Define the source code file and debug settings
        String sourceFileName = homeClassName.substring(homeClassName.lastIndexOf(".") + 1) + ".java";
        cw.visitSource(sourceFileName, null);

        // Add the public no parameter local object (wrapper) constructor
        addEJBHomeImplCtor(cw);

        // Add the public home methods (i.e. create) from the remote home interface
        if (remoteHomeInterface != null)
        {
            methods = remoteHomeInterface.getMethods();
            for (int i = 0; i < methods.length; ++i)
            {
                Method method = methods[i];
                String methodName = method.getName();

                Class<?> declaringClass = method.getDeclaringClass();
                if (declaringClass.equals(EJBHome.class))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, INDENT + "skipping      : " +
                                     declaringClass.getName() + "." + methodName);
                }
                else
                {
                    switch (beanType)
                    {
                        case InternalConstants.TYPE_STATELESS_SESSION:
                            if (methodName.equals("create") &&
                                method.getReturnType() != Void.TYPE)
                            {
                                addEJBHomeStatelessMethod(cw, method, false);
                            }
                            else
                            {
                                // Log the error and throw meaningful exception. d457128.2
                                Tr.error(tc, "JIT_INVALID_SL_HOME_METHOD_CNTR5022E",
                                         new Object[] { beanName,
                                                       remoteHomeInterface.getName(),
                                                       methodName });
                                throw new EJBConfigurationException("EJB stateless session home interface " +
                                                                    remoteHomeInterface.getName() +
                                                                    " must have only one method, and it must be " +
                                                                    "create() : " + beanName + " : " + methodName);
                            }
                            break;

                        case InternalConstants.TYPE_STATEFUL_SESSION:
                            if (methodName.startsWith("create") &&
                                method.getReturnType() != Void.TYPE)
                            {
                                addEJBHomeCreateMethod(cw, method, initMethods, internalEJBClassName, beanName, false);
                            }
                            else
                            {
                                // Log the error and throw meaningful exception. d457128.2
                                Tr.error(tc, "JIT_INVALID_SF_HOME_METHOD_CNTR5023E",
                                         new Object[] { beanName,
                                                       remoteHomeInterface.getName(),
                                                       methodName });
                                throw new EJBConfigurationException("EJB stateful session home interface " +
                                                                    remoteHomeInterface.getName() +
                                                                    " must define only create methods : " +
                                                                    beanName + " : " + methodName);
                            }
                            break;

                        case InternalConstants.TYPE_BEAN_MANAGED_ENTITY:
                            if (methodName.startsWith("create"))
                            {
                                addBMPEJBHomeCreateMethod(cw, method, ejbClass, pkeyClass,
                                                          internalEJBClassName,
                                                          beanName, false);
                            }
                            else if (methodName.equals("findByPrimaryKey"))
                            {
                                addBMPEJBHomeFindByPrimaryKeyMethod(cw, method, ejbClass, pkeyClass,
                                                                    internalEJBClassName,
                                                                    beanName, false);
                            }
                            else if (methodName.startsWith("find"))
                            {
                                addBMPEJBHomeFindMethod(cw, method, ejbClass, pkeyClass,
                                                        internalEJBClassName,
                                                        beanName, false);
                            }
                            else if (methodName.startsWith("remove"))
                            {
                                // Log the error and throw meaningful exception. d457128.2
                                Tr.error(tc, "JIT_INVALID_BMP_HOME_METHOD_CNTR5024E",
                                         new Object[] { beanName,
                                                       remoteHomeInterface.getName(),
                                                       methodName });
                                throw new EJBConfigurationException("EJB entity bean home interface " +
                                                                    remoteHomeInterface.getName() + " must not " +
                                                                    "define a method that starts with remove : " +
                                                                    beanName + " : " + methodName);
                            }
                            else
                            {
                                addBMPEJBHomeHomeMethod(cw, method, ejbClass,
                                                        internalEJBClassName,
                                                        beanName);
                                // Save this 'home method' signature, to insure the same
                                // 'home method' is not added again for the local home
                                // interface (below).                              d490485
                                homeMethods.put(methodKey(method), method);
                            }
                            break;

                        default:
                            // This is an APAR condition... and should never really happen.
                            // Really, this is here to make it obvious that a change is
                            // needed here when a new bean type (like CMP) is added
                            // in the future, so a Tr.error is not needed.      d457128.2
                            throw new ContainerEJBException("EJBContainer internal error: " +
                                                            "Bean Type not supported: " + beanType);
                    }
                }
            }
        }

        // Add the public home methods (i.e. create) from the local home interface
        if (localHomeInterface != null)
        {
            methods = localHomeInterface.getMethods();
            for (int i = 0; i < methods.length; ++i)
            {
                Method method = methods[i];
                String methodName = method.getName();

                Class<?> declaringClass = method.getDeclaringClass();
                if (declaringClass.equals(EJBLocalHome.class))
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, INDENT + "skipping      : " +
                                     declaringClass.getName() + "." + methodName);
                }
                else
                {
                    switch (beanType)
                    {
                        case InternalConstants.TYPE_STATELESS_SESSION:
                            if (methodName.equals("create") &&
                                method.getReturnType() != Void.TYPE)
                            {
                                addEJBHomeStatelessMethod(cw, method, true);
                            }
                            else
                            {
                                // Log the error and throw meaningful exception. d457128.2
                                Tr.error(tc, "JIT_INVALID_SL_HOME_METHOD_CNTR5022E",
                                         new Object[] { beanName,
                                                       localHomeInterface.getName(),
                                                       methodName });
                                throw new EJBConfigurationException("EJB stateless session home interface " +
                                                                    localHomeInterface.getName() +
                                                                    " must have only one method, and it must be " +
                                                                    "create() : " + beanName + " : " + methodName);
                            }
                            break;

                        case InternalConstants.TYPE_STATEFUL_SESSION:
                            if (methodName.startsWith("create") &&
                                method.getReturnType() != Void.TYPE)
                            {
                                addEJBHomeCreateMethod(cw, method, initMethods, internalEJBClassName, beanName, true);
                            }
                            else
                            {
                                // Log the error and throw meaningful exception. d457128.2
                                Tr.error(tc, "JIT_INVALID_SF_HOME_METHOD_CNTR5023E",
                                         new Object[] { beanName,
                                                       localHomeInterface.getName(),
                                                       methodName });
                                throw new EJBConfigurationException("EJB stateful session home interface " +
                                                                    localHomeInterface.getName() +
                                                                    " must define only create methods : " +
                                                                    beanName + " : " + methodName);
                            }
                            break;

                        case InternalConstants.TYPE_BEAN_MANAGED_ENTITY:
                            if (methodName.startsWith("create"))
                            {
                                addBMPEJBHomeCreateMethod(cw, method, ejbClass, pkeyClass,
                                                          internalEJBClassName,
                                                          beanName, true);
                            }
                            else if (methodName.equals("findByPrimaryKey"))
                            {
                                addBMPEJBHomeFindByPrimaryKeyMethod(cw, method, ejbClass, pkeyClass,
                                                                    internalEJBClassName,
                                                                    beanName, true);
                            }
                            else if (methodName.startsWith("find"))
                            {
                                addBMPEJBHomeFindMethod(cw, method, ejbClass, pkeyClass,
                                                        internalEJBClassName,
                                                        beanName, true);
                            }
                            else if (methodName.startsWith("remove"))
                            {
                                // Log the error and throw meaningful exception. d457128.2
                                Tr.error(tc, "JIT_INVALID_BMP_HOME_METHOD_CNTR5024E",
                                         new Object[] { beanName,
                                                       localHomeInterface.getName(),
                                                       methodName });
                                throw new EJBConfigurationException("EJB entity bean home interface " +
                                                                    localHomeInterface.getName() + " must not " +
                                                                    "define a method that starts with remove : " +
                                                                    beanName + " : " + methodName);
                            }
                            else
                            {
                                // If the 'home method' has not already been added for
                                // the remote home interface, then add it now.     d490485
                                String methodSig = methodKey(method);
                                if (homeMethods.containsKey(methodSig))
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        Tr.debug(tc, INDENT + "skipping      : " + methodSig +
                                                     " : home method already added for remote interface");
                                }
                                else
                                {
                                    addBMPEJBHomeHomeMethod(cw, method, ejbClass,
                                                            internalEJBClassName,
                                                            beanName);
                                }
                            }
                            break;

                        default:
                            // This is an APAR condition... and should never really happen.
                            // Really, this is here to make it obvious that a change is
                            // needed here when a new bean type (like CMP) is added
                            // in the future, so a Tr.error is not needed.      d457128.2
                            throw new ContainerEJBException("EJBContainer internal error: " +
                                                            "Bean Type not supported: " + beanType);
                    }
                }
            }
        }

        cw.visitEnd();

        byte[] classBytes = cw.toByteArray();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            writeToClassFile(internalClassName, classBytes);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "generateEJBHomeImplBytes: " + classBytes.length + " bytes");

        return classBytes;
    }

    private static void addEJBHomeImplCtor(ClassWriter cw)
    {
        MethodVisitor mv;
        String[] exceptions = new String[] { "java/rmi/RemoteException" };

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : <init> ()V");

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, exceptions);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ejs/container/EJSHome", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private static void addEJBHomeStatelessMethod(ClassWriter cw,
                                                  Method method,
                                                  boolean isLocal)
    {
        GeneratorAdapter mg;
        String methodName = isLocal ? (method.getName() + "_Local") : method.getName();
        String methodSignature = MethodAttribUtils.jdiMethodSignature(method);
        String implMethodName = isLocal ? "createWrapper_Local" : "createWrapper";
        String implMethodSignature = isLocal
                        ? "(Lcom/ibm/ejs/container/BeanId;)Lcom/ibm/ejs/container/EJSLocalWrapper;"
                        : "(Lcom/ibm/ejs/container/BeanId;)Lcom/ibm/ejs/container/EJSWrapper;";

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, INDENT + "adding method : " +
                         methodName + " " + methodSignature);
            Tr.debug(tc, INDENT + "                -> " +
                         implMethodName + " " + implMethodSignature);
        }

        Type returnType = Type.getType(method.getReturnType());
        Type[] argTypes = getTypes(method.getParameterTypes());
        Type[] exceptionTypes = getTypes(method.getExceptionTypes());

        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method(methodName,
                        returnType,
                        argTypes);

        mg = new GeneratorAdapter(ACC_PUBLIC, m, null, exceptionTypes, cw);

        // -----------------------------------------------------------------------
        // Begin Method Code...
        // -----------------------------------------------------------------------
        mg.visitCode();

        // -----------------------------------------------------------------------
        // <return type> returnValue = false | 0 | null;
        // -----------------------------------------------------------------------
        int returnValue = -1;
        if (returnType != Type.VOID_TYPE)
        {
            returnValue = mg.newLocal(returnType);
            switch (returnType.getSort())
            {
                case Type.BOOLEAN:
                    mg.push(false);
                    break;

                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    mg.push(0);
                    break;

                case Type.FLOAT:
                    mg.push((float) 0);
                    break;

                case Type.LONG:
                    mg.push((long) 0);
                    break;

                case Type.DOUBLE:
                    mg.push((double) 0);
                    break;

                // ARRAY & OBJECT - push a null on the stack
                default:
                    mg.visitInsn(ACONST_NULL);
                    break;
            }
            mg.storeLocal(returnValue);
        }
        //mv.visitInsn(ACONST_NULL);
        //mv.visitVarInsn(ASTORE, 1);
        //mv.visitInsn(ACONST_NULL);
        //mv.visitVarInsn(ASTORE, 2);
        //mv.visitInsn(ICONST_0);
        //mv.visitVarInsn(ISTORE, 3);
        //mv.visitInsn(ICONST_0);
        //mv.visitVarInsn(ISTORE, 4);

        // -----------------------------------------------------------------------
        // try
        // {
        // -----------------------------------------------------------------------
        Label main_try_begin = new Label();
        mg.visitLabel(main_try_begin);
        //Label l0 = new Label();
        //mv.visitLabel(l0);

        // -----------------------------------------------------------------------
        //   rtnValue = (<return type>)super.createWrapper(null);
        // -----------------------------------------------------------------------
        mg.loadThis();
        //mv.visitVarInsn(ALOAD, 0);
        mg.visitInsn(ACONST_NULL);
        //mv.visitInsn(ACONST_NULL);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           implMethodName,
                           implMethodSignature);
        mg.visitTypeInsn(CHECKCAST, returnType.getInternalName());
        mg.storeLocal(returnValue);
        //mv.visitVarInsn(ASTORE, 2);

        // -----------------------------------------------------------------------
        // }                     // end of try
        // -----------------------------------------------------------------------

        Label main_tcf_exit = new Label();
        mg.visitJumpInsn(GOTO, main_tcf_exit); // Skip to very end

        Label main_try_end = new Label();
        mg.visitLabel(main_try_end); // mark the end
        //Label l1 = new Label();
        //mv.visitJumpInsn(GOTO, l1);

        // -----------------------------------------------------------------------
        // catch (<checked exception> <ex>)  ->  for each <checked exception>
        // {
        //   throw <ex>;
        // }
        // -----------------------------------------------------------------------
        Label[] main_catch_label = new Label[exceptionTypes.length];
        int caught_ex = mg.newLocal(Type.getType("Ljava/lang/Exception;"));

        for (int i = 0; i < exceptionTypes.length; i++)
        {
            main_catch_label[i] = new Label();
            mg.visitLabel(main_catch_label[i]);
            mg.storeLocal(caught_ex);
            mg.loadLocal(caught_ex);
            mg.visitInsn(ATHROW);
        }
        //Label l2 = new Label();
        //mv.visitLabel(l2);
        //mv.visitVarInsn(ASTORE, 5);
        //mv.visitInsn(ICONST_1);
        //mv.visitVarInsn(ISTORE, 3);
        //mv.visitVarInsn(ALOAD, 5);
        //mv.visitInsn(ATHROW);
        //Label l3 = new Label();
        //mv.visitLabel(l3);
        //mv.visitVarInsn(ASTORE, 5);
        //mv.visitInsn(ICONST_1);
        //mv.visitVarInsn(ISTORE, 3);
        //mv.visitVarInsn(ALOAD, 5);
        //mv.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        // catch (Throwable th)
        // {
        //   throw new CreateFailureException(th);
        // }
        // -----------------------------------------------------------------------

        Label main_catch_throwable = new Label();
        mg.visitLabel(main_catch_throwable);
        int main_th = mg.newLocal(Type.getType("Ljava/lang/Throwable;"));
        mg.storeLocal(main_th);
        mg.visitTypeInsn(NEW, "com/ibm/ejs/container/CreateFailureException");
        mg.visitInsn(DUP);
        mg.loadLocal(main_th);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/CreateFailureException",
                           "<init>", "(Ljava/lang/Throwable;)V");
        mg.visitInsn(ATHROW);
        //Label l4 = new Label();
        //mv.visitLabel(l4);
        //mv.visitVarInsn(ASTORE, 5);
        //mv.visitInsn(ICONST_1);
        //mv.visitVarInsn(ISTORE, 3);
        //mv.visitTypeInsn(NEW, "com/ibm/ejs/container/CreateFailureException");
        //mv.visitInsn(DUP);
        //mv.visitVarInsn(ALOAD, 5);
        //mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ejs/container/CreateFailureException", "<init>", "(Ljava/lang/Throwable;)V");
        //mv.visitInsn(ATHROW);

        // Finally block not needed for Stateless Session create!!!!!!!!
        //Label l5 = new Label();
        //mv.visitLabel(l5);
        //mv.visitVarInsn(ASTORE, 7);
        //Label l6 = new Label();
        //mv.visitJumpInsn(JSR, l6);
        //mv.visitVarInsn(ALOAD, 7);
        //mv.visitInsn(ATHROW);
        //mv.visitLabel(l6);
        //mv.visitVarInsn(ASTORE, 6);
        //mv.visitVarInsn(ILOAD, 3);
        //Label l7 = new Label();
        //mv.visitJumpInsn(IFEQ, l7);
        //mv.visitVarInsn(ALOAD, 0);
        //mv.visitVarInsn(ALOAD, 1);
        //mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ejs/container/EJSHome", "createFailure", "(Lcom/ibm/ejs/container/BeanO;)V");
        //mv.visitLabel(l7);
        //mv.visitVarInsn(RET, 6);
        //mv.visitLabel(l1);
        //mv.visitJumpInsn(JSR, l6);
        //Label l8 = new Label();
        //mv.visitLabel(l8);

        // -----------------------------------------------------------------------
        // }                     // end of try / catch / finally
        // -----------------------------------------------------------------------

        mg.visitLabel(main_tcf_exit);

        // -----------------------------------------------------------------------
        // return
        // -----------------------------------------------------------------------
        mg.loadLocal(returnValue);
        mg.returnValue();
        //mv.visitVarInsn(ALOAD, 2);
        //mv.visitInsn(ARETURN);

        // -----------------------------------------------------------------------
        // All Try-Catch-Finally definitions for above
        // -----------------------------------------------------------------------
        for (int i = 0; i < exceptionTypes.length; i++)
        {
            mg.visitTryCatchBlock(main_try_begin, main_try_end,
                                  main_catch_label[i],
                                  exceptionTypes[i].getInternalName());
        }
        mg.visitTryCatchBlock(main_try_begin, main_try_end,
                              main_catch_throwable, "java/lang/Throwable");
        //mv.visitTryCatchBlock(l0, l2, l2, "javax/ejb/CreateException");
        //mv.visitTryCatchBlock(l0, l2, l3, "java/rmi/RemoteException");
        //mv.visitTryCatchBlock(l0, l2, l4, "java/lang/Throwable");
        //mv.visitTryCatchBlock(l0, l5, l5, null);
        //mv.visitTryCatchBlock(l1, l8, l5, null);

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
        //mv.visitMaxs(3, 8);
        //mv.visitEnd();
    }

    private static void addEJBHomeCreateMethod(ClassWriter cw,
                                               Method method,
                                               HashMap<String, String> initMethods,
                                               String implClassName,
                                               String beanName,
                                               boolean isLocal)
                    throws EJBConfigurationException
    {
        GeneratorAdapter mg;
        String methodName = isLocal ? (method.getName() + "_Local") : method.getName();
        String methodSignature = MethodAttribUtils.jdiMethodSignature(method);

        // -----------------------------------------------------------------------
        // Determine the name of the implementation method...
        //
        // This will be either ejbCreate<XXX> or an @Init method where the
        // signature matches that of this create method, except the return type.
        //
        // Look in the init map first using the method name and signature, in
        // case the name was specified on @Init, then look for just the signature.
        // The method name will always be present for ejbCreate<XXX>.    d369262.6
        // -----------------------------------------------------------------------
        if (initMethods == null)
        {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_NO_INIT_METHOD_CNTR5025E",
                     new Object[] { beanName,
                                   method.getDeclaringClass().getName(),
                                   method.getName(),
                                   implClassName });
            throw new EJBConfigurationException("ejbCreate or init method required for home method " +
                                                method.getName() + methodSignature + " of interface " +
                                                method.getDeclaringClass().getName() + " for bean " +
                                                beanName + ", and no ejbCreate or init methods are present " +
                                                "on class " + implClassName);
        }
        String implMethodSignature = methodSignature.substring(0, (methodSignature.lastIndexOf(")") + 1)) + "V";
        String implMethodName = initMethods.get(method.getName() + implMethodSignature);
        if (implMethodName == null)
        {
            implMethodName = initMethods.get(implMethodSignature);
        }

        if (implMethodName == null)
        {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_NO_INIT_METHOD_CNTR5025E",
                     new Object[] { beanName,
                                   method.getDeclaringClass().getName(),
                                   method.getName(),
                                   implClassName });
            throw new EJBConfigurationException("ejbCreate or init method required for home method " +
                                                method.getName() + methodSignature + " of interface " +
                                                method.getDeclaringClass().getName() + " for bean " +
                                                beanName + ", and a corresponding method was not found " +
                                                "on class " + implClassName);
        }

        String postCreate = isLocal ? "postCreate_Local" : "postCreate";
        String postCreate_sig = isLocal
                        ? "(Lcom/ibm/ejs/container/BeanO;)Ljavax/ejb/EJBLocalObject;"
                        : "(Lcom/ibm/ejs/container/BeanO;)Ljavax/ejb/EJBObject;";

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, INDENT + "adding method : " +
                         methodName + " " + methodSignature);
            Tr.debug(tc, INDENT + "              : " +
                         implMethodName + " " + implMethodSignature);
        }

        Type returnType = Type.getType(method.getReturnType());
        Type[] argTypes = getTypes(method.getParameterTypes());
        Type[] exceptionTypes = getTypes(method.getExceptionTypes());

        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method(methodName,
                        returnType,
                        argTypes);

        mg = new GeneratorAdapter(ACC_PUBLIC, m, null, exceptionTypes, cw);

        // -----------------------------------------------------------------------
        // Begin Method Code...
        // -----------------------------------------------------------------------
        mg.visitCode();

        // -----------------------------------------------------------------------
        // <return type> returnValue = false | 0 | null;
        // -----------------------------------------------------------------------
        int returnValue = -1;
        if (returnType != Type.VOID_TYPE)
        {
            returnValue = mg.newLocal(returnType);
            switch (returnType.getSort())
            {
                case Type.BOOLEAN:
                    mg.push(false);
                    break;

                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    mg.push(0);
                    break;

                case Type.FLOAT:
                    mg.push((float) 0);
                    break;

                case Type.LONG:
                    mg.push((long) 0);
                    break;

                case Type.DOUBLE:
                    mg.push((double) 0);
                    break;

                // ARRAY & OBJECT - push a null on the stack
                default:
                    mg.visitInsn(ACONST_NULL);
                    break;
            }
            mg.storeLocal(returnValue);
        }

        // -----------------------------------------------------------------------
        // BeanO beanO = null;
        // boolean exceptionOccurred = false;
        // boolean preEjbCreateCalled = false;
        // -----------------------------------------------------------------------
        int beanO = mg.newLocal(Type.getType("Lcom/ibm/ejs/container/BeanO;"));
        mg.visitInsn(ACONST_NULL);
        mg.storeLocal(beanO);

        int exceptionOccurred = mg.newLocal(Type.BOOLEAN_TYPE);
        mg.push(false);
        mg.storeLocal(exceptionOccurred);

        int preEjbCreateCalled = mg.newLocal(Type.BOOLEAN_TYPE);
        mg.push(false);
        mg.storeLocal(preEjbCreateCalled);

        // -----------------------------------------------------------------------
        // try
        // {
        // -----------------------------------------------------------------------
        Label main_try_begin = new Label();
        mg.visitLabel(main_try_begin);

        // -----------------------------------------------------------------------
        //   beano = super.createBeanO();
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "createBeanO", "()Lcom/ibm/ejs/container/BeanO;");
        mg.storeLocal(beanO);

        // -----------------------------------------------------------------------
        //   <bean impl> bean = (<bean impl>)beano.getBeanInstance();
        // -----------------------------------------------------------------------
        Type implType = Type.getType("L" + implClassName + ";");
        int bean = mg.newLocal(implType);
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKEVIRTUAL,
                           "com/ibm/ejs/container/BeanO",
                           "getBeanInstance", "()Ljava/lang/Object;");
        mg.checkCast(implType);
        mg.storeLocal(bean);

        // -----------------------------------------------------------------------
        //   preEjbCreateCalled = super.preEjbCreate(beano);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "preEjbCreate", "(Lcom/ibm/ejs/container/BeanO;)Z");
        mg.storeLocal(preEjbCreateCalled);

        // -----------------------------------------------------------------------
        //   bean.ejbCreate<XXX>(<args...>);          (or @Init method)
        // -----------------------------------------------------------------------
        mg.loadLocal(bean);
        mg.loadArgs(0, argTypes.length);
        mg.visitMethodInsn(INVOKEVIRTUAL,
                           implClassName,
                           implMethodName, implMethodSignature);

        // -----------------------------------------------------------------------
        //   returnValue = (<return type>)super.postCreate<_Local>(beano);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           postCreate, postCreate_sig);
        mg.checkCast(returnType);
        mg.storeLocal(returnValue);

        // -----------------------------------------------------------------------
        // }                     // end of try
        // -----------------------------------------------------------------------
        Label main_try_end = new Label();
        mg.visitLabel(main_try_end); // mark the end

        Label main_finally_begin = new Label();
        mg.visitJumpInsn(JSR, main_finally_begin); // execte the finally

        Label main_tcf_exit = new Label();
        mg.visitJumpInsn(GOTO, main_tcf_exit); // Skip to very end

        // -----------------------------------------------------------------------
        // catch (<checked exception> <ex>)  ->  for each <checked exception>
        // {
        //   exceptionOccurred = true;
        //   throw <ex>;
        // }
        // -----------------------------------------------------------------------
        Label[] main_catch_label = new Label[exceptionTypes.length];
        int caught_ex = mg.newLocal(Type.getType("Ljava/lang/Exception;"));

        for (int i = 0; i < exceptionTypes.length; i++)
        {
            main_catch_label[i] = new Label();
            mg.visitLabel(main_catch_label[i]);
            mg.storeLocal(caught_ex);
            mg.push(true);
            mg.storeLocal(exceptionOccurred);
            mg.loadLocal(caught_ex);
            mg.visitInsn(ATHROW);
        }

        // -----------------------------------------------------------------------
        // catch (Throwable th)
        // {
        //   exceptionOccurred = true;
        //
        //   throw super.newCreateFailureException_Local(th);              [Local]
        // [or]
        //   throw new CreateFailureException(th);                        [Remote]
        // }
        // -----------------------------------------------------------------------
        Label main_catch_throwable = new Label();
        mg.visitLabel(main_catch_throwable);
        int main_th = mg.newLocal(Type.getType("Ljava/lang/Throwable;"));
        mg.storeLocal(main_th);
        mg.push(true);
        mg.storeLocal(exceptionOccurred);

        if (isLocal)
        {
            mg.loadThis();
            mg.loadLocal(main_th);
            mg.visitMethodInsn(INVOKESPECIAL,
                               "com/ibm/ejs/container/EJSHome",
                               "newCreateFailureException_Local",
                               "(Ljava/lang/Throwable;)Ljavax/ejb/EJBException;");
        }
        else
        {
            mg.visitTypeInsn(NEW, "com/ibm/ejs/container/CreateFailureException");
            mg.visitInsn(DUP);
            mg.loadLocal(main_th);
            mg.visitMethodInsn(INVOKESPECIAL,
                               "com/ibm/ejs/container/CreateFailureException",
                               "<init>", "(Ljava/lang/Throwable;)V");
        }
        mg.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        // finally
        // {
        // -----------------------------------------------------------------------
        Label main_finally_ex = new Label();
        mg.visitLabel(main_finally_ex); // finally - after caught exception
        int main_finally_th = mg.newLocal(Type.getType("Ljava/lang/Throwable;"));
        mg.storeLocal(main_finally_th);
        mg.visitJumpInsn(JSR, main_finally_begin); // finally - jump/return to actual code
        mg.loadLocal(main_finally_th);
        mg.visitInsn(ATHROW); // finally - re-throw exception

        mg.visitLabel(main_finally_begin); // finally - after normal code flow

        int main_finally_return = mg.newLocal(Type.getType("Ljava/lang/Object;"));
        mg.storeLocal(main_finally_return);

        // -----------------------------------------------------------------------
        //   if ( exceptionOccurred )
        //   {
        // -----------------------------------------------------------------------
        Label if_exception_else = mg.newLabel();
        Label if_exception_end = mg.newLabel();
        mg.loadLocal(exceptionOccurred);
        mg.visitJumpInsn(IFEQ, if_exception_else); // if equal false

        // -----------------------------------------------------------------------
        //     super.createFailure(beanO);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "createFailure", "(Lcom/ibm/ejs/container/BeanO;)V");
        mg.visitJumpInsn(GOTO, if_exception_end);

        // -----------------------------------------------------------------------
        //   }
        //   else if ( preEjbCreateCalled )
        //   {
        // -----------------------------------------------------------------------
        mg.visitLabel(if_exception_else);
        mg.loadLocal(preEjbCreateCalled);
        mg.visitJumpInsn(IFEQ, if_exception_end); // if equal false

        // -----------------------------------------------------------------------
        //     super.afterPostCreatCompletion(beanO);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "afterPostCreateCompletion", "(Lcom/ibm/ejs/container/BeanO;)V");

        // -----------------------------------------------------------------------
        //   }        // end if exceptionOccurred else preEjbCreateCalled
        // -----------------------------------------------------------------------
        mg.visitLabel(if_exception_end);

        // -----------------------------------------------------------------------
        // }                     // end of try / catch / finally
        // -----------------------------------------------------------------------
        // Return from finally, either to the end of the try-catch-finally
        // below for normal code flow... or to the beginning of the finally
        // above for the exception path, to rethrow the exception.
        mg.ret(main_finally_return);

        mg.visitLabel(main_tcf_exit);

        // -----------------------------------------------------------------------
        // return returnValue;
        // -----------------------------------------------------------------------
        mg.loadLocal(returnValue);
        mg.returnValue();

        // -----------------------------------------------------------------------
        // All Try-Catch-Finally definitions for above
        // -----------------------------------------------------------------------
        for (int i = 0; i < exceptionTypes.length; i++)
        {
            mg.visitTryCatchBlock(main_try_begin, main_try_end,
                                  main_catch_label[i],
                                  exceptionTypes[i].getInternalName());
        }
        mg.visitTryCatchBlock(main_try_begin, main_try_end,
                              main_catch_throwable, "java/lang/Throwable");
        mg.visitTryCatchBlock(main_try_begin, main_finally_ex, main_finally_ex, null);

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

    private static void addBMPEJBHomeCreateMethod(ClassWriter cw,
                                                  Method method,
                                                  Class<?> ejbClass,
                                                  Class<?> pkeyClass,
                                                  String implClassName,
                                                  String beanName,
                                                  boolean isLocal)
                    throws EJBConfigurationException
    {
        GeneratorAdapter mg;
        Method ejbCreateMethod;
        Method ejbPostCreateMethod;

        String methodName = method.getName();
        String ejbCreateMethodName = "ejbC" + methodName.substring(1);
        String ejbPostCreateMethodName = "ejbPostC" + methodName.substring(1);
        String methodSignature = MethodAttribUtils.jdiMethodSignature(method);

        if (isLocal)
            methodName += "_Local";

        // -----------------------------------------------------------------------
        // Determine the name and signature of the implementation methods...
        //
        // This will be both ejbCreate<XXX> and ejbPostCreate<XXX>, where the
        // signature matches that of this create method, except the return type.
        // ejbCreate must return the primary key, and ejbPostCreate must return
        // void.
        // -----------------------------------------------------------------------
        try
        {
            ejbCreateMethod = ejbClass.getMethod(ejbCreateMethodName,
                                                 method.getParameterTypes());
        } catch (NoSuchMethodException ex)
        {
            // FFDC is not needed, as a meaningful exception is being thrown.
            // FFDCFilter.processException(ejbex, CLASS_NAME + ".addBMPEJBHomeCreateMethod", "1280");
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_NO_EJBCREATE_METHOD_CNTR5026E",
                     new Object[] { beanName,
                                   method.getDeclaringClass().getName(),
                                   method.getName(),
                                   ejbClass.getName() });
            throw new EJBConfigurationException("ejbCreate method required for home method " +
                                                method.getName() + methodSignature + " of interface " +
                                                method.getDeclaringClass().getName() + " for bean " +
                                                beanName + ", and a corresponding method was not found " +
                                                "on class " + ejbClass.getName());
        }
        if (ejbCreateMethod.getReturnType() != pkeyClass)
        {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_INVALID_CREATE_RETURN_CNTR5027E",
                     new Object[] { beanName,
                                   ejbClass.getName(),
                                   ejbCreateMethod.getName(),
                                   ejbCreateMethod.getReturnType().getName(),
                                   pkeyClass.getName() });
            throw new EJBConfigurationException("Method " + ejbCreateMethod.getName() + " of class " +
                                                ejbCreateMethod.getReturnType().getName() + " with return type " +
                                                ejbCreateMethod.getReturnType().getName() +
                                                " must return the primary key type " + pkeyClass.getName() +
                                                " : " + beanName);
        }
        String ejbCreateMethodSignature =
                        MethodAttribUtils.jdiMethodSignature(ejbCreateMethod);

        try
        {
            ejbPostCreateMethod = ejbClass.getMethod(ejbPostCreateMethodName,
                                                     method.getParameterTypes());
        } catch (NoSuchMethodException ex)
        {
            // FFDC is not needed, as a meaningful exception is being thrown.
            // FFDCFilter.processException(ejbex, CLASS_NAME + ".addBMPEJBHomeCreateMethod", "1321");
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_NO_POSTCREATE_METHOD_CNTR5028E",
                     new Object[] { beanName,
                                   method.getDeclaringClass().getName(),
                                   method.getName(),
                                   ejbClass.getName() });
            throw new EJBConfigurationException("ejbPostCreate method required for home method " +
                                                method.getName() + methodSignature + " of interface " +
                                                method.getDeclaringClass().getName() + " for bean " +
                                                beanName + ", and a corresponding method was not found " +
                                                "on class " + ejbClass.getName());
        }
        if (!("void".equals(ejbPostCreateMethod.getReturnType().getName())))
        {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_INVALID_POSTCREATE_RETURN_CNTR5029E",
                     new Object[] { beanName,
                                   ejbClass.getName(),
                                   ejbPostCreateMethod.getName(),
                                   ejbPostCreateMethod.getReturnType().getName() });
            throw new EJBConfigurationException("Method " + ejbPostCreateMethod.getName() + " of class " +
                                                ejbPostCreateMethod.getReturnType().getName() + " with return type " +
                                                ejbPostCreateMethod.getReturnType().getName() +
                                                " must return void : " + beanName);
        }
        String ejbPostCreateMethodSignature =
                        MethodAttribUtils.jdiMethodSignature(ejbPostCreateMethod);

        String postCreate = isLocal ? "postCreate_Local" : "postCreate";
        String postCreate_sig = isLocal
                        ? "(Lcom/ibm/ejs/container/BeanO;Ljava/lang/Object;Z)Ljavax/ejb/EJBLocalObject;"
                        : "(Lcom/ibm/ejs/container/BeanO;Ljava/lang/Object;Z)Ljavax/ejb/EJBObject;";

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, INDENT + "adding method : " +
                         methodName + " " + methodSignature);
            Tr.debug(tc, INDENT + "              : " +
                         ejbCreateMethodName + " " + ejbCreateMethodSignature);
            Tr.debug(tc, INDENT + "              : " +
                         ejbPostCreateMethodName + " " + ejbPostCreateMethodSignature);
        }

        Type returnType = Type.getType(method.getReturnType());
        Type[] argTypes = getTypes(method.getParameterTypes());
        Type[] exceptionTypes = getTypes(method.getExceptionTypes());

        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method(methodName,
                        returnType,
                        argTypes);

        mg = new GeneratorAdapter(ACC_PUBLIC, m, null, exceptionTypes, cw);

        // -----------------------------------------------------------------------
        // Begin Method Code...
        // -----------------------------------------------------------------------
        mg.visitCode();

        // -----------------------------------------------------------------------
        // <return type> returnValue = false | 0 | null;
        // -----------------------------------------------------------------------
        int returnValue = -1;
        if (returnType != Type.VOID_TYPE)
        {
            returnValue = mg.newLocal(returnType);
            switch (returnType.getSort())
            {
                case Type.BOOLEAN:
                    mg.push(false);
                    break;

                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    mg.push(0);
                    break;

                case Type.FLOAT:
                    mg.push((float) 0);
                    break;

                case Type.LONG:
                    mg.push((long) 0);
                    break;

                case Type.DOUBLE:
                    mg.push((double) 0);
                    break;

                // ARRAY & OBJECT - push a null on the stack
                default:
                    mg.visitInsn(ACONST_NULL);
                    break;
            }
            mg.storeLocal(returnValue);
        }

        // -----------------------------------------------------------------------
        // BeanO beanO = null;
        // boolean exceptionOccurred = false;
        // boolean preEjbCreateCalled = false;
        // -----------------------------------------------------------------------
        int beanO = mg.newLocal(Type.getType("Lcom/ibm/ejs/container/BeanO;"));
        mg.visitInsn(ACONST_NULL);
        mg.storeLocal(beanO);

        int exceptionOccurred = mg.newLocal(Type.BOOLEAN_TYPE);
        mg.push(false);
        mg.storeLocal(exceptionOccurred);

        int preEjbCreateCalled = mg.newLocal(Type.BOOLEAN_TYPE);
        mg.push(false);
        mg.storeLocal(preEjbCreateCalled);

        // -----------------------------------------------------------------------
        // try
        // {
        // -----------------------------------------------------------------------
        Label main_try_begin = new Label();
        mg.visitLabel(main_try_begin);

        // -----------------------------------------------------------------------
        //   beanO = super.createBeanO();
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "createBeanO", "()Lcom/ibm/ejs/container/BeanO;");
        mg.storeLocal(beanO);

        // -----------------------------------------------------------------------
        //   <bean impl> bean = (<bean impl>)beanO.getBeanInstance();
        // -----------------------------------------------------------------------
        Type implType = Type.getType("L" + implClassName + ";");
        int bean = mg.newLocal(implType);
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKEVIRTUAL,
                           "com/ibm/ejs/container/BeanO",
                           "getBeanInstance", "()Ljava/lang/Object;");
        mg.checkCast(implType);
        mg.storeLocal(bean);

        // -----------------------------------------------------------------------
        //   preEjbCreateCalled = super.preEjbCreate(beanO);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "preEjbCreate", "(Lcom/ibm/ejs/container/BeanO;)Z");
        mg.storeLocal(preEjbCreateCalled);

        // -----------------------------------------------------------------------
        //   <pkey type> pkey = bean.ejbCreate<XXX>(<args...>);
        // -----------------------------------------------------------------------
        int pkey = mg.newLocal(Type.getType(pkeyClass));
        mg.loadLocal(bean);
        mg.loadArgs(0, argTypes.length);
        mg.visitMethodInsn(INVOKEVIRTUAL,
                           implClassName,
                           ejbCreateMethodName, ejbCreateMethodSignature);
        mg.storeLocal(pkey);

        // -----------------------------------------------------------------------
        //   returnValue = (<return type>)super.postCreate<_Local>(beanO, pkey, true);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.loadLocal(pkey);
        mg.push(true);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           postCreate, postCreate_sig);
        mg.checkCast(returnType);
        mg.storeLocal(returnValue);

        // -----------------------------------------------------------------------
        //   bean.ejbPostCreate<XXX>(<args...>);
        // -----------------------------------------------------------------------
        mg.loadLocal(bean);
        mg.loadArgs(0, argTypes.length);
        mg.visitMethodInsn(INVOKEVIRTUAL,
                           implClassName,
                           ejbPostCreateMethodName, ejbPostCreateMethodSignature);

        // -----------------------------------------------------------------------
        //   super.afterPostCreate(beanO, pkey);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.loadLocal(pkey);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "afterPostCreate", "(Lcom/ibm/ejs/container/BeanO;Ljava/lang/Object;)V");

        // -----------------------------------------------------------------------
        // }                     // end of try
        // -----------------------------------------------------------------------
        Label main_try_end = new Label();
        mg.visitLabel(main_try_end); // mark the end

        Label main_finally_begin = new Label();
        mg.visitJumpInsn(JSR, main_finally_begin); // execte the finally

        Label main_tcf_exit = new Label();
        mg.visitJumpInsn(GOTO, main_tcf_exit); // Skip to very end

        // -----------------------------------------------------------------------
        // catch (<checked exception> <ex>)  ->  for each <checked exception>
        // {
        //   exceptionOccurred = true;
        //   throw <ex>;
        // }
        // -----------------------------------------------------------------------
        Label[] main_catch_label = new Label[exceptionTypes.length];
        int caught_ex = mg.newLocal(Type.getType("Ljava/lang/Exception;"));

        for (int i = 0; i < exceptionTypes.length; i++)
        {
            main_catch_label[i] = new Label();
            mg.visitLabel(main_catch_label[i]);
            mg.storeLocal(caught_ex);
            mg.push(true);
            mg.storeLocal(exceptionOccurred);
            mg.loadLocal(caught_ex);
            mg.visitInsn(ATHROW);
        }

        // -----------------------------------------------------------------------
        // catch (Throwable th)
        // {
        //   exceptionOccurred = true;
        //
        //   throw super.newCreateFailureException_Local(th);              [Local]
        // [or]
        //   throw new CreateFailureException(th);                        [Remote]
        // }
        // -----------------------------------------------------------------------
        Label main_catch_throwable = new Label();
        mg.visitLabel(main_catch_throwable);
        int main_th = mg.newLocal(Type.getType("Ljava/lang/Throwable;"));
        mg.storeLocal(main_th);
        mg.push(true);
        mg.storeLocal(exceptionOccurred);

        if (isLocal)
        {
            mg.loadThis();
            mg.loadLocal(main_th);
            mg.visitMethodInsn(INVOKESPECIAL,
                               "com/ibm/ejs/container/EJSHome",
                               "newCreateFailureException_Local",
                               "(Ljava/lang/Throwable;)Ljavax/ejb/EJBException;");
        }
        else
        {
            mg.visitTypeInsn(NEW, "com/ibm/ejs/container/CreateFailureException");
            mg.visitInsn(DUP);
            mg.loadLocal(main_th);
            mg.visitMethodInsn(INVOKESPECIAL,
                               "com/ibm/ejs/container/CreateFailureException",
                               "<init>", "(Ljava/lang/Throwable;)V");
        }
        mg.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        // finally
        // {
        // -----------------------------------------------------------------------
        Label main_finally_ex = new Label();
        mg.visitLabel(main_finally_ex); // finally - after caught exception
        int main_finally_th = mg.newLocal(Type.getType("Ljava/lang/Throwable;"));
        mg.storeLocal(main_finally_th);
        mg.visitJumpInsn(JSR, main_finally_begin); // finally - jump/return to actual code
        mg.loadLocal(main_finally_th);
        mg.visitInsn(ATHROW); // finally - re-throw exception

        mg.visitLabel(main_finally_begin); // finally - after normal code flow

        int main_finally_return = mg.newLocal(Type.getType("Ljava/lang/Object;"));
        mg.storeLocal(main_finally_return);

        // -----------------------------------------------------------------------
        //   if ( exceptionOccurred )
        //   {
        // -----------------------------------------------------------------------
        Label if_exception_else = mg.newLabel();
        Label if_exception_end = mg.newLabel();
        mg.loadLocal(exceptionOccurred);
        mg.visitJumpInsn(IFEQ, if_exception_else); // if equal false

        // -----------------------------------------------------------------------
        //     super.createFailure(beanO);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "createFailure", "(Lcom/ibm/ejs/container/BeanO;)V");
        mg.visitJumpInsn(GOTO, if_exception_end);

        // -----------------------------------------------------------------------
        //   }
        //   else if ( preEjbCreateCalled )
        //   {
        // -----------------------------------------------------------------------
        mg.visitLabel(if_exception_else);
        mg.loadLocal(preEjbCreateCalled);
        mg.visitJumpInsn(IFEQ, if_exception_end); // if equal false

        // -----------------------------------------------------------------------
        //     super.afterPostCreatCompletion(beanO);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "afterPostCreateCompletion", "(Lcom/ibm/ejs/container/BeanO;)V");

        // -----------------------------------------------------------------------
        //   }        // end if exceptionOccurred else preEjbCreateCalled
        // -----------------------------------------------------------------------
        mg.visitLabel(if_exception_end);

        // -----------------------------------------------------------------------
        // }                     // end of try / catch / finally
        // -----------------------------------------------------------------------
        // Return from finally, either to the end of the try-catch-finally
        // below for normal code flow... or to the beginning of the finally
        // above for the exception path, to rethrow the exception.
        mg.ret(main_finally_return);

        mg.visitLabel(main_tcf_exit);

        // -----------------------------------------------------------------------
        // return returnValue;
        // -----------------------------------------------------------------------
        mg.loadLocal(returnValue);
        mg.returnValue();

        // -----------------------------------------------------------------------
        // All Try-Catch-Finally definitions for above
        // -----------------------------------------------------------------------
        for (int i = 0; i < exceptionTypes.length; i++)
        {
            mg.visitTryCatchBlock(main_try_begin, main_try_end,
                                  main_catch_label[i],
                                  exceptionTypes[i].getInternalName());
        }
        mg.visitTryCatchBlock(main_try_begin, main_try_end,
                              main_catch_throwable, "java/lang/Throwable");
        mg.visitTryCatchBlock(main_try_begin, main_finally_ex, main_finally_ex, null);

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

    private static void addBMPEJBHomeFindByPrimaryKeyMethod(ClassWriter cw,
                                                            Method method,
                                                            Class<?> ejbClass,
                                                            Class<?> pkeyClass,
                                                            String implClassName,
                                                            String beanName,
                                                            boolean isLocal)
                    throws EJBConfigurationException
    {
        GeneratorAdapter mg;
        Method ejbFindMethod;

        String methodName = method.getName();
        String ejbFindMethodName = "ejbF" + methodName.substring(1);
        String methodSignature = MethodAttribUtils.jdiMethodSignature(method);

        // -----------------------------------------------------------------------
        // Determine the name and signature of the implementation method...
        //
        // This will be ejbFind<XXX>,  where the signature matches that of
        // this find method, except the return type. ejbFind must return the
        // primary key.
        // -----------------------------------------------------------------------
        try
        {
            ejbFindMethod = ejbClass.getMethod(ejbFindMethodName,
                                               method.getParameterTypes());
        } catch (NoSuchMethodException ex)
        {
            // FFDC is not needed, as a meaningful exception is being thrown.
            // FFDCFilter.processException(ejbex, CLASS_NAME + ".addBMPEJBHomeFindByPrimaryKeyMethod", "1716");
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_NO_EJBFIND_METHOD_CNTR5030E",
                     new Object[] { beanName,
                                   method.getDeclaringClass().getName(),
                                   method.getName(),
                                   ejbClass.getName() });
            throw new EJBConfigurationException("ejbfind method required for home method " +
                                                method.getName() + methodSignature + " of interface " +
                                                method.getDeclaringClass().getName() + " for bean " +
                                                beanName + ", and a corresponding method was not found " +
                                                "on class " + ejbClass.getName());
        }
        if (ejbFindMethod.getReturnType() != pkeyClass)
        {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_INVALID_EJBFIND_RETURN_CNTR5031E",
                     new Object[] { beanName,
                                   ejbClass.getName(),
                                   ejbFindMethod.getName(),
                                   ejbFindMethod.getReturnType().getName(),
                                   pkeyClass.getName() });
            throw new EJBConfigurationException("Method " + ejbFindMethod.getName() + " of class " +
                                                ejbFindMethod.getReturnType().getName() + " with return type " +
                                                ejbFindMethod.getReturnType().getName() +
                                                " must return the primary key type " + pkeyClass.getName() +
                                                " : " + beanName);
        }
        String ejbFindMethodSignature =
                        MethodAttribUtils.jdiMethodSignature(ejbFindMethod);

        String getBean = "getBean";
        String getBean_sig = "(Ljava/lang/Object;)Ljavax/ejb/EJBObject;";
        String activateBean = "activateBean";
        String activateBean_sig = "(Ljava/lang/Object;)Ljavax/ejb/EJBObject;";

        if (isLocal)
        {
            methodName += "_Local";
            getBean = "getBean_Local";
            getBean_sig = "(Ljava/lang/Object;)Ljavax/ejb/EJBLocalObject;";
            activateBean = "activateBean_Local";
            activateBean_sig = "(Ljava/lang/Object;)Ljavax/ejb/EJBLocalObject;";
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, INDENT + "adding method : " +
                         methodName + " " + methodSignature);
            Tr.debug(tc, INDENT + "              : " +
                         ejbFindMethodName + " " + ejbFindMethodSignature);
        }

        Type returnType = Type.getType(method.getReturnType());
        Type[] argTypes = getTypes(method.getParameterTypes());
        Type[] exceptionTypes = getTypes(method.getExceptionTypes());

        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method(methodName,
                        returnType,
                        argTypes);

        mg = new GeneratorAdapter(ACC_PUBLIC, m, null, exceptionTypes, cw);

        // -----------------------------------------------------------------------
        // Begin Method Code...
        // -----------------------------------------------------------------------
        mg.visitCode();

        // -----------------------------------------------------------------------
        // <return type> returnValue = false | 0 | null;
        // -----------------------------------------------------------------------
        int returnValue = -1;
        if (returnType != Type.VOID_TYPE)
        {
            returnValue = mg.newLocal(returnType);
            switch (returnType.getSort())
            {
                case Type.BOOLEAN:
                    mg.push(false);
                    break;

                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    mg.push(0);
                    break;

                case Type.FLOAT:
                    mg.push((float) 0);
                    break;

                case Type.LONG:
                    mg.push((long) 0);
                    break;

                case Type.DOUBLE:
                    mg.push((double) 0);
                    break;

                // ARRAY & OBJECT - push a null on the stack
                default:
                    mg.visitInsn(ACONST_NULL);
                    break;
            }
            mg.storeLocal(returnValue);
        }

        // -----------------------------------------------------------------------
        // returnValue = (<return type>)super.getBean_Local(<arg>);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadArgs(0, 1);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           getBean, getBean_sig);
        mg.checkCast(returnType);
        mg.storeLocal(returnValue);

        // -----------------------------------------------------------------------
        // if ( returnValue == null )
        // {
        // -----------------------------------------------------------------------
        Label if_returnValue_end = mg.newLabel();
        mg.loadLocal(returnValue);
        mg.visitJumpInsn(IFNONNULL, if_returnValue_end); // if != null

        // -----------------------------------------------------------------------
        //   EntityBeanO beanO = null;
        //   <pkey type> pkey = null;
        // -----------------------------------------------------------------------
        int beanO = mg.newLocal(Type.getType("Lcom/ibm/ejs/container/EntityBeanO;"));
        mg.visitInsn(ACONST_NULL);
        mg.storeLocal(beanO);

        int pkey = mg.newLocal(Type.getType(pkeyClass));
        mg.visitInsn(ACONST_NULL);
        mg.storeLocal(pkey);

        // -----------------------------------------------------------------------
        //   try
        //   {
        // -----------------------------------------------------------------------
        Label main_try_begin = new Label();
        mg.visitLabel(main_try_begin);

        // -----------------------------------------------------------------------
        //     beanO = super.getFindByPrimaryKeyEntityBeanO();
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "getFindByPrimaryKeyEntityBeanO", "()Lcom/ibm/ejs/container/EntityBeanO;");
        mg.storeLocal(beanO);

        // -----------------------------------------------------------------------
        //     <bean impl> bean = (<bean impl>)beanO.getBeanInstance();
        // -----------------------------------------------------------------------
        Type implType = Type.getType("L" + implClassName + ";");
        int bean = mg.newLocal(implType);
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKEVIRTUAL,
                           "com/ibm/ejs/container/BeanO",
                           "getBeanInstance", "()Ljava/lang/Object;");
        mg.checkCast(implType);
        mg.storeLocal(bean);

        // -----------------------------------------------------------------------
        //     pkey = bean.ejbFindByPrimaryKey(<args...>);
        // -----------------------------------------------------------------------
        mg.loadLocal(bean);
        mg.loadArgs(0, argTypes.length);
        mg.visitMethodInsn(INVOKEVIRTUAL,
                           implClassName,
                           ejbFindMethodName, ejbFindMethodSignature);
        mg.storeLocal(pkey);

        // -----------------------------------------------------------------------
        //     super.releaseFinderEntityBeanO(beanO);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "releaseFinderEntityBeanO", "(Lcom/ibm/ejs/container/EntityBeanO;)V");

        // -----------------------------------------------------------------------
        //     beanO = null;
        // -----------------------------------------------------------------------
        mg.visitInsn(ACONST_NULL);
        mg.storeLocal(beanO);

        // -----------------------------------------------------------------------
        //   }                     // end of try
        // -----------------------------------------------------------------------
        Label main_try_end = new Label();
        mg.visitLabel(main_try_end); // mark the end

        Label main_finally_begin = new Label();
        mg.visitJumpInsn(JSR, main_finally_begin); // execte the finally

        Label main_tcf_exit = new Label();
        mg.visitJumpInsn(GOTO, main_tcf_exit); // Skip to very end

        // -----------------------------------------------------------------------
        //   catch (<checked exception> <ex>)  ->  for each <checked exception>
        //   {
        //     super.releaseFinderEntityBeanO(beanO);
        //     beanO = null;
        //     throw <ex>;
        //   }
        // -----------------------------------------------------------------------
        Label[] main_catch_label = new Label[exceptionTypes.length];
        int caught_ex = mg.newLocal(Type.getType("Ljava/lang/Exception;"));

        for (int i = 0; i < exceptionTypes.length; i++)
        {
            main_catch_label[i] = new Label();
            mg.visitLabel(main_catch_label[i]);
            mg.storeLocal(caught_ex);
            mg.loadThis();
            mg.loadLocal(beanO);
            mg.visitMethodInsn(INVOKESPECIAL,
                               "com/ibm/ejs/container/EJSHome",
                               "releaseFinderEntityBeanO", "(Lcom/ibm/ejs/container/EntityBeanO;)V");
            mg.visitInsn(ACONST_NULL);
            mg.storeLocal(beanO);
            mg.loadLocal(caught_ex);
            mg.visitInsn(ATHROW);
        }

        // -----------------------------------------------------------------------
        //   finally
        //   {
        // -----------------------------------------------------------------------
        Label main_finally_ex = new Label();
        mg.visitLabel(main_finally_ex); // finally - after caught exception
        int main_finally_th = mg.newLocal(Type.getType("Ljava/lang/Throwable;"));
        mg.storeLocal(main_finally_th);
        mg.visitJumpInsn(JSR, main_finally_begin); // finally - jump/return to actual code
        mg.loadLocal(main_finally_th);
        mg.visitInsn(ATHROW); // finally - re-throw exception

        mg.visitLabel(main_finally_begin); // finally - after normal code flow

        int main_finally_return = mg.newLocal(Type.getType("Ljava/lang/Object;"));
        mg.storeLocal(main_finally_return);

        // -----------------------------------------------------------------------
        //     if ( beanO != null )
        //     {
        // -----------------------------------------------------------------------
        Label if_notBeanO_end = mg.newLabel();
        mg.loadLocal(beanO);
        mg.visitJumpInsn(IFNULL, if_notBeanO_end); // if == null

        // -----------------------------------------------------------------------
        //       super.discardFinderEntityBeanO(beanO);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "discardFinderEntityBeanO", "(Lcom/ibm/ejs/container/EntityBeanO;)V");

        // -----------------------------------------------------------------------
        //     }        // end if beanO != null
        // -----------------------------------------------------------------------
        mg.visitLabel(if_notBeanO_end);

        // -----------------------------------------------------------------------
        //   }                     // end of try / catch / finally
        // -----------------------------------------------------------------------
        // Return from finally, either to the end of the try-catch-finally
        // below for normal code flow... or to the beginning of the finally
        // above for the exception path, to rethrow the exception.
        mg.ret(main_finally_return);

        mg.visitLabel(main_tcf_exit);

        // -----------------------------------------------------------------------
        //   returnValue = <return type>super.activateBean[_Local](pkey);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(pkey);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           activateBean, activateBean_sig);
        mg.checkCast(returnType);
        mg.storeLocal(returnValue);

        // -----------------------------------------------------------------------
        // }              // end if ( returnValue == null )
        // -----------------------------------------------------------------------
        mg.visitLabel(if_returnValue_end);

        // -----------------------------------------------------------------------
        // return returnValue;
        // -----------------------------------------------------------------------
        mg.loadLocal(returnValue);
        mg.returnValue();

        // -----------------------------------------------------------------------
        // All Try-Catch-Finally definitions for above
        // -----------------------------------------------------------------------
        for (int i = 0; i < exceptionTypes.length; i++)
        {
            mg.visitTryCatchBlock(main_try_begin, main_try_end,
                                  main_catch_label[i],
                                  exceptionTypes[i].getInternalName());
        }
        mg.visitTryCatchBlock(main_try_begin, main_finally_ex, main_finally_ex, null);

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

    private static void addBMPEJBHomeFindMethod(ClassWriter cw,
                                                Method method,
                                                Class<?> ejbClass,
                                                Class<?> pkeyClass,
                                                String implClassName,
                                                String beanName,
                                                boolean isLocal)
                    throws EJBConfigurationException
    {
        GeneratorAdapter mg;
        Method ejbFindMethod;

        String methodName = method.getName();
        String ejbFindMethodName = "ejbF" + methodName.substring(1);
        String methodSignature = MethodAttribUtils.jdiMethodSignature(method);

        // -----------------------------------------------------------------------
        // Determine the name and signature of the implementation method...
        //
        // This will be ejbFind<XXX>,  where the signature matches that of
        // this find method, except the return type. ejbFind must return the
        // primary key.
        // -----------------------------------------------------------------------
        try
        {
            ejbFindMethod = ejbClass.getMethod(ejbFindMethodName,
                                               method.getParameterTypes());
        } catch (NoSuchMethodException ex)
        {
            // FFDC is not needed, as a meaningful exception is being thrown.
            // FFDCFilter.processException(ejbex, CLASS_NAME + ".addBMPEJBHomeFindMethod", "2073");
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_NO_EJBFIND_METHOD_CNTR5030E",
                     new Object[] { beanName,
                                   method.getDeclaringClass().getName(),
                                   method.getName(),
                                   ejbClass.getName() });
            throw new EJBConfigurationException("ejbfind method required for home method " +
                                                method.getName() + methodSignature + " of interface " +
                                                method.getDeclaringClass().getName() + " for bean " +
                                                beanName + ", and a corresponding method was not found " +
                                                "on class " + ejbClass.getName());
        }

        String ejbFindMethodSignature =
                        MethodAttribUtils.jdiMethodSignature(ejbFindMethod);

        String postEjbFindMethodName = null;
        String postEjbFindMethodSignature = null;

        Class<?> returnTypeClass = ejbFindMethod.getReturnType();
        Type ejbFindReturnType = Type.getType(returnTypeClass);
        boolean postEjbFindIsActivate = false;
        if (returnTypeClass == pkeyClass)
        {
            // single object finder
            postEjbFindIsActivate = true;
            postEjbFindMethodName = "activateBean";
            postEjbFindMethodSignature = "(Ljava/lang/Object;)Ljavax/ejb/EJBObject;";
        }
        else if (returnTypeClass == Collection.class)
        {
            // multi-object finder via Collection
            postEjbFindMethodName = "getCMP20Collection";
            postEjbFindMethodSignature = "(Ljava/util/Collection;)Ljava/util/Collection;";
        }
        else if (returnTypeClass == Enumeration.class)
        {
            // multi-object finder via Enumeration
            postEjbFindMethodName = "getCMP20Enumeration";
            postEjbFindMethodSignature = "(Ljava/util/Enumeration;)Ljava/util/Enumeration;";
        }
        else
        {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_INVALID_CUSTOM_EJBFIND_RETURN_CNTR5032E",
                     new Object[] { beanName,
                                   ejbClass.getName(),
                                   ejbFindMethod.getName(),
                                   ejbFindMethod.getReturnType().getName(),
                                   pkeyClass.getName() });
            throw new EJBConfigurationException("Method " + ejbFindMethod.getName() + " of class " +
                                                ejbFindMethod.getReturnType().getName() + " with return type " +
                                                ejbFindMethod.getReturnType().getName() +
                                                " must return the primary key type " + pkeyClass.getName() +
                                                ", Collection, or Enumeration : " + beanName);
        }

        if (isLocal)
        {
            methodName += "_Local";
            postEjbFindMethodName += "_Local";
            if (postEjbFindIsActivate) // activateBean
                postEjbFindMethodSignature = "(Ljava/lang/Object;)Ljavax/ejb/EJBLocalObject;";
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, INDENT + "adding method : " +
                         methodName + " " + methodSignature);
            Tr.debug(tc, INDENT + "              : " +
                         ejbFindMethodName + " " + ejbFindMethodSignature);
        }

        Type returnType = Type.getType(method.getReturnType());
        Type[] argTypes = getTypes(method.getParameterTypes());
        Type[] exceptionTypes = getTypes(method.getExceptionTypes());

        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method(methodName,
                        returnType,
                        argTypes);

        mg = new GeneratorAdapter(ACC_PUBLIC, m, null, exceptionTypes, cw);

        // -----------------------------------------------------------------------
        // Begin Method Code...
        // -----------------------------------------------------------------------
        mg.visitCode();

        // -----------------------------------------------------------------------
        // <return type> returnValue = false | 0 | null;
        // -----------------------------------------------------------------------
        int returnValue = -1;
        if (returnType != Type.VOID_TYPE)
        {
            returnValue = mg.newLocal(returnType);
            switch (returnType.getSort())
            {
                case Type.BOOLEAN:
                    mg.push(false);
                    break;

                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    mg.push(0);
                    break;

                case Type.FLOAT:
                    mg.push((float) 0);
                    break;

                case Type.LONG:
                    mg.push((long) 0);
                    break;

                case Type.DOUBLE:
                    mg.push((double) 0);
                    break;

                // ARRAY & OBJECT - push a null on the stack
                default:
                    mg.visitInsn(ACONST_NULL);
                    break;
            }
            mg.storeLocal(returnValue);
        }

        // -----------------------------------------------------------------------
        // EntityBeanO beanO = null;
        // -----------------------------------------------------------------------
        int beanO = mg.newLocal(Type.getType("Lcom/ibm/ejs/container/EntityBeanO;"));
        mg.visitInsn(ACONST_NULL);
        mg.storeLocal(beanO);

        // -----------------------------------------------------------------------
        // try
        // {
        // -----------------------------------------------------------------------
        Label main_try_begin = new Label();
        mg.visitLabel(main_try_begin);

        // -----------------------------------------------------------------------
        //   beanO = super.getFinderEntityBeanO();
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "getFinderEntityBeanO", "()Lcom/ibm/ejs/container/EntityBeanO;");
        mg.storeLocal(beanO);

        // -----------------------------------------------------------------------
        //   <bean impl> bean = (<bean impl>)beanO.getBeanInstance();
        // -----------------------------------------------------------------------
        Type implType = Type.getType("L" + implClassName + ";");
        int bean = mg.newLocal(implType);
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKEVIRTUAL,
                           "com/ibm/ejs/container/BeanO",
                           "getBeanInstance", "()Ljava/lang/Object;");
        mg.checkCast(implType);
        mg.storeLocal(bean);

        // -----------------------------------------------------------------------
        //   <pkey type> ejbFindReturnValue = bean.ejbFind<Method>(<args...>);
        //      or
        //   Collection  ejbFindReturnValue = bean.ejbFind<Method>(<args...>);
        //      or
        //   Enumeration ejbFindReturnValue = bean.ejbFind<Method>(<args...>);
        // -----------------------------------------------------------------------
        int ejbFindReturnValue = mg.newLocal(ejbFindReturnType);
        mg.loadLocal(bean);
        mg.loadArgs(0, argTypes.length);
        mg.visitMethodInsn(INVOKEVIRTUAL,
                           implClassName,
                           ejbFindMethodName, ejbFindMethodSignature);
        mg.storeLocal(ejbFindReturnValue);

        // -----------------------------------------------------------------------
        //   returnValue = (<bean type>)activateBean[_Local](ejbFindReturnValue);
        //       or
        //   returnValue = getCMP20Collection[_Local](ejbFindReturnValue);
        //       or
        //   returnValue = getCMP20Enumeration[_Local](ejbFindReturnValue);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(ejbFindReturnValue);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           postEjbFindMethodName, postEjbFindMethodSignature);
        if (postEjbFindIsActivate)
            mg.checkCast(returnType);
        mg.storeLocal(returnValue);

        // -----------------------------------------------------------------------
        //   super.releaseFinderEntityBeanO(beanO);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "releaseFinderEntityBeanO", "(Lcom/ibm/ejs/container/EntityBeanO;)V");

        // -----------------------------------------------------------------------
        //   beanO = null;
        // -----------------------------------------------------------------------
        mg.visitInsn(ACONST_NULL);
        mg.storeLocal(beanO);

        // -----------------------------------------------------------------------
        // }                     // end of try
        // -----------------------------------------------------------------------
        Label main_try_end = new Label();
        mg.visitLabel(main_try_end); // mark the end

        Label main_finally_begin = new Label();
        mg.visitJumpInsn(JSR, main_finally_begin); // execte the finally

        Label main_tcf_exit = new Label();
        mg.visitJumpInsn(GOTO, main_tcf_exit); // Skip to very end

        // -----------------------------------------------------------------------
        // catch (<checked exception> <ex>)  ->  for each <checked exception>
        // {
        //   super.releaseFinderEntityBeanO(beanO);
        //   beanO = null;
        //   throw <ex>;
        // }
        // -----------------------------------------------------------------------
        Label[] main_catch_label = new Label[exceptionTypes.length];
        int caught_ex = mg.newLocal(Type.getType("Ljava/lang/Exception;"));

        for (int i = 0; i < exceptionTypes.length; i++)
        {
            main_catch_label[i] = new Label();
            mg.visitLabel(main_catch_label[i]);
            mg.storeLocal(caught_ex);
            mg.loadThis();
            mg.loadLocal(beanO);
            mg.visitMethodInsn(INVOKESPECIAL,
                               "com/ibm/ejs/container/EJSHome",
                               "releaseFinderEntityBeanO", "(Lcom/ibm/ejs/container/EntityBeanO;)V");
            mg.visitInsn(ACONST_NULL);
            mg.storeLocal(beanO);
            mg.loadLocal(caught_ex);
            mg.visitInsn(ATHROW);
        }

        // -----------------------------------------------------------------------
        // finally
        // {
        // -----------------------------------------------------------------------
        Label main_finally_ex = new Label();
        mg.visitLabel(main_finally_ex); // finally - after caught exception
        int main_finally_th = mg.newLocal(Type.getType("Ljava/lang/Throwable;"));
        mg.storeLocal(main_finally_th);
        mg.visitJumpInsn(JSR, main_finally_begin); // finally - jump/return to actual code
        mg.loadLocal(main_finally_th);
        mg.visitInsn(ATHROW); // finally - re-throw exception

        mg.visitLabel(main_finally_begin); // finally - after normal code flow

        int main_finally_return = mg.newLocal(Type.getType("Ljava/lang/Object;"));
        mg.storeLocal(main_finally_return);

        // -----------------------------------------------------------------------
        //   if ( beanO != null )
        //   {
        // -----------------------------------------------------------------------
        Label if_notBeanO_end = mg.newLabel();
        mg.loadLocal(beanO);
        mg.visitJumpInsn(IFNULL, if_notBeanO_end); // if == null

        // -----------------------------------------------------------------------
        //     super.discardFinderEntityBeanO(beanO);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "discardFinderEntityBeanO", "(Lcom/ibm/ejs/container/EntityBeanO;)V");

        // -----------------------------------------------------------------------
        //   }        // end if beanO != null
        // -----------------------------------------------------------------------
        mg.visitLabel(if_notBeanO_end);

        // -----------------------------------------------------------------------
        // }                     // end of try / catch / finally
        // -----------------------------------------------------------------------
        // Return from finally, either to the end of the try-catch-finally
        // below for normal code flow... or to the beginning of the finally
        // above for the exception path, to rethrow the exception.
        mg.ret(main_finally_return);

        mg.visitLabel(main_tcf_exit);

        // -----------------------------------------------------------------------
        // return returnValue;
        // -----------------------------------------------------------------------
        mg.loadLocal(returnValue);
        mg.returnValue();

        // -----------------------------------------------------------------------
        // All Try-Catch-Finally definitions for above
        // -----------------------------------------------------------------------
        for (int i = 0; i < exceptionTypes.length; i++)
        {
            mg.visitTryCatchBlock(main_try_begin, main_try_end,
                                  main_catch_label[i],
                                  exceptionTypes[i].getInternalName());
        }
        mg.visitTryCatchBlock(main_try_begin, main_finally_ex, main_finally_ex, null);

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

    private static void addBMPEJBHomeHomeMethod(ClassWriter cw,
                                                Method method,
                                                Class<?> ejbClass,
                                                String implClassName,
                                                String beanName)
                    throws EJBConfigurationException
    {
        GeneratorAdapter mg;
        Method ejbHomeMethod;

        String methodName = method.getName();
        StringBuilder ejbHomeBuilder = new StringBuilder("ejbHome");
        ejbHomeBuilder.append(methodName.substring(0, 1).toUpperCase());
        ejbHomeBuilder.append(methodName.substring(1));
        String ejbHomeMethodName = ejbHomeBuilder.toString();
        String methodSignature = MethodAttribUtils.jdiMethodSignature(method);

        // -----------------------------------------------------------------------
        // Determine the name and signature of the implementation method...
        //
        // This will be ejbFind<XXX>,  where the signature matches that of
        // this find method, except the return type. ejbFind must return the
        // primary key.
        // -----------------------------------------------------------------------
        try
        {
            ejbHomeMethod = ejbClass.getMethod(ejbHomeMethodName,
                                               method.getParameterTypes());
        } catch (NoSuchMethodException ex)
        {
            // FFDC is not needed, as a meaningful exception is being thrown.
            // FFDCFilter.processException(ejbex, CLASS_NAME + ".addBMPEJBHomeHomeMethod", "2433");
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_NO_EJBHOME_METHOD_CNTR5033E",
                     new Object[] { beanName,
                                   method.getDeclaringClass().getName(),
                                   method.getName(),
                                   ejbClass.getName() });
            throw new EJBConfigurationException("ejbHome method required for home method " +
                                                method.getName() + methodSignature + " of interface " +
                                                method.getDeclaringClass().getName() + " for bean " +
                                                beanName + ", and a corresponding method was not found " +
                                                "on class " + ejbClass.getName());
        }
        if (ejbHomeMethod.getReturnType() != method.getReturnType())
        {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_INVALID_EJBHOME_RETURN_CNTR5034E",
                     new Object[] { beanName,
                                   ejbClass.getName(),
                                   ejbHomeMethod.getName(),
                                   ejbHomeMethod.getReturnType().getName(),
                                   method.getReturnType() });
            throw new EJBConfigurationException("Method " + ejbHomeMethod.getName() + " of class " +
                                                ejbHomeMethod.getReturnType().getName() + " with return type " +
                                                ejbHomeMethod.getReturnType().getName() +
                                                " must return " + method.getReturnType() +
                                                " : " + beanName);
        }
        String ejbHomeMethodSignature =
                        MethodAttribUtils.jdiMethodSignature(ejbHomeMethod);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Tr.debug(tc, INDENT + "adding method : " +
                         methodName + " " + methodSignature);
            Tr.debug(tc, INDENT + "              : " +
                         ejbHomeMethodName + " " + ejbHomeMethodSignature);
        }

        Type returnType = Type.getType(method.getReturnType());
        Type[] argTypes = getTypes(method.getParameterTypes());
        Type[] exceptionTypes = getTypes(method.getExceptionTypes());

        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method(methodName,
                        returnType,
                        argTypes);

        mg = new GeneratorAdapter(ACC_PUBLIC, m, null, exceptionTypes, cw);

        // -----------------------------------------------------------------------
        // Begin Method Code...
        // -----------------------------------------------------------------------
        mg.visitCode();

        // -----------------------------------------------------------------------
        // <return type> returnValue = false | 0 | null;
        // -----------------------------------------------------------------------
        int returnValue = -1;
        if (returnType != Type.VOID_TYPE)
        {
            returnValue = mg.newLocal(returnType);
            switch (returnType.getSort())
            {
                case Type.BOOLEAN:
                    mg.push(false);
                    break;

                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    mg.push(0);
                    break;

                case Type.FLOAT:
                    mg.push((float) 0);
                    break;

                case Type.LONG:
                    mg.push((long) 0);
                    break;

                case Type.DOUBLE:
                    mg.push((double) 0);
                    break;

                // ARRAY & OBJECT - push a null on the stack
                default:
                    mg.visitInsn(ACONST_NULL);
                    break;
            }
            mg.storeLocal(returnValue);
        }

        // -----------------------------------------------------------------------
        // EntityBeanO beanO = null;
        // -----------------------------------------------------------------------
        int beanO = mg.newLocal(Type.getType("Lcom/ibm/ejs/container/EntityBeanO;"));
        mg.visitInsn(ACONST_NULL);
        mg.storeLocal(beanO);

        // -----------------------------------------------------------------------
        // try
        // {
        // -----------------------------------------------------------------------
        Label main_try_begin = new Label();
        mg.visitLabel(main_try_begin);

        // -----------------------------------------------------------------------
        //   beanO = super.getHomeMethodEntityBeanO();
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "getHomeMethodEntityBeanO", "()Lcom/ibm/ejs/container/EntityBeanO;");
        mg.storeLocal(beanO);

        // -----------------------------------------------------------------------
        //   <bean impl> bean = (<bean impl>)beanO.getBeanInstance();
        // -----------------------------------------------------------------------
        Type implType = Type.getType("L" + implClassName + ";");
        int bean = mg.newLocal(implType);
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKEVIRTUAL,
                           "com/ibm/ejs/container/BeanO",
                           "getBeanInstance", "()Ljava/lang/Object;");
        mg.checkCast(implType);
        mg.storeLocal(bean);

        // -----------------------------------------------------------------------
        //   bean.ejbHome<Method>(<args...>);
        //      or
        //   returnValue = bean.ejbHome<Method>(<args...>);
        // -----------------------------------------------------------------------
        mg.loadLocal(bean);
        mg.loadArgs(0, argTypes.length);
        mg.visitMethodInsn(INVOKEVIRTUAL,
                           implClassName,
                           ejbHomeMethodName, ejbHomeMethodSignature);
        if (returnType != Type.VOID_TYPE)
        {
            mg.storeLocal(returnValue);
        }

        // -----------------------------------------------------------------------
        //   super.releaseHomeMethodEntityBeanO(beanO);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "releaseHomeMethodEntityBeanO", "(Lcom/ibm/ejs/container/EntityBeanO;)V");

        // -----------------------------------------------------------------------
        //   beanO = null;
        // -----------------------------------------------------------------------
        mg.visitInsn(ACONST_NULL);
        mg.storeLocal(beanO);

        // -----------------------------------------------------------------------
        // }                     // end of try
        // -----------------------------------------------------------------------
        Label main_try_end = new Label();
        mg.visitLabel(main_try_end); // mark the end

        Label main_finally_begin = new Label();
        mg.visitJumpInsn(JSR, main_finally_begin); // execte the finally

        Label main_tcf_exit = new Label();
        mg.visitJumpInsn(GOTO, main_tcf_exit); // Skip to very end

        // -----------------------------------------------------------------------
        // catch (<checked exception> <ex>)  ->  for each <checked exception>
        // {
        //   super.releaseHomeEntityBeanO(beanO);
        //   beanO = null;
        //   throw <ex>;
        // }
        // -----------------------------------------------------------------------
        Label[] main_catch_label = new Label[exceptionTypes.length];
        int caught_ex = mg.newLocal(Type.getType("Ljava/lang/Exception;"));

        for (int i = 0; i < exceptionTypes.length; i++)
        {
            main_catch_label[i] = new Label();
            mg.visitLabel(main_catch_label[i]);
            mg.storeLocal(caught_ex);
            mg.loadThis();
            mg.loadLocal(beanO);
            mg.visitMethodInsn(INVOKESPECIAL,
                               "com/ibm/ejs/container/EJSHome",
                               "releaseHomeMethodEntityBeanO", "(Lcom/ibm/ejs/container/EntityBeanO;)V");
            mg.visitInsn(ACONST_NULL);
            mg.storeLocal(beanO);
            mg.loadLocal(caught_ex);
            mg.visitInsn(ATHROW);
        }

        // -----------------------------------------------------------------------
        // finally
        // {
        // -----------------------------------------------------------------------
        Label main_finally_ex = new Label();
        mg.visitLabel(main_finally_ex); // finally - after caught exception
        int main_finally_th = mg.newLocal(Type.getType("Ljava/lang/Throwable;"));
        mg.storeLocal(main_finally_th);
        mg.visitJumpInsn(JSR, main_finally_begin); // finally - jump/return to actual code
        mg.loadLocal(main_finally_th);
        mg.visitInsn(ATHROW); // finally - re-throw exception

        mg.visitLabel(main_finally_begin); // finally - after normal code flow

        int main_finally_return = mg.newLocal(Type.getType("Ljava/lang/Object;"));
        mg.storeLocal(main_finally_return);

        // -----------------------------------------------------------------------
        //   if ( beanO != null )
        //   {
        // -----------------------------------------------------------------------
        Label if_notBeanO_end = mg.newLabel();
        mg.loadLocal(beanO);
        mg.visitJumpInsn(IFNULL, if_notBeanO_end); // if == null

        // -----------------------------------------------------------------------
        //     super.discardHomeMethodEntityBeanO(beanO);
        // -----------------------------------------------------------------------
        // TODO : TKB : need to add 'discardHomeMethodEntityBeanO'... and call it
        //              seems the home methods should do connection handle management too?
        mg.loadThis();
        mg.loadLocal(beanO);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "com/ibm/ejs/container/EJSHome",
                           "releaseHomeMethodEntityBeanO", "(Lcom/ibm/ejs/container/EntityBeanO;)V");

        // -----------------------------------------------------------------------
        //   }        // end if beanO != null
        // -----------------------------------------------------------------------
        mg.visitLabel(if_notBeanO_end);

        // -----------------------------------------------------------------------
        // }                     // end of try / catch / finally
        // -----------------------------------------------------------------------
        // Return from finally, either to the end of the try-catch-finally
        // below for normal code flow... or to the beginning of the finally
        // above for the exception path, to rethrow the exception.
        mg.ret(main_finally_return);

        mg.visitLabel(main_tcf_exit);

        // -----------------------------------------------------------------------
        // return <returnValue>;
        // -----------------------------------------------------------------------
        if (returnType != Type.VOID_TYPE) // d499688
        {
            mg.loadLocal(returnValue);
        }
        mg.returnValue();

        // -----------------------------------------------------------------------
        // All Try-Catch-Finally definitions for above
        // -----------------------------------------------------------------------
        for (int i = 0; i < exceptionTypes.length; i++)
        {
            mg.visitTryCatchBlock(main_try_begin, main_try_end,
                                  main_catch_label[i],
                                  exceptionTypes[i].getInternalName());
        }
        mg.visitTryCatchBlock(main_try_begin, main_finally_ex, main_finally_ex, null);

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

}
