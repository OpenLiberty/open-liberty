/*******************************************************************************
 * Copyright (c) 2007, 2014 IBM Corporation and others.
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
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.TYPE_CORBA_InputStream;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.TYPE_CORBA_OutputStream;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.TYPE_CORBA_ResponseHandler;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.TYPE_CORBA_SystemException;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.getRemoteTypeIds;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.read_value;
import static com.ibm.ws.ejbcontainer.jitdeploy.CORBA_Utils.write_value;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.INDENT;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_String;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_Throwable;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.convertClassName;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.getTypes;
import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.writeToClassFile;
import static com.ibm.ws.ejbcontainer.jitdeploy.RMItoIDL.getIdlExceptionName;
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
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.V1_2;
import static org.objectweb.asm.Type.VOID_TYPE;

import java.lang.reflect.Method;
import java.rmi.Remote;
import java.util.Arrays;
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

/**
 * Provides Just In Time deployment of Ties for EJB Wrapper classes. <p>
 */
public final class JIT_Tie
{
    private static final TraceComponent tc = Tr.register(JIT_Tie.class, JITUtils.JIT_TRACE_GROUP, JITUtils.JIT_RSRC_BUNDLE);

    /**
     * Utility method that returns the name of the Tie class that needs to
     * be generated for the specified remote object class. Intended for
     * use by JITDeploy only (should not be called directly). <p>
     *
     * Basically, the name of the Tie class for any remote object is
     * the name of the remote object class, with an '_' prepended,
     * and '_Tie' appended. <p>
     *
     * This method properly modifies the class name with or without
     * a package qualification. <p>
     *
     * @param remoteClassName the fully qualified class name of the
     *            remote object.
     **/
    static String getTieClassName(String remoteClassName)
    {
        StringBuilder tieBuilder = new StringBuilder(remoteClassName);
        int packageOffset = Math.max(remoteClassName.lastIndexOf('.') + 1,
                                     remoteClassName.lastIndexOf('$') + 1);
        tieBuilder.insert(packageOffset, '_');
        tieBuilder.append("_Tie");

        return tieBuilder.toString();
    }

    /**
     * Core method for generating the EJB Wrapper class bytes. Intended for
     * use by JITDeploy only (should not be called directly). <p>
     *
     * Although the 'methods' parameter could be obtained from the
     * specified remote interface, the 'methods' and 'idlNames'
     * parameters are requested to improve performance... allowing
     * the two arrays to be obtained once and shared by the code
     * that generates the corresponding Stub class. <p>
     *
     * The 'idlNames' must be in the same order, corresponding to the methods.
     * In some cases, where an idl name was generated incorrectly in the past,
     * the fix has been to support both the old and new version of the idl name
     * by adding the extra idl name to the end of the 'idlNames' parameter.
     * When this is done, the corresponding method must also be added to the
     * end of the 'remoteMethods' parameter as well. The generated Tie will
     * support BOTH names, to be compatible with RMIC and older versions of
     * JITDeploy. <p>
     *
     * @param tieClassName name of the tie class to be generated.
     * @param servantClassName name of the remote implementation class that the
     *            generated tie class will invoke methods on.
     * @param remoteInterface Remote interface implemented by the generated tie
     *            and the servant class.
     * @param remoteMethods Methods from the specified remote interface;
     *            passed for improved performance.
     * @param idlNames OMG IDL names corresponding to the
     *            remoteMethods parameter. May include additional
     *            names if the class name conflicts with a method.
     * @param rmicCompatible rmic compatibility flags
     * @param portableServer true if the tie class should extend
     *            org.omg.PortableServer.Servant
     **/
    static byte[] generateClassBytes(String tieClassName,
                                     String servantClassName,
                                     Class<?> remoteInterface,
                                     Method[] remoteMethods,
                                     String[] idlNames,
                                     int rmicCompatible,
                                     boolean portableServer)
                    throws EJBConfigurationException
    {
        int numMethods = remoteMethods.length;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d576626

        // ASM uses 'internal' java class names (like JNI) where '/' is
        // used instead of '.', so convert the parameters to 'internal' format.
        String internalTieClassName = convertClassName(tieClassName);
        String internalServantClassName = convertClassName(servantClassName);
        String internalInterfaceName = convertClassName(remoteInterface.getName());

        String servantDescriptor = "L" + internalServantClassName + ";";

        // Remote Business interfaces may or may not extend java.rmi.Remote
        boolean isRmiRemote = (Remote.class).isAssignableFrom(remoteInterface);

        if (isTraceOn)
        {
            if (tc.isEntryEnabled())
                Tr.entry(tc, "generateClassBytes");
            if (tc.isDebugEnabled())
            {
                Tr.debug(tc, INDENT + "className = " + internalTieClassName);
                Tr.debug(tc, INDENT + "interface = " + internalInterfaceName);
                if (isRmiRemote)
                    Tr.debug(tc, INDENT + "implements java.rmi.Remote");
            }
        }

        Set<String> classConstantFieldNames = new LinkedHashSet<String>(); // RTC111522

        // Create the ASM Class Writer to write out a Tie
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS); //F743-11995

        String parentClassName = portableServer ? "org/omg/PortableServer/Servant" : "org/omg/CORBA_2_3/portable/ObjectImpl";

        // Define the Tie Class object
        cw.visit(V1_2, ACC_PUBLIC + ACC_SUPER,
                 internalTieClassName,
                 null,
                 parentClassName,
                 new String[] { "javax/rmi/CORBA/Tie" });

        // Define the source code file and debug settings
        String sourceFileName = tieClassName.substring(tieClassName.lastIndexOf(".") + 1) + ".java";
        cw.visitSource(sourceFileName, null);

        // Add the static and instance variables common to all Ties.
        addFields(cw, servantDescriptor);

        // Initialize the static fields common to all Ties.
        initializeStaticFields(cw, internalTieClassName, remoteInterface);

        // Add the public no parameter Tie constructor
        addCtor(cw, internalTieClassName, parentClassName, servantDescriptor);

        // Add the methods common to all Tie classes.
        addCommonTieMethods(cw, internalTieClassName, internalServantClassName,
                            servantDescriptor, portableServer);

        // Add the _invoke method which will route the incoming request to the
        // correct private method that understands the arguments/return value.
        add_invokeMethod(cw, internalTieClassName, idlNames);

        // Now add all of the private 'delegate' methods that correspond to all
        // of the methods of the remote interface (obtained via Reflections).
        for (int i = 0; i < numMethods; ++i)
        {
            addDelegateMethod(cw, internalTieClassName, classConstantFieldNames, // RTC111522
                              internalServantClassName,
                              servantDescriptor, remoteMethods[i], idlNames[i],
                              isRmiRemote, rmicCompatible); // PM46698
        }

        // Add class constants fields and methods.                       RTC111522
        JITUtils.addClassConstantMembers(cw, classConstantFieldNames);

        // Mark the end of the generated wrapper class
        cw.visitEnd();

        // Dump the class bytes out to a byte array.
        byte[] classBytes = cw.toByteArray();

        if (isTraceOn && tc.isDebugEnabled())
            writeToClassFile(internalTieClassName, classBytes);

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "generateClassBytes: " + classBytes.length + " bytes");

        return classBytes;
    }

    /**
     * Defines the static and instance variables that are the same
     * for all Tie classes. <p>
     *
     * <ul>
     * <li> private <remote class> target;
     * <li> private ORB orb;
     * <li> private static final String _type_ids[];
     * </ul>
     *
     * The fields are NOT initialized. <p>
     *
     * @param cw ASM ClassWriter to add the fields to.
     * @param servantDescriptor fully qualified name of the servant
     *            (wrapper) class with '/' as the separator
     *            character (i.e. internal name), and
     *            wrapped with L; (jni style).
     **/
    private static void addFields(ClassWriter cw, String servantDescriptor)
    {
        FieldVisitor fv;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d576626

        // -----------------------------------------------------------------------
        // private <servant class> target;
        // -----------------------------------------------------------------------
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding field : target " + servantDescriptor);

        fv = cw.visitField(ACC_PRIVATE, "target", servantDescriptor, null, null);
        fv.visitEnd();

        // -----------------------------------------------------------------------
        // private ORB orb;
        // -----------------------------------------------------------------------
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding field : orb Lorg/omg/CORBA/ORB;");

        fv = cw.visitField(ACC_PRIVATE, "orb", "Lorg/omg/CORBA/ORB;", null, null);
        fv.visitEnd();

        // -----------------------------------------------------------------------
        // private static final String _type_ids[];
        // -----------------------------------------------------------------------
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding field : _type_ids [Ljava/lang/String;");

        fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, "_type_ids",
                           "[Ljava/lang/String;", null, null);
        fv.visitEnd();
    }

    /**
     * Initializes the static variables common to all Tie classes. <p>
     *
     * _type_ids = { "RMI:<remote interface name>:0000000000000000",
     * "RMI:<remote interface parent>:0000000000000000",
     * etc... };
     *
     * Note: unlike a Tie generated with RMIC from the generated
     * wrapper, the _type_ids field for JIT Deploy Ties do NOT include
     * CSIServant or TransactionalObject. These are WebSphere
     * implementation details that should NOT be exposed through the
     * Tie/Stub. <p>
     *
     * @param cw ASM ClassWriter to add the fields to.
     * @param tieClassName fully qualified name of the Tie class
     *            with '/' as the separator character
     *            (i.e. internal name).
     * @param remoteInterface remote business or component interface
     *            represented by this Tie.
     **/
    private static void initializeStaticFields(ClassWriter cw,
                                               String tieClassName,
                                               Class<?> remoteInterface)
    {
        GeneratorAdapter mg;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d576626

        // -----------------------------------------------------------------------
        // Static fields are initialized  in a class constructor method
        // -----------------------------------------------------------------------
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : <clinit> ()V");

        org.objectweb.asm.commons.Method m = new org.objectweb.asm.commons.Method
                        ("<clinit>",
                                        VOID_TYPE,
                                        new Type[0]);

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

        for (int i = 0; i < remoteTypes.length; ++i)
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, INDENT + "   _type_ids = " + remoteTypes[i]);
            mg.visitInsn(DUP);
            mg.push(i);
            mg.visitLdcInsn(remoteTypes[i]);
            mg.visitInsn(AASTORE);
        }

        mg.visitFieldInsn(PUTSTATIC, tieClassName, "_type_ids", "[Ljava/lang/String;");

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
     * for the constructors of EJB Wrappers (local or remote). <p>
     *
     * Currently, the generated method body is intentionally empty;
     * EJB Wrappers require no initialization in the constructor. <p>
     *
     * @param cw ASM ClassWriter to add the constructor to.
     * @param tieClassName fully qualified name of the Tie class
     *            with '/' as the separator character
     *            (i.e. internal name).
     * @param servantDescriptor fully qualified name of the servant
     *            (wrapper) class with '/' as the separator
     *            character (i.e. internal name), and
     *            wrapped with L; (jni style).
     **/
    private static void addCtor(ClassWriter cw, String tieClassName, String parentClassName, String servantDescriptor)
    {
        MethodVisitor mv;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : <init> ()V");

        // -----------------------------------------------------------------------
        // public <Class Name>_Tie()
        // {
        //    target = null;
        //    orb = null;
        // }
        // -----------------------------------------------------------------------
        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, parentClassName, "<init>", "()V");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitFieldInsn(PUTFIELD, tieClassName, "target", servantDescriptor);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitFieldInsn(PUTFIELD, tieClassName, "orb", "Lorg/omg/CORBA/ORB;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    /**
     * Adds all methods common to all Tie classes. <p>
     *
     * <ul>
     * <li> public String[] _ids()
     * <li> public void _set_delegate(Delegate delegate)
     * <li> public void deactivate()
     * <li> public Remote getTarget()
     * <li> public ORB orb()
     * <li> public void orb(ORB orb)
     * <li> public void setTarget(Remote remote)
     * <li> public org.omg.CORBA.Object thisObject()
     * </ul>
     *
     * These methods are all defined by the Tie interface, javax.rmi.CORBA.Tie.
     * Methods inherited by Tie (like _invoke from InvokeHandler) are
     * NOT implemented here, but added elsewhere if required. <p>
     *
     * @param cw ASM ClassWriter to add the constructor to.
     * @param tieClassName fully qualified name of the Tie class
     *            with '/' as the separator character
     *            (i.e. internal name).
     * @param servantClassName name of the remote implementation class that the
     *            generated tie class will invoke methods on with '/'
     *            as the separator character (i.e. internal name).
     * @param servantDescriptor fully qualified name of the servant
     *            (wrapper) class with '/' as the separator
     *            character (i.e. internal name), and
     *            wrapped with L; (jni style).
     * @param portableServer true if the tie class should extend
     *            org.omg.PortableServer.Servant
     **/
    private static void addCommonTieMethods(ClassWriter cw,
                                            String tieClassName,
                                            String servantClassName,
                                            String servantDescriptor,
                                            boolean portableServer)
    {
        MethodVisitor mv;
        Label if_notnull_else, if_notnull_end;
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d576626

        // -----------------------------------------------------------------------
        // #if portableServer
        // public String[] _all_interfaces(POA poa, byte[] objectId) {
        // #else
        // public String[] _ids()
        // #endif
        // {
        //    return (String)[]) _type_ids.clone();
        // }
        // -----------------------------------------------------------------------
        if (portableServer) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, INDENT + "adding method : _all_interfaces(Lorg/omg/PortableServer/POA;[B)[Ljava/lang/String;");
            mv = cw.visitMethod(ACC_PUBLIC, "_all_interfaces", "(Lorg/omg/PortableServer/POA;[B)[Ljava/lang/String;", null, null);
        } else {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, INDENT + "adding method : _ids ()[Ljava/lang/String;");
            mv = cw.visitMethod(ACC_PUBLIC, "_ids", "()[Ljava/lang/String;", null, null);
        }
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, tieClassName, "_type_ids", "[Ljava/lang/String;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "clone", "()Ljava/lang/Object;"); // RTC67550
        mv.visitTypeInsn(CHECKCAST, "[Ljava/lang/String;"); // RTC67550
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        if (!portableServer) {
            // -----------------------------------------------------------------------
            // public void _set_delegate(Delegate delegate)
            // {
            //    super._set_delegate(delegate);
            //    if(delegate != null)
            //       orb = _orb();
            //    else
            //       orb = null;
            // }
            // -----------------------------------------------------------------------
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, INDENT + "adding method : _set_delegate (Lorg/omg/CORBA/portable/Delegate;)V");

            mv = cw.visitMethod(ACC_PUBLIC, "_set_delegate",
                                "(Lorg/omg/CORBA/portable/Delegate;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESPECIAL, "org/omg/CORBA/portable/ObjectImpl",
                               "_set_delegate", "(Lorg/omg/CORBA/portable/Delegate;)V");
            mv.visitVarInsn(ALOAD, 1);
            if_notnull_else = new Label();
            mv.visitJumpInsn(IFNULL, if_notnull_else);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/ObjectImpl",
                               "_orb", "()Lorg/omg/CORBA/ORB;");
            mv.visitFieldInsn(PUTFIELD, tieClassName, "orb", "Lorg/omg/CORBA/ORB;");
            if_notnull_end = new Label();
            mv.visitJumpInsn(GOTO, if_notnull_end);
            mv.visitLabel(if_notnull_else);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ACONST_NULL);
            mv.visitFieldInsn(PUTFIELD, tieClassName, "orb", "Lorg/omg/CORBA/ORB;");
            mv.visitLabel(if_notnull_end);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }

        // -----------------------------------------------------------------------
        // public void deactivate()
        // {
        // #if portableServer
        //    try {
        //      _poa().deactivate_object(_poa().servant_to_id(this));
        //    } catch (WrongPolicy e) {
        //    } catch (ObjectNotActive e) {
        //    } catch (ServantNotActive e) {
        //    }
        // #else
        //    if(orb != null)
        //    {
        //       orb.disconnect(this);
        //       _set_delegate(null);
        //    }
        // #endif
        // }
        // -----------------------------------------------------------------------
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : deactivate ()V");

        mv = cw.visitMethod(ACC_PUBLIC, "deactivate", "()V", null, null);
        mv.visitCode();
        if (portableServer) {
            Label try_begin = new Label();
            mv.visitLabel(try_begin);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, tieClassName, "_poa", "()Lorg/omg/PortableServer/POA;");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, tieClassName, "_poa", "()Lorg/omg/PortableServer/POA;");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/omg/PortableServer/POA", "servant_to_id", "(Lorg/omg/PortableServer/Servant;)[B");
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/omg/PortableServer/POA", "deactivate_object", "([B)V");
            mv.visitInsn(RETURN);

            Label try_end = new Label();
            mv.visitLabel(try_end);

            Label catch_WrongPolicy_label = try_end;
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);

            Label catch_ObjectNotActive_label = new Label();
            mv.visitLabel(catch_ObjectNotActive_label);
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);

            Label catch_ServantNotActive_label = new Label();
            mv.visitLabel(catch_ServantNotActive_label);
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);

            mv.visitTryCatchBlock(try_begin, try_end,
                                  catch_WrongPolicy_label,
                                  "org/omg/PortableServer/POAPackage/WrongPolicy");
            mv.visitTryCatchBlock(try_begin, try_end,
                                  catch_ObjectNotActive_label,
                                  "org/omg/PortableServer/POAPackage/ObjectNotActive");
            mv.visitTryCatchBlock(try_begin, try_end,
                                  catch_ServantNotActive_label,
                                  "org/omg/PortableServer/POAPackage/ServantNotActive");
        } else {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, tieClassName, "orb", "Lorg/omg/CORBA/ORB;");
            if_notnull_end = new Label();
            mv.visitJumpInsn(IFNULL, if_notnull_end);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, tieClassName, "orb", "Lorg/omg/CORBA/ORB;");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/ORB", "disconnect",
                               "(Lorg/omg/CORBA/Object;)V");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKEVIRTUAL, tieClassName, "_set_delegate",
                               "(Lorg/omg/CORBA/portable/Delegate;)V");
            mv.visitLabel(if_notnull_end);
            mv.visitInsn(RETURN);
        }
        mv.visitMaxs(2, 1);
        mv.visitEnd();

        // -----------------------------------------------------------------------
        // public Remote getTarget()
        // {
        //    return target;
        // }
        // -----------------------------------------------------------------------
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : getTarget ()Ljava/rmi/Remote;");

        mv = cw.visitMethod(ACC_PUBLIC, "getTarget", "()Ljava/rmi/Remote;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, tieClassName, "target", servantDescriptor);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // -----------------------------------------------------------------------
        // public ORB orb()
        // {
        //    return _orb();
        // }
        // -----------------------------------------------------------------------
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : orb ()Lorg/omg/CORBA/ORB;");

        mv = cw.visitMethod(ACC_PUBLIC, "orb", "()Lorg/omg/CORBA/ORB;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, tieClassName, "_orb", "()Lorg/omg/CORBA/ORB;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // -----------------------------------------------------------------------
        // public void orb(ORB orb)
        // {
        // #if portableServer
        //    try {
        //      ((org.omg.CORBA_2_3.ORB)orb).set_delegate(this);
        //    } catch (ClassCastException e) {
        //      throw new BAD_PARAM("POA Servant needs an org.omg.CORBA_2_3.ORB");
        //    }
        // #else
        //    orb.connect(this);
        // #endif
        // }
        // -----------------------------------------------------------------------
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : orb (Lorg/omg/CORBA/ORB;)V");

        mv = cw.visitMethod(ACC_PUBLIC, "orb", "(Lorg/omg/CORBA/ORB;)V", null, null);
        mv.visitCode();
        if (portableServer) {
            Label try_begin = new Label();
            mv.visitLabel(try_begin);

            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "org/omg/CORBA_2_3/ORB");
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA_2_3/ORB", "set_delegate", "(Ljava/lang/Object;)V");
            mv.visitInsn(RETURN);

            Label try_end = new Label();
            mv.visitLabel(try_end);

            Label catch_ClassCastException_label = try_end;
            mv.visitInsn(POP);
            mv.visitTypeInsn(NEW, "org/omg/CORBA/BAD_PARAM");
            mv.visitInsn(DUP);
            mv.visitLdcInsn("POA Servant needs an org.omg.CORBA_2_3.ORB");
            mv.visitMethodInsn(INVOKESPECIAL, "org/omg/CORBA/BAD_PARAM", "<init>", "(Ljava/lang/String;)V");
            mv.visitInsn(ATHROW);

            mv.visitTryCatchBlock(try_begin, try_end,
                                  catch_ClassCastException_label,
                                  "java/lang/ClassCastException");
        } else {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/ORB", "connect", "(Lorg/omg/CORBA/Object;)V");
            mv.visitInsn(RETURN);
        }
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        // -----------------------------------------------------------------------
        // public void setTarget(Remote remote)
        // {
        //    target = (EJSRemoteBMPBMPRaHome_a30a6d0b)remote;
        // }
        // -----------------------------------------------------------------------
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : setTarget (Ljava/rmi/Remote;)V");

        mv = cw.visitMethod(ACC_PUBLIC, "setTarget", "(Ljava/rmi/Remote;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, servantClassName);
        mv.visitFieldInsn(PUTFIELD, tieClassName, "target", servantDescriptor);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        // -----------------------------------------------------------------------
        // public org.omg.CORBA.Object thisObject()
        // {
        // #if portableServer
        //    return _this_object();
        // #else
        //    return this;
        // #endif
        // }
        // -----------------------------------------------------------------------
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : orb (Lorg/omg/CORBA/ORB;)V");

        mv = cw.visitMethod(ACC_PUBLIC, "thisObject", "()Lorg/omg/CORBA/Object;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        if (portableServer) {
            mv.visitMethodInsn(INVOKEVIRTUAL, tieClassName, "_this_object", "()Lorg/omg/CORBA/Object;");
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

    }

    /**
     * Adds the _invoke method inherited by Tie from InvokeHandler. <p>
     *
     * The _invoke method of org.omg.CORBA.portable.InvokeHandler is
     * invoked by the ORB to dispatch a request to the servant (wrapper).
     * The genarated implementation must determine which method is being
     * invoked, and route the request to the corresponding private
     * 'delegate' method on the Tie that knows how to decode/encode the
     * arguments and return value. <p>
     *
     * @param cw ASM ClassWriter to add the constructor to.
     * @param tieClassName fully qualified name of the Tie class
     *            with '/' as the separator character
     *            (i.e. internal name).
     * @param idlNames OMG IDL names corresponding to the
     *            remoteMethods for this tie.
     **/
    private static void add_invokeMethod(ClassWriter cw,
                                         String tieClassName,
                                         String[] idlNames)
    {
        GeneratorAdapter mg;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : _invoke " +
                         "(Ljava/lang/String;Lorg/omg/CORBA/portable/InputStream;" +
                         "Lorg/omg/CORBA/portable/ResponseHandler;)" +
                         "Lorg/omg/CORBA/portable/OutputStream;");

        // -----------------------------------------------------------------------
        // public OutputStream _invoke( String method,
        //                              InputStream _in,
        //                              ResponseHandler reply )
        //    throws SystemException
        // -----------------------------------------------------------------------

        Type returnType = TYPE_CORBA_OutputStream;
        Type[] argTypes = new Type[] { TYPE_String,
                                      TYPE_CORBA_InputStream,
                                      TYPE_CORBA_ResponseHandler };
        Type[] exceptionTypes = new Type[] { TYPE_CORBA_SystemException };

        org.objectweb.asm.commons.Method m =
                        new org.objectweb.asm.commons.Method("_invoke",
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

        /*
         * mg.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
         * mg.visitTypeInsn(NEW, "java/lang/StringBuilder");
         * mg.visitInsn(DUP);
         * mg.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V");
         * mg.visitLdcInsn("_invoke called for ");
         * mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
         * mg.visitVarInsn(ALOAD, 1);
         * mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
         * mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
         * mg.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
         * "(Ljava/lang/String;)V");
         */

        // -----------------------------------------------------------------------
        // try
        // {
        // -----------------------------------------------------------------------
        Label try_begin = new Label();
        mg.visitLabel(try_begin);

        // -----------------------------------------------------------------------
        //   InputStream in = (org.omg.CORBA_2_3.portable.InputStream) _in;
        // -----------------------------------------------------------------------
        int in = mg.newLocal(TYPE_CORBA_2_3_InputStream);
        mg.loadArg(1);
        mg.checkCast(TYPE_CORBA_2_3_InputStream);
        mg.storeLocal(in);

        // -----------------------------------------------------------------------
        // The next code to 'generate' is a switch statement, which is basically
        // what '_invoke' is all about.  The code will 'switch' based on the
        // hashCode of the OMG IDL name of the method invoked, and each 'case'
        // will determine the specific method for that hashCode, and delegate
        // the work to a private method by that name. However, before adding the
        // 'switch' statement, a few things need to be set up:
        //
        // First, in bytecode, all of the 'switch' statement keys (hashCode
        // values in this case) must be in ascending order. So, all of the OMG
        // IDL names for the methods must be sorted in hashcode ascending order.
        //
        // Second, the 'switch' bytecode requires arrays of the possible key
        // values (hashCodes), and labels (case jump points). So, pre-create
        // the arrays and labels (in ascending order).  These arrays will be
        // created with a length equal to the number of methods, since this will
        // be the most likely scenario: one 'case' per method.
        //
        // Finally, if two or more methods happen to have the same hashCode,
        // then reduce the size of the 'switch/case' arrays.  Note that each
        // method will still have a unique label, just not all will be
        // 'case' statements.
        // -----------------------------------------------------------------------

        // Clone the array returned by reflections (so the caller isn't affected)
        // and then use the java Arrays utility method to perform the sort.  A
        // special hash sort comparator is used that knows how to sort objects
        // based on the hashCode, rather than 'natural order'.
        int numMethods = idlNames.length;
        String[] sortedNames = idlNames.clone();
        Arrays.sort(sortedNames, new HashSorter<String>());

        int numCases = 0; // might not equal numMethods!
        int[] case_keys = new int[numMethods]; // idl hashCodes
        Label[] case_labels = new Label[numMethods];
        Label case_default = new Label();
        Label[] method_labels = new Label[numMethods];

        // Create a Label for every method, and add that Label and corresponding
        // hashcode to the lists for the 'case' statements if the hashCode is not
        // a duplicate.
        for (int i = 0; i < numMethods; ++i)
        {
            Label method_label = new Label();
            method_labels[i] = method_label;
            int hashCode = sortedNames[i].hashCode();
            if (i == 0 ||
                hashCode != sortedNames[i - 1].hashCode())
            {
                case_keys[numCases] = hashCode;
                case_labels[numCases] = method_label;
                ++numCases;
            }
        }

        // Reduce the sizes of the 'switch/case' statement arrays if some
        // methods had the same hashCode.
        if (numCases != numMethods)
        {
            int[] new_keys = new int[numCases];
            System.arraycopy(case_keys, 0, new_keys, 0, numCases);
            case_keys = new_keys;

            Label[] new_labels = new Label[numCases];
            System.arraycopy(case_labels, 0, new_labels, 0, numCases);
            case_labels = new_labels;
        }

        // -----------------------------------------------------------------------
        //   switch ( method.hashCode() )
        //   {
        // -----------------------------------------------------------------------
        mg.loadArg(0);
        mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I");
        mg.visitLookupSwitchInsn(case_default, case_keys, case_labels);

        // -----------------------------------------------------------------------
        //     // For all methods.... add the following case:
        //     // Note: if multiple methods have the same hash code, then there
        //     //       would be multiple 'if' statements for that 'case'
        //     case <hash code>:
        //       if ( method.equals( <idl name> ) )
        //       {
        //         return <idl name>( in, reply );     // private delegate method
        //       }
        //
        //       // Note: no break statement. Fall through... to the 'default'
        //       // for the BadOperationException... bytcode is much much easier
        //       // without the break statements.
        // -----------------------------------------------------------------------
        for (int i = 0; i < numMethods; ++i)
        {
            mg.visitLabel(method_labels[i]);
            mg.loadArg(0);
            mg.push(sortedNames[i]);
            mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String",
                               "equals", "(Ljava/lang/Object;)Z");
            int nextMethod = i + 1;
            Label next_label = case_default;
            if (nextMethod < numMethods)
                next_label = method_labels[nextMethod];
            mg.visitJumpInsn(IFEQ, next_label);
            mg.loadThis();
            mg.loadLocal(in);
            mg.loadArg(2);
            mg.visitMethodInsn(INVOKESPECIAL, tieClassName, sortedNames[i],
                               "(Lorg/omg/CORBA_2_3/portable/InputStream;Lorg/omg/CORBA/portable/ResponseHandler;)Lorg/omg/CORBA/portable/OutputStream;");
            mg.returnValue();
        }

        // -----------------------------------------------------------------------
        //     default:
        // -----------------------------------------------------------------------
        mg.visitLabel(case_default);

        // -----------------------------------------------------------------------
        //       throw new BAD_OPERATION( method );
        // -----------------------------------------------------------------------
        // Note: RMIC uses the default constructor. The method name is passed
        //       here to provide a more meaningful exception.
        mg.visitTypeInsn(NEW, "org/omg/CORBA/BAD_OPERATION");
        mg.visitInsn(DUP);
        mg.loadArg(0);
        mg.visitMethodInsn(INVOKESPECIAL, "org/omg/CORBA/BAD_OPERATION",
                           "<init>", "(Ljava/lang/String;)V");
        mg.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        //   }     // end switch
        // }     // end try
        // -----------------------------------------------------------------------
        Label try_end = new Label();
        mg.visitLabel(try_end);

        // -----------------------------------------------------------------------
        // catch ( SystemException ex )
        // {
        //   throw ex;
        // }
        // -----------------------------------------------------------------------
        Label catch_SysException_label = try_end;
        int ex = mg.newLocal(TYPE_CORBA_SystemException);
        mg.storeLocal(ex);
        mg.loadLocal(ex);
        mg.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        // catch ( Throwable th )
        // {
        //   throw new UnknownException( th );
        // }
        // -----------------------------------------------------------------------
        Label catch_Throwable_label = new Label();
        mg.visitLabel(catch_Throwable_label);
        int th = mg.newLocal(TYPE_Throwable);
        mg.storeLocal(th);
        mg.visitTypeInsn(NEW, "org/omg/CORBA/portable/UnknownException");
        mg.visitInsn(DUP);
        mg.loadLocal(th);
        mg.visitMethodInsn(INVOKESPECIAL,
                           "org/omg/CORBA/portable/UnknownException",
                           "<init>", "(Ljava/lang/Throwable;)V");
        mg.visitInsn(ATHROW);

        // -----------------------------------------------------------------------
        // All Try-Catch-Finally definitions for above
        // -----------------------------------------------------------------------
        mg.visitTryCatchBlock(try_begin, try_end,
                              catch_SysException_label,
                              "org/omg/CORBA/SystemException");
        mg.visitTryCatchBlock(try_begin, try_end,
                              catch_Throwable_label,
                              "java/lang/Throwable");

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

    /**
     * Adds a 'delegate' method to the Tie class. The 'delegate' methods
     * are private methods, one per method on the remote interface, called
     * by the public _invoke method. <p>
     *
     * All 'delegate' methods have the same signature, other than the
     * method name, which is the OMG IDL name of the remote method. <p>
     *
     * Each 'delegate' method is unique in that it knows how to read
     * the method parameters from the input stream, call the specific
     * servant method, and write the return value to the reply. <p>
     *
     * @param cw ASM ClassWriter to add the delegate method to.
     * @param tieClassName fully qualified name of the Tie class
     *            with '/' as the separator character
     *            (i.e. internal name).
     * @param servantClassName name of the remote implementation class that the
     *            generated tie class will invoke methods on with '/'
     *            as the separator character (i.e. internal name).
     * @param servantDescriptor fully qualified name of the servant
     *            (wrapper) class with '/' as the separator
     *            character (i.e. internal name), and
     *            wrapped with L; (jni style).
     * @param method reflection method from the interface defining
     *            method to be added to the tie.
     * @param idlName OMG IDL name of the specified method.
     * @param rmicCompatible rmic compatibility flags
     **/
    // dtkb
    private static void addDelegateMethod(ClassWriter cw,
                                          String tieClassName,
                                          Set<String> classConstantFieldNames,
                                          String servantClassName,
                                          String servantDescriptor,
                                          Method method,
                                          String idlName,
                                          boolean isRmiRemote,
                                          int rmicCompatible)
                    throws EJBConfigurationException
    {
        GeneratorAdapter mg;

        Class<?>[] methodParameters = method.getParameterTypes();
        Type[] argTypes = getTypes(methodParameters);
        final int numArgs = argTypes.length;
        Class<?> returnClass = method.getReturnType();
        Type returnType = Type.getType(returnClass);
        String methodDescriptor = Type.getMethodDescriptor(returnType,
                                                           argTypes);
        Class<?>[] checkedExceptions = DeploymentUtil.getCheckedExceptions(method,
                                                                           isRmiRemote,
                                                                           DeploymentUtil.DeploymentTarget.TIE); // d660332
        Type[] checkedTypes = getTypes(checkedExceptions);
        int numChecked = checkedExceptions.length;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : " + idlName +
                         " (Lorg/omg/CORBA_2_3/portable/InputStream;" +
                         "Lorg/omg/CORBA/portable/ResponseHandler;)" +
                         "Lorg/omg/CORBA/portable/OutputStream;");

        // -----------------------------------------------------------------------
        // private OutputStream <idl name> (InputStream in , ResponseHandler reply)
        //    throws Throwable
        // -----------------------------------------------------------------------
        Type delegateReturnType = TYPE_CORBA_OutputStream;
        Type[] delegateArgTypes = { TYPE_CORBA_2_3_InputStream,
                                   TYPE_CORBA_ResponseHandler };
        Type[] delegateExceptionTypes = { TYPE_Throwable };

        org.objectweb.asm.commons.Method m =
                        new org.objectweb.asm.commons.Method(idlName,
                                        delegateReturnType,
                                        delegateArgTypes);

        // Create an ASM GeneratorAdapter object for the ASM Method, which
        // makes generating dynamic code much easier... as it keeps track
        // of the position of the arguements and local variables, etc.
        mg = new GeneratorAdapter(ACC_PRIVATE, m, null, delegateExceptionTypes, cw);

        // -----------------------------------------------------------------------
        // Begin Method Code...
        // -----------------------------------------------------------------------
        mg.visitCode();

        /*
         * mg.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
         * mg.visitTypeInsn(NEW, "java/lang/StringBuilder");
         * mg.visitInsn(DUP);
         * mg.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V");
         * mg.visitLdcInsn(idlName + " called!!");
         * mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
         * mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
         * mg.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println",
         * "(Ljava/lang/String;)V");
         */

        // -----------------------------------------------------------------------
        // <arg type> argX = in.read_<primitive>();
        //              or
        // <arg type> argX = (<arg type>)in.read_value( <arg class> );
        //              or
        // <arg type> argX = (<arg type>)in.read_Object( <arg class> ); // CORBA.Object
        // -----------------------------------------------------------------------
        int[] argValues = new int[numArgs];

        for (int i = 0; i < numArgs; i++)
        {
            JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_ARG_BEGIN + i); // d726162

            argValues[i] = mg.newLocal(argTypes[i]);
            mg.loadArg(0);
            read_value(mg, tieClassName, classConstantFieldNames, // RTC111522
                       method, methodParameters[i], argTypes[i], rmicCompatible); // d450525
            mg.storeLocal(argValues[i]);
        }

        JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_DEFAULT); // d726162

        // -----------------------------------------------------------------------
        // // The try/catch block is only needed if there are
        // // checked exceptions other than RemoteException.
        // try
        // {
        // -----------------------------------------------------------------------
        Label try_begin = null;
        if (numChecked > 0)
        {
            try_begin = new Label();
            mg.visitLabel(try_begin);
        }

        // -----------------------------------------------------------------------
        //   target.<servant method>( argX, argX,... );
        //              or
        //   <return type> result = target.<servant method>( argX, argX,... );
        // -----------------------------------------------------------------------
        mg.loadThis();
        mg.visitFieldInsn(GETFIELD, tieClassName,
                          "target", servantDescriptor);
        for (int arg : argValues)
        {
            mg.loadLocal(arg);
        }
        mg.visitMethodInsn(INVOKEVIRTUAL, servantClassName,
                           method.getName(), methodDescriptor);

        int result = -1;
        if (returnType != Type.VOID_TYPE)
        {
            result = mg.newLocal(returnType);
            mg.storeLocal(result);
        }

        // -----------------------------------------------------------------------
        // }    // end of try - only when checked exceptions
        // -----------------------------------------------------------------------
        Label try_end = null;
        Label try_catch_exit = null;

        if (numChecked > 0)
        {
            try_end = new Label();
            mg.visitLabel(try_end);

            try_catch_exit = new Label();
            mg.visitJumpInsn(GOTO, try_catch_exit); // jump past catch blocks
        }

        // -----------------------------------------------------------------------
        // // For every checked exception (that is NOT a RemoteException).
        // // Note that the exceptions have been sorted in order, so that the
        // // subclasses are first... to avoid 'unreachable' code.
        //
        // catch ( <exception type> ex )
        // {
        //   String id = <idl exception name>;              // "IDL:xxx/XxxEx:1.0"
        //   2_3.OutputStream out = (2_3.OutputStream) reply.createExceptionReply();
        //   out.write_string( id );
        //   out.write_value( ex, <exception type>.class );
        //   return out;
        // }
        // -----------------------------------------------------------------------
        Label[] catch_checked_labels = null;
        if (numChecked > 0)
        {
            catch_checked_labels = new Label[numChecked];
            for (int i = 0; i < numChecked; ++i)
            {
                JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_CATCH_BEGIN + i); // d726162

                // catch ( <exception type> ex )
                Label catch_label = new Label();
                catch_checked_labels[i] = catch_label;
                mg.visitLabel(catch_label);
                int ex = mg.newLocal(checkedTypes[i]);
                mg.storeLocal(ex);

                //   String id = <idl exception name>;              // "IDL:xxx/XxxEx:1.0"
                int id = mg.newLocal(TYPE_String);
                boolean mangleComponents = com.ibm.wsspi.ejbcontainer.JITDeploy.isRMICCompatibleExceptions(rmicCompatible); // PM94096
                String exIdlName = getIdlExceptionName(checkedExceptions[i].getName(), mangleComponents);
                mg.visitLdcInsn(exIdlName);
                mg.storeLocal(id);

                //   2_3.OutputStream out = (2_3.OutputStream) reply.createExceptionReply();
                int out = mg.newLocal(TYPE_CORBA_2_3_OutputStream);
                mg.loadArg(1);
                mg.visitMethodInsn(INVOKEINTERFACE, "org/omg/CORBA/portable/ResponseHandler",
                                   "createExceptionReply",
                                   "()Lorg/omg/CORBA/portable/OutputStream;");
                mg.checkCast(TYPE_CORBA_2_3_OutputStream);
                mg.storeLocal(out);

                //   out.write_string( id );
                mg.loadLocal(out);
                mg.loadLocal(id);
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/OutputStream",
                                   "write_string", "(Ljava/lang/String;)V");

                //   out.write_value( ex, <exception type>.class );
                mg.loadLocal(out);
                mg.loadLocal(ex);
                JITUtils.loadClassConstant(mg, tieClassName, classConstantFieldNames, checkedExceptions[i]); // RTC111522
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA_2_3/portable/OutputStream",
                                   "write_value", "(Ljava/io/Serializable;Ljava/lang/Class;)V");

                //   return out;
                mg.loadLocal(out);
                mg.returnValue();
            }

            mg.visitLabel(try_catch_exit);
        }

        JITUtils.setLineNumber(mg, JITUtils.LINE_NUMBER_RETURN); // d726162

        // -----------------------------------------------------------------------
        // OutputStream out = reply.createReply();
        //              or
        // CORBA_2_3.OutputStream out = (CORBA_2_3.OutputStream)reply.createReply();
        // -----------------------------------------------------------------------
        Type outType = CORBA_Utils.getRequiredOutputStreamType(returnClass, rmicCompatible); // PM46698
        int out = mg.newLocal(TYPE_CORBA_OutputStream);
        mg.loadArg(1);
        mg.visitMethodInsn(INVOKEINTERFACE,
                           "org/omg/CORBA/portable/ResponseHandler",
                           "createReply",
                           "()Lorg/omg/CORBA/portable/OutputStream;");
        if (outType != TYPE_CORBA_OutputStream)
        {
            mg.checkCast(outType);
        }
        mg.storeLocal(out);

        // -----------------------------------------------------------------------
        // out.write_<primitive>( result );
        //              or
        // out.write_value( result, <class> );
        //              or
        // Util.writeAny( out, result );
        //              or
        // Util.writeRemoteObject( out, result );
        // -----------------------------------------------------------------------
        if (returnType != Type.VOID_TYPE)
        {
            mg.loadLocal(out);
            mg.loadLocal(result);
            write_value(mg, tieClassName, classConstantFieldNames, // RTC111522
                        method, returnClass, rmicCompatible); // d450525, PM46698
        }

        // -----------------------------------------------------------------------
        //   return out;
        // -----------------------------------------------------------------------
        mg.loadLocal(out);
        mg.returnValue();

        // -----------------------------------------------------------------------
        // All Try-Catch-Finally definitions for above
        // -----------------------------------------------------------------------
        for (int i = 0; i < numChecked; ++i)
        {
            String exName = convertClassName(checkedExceptions[i].getName());
            mg.visitTryCatchBlock(try_begin, try_end,
                                  catch_checked_labels[i],
                                  exName);
        }

        // -----------------------------------------------------------------------
        // End Method Code...
        // -----------------------------------------------------------------------
        mg.endMethod(); // GeneratorAdapter accounts for visitMaxs(x,y)
        mg.visitEnd();
    }

}
