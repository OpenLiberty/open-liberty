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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.anno.util.Util_Factory;
import com.ibm.wsspi.anno.util.Util_InternMap;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM"})
public class AnnotationTargetsImpl_Factory implements AnnotationTargets_Factory {
    public static final TraceComponent tc = Tr.register(AnnotationTargetsImpl_Factory.class);
    public static final String CLASS_NAME = AnnotationTargetsImpl_Factory.class.getName();

    //

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    @Activate
    public AnnotationTargetsImpl_Factory(@Reference Util_Factory utilFactory,
                                         @Reference ClassSource_Factory classSourceFactory) {
        super();

        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this);

        this.utilFactory = utilFactory;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Created", this.hashText));
            Tr.debug(tc, MessageFormat.format("[ {0} ] Util factory [ {1} ]",
                                              new Object[] { this.hashText,
                                                            this.utilFactory.getHashText() }));
        }
    }

    //

    protected final Util_Factory utilFactory;

    @Override
    public Util_Factory getUtilFactory() {
        return utilFactory;
    }

    //

    @Override
    public AnnotationTargets_Exception newAnnotationTargetsException(TraceComponent logger, String message) {
        AnnotationTargets_Exception exception = new AnnotationTargets_Exception(message);

        if (logger.isDebugEnabled()) {
            Tr.debug(logger, exception.getMessage(), exception);
        }

        return exception;
    }

    @Override
    public AnnotationTargets_Exception wrapIntoAnnotationTargetsException(TraceComponent logger,
                                                                          String callingClassName,
                                                                          String callingMethodName,
                                                                          String message, Throwable th) {

        AnnotationTargets_Exception wrappedException = new AnnotationTargets_Exception(message, th);

        if (logger.isDebugEnabled()) {
            Tr.debug(logger, MessageFormat.format(" [ {0} ] [ {1} ] Wrap [ {2} ] as [ {3} ]",
                                                  callingClassName, callingMethodName, th.getClass().getName(),
                                                  wrappedException.getClass().getName()));

            Tr.debug(logger, th.getMessage(), th);
            Tr.debug(logger, message, wrappedException);
        }

        return wrappedException;
    }

    //

    @Override
    public AnnotationTargetsImpl_Targets createTargets() throws AnnotationTargets_Exception {
        return createTargets(AnnotationTargets_Factory.DETAIL_IS_ENABLED);
    }

    @Override
    public AnnotationTargetsImpl_Targets createTargets(boolean isDetailEnabled)
                    throws AnnotationTargets_Exception {

        Util_InternMap classInternMap = getUtilFactory().createInternMap(Util_InternMap.ValueType.VT_CLASS_NAME, "classes and package names");

        return new AnnotationTargetsImpl_Targets(this, classInternMap, isDetailEnabled);
    }

    //

    @Override
    public AnnotationTargetsImpl_Fault createFault(String unresolvedText, String[] parameters) {

        return new AnnotationTargetsImpl_Fault(unresolvedText, parameters);
    }
}