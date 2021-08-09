/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import java.lang.reflect.Method;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.util.MethodAttribUtils;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import static com.ibm.ws.ejbcontainer.jitdeploy.EJBUtils.getMethodId;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.convertClassName;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.sortExceptions;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.getTypes;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.unbox;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.writeToClassFile;

import static org.objectweb.asm.Opcodes.*;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.INDENT;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_Exception;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_Object_ARRAY;

/**
 * Provides Just In Time runtime deployment of WebService Endpoint EJB Proxys. <p>
 */
public final class WSEJBProxy
{
    private static final TraceComponent tc = Tr.register(WSEJBProxy.class,
                                                         JITUtils.JIT_TRACE_GROUP,
                                                         JITUtils.JIT_RSRC_BUNDLE);

    /**
     * Core method for generating the EJB Proxy class bytes. Intended for
     * use by JITDeploy only (should not be called directly). <p>
     * 
     * @param proxyClassName name of the proxy class to be generated.
     * @param proxyInterface Interface implemented by the generated proxy (optional).
     * @param proxyMethods Set of all WebService Endpoint methods that must
     *            be implemented by the proxy being generated.
     * @param methodInfos EJB method info objects for all of the
     *            WebService Endpoint methods for the type of
     *            proxy being generated.
     * @param ejbClassName Name of the EJB implementation class, that
     *            the generated proxy will route methods to.
     * @param beanName Name of the EJB (for messages)
     **/
    static byte[] generateClassBytes(String proxyClassName,
                                     Class proxyInterface,
                                     Method[] proxyMethods,
                                     EJBMethodInfoImpl[] methodInfos,
                                     String ejbClassName,
                                     String beanName)
                    throws EJBConfigurationException
    {
        // ASM uses 'internal' java class names (like JNI) where '/' is
        // used instead of '.', so convert the parameters to 'internal' format.
        String internalClassName = convertClassName(proxyClassName);
        String internalInterfaceName = convertClassName(proxyInterface);
        String internalEJBClassName = convertClassName(ejbClassName);
        String internalParentName = "com/ibm/ejs/container/WSEJBProxy";
        String[] internalImplements = (internalInterfaceName != null)
                        ? (new String[] { internalInterfaceName }) : null;

        if (TraceComponent.isAnyTracingEnabled())
        {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "generateClassBytes");
            if (tc.isDebugEnabled())
            {
                Tr.debug(tc, INDENT + "className = " + internalClassName);
                Tr.debug(tc, INDENT + "interface = " + internalInterfaceName);
                Tr.debug(tc, INDENT + "parent    = " + internalParentName);
                Tr.debug(tc, INDENT + "ejb       = " + internalEJBClassName);
            }
        }

        // Create the ASM Class Writer to write out a proxy
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS); //F743-11995

        // Define the proxy Class object
        cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER,
                 internalClassName,
                 null,
                 internalParentName,
                 internalImplements);

        // Define the source code file and debug settings
        String sourceFileName = proxyClassName.substring(proxyClassName.lastIndexOf(".") + 1) + ".java";
        cw.visitSource(sourceFileName, null);

        // Add the public no parameter proxy constructor
        addCtor(cw, internalParentName);

        // Add all of the methods to the proxy, based on the reflected
        // Method objects from the interface.
        int methodId = -1;
        for (int i = 0; i < proxyMethods.length; ++i)
        {
            Method method = proxyMethods[i];
            String implMethodName = method.getName();

            // Business methods must not start with "ejb", per spec.
            if (implMethodName.startsWith("ejb"))
            {
                String interfaceName = ((proxyInterface != null) ? proxyInterface.getName()
                                : "WebService Endpoint");
                // Log the error and throw meaningful exception.
                Tr.error(tc, "JIT_INVALID_MTHD_PREFIX_CNTR5010E",
                         new Object[] { beanName,
                                       interfaceName,
                                       implMethodName });
                throw new EJBConfigurationException("EJB business method " + implMethodName +
                                                    " on interface " + interfaceName +
                                                    " must not start with 'ejb'.");
            }

            // Determine the Method Id that will be hard coded into the
            // proxy to allow preInvoke to quickly find the correct method.
            methodId = getMethodId(method, proxyMethods, ++methodId);

            // Determine if interceptors are called from methodinfo.
            EJBMethodInfoImpl methodInfo = methodInfos[methodId];
            boolean aroundInvoke =
                            (methodInfo.getAroundInterceptorProxies() != null); // F743-17763.1

            // When 'generics' are used, like for the Provider interface, the
            // signature of the method on the bean implemenation will be different
            // from that of the interface. Since WebServices will be calling using
            // the signature of the implementation, generate the methods using
            // the ejb method (taken from methodInfo).  The 'interface' method
            // would exist on the ejb, but as a 'bridge' method, and may not be
            // needed on the proxy.                                         d540438
            Method ejbMethod = methodInfo.getMethod();

            if (aroundInvoke)
            {
                addEJBInterceptorMethod(cw,
                                        internalClassName,
                                        internalEJBClassName,
                                        ejbMethod);
            }
            else
            {
                addEJBMethod(cw,
                             internalClassName,
                             internalEJBClassName,
                             ejbMethod);
            }

            // If the Prxoy implements an interface, then the 'bridge' method
            // is also needed (if there is one).  This method will not be added
            // as a 'bridge' method, but just as a normal method, because although
            // the customer may have used generics to define the EJB, generics
            // are NOT usd when defining the Proxy.                         d540438
            if (proxyInterface != null)
            {
                Method bridgeMethod = methodInfo.getBridgeMethod();
                if (bridgeMethod != null)
                {
                    if (aroundInvoke)
                    {
                        addEJBInterceptorMethod(cw,
                                                internalClassName,
                                                internalEJBClassName,
                                                bridgeMethod);
                    }
                    else
                    {
                        addEJBMethod(cw,
                                     internalClassName,
                                     internalEJBClassName,
                                     bridgeMethod);
                    }
                } // end if bridgeMethod
            } // end if proxyInterface

        } // end proxyMethods loop

        // Mark the end of the generated proxy class
        cw.visitEnd();

        // Dump the class bytes out to a byte array.
        byte[] classBytes = cw.toByteArray();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            writeToClassFile(internalClassName, classBytes);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "generateClassBytes: " + classBytes.length + " bytes");

        return classBytes;
    }

    /**
     * Adds the default (no arg) constructor. <p>
     * 
     * There are no exceptions in the throws clause; none are required
     * for the constructors of EJB Proxys. <p>
     * 
     * Currently, the generated method body is intentionally empty;
     * EJB Proxys require no initialization in the constructor. <p>
     * 
     * @param cw ASM ClassWriter to add the constructor to.
     * @param parent fully qualified name of the parent class
     *            with '/' as the separator character
     *            (i.e. internal name).
     **/
    private static void addCtor(ClassWriter cw, String parent)
    {
        MethodVisitor mv;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : <init> ()V");

        // -----------------------------------------------------------------------
        // public <Class Name>()
        // {
        // }
        // -----------------------------------------------------------------------
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, parent, "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * Adds a standard EJB Proxy Method. <p>
     * 
     * The method added just calls the EJB instance method directly. <p>
     * 
     * @param cw ASM Class writer to add the method to.
     * @param className name of the proxy class being generated.
     * @param implClassName name of the EJB implementation class.
     * @param method reflection method from the interface defining
     *            method to be added to the proxy.
     **/
    private static void addEJBMethod(ClassWriter cw,
                                     String className,
                                     String implClassName,
                                     Method method)
    {
        GeneratorAdapter mg;
        String methodName = method.getName();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : " + methodName + " " +
                         MethodAttribUtils.jdiMethodSignature(method) +
                         " : isBridge = " + method.isBridge() +
                         " : aroundInvoke = false");

        // Convert the return value, arguments, and exception classes to
        // ASM Type objects, and create the ASM Method object which will
        // be used to actually add the method and method code.
        Type returnType = Type.getType(method.getReturnType());
        Type[] argTypes = getTypes(method.getParameterTypes());
        Type[] exceptionTypes = getTypes(method.getExceptionTypes());

        org.objectweb.asm.commons.Method m =
                        new org.objectweb.asm.commons.Method(methodName,
                                        returnType,
                                        argTypes);

        // Create an ASM GeneratorAdapter object for the ASM Method, which
        // makes generating dynamic code much easier... as it keeps track
        // of the position of the arguements and local variables, etc.
        mg = new GeneratorAdapter(ACC_PUBLIC, m, null, exceptionTypes, cw);

        // -----------------------------------------------------------------------
        // Begin Method Code...
        // -----------------------------------------------------------------------
        mg.visitCode();

        // -----------------------------------------------------------------------
        // Now invoke the business method;
        // - Directly, by calling the method on the bean instance.
        // -----------------------------------------------------------------------

        // -----------------------------------------------------------------------
        // ((bean impl)ivEjbInstance).<method>(<args...>);
        //      or
        // return ((bean impl)ivEjbInstance).<method>(<args...>);
        // -----------------------------------------------------------------------
        Type implType = Type.getType("L" + implClassName + ";");
        mg.loadThis();
        mg.visitFieldInsn(GETFIELD, className, "ivEjbInstance", "Ljava/lang/Object;");
        mg.checkCast(implType);
        mg.loadArgs(0, argTypes.length); // do not pass "this"
        mg.visitMethodInsn(INVOKEVIRTUAL, implClassName, methodName,
                           m.getDescriptor());

        // -----------------------------------------------------------------------
        // return
        // -----------------------------------------------------------------------
        mg.returnValue();

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

    /**
     * Adds an EJB Proxy Method that invokes EJB interceptors. <p>
     * 
     * @param cw ASM Class writer to add the method to.
     * @param className name of the proxy class being generated.
     * @param implClassName name of the EJB implementation class.
     * @param method reflection method from the interface defining
     *            method to be added to the proxy.
     **/
    private static void addEJBInterceptorMethod(ClassWriter cw,
                                                String className,
                                                String implClassName,
                                                Method method)
                    throws EJBConfigurationException
    {
        GeneratorAdapter mg;
        String methodName = method.getName();
        String methodSignature = MethodAttribUtils.jdiMethodSignature(method);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : " +
                         methodName + " " + methodSignature +
                         " : isBridge = " + method.isBridge() +
                         " : aroundInvoke = true");

        // Determine the list of all exceptions, which will be on the throws
        // clause of the proxy method.  This will be all of the exceptions
        // on the bean implementation method throws clause.  This may not be
        // the same as the throws clause of the corresponding method on the
        // enpoint interface (if present), as the endpoint interface methods
        // may also throw RemoteException.  A Proxy represents the actual
        // bean implementation, unlike a Wrapper, which represents the
        // interface.
        Class<?>[] methodExceptions = method.getExceptionTypes();

        // Determine the list of 'checked' exceptions, which will need to have
        // catch blocks in the proxy. Exceptions that are subclasses of other
        // 'checked' exceptions will be eliminated, to avoid 'unreachable' code.
        Class<?>[] checkedExceptions = sortExceptions(methodExceptions,
                                                      true);

        // Convert the return value, arguments, and exception classes to
        // ASM Type objects, and create the ASM Method object which will
        // be used to actually add the method and method code.
        Type returnType = Type.getType(method.getReturnType());
        Type[] argTypes = getTypes(method.getParameterTypes());
        Type[] exceptionTypes = getTypes(methodExceptions);

        org.objectweb.asm.commons.Method m =
                        new org.objectweb.asm.commons.Method(methodName,
                                        returnType,
                                        argTypes);

        // Create an ASM GeneratorAdapter object for the ASM Method, which
        // makes generating dynamic code much easier... as it keeps track
        // of the position of the arguements and local variables, etc.
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
        // Parameters were not available during preInvoke, so must be captured
        // here to pass to the around invoke interceptor processing.       d507967
        //
        // Object[] args = new Object[# of args];
        // args[i] = "parameter";   ->  for each parameter
        // -----------------------------------------------------------------------
        int args = createParameterArray(mg, argTypes);

        // -----------------------------------------------------------------------
        // Now invoke the business method;
        // - Through the interceptors via EJSContainer.invoke()
        // -----------------------------------------------------------------------

        // -----------------------------------------------------------------------
        // try
        // {
        // -----------------------------------------------------------------------
        Label main_try_begin = new Label();
        mg.visitLabel(main_try_begin);

        // --------------------------------------------------------------------
        //   ivContainer.invoke(s, args);
        //      or
        //   rtnValue = (type)ivContainer.invoke(s, args);
        //      or
        //   rtnValue = ((object type)ivContainer.invoke(s, args)).<type>Value();  // unbox
        // --------------------------------------------------------------------
        mg.loadThis();
        mg.visitFieldInsn(GETFIELD, className, "ivContainer", "Lcom/ibm/ejs/container/EJSContainer;");
        mg.loadThis();
        mg.visitFieldInsn(GETFIELD, className, "ivMethodContext",
                          "Lcom/ibm/ejs/container/EJSDeployedSupport;");
        mg.loadLocal(args); // d507967
        mg.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/EJSContainer",
                           "invoke",
                           "(Lcom/ibm/ejs/container/EJSDeployedSupport;[Ljava/lang/Object;)Ljava/lang/Object;");

        if (returnType == Type.VOID_TYPE)
        {
            // No return value, just pop the returned value (null) off the stack
            mg.pop();
        }
        else
        {
            // Unbox any primitive values or add the appropriate cast
            // for object/array values, and then store in local variable.
            unbox(mg, returnType);
            mg.storeLocal(returnValue);
        }

        // -----------------------------------------------------------------------
        // }                     // end of try
        // -----------------------------------------------------------------------
        Label main_try_end = new Label();
        mg.visitLabel(main_try_end); // mark the end

        Label main_tcf_exit = new Label();
        mg.visitJumpInsn(GOTO, main_tcf_exit); // Skip to very end

        // -----------------------------------------------------------------------
        // Appear as if bean instance was called directly and just re-throw.
        //
        // catch (<checked exception> <ex>)  ->  for each <checked exception>
        // {
        //   throw <ex>;
        // }
        // -----------------------------------------------------------------------
        Label[] main_catch_label = new Label[checkedExceptions.length];
        int caught_ex = mg.newLocal(TYPE_Exception);

        for (int i = 0; i < checkedExceptions.length; i++)
        {
            main_catch_label[i] = new Label();
            mg.visitLabel(main_catch_label[i]);
            mg.storeLocal(caught_ex);
            mg.loadLocal(caught_ex);
            mg.visitInsn(ATHROW);
        }

        // -----------------------------------------------------------------------
        // Appear as if bean instance was called directly and just re-throw.
        //
        // catch (RuntimeException <ex>)
        // {
        //   throw <ex>;
        // }
        // -----------------------------------------------------------------------
        Label main_catch_runtime = new Label();
        mg.visitLabel(main_catch_runtime);
        mg.storeLocal(caught_ex);
        mg.loadLocal(caught_ex);
        mg.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        // Appear as if bean instance was called directly and just re-throw.
        //
        // catch (Error <ex>)
        // {
        //   throw <ex>;
        // }
        // -----------------------------------------------------------------------
        Label main_catch_error = new Label();
        mg.visitLabel(main_catch_error);
        mg.storeLocal(caught_ex);
        mg.loadLocal(caught_ex);
        mg.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        // Bean instance could not possibly have thrown this (unless Throwable
        // itself), so this must be coming from a poorly behaving interceptor.
        // This is a spec violation and cannot be reported dirctly.  Do the best
        // we can, and wrap it in an EJBException (i.e. RuntimeException).
        //
        // catch (Throwable <ex>)
        // {
        //   throw ExceptionUtil.EJBException( <ex> );
        // }
        // -----------------------------------------------------------------------
        Label main_catch_throwable = new Label();
        mg.visitLabel(main_catch_throwable);
        mg.storeLocal(caught_ex);
        mg.loadLocal(caught_ex);
        mg.visitMethodInsn(INVOKESTATIC, "com/ibm/ejs/container/util/ExceptionUtil",
                           "EJBException", "(Ljava/lang/Throwable;)Ljavax/ejb/EJBException;");
        mg.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        // }                     // end of try / catch / finally
        // -----------------------------------------------------------------------
        mg.visitLabel(main_tcf_exit);

        // -----------------------------------------------------------------------
        // return
        // -----------------------------------------------------------------------
        if (returnType != Type.VOID_TYPE)
        {
            mg.loadLocal(returnValue);
        }
        mg.returnValue();

        // -----------------------------------------------------------------------
        // All Try-Catch-Finally definitions for above
        // -----------------------------------------------------------------------
        for (int i = 0; i < checkedExceptions.length; i++)
        {
            mg.visitTryCatchBlock(main_try_begin, main_try_end,
                                  main_catch_label[i],
                                  convertClassName(checkedExceptions[i].getName()));
        }

        mg.visitTryCatchBlock(main_try_begin, main_try_end,
                              main_catch_runtime, "java/lang/RuntimeException");
        mg.visitTryCatchBlock(main_try_begin, main_try_end,
                              main_catch_error, "java/lang/Error");
        mg.visitTryCatchBlock(main_try_begin, main_try_end,
                              main_catch_throwable, "java/lang/Throwable");

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

    /**
     * Adds the following code which will define and create an object array
     * which holds references to all of the EJB method parameters. <p>
     * 
     * Object[] args = new Object[# of args];
     * args[i] = "parameter"; -> for each parameter
     * 
     * Primitives will be 'boxed' (i.e. converted to object wrapper type). <p>
     * 
     * If there are no method parameters, then code to create a zero length
     * array will be added. <p>
     * 
     * @param mg ASM Method Generator for the method being generated.
     * @param argTypes array of ASM Types that represent the parameter types,
     *            in declaration order.
     * 
     * @return the index of the local variable holding the array of arguments.
     **/
    // d507967
    private static int createParameterArray(GeneratorAdapter mg,
                                            Type[] argTypes)
    {
        // -----------------------------------------------------------------------
        //     Object[] args = new Object[# of args];
        // -----------------------------------------------------------------------
        int args = mg.newLocal(TYPE_Object_ARRAY);
        mg.push(argTypes.length);
        mg.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mg.storeLocal(args);

        // -----------------------------------------------------------------------
        //     args[i] = "parameter";  ->  for each parameter
        // -----------------------------------------------------------------------
        for (int i = 0; i < argTypes.length; i++)
        {
            mg.loadLocal(args);
            mg.push(i);

            // Convert primities to objects to put in 'args' array.
            switch (argTypes[i].getSort())
            {
                case Type.BOOLEAN:
                    mg.visitTypeInsn(NEW, "java/lang/Boolean");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Boolean", "<init>", "(Z)V");
                    break;

                case Type.CHAR:
                    mg.visitTypeInsn(NEW, "java/lang/Character");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Character", "<init>", "(C)V");
                    break;

                case Type.BYTE:
                    mg.visitTypeInsn(NEW, "java/lang/Byte");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Byte", "<init>", "(B)V");
                    break;

                case Type.SHORT:
                    mg.visitTypeInsn(NEW, "java/lang/Short");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Short", "<init>", "(S)V");
                    break;

                case Type.INT:
                    mg.visitTypeInsn(NEW, "java/lang/Integer");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
                    break;

                case Type.FLOAT:
                    mg.visitTypeInsn(NEW, "java/lang/Float");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Float", "<init>", "(F)V");
                    break;

                case Type.LONG:
                    mg.visitTypeInsn(NEW, "java/lang/Long");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Long", "<init>", "(J)V");
                    break;

                case Type.DOUBLE:
                    mg.visitTypeInsn(NEW, "java/lang/Double");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Double", "<init>", "(D)V");
                    break;

                // ARRAY & OBJECT - no need to copy, just load the arg
                default:
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    break;
            }

            mg.visitInsn(AASTORE);
        }

        return args;
    }

}
