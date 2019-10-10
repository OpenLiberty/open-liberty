/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.info.internal;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.anno.info.ClassInfo;

public class DelayedClassInfo extends ClassInfoImpl {

    private static final TraceComponent tc = Tr.register(DelayedClassInfo.class);
    public static final String CLASS_NAME = DelayedClassInfo.class.getName();

    // Top O' the World ...

    public DelayedClassInfo(String name, InfoStoreImpl infoStore) {
        super(name, 0, infoStore);

        this.packageName = getInfoStore().internPackageName(ClassInfoImpl.getPackageName(name));
        this.packageInfo = null;

        this.isJavaClass = ClassInfoImpl.isJavaClass(name);

        this.classInfo = null;
        this.isArtificial = false;

        if (tc.isDebugEnabled()) {
            setLogParms();
            Tr.debug(tc, MessageFormat.format("[ {0} ] Created for ", getHashText()));
        }
    }

    //

    protected String[] logParms;

    public static final int HASH_OFFSET = 0;
    public static final int EXTRA_DATA_OFFSET_0 = 1;
    public static final int EXTRA_DATA_OFFSET_1 = 2;

    public String[] getLogParms() {
        return logParms;
    }

    protected String[] setLogParms() {
        logParms =
                        new String[] {
                                      getHashText(),
                                      null,
                                      null };

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

    protected NonDelayedClassInfo classInfo;
    protected boolean isArtificial;

    public void setClassInfo(NonDelayedClassInfo classInfo) {

        String[] useLogParms = getLogParms();
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

            Tr.debug(tc, MessageFormat.format("[ {0} ] - Update class info [ {1} ] to [ {2} ]",
                                              (Object[]) logParms));
        }

        this.classInfo = classInfo;
    }

    // The extra steps for accessing and updating the log parms are to insure that there are
    // no collisions between the use here and use in other methods.
    //
    // Assignments to the extra data of the log parms may not be held across other method calls.

    public NonDelayedClassInfo getClassInfo() {

        String useName = getName();

        NonDelayedClassInfo useClassInfo = this.classInfo;

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

        String[] useLogParms = getLogParms();
        if (useLogParms != null) {
            useLogParms[EXTRA_DATA_OFFSET_0] = useName;
        }

        if (useClassInfo == null) {
            // defect 84235: If an error caused this, the warning was reported at a lower level. Since this is 
            // going to be 'handled', we should not generate another warning message.
            //Tr.warning(tc, "ANNO_CLASSINFO_CLASS_NOTFOUND", getHashText(), useName); // CWWKC0025W

            if (useLogParms != null) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] - Class not found [ {1} ]",
                                                  (Object[]) useLogParms));
            }

            useClassInfo = new NonDelayedClassInfo(useName, getInfoStore());

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
                Tr.debug(tc, MessageFormat.format("[ {0} ] RETURN [ {1} ] as [ {2} ] ** ARTFICIAL **",
                                                  (Object[]) useLogParms));
            } else {
                Tr.debug(tc, MessageFormat.format("[ {0} ] RETURN [ {1} ] as [ {2} ] ",
                                                  (Object[]) useLogParms));
            }
        }

        // END: TFB: Defect 59284: Don't throw an exception for a failed load.

        return useClassInfo;
    }

    @Override
    public boolean isArtificial() {
        if (this.classInfo == null) {
            NonDelayedClassInfo useClassInfo = getClassInfo();

            ClassInfoImpl.consumeRef(useClassInfo);
            // 'useClassInfo' is only obtained to cause 'isArtificial' to be set.
        }

        return this.isArtificial;
    }

    // Delegating getters ...

    protected boolean isModifiersSet;

    @Override
    public int getModifiers() {
        if (!isModifiersSet) {
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
            packageInfo = getInfoStore().getPackageInfo(packageName, ClassInfoCache.DO_FORCE_PACKAGE);
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
        if (interfaceNames == null) {
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

        return isInterface;
    }

    @Override
    public boolean isAnnotationClass() {
        if (isAnnotationClass == null) {
            isAnnotationClass = getClassInfo().isAnnotationClass();
        }

        return isAnnotationClass;
    }

    //

    // The superclass name is interned.
    // The superclass is a delayed class.

    protected String superclassName;

    protected ClassInfoImpl superclass;

    @Override
    public String getSuperclassName() {
        if (superclassName == null) {
            NonDelayedClassInfo useClassInfo = getClassInfo();
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
        if (getName().equals(className)) {
            return true; // Quick test: Always assignable from yourself.
        } else {
            return getInfoStore().getDelayableClassInfo(className).isInstanceOf(this.getName());
        }
    }

    //
    Boolean isEmptyDeclaredFields;

    @Override
    public List<FieldInfoImpl> getDeclaredFields() {
        if (isEmptyDeclaredFields == null || !isEmptyDeclaredFields) {
            List<FieldInfoImpl> useDeclaredFields = getClassInfo().getDeclaredFields();
            isEmptyDeclaredFields = useDeclaredFields.isEmpty();

            return useDeclaredFields;
        }

        return Collections.emptyList();
    }

    protected Boolean isEmptyDeclaredConstructors;

    @Override
    public List<MethodInfoImpl> getDeclaredConstructors() {
        if (isEmptyDeclaredConstructors == null || !isEmptyDeclaredConstructors) {
            List<MethodInfoImpl> useDeclaredConstructors = getClassInfo().getDeclaredConstructors();
            isEmptyDeclaredConstructors = useDeclaredConstructors.isEmpty();
            return useDeclaredConstructors;
        }

        return Collections.emptyList();
    }

    Boolean isEmptyDeclaredMethods;
    Boolean isEmptyMethods;

    @Override
    public List<MethodInfoImpl> getDeclaredMethods() {
        if (isEmptyDeclaredMethods == null || !isEmptyDeclaredMethods) {
            List<MethodInfoImpl> useDeclaredMethods = getClassInfo().getDeclaredMethods();
            isEmptyDeclaredMethods = useDeclaredMethods.isEmpty();

            return useDeclaredMethods;
        }
        return Collections.emptyList();
    }

    @Override
    public List<MethodInfoImpl> getMethods() {
        if (isEmptyMethods == null || !isEmptyMethods) {
            List<MethodInfoImpl> useMethods = getClassInfo().getMethods();
            isEmptyMethods = useMethods.isEmpty();

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
        if (isDeclaredAnnotationPresent == null) {
            isDeclaredAnnotationPresent = getClassInfo().isDeclaredAnnotationPresent();
        }

        return isDeclaredAnnotationPresent;
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
        if (isAnnotationPresent == null) {
            isAnnotationPresent = getClassInfo().isAnnotationPresent();
        }

        return isAnnotationPresent;
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
        if (isFieldAnnotationPresent == null) {
            isFieldAnnotationPresent = getClassInfo().isFieldAnnotationPresent();
        }

        return isFieldAnnotationPresent;
    }

    protected Boolean isMethodAnnotationPresent;

    @Override
    public boolean isMethodAnnotationPresent() {
        if (isMethodAnnotationPresent == null) {
            isMethodAnnotationPresent = getClassInfo().isMethodAnnotationPresent();
        }
        return isMethodAnnotationPresent;
    }

    @Override
    @Trivial
    public void log(TraceComponent logger) {

        Tr.debug(logger, MessageFormat.format("Delayed Class [ {0} ]", getHashText()));

        Tr.debug(logger, MessageFormat.format("  classInfo [ {0} ]", ((classInfo != null) ? classInfo.getHashText() : null)));
        Tr.debug(logger, MessageFormat.format("  isArtificial [ {0} ]", Boolean.valueOf(isArtificial)));

        // only write out the remainder of the messages if 'all' trace is enabled
        if (!logger.isDumpEnabled()) {
            return;
        }

        Tr.dump(logger, MessageFormat.format("  isModifiersSet [ {0} ]", Boolean.valueOf(isModifiersSet)));
        Tr.dump(logger, MessageFormat.format("  modifiers [ {0} ]", Integer.valueOf(modifiers)));

        Tr.dump(logger, MessageFormat.format("  packageName [ {0} ]", packageName));
        Tr.dump(logger, MessageFormat.format("  packageInfo [ {0} ]", ((packageInfo != null) ? packageInfo.getHashText() : null)));

        Tr.dump(logger, MessageFormat.format("  isJavaClass [ {0} ]", Boolean.valueOf(isJavaClass)));

        if (interfaceNames != null) {
            for (String interfaceName : interfaceNames) {
                Tr.dump(logger, MessageFormat.format("    [ {0} ]", interfaceName));
            }
        }

        Tr.dump(logger, MessageFormat.format("  isInterface [ {0} ]", isInterface));

        Tr.dump(logger, MessageFormat.format("  isAnnotationClass [ {0} ]", isAnnotationClass));

        Tr.dump(logger, MessageFormat.format("  superclassName [ {0} ]", superclassName));

        Tr.dump(logger, MessageFormat.format("  superclass [ {0} ]", ((superclass != null) ? superclass.getHashText() : null)));

        Tr.dump(logger, MessageFormat.format("  isEmptyDeclaredFields [ {0} ]", isEmptyDeclaredFields));

        Tr.dump(logger, MessageFormat.format("  isEmptyDeclaredConstructors [ {0} ]", isEmptyDeclaredConstructors));

        Tr.dump(logger, MessageFormat.format("  isEmptyDeclaredMethods [ {0} ]", isEmptyDeclaredMethods));

        Tr.dump(logger, MessageFormat.format("  isEmptyMethods [ {0} ]", isEmptyMethods));

        Tr.dump(logger, MessageFormat.format("  isDeclaredAnnotationPresent [ {0} ]", isDeclaredAnnotationPresent));
        Tr.dump(logger, MessageFormat.format("  isAnnotationPresent [ {0} ]", isAnnotationPresent));

        Tr.dump(logger, MessageFormat.format("  isFieldAnnotationPresent [ {0} ]", isFieldAnnotationPresent));
        Tr.dump(logger, MessageFormat.format("  isMethodAnnotationPresent [ {0} ]", isMethodAnnotationPresent));

        // Don't log the underlying non-delayed class.
        // Don't log the annotations.
    }
}
