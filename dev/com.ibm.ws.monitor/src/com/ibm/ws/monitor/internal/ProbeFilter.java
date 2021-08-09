/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.monitor.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.objectweb.asm.Type;

import com.ibm.websphere.monitor.annotation.ProbeSite;

// Subclasses of java.io.Serializable:
//     +java.io.Serializable
//
// Class or method annotated with an annotation that ends with WebService:
//     @*WebService
//
// Any package qualified class name that starts with com.ibm.websphere:
//     com.ibm.websphere.*
//
// Any package qualified class name that starts with com.ibm.ws and contains
// .internal. in its name:
//     com.ibm.ws.*.internal.*
//
// Any class that starts with com.ibm.websphere or com.ibm.ws but does not contain
// .internal. in its name:
//     ~com\.ibm\.(websphere|ws)\.(?!internal\.)((?!\.internal\.).)*
//
// Any class that implements a request or response:
//     +~javax\.servlet\.http\.(HttpServletRequest|HttpServletResponse)
//
public final class ProbeFilter {

    private final static int CACHE_SIZE = 100;

    final static String ASSIGNABLE_TO_INDICATOR = "+";
    final static String ANNOTATION_INDICATOR = "@";
    final static String REGEX_INDICATOR = "~";

    final static String MATCH_ALL_REGEX = "^.*$";

    final static Pattern MATCH_ALL_PATTERN = Pattern.compile(MATCH_ALL_REGEX);

    final static Map<String, Pattern> patternCache = Collections.synchronizedMap(new LinkedHashMap<String, Pattern>(32, 0.75f, true) {
        private static final long serialVersionUID = 4240783859644144196L;

        protected boolean removeEldestEntry(Map.Entry<String, Pattern> eldest) {
            return size() > CACHE_SIZE;
        }
    });

    final static Map<String, String> globCache = Collections.synchronizedMap(new LinkedHashMap<String, String>(32, 0.75f, true) {
        private static final long serialVersionUID = 4240783859644144196L;

        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > CACHE_SIZE;
        }
    });

    Pattern classNamePattern;
    Pattern classAssignableToPattern;
    Pattern classAnnotationPattern;

    Pattern memberNamePattern;
    Pattern memberAnnotationPattern;

    Pattern methodArgsPattern;

    boolean advancedFilter = false;

    public ProbeFilter(ProbeSite probeSite) {
        this(probeSite.clazz(), probeSite.method(), probeSite.args(), probeSite.bundleName(), probeSite.bundleVersion());
    }

    public ProbeFilter(String clazz, String memberNames, String methodArgs, String bundleName, String bundleVersion) {
        if (clazz.startsWith(ASSIGNABLE_TO_INDICATOR)) {
            classAssignableToPattern = getPatternFromFilter(clazz.substring(1));
        } else if (clazz.startsWith(ANNOTATION_INDICATOR)) {
            classAnnotationPattern = getPatternFromFilter(clazz.substring(1));
        } else {
            classNamePattern = getPatternFromFilter(clazz);
        }
        if (memberNames == null) {
            memberNamePattern = getPatternFromFilter("*");
        } else if (memberNames.startsWith(ANNOTATION_INDICATOR)) {
            memberAnnotationPattern = getPatternFromFilter(memberNames.substring(1));
        } else {
            memberNamePattern = getPatternFromFilter(memberNames);
        }
        methodArgsPattern = getPatternFromFilter(methodArgs == null ? "*" : methodArgs);

        if (classAssignableToPattern != null || classAnnotationPattern != null || memberAnnotationPattern != null) {
            advancedFilter = true;
        }
    }

    static Pattern getPatternFromFilter(String filter) {
        String regex = null;
        if (filter.startsWith(REGEX_INDICATOR)) {
            regex = filter.substring(1);
        } else {
            regex = getRegexFromGlob(filter);
        }

        return getCompiledPattern(regex);
    }

    static String getRegexFromGlob(String glob) {
        String regex = globCache.get(glob);

        if (regex == null) {
            StringBuilder sb = new StringBuilder(glob.length() * 2);
            StringTokenizer tokenizer = new StringTokenizer(glob, "*?", true);

            // Anchor the start of the expression
            sb.append("^");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken();
                if (token.equals("*")) {
                    sb.append(".*");
                } else if (token.equals("?")) {
                    sb.append(".");
                } else {
                    if (!token.isEmpty()) {
                        sb.append(Pattern.quote(token));
                    }
                }
            }
            sb.append("$");
            regex = sb.toString();
            globCache.put(glob, regex);
        }

        return regex;
    }

    static Pattern getCompiledPattern(String regex) {
        Pattern pattern = patternCache.get(regex);
        if (pattern == null) {
            try {
                if (MATCH_ALL_REGEX.equals(regex)) {
                    pattern = MATCH_ALL_PATTERN;
                } else {
                    pattern = Pattern.compile(regex);
                }
            } catch (PatternSyntaxException pse) {
                // TODO: Issue message about invalid / unsupported regex
                pattern = Pattern.compile(Pattern.quote(regex));
            }
            patternCache.put(regex, pattern);
        }
        return pattern;
    }

    static String getArgsDescriptor(Method method) {
        Type[] argTypes = Type.getArgumentTypes(method);
        return buildArgsDescriptor(argTypes);
    }

    static String getArgsDescriptor(Constructor<?> ctor) {
        String desc = Type.getConstructorDescriptor(ctor);
        Type[] argTypes = Type.getArgumentTypes(desc);

        return buildArgsDescriptor(argTypes);
    }

    public static String buildArgsDescriptor(Type[] argTypes) {
        StringBuilder sb = new StringBuilder(100);
        for (int i = 0; i < argTypes.length; i++) {
            sb.append(argTypes[i].getClassName());
            if (i < argTypes.length - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    public boolean matches(Class<?> clazz) {
        if (classNamePattern != null) {
            return patternMatches(classNamePattern, clazz.getName());
        } else if (classAssignableToPattern != null) {
            Class<?> cls = clazz;
            while (cls != null) {
                if (patternMatches(classAssignableToPattern, cls.getName())) {
                    return true;
                }
                Class<?>[] interfaces = ReflectionHelper.getInterfaces(clazz);
                for (Class<?> c : interfaces) {
                    if (patternMatches(classAssignableToPattern, c.getName())) {
                        return true;
                    }
                }
                cls = ReflectionHelper.getSuperclass(cls);
            }
        } else if (classAnnotationPattern != null) {
            return annotationMatches(classAnnotationPattern, clazz);
        }
        return false;
    }

    public boolean matches(Method method) {
        return matches(method, false);
    }

    public boolean matches(Method method, boolean skipDeclaring) {
        if (!skipDeclaring && !matches(method.getDeclaringClass())) {
            return false;
        }
        if (memberNamePattern != null) {
            String methodName = method.getName();
            if (patternMatches(memberNamePattern, methodName)) {
                if (methodArgsPattern == MATCH_ALL_PATTERN) {
                    return true;
                } else {
                    return patternMatches(methodArgsPattern, getArgsDescriptor(method));
                }
            }
        } else if (memberAnnotationPattern != null) {
            if (annotationMatches(memberAnnotationPattern, method)) {
                if (methodArgsPattern == MATCH_ALL_PATTERN) {
                    return true;
                } else {
                    return patternMatches(methodArgsPattern, getArgsDescriptor(method));
                }
            }
        }
        return false;
    }

    public boolean matches(Constructor<?> ctor) {
        return matches(ctor, false);
    }

    public boolean matches(Constructor<?> ctor, boolean skipDeclaring) {
        if (!skipDeclaring && !matches(ctor.getDeclaringClass())) {
            return false;
        }
        if (memberNamePattern != null) {
            final String methodName = "<init>";
            if (patternMatches(memberNamePattern, methodName)) {
                if (methodArgsPattern == MATCH_ALL_PATTERN) {
                    return true;
                } else {
                    return patternMatches(methodArgsPattern, getArgsDescriptor(ctor));
                }
            }
        } else if (memberAnnotationPattern != null) {
            if (annotationMatches(memberAnnotationPattern, ctor)) {
                if (methodArgsPattern == MATCH_ALL_PATTERN) {
                    return true;
                } else {
                    return patternMatches(methodArgsPattern, getArgsDescriptor(ctor));
                }
            }
        }
        return false;
    }

    public boolean matches(Field field) {
        return matches(field, false);
    }

    public boolean matches(Field field, boolean skipDeclaring) {
        if (!skipDeclaring && !matches(field.getDeclaringClass())) {
            return false;
        }
        if (memberNamePattern != null) {
            final String fieldName = field.getName();
            if (patternMatches(memberNamePattern, fieldName)) {
                return true;
            }
        } else if (memberAnnotationPattern != null) {
            if (annotationMatches(memberAnnotationPattern, field)) {
                return true;
            }
        }
        return false;
    }

    boolean annotationMatches(Pattern pattern, AnnotatedElement annotated) {
        Annotation[] annotations = ReflectionHelper.getAnnotations(annotated);
        for (Annotation a : annotations) {
            if (patternMatches(classAnnotationPattern, a.getClass().getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean basicClassNameMatches(String className) {
        // Advanced patterns imply true to cause full evaluation
        if (classNamePattern != null) {
            return patternMatches(classNamePattern, className);
        } else if (classAssignableToPattern != null) {
            return patternMatches(classAssignableToPattern, className);
        }
        return true;
    }

    public boolean basicMemberNameMatches(String memberName) {
        // Advanced patterns imply true to cause full evaluation
        if (memberNamePattern != null) {
            return patternMatches(memberNamePattern, memberName);
        }
        return true;
    }

    public boolean basicArgsDescriptorMatches(String argsDescriptor) {
        if (methodArgsPattern != null) {
            return patternMatches(methodArgsPattern, argsDescriptor);
        }
        return true;
    }

    private static boolean patternMatches(Pattern pattern, String name) {
        // If we're dealing with a pattern that matches everything, don't
        // bother with the expensive regex evaluation.
        if (pattern == MATCH_ALL_PATTERN) {
            return true;
        }

        // Try to avoid expense of greedy tail match where we know
        // anything we throw at it will match.
        boolean useLookingAt = false;
        String regex = pattern.pattern();
        if (regex.endsWith(".*$")) {
            pattern = getCompiledPattern(regex.substring(0, regex.length() - 3));
            useLookingAt = true;
        } else if (regex.endsWith(".*")) {
            pattern = getCompiledPattern(regex.substring(0, regex.length() - 2));
            useLookingAt = true;
        }

        Matcher matcher = pattern.matcher(name);
        if (useLookingAt) {
            return matcher.lookingAt();
        }
        return matcher.matches();
    }

    public boolean isBasicFilter() {
        return !isAdvancedFilter();
    }

    public boolean isAdvancedFilter() {
        return advancedFilter;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(";");
        sb.append("filter=");
        if (classNamePattern != null) {
            sb.append(classNamePattern);
        } else if (classAssignableToPattern != null) {
            sb.append("+").append(classAssignableToPattern);
        } else if (classAnnotationPattern != null) {
            sb.append("@").append(classAnnotationPattern);
        }
        sb.append(" :: ").append(memberNamePattern);
        sb.append(" :: ").append(methodArgsPattern);

        return sb.toString();
    }

    // TODO: Create test cases for this. 
    //    public static void main(String[] args) {
    //
    //        Pattern pattern = getPatternFromFilter("~com\\.ibm\\.(websphere|ws)\\.(?!internal\\.)((?!\\.internal\\.).)*");
    //        for (String test : new String[] {
    //                "java.lang.Object",
    //                "com.ibm.ws.package1.package2.Class",
    //                "com.ibm.ws.package1.internal.package2.Class",
    //                "com.ibm.websphere.threading.WorkManager", 
    //                "com.ibm.websphere.internal.WorkManager.interna",
    //                "com.ibm.websphere.someinternal.WorkManager",
    //                "com.ibm.ws.threading.internal.Work",
    //                "com.ibm.ws.internal.Work",
    //                "com.ibm.ws.internalinternal.Work" }) {
    //            System.out.println(pattern + " matches \"" + test + "\"? " + pattern.matcher(test).matches());
    //            System.out.println("Pattern " + pattern + " matches \"" + test + "\"? " + patternMatches(pattern, test));
    //        }
    //
    //        for (Method m : ReflectionHelper.getDeclaredMethods(ProbeFilter.class)) {
    //            System.out.println("m = " + m);
    //            System.out.println("argsDescriptor(m) = " + getArgsDescriptor(m));
    //        }
    //        for (Constructor<?> ctor : ReflectionHelper.getDeclaredConstructors(ProbeFilter.class)) {
    //            System.out.println("ctor = " + ctor);
    //            System.out.println("argsDescriptor(ctor) = " + getArgsDescriptor(ctor));
    //        }
    //
    //        ProbeFilter filter = new ProbeFilter("com.ibm.ws.threading.*", "*", "*", "com.ibm.ws.threading", "0.0.0");
    //        System.out.println("filter = " + filter);
    //
    //        Pattern p = getPatternFromFilter("~^(?!toString|<init>).*$");
    //        for (String test : new String[] {
    //                "toString",
    //                "<init>",
    //        "executeWork" }) {
    //            System.out.println("Pattern " + p + " matches \"" + test + "\"? " + p.matcher(test).matches());
    //            System.out.println("Pattern " + p + " lookingAt \"" + test + "\"? " + p.matcher(test).lookingAt());
    //            System.out.println("Pattern " + p + " matches \"" + test + "\"? " + patternMatches(p, test));
    //        }
    //
    //        System.out.println(Pattern.compile("foo").matcher("foobar").lookingAt());
    //        System.out.println(patternMatches(Pattern.compile("foo.*"), "foot"));
    //    }

}
