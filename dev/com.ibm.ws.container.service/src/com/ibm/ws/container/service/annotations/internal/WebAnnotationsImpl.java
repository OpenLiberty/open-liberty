/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annotations.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.FragmentAnnotations;
import com.ibm.ws.container.service.annotations.SpecificAnnotations;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.container.service.config.WebFragmentsInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.InfoStoreException;
import com.ibm.wsspi.anno.info.InfoStoreFactory;
import com.ibm.wsspi.anno.service.AnnotationService_Service;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/*
 * Web module annotation service implementation.
 *
 * This implementation acts (in effect) as both a Future<AnnotationTargets_Targets>
 * and a Future<InfoStore>, with a three part resolution:
 *
 * 1) An initial adapt is performed on the root adaptable container of a module.
 *    Currently, the module must be a web module.
 *
 * 2) Completion parameters are assigned into the future: These are an application
 *    name, a module name, and a module root classloader.
 *
 * 3) The future is resolved through an appropriate getter.
 *
 * The implementation performs steps using web module rules.
 *
 * Note that the initial adapt call accepts four parameters.  The additional
 * parameters are accepted as debugging assists.
 *
 * The expected usage is for a target module to obtain an annotation services
 * object, and to retain a reference to that services object.
 *
 * The services object has retained state, which is shared between the two
 * obtainable objects.  That allows the class source (which has useful tables
 * of class lookup information) to be shared, and provides storage so that
 * multiple callers obtain the same target or info store objects.
 *
 * Current references are from:
 *
 * com.ibm.ws.webcontainer.osgi.DeployedModImpl.adapt(Class<T>)
 *
 * That adapt implementation provides three entries into the annotation
 * services:
 *
 * *) DeployedModule adapt to ClassSource_Aggregate
 * *) DeployedModule adapt to AnnotationTargets_Targets
 * *) DeployedModule adapt to ClassSource
 *
 * Notification plan:
 *
 * Adaptation to annotation targets requires a possibly time consuming scan.
 *
 * Informational messages are generated for the initiation of a scan, and for the
 * completion of a scan.
 */
public class WebAnnotationsImpl implements WebAnnotations {

    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(WebAnnotationsImpl.class);

    // Reference to the underlying annotation service.  That links to the
    // more detailed service entities.
    private final AnnotationService_Service annotationService;

    // Debugging values ... these three are related to the container which is being adapted.
    private final Container rootContainer;
    private final OverlayContainer rootOverlayContainer;
    private final ArtifactContainer rootArtifactContainer;

    // The actual container which is being adapted.  This is the root
    // container of a target module.  Currently, the root container must
    // be for a web module.
    private final Container adaptableContainer;

    // Web app specific values ... these are retrieved from the overlay container.
    private final ApplicationInfo appInfo;
    private final String appName;
    private final WebModuleInfo webModuleInfo;
    private final String webModuleName;

    // Note that this should be the module context class loader, not the entire class
    // loader of the module.
    private final ClassLoader webModuleClassLoader;

    // TODO: Are the module manifest entries and application library entries needed?

    // TODO: Is the class loader, as currently retrieved, the subset class loader,
    //       or is it for the entire module?

    private WebFragmentsInfo webFragments;

    // Mapping between unique ids and fragments
    private Map<String, WebFragmentInfo> idToFragmentMap;

    // Service results ...
    private ClassSource_Aggregate webModuleClassSource;
    private AnnotationTargets_Targets webModuleAnnotationTargets;
    private InfoStore webModuleInfoStore;

    //

    public WebAnnotationsImpl(Container root,
                              OverlayContainer rootOverlay,
                              ArtifactContainer artifactContainer,
                              Container containerToAdapt,
                              AnnotationService_Service annotationService) {

        this.annotationService = annotationService;

        this.rootContainer = root;
        this.rootOverlayContainer = rootOverlay;
        this.rootArtifactContainer = artifactContainer;
        this.adaptableContainer = containerToAdapt;

        this.webModuleInfo = this.retrieveFromOverlay(WebModuleInfo.class);
        this.webModuleName = this.webModuleInfo.getName();

        this.appInfo = this.webModuleInfo.getApplicationInfo();
        this.appName = this.appInfo.getName();

        this.webModuleClassLoader = this.webModuleInfo.getClassLoader();
    }

    // Service bridge access ...

    private AnnotationService_Service getAnnotationService() {
        return this.annotationService;
    }

    private ClassSource_Factory getClassSourceFactory() {
        return getAnnotationService().getClassSourceFactory();
    }

    private AnnotationTargets_Factory getAnnotationTargetsFactory() {
        return getAnnotationService().getAnnotationTargetsFactory();
    }

    private InfoStoreFactory getInfoStoreFactory() {
        return getAnnotationService().getInfoStoreFactory();
    }

    // Initial parameters ...

    protected Container getRootContainer() {
        return this.rootContainer;
    }

    private OverlayContainer getRootOverlayContainer() {
        return this.rootOverlayContainer;
    }

    @SuppressWarnings("unchecked")
    private <T> T retrieveFromOverlay(Class<T> targetClass) {
        Object retrievedInfo = getRootOverlayContainer().getFromNonPersistentCache(getRootArtifactPath(), targetClass);
        return (T) retrievedInfo;
    }

    private <T> void addToOverlay(Class<T> targetClass, T targetObject) {
        getRootOverlayContainer().addToNonPersistentCache(getRootArtifactPath(),
                                                          targetClass,
                                                          targetObject);
    }

    private ArtifactContainer getRootArtifactContainer() {
        return this.rootArtifactContainer;
    }

    private String getRootArtifactPath() {
        return getRootArtifactContainer().getPath();
    }

    private Container getAdaptableContainer() {
        return this.adaptableContainer;
    }

    //

    private String getAppName() {
        return this.appName;
    }

    private String getWebModuleName() {
        return this.webModuleName;
    }

    private ClassLoader getWebModuleClassLoader() {
        return this.webModuleClassLoader;
    }

    // Intermediate parameters ...

    // TODO: Add application library jars.
    // TODO: Add module manifest jars.

    //

    @Override
    public ClassInfo getClassInfo(String className) throws UnableToAdaptException {
        return getInfoStore().getDelayableClassInfo(className);
    }

    private WebFragmentsInfo getWebFragmentsInfo() throws UnableToAdaptException {
        if (this.webFragments == null) {
            Container useAdaptableContainer = getAdaptableContainer();
            this.webFragments = useAdaptableContainer.adapt(WebFragmentsInfo.class);
        }
        return this.webFragments;
    }

    @Override
    public List<WebFragmentInfo> getOrderedItems() throws UnableToAdaptException {
        return getWebFragmentsInfo().getOrderedFragments();
    }

    @Override
    public List<WebFragmentInfo> getExcludedItems() throws UnableToAdaptException {
        return getWebFragmentsInfo().getExcludedFragments();
    }

    //

    @Override
    public ClassSource_Aggregate getClassSource() throws UnableToAdaptException {
        if (this.webModuleClassSource != null) {
            return this.webModuleClassSource;
        }

        this.webModuleClassSource = retrieveFromOverlay(ClassSource_Aggregate.class);
        if (this.webModuleClassSource != null) {
            return this.webModuleClassSource;
        }

        String containerName = getAppName() + " " + getWebModuleName();

        ClassSource_Aggregate useClassSource;

        ClassSource_Options options = getClassSourceFactory().createOptions();
        options.setUseJandex(appInfo.getUseJandex());

        try {
            useClassSource = getClassSourceFactory().createAggregateClassSource(containerName, options);
        } catch (ClassSource_Exception e) {
            String msg = Tr.formatMessage(tc, "failed.to.create.module.class.source.CWWKM0465E", "Failed to create module class source", containerName, e);
            throw new UnableToAdaptException(msg);
        }

        for (WebFragmentInfo fragment : getOrderedItems()) {
            String name = fragment.getLibraryURI();
            int nameSuffix = 1;
            if (idToFragmentMap == null) {
                idToFragmentMap = new HashMap<String, WebFragmentInfo>();
            }
            String cName = getClassSourceFactory().getCanonicalName(name);
            name = cName;
            while (idToFragmentMap.containsKey(name)) {
                nameSuffix++;
                name = cName + "_" + nameSuffix;
            }
            idToFragmentMap.put(name, fragment);

            ClassSource_Aggregate.ScanPolicy scanPolicy;
            if (fragment.isSeedFragment()) {
                scanPolicy = ClassSource_Aggregate.ScanPolicy.SEED;
            } else {
                scanPolicy = ClassSource_Aggregate.ScanPolicy.PARTIAL;
            }

            Container container = fragment.getFragmentContainer();

            try {
                ClassSource containerSource = getClassSourceFactory().createContainerClassSource(useClassSource.getInternMap(), name, container);
                useClassSource.addClassSource(containerSource, scanPolicy);

                cName = containerSource.getCanonicalName();
                if (!name.equals(cName)) {
                    String msg = Tr.formatMessage(tc, "canonical.name.does.not.match.original.name.for.module.CWWKM0466E",
                                                  "Canonical name does not match original name", containerName, cName, name);
                    throw new UnableToAdaptException(msg);
                }
            } catch (ClassSource_Exception e) {
                String msg = Tr.formatMessage(tc, "failed.to.create.module.fragment.class.source.CWWKM0467E",
                                              "Failed to create module fragment class source", containerName, name, e);
                throw new UnableToAdaptException(msg);
            }
        }

        for (WebFragmentInfo fragment : getExcludedItems()) {
            String name = fragment.getLibraryURI();
            Container container = fragment.getFragmentContainer();
            ClassSource_Aggregate.ScanPolicy scanPolicy = ClassSource_Aggregate.ScanPolicy.EXCLUDED;

            try {
                ClassSource containerSource = getClassSourceFactory().createContainerClassSource(useClassSource.getInternMap(), name, container);
                useClassSource.addClassSource(containerSource, scanPolicy);
            } catch (ClassSource_Exception e) {
                String msg = Tr.formatMessage(tc, "failed.to.create.module.fragment.class.source.CWWKM0468E",
                                              "Failed to create module fragment class source", containerName, name, e);
                throw new UnableToAdaptException(msg);
            }
        }

        String clName = containerName + " parent classloader";
        ClassLoader cl = getWebModuleClassLoader();

        try {
            ClassSource clSource = getClassSourceFactory().createClassLoaderClassSource(useClassSource.getInternMap(), clName, cl);
            useClassSource.addClassSource(clSource, ClassSource_Aggregate.ScanPolicy.EXTERNAL);
        } catch (ClassSource_Exception e) {
            String msg = Tr.formatMessage(tc, "failed.to.create.class.source.for.module.classloader.CWWKM0469E",
                                          "Failed to create module fragment class source", containerName, clName, e);
            throw new UnableToAdaptException(msg);
        }

        this.webModuleClassSource = useClassSource;
        this.addToOverlay(ClassSource_Aggregate.class, this.webModuleClassSource);

        return this.webModuleClassSource;
    }

    private String getClassSourceName(WebFragmentInfo webFragmentItem) {

        if (idToFragmentMap == null) {
            return null;
        }

        for (Map.Entry<String, WebFragmentInfo> mapEntry : idToFragmentMap.entrySet()) {
            if (mapEntry.getValue() == webFragmentItem) {
                return mapEntry.getKey();
            }
        }
        return null;
    }

    @Override
    public AnnotationTargets_Targets getAnnotationTargets() throws UnableToAdaptException {
        if (this.webModuleAnnotationTargets != null) {
            return this.webModuleAnnotationTargets;
        }

        this.webModuleAnnotationTargets = retrieveFromOverlay(AnnotationTargets_Targets.class);
        if (this.webModuleAnnotationTargets != null) {
            return this.webModuleAnnotationTargets;
        }

        long startTime = reportScanStart();

        ClassSource_Aggregate useClassSource = getClassSource();

        AnnotationTargets_Factory useTargetsFactory = getAnnotationTargetsFactory();

        AnnotationTargets_Targets useTargets;
        try {
            useTargets = useTargetsFactory.createTargets();
        } catch (AnnotationTargets_Exception e) {
            String msg = Tr.formatMessage(tc, "failed.to.obtain.module.annotation.targets.CWWKM0470E",
                                          "Failed to obtain module annotation targets", e);
            throw new UnableToAdaptException(msg);
        }

        // Hook point for serialization: The scanner uses the class source as input, and transfers
        // class and annotations data to the annotation targets data set.
        //
        // An alternative would use the targets serialization, and instead read the targets data from
        // an input stream.

        try {
            useTargets.scan(useClassSource);
        } catch (AnnotationTargets_Exception e) {
            String msg = Tr.formatMessage(tc, "failed.to.obtain.module.annotation.targets.CWWKM0471E",
                                          "Failed to obtain module annotation targets", e);
            throw new UnableToAdaptException(msg);
        }

        reportScanEnd(startTime, useTargets);

        this.webModuleAnnotationTargets = useTargets;
        this.addToOverlay(AnnotationTargets_Targets.class, this.webModuleAnnotationTargets);

        return this.webModuleAnnotationTargets;
    }

    @Override
    public InfoStore getInfoStore() throws UnableToAdaptException {
        if (this.webModuleInfoStore != null) {
            return this.webModuleInfoStore;
        }

        this.webModuleInfoStore = retrieveFromOverlay(InfoStore.class);
        if (this.webModuleInfoStore != null) {
            return this.webModuleInfoStore;
        }

        ClassSource_Aggregate useClassSource = getClassSource();

        try {
            this.webModuleInfoStore = getInfoStoreFactory().createInfoStore(useClassSource);
        } catch (InfoStoreException e) {
            String msg = Tr.formatMessage(tc, "failed.to.obtain.module.annotation.targets.CWWKM0472E",
                                          "Failed to obtain module annotation targets", e);
            throw new UnableToAdaptException(msg);
        }

        this.addToOverlay(InfoStore.class, this.webModuleInfoStore);

        return this.webModuleInfoStore;
    }

    private static class FragmentAnnotationsImpl implements FragmentAnnotations {
        private final AnnotationTargets_Targets annoTargets;
        private final String classSourceName;

        FragmentAnnotationsImpl(AnnotationTargets_Targets annoTargets, String classSourceName) {
            this.annoTargets = annoTargets;
            this.classSourceName = classSourceName;
        }

        @Override
        public Set<String> selectAnnotatedClasses(Class<?> annotationClass) throws UnableToAdaptException {
            String annotationClassName = annotationClass.getName();

            // d95160: Not sure if SEED is the correct value to use here ...

            Set<String> selectedClassNames = this.annoTargets.getAnnotatedClasses(classSourceName,
                                                                                  annotationClassName,
                                                                                  AnnotationTargets_Targets.POLICY_SEED);
            return selectedClassNames;
        }
    }

    @Override
    public FragmentAnnotations getFragmentAnnotations(WebFragmentInfo webFragmentItem) throws UnableToAdaptException {
        return new FragmentAnnotationsImpl(getAnnotationTargets(), getClassSourceName(webFragmentItem));
    }

    private static class SpecificAnnotationsImpl implements SpecificAnnotations {
        private final AnnotationTargets_Targets specificTargets;

        SpecificAnnotationsImpl(AnnotationTargets_Targets specificTargets) {
            this.specificTargets = specificTargets;
        }

        @Override
        public Set<String> selectAnnotatedClasses(Class<?> annotationClass) throws UnableToAdaptException {
            String annotationClassName = annotationClass.getName();
            Set<String> selectedClassNames = this.specificTargets.getAnnotatedClasses(annotationClassName, AnnotationTargets_Targets.POLICY_SEED);
            return selectedClassNames;
        }
    }

    @Override
    public SpecificAnnotations getSpecificAnnotations(Set<String> specificClassNames) throws UnableToAdaptException {
        long startTime = reportScanStart();

        ClassSource_Aggregate useClassSource = getClassSource();

        AnnotationTargets_Factory useTargetsFactory = getAnnotationTargetsFactory();

        AnnotationTargets_Targets specificTargets;
        try {
            specificTargets = useTargetsFactory.createTargets();
        } catch (AnnotationTargets_Exception e) {
            String msg = Tr.formatMessage(tc, "failed.to.obtain.module.annotation.targets.CWWKM0473E",
                                          "Failed to obtain module annotation targets", e);
            throw new UnableToAdaptException(msg);
        }

        // Hook point for serialization: The scanner uses the class source as input, and transfers
        // class and annotations data to the annotation targets data set.
        //
        // An alternative would use the targets serialization, and instead read the targets data from
        // an input stream.

        try {
            specificTargets.scan(useClassSource, specificClassNames);
        } catch (AnnotationTargets_Exception e) {
            String msg = Tr.formatMessage(tc, "failed.to.obtain.module.annotation.targets.CWWKM0474E",
                                          "Failed to obtain module annotation targets", specificClassNames, e);
            throw new UnableToAdaptException(msg);
        }

        reportScanEnd(startTime, specificTargets);

        return new SpecificAnnotationsImpl(specificTargets);
    }

    //

    // TODO: Re-enable on a NLS pass.

    // Not able to get these to resolve:
    //
    // [INFO    ] Message not found in resource bundle (key=ATO_SCAN_STARTED, bundle=com.ibm.ws.anno.resources.internal.AnnoMessages, tc for com.ibm.ws.anno.adapter.AnnotationServicesImpl)
    //
    // For now, changed back to debug type messages.

    private long reportScanStart() {
        long startTime = getTimeInMillis();

        //        String useApplicationName = getApplicationName();
        //        String useModuleName = getModuleName();

        //        Tr.info(tc, "ATO_SCAN_STARTED", new Object[] { useApplicationName, useModuleName });

        return startTime;
    }

    private void reportScanEnd(long startTime, AnnotationTargets_Targets targetsTable) {
        //        long endTime = getTimeInMillis();
        //        long elapsedTime = endTime - startTime;
        //
        //        // Report header ...
        //
        //        String useApplicationName = getApplicationName();
        //        String useModuleName = getModuleName();
        //
        //        Tr.info(tc, "ATO_SCAN_COMPLETED",  new Object[] { useApplicationName, useModuleName, Long.valueOf(elapsedTime) });
        //
        //        // Base counts ...
        //
        //        int numScannedPackages = targetsTable.getScannedPackageNames().size();
        //        int numScannedClasses = targetsTable.getScannedClassNames().size();
        //
        //        Tr.info(tc,
        //                "ATO_SCAN_CLASS_COUNTS",
        //                new Object[] { useApplicationName, useModuleName,
        //                               Integer.valueOf(numScannedPackages),
        //                               Integer.valueOf(numScannedClasses) });
        //
        //        // Annotation counts ...
        //
        //        Util_BidirectionalMap packageAnnotationTable = targetsTable.getPackageAnnotationData();
        //        int numAnnotatedPackages = packageAnnotationTable.getHolderSet().size();
        //        int numPackageAnnotationTypes = packageAnnotationTable.getHeldSet().size();
        //
        //        Util_BidirectionalMap classAnnotationTable = targetsTable.getClassAnnotationData();
        //        int numAnnotatedClasses = classAnnotationTable.getHolderSet().size();
        //        int numClassAnnotationTypes = classAnnotationTable.getHeldSet().size();
        //
        //        Util_BidirectionalMap classFieldAnnotationTable = targetsTable.getFieldAnnotationData();
        //        int numFieldAnnotatedClasses = classFieldAnnotationTable.getHolderSet().size();
        //        int numFieldAnnotationTypes = classFieldAnnotationTable.getHeldSet().size();
        //
        //        Util_BidirectionalMap classMethodAnnotationTable = targetsTable.getMethodAnnotationData();
        //        int numMethodAnnotatedClasses = classMethodAnnotationTable.getHolderSet().size();
        //        int numMethodAnnotationTypes = classMethodAnnotationTable.getHeldSet().size();
        //
        //        Tr.info(tc,
        //                "ATO_SCAN_ANNOTATION_COUNTS",
        //                new Object[] { useApplicationName, useModuleName,
        //                               Integer.valueOf(numAnnotatedPackages),
        //                               Integer.valueOf(numAnnotatedClasses) });
        //
        //        if ( tc.isDebugEnabled() ) {
        //            Tr.debug(tc,
        //                     "Scan data [ {0} ] [ {1} ]: Number of package annotation types [ {2} ]",
        //                     new Object[] { useApplicationName, useModuleName, Integer.valueOf(numPackageAnnotationTypes) });
        //            Tr.debug(tc,
        //                     "Scan data [ {0} ] [ {1} ]: Number of class annotation types [ {2} ]",
        //                     new Object[] { useApplicationName, useModuleName, Integer.valueOf(numClassAnnotationTypes) });
        //            Tr.debug(tc,
        //                     "Scan data [ {0} ] [ {1} ]: Number of classes with field annotations [ {2} ]",
        //                     new Object[] { useApplicationName, useModuleName, Integer.valueOf(numFieldAnnotatedClasses) });
        //            Tr.debug(tc,
        //                     "Scan data [ {0} ] [ {1} ]: Number of unique field annotation types [ {2} ]",
        //                     new Object[] { useApplicationName, useModuleName, Integer.valueOf(numFieldAnnotationTypes) });
        //            Tr.debug(tc,
        //                     "Scan data [ {0} ] [ {1} ]: Number of classes with method annotations [ {2} ]",
        //                     new Object[] { useApplicationName, useModuleName, Integer.valueOf(numMethodAnnotatedClasses) });
        //            Tr.debug(tc,
        //                     "Scan data [ {0} ] [ {1} ]: Number of unique method annotation types [ {2} ]",
        //                     new Object[] { useApplicationName, useModuleName, Integer.valueOf(numMethodAnnotationTypes) });
        //
        //            // Name table sizes ...
        //
        //            int numUniqueClassNames = targetsTable.getClassInternMap().getSize();
        //            int totalClassNameSize = targetsTable.getClassInternMap().getTotalLength();
        //
        //            int numUniqueFieldNames = targetsTable.getFieldInternMap().getSize();
        //            int totalFieldNameSize = targetsTable.getFieldInternMap().getTotalLength();
        //
        //            int numUniqueMethodNames = targetsTable.getMethodInternMap().getSize();
        //            int totalMethodNameSize = targetsTable.getMethodInternMap().getTotalLength();
        //
        //            Tr.debug(tc,
        //                     "Scan data [ {0} ] [ {1} ]: Number package and class names [ {2} ]",
        //                     new Object[] { useApplicationName, useModuleName, Integer.valueOf(numUniqueClassNames) });
        //            Tr.debug(tc,
        //                     "Scan data [ {0} ] [ {1} ]: Total length of package and class names [ {2} ]",
        //                     new Object[] { useApplicationName, useModuleName, Integer.valueOf(totalClassNameSize) });
        //
        //            Tr.debug(tc,
        //                     "Scan data [ {0} ] [ {1} ]: Number of field names [ {2} ]",
        //                     new Object[] { useApplicationName, useModuleName, Integer.valueOf(numUniqueFieldNames) });
        //            Tr.debug(tc,
        //                     "Scan data [ {0} ] [ {1} ]: Total length of field names [ {2} ]",
        //                     new Object[] { useApplicationName, useModuleName, Integer.valueOf(totalFieldNameSize) });
        //
        //            Tr.debug(tc,
        //                     "Scan data [ {0} ] [ {1} ]: Number of method names [ {2} ]",
        //                     new Object[] { useApplicationName, useModuleName, Integer.valueOf(numUniqueMethodNames) });
        //            Tr.debug(tc,
        //                     "Scan data [ {0} ] [ {1} ]: Total length of method names [ {2} ]",
        //                     new Object[] { useApplicationName, useModuleName, Integer.valueOf(totalMethodNameSize) });
        //        }
    }

    private long getTimeInMillis() {
        return System.currentTimeMillis();
    }

    //

    /** {@inheritDoc} */
    @Override
    public boolean isIncludedClass(String className) throws UnableToAdaptException {
        return getAnnotationTargets().isSeedClassName(className);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isPartialClass(String className) throws UnableToAdaptException {
        return getAnnotationTargets().isPartialClassName(className);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExcludedClass(String className) throws UnableToAdaptException {
        return getAnnotationTargets().isExcludedClassName(className);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isExternalClass(String className) throws UnableToAdaptException {
        return getAnnotationTargets().isExternalClassName(className);
    }

    /** {@inheritDoc} */
    @Override
    public void openInfoStore() throws UnableToAdaptException {
        try {
            getInfoStore().open();
        } catch (InfoStoreException e) {
            String msg = Tr.formatMessage(tc, "failed.to.open.web.module.info.store.CWWKM0475E",
                                          "Failed to open web module info store", e);
            throw new UnableToAdaptException(msg);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void closeInfoStore() throws UnableToAdaptException {
        try {
            getInfoStore().close();
        } catch (InfoStoreException e) {
            String msg = Tr.formatMessage(tc, "failed.to.close.web.module.info.store.CWWKM0476E",
                                          "Failed to close web module info store", e);
            throw new UnableToAdaptException(msg);
        }
    }
}
