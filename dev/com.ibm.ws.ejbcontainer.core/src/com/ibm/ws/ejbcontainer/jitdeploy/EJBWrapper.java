/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import static com.ibm.ejs.container.ContainerProperties.DeclaredRemoteAreApplicationExceptions;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBUtils.getMethodId;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.BUSINESS_LOCAL;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.BUSINESS_REMOTE;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.LOCAL;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.LOCAL_BEAN;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.LOCAL_HOME;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.MANAGED_BEAN;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.MDB_NO_METHOD_INTERFACE_PROXY;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.MDB_PROXY;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.REMOTE;
import static com.ibm.ws.ejbcontainer.jitdeploy.EJBWrapperType.REMOTE_HOME;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.INDENT;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_Exception;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_Object;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_Object_ARRAY;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_RuntimeException;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_Throwable;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.convertClassName;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.getTypes;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.unbox;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.writeToClassFile;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.JSR;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_2;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.ibm.ejs.container.ContainerEJBException;
import com.ibm.ejs.container.ContainerProperties;
import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ejs.container.util.DeploymentUtil;
import com.ibm.ejs.container.util.MethodAttribUtils;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Provides Just In Time deployment of EJB Wrapper classes. The properties of
 * the wrapper proxy class vary depending on the interface type:
 *
 * <ul>
 * <li>EJBObject - extends EJSWrapper
 * <li>EJBHome - extends EJSWrapper
 * <li>EJBLocalObject - extends EJSLocalWrapper
 * <li>EJBLocalHome - extends EJSLocalWrapper
 * <li>remote business interface - extends BusinessRemoteWrapper
 * <li>local business interface - extends BusinessLocalWrapper
 * <li>no-interface, managed bean - extends bean class, implements
 * LocalBeanWrapper, contains BusinessLocalWrapper
 * </ul>
 */
@SuppressWarnings("deprecation")
public final class EJBWrapper {
    private static final TraceComponent tc = Tr.register(EJBWrapper.class,
                                                         JITUtils.JIT_TRACE_GROUP,
                                                         JITUtils.JIT_RSRC_BUNDLE);

    // --------------------------------------------------------------------------
    // The following static finals are for commonly used ASM Type objects for
    // EJB Wrapper classes, and allow the JITDeploy code to avoid creating these
    // objects over and over again.
    // --------------------------------------------------------------------------
    static final Type TYPE_EJSDeployedSupport = Type.getType("Lcom/ibm/ejs/container/EJSDeployedSupport;");
    static final Type TYPE_RemoteException = Type.getType("Ljava/rmi/RemoteException;");
    static final String EJS_CONTAINER_FIELD_TYPE = "Lcom/ibm/ejs/container/EJSContainer;";

    // --------------------------------------------------------------------------
    // Constants for generating No-Interface View (LocalBean)
    // --------------------------------------------------------------------------
    public static final String LOCAL_BEAN_WRAPPER_FIELD = "ivWrapperBase"; // F743-1756
    static final String LOCAL_BEAN_WRAPPER_FIELD_TYPE = "Lcom/ibm/ejs/container/BusinessLocalWrapper;"; // F743-1756

    // --------------------------------------------------------------------------
    // Constants for generating MDB proxies
    // --------------------------------------------------------------------------
    public static final String MESSAGE_ENDPOINT_BASE_FIELD = "ivMessageEndpointBase";
    static final String MESSAGE_ENDPOINT_BASE_FIELD_TYPE = "Lcom/ibm/ws/ejbcontainer/mdb/MessageEndpointBase;";
    static final String MESSAGE_ENDPOINT_BASE_STRING = "com/ibm/ws/ejbcontainer/mdb/MessageEndpointBase";

    // --------------------------------------------------------------------------
    // Constants for generating ManagedBean wrappers
    // --------------------------------------------------------------------------
    public static final String MANAGED_BEAN_BEANO_FIELD = "ivBeanO"; // F743-34301.1
    static final String MANAGED_BEAN_BEANO_FIELD_TYPE = "Lcom/ibm/ejs/container/BeanO;"; // F743-34301.1

    /**
     * Core method for generating the EJB Wrapper class bytes. Intended for
     * use by JITDeploy only (should not be called directly). <p>
     *
     * @param wrapperClassName  name of the wrapper class to be generated.
     * @param wrapperInterfaces Interfaces implemented by the generated wrapper.
     * @param wrapperType       Type of wrapper to be generated (REMOTE, LOCAL, etc.)
     * @param allMethods        Set of all methods from all interfaces of the
     *                              type of wrapper being generated.
     * @param methodInfos       EJB method info objects for all of the methods
     *                              from all interfaces of the type of wrapper being
     *                              generated.
     * @param ejbClassName      Name of the EJB implementation class, that
     *                              the generated wrapper will route methods to.
     * @param beanName          Name of the EJB (for messages)
     **/
    static byte[] generateClassBytes(String wrapperClassName,
                                     Class<?>[] wrapperInterfaces,
                                     EJBWrapperType wrapperType,
                                     Method[] allMethods,
                                     EJBMethodInfoImpl[] methodInfos,
                                     String ejbClassName,
                                     String beanName) throws EJBConfigurationException {
        Method[] methods;
        Class<?> mdbEnterpriseClass = null;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        final boolean isNoMethodInterfaceMDB = wrapperType == MDB_NO_METHOD_INTERFACE_PROXY;

        if (wrapperType == MDB_PROXY || wrapperType == MDB_NO_METHOD_INTERFACE_PROXY) {
            // For MDB's the second entry in wrapperInterfaces holds the MDB enterprise class.
            mdbEnterpriseClass = wrapperInterfaces[1];
            wrapperInterfaces = new Class<?>[] { wrapperInterfaces[0] };
        }

        // Only business local and no-interface supported for aggregate wrappers. d677413
        boolean isAggregateWrapper = wrapperInterfaces.length > 1;
        if (isAggregateWrapper && wrapperType != LOCAL_BEAN && wrapperType != BUSINESS_LOCAL) {
            throw new IllegalArgumentException("wrapper type = " + wrapperType);
        }

        // Home wrappers do have some slight differences in generated code
        boolean isHomeWrapper = (wrapperType == REMOTE_HOME ||
                                 wrapperType == LOCAL_HOME) ? true : false;

        // This is the only wrapper interface for non-aggregate, and is the bean
        // class for LocalBean (No-Interface view).                        d677413
        Class<?> wrapperInterface = wrapperInterfaces[0];

        // ASM uses 'internal' java class names (like JNI) where '/' is
        // used instead of '.', so convert the parameters to 'internal' format.
        String internalClassName = convertClassName(wrapperClassName);
        String internalEJBClassName = convertClassName(ejbClassName);
        String[] internalInterfaceNames = new String[wrapperInterfaces.length];
        for (int i = 0; i < wrapperInterfaces.length; i++) {
            String wrapperInterfaceName = wrapperInterfaces[i].getName();
            internalInterfaceNames[i] = convertClassName(wrapperInterfaceName);
        }

        // no-method interface MDB's will directly extend the EJB Bean class.
        // Therefore, we will already be picking up the single interface, but
        // we need to add MessageEndpoint since the MDB proxy won't be extending
        // MessageEndpointBase.
        if (isNoMethodInterfaceMDB) {
            internalInterfaceNames[0] = "javax/resource/spi/endpoint/MessageEndpoint";
        }

        // The parent of the wrapper varies based on type of wrapper interface
        String internalParentName = getParentClassName(wrapperType, internalEJBClassName);

        // Remote Business interfaces may or may not extend java.rmi.Remote
        boolean isRmiRemote = (Remote.class).isAssignableFrom(wrapperInterface);

        // For No-Interface View (LocalBean), switch the interface and parent
        // names, as the EJB itself must be the parent and it will implement
        // a marker interface so passivation knows it is a wrapper.      F743-1756
        // Same is required for ManagedBean wrappers.                 F743-34301.1
        boolean isLocalBean = (wrapperType == LOCAL_BEAN ||
                               wrapperType == MANAGED_BEAN);
        if (isLocalBean) {
            // No-Interface is always first in the list                     d677413
            String tempName = internalInterfaceNames[0];
            internalInterfaceNames[0] = internalParentName;
            internalParentName = tempName;
        }

        if (isTraceOn) {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "generateClassBytes");
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, INDENT + "className = " + internalClassName);
                Tr.debug(tc, INDENT + "interface = " + Arrays.toString(internalInterfaceNames));
                Tr.debug(tc, INDENT + "parent    = " + internalParentName);
                Tr.debug(tc, INDENT + "ejb       = " + internalEJBClassName);
                if (isRmiRemote)
                    Tr.debug(tc, INDENT + "implements java.rmi.Remote");
            }
        }

        // Before generating the wrapper... first perform some basic validation
        // on the configured wrapper interface to insure it conforms to the
        // spec.. for such things as implementing EJBObject (2.1), etc.    d431543
        // For aggregate wrappers, validation performed previously.        d677413
        if (!isAggregateWrapper) {
            validateInterfaceBasics(wrapperInterface, wrapperType, ejbClassName);
        }

        // Create the ASM Class Writer to write out a Wrapper
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS); //F743-11995

        // Define the Wrapper Class object
        cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER,
                 internalClassName,
                 null,
                 internalParentName,
                 internalInterfaceNames);

        // Define the source code file and debug settings
        String sourceFileName = wrapperClassName.substring(wrapperClassName.lastIndexOf(".") + 1) + ".java";
        cw.visitSource(sourceFileName, null);

        // Add static and instance variables per wrapper type.           F743-1756
        addFields(cw, wrapperType);

        // Add the public no parameter wrapper constructor
        addCtor(cw, internalParentName);

        // For no-method interface MDBs, add the MessageEndpoint interface methods.
        if (isNoMethodInterfaceMDB) {
            addBeforeDeliveryMethod(cw, internalClassName);
            addAfterDeliveryMethod(cw, internalClassName);
            addReleaseMethod(cw, internalClassName);
        }

        // d583041 - No-Interface (LocalBean) needs to override Object.equals and
        // Object.hashCode.  Other local views get this from EJSWrapperBase.
        if (wrapperType == LOCAL_BEAN) {
            addNoInterfaceEqualsMethod(cw, internalClassName);
            addNoInterfaceHashCodeMethod(cw, internalClassName);
        }

        // MDB Proxy instances need to override equals and hashcode to provide
        // the java.lang.Object default behavior. If not, then all Proxy instances
        // for an MDB where the Proxy class extends MessageEndpointBase will report
        // equal and have the same hashcode; which does not follow JCA specification.
        // Proxy classes that extend the customer class would have undefined behavior.
        if (wrapperType == MDB_PROXY || wrapperType == MDB_NO_METHOD_INTERFACE_PROXY) {
            addDefaultEqualsMethod(cw, internalClassName);
            addDefaultHashCodeMethod(cw, internalClassName);
        }

        // Use Reflections to determine which methods are implemented by
        // the interface, and therefore must be on the wrapper.  Note that
        // if this interface is the only interface of its type (i.e. Local,
        // Remote, etc), then the list of all methods will be the same
        // as the list for this interface.
        if (isHomeWrapper ||
            wrapperType == MDB_PROXY ||
            wrapperType == MDB_NO_METHOD_INTERFACE_PROXY) {
            methods = allMethods;
        } else if (wrapperType == BUSINESS_LOCAL ||
                   wrapperType == BUSINESS_REMOTE ||
                   wrapperType == LOCAL_BEAN ||
                   wrapperType == MANAGED_BEAN) {
            // For 3.0 and later business interfaces (including no-interface view),
            // DeploymentUtil is expecting them in the second parameter.  F743-1756
            methods = DeploymentUtil.getMethods(null, wrapperInterfaces);
        } else {
            // Component interfaces are passed as the first parameter.
            methods = DeploymentUtil.getMethods(wrapperInterface, null);
        }

        // Add all of the methods to the wrapper, based on the reflected
        // Method objects from the interface.
        int methodId = -1;
        for (int i = 0; i < methods.length; ++i) {
            Method method = methods[i];
            String implMethodName = method.getName();

            // For aggregate wrappers, validation performed previously.     d677413
            if (!isAggregateWrapper) {
                // For Local Home wrappers, the create and find methods must have
                // _Local appended, since there is only 1 home implementation.
                if (isHomeWrapper && !isRmiRemote) {
                    if (implMethodName.startsWith("create") ||
                        implMethodName.startsWith("find"))
                        implMethodName += "_Local";
                }

                // Business methods must not start with "ejb", per spec.     d457128
                if (!isHomeWrapper &&
                    wrapperType != MANAGED_BEAN &&
                    wrapperType != MDB_PROXY &&
                    wrapperType != MDB_NO_METHOD_INTERFACE_PROXY &&
                    implMethodName.startsWith("ejb")) {
                    // Log the error and throw meaningful exception.        d457128.2
                    Tr.error(tc, "JIT_INVALID_MTHD_PREFIX_CNTR5010E",
                             new Object[] { beanName,
                                            wrapperInterface.getName(),
                                            implMethodName });
                    throw new EJBConfigurationException("EJB business method " + implMethodName +
                                                        " on interface " + wrapperInterface.getName() +
                                                        " must not start with 'ejb'.");
                }

                // No-Interface View (LocalBean) methods must not be final
                // or throw a RemoteException.                             F743-1756
                if (isLocalBean) {
                    validateLocalBeanMethod(method, beanName);
                }
            }

            // Determine the Method Id that will be hard coded into the
            // wrapper to allow preInvoke to quickly find the correct method.
            methodId = getMethodId(method, allMethods, ++methodId);

            // Determine if interceptors are called from methodinfo.      d367572.4
            EJBMethodInfoImpl methodInfo = methodInfos[methodId];
            boolean aroundInvoke = (methodInfo.getAroundInterceptorProxies() != null); // F743-17763.1

            // Determine if this is a Stateless Home create, which will
            // optimize the wrapper to call a special preInvoke.
            boolean isStatelessCreate = methodInfo.isHomeCreate() &&
                                        methodInfo.isStatelessSessionBean();

            // For aggregate wrappers, if the same method (name and parameters) is
            // present on multiple interfaces, verify that the thrown exceptions
            // match across all occurrences. If there is a mismatch, then log a
            // warning, and use the method (and thus exceptions) from the
            // implementation. Only and all exceptions from the implementation are
            // possible.                                                    d677413
            if (isAggregateWrapper) {
                Method previousMethod = null, currentMethod = null;
                for (Class<?> curInterface : wrapperInterfaces) {
                    try {
                        currentMethod = curInterface.getMethod(implMethodName, method.getParameterTypes());
                    } catch (NoSuchMethodException ex) {
                        continue; // not all interfaces have all methods... normal
                    }

                    if (previousMethod == null) {
                        previousMethod = currentMethod;
                    } else {
                        Class<?>[] previousEx = previousMethod.getExceptionTypes();
                        Class<?>[] currentEx = currentMethod.getExceptionTypes();
                        if (!Arrays.equals(previousEx, currentEx)) {
                            method = methodInfo.getMethod();
                            Tr.warning(tc, "JIT_THROWS_CLAUSE_MISMATCH_CNTR5035W",
                                       new Object[] { beanName, method }); // d677413
                        }
                    }
                }
            }

            // F743-609, F743-609CodRev F743-34301.1
            // Add the method using one of 3 styles:
            // 1 - ManagedBean method (no pre/post invoke processing)
            // 2 - Synchronous EJB method
            // 3 - Asynchronous EJB method
            // Each 'add' method accounts for interceptors (or lack of)
            if (wrapperType == MANAGED_BEAN) {
                addManagedBeanMethod(cw,
                                     internalClassName,
                                     internalEJBClassName,
                                     method,
                                     implMethodName,
                                     methodId,
                                     aroundInvoke);
            } else if (!methodInfo.isAsynchMethod()) {
                addEJBMethod(cw,
                             internalClassName,
                             internalEJBClassName,
                             wrapperType, // RTC116527
                             method,
                             implMethodName,
                             methodId,
                             isRmiRemote,
                             aroundInvoke,
                             isStatelessCreate);

            } else {
                addEJBAsynchMethod(cw,
                                   internalClassName,
                                   method,
                                   methodId,
                                   isRmiRemote,
                                   wrapperType);
            }
        }

        // For No-Interface View (LocalBean) or no-method interface MDB,
        // also add method overrides for all non-public methods, which
        // just throw EJBException.        F743-1756
        if (isLocalBean || isNoMethodInterfaceMDB) {
            Class<?> ejbClass;
            if (isNoMethodInterfaceMDB) {
                // For no method interface MDBs we need to use the enterprise
                // class instead of the interface for finding non-public methods.
                ejbClass = mdbEnterpriseClass;
            } else {
                ejbClass = wrapperInterface;
            }
            ArrayList<Method> nonPublicMethods = DeploymentUtil.getNonPublicMethods(ejbClass,
                                                                                    methods);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, INDENT + "Non-public methods : " +
                             nonPublicMethods.size());

            for (Method method : nonPublicMethods) {
                if (!isAggregateWrapper && isLocalBean) {
                    validateLocalBeanMethod(method, beanName); // must not be final
                }
                addNonPublicMethod(cw, method);
            }
        }

        // Mark the end of the generated wrapper class
        cw.visitEnd();

        // Dump the class bytes out to a byte array.
        byte[] classBytes = cw.toByteArray();

        if (isTraceOn) {
            if (tc.isDebugEnabled())
                writeToClassFile(internalClassName, classBytes);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "generateClassBytes: " + classBytes.length + " bytes");
        }

        return classBytes;
    }

    /**
     * Defines the static and instance variables that are required for
     * each wrapper type. <p>
     *
     * Fields for LOCAL_BEAN (No-Interface View)
     * <ul>
     * <li> private EJSContainer container;
     * <li> private BusinessLocalWrapper ivWrapperBase;
     * </ul>
     *
     * The fields are NOT initialized. <p>
     *
     * @param cw          ASM ClassWriter to add the fields to.
     * @param wrapperType Type of wrapper to be generated (REMOTE, LOCAL, etc.)
     **/
    private static void addFields(ClassWriter cw, EJBWrapperType wrapperType) {
        FieldVisitor fv;

        // For No-Interface view (LocalBean) add an instance variables to the
        // EJSContainer and associated BusinessLocalWrapper instance.    F743-1756
        if (wrapperType == LOCAL_BEAN || wrapperType == MANAGED_BEAN) {
            final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

            // --------------------------------------------------------------------
            // private BusinessLocalWrapper ivWrapperBase;
            // --------------------------------------------------------------------
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, INDENT + "adding field : " +
                             LOCAL_BEAN_WRAPPER_FIELD + " " +
                             LOCAL_BEAN_WRAPPER_FIELD_TYPE);

            fv = cw.visitField(ACC_PRIVATE, LOCAL_BEAN_WRAPPER_FIELD,
                               LOCAL_BEAN_WRAPPER_FIELD_TYPE,
                               null, null);
            fv.visitEnd();

            if (wrapperType == MANAGED_BEAN) {
                // --------------------------------------------------------------------
                // private BeanO ivBeanO;
                // --------------------------------------------------------------------
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, INDENT + "adding field : " +
                                 MANAGED_BEAN_BEANO_FIELD + " " +
                                 MANAGED_BEAN_BEANO_FIELD_TYPE);

                fv = cw.visitField(ACC_PRIVATE, MANAGED_BEAN_BEANO_FIELD,
                                   MANAGED_BEAN_BEANO_FIELD_TYPE,
                                   null, null);
                fv.visitEnd();
            }
        }

        if (wrapperType == MDB_NO_METHOD_INTERFACE_PROXY) {
            // --------------------------------------------------------------------
            // private MessageEndpointBase ivMessageEndpointBase;
            // --------------------------------------------------------------------
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, INDENT + "adding field : " +
                             MESSAGE_ENDPOINT_BASE_FIELD + " " +
                             MESSAGE_ENDPOINT_BASE_FIELD_TYPE);

            fv = cw.visitField(ACC_PRIVATE, MESSAGE_ENDPOINT_BASE_FIELD,
                               MESSAGE_ENDPOINT_BASE_FIELD_TYPE,
                               null, null);
            fv.visitEnd();
        }
    }

    /**
     * Adds the default (no arg) constructor. <p>
     *
     * There are no exceptions in the throws clause; none are required
     * for the constructors of EJB Wrappers (local or remote). <p>
     *
     * Currently, the generated method body is intentionally empty;
     * EJB Wrappers require no initialization in the constructor. <p>
     *
     * @param cw     ASM ClassWriter to add the constructor to.
     * @param parent fully qualified name of the parent class
     *                   with '/' as the separator character
     *                   (i.e. internal name).
     **/
    private static void addCtor(ClassWriter cw, String parent) {
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

        // -----------------------------------------------------------------------
        //    super();
        // -----------------------------------------------------------------------
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, parent, "<init>", "()V");

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * Adds the default definition for the Object.equals method.
     *
     * @param cw            ASM ClassWriter to add the method to.
     * @param implClassName name of the wrapper class being generated.
     */
    private static void addDefaultEqualsMethod(ClassWriter cw, String implClassName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : equals (Ljava/lang/Object;)Z");

        // -----------------------------------------------------------------------
        // public boolean equals(Object other)
        // {
        // -----------------------------------------------------------------------
        final String desc = "(Ljava/lang/Object;)Z";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "equals", desc, null, null);
        GeneratorAdapter mg = new GeneratorAdapter(mv, ACC_PUBLIC, "equals", desc);
        mg.visitCode();

        // -----------------------------------------------------------------------
        //    return this == other;
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadArg(0);
        Label not_equal = new Label();
        mv.visitJumpInsn(IF_ACMPNE, not_equal);
        mg.visitInsn(ICONST_1);
        mg.returnValue();

        mg.visitLabel(not_equal);
        mg.visitInsn(ICONST_0);
        mg.returnValue();

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mg.endMethod();
        mg.visitEnd();
    }

    /**
     * Adds the default definition for the Object.hashCode method.
     *
     * @param cw            ASM ClassWriter to add the method to.
     * @param implClassName name of the wrapper class being generated.
     */
    private static void addDefaultHashCodeMethod(ClassWriter cw, String implClassName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : hashCode ()I");

        // -----------------------------------------------------------------------
        // public int hashCode()
        // {
        // -----------------------------------------------------------------------
        final String desc = "()I";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "hashCode", desc, null, null);
        GeneratorAdapter mg = new GeneratorAdapter(mv, ACC_PUBLIC, "hashCode", desc);
        mg.visitCode();

        // -----------------------------------------------------------------------
        //    return System.identityHashCode(this);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I");
        mg.returnValue();

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mg.endMethod();
        mg.visitEnd();
    }

    /**
     * Adds a definition for Object.equals method for the No-Interface
     * (LocalBean).
     *
     * @param cw            ASM ClassWriter to add the method to.
     * @param implClassName name of the wrapper class being generated.
     */
    // d583041
    private static void addNoInterfaceEqualsMethod(ClassWriter cw,
                                                   String implClassName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : equals (Ljava/lang/Object;)Z");

        // -----------------------------------------------------------------------
        // public boolean equals(Object other)
        // {
        // -----------------------------------------------------------------------
        final String desc = "(Ljava/lang/Object;)Z";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "equals", desc, null, null);
        GeneratorAdapter mg = new GeneratorAdapter(mv, ACC_PUBLIC, "equals", desc);
        mg.visitCode();

        // -----------------------------------------------------------------------
        //    if (other instanceof type)
        //    {
        // -----------------------------------------------------------------------
        mg.loadArg(0);
        mg.visitTypeInsn(INSTANCEOF, implClassName);
        Label if_instanceofType_End = new Label();
        mg.visitJumpInsn(IFEQ, if_instanceofType_End);

        // -----------------------------------------------------------------------
        //       return this.ivWrapperBase.equals(((type)other).ivWrapperBase)
        // -----------------------------------------------------------------------
        loadWrapperBase(mg, implClassName, LOCAL_BEAN);
        mg.loadArg(0);
        mg.visitTypeInsn(CHECKCAST, implClassName);
        mv.visitFieldInsn(GETFIELD, implClassName, LOCAL_BEAN_WRAPPER_FIELD,
                          LOCAL_BEAN_WRAPPER_FIELD_TYPE);
        mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object",
                           "equals",
                           desc);
        mg.returnValue();

        // -----------------------------------------------------------------------
        //    }
        // -----------------------------------------------------------------------
        mg.visitLabel(if_instanceofType_End);

        // -----------------------------------------------------------------------
        //    return false;
        // -----------------------------------------------------------------------
        mg.push(false);
        mg.returnValue();

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mg.endMethod();
        mg.visitEnd();
    }

    /**
     * Adds a definition for Object.hashCode method for the No-Interface view
     * (LocalBean).
     *
     * @param cw            ASM ClassWriter to add the method to.
     * @param implClassName name of the wrapper class being generated.
     */
    // d583041
    private static void addNoInterfaceHashCodeMethod(ClassWriter cw,
                                                     String implClassName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : hashCode ()I");

        // -----------------------------------------------------------------------
        // public int hashCode()
        // {
        // -----------------------------------------------------------------------
        final String desc = "()I";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "hashCode", desc, null, null);
        GeneratorAdapter mg = new GeneratorAdapter(mv, ACC_PUBLIC, "hashCode", desc);
        mg.visitCode();

        // -----------------------------------------------------------------------
        //    return this.ivWrapperBase.hashCode();
        // -----------------------------------------------------------------------
        loadWrapperBase(mg, implClassName, LOCAL_BEAN);
        mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object",
                           "hashCode",
                           desc);
        mg.returnValue();

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mg.endMethod();
        mg.visitEnd();
    }

    /**
     * Adds a definition for an afterDelivery() wrapper method that invokes
     * MessageEndpointBase.afterDelivery(). It is only needed for no-method
     * Interface MDB's. Otherwise, MessageEndpointBase.afterDelivery()
     * is inherited.
     *
     * @param cw            ASM ClassWriter to add the method to.
     * @param implClassName name of the wrapper class being generated.
     */
    private static void addAfterDeliveryMethod(ClassWriter cw,
                                               String implClassName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : afterDelivery ()V");

        // -----------------------------------------------------------------------
        // public void afterDelivery() throws ResourceException
        // {
        // -----------------------------------------------------------------------
        final String desc = "()V";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "afterDelivery", desc, null, new String[] { "javax/resource/ResourceException" });
        GeneratorAdapter mg = new GeneratorAdapter(mv, ACC_PUBLIC, "afterDelivery", desc);
        mg.visitCode();

        // -----------------------------------------------------------------------
        //    this.ivMessageEndpointBase.afterDelivery();
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.visitFieldInsn(GETFIELD, implClassName, MESSAGE_ENDPOINT_BASE_FIELD,
                          MESSAGE_ENDPOINT_BASE_FIELD_TYPE);
        mg.visitMethodInsn(INVOKEVIRTUAL, MESSAGE_ENDPOINT_BASE_STRING,
                           "afterDelivery",
                           desc);
        mg.returnValue();

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mg.endMethod();
        mg.visitEnd();
    }

    /**
     * Adds a definition for a beforeDelivery() wrapper method that invokes
     * MessageEndpointBase.beforeDelivery(). It is only needed for no-method
     * Interface MDB's. Otherwise, MessageEndpointBase.beforeDelivery()
     * is inherited.
     *
     * @param cw            ASM ClassWriter to add the method to.
     * @param implClassName name of the proxy class being generated.
     */
    private static void addBeforeDeliveryMethod(ClassWriter cw,
                                                String implClassName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : beforeDelivery (Method)V");

        // -----------------------------------------------------------------------
        // public void beforeDelivery(Method m) throws NoSuchMethodException, ResourceException
        // {
        // -----------------------------------------------------------------------
        final String desc = "(Ljava/lang/reflect/Method;)V";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "beforeDelivery", desc, null, new String[] { "java/lang/NoSuchMethodException", "javax/resource/ResourceException" });
        GeneratorAdapter mg = new GeneratorAdapter(mv, ACC_PUBLIC, "beforeDelivery", desc);
        mg.visitCode();

        // -----------------------------------------------------------------------
        //    this.ivMessageEndpointBase.beforeDelivery();
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.visitFieldInsn(GETFIELD, implClassName, MESSAGE_ENDPOINT_BASE_FIELD,
                          MESSAGE_ENDPOINT_BASE_FIELD_TYPE);
        mg.loadArg(0);
        mg.visitMethodInsn(INVOKEVIRTUAL, MESSAGE_ENDPOINT_BASE_STRING,
                           "beforeDelivery",
                           desc);
        mg.returnValue();

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mg.endMethod();
        mg.visitEnd();
    }

    /**
     * Adds a definition for an release() wrapper method that invokes
     * MessageEndpointBase.release(). It is only needed for no-method
     * Interface MDB's. Otherwise, MessageEndpointBase.release()
     * is inherited.
     *
     * @param cw            ASM ClassWriter to add the method to.
     * @param implClassName name of the proxy class being generated.
     */
    private static void addReleaseMethod(ClassWriter cw,
                                         String implClassName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : release ()V");

        // -----------------------------------------------------------------------
        // public void release()
        // {
        // -----------------------------------------------------------------------
        final String desc = "()V";
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "release", desc, null, null);
        GeneratorAdapter mg = new GeneratorAdapter(mv, ACC_PUBLIC, "release", desc);
        mg.visitCode();

        // -----------------------------------------------------------------------
        //    this.ivMessageEndpointBase.release();
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.visitFieldInsn(GETFIELD, implClassName, MESSAGE_ENDPOINT_BASE_FIELD,
                          MESSAGE_ENDPOINT_BASE_FIELD_TYPE);
        mg.visitMethodInsn(INVOKEVIRTUAL, MESSAGE_ENDPOINT_BASE_STRING,
                           "release",
                           desc);
        mg.returnValue();

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        mg.endMethod();
        mg.visitEnd();
    }

    /**
     * Adds a standard (ie. synchronous) EJB Wrapper Method. <p>
     *
     * @param cw                ASM Class writer to add the method to.
     * @param className         name of the wrapper class being generated.
     * @param implClassName     name of the EJB implementation class.
     * @param wrapperType       Type of wrapper to be generated (REMOTE, LOCAL, etc.)
     * @param method            reflection method from the interface defining
     *                              method to be added to the wrapper.
     * @param implMethodName    name of the method on the EJB implementation
     *                              class (may be different for homes).
     * @param methodId          index into the list of all methods of this
     *                              wrapper type, for this specific method.
     * @param isRmiRemote       true if interface extends java.rmi.Remote.
     * @param aroundInvoke      true if interceptors are present.
     * @param isStatelessCreate true if this is for the create method on
     *                              a Stateless Session bean Home.
     **/
    private static void addEJBMethod(ClassWriter cw,
                                     String className,
                                     String implClassName,
                                     EJBWrapperType wrapperType,
                                     Method method,
                                     String implMethodName,
                                     int methodId,
                                     boolean isRmiRemote,
                                     boolean aroundInvoke,
                                     boolean isStatelessCreate) throws EJBConfigurationException {
        GeneratorAdapter mg;
        String methodName = method.getName();
        String methodSignature = MethodAttribUtils.jdiMethodSignature(method);
        String EjbPreInvoke = isStatelessCreate ? "EjbPreInvokeForStatelessCreate" : "EjbPreInvoke";
        String EjbPostInvoke = isStatelessCreate ? "EjbPostInvokeForStatelessCreate" : "postInvoke";
        String setUncheckedException = isRmiRemote ? "setUncheckedException" : "setUncheckedLocalException";

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : " +
                         methodName + " " + methodSignature +
                         " : aroundInvoke = " + aroundInvoke);

        // Determine the list of 'checked' exceptions, which will need to have
        // catch blocks in the wrapper... and also perform validation.
        // Note that RemoteException is never a 'checked' exception, and
        // exceptions that are subclasses of other 'checked' exceptions
        // will be eliminated, to avoid 'unreachable' code.
        Class<?>[] methodExceptions = method.getExceptionTypes();
        Class<?>[] checkedExceptions = DeploymentUtil.getCheckedExceptions(method,
                                                                           isRmiRemote,
                                                                           DeploymentUtil.DeploymentTarget.WRAPPER, // d660332
                                                                           wrapperType); // RTC116527

        // Convert the return value, arguments, and exception classes to
        // ASM Type objects, and create the ASM Method object which will
        // be used to actually add the method and method code.
        Type returnType = Type.getType(method.getReturnType());
        Type[] argTypes = getTypes(method.getParameterTypes());
        Type[] exceptionTypes = getTypes(methodExceptions);

        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method(methodName, returnType, argTypes);

        // Create an ASM GeneratorAdapter object for the ASM Method, which
        // makes generating dynamic code much easier... as it keeps track
        // of the position of the arguments and local variables, etc.
        mg = new GeneratorAdapter(ACC_PUBLIC, m, null, exceptionTypes, cw);

        // -----------------------------------------------------------------------
        // Begin Method Code...
        // -----------------------------------------------------------------------
        mg.visitCode();

        // -----------------------------------------------------------------------
        // <return type> returnValue = false | 0 | null;
        // -----------------------------------------------------------------------
        int returnValue = -1;
        if (returnType != Type.VOID_TYPE) {
            returnValue = mg.newLocal(returnType);
            switch (returnType.getSort()) {
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

        int s = -1;
        if (wrapperType != MDB_PROXY && wrapperType != MDB_NO_METHOD_INTERFACE_PROXY) {
            // -----------------------------------------------------------------------
            // EJSDeployedSupport s = new EJSDeployedSupport();
            // -----------------------------------------------------------------------
            s = mg.newLocal(TYPE_EJSDeployedSupport);
            mg.newInstance(TYPE_EJSDeployedSupport);
            mg.dup();
            mg.visitMethodInsn(INVOKESPECIAL, "com/ibm/ejs/container/EJSDeployedSupport", "<init>", "()V");
            mg.storeLocal(s);
        }
        // For MDBs we need to call MessageEndpointBase.checkState() outside
        // the try/catch/finally block.
        // This will verify we are in a valid state to process this invocation.
        // An IllegalStateException is thrown if we are not in a valid state or
        // if we detect the RA violated the JCA 1.5 specification.
        else {
            // -----------------------------------------------------------------------
            // this.checkState(methodId, null, MDB_BUSINESS_METHOD);
            // -----------------------------------------------------------------------
            loadWrapperBase(mg, className, wrapperType);
            mg.push(methodId);
            mg.visitInsn(ACONST_NULL);
            mg.push(3);
            mg.visitMethodInsn(wrapperType == MDB_NO_METHOD_INTERFACE_PROXY ? INVOKEVIRTUAL : INVOKESPECIAL, MESSAGE_ENDPOINT_BASE_STRING,
                               "checkState",
                               "(ILjava/lang/reflect/Method;B)V");
        }

        // -----------------------------------------------------------------------
        // try
        // {
        // -----------------------------------------------------------------------
        Label main_try_begin = new Label();
        mg.visitLabel(main_try_begin);

        Label if_JaccArgs_End = null;

        // -----------------------------------------------------------------------
        // Object[] args;
        // -----------------------------------------------------------------------
        int args = mg.newLocal(TYPE_Object_ARRAY);

        // The parameters always need to be copied for AroundInvoke interceptors,
        // but otherwise only need to be copied for JACC... so only add the
        // JACC check when there are no interceptors for the method.     d367572.4
        if (!aroundInvoke) {
            // --------------------------------------------------------------------
            // Object[] args = null;
            // --------------------------------------------------------------------
            mg.visitInsn(ACONST_NULL);
            mg.storeLocal(args);

            // --------------------------------------------------------------------
            //   if ( container.doesJaccNeedsEJBArguments( this ) )
            //   {
            // --------------------------------------------------------------------
            loadContainer(mg, className, wrapperType);
            loadWrapperBase(mg, className, wrapperType); // F743-1756
            mg.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/EJSContainer", "doesJaccNeedsEJBArguments", "(Lcom/ibm/ejs/container/EJSWrapperBase;)Z");
            if_JaccArgs_End = new Label();
            mg.visitJumpInsn(IFEQ, if_JaccArgs_End); // comparison to 0
        }

        // -----------------------------------------------------------------------
        //     args = new Object[# of args];
        // -----------------------------------------------------------------------
        mg.push(argTypes.length);
        mg.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mg.storeLocal(args);

        // -----------------------------------------------------------------------
        //     args[i] = "parameter";  ->  for each parameter
        // -----------------------------------------------------------------------
        for (int i = 0; i < argTypes.length; i++) {
            mg.loadLocal(args);
            mg.push(i);

            // Convert primitives to objects to put in 'args' array.
            switch (argTypes[i].getSort()) {
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

        // As above, the check around the copy of the parameters was only added
        // when no interceptors, so only need the closing label when the if
        // check was added.                                              d367572.4
        if (!aroundInvoke) {
            // --------------------------------------------------------------------
            //   }                   // end of container.doesJaccNeedsEJBAruments()
            // --------------------------------------------------------------------
            mg.visitLabel(if_JaccArgs_End);
        }

        // -----------------------------------------------------------------------
        //   <bean impl> bean = (bean impl)container.EjbPreInvoke(this, methodId, s, args);
        //      or
        //   <bean impl> bean = (bean impl)container.EjbPreInvokeForStatelessCreate(this, methodId, s, args
        //      or
        //   <bean impl> bean = (bean impl) super.mdbMethodPreInvoke(methodId,args);
        // -----------------------------------------------------------------------
        Type implType = Type.getType("L" + implClassName + ";");
        int bean = mg.newLocal(implType);

        if (wrapperType != MDB_PROXY && wrapperType != MDB_NO_METHOD_INTERFACE_PROXY) {
            // non-MDB preInvoke processing
            loadContainer(mg, className, wrapperType);
            loadWrapperBase(mg, className, wrapperType); // F743-1756
            mg.push(methodId);
            mg.loadLocal(s);
            mg.loadLocal(args);
            mg.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/EJSContainer",
                               EjbPreInvoke,
                               "(Lcom/ibm/ejs/container/EJSWrapperBase;ILcom/ibm/ejs/container/EJSDeployedSupport;[Ljava/lang/Object;)Ljava/lang/Object;");
        } else {
            // call MessageEndpointBase.mdbMethodPreInvoke() for a MDB
            loadWrapperBase(mg, className, wrapperType);
            mg.push(methodId);
            mg.loadLocal(args);
            mg.visitMethodInsn(wrapperType == MDB_NO_METHOD_INTERFACE_PROXY ? INVOKEVIRTUAL : INVOKESPECIAL, MESSAGE_ENDPOINT_BASE_STRING,
                               "mdbMethodPreInvoke",
                               "(I[Ljava/lang/Object;)Ljava/lang/Object;");
        }
        mg.checkCast(implType);
        mg.storeLocal(bean);

        // -----------------------------------------------------------------------
        // Now invoke the business method; one of 2 ways:
        // 1 ) Through the interceptors via EJSContainer.invoke()        d367572.4
        // 2 ) Directly, by calling the method on the bean instance.
        // -----------------------------------------------------------------------

        if (aroundInvoke) {
            // --------------------------------------------------------------------
            //   this.container.invoke(s,null);
            //      or
            //   rtnValue = (type)this.container.invoke(s,null);
            //      or
            //   rtnValue = ((object type)this.container.invoke(s,null)).<type>Value();  // unbox
            // --------------------------------------------------------------------
            loadContainer(mg, className, wrapperType);
            loadEJSDeployedSupport(mg, className, wrapperType, s);
            mg.visitInsn(ACONST_NULL); // F743-17763.1
            mg.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/EJSContainer",
                               "invoke",
                               "(Lcom/ibm/ejs/container/EJSDeployedSupport;Ljavax/ejb/Timer;)Ljava/lang/Object;"); // F743-17763.1

            if (returnType == Type.VOID_TYPE) {
                // No return value, just pop the returned value (null) off the stack
                mg.pop();
            } else {
                // Unbox any primitive values or add the appropriate cast
                // for object/array values, and then store in local variable. d369262.7
                unbox(mg, returnType);
                mg.storeLocal(returnValue);
            }
        } else {
            // --------------------------------------------------------------------
            //   bean.<method>(<args...>);
            //      or
            //   rtnValue = bean.<method>(<args...>);
            // --------------------------------------------------------------------
            mg.loadLocal(bean);
            mg.loadArgs(0, argTypes.length); // do not pass "this"
            mg.visitMethodInsn(INVOKEVIRTUAL, implClassName, implMethodName,
                               m.getDescriptor());
            if (returnType != Type.VOID_TYPE) {
                mg.storeLocal(returnValue);
            }
        }

        // -----------------------------------------------------------------------
        // }                     // end of try
        // -----------------------------------------------------------------------
        Label main_try_end = new Label();
        mg.visitLabel(main_try_end); // mark the end

        Label main_finally_begin = new Label();
        mg.visitJumpInsn(JSR, main_finally_begin); // execute the finally

        Label main_tcf_exit = new Label();
        mg.visitJumpInsn(GOTO, main_tcf_exit); // Skip to very end

        // d660332 - If the throws clause contains Exception, then we need a
        // special catch block for RuntimeException so that it is not treated as
        // an application exception.
        boolean hasThrowsException = false;
        if (ContainerProperties.DeclaredUncheckedAreSystemExceptions) {
            for (Class<?> checkedException : checkedExceptions) {
                hasThrowsException |= checkedException == Exception.class;
            }
        }

        Label main_catch_runtime_exception = null;
        if (hasThrowsException) {
            // -----------------------------------------------------------------------
            // catch (RuntimeException th)
            // {
            //   s.setUncheckedException(th);   or   s.setUncheckedLocalException(th);
            // }
            // -----------------------------------------------------------------------
            main_catch_runtime_exception = new Label();
            mg.visitLabel(main_catch_runtime_exception);
            int main_th = mg.newLocal(TYPE_RuntimeException);
            mg.storeLocal(main_th);
            loadEJSDeployedSupport(mg, className, wrapperType, s);
            mg.loadLocal(main_th);
            mg.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/EJSDeployedSupport",
                               setUncheckedException, "(Ljava/lang/Throwable;)V");

            mg.visitJumpInsn(JSR, main_finally_begin); // execute the finally
            mg.visitJumpInsn(GOTO, main_tcf_exit); // Skip to very end
        }

        // For RMI Remote interfaces, if the throws clause contains Exception (and not
        // RemoteException), then we need a special catch block for RemoteException so
        // that it is not treated as an application exception. Legacy behavior did
        // not add this catch if RemotException is on the throws clause, so maintaining
        // that behavior for compatibility. Throwing a subclass of RemoteException was
        // previously not allowed, so that scenario has no compatibility concerns and
        // will treat the RemoteException subclass as a system exception.
        // Also, legacy behavior did not add the catch of RemoteException if only
        // IOException was present; maintaining that behavior.
        // For 2.x local interfaces, catch block is added if either Exception or
        // IOException are present.
        boolean catchRemoteException = false;
        boolean throwsIOException = false;
        if ((isRmiRemote && !DeclaredRemoteAreApplicationExceptions) ||
            (wrapperType == EJBWrapperType.REMOTE || wrapperType == EJBWrapperType.REMOTE_HOME ||
             wrapperType == EJBWrapperType.LOCAL || wrapperType == EJBWrapperType.LOCAL_HOME)) {
            for (Class<?> checkedException : checkedExceptions) {
                catchRemoteException |= checkedException == Exception.class;
                throwsIOException |= checkedException == IOException.class;
            }
            if (isRmiRemote) {
                if (catchRemoteException) {
                    for (Class<?> methodException : methodExceptions) {
                        if (methodException == RemoteException.class) {
                            catchRemoteException = false;
                        } else if (RemoteException.class.isAssignableFrom(methodException)) {
                            catchRemoteException = true;
                            break;
                        }
                    }
                } else if (throwsIOException) {
                    for (Class<?> methodException : methodExceptions) {
                        if (RemoteException.class.isAssignableFrom(methodException)) {
                            catchRemoteException = true;
                            break;
                        }
                    }
                }
            } else {
                catchRemoteException |= throwsIOException;
            }
        }

        Label main_catch_remote_exception = null;
        if (catchRemoteException) {
            // -----------------------------------------------------------------------
            // catch (RemoteException ex)
            // {
            //   s.setUncheckedException(ex);   or   s.setUncheckedLocalException(ex);
            // }
            // -----------------------------------------------------------------------
            main_catch_remote_exception = new Label();
            mg.visitLabel(main_catch_remote_exception);
            int remote_th = mg.newLocal(TYPE_RemoteException);
            mg.storeLocal(remote_th);
            loadEJSDeployedSupport(mg, className, wrapperType, s);
            mg.loadLocal(remote_th);
            mg.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/EJSDeployedSupport",
                               setUncheckedException, "(Ljava/lang/Throwable;)V");

            mg.visitJumpInsn(JSR, main_finally_begin); // execute the finally
            mg.visitJumpInsn(GOTO, main_tcf_exit); // Skip to very end
        }

        // -----------------------------------------------------------------------
        // catch (<checked exception> <ex>)  ->  for each <checked exception>
        // {
        //   s.setCheckedException(<ex>);
        //   throw <ex>;
        // }
        // -----------------------------------------------------------------------
        Label[] main_catch_label = new Label[checkedExceptions.length];
        int caught_ex = mg.newLocal(TYPE_Exception);

        for (int i = 0; i < checkedExceptions.length; i++) {
            main_catch_label[i] = new Label();
            mg.visitLabel(main_catch_label[i]);
            mg.storeLocal(caught_ex);
            loadEJSDeployedSupport(mg, className, wrapperType, s);
            mg.loadLocal(caught_ex);
            mg.visitMethodInsn(INVOKEVIRTUAL,
                               "com/ibm/ejs/container/EJSDeployedSupport",
                               "setCheckedException", "(Ljava/lang/Exception;)V");
            mg.loadLocal(caught_ex);
            mg.visitInsn(ATHROW);
        }

        // -----------------------------------------------------------------------
        // catch (Throwable th)
        // {
        //   s.setUncheckedException(th);   or   s.setUncheckedLocalException(th);
        // }
        // -----------------------------------------------------------------------
        Label main_catch_throwable = new Label();
        mg.visitLabel(main_catch_throwable);
        int main_th = mg.newLocal(TYPE_Throwable);
        mg.storeLocal(main_th);
        loadEJSDeployedSupport(mg, className, wrapperType, s);
        mg.loadLocal(main_th);
        mg.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/EJSDeployedSupport",
                           setUncheckedException, "(Ljava/lang/Throwable;)V");

        mg.visitJumpInsn(JSR, main_finally_begin); // execute the finally
        mg.visitJumpInsn(GOTO, main_tcf_exit); // Skip to very end

        // -----------------------------------------------------------------------
        // finally
        // {
        // -----------------------------------------------------------------------
        Label main_finally_ex = new Label();
        mg.visitLabel(main_finally_ex); // finally - after caught exception
        int main_finally_th = mg.newLocal(TYPE_Throwable);
        mg.storeLocal(main_finally_th);
        mg.visitJumpInsn(JSR, main_finally_begin); // finally - jump/return to actual code
        mg.loadLocal(main_finally_th);
        mg.visitInsn(ATHROW); // finally - re-throw exception

        mg.visitLabel(main_finally_begin); // finally - after normal code flow

        int main_finally_return = mg.newLocal(TYPE_Object);
        mg.storeLocal(main_finally_return);

        // -----------------------------------------------------------------------
        //   try
        //   {
        // -----------------------------------------------------------------------
        Label finally_try_begin = new Label();
        mg.visitLabel(finally_try_begin);

        // -----------------------------------------------------------------------
        //     this.container.postInvoke(this, methodId, s);
        //      or
        //     super.mdbMethodPostInvoke();
        // -----------------------------------------------------------------------
        if (wrapperType != MDB_PROXY && wrapperType != MDB_NO_METHOD_INTERFACE_PROXY) {
            // non-MDB postInvoke processing
            loadContainer(mg, className, wrapperType);
            loadWrapperBase(mg, className, wrapperType); // F743-1756
            mg.push(methodId);
            mg.loadLocal(s);
            mg.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/EJSContainer",
                               EjbPostInvoke,
                               "(Lcom/ibm/ejs/container/EJSWrapperBase;ILcom/ibm/ejs/container/EJSDeployedSupport;)V");
        } else {
            // call MessageEndpointBase.mdbMethodPostInvoke() for a MDB
            loadWrapperBase(mg, className, wrapperType);
            mg.visitMethodInsn(wrapperType == MDB_NO_METHOD_INTERFACE_PROXY ? INVOKEVIRTUAL : INVOKESPECIAL, MESSAGE_ENDPOINT_BASE_STRING,
                               "mdbMethodPostInvoke",
                               "()V");
        }

        // -----------------------------------------------------------------------
        //   }                 // end nested try for postInvoke
        // -----------------------------------------------------------------------
        Label finally_try_end = new Label();
        mg.visitLabel(finally_try_end);

        Label finally_tcf_exit = new Label();
        mg.visitJumpInsn(GOTO, finally_tcf_exit); // Skip to very end

        // -----------------------------------------------------------------------
        // Non-RMI interfaces do not throw RemoteException, but the EJBContainer
        // postInvoke method does specify it is thrown, so for the non-RMI
        // methods, there must be a catch for RemoteException... that will
        // never be executed, but must be present to make Java happy.
        //
        // For MDBs postInvoke is not called on EJBContainer, but on
        // MessageEndpointBase, which doesn't throw RemoteException.  Therefore,
        // we don't need to catch a remoteException for MDBs.
        // -----------------------------------------------------------------------
        Label finally_catch_remote = new Label();
        if (!isRmiRemote && wrapperType != MDB_PROXY && wrapperType != MDB_NO_METHOD_INTERFACE_PROXY) {
            // -----------------------------------------------------------------------
            //   catch ( RemoteException rex )
            //   {
            //     s.setUncheckedLocalException(rex);
            //   }
            // -----------------------------------------------------------------------
            mg.visitLabel(finally_catch_remote);
            int finally_remote = mg.newLocal(TYPE_RemoteException);
            mg.storeLocal(finally_remote);
            loadEJSDeployedSupport(mg, className, wrapperType, s);
            mg.loadLocal(finally_remote);
            mg.visitMethodInsn(INVOKEVIRTUAL,
                               "com/ibm/ejs/container/EJSDeployedSupport",
                               "setUncheckedLocalException", "(Ljava/lang/Throwable;)V");
            mg.visitJumpInsn(GOTO, finally_tcf_exit); // Skip to very end
        }

        mg.visitLabel(finally_tcf_exit);

        // -----------------------------------------------------------------------
        // }                     // end of try / catch / finally
        // -----------------------------------------------------------------------
        // Return from finally, either to the end of the try-catch-finally
        // below for normal code flow... or to the beginning of the finally
        // above for the exception path, to rethrow the exception.
        mg.ret(main_finally_return);

        mg.visitLabel(main_tcf_exit);

        // -----------------------------------------------------------------------
        // return
        // -----------------------------------------------------------------------
        if (returnType != Type.VOID_TYPE) {
            mg.loadLocal(returnValue);
        }
        mg.returnValue();

        // -----------------------------------------------------------------------
        // All Try-Catch-Finally definitions for above
        // -----------------------------------------------------------------------
        if (main_catch_runtime_exception != null) {
            mg.visitTryCatchBlock(main_try_begin, main_try_end,
                                  main_catch_runtime_exception, "java/lang/RuntimeException");
        }
        if (main_catch_remote_exception != null) {
            mg.visitTryCatchBlock(main_try_begin, main_try_end,
                                  main_catch_remote_exception, "java/rmi/RemoteException");
        }
        for (int i = 0; i < checkedExceptions.length; i++) {
            mg.visitTryCatchBlock(main_try_begin, main_try_end,
                                  main_catch_label[i],
                                  convertClassName(checkedExceptions[i].getName()));
        }
        mg.visitTryCatchBlock(main_try_begin, main_try_end,
                              main_catch_throwable, "java/lang/Throwable");
        mg.visitTryCatchBlock(main_try_begin, main_finally_ex, main_finally_ex, null);

        if (!isRmiRemote && wrapperType != MDB_PROXY && wrapperType != MDB_NO_METHOD_INTERFACE_PROXY) {
            mg.visitTryCatchBlock(finally_try_begin, finally_try_end,
                                  finally_catch_remote, "java/rmi/RemoteException");
        }

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

    /**
     * F743-609
     *
     * Adds an asynchronous EJB Wrapper Method. This is a special wrapper method
     * that forwards the asynch method call to the container so it can be scheduled
     * on a work manager for execution on a different thread. <p>
     *
     * @param cw          ASM Class writer to add the method to.
     * @param className   Name of the wrapper class being generated.
     * @param method      Reflection method from the interface defining method to be added to the wrapper.
     * @param methodId    Index into the list of all methods of this wrapper type, for this specific method.
     * @param isRmiRemote true if interface extends java.rmi.Remote.
     * @param wrapperType Type of wrapper to be generated (REMOTE, LOCAL, etc.)
     */
    private static void addEJBAsynchMethod(ClassWriter cw,
                                           String className,
                                           Method method,
                                           int methodId,
                                           boolean isRmiRemote,
                                           EJBWrapperType wrapperType) {
        GeneratorAdapter mg;
        String methodName = method.getName();
        String methodSignature = MethodAttribUtils.jdiMethodSignature(method);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding asynch method : " + methodName + " "
                         + methodSignature);

        // Determine the list of 'checked' exceptions, which will need to have
        // catch blocks in the wrapper... and also perform validation.
        // Note that RemoteException is never a 'checked' exception, and
        // exceptions that are subclasses of other 'checked' exceptions
        // will be eliminated, to avoid 'unreachable' code.
        Class<?>[] methodExceptions = method.getExceptionTypes();

        // Convert the return value, arguments, and exception classes to
        // ASM Type objects, and create the ASMMethod object which will
        // be used to actually add the method and method code.
        Type returnType = Type.getType(method.getReturnType());
        Type[] argTypes = getTypes(method.getParameterTypes());
        Type[] exceptionTypes = getTypes(methodExceptions);

        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method(methodName, returnType, argTypes);

        // Create an ASM GeneratorAdapter object for the ASM Method, which
        // makes generating dynamic code much easier... as it keeps track
        // of the position of the arguments and local variables, etc.
        mg = new GeneratorAdapter(ACC_PUBLIC, m, null, exceptionTypes, cw);

        // -----------------------------------------------------------------------
        // Begin Method Code...
        // -----------------------------------------------------------------------
        mg.visitCode();

        // -----------------------------------------------------------------------
        // EJSDeployedSupport s = new EJSDeployedSupport();
        // -----------------------------------------------------------------------
        int s = mg.newLocal(TYPE_EJSDeployedSupport);
        mg.newInstance(TYPE_EJSDeployedSupport);
        mg.dup();
        mg.visitMethodInsn(INVOKESPECIAL, "com/ibm/ejs/container/EJSDeployedSupport", "<init>", "()V");
        mg.storeLocal(s);

        // -----------------------------------------------------------------------
        // returnValue = null
        // -----------------------------------------------------------------------
        int returnValue = -1;
        if (returnType != Type.VOID_TYPE) {
            // If method return type is not void, save room on stack for return value.
            returnValue = mg.newLocal(returnType);

            // push a null on the stack
            mg.visitInsn(ACONST_NULL);

            mg.storeLocal(returnValue);
        }

        // --------------------------------------------------------------------
        // Object[] args = null;
        // --------------------------------------------------------------------
        int args = mg.newLocal(TYPE_Object_ARRAY);
        mg.visitInsn(ACONST_NULL);
        mg.storeLocal(args);

        // -----------------------------------------------------------------------
        // args = new Object[# of args];
        // -----------------------------------------------------------------------
        mg.push(argTypes.length);
        mg.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mg.storeLocal(args);

        // -----------------------------------------------------------------------
        // args[i] = "parameter"; -> for each parameter
        // -----------------------------------------------------------------------
        for (int i = 0; i < argTypes.length; i++) {
            mg.loadLocal(args);
            mg.push(i);

            // Convert primitives to objects to put in 'args' array.
            switch (argTypes[i].getSort()) {
                case Type.BOOLEAN:
                    mg.visitTypeInsn(NEW, "java/lang/Boolean");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Boolean", "<init>",
                                       "(Z)V");
                    break;

                case Type.CHAR:
                    mg.visitTypeInsn(NEW, "java/lang/Character");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Character", "<init>",
                                       "(C)V");
                    break;

                case Type.BYTE:
                    mg.visitTypeInsn(NEW, "java/lang/Byte");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Byte", "<init>",
                                       "(B)V");
                    break;

                case Type.SHORT:
                    mg.visitTypeInsn(NEW, "java/lang/Short");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Short", "<init>",
                                       "(S)V");
                    break;

                case Type.INT:
                    mg.visitTypeInsn(NEW, "java/lang/Integer");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>",
                                       "(I)V");
                    break;

                case Type.FLOAT:
                    mg.visitTypeInsn(NEW, "java/lang/Float");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Float", "<init>",
                                       "(F)V");
                    break;

                case Type.LONG:
                    mg.visitTypeInsn(NEW, "java/lang/Long");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Long", "<init>",
                                       "(J)V");
                    break;

                case Type.DOUBLE:
                    mg.visitTypeInsn(NEW, "java/lang/Double");
                    mg.visitInsn(DUP);
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    mg.visitMethodInsn(INVOKESPECIAL, "java/lang/Double", "<init>",
                                       "(D)V");
                    break;

                // ARRAY & OBJECT - no need to copy, just load the arg
                default:
                    mg.loadArg(i); // for non-static, arg 0 = "this"
                    break;
            }

            mg.visitInsn(AASTORE);

        } // End for loop

        // -----------------------------------------------------------------------
        // returnValue = container.scheduleAsynchMethod( this,
        //                                               methodId,
        //                                               args);
        // -----------------------------------------------------------------------
        loadContainer(mg, className, wrapperType);
        loadWrapperBase(mg, className, wrapperType); // F743-1756
        mg.push(methodId);
        mg.loadLocal(args);
        mg.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/EJSContainer",
                           "scheduleAsynchMethod",
                           "(Lcom/ibm/ejs/container/EJSWrapperBase;I[Ljava/lang/Object;)Ljava/util/concurrent/Future;");

        // F743-609CodRev
        // If method return type is not void, store the return value on the stack.
        // Otherwise, pop the extra space off the stack.  We don't have anything to return.
        if (returnType != Type.VOID_TYPE) {
            mg.storeLocal(returnValue);
        } else {
            mg.pop();
        }

        // -----------------------------------------------------------------------
        // return
        // -----------------------------------------------------------------------
        if (returnType != Type.VOID_TYPE) {
            mg.loadLocal(returnValue);
        }
        mg.returnValue();

        // -----------------------------------------------------------------------
        // End Asynch Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

    /**
     * Adds a ManagedBean Wrapper Method. <p>
     *
     * @param cw             ASM Class writer to add the method to.
     * @param className      name of the wrapper class being generated.
     * @param implClassName  name of the EJB implementation class.
     * @param method         reflection method from the interface defining
     *                           method to be added to the wrapper.
     * @param implMethodName name of the method on the EJB implementation
     *                           class (may be different for homes).
     * @param methodId       index into the list of all methods of this
     *                           wrapper type, for this specific method.
     * @param aroundInvoke   true if interceptors are present.
     **/
    // F743-34301.1
    private static void addManagedBeanMethod(ClassWriter cw,
                                             String className,
                                             String implClassName,
                                             Method method,
                                             String implMethodName,
                                             int methodId,
                                             boolean aroundInvoke) {
        GeneratorAdapter mg;
        String methodName = method.getName();
        String methodSignature = MethodAttribUtils.jdiMethodSignature(method);
        String EjbPreInvoke = "EjbPreInvokeForManagedBean";

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : " +
                         methodName + " " + methodSignature +
                         " : aroundInvoke = " + aroundInvoke);

        // Determine the list of 'checked' exceptions, which will need to have
        // catch blocks in the wrapper... and also perform validation.
        // Note that RemoteException is never a 'checked' exception, and
        // exceptions that are subclasses of other 'checked' exceptions
        // will be eliminated, to avoid 'unreachable' code.
        Class<?>[] methodExceptions = method.getExceptionTypes();

        // Convert the return value, arguments, and exception classes to
        // ASM Type objects, and create the ASM Method object which will
        // be used to actually add the method and method code.
        Type returnType = Type.getType(method.getReturnType());
        Type[] argTypes = getTypes(method.getParameterTypes());
        Type[] exceptionTypes = getTypes(methodExceptions);

        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method(methodName, returnType, argTypes);

        // Create an ASM GeneratorAdapter object for the ASM Method, which
        // makes generating dynamic code much easier... as it keeps track
        // of the position of the arguments and local variables, etc.
        mg = new GeneratorAdapter(ACC_PUBLIC, m, null, exceptionTypes, cw);

        // -----------------------------------------------------------------------
        // Begin Method Code...
        // -----------------------------------------------------------------------
        mg.visitCode();

        // -----------------------------------------------------------------------
        // When no AroundInvoke interceptors... the code is very simple - the
        // method call is just passed through to the actual instance, which may
        // be found in the BeanO stored on the wrapper.
        // -----------------------------------------------------------------------
        if (!aroundInvoke) {
            // --------------------------------------------------------------------
            //   <bean impl> bean = (<bean impl>)ivBeanO.getBeanInstance();
            // --------------------------------------------------------------------
            Type implType = Type.getType("L" + implClassName + ";");
            int bean = mg.newLocal(implType);
            mg.loadThis();
            mg.visitFieldInsn(GETFIELD, className, "ivBeanO",
                              "Lcom/ibm/ejs/container/BeanO;");
            mg.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/BeanO",
                               "getBeanInstance", "()Ljava/lang/Object;");
            mg.checkCast(implType);
            mg.storeLocal(bean);

            // --------------------------------------------------------------------
            //   bean.<method>(<args...>);
            //      or
            //   rtnValue = bean.<method>(<args...>);
            // --------------------------------------------------------------------
            mg.loadLocal(bean);
            mg.loadArgs(0, argTypes.length); // do not pass "this"
            mg.visitMethodInsn(INVOKEVIRTUAL, implClassName, implMethodName,
                               m.getDescriptor());
            mg.returnValue();

            // -----------------------------------------------------------------------
            // End Method Code...
            // -----------------------------------------------------------------------
            mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
            mg.visitEnd();

            return;
        }

        // -----------------------------------------------------------------------
        // The rest of the code is for handling AroundInvoke interceptors.
        // -----------------------------------------------------------------------

        // -----------------------------------------------------------------------
        // EJSDeployedSupport s = new EJSDeployedSupport();
        // -----------------------------------------------------------------------
        int s = mg.newLocal(TYPE_EJSDeployedSupport);
        mg.newInstance(TYPE_EJSDeployedSupport);
        mg.dup();
        mg.visitMethodInsn(INVOKESPECIAL, "com/ibm/ejs/container/EJSDeployedSupport", "<init>", "()V");
        mg.storeLocal(s);

        // -----------------------------------------------------------------------
        // <return type> returnValue = false | 0 | null;
        // -----------------------------------------------------------------------
        int returnValue = -1;
        if (returnType != Type.VOID_TYPE) {
            returnValue = mg.newLocal(returnType);
            switch (returnType.getSort()) {
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
        // Object[] args;
        // -----------------------------------------------------------------------
        int args = mg.newLocal(TYPE_Object_ARRAY);

        // -----------------------------------------------------------------------
        // args = new Object[# of args];
        // -----------------------------------------------------------------------
        mg.push(argTypes.length);
        mg.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mg.storeLocal(args);

        // -----------------------------------------------------------------------
        // args[i] = "parameter";  ->  for each parameter
        // -----------------------------------------------------------------------
        for (int i = 0; i < argTypes.length; i++) {
            mg.loadLocal(args);
            mg.push(i);

            // Convert primitives to objects to put in 'args' array.
            switch (argTypes[i].getSort()) {
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

        // -----------------------------------------------------------------------
        // <bean impl> bean = (bean impl)container.EjbPreInvokeForManagedBean
        //                                                    ( this,
        //                                                      methodId,
        //                                                      s,
        //                                                      ivBeanO,
        //                                                      args );
        // -----------------------------------------------------------------------
        Type implType = Type.getType("L" + implClassName + ";");
        int bean = mg.newLocal(implType);
        loadContainer(mg, className, MANAGED_BEAN);
        loadWrapperBase(mg, className, MANAGED_BEAN);
        mg.push(methodId);
        mg.loadLocal(s);
        mg.loadThis();
        mg.visitFieldInsn(GETFIELD, className, "ivBeanO", "Lcom/ibm/ejs/container/BeanO;");
        mg.loadLocal(args);
        mg.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/EJSContainer",
                           EjbPreInvoke,
                           "(Lcom/ibm/ejs/container/EJSWrapperBase;ILcom/ibm/ejs/container/EJSDeployedSupport;Lcom/ibm/ejs/container/BeanO;[Ljava/lang/Object;)Ljava/lang/Object;");
        mg.checkCast(implType);
        mg.storeLocal(bean);

        // -----------------------------------------------------------------------
        // Now invoke the business method through the interceptors via
        // EJSContainer.invoke();
        // -----------------------------------------------------------------------

        // --------------------------------------------------------------------
        // container.invoke(s);
        //    or
        // rtnValue = (type)container.invoke(s);
        //    or
        // rtnValue = ((object type)container.invoke(s)).<type>Value();  // unbox
        // --------------------------------------------------------------------
        loadContainer(mg, className, MANAGED_BEAN);
        mg.loadLocal(s);
        mg.visitInsn(ACONST_NULL);
        mg.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/container/EJSContainer",
                           "invoke",
                           "(Lcom/ibm/ejs/container/EJSDeployedSupport;Ljavax/ejb/Timer;)Ljava/lang/Object;");

        if (returnType == Type.VOID_TYPE) {
            // No return value, just pop the returned value (null) off the stack
            mg.pop();
        } else {
            // Unbox any primitive values or add the appropriate cast
            // for object/array values, and then store in local variable. d369262.7
            unbox(mg, returnType);
            mg.storeLocal(returnValue);
        }

        // -----------------------------------------------------------------------
        // return
        // -----------------------------------------------------------------------
        if (returnType != Type.VOID_TYPE) {
            mg.loadLocal(returnValue);
        }
        mg.returnValue();

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

    /**
     * Adds a non-public no-interface view EJB Wrapper Method. <p>
     *
     * The added method will just throw an EJBException. <p>
     *
     * @param cw     ASM Class writer to add the method to.
     * @param method reflection method from the interface defining
     *                   method to be added to the wrapper.
     **/
    // F743-1756
    private static void addNonPublicMethod(ClassWriter cw,
                                           Method method) {
        GeneratorAdapter mg;
        String methodName = method.getName();
        String methodSignature = MethodAttribUtils.jdiMethodSignature(method);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : " + methodName +
                         " " + methodSignature + " : Non-Public");

        Class<?>[] methodExceptions = method.getExceptionTypes();

        // Convert the return value, arguments, and exception classes to
        // ASM Type objects, and create the ASM Method object which will
        // be used to actually add the method and method code.
        Type returnType = Type.getType(method.getReturnType());
        Type[] argTypes = getTypes(method.getParameterTypes());
        Type[] exceptionTypes = getTypes(methodExceptions);

        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method(methodName, returnType, argTypes);

        // Create an ASM GeneratorAdapter object for the ASM Method, which
        // makes generating dynamic code much easier... as it keeps track
        // of the position of the arguements and local variables, etc.
        mg = new GeneratorAdapter(ACC_PUBLIC, m, null, exceptionTypes, cw);

        // -----------------------------------------------------------------------
        // Begin Method Code...
        // -----------------------------------------------------------------------
        mg.visitCode();

        // -----------------------------------------------------------------------
        // throw new EJBException("Only public methods of the bean class may be
        //                         invoked through the no-interface view.");
        // -----------------------------------------------------------------------
        mg.visitTypeInsn(NEW, "javax/ejb/EJBException");
        mg.visitInsn(DUP);
        mg.visitLdcInsn("Only public methods of the bean class may be invoked through the no-interface or no-method-interface view.");
        mg.visitMethodInsn(INVOKESPECIAL, "javax/ejb/EJBException", "<init>", "(Ljava/lang/String;)V");
        mg.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        // End Non-Public Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

    /**
     * Returns the fully qualified 'internal' name of the parent class
     * for the specified wrapper type. <p>
     *
     * Note that for No-Interface View (LocalBean), an interface to be
     * implemented is returned rather than the parent class, as for
     * No-Interface View, the parent class must be the bean implementation. <p>
     *
     * @param wrapperType the type of wrapper being generated.
     **/
    private static String getParentClassName(EJBWrapperType wrapperType, String internalEJBClassName) {
        // Basically, just map the wrapper type to the subclass of
        // EJSWrapperBase that corresponds.

        // Note: a 'switch' statement could be used here.... except
        // when using enums, that results in very inefficient code
        // being generated, as it will use an inner class and multiple
        // try catch blocks... ouch!

        String internalParentName = null;

        if (wrapperType == REMOTE ||
            wrapperType == REMOTE_HOME) {
            internalParentName = "com/ibm/ejs/container/EJSWrapper";
        } else if (wrapperType == LOCAL ||
                   wrapperType == LOCAL_HOME) {
            internalParentName = "com/ibm/ejs/container/EJSLocalWrapper";
        } else if (wrapperType == BUSINESS_LOCAL) {
            internalParentName = "com/ibm/ejs/container/BusinessLocalWrapper";
        } else if (wrapperType == BUSINESS_REMOTE) {
            internalParentName = "com/ibm/ejs/container/BusinessRemoteWrapper";
        } else if (wrapperType == LOCAL_BEAN || // F743-1756
                   wrapperType == MANAGED_BEAN) {
            internalParentName = "com/ibm/ejs/container/LocalBeanWrapper";
        } else if (wrapperType == MDB_PROXY) {
            internalParentName = "com/ibm/ws/ejbcontainer/mdb/MessageEndpointBase";
        } else if (wrapperType == MDB_NO_METHOD_INTERFACE_PROXY) {
            internalParentName = internalEJBClassName;
        } else {
            // This is an APAR condition... but should never really happen.
            // Really, this is here to make it obvious that a change is
            // needed here when new wrapperTypes are added in the future,
            // so a Tr.error is not needed.                               d457128.2
            throw new ContainerEJBException("EJBContainer internal error: " +
                                            "Wrapper Type not supported: " +
                                            wrapperType);
        }

        return internalParentName;
    }

    /**
     * Generates code to load the object which extends EJSWrapperBase. <p>
     *
     * Historically, the wrapper itself would have extended EJSWrapperBase,
     * so this would be equivalent to 'loadThis()'. However, when the wrapper
     * is for the No-Interface View or no-method interface MDB, it must extend
     * the EJB class, and so will instead hold a reference (instance variable)
     * to an object that does extend EJSWrapperBase. <p>
     *
     * This method will properly load the EJSWrapperBase object, whether that
     * be the wrapper itself, or an instance variable on the wrapper. <p>
     *
     * Although this method could be in-lined, it has been provided to make
     * the calling code more readable, and to make it more obvious why
     * this change is required. <p>
     *
     * @param mg          ASM method generator for the current method.
     * @param className   Name of the wrapper class being generated.
     * @param wrapperType used to check if this is for a No-Interface
     *                        view wrapper or no-method interface MDB proxy.
     **/
    // F743-1756
    private static void loadWrapperBase(GeneratorAdapter mg,
                                        String className,
                                        EJBWrapperType wrapperType) {
        mg.loadThis();
        if (wrapperType == LOCAL_BEAN || wrapperType == MANAGED_BEAN) {
            mg.visitFieldInsn(GETFIELD, className, LOCAL_BEAN_WRAPPER_FIELD,
                              LOCAL_BEAN_WRAPPER_FIELD_TYPE);
        } else if (wrapperType == MDB_NO_METHOD_INTERFACE_PROXY) {
            mg.visitFieldInsn(GETFIELD, className, MESSAGE_ENDPOINT_BASE_FIELD,
                              MESSAGE_ENDPOINT_BASE_FIELD_TYPE);
        }
    }

    /**
     * Generates code to load the container object on EJSWrapperBase. <p>
     *
     * This method will properly load the EJSWrapperBase.container object, whether that
     * be (1) located on the wrapper itself, or (2) from the EJSWrapperBase instance variable
     * on the wrapper. (2) occurrs when we have a LocalBean or a no-method interface MDB.<p>
     *
     * Although this method could be in-lined, it has been provided to make
     * the calling code more readable, and to make it more obvious why
     * this change is required. <p>
     *
     * @param mg          ASM method generator for the current method.
     * @param className   Name of the wrapper class being generated.
     * @param wrapperType used to check if this is for a No-Interface
     *                        view wrapper or no-method interface MDB proxy.
     **/
    // F743-1756
    private static void loadContainer(GeneratorAdapter mg,
                                      String className,
                                      EJBWrapperType wrapperType) {
        loadWrapperBase(mg, className, wrapperType);
        mg.visitFieldInsn(GETFIELD, "com/ibm/ejs/container/EJSWrapperBase", "container", EJS_CONTAINER_FIELD_TYPE);
    }

    private static void loadEJSDeployedSupport(GeneratorAdapter mg, String className, EJBWrapperType wrapperType, int s) {

        if (wrapperType != MDB_PROXY && wrapperType != MDB_NO_METHOD_INTERFACE_PROXY) {
            mg.loadLocal(s);
        } else {
            // MDBs hold the EJSDeployedSupport under MessageEndpontBase.ivEJSDeployedSupport
            loadWrapperBase(mg, className, wrapperType);
            mg.visitFieldInsn(GETFIELD, MESSAGE_ENDPOINT_BASE_STRING, "ivEJSDeployedSupport", "Lcom/ibm/ejs/container/EJSDeployedSupport;");
        }
    }

    /**
     * Validate the basic aspects of a business or component interface as
     * required by the EJB specification. This includes checking to insure
     * the interface is an interface, and that it does or does not extend
     * the javax.ejb interfaces (such as javax.ejb.EJBObject), corresponding
     * to the configured interface type ( business or component / local or
     * remote / home ). <p>
     *
     * Additional method level validation will be performed when the
     * 'wrapper' is generated. <p>
     *
     * @param wrapperInterface business or component interface class to validate.
     * @param wrapperType      the type of wrapper being generated.
     * @param beanName         name used to identify the bean if an error is logged.
     *
     * @throws EJBConfigurationException whenever the specified interface does
     *                                       not conform the the EJB Specification requirements.
     **/
    // d431543
    static void validateInterfaceBasics(Class<?> wrapperInterface,
                                        EJBWrapperType wrapperType,
                                        String beanName) throws EJBConfigurationException {
        // All business and component interfaces must be 'interfaces'!
        // Except the No-Interface View (LocalBean).                     F743-1756
        if (wrapperType != LOCAL_BEAN &&
            wrapperType != MANAGED_BEAN && // F743-34301.1
            !Modifier.isInterface(wrapperInterface.getModifiers())) {
            // Log the error and throw meaningful exception.              d457128.2
            Tr.error(tc, "JIT_INTERFACE_NOT_INTERFACE_CNTR5011E",
                     new Object[] { beanName,
                                    wrapperInterface.getName() });
            throw new EJBConfigurationException("Configured " + wrapperType + " interface is not an interface : " +
                                                wrapperInterface.getName() + " of bean " + beanName);
        }

        // This may appear to be a good place to use a 'switch', but using
        // 'switch' with 'enum's performs very poorly, as it involves inner
        // classes, and repeated validation of the types.

        if (wrapperType == EJBWrapperType.BUSINESS_LOCAL ||
            wrapperType == EJBWrapperType.BUSINESS_REMOTE) {
            Class<?> invalidExtends = getInvalidBusinessExtends(wrapperInterface);
            if (invalidExtends != null) {
                // Log the error and throw meaningful exception.           d457128.2
                Tr.error(tc, "JIT_INVALID_EXTENDS_JAVAX_EJB_CNTR5012E",
                         new Object[] { beanName,
                                        wrapperInterface.getName(),
                                        invalidExtends.getName() });
                throw new EJBConfigurationException("Configured " + wrapperType + " interface extends " +
                                                    invalidExtends.getName() + " : " +
                                                    wrapperInterface.getName() + " of bean " + beanName);
            }

            if (wrapperType == EJBWrapperType.BUSINESS_LOCAL &&
                (Remote.class).isAssignableFrom(wrapperInterface)) {
                // Log the error and throw meaningful exception.           d457128.2
                Tr.error(tc, "JIT_INVALID_EXTENDS_REMOTE_CNTR5013E",
                         new Object[] { beanName,
                                        wrapperInterface.getName() });
                throw new EJBConfigurationException("Configured " + wrapperType + " interface extends " +
                                                    "javax.rmi.Remote : " +
                                                    wrapperInterface.getName() + " of bean " + beanName);
            }
        } else if (wrapperType == EJBWrapperType.LOCAL) {
            if (!(EJBLocalObject.class).isAssignableFrom(wrapperInterface)) {
                // Log the error and throw meaningful exception.           d457128.2
                Tr.error(tc, "JIT_MUST_EXTEND_EJBLOCAL_CNTR5014E",
                         new Object[] { beanName,
                                        wrapperInterface.getName() });
                throw new EJBConfigurationException("Configured " + wrapperType + " interface does not extend " +
                                                    (EJBLocalObject.class).getName() + " : " +
                                                    wrapperInterface.getName() + " of bean " + beanName);
            }
        } else if (wrapperType == EJBWrapperType.REMOTE) {
            if (!(EJBObject.class).isAssignableFrom(wrapperInterface)) {
                // Log the error and throw meaningful exception.           d457128.2
                Tr.error(tc, "JIT_MUST_EXTEND_EJBOBJECT_CNTR5015E",
                         new Object[] { beanName,
                                        wrapperInterface.getName() });
                throw new EJBConfigurationException("Configured " + wrapperType + " interface does not extend " +
                                                    (EJBObject.class).getName() + " : " +
                                                    wrapperInterface.getName() + " of bean " + beanName);
            }
        }

        // The optional WebService Endpoint interface must be Remote.
        // And must not contain any constant declarations.               LI3294-35
        else if (wrapperType == EJBWrapperType.SERVICE_ENDPOINT) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                // JAX-RPC must implement Remote, but JAX-WS does not have that
                // requirement... so just log this information to trace.     d545748
                if (!(Remote.class).isAssignableFrom(wrapperInterface)) {
                    Tr.debug(tc, "Configured " + wrapperType +
                                 " interface does not         extend " +
                                 (Remote.class).getName() + " : " +
                                 wrapperInterface.getName() + " of bean " +
                                 beanName);
                }

                // WebServices has message CHKW6010E for this condition, but ignores
                // the problem, so JITDeploy will too... though the condition will
                // be traced for now.                                        d493833
                Field[] fields = wrapperInterface.getFields();
                for (Field field : fields) {
                    int modifier = field.getModifiers();
                    if (Modifier.isStatic(modifier) &&
                        Modifier.isFinal(modifier)) {
                        Tr.debug(tc, "Configured " + wrapperType +
                                     " interface declares a constant : " + field.getName() +
                                     " on " + wrapperInterface.getName() +
                                     " of bean " + beanName);
                    }
                }
            }
        }

        // For No-Interface View (LocalBean), the 'interface' is the
        // EJB implementation class, and must just be a valid one.
        // There are some additional requirments on the methods, but the
        // more advanced checking for that will be done when the methods
        // are generated.                                                F743-1756
        else if (wrapperType == LOCAL_BEAN) {
            // For performance, the EJB Class will not be validated here,
            // as it is assumed to be validated separately already.
        }
    }

    /**
     * Internal method used to check the business interfaces (local and remote)
     * to insure they do not extend any of the javax.ejb classes. <p>
     *
     * If the wrapper interface is found to extend one of the invalid
     * interfaces, that invalid interface is returned to allow a specific
     * message to be logged. <p>
     *
     * @param wrapperInterface one of the business local or remote interfaces.
     *
     * @return the invalid class that is extended, or null if none.
     **/
    //  d457128.2
    private static Class<?> getInvalidBusinessExtends(Class<?> wrapperInterface) {
        if ((EJBLocalObject.class).isAssignableFrom(wrapperInterface))
            return EJBLocalObject.class;
        if ((EJBLocalHome.class).isAssignableFrom(wrapperInterface))
            return EJBLocalHome.class;
        if ((EJBObject.class).isAssignableFrom(wrapperInterface))
            return EJBObject.class;
        if ((EJBHome.class).isAssignableFrom(wrapperInterface))
            return EJBHome.class;

        return null;
    }

    /**
     * Validate the specified method for a no-interface view (LocalBean). <p>
     *
     * The following are verified:
     * <ul>
     * <li> The method is not final.
     * <li> Public methods do not throw RemoteException.
     * </ul>
     *
     * @param method   the method to be validated.
     * @param beanName name of the bean for serviceability.
     *
     * @throws EJBConfigurationException is thrown if the method is determined
     *                                       to not be a valid no-interface view (LocalBean) method.
     **/
    // F743-1756
    private static void validateLocalBeanMethod(Method method,
                                                String beanName) throws EJBConfigurationException {
        int modifiers = method.getModifiers();

        // -----------------------------------------------------------------------
        // Methods on an EJB that exposes a No-Interface view (LocalBean),
        // must not be final (regardless whether public or not).
        // -----------------------------------------------------------------------
        if (Modifier.isFinal(modifiers)) {
            // Log the error and throw meaningful exception.
            String className = method.getDeclaringClass().getName();
            Tr.error(tc, "JIT_INVALID_FINAL_METHOD_CNTR5106E",
                     new Object[] { method.getName(), className, beanName });
            throw new EJBConfigurationException("Method " + method.getName() + " on class " +
                                                className + " must not be declared as final " +
                                                "for the no-interface local view of bean " + beanName + ".");
        }

        // -----------------------------------------------------------------------
        // Public methods exposed through the No-Interface view (LocalBean)
        // must not have RemoteException (or subclass of) on the throws clause.
        // -----------------------------------------------------------------------
        if (Modifier.isPublic(modifiers)) {
            Class<?>[] exceptions = method.getExceptionTypes();

            for (Class<?> exception : exceptions) {
                if ((RemoteException.class).isAssignableFrom(exception)) {
                    // Log the error and throw meaningful exception.
                    String className = method.getDeclaringClass().getName();
                    Tr.error(tc, "JIT_INVALID_THROW_REMOTE_CNTR5101W",
                             new Object[] { method.getName(), className });
                    throw new EJBConfigurationException("Method " + method.getName() + " on class " +
                                                        className + " must not define the " +
                                                        "java.rmi.RemoteException exception on the throws " +
                                                        "clause for the no-interface local view of bean " +
                                                        beanName + ".");
                }
            }
        }
    }
}
