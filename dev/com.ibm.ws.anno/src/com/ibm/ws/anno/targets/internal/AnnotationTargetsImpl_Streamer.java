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

package com.ibm.ws.anno.targets.internal;

import java.io.InputStream;
import java.text.MessageFormat;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Streamer;

public class AnnotationTargetsImpl_Streamer implements ClassSource_Streamer {
    private static final TraceComponent tc = Tr.register(AnnotationTargetsImpl_Streamer.class);

    public static final String CLASS_NAME = AnnotationTargetsImpl_Streamer.class.getName();

    protected final String hashText;

    public String getHashText() {
        return hashText;
    }

    //

    protected AnnotationTargetsImpl_Streamer(AnnotationTargetsImpl_Scanner scanner) {
        super();

        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this);

        this.scanner = scanner;
        this.targets = scanner.getAnnotationTargets();

        this.jandexConverter = new AnnotationTargetsImpl_JandexConverter(this.targets);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format(" [ {0} ]", this.hashText));
            Tr.debug(tc, MessageFormat.format("  Scanner [ {0} ]", this.scanner.getHashText()));
        }
    }

    //

    protected final AnnotationTargetsImpl_Scanner scanner;

    public AnnotationTargetsImpl_Scanner getScanner() {
        return scanner;
    }

    protected ClassSource_Factory getClassSourceFactory() {
        return getScanner().getClassSource().getFactory();
    }

    protected ClassSource_Exception wrapIntoClassSourceException(String methodName, String message, Throwable th) {
        return getClassSourceFactory().wrapIntoClassSourceException(CLASS_NAME, methodName, message, th);
    }

    //

    protected final AnnotationTargetsImpl_Targets targets;

    protected AnnotationTargetsImpl_Targets getTargets() {
        return targets;
    }

    //

    protected final AnnotationTargetsImpl_JandexConverter jandexConverter;

    protected AnnotationTargetsImpl_JandexConverter getJandexConverter() {
        return jandexConverter;
    }

    //

    @Override
    public boolean doProcess(String className, ScanPolicy scanPolicy) {
        return true;
    }

    // Entry from class sources, for example:
    //   ClassSourceImpl.process(ClassSource_Streamer, String, boolean, boolean, boolean)

    @Override
    public boolean process(String classSourceName, String className, InputStream inputStream, ScanPolicy scanPolicy) {
        return getTargets().scanClass(classSourceName, className, inputStream, scanPolicy);
    }

    //

    /**
     * <p>Tell if this streamer supports the processing of JANDEX class information.</p>
     *
     * <p>This implementation answers true.</p>
     *
     * @return True or false telling if this streamer supports the processing of JANDEX
     *     class information.
     */
    @Override
    public boolean supportsJandex() {
        return true;
    }

    /**
     * <p>Process the data for the specified class.</p>
     *
     * @param classSourceName The name of the class source which contains the class.
     * @param jandexClassInfo JANDEX class information for the class.
     * @param scanPolicy The policy active on the class.
     * 
     * @return True if the class was processed. Otherwise, false.
     * 
     * @throws ClassSource_Exception Thrown if an error occurred while
     *             testing the specified class.
     */
    @Override
    public boolean process(
        String classSourceName,
        org.jboss.jandex.ClassInfo jandexClassInfo,
        ScanPolicy scanPolicy) throws ClassSource_Exception {

        String i_classSourceName = getTargets().internClassSourceName(classSourceName);

        getJandexConverter().convertClassInfo(i_classSourceName, jandexClassInfo, scanPolicy);

        return true;
    }
}
