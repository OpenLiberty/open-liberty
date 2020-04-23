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

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl_Factory;
import com.ibm.ws.annocache.info.internal.InfoStoreFactoryImpl;
import com.ibm.ws.annocache.targets.cache.internal.TargetCacheImpl_Factory;
import com.ibm.ws.annocache.targets.cache.internal.TargetCacheImpl_Options;
import com.ibm.ws.annocache.targets.internal.AnnotationTargetsImpl_Factory;
import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.annocache.service.AnnotationCacheService_Service;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_Options;

public class AnnotationCacheServiceImpl_Service implements AnnotationCacheService_Service {
    public static final String CLASS_NAME = "AnnotationCacheServiceImpl_Service";

    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.service");

    //

    // Service entry point ...

    protected void activate(ComponentContext componentContext, Map<String, Object> properties) {
        String methodName = "activate";

        setBundleContext( componentContext.getBundleContext() );

        String workArea = getOsgiWorkArea(); // Need the bundle context to get the work area path.

        setWorkAreaPath(workArea);

        TargetCacheImpl_Options useCacheOptions = TargetCacheImpl_Factory.createOptionsFromProperties();
        if ( !useCacheOptions.getDisabled() ) {
            if ( workArea != null ) {
                useCacheOptions.setDir(workArea + File.separatorChar + TargetCache_Options.CACHE_NAME_DEFAULT);
            }
        }
        setCacheOptions(useCacheOptions);

        if ( logger.isLoggable(Level.FINER) ) { // INFO is temporary
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Cache Disabled [ {0} ]",
                Boolean.valueOf( useCacheOptions.getDisabled() ));
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Cache Dir [ {0} ]",
                useCacheOptions.getDir());
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Cache Write Threads [ {0} ]",
                Integer.valueOf(useCacheOptions.getWriteThreads()));
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Cache Write Limit [ {0} ]",
                Integer.valueOf(useCacheOptions.getWriteLimit()));
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Use Jandex Format For Containers [ {0} ]",
                Boolean.valueOf(useCacheOptions.getUseJandexFormat()));
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "Use Binary Format [ {0} ]",
                Boolean.valueOf(useCacheOptions.getUseBinaryFormat()));
            
        }

        setFactories(); // Need the work area path to setup the cache instance.
   }

    // Test entry point ...

    public void activate(TargetCacheImpl_Options useCacheOptions) {
        setBundleContext(null);
        setWorkAreaPath(null);
        setCacheOptions(useCacheOptions);
        setFactories();
    }

    //

    protected BundleContext bundleContext;

    protected void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    private String getOsgiWorkArea() {
        String methodName = "getOsgiWorkArea";

        File osgiWorkFile = getBundleContext().getDataFile(""); // Empty string obtains the work directory.
        if ( osgiWorkFile == null ) {
            if ( logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "OSGi Platform does not have file system support.");
            }
            return null;
        }

        String osgiWorkPath = osgiWorkFile.getAbsolutePath();
        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "OSGi Work Path [ {0} ]", osgiWorkPath);
        }
        return osgiWorkPath;
    }

    //

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public AnnotationCacheServiceImpl_Service() {
        super();

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }

    //

    private String workAreaPath;

    protected void setWorkAreaPath(String workAreaPath) {
        this.workAreaPath = workAreaPath;
    }

    public String getWorkAreaPath() {
        return workAreaPath;
    }

    private TargetCacheImpl_Options cacheOptions;

    protected void setCacheOptions(TargetCacheImpl_Options cacheOptions) {
        this.cacheOptions = cacheOptions;
    }

    public TargetCacheImpl_Options getCacheOptions() {
        return cacheOptions;
    }

    //

    protected UtilImpl_Factory createUtilFactory() {
        return new UtilImpl_Factory(this);
    }

    protected ClassSourceImpl_Factory createClassSourceFactory() {
        return new ClassSourceImpl_Factory( this, getUtilFactory() );
    }

    protected TargetCacheImpl_Factory createCacheFactory() {
        return new TargetCacheImpl_Factory(this);
    }

    protected AnnotationTargetsImpl_Factory createAnnotationTargetsFactory() {
        return new AnnotationTargetsImpl_Factory(
            this,
            getUtilFactory(),
            getClassSourceFactory(),
            getTargetCacheFactory() );
    }

    protected InfoStoreFactoryImpl createInfoStoreFactory() {
        return new InfoStoreFactoryImpl( this, getUtilFactory() );
    }

    //
    
    protected void setFactories() {
        setUtilFactory( createUtilFactory() );
        setClassSourceFactory( createClassSourceFactory() );

        TargetCacheImpl_Factory useCacheFactory = createCacheFactory();
        useCacheFactory.setOptions( getCacheOptions() );
        setTargetCacheFactory(useCacheFactory);

        setAnnotationTargetsFactory( createAnnotationTargetsFactory() );

        setInfoStoreFactory( createInfoStoreFactory() );
    }

    //

    protected UtilImpl_Factory utilFactory;

    @Override
    public UtilImpl_Factory getUtilFactory() {
        return utilFactory;
    }

    protected void setUtilFactory(UtilImpl_Factory utilFactory) {
        this.utilFactory = utilFactory;
    }

    //

    protected ClassSourceImpl_Factory classSourceFactory;

    @Override
    public ClassSourceImpl_Factory getClassSourceFactory() {
        return classSourceFactory;
    }

    protected void setClassSourceFactory(ClassSourceImpl_Factory classSourceFactory) {
        this.classSourceFactory = classSourceFactory;
    }

    //

    protected TargetCacheImpl_Factory targetCacheFactory;

    @Override
    public TargetCacheImpl_Factory getTargetCacheFactory() {
        return targetCacheFactory;
    }

    protected void setTargetCacheFactory(TargetCacheImpl_Factory targetCacheFactory) {
        this.targetCacheFactory = targetCacheFactory;
    }

    //

    protected AnnotationTargetsImpl_Factory annotationTargetsFactory;

    @Override
    public AnnotationTargetsImpl_Factory getAnnotationTargetsFactory() {
        return annotationTargetsFactory;
    }

    protected void setAnnotationTargetsFactory(AnnotationTargetsImpl_Factory annotationTargetsFactory) {
        this.annotationTargetsFactory = annotationTargetsFactory;
    }

    //

    protected InfoStoreFactoryImpl infoStoreFactory;

    @Override
    public InfoStoreFactoryImpl getInfoStoreFactory() {
        return infoStoreFactory;
    }

    protected void setInfoStoreFactory(InfoStoreFactoryImpl infoStoreFactory) {
        this.infoStoreFactory = infoStoreFactory;
    }
}
