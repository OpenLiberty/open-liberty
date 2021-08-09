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

import static com.ibm.ws.ejbcontainer.jitdeploy.JITUtils.TYPE_Serializable;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

import java.io.Externalizable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Set;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.ibm.ejs.container.EJBConfigurationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.ejbcontainer.JITDeploy;

/**
 * Just In Time runtime deployment CORBA utility methods for generating
 * Stubs and Ties. <p>
 */
public final class CORBA_Utils
{
    private static final TraceComponent tc = Tr.register(CORBA_Utils.class, JITUtils.JIT_TRACE_GROUP, JITUtils.JIT_RSRC_BUNDLE);

    // --------------------------------------------------------------------------
    // The following static finals are for commonly used ASM Type objects for
    // CORBA classes, and allow the JITDeploy code to avoid creating these
    // objects over and over again.
    // --------------------------------------------------------------------------
    static final Type TYPE_CORBA_ApplicationException =
                    Type.getType("Lorg/omg/CORBA/portable/ApplicationException;");
    static final Type TYPE_CORBA_InputStream =
                    Type.getType("Lorg/omg/CORBA/portable/InputStream;");
    static final Type TYPE_CORBA_OutputStream =
                    Type.getType("Lorg/omg/CORBA/portable/OutputStream;");
    static final Type TYPE_CORBA_2_3_InputStream =
                    Type.getType("Lorg/omg/CORBA_2_3/portable/InputStream;");
    static final Type TYPE_CORBA_2_3_OutputStream =
                    Type.getType("Lorg/omg/CORBA_2_3/portable/OutputStream;");
    static final Type TYPE_CORBA_ResponseHandler =
                    Type.getType("Lorg/omg/CORBA/portable/ResponseHandler;");
    static final Type TYPE_CORBA_ServantObject =
                    Type.getType("Lorg/omg/CORBA/portable/ServantObject;");
    static final Type TYPE_CORBA_SystemException =
                    Type.getType("Lorg/omg/CORBA/SystemException;");

    /**
     * Returns a list of the remote interface types that the
     * generated Stub or Tie may be 'narrowed' to, in the format
     * RMI:class:0000000000000000. This includes the remote
     * ejb interface class specified, and all super classes,
     * excluding Object and Remote. <p>
     *
     * @param remoteInterface the Remote Business or Component
     *            interface for the ejb.
     **/
    static String[] getRemoteTypeIds(Class<?> remoteInterface) {
        ArrayList<String> remoteTypes = new ArrayList<String>();
        remoteTypes.add(getRemoteTypeId(remoteInterface));

        Class<?>[] interfaces = remoteInterface.getInterfaces();
        for (Class<?> remote : interfaces) {
            if (remote != Remote.class) {
                remoteTypes.add(getRemoteTypeId(remote));
            }
        }

        return remoteTypes.toArray(new String[remoteTypes.size()]);
    }

    /**
     * Returns the remote interface type ID for a class, in
     * in the format RMI:class:0000000000000000.
     *
     * @param remoteInterface the Remote Business or Component
     *            interface for the ejb.
     */
    public static String getRemoteTypeId(Class<?> remoteInterface) {
        return getRemoteTypeId(remoteInterface.getName());
    }

    /**
     * Returns the remote interface type ID for a class, in
     * in the format RMI:class:0000000000000000.
     *
     * @param remoteInterface the Remote Business or Component
     *            interface for the ejb.
     */
    public static String getRemoteTypeId(String remoteInterface) {
        return "RMI:" + remoteInterface + ":0000000000000000";
    }

    /**
     * Returns the ASM Type object for the OutputStream required to write
     * an instance of the specified class. <p>
     *
     * Many parameter and return value types may be serialized using the
     * original org.omg.CORBA.portable.OutputStream implementation,
     * however, some require the new CORBA_2_3 subclass. This method
     * consolidates the logic to determine when the subclass is
     * required. <p>
     *
     * Determining which type of outputstream class is required may be
     * useful in avoiding a cast in generated Stub and Tie classes. <p>
     *
     * @param clazz class for a method parameter or return type
     *            that needs to be written to an output stream.
     * @param rmicCompatible rmic compatibility flags
     **/
    static Type getRequiredOutputStreamType(Class<?> clazz,
                                            int rmicCompatible) // PM46698
    {
        // NOTE: This logic must be kept in sync with write_value

        if (clazz == Void.TYPE || // nothing to write
            clazz == Object.class || // writeAny
            clazz.isPrimitive() || // write_<primitive>
            (clazz.isInterface() &&
            (clazz == Serializable.class || // writeAny
             clazz == Externalizable.class || // writeAny
             isCORBAObject(clazz, rmicCompatible) || // write_Object
             (Remote.class).isAssignableFrom(clazz) || // writeRemoteObject
            isAbstractInterface(clazz, rmicCompatible)))) // writeAbstractObject
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "getRequiredOutputStreamType: " + clazz.getName() +
                             " => org.omg.CORBA.portable.OutputStream");
            return TYPE_CORBA_OutputStream;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getRequiredOutputStreamType: " + clazz.getName() +
                         " => org.omg.CORBA_2_3.portable.OutputStream");

        // requires 'write_value'
        return TYPE_CORBA_2_3_OutputStream;
    }

    /**
     * Returns true if the state of an instance of the specified class may
     * be modified after the instance has been created. <p>
     *
     * Returns false for the pimitives, primitive wrapper classes, and
     * Sring. <p>
     *
     * This method is useful when generating the 'local' optimization code
     * for a stub, as only 'mutable' objects must be copied. <p>
     *
     * @param clazz class to check for mutability.
     **/
    static boolean isMutable(Class<?> clazz)
    {
        if (clazz.isPrimitive())
            return false;

        if (clazz == String.class)
            return false;
        if (clazz == Boolean.class)
            return false;
        if (clazz == Character.class)
            return false;
        if (clazz == Byte.class)
            return false;
        if (clazz == Short.class)
            return false;
        if (clazz == Integer.class)
            return false;
        if (clazz == Long.class)
            return false;
        if (clazz == Float.class)
            return false;
        if (clazz == Double.class)
            return false;

        return true;
    }

    /**
     * Return true if the class can be written using write_Object.
     *
     * @param valueClass class of the value
     * @param rmicCompatible rmic compatibility flags
     */
    // PM46698
    private static boolean isCORBAObject(Class<?> valueClass, int rmicCompatible)
    {
        if (JITDeploy.isRMICCompatibleValues(rmicCompatible) &&
            org.omg.CORBA.Object.class.isAssignableFrom(valueClass))
        {
            return true;
        }

        return false;
    }

    /**
     * Returns true if the class can be written using writeAbstractInterface.
     *
     * @param valueClass the non-java.rmi.Remote interface
     * @param rmicCompatible rmic compatibility flags
     */
    // PM46698
    public static boolean isAbstractInterface(Class<?> valueClass, int rmicCompatible)
    {
        if (JITDeploy.isRMICCompatibleValues(rmicCompatible))
        {
            for (Method method : valueClass.getMethods())
            {
                if (!hasRemoteException(method))
                {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Returns true if the method contains an exception on the throws clause
     * that is RemoteException or a super-class.
     */
    // PM46698
    private static boolean hasRemoteException(Method method)
    {
        for (Class<?> exceptionType : method.getExceptionTypes())
        {
            if (exceptionType == RemoteException.class ||
                exceptionType == IOException.class ||
                exceptionType == Exception.class ||
                exceptionType == Throwable.class)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines whether a value of the specified type could be a remote
     * object reference. This should return true if read_Object or
     * read_abstract_interface is used for this type.
     *
     * @param clazz class for a method parameter or return type
     *            that needs to be read from an input stream
     * @param rmicCompatible rmic compatibility flags
     * @return true if a stub is needed
     */
    public static boolean isRemoteable(Class<?> valueClass, int rmicCompatible) {
        // NOTE: This logic must be kept in sync with write_value.

        return valueClass.isInterface() &&
               valueClass != Serializable.class &&
               valueClass != Externalizable.class &&
               (isCORBAObject(valueClass, rmicCompatible) ||
                Remote.class.isAssignableFrom(valueClass) ||
               isAbstractInterface(valueClass, rmicCompatible));
    }

    /**
     * Adds the code to read the specified type from a CORBA InputStream. <p>
     *
     * When generating Stubs and Ties, every parameter (Ties) and return value
     * (Stubs) must be read using the correct type specific read method on the
     * CORBA InputStream. For example, a boolean must be read with read_boolean.
     * This method will add the invocation of the correct read method to the
     * ASM MethodAdapter for the spcified type. <p>
     *
     * The ASM Type parameter is for performance. And, the Method parameter is
     * for trace/error messages. <p>
     *
     * @param mg ASM GeneratorAdapter for the current method being generated
     * @param className The name of the current class being generated.
     * @param classConstantFieldNames The field names of used class constants.
     * @param method Method object for the method being generated (for trace)
     * @param valueClass Class of the value which needs to be read
     * @param valueType ASM Type corresponding to the valueClass parameter
     * @param rmicCompatible rmic compatibility flags
     **/
    static void read_value(GeneratorAdapter mg,
                           String className,
                           Set<String> classConstantFieldNames,
                           Method method,
                           Class<?> valueClass,
                           Type valueType,
                           int rmicCompatible)
                    throws EJBConfigurationException
    {
        if (valueClass.isPrimitive())
        {
            if (valueClass == Boolean.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/InputStream",
                                   "read_boolean", "()Z");
            }
            else if (valueClass == Character.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/InputStream",
                                   "read_wchar", "()C");
            }
            else if (valueClass == Byte.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/InputStream",
                                   "read_octet", "()B");
            }
            else if (valueClass == Short.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/InputStream",
                                   "read_short", "()S");
            }
            else if (valueClass == Integer.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/InputStream",
                                   "read_long", "()I");
            }
            else if (valueClass == Long.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/InputStream",
                                   "read_longlong", "()J");
            }
            else if (valueClass == Float.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/InputStream",
                                   "read_float", "()F");
            }
            else if (valueClass == Double.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/InputStream",
                                   "read_double", "()D");
            }
        }
        else if (valueClass == Object.class)
        {
            mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "readAny",
                               "(Lorg/omg/CORBA/portable/InputStream;)Ljava/lang/Object;");
        }
        else
        {
            if (valueClass.isInterface())
            {
                if (valueClass == Serializable.class ||
                    valueClass == Externalizable.class)
                {
                    mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "readAny",
                                       "(Lorg/omg/CORBA/portable/InputStream;)Ljava/lang/Object;");
                }
                else if (isCORBAObject(valueClass, rmicCompatible) || // PM46698
                         (Remote.class).isAssignableFrom(valueClass))
                {
                    if (valueClass == org.omg.CORBA.Object.class) // RTC111522
                    {
                        mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/InputStream",
                                           "read_Object", "()Lorg/omg/CORBA/Object;");
                    }
                    else
                    {
                        JITUtils.loadClassConstant(mg, className, classConstantFieldNames, valueClass); // RTC111522
                        mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/InputStream",
                                           "read_Object", "(Ljava/lang/Class;)Lorg/omg/CORBA/Object;");
                    }
                }
                else if (isAbstractInterface(valueClass, rmicCompatible)) // PM46698
                {
                    JITUtils.loadClassConstant(mg, className, classConstantFieldNames, valueClass); // RTC111522
                    mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA_2_3/portable/InputStream",
                                       "read_abstract_interface", "(Ljava/lang/Class;)Ljava/lang/Object;");
                }
                else
                {
                    JITUtils.loadClassConstant(mg, className, classConstantFieldNames, valueClass); // RTC111522
                    mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA_2_3/portable/InputStream",
                                       "read_value", "(Ljava/lang/Class;)Ljava/io/Serializable;");
                }
            }
            else if ((Remote.class).isAssignableFrom(valueClass))
            {
                // Not a valid RMI/IIOP type - log message and throw exception. d450525
                Tr.error(tc, "JIT_INVALID_ARG_RETURN_TYPE_CNTR5100E",
                         new Object[] { valueClass.getName(),
                                       method.getName(),
                                       method.getDeclaringClass().getName() });
                throw new EJBConfigurationException("Argument or return type " + valueClass.getName() + " of method " +
                                                    method.getName() + " on class " + method.getDeclaringClass().getName() +
                                                    " is not a valid type for RMI/IIOP.");
            }
            else
            {
                JITUtils.loadClassConstant(mg, className, classConstantFieldNames, valueClass); // RTC111522
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA_2_3/portable/InputStream",
                                   "read_value", "(Ljava/lang/Class;)Ljava/io/Serializable;");
            }

            JITUtils.checkCast(mg, valueType.getInternalName());
        }
    }

    /**
     * Adds the code to write the specified type to a CORBA OutputStream. <p>
     *
     * When generating Stubs and Ties, every parameter (Stubs) and return value
     * (Ties) must be written using the correct type specific write method on the
     * CORBA OutputStream. For example, a boolean must be written with write_boolean.
     * This method will add the invocation of the correct write method to the
     * ASM MethodAdapter for the spcified type. <p>
     *
     * The Method parameter is for trace/error messages. <p>
     *
     * @param mg ASM GeneratorAdapter for the current method being generated
     * @param className The name of the current class being generated.
     * @param classConstantFieldNames The field names of used class constants.
     * @param method Method object for the method being generated (for trace)
     * @param valueClass Class of the value which needs to be read
     * @param rmicCompatible rmic compatibility flags
     **/
    static void write_value(GeneratorAdapter mg,
                            String className,
                            Set<String> classConstantFieldNames,
                            Method method,
                            Class<?> valueClass,
                            int rmicCompatible)
                    throws EJBConfigurationException
    {
        // NOTE: This logic must be kept in sync with getRequiredOutputStreamType.

        if (valueClass.isPrimitive())
        {
            if (valueClass == Boolean.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/OutputStream",
                                   "write_boolean", "(Z)V");
            }
            else if (valueClass == Character.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/OutputStream",
                                   "write_wchar", "(C)V");
            }
            else if (valueClass == Byte.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/OutputStream",
                                   "write_octet", "(B)V");
            }
            else if (valueClass == Short.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/OutputStream",
                                   "write_short", "(S)V");
            }
            else if (valueClass == Integer.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/OutputStream",
                                   "write_long", "(I)V");
            }
            else if (valueClass == Long.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/OutputStream",
                                   "write_longlong", "(J)V");
            }
            else if (valueClass == Float.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/OutputStream",
                                   "write_float", "(F)V");
            }
            else if (valueClass == Double.TYPE)
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/OutputStream",
                                   "write_double", "(D)V");
            }
        }
        else if (valueClass.isInterface())
        {
            if (valueClass == Serializable.class ||
                valueClass == Externalizable.class)
            {
                mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "writeAny",
                                   "(Lorg/omg/CORBA/portable/OutputStream;Ljava/lang/Object;)V");
            }
            else if (isCORBAObject(valueClass, rmicCompatible)) // PM46698
            {
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA/portable/OutputStream",
                                   "write_Object", "(Lorg/omg/CORBA/Object;)V");
            }
            else if ((Remote.class).isAssignableFrom(valueClass))
            {
                mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "writeRemoteObject",
                                   "(Lorg/omg/CORBA/portable/OutputStream;Ljava/lang/Object;)V");
            }
            else if (isAbstractInterface(valueClass, rmicCompatible)) // PM46698
            {
                mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "writeAbstractObject",
                                   "(Lorg/omg/CORBA/portable/OutputStream;Ljava/lang/Object;)V");
            }
            else
            {
                if (!Serializable.class.isAssignableFrom(valueClass)) // PM46698
                {
                    mg.visitTypeInsn(CHECKCAST, "java/io/Serializable");
                }

                JITUtils.loadClassConstant(mg, className, classConstantFieldNames, valueClass); // RTC111522
                mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA_2_3/portable/OutputStream",
                                   "write_value", "(Ljava/io/Serializable;Ljava/lang/Class;)V");
            }
        }
        else if (valueClass == Object.class)
        {
            mg.visitMethodInsn(INVOKESTATIC, "javax/rmi/CORBA/Util", "writeAny",
                               "(Lorg/omg/CORBA/portable/OutputStream;Ljava/lang/Object;)V");
        }
        else if ((Remote.class).isAssignableFrom(valueClass))
        {
            // Not a valid RMI/IIOP type - log message and throw exception. d450525
            Tr.error(tc, "JIT_INVALID_ARG_RETURN_TYPE_CNTR5100E",
                     new Object[] { valueClass.getName(),
                                   method.getName(),
                                   method.getDeclaringClass().getName() });
            throw new EJBConfigurationException("Argument or return type " + valueClass.getName() + " of method " +
                                                method.getName() + " on class " + method.getDeclaringClass().getName() +
                                                " is not a valid type for RMI/IIOP.");
        }
        else
        {
            mg.checkCast(TYPE_Serializable);

            JITUtils.loadClassConstant(mg, className, classConstantFieldNames, valueClass); // RTC111522
            mg.visitMethodInsn(INVOKEVIRTUAL, "org/omg/CORBA_2_3/portable/OutputStream",
                               "write_value", "(Ljava/io/Serializable;Ljava/lang/Class;)V");
        }
    }

}
