/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets.internal;

import java.util.List;
import java.util.logging.Level;

import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;

public class TargetsScannerLimitedImpl extends TargetsScannerBaseImpl {

    public static final String CLASS_NAME = TargetsScannerLimitedImpl.class.getSimpleName();
    
    //

    protected TargetsScannerLimitedImpl(
        AnnotationTargetsImpl_Targets targets,
        ClassSource_Aggregate rootClassSource) {

        super(targets, rootClassSource);

        this.targetsData = null;
    }

    protected TargetsTableImpl targetsData;

    public TargetsTableImpl getTargetsData() {
        return targetsData;
    }

    public void setTargetsData(TargetsTableImpl targetsData) {
        this.targetsData = targetsData;
    }

    protected void scanContainer() {
        ClassSource initialClassSource = getRootClassSource().getClassSources().get(0);

        setTargetsData( scanInternal( initialClassSource,
                                      TargetsVisitorClassImpl.DONT_RECORD_UNRESOLVED,
                                      TargetsVisitorClassImpl.DONT_RECORD_RESOLVED ) );
    }

    protected void validate() {
        if (!logger.isLoggable(Level.FINER)) {
            return;
        }

        // A single class source is expected.

        List<? extends ClassSource> useClassSources = getRootClassSource().getClassSources();
        if ( useClassSources.isEmpty() ) {
            throw new IllegalArgumentException("Empty class sources!");
        }
    }
}
