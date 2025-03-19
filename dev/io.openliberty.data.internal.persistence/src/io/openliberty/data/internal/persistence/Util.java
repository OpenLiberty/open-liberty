/*******************************************************************************
 * Copyright (c) 2024,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

/**
 * A location for helper methods that do not require any state.
 */
public class Util {
    /**
     * End of line character(s).
     */
    public static final String EOLN = String.format("%n");

    /**
     * Type of life cycle methods (by annotation name) that are capable of
     * returning entities.
     */
    static final List<String> LIFE_CYCLE_METHODS_THAT_RETURN_ENTITIES = //
                    List.of(Insert.class.getSimpleName(),
                            Save.class.getSimpleName(),
                            Update.class.getSimpleName());

    /**
     * Commonly used result types that are not entities.
     */
    static final Set<Class<?>> NON_ENTITY_RESULT_TYPES = new HashSet<>();

    /**
     * Primitive types for numeric values.
     */
    static final Set<Class<?>> PRIMITIVE_NUMERIC_TYPES = //
                    Set.of(long.class, int.class, short.class, byte.class,
                           double.class, float.class);

    /**
     * List of Jakarta Data operation annotations for use in NLS messages.
     */
    static final String OP_ANNOS = "Delete, Find, Insert, Query, Save, Update";

    /**
     * Return types for deleteBy that distinguish delete-only from find-and-delete.
     */
    static final Set<Class<?>> RETURN_TYPES_FOR_DELETE_ONLY = //
                    Set.of(void.class, Void.class,
                           boolean.class, Boolean.class,
                           int.class, Integer.class,
                           long.class, Long.class,
                           Number.class);

    /**
     * Valid types for repository method parameters that specify sort criteria.
     */
    static final Set<Class<?>> SORT_PARAM_TYPES = //
                    Set.of(Order.class, Sort.class, Sort[].class);

    /**
     * These types are never supported for entity attributes.
     *
     * ZonedDateTime is not one of the supported Temporal types of Jakarta Data
     * or Jakarta Persistence, and it does not behave correctly in EclipseLink,
     * where we have observed it reading back a different value from the database
     * than was persisted. If proper support is added for it in the future,
     * then this restriction against using it can be made version dependent.
     */
    static final Set<Class<?>> UNSUPPORTED_ATTR_TYPES = //
                    Set.of(Byte[].class, // deprecated in JPA 3.2
                           Character[].class, // deprecated in JPA 3.2
                           java.sql.Date.class, // deprecated in JPA 3.2
                           java.sql.Time.class, // deprecated in JPA 3.2
                           java.sql.Timestamp.class, // deprecated in JPA 3.2
                           java.util.Calendar.class, // deprecated in JPA 3.2
                           java.util.Date.class, // deprecated in JPA 3.2
                           ZonedDateTime.class); // would be useful if it worked

    /**
     * Valid types for when a repository method computes an update count
     */
    static final Set<Class<?>> UPDATE_COUNT_TYPES = //
                    Set.of(boolean.class, Boolean.class,
                           int.class, Integer.class,
                           long.class, Long.class,
                           void.class, Void.class,
                           Number.class);

    /**
     * Valid types for jakarta.persistence.Version, except for
     * java.sql.Timestamp, which is not a valid type in Jakarta Data.
     */
    public static final Set<Class<?>> VERSION_TYPES = //
                    Set.of(Instant.class,
                           int.class, Integer.class,
                           LocalDateTime.class,
                           long.class, Long.class,
                           short.class, Short.class);

    /**
     * Mapping of Java primitive class to wrapper class.
     */
    private static final Map<Class<?>, Class<?>> WRAPPER_CLASSES = //
                    Map.of(boolean.class, Boolean.class,
                           byte.class, Byte.class,
                           char.class, Character.class,
                           double.class, Double.class,
                           float.class, Float.class,
                           int.class, Integer.class,
                           long.class, Long.class,
                           short.class, Short.class,
                           void.class, Void.class);

    /**
     * Alphabetize properties to make them more readable when debugging.
     *
     * @param props key/value pairs
     * @return sorted map
     */
    public static SortedMap<String, Object> alphabetize(Dictionary<String, Object> props) {
        SortedMap<String, Object> sorted = new TreeMap<>();
        for (Enumeration<String> keys = props.keys(); keys.hasMoreElements();) {
            String key = keys.nextElement();
            sorted.put(key, props.get(key));
        }
        return sorted;
    }

    /**
     * Returns true if it is certain the class cannot be an entity
     * because it is one of the common non-entity result types
     * or it is an enumeration, interface, or abstract class.
     * Otherwise, returns false.
     *
     * @param c class of result.
     * @return true if a result of the type might be an entity, otherwise false.
     */
    @Trivial
    public static boolean cannotBeEntity(Class<?> c) {
        int modifiers;
        return NON_ENTITY_RESULT_TYPES.contains(c) ||
               c.isEnum() ||
               Modifier.isInterface(modifiers = c.getModifiers()) ||
               Modifier.isAbstract(modifiers);
    }

    /**
     * Identifies whether a method is annotated with a Jakarta Data annotation
     * that performs and operation, such as Query, Find, or Save. This method is
     * for use by error reporting only, so it does not need to be very efficient.
     *
     * @param method repository method.
     * @return if the repository method has an annotation indicating an operation.
     */
    @Trivial
    static final boolean hasOperationAnno(Method method) {
        Set<Class<? extends Annotation>> operationAnnos = //
                        Set.of(Delete.class,
                               Insert.class,
                               Find.class,
                               Query.class,
                               Save.class,
                               Update.class);

        for (Annotation anno : method.getAnnotations())
            if (operationAnnos.contains(anno.annotationType()))
                return true;

        return false;
    }

    /**
     * Indicates if the specified class is a wrapper for the primitive class.
     *
     * @param primitive primitive class.
     * @param cl        another class that might be a wrapper class for the primitive class.
     * @return true if the class is the wrapper class for the primitive class, otherwise false.
     */
    static final boolean isWrapperClassFor(Class<?> primitive, Class<?> cl) {
        return primitive == long.class && cl == Long.class ||
               primitive == int.class && cl == Integer.class ||
               primitive == float.class && cl == Float.class ||
               primitive == double.class && cl == Double.class ||
               primitive == char.class && cl == Character.class ||
               primitive == byte.class && cl == Byte.class ||
               primitive == boolean.class && cl == Boolean.class ||
               primitive == short.class && cl == Short.class;
    }

    static {
        for (Entry<Class<?>, Class<?>> e : WRAPPER_CLASSES.entrySet()) {
            NON_ENTITY_RESULT_TYPES.add(e.getKey()); // primitive classes
            NON_ENTITY_RESULT_TYPES.add(e.getValue()); // wrapper classes
        }
        NON_ENTITY_RESULT_TYPES.add(BigDecimal.class);
        NON_ENTITY_RESULT_TYPES.add(BigInteger.class);
        NON_ENTITY_RESULT_TYPES.add(Object.class);
        NON_ENTITY_RESULT_TYPES.add(String.class);
    }

    /**
     * Returns some of the more commonly used return types that are valid
     * for a life cycle method.
     *
     * @param singularClassName        simple class name of the entity
     * @param hasSingularEntityParam   if the life cycle method entity parameter is
     *                                     singular (not an Iterable or array)
     * @param includeBooleanAndNumeric whether to include boolean and numeric types
     *                                     as valid.
     * @return some of the more commonly used return types that are valid for a
     *         life cycle method.
     */
    static List<String> lifeCycleReturnTypes(String singularClassName,
                                             boolean hasSingularEntityParam,
                                             boolean includeBooleanAndNumeric) {
        List<String> validReturnTypes = new ArrayList<>();
        if (includeBooleanAndNumeric) {
            validReturnTypes.add("boolean");
            validReturnTypes.add("int");
            validReturnTypes.add("long");
        }

        validReturnTypes.add("void");

        if (hasSingularEntityParam) {
            validReturnTypes.add(singularClassName);
        } else {
            validReturnTypes.add(singularClassName + "[]");
            validReturnTypes.add("List<" + singularClassName + ">");
        }

        return validReturnTypes;
    }

    /**
     * Print the string, adding indentation after end-of-line characters that are
     * within the string. Indentation is not added before the first line.
     *
     * @param s      string to print, which might have end-of-line characters.
     * @param writer writer for output.
     * @param indent indentation for lines.
     */
    @Trivial
    public static void printlnIndented(String s, PrintWriter writer, String indent) {
        if (s == null) {
            writer.println("null");
            return;
        }
        int start = 0, eoln;
        while ((eoln = s.indexOf(EOLN, start)) >= 0) {
            writer.print(s.substring(start, eoln));
            writer.println();
            writer.print(indent);
            start = eoln + EOLN.length();
        }
        writer.println(s.substring(start, s.length()));
    }

    /**
     * Print the exception, its stack, and causes to the specified writer.
     *
     * @param x                 the exception or error.
     * @param writer            writer for output.
     * @param indent            indentation for lines.
     * @param suppressedIgnores exceptions/errors already printed that should be
     *                              ignored when printing a suppressed Throwable.
     *                              Null if not printing a suppressed Throwable.
     *                              This method adds to the set.
     */
    @Trivial
    public static void printStackTrace(Throwable x,
                                       PrintWriter writer,
                                       String indent,
                                       Set<Throwable> suppressedIgnores) {
        Set<Throwable> alreadyPrinted = suppressedIgnores == null //
                        ? new HashSet<>() //
                        : suppressedIgnores;

        for (Throwable cause = x; cause != null; cause = cause.getCause()) {
            if (alreadyPrinted.add(cause)) {
                writer.print(indent);
                if (cause != x)
                    writer.print("Caused by: ");
                else if (suppressedIgnores != null)
                    writer.print("Suppressed: ");

                printlnIndented(cause.toString(), writer, indent + "  ");

                for (StackTraceElement e : cause.getStackTrace())
                    writer.println(indent + "  at " + e.toString());

                for (Throwable suppressed : x.getSuppressed())
                    printStackTrace(suppressed, writer, indent + "  ", alreadyPrinted);
            } else {
                writer.println(indent + "[CIRCULAR REFERENCE: " +
                               cause.getClass().getName() + ']');
            }
        }
    }

    /**
     * Returns a textual representation of the annotation, omitting parts
     * that can be assumed. This helps make the introspector output more
     * concise and less cluttered.
     *
     * @param anno annotation.
     * @return a shortened textual representation of the annotation.
     */
    @Trivial
    private static String toString(Annotation anno) {
        String s = anno.toString();

        int openParen = s.indexOf('(');
        int dot = openParen > 0 ? s.lastIndexOf('.', openParen) : -1;
        int end = s.length() - (s.endsWith("()") ? 2 : 0);

        // omit jakarta data package names and any ending ()
        if (dot > 0 && s.startsWith("@jakarta.data."))
            s = '@' + s.substring(dot + 1, end);
        else
            s = s.substring(0, end);

        return s;
    }

    /**
     * String representation of a class, for logging to trace or introspector output.
     *
     * @param c      generated entity class.
     * @param indent indentation for lines.
     * @return textual representation.
     */
    @Trivial
    public static String toString(Class<?> c, String indent) {
        final String className_ = c.getName() + '.';
        final String packageName_ = c.getPackage().getName() + '.';

        Function<String, String> shorten = str -> {
            return str.replace(className_, "") // omit from every method
                            .replace(packageName_, ""); // omit from type params
        };

        StringBuilder s = new StringBuilder(1000);
        for (Annotation anno : c.getDeclaredAnnotations())
            s.append(indent).append(toString(anno)).append(EOLN);
        s.append(indent).append(c.toGenericString());

        RecordComponent[] components = c.getRecordComponents();
        if (components != null) {
            s.append('(');
            for (int rc = 0; rc < components.length; rc++) {
                if (rc > 0)
                    s.append(", ");
                s.append(shorten.apply(components[rc].getGenericType().getTypeName())) //
                                .append(' ') //
                                .append(components[rc].getName());
            }
            s.append(')');
        }

        Type supertype = c.getGenericSuperclass();
        if (supertype != null)
            s.append(" extends ").append(shorten.apply(supertype.getTypeName()));

        Type[] interfaces = c.getGenericInterfaces();
        if (interfaces != null && interfaces.length > 0) {
            s.append(c.isInterface() ? " extends" : " implements");
            for (int i = 0; i < interfaces.length; i++)
                s.append(i == 0 ? " " : ", ") //
                                .append(shorten.apply(interfaces[i].getTypeName()));
        }

        s.append(" {").append(EOLN);

        // inner classes
        for (Class<?> inner : c.getDeclaredClasses())
            s.append(EOLN).append(toString(inner, indent + "  ")).append(EOLN);

        // fields
        TreeMap<String, Field> fields = new TreeMap<>();
        for (Field f : c.getFields())
            fields.put(f.getName(), f);
        for (Field f : fields.values()) {
            s.append(EOLN);
            for (Annotation anno : f.getDeclaredAnnotations())
                s.append(indent).append("  ").append(toString(anno)).append(EOLN);
            s.append(indent).append("  ") //
                            .append(shorten.apply(f.toGenericString())) //
                            .append(';').append(EOLN);
        }

        // constructors
        TreeMap<String, Constructor<?>> ctors = new TreeMap<>();
        for (Constructor<?> ctor : c.getConstructors())
            ctors.put(ctor.getName(), ctor);
        for (Constructor<?> ctor : ctors.values()) {
            s.append(EOLN);
            toStringAppend(ctor, shorten, indent + "  ", s);
        }

        // methods
        TreeMap<String, Method> methods = new TreeMap<>();
        for (Method m : c.getMethods())
            if (!Object.class.equals(m.getDeclaringClass()))
                methods.put(m.getName(), m);
        for (Method m : methods.values()) {
            s.append(EOLN);
            toStringAppend(m, shorten, indent + "  ", s);
        }

        s.append(indent).append('}');
        return s.toString();
    }

    /**
     * Append a textual representation of a method or constructor,
     * including annotations. This method is intended for producing
     * trace and introspector output.
     *
     * @param m       method or constructor.
     * @param shorten shortens the representation of a method or constructor.
     * @param indent  indentation for lines.
     * @param b       string builder to which to append.
     */
    @Trivial
    private static void toStringAppend(Executable m,
                                       Function<String, String> shorten,
                                       String indent,
                                       StringBuilder b) {
        // method or constructor annotations first:
        for (Annotation anno : m.getDeclaredAnnotations())
            b.append(indent).append(toString(anno)).append(EOLN);
        String s = shorten.apply(m.toGenericString());
        // insert parameter annotations because they are absent from the above
        Annotation[][] paramAnnos = m.getParameterAnnotations();
        if (paramAnnos.length == 0) {
            b.append(indent).append(s).append(EOLN);
        } else {
            int paramStart = s.indexOf('(') + 1; // first method parameter
            b.append(indent).append(s.substring(0, paramStart));
            for (int a = 0; a < paramAnnos.length; a++) {
                for (Annotation anno : paramAnnos[a])
                    b.append(toString(anno)).append(' ');
                int paramNext = s.indexOf(',', paramStart);
                paramNext = paramNext == -1 ? s.length() : paramNext + 1;
                b.append(s.substring(paramStart, paramNext)).append(' ');
                paramStart = paramNext;
            }
            b.append(EOLN);
        }
    }

    /**
     * Returns the wrapper class if a primitive class, otherwise the same class.
     *
     * @param c class that is possibly a primitive class.
     * @return wrapper class for a primitive, otherwise the same class that was supplied as a parameter.
     */
    @Trivial
    static final Class<?> wrapperClassIfPrimitive(Class<?> c) {
        Class<?> w = WRAPPER_CLASSES.get(c);
        return w == null ? c : w;
    }
}
