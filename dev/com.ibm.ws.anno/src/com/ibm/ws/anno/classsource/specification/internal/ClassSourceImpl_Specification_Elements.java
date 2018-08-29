/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.classsource.specification.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Aggregate;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;

import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Element;
import com.ibm.ws.anno.classsource.specification.ClassSource_Specification_Elements;

import com.ibm.ws.anno.classsource.specification.internal.ClassSourceImpl_Specification;

import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;

import com.ibm.wsspi.anno.util.Util_RelativePath;

public class ClassSourceImpl_Specification_Elements
    extends ClassSourceImpl_Specification
    implements ClassSource_Specification_Elements {

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
    public void addInternalClassSources(ClassSourceImpl_Aggregate rootClassSource)
        throws ClassSource_Exception {

        for ( ClassSourceImpl_Specification_Element internalElement : getInternalElements() ) {
            internalElement.addTo(rootClassSource);
        }
    }

    @Override
    public void addExternalClassSources(ClassSourceImpl_Aggregate rootClassSource)
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
