/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.anno.classsource.internal;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;
import com.ibm.wsspi.anno.util.Util_InternMap;

public abstract class ClassSourceImpl implements ClassSource {
    private static final TraceComponent tc = Tr.register(ClassSourceImpl.class);
    public static final String CLASS_NAME = ClassSourceImpl.class.getName();

    //

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    @Override
    public String toString() {
        return hashText;
    }

    //

    protected ClassSourceImpl(ClassSourceImpl_Factory factory,
                              Util_InternMap internMap,
                              String name,
                              String hashTextSuffix) {
        super();

        this.factory = factory;

        this.internMap = internMap;

        this.name = name;
        this.canonicalName = factory.getCanonicalName(this.name);

        this.parentSource = null;

        String useHashText = AnnotationServiceImpl_Logging.getBaseHash(this);
        useHashText += "(" + this.canonicalName;
        if (hashTextSuffix != null) {
            useHashText += ", " + hashTextSuffix;
        }
        useHashText += ")";

        this.hashText = useHashText;

        this.scanCounts = new ClassSourceImpl_ScanCounts();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] InternMap [ {1} ]",
                                              this.hashText, internMap.getHashText()));

        }
    }

    //

    protected final ClassSourceImpl_Factory factory;

    @Override
    @Trivial
    public ClassSourceImpl_Factory getFactory() {
        return factory;
    }

    protected final String name;

    @Override
    @Trivial
    public String getName() {
        return name;
    }

    protected final String canonicalName;

    @Override
    @Trivial
    public String getCanonicalName() {
        return canonicalName;
    }

    //

    protected ClassSource parentSource;

    @Override
    @Trivial
    public ClassSource getParentSource() {
        return parentSource;
    }

    @Override
    public void setParentSource(ClassSource parentSource) {
        this.parentSource = parentSource;
    }

    //

    @Trivial
    public boolean getConvertResourceNames() {
        return false;
    }

    @Override
    public String inconvertResourceName(String externalResourceName) {
        return externalResourceName;
    }

    @Override
    public String outconvertResourceName(String resourceName) {
        return resourceName;
    }

    //

    protected final Util_InternMap internMap;

    @Override
    @Trivial
    public Util_InternMap getInternMap() {
        return internMap;
    }

    protected String internClassName(String className) {
        return getInternMap().intern(className);
    }

    //

    @Override
    public abstract void open() throws ClassSource_Exception;

    @Override
    public abstract void close() throws ClassSource_Exception;

    //

    @Override
    @Trivial
    public boolean isDirectoryResource(String resourceName) {
        return resourceName.endsWith(ClassSource.RESOURCE_SEPARATOR_STRING);
    }

    @Override
    @Trivial
    public boolean isClassResource(String resourceName) {
        return resourceName.endsWith(CLASS_EXTENSION);
    }

    @Override
    public String getClassNameFromResourceName(String resourceName) {
        int endingOffset = resourceName.length() - ClassSource.CLASS_EXTENSION.length();
        String className = resourceName.substring(0, endingOffset);
        className = className.replace(RESOURCE_SEPARATOR_CHAR, ClassSource.CLASS_SEPARATOR_CHAR);

        return className;
    }

    @Override
    public String getResourceNameFromClassName(String className) {
        return className.replace(ClassSource.CLASS_SEPARATOR_CHAR, RESOURCE_SEPARATOR_CHAR) +
               ClassSource.CLASS_EXTENSION;
    }

    @Override
    public String resourceAppend(String head, String tail) {
        if (head.isEmpty()) {
            return tail;
        } else {
            return (head + ClassSource.RESOURCE_SEPARATOR_CHAR + tail);
        }
    }

    //

    protected final ClassSourceImpl_ScanCounts scanCounts;

    @Override
    @Trivial
    public ClassSourceImpl_ScanCounts getScanResults() {
        return scanCounts;
    }

    protected void markResult(ClassSource_ScanCounts.ResultField resultField) {
        scanCounts.increment(resultField);
    }

    protected void addResults(ClassSource_ScanCounts seepCounts) {
        scanCounts.addResults(seepCounts);
    }

    @Trivial
    @Override
    public int getResult(ClassSource_ScanCounts.ResultField resultField) {
        return scanCounts.getResult(resultField);
    }

    //

    @Override
    public abstract void scanClasses(ClassSource_Streamer streamer, Set<String> i_seedClassNamesSet, ScanPolicy scanPolicy);

    //

    @Override
    @Trivial
    public boolean scanSpecificSeedClass(String className, ClassSource_Streamer streamer) throws ClassSource_Exception {
        return scanClass(streamer, className, ScanPolicy.SEED); // throws ClassSource_Exception
    }

    @Override
    @Trivial
    public boolean scanReferencedClass(String referencedClassName, ClassSource_Streamer streamer) throws ClassSource_Exception {
        return scanClass(streamer, referencedClassName, ScanPolicy.EXTERNAL); // throws ClassSource_Exception
    }

    // Entry from:
    //   ClassSourceImpl.scanSpecifiedSeedClasses(Set<String>, ClassSource_Streamer)
    //   ClassSourceImpl.scanReferencedClasses(Set<String>, ClassSource_Streamer)

    /**
     * <p>Attempt to process a specified class. Answer whether processing was handed
     * successfully by the streamer via {@link ClassSource_Streamer#process(String, InputStream, boolean, boolean, boolean)}.</p>
     * 
     * <p>A failure (false) result occurs the class is blocked by {@link ClassSource_Streamer#doProcess(String, boolean, boolean, boolean)},
     * or if no resource is available for the class. A failure to open the resource results
     * in an exception. Certain processing failures also result in an exception.</p>
     * 
     * <p>An exception thrown by the streamer indicates that the class was not successfully
     * handled by the streamer.</p>
     * 
     * <p>A failure to close the stream for the class is handled locally.</p>
     * 
     * @param streamer The streamer which is to be used to process the named class.
     * @param className The class which is to be processed.
     * @param scanPolicy The scan policy of the class source of the named class.
     * 
     * @return True if processing reaches the streamer. Otherwise, false.
     * 
     * @throws ClassSource_Exception Thrown in case of a processing failure.
     */
    protected boolean scanClass(ClassSource_Streamer streamer, String className, ScanPolicy scanPolicy)
                    throws ClassSource_Exception {

        String methodName = "scanClass";
        Object[] logParms;
        if (tc.isEntryEnabled()) {
            logParms = new Object[] { getHashText(), className };
            Tr.entry(tc, methodName, MessageFormat.format("[ {0} ] Name [ {1} ]", logParms));
        } else {
            logParms = null;
        }

        if (!streamer.doProcess(className, scanPolicy)) {
            if (logParms != null) {
                Tr.exit(tc, methodName,
                        MessageFormat.format("[ {0} ] Return [ {1} ] [ false ]: Filtered by streamer", logParms));
            }
            return false;
        }

        String resourceName = getResourceNameFromClassName(className);

        InputStream inputStream = openResourceStream(className, resourceName); // throws ClassSource_Exception
        if (inputStream == null) {
            if (logParms != null) {
                Tr.exit(tc, methodName, MessageFormat.format("[ {0} ] Return [ {1} ] [ false ]: No resource is available", logParms));
            }
            return false;
        }

        try {
            streamer.process(this.getCanonicalName(), className, inputStream, scanPolicy);
        } finally {
            closeResourceStream(className, resourceName, inputStream);
        }

        if (logParms != null) {
            Tr.exit(tc, methodName, MessageFormat.format("[ {0} ] Return [ {1} ] [ true ]", logParms));
        }
        return true;
    }

    //

    @Trivial
    protected boolean i_maybeAdd(String i_resourceName, Set<String> i_seedClassNamesSet) {
        String methodName = "i_maybeAdd";

        boolean didAdd;
        if (didAdd = !i_seedClassNamesSet.contains(i_resourceName)) {
            i_seedClassNamesSet.add(i_resourceName);
        }

        // Explicit trace: We want to trace additions, but don't
        // want to trace the seed class names parameter.  The
        // seed class names can be large, and displaying it to trace
        // rather bloats the trace.

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName,
                     MessageFormat.format("[ {0} ] Resource [ {1} ]: [ {2} ]", getHashText(), i_resourceName, Boolean.valueOf(didAdd)));
        }

        return didAdd;
    }

    //

    // Mostly common, but overloaded by mapped jars.
    @Override
    public InputStream openClassStream(String className) throws ClassSource_Exception {
        String resourceName = getResourceNameFromClassName(className);

        return openResourceStream(className, resourceName); // throws ClassSource_Exception
    }

    // Mostly common, but overloaded by mapped jars.
    @Override
    public void closeClassStream(String className, InputStream inputStream) {
        String resourceName = getResourceNameFromClassName(className);

        closeResourceStream(className, resourceName, inputStream);
    }

    @Override
    public abstract InputStream openResourceStream(String className, String resourceName)
                    throws ClassSource_Exception;

    @Override
    public abstract void closeResourceStream(String className, String resourceName, InputStream inputStream);

    //

    protected int resourceExclusionCount;
    protected int classExclusionCount;
    protected int classInclusionCount;

    protected void incrementResourceExclusionCount() {
        resourceExclusionCount++;
    }

    @Override
    @Trivial
    public int getResourceExclusionCount() {
        return resourceExclusionCount;
    }

    protected void incrementClassExclusionCount() {
        classExclusionCount++;
    }

    @Override
    @Trivial
    public int getClassExclusionCount() {
        return classExclusionCount;
    }

    protected void incrementClassInclusionCount() {
        classInclusionCount++;
    }

    @Override
    @Trivial
    public int getClassInclusionCount() {
        return classInclusionCount;
    }

    //

    @Override
    @Trivial
    public void logState() {
        TraceComponent stateLogger = AnnotationServiceImpl_Logging.stateLogger;

        if (stateLogger.isDebugEnabled()) {
            log(stateLogger);
        }
    }

    @Override
    public abstract void log(TraceComponent logger);

    @Trivial
    protected void logCounts(TraceComponent logger) {
        if (logger.isDebugEnabled()) {
            Tr.debug(logger, MessageFormat.format("  Included classes: [ {0} ]",
                                                  Integer.valueOf(getClassInclusionCount())));

            Tr.debug(logger, MessageFormat.format("  Excluded classes: [ {0} ]",
                                                  Integer.valueOf(getClassExclusionCount())));
        }
    }
}
