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

import java.text.MessageFormat;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Factory;

public class AnnotationTargetsImpl_Scanner {
    private static final TraceComponent tc = Tr.register(AnnotationTargetsImpl_Scanner.class);
    public static final String CLASS_NAME = AnnotationTargetsImpl_Scanner.class.getName();

    protected final String hashText;

    public String getHashText() {
        return hashText;
    }

    //

    @SuppressWarnings("unused")
    protected AnnotationTargetsImpl_Scanner(AnnotationTargets_Factory factory,
                                            ClassSource_Aggregate classSource,
                                            AnnotationTargetsImpl_Targets annotationTargets) throws AnnotationTargets_Exception {
        super();

        String methodName = "init";

        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this);

        this.factory = factory;
        this.classSource = classSource;
        this.annotationTargets = annotationTargets;

        this.streamer = new AnnotationTargetsImpl_Streamer(this);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ]", this.hashText));

            Tr.debug(tc, MessageFormat.format("  Class Source [ {0} ]", this.classSource.getHashText()));
            Tr.debug(tc, MessageFormat.format("  Targets [ {0} ]", this.annotationTargets.getHashText()));
        }
    }

    //

    protected final AnnotationTargets_Factory factory;

    @Trivial
    public AnnotationTargets_Factory getFactory() {
        return factory;
    }

    //

    protected final ClassSource_Aggregate classSource;

    @Trivial
    public ClassSource_Aggregate getClassSource() {
        return classSource;
    }

    protected void openClassSource() throws AnnotationTargets_Exception {
        String methodName = "openClassSource";

        // Open failures should be rare: The default aggregate class source
        // implementation handles failures to open child class sources by
        // masking out those child class sources.  Processing is allowed
        // to continue.

        try {
            getClassSource().open(); // 'open' throws ClassSource_Exception

        } catch (ClassSource_Exception e) {
            Tr.warning(tc, "ANNO_TARGETS_SCAN_EXCEPTION", e); // CWWKC0044W

            throw getFactory().wrapIntoAnnotationTargetsException(tc, CLASS_NAME, methodName,
                                                                  "Failed to open class source", e);
        }
    }

    protected void closeClassSource() throws AnnotationTargets_Exception {
        String methodName = "closeClassSource";

        // Close failures should be rare: The default aggregate class source
        // implementation internally handles failures to close child class sources.

        try {
            getClassSource().close(); // 'close' throws ClassSource_Exception

        } catch (ClassSource_Exception e) {
            Tr.warning(tc, "ANNO_TARGETS_SCAN_EXCEPTION", e); // CWWKC0044W

            throw getFactory().wrapIntoAnnotationTargetsException(tc, CLASS_NAME, methodName,
                                                                  "Failed to close class source", e);
        }
    }

    //

    protected final AnnotationTargetsImpl_Targets annotationTargets;

    @Trivial
    public AnnotationTargetsImpl_Targets getAnnotationTargets() {
        return annotationTargets;
    }

    //

    protected final AnnotationTargetsImpl_Streamer streamer;

    @Trivial
    protected AnnotationTargetsImpl_Streamer getStreamer() {
        return streamer;
    }

    //

    // Scanning ...

    // Two types of scanning are performed:
    //
    // 1) Iterative scans
    // 2) Specific scans
    //
    // An iterative scan operates on all classes of the class source; a specific scan operates
    // on a specified subset of classes of the class source.
    //
    // Scanning has two phases for both types of scans:
    //
    // 1) Scanning of the immediate (available or specified) classes.
    // 2) Scanning of indirect classes (referenced superclasses and interfaces).
    //
    // Scanning and error handling:
    //
    // Errors may occur in several steps of the scan processing:
    //
    // 1) An input stream may be unavailable for a class because no resource is
    //    available for that class.
    //
    // 2) An input stream may be unavailable for a class because an exception
    //    occurred while obtaining the input stream for the class.
    //
    // 3) Scan processing may fail because an exception occurs while reading
    //    or processing the class data.
    //
    // 4) Scan processing may fail because class data does not match the externally
    //    specified name.
    //
    // 5) Scan processing may fail because the processing request duplicates already
    //    processed data.
    //
    // The response to an error that occurs during scan processing depends on the context
    // of the scan processing:
    //
    // 1) An error during an immediate scan emits a warning and ignores the class.
    //
    // 2) An error during an indirect scan emits a warning, removes the class from
    //    referenced classes, and adds the class to unresolved classes.

    long timeSpentInDeclaredScan = 0;
    long timeSpentInReferenceScan = 0;

    public void scanReferenced() throws AnnotationTargets_Exception {
        String methodName = "scanReferenced";

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, getHashText());
        }

        long startTime = System.currentTimeMillis();

        try {
            openClassSource(); // throws AnnotationTargets_Exception
            scanReferencedClasses();
        } finally {
            closeClassSource(); // throws AnnotationTargets_Exception
        }

        long stopTime = System.currentTimeMillis();
        logState(); // Doesn't use 'logger': don't guard this call.
        timeSpentInReferenceScan += (stopTime - startTime);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, getHashText());
        }
    }

    public void scanDirect() throws AnnotationTargets_Exception {
        String methodName = "scanDirect";

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, getHashText());
        }

        long startTime = System.currentTimeMillis();

        try {
            openClassSource(); // throws AnnotationTargets_Exception
            scanClasses();
        } finally {
            closeClassSource(); // throws AnnotationTargets_Exception
        }

        long stopTime = System.currentTimeMillis();
        logState(); // Doesn't use 'logger': don't guard this call.
        timeSpentInDeclaredScan += (stopTime - startTime);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, getHashText());
        }
    }

    public void scan(Set<String> specificClassNames) throws AnnotationTargets_Exception {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] ENTER [ {1} ] specific classes",
                                              new Object[] { getHashText(),
                                                            Integer.valueOf(specificClassNames.size()) }));
        }

        try {
            openClassSource(); // throws AnnotationTargets_Exception

            scanSpecificClasses(specificClassNames);
            scanReferencedClasses();

        } finally {
            closeClassSource(); // throws AnnotationTargets_Exception
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] RETURN", getHashText()));
        }
    }

    protected void scanClasses() {
        getClassSource().scanClasses(getStreamer());
    }

    protected void scanSpecificClasses(Set<String> specificClassNames) {
        // Specific classes which cannot be scanned are ignored.

        ClassSource_Aggregate useClassSource = getClassSource();
        AnnotationTargetsImpl_Streamer useStreamer = getStreamer();

        for (String className : specificClassNames) {
            try {
                useClassSource.scanSpecificSeedClass(className, useStreamer); // throws ClassSource_Exception

            } catch (ClassSource_Exception e) {
                // TODO: Is there a warning which includes the class name?
                Tr.warning(tc, "ANNO_TARGETS_SCAN_EXCEPTION", e); // CWWKC0044W
            }
        }
    }

    protected void scanReferencedClasses() {

        AnnotationTargetsImpl_Targets useAnnotationTargets = getAnnotationTargets();

        Set<String> scannedClassNames = useAnnotationTargets.getScannedClassNames();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Initial scan found [ {1} ] classes",
                                              new Object[] { getHashText(),
                                                            Integer.valueOf(scannedClassNames.size()) }));
        }

        AnnotationTargetsImpl_Streamer useStreamer = getStreamer();
        ClassSource_Aggregate useClassSource = getClassSource();

        int referencePassNo = 0;

        Set<String> referencedClassNames;
        while (!(referencedClassNames = getAnnotationTargets().getReferencedClassNames()).isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Scanning referenced classes [ {1} ] -- pass [ {2} ]",
                                                  new Object[] { getHashText(),
                                                                Integer.valueOf(referencedClassNames.size()),
                                                                Integer.valueOf(referencePassNo) }));
            }

            for (String referencedClassName : referencedClassNames) {
                boolean didScan;

                try {
                    didScan = useClassSource.scanReferencedClass(referencedClassName, useStreamer);

                } catch (ClassSource_Exception e) {
                    didScan = false;

                    // TODO: Is there a warning which includes the class name?
                    Tr.warning(tc, "ANNO_TARGETS_SCAN_EXCEPTION", e); // CWWKC0044W
                }

                if (!didScan) {
                    useAnnotationTargets.recordUnresolvedClass(referencedClassName);
                }
            }

            scannedClassNames = useAnnotationTargets.getScannedClassNames();

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, MessageFormat.format("[ {0} ] Reference pass [ {1} ] scan found [ {2} ] classes",
                                                  new Object[] { getHashText(),
                                                                Integer.valueOf(scannedClassNames.size()) }));
            }

            referencePassNo++;
        }
    }

    //

    public long getTotalLookups() {
        return getClassSource().getTotalLookups();
    }

    public long getRepeatLookups() {
        return getClassSource().getRepeatLookups();
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
        getClassSource().log(logger);
        getAnnotationTargets().log(logger);

        Tr.debug(logger, MessageFormat.format("END STATE [ {0} ]", getHashText()));
    }
}