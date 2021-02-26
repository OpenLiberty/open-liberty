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

package com.ibm.ws.anno.targets.internal;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.ws.anno.util.internal.UtilImpl_BidirectionalMap;
import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.ws.anno.util.internal.UtilImpl_InternMap;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Fault;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.anno.util.Util_BidirectionalMap;
import com.ibm.wsspi.anno.util.Util_Factory;
import com.ibm.wsspi.anno.util.Util_InternMap;

public class AnnotationTargetsImpl_Targets implements AnnotationTargets_Targets {
    private static final TraceComponent tc = Tr.register(AnnotationTargetsImpl_Targets.class);
    public static final String CLASS_NAME = AnnotationTargetsImpl_Targets.class.getName();

    protected final String hashText;

    public String getHashText() {
        return hashText;
    }

    //

    protected AnnotationTargetsImpl_Targets(AnnotationTargets_Factory factory,
                                            Util_InternMap classInternMap,
                                            boolean isDetailEnabled) {
        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this);

        this.factory = factory;

        //

        // Intern map for class source names;
        // Not entirely necessary, but allows reuse of the Bidi map.
        this.classSourceInternMap = createInternMap(Util_InternMap.ValueType.VT_OTHER, "class sources");

        // Mapping of class source name to class names.
        // Not entirely proper, as classes can arise from at most one class source.
        this.classSourceClassData = createBidiMap("class source", classSourceInternMap,
                                                  "class", classInternMap,
                                                  Util_BidirectionalMap.IS_ENABLED);

        this.classInternMap = classInternMap; // Holds both class and package names

        //

        // Class tables ... these span the result regions.

        this.i_scannedClassNames = new IdentityHashMap<String, String>();
        this.i_superclassNameMap = new IdentityHashMap<String, String>();
        this.i_interfaceNameMap = new IdentityHashMap<String, String[]>();

        this.isDetailEnabled = isDetailEnabled; // To enable recording field and method results

        // TODO:
        //
        // 1) Fold each of these data sets into a data structure; re-use that data structure.
        // 2) Split the data sets into data source specific structures.
        // 3) Create a facade for the result selection.  Use the facade to encapsulate and hide
        //    the scan policy selection variable. 

        this.seedData = new AnnotationTargetsImpl_PolicyData(this, classInternMap, ScanPolicy.SEED, isDetailEnabled);
        this.partialData = new AnnotationTargetsImpl_PolicyData(this, classInternMap, ScanPolicy.PARTIAL, isDetailEnabled);
        this.excludedData = new AnnotationTargetsImpl_PolicyData(this, classInternMap, ScanPolicy.EXCLUDED, isDetailEnabled);
        this.externalData = new AnnotationTargetsImpl_PolicyData(this, classInternMap, ScanPolicy.EXTERNAL, isDetailEnabled);

        // Extra tracking data.  The set of referenced classes is used only
        // during scan processing, and is empty at the conclusion of processing.

        this.i_referencedClassNames = new IdentityHashMap<String, String>();

        this.i_unresolvedPackageNames = new IdentityHashMap<String, String>();
        this.i_unresolvedClassNames = new IdentityHashMap<String, String>();

        //

        this.visitor = new AnnotationTargetsVisitor(this);

        // Derived from the i_superclassNamesMap, which will therefore
        // need to be populated before the i_subclassNameMap
        this.i_descendantsMap = null;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Visitor [ {1} ]",
                                              new Object[] { this.hashText, this.visitor.getHashText() }));
        }
    }

    //

    protected final AnnotationTargets_Factory factory;

    @Override
    public AnnotationTargets_Factory getFactory() {
        return factory;
    }

    protected AnnotationTargets_Fault createFault(String unresolvedText, String[] parameters) {
        return getFactory().createFault(unresolvedText, parameters);
    }

    protected AnnotationTargetsImpl_Scanner createScanner(ClassSource_Aggregate classSource) throws AnnotationTargets_Exception {
        return new AnnotationTargetsImpl_Scanner(getFactory(), classSource, this);
    }

    protected UtilImpl_BidirectionalMap createBidiMap(String holderTag, Util_InternMap holderInternMap,
                                                      String heldTag, Util_InternMap heldInternMap,
                                                      boolean isEnabled) {

        return ((UtilImpl_Factory) getFactory().getUtilFactory()).createBidirectionalMap(holderTag, holderInternMap,
                                                                    heldTag, heldInternMap,
                                                                    isEnabled);
    }

    protected Util_InternMap createInternMap(Util_InternMap.ValueType valueType, String mapName) {
        return getFactory().getUtilFactory().createInternMap(valueType, mapName);
    }

    //

    protected final Util_InternMap classSourceInternMap;

    protected Util_InternMap getClassSourceInternMap() {
        return classSourceInternMap;
    }

    protected String internClassSourceName(String classSourceName) {
        return getClassSourceInternMap().intern(classSourceName);
    }

    protected String internClassSourceName(String className, boolean doForce) {
        return getClassSourceInternMap().intern(className, doForce);
    }

    //

    // State0: Table created, but no scan requested.

    // State1: The root class source was set.  This
    //         is tested as ('haveScannedReferencedClasses'
    //         || (classSource != null)).
    protected ClassSource_Aggregate rootClassSource;

    // State2: At least one non-external class source
    //         has been provided.  Adding a direct class  
    //         requires that the root class source is set. 
    protected int directClassSourceCount;

    // State3: Direct classes have been scanned.  Requires
    //         that at least one direct class source has
    //         been provided.
    protected boolean haveScannedDirectClasses;

    // State4: At least one external class source has been
    //         provided.  Requires that at least one direct
    //         class source is set.
    protected int externalClassSourceCount;

    // State5: Reference classes have been scanned.  Requires
    //         That direct classes have been scanned and at
    //         least one external class source is set.
    protected boolean haveScannedReferencedClasses;

    public boolean activated() {
        return (haveScannedReferencedClasses || (rootClassSource != null));
    }

    public ClassSource_Aggregate getRootClassSource() {
        return rootClassSource;
    }

    protected void setRootClassSource(ClassSource_Aggregate classSource) {
        rootClassSource = classSource;

        Set<? extends ClassSource> seedClassSources = classSource.getSeedClassSources();
        Set<? extends ClassSource> partialClassSources = classSource.getPartialClassSources();
        Set<? extends ClassSource> excludedClassSources = classSource.getExcludedClassSources();
        Set<? extends ClassSource> externalClassSources = classSource.getExternalClassSources();

        directClassSourceCount =
                        seedClassSources.size() + partialClassSources.size() + excludedClassSources.size();
        externalClassSourceCount =
                        externalClassSources.size();
    }

    public int getDirectClassSourceCount() {
        return directClassSourceCount;
    }

    public boolean scannedDirectClasses() {
        return haveScannedDirectClasses;
    }

    public int getExternalClassSourceCount() {
        return externalClassSourceCount;
    }

    public boolean scannedReferencedClasses() {
        return haveScannedReferencedClasses;
    }

    @Override
    public void addClassSource(ClassSource source, ScanPolicy scanPolicy) {
        if (source == null) {
            throw new IllegalArgumentException("Null added class source");
        }

        // These should be errors.

        if (haveScannedReferencedClasses) {
            if (tc.isDebugEnabled()) {
                String eMsg = MessageFormat.format("Attempt to add class source [ {0} ] [ {1} ] after both direct and reference scans", source, scanPolicy);
                Tr.debug(tc, eMsg);
            }

        } else if (haveScannedDirectClasses && (scanPolicy != ScanPolicy.EXTERNAL)) {
            if (tc.isDebugEnabled()) {
                String eMsg = MessageFormat.format("Non-external class source [ {0} ] [ {1} ] added after the direct scan", source, scanPolicy);
                Tr.debug(tc, eMsg);
            }

        } else if (rootClassSource == null) {
            if (tc.isDebugEnabled()) {
                String eMsg = MessageFormat.format("Attempt to add class source [ {0} ] [ {1} ] before activation", source, scanPolicy);
                Tr.debug(tc, eMsg);
            }

        } else {
            if (scanPolicy == ScanPolicy.EXTERNAL) {
                if (directClassSourceCount == 0) {
                    if (tc.isDebugEnabled()) {
                        String eMsg = MessageFormat.format("Strange addition external class source [ {0} ] [ {1} ] with no direct class sources", source, scanPolicy);
                        Tr.debug(tc, eMsg);
                    }
                }

                externalClassSourceCount++;

            } else if (externalClassSourceCount != 0) {
                if (tc.isDebugEnabled()) {
                    String eMsg = MessageFormat.format("Strange addition direct class source [ {0} ] [ {1} ] after an external class source", source, scanPolicy);
                    Tr.debug(tc, eMsg);
                }

                directClassSourceCount++;
            }

            if (tc.isDebugEnabled()) {
                String eMsg = MessageFormat.format("Adding class source [ {0} ] [ {1} ]", source, scanPolicy);
                Tr.debug(tc, eMsg);
            }

            rootClassSource.addClassSource(source, scanPolicy);
        }
    }

    protected void scanReferenceClasses() {
        if (haveScannedReferencedClasses) {
            return;
        }

        try {
            doScanReferenceClasses(); // throws AnnotationTargets_Exception

        } catch (AnnotationTargets_Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception scanning referenced classes for annotations");
                Tr.debug(tc, MessageFormat.format("[ {0} ]", e.toString()));
            }
        }
    }

    // PostCondition: haveScannedReferencedClasses == true
    // PostCondition: rootClassSource == null

    protected void doScanReferenceClasses() throws AnnotationTargets_Exception {
        if (haveScannedReferencedClasses) {
            return;
        }

        try {
            doScanDirectClasses();

        } catch (AnnotationTargets_Exception e) {
            haveScannedReferencedClasses = true;
            rootClassSource = null;

            throw e;
        }

        haveScannedReferencedClasses = true;

        ClassSource_Aggregate useRootClassSource = rootClassSource;
        rootClassSource = null;

        if (useRootClassSource == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Attempt scan referenced classes before activation");
            }

        } else {
            if (externalClassSourceCount == 0) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Strange call to scan reference classes with no external class sources");
                }
            }

            createScanner(useRootClassSource).scanReferenced();
            // 'createScanner' and 'scan' both throw AnnotationTargets_Exception
        }
    }

    protected void scanDirectClasses() {
        if (haveScannedDirectClasses) {
            return;
        }

        try {
            doScanDirectClasses(); // throws AnnotationTargets_Exception            

        } catch (AnnotationTargets_Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception scanning referenced classes for annotations");
                Tr.debug(tc, MessageFormat.format("[ {0} ]", e.toString()));
            }
        }
    }

    // PostCondition: haveScannedDirectClasses == true

    protected void doScanDirectClasses() throws AnnotationTargets_Exception {
        if (haveScannedDirectClasses) {
            return;
        }

        haveScannedDirectClasses = true;

        if (rootClassSource == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Call to scan direct classes before activation");
            }

        } else {
            if (directClassSourceCount == 0) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Strange call to scan direct classes before adding direct class sources");
                }
            }

            createScanner(rootClassSource).scanDirect();
            // 'createScanner' and 'scanDirect' both throw AnnotationTargets_Exception
        }
    }

    //

    @Override
    public void scan(ClassSource_Aggregate classSource, boolean scanImmediate) throws AnnotationTargets_Exception {
        if (haveScannedReferencedClasses) {
            if (tc.isDebugEnabled()) {
                String eMsg = MessageFormat.format("Scan requested on class source [ {0} ] but a reference scan has already been performed", classSource);
                Tr.debug(tc, eMsg);
            }

        } else if (rootClassSource != null) {
            if (tc.isDebugEnabled()) {
                String eMsg = MessageFormat.format("Scan requested on class source [ {0} ] but root class source [ {1} ] is already set", classSource, rootClassSource);
                Tr.debug(tc, eMsg);
            }

        } else {
            setRootClassSource(classSource);

            if (scanImmediate) {
                doScanDirectClasses(); // throws AnnotationTargets_Exception
                doScanReferenceClasses(); // throws AnnotationTargets_Exception
            }
        }
    }

    @Override
    public void scan(ClassSource_Aggregate classSource) throws AnnotationTargets_Exception {
        this.scan(classSource, false);
    }

    // PostCondition: haveScannedDirectClasses == true
    // PostCondition: haveScannedReferencedClasses == true    

    @Override
    public void scan(ClassSource_Aggregate classSource, Set<String> specificClassNames) throws AnnotationTargets_Exception {
        try {
            createScanner(classSource).scan(specificClassNames);
            // 'createScanner' throws class source exception

        } finally {
            haveScannedDirectClasses = true;
            haveScannedReferencedClasses = true;
        }
    }

    //

    protected final Util_InternMap classInternMap;

    public Util_InternMap getClassInternMap() {
        return classInternMap;
    }

    public String internClassName(String className) {
        return getClassInternMap().intern(className);
    }

    public String internClassName(String className, boolean doForce) {
        return getClassInternMap().intern(className, doForce);
    }

    // Class source call back:
    //
    // A scan entry to the class source will call back to 'scanClass' after retrieving
    // the input stream for a class.
    //
    // Class source entry is through one of 'scanDirectClasses', 'scanSpecifiedSeedClasses',
    // or 'scanReferencedClasses'.
    //
    // The class name is the external class name of the class, as computed from the
    // resource name.
    //
    // The 'class' is a class resource (a resource having the signature of a class file,
    // that is, which has the extension ".class").  The class resource may represent
    // a java package or a java class.

    // Entry is from:
    //     AnnotationTargetsImpl_Streamer.process(String, InputStream, boolean, boolean, boolean)

    @FFDCIgnore({ AnnotationTargetsVisitor.VisitEnded.class, ArrayIndexOutOfBoundsException.class })
    protected boolean scanClass(String classSourceName,
                                String className,
                                InputStream inputStream,
                                ScanPolicy scanPolicy) {

        Object[] logParms;
        if (tc.isDebugEnabled()) {
            logParms = new Object[] { getHashText(), className, null };
            Tr.debug(tc, MessageFormat.format("[ {0} ] ENTER class [ {1} ]", logParms));
        } else {
            logParms = null;
        }

        AnnotationTargetsVisitor useVisitor = getVisitor();

        useVisitor.i_setClassSourceName(internClassSourceName(classSourceName));

        // For non-seed scans, tell the visitor to only process the
        // class relationship information.  Skip class annotations and
        // and class contents (fields and methods).

        useVisitor.setScanPolicy(scanPolicy);
        useVisitor.setExternalName(className);

        boolean failedScan;

        try {
            // Both IOException and Exception have been seen;
            // in particular ArrayindexOutOfBoundsException for a non-valid class.

            ClassReader classReader = new ClassReader(inputStream); // throws IOException, Exception

            classReader.accept(getVisitor(),
                               (ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE));
            // 'accept' throws IOException, VisitEnded, Exception

            failedScan = false;

            if (logParms != null) {
                logParms[2] = "Success";
            }

        } catch (IOException e) {
            failedScan = true;

            // String eMsg = "[ " + getHashText() + " ] Class [ " + className + " ] Exception creating reader";
            Tr.warning(tc, "ANNO_TARGETS_FAILED_TO_CREATE_READER", className); // CWWKC0049W

        } catch (AnnotationTargetsVisitor.VisitEnded e) {
            // 'VisitEnded' is used both for a normal flow control for
            // skipping detailed scan data (which is not a failed scan)
            // and for other cases, such as duplicate scan requests
            // and name mismatch cases, which are failures.

            failedScan = !e.isDetailCase();

            if (logParms != null) {
                logParms[2] = "Halted: " + e.getEndCase();
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            Tr.info(tc, "ANNO_TARGETS_CORRUPT_CLASS", className, classSourceName);
            failedScan = true;

        } catch (Exception e) {
            // String eMsg = "[ " + getHashText() + " ] Visit failure [ " + className + " ]";
            // CWWKC0044W: An exception occurred while scanning class and annotation data.
            Tr.warning(tc, "ANNO_TARGETS_SCAN_EXCEPTION", e);

            if (logParms != null) {
                logParms[2] = "Exception: " + e.getMessage();
            }

            failedScan = true;
        }

        if (logParms != null) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] RETURN From class [ {1} ]: [ {2} ]", logParms));
        }

        return (!failedScan);
    }

    protected final AnnotationTargetsVisitor visitor;

    protected AnnotationTargetsVisitor getVisitor() {
        return visitor;
    }

    //

    // Record a class as being unresolvable.
    //
    // This is used to record referenced classes which could not be
    // scanned.  The class must be removed from the referenced classes
    // list, but must be remembered, so not to add it to the referenced
    // classes list by a later processing step.

    protected void recordUnresolvedClass(String classOrPackageName) {
        if (!AnnotationTargetsVisitor.isPackageName(classOrPackageName)) {
            String i_className = internClassName(classOrPackageName);

            // Make sure to not double record the class:
            // A failure occur *after* recording the class.

            if (!i_containsScannedClassName(i_className)) {
                i_addUnresolvedClassName(i_className);
            }
        }
    }

    //

    protected final Map<String, String> i_unresolvedPackageNames;

    public Set<String> getUnresolvedPackageNames() {
        scanReferenceClasses();

        // 18994: The result set must not be identity based.
        return new HashSet<String>( i_unresolvedPackageNames.keySet() );
    }

    protected boolean i_addUnresolvedPackageName(String i_packageName) {
        return (i_unresolvedPackageNames.put(i_packageName, i_packageName) == null);
    }

    //

    protected boolean i_addScannedClassName(String i_className, ScanPolicy scanPolicy) {
        boolean result = (i_scannedClassNames.put(i_className, i_className) == null);

        doGetPolicyData(scanPolicy).i_addScannedClassName(i_className);

        return result;
    }

    protected final Map<String, String> i_scannedClassNames;

    public Set<String> getScannedClassNames() {
        scanReferenceClasses();

        // 18994: The result set must not be identity based.
        return new HashSet<String>( i_scannedClassNames.keySet() );
    }

    public boolean i_containsScannedClassName(String i_className) {
        scanDirectClasses();

        return i_scannedClassNames.containsKey(i_className);
    }

    @Override
    public Set<String> getSeedClassNames() {
        scanDirectClasses();

        // 18994: The result set must not be identity based.
        return new HashSet<String>( seedData.getClassNames() );
    }

    @Override
    public boolean isSeedClassName(String className) {
        scanDirectClasses();

        String i_className = getClassInternMap().intern(className, Util_InternMap.DO_NOT_FORCE);
        if (i_className == null) {
            return false;
        } else {
            return seedData.i_isClassName(i_className);
        }
    }

    @Override
    public Set<String> getPartialClassNames() {
        scanDirectClasses();

        // 18994: The result set must not be identity based.
        return new HashSet<String>( partialData.getClassNames() );
    }

    @Override
    public boolean isPartialClassName(String className) {
        scanDirectClasses();

        String i_className = getClassInternMap().intern(className, Util_InternMap.DO_NOT_FORCE);
        if (i_className == null) {
            return false;
        }
        return partialData.i_isClassName(i_className);
    }

    @Override
    public Set<String> getExcludedClassNames() {
        scanDirectClasses();

        // 18994: The result set must not be identity based.
        return new HashSet<String>( excludedData.getClassNames() );
    }

    @Override
    public boolean isExcludedClassName(String className) {
        scanDirectClasses();

        String i_className = getClassInternMap().intern(className, Util_InternMap.DO_NOT_FORCE);
        if (i_className == null) {
            return false;
        }
        return excludedData.i_isClassName(i_className);
    }

    @Override
    public Set<String> getExternalClassNames() {
        // External classes are not visited until the referenced
        // scan is performed.        
        scannedReferencedClasses();

        // 18994: The result set must not be identity based.
        return new HashSet<String>( externalData.getClassNames() );
    }

    @Override
    public boolean isExternalClassName(String className) {
        // External classes are not visited until the referenced
        // scan is performed.                
        scannedReferencedClasses();

        String i_className = getClassInternMap().intern(className, Util_InternMap.DO_NOT_FORCE);
        if (i_className == null) {
            return false;
        }
        return externalData.i_isClassName(i_className);
    }

    @Override
    public Set<String> getClassNames(int scanPolicy) {
        return selectClassNames(scanPolicy);
    }

    //

    protected Map<String, String> i_referencedClassNames;

    protected void i_removeReferencedClassName(String i_className) {
        i_referencedClassNames.remove(i_className);
    }

    protected boolean i_addReferencedClassName(String i_className) {
        if (i_scannedClassNames.containsKey(i_className)) {
            return false;
        }

        if (i_unresolvedClassNames.containsKey(i_className)) {
            return false;
        }

        // System.out.println("Referenced class [ " + i_className + " ]");

        return (i_referencedClassNames.put(i_className, i_className) == null);
    }

    public Set<String> getReferencedClassNames() {
        // External classes are not visited until the referenced
        // scan is performed.                
        scanReferenceClasses();

        Set<String> i_retrievedClassNames = i_referencedClassNames.keySet();
        i_referencedClassNames = new IdentityHashMap<String, String>();
        return i_retrievedClassNames;
    }

    //

    protected final Map<String, String> i_unresolvedClassNames;

    public Set<String> getUnresolvedClassNames() {
        scanReferenceClasses();

        return i_unresolvedClassNames.keySet();
    }

    protected boolean i_addUnresolvedClassName(String i_className) {
        return (i_unresolvedClassNames.put(i_className, i_className) == null);
    }

    //

    protected final UtilImpl_BidirectionalMap classSourceClassData;

    protected boolean i_placeClass(String i_classSourceName, String i_className) {
        return classSourceClassData.i_record(i_classSourceName, i_className);
    }

    protected Util_BidirectionalMap getClassSourceClassData() {
        return classSourceClassData;
    }

    public Set<String> getScannedClassNames(String classSourceName) {
        scanReferenceClasses();

        return getClassSourceClassData().selectHeldOf(classSourceName);
    }

    public String getClassClassSourceName(String className) {
        scanReferenceClasses();

        Set<String> classSourceNames = getClassSourceClassData().selectHoldersOf(className);
        for (String classSourceName : classSourceNames) {
            return classSourceName;
        }
        return null;
    }

    //

    @Override
    public Set<String> getAnnotatedPackages() {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotatedTargets(AnnotationCategory.PACKAGE) );
    }

    @Override
    public Set<String> getAnnotatedPackages(String annotationName) {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotatedTargets(annotationName, AnnotationCategory.PACKAGE) );
    }

    @Override
    public Set<String> getPackageAnnotations() {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotations(AnnotationCategory.PACKAGE) );
    }

    @Override
    public Set<String> getPackageAnnotations(String packageName) {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotations(packageName, AnnotationCategory.PACKAGE) );
    }

    @Override
    public Set<String> getAnnotatedClasses() {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotatedTargets(AnnotationCategory.CLASS) );
    }

    @Override
    public Set<String> getAnnotatedClasses(String annotationName) {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotatedTargets(annotationName, AnnotationCategory.CLASS) );
    }

    @Override
    public Set<String> getClassAnnotations() {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotations(AnnotationCategory.CLASS) );
    }

    @Override
    public Set<String> getClassAnnotations(String className) {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotations(className, AnnotationCategory.CLASS) );
    }

    public Set<String> getClassesWithFieldAnnotations() {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotatedTargets(AnnotationCategory.FIELD) );
    }

    @Override
    public Set<String> getClassesWithFieldAnnotation(String annotationName) {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotatedTargets(annotationName, AnnotationCategory.FIELD) );
    }

    @Override
    public Set<String> getFieldAnnotations() {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotations(AnnotationCategory.FIELD) );
    }

    @Override
    public Set<String> getFieldAnnotations(String className) {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotations(className, AnnotationCategory.FIELD) );
    }

    public Set<String> getClassesWithMethodAnnotations() {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotatedTargets(AnnotationCategory.METHOD) );
    }

    @Override
    public Set<String> getClassesWithMethodAnnotation(String annotationName) {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotatedTargets(annotationName, AnnotationCategory.METHOD) );
    }

    @Override
    public Set<String> getMethodAnnotations() {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotations(AnnotationCategory.METHOD) );
    }

    @Override
    public Set<String> getMethodAnnotations(String className) {
        scanDirectClasses();

        return new HashSet<String>( seedData.getAnnotations(className, AnnotationCategory.METHOD) );
    }

    //

    @Override
    public Set<String> getAnnotatedPackages(int scanPolicies) {
        return selectAnnotatedTargets(scanPolicies, AnnotationCategory.PACKAGE);
    }

    @Override
    public Set<String> getAnnotatedPackages(String annotationName, int scanPolicies) {
        return selectAnnotatedTargets(annotationName, scanPolicies, AnnotationCategory.PACKAGE);
    }

    @Override
    public Set<String> getPackageAnnotations(int scanPolicies) {
        return selectAnnotations(scanPolicies, AnnotationCategory.PACKAGE);
    }

    @Override
    public Set<String> getPackageAnnotations(String packageName, int scanPolicies) {
        return selectAnnotations(packageName, scanPolicies, AnnotationCategory.PACKAGE);
    }

    //

    @Override
    public Set<String> getAnnotatedClasses(int scanPolicies) {
        return selectAnnotatedTargets(scanPolicies, AnnotationCategory.CLASS);
    }

    @Override
    public Set<String> getAnnotatedClasses(String annotationName, int scanPolicies) {
        return selectAnnotatedTargets(annotationName, scanPolicies, AnnotationCategory.CLASS);
    }

    @Override
    public Set<String> getClassAnnotations(int scanPolicies) {
        return selectAnnotations(scanPolicies, AnnotationCategory.CLASS);
    }

    @Override
    public Set<String> getClassAnnotations(String className, int scanPolicies) {
        return selectAnnotations(className, scanPolicies, AnnotationCategory.CLASS);
    }

    //

    @Override
    public Set<String> getClassesWithFieldAnnotations(int scanPolicies) {
        return selectAnnotatedTargets(scanPolicies, AnnotationCategory.FIELD);
    }

    @Override
    public Set<String> getClassesWithFieldAnnotation(String annotationName, int scanPolicies) {
        return selectAnnotatedTargets(annotationName, scanPolicies, AnnotationCategory.FIELD);
    }

    @Override
    public Set<String> getFieldAnnotations(int scanPolicies) {
        return selectAnnotations(scanPolicies, AnnotationCategory.FIELD);
    }

    @Override
    public Set<String> getFieldAnnotations(String className, int scanPolicies) {
        return selectAnnotations(className, scanPolicies, AnnotationCategory.FIELD);
    }

    //

    @Override
    public Set<String> getClassesWithMethodAnnotations(int scanPolicies) {
        return selectAnnotatedTargets(scanPolicies, AnnotationCategory.METHOD);
    }

    @Override
    public Set<String> getClassesWithMethodAnnotation(String annotationName, int scanPolicies) {
        return selectAnnotatedTargets(annotationName, scanPolicies, AnnotationCategory.METHOD);
    }

    @Override
    public Set<String> getMethodAnnotations(int scanPolicies) {
        return selectAnnotations(scanPolicies, AnnotationCategory.METHOD);
    }

    @Override
    public Set<String> getMethodAnnotations(String className, int scanPolicies) {
        return selectAnnotations(className, scanPolicies, AnnotationCategory.METHOD);
    }

    //

    @Override
    public Set<String> getAnnotatedClasses(String classSourceName, String annotationName) {
        Set<String> annotatedClassNames = getAnnotatedClasses(annotationName);
        Set<String> classSourceClassNames = getScannedClassNames(classSourceName);

        return (restrict(annotatedClassNames, classSourceClassNames));
    }

    @Override
    public Set<String> getAnnotatedClasses(String classSourceName, String annotationName, int scanPolicies) {

        Set<String> annotatedClassNames = getAnnotatedClasses(annotationName, scanPolicies);
        Set<String> classSourceClassNames = getScannedClassNames(classSourceName);

        return (restrict(annotatedClassNames, classSourceClassNames));
    }

    //

    @Override
    public Set<String> getAllInheritedAnnotatedClasses(String annotationName) {
        scanDirectClasses();

        Set<String> allClassNames = new HashSet<String>();

        // For each class which has the specified annotation as a class annotation ...

        for (String className : getAnnotatedClasses(annotationName)) {
            // Add that class as a declared target ...
            allClassNames.add(className);

            // And add all subclasses as targets ...
            allClassNames.addAll(getSubclassNames(className));

            // The result of 'getSubclassnames' can never be null for
            // a class which is recorded as a declared class annotation
            // target.  That is only possible for a class which was
            // scanned, and such never answer null from 'getSubclassNames'.
        }

        return allClassNames;
    }

    @Override
    public Set<String> getAllInheritedAnnotatedClasses(String annotationName, int scanResults) {
        return getAllInheritedAnnotatedClasses(annotationName, scanResults, scanResults);
    }

    @Override
    public Set<String> getAllInheritedAnnotatedClasses(String annotationName,
                                                       int declarerScanPolicies,
                                                       int inheritorScanPolicies) {

        scanDirectClasses();

        Set<String> allClassNames = new HashSet<String>();

        // For each class which has the specified annotation as a class annotation ...

        for (String className : getAnnotatedClasses(annotationName, declarerScanPolicies)) {
            // Add that class as a declared target ...
            allClassNames.add(className);

            // And add all subclasses as targets ...
            allClassNames.addAll(getSubclassNames(className));

            // The result of 'getSubclassnames' can never be null for
            // a class which is recorded as a declared class annotation
            // target.  That is only possible for a class which was
            // scanned, and such never answer null from 'getSubclassNames'.
        }

        Set<String> regionClassNames = getClassNames(inheritorScanPolicies);

        return restrict(allClassNames, regionClassNames);
    }

    //

    protected final Map<String, String> i_superclassNameMap;

    public String i_getSuperclassName(String i_subclassName) {
        return i_superclassNameMap.get(i_subclassName);
    }

    @Override
    public String getSuperclassName(String subclassName) {
        return i_getSuperclassName(internClassName(subclassName));
    }

    public Map<String, String> getSuperclassNames() {
        scanReferenceClasses();

        return i_superclassNameMap; // TODO: Should this be un-interned?
    }

    // TODO should this clear the subclass names map?

    // When reading from serialization, don't set the referenced classes.
    protected void i_setSuperclassName(String i_subclassName, String i_superclassName) {
        i_superclassNameMap.put(i_subclassName, i_superclassName);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Subclass [ {1} ] has superclass [ {2} ]",
                                              new Object[] { getHashText(), i_subclassName, i_superclassName }));
        }
    }

    //

    protected Map<String, String[]> i_interfaceNameMap;

    protected String[] i_getInterfaceNames(String i_classOrInterfaceName) {
        return i_interfaceNameMap.get(i_classOrInterfaceName);
    }

    public String[] getInterfaceNames(String classOrInterfaceName) {
        scanReferenceClasses();

        return i_getInterfaceNames(internClassName(classOrInterfaceName));
    }

    public Map<String, String[]> getInterfaceNames() {
        scanReferenceClasses();

        return i_interfaceNameMap; // TODO: Should this be un-interned?
    }

    protected void i_setInterfaceNames(String i_classOrInterfaceName, String[] i_interfaceNames) {
        i_interfaceNameMap.put(i_classOrInterfaceName, i_interfaceNames);

        if (tc.isDebugEnabled()) {
            for (String i_interfaceName : i_interfaceNames) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Child [ {1} ] has interface [ {2} ]",
                                                  new Object[] { getHashText(), i_classOrInterfaceName, i_interfaceName }));
            }
        }
    }

    //
    // Map direction is: implementer to implemented
    protected IdentityHashMap<String, Set<String>> i_allImplementersMap;

    @Override
    public Set<String> getAllImplementorsOf(String interfaceName) {
        scanReferenceClasses();

        String i_interfaceName = getClassInternMap().intern(interfaceName, Util_InternMap.DO_NOT_FORCE);
        if (i_interfaceName == null) {
            return Collections.emptySet(); // No data is available for a result.
        }

        if (this.i_allImplementersMap == null) {
            createAllImplementersMap();
        }

        // Map direction is: implementer to implemented
        Set<String> result = this.i_allImplementersMap.get(i_interfaceName);
        if (result == null) {
            result = Collections.emptySet();
        } else {
            // 18994: The result set must not be identity based.
            result = new HashSet<String>(result);
        }

        return result;
    }

    protected void createAllImplementersMap() {
        Util_InternMap useClassNameMap = getClassInternMap();
        Util_Factory useUtilFactory = useClassNameMap.getFactory();

        // Map direction is: implementer to implemented
        this.i_allImplementersMap = new IdentityHashMap<String, Set<String>>();

        // Do NOT use the direct interface map to seed table generation;
        // the direct interface map only locates classes which are immediate
        // implementers.
        //
        // All classes which are immediate implementers ... or which are subclasses ...
        // must be scanned!  A subclass may be an indirect implementer, through
        // one of it's superclasses.

        // A class implements the interfaces that it declares,
        // and implements and superinterfaces of the interfaces that it declares,
        // and implements the interfaces implemented by its superclasses.

        for (String i_implementerName : this.i_scannedClassNames.keySet()) {
            // System.out.println("Recording implements of [ " + i_implementerName + " ]");

            String i_nextImplementsSource = i_implementerName;

            while (i_nextImplementsSource != null) {
                String[] i_immediateImplements = this.i_interfaceNameMap.get(i_nextImplementsSource);
                if (i_immediateImplements != null) {
                    for (String i_immediateImplement : i_immediateImplements) {
                        Set<String> implementers = i_allImplementersMap.get(i_immediateImplement);
                        if (implementers == null) {
                            implementers = useUtilFactory.createIdentityStringSet();
                            i_allImplementersMap.put(i_immediateImplement, implementers);
                        }
                        implementers.add(i_implementerName);
                    }
                }

                i_nextImplementsSource = this.i_superclassNameMap.get(i_nextImplementsSource);
            }
        }
    }

    // Map direction is: superclass to subclass
    protected IdentityHashMap<String, Set<String>> i_descendantsMap;

    @Override
    public Set<String> getSubclassNames(String superclassName) {
        scanReferenceClasses();

        String i_superclassName = getClassInternMap().intern(superclassName, Util_InternMap.DO_NOT_FORCE);
        if (i_superclassName == null) {
            return Collections.emptySet();
        }

        if (i_descendantsMap == null) {
            createDescendantsMap();
        }

        // Map direction is: superclass to subclass
        Set<String> result = i_descendantsMap.get(i_superclassName);
        if (result == null) {
            result = Collections.emptySet();
        } else {
            // 18994: The result set must not be identity based.
            result = new HashSet<String>(result);
        }
        return result;
    }

    protected void createDescendantsMap() {
        Util_InternMap useClassNameMap = getClassInternMap();
        Util_Factory useUtilFactory = useClassNameMap.getFactory();

        // Map direction is: superclass to subclass
        this.i_descendantsMap = new IdentityHashMap<String, Set<String>>();

        // For each class recorded as a subclass ...
        for (String i_subclassName : i_superclassNameMap.keySet()) {

            // Start with the immediate super class of that subclass, and walk
            // upwards to the ascendant super classes.
            //
            // For each super class, either as an immediate super class or as an
            // ancestral super class, record the subclass as a descendant of the
            // superclass.

            String i_superclassName = i_subclassName;
            while ((i_superclassName = i_superclassNameMap.get(i_superclassName)) != null) {
                Set<String> subclasses = i_descendantsMap.get(i_superclassName);
                if (subclasses == null) {
                    subclasses = useUtilFactory.createIdentityStringSet();
                    i_descendantsMap.put(i_superclassName, subclasses);
                }
                subclasses.add(i_subclassName);
            }

            // Note: The irreflexive closure is generated: A class is not recorded
            //       as the subclass of itself.
        }
    }

    //

    public void logState() {
        TraceComponent stateLogger = AnnotationServiceImpl_Logging.stateLogger;

        if (stateLogger.isDebugEnabled()) {
            log(stateLogger);
        }
    }

    public void log(TraceComponent logger) {
        Tr.debug(logger, MessageFormat.format("BEGIN STATE [ {0} ]", getHashText()));

        Tr.debug(logger, MessageFormat.format("  Is Detail Enabled [ {0} ]",
                                              Boolean.valueOf(getIsDetailEnabled())));

        logScannedClasses(logger);
        logClassClassSources(logger);
        logSuperclassNames(logger);
        logInterfaceNames(logger);

        logUnresolvedPackages(logger);
        logUnresolvedClasses(logger);

        seedData.log(logger);
        partialData.log(logger);
        excludedData.log(logger);
        externalData.log(logger);

        Tr.debug(logger, MessageFormat.format("END STATE [ {0} ]", getHashText()));
    }

    public void logScannedClasses(TraceComponent logger) {
        Tr.debug(logger, "Scanned classes: BEGIN");

        for (String className : getScannedClassNames()) {
            Tr.debug(logger, MessageFormat.format("  [ {0} ]", className));
        }

        Tr.debug(logger, "Scanned classes: END");
    }

    public void logClassClassSources(TraceComponent logger) {
        Tr.debug(logger, "Class Class Sources: BEGIN");

        for (String className : getScannedClassNames()) {
            Tr.debug(logger, MessageFormat.format("  [ {0} ]: [ {1} ]",
                                                  new Object[] { className, getClassClassSourceName(className) }));
        }

        Tr.debug(logger, "Class Class Sources: END");
    }

    public void logSuperclassNames(TraceComponent logger) {
        Tr.debug(logger, "Superclasses: BEGIN");

        Object[] logParms = new Object[] { null, null };

        Map<String, String> mapToIterate = i_superclassNameMap == null ? Collections.<String, String> emptyMap() : i_superclassNameMap;

        for (Map.Entry<String, String> superclassNameEntry : mapToIterate.entrySet()) {
            String subclassName = superclassNameEntry.getKey();
            String superclassName = superclassNameEntry.getValue();

            logParms[0] = subclassName;
            logParms[1] = superclassName;

            Tr.debug(logger, MessageFormat.format("  Subclass [ {0} ] Superclass [ {1} ]", logParms));
        }

        Tr.debug(logger, "Superclasses: END");
    }

    public void logInterfaceNames(TraceComponent logger) {
        Tr.debug(logger, "Interfaces: BEGIN");

        Object[] logParms = new Object[] { null, null };

        Map<String, String[]> mapToIterate = i_interfaceNameMap == null ? Collections.<String, String[]> emptyMap() : i_interfaceNameMap;

        for (Map.Entry<String, String[]> interfaceNamesEntry : mapToIterate.entrySet()) {
            String childName = interfaceNamesEntry.getKey();
            String[] interfaceNames = interfaceNamesEntry.getValue();

            logParms[0] = childName;
            logParms[1] = interfaceNames;

            Tr.debug(logger, MessageFormat.format("  Child [ {0} ] Interfaces [ {1} ]", logParms));
        }

        Tr.debug(logger, "Interfaces: END");
    }

    public void logUnresolvedPackages(TraceComponent logger) {
        Tr.debug(logger, "Unresolved packages: BEGIN");

        for (String packageName : getUnresolvedPackageNames()) {
            Tr.debug(logger, MessageFormat.format("  [ {0} ]", packageName));
        }

        Tr.debug(logger, "Unresolved packages: END");
    }

    public void logUnresolvedClasses(TraceComponent logger) {
        Tr.debug(logger, "Unresolved classes: BEGIN");

        for (String className : getUnresolvedClassNames()) {
            Tr.debug(logger, MessageFormat.format("  [ {0} ]", className));
        }

        Tr.debug(logger, "Unresolved classes: END");
    }

    //

    protected Set<String> restrict(Set<String> candidates, Set<String> allowed) {
        if ((candidates == null) || (allowed == null) || candidates.isEmpty() || allowed.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> restrictedSet = new HashSet<String>();

        for (String candidate : candidates) {
            if (allowed.contains(candidate)) {
                restrictedSet.add(candidate);
            }
        }

        return restrictedSet;
    }

    @Override
    public boolean isInstanceOf(String className, Class<?> targetClass) {
        scanReferenceClasses();

        String i_className = internClassName(className, false);
        if ( i_className == null ) {
            return false; // The immediate class is not in the targets data.
        }

        String i_targetClassName = internClassName(targetClass.getName(), false);
        if ( i_targetClassName == null ) {
            return false; // The target class is not in the targets data.
        }

        if ( i_className == i_targetClassName ) {
            return true; // The immediate class is the target class.
        }

        String i_targetInterfaceName = null;
        if ( targetClass.isInterface() ) {
            i_targetInterfaceName = i_targetClassName;
            i_targetClassName = null;
        }

        while ( i_className != null ) {
            if ( i_targetInterfaceName != null ) {
                // Match on one of the interfaces of the next super class.

                String[] i_interfaces = i_getInterfaceNames(i_className);
                if ( i_interfaces != null ) {
                    for ( String i_interface : i_interfaces ) {
                        if ( i_targetInterfaceName == i_interface ) {
                            return true;
                        }
                    }
                }

            } else if ( i_className == i_targetClassName ) {
                return true;
            }

            i_className = i_getSuperclassName(i_className);

            // TODO: This should build a table of visited super classes.  An
            //       infinite loop will result from a class inheritance loop.
            //       We need to detect that case and fail gracefully.
        }

        return false;
    }

    /**
     * <p>Record the annotation to the specified table. Scan policies SEED, PARTIAL, and EXCLUDED
     * are allowed. Scan policy EXTERNAL should never be used as a parameter.</p>
     * 
     * @param policy
     *            The policy of the class, as derived from the class source which present
     *            the class (SEED, PARTIAL, EXCLUDED, or EXTERNAL).
     * @param category
     *            The category of the annotation (PACKAGE, CLASS, METHOD, or FIELD).
     * @param i_targetName
     *            The name of the class or package having the annotation.
     * @param i_annotationClassName
     *            The name of the class of the annotation.
     */
    protected void i_recordAnnotation(ScanPolicy policy,
                                      AnnotationCategory category,
                                      String i_targetName,
                                      String i_annotationClassName) {

        UtilImpl_BidirectionalMap annoMap = doGetAnnotationsMap(policy, category);

        annoMap.record(i_targetName, i_annotationClassName);

        i_addReferencedClassName(i_annotationClassName);
    }

    // Detail (field and method) enablement ...

    protected final boolean isDetailEnabled;

    @Override
    public boolean getIsDetailEnabled() {
        return isDetailEnabled;
    }

    // Policy data fan-outs ...

    protected final AnnotationTargetsImpl_PolicyData seedData;
    protected final AnnotationTargetsImpl_PolicyData partialData;
    protected final AnnotationTargetsImpl_PolicyData excludedData;
    protected final AnnotationTargetsImpl_PolicyData externalData;

    // Version for results retrieval ... trigger a scan.
    protected AnnotationTargetsImpl_PolicyData getPolicyData(ScanPolicy policy) {
        if (policy == ScanPolicy.SEED) {
            scanDirectClasses();
            return seedData;

        } else if (policy == ScanPolicy.PARTIAL) {
            scanDirectClasses();
            return partialData;

        } else if (policy == ScanPolicy.EXCLUDED) {
            scanDirectClasses();
            return excludedData;

        } else if (policy == ScanPolicy.EXTERNAL) {
            scanReferenceClasses();
            return externalData;

        } else {
            throw new IllegalArgumentException("Policy [ " + policy + " ]");
        }
    }

    // Version for recording ... don't trigger a scan! 
    protected AnnotationTargetsImpl_PolicyData doGetPolicyData(ScanPolicy policy) {

        if (policy == ScanPolicy.SEED) {
            return seedData;

        } else if (policy == ScanPolicy.PARTIAL) {
            return partialData;

        } else if (policy == ScanPolicy.EXCLUDED) {
            return excludedData;

        } else if (policy == ScanPolicy.EXTERNAL) {
            return externalData;

        } else {
            throw new IllegalArgumentException("Policy [ " + policy + " ]");
        }
    }

    // The SEED data accessors are preserved as unit test entry points.
    // These must ensure the scan has been performed.

    public UtilImpl_BidirectionalMap getPackageAnnotationData() {
        return getAnnotationsMap(ScanPolicy.SEED, AnnotationCategory.PACKAGE);
    }

    public UtilImpl_BidirectionalMap getClassAnnotationData() {
        return getAnnotationsMap(ScanPolicy.SEED, AnnotationCategory.CLASS);
    }

    public UtilImpl_BidirectionalMap getFieldAnnotationData() {
        return getAnnotationsMap(ScanPolicy.SEED, AnnotationCategory.FIELD);
    }

    public UtilImpl_BidirectionalMap getMethodAnnotationData() {
        return getAnnotationsMap(ScanPolicy.SEED, AnnotationCategory.METHOD);
    }

    // Do trigger a scan: This is used to retrieve results.
    protected UtilImpl_BidirectionalMap getAnnotationsMap(ScanPolicy policy, AnnotationCategory category) {
        return getPolicyData(policy).getTargetData(category);
    }

    // Do not trigger a scan: This is used by the recording steps.
    protected UtilImpl_BidirectionalMap doGetAnnotationsMap(ScanPolicy policy, AnnotationCategory category) {
        return doGetPolicyData(policy).getTargetData(category);
    }

    //

    protected Set<String> selectAnnotations(int scanPolicies, AnnotationCategory category) {
        int nonEmptyCount = 0;

        Set<String> selected_SEED;
        if (ScanPolicy.SEED.accept(scanPolicies)) {
            selected_SEED = seedData.getAnnotations(category);
            if (selected_SEED.isEmpty()) {
                selected_SEED = null;
            } else {
                nonEmptyCount++;
            }
        } else {
            selected_SEED = null;
        }

        Set<String> selected_PARTIAL;
        if (ScanPolicy.PARTIAL.accept(scanPolicies)) {
            selected_PARTIAL = partialData.getAnnotations(category);
            if (selected_PARTIAL.isEmpty()) {
                selected_PARTIAL = null;
            } else {
                nonEmptyCount++;
            }
        } else {
            selected_PARTIAL = null;
        }

        Set<String> selected_EXCLUDED;
        if (ScanPolicy.EXCLUDED.accept(scanPolicies)) {
            selected_EXCLUDED = excludedData.getAnnotations(category);
            if (selected_EXCLUDED.isEmpty()) {
                selected_EXCLUDED = null;
            } else {
                nonEmptyCount++;
            }
        } else {
            selected_EXCLUDED = null;
        }

        Set<String> selected_EXTERNAL;
        if (ScanPolicy.EXTERNAL.accept(scanPolicies)) {
            selected_EXTERNAL = externalData.getAnnotations(category);
            if (selected_EXTERNAL.isEmpty()) {
                selected_EXTERNAL = null;
            } else {
                nonEmptyCount++;
            }
        } else {
            selected_EXTERNAL = null;
        }

        if (nonEmptyCount == 0) {
            return Collections.emptySet();

        } else if (nonEmptyCount == 1) {
        	Set<String> result;
            if (selected_SEED != null) {
                result = selected_SEED;
            } else if (selected_PARTIAL != null) {
                result = selected_PARTIAL;
            } else if (selected_EXCLUDED != null) {
                result = selected_EXCLUDED;
            } else {
                result = selected_EXTERNAL;
            }
            // 18994: The result set must not be identity based.
            return new HashSet<String>(result);

        } else {
            // Handles both the case when all three are requested and
            // when just two are requested.  When just two are requested,
            // or when just two are non-empty, there is one extra call
            // to 'addAll'.

            Set<String> result = new HashSet<String>();
            if (selected_SEED != null) {
                result.addAll(selected_SEED);
            }
            if (selected_PARTIAL != null) {
                result.addAll(selected_PARTIAL);
            }
            if (selected_EXCLUDED != null) {
                result.addAll(selected_EXCLUDED);
            }
            if (selected_EXTERNAL != null) {
                result.addAll(selected_EXTERNAL);
            }
            return result;
        }
    }

    protected Set<String> selectAnnotations(String targetName,
                                            int scanPolicies,
                                            AnnotationCategory category) {

        if (ScanPolicy.SEED.accept(scanPolicies)) {
            Set<String> selected = seedData.getAnnotations(targetName, category);
            if (!selected.isEmpty()) {
                return selected;
            }
        }

        if (ScanPolicy.PARTIAL.accept(scanPolicies)) {
            Set<String> selected = partialData.getAnnotations(targetName, category);
            if (!selected.isEmpty()) {
                return selected;
            }
        }

        if (ScanPolicy.EXCLUDED.accept(scanPolicies)) {
            Set<String> selected = excludedData.getAnnotations(targetName, category);
            if (!selected.isEmpty()) {
                return selected;
            }
        }

        if (ScanPolicy.EXTERNAL.accept(scanPolicies)) {
            Set<String> selected = externalData.getAnnotations(targetName, category);
            if (!selected.isEmpty()) {
                return selected;
            }
        }

        return Collections.emptySet();
    }

    protected Set<String> selectAnnotatedTargets(int scanPolicies, AnnotationCategory category) {
        scanDirectClasses();

        int nonEmptyCount = 0;

        Set<String> selected_SEED;
        if (ScanPolicy.SEED.accept(scanPolicies)) {
            selected_SEED = seedData.getAnnotatedTargets(category);
            if (selected_SEED.isEmpty()) {
                selected_SEED = null;
            } else {
                nonEmptyCount++;
            }
        } else {
            selected_SEED = null;
        }

        Set<String> selected_PARTIAL;
        if (ScanPolicy.PARTIAL.accept(scanPolicies)) {
            selected_PARTIAL = partialData.getAnnotatedTargets(category);
            if (selected_PARTIAL.isEmpty()) {
                selected_PARTIAL = null;
            } else {
                nonEmptyCount++;
            }
        } else {
            selected_PARTIAL = null;
        }

        Set<String> selected_EXCLUDED;
        if (ScanPolicy.EXCLUDED.accept(scanPolicies)) {
            selected_EXCLUDED = excludedData.getAnnotatedTargets(category);
            if (selected_EXCLUDED.isEmpty()) {
                selected_EXCLUDED = null;
            } else {
                nonEmptyCount++;
            }
        } else {
            selected_EXCLUDED = null;
        }

        Set<String> selected_EXTERNAL;
        if (ScanPolicy.EXTERNAL.accept(scanPolicies)) {
            selected_EXTERNAL = externalData.getAnnotatedTargets(category);
            if (selected_EXTERNAL.isEmpty()) {
                selected_EXTERNAL = null;
            } else {
                nonEmptyCount++;
            }
        } else {
            selected_EXTERNAL = null;
        }

        if (nonEmptyCount == 0) {
            return Collections.emptySet();

        } else if (nonEmptyCount == 1) {
            Set<String> result;
            if (selected_SEED != null) {
                result = selected_SEED;
            } else if (selected_PARTIAL != null) {
                result = selected_PARTIAL;
            } else if (selected_EXCLUDED != null) {
                result = selected_EXCLUDED;
            } else {
                result = selected_EXTERNAL;
            }
            // 18994: The result set must not be identity based.
            return new HashSet<String>(result);

        } else {
            // Handles both the case when all three are requested and
            // when just two are requested.  When just two are requested,
            // or when just two are non-empty, there is one extra call
            // to 'addAll'.

            Set<String> result = new HashSet<String>();
            if (selected_SEED != null) {
                result.addAll(selected_SEED);
            }
            if (selected_PARTIAL != null) {
                result.addAll(selected_PARTIAL);
            }
            if (selected_EXCLUDED != null) {
                result.addAll(selected_EXCLUDED);
            }
            if (selected_EXTERNAL != null) {
                result.addAll(selected_EXTERNAL);
            }
            return result;
        }
    }

    protected Set<String> selectAnnotatedTargets(String annotationName,
                                                 int scanPolicies,
                                                 AnnotationCategory category) {

        if (ScanPolicy.EXTERNAL.accept(scanPolicies)) {
            scanReferenceClasses();
        } else if ((scanPolicies | AnnotationTargets_Targets.POLICY_ALL_EXCEPT_EXTERNAL) != 0) {
            scanDirectClasses();
        } else {
            return Collections.emptySet(); // Strange: NO-OP selection
        }

        int nonEmptyCount = 0;

        Set<String> selected_SEED;
        if (ScanPolicy.SEED.accept(scanPolicies)) {
            selected_SEED = seedData.getAnnotatedTargets(annotationName, category);
            if (selected_SEED.isEmpty()) {
                selected_SEED = null;
            } else {
                nonEmptyCount++;
            }
        } else {
            selected_SEED = null;
        }

        Set<String> selected_PARTIAL;
        if (ScanPolicy.PARTIAL.accept(scanPolicies)) {
            selected_PARTIAL = partialData.getAnnotatedTargets(annotationName, category);
            if (selected_PARTIAL.isEmpty()) {
                selected_PARTIAL = null;
            } else {
                nonEmptyCount++;
            }
        } else {
            selected_PARTIAL = null;
        }

        Set<String> selected_EXCLUDED;
        if (ScanPolicy.EXCLUDED.accept(scanPolicies)) {
            selected_EXCLUDED = excludedData.getAnnotatedTargets(annotationName, category);
            if (selected_EXCLUDED.isEmpty()) {
                selected_EXCLUDED = null;
            } else {
                nonEmptyCount++;
            }
        } else {
            selected_EXCLUDED = null;
        }

        Set<String> selected_EXTERNAL;
        if (ScanPolicy.EXTERNAL.accept(scanPolicies)) {
            selected_EXTERNAL = externalData.getAnnotatedTargets(annotationName, category);
            if (selected_EXTERNAL.isEmpty()) {
                selected_EXTERNAL = null;
            } else {
                nonEmptyCount++;
            }
        } else {
            selected_EXTERNAL = null;
        }

        if (nonEmptyCount == 0) {
            return Collections.emptySet();

        } else if (nonEmptyCount == 1) {
            Set<String> result;
            if (selected_SEED != null) {
                result = selected_SEED;
            } else if (selected_PARTIAL != null) {
                result = selected_PARTIAL;
            } else if (selected_EXCLUDED != null) {
                result = selected_EXCLUDED;
            } else {
                result = selected_EXTERNAL;
            }
            // 18994: The result set must not be identity based.
            return new HashSet<String>(result);

        } else {
            // Handles both the case when all three are requested and
            // when just two are requested.  When just two are requested,
            // or when just two are non-empty, there is one extra call
            // to 'addAll'.

            Set<String> result = new HashSet<String>();
            if (selected_SEED != null) {
                result.addAll(selected_SEED);
            }
            if (selected_PARTIAL != null) {
                result.addAll(selected_PARTIAL);
            }
            if (selected_EXCLUDED != null) {
                result.addAll(selected_EXCLUDED);
            }
            if (selected_EXTERNAL != null) {
                result.addAll(selected_EXTERNAL);
            }
            return result;
        }
    }

    protected Set<String> selectClassNames(int scanPolicies) {

        int nonEmptyCount = 0;

        Set<String> seed;
        if (!ScanPolicy.SEED.accept(scanPolicies)) {
            seed = null;
        } else {
            seed = seedData.getClassNames();
            if (seed.isEmpty()) {
                seed = null;
            } else {
                nonEmptyCount++;
            }
        }

        Set<String> partial;
        if (!ScanPolicy.PARTIAL.accept(scanPolicies)) {
            partial = null;
        } else {
            partial = partialData.getClassNames();
            if (partial.isEmpty()) {
                partial = null;
            } else {
                nonEmptyCount++;
            }
        }

        Set<String> excluded;
        if (!ScanPolicy.PARTIAL.accept(scanPolicies)) {
            excluded = null;
        } else {
            excluded = excludedData.getClassNames();
            if (excluded.isEmpty()) {
                excluded = null;
            } else {
                nonEmptyCount++;
            }
        }

        Set<String> external;
        if (!ScanPolicy.PARTIAL.accept(scanPolicies)) {
            external = null;
        } else {
            external = externalData.getClassNames();
            if (external.isEmpty()) {
                external = null;
            } else {
                nonEmptyCount++;
            }
        }

        if (nonEmptyCount == 0) {
            return Collections.emptySet();

        } else if (nonEmptyCount == 1) {
            Set<String> result;
            if (seed != null) {
                result = seed;
            } else if (partial != null) {
                result = partial;
            } else if (excluded != null) {
                result = excluded;
            } else {
                result = external;
            }
            // 18994: The result set must not be identity based.
            return new HashSet<String>(result);

        } else {
            Set<String> result = new HashSet<String>();
            if (seed != null) {
                result.addAll(seed);
            }
            if (partial != null) {
                result.addAll(partial);
            }
            if (excluded != null) {
                result.addAll(excluded);
            }
            if (external != null) {
                result.addAll(external);
            }
            return result;
        }
    }
}
