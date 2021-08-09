/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annocache.internal;

import java.net.URL;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;

import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.classsource.ClassSource_ClassLoader;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.classsource.ClassSource_MappedContainer;
import com.ibm.wsspi.annocache.classsource.ClassSource_Options;
import com.ibm.wsspi.annocache.info.ClassInfo;
import com.ibm.wsspi.annocache.info.InfoStore;
import com.ibm.wsspi.annocache.info.InfoStoreException;
import com.ibm.wsspi.annocache.info.InfoStoreFactory;
import com.ibm.wsspi.annocache.service.AnnotationCacheService_Service;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets;

import com.ibm.ws.container.service.annocache.Annotations;
import com.ibm.ws.container.service.annocache.SpecificAnnotations;

/**
 * Common annotations code.
 */
public abstract class AnnotationsImpl implements Annotations {
    public static final TraceComponent tc = Tr.register(AnnotationsImpl.class);
    // private static final String CLASS_NAME = AnnotationsImpl.class.getSimpleName();

    //

    /**
     * Data from a path lookup.
     * 
     * When finding the full path above a specified container, tell where
     * a given parent container is relative to the initial container.
     * 
     * Set the span values to -1 if the parent container is not a parent
     * of the specified container.
     */
    public static class PathData {
        public final String path;
        public final int spans;
        public final int spansAboveParent;
        public final int spansBelowParent;

        public PathData(String path, int spans) {
            this.path = path;
            this.spans = spans;
            this.spansAboveParent = -1;
            this.spansBelowParent = -1;
        }

        public PathData(
            String path, int spans,
            int spansBelowParent, int spansAboveParent) {

            this.path = path;
            this.spans = spans;
            this.spansBelowParent = spansBelowParent;
            this.spansAboveParent = spansAboveParent;
        }
    }

    public static ArtifactContainer getRootOfRoots(ArtifactContainer container) {
        ArtifactEntry entry;
        while ( (entry = container.getRoot().getEntryInEnclosingContainer()) != null ) {
            container = entry.getEnclosingContainer();
        }
        return container;
    }

    /**
     * Obtain the full path to a specified container.
     * 
     * Return data which indicates whether the path reaches above
     * a specified parent container.
     *
     * @param parentContainer A parent container.
     * @param container The container for which to obtain the full path.
     *
     * @return Path data for the container.
     *
     * @throws UnableToAdaptException Thrown if traversal from the containre
     *     to its parents fails.
     */
    public static PathData getPathData(
        Container parentContainer, Container container)
        throws UnableToAdaptException {

        StringBuilder pathBuilder = new StringBuilder();

        int spans = 0;
        boolean foundParent = ( container == parentContainer );

        int spansBelow = 0;
        int spansAbove = 0;

        Entry entry;
        while ( (entry = container.adapt(Entry.class)) != null ) { // throws UnableToAdaptException
            // Each step upwards adds to the count of spans.
            spans++;

            // Acquire the next path-to-root.
            pathBuilder.insert(0,  entry.getPath() );
            container = entry.getRoot();

            // Each step upwards adds to one of the span counts.
            // If the parent hasn't been found, the span is below the
            // parent.  Otherwise, the span is above the parent.

            if ( !foundParent ) {
                spansBelow++;
            } else {
                spansAbove++;
            }

            // Test now if the new parent container is the target
            // parent container.  Do this *after* adjusting the
            // span counts, since the current traversal was used
            // to reach upwards towards the next parent.

            if ( !foundParent ) {
                foundParent = ( container == parentContainer );
            }
        }

        String path = pathBuilder.toString();

        if ( foundParent ) {
            return new PathData(path, spans, spansBelow, spansAbove);
        } else {
            return new PathData(path, spans);
        }
    }

    public static String getPath(Container container) throws UnableToAdaptException {
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, "getPath Initial [ " + container + " ]");
        }
        StringBuilder pathBuilder = new StringBuilder();

        Entry entry;
        while ( (entry = container.adapt(Entry.class)) != null ) { // throws UnableToAdaptException
            pathBuilder.insert(0,  entry.getPath() );
            container = entry.getRoot();
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, "getPath Next [ " + container + " ]");
            }
        }

        return pathBuilder.toString();
    }

    protected String getContainerPath(Container targetContainer) {
        String useAppName = getAppName();
        String useModName = getModName();
        Container modContainer = rootAdaptableContainer;

        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, "AppName [ " + useAppName + " ]");
            Tr.debug(tc, "Mod Name [ " + useModName + " ]");
            Tr.debug(tc, "Module Container [ " + modContainer + " ]");
            Tr.debug(tc, "Target Container [ " + targetContainer + " ]");
        }

        String targetPath;
        try {
            targetPath = getPath(targetContainer);
        } catch ( UnableToAdaptException e ) {
            return null; // FFDC
        }
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, "Target Path [ " + targetPath + " ]");
        }

        ArtifactContainer modDelegate;
        try {
            modDelegate = modContainer.adapt(ArtifactContainer.class);
        } catch ( UnableToAdaptException e ) {
            return null; // FFDC
        }
        if ( tc.isDebugEnabled() ) {
        	Tr.debug(tc, "Module Delegate [ " + modDelegate + " ]");
        }

        ArtifactContainer targetDelegate;
        try {
            targetDelegate = targetContainer.adapt(ArtifactContainer.class);
        } catch ( UnableToAdaptException e ) {
            return null;
        }
        if ( tc.isDebugEnabled() ) {
            Tr.debug(tc, "Target Delegate [ " + targetDelegate + " ]");
        }

        ArtifactContainer rootOfRootsModDelegate = getRootOfRoots(modDelegate);
    	ArtifactContainer rootOfRootsTargetDelegate = getRootOfRoots(targetDelegate);
        if ( tc.isDebugEnabled() ) {
        	Tr.debug(tc, "Module Delegate Root-of-roots [ " + rootOfRootsModDelegate + " ]");
        	Tr.debug(tc, "Target Delegate Root-of-roots[ " + rootOfRootsTargetDelegate + " ]");
        }

        String targetPathCase;

        if ( targetPath.isEmpty() ||
             (modDelegate == rootOfRootsModDelegate) ||
             (rootOfRootsModDelegate == null) || (rootOfRootsTargetDelegate == null) ||
             (rootOfRootsModDelegate != rootOfRootsTargetDelegate) ) {

            if ( (useModName == null) || useModName.isEmpty() ) {
                if ( (useAppName == null) || useAppName.isEmpty() ) {
                    Tr.warning(tc, "Unable to obtain path for container [ " + targetContainer + " ] relative to [ " + modContainer + " ]");
                    return null;

                } else {
                    if ( targetPath.isEmpty() ) {
                        targetPath = useAppName;
                        targetPathCase = "App name replaces empty target path";
                    } else {
                        targetPath = useAppName + "_" + targetPath;
                        targetPathCase = "App name prefix to non-local target path";
                    }
                }

            } else {
                if ( targetPath.isEmpty() ) {
                    targetPath = useModName;
                    targetPathCase = "Mod name replaces empty target path";
                } else {
                    targetPath = useModName + "_" + targetPath;
                    targetPathCase = "Mod name prefix to non-local target path";
                }
            }

        } else { 
            targetPathCase = "Full local path";
        }

        String message = getClass().getSimpleName() + ".getContainerPath:" +
            " Container [ " + targetContainer + " ]" +
            " Path [ " + targetPath + " ]: " + targetPathCase;
        Tr.debug(tc, message);

        return targetPath;
    }

    //
    
    @SuppressWarnings("unchecked")
    protected static <T> T cacheGet(
        OverlayContainer container,
        String targetPath, Class<T> targetClass) {

        Object retrievedInfo = container.getFromNonPersistentCache(targetPath, targetClass);
        return (T) retrievedInfo;
    }

    protected static <T> void cachePut(
        OverlayContainer container,
        String targetPath, Class<T> targetClass,
        T targetObject) {

        container.addToNonPersistentCache(targetPath, targetClass, targetObject);
    }

    protected static <T> void cacheRemove(
        OverlayContainer container,
        String targetPath, Class<T> targetClass) {

        container.removeFromNonPersistentCache(targetPath, targetClass);
    }

    //

    /**
     * Create annotations data for an annotations adapter.
     *
     * The adapter provides access to service entities, most importantly, the
     * annotation factories.
     *
     * @param annotationsAdapter The adapter which is creating the annotations data.
     * @param rootContainer The root adaptable container.  Currently always the same
     *     as 'rootAdaptableContainer'.
     * @param rootOverlayContainer The root overlay container.
     * @param rootDelegateContainer The delegate of the root adaptable container.
     * @param rootAdaptableContainer The root adaptable container.  Currently, always
     *     the same as 'rootContainer'.
     * @param appName The name of the enclosing application.  Null if there is no enclosing
     *     application.
     * @param isUnnamedMod Set that the module is unnamed.  This prevents default module
     *     naming from occurring. 
     * @param modName The name of the enclosing module.  Null if there is no enclosing module.
     * @param modCatName A category name for the module.  Used to enable multiple results for
     *     ths same module.
     */
    public AnnotationsImpl(
        AnnotationsAdapterImpl annotationsAdapter,
        Container rootContainer, OverlayContainer rootOverlayContainer,
        ArtifactContainer rootDelegateContainer, Container rootAdaptableContainer,
        String appName, boolean isUnnamedMod, String modName, String modCatName) {

        // (new Throwable("Annotations (cache)")).printStackTrace(System.out);

        this.annotationsAdapter = annotationsAdapter;

        this.rootContainer = rootContainer; // Currently the same as 'rootAdaptableContainer', and unused.

        this.rootDelegateContainer = rootDelegateContainer; // Delegate underlying the target container.
        this.rootOverlayContainer = rootOverlayContainer; // Overlay view of the target container.
        this.rootAdaptableContainer = rootAdaptableContainer; // Adaptable view of the target container.

        this.appName = appName;
        this.modName = modName;
        this.modCatName = modCatName;

        this.isLightweight = AnnotationTargets_Factory.IS_NOT_LIGHTWEIGHT;

        this.rootClassSource = null;

        this.isSetTargets = false;
        this.annotationTargets = null;

        this.isSetInfoStore = false;
        this.infoStore = null;

        //

        if ( tc.isDebugEnabled() ) {
            String prefix = getClass().getSimpleName() + ".<init>: ";
            Tr.debug(tc, prefix + "Root container [ " + rootContainer + " ]");
            for ( URL url : rootContainer.getURLs() ) {
                Tr.debug(tc, prefix + "  URL [ " + url + " ]");
            }
            Tr.debug(tc, prefix + "Application [ " + appName + " ]");
            Tr.debug(tc, prefix + "Module [ " + modName + " ]");
            Tr.debug(tc, prefix + "Module Category [ " + modCatName + " ]");
        }
    }

    //

    private final AnnotationsAdapterImpl annotationsAdapter;

    public AnnotationCacheService_Service getAnnoCacheService() {
        try {
            return annotationsAdapter.getAnnoCacheService();
            // throws UnableToAdaptException
        } catch ( UnableToAdaptException e ) {
            return null; // FFDC
        }
    }

    public ClassSource_Factory getClassSourceFactory() {
        AnnotationCacheService_Service useService = getAnnoCacheService();
        return ( (useService == null) ? null : useService.getClassSourceFactory() );
    }

    public AnnotationTargets_Factory getTargetsFactory() {
        AnnotationCacheService_Service useService = getAnnoCacheService();
        return ( (useService == null) ? null : useService.getAnnotationTargetsFactory() );
    }

    public InfoStoreFactory getInfoStoreFactory() {
        AnnotationCacheService_Service useService = getAnnoCacheService();
        return ( (useService == null) ? null : useService.getInfoStoreFactory() );
    }

    //

    @SuppressWarnings("unused")
    private final Container rootContainer;

    //

    private final ArtifactContainer rootDelegateContainer;

    public ArtifactContainer getRootDelegateContainer() {
        return rootDelegateContainer;
    }

    //

    private final OverlayContainer rootOverlayContainer;

    public OverlayContainer getRootOverlayContainer() {
        return rootOverlayContainer;
    }

    protected <T> T cacheGet(Class<T> targetClass) {
        return cacheGet(getRootOverlayContainer(), getContainerPath(), targetClass);
    }

    protected <T> void cachePut(Class<T> targetClass, T targetObject) {
        cachePut( getRootOverlayContainer(), getContainerPath(), targetClass, targetObject);
    }

    protected <T> void cacheRemove(Class<T> targetClass) {
        cacheRemove( getRootOverlayContainer(), getContainerPath(), targetClass);
    }

    //

    private final Container rootAdaptableContainer;

    @Override
    public Container getContainer() {
        return rootAdaptableContainer;
    }

    @Override
    public String getContainerName() {
        return getContainer().getName();
    }

    @Override 
    public String getContainerPath() {
        return getContainer().getPath();
    }

    //

    private boolean useJandex;

    @Override
    public boolean getUseJandex() {
        return useJandex;
    }

    @Override
    public void setUseJandex(boolean useJandex) {
        this.useJandex = useJandex;
    }

    protected ClassSource_Options createOptions() {
        ClassSource_Factory classSourceFactory = getClassSourceFactory();
        if ( classSourceFactory == null ) {
            return null;
        }

        ClassSource_Options options = classSourceFactory.createOptions();
        if ( !options.getIsSetUseJandex() ) {
            options.setUseJandex( getUseJandex() );
        } else {
            // TODO: *Maybe*, NLS enable this.
            //       The override is an unpublished system property.  This message should
            //       never appear except during internal testing.
            Tr.info(tc, "Application jandex setting [ " + getUseJandex() + " ] overridden by property setting [ " + options.getUseJandex() + " ]");
        }
        return options;
    }

    private String appName;

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public void setAppName(String appName) {
        this.appName = appName;
    }

    private String modName;
    private boolean isUnnamedMod;

    @Override
    public String getModName() {
        return modName;
    }

    @Override
    public void setIsUnnamedMod(boolean isUnnamedMod) {
        this.isUnnamedMod = isUnnamedMod;
    }

    @Override 
    public boolean getIsUnnamedMod() { 
        return isUnnamedMod;
    }

    protected void forceModName() throws UnableToAdaptException {
        if ( getIsUnnamedMod() ) {
            return;
        }

        if ( (modName == null) || modName.isEmpty() ) {
            String modNameCase;
            String useModName = getPath( getContainer() ); // 'getPath' throws UnableToAdaptException
            if ( (useModName == null) || useModName.isEmpty() ) {
                useModName = getAppName();
                if ( (useModName == null) || useModName.isEmpty() ) {
                    useModName = null;
                    modNameCase = "unavailable";
                    return;
                } else {
                    modNameCase = "assigned from application";
                }
            } else {
                modNameCase = "assigned from module container";
            }
            modName = useModName;

            if ( tc.isDebugEnabled() ) {
                String message = getClass().getSimpleName() + ".forceModName:" +
                    " App [ " + getAppName() + " ]" +
                    " Container [ " + getContainer() + " ]" +
                    " Mod [ " + modName + " ] (" + modNameCase + ")";
                Tr.debug(tc, message);
            }
        }
    }

    @Override
    public void setModName(String modName) {
        this.modName = modName;
    }

    private final String modCatName;
    
    @Override
    public String getModCategoryName() {
        return modCatName;
    }

    //

    private boolean isLightweight;

    @Override
    public boolean getIsLightweight() {
        return isLightweight;
    }

    @Override
    public void setIsLightweight(boolean isLightweight) {
        this.isLightweight = isLightweight;
    }

    //

    public class ClassSourceLock {
        // EMPTY
    }
    private final ClassSourceLock classSourceLock = new ClassSourceLock();

    protected ClassLoader classLoader;

    protected ClassSource_Aggregate rootClassSource;

    protected boolean addContainerClassSource(String path, Container container) {
        return addContainerClassSource(path, container, ClassSource_Factory.UNUSED_ENTRY_PREFIX);
    }

    protected boolean addContainerClassSource(String path, Container container, String entryPrefix) {
        ClassSource_MappedContainer containerClassSource;
        try {
            containerClassSource = getClassSourceFactory().createContainerClassSource(
                rootClassSource, path, container, entryPrefix); // throws ClassSource_Exception
        } catch ( ClassSource_Exception e ) {
            return false; // FFDC
        }
        rootClassSource.addClassSource(containerClassSource);
        return true;
    }

    protected boolean addContainerClassSource(String path, Container container, ScanPolicy scanPolicy) {
        return addContainerClassSource(path, container, ClassSource_Factory.UNUSED_ENTRY_PREFIX, scanPolicy);
    }

    protected boolean addContainerClassSource(String path, Container container, String entryPrefix, ScanPolicy scanPolicy) {
        ClassSource_MappedContainer containerClassSource;
        try {
            containerClassSource = getClassSourceFactory().createContainerClassSource(
                rootClassSource, path, container, entryPrefix); // throws ClassSource_Exception
        } catch ( ClassSource_Exception e ) {
            return false; // FFDC
        }
        rootClassSource.addClassSource(containerClassSource, scanPolicy);
        return true;
    }

    @Override
    public ClassLoader getClassLoader() {
        synchronized ( classSourceLock ) {
            return classLoader;
        }
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        synchronized ( classSourceLock ) { 
            if ( this.classLoader != null ) {
                if ( this.classLoader != classLoader ) {
                    throw new IllegalArgumentException("Duplicate class loader [ " + classLoader + " ]; previous [ " + this.classLoader + " ]");
                } else {
                    return; // Nothing to do.
                }
            } else {
                this.classLoader = classLoader;
                addExternalToClassSource();
            }
        }
    }

    //

    @Override
    public ClassSource_Aggregate releaseClassSource() {
        synchronized ( classSourceLock ) { 
            if ( rootClassSource == null ) {
                return null;
            }

            ClassSource_Aggregate oldClassSource = rootClassSource;
            rootClassSource = null;

            return oldClassSource;
        }
    }

    @Override
    public ClassSource_Aggregate getClassSource() {
        synchronized( classSourceLock ) {
            if ( rootClassSource == null ) {
                rootClassSource = createRootClassSource();
                if ( rootClassSource == null ) {
                    String message =
                        "Null class source created for " +
                        " app [ " + appName + " ]" +
                        " module [ " + modName + " ]" +
                        " module category [ " + modCatName + " ]";
                    Tr.warning(tc, getClass().getName() + ": " + message);
                }
                addInternalToClassSource();
                addExternalToClassSource();
            }
            return rootClassSource;
        }
    }

    protected ClassSource_Aggregate createRootClassSource() {
        ClassSource_Factory classSourceFactory = getClassSourceFactory();
        if ( classSourceFactory == null ) {
            return null;
        }

        try {
            forceModName(); // throws UnableToAdaptException
        } catch ( UnableToAdaptException e ) {
            // FFDC
            return null;
        }

        String useAppName = getAppName();
        String useModName = getModName();
        String useModCatName = getModCategoryName();

        ClassSource_Options options = createOptions();

        if ( tc.isDebugEnabled() ) {
            String prefix = getClass().getSimpleName() + ".createRootClassSource:";
            String message1 = prefix +
                " Class source " +
                " app [ " + useAppName + " ]" +
                " module [ " + useModName + " ]" +
                " module category [ " + useModCatName + " ]";
            String message2 = prefix +
                " Scan threads [ " + options.getScanThreads() + " ]";
            Tr.debug(tc, message1);
            Tr.debug(tc, message2);
        }
        
        try {
            return classSourceFactory.createAggregateClassSource(
                useAppName, useModName, useModCatName,
                options); // throws ClassSource_Exception
        } catch ( ClassSource_Exception e ) {
            return null; // FFDC
        }
    }

    protected abstract void addInternalToClassSource();

    protected void addExternalToClassSource() {
        if ( rootClassSource == null ) {
            return; // Nothing yet to do.
        } else if ( classLoader == null ) {
            return; // Nothing yet to do.
        }

        ClassSource_Factory classSourceFactory = getClassSourceFactory();
        if ( classSourceFactory == null ) {
            return;
        }

        ClassSource_ClassLoader classLoaderClassSource;
        try {
            classLoaderClassSource = classSourceFactory.createClassLoaderClassSource(
                rootClassSource, "classloader", classLoader); // throws ClassSource_Exception
        } catch ( ClassSource_Exception e ) {
            return; // FFDC
        }
        rootClassSource.addClassLoaderClassSource(classLoaderClassSource);
    }

    //

    public class TargetsLock {
        // EMPTY
    }
    private final TargetsLock targetsLock = new TargetsLock();

    private boolean isSetTargets;
    private AnnotationTargets_Targets annotationTargets;

    @Override
    public AnnotationTargets_Targets getTargets() {
        synchronized( targetsLock ) {
            if ( !isSetTargets ) {
                isSetTargets = true;
                annotationTargets = createTargets();
            }
            return annotationTargets;
        }
    }

    @Override
    public AnnotationTargets_Targets getAnnotationTargets() {
        return getTargets();
    }

    @Override
    public AnnotationTargets_Targets releaseTargets() {
        synchronized ( targetsLock ) {
            if ( !isSetTargets ) {
                return null;
            }

            isSetTargets = false;

            AnnotationTargets_Targets oldTargets = annotationTargets;
            annotationTargets = null;

            cacheRemove(AnnotationTargets_Targets.class);

            return oldTargets;
        }
    }

    protected AnnotationTargets_Targets createTargets() {
        ClassSource_Aggregate useClassSource = getClassSource();
        if ( useClassSource == null ) {
            String message =
                "Null class source creating targets for " +
                " app [ " + appName + " ]" +
                " module [ " + modName + " ]" +
                " module category [ " + modCatName + " ]";
            Tr.warning(tc, getClass().getName() + ": " + message);
            return null;
        }

        AnnotationTargets_Factory targetsFactory = getTargetsFactory();
        if ( targetsFactory == null ) {
            String message =
                "Null factory creating targets for " +
                " app [ " + appName + " ]" +
                " module [ " + modName + " ]" +
                " module category [ " + modCatName + " ]";
            Tr.warning(tc, getClass().getName() + ": " + message);
            return null;
        }

        if ( classLoader == null ) {
            String message =
                "Null class loader creating targets for " +
                " app [ " + appName + " ]" +
                " module [ " + modName + " ]" +
                " module category [ " + modCatName + " ]";
            Tr.debug(tc, getClass().getName() + ": " + message);
        }

        AnnotationTargets_Targets useTargets;
        try {
            useTargets = targetsFactory.createTargets( getIsLightweight() ); // throws AnnotationTargets_Exception
        } catch ( AnnotationTargets_Exception e ) {
            return null; // FFDC
        }

        // Set the class source here, before the targets are stored
        // to the non-persistent cache.  The class source can then
        // be stored only when the targets are initially created, and
        // the store does not need synchronization.

        useTargets.scan(useClassSource);

        return useTargets;
    }

    //

    public class InfoStoreLock {
        // EMPTY
    }
    private final InfoStoreLock infoStoreLock = new InfoStoreLock();

    private boolean isSetInfoStore;
    private InfoStore infoStore;

    @Override
    public InfoStore releaseInfoStore() {
        synchronized ( infoStoreLock ) {
            if ( !isSetInfoStore ) {
                return null;
            }

            isSetInfoStore = false;

            InfoStore oldStore = infoStore;
            infoStore = null;

            cacheRemove(InfoStore.class);

            return oldStore;
        }
    }

    public InfoStore getInfoStore() {
        synchronized( infoStoreLock ) {
            if ( !isSetInfoStore ) {
                isSetInfoStore = true;
                infoStore = createInfoStore();
            }

            return infoStore;
        }
    }

    protected InfoStore createInfoStore() {
        ClassSource_Aggregate useClassSource = getClassSource();
        if ( useClassSource == null ) {
            return null;
        }

        InfoStoreFactory infoStoreFactory = getInfoStoreFactory();
        if ( infoStoreFactory == null ) {
            return null;
        }

        try {
            return infoStoreFactory.createInfoStore(useClassSource);
        } catch ( InfoStoreException e ) {
            return null; // FFDC
        }
    }

    //

    @Override
    public boolean isIncludedClass(String className) {
        AnnotationTargets_Targets useTargets = getTargets();
        if ( useTargets == null ) {
            return false;
        } else {
            return useTargets.isSeedClassName(className);
        }
    }

    @Override
    public boolean isPartialClass(String className) {
        AnnotationTargets_Targets useTargets = getTargets();
        if ( useTargets == null ) {
            return false;
        } else {
            return useTargets.isPartialClassName(className);
        }
    }

    @Override
    public boolean isExcludedClass(String className) {
        AnnotationTargets_Targets useTargets = getTargets();
        if ( useTargets == null ) {
            return false;
        } else {
            return useTargets.isExcludedClassName(className);
        }
    }

    @Override
    public boolean isExternalClass(String className) {
        AnnotationTargets_Targets useTargets = getTargets();
        if ( useTargets == null ) {
            return false;
        } else {
            return useTargets.isExternalClassName(className);
        }
    }

    //

    @Override
    public ClassInfo getClassInfo(String className) {
        return getInfoStore().getDelayableClassInfo(className);
    }

    @Override
    public void openInfoStore() {
        try {
            getInfoStore().open();
        } catch ( InfoStoreException e ) {
            // FFDC
        }
    }

    @Override
    public void closeInfoStore() {
        try {
            getInfoStore().close();
        } catch ( InfoStoreException e ) {
            // FFDC
        }
    }

    //

    @Override
    public SpecificAnnotations getSpecificAnnotations(Set<String> specificClassNames) throws UnableToAdaptException {
        ClassSource_Aggregate useClassSource = getClassSource();
        if ( useClassSource == null ) {
            return null;
        }

        AnnotationTargets_Factory useTargetsFactory = getTargetsFactory();
        if ( useTargetsFactory == null ) {
            return null;
        }

        AnnotationTargets_Targets specificTargets;
        try {
            specificTargets = useTargetsFactory.createTargets();
        } catch (AnnotationTargets_Exception e) {
            String msg = Tr.formatMessage(tc, "failed.to.obtain.module.annotation.targets.CWWKM0473E",
                "Failed to obtain annotation targets", e);
            throw new UnableToAdaptException(msg);
        }

        try {
            specificTargets.scan(useClassSource, specificClassNames);
        } catch ( AnnotationTargets_Exception e ) {
            String msg = Tr.formatMessage(tc, "failed.to.obtain.module.annotation.targets.CWWKM0474E",
                "Failed to obtain annotation targets", specificClassNames, e);
            throw new UnableToAdaptException(msg);
        }

        return new SpecificAnnotationsImpl(specificTargets);
    }

    //

    @Override
    public boolean hasSpecifiedAnnotations(Collection<String> annotationClassNames) {
        AnnotationTargets_Targets useTargets = getTargets();
        if ( useTargets == null ) {
            return false;
        }

        // d95160: The prior implementation obtained classes from the SEED location.
        //         That implementation is not changed by d95160.

        for ( String annotationClassName : annotationClassNames ) {
            Set<String> annotatedClassNames = useTargets.getAnnotatedClasses(annotationClassName);
            if ( !annotatedClassNames.isEmpty() ) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Trivial
    public Set<String> getClassesWithAnnotation(String annotationClassName) {
        return getClassesWithAnnotations( Collections.singletonList(annotationClassName) );
    }

    @Override
    public Set<String> getClassesWithAnnotations(Collection<String> annotationClassNames) {
        AnnotationTargets_Targets useTargets = getTargets();
        if ( useTargets == null ) {
            return Collections.emptySet();
        }

        Set<String> annotatedClassNames = null;

        for ( String annotationClassName : annotationClassNames ) {
            Set<String> nextClassNames = useTargets.getAnnotatedClasses(annotationClassName);
            if ( !nextClassNames.isEmpty() ) {
                if ( annotatedClassNames == null ) {
                    annotatedClassNames = new HashSet<String>(nextClassNames);
                } else {
                    annotatedClassNames.addAll(nextClassNames);
                }
            }
        }

        if ( annotatedClassNames == null ) {
            return Collections.emptySet();
        } else {
            return annotatedClassNames;
        }
    }

    @Override
    public Set<String> getClassesWithSpecifiedInheritedAnnotations(Collection<String> annotationClassNames) {
        // String methodName = "getClassesWithSpecifiedInheritedAnnotations";
        // String prefix = CLASS_NAME + "." + methodName + ": ";
        // Tr.info(tc, prefix + " ENTER Annotations [ " + annotationClassNames + " ]");

        AnnotationTargets_Targets useTargets = getTargets();
        if ( useTargets == null ) {
            // Tr.info(tc, prefix + "RETURN [ ]: Null targets");
            return Collections.emptySet();
        }

        Set<String> allAnnotatedClassNames = new HashSet<String>();

        for ( String annotationClassName : annotationClassNames ) {
            Set<String> annotatedClassNames =
                useTargets.getAllInheritedAnnotatedClasses(annotationClassName);
            // Tr.info(tc, prefix + "Annotation [ " + annotationClassName + " ] [ " + annotatedClassNames + " ]");
            allAnnotatedClassNames.addAll(annotatedClassNames);
        }

        // Tr.info(tc, prefix + "RETURN [ " + allAnnotatedClassNames + " ]");
        return allAnnotatedClassNames;
    }

    //

    protected long scanStart() {
        long startTime = getTimeInNanoSec();
        return startTime;
    }

    protected long scanEnd(long startTime) {
        long endTime = getTimeInNanoSec();
        long elapsedTime = endTime - startTime;
        return elapsedTime;
    }

    private long getTimeInNanoSec() {
        return System.nanoTime();
    }
}
