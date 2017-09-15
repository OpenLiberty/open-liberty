/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.info.internal;

import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.objectweb.asm.Opcodes;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.anno.info.ClassInfo;

public class NonDelayedClassInfo extends ClassInfoImpl {

    private static final TraceComponent tc = Tr.register(NonDelayedClassInfo.class);
    public static final String CLASS_NAME = NonDelayedClassInfo.class.getName();

    //

    public static boolean isInterface(int modifiers) {
        return Modifier.isInterface(modifiers);
    }

    // Instance state ...

    protected static final boolean IS_ARTIFICIAL = true;
    protected static final boolean IS_NOT_ARTIFICIAL = false;

    private final boolean isArtificial;

    private final boolean isInterface;

    private final String packageName;
    private PackageInfoImpl packageInfo;

    private final boolean isJavaClass;

    private final String[] interfaceNames;
    private ClassInfoImpl[] interfaces;

    private final String superClassName;
    private ClassInfoImpl superClass;

    private List<FieldInfoImpl> declaredFields = Collections.emptyList();

    private List<MethodInfoImpl> declaredConstructors = Collections.emptyList();

    private List<MethodInfoImpl> declaredMethods = Collections.emptyList();

    //

    public static final String[] EMPTY_INTERFACE_NAMES = new String[] {};
    public static final int MODIFIER_PUBLIC_NONINTERFACE = Modifier.PUBLIC;

    protected NonDelayedClassInfo(String name, InfoStoreImpl infoStore) {
        this(name,
             ClassInfo.OBJECT_CLASS_NAME,
             MODIFIER_PUBLIC_NONINTERFACE,
             EMPTY_INTERFACE_NAMES,
             IS_ARTIFICIAL,
             infoStore);
    }

    public NonDelayedClassInfo(String name, String superClassName, int modifiers, String[] interfaceNames,
                               InfoStoreImpl infoStore) {
        this(name, superClassName, modifiers, interfaceNames, IS_NOT_ARTIFICIAL, infoStore);
    }

    public NonDelayedClassInfo(String name, String superClassName, int modifiers, String[] interfaceNames,
                               boolean isArtificial,
                               InfoStoreImpl infoStore) {

        super(name, modifiers, infoStore);

        // java.lang.Object has no super class
        if (name.equals(OBJECT_CLASS_NAME)) {
            superClassName = null;
        }

        InfoStoreImpl useStore = getInfoStore();

        this.isArtificial = isArtificial;

        this.isInterface = isInterface(modifiers);

        this.packageName = useStore.internPackageName(ClassInfoImpl.getPackageName(name));
        this.packageInfo = null;

        this.isJavaClass = ClassInfoImpl.isJavaClass(name);

        for (int nameNo = 0; nameNo < interfaceNames.length; nameNo++) {
            interfaceNames[nameNo] = useStore.internClassName(interfaceNames[nameNo]);
        }

        this.interfaceNames = interfaceNames;
        this.interfaces = null;

        if ((superClassName == null) && !name.equals(ClassInfo.OBJECT_CLASS_NAME) && !isInterface) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Strange: Null superclass name for non-interface!");
        }

        this.superClassName = useStore.internClassName(superClassName);
        this.superClass = null;

        if (tc.isDebugEnabled()) {
            if (this.isArtificial) {
                Tr.debug(tc, MessageFormat.format("<init> [ {0} ] Created [ ** ARTIFICIAL ** ]",
                                                  getHashText()));
            } else {
                Tr.debug(tc, MessageFormat.format("<init> [ {0} ] Created", getHashText()));
            }
        }
    }

    //

    @Override
    public boolean isArtificial() {
        return this.isArtificial;
    }

    //

    public boolean isNonDelayed() {
        return true;
    }

    @Override
    public NonDelayedClassInfo asNonDelayedClass() {
        return this;
    }

    //

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public PackageInfoImpl getPackage() {
        if ((packageInfo == null) && (packageName != null)) {
            packageInfo = getInfoStore().getPackageInfo(packageName, ClassInfoCache.DO_FORCE_PACKAGE);
        }
        return packageInfo;
    }

    //

    @Override
    public boolean isJavaClass() {
        return isJavaClass;
    }

    //

    @Override
    public List<String> getInterfaceNames() {
        return Arrays.asList(interfaceNames);
    }

    @Override
    public List<ClassInfoImpl> getInterfaces() {
        if (interfaces == null) {
            interfaces = new ClassInfoImpl[interfaceNames.length];

            int i = 0;
            for (String interfaceName : interfaceNames) {
                ClassInfoImpl nextInterface = getInfoStore().getDelayableClassInfo(interfaceName);
                interfaces[i++] = nextInterface;
            }

        }
        return Arrays.asList(interfaces);
    }

    @Override
    public boolean isAnnotationClass() {
        return (getModifiers() & Opcodes.ACC_ANNOTATION) != 0;
    }

    @Override
    public boolean isInterface() {
        return isInterface; // Derived from getModifiers(); see the constructor.
    }

    //

    @Override
    public String getSuperclassName() {
        return superClassName;
    }

    @Override
    public ClassInfoImpl getSuperclass() {
        if (superClass == null) {
            if (superClassName != null) {
                superClass = getInfoStore().getDelayableClassInfo(superClassName);
            }
        }

        return superClass;
    }

    @Override
    public boolean isInstanceOf(String className) {
        if (getName().equals(className)) {
            // Always an instance of yourself!
            return true;

        }

        for (String intfName : interfaceNames) {
            if (className.equals(intfName)) {
                return true;
            }
        }

        if (isInterface()) {
            // If an interface, there is no superclass, so testing is complete.
            return false;

        }
        // Always an instance of one of your superclasses.
        ClassInfo useSuperClass = getSuperclass();
        return ((useSuperClass != null) && useSuperClass.isInstanceOf(className));
    }

    @Override
    public boolean isAssignableFrom(String className) {
        if (getName().equals(className)) {
            return true; // Quick test: Always assignable from yourself.
        } else {
            return getInfoStore().getDelayableClassInfo(className).isInstanceOf(getName());
        }
    }

    //

    @Override
    public List<FieldInfoImpl> getDeclaredFields() {
        return declaredFields;
    }

    @Override
    public List<MethodInfoImpl> getDeclaredConstructors() {
        return declaredConstructors;
    }

    @Override
    public List<MethodInfoImpl> getDeclaredMethods() {
        return declaredMethods;
    }

    private static final Comparator<MethodInfoImpl> METHOD_COMPARATOR = new Comparator<MethodInfoImpl>() {

        @Override
        public int compare(MethodInfoImpl m1, MethodInfoImpl m2) {
            int result = m1.getName().compareTo(m2.getName());
            if (result == 0) {
                result = m1.getDescription().compareTo(m2.getDescription());
            }
            return result;
        }
    };

    @Override
    public List<MethodInfoImpl> getMethods() {
        // return inherited fields and all private fields
        LinkedList<MethodInfoImpl> methods = new LinkedList<MethodInfoImpl>();

        methods.addAll(declaredMethods);

        ClassInfoImpl superClass = getSuperclass();
        if (superClass != null) {
            Map<MethodInfoImpl, MethodInfoImpl> overriden = Collections.emptyMap();
            if (declaredMethods.size() > 0) {
                overriden = new TreeMap<MethodInfoImpl, MethodInfoImpl>(METHOD_COMPARATOR);
                for (MethodInfoImpl method : declaredMethods) {
                    if (!method.isPrivate()) {
                        overriden.put(method, method);
                    }
                }
            }

            // get the super methods rather than walk to deal with package private evaluation
            List<MethodInfoImpl> superMethods = superClass.getMethods();

            // add all methods that do not exist unless private or package protected
            for (MethodInfoImpl method : superMethods) {
                if (method.isPackagePrivate() && method.getDeclaringClass().getPackage() != getPackage()) {
                    // not visible
                    continue;
                }

                if (!method.isPrivate()) {
                    // visible method, check if overridden
                    MethodInfoImpl oMethod = overriden.get(method);
                    if (oMethod != null) {
                        // overridden
                        continue;
                    }
                }

                methods.add(method);
            }
        }

        return methods;
    }

    //

    // 1) An inherited (not overridden) field or method keeps any annotations from the
    //    defining class.  This includes any annotations applied because of class spanning.
    //
    // 2) A subclass or overridden method does not acquire any annotations from a superclass
    //    or method definition in a superclass, except in the specific case of inherited
    //    annotations, which applies only to classes, not to fields or methods.
    //
    // 3) Class spanning annotations (that is, the application to a class of an annotation
    //    specifying @Target(ElementType.TYPE, ElementType.FIELD) or
    //    specifying @Target(ElementType.TYPE, ElementType.METHOD), are specific to the javaEE
    //    specifications (or to other particular specifications), and, importantly, are not
    //    a part of general java annotations processing.
    //
    // 4) Class spanning annotations are applied from the class level to either fields or
    //    methods (or both), and are applied to all declared fields or methods, and not to
    //    inherited fields and methods.

    protected boolean isFieldAnnotationPresent;

    @Override
    public boolean isFieldAnnotationPresent() {
        return isFieldAnnotationPresent;
    }

    //

    protected boolean isMethodAnnotationPresent;

    @Override
    public boolean isMethodAnnotationPresent() {
        return isMethodAnnotationPresent;
    }

    private List<AnnotationInfoImpl> annotations; // declared + inherited

    @Override
    public List<AnnotationInfoImpl> getAnnotations() {
        if (annotations != null) {
            return annotations;
        }

        // Several cases where superclasses contribute no annotations.  In
        // each of these cases case, simply re-use the declared annotations
        // collection.
        //
        // (One case: All of the inheritable superclass annotations is
        // overridden by the immediate class, is not detected.)

        ClassInfoImpl useSuperClass = getSuperclass(); // Null for 'java.lang.Object'; null for interfaces.  
        if (useSuperClass == null) {
                annotations = declaredAnnotations;
            return annotations;
                    }

        // Retrieve *all* annotations of the superclass.  The effect
        // is to recurse across annotations of all superclasses.

        List<AnnotationInfoImpl> superAnnos = useSuperClass.getAnnotations();
        if (superAnnos.isEmpty()) {
            annotations = declaredAnnotations;
            return annotations;
                }

        Map<String, AnnotationInfoImpl> allAnnotations =
                        new HashMap<String, AnnotationInfoImpl>(superAnnos.size() + declaredAnnotations.size(), 1.0f);

        boolean sawInherited = false;
        for (AnnotationInfoImpl superAnno : superAnnos) {
            if (sawInherited = superAnno.isInherited()) {
                allAnnotations.put(superAnno.getAnnotationClassName(), superAnno);
            }
                }

        if (!sawInherited) {
            annotations = declaredAnnotations;
            return annotations;
        }

        // Make sure to add the declared annotations *after* adding the super class
        // annotations.  The immediate declared annotations have precedence.

        // We could test the number of overwritten annotations against the
        // number of inherited annotations.  That case seems infrequent, and
        // is not implemented.

        for (AnnotationInfoImpl declaredAnno : declaredAnnotations) {
            AnnotationInfoImpl overwrittenAnno = allAnnotations.put(declaredAnno.getAnnotationClassName(), declaredAnno);
            if (overwrittenAnno != null) {
                // NO-OP: But maybe we want to log this
            }
        }

        annotations = new ArrayList<AnnotationInfoImpl>(allAnnotations.values());
        return annotations;
    }

    //

    protected DelayedClassInfo delayedClassInfo;

    public void setDelayedClassInfo(DelayedClassInfo delayedClassInfo) {
        this.delayedClassInfo = delayedClassInfo;
    }

    public DelayedClassInfo getDelayedClassInfo() {
        return delayedClassInfo;
    }

    //

    protected NonDelayedClassInfo priorClassInfo;
    protected NonDelayedClassInfo nextClassInfo;

    public NonDelayedClassInfo setPriorClassInfo(NonDelayedClassInfo newPriorClassInfo) {
        NonDelayedClassInfo oldPriorClassInfo = priorClassInfo;
        priorClassInfo = newPriorClassInfo;
        return oldPriorClassInfo;
    }

    public NonDelayedClassInfo getPriorClassInfo() {
        return priorClassInfo;
    }

    public NonDelayedClassInfo setNextClassInfo(NonDelayedClassInfo newNextClassInfo) {
        NonDelayedClassInfo oldNextClassInfo = nextClassInfo;
        nextClassInfo = newNextClassInfo;
        return oldNextClassInfo;
    }

    public NonDelayedClassInfo getNextClassInfo() {
        return nextClassInfo;
    }

    //

    @Override
    public void log(TraceComponent logger) {

        Tr.debug(logger, MessageFormat.format(" Non-Delayed Class [ {0} ]", getHashText()));
        Tr.debug(logger, MessageFormat.format("  delayedClassInfo [ {0} ]", ((delayedClassInfo == null) ? null : delayedClassInfo.getHashText())));

        if (!logger.isDumpEnabled()) {
            return;
        }

        Tr.dump(logger, MessageFormat.format("  isInterface [ {0} ]", Boolean.valueOf(isInterface)));

        Tr.dump(logger, MessageFormat.format("  packageName [ {0} ]", packageName));
        Tr.dump(logger, MessageFormat.format("  packageInfo [ {0} ]", ((packageInfo == null) ? null : packageInfo.getHashText())));

        Tr.dump(logger, MessageFormat.format("  isJavaClass [ {0} ]", Boolean.valueOf(isJavaClass)));

        if (interfaceNames == null) {
            Tr.dump(logger, "  interfaceNames [ null ]");
        } else {
            for (String interfaceName : interfaceNames) {
                Tr.dump(logger, MessageFormat.format("  interfaceName [ {0} ]", interfaceName));
            }
        }

        if (interfaces == null) {
            Tr.dump(logger, "  interfaces [ null ]");
        } else {
            for (ClassInfoImpl interfaceInfo : interfaces) {
                Tr.dump(logger, MessageFormat.format("  interface [ {0} ]", interfaceInfo.getHashText()));
            }
        }

        //

        Tr.dump(logger, MessageFormat.format("  superClassName [ {0} ]", superClassName));
        Tr.dump(logger, MessageFormat.format("  superClass [ {0} ]", ((superClass == null) ? null : superClass.getHashText())));

        if (declaredFields == null) {
            Tr.dump(logger, "  declaredFields [ null ]");
        } else {
            for (FieldInfoImpl fieldInfo : declaredFields) {
                Tr.dump(logger, MessageFormat.format("  declaredFields [ {0} ]", fieldInfo.getHashText()));
            }
        }

        if (declaredConstructors == null) {
            Tr.dump(logger, "  declaredConstructors [ null ]");
        } else {
            for (MethodInfoImpl methodInfo : declaredConstructors) {
                Tr.dump(logger, MessageFormat.format("  declaredConstructors [ {0} ]", methodInfo.getHashText()));
            }
        }

        if (declaredMethods == null) {
            Tr.dump(logger, "  declaredMethods [ null ]");
        } else {
            for (MethodInfoImpl methodInfo : declaredMethods) {
                Tr.dump(logger, MessageFormat.format("  declaredMethods [ {0} ]", methodInfo.getHashText()));
            }
        }

        Tr.dump(logger, MessageFormat.format("  isFieldAnnotationPresent [ {0} ]", Boolean.valueOf(isFieldAnnotationPresent)));
        Tr.dump(logger, MessageFormat.format("  isMethodAnnotationPresent [ {0} ]", Boolean.valueOf(isMethodAnnotationPresent)));

        Tr.dump(logger, MessageFormat.format("  delayedClassInfo [ {0} ]", ((delayedClassInfo == null) ? null : delayedClassInfo.getHashText())));
        Tr.dump(logger, MessageFormat.format("  priorClassInfo [ {0} ]", ((priorClassInfo == null) ? null : priorClassInfo.getHashText())));
        Tr.dump(logger, MessageFormat.format("  nextClassInfo [ {0} ]", ((nextClassInfo == null) ? null : nextClassInfo.getHashText())));

        logAnnotations(logger);

        if (declaredConstructors != null) {
            for (MethodInfoImpl methodInfo : declaredConstructors) {
                methodInfo.log(logger);
            }
        }

        Tr.dump(logger, MessageFormat.format(" Non-Delayed Class [ {0} ]", getHashText()));
    }

    public void setFields(FieldInfoImpl[] fields) {
        declaredFields = Arrays.asList(fields);
    }

    public void setConstructors(MethodInfoImpl[] constructors) {
        declaredConstructors = Arrays.asList(constructors);

    }

    public void setMethods(MethodInfoImpl[] methods) {
        declaredMethods = Arrays.asList(methods);

    }

    @Override
    public void setDeclaredAnnotations(AnnotationInfoImpl[] annos) {
        annotations = null;
        super.setDeclaredAnnotations(annos);
    }
}
