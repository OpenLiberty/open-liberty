/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.info.internal;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.annocache.info.ClassInfo;

public class DelayedClassInfoImpl extends ClassInfoImpl {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.info");
    private static final String CLASS_NAME = DelayedClassInfoImpl.class.getSimpleName();

    // Top O' the World ...

    public DelayedClassInfoImpl(String name, InfoStoreImpl infoStore) {
        super(name, 0, infoStore);

        String methodName = "<init>";

        this.packageName = getInfoStore().internPackageName(ClassInfoImpl.getPackageName(name));
        this.packageInfo = null;

        this.isJavaClass = ClassInfoImpl.isJavaClass(name);

        this.classInfo = null;
        this.isArtificial = false;

        if (logger.isLoggable(Level.FINER)) {
            setLogParms();
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Created for ", getHashText());
        }
    }

    //

    protected Object[] logParms;

    public static final int HASH_OFFSET = 0;
    public static final int EXTRA_DATA_OFFSET_0 = 1;
    public static final int EXTRA_DATA_OFFSET_1 = 2;

    public Object[] getLogParms() {
        return logParms;
    }

    protected Object[] setLogParms() {
        logParms = new Object[] { getHashText(), null, null };
        return logParms;
    }

    //

    @Override
    public boolean isDelayedClass() {
        return true;
    }

    @Override
    public ClassInfoImpl asDelayedClass() {
        return this;
    }

    //

    protected NonDelayedClassInfoImpl classInfo;
    protected boolean isArtificial;

    public void setClassInfo(NonDelayedClassInfoImpl classInfo) {
        String methodName = "setClassInfo";

        Object[] useLogParms = getLogParms();
        if (useLogParms != null) {
            if (this.classInfo != null) {
                useLogParms[EXTRA_DATA_OFFSET_0] = this.classInfo.getHashText();
            } else {
                useLogParms[EXTRA_DATA_OFFSET_0] = null;
            }

            if (classInfo != null) {
                useLogParms[EXTRA_DATA_OFFSET_1] = classInfo.getHashText();
            } else {
                useLogParms[EXTRA_DATA_OFFSET_1] = null;
            }

            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] - Update class info [ {1} ] to [ {2} ]", useLogParms);
        }

        this.classInfo = classInfo;
    }

    // The extra steps for accessing and updating the log parms are to insure that there are
    // no collisions between the use here and use in other methods.
    //
    // Assignments to the extra data of the log parms may not be held across other method calls.

    public NonDelayedClassInfoImpl getClassInfo() {
        String methodName = "getClassInfo";

        String useName = getName();

        NonDelayedClassInfoImpl useClassInfo = this.classInfo;

        if (useClassInfo != null) {
            if (!useClassInfo.isJavaClass() &&
                !(useClassInfo.isAnnotationPresent() ||
                  useClassInfo.isFieldAnnotationPresent() ||
                useClassInfo.isMethodAnnotationPresent())) {
                getInfoStore().recordAccess(useClassInfo);
            }
            return useClassInfo;
        }

        useClassInfo = getInfoStore().resolveClassInfo(useName);

        Object[] useLogParms = getLogParms();
        if (useLogParms != null) {
            useLogParms[EXTRA_DATA_OFFSET_0] = useName;
        }

        if (useClassInfo == null) {
            // defect 84235: If an error caused this, the warning was reported at a lower level. Since this is 
            // going to be 'handled', we should not generate another warning message.
            // logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CLASSINFO_CLASS_NOTFOUND",
            //     new Object[] { getHashText(), useName }); // CWWKC0025W

            if (useLogParms != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] - Class not found [ {1} ]", useLogParms);
            }

            useClassInfo = new NonDelayedClassInfoImpl(useName, getInfoStore());

            this.isArtificial = true;

            // START: TFB: Defect 59284: More required updates:
            //
            // The newly created class info was not properly added as a class info,
            // leading to an NPE in the cache list management (in ClassInfoCache.makeFirst).

            getInfoStore().getClassInfoCache().addClassInfo(useClassInfo);

            // END: TFB: Defect 59284: More required updates:
        }

        // START: TFB: Defect 59284: Don't throw an exception for a failed load.
        //
        // Tracing enablement for defect 59284 showed an error of the sequence
        // of assigning the result class info hash text into the logging parameters.
        //
        // Prior to the assignment steps, 'this.classInfo' is null.
        //
        // A NullPointerException results, respective only of trace enablement
        // and irrespective of the d59284 changes.
        //
        // Move the logging code to after the 'this.classInfo' assignment.

        // if (useLogParms != null) {
        //     useLogParms[EXTRA_DATA_OFFSET_1] = this.classInfo.getHashText();
        // }

        useClassInfo.setDelayedClassInfo(this);
        this.classInfo = useClassInfo;

        if (useLogParms != null) {
            useLogParms[EXTRA_DATA_OFFSET_1] = this.classInfo.getHashText(); // D59284

            if (this.isArtificial) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN [ {1} ] as [ {2} ] ** ARTFICIAL **", useLogParms);
            } else {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN [ {1} ] as [ {2} ] ", useLogParms);
            }
        }

        // END: TFB: Defect 59284: Don't throw an exception for a failed load.

        return useClassInfo;
    }

    @Override
    public boolean isArtificial() {
        if ( classInfo == null ) {
            NonDelayedClassInfoImpl useClassInfo = getClassInfo();
            ClassInfoImpl.consumeRef(useClassInfo);
            // 'useClassInfo' is only obtained to cause 'isArtificial' to be set.
        }
        return isArtificial;
    }

    // Delegating getters ...

    protected boolean isModifiersSet;

    @Override
    public int getModifiers() {
        if ( !isModifiersSet ) {
            setModifiers(getClassInfo().getModifiers());
            isModifiersSet = true;
        }
        return super.getModifiers();
    }

    //

    protected String packageName;
    protected PackageInfoImpl packageInfo;

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public PackageInfoImpl getPackage() {
        if (packageInfo == null && (packageName != null)) {
            packageInfo = getInfoStore().getPackageInfo(packageName, ClassInfoCacheImpl.DO_FORCE_PACKAGE);
        }
        return packageInfo;
    }

    protected boolean isJavaClass;

    @Override
    public boolean isJavaClass() {
        return isJavaClass;
    }

    // The interface names are interned.
    //
    // A clear and reset of the class info will create duplicate sets.

    private List<String> interfaceNames;

    @Override
    public List<String> getInterfaceNames() {
        if ( interfaceNames == null ) {
            interfaceNames = getClassInfo().getInterfaceNames();
        }
        return interfaceNames;
    }

    @Override
    public List<ClassInfoImpl> getInterfaces() {
        return getClassInfo().getInterfaces();
    }

    protected Boolean isInterface;
    protected Boolean isAnnotationClass;

    @Override
    public boolean isInterface() {
        if (isInterface == null) {
            isInterface = Boolean.valueOf(getClassInfo().isInterface());
        }
        return isInterface.booleanValue();
    }

    @Override
    public boolean isAnnotationClass() {
        if ( isAnnotationClass == null ) {
            isAnnotationClass = Boolean.valueOf( getClassInfo().isAnnotationClass() );
        }
        return isAnnotationClass.booleanValue();
    }

    //

    // The superclass name is interned.
    // The superclass is a delayed class.

    protected String superclassName;

    protected ClassInfoImpl superclass;

    @Override
    public String getSuperclassName() {
        if (superclassName == null) {
            NonDelayedClassInfoImpl useClassInfo = getClassInfo();
            superclassName = useClassInfo.getSuperclassName();

            // TODO:
            //
            // This case should not occur once the replacement function is updated.
            //
            // What is happening is that the class is failing to resolve, leading to
            // an association of a delayed class info for the failed class to
            // the non-delayed class info for java.lang.Object.
            //
            // That is:
            //
            // Delayed("some.failed.class") --> NonDelayed("java.lang.Object")
            //
            // java.lang.object has a null superclass (and is the only non-interface
            // for which this is the case).

            if ((superclassName == null) &&
                !getName().equals(ClassInfo.OBJECT_CLASS_NAME) &&
                !useClassInfo.getName().equals(ClassInfo.OBJECT_CLASS_NAME) &&
                !isInterface()) {

                // TODO: What to do here?  Is this check now obsolete?

            }
        }

        return superclassName;
    }

    @Override
    public ClassInfoImpl getSuperclass() {
        if (superclass == null) {
            String useSuperclassName = getSuperclassName();
            if (useSuperclassName != null) {
                superclass = getDelayableClassInfo(getSuperclassName());
            } else {
                superclass = null;
            }

            // superclass = getClassInfo().getSuperclass();
        }

        return superclass;
    }

    @Override
    public boolean isInstanceOf(String className) {
        if (getName().equals(className)) {
            // Always an instance of yourself!
            return true;

        }

        for (String iName : getInterfaceNames()) {
            if (iName.equals(className)) {
                return true;
            }
        }

        if (isInterface()) {
            // If an interface, there is no superclass, so testing is complete.
            return false;
        }

        // Always an instance of one of your super classes.
        ClassInfo useSuperClass = getSuperclass();
        return ((useSuperClass != null) && useSuperClass.isInstanceOf(className));
    }

    @Override
    public boolean isAssignableFrom(String className) {
        String useClassName = getName();
        if ( useClassName.equals(className) ) {
            return true; // Quick test: Always assignable from yourself.
        } else {
            return getInfoStore().getDelayableClassInfo(className).isInstanceOf(useClassName);
        }
    }

    //

    Boolean isEmptyDeclaredFields;

    @Override
    public List<FieldInfoImpl> getDeclaredFields() {
        if ( (isEmptyDeclaredFields == null) || !isEmptyDeclaredFields.booleanValue() ) {
            List<FieldInfoImpl> useDeclaredFields = getClassInfo().getDeclaredFields();
            isEmptyDeclaredFields = Boolean.valueOf( useDeclaredFields.isEmpty() );
            return useDeclaredFields;
        }
        return Collections.emptyList();
    }

    protected Boolean isEmptyDeclaredConstructors;

    @Override
    public List<MethodInfoImpl> getDeclaredConstructors() {
        if ( (isEmptyDeclaredConstructors == null) || !isEmptyDeclaredConstructors.booleanValue() ) {
            List<MethodInfoImpl> useDeclaredConstructors = getClassInfo().getDeclaredConstructors();
            isEmptyDeclaredConstructors = Boolean.valueOf( useDeclaredConstructors.isEmpty() );
            return useDeclaredConstructors;
        }

        return Collections.emptyList();
    }

    Boolean isEmptyDeclaredMethods;
    Boolean isEmptyMethods;

    @Override
    public List<MethodInfoImpl> getDeclaredMethods() {
        if ( (isEmptyDeclaredMethods == null) || !isEmptyDeclaredMethods.booleanValue() ) {
            List<MethodInfoImpl> useDeclaredMethods = getClassInfo().getDeclaredMethods();
            isEmptyDeclaredMethods = Boolean.valueOf( useDeclaredMethods.isEmpty() );

            return useDeclaredMethods;
        }
        return Collections.emptyList();
    }

    @Override
    public List<MethodInfoImpl> getMethods() {
        if ( (isEmptyMethods == null) || !isEmptyMethods.booleanValue() ) {
            List<MethodInfoImpl> useMethods = getClassInfo().getMethods();
            isEmptyMethods = Boolean.valueOf( useMethods.isEmpty() );

            return useMethods;
        }
        return Collections.emptyList();
    }

    protected Boolean isDeclaredAnnotationPresent;

    @Override
    public List<AnnotationInfoImpl> getDeclaredAnnotations() {
        return getClassInfo().getDeclaredAnnotations();
    }

    @Override
    public boolean isDeclaredAnnotationPresent() {
        if ( isDeclaredAnnotationPresent == null ) {
            isDeclaredAnnotationPresent = Boolean.valueOf( getClassInfo().isDeclaredAnnotationPresent() );
        }
        return isDeclaredAnnotationPresent.booleanValue();
    }

    @Override
    public boolean isDeclaredAnnotationPresent(String annotationName) {
        return getClassInfo().isDeclaredAnnotationPresent(annotationName);
    }

    @Override
    public AnnotationInfoImpl getDeclaredAnnotation(String annotationClassName) {
        return getClassInfo().getDeclaredAnnotation(annotationClassName);
    }

    //

    protected Boolean isAnnotationPresent;

    @Override
    public boolean isAnnotationPresent() {
        if ( isAnnotationPresent == null ) {
            isAnnotationPresent = Boolean.valueOf( getClassInfo().isAnnotationPresent() );
        }
        return isAnnotationPresent.booleanValue();
    }

    @Override
    public List<AnnotationInfoImpl> getAnnotations() {
        return getClassInfo().getAnnotations();
    }

    @Override
    public boolean isAnnotationPresent(String annotationName) {
        return getClassInfo().isAnnotationPresent(annotationName);
    }

    @Override
    public AnnotationInfoImpl getAnnotation(String annotationName) {
        return getClassInfo().getAnnotation(annotationName);
    }

    //

    protected Boolean isFieldAnnotationPresent;

    @Override
    public boolean isFieldAnnotationPresent() {
        if ( isFieldAnnotationPresent == null ) {
            isFieldAnnotationPresent = Boolean.valueOf( getClassInfo().isFieldAnnotationPresent() );
        }
        return isFieldAnnotationPresent.booleanValue();
    }

    protected Boolean isMethodAnnotationPresent;

    @Override
    public boolean isMethodAnnotationPresent() {
        if ( isMethodAnnotationPresent == null ) {
            isMethodAnnotationPresent = Boolean.valueOf( getClassInfo().isMethodAnnotationPresent() );
        }
        return isMethodAnnotationPresent.booleanValue();
    }

    @Override
    public void log(Logger useLogger) {
        String methodName = "log";
        
        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Delayed Class [ {0} ]", getHashText());

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  classInfo [ {0} ]", ((classInfo != null) ? classInfo.getHashText() : null));
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  isArtificial [ {0} ]", Boolean.valueOf(isArtificial));

        // only write out the remainder of the messages if 'all' trace is enabled
        if ( !useLogger.isLoggable(Level.FINEST) ) {
            return;
        }

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isModifiersSet [ {0} ]", Boolean.valueOf(isModifiersSet));
        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  modifiers [ {0} ]", Integer.valueOf(modifiers));

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  packageName [ {0} ]", packageName);
        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  packageInfo [ {0} ]", ((packageInfo != null) ? packageInfo.getHashText() : null));

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isJavaClass [ {0} ]", Boolean.valueOf(isJavaClass));

        if (interfaceNames != null) {
            for (String interfaceName : interfaceNames) {
                useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "    [ {0} ]", interfaceName);
            }
        }

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isInterface [ {0} ]", isInterface);

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isAnnotationClass [ {0} ]", isAnnotationClass);

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  superclassName [ {0} ]", superclassName);

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  superclass [ {0} ]", ((superclass != null) ? superclass.getHashText() : null));

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isEmptyDeclaredFields [ {0} ]", isEmptyDeclaredFields);

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isEmptyDeclaredConstructors [ {0} ]", isEmptyDeclaredConstructors);

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isEmptyDeclaredMethods [ {0} ]", isEmptyDeclaredMethods);

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isEmptyMethods [ {0} ]", isEmptyMethods);

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isDeclaredAnnotationPresent [ {0} ]", isDeclaredAnnotationPresent);
        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isAnnotationPresent [ {0} ]", isAnnotationPresent);

        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isFieldAnnotationPresent [ {0} ]", isFieldAnnotationPresent);
        useLogger.logp(Level.FINEST, CLASS_NAME, methodName, "  isMethodAnnotationPresent [ {0} ]", isMethodAnnotationPresent);

        // Don't log the underlying non-delayed class.
        // Don't log the annotations.
    }

    @Override
    @Trivial
    public void log(TraceComponent tc) {

        Tr.debug(tc, MessageFormat.format("Delayed Class [ {0} ]", getHashText()));

        Tr.debug(tc, MessageFormat.format("  classInfo [ {0} ]", ((classInfo != null) ? classInfo.getHashText() : null)));
        Tr.debug(tc, MessageFormat.format("  isArtificial [ {0} ]", Boolean.valueOf(isArtificial)));

        // only write out the remainder of the messages if 'all' trace is enabled
        if (!tc.isDumpEnabled()) {
            return;
        }

        Tr.dump(tc, MessageFormat.format("  isModifiersSet [ {0} ]", Boolean.valueOf(isModifiersSet)));
        Tr.dump(tc, MessageFormat.format("  modifiers [ {0} ]", Integer.valueOf(modifiers)));

        Tr.dump(tc, MessageFormat.format("  packageName [ {0} ]", packageName));
        Tr.dump(tc, MessageFormat.format("  packageInfo [ {0} ]", ((packageInfo != null) ? packageInfo.getHashText() : null)));

        Tr.dump(tc, MessageFormat.format("  isJavaClass [ {0} ]", Boolean.valueOf(isJavaClass)));

        if (interfaceNames != null) {
            for (String interfaceName : interfaceNames) {
                Tr.dump(tc, MessageFormat.format("    [ {0} ]", interfaceName));
            }
        }

        Tr.dump(tc, MessageFormat.format("  isInterface [ {0} ]", isInterface));

        Tr.dump(tc, MessageFormat.format("  isAnnotationClass [ {0} ]", isAnnotationClass));

        Tr.dump(tc, MessageFormat.format("  superclassName [ {0} ]", superclassName));

        Tr.dump(tc, MessageFormat.format("  superclass [ {0} ]", ((superclass != null) ? superclass.getHashText() : null)));

        Tr.dump(tc, MessageFormat.format("  isEmptyDeclaredFields [ {0} ]", isEmptyDeclaredFields));

        Tr.dump(tc, MessageFormat.format("  isEmptyDeclaredConstructors [ {0} ]", isEmptyDeclaredConstructors));

        Tr.dump(tc, MessageFormat.format("  isEmptyDeclaredMethods [ {0} ]", isEmptyDeclaredMethods));

        Tr.dump(tc, MessageFormat.format("  isEmptyMethods [ {0} ]", isEmptyMethods));

        Tr.dump(tc, MessageFormat.format("  isDeclaredAnnotationPresent [ {0} ]", isDeclaredAnnotationPresent));
        Tr.dump(tc, MessageFormat.format("  isAnnotationPresent [ {0} ]", isAnnotationPresent));

        Tr.dump(tc, MessageFormat.format("  isFieldAnnotationPresent [ {0} ]", isFieldAnnotationPresent));
        Tr.dump(tc, MessageFormat.format("  isMethodAnnotationPresent [ {0} ]", isMethodAnnotationPresent));

        // Don't log the underlying non-delayed class.
        // Don't log the annotations.
    }
}
