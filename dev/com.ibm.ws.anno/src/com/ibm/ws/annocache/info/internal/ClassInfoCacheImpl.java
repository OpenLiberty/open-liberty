/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.info.internal;

import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Type;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.wsspi.annocache.info.InfoStoreException;
import com.ibm.wsspi.anno.util.Util_InternMap.ValueType;

// FIS F005393

// Creation rules:
//
// ClassInfo objects are created by direct calls from InfoVisitor.
//
// Interfaces of a ClassInfo object are assigned during the visitor
// call as DelayedClassInfo objects.
//
// ArrayClassInfo objects explicitly use "java.lang.Object" as the
// superclass.
//
// All other references to class type objects (one of PrimitiveClassInfo,
// ArrayClassInfo, or DelayedClassInfo) are created dynamically as
// required.
//
// Sequencing:
//
// InfoVisitor.visit, InfoVisitor.visitEnd
//
// InfoVisitor.visit
//   create ClassInfo
//     for each interface, add (or reuse) a DelayedClassInfo
//   add ClassInfo
//
// InfoVisitor.visitEnd
//   per inheritance rules, walk the class information,
//   possibly causing the resolution of delayed class information
//
// LRU updates:
//
//   When adding a class info, move that to the head,
//   and remove any excess class info.
//
//   When creating a DelayedClassInfo, if the class info
//   is live, link the delayed class info to the class info.
//
//   When adding a class info, if the delayed class info is
//   is live, link the delayed class info to the class info.
//
//   When removing a class info, if the delayed class info
//   is live, unlink the delayed class info from the class
//   info.
//
//   When accessing a link from a DelayedClassInfo to a ClassInfo,
//   move the ClassInfo to be first.

public class ClassInfoCacheImpl {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.info");

    private static final String CLASS_NAME = "ClassInfoCacheImpl";

    // Trace point for deliberately discarding a reference.
    public void discardRef(Object objectRef) {
        // NO-OP
    }

    // Top O' the world
    //
    // There are five main linkages:
    //
    // Bind the cache to a class info manager.  Context information
    // is provided by the class info manager.
    //
    // Maintain distinct stores for primitive, java, and delayed
    // class info, as well as the main store of class info.
    //
    // Added state is provided on the main store, which has a maximum
    // allowed size, and a LRU cache policy.
    //
    // Array classes are not cached.

    public ClassInfoCacheImpl(InfoStoreImpl infoStore) {
        super();

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.infoStore = infoStore;

        UtilImpl_Factory utilFactory = (UtilImpl_Factory) infoStore.getInfoStoreFactory().getUtilFactory();

        this.descriptionInternMap = utilFactory.createInternMap(ValueType.VT_OTHER, "DescriptionMap");
        this.packageNameInternMap = utilFactory.createInternMap(ValueType.VT_CLASS_NAME, "PackageNameMap");
        this.classNameInternMap = utilFactory.createInternMap(ValueType.VT_CLASS_NAME, "ClassNameMap");
        this.fieldNameInternMap = utilFactory.createInternMap(ValueType.VT_FIELD_NAME, "FieldNameMap");
        this.methodNameInternMap = utilFactory.createInternMap(ValueType.VT_METHOD_NAME, "MethodNameMap");

        this.packageInfos = new HashMap<String, PackageInfoImpl>();

        this.primitiveClassInfos = new HashMap<String, PrimitiveClassInfoImpl>();
        this.delayedClassInfos = new HashMap<String, DelayedClassInfoImpl>();

        this.javaClassInfos = new HashMap<String, NonDelayedClassInfoImpl>();
        this.annotatedClassInfos = new HashMap<String, NonDelayedClassInfoImpl>();
        this.classInfos = new HashMap<String, NonDelayedClassInfoImpl>();

        this.firstClassInfo = null;
        this.lastClassInfo = null;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Created on manager [ {1} ]",
                        new Object[] { getHashText(), getInfoStore().getHashText() });
        }
    }

    // Debug ...

    protected String hashText;

    protected String getHashText() {
        return hashText;
    }

    // Context binding ...

    protected InfoStoreImpl infoStore;

    public InfoStoreImpl getInfoStore() {
        return infoStore;
    }

    // The info store records delayable and non-delayable classes ...
    // ... meaning the call to 'scanClass' must be used.
    protected void scanClass(String name) throws InfoStoreException {
        getInfoStore().scanClass(name); // 'scanClass' throws InfoStoreException
    }

    // Whereas the info store does not record delayable packages ...
    // ... a direct call to 'scanNewClass' is used.

    protected void scanPackage(String packageClassName) throws InfoStoreException {
        getInfoStore().scanNewClass(packageClassName);
    }

    // Intern reporting ...

    protected UtilImpl_InternMap descriptionInternMap;

    protected UtilImpl_InternMap packageNameInternMap;

    protected UtilImpl_InternMap classNameInternMap;

    protected UtilImpl_InternMap fieldNameInternMap;

    protected UtilImpl_InternMap methodNameInternMap;

    public UtilImpl_InternMap getDescriptionInternMap() {
        return descriptionInternMap;
    }

    public String internDescription(String description) {
        return getDescriptionInternMap().intern(description);
    }

    public UtilImpl_InternMap getPackageNameInternMap() {
        return packageNameInternMap;
    }

    public String internPackageName(String packageName) {
        return getPackageNameInternMap().intern(packageName);
    }

    public UtilImpl_InternMap getClassNameInternMap() {
        return classNameInternMap;
    }

    public String internClassName(String className) {
        return getClassNameInternMap().intern(className);
    }

    public UtilImpl_InternMap getFieldNameInternMap() {
        return fieldNameInternMap;
    }

    public String internFieldName(String fieldName) {
        return getFieldNameInternMap().intern(fieldName);
    }

    public UtilImpl_InternMap getMethodNameInternMap() {
        return methodNameInternMap;
    }

    public String internMethodName(String fieldName) {
        return getMethodNameInternMap().intern(fieldName);
    }

    // Package info ...

    protected Map<String, PackageInfoImpl> packageInfos;

    protected PackageInfoImpl basicGetPackageInfo(String packageName) {
        return packageInfos.get(packageName);
    }

    protected PackageInfoImpl basicAddPackageInfo(String name, int access) {
        PackageInfoImpl packageInfo = new PackageInfoImpl(name, access, getInfoStore());
        packageInfo.setModifiers(access);

        packageInfos.put(name, packageInfo);

        return packageInfo;
    }

    protected static final boolean DO_FORCE_PACKAGE = true;
    protected static final boolean DO_NOT_FORCE_PACKAGE = false;

    protected PackageInfoImpl getPackageInfo(String packageName, boolean doForce) {
        String methodName = "getPackageInfo";

        PackageInfoImpl packageInfo = packageInfos.get(packageName);

        if (packageInfo != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] RETURN [ {1} ] - cached",
                            new Object[] { getHashText(), packageInfo.getHashText() });
            }
            return packageInfo;
        }

        String packageClassName = PackageInfoImpl.addClassNameToPackageName(packageName);

        // 'scanPackage' either successfully scanned the package,
        // or failed to obtain a stream for the package,
        // or failed with an exception, either reading the package,
        // or processing the package data

        // If the load failed, as opposed to simply not locating the package data,
        // force the package info to be created, regardless of 'doForce'.
        //
        // On the other hand, if there was no data to load, only force the package
        // data if explicitly requested via 'doForce'.

        boolean failedLoad;

        try {
            scanPackage(packageClassName);
            failedLoad = false;

        } catch (InfoStoreException e) {
            failedLoad = true;

            // CWWKC0022W            
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
                    "[ {0} ] ANNO_CLASSINFO_SCAN_EXCEPTION [ {1} ] [ {2} ] [ {3} ]",
                    new Object[] { getHashText(),
                                   packageClassName,
                                   e.getMessage(),
                                   ((e.getCause() == null) ? e : e.getCause()) });
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Scan failure", e);
            
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] [ {1} ]: Scan exception: Generating artifical package",
                            new Object[] { getHashText(), packageClassName });
            }
        }

        // In case of a failure, still need to do a safety check for a stored package:
        // Maybe, the failure occurred after the package was added.

        if ((packageInfo = packageInfos.get(packageName)) == null) {
            if (doForce || failedLoad) {
                packageInfo = storeArtificalPackage(packageName, failedLoad);
            }
        }

        if (logger.isLoggable(Level.FINER)) {
            String packageText = ((packageInfo != null) ? packageInfo.getHashText() : null);
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] RETURN [ {1} ] - new ",
                        new Object[] { getHashText(), packageText });
        }
        return packageInfo;
    }

    protected static final boolean FOR_FAILED_LOAD = true;
    protected static final boolean NOT_FOR_FAILED_LOAD = false;

    protected PackageInfoImpl storeArtificalPackage(String packageName, boolean forFailedLoad) {
        PackageInfoImpl packageInfo = new PackageInfoImpl(packageName, 0, getInfoStore());

        packageInfo.setIsArtificial(true);
        packageInfo.setForFailedLoad(forFailedLoad);

        String i_packageName = packageInfo.getName();

        packageInfos.put(i_packageName, packageInfo);

        return packageInfo;
    }

    // TODO: Need to tighten these up

    // Used by fields and methods to retrieve their associated class info.

    // Use the class name as provided by the type.  That is, the actual
    // java class name for non-array, non-primitive types, the element class
    // name plus "[]" for array types, and the primitive class name for
    // java primitive types, one of "void", "boolean", "char", "byte",
    // "short", "int", "float", "long", or "double".
    //
    // Note that these names are changed relative to prior implementations,
    // for array and primitive types.
    //
    // For primitive types, the previous implementation used either the
    // java class name or the java type description for the primitive name.
    //
    // For array types, the previous implementation used the element name.

    protected ClassInfoImpl getDelayableClassInfo(Type type) {
        String methodName = "getDelayableClassInfo";

        String typeClassName = type.getClassName();

        if (logger.isLoggable(Level.FINER)) {
            // Type 'toString' answers the descriptor;
            // show the type class name, too, for clarity.

            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] ENTER [ {1} ] [ {2} ]",
                        new Object[] { getHashText(), type, typeClassName });
        }

        // Now that we have the type class name, pass it along instead
        // of recomputing when needed.

        ClassInfoImpl classInfo;
        String classInfoCase;

        int sort = type.getSort();
        if (sort == Type.ARRAY) {
            classInfo = getArrayClassInfo(typeClassName, type);
            classInfoCase = "array class";

        } else if (sort == Type.OBJECT) {
            classInfo = getDelayableClassInfo(typeClassName, DO_NOT_ALLOW_PRIMITIVE);

            if (classInfo.isJavaClass()) {
                if (classInfo.isDelayedClass()) {
                    classInfoCase = "java delayed";
                } else {
                    classInfoCase = "java non-delayed";
                }
            } else {
                if (classInfo.isDelayedClass()) {
                    classInfoCase = "non-java delayed";
                } else {
                    classInfoCase = "non-java non-delayed";
                }
            }

        } else {
            classInfo = getPrimitiveClassInfo(typeClassName, type);
            classInfoCase = "primitive class";
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] RETURN [ {1} ] [ {2} ]",
                        new Object[] { getHashText(),
                                       classInfo.getHashText(),
                                       classInfoCase });
        }
        return classInfo;
    }

    //

    // ArrayClassInfo is not cached.
    //
    // Note that this will recurse as long as the element type is still an array type.

    public ArrayClassInfoImpl getArrayClassInfo(String typeClassName, Type arrayType) {
        ClassInfoImpl elementClassInfo = getDelayableClassInfo(arrayType.getElementType());

        return new ArrayClassInfoImpl(typeClassName, elementClassInfo);
    }

    // Primitive class management ...

    // Primitive classes are not delayed, but are cached.  (There are a maximum of
    // nine primitive classes.)

    // TODO: Cache these more broadly?

    // This uses the type's descriptor, since that is
    // unambiguous and has a single character for primitive types.
    //
    // The type class name is ambiguous in the first letter, e.g.,
    // '[b]oolean' and '[b]yte'.

    public static Class<?> getPrimitiveClass(Type type) {
        switch (type.getDescriptor().charAt(0)) {
            case 'B':
                return byte.class;
            case 'C':
                return char.class;
            case 'D':
                return double.class;
            case 'F':
                return float.class;
            case 'I':
                return int.class;
            case 'J':
                return long.class;
            case 'S':
                return short.class;
            case 'V':
                return void.class;
            case 'Z':
                return boolean.class;

            default:
                throw new IllegalArgumentException("Unrecognized type [ " + type.getDescriptor() + " ]");
        }
    }

    public static Type getPrimitiveType(String className) {
        if (className.equals("byte")) {
            return Type.BYTE_TYPE;
        } else if (className.equals("char")) {
            return Type.CHAR_TYPE;
        } else if (className.equals("double")) {
            return Type.DOUBLE_TYPE;
        } else if (className.equals("float")) {
            return Type.FLOAT_TYPE;
        } else if (className.equals("int")) {
            return Type.INT_TYPE;
        } else if (className.equals("long")) {
            return Type.LONG_TYPE;
        } else if (className.equals("short")) {
            return Type.SHORT_TYPE;
        } else if (className.equals("void")) {
            return Type.VOID_TYPE;
        } else if (className.equals("boolean")) {
            return Type.BOOLEAN_TYPE;
        } else {
            return null;
        }
    }

    protected HashMap<String, PrimitiveClassInfoImpl> primitiveClassInfos;

    public PrimitiveClassInfoImpl getPrimitiveClassInfo(String typeClassName, Type primitiveType) {
        String methodName = "getPrimitiveClassInfo";

        PrimitiveClassInfoImpl primitiveClassInfo = primitiveClassInfos.get(typeClassName);
        String primitiveClassInfoCase;

        if (primitiveClassInfo == null) {
            Class<?> typeClass = ClassInfoCacheImpl.getPrimitiveClass(primitiveType);

            primitiveClassInfo = new PrimitiveClassInfoImpl(typeClassName, typeClass, getInfoStore());
            primitiveClassInfoCase = "created";

            // Use the interned class name.
            typeClassName = primitiveClassInfo.getName();

            primitiveClassInfos.put(typeClassName, primitiveClassInfo);

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] Primitives cache size [ {1} ]",
                            new Object[] { getHashText(),
                                           Integer.valueOf(primitiveClassInfos.size()) });
            }

        } else {
            primitiveClassInfoCase = "cached";
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] RETURN [ {1} ] [ {2} ]",
                        new Object[] { getHashText(),
                                       primitiveClassInfo.getHashText(),
                                       primitiveClassInfoCase });
        }
        return primitiveClassInfo;
    }

    //

    protected final static boolean DO_ALLOW_PRIMITIVE = true;
    protected final static boolean DO_NOT_ALLOW_PRIMITIVE = false;

    public ClassInfoImpl getDelayableClassInfo(String name, boolean allowPrimitive) {
        String methodName = "getDelayableClassInfo";

        String useHashText = (logger.isLoggable(Level.FINER) ? getHashText() : null);

        if (allowPrimitive) {
            Type primitiveType = ClassInfoCacheImpl.getPrimitiveType(name);

            if (primitiveType != null) {
                PrimitiveClassInfoImpl primitiveClassInfo = getPrimitiveClassInfo(name, primitiveType);

                if (useHashText != null) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                                "[ {0} ] ENTER / RETURN [ {1} ] primitive class",
                                new Object[] { useHashText, primitiveClassInfo.getHashText() });
                }

                return primitiveClassInfo;
            }
        }

        NonDelayedClassInfoImpl javaClassInfo = basicGetJavaClassInfo(name);

        if (javaClassInfo != null) {
            if (useHashText != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] ENTER / RETURN [ {1} ] java class",
                            new Object[] { useHashText, javaClassInfo.getHashText() });
            }

            return javaClassInfo;
        }

        NonDelayedClassInfoImpl annotatedClassInfo = basicGetAnnotatedClassInfo(name);

        if (annotatedClassInfo != null) {
            if (useHashText != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] ENTER / RETURN [ {1} ] annotated class",
                            new Object[] { useHashText, annotatedClassInfo.getHashText() });
            }

            return annotatedClassInfo;
        }

        // Allow java classes to be delayed.

        DelayedClassInfoImpl delayedClassInfo = basicGetDelayedClassInfo(name);

        if (delayedClassInfo != null) {
            if (useHashText != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] ENTER / RETURN [ {1} ] already delayed",
                            new Object[] { useHashText, delayedClassInfo.getHashText() });
            }

            return delayedClassInfo;

        } else {
            delayedClassInfo = basicPutDelayedClassInfo(name);

            NonDelayedClassInfoImpl nonDelayedClassInfo = associate(delayedClassInfo);

            if (useHashText != null) {
                if (nonDelayedClassInfo != null) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                                "[ {0} ] ENTER / RETURN [ {1} ] newly created delayed for [ {2} ]",
                                new Object[] { useHashText,
                                               delayedClassInfo.getHashText(),
                                               nonDelayedClassInfo.getHashText() });

                } else {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                                "[ {0} ] ENTER / RETURN [ {1} ] newly created delayed",
                                new Object[] { useHashText, delayedClassInfo.getHashText() });
                }
            }

            return delayedClassInfo;
        }
    }

    public ClassInfoImpl getNonDelayedClassInfo(String name, boolean allowPrimitive) {
        String methodName = "getNonDelayableClassInfo";

        String useHashText = (logger.isLoggable(Level.FINER) ? getHashText() : null);

        if (allowPrimitive) {
            Type primitiveType = ClassInfoCacheImpl.getPrimitiveType(name);

            if (primitiveType != null) {
                PrimitiveClassInfoImpl primitiveClassInfo = getPrimitiveClassInfo(name, primitiveType);

                if (useHashText != null) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                                "[ {0} ] ENTER / RETURN [ {1} ] primitive class",
                                new Object[] { useHashText, primitiveClassInfo.getHashText() });
                }

                return primitiveClassInfo;
            }
        }

        NonDelayedClassInfoImpl javaClassInfo = basicGetJavaClassInfo(name);

        if (javaClassInfo != null) {
            if (useHashText != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] ENTER / RETURN [ {1} ] java class",
                            new Object[] { useHashText, javaClassInfo.getHashText() });
            }

            return javaClassInfo;
        }

        NonDelayedClassInfoImpl annotatedClassInfo = basicGetAnnotatedClassInfo(name);

        if (annotatedClassInfo != null) {
            if (useHashText != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] ENTER / RETURN [ {1} ] annotated class",
                            new Object[] { useHashText, annotatedClassInfo.getHashText() });
            }

            return annotatedClassInfo;
        }

        NonDelayedClassInfoImpl nonDelayedClassInfo = basicGetClassInfo(name);

        if (nonDelayedClassInfo != null) {
            if (useHashText != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] ENTER / RETURN [ {1} ]",
                            new Object[] { useHashText, nonDelayedClassInfo.getHashText() });
            }

        } else {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] ENTER / RETURN [ null ]",
                        useHashText);
        }

        return nonDelayedClassInfo;
    }

    //

    // Delayed class info management ...
    //
    // Delayed classes allow soft linkages between class infos
    // and their related classes (interfaces, superclass, method parameters,
    // method exceptions, method result type, and field type).
    //
    // Delayed classes could be discarded when classes are removed from the main store,
    // but, this would require extra processing and reference counting, and is not seen
    // as worth doing.

    protected Map<String, DelayedClassInfoImpl> delayedClassInfos;

    // Allow a scan for delayed class info which does not cause the
    // class info to be created.
    //
    // This is for use for new class info, to the allow the class info
    // to be attached to any already created delayed class info.

    protected DelayedClassInfoImpl basicGetDelayedClassInfo(String name) {
        return delayedClassInfos.get(name);
    }

    protected DelayedClassInfoImpl basicPutDelayedClassInfo(String name) {
        String methodName = "basicPutDelayedClassInfo";

        DelayedClassInfoImpl delayedClassInfo = new DelayedClassInfoImpl(name, getInfoStore());

        // Use the interned class name.
        name = delayedClassInfo.getName();

        delayedClassInfos.put(name, delayedClassInfo);

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Adding delayed class info [ {1} ]",
                        new Object[] { getHashText(), delayedClassInfo.getHashText() });

            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Delayed cache size [ {1} ]",
                        new Object[] { getHashText(), Integer.valueOf(delayedClassInfos.size()) });
        }

        return delayedClassInfo;
    }

    protected NonDelayedClassInfoImpl associate(DelayedClassInfoImpl delayedClassInfo) {
        String methodName = "associate";

        NonDelayedClassInfoImpl nonDelayedClassInfo = basicGetClassInfo(delayedClassInfo.getName());

        if (nonDelayedClassInfo != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] Attaching delayed [ {1} ] to non-delayed [ {2} ]",
                            new Object[] { getHashText(),
                                           delayedClassInfo.getHashText(),
                                           nonDelayedClassInfo.getHashText() });
            }

            delayedClassInfo.setClassInfo(nonDelayedClassInfo);
            nonDelayedClassInfo.setDelayedClassInfo(delayedClassInfo);

            return nonDelayedClassInfo;

        } else {
            return null;
        }
    }

    protected ClassInfoImpl associate(NonDelayedClassInfoImpl nonDelayedClassInfo) {
        String methodName = "associate(NonDelayedClassInfo)";

        DelayedClassInfoImpl delayedClassInfo = basicGetDelayedClassInfo(nonDelayedClassInfo.getName());

        if (delayedClassInfo != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] Attaching delayed [ {1} ] to non-delayed [ {2} ]",
                            new Object[] { getHashText(),
                                           delayedClassInfo.getHashText(),
                                           nonDelayedClassInfo.getHashText() });
            }

            delayedClassInfo.setClassInfo(nonDelayedClassInfo);
            nonDelayedClassInfo.setDelayedClassInfo(delayedClassInfo);

            return delayedClassInfo;

        } else {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] No delayed available for non-delayed [ {1} ]",
                            new Object[] { getHashText(),
                                           nonDelayedClassInfo.getHashText() });
            }

            return null;
        }
    }

    // Class info management ...

    public static final String CLASSINFO_CACHE_LIMIT_PROPERTY_NAME = "classinfocachesize";

    public static final int MIN_CLASSINFO_CACHE_LIMIT = 100;
    public static final int MAX_CLASSINFO_CACHE_LIMIT = 10000;

    public static final int DEFAULT_CLASSINFO_CACHE_LIMIT = 2000;

    protected static final int classInfoCacheLimit;

    static {
        String methodName = "<static init>";

        String classInfoCacheSizeText = java.security.AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(CLASSINFO_CACHE_LIMIT_PROPERTY_NAME);
            }
        });

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "Cache Property [ {0} ] and value [ {1} ]",
                        new Object[] { CLASSINFO_CACHE_LIMIT_PROPERTY_NAME,
                                       classInfoCacheSizeText });
        }

        int useCacheLimit;
        String scanLimitCase = "Default; No property";

        if (classInfoCacheSizeText == null) {
            useCacheLimit = DEFAULT_CLASSINFO_CACHE_LIMIT;
            scanLimitCase = "default; no property value";

        } else {
            try {
                useCacheLimit = Integer.valueOf(classInfoCacheSizeText).intValue();

                if (useCacheLimit < MIN_CLASSINFO_CACHE_LIMIT) {
                    useCacheLimit = MIN_CLASSINFO_CACHE_LIMIT;
                    scanLimitCase = "out of range; reassigned to minimum";

                } else if (useCacheLimit > MAX_CLASSINFO_CACHE_LIMIT) {
                    useCacheLimit = MAX_CLASSINFO_CACHE_LIMIT;
                    scanLimitCase = "out of range; reassigned to maximum";

                } else {
                    scanLimitCase = "in range; assigned from property";
                }

            } catch (NumberFormatException nfe) {
                useCacheLimit = DEFAULT_CLASSINFO_CACHE_LIMIT;
                scanLimitCase = "defaulted; non-valid integer value ( " + nfe.getMessage() + " )";
            }
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "Setting class info cache size [ {0} ]: [ {1} ]",
                        new Object[] { Integer.valueOf(useCacheLimit), scanLimitCase });
        }

        classInfoCacheLimit = useCacheLimit;
    }

    //

    protected Map<String, NonDelayedClassInfoImpl> javaClassInfos;

    protected NonDelayedClassInfoImpl basicGetJavaClassInfo(String name) {
        return javaClassInfos.get(name);
    }

    protected boolean basicPutJavaClassInfo(NonDelayedClassInfoImpl classInfo) {
        String methodName = "basicPutJavaClassInfo";

        // Use the interned class name.
        String classInfoName = classInfo.getName();

        NonDelayedClassInfoImpl extantClassInfo = javaClassInfos.get(classInfoName);

        if (extantClassInfo == null) {
            javaClassInfos.put(classInfoName, classInfo);

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] Add of [ {1} ] increases java cache to [ {2} ]",
                            new Object[] { getHashText(),
                                           classInfo.getHashText(),
                                           Integer.valueOf(javaClassInfos.size()) });
            }

            return true;

        } else {
            if (extantClassInfo != classInfo) {
                // CWWKC0018W
                logger.logp(Level.WARNING, CLASS_NAME, methodName,
                        "[ {0} ] ANNO_CLASSINFO_EXISTS1 [ {1} ] [ {2} ]",
                        new Object[] { getHashText(),
                                       classInfo.getHashText(),
                                       extantClassInfo.getHashText() });                
            } else {
                // CWWKC0019W
                logger.logp(Level.WARNING, CLASS_NAME, methodName,
                        "[ {0} ] ANNO_CLASSINFO_EXISTS2 [ {2} ]",
                        new Object[] { getHashText(), classInfo.getHashText() });
            }

            return false;
        }
    }

    protected Map<String, NonDelayedClassInfoImpl> annotatedClassInfos;

    protected NonDelayedClassInfoImpl basicGetAnnotatedClassInfo(String name) {
        return annotatedClassInfos.get(name);
    }

    protected boolean basicPutAnnotatedClassInfo(NonDelayedClassInfoImpl classInfo) {
        String methodName = "basicPutAnnotatedClassInfo";

        // Use the interned class name.
        String classInfoName = classInfo.getName();

        NonDelayedClassInfoImpl extantClassInfo = annotatedClassInfos.get(classInfoName);

        if (extantClassInfo == null) {
            annotatedClassInfos.put(classInfoName, classInfo);

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] Add of [ {1} ] increases annotated cache to [ {2} ]",
                            new Object[] { getHashText(),
                                           classInfo.getHashText(),
                                           Integer.valueOf(annotatedClassInfos.size()) });
            }

            return true;

        } else {
            if (extantClassInfo != classInfo) {
                // CWWKC0020W
                logger.logp(Level.WARNING, CLASS_NAME, methodName,
                        "[ {0} ] ANNO_CLASSINFO_EXISTS3 [ {1} ] [ {2} ]",
                        new Object[] { getHashText(),
                                       classInfo.getHashText(),
                                       extantClassInfo.getHashText() });
            } else {
                // CWWKC0021W
                logger.logp(Level.WARNING, CLASS_NAME, methodName,
                        "[ {0} ] ANNO_CLASSINFO_EXISTS4 [ {1} ]",
                        new Object[] { getHashText(), classInfo.getHashText() });
            }

            return false;
        }
    }

    //

    protected NonDelayedClassInfoImpl firstClassInfo;
    protected NonDelayedClassInfoImpl lastClassInfo;

    protected Map<String, NonDelayedClassInfoImpl> classInfos;

    protected NonDelayedClassInfoImpl basicGetClassInfo(String name) {
        if ((firstClassInfo != null) && name.equals(firstClassInfo.getName())) {
            return firstClassInfo;
        }

        return classInfos.get(name);
    }

    protected boolean basicPutClassInfo(NonDelayedClassInfoImpl classInfo) {
        String methodName = "basicPutClassInfo";

        // Use the interned class name.
        String classInfoName = classInfo.getName();

        NonDelayedClassInfoImpl extantClassInfo = classInfos.get(classInfoName);

        if (extantClassInfo == null) {
            classInfos.put(classInfoName, classInfo);

            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] Add of [ {1} ] increases class cache to [ {2} ]",
                            new Object[] { getHashText(),
                                           classInfo.getHashText(),
                                           Integer.valueOf(classInfos.size()) });
            }

            return true;

        } else {
            if (extantClassInfo != classInfo) {
                // CWWKC0018W
                logger.logp(Level.WARNING, CLASS_NAME, methodName,
                        "[ {0} ] ANNO_CLASSINFO_EXISTS1 [ {1} ] [ {2} ]",
                        new Object[] { getHashText(),
                                       classInfo.getHashText(),
                                       extantClassInfo.getHashText() });
            } else {
                // CWWKC0019W
                logger.logp(Level.WARNING, CLASS_NAME, methodName,
                        "[ {0} ] ANNO_CLASSINFO_EXISTS2 [ {2} ]",
                        new Object[] { getHashText(), classInfo.getHashText() });
            }

            return false;
        }
    }

    protected NonDelayedClassInfoImpl createClassInfo(String name, String superName, int access, String[] interfaces) {
        return new NonDelayedClassInfoImpl(name, superName, access, interfaces, getInfoStore());
    }

    // Single point at which to add a class info:
    //
    // If a java class info, simply add it and return.
    //
    // A primitive or array class will never be added.

    // Do not immediately attach the class to it's
    // delayed class info.  (The attachment could be made
    // immediately, but is not yet necessary.)
    //
    // Do update the LRU state.

    protected boolean addClassInfo(NonDelayedClassInfoImpl classInfo) {
        boolean didAdd;

        if (classInfo.isJavaClass()) {
            didAdd = basicPutJavaClassInfo(classInfo);

        } else if (classInfo.isAnnotationPresent() ||
                   classInfo.isFieldAnnotationPresent() ||
                   classInfo.isMethodAnnotationPresent()) {
            didAdd = basicPutAnnotatedClassInfo(classInfo);

        } else {
            didAdd = basicPutClassInfo(classInfo);

            // Note: 'addAsFirst' must only be performed for non-java, non-annotated
            //        classes.  Both java and annotated classes are put in separate
            //        storage which is never swapped.  Non-java, non-annotated classes
            //        are swappable, and are maintained in a last-access ordered
            //        linked list.
            //
            //        The current addition counts as an access, meaning, the class
            //        info is placed into the last-access list as the first element.

            if (didAdd) {
                addAsFirst(classInfo);
            }
        }

        if (didAdd) {
            ClassInfoImpl delayedClassInfo = associate(classInfo);
            discardRef(delayedClassInfo); // No current use for the return value; discard it.
        }

        return didAdd;
    }

    // TFB: START: Defect 59284 Notes
    //
    // Resolve the class info as a non-delayed class info.
    //
    // This operation is performed from delayed class info.  This is a base
    // operation which must succeed in obtaining a definite non-delayed class
    // info, or must fail with a null return value.
    //
    // There are several initial checks which look for an already present
    // non-delayed class info.  The several checks are required because
    // of particular storage of select classes in non-swappable storage.
    //
    // The three checks are:
    //
    // Pre1) basic get (swappable class which is neither java, javax, or annotated)
    // Pre2) java (includes javax) basic get
    // Pre3) annotated class basic get
    //
    // Success of any of these results in an immediate return of the retrieved
    // class info.
    //
    // Failure of all three results in a call to perform the raw class
    // scan.  That has several possible results:
    //
    // Result1) The class resource was not found.
    // Result2) The class resource was found and was successfully loaded.
    // Result3) The class resource was found, with a failure during the
    //          processing of the class bytes.
    //
    // (Result3) is visible because of a thrown exception, which, at
    // this time results in a runtime exception being thrown.
    //
    // TODO: The (Result3) case must be revisited.
    //
    // (Result1) and (Result2) are visible by the side effect of the class
    // being stored in the basic tables.  Using the same three tests:
    //
    // Post1) basic get
    // Post2) java and javax basic get
    // Post3) annotated class basic get
    //
    // The result case is determined, and, if an actual class info was obtained,
    // that class info is returned (Result 2).  If no actual class info was
    // obtained, null is returned (Result 1).
    //
    // When no actual class info is obtained, a warning message is displayed.
    //
    // TODO: The warning message is subject to tuning, based either on user
    //       preferences, or if too many messages are being generated.
    //
    //       That is, failure to load a particular class should result in at
    //       most one warning: The info cache may need to keep a table of failed
    //       loads and use this to block multiple warnings for the same class.
    //
    //       For storage considerations, the table of failures should be limited
    //       to a fixed maximum size, resulting in possibly several warnings for
    //       the same class, but with still a reduction.
    //
    // TFB: END: Defect 59284 Notes

    public NonDelayedClassInfoImpl resolveClassInfo(String name) {
        String methodName = "resolveClassInfo";

        NonDelayedClassInfoImpl cachedClassInfo = basicGetClassInfo(name);
        if (cachedClassInfo != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] ENTER / RETURN [ {1} ] - cached",
                            new Object[] { getHashText(), cachedClassInfo.getHashText() });
            }
            return cachedClassInfo;
        }

        String useHashText = (logger.isLoggable(Level.FINER) ? getHashText() : null);

        NonDelayedClassInfoImpl javaClassInfo = basicGetJavaClassInfo(name);

        if (javaClassInfo != null) {
            if (useHashText != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] RETURN [ {1} ] java class",
                            new Object[] { useHashText, javaClassInfo.getHashText() });
            }
            return javaClassInfo;
        }

        NonDelayedClassInfoImpl annotatedClassInfo = basicGetAnnotatedClassInfo(name);

        if (annotatedClassInfo != null) {
            if (useHashText != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] RETURN [ {1} ] annotated class",
                            new Object[] { useHashText, annotatedClassInfo.getHashText() });
            }
            return annotatedClassInfo;
        }

        if (useHashText != null) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "ENTRY [ {0} ] Class [ {1} ] ",
                        new Object[] { useHashText, name });
        }

        try {
            scanClass(name); // This has side effects on the ClassInfoCache!
            // throws InfoStoreException

        } catch (InfoStoreException e) {
             // CWWKC0022W
            // "[ {0} ] ANNO_CLASSINFO_SCAN_EXCEPTION [ {1} ] [ {2} ] [ {3} ]",
            logger.logp(Level.WARNING, CLASS_NAME, methodName,
                "ANNO_CLASSINFO_SCAN_EXCEPTION",
                new Object[] { getHashText(),
                               name,
                               e.getMessage(),
                               ((e.getCause() == null) ? e : e.getCause()) });
            logger.logp(Level.WARNING, CLASS_NAME, methodName, "Scan failure", e);
            
            if (useHashText != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] RETURN [ null ]; class scan failed with an exception",
                    new Object[] { useHashText, name });
            }
            return null;
        }

        cachedClassInfo = basicGetClassInfo(name);

        if (cachedClassInfo != null) {
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] ENTER / RETURN [ {1} ] - newly cached",
                    new Object[] { getHashText(), cachedClassInfo.getHashText() });
            }
            return cachedClassInfo;
        }

        javaClassInfo = basicGetJavaClassInfo(name);

        if (javaClassInfo != null) {
            if (useHashText != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] RETURN [ {1} ] newly added java class",
                    new Object[] { useHashText, javaClassInfo.getHashText() });
            }
            return javaClassInfo;
        }

        annotatedClassInfo = basicGetAnnotatedClassInfo(name);

        if (annotatedClassInfo != null) {
            if (useHashText != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] RETURN [ {1} ] new added annotated class",
                    new Object[] { useHashText, annotatedClassInfo.getHashText() });
            }
            return annotatedClassInfo;
        }

        // START: TFB: Defect 59284: Don't throw an exception for a failed load.

        // A warning is logged by DelayedClassInfo.getClassInfo.
        //
        // scanLogger.logp(Level.WARNING, CLASS_NAME, methodName,
        //                 "[ {0} ] Internal error resolving [ {1} ]",
        //                 new Object[] { getHashText(), name });

        if (useHashText != null) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] RETURN [ null ] for class which was not found",
                new Object[] { useHashText, name });
        }
        return null;

        // String eMsg = "[ " + getHashText() + " ] Internal error resolving [ " + name + " ]";
        // RuntimeException e = new RuntimeException(eMsg);
        //
        // scanLogger.log(Level.WARNING, eMsg, e);
        // throw e;

        // END: TFB: Defect 59284
    }

    //

    // Direct operation to link a class info into the LRU list;
    // There are three cases: When the storage is empty, when the
    // storage is a singleton, and when the storage has more than
    // one element.
    //
    // If the storage has more than one element, if the addition
    // put us over the maximum size, trim off the last element.

    protected void addAsFirst(NonDelayedClassInfoImpl classInfo) {
        String methodName = "addAsFirst";
        boolean doLog = logger.isLoggable(Level.FINER);
        String useHashText = (doLog ? getHashText() : null);
        String useClassHashText = (doLog ? classInfo.getHashText() : null);

        if (doLog) {
            logLinks(methodName, classInfo);
        }

        if (firstClassInfo == null) {
            if (doLog) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Adding [ {1} ] to empty",
                    new Object[] { useHashText, useClassHashText });
            }

            firstClassInfo = classInfo;

            lastClassInfo = classInfo;

            // last == first
            // lastClassInfoName == firstClassInfoName

            // first.next remains null
            // first.prev remains null

        } else if (firstClassInfo == lastClassInfo) {
            if (doLog) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Adding [ {1} ] to singleton [ {2} ]",
                    new Object[] { useHashText, useClassHashText, firstClassInfo.getHashText() });
            }

            firstClassInfo = classInfo;

            firstClassInfo.setNextClassInfo(lastClassInfo);
            lastClassInfo.setPriorClassInfo(classInfo);

            // last != first
            // lastClassInfoName != firstClassInfoName

            // first.prev == null
            // first.next == last

            // last.prev == first
            // last.next == null

        } else {
            if (doLog) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Adding [ {1} ] to multitude [ {2} ]",
                    new Object[] { useHashText,
                                   useClassHashText,
                                   firstClassInfo.getHashText() });
            }

            classInfo.setNextClassInfo(firstClassInfo);

            firstClassInfo.setPriorClassInfo(classInfo);

            firstClassInfo = classInfo;

            if (classInfos.size() > ClassInfoCacheImpl.classInfoCacheLimit) {
                NonDelayedClassInfoImpl oldLastClassInfo = lastClassInfo;
                String lastClassName = lastClassInfo.getName();
                classInfos.remove(lastClassName);

                discardRef(lastClassName); // No current use for the old last class name; discard it.

                lastClassInfo = oldLastClassInfo.getPriorClassInfo();

                lastClassInfo.setNextClassInfo(null);

                // oldLastClassInfo.setNextClassInfo(null);
                oldLastClassInfo.setPriorClassInfo(null);

                if (doLog) {
                    logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] new last [ {1} ] displaces [ {2} ]",
                        new Object[] { useHashText,
                                       lastClassInfo.getHashText(),
                                       oldLastClassInfo.getHashText() });
                }

                DelayedClassInfoImpl delayedClassInfo = oldLastClassInfo.getDelayedClassInfo();
                if (delayedClassInfo != null) {
                    if (doLog) {
                        logger.logp(Level.FINER, CLASS_NAME, methodName,
                            "[ {0} ] Clearing link on displaced [ {1} ]",
                            new Object[] { useHashText, oldLastClassInfo.getHashText() });
                    }

                    delayedClassInfo.setClassInfo(null);
                    oldLastClassInfo.setDelayedClassInfo(null);
                }
            }
        }
    }

    // Move a class info (which must be present, and therefore linked)
    // to the first position.
    //
    // There are three cases: The class info is already first,
    // The class info is last, or the class info is somewhere in the middle.

    public void makeFirst(NonDelayedClassInfoImpl classInfo) {
        String methodName = "makeFirst";
        boolean doLog = logger.isLoggable(Level.FINER);
        String useHashText = (doLog ? getHashText() : null);
        String useClassHashText = (doLog ? classInfo.getHashText() : null);

        if (doLog) {
            logLinks(methodName, classInfo);
        }

        if (classInfo == firstClassInfo) {
            if (doLog) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Already first [ {1} ]",
                    new Object[] { useHashText, useClassHashText });
            }

            return;

        } else if (classInfo == lastClassInfo) {
            if (doLog) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Moving from last [ {1} ]",
                    new Object[] { useHashText, useClassHashText });
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Old first [ {1} ]",
                    new Object[] { useHashText, firstClassInfo.getHashText() });
            }

            lastClassInfo = classInfo.getPriorClassInfo();
            lastClassInfo.setNextClassInfo(null);

            if (doLog) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] New last [ {1} ]",
                    new Object[] { useHashText, lastClassInfo.getHashText() });
            }

            firstClassInfo.setPriorClassInfo(classInfo);

            classInfo.setPriorClassInfo(null);
            classInfo.setNextClassInfo(firstClassInfo);

            firstClassInfo = classInfo;

        } else {
            if (doLog) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Moving from middle [ {1} ]",
                    new Object[] { useHashText, useClassHashText });
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Old first [ {1} ]",
                    new Object[] { useHashText, firstClassInfo.getHashText() });
            }

            NonDelayedClassInfoImpl currentPrior = classInfo.getPriorClassInfo();
            NonDelayedClassInfoImpl currentNext = classInfo.getNextClassInfo();

            currentPrior.setNextClassInfo(currentNext);
            currentNext.setPriorClassInfo(currentPrior);

            firstClassInfo.setPriorClassInfo(classInfo);
            classInfo.setNextClassInfo(firstClassInfo);
            classInfo.setPriorClassInfo(null);

            firstClassInfo = classInfo;
        }
    }

    // Remove a non-delayed class from the delayable collection.
    //
    // There can still be an associated delayed class info.
    //
    // The key detail is that this non-delayed class info is prevented from
    // being swapped out.

    public void removeAsDelayable(NonDelayedClassInfoImpl classInfo) {
        String methodName = "removeAsDelayable";

        boolean doLog = logger.isLoggable(Level.FINER);
        String useHashText = (doLog ? getHashText() : null);
        String useClassHashText = (doLog ? classInfo.getHashText() : null);

        if (doLog) {
            logLinks(methodName, classInfo);
        }

        if (firstClassInfo == null) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Ignoring remove request; no linked classes [ {1} ]",
                new Object[] { useHashText, useClassHashText });
            return;
        }

        NonDelayedClassInfoImpl currentPrior = classInfo.getPriorClassInfo();
        NonDelayedClassInfoImpl currentNext = classInfo.getNextClassInfo();

        // The only way for both the current prior can be null is for the class to be the singleton
        // first class.  So, if the class is not the first class, and its prior and next are both
        // null, then it is not linked.

        if ((firstClassInfo != classInfo) && (currentPrior == null) && (currentNext == null)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Ignoring remove request; not yet linked [ {1} ]",
                new Object[] { useHashText, useClassHashText });
            return;
        }

        if (currentPrior != null) {
            currentPrior.setNextClassInfo(currentNext);
        } else {
            firstClassInfo = currentNext;
        }

        if (currentNext != null) {
            currentNext.setPriorClassInfo(currentPrior);
        } else {
            lastClassInfo = currentPrior;
        }

        // Clear the prior and next pointers to indicate the ClassInfo is no longer linked
        classInfo.setPriorClassInfo(null);
        classInfo.setNextClassInfo(null);

        // Remove the class from the delayable collection
        classInfos.remove(classInfo.getName());

        // the NonDelayedClassInfo needs to be in one of the ClassInfo maps. It was just removed 
        // from the delayable map (classInfos) therefore is not a primitive object so the 
        // primitiveClassInfos map is not applicable. The only reason to make it nondelayable is 
        // because an annotation has been found, so we will add it to the annotatedClassInfos map.
        basicPutAnnotatedClassInfo(classInfo);

        if (doLog) {
            // Note: The extra '== null' and '!= null' tests are
            //       present because the eclipse compiler is not
            //       smart enough to use all of the test conditions
            //       when doing null pointer analysis in each of the
            //       blocks.  The extra checks avoid compiler warnings.

            if ((currentPrior == null) && (currentNext == null)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Removing as singleton [ {1} ]",
                    new Object[] { useHashText, useClassHashText });

            } else if ((currentPrior == null) && (currentNext != null)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Removing as first [ {1} ]",
                    new Object[] { useHashText, useClassHashText });
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] New first [ {1} ]",
                    new Object[] { useHashText, currentNext.getHashText() });

            } else if ((currentPrior != null) && (currentNext == null)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Removing as last [ {1} ]",
                    new Object[] { useHashText, useClassHashText });
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] New last [ {1} ]",
                    new Object[] { useHashText, currentPrior.getHashText() });

            } else if ((currentPrior != null) && (currentNext != null)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Removing from middle [ {1} ]",
                    new Object[] { useHashText, useClassHashText });

                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Old prior [ {1} ]",
                    new Object[] { useHashText, currentPrior.getHashText() });
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] Old next [ {1} ]",
                    new Object[] { useHashText, currentNext.getHashText() });
            }
        }
    }

    protected void logLinks(String methodName, NonDelayedClassInfoImpl classInfo) {

        if (classInfo.getNextClassInfo() != null) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Class [ {1} ] Next [ {2} ]",
                new Object[] { getHashText(),
                               classInfo.getHashText(),
                               classInfo.getNextClassInfo().getHashText() });
        }

        if (classInfo.getPriorClassInfo() != null) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] Class [ {1} ] Prior [ {2} ]",
                new Object[] { getHashText(),
                               classInfo.getHashText(),
                               classInfo.getPriorClassInfo().getHashText() });
        }
    }

    //

    public void recordAccess(NonDelayedClassInfoImpl classInfo) {
        makeFirst(classInfo);
    }

    //

    protected void log(Logger useLogger) {
        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN STATE [ {0} ]", getHashText());
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Info Store [ {0} ]", getInfoStore().getHashText());

        log_internMaps(useLogger);
        log_packages(useLogger);
        log_primitiveClasses(useLogger);
        log_delayedClasses(useLogger);
        log_javaClasses(useLogger);
        log_annotatedClasses(useLogger);
        log_classes(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END STATE [ {0} ]", getHashText());
    }

    protected void log_internMaps(Logger useLogger) {
        String methodName = "log_internMaps";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN Intern Maps:");

        getDescriptionInternMap().log(useLogger);
        getPackageNameInternMap().log(useLogger);
        getClassNameInternMap().log(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END Intern Maps:");
    }

    protected void log_packages(Logger useLogger) {
        String methodName = "log_packages";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN Packages:");

        for (PackageInfoImpl packageInfo : packageInfos.values()) {
            packageInfo.log(useLogger);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END Packages");
    }

    protected void log_classes(Logger useLogger) {
        String methodName = "log_classes";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN Classes:");

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  First Class [ {0} ]:",
            ((firstClassInfo == null) ? null : firstClassInfo.getHashText()));

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  Last Class [ {0} ]:",
            ((lastClassInfo == null) ? null : lastClassInfo.getHashText()));

        for (NonDelayedClassInfoImpl classInfo : classInfos.values()) {
            classInfo.log(useLogger);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END Classes");
    }

    protected void log_annotatedClasses(Logger useLogger) {
        String methodName = "log_annotatedClasses";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN Annotated Classes:");

        for (NonDelayedClassInfoImpl annotatedClassInfo : annotatedClassInfos.values()) {
            annotatedClassInfo.log(useLogger);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END Annotated Classes");
    }

    protected void log_javaClasses(Logger useLogger) {
        String methodName = "log_javaClasses";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN Java Classes:");

        for (NonDelayedClassInfoImpl javaClassInfo : javaClassInfos.values()) {
            javaClassInfo.log(useLogger);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END Java Classes");
    }

    protected void log_delayedClasses(Logger useLogger) {
        String methodName = "log_delayedClasses";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN Delayed Classes:");

        for (ClassInfoImpl delayedClassInfo : delayedClassInfos.values()) {
            delayedClassInfo.log(useLogger);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END Delayed Classes");
    }

    protected void log_primitiveClasses(Logger useLogger) {
        String methodName = "log_primitiveClasses";

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN Primitive Classes:");

        for (PrimitiveClassInfoImpl primitiveClassInfo : primitiveClassInfos.values()) {
            primitiveClassInfo.log(useLogger);
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END Primitive Classes");
    }
    
    //
    
    protected void log(TraceComponent useLogger) {
        Tr.debug(useLogger, MessageFormat.format("BEGIN STATE [ {0} ]", getHashText()));
        Tr.debug(useLogger, MessageFormat.format("  Info Store [ {0} ]", getInfoStore().getHashText()));

        log_internMaps(useLogger);
        log_packages(useLogger);
        log_primitiveClasses(useLogger);
        log_delayedClasses(useLogger);
        log_javaClasses(useLogger);
        log_annotatedClasses(useLogger);
        log_classes(useLogger);

        Tr.debug(useLogger, MessageFormat.format("END STATE [ {0} ]", getHashText()));
    }

    protected void log_internMaps(TraceComponent useLogger) {
        Tr.debug(useLogger, "BEGIN Intern Maps:");

        getDescriptionInternMap().log(useLogger);
        getPackageNameInternMap().log(useLogger);
        getClassNameInternMap().log(useLogger);

        Tr.debug(useLogger, "END Intern Maps:");
    }

    protected void log_packages(TraceComponent useLogger) {
        Tr.debug(useLogger, "BEGIN Packages:");

        for (PackageInfoImpl packageInfo : packageInfos.values()) {
            packageInfo.log(useLogger);
        }

        Tr.debug(useLogger, "END Packages");
    }

    protected void log_classes(TraceComponent useLogger) {
        Tr.debug(useLogger, "BEGIN Classes:");

        Tr.debug(useLogger, MessageFormat.format("  First Class [ {0} ]:",
                                              ((firstClassInfo == null) ? null : firstClassInfo.getHashText())));

        Tr.debug(useLogger, MessageFormat.format("  Last Class [ {0} ]:",
                                              ((lastClassInfo == null) ? null : lastClassInfo.getHashText())));

        for (NonDelayedClassInfoImpl classInfo : classInfos.values()) {
            classInfo.log(useLogger);
        }

        Tr.debug(useLogger, "END Classes");
    }

    protected void log_annotatedClasses(TraceComponent useLogger) {
        Tr.debug(useLogger, "BEGIN Annotated Classes:");

        for (NonDelayedClassInfoImpl annotatedClassInfo : annotatedClassInfos.values()) {
            annotatedClassInfo.log(useLogger);
        }

        Tr.debug(useLogger, "END Annotated Classes");
    }

    protected void log_javaClasses(TraceComponent useLogger) {
        Tr.debug(useLogger, "BEGIN Java Classes:");

        for (NonDelayedClassInfoImpl javaClassInfo : javaClassInfos.values()) {
            javaClassInfo.log(useLogger);
        }

        Tr.debug(useLogger, "END Java Classes");
    }

    protected void log_delayedClasses(TraceComponent useLogger) {
        Tr.debug(useLogger, "BEGIN Delayed Classes:");

        for (ClassInfoImpl delayedClassInfo : delayedClassInfos.values()) {
            delayedClassInfo.log(useLogger);
        }

        Tr.debug(useLogger, "END Delayed Classes");
    }

    protected void log_primitiveClasses(TraceComponent useLogger) {
        Tr.debug(useLogger, "BEGIN Primitive Classes:");

        for (PrimitiveClassInfoImpl primitiveClassInfo : primitiveClassInfos.values()) {
            primitiveClassInfo.log(useLogger);
        }

        Tr.debug(useLogger, "END Primitive Classes");
    }
}
