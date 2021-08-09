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
package com.ibm.ws.annocache.service.internal;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.info.InfoStoreFactory;
import com.ibm.wsspi.annocache.service.AnnotationCacheService_Service;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_Factory;
import com.ibm.wsspi.annocache.util.Util_Factory;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM"})
public class AnnotationCacheServiceImpl_Service implements AnnotationCacheService_Service {
    public static final String CLASS_NAME = "AnnotationCacheServiceImpl_Service";

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    @Activate
    public AnnotationCacheServiceImpl_Service(@Reference TargetCache_Factory targetCacheFactory,
                                              @Reference Util_Factory utilFactory,
                                              @Reference InfoStoreFactory infoStoreFactory,
                                              @Reference ClassSource_Factory classSourceFactory,
                                              @Reference AnnotationTargets_Factory annotationTargetsFactory) {
        super();
        this.targetCacheFactory = targetCacheFactory;
        this.utilFactory = utilFactory;
        this.infoStoreFactory = infoStoreFactory;
        this.classSourceFactory = classSourceFactory;
        this.annotationTargetsFactory = annotationTargetsFactory;
        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }

    //

    private final Util_Factory utilFactory;

    @Override
    public Util_Factory getUtilFactory() {
        return utilFactory;
    }

    //

    private final ClassSource_Factory classSourceFactory;

    @Override
    public ClassSource_Factory getClassSourceFactory() {
        return classSourceFactory;
    }


    //

    private final TargetCache_Factory targetCacheFactory;

    @Override
    public TargetCache_Factory getTargetCacheFactory() {
        return targetCacheFactory;
    }

    //

    private final AnnotationTargets_Factory annotationTargetsFactory;

    @Override
    public AnnotationTargets_Factory getAnnotationTargetsFactory() {
        return annotationTargetsFactory;
    }

    //

    private final InfoStoreFactory infoStoreFactory;

    @Override
    public InfoStoreFactory getInfoStoreFactory() {
        return infoStoreFactory;
    }
}
