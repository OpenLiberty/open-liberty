/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates and others.
 * All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jakarta.el;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Utility methods for this portion of the Jakarta Expression Language implementation
 *
 * <p>
 * Methods on this class use a Map instance stored in ThreadLocal storage to minimize the performance impact on
 * operations that take place multiple times on a single Thread. The keys and values of the Map are implementation
 * private.
 *
 * @author edburns
 * @author Kin-man Chung
 * @author Dongbin Nie
 */
class ELUtil {

    /**
     * This class may not be constructed.
     */
    private ELUtil() {
    }

    /*
     * For testing Backward Compatibility option static java.util.Properties properties = new java.util.Properties(); static
     * { properties.setProperty("jakarta.el.bc2.2", "true"); }
     */
    public static ExpressionFactory exprFactory = ExpressionFactory.newInstance(/* properties */);

    /**
     * <p>
     * The <code>ThreadLocal</code> variable used to record the <code>jakarta.faces.context.FacesContext</code> instance for
     * each processing thread.
     * </p>
     */
    private static ThreadLocal<Map<String, ResourceBundle>> instance = new ThreadLocal<Map<String, ResourceBundle>>() {
        @Override
        protected Map<String, ResourceBundle> initialValue() {
            return (null);
        }
    };

    /**
     * @return a Map stored in ThreadLocal storage. This may be used by methods of this class to minimize the performance
     * impact for operations that may take place multiple times on a given Thread instance.
     */
    private static Map<String, ResourceBundle> getCurrentInstance() {
        Map<String, ResourceBundle> result = instance.get();
        if (result == null) {
            result = new HashMap<>();
            setCurrentInstance(result);
        }

        return result;

    }

    /**
     * Replace the Map with the argument context.
     *
     * @param context the Map to be stored in ThreadLocal storage.
     */
    private static void setCurrentInstance(Map<String, ResourceBundle> context) {
        instance.set(context);
    }

    /**
     * Convenience method, calls through to getExceptionMessageString(ELContext,java.lang.String,Object []).
     *
     * @param context the ELContext from which the Locale for this message is extracted.
     * @param messageId the messageId String in the ResourceBundle
     *
     * @return a localized String for the argument messageId
     */
    public static String getExceptionMessageString(ELContext context, String messageId) {
        return getExceptionMessageString(context, messageId, null);
    }

    /*
     * <p>Return a Localized message String suitable for use as an Exception message. Examine the argument
     * <code>context</code> for a <code>Locale</code>. If not present, use <code>Locale.getDefault()</code>. Load the
     * <code>ResourceBundle</code> "jakarta.el.Messages" using that locale. Get the message string for argument
     * <code>messageId</code>. If not found return "Missing Resource in Jakarta Expression Language implementation ??? messageId ???" with messageId
     * substituted with the runtime value of argument <code>messageId</code>. If found, and argument <code>params</code> is
     * non-null, format the message using the params. If formatting fails, return a sensible message including the
     * <code>messageId</code>. If argument <code>params</code> is <code>null</code>, skip formatting and return the message
     * directly, otherwise return the formatted message.</p>
     *
     * @param context the ELContext from which the Locale for this message is extracted.
     *
     * @param messageId the messageId String in the ResourceBundle
     *
     * @param params parameters to the message
     *
     * @return a localized String for the argument messageId
     */
    public static String getExceptionMessageString(ELContext context, String messageId, Object[] params) {
        String result = "";
        Locale locale = null;

        if (null == context || null == messageId) {
            return result;
        }

        if (null == (locale = context.getLocale())) {
            locale = Locale.getDefault();
        }

        if (locale != null) {
            Map<String, ResourceBundle> threadMap = getCurrentInstance();
            ResourceBundle resourceBundle = null;
            if (null == (resourceBundle = threadMap.get(locale.toString()))) {
                resourceBundle = ResourceBundle.getBundle("jakarta.el.PrivateMessages", locale);
                threadMap.put(locale.toString(), resourceBundle);
            }

            if (null != resourceBundle) {
                try {
                    result = resourceBundle.getString(messageId);
                    if (null != params) {
                        result = MessageFormat.format(result, params);
                    }
                } catch (IllegalArgumentException iae) {
                    result = "Can't get localized message: parameters to message appear to be incorrect.  Message to format: " + messageId;
                } catch (MissingResourceException mre) {
                    result = "Missing Resource in Jakarta Expression Language implementation: ???" + messageId + "???";
                } catch (Exception e) {
                    result = "Exception resolving message in Jakarta Expression Language implementation: ???" + messageId + "???";
                }
            }
        }

        return result;
    }

    static ExpressionFactory getExpressionFactory() {
        return exprFactory;
    }

    static Constructor<?> findConstructor(Class<?> klass, Class<?>[] paramTypes, Object[] params) {
        String methodName = "<init>";

        if (klass == null) {
            throw new MethodNotFoundException("Method not found: " + klass + "." + methodName + "(" + paramString(paramTypes) + ")");
        }

        if (paramTypes == null) {
            paramTypes = getTypesFromValues(params);
        }

        Constructor<?>[] constructors = klass.getConstructors();

        List<Wrapper> wrappers = Wrapper.wrap(constructors);

        Wrapper result = findWrapper(klass, wrappers, methodName, paramTypes, params);

        if (result == null) {
            return null;
        }

        return getConstructor(klass, (Constructor<?>) result.unWrap());
    }

    static Object invokeConstructor(ELContext context, Constructor<?> constructor, Object[] params) {
        Object[] parameters = buildParameters(context, constructor.getParameterTypes(), constructor.isVarArgs(), params);
        try {
            return constructor.newInstance(parameters);
        } catch (IllegalAccessException iae) {
            throw new ELException(iae);
        } catch (IllegalArgumentException iae) {
            throw new ELException(iae);
        } catch (InvocationTargetException ite) {
            throw new ELException(ite.getCause());
        } catch (InstantiationException ie) {
            throw new ELException(ie.getCause());
        }
    }

    static Method findMethod(Class<?> klass, String methodName, Class<?>[] paramTypes, Object[] params, boolean staticOnly) {
        Method method = findMethod(klass, methodName, paramTypes, params);
        if (staticOnly && !Modifier.isStatic(method.getModifiers())) {
            throw new MethodNotFoundException("Method " + methodName + "for class " + klass + " not found or accessible");
        }

        return method;
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    static Object invokeMethod(ELContext context, Method method, Object base, Object[] params) {

        Object[] parameters = buildParameters(context, method.getParameterTypes(), method.isVarArgs(), params);
        try {
            return method.invoke(base, parameters);
        } catch (IllegalAccessException iae) {
            throw new ELException(iae);
        } catch (IllegalArgumentException iae) {
            throw new ELException(iae);
        } catch (InvocationTargetException ite) {
            throw new ELException(ite.getCause());
        }
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    static Method findMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes, Object[] paramValues) {
        if (clazz == null || methodName == null) {
            throw new MethodNotFoundException("Method not found: " + clazz + "." + methodName + "(" + paramString(paramTypes) + ")");
        }

        if (paramTypes == null) {
            paramTypes = getTypesFromValues(paramValues);
        }

        Method[] methods = clazz.getMethods();

        List<Wrapper> wrappers = Wrapper.wrap(methods, methodName);

        Wrapper result = findWrapper(clazz, wrappers, methodName, paramTypes, paramValues);

        if (result == null) {
            return null;
        }

        return getMethod(clazz, (Method) result.unWrap());
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    @SuppressWarnings("null")
    private static Wrapper findWrapper(Class<?> clazz, List<Wrapper> wrappers, String name, Class<?>[] paramTypes, Object[] paramValues) {
        List<Wrapper> assignableCandidates = new ArrayList<>();
        List<Wrapper> coercibleCandidates = new ArrayList<>();
        List<Wrapper> varArgsCandidates = new ArrayList<>();

        int paramCount;
        if (paramTypes == null) {
            paramCount = 0;
        } else {
            paramCount = paramTypes.length;
        }

        for (Wrapper w : wrappers) {
            Class<?>[] mParamTypes = w.getParameterTypes();
            int mParamCount;
            if (mParamTypes == null) {
                mParamCount = 0;
            } else {
                mParamCount = mParamTypes.length;
            }

            // Check the number of parameters
            if (!(paramCount == mParamCount || (w.isVarArgs() && paramCount >= mParamCount - 1))) {
                // Method has wrong number of parameters
                continue;
            }

            // Check the parameters match
            boolean assignable = false;
            boolean coercible = false;
            boolean varArgs = false;
            boolean noMatch = false;
            for (int i = 0; i < mParamCount; i++) {
                if (i == (mParamCount - 1) && w.isVarArgs()) {
                    varArgs = true;
                    // exact var array type match
                    if (mParamCount == paramCount) {
                        if (mParamTypes[i] == paramTypes[i]) {
                            continue;
                        }
                    }

                    // unwrap the array's component type
                    Class<?> varType = mParamTypes[i].getComponentType();
                    for (int j = i; j < paramCount; j++) {
                        if (!isAssignableFrom(paramTypes[j], varType)
                                && !(paramValues != null && j < paramValues.length && isCoercibleFrom(paramValues[j], varType))) {
                            noMatch = true;
                            break;
                        }
                    }
                } else if (mParamTypes[i].equals(paramTypes[i])) {
                } else if (isAssignableFrom(paramTypes[i], mParamTypes[i])) {
                    assignable = true;
                } else {
                    if (paramValues == null || i >= paramValues.length) {
                        noMatch = true;
                        break;
                    } else {
                        if (isCoercibleFrom(paramValues[i], mParamTypes[i])) {
                            coercible = true;
                        } else {
                            noMatch = true;
                            break;
                        }
                    }
                }
            }
            if (noMatch) {
                continue;
            }

            if (varArgs) {
                varArgsCandidates.add(w);
            } else if (coercible) {
                coercibleCandidates.add(w);
            } else if (assignable) {
                assignableCandidates.add(w);
            } else {
                // If a method is found where every parameter matches exactly,
                // return it
                return w;
            }

        }

        String errorMsg = "Unable to find unambiguous method: " + clazz + "." + name + "(" + paramString(paramTypes) + ")";
        if (!assignableCandidates.isEmpty()) {
            return findMostSpecificWrapper(assignableCandidates, paramTypes, false, errorMsg);
        } else if (!coercibleCandidates.isEmpty()) {
            return findMostSpecificWrapper(coercibleCandidates, paramTypes, true, errorMsg);
        } else if (!varArgsCandidates.isEmpty()) {
            return findMostSpecificWrapper(varArgsCandidates, paramTypes, true, errorMsg);
        } else {
            throw new MethodNotFoundException("Method not found: " + clazz + "." + name + "(" + paramString(paramTypes) + ")");
        }

    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static Wrapper findMostSpecificWrapper(List<Wrapper> candidates, Class<?>[] matchingTypes, boolean elSpecific, String errorMsg) {
        List<Wrapper> ambiguouses = new ArrayList<>();
        for (Wrapper candidate : candidates) {
            boolean lessSpecific = false;

            Iterator<Wrapper> it = ambiguouses.iterator();
            while (it.hasNext()) {
                int result = isMoreSpecific(candidate, it.next(), matchingTypes, elSpecific);
                if (result == 1) {
                    it.remove();
                } else if (result == -1) {
                    lessSpecific = true;
                }
            }

            if (!lessSpecific) {
                ambiguouses.add(candidate);
            }
        }

        if (ambiguouses.size() > 1) {
            throw new MethodNotFoundException(errorMsg);
        }

        return ambiguouses.get(0);
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static int isMoreSpecific(Wrapper wrapper1, Wrapper wrapper2, Class<?>[] matchingTypes, boolean elSpecific) {
        Class<?>[] paramTypes1 = wrapper1.getParameterTypes();
        Class<?>[] paramTypes2 = wrapper2.getParameterTypes();

        if (wrapper1.isVarArgs()) {
            // JLS8 15.12.2.5 Choosing the Most Specific Method
            int length = Math.max(Math.max(paramTypes1.length, paramTypes2.length), matchingTypes.length);
            paramTypes1 = getComparingParamTypesForVarArgsMethod(paramTypes1, length);
            paramTypes2 = getComparingParamTypesForVarArgsMethod(paramTypes2, length);

            if (length > matchingTypes.length) {
                Class<?>[] matchingTypes2 = new Class<?>[length];
                System.arraycopy(matchingTypes, 0, matchingTypes2, 0, matchingTypes.length);
                matchingTypes = matchingTypes2;
            }
        }

        int result = 0;
        for (int i = 0; i < paramTypes1.length; i++) {
            if (paramTypes1[i] != paramTypes2[i]) {
                int r2 = isMoreSpecific(paramTypes1[i], paramTypes2[i], matchingTypes[i], elSpecific);
                if (r2 == 1) {
                    if (result == -1) {
                        return 0;
                    }
                    result = 1;
                } else if (r2 == -1) {
                    if (result == 1) {
                        return 0;
                    }
                    result = -1;
                } else {
                    return 0;
                }
            }
        }

        if (result == 0) {
            // The nature of bridge methods is such that it actually
            // doesn't matter which one we pick as long as we pick
            // one. That said, pick the 'right' one (the non-bridge
            // one) anyway.
            result = Boolean.compare(wrapper1.isBridge(), wrapper2.isBridge());
        }

        return result;
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static int isMoreSpecific(Class<?> type1, Class<?> type2, Class<?> matchingType, boolean elSpecific) {
        type1 = getBoxingTypeIfPrimitive(type1);
        type2 = getBoxingTypeIfPrimitive(type2);
        if (type2.isAssignableFrom(type1)) {
            return 1;
        } else if (type1.isAssignableFrom(type2)) {
            return -1;
        } else {
            if (elSpecific) {
                /*
                 * Number will be treated as more specific
                 *
                 * ASTInteger only return Long or BigInteger, no Byte / Short / Integer. ASTFloatingPoint also.
                 *
                 */
                if (matchingType != null && Number.class.isAssignableFrom(matchingType)) {
                    boolean b1 = Number.class.isAssignableFrom(type1) || type1.isPrimitive();
                    boolean b2 = Number.class.isAssignableFrom(type2) || type2.isPrimitive();
                    if (b1 && !b2) {
                        return 1;
                    } else if (b2 && !b1) {
                        return -1;
                    } else {
                        return 0;
                    }
                }

                return 0;
            } else {
                return 0;
            }
        }
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static Class<?> getBoxingTypeIfPrimitive(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            if (clazz == Boolean.TYPE) {
                return Boolean.class;
            }
            if (clazz == Character.TYPE) {
                return Character.class;
            }
            if (clazz == Byte.TYPE) {
                return Byte.class;
            }
            if (clazz == Short.TYPE) {
                return Short.class;
            }
            if (clazz == Integer.TYPE) {
                return Integer.class;
            }
            if (clazz == Long.TYPE) {
                return Long.class;
            }
            if (clazz == Float.TYPE) {
                return Float.class;
            }

            return Double.class;
        } else {
            return clazz;
        }
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static Class<?>[] getComparingParamTypesForVarArgsMethod(Class<?>[] paramTypes, int length) {
        Class<?>[] result = new Class<?>[length];
        System.arraycopy(paramTypes, 0, result, 0, paramTypes.length - 1);
        Class<?> type = paramTypes[paramTypes.length - 1].getComponentType();
        for (int i = paramTypes.length - 1; i < length; i++) {
            result[i] = type;
        }

        return result;
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static final String paramString(Class<?>[] types) {
        if (types != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < types.length; i++) {
                if (types[i] == null) {
                    sb.append("null, ");
                } else {
                    sb.append(types[i].getName()).append(", ");
                }
            }
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2);
            }
            return sb.toString();
        }
        return null;
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    static boolean isAssignableFrom(Class<?> src, Class<?> target) {
        // src will always be an object
        // Short-cut. null is always assignable to an object and in Jakarta Expression Language null
        // can always be coerced to a valid value for a primitive
        if (src == null) {
            return true;
        }

        target = getBoxingTypeIfPrimitive(target);

        return target.isAssignableFrom(src);
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static boolean isCoercibleFrom(Object src, Class<?> target) {
        // TODO: This isn't pretty but it works. Significant refactoring would
        // be required to avoid the exception.
        try {
            getExpressionFactory().coerceToType(src, target);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static Class<?>[] getTypesFromValues(Object[] values) {
        if (values == null) {
            return null;
        }

        Class<?> result[] = new Class<?>[values.length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                result[i] = null;
            } else {
                result[i] = values[i].getClass();
            }
        }

        return result;
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     *
     * Get a public method form a public class or interface of a given method. Note that if a PropertyDescriptor is obtained
     * for a non-public class that implements a public interface, the read/write methods will be for the class, and
     * therefore inaccessible. To correct this, a version of the same method must be found in a superclass or interface.
     *
     */
    static Method getMethod(Class<?> type, Method m) {
        if (m == null || Modifier.isPublic(type.getModifiers())) {
            return m;
        }
        Class<?>[] inf = type.getInterfaces();
        Method mp = null;
        for (int i = 0; i < inf.length; i++) {
            try {
                mp = inf[i].getMethod(m.getName(), m.getParameterTypes());
                mp = getMethod(mp.getDeclaringClass(), mp);
                if (mp != null) {
                    return mp;
                }
            } catch (NoSuchMethodException e) {
                // Ignore
            }
        }
        Class<?> sup = type.getSuperclass();
        if (sup != null) {
            try {
                mp = sup.getMethod(m.getName(), m.getParameterTypes());
                mp = getMethod(mp.getDeclaringClass(), mp);
                if (mp != null) {
                    return mp;
                }
            } catch (NoSuchMethodException e) {
                // Ignore
            }
        }
        return null;
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    static Constructor<?> getConstructor(Class<?> type, Constructor<?> c) {
        if (c == null || Modifier.isPublic(type.getModifiers())) {
            return c;
        }
        Constructor<?> cp = null;
        Class<?> sup = type.getSuperclass();
        if (sup != null) {
            try {
                cp = sup.getConstructor(c.getParameterTypes());
                cp = getConstructor(cp.getDeclaringClass(), cp);
                if (cp != null) {
                    return cp;
                }
            } catch (NoSuchMethodException e) {
                // Ignore
            }
        }
        return null;
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    @SuppressWarnings("null")
    static Object[] buildParameters(ELContext context, Class<?>[] parameterTypes, boolean isVarArgs, Object[] params) {
        Object[] parameters = null;
        if (parameterTypes.length > 0) {
            parameters = new Object[parameterTypes.length];
            int paramCount = params == null ? 0 : params.length;
            if (isVarArgs) {
                int varArgIndex = parameterTypes.length - 1;
                // First argCount-1 parameters are standard
                for (int i = 0; (i < varArgIndex && i < paramCount); i++) {
                    parameters[i] = context.convertToType(params[i], parameterTypes[i]);
                }
                // Last parameter is the varargs
                if (parameterTypes.length == paramCount && parameterTypes[varArgIndex] == params[varArgIndex].getClass()) {
                    parameters[varArgIndex] = params[varArgIndex];
                } else {
                    Class<?> varArgClass = parameterTypes[varArgIndex].getComponentType();
                    final Object varargs = Array.newInstance(varArgClass, (paramCount - varArgIndex));
                    for (int i = (varArgIndex); i < paramCount; i++) {
                        Array.set(varargs, i - varArgIndex, context.convertToType(params[i], varArgClass));
                    }
                    parameters[varArgIndex] = varargs;
                }
            } else {
                for (int i = 0; i < parameterTypes.length && i < paramCount; i++) {
                    parameters[i] = context.convertToType(params[i], parameterTypes[i]);
                }
            }
        }
        return parameters;
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private abstract static class Wrapper {

        public static List<Wrapper> wrap(Method[] methods, String name) {
            List<Wrapper> result = new ArrayList<>();
            for (Method method : methods) {
                if (method.getName().equals(name)) {
                    result.add(new MethodWrapper(method));
                }
            }
            return result;
        }

        public static List<Wrapper> wrap(Constructor<?>[] constructors) {
            List<Wrapper> result = new ArrayList<>();
            for (Constructor<?> constructor : constructors) {
                result.add(new ConstructorWrapper(constructor));
            }
            return result;
        }

        public abstract Object unWrap();

        public abstract Class<?>[] getParameterTypes();

        public abstract boolean isVarArgs();

        public abstract boolean isBridge();
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static class MethodWrapper extends Wrapper {
        private final Method m;

        public MethodWrapper(Method m) {
            this.m = m;
        }

        @Override
        public Object unWrap() {
            return m;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return m.getParameterTypes();
        }

        @Override
        public boolean isVarArgs() {
            return m.isVarArgs();
        }

        @Override
        public boolean isBridge() {
            return m.isBridge();
        }
    }

    /*
     * This method duplicates code in com.sun.el.util.ReflectionUtil. When making changes keep the code in sync.
     */
    private static class ConstructorWrapper extends Wrapper {
        private final Constructor<?> c;

        public ConstructorWrapper(Constructor<?> c) {
            this.c = c;
        }

        @Override
        public Object unWrap() {
            return c;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return c.getParameterTypes();
        }

        @Override
        public boolean isVarArgs() {
            return c.isVarArgs();
        }

        @Override
        public boolean isBridge() {
            return false;
        }
    }

}
