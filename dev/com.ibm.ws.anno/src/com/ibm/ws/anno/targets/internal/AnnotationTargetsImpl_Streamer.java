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
}
