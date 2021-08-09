/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.jitdeploy;

import static com.ibm.ws.ejbcontainer.jitdeploy.JITPlatformHelper.getLogLocation;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTSTATIC;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Set;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Just In Time Deployment utility methods, which are of general purpose use
 * for dealing with ASM, generating code, or managing class bytes. <p>
 */
final class JITUtils
{
    // --------------------------------------------------------------------------
    // Constants used throughout JITDeploy - including trace group / bundle
    // --------------------------------------------------------------------------
    static final String JIT_TRACE_GROUP = "JITDeploy";
    static final String JIT_RSRC_BUNDLE = "com.ibm.ejs.container.container";

    static final String INDENT = "     ";
    static final Class<?>[] NO_PARAMS = new Class[0];

    /**
     * The line number to use when no useful information can be conveyed.
     */
    static final int LINE_NUMBER_DEFAULT = 1; // d726162

    /**
     * The line number to use for return values.
     */
    static final int LINE_NUMBER_RETURN = 2; // d726162

    /**
     * The line number to use to represent the first argument. Subsequent
     * arguments should use LINE_NUMBER_ARG_BEGIN + 1, etc.
     */
    static final int LINE_NUMBER_ARG_BEGIN = 100; // d726162

    /**
     * The line number to use to represent the first catch statement. Subsequent
     * catch statements should use LINE_NUMBER_CATCH_BEGIN + 1, etc.
     */
    static final int LINE_NUMBER_CATCH_BEGIN = 1000; // d726162

    // --------------------------------------------------------------------------
    // Private variables & constants used within this class - including trace
    // --------------------------------------------------------------------------
    private static final String CLASS_NAME = JITUtils.class.getName();

    private static final TraceComponent tc = Tr.register(JITUtils.class,
                                                         JIT_TRACE_GROUP,
                                                         JIT_RSRC_BUNDLE);

    private static final TraceComponent tcJITDeployRuntime = Tr.register(CLASS_NAME + "-Runtime",
                                                                         JITUtils.class,
                                                                         "JITDeployRuntime",
                                                                         null);

    private static final String JIT_DEPLOY_DIR = File.separator + "jitdeploy" + File.separator;

    // --------------------------------------------------------------------------
    // The following static finals are for commonly used ASM Type objects,
    // and allow the JITDeploy code to avoid creating these objects over
    // and over again.
    // --------------------------------------------------------------------------
    static final Type TYPE_Exception = Type.getType("Ljava/lang/Exception;");
    static final Type TYPE_Object = Type.getType("Ljava/lang/Object;");
    static final Type TYPE_Object_ARRAY = Type.getType("[Ljava/lang/Object;");
    static final Type TYPE_String = Type.getType("Ljava/lang/String;");
    static final Type TYPE_Throwable = Type.getType("Ljava/lang/Throwable;");
    static final Type TYPE_RuntimeException = Type.getType("Ljava/lang/RuntimeException;"); // d660332
    static final Type TYPE_Serializable = Type.getType("Ljava/io/Serializable;");

    /**
     * Converts a standard Java class name to the 'internal' Java
     * class name format. <p>
     * 
     * ASM uses Java 'internal' class names (like JNI), where '/'
     * is used instead of '.' as the separator character. This
     * utility method just replaces all occurrences of '.' with
     * '/'. <p>
     * 
     * @param className Java class name to be converted
     * 
     * @return the class name in 'internal' format.
     **/
    static String convertClassName(String className)
    {
        return className.replace('.', '/');
    }

    /**
     * Converts a standard Java class name of the specified Class
     * to the 'internal' Java class name format. <p>
     * 
     * ASM uses Java 'internal' class names (like JNI), where '/'
     * is used instead of '.' as the separator character. This
     * utility method just replaces all occurrences of '.' with
     * '/'. <p>
     * 
     * If null is passed as the class parameter, null will be returned. <p>
     * 
     * @param clazz Java class from which to obtain the name to be converted
     * 
     * @return the class name in 'internal' format, or null if the class is null.
     **/
    // d497921
    static String convertClassName(Class<?> clazz)
    {
        if (clazz == null)
            return null;

        String className = clazz.getName();

        return className.replace('.', '/');
    }

    /**
     * Get method string of form:
     * 
     * methodName(<class name of arg1>,<class name of arg2>,...)
     **/
    // d490485
    public static String methodKey(Method m)
    {
        StringBuilder result = new StringBuilder(m.getName());

        result.append("(");

        Class<?> argTypes[] = m.getParameterTypes();
        for (int i = 0; i < argTypes.length; i++)
        {
            if (i > 0)
                result.append(",");
            result.append(argTypes[i].getName());
        }
        result.append(")");

        return result.toString();
    }

    /**
    *
    **/
    // d457086
    static final String jdiMethodSignature(Method method)
    {
        StringBuilder sb = new StringBuilder();
        Class<?>[] methodParams = method.getParameterTypes();
        sb.append("(");
        for (int j = 0; j < methodParams.length; j++) {
            sb.append(mapTypeToJDIEncoding(methodParams[j]));
        }
        sb.append(")");
        sb.append(mapTypeToJDIEncoding(method.getReturnType()));
        return sb.toString();
    }

    /**
    *
    **/
    // d457086
    private static final String mapTypeToJDIEncoding(Class<?> type)
    {
        String returnValue;
        String typeName = type.getName();
        if (type.isArray()) {
            returnValue = typeName.replace('.', '/');
        } else {
            // check for these in rough order of frequency for best performance
            if (typeName.indexOf('.') > 0)
                returnValue = "L" + typeName.replace('.', '/') + ";";
            else if (typeName.equals("void"))
                returnValue = "V";
            else if (typeName.equals("boolean"))
                returnValue = "Z";
            else if (typeName.equals("int"))
                returnValue = "I";
            else if (typeName.equals("long"))
                returnValue = "J";
            else if (typeName.equals("double"))
                returnValue = "D";
            else if (typeName.equals("float"))
                returnValue = "F";
            else if (typeName.equals("char"))
                returnValue = "C";
            else if (typeName.equals("byte"))
                returnValue = "B";
            else if (typeName.equals("short"))
                returnValue = "S";

            else
                returnValue = "L" + typeName + ";";
        }
        return returnValue;
    }

    /**
     * Converts an array of Java classes to an array of ASM 'Type'
     * objects that represent those classes. <p>
     * 
     * Useful when creating an ASM 'Method' object from a Java reflect
     * 'Method' object. Java reflections uses Java classes for the parameters
     * and thrown exceptions, whereas ASM requires the use of 'Type's. <p>
     * 
     * @param classes array of java classes
     * 
     * @return an array of ASM Type objects corresponding the specified classes.
     **/
    static Type[] getTypes(Class<?>[] classes)
    {
        int size = classes.length;
        Type[] types = new Type[size];
        for (int i = 0; i < size; ++i)
        {
            types[i] = Type.getType(classes[i]);
        }
        return types;
    }

    /**
     * Returns a list of 'checked'/Application exceptions where exception that
     * are subclasses of other 'checked' exceptions will either be eliminated,
     * or sorted in parent-last order to avoid 'unreachable' code. See the
     * 'removeSubclasses parameter. <p>
     * 
     * This method is designed for use when generating the EJB Proxys,
     * to determine which exceptions will require 'catch' blocks. <p>
     * 
     * @param exceptions list of method exceptions to be sorted.
     * @param removeSubclasses true if exceptions that are subclasses of
     *            other exceptions on the throws clause are to be removed;
     *            otherwise, they are sorted in parent-last order.
     * 
     * @return an array of checked/application exceptions that must be
     *         handled by the generated code.
     **/
    // d497921
    static Class<?>[] sortExceptions(Class<?>[] exceptions,
                                     boolean removeSubclasses)
    {
        int numExceptions = exceptions.length;
        ArrayList<Class<?>> checkedExceptions = new ArrayList<Class<?>>(numExceptions);

        // -----------------------------------------------------------------------
        // Assume all passed exception are valid application exception, but they
        // cannot just be added to the end of the list, since they may be a
        // subclass of other exceptions on the throws clause, which could result
        // in 'unreachable' code when adding 'catch' blocks.
        //
        // Instead, do one of two things:
        // 1 - Remove any exceptions that are subclasses of other exceptions.
        //     This is useful when a catch block is being added for each
        //     checked exception, and the processing done in the parent
        //     exception would be the same as any subclasses.  For example,
        //     in a generated wrapper, catch blocks for all checked exceptions
        //     just call 'setCheckedException'... so no need to duplicate that
        //     for all subclass exceptions.
        // 2 - Insert the current exception ahead of the first exception
        //     already in the list that is a parent.  This eliminates the
        //     'unreachable' code problem, and also ends up with the
        //     exceptions in the original order if there are no subclasses.
        // -----------------------------------------------------------------------
        for (Class<?> exception : exceptions)
        {
            if (removeSubclasses)
            {
                // -----------------------------------------------------------------
                // Remove any exceptions that are subclasses of others
                // -----------------------------------------------------------------
                boolean isSubclass = false;
                for (Class<?> parentEx : exceptions)
                {
                    if ((exception != parentEx) &&
                        (parentEx.isAssignableFrom(exception)))
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "getCheckedExceptions: ignoring " +
                                         exception.getName() + ", subclass of " +
                                         parentEx.getName());
                        isSubclass = true;
                        break;
                    }
                }

                if (!isSubclass)
                    checkedExceptions.add(exception);
            }
            else
            {
                // -----------------------------------------------------------------
                // Sort exceptions, so that subclasses are ahead of parents
                // -----------------------------------------------------------------
                int j;
                int numChecked = checkedExceptions.size();
                for (j = 0; j < numChecked; ++j)
                {
                    Class<?> checkedEx = checkedExceptions.get(j);
                    if (checkedEx.isAssignableFrom(exception))
                        break;
                }

                checkedExceptions.add(j, exception);
            }
        }

        return checkedExceptions.toArray(new Class[checkedExceptions.size()]);
    }

    /**
     * Sets the line number for the current position in the code.
     * 
     * @param mv method visitor
     * @param line the number to use
     * @see #LINE_NUMBER_DEFAULT
     * @see #LINE_NUMBER_RETURN
     * @see #LINE_NUMBER_ARG_BEGIN
     * @see #LINE_NUMBER_CATCH_BEGIN
     */
    // d726162
    public static void setLineNumber(MethodVisitor mv, int line)
    {
        Label label = new Label();
        mv.visitLabel(label);
        mv.visitLineNumber(line, label);
    }

    /**
     * Performs a checked cast of the value on top of the stack, and optionally
     * inserts tracing.
     * 
     * @param mv method visitor used to perform the cast
     * @param internalName the internal name of the target type
     */
    // d726162
    public static void checkCast(MethodVisitor mv, String internalName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tcJITDeployRuntime.isDebugEnabled())
        {
            String className;
            if (internalName.charAt(0) == '[')
            {
                int begin = 0;
                do
                {
                    begin++;
                } while (internalName.charAt(begin) == '[');

                if (internalName.charAt(begin) == 'L')
                {
                    // [[[Ljava/lang/Object; -> [[[java.lang.Object
                    className = internalName.substring(0, begin) +
                                internalName.substring(begin + 1, internalName.length() - 1).replace('/', '.');
                }
                else
                {
                    // [[[B remains unchanged.
                    className = internalName;
                }
            }
            else
            {
                // java/lang/Object -> java.lang.Object
                className = internalName.replace('/', '.');
            }

            mv.visitVarInsn(ALOAD, 0);
            mv.visitLdcInsn(className);
            mv.visitMethodInsn(INVOKESTATIC, "com/ibm/ejs/container/JIT_Debug", "checkCast",
                               "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
        }

        mv.visitTypeInsn(CHECKCAST, internalName);
    }

    /**
     * Unboxes the value at the top of the stack, replacing the value on the
     * stack with the unboxed equivalent. <p>
     * 
     * Unboxing to 'void' is a no-op and unboxing to an Object or Array
     * type will just result in a cast. <p>
     * 
     * This method is similar to the 'unbox' method on GeneratorAdapter,
     * but always unboxes from the corresponding object wrapper type.
     * For example, int from Integer, short from Short, byte from Byte...
     * whereas the method on GeneratorAdapter will unbox these from
     * Number. <p>
     * 
     * @param mg GeneratorAdaptor used to perform the unbox
     * @param type primitive type to unbox to.
     **/
    // d369262.7
    static void unbox(GeneratorAdapter mg, Type type)
    {
        switch (type.getSort())
        {
            case Type.VOID:
                return;
            case Type.BOOLEAN:
                mg.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean",
                                   "booleanValue", "()Z");
                break;
            case Type.CHAR:
                mg.visitTypeInsn(CHECKCAST, "java/lang/Character");
                mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character",
                                   "charValue", "()C");
                break;
            case Type.BYTE:
                mg.visitTypeInsn(CHECKCAST, "java/lang/Byte");
                mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte",
                                   "byteValue", "()B");
                break;
            case Type.SHORT:
                mg.visitTypeInsn(CHECKCAST, "java/lang/Short");
                mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short",
                                   "shortValue", "()S");
                break;
            case Type.INT:
                mg.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer",
                                   "intValue", "()I");
                break;
            case Type.FLOAT:
                mg.visitTypeInsn(CHECKCAST, "java/lang/Float");
                mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float",
                                   "floatValue", "()F");
                break;
            case Type.LONG:
                mg.visitTypeInsn(CHECKCAST, "java/lang/Long");
                mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long",
                                   "longValue", "()J");
                break;
            case Type.DOUBLE:
                mg.visitTypeInsn(CHECKCAST, "java/lang/Double");
                mg.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double",
                                   "doubleValue", "()D");
                break;
            default:
                mg.checkCast(type);
                break;
        }
    }

    /**
     * Returns the synthetic field name for a Class constant.
     * 
     * @param klass the class constant
     * @return the synthetic field name
     */
    public static String getClassConstantFieldName(Class<?> klass) // d676434
    {
        String dollarName = klass.getName().replace('.', '$');

        if (dollarName.startsWith("["))
        {
            if (dollarName.endsWith(";"))
            {
                // Remove trailing ";" from "[Ljava$lang$String;".
                dollarName = dollarName.substring(0, dollarName.length() - 1);
            }

            return "array" + dollarName.replace('[', '$');
        }

        return "class$" + dollarName;
    }

    /**
     * Generates code to load a Class constant. After all calls to this method
     * have been made, {@link #addClassConstantFields} must be called.
     * 
     * @param mg ASM method generator for the current method.
     * @param className The name of the class being written.
     * @param classConstantFieldNames The set of class constants to be updated.
     * @param classConstant The class constant.
     * @see #addClassConstantFields
     */
    public static void loadClassConstant(MethodVisitor mg,
                                         String className,
                                         Set<String> classConstantFieldNames,
                                         Class<?> classConstant) // d676434
    {
        String fieldName = getClassConstantFieldName(classConstant);
        classConstantFieldNames.add(fieldName);

        // -----------------------------------------------------------------------
        //   class$com$ibm$example$Test != null ?
        // -----------------------------------------------------------------------
        mg.visitFieldInsn(GETSTATIC, className, fieldName, "Ljava/lang/Class;");
        Label if_null_then = new Label();
        mg.visitJumpInsn(IFNULL, if_null_then);

        // -----------------------------------------------------------------------
        //     class$com$ibm$example$Test :
        // -----------------------------------------------------------------------
        mg.visitFieldInsn(GETSTATIC, className, fieldName, "Ljava/lang/Class;");
        Label else_nonnull = new Label();
        mg.visitJumpInsn(GOTO, else_nonnull);

        // -----------------------------------------------------------------------
        //     (class$com$ibm$example$Test = class$("com.ibm.example.Test"));
        // -----------------------------------------------------------------------
        mg.visitLabel(if_null_then);
        mg.visitLdcInsn(classConstant.getName());
        mg.visitMethodInsn(INVOKESTATIC, className, "class$",
                           "(Ljava/lang/String;)Ljava/lang/Class;");
        mg.visitInsn(DUP);
        mg.visitFieldInsn(PUTSTATIC, className, fieldName, "Ljava/lang/Class;");

        mg.visitLabel(else_nonnull);
    }

    /**
     * Add synthetic fields required by {@link #loadClassConstant}.
     * 
     * @param cv The current class visitor.
     * @param classConstantFieldNames The set of class constant field names.
     */
    public static void addClassConstantMembers(ClassVisitor cv,
                                               Set<String> classConstantFieldNames)
    {
        if (!classConstantFieldNames.isEmpty())
        {
            addClassConstantMethod(cv);

            // Generate synthetic fields for each of class constants.          d676434
            for (String fieldName : classConstantFieldNames)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, INDENT + "adding class constant field : " + fieldName);

                FieldVisitor fv = cv.visitField(ACC_STATIC + ACC_SYNTHETIC, fieldName, "Ljava/lang/Class;", null, null);
                fv.visitEnd();
            }
        }
    }

    /**
     * Adds the class$ method needed to load class constants.
     * 
     * @param cv The current class visitor.
     */
    private static void addClassConstantMethod(ClassVisitor cv) // RTC111522
    {
        // -----------------------------------------------------------------------
        // static synthetic Class class$(String className)
        // {
        //   try
        //   {
        //     return Class.forName( className );
        //   }
        //   catch ( ClassNotFoundException cnfex )
        //   {
        //     throw new NoClassDefFoundError(cnfex.getMessage());
        //   }
        // }
        // -----------------------------------------------------------------------
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, INDENT + "adding method : class$ (Ljava/lang/String;)Ljava/lang/Class;");

        MethodVisitor mv;
        mv = cv.visitMethod(ACC_STATIC + ACC_SYNTHETIC, "class$",
                            "(Ljava/lang/String;)Ljava/lang/Class;", null, null);
        mv.visitCode();
        Label try_begin = new Label();
        mv.visitLabel(try_begin);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName",
                           "(Ljava/lang/String;)Ljava/lang/Class;");
        mv.visitInsn(ARETURN);
        Label try_end = new Label();
        mv.visitLabel(try_end);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitTypeInsn(NEW, "java/lang/NoClassDefFoundError");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Throwable", "getMessage",
                           "()Ljava/lang/String;");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NoClassDefFoundError",
                           "<init>", "(Ljava/lang/String;)V");
        mv.visitInsn(ATHROW);
        mv.visitTryCatchBlock(try_begin, try_end, try_end, "java/lang/ClassNotFoundException");
        mv.visitMaxs(3, 2);
        mv.visitEnd();
    }

    /**
     * Writes the in memory bytecode bytearray for a generated class
     * out to a .class file with the correct class name and in the
     * correct package directory structure. <p>
     * 
     * This method is useful for debug, to determine if classes
     * are being generated properly, and may also be useful
     * when it is required to make the classes available on a
     * client without using the Remote ByteCode Server. <p>
     * 
     * @param internalClassName fully qualified name of the class
     *            with '/' as the separator.
     * @param classBytes bytearray of the class bytecodes.
     **/
    static void writeToClassFile(final String internalClassName,
                                 final byte[] classBytes)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "writeToClassFile (" + internalClassName + ", " +
                         ((classBytes == null) ? "null"
                                         : (classBytes.length + " bytes")) + ")");
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws Exception {
                    String fileName = getLogLocation() + JIT_DEPLOY_DIR + internalClassName + ".class";
                    File file = new File(fileName);
                    File directory = file.getParentFile();
                    directory.mkdirs();
                    FileOutputStream classFile = new FileOutputStream(file);
                    classFile.write(classBytes);
                    classFile.flush();
                    classFile.close();
                    return null;
                }
            });

        } catch (PrivilegedActionException paex) {
            FFDCFilter.processException(paex.getCause(), CLASS_NAME + ".writeToClassFile", "674");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "writeToClassFile failed for class " + internalClassName +
                             " : " + paex.getCause().getMessage());
        } catch (Throwable ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".writeToClassFile", "463");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "writeToClassFile failed for class " + internalClassName +
                             " : " + ex.getMessage());
        }
    }

}
