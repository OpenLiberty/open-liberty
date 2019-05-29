/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.test.scan.errors;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification_Direct;
import com.ibm.ws.annocache.test.scan.Test_Base;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Fault;

public abstract class AnnotationTargets_TestBase_ScanError extends Test_Base {

    @Override
    public ClassSourceImpl_Specification_Direct createClassSourceSpecification(ClassSourceImpl_Factory factory) {
        return AnnotationTargets_ErrorData.createClassSourceSpecification( factory, getModSimpleName(), getModName() );
    }

    //

    @Override
    public String getAppName() {
    	return AnnotationTargets_ErrorData.EAR_NAME;
    }

    @Override
    public String getAppSimpleName() {
    	return AnnotationTargets_ErrorData.EAR_SIMPLE_NAME;
    }

    @Override
    public abstract String getModName();

    @Override
    public abstract String getModSimpleName();

    //

    public String[] getValidPackageNames() {
        return AnnotationTargets_ErrorData.VALID_PACKAGE_NAMES;
    }

    public String[] getNonValidPackageNames() {
        return AnnotationTargets_ErrorData.NON_VALID_PACKAGE_NAMES;
    }

    public String[] getValidClassNames() {
        return AnnotationTargets_ErrorData.VALID_CLASS_NAMES;
    }

    public String[] getNonValidClassNames() {
        return AnnotationTargets_ErrorData.NON_VALID_CLASS_NAMES;
    }

    @Test
    public void validateTargets() {
    	List<AnnotationTargets_Fault> faults = new ArrayList<AnnotationTargets_Fault>();
 
        getBaseResults().validateClasses(
        	getWriter(),
            getBaseTargets(),
            getValidClassNames(), getNonValidClassNames(),
            faults);

        if ( !faults.isEmpty() ) {
        	for ( AnnotationTargets_Fault fault : faults ) {
        		System.out.println("Fault: [ " + fault.getResolvedText() + " ]");
        	}
        }
        Assert.assertTrue("Faults detected validating classes", faults.isEmpty());
    }
}