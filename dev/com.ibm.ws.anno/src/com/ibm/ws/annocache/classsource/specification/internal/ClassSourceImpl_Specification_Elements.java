/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.classsource.specification.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.classsource.specification.ClassSource_Specification_Element;
import com.ibm.ws.annocache.classsource.specification.ClassSource_Specification_Elements;
import com.ibm.ws.annocache.classsource.specification.internal.ClassSourceImpl_Specification;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.util.Util_RelativePath;

public class ClassSourceImpl_Specification_Elements
    extends ClassSourceImpl_Specification
    implements ClassSource_Specification_Elements {

    @SuppressWarnings("hiding")
	public static final String CLASS_NAME = ClassSourceImpl_Specification_Elements.class.getSimpleName();

    //

    public ClassSourceImpl_Specification_Elements(
        ClassSourceImpl_Factory factory,
        String appName, String modName, String modCatName) {

        super(factory, appName, modName, modCatName);

        String methodName = "<init>";

        this.internalElements = new ArrayList<ClassSourceImpl_Specification_Element>();
        this.externalElements = new ArrayList<ClassSourceImpl_Specification_Element>();

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, this.hashText);
        }
    }

    //

    protected final List<ClassSourceImpl_Specification_Element> internalElements;

    @Override
    public List<ClassSourceImpl_Specification_Element> getInternalElements() {
        return internalElements;
    }

    protected void addInternalElement(ClassSourceImpl_Specification_Element internalElement) {
        getInternalElements().add(internalElement);
    }

    @Override
    public ClassSourceImpl_Specification_Element addInternalElement(
        String internalElementName, ScanPolicy policy, Util_RelativePath relativePath) {

        ClassSourceImpl_Specification_Element internalElement =
            getFactory().newElementSpecification(internalElementName, policy, relativePath);

        addInternalElement(internalElement);

        return internalElement;
    }

    //

    protected final List<ClassSourceImpl_Specification_Element> externalElements;

    @Override
    public List<ClassSourceImpl_Specification_Element> getExternalElements() {
        return externalElements;
    }

    protected void addExternalElement(ClassSourceImpl_Specification_Element externalElement) {
        getExternalElements().add(externalElement);
    }

    @Override
    public ClassSourceImpl_Specification_Element addExternalElement(
        String externalElementName, Util_RelativePath relativePath) {

        ClassSourceImpl_Specification_Element externalElement =
            getFactory().newElementSpecification(externalElementName, ScanPolicy.EXCLUDED, relativePath);

        addExternalElement(externalElement);

        return externalElement;
    }

    //

    @Override
    public void addInternalClassSources(ClassSource_Aggregate rootClassSource)
        throws ClassSource_Exception {

        for ( ClassSourceImpl_Specification_Element internalElement : getInternalElements() ) {
            internalElement.addTo(rootClassSource);
        }
    }

    @Override
    public void addExternalClassSources(ClassSource_Aggregate rootClassSource)
        throws ClassSource_Exception {

        for ( ClassSourceImpl_Specification_Element externalElement : getExternalElements() ) {
            externalElement.addTo(rootClassSource);
        }
    }

    //

    @Override
    @Trivial
    public void logInternal(Logger useLogger) {
        String methodName = "logInternal";

        useLogger.logp(Level.FINER, CLASS_NAME,  methodName, "  Internal Elements:");

        for ( ClassSource_Specification_Element internalElement : getInternalElements() ) {
            internalElement.log(useLogger);
        }
    }

    @Override
    @Trivial
    public void logExternal(Logger useLogger) {
        String methodName = "logExternal";

        useLogger.logp(Level.FINER, CLASS_NAME,  methodName, "  External Elements:");

        for ( ClassSource_Specification_Element externalElement : getExternalElements() ) {
            externalElement.log(useLogger);
        }
    }
}
