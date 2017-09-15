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

package com.ibm.ws.anno.service.internal;

import java.text.MessageFormat;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.anno.info.internal.InfoStoreFactoryImpl;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.anno.service.AnnotationService_Service;

public class AnnotationServiceImpl_Service implements AnnotationService_Service {
    public static final TraceComponent tc = Tr.register(AnnotationServiceImpl_Service.class);
    public static final String CLASS_NAME = AnnotationServiceImpl_Service.class.getName();

    //

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    //

    protected BundleContext bundleContext;

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        String methodName = "activate";

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, getHashText());
        }
        bundleContext = componentContext.getBundleContext();

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, getHashText());
        }
    }

    //

    public AnnotationServiceImpl_Service() {
        super();

        String methodName = "<init>";

        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this);

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, this.hashText);
        }

        UtilImpl_Factory useUtilFactory = new UtilImpl_Factory();
        setUtilFactory(useUtilFactory);

        ClassSourceImpl_Factory useClassSourceFactory = new ClassSourceImpl_Factory(useUtilFactory);
        setClassSourceFactory(useClassSourceFactory);

        AnnotationTargetsImpl_Factory useAnnotationTargetsFactory =
                        new AnnotationTargetsImpl_Factory(useUtilFactory, useClassSourceFactory);
        setAnnotationTargetsFactory(useAnnotationTargetsFactory);

        InfoStoreFactoryImpl useInfoStoreFactory = new InfoStoreFactoryImpl(useUtilFactory);
        setInfoStoreFactory(useInfoStoreFactory);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, this.hashText);

            // Logged in the setters.
            //
            // logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Util Factory [ {1} ]",
            //             new Object[] { this.hashText, useUtilFactory.getHashText() });
            // logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Class Source Factory [ {1} ]",
            //             new Object[] { this.hashText, useClassSourceFactory.getHashText() });
            // logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Annotation Targets Factory [ {1} ]",
            //             new Object[] { this.hashText, useAnnotationTargetsFactory.getHashText() });
            // logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Info Store Factory [ {1} ]",
            //             new Object[] { this.hashText, useInfoStoreFactory.getHashText() });
        }
    }

    //

    protected UtilImpl_Factory utilFactory;

    @Override
    @Trivial
    public UtilImpl_Factory getUtilFactory() {
        return utilFactory;
    }

    protected void setUtilFactory(UtilImpl_Factory utilFactory) {

        this.utilFactory = utilFactory;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format(" [ {0} ] Set utility factory [ {1} ]",
                                              this.hashText, this.utilFactory.getHashText()));
        }
    }

    //

    protected ClassSourceImpl_Factory classSourceFactory;

    @Override
    @Trivial
    public ClassSourceImpl_Factory getClassSourceFactory() {
        return classSourceFactory;
    }

    protected void setClassSourceFactory(ClassSourceImpl_Factory classSourceFactory) {
        this.classSourceFactory = classSourceFactory;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format(" [ {0} ] Set class source factory [ {1} ]",
                                              this.hashText, this.classSourceFactory.getHashText()));
        }
    }

    //

    protected AnnotationTargetsImpl_Factory annotationTargetsFactory;

    @Override
    @Trivial
    public AnnotationTargetsImpl_Factory getAnnotationTargetsFactory() {
        return annotationTargetsFactory;
    }

    protected void setAnnotationTargetsFactory(AnnotationTargetsImpl_Factory annotationTargetsFactory) {
        this.annotationTargetsFactory = annotationTargetsFactory;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format(" [ {0} ] Set annotation targets factory [ {1} ]",
                                              this.hashText, this.annotationTargetsFactory.getHashText()));
        }
    }

    //

    protected InfoStoreFactoryImpl infoStoreFactory;

    @Override
    @Trivial
    public InfoStoreFactoryImpl getInfoStoreFactory() {
        return infoStoreFactory;
    }

    protected void setInfoStoreFactory(InfoStoreFactoryImpl infoStoreFactory) {
        this.infoStoreFactory = infoStoreFactory;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format(" {0} ] Set info store factory [ {1} ]",
                                              this.hashText, this.infoStoreFactory.getHashText()));
        }
    }
}