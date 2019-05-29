/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.annocache.service;

import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.info.InfoStoreFactory;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_Factory;
import com.ibm.wsspi.annocache.util.Util_Factory;

/**
 * <p>Core annotation service.  Annotation services are accessed
 * through four root factory types: {@link Util_Factory}, {@link ClassSource_Factory},
 * {@link AnnotationTargets_Factory}, and {@link InfoStoreFactory}.</p>
 */
public interface AnnotationCacheService_Service {
    String getHashText();

    Util_Factory getUtilFactory();

    ClassSource_Factory getClassSourceFactory();

    TargetCache_Factory getTargetCacheFactory();
    AnnotationTargets_Factory getAnnotationTargetsFactory();

    InfoStoreFactory getInfoStoreFactory();
}
