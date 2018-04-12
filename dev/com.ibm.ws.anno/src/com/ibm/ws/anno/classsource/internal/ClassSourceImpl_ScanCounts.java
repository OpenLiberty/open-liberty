/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.classsource.internal;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.anno.classsource.ClassSource_ScanCounts;

public class ClassSourceImpl_ScanCounts implements ClassSource_ScanCounts {

    @Trivial
    public ClassSourceImpl_ScanCounts() {
        super();

        this.results = new int[ClassSource_ScanCounts.NUM_RESULT_FIELDS];
    }

    //

    protected final int[] results;

    @Override
    @Trivial
    public int getResult(ResultField resultField) {
        return results[resultField.ordinal()];
    }

    @Override
    @Trivial
    public int getResult(int resultField) {
        return results[resultField];
    }

    //

    @Override
    @Trivial
    public void addResults(ClassSource_ScanCounts seep) {
        for ( int resultNo = 0; resultNo < ClassSource_ScanCounts.NUM_RESULT_FIELDS; resultNo++ ) {
            results[resultNo] = seep.getResult(resultNo);
        }
    }

    @Override
    @Trivial
    public void increment(ResultField resultField) {
        results[resultField.ordinal()]++;
    }

    @Override
    @Trivial
    public void increment(int resultField) {
        results[resultField]++;
    }
}
