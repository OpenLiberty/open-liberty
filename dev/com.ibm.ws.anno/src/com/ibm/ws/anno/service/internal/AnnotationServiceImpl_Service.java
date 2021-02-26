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

package com.ibm.ws.anno.service.internal;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.info.internal.InfoStoreFactoryImpl;
import com.ibm.ws.anno.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.anno.info.InfoStoreFactory;
import com.ibm.wsspi.anno.service.AnnotationService_Service;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.anno.util.Util_Factory;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM"})
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

    final protected BundleContext bundleContext;

    @Activate
    public AnnotationServiceImpl_Service(BundleContext bundleContext,
                                         @Reference Util_Factory utilFactory,
                                         @Reference ClassSource_Factory classSourceFactory,
                                         @Reference AnnotationTargets_Factory annotationTargetsFactory,
                                         @Reference InfoStoreFactory infoStoreFactory) {
        super();

        this.bundleContext = bundleContext;
        String methodName = "<init>";

        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this);

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, this.hashText);
        }

        this.utilFactory = utilFactory;

        this.classSourceFactory = classSourceFactory;

        this.annotationTargetsFactory = annotationTargetsFactory;

        this.infoStoreFactory = infoStoreFactory;

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

    final protected Util_Factory utilFactory;

    @Override
    @Trivial
    public Util_Factory getUtilFactory() {
        return utilFactory;
    }

    final protected ClassSource_Factory classSourceFactory;

    @Override
    @Trivial
    public ClassSource_Factory getClassSourceFactory() {
        return classSourceFactory;
    }


    final protected AnnotationTargets_Factory annotationTargetsFactory;

    @Override
    @Trivial
    public AnnotationTargets_Factory getAnnotationTargetsFactory() {
        return annotationTargetsFactory;
    }

    final protected InfoStoreFactory infoStoreFactory;

    @Override
    @Trivial
    public InfoStoreFactory getInfoStoreFactory() {
        return infoStoreFactory;
    }

}