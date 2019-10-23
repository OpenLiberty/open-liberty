/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.info.internal;

import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.Opcodes;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.annocache.info.ClassInfo;

public class NonDelayedClassInfoImpl extends ClassInfoImpl {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.info");
    private static final String CLASS_NAME = NonDelayedClassInfoImpl.class.getSimpleName();

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

    private List<FieldInfoImpl> declaredFields;
    private List<MethodInfoImpl> declaredConstructors;
    private List<MethodInfoImpl> declaredMethods;

    //

    public static final String[] EMPTY_INTERFACE_NAMES = new String[] {};
    public static final int MODIFIER_PUBLIC_NONINTERFACE = Modifier.PUBLIC;

    protected NonDelayedClassInfoImpl(String name, InfoStoreImpl infoStore) {
        this(name,
             ClassInfo.OBJECT_CLASS_NAME,
             MODIFIER_PUBLIC_NONINTERFACE,
             EMPTY_INTERFACE_NAMES,
             IS_ARTIFICIAL,
             infoStore);
    }

    public NonDelayedClassInfoImpl(String name, String superClassName, int modifiers, String[] interfaceNames,
                                   InfoStoreImpl infoStore) {
        this(name, superClassName, modifiers, interfaceNames, IS_NOT_ARTIFICIAL, infoStore);
    }

    public NonDelayedClassInfoImpl(String name, String superClassName, int modifiers, String[] interfaceNames,
                                   boolean isArtificial,
                                   InfoStoreImpl infoStore) {

        super(name, modifiers, infoStore);

        String methodName = "<init>";

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
            if (logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "Strange: Null superclass name for non-interface!");
            }
        }

        this.superClassName = useStore.internClassName(superClassName);
        this.superClass = null;

        this.declaredFields = Collections.emptyList();
        this.declaredConstructors = Collections.emptyList();
        this.declaredMethods = Collections.emptyList();
        
        if (logger.isLoggable(Level.FINER)) {
            if (this.isArtificial) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "<init> [ {0} ] Created [ ** ARTIFICIAL ** ]", getHashText());
            } else {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "<init> [ {0} ] Created", getHashText());
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
    public NonDelayedClassInfoImpl asNonDelayedClass() {
        return this;
    }

    //

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public PackageInfoImpl getPackage() {
        if ( (packageInfo == null) && (packageName != null) ) {
            packageInfo = getInfoStore().getPackageInfo(packageName, ClassInfoCacheImpl.DO_FORCE_PACKAGE);
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
        if ( interfaces == null ) {
            interfaces = new ClassInfoImpl[interfaceNames.length];

            int interfaceNo = 0;
            for ( String interfaceName : interfaceNames ) {
                ClassInfoImpl nextInterface = getInfoStore().getDelayableClassInfo(interfaceName);
                interfaces[interfaceNo++] = nextInterface;
            }
        }

        return Arrays.asList(interfaces);
    }

    @Override
    public boolean isAnnotationClass() {
        return ( (getModifiers() & Opcodes.ACC_ANNOTATION) != 0 );
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
        if ( superClass == null ) {
            if ( superClassName != null ) {
                superClass = getInfoStore().getDelayableClassInfo(superClassName);
            }
        }
        return superClass;
    }

    @Override
    public boolean isInstanceOf(String className) {
        if ( getName().equals(className) ) {
            return true; // Always an instance of yourself!
        }

        for ( String interfaceName : interfaceNames ) {
            if ( className.equals(interfaceName) ) {
                return true;
            }
        }

        if ( isInterface() ) {
            return false; // No super class to check.
        }
        
        ClassInfo useSuperClass = getSuperclass();
        return ( (useSuperClass != null) && useSuperClass.isInstanceOf(className) );
    }

    @Override
    public boolean isAssignableFrom(String className) {
        String useName = getName();
        if ( useName.equals(className) ) {
            return true; // Quick test: Always assignable from yourself.
        } else {
            return getInfoStore().getDelayableClassInfo(className).isInstanceOf(useName);
        }
    }

    //

    protected void storeFields(List<FieldInfoImpl> fields) {
        if ( (fields == null) || fields.isEmpty() ) {
            declaredFields = Collections.emptyList();
        } else {
            declaredFields = new ArrayList<>(fields);
        }
    }

    @Override
    public List<FieldInfoImpl> getDeclaredFields() {
        return declaredFields;
    }

    //
    
    protected void storeConstructors(List<MethodInfoImpl> constructors) {
        if ( (constructors == null) || constructors.isEmpty() ) {
            declaredConstructors = Collections.emptyList();
        } else {
            declaredConstructors = new ArrayList<>(constructors);
        }
    }

    @Override
    public List<MethodInfoImpl> getDeclaredConstructors() {
        return declaredConstructors;
    }

    //

    public void storeMethods(List<MethodInfoImpl> methods) {
        if ( (methods == null) || methods.isEmpty() ) {
            declaredMethods = Collections.emptyList();
        } else {
            declaredMethods = new ArrayList<>(methods);
        }
    }

    @Override
    public List<MethodInfoImpl> getDeclaredMethods() {
        return declaredMethods;
    }

    private static final Comparator<MethodInfoImpl> METHOD_COMPARATOR = new Comparator<MethodInfoImpl>() {
        @Override
        public int compare(MethodInfoImpl m1, MethodInfoImpl m2) {
            int result = m1.getName().compareTo(m2.getName());
            if ( result == 0 ) {
                result = m1.getDescription().compareTo(m2.getDescription() );

                // Compare using the full description, which includes both the parameter types
                // and the return type.

                // A duplication of name plus parameter types is possible, because of cases with
                // BRIDGE methods.

                // result = NonDelayedClassInfoImpl.compare( m1.getParameterTypeNames(), m2.getParameterTypeNames() );
                // compare the return types
            }
            return result;
        }
    };

    protected static int compare(List<String> names1, List<String> names2) {
        Iterator<String> useNames1 = names1.iterator();
        Iterator<String> useNames2 = names2.iterator();

        while ( useNames1.hasNext() ) {
            if ( !useNames2.hasNext() ) {
                return +1;
            }
            String useName1 = useNames1.next();
            String useName2 = useNames2.next();
            int comparison = useName1.compareTo(useName2);
            if ( comparison != 0 ) {
                return comparison;
            }
        }
        if ( useNames2.hasNext() ) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * Answer all of the visible methods of this class.  That is, all of the methods
     * of this class and its superclasses, omitting private methods of superclasses,
     * omitting package private methods of superclasses which are in other packages,
     * and replacing methods which are overloaded. 
     */
    @Override
    public List<MethodInfoImpl> getMethods() {
        LinkedList<MethodInfoImpl> allMethods = new LinkedList<MethodInfoImpl>();

        // All declared methods are present automatically.
        allMethods.addAll(declaredMethods);

        // for ( MethodInfoImpl methodInfo : declaredMethods ) {
        //     System.out.println("Class [ " + getName() + " ]");;
        //     System.out.println("  Declared [ " + methodInfo + " ]");
        // }

        ClassInfoImpl useSuperClass = getSuperclass();
        if ( useSuperClass != null ) {
            // 'overloading' methods are methods which mask methods declared
            // in superclasses.
            Map<MethodInfoImpl, MethodInfoImpl> overloadingMethods;
            if ( !declaredMethods.isEmpty() ) {
                overloadingMethods = new TreeMap<MethodInfoImpl, MethodInfoImpl>(METHOD_COMPARATOR);
                for ( MethodInfoImpl declaredMethod : declaredMethods ) {
                    // Private methods aren't put in the overloading methods
                    // collection because private methods in superclasses
                    // are entirely skipped when adding superclass methods.
                    if ( !declaredMethod.isPrivate() ) {
                        overloadingMethods.put(declaredMethod, declaredMethod);
                    }
                }
            } else {
                overloadingMethods = Collections.emptyMap();
            }

            // for ( MethodInfoImpl overloadingMethod : declaredMethods ) {
            //     System.out.println("Class [ " + getName() + " ]");
            //     System.out.println("   Block Overload [ " + overloadingMethod + " ]");
            // }

            // TODO: There is a potential problem here.  Consider:
            //
            //     package1 class Gen1 method m1
            //     package2 class Gen2 extends Gen1
            //     package1 class Gen3 extends Gen2
            //
            // Method 'm1' is not present in the methods of Gen3 because of
            // because of the transition from package1 to package2 then back to package1.

            // Let the superclass do a share of the work.
            List<MethodInfoImpl> superMethods = useSuperClass.getMethods();

            // for ( MethodInfoImpl superMethod : superMethods ) {
            //     System.out.println("Class [ " + getName() + " ]");
            //     System.out.println("  Super [ " + superMethod + " ]");
            // }
            
            // Then add the superclass methods which are not package private,
            // which are not private, and which are not overloaded.

            String usePackageName = getPackageName();

            for ( MethodInfoImpl superMethod : superMethods ) {
                if ( superMethod.isPackagePrivate() &&
                     !superMethod.getDeclaringClass().getPackageName().equals(usePackageName) ) {
                    // System.out.println("Package private [ " + superMethod + " ]");
                    continue; // Not visible: Super method is package private and declared in a different package.

                } else if ( superMethod.isPrivate() ) {
                    // System.out.println("Private [ " + superMethod + " ]");
                    continue; // Not visible: Super method is private.

                } else if ( overloadingMethods.get(superMethod) != null ) {
                    // System.out.println("Overridden [ " + superMethod + " ]");
                    continue; // Overridden: The override is already in the results collection.
                }

                // System.out.println("Keep [ " + superMethod + " ]");
                allMethods.add(superMethod); // Visible and not overridden.
            }
        }

        return allMethods;
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

    //

    private List<AnnotationInfoImpl> annotations; // declared + inherited

    @Override
    public void storeDeclaredAnnotations(List<AnnotationInfoImpl> annos) {
        this.annotations = null;

        super.storeDeclaredAnnotations(annos);
    }

    @Override
    public List<AnnotationInfoImpl> getAnnotations() {
        if ( annotations == null ) {
            ClassInfoImpl useSuperClass = getSuperclass();

            if ( useSuperClass == null ) {
                annotations = declaredAnnotations;

            } else {
                List<AnnotationInfoImpl> allAnnotations = new ArrayList<AnnotationInfoImpl>( declaredAnnotations.size() );
                Set<String> declaredAnnotationNames = new HashSet<String>( declaredAnnotations.size() );
                for ( AnnotationInfoImpl declaredAnnotation : declaredAnnotations ) {
                    declaredAnnotationNames.add( declaredAnnotation.getAnnotationClassName() );
                    allAnnotations.add(declaredAnnotation);
                }

                for ( AnnotationInfoImpl superAnnotation : useSuperClass.getAnnotations() ) {
                    if ( superAnnotation.isInherited() ) {
                        if ( !declaredAnnotationNames.contains( superAnnotation.getAnnotationClassName() ) ) {
                            allAnnotations.add(superAnnotation);
                        }
                    }
                }

                if ( allAnnotations.isEmpty() ) {
                    annotations = Collections.emptyList();
                } else {
                    annotations = new ArrayList<AnnotationInfoImpl>(allAnnotations);
                }
            }
        }

        return annotations;
    }

    //

    protected DelayedClassInfoImpl delayedClassInfo;

    public void setDelayedClassInfo(DelayedClassInfoImpl delayedClassInfo) {
        this.delayedClassInfo = delayedClassInfo;
    }

    public DelayedClassInfoImpl getDelayedClassInfo() {
        return delayedClassInfo;
    }

    //

    protected NonDelayedClassInfoImpl priorClassInfo;
    protected NonDelayedClassInfoImpl nextClassInfo;

    public NonDelayedClassInfoImpl setPriorClassInfo(NonDelayedClassInfoImpl newPriorClassInfo) {
        NonDelayedClassInfoImpl oldPriorClassInfo = priorClassInfo;
        priorClassInfo = newPriorClassInfo;
        return oldPriorClassInfo;
    }

    public NonDelayedClassInfoImpl getPriorClassInfo() {
        return priorClassInfo;
    }

    public NonDelayedClassInfoImpl setNextClassInfo(NonDelayedClassInfoImpl newNextClassInfo) {
        NonDelayedClassInfoImpl oldNextClassInfo = nextClassInfo;
        nextClassInfo = newNextClassInfo;
        return oldNextClassInfo;
    }

    public NonDelayedClassInfoImpl getNextClassInfo() {
        return nextClassInfo;
    }

    //

    @Override
    public void log(Logger useLogger) {
        String methodName = "log";
        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                    " Non-Delayed Class [ {0} ]",
                    getHashText());
        useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                    "  delayedClassInfo [ {0} ]",
                    ((delayedClassInfo == null) ? null : delayedClassInfo.getHashText()));

        if ( !useLogger.isLoggable(Level.FINEST) ) {
            return;
        }

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isInterface [ {0} ]", Boolean.valueOf(isInterface));

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  packageName [ {0} ]", packageName);
        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  packageInfo [ {0} ]", ((packageInfo == null) ? null : packageInfo.getHashText()));

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isJavaClass [ {0} ]", Boolean.valueOf(isJavaClass));

        if (interfaceNames == null) {
            useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  interfaceNames [ null ]");
        } else {
            for (String interfaceName : interfaceNames) {
                useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  interfaceName [ {0} ]", interfaceName);
            }
        }

        if (interfaces == null) {
            useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  interfaces [ null ]");
        } else {
            for (ClassInfoImpl interfaceInfo : interfaces) {
                useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  interface [ {0} ]", interfaceInfo.getHashText());
            }
        }

        //

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  superClassName [ {0} ]", superClassName);
        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  superClass [ {0} ]", ((superClass == null) ? null : superClass.getHashText()));

        if (declaredFields == null) {
            useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  declaredFields [ null ]");
        } else {
            for (FieldInfoImpl fieldInfo : declaredFields) {
                useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  declaredFields [ {0} ]", fieldInfo.getHashText());
            }
        }

        if (declaredConstructors == null) {
            useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  declaredConstructors [ null ]");
        } else {
            for (MethodInfoImpl methodInfo : declaredConstructors) {
                useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  declaredConstructors [ {0} ]", methodInfo.getHashText());
            }
        }

        if (declaredMethods == null) {
            useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  declaredMethods [ null ]");
        } else {
            for (MethodInfoImpl methodInfo : declaredMethods) {
                useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  declaredMethods [ {0} ]", methodInfo.getHashText());
            }
        }

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isFieldAnnotationPresent [ {0} ]", Boolean.valueOf(isFieldAnnotationPresent));
        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isMethodAnnotationPresent [ {0} ]", Boolean.valueOf(isMethodAnnotationPresent));

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  delayedClassInfo [ {0} ]", ((delayedClassInfo == null) ? null : delayedClassInfo.getHashText()));
        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  priorClassInfo [ {0} ]", ((priorClassInfo == null) ? null : priorClassInfo.getHashText()));
        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  nextClassInfo [ {0} ]", ((nextClassInfo == null) ? null : nextClassInfo.getHashText()));

        logAnnotations(useLogger);

        if (declaredConstructors != null) {
            for (MethodInfoImpl methodInfo : declaredConstructors) {
                methodInfo.log(useLogger);
            }
        }

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, " Non-Delayed Class [ {0} ]", getHashText());
    }

    //

    @Override
    public void log(TraceComponent useLogger) {

        Tr.debug(useLogger, MessageFormat.format(" Non-Delayed Class [ {0} ]", getHashText()));
        Tr.debug(useLogger, MessageFormat.format("  delayedClassInfo [ {0} ]", ((delayedClassInfo == null) ? null : delayedClassInfo.getHashText())));

        if (!useLogger.isDumpEnabled()) {
            return;
        }

        Tr.dump(useLogger, MessageFormat.format("  isInterface [ {0} ]", Boolean.valueOf(isInterface)));

        Tr.dump(useLogger, MessageFormat.format("  packageName [ {0} ]", packageName));
        Tr.dump(useLogger, MessageFormat.format("  packageInfo [ {0} ]", ((packageInfo == null) ? null : packageInfo.getHashText())));

        Tr.dump(useLogger, MessageFormat.format("  isJavaClass [ {0} ]", Boolean.valueOf(isJavaClass)));

        if (interfaceNames == null) {
            Tr.dump(useLogger, "  interfaceNames [ null ]");
        } else {
            for (String interfaceName : interfaceNames) {
                Tr.dump(useLogger, MessageFormat.format("  interfaceName [ {0} ]", interfaceName));
            }
        }

        if (interfaces == null) {
            Tr.dump(useLogger, "  interfaces [ null ]");
        } else {
            for (ClassInfoImpl interfaceInfo : interfaces) {
                Tr.dump(useLogger, MessageFormat.format("  interface [ {0} ]", interfaceInfo.getHashText()));
            }
        }

        //

        Tr.dump(useLogger, MessageFormat.format("  superClassName [ {0} ]", superClassName));
        Tr.dump(useLogger, MessageFormat.format("  superClass [ {0} ]", ((superClass == null) ? null : superClass.getHashText())));

        if (declaredFields == null) {
            Tr.dump(useLogger, "  declaredFields [ null ]");
        } else {
            for (FieldInfoImpl fieldInfo : declaredFields) {
                Tr.dump(useLogger, MessageFormat.format("  declaredFields [ {0} ]", fieldInfo.getHashText()));
            }
        }

        if (declaredConstructors == null) {
            Tr.dump(useLogger, "  declaredConstructors [ null ]");
        } else {
            for (MethodInfoImpl methodInfo : declaredConstructors) {
                Tr.dump(useLogger, MessageFormat.format("  declaredConstructors [ {0} ]", methodInfo.getHashText()));
            }
        }

        if (declaredMethods == null) {
            Tr.dump(useLogger, "  declaredMethods [ null ]");
        } else {
            for (MethodInfoImpl methodInfo : declaredMethods) {
                Tr.dump(useLogger, MessageFormat.format("  declaredMethods [ {0} ]", methodInfo.getHashText()));
            }
        }

        Tr.dump(useLogger, MessageFormat.format("  isFieldAnnotationPresent [ {0} ]", Boolean.valueOf(isFieldAnnotationPresent)));
        Tr.dump(useLogger, MessageFormat.format("  isMethodAnnotationPresent [ {0} ]", Boolean.valueOf(isMethodAnnotationPresent)));

        Tr.dump(useLogger, MessageFormat.format("  delayedClassInfo [ {0} ]", ((delayedClassInfo == null) ? null : delayedClassInfo.getHashText())));
        Tr.dump(useLogger, MessageFormat.format("  priorClassInfo [ {0} ]", ((priorClassInfo == null) ? null : priorClassInfo.getHashText())));
        Tr.dump(useLogger, MessageFormat.format("  nextClassInfo [ {0} ]", ((nextClassInfo == null) ? null : nextClassInfo.getHashText())));

        logAnnotations(useLogger);

        if (declaredConstructors != null) {
            for (MethodInfoImpl methodInfo : declaredConstructors) {
                methodInfo.log(useLogger);
            }
        }

        Tr.dump(useLogger, MessageFormat.format(" Non-Delayed Class [ {0} ]", getHashText()));
    }
}
