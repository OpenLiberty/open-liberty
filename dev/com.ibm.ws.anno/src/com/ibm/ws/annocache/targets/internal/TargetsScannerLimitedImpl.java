/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.internal;

import java.util.List;
import java.util.logging.Level;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;

public class TargetsScannerLimitedImpl extends TargetsScannerBaseImpl {
    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = TargetsScannerLimitedImpl.class.getSimpleName();

    //

    protected TargetsScannerLimitedImpl(
        AnnotationTargetsImpl_Targets targets,
        ClassSource_Aggregate rootClassSource) {

        super(targets, rootClassSource);

        this.targetsTable = null;
    }

    protected TargetsTableImpl targetsTable;

    @Trivial
    public TargetsTableImpl getTargetsTable() {
        return targetsTable;
    }

    public void setTargetsTable(TargetsTableImpl targetsTable) {
        this.targetsTable = targetsTable;
    }

    protected void scanContainer() {
        ClassSource initialClassSource = getRootClassSource().getClassSources().get(0);

        setTargetsTable( scanInternal( initialClassSource,
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
