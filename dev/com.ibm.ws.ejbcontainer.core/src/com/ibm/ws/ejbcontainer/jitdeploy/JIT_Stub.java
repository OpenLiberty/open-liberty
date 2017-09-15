/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.TYPE_CORBA_2_3_InputStream;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.TYPE_CORBA_2_3_OutputStream;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.TYPE_CORBA_ApplicationException;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.TYPE_CORBA_InputStream;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.TYPE_CORBA_ServantObject;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.TYPE_CORBA_SystemException;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.getRemoteTypeIds;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.isMutable;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.read_value;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.write_value;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.INDENT;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_Object;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_Object_ARRAY;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_String;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_Throwable;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.convertClassName;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.getTypes;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.jdiMethodSignature;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.writeToClassFile;
import static com.ibm.ws.ejbcontainer.jitdeploy.RMItoIDL.getIdlExceptionName;
import static com.ibm.ws.ejbcontainer.jitdeploy.RMItoIDL.getIdlMethodNames;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.JSR;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_2;
import static org.objectweb.asm.Type.VOID_TYPE;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.ejs.container.util.DeploymentUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.ejbcontainer.JITDeploy;

/**
 * Provides Just In Time deployment of Stubs for EJB Wrapper classes. <p>
 */
public final class JIT_Stub {

    private static final TraceComponent tc = Tr.register(JIT_Stub.class,
                                                         JITUtils.JIT_TRACE_GROUP,
                                                         JITUtils.JIT_RSRC_BUNDLE);

    private static final Type TYPE_RemoteException = Type.getType("Ljava/rmi/RemoteException;");
    private static final Type TYPE_TransactionRolledbackException = Type.getType("Ljavax/transaction/TransactionRolledbackException;");

    /**
     * Utility method that returns the name of the remote interface
     * corresponding to the specified Stub class name. <p>
     *
     * Null will be returned if the specified name is either null or
     * not a valid Stub class name. <p>
     *
     * Basically, the name of the Stub class for any remote interface is
     * the name of the remote interface class, with an '_' prepended,
     * and '_Stub' appended. <p>
     *
     * This method properly modifies the class name with or without
     * a package qualification. <p>
     *
     * @param stubClassName the fully qualified class name of the
     *            Stub class.
     **/
    public static String getRemoteInterfaceName(String stubClassName) {
        if (stubClassName != null && stubClassName.endsWith("_Stub") &&
            !stubClassName.endsWith("._Stub")) // PM25511
        {
            StringBuilder nameBuilder = new StringBuilder(stubClassName);
            nameBuilder.setLength(nameBuilder.length() - 5);

            int packageOffset = nameBuilder.lastIndexOf(".") + 1;
            if (nameBuilder.charAt(packageOffset) == '_')
                nameBuilder.deleteCharAt(packageOffset);
            else
                return null;

            return nameBuilder.toString();
        }

        return null;
    }

    /**
     * Core method for generating the Stub class bytes. Intended for
     * use by JITDeploy only (should not be called directly). <p>
     *
     * @param remoteInterface Interface implemented by the generated Stub;
     *            not required to implement java.rmi.Remote.
     * @param rmicCompatible rmic compatibility flags
     **/
    // d457086
    public static byte[] generateStubBytes(Class<?> remoteInterface,
                                           int rmicCompatible) throws EJBConfigurationException {
        String stubClassName = getStubClassName(remoteInterface.getName());
        Method[] remoteMethods = remoteInterface.getMethods();
        String[] idlNames = getIdlMethodNames(remoteMethods);

        byte[] classBytes = generateClassBytes(stubClassName,
                                               remoteInterface,
                                               remoteMethods,
                                               idlNames,
                                               rmicCompatible); // PM46698
        return classBytes;
    }

    /**
     * Utility method that returns the name of the Stub class that needs to
     * be generated for the specified remote interface class. Intended for
     * use by JITDeploy only (should not be called directly). <p>
     *
     * Basically, the name of the Stub class for any remote interface is
     * the name of the remote interface class, with an '_' prepended,
     * and '_Stub' appended. <p>
     *
     * This method properly modifies the class name with or without
     * a package qualification. <p>
     *
     * @param remoteInterfaceName the fully qualified class name of the
     *            remote interface.
     **/
    public static String getStubClassName(String remoteInterfaceName) {
        StringBuilder stubBuilder = new StringBuilder(remoteInterfaceName);
        int packageOffset = Math.max(remoteInterfaceName.lastIndexOf('.') + 1,
                                     remoteInterfaceName.lastIndexOf('$') + 1);
        stubBuilder.insert(packageOffset, '_');
        stubBuilder.append("_Stub");

        return stubBuilder.toString();
    }

    /**
     * Core method for generating the Stub class bytes. Intended for
     * use by JITDeploy only (should not be called directly). <p>
     *
     * Although the 'methods' parameter could be obtained from the
     * specified remote interface, the 'methods' and 'idlNames'
     * parameters are requested to improve performance... allowing
     * the two arrays to be obtained once and shared by the code
     * that generates the corresponding Tie class. <p>
     *
     * @param stubClassName name of the Stub class to be generated.
     * @param remoteInterface Interface implemented by the generated Stub;
     *            not required to implement java.rmi.Remote.
     * @param remoteMethods Methods from the specified remote interface;
     *            passed for improved performance.
     * @param idlNames OMG IDL names corresponding to the
     *            remoteMethods parameter.
     * @param rmicCompatible rmic compatibility flags
     **/
    static byte[] generateClassBytes(String stubClassName,
                                     Class<?> remoteInterface,
                                     Method[] remoteMethods,
                                     String[] idlNames,
                                     int rmicCompatible) throws EJBConfigurationException {
        String[] remoteInterfaceNames;
        int numMethods = remoteMethods.length;

        // ASM uses 'internal' java class names (like JNI) where '/' is
        // used instead of '.', so convert the parameters to 'internal' format.
        String internalStubClassName = convertClassName(stubClassName);
        String internalInterfaceName = convertClassName(remoteInterface.getName());

        // Stubs may only be generated for interfaces.                     d458392
        if (!Modifier.isInterface(remoteInterface.getModifiers())) {
            throw new EJBConfigurationException("The " + remoteInterface.getName() + " class is not an interface class. " +
                                                "Stubs may only be generated for interface classes.");
        }

        // Remote Business interfaces may or may not extend java.rmi.Remote
        boolean isRmiRemote = (Remote.class).isAssignableFrom(remoteInterface);
        boolean isAbstractInterface = isRmiRemote || CORBA_Utils.isAbstractInterface(remoteInterface, rmicCompatible);

        // The ORB requires that all stubs implement java.rmi.Remote, so add
        // that in for those EJB 'business' remote interfaces that do not.
        // The methods will NOT throw RemoteException, but the generated
        // stub implementation will handle that.                         d413752.1
        if (isRmiRemote)
            remoteInterfaceNames = new String[] { internalInterfaceName };
        else
            remoteInterfaceNames = new String[] { internalInterfaceName, "java/rmi/Remote" };

        // The set of classes that are used as constants.  Support for the
        // CONSTANT_Class type for the "ldc" opcode was not added to the class
        // file format until 1.5.  Prior to that, class files must generate
        // synthetic fields for each class constant, and then invoke a synthetic
        // class$() method that calls Class.forName.                      d676434
        Set<String> classConstantFieldNames = new LinkedHashSet<String>();

        Class<?>[][] checkedExceptions = new Class<?>[numMethods][];
        for (int i = 0; i < numMethods; ++i) {
            checkedExceptions[i] = DeploymentUtil.getCheckedExceptions(remoteMethods[i],
                                                                       isRmiRemote,
                                                                       DeploymentUtil.DeploymentTarget.STUB); // d660332
        }

        if (TraceComponent.isAnyTracingEnabled()) {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "generateClassBytes");
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, INDENT + "className = " + internalStubClassName);
                Tr.debug(tc, INDENT + "interface = " + internalInterfaceName);
                if (isRmiRemote)
                    Tr.debug(tc, INDENT + "implements java.rmi.Remote");
            }
        }

        // Create the ASM Class Writer to write out a Stub
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS); //F743-11995

        // Define the Stub Class object
        cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER,
                 internalStubClassName,
                 null,
                 isAbstractInterface ? "javax/rmi/CORBA/Stub" : "com/ibm/ejs/container/SerializableStub", // PM46698
                 remoteInterfaceNames);

        // Define the source code file and debug settings
        String sourceFileName = stubClassName.substring(stubClassName.lastIndexOf(".") + 1) + ".java";
        cw.visitSource(sourceFileName, null);

        // Add the static and instance variables common to all Stubs.
        addFields(cw);

        // Initialize the static fields common to all Stubs.
        initializeStaticFields(cw, internalStubClassName, remoteInterface);

        // Add the public no parameter Stub constructor
        addCtor(cw, internalStubClassName, classConstantFieldNames, // RTC111522
                remoteInterface, isAbstractInterface);

        // Add the methods common to all Stub classes.
        addCommonStubMethods(cw, internalStubClassName);

        // Add all of the methods to the Stub, based on the reflected
        // Method objects from the interface.
        for (int i = 0; i < numMethods; ++i) {
            addStubMethod(cw,
                          internalStubClassName,
                          classConstantFieldNames, // RTC111522
                          remoteMethods[i],
                          checkedExceptions[i], // d676434
                          idlNames[i],
                          isRmiRemote,
                          rmicCompatible);
        }

        // Add class constants fields and methods.                       RTC111522
        JITUtils.addClassConstantMembers(cw, classConstantFieldNames);

        // Mark the end of the generated wrapper class
        cw.visitEnd();

        // Dump the class bytes out to a byte array.
        byte[] classBytes = cw.toByteArray();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            writeToClassFile(internalStubClassName, classBytes);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "generateClassBytes: " + classBytes.length + " bytes");

        return classBytes;
    }

    /**
     * Defines the static and instance variables that are the same
     * for all Stub classes. <p>
     *
     * <ul>
     * <li> private static final String _type_ids[];
     * </ul>
     *
     * The fields are NOT initialized. <p>
     *
     * @param cw ASM ClassWriter to add the fields to.
     **/
    private static void addFields(ClassWriter cw) {
        FieldVisitor fv;

        // -----------------------------------------------------------------------
        // private static final String _type_ids[];
        // -----------------------------------------------------------------------
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding field : _type_ids [Ljava/lang/String;");

        fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, "_type_ids",
                           "[Ljava/lang/String;", null, null);
        fv.visitEnd();
    }

    /**
     * Initializes the static variables common to all Stub classes. <p>
     *
     * _type_ids = { "RMI:<remote interface name>:0000000000000000",
     * "RMI:<remote interface parent>:0000000000000000",
     * etc... };
     *
     * Note: like a Stub generated with RMIC from the remote
     * interface, the _type_ids field for JIT Deploy Subs includes
     * the remote interface and all parents, excluding Remote. <p>
     *
     * @param cw ASM ClassWriter to add the fields to.
     * @param stubClassName fully qualified name of the Stub class
     *            with '/' as the separator character
     *            (i.e. internal name).
     * @param remoteInterface remote business or component interface
     *            represented by this Stub.
     **/
    private static void initializeStaticFields(ClassWriter cw,
                                               String stubClassName,
                                               Class<?> remoteInterface) {
        GeneratorAdapter mg;

        // -----------------------------------------------------------------------
        // Static fields are initialized  in a class constructor method
        // -----------------------------------------------------------------------
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : <clinit> ()V");

        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method("<clinit>", VOID_TYPE, new Type[0]);

        mg = new GeneratorAdapter(ACC_STATIC, m, null, null, cw);
        mg.visitCode();

        // -----------------------------------------------------------------------
        // Initialize Static Fields
        //
        // _type_ids = { "RMI:<remote interface name>:0000000000000000",
        //               "RMI:<remote interface parent>:0000000000000000",
        //               etc... };
        // -----------------------------------------------------------------------
        String[] remoteTypes = getRemoteTypeIds(remoteInterface);
        mg.push(remoteTypes.length);
        mg.visitTypeInsn(ANEWARRAY, "java/lang/String");

        for (int i = 0; i < remoteTypes.length; ++i) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, INDENT + "   _type_ids = " + remoteTypes[i]);
            mg.visitInsn(DUP);
            mg.push(i);
            mg.visitLdcInsn(remoteTypes[i]);
            mg.visitInsn(AASTORE);
        }

        mg.visitFieldInsn(PUTSTATIC, stubClassName, "_type_ids", "[Ljava/lang/String;");

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.visitInsn(RETURN);
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

    /**
     * Adds the default (no arg) constructor. <p>
     *
     * There are no exceptions in the throws clause; none are required
     * for the constructors of Stubs. <p>
     *
     * Currently, the generated method body is intentionally empty;
     * Stubs require no initialization in the constructor. <p>
     *
     * @param cw ASM ClassWriter to add the constructor to.
     * @param stubClassName the fully qualified class name of the
     *            Stub class.
     * @param classConstantFieldNames The field names of used class constants.
     * @param remoteInterface Interface implemented by the generated Stub;
     *            not required to implement java.rmi.Remote.
     * @param isAbstractInterface true if the interface is an RMI/IDL abstract
     *            interface.
     **/
    private static void addCtor(ClassWriter cw,
                                String stubClassName,
                                Set<String> classConstantFieldNames,
                                Class<?> remoteInterface,
                                boolean isAbstractInterface) {
        MethodVisitor mv;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : <init> ()V");

        // -----------------------------------------------------------------------
        // public <Class Name>_Stub()
        // {
        // }
        // -----------------------------------------------------------------------
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        if (isAbstractInterface) {
            mv.visitMethodInsn(INVOKESPECIAL, "javax/rmi/CORBA/Stub", "<init>", "()V");
        } else {
            // Non-abstract interfaces extend SerializableStub.          // PM46698
            JITUtils.loadClassConstant(mv, stubClassName, classConstantFieldNames, remoteInterface); // RTC111522
            mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ejs/container/SerializableStub",
                               "<init>", "(Ljava/lang/Class;)V");
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(3, 1);
        mv.visitEnd();
    }

    /**
     * Adds all methods common to all Stub classes. <p>
     *
     * These methods are all defined by the Tie interface, javax.rmi.CORBA.Tie.
     * Methods inherited by Tie (like _invoke from InvokeHandler) are
     * NOT implemented here, but added elsewhere if required. <p>
     *
     * @param cw ASM ClassWriter to add the methods to.
     * @param stubClassName fully qualified name of the Stub class
     *            with '/' as the separator character
     *            (i.e. internal name).
     **/
    private static void addCommonStubMethods(ClassWriter cw,
                                             String stubClassName) {
        MethodVisitor mv;

        // -----------------------------------------------------------------------
        // public String[] _ids()
        // {
        //    return (String[]) _type_ids.clone();
        // }
        // -----------------------------------------------------------------------
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : _ids ()[Ljava/lang/String;");

        mv = cw.visitMethod(ACC_PUBLIC, "_ids", "()[Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, stubClassName, "_type_ids", "[Ljava/lang/String;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "clone", "()Ljava/lang/Object;"); // RTC67550
        mv.visitTypeInsn(CHECKCAST, "[Ljava/lang/String;"); // RTC67550
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    /**
     * Adds a standard Stub method corresponding to a method on the
     * remote interface. <p>
     *
     * The added method will handle both normal 'remote' ORB flow, as well as
     * the 'local' optimization; just like a stub generated from RMIC. <p>
     *
     * @param cw ASM Class writer to add the method to.
     * @param className fully qualified name of the Stub class
     *            with '/' as the separator character
     *            (i.e. internal name).
     * @param classConstantFieldNames The field names of used class constants.
     * @param method reflection method from the interface defining
     *            method to be added to the stub.
     * @param checkedExceptions the list of checked exceptions for the method
     * @param idlName OMG IDL name of the specified method.
     * @param isRmiRemote true if interface extends java.rmi.Remote.
     **/
    private static void addStubMethod(ClassWriter cw,
                                      String className,
                                      Set<String> classConstantFieldNames,
                                      Method method,
                                      Class<?>[] checkedExceptions, // d676434
                                      String idlName,
                                      boolean isRmiRemote,
                                      int rmicCompatible) throws EJBConfigurationException {
        GeneratorAdapter mg;
        String methodName = method.getName();
        String methodSignature = jdiMethodSignature(method); // d457086
        Class<?> declaringClass = method.getDeclaringClass();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : " +
                         methodName + " " + methodSignature);

        // Determine the list of 'checked' exceptions, which will need to
        // be thrown in the Stub... and also perform validation.
        // Note that RemoteException is never a 'checked' exception, and
        // exceptions that are subclasses of other 'checked' exceptios
        // will be sorted to parent-last order, to avoid 'unreachable' code.
        Class<?>[] methodExceptions = method.getExceptionTypes();

        // If the interface extends Remote, the property is enabled to allow
        // remote exceptions, or any of the thrown exceptions are remote exceptions,
        // then allow remote exceptions to be thrown from the stub methods.
        boolean throwsRemoteEx = isRmiRemote || JITDeploy.ThrowRemoteFromEjb3Stub;
        if (!throwsRemoteEx) {
            for (Class<?> exClass : methodExceptions) {
                if (RemoteException.class.isAssignableFrom(exClass)) {
                    throwsRemoteEx = true;
                    break;
                }
            }
        }

        // Convert the return value, arguments, and exception classes to
        // ASM Type objects, and create the ASM Method object which will
        // be used to actually add the method and method code.
        Class<?>[] methodParameters = method.getParameterTypes();
        Class<?> returnClass = method.getReturnType();
        Type returnType = Type.getType(returnClass);
        Type[] argTypes = getTypes(methodParameters);
        Type[] exceptionTypes = getTypes(methodExceptions);
        Type[] checkedTypes = getTypes(checkedExceptions);
        Type declaringType = Type.getType(declaringClass);
        String declaringName = convertClassName(declaringClass.getName());
        final int numArgs = methodParameters.length;

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
        // // If EJB 3 non-RMI method; add try/catch to map RemoteException
        // try
        // {
        // -----------------------------------------------------------------------
        Label map_remote_try_begin = new Label();
        if (!throwsRemoteEx) {
            mg.visitLabel(map_remote_try_begin);
        }

        // -----------------------------------------------------------------------
        // ServantObject servantObj;
        // -----------------------------------------------------------------------
        int servantObj = mg.newLocal(TYPE_CORBA_ServantObject);

        // -----------------------------------------------------------------------
        // do
        // {
        // -----------------------------------------------------------------------
        Label do_while_begin = new Label();
        mg.visitLabel(do_while_begin);

        // -----------------------------------------------------------------------
        //   while( !Util.isLocal(this) )
        //   {
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "isLocal",
                           "(Ljavax/rmi/CORBA/Stub;)Z");
        Label while_not_local_end = new Label();
        mg.visitJumpInsn(IFNE, while_not_local_end);

        // -----------------------------------------------------------------------
        //     org.omg.CORBA_2_3.portable.InputStream inputstream = null;
        // -----------------------------------------------------------------------
        Type inputStreamType = TYPE_CORBA_2_3_InputStream;
        int inputstream = mg.newLocal(inputStreamType);
        mg.visitInsn(ACONST_NULL);
        mg.storeLocal(inputstream);

        // -----------------------------------------------------------------------
        //     try
        //     {
        //       try
        //       {
        // -----------------------------------------------------------------------
        // only one label needed for both try begins
        Label not_local_try_begin = new Label();
        mg.visitLabel(not_local_try_begin);

        // -----------------------------------------------------------------------
        //         OutputStream outputstream = (OutputStream)_request("<idl name>", true);
        // -----------------------------------------------------------------------
        Type outputStreamType = TYPE_CORBA_2_3_OutputStream;
        int outputstream = mg.newLocal(outputStreamType);
        mg.loadThis();
        mg.push(idlName);
        mg.push(true);
        mg.visitMethodInsn(INVOKEVIRTUAL,
                           "org/omg/CORBA/portable/ObjectImpl",
                           "_request",
                           "(Ljava/lang/String;Z)Lorg/omg/CORBA/portable/OutputStream;");
        mg.checkCast(outputStreamType);
        mg.storeLocal(outputstream);

        // -----------------------------------------------------------------------
        //         outputstream.write_<primitive>( arg );
        //              or
        //         outputstream.write_value( arg, <class> );
        //              or
        //         Util.writeAny(outputstream, arg);
        //              or
        //         Util.writeRemoteObject(outputstream, arg);
        // -----------------------------------------------------------------------
        for (int i = 0; i < numArgs; i++) {
            JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_ARG_BEGIN + i); // d726162
            mg.loadLocal(outputstream);
            mg.loadArg(i);
            write_value(mg, className, classConstantFieldNames, // RTC111522
                        method, methodParameters[i], rmicCompatible); // d450525, PM46698
        }

        JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_DEFAULT); // d726162

        // -----------------------------------------------------------------------
        //         _invoke(outputstream);                     // void return value
        //              or
        //         inputstream = (InputStream)_invoke(outputstream)
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(outputstream);
        mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/ObjectImpl", "_invoke",
                           "(Lorg/omg/CORBA/portable/OutputStream;)Lorg/omg/CORBA/portable/InputStream;");
        if (returnType == Type.VOID_TYPE) {
            mg.pop();
        } else {
            mg.checkCast(inputStreamType);
            mg.storeLocal(inputstream);
        }

        // -----------------------------------------------------------------------
        //         <return type> returnValue = inputstream.read_<primitive>();
        //              or
        //         <return type> returnValue = (<return type>)
        //                            inputstream.read_value(<return type>.class);
        //              or
        //         <return type> returnValue = (<return type>)
        //                           inputstream.read_Object(<return type>.class);
        //              or
        //         <return type> returnValue = (<return type>)
        //               inputstream.read_abstract_interface(<return type>.class);
        // -----------------------------------------------------------------------
        JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_RETURN); // d726162
        int returnValue = -1;

        if (returnType != Type.VOID_TYPE) {
            returnValue = mg.newLocal(returnType);
            mg.loadLocal(inputstream);
            read_value(mg, className, classConstantFieldNames, // RTC111522
                       method, returnClass, returnType, rmicCompatible); // d450525, PM46698
            mg.storeLocal(returnValue);
        }

        // -----------------------------------------------------------------------
        //         return - executing the finally first!
        // -----------------------------------------------------------------------
        Label not_local_finally_begin = new Label();
        mg.visitJumpInsn(JSR, not_local_finally_begin); // execute finally

        if (returnType != Type.VOID_TYPE) {
            mg.loadLocal(returnValue);
        }
        mg.returnValue();

        // -----------------------------------------------------------------------
        //       }
        // -----------------------------------------------------------------------
        Label not_local_inner_try_end = new Label();
        mg.visitLabel(not_local_inner_try_end);

        // -----------------------------------------------------------------------
        //       catch(ApplicationException appException)
        //       {
        // -----------------------------------------------------------------------
        JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_CATCH_BEGIN); // d726162
        Label catch_appException_label = not_local_inner_try_end;
        int appException = mg.newLocal(TYPE_CORBA_ApplicationException);
        mg.storeLocal(appException);

        // -----------------------------------------------------------------------
        //         inputstream = (org.omg.CORBA_2_3.portable.InputStream)
        //                                          appException.getInputStream();
        // -----------------------------------------------------------------------
        mg.loadLocal(appException);
        mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/ApplicationException",
                           "getInputStream", "()Lorg/omg/CORBA/portable/InputStream;");
        if (inputStreamType != TYPE_CORBA_InputStream)
            mg.checkCast(inputStreamType);
        mg.storeLocal(inputstream);

        // -----------------------------------------------------------------------
        //         String exIdlName = inputstream.read_string();
        // -----------------------------------------------------------------------
        int exIdlName = mg.newLocal(TYPE_String);
        mg.loadLocal(inputstream);
        mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/InputStream",
                           "read_string", "()Ljava/lang/String;");
        mg.storeLocal(exIdlName);

        // -----------------------------------------------------------------------
        //         // For every checked exception (not a RemoteException
        //         if ( exIdlName.equals( <exception IDL> ) )
        //         {
        //           throw (<exception type>)in.read_value(<exception type>.class);
        //         }
        // -----------------------------------------------------------------------

        // The Java-to-IDL specification says that exception class names need to
        // be mangled.  Historically, we did not do this, so by default, we accept
        // both mangled and unmangled, but we look for unmangled first.    PM94096
        boolean rmicCompatibleExceptions = JITDeploy.isRMICCompatibleExceptions(rmicCompatible);
        Set<String> exceptionNamesIDLUsed = rmicCompatibleExceptions ? null : new HashSet<String>();
        for (int pass = rmicCompatibleExceptions ? 1 : 0; pass < 2; pass++) {
            boolean mangleComponents = pass == 1;

            for (int i = 0; i < checkedExceptions.length; ++i) {
                String exceptionName = checkedExceptions[i].getName();
                String exceptionNameIDL = getIdlExceptionName(exceptionName, mangleComponents); // PM94096

                if (exceptionNamesIDLUsed == null || exceptionNamesIDLUsed.add(exceptionNameIDL)) {
                    JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_CATCH_BEGIN + 1 + i); // d726162
                    mg.loadLocal(exIdlName);
                    mg.visitLdcInsn(exceptionNameIDL);
                    mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals",
                                       "(Ljava/lang/Object;)Z");
                    Label if_equals_else = new Label();
                    mg.visitJumpInsn(IFEQ, if_equals_else);

                    mg.loadLocal(inputstream);
                    JITUtils.loadClassConstant(mg, className, classConstantFieldNames, checkedExceptions[i]); // d676434, RTC111522
                    mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA_2_3/portable/InputStream",
                                       "read_value", "(Ljava/lang/Class;)Ljava/io/Serializable;");
                    JITUtils.checkCast(mg, checkedTypes[i].getInternalName()); // d726162
                    mg.visitInsn(ATHROW);
                    mg.visitLabel(if_equals_else);
                }
            }
        }

        JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_DEFAULT); // d726162

        // -----------------------------------------------------------------------
        //         throw new UnexpectedException(exIdlName);
        // -----------------------------------------------------------------------
        mg.visitTypeInsn(NEW, "java/rmi/UnexpectedException");
        mg.visitInsn(DUP);
        mg.loadLocal(exIdlName);
        mg.visitMethodInsn(INVOKESPECIAL, "java/rmi/UnexpectedException", "<init>",
                           "(Ljava/lang/String;)V");
        mg.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        //       }
        //       catch ( RemarshalException rex )
        //       {
        //       }
        // -----------------------------------------------------------------------
        Label catch_remarshalException_label = new Label();
        mg.visitLabel(catch_remarshalException_label);
        mg.visitInsn(POP);
        mg.visitJumpInsn(JSR, not_local_finally_begin); // execute finally
        mg.visitJumpInsn(GOTO, do_while_begin); // reiterate loop

        // -----------------------------------------------------------------------
        //     }
        // -----------------------------------------------------------------------
        Label not_local_outer_try_end = new Label();
        mg.visitLabel(not_local_outer_try_end);

        // -----------------------------------------------------------------------
        //     catch(SystemException systemexception)
        //     {
        //       throw Util.mapSystemException(systemexception);
        //     }
        // -----------------------------------------------------------------------
        Label catch_sysException_label = not_local_outer_try_end;
        int sysException = mg.newLocal(TYPE_CORBA_SystemException);
        mg.storeLocal(sysException);
        mg.loadLocal(sysException);
        mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "mapSystemException",
                           "(Lorg/omg/CORBA/SystemException;)Ljava/rmi/RemoteException;");
        mg.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        //     }
        // -----------------------------------------------------------------------
        Label not_local_outer_try_catch_end = new Label();
        mg.visitLabel(not_local_outer_try_catch_end);

        // -----------------------------------------------------------------------
        //     finally
        //     {
        // -----------------------------------------------------------------------
        // finally label - after caught exception
        Label not_local_finally_ex = not_local_outer_try_catch_end;

        int not_local_finally_th = mg.newLocal(TYPE_Throwable);
        mg.storeLocal(not_local_finally_th);
        mg.visitJumpInsn(JSR, not_local_finally_begin); // finally - jump/return to actual code
        mg.loadLocal(not_local_finally_th);
        mg.visitInsn(ATHROW); // finally - re-throw exception

        // finally label - after normal code flow
        mg.visitLabel(not_local_finally_begin);

        int not_local_finally_return = mg.newLocal(TYPE_Object);
        mg.storeLocal(not_local_finally_return);

        // -----------------------------------------------------------------------
        //       _releaseReply( inputstream );
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(inputstream);
        mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/ObjectImpl",
                           "_releaseReply", "(Lorg/omg/CORBA/portable/InputStream;)V");

        // -----------------------------------------------------------------------
        //     }                   // end of try / catch / finally
        // -----------------------------------------------------------------------
        // Return from finally, either to the return statement above for normal
        // code flow... or to the beginning of the finally above for the exception
        // path, to rethrow the exception.
        mg.ret(not_local_finally_return);

        // -----------------------------------------------------------------------
        //   }                     // end while( !Util.isLocal(this) )
        // -----------------------------------------------------------------------
        mg.visitLabel(while_not_local_end);

        // -----------------------------------------------------------------------
        //   servantObj = _servant_preinvoke(<idl name>, <method interface>.class);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.push(idlName);

        JITUtils.loadClassConstant(mg, className, classConstantFieldNames, declaringClass); // d676434, RTC111522

        mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/ObjectImpl",
                           "_servant_preinvoke",
                           "(Ljava/lang/String;Ljava/lang/Class;)Lorg/omg/CORBA/portable/ServantObject;");
        mg.storeLocal(servantObj);

        // -----------------------------------------------------------------------
        // }
        // while(servantobject == null);
        // -----------------------------------------------------------------------
        mg.loadLocal(servantObj);
        mg.visitJumpInsn(IFNULL, do_while_begin);

        // -----------------------------------------------------------------------
        // try
        // {
        // -----------------------------------------------------------------------
        Label local_try_begin = new Label();
        mg.visitLabel(local_try_begin);

        // -----------------------------------------------------------------------
        //   At this point in the Stub, all of the 'mutable' parameters must be
        //   copied.  Prior to adding that copy code, build an array that
        //   corresponds to all of the parameters, which contains either a local
        //   variable index to hold that copy (for mutable parameters), or a -1,
        //   indicating there is no copy (for immutable parameters).
        // -----------------------------------------------------------------------
        int numMutableArgs = 0;
        int firstMutableArg = -1;
        int[] argCopy = null;

        if (numArgs > 0) {
            argCopy = new int[numArgs];

            for (int i = 0; i < numArgs; ++i) {
                if (isMutable(methodParameters[i])) {
                    argCopy[i] = mg.newLocal(argTypes[i]);
                    ++numMutableArgs;
                    if (firstMutableArg == -1) {
                        firstMutableArg = i;
                    }
                } else {
                    argCopy[i] = -1;
                }
            }
        }

        // -----------------------------------------------------------------------
        //   // For a single mutable parameter, just copy that one object.
        //
        //   <arg type> argCopy = Util.copyObject( arg, _orb() );
        // -----------------------------------------------------------------------
        if (numMutableArgs == 1) {
            JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_ARG_BEGIN); // d726162
            mg.loadArg(firstMutableArg);
            mg.loadThis();
            mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/ObjectImpl",
                               "_orb", "()Lorg/omg/CORBA/ORB;");
            mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "copyObject",
                               "(Ljava/lang/Object;Lorg/omg/CORBA/ORB;)Ljava/lang/Object;");
            JITUtils.checkCast(mg, argTypes[firstMutableArg].getInternalName()); // d726162
            mg.storeLocal(argCopy[firstMutableArg]);
        }

        // -----------------------------------------------------------------------
        //   // For multiple mutable parameters, copy them all at once by passing
        //   // them all in an Object array.
        //
        //   Object argCopyarray[] = Util.copyObjects(new Object[] { args... }, _orb());
        // -----------------------------------------------------------------------
        else if (numMutableArgs > 1) {
            JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_ARG_BEGIN); // d726162
            mg.push(numMutableArgs);
            mg.visitTypeInsn(ANEWARRAY, "java/lang/Object");

            int argCopyArrayIndex = 0;
            for (int i = firstMutableArg; i < numArgs; ++i) {
                if (argCopy[i] > -1) {
                    mg.visitInsn(DUP);
                    mg.push(argCopyArrayIndex++);
                    mg.loadArg(i);
                    mg.visitInsn(AASTORE);
                }
            }

            mg.loadThis();
            mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/ObjectImpl",
                               "_orb", "()Lorg/omg/CORBA/ORB;");
            mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "copyObjects",
                               "([Ljava/lang/Object;Lorg/omg/CORBA/ORB;)[Ljava/lang/Object;");
            int argCopyArray = mg.newLocal(TYPE_Object_ARRAY);
            mg.storeLocal(argCopyArray);

            // -----------------------------------------------------------------------
            //   // And then pull the copy out of the array into a local variable
            //
            //   <arg type> argCopy<i> = (<arg type>)argCopyArray[<i>]; // for each arg
            // -----------------------------------------------------------------------
            argCopyArrayIndex = 0;
            for (int i = firstMutableArg; i < numArgs; ++i) {
                if (argCopy[i] > -1) {
                    JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_ARG_BEGIN + i); // d726162
                    mg.loadLocal(argCopyArray);
                    mg.push(argCopyArrayIndex++);
                    mg.visitInsn(AALOAD);
                    JITUtils.checkCast(mg, argTypes[i].getInternalName()); // d726162
                    mg.storeLocal(argCopy[i]);
                }
            }
        }

        JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_DEFAULT); // d726162

        // -----------------------------------------------------------------------
        //   Note that the parameters passed may be copies (if mutable) or
        //   may be the actual parameters (if immutable).
        //
        //   ((<interface>)servantObj.servant).<method>(argCopy<i>.....);
        //              or
        //   <return type> returnValue = ((<interface>)servantObj.servant).<method>(argCopy<i>.....);
        //   returnValue = (<return type>)Util.copyObject(returnValue, _orb());
        // -----------------------------------------------------------------------
        mg.loadLocal(servantObj);
        mg.visitFieldInsn(GETFIELD, "org/omg/CORBA/portable/ServantObject",
                          "servant", "Ljava/lang/Object;");
        JITUtils.checkCast(mg, declaringType.getInternalName()); // d726162

        for (int i = 0; i < numArgs; ++i) {
            if (argCopy[i] > -1)
                mg.loadLocal(argCopy[i]); // mutable - the copy
            else
                mg.loadArg(i); // immutable - real arg
        }
        mg.visitMethodInsn(INVOKEINTERFACE, declaringName,
                           methodName, m.getDescriptor());

        if (returnType != Type.VOID_TYPE) {
            JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_RETURN); // d726162
            returnValue = mg.newLocal(returnType);
            mg.storeLocal(returnValue);

            if (isMutable(returnClass)) {
                mg.loadLocal(returnValue);
                mg.loadThis();
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/ObjectImpl",
                                   "_orb", "()Lorg/omg/CORBA/ORB;");
                mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "copyObject",
                                   "(Ljava/lang/Object;Lorg/omg/CORBA/ORB;)Ljava/lang/Object;");
                JITUtils.checkCast(mg, returnType.getInternalName()); // d726162
                mg.storeLocal(returnValue);
            }
        }

        // -----------------------------------------------------------------------
        //   return - executing the finally first!
        // -----------------------------------------------------------------------
        Label local_finally_begin = new Label();
        mg.visitJumpInsn(JSR, local_finally_begin); // execute finally

        if (returnType != Type.VOID_TYPE) {
            mg.loadLocal(returnValue);
        }
        mg.returnValue();

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        Label local_try_end = new Label();
        mg.visitLabel(local_try_end);

        // -----------------------------------------------------------------------
        // catch(Throwable ex)
        // {
        // -----------------------------------------------------------------------
        JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_CATCH_BEGIN); // d726162
        Label catch_throwable_label = local_try_end;
        int ex = mg.newLocal(TYPE_Throwable);
        mg.storeLocal(ex);

        // -----------------------------------------------------------------------
        //   Object exCopy = Util.copyObject(ex, _orb());
        // -----------------------------------------------------------------------
        int exCopy = mg.newLocal(TYPE_Object);
        mg.loadLocal(ex);
        mg.loadThis();
        mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/ObjectImpl",
                           "_orb", "()Lorg/omg/CORBA/ORB;");
        mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "copyObject",
                           "(Ljava/lang/Object;Lorg/omg/CORBA/ORB;)Ljava/lang/Object;");
        mg.storeLocal(exCopy);

        // -----------------------------------------------------------------------
        //   if ( exCopy instanceof <checked exception> )
        //     throw (<checed exception>)exCopy;
        //   else
        // -----------------------------------------------------------------------
        Label[] checked_else = new Label[checkedExceptions.length];
        for (int i = 0; i < checkedExceptions.length; ++i) {
            JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_CATCH_BEGIN + 1 + i); // d726162
            String checkedName = convertClassName(checkedExceptions[i].getName());
            mg.loadLocal(exCopy);
            mg.visitTypeInsn(INSTANCEOF, checkedName);
            checked_else[i] = new Label();
            mg.visitJumpInsn(IFEQ, checked_else[i]);

            mg.loadLocal(exCopy);
            mg.visitTypeInsn(CHECKCAST, checkedName);
            mg.visitInsn(ATHROW);

            mg.visitLabel(checked_else[i]);
        }

        JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_DEFAULT); // d726162

        // -----------------------------------------------------------------------
        //     throw Util.wrapException((Throwable)exCopy);
        // -----------------------------------------------------------------------
        mg.loadLocal(exCopy);
        mg.checkCast(TYPE_Throwable);
        mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "wrapException",
                           "(Ljava/lang/Throwable;)Ljava/rmi/RemoteException;");
        mg.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        // }
        // -----------------------------------------------------------------------
        Label local_try_catch_end = new Label();
        mg.visitLabel(local_try_catch_end);

        // -----------------------------------------------------------------------
        // finally
        // {
        // -----------------------------------------------------------------------
        // finally label - after caught exception
        Label local_finally_ex = local_try_catch_end;

        int local_finally_th = mg.newLocal(TYPE_Throwable);
        mg.storeLocal(local_finally_th);
        mg.visitJumpInsn(JSR, local_finally_begin); // finally - jump/return to actual code
        mg.loadLocal(local_finally_th);
        mg.visitInsn(ATHROW); // finally - re-throw exception

        // finally label - after normal code flow
        mg.visitLabel(local_finally_begin);

        int local_finally_return = mg.newLocal(TYPE_Object);
        mg.storeLocal(local_finally_return);

        // -----------------------------------------------------------------------
        //   _servant_postinvoke(servantObj);
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.loadLocal(servantObj);
        mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/ObjectImpl",
                           "_servant_postinvoke", "(Lorg/omg/CORBA/portable/ServantObject;)V");

        // -----------------------------------------------------------------------
        // }                   // end of try / catch / finally
        // -----------------------------------------------------------------------
        // Return from finally, either to the return statement above for normal
        // code flow... or to the beginning of the finally above for the exception
        // path, to rethrow the exception.
        mg.ret(local_finally_return);

        // -----------------------------------------------------------------------
        // // If EJB 3 non-RMI method; complete try/catch to map RemoteException
        // -----------------------------------------------------------------------
        Label map_remote_try_end = null;
        Label catch_tran_rollback_label = null;
        Label catch_remote_label = null;
        if (!throwsRemoteEx) {
            // -----------------------------------------------------------------------
            // }
            // -----------------------------------------------------------------------
            map_remote_try_end = new Label();
            mg.visitLabel(map_remote_try_end);

            // -----------------------------------------------------------------------
            // catch(TransactionRolledbackException trbex)
            // {
            //   throw new EJBTransactionRolledbackException(trbex);
            // }
            // -----------------------------------------------------------------------
            JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_CATCH_BEGIN + 100);
            catch_tran_rollback_label = map_remote_try_end;
            int trbex = mg.newLocal(JIT_Stub.TYPE_TransactionRolledbackException);
            mg.storeLocal(trbex);

            mg.visitTypeInsn(NEW, "javax/ejb/EJBTransactionRolledbackException");
            mg.visitInsn(DUP);
            mg.visitInsn(ACONST_NULL);
            mg.loadLocal(trbex);
            mg.visitMethodInsn(INVOKESPECIAL, "javax/ejb/EJBTransactionRolledbackException", "<init>", "(Ljava/lang/String;Ljava/lang/Exception;)V");
            mg.visitInsn(ATHROW);

            // -----------------------------------------------------------------------
            // catch(RemoteException rex)
            // {
            //   throw new EJBException(rex);
            // }
            // -----------------------------------------------------------------------
            JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_CATCH_BEGIN + 101);
            catch_remote_label = new Label();
            mg.visitLabel(catch_remote_label);
            int rex = mg.newLocal(JIT_Stub.TYPE_RemoteException);
            mg.storeLocal(rex);

            mg.visitTypeInsn(NEW, "javax/ejb/EJBException");
            mg.visitInsn(DUP);
            mg.loadLocal(rex);
            mg.visitMethodInsn(INVOKESPECIAL, "javax/ejb/EJBException", "<init>", "(Ljava/lang/Exception;)V");
            mg.visitInsn(ATHROW);
        }

        // -----------------------------------------------------------------------
        // All Try-Catch-Finally definitions for above
        // -----------------------------------------------------------------------

        mg.visitTryCatchBlock(not_local_try_begin,
                              not_local_inner_try_end,
                              catch_appException_label,
                              "org/omg/CORBA/portable/ApplicationException");
        mg.visitTryCatchBlock(not_local_try_begin,
                              not_local_inner_try_end,
                              catch_remarshalException_label,
                              "org/omg/CORBA/portable/RemarshalException");
        mg.visitTryCatchBlock(not_local_try_begin,
                              not_local_outer_try_end,
                              catch_sysException_label,
                              "org/omg/CORBA/SystemException");
        mg.visitTryCatchBlock(not_local_try_begin,
                              not_local_outer_try_catch_end,
                              not_local_finally_ex,
                              null);

        mg.visitTryCatchBlock(local_try_begin,
                              local_try_end,
                              catch_throwable_label,
                              "java/lang/Throwable");
        mg.visitTryCatchBlock(local_try_begin,
                              local_try_catch_end,
                              local_finally_ex,
                              null);

        if (!throwsRemoteEx) {
            mg.visitTryCatchBlock(map_remote_try_begin,
                                  map_remote_try_end,
                                  catch_tran_rollback_label,
                                  "javax/transaction/TransactionRolledbackException");
            mg.visitTryCatchBlock(map_remote_try_begin,
                                  map_remote_try_end,
                                  catch_remote_label,
                                  "java/rmi/RemoteException");
        }

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

}
