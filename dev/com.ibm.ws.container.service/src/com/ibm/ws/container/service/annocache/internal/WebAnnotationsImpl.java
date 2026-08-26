/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annocache.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.ws.container.service.annocache.FragmentAnnotations;
import com.ibm.ws.container.service.annocache.WebAnnotations;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.ModuleClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.app.deploy.extended.ApplicationInfoForContainer;
import com.ibm.ws.container.service.app.deploy.extended.LibraryClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.extended.LibraryContainerInfo;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.container.service.config.WebFragmentsInfo;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.web.common.AbsoluteOrdering;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Targets;
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
public class WebAnnotationsImpl extends ModuleAnnotationsImpl implements WebAnnotations {
    
    public WebAnnotationsImpl(AnnotationsAdapterImpl annotationsAdapter,
                              Container rootContainer, OverlayContainer rootOverlayContainer,
                              ArtifactContainer rootArtifactContainer, Container rootAdaptableContainer,
                              WebModuleInfo webModuleInfo) throws UnableToAdaptException {

        super(annotationsAdapter,
              rootContainer, rootOverlayContainer,
              rootArtifactContainer, rootAdaptableContainer,
              webModuleInfo);

        this.webModuleName = webModuleInfo.getName();
        this.webFragments = rootAdaptableContainer.adapt(WebFragmentsInfo.class);
        // throws UnableToAdaptException

        this.fragmentToPath = new IdentityHashMap<WebFragmentInfo, String>();
        this.pathToFragments = new HashMap<String, WebFragmentInfo>();
    }

    protected WebAnnotationsImpl(WebAnnotationsImpl webAnnotations, String catName) throws UnableToAdaptException {
        super( webAnnotations.getAnnotationsAdapter(),
               webAnnotations.getRootContainer(),
               webAnnotations.getRootOverlayContainer(),
               webAnnotations.getRootDelegateContainer(),
               webAnnotations.getContainer(),
               webAnnotations.getModuleInfo(),
               catName);

        this.webModuleName = webAnnotations.webModuleName;
        this.webFragments = webAnnotations.webFragments;

        this.fragmentToPath = webAnnotations.fragmentToPath;
        this.pathToFragments = webAnnotations.pathToFragments;
    }

    //

    @Override
    public WebModuleInfo getModuleInfo() {
        return (WebModuleInfo) super.getModuleInfo();
    }
    
    //

    private final String webModuleName;

    @Override
    public String getWebModuleName() {
        return webModuleName;
    }

    //

    private final WebFragmentsInfo webFragments;

    @Override
    public WebFragmentsInfo getWebFragments() {
        return webFragments;
    }

    @Override
    public List<WebFragmentInfo> getOrderedItems() {
        return getWebFragments().getOrderedFragments();
    }

    @Override
    public List<WebFragmentInfo> getExcludedItems() {
        return getWebFragments().getExcludedFragments();
    }

    //

    @Override
    public FragmentAnnotations getFragmentAnnotations(WebFragmentInfo fragment) {
        AnnotationTargets_Targets useTargets = getTargets();
        if (useTargets == null) {
            return null;
        }
        return new FragmentAnnotationsImpl(useTargets, getFragmentPath(fragment));
    }

    private final Map<String, WebFragmentInfo> pathToFragments;
    private final Map<WebFragmentInfo, String> fragmentToPath;

    private String getFragmentPath(WebFragmentInfo fragment) {
        return fragmentToPath.get(fragment);
    }

    private String getUniquePath(String fragmentPath) {
        String uniquePath = fragmentPath;

        int count = 1;
        while (pathToFragments.containsKey(uniquePath)) {
            uniquePath = fragmentPath + "_" + count;
            count++;
        }

        return uniquePath;
    }

    private String putUniquePath(WebFragmentInfo fragment, String fragmentPath) {
        String uniqueFragmentPath = getUniquePath(fragmentPath);

        fragmentToPath.put(fragment, uniqueFragmentPath);
        pathToFragments.put(uniqueFragmentPath, fragment);

        return uniqueFragmentPath;
    }

    //
    
    protected final List<String> internalContainers = new ArrayList<>();
    protected final List<String> extraContainers = new ArrayList<>(0);

    protected void addInternalContainer(String path) {
        internalContainers.add(path);
    }
    
    public List<String> getInternalContainers() {
        return internalContainers;
    }

    protected void addExtraContainer(String path) {
        extraContainers.add(path);
    }
    
    public List<String> getExtraContainers() {
        return extraContainers;
    }
    
    @Override
    protected void addInternalToClassSource() {
        String methodName = "addInternalToClassSource";
        boolean isDebug = tc.isDebugEnabled();
        
        if (rootClassSource == null) {
            return;
        }

        ClassSource_Factory classSourceFactory = getClassSourceFactory();
        if (classSourceFactory == null) {
            return;
        }

        // The classes folder is processed as if it were a fragment item.

        // Web module internal class path locations are categorized as either:
        //  'SEED': Non-metadata-complete, non-excluded
        //  'PARTIAL': Metadata-complete, non-excluded
        //  'EXCLUDED': Excluded
        //
        // Where 'excluded' means excluded by an absolute ordering element
        // of the web module deployment descriptor.  When an absolute ordering
        // element is present in the descriptor, if the element does not contain
        // an 'others' element, any fragment not listed in the element is an
        // excluded element.  Less class information is used from excluded
        // fragments than is used from partial fragments.

        for (WebFragmentInfo nextFragment : getOrderedItems()) {
            String nextUri = nextFragment.getLibraryURI();
            Container nextContainer = nextFragment.getFragmentContainer();

            boolean nextIsMetadataComplete;
            ScanPolicy nextPolicy;
            if (nextFragment.isSeedFragment()) {
                nextPolicy = ClassSource_Aggregate.ScanPolicy.SEED;
                nextIsMetadataComplete = false;
            } else {
                nextPolicy = ClassSource_Aggregate.ScanPolicy.PARTIAL;
                nextIsMetadataComplete = true;
            }

            if (isDebug) {
                Tr.debug(tc, methodName + ": Fragment [ " + nextFragment + " ]");
                Tr.debug(tc, methodName + ": URI [ " + nextUri + " ]");
                Tr.debug(tc, methodName + ": Container [ " + nextContainer + " ]");
                Tr.debug(tc, methodName + ": Metadata Complete [ " + nextIsMetadataComplete + " ]");
            }

            String nextPrefix;
            if (nextUri.equals("WEB-INF/classes")) {
                // The expectation is that the supplied container is twice nested
                // local child of the module container.
                nextContainer = nextContainer.getEnclosingContainer().getEnclosingContainer();
                nextPrefix = "WEB-INF/classes/";
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + ": Assigned Prefix [ " + nextPrefix + " ]");
                }
            } else {
                nextPrefix = null;
            }

            String nextPath = getContainerPath(nextContainer);
            if (nextPath == null) {
                return; // FFDC in 'getContainerPath'
            }
            nextPath = putUniquePath(nextFragment, nextPath);
            if (isDebug) {
                Tr.debug(tc, methodName + ": Fragment [ " + nextFragment + " ]");
                Tr.debug(tc, methodName + ": Path [ " + nextPath + " ]");
            }

            if ( !addContainerClassSource(nextPath, nextContainer, nextPrefix, nextPolicy) ) {
                return; // FFDC in 'addContainerClassSource'
            } else {
                addInternalContainer(nextPath);
            }
        }

        for (WebFragmentInfo nextFragment : getExcludedItems()) {
            String nextUri = nextFragment.getLibraryURI();
            Container nextContainer = nextFragment.getFragmentContainer();

            if (isDebug) {
                Tr.debug(tc, methodName + ": Fragment [ " + nextFragment + " ]");
                Tr.debug(tc, methodName + ": URI [ " + nextUri + " ]");
                Tr.debug(tc, methodName + ": Container [ " + nextContainer + " ]");
                Tr.debug(tc, methodName + ": Excluded [ true ]");
            }

            String nextPath = getContainerPath(nextContainer);
            if (nextPath == null) {
                return; // FFDC in 'getContainerPath'
            }
            nextPath = putUniquePath(nextFragment, nextPath);

            if (isDebug) {
                Tr.debug(tc, methodName + ": Fragment [ " + nextFragment + " ]");
                Tr.debug(tc, methodName + ": Path [ " + nextPath + " ]");
            }

            if ( !addContainerClassSource(nextPath, nextContainer, ClassSource_Aggregate.ScanPolicy.EXCLUDED) ) {
                return; // FFDC in 'addContainerClassSource'
            } else {
                addInternalContainer(nextPath);
            }
        }

        // TODO: The policy assignment is debatable.  There are two key decisions: What to do
        //       when the web module is metadata complete, and what to do when the web module
        //       has an absolute ordering which has no 'others' element.
        //
        //       This implementation treats extra libraries as if they were fragments which
        //       have no descriptor.  That means, the extra libraries are completely ignored
        //       if 'others' exclusion is in effect, and, that means that the extra libraries
        //       scan policy matches the metadata complete setting.

        // TODO: The path computation doesn't work for shared libraries.  Those will usually
        //       be root of roots containers.
        //
        // Manifest jars and enterprise application library jars are always with an
        // enterprise application.  Their path must be unique relative to the enclosing
        // application.  No unique path adjustment is necessary.
        
        // TODO: The extra jars are not actually present as fragments.  Will that create problems?
        //       Scanning that asks for fragment information might have problems.

        if ( getExtendScans() && !getHasExcludes() ) {
            ClassSource_Aggregate.ScanPolicy extraPolicy =
                ( getIsMetadataComplete() ? ClassSource_Aggregate.ScanPolicy.PARTIAL
                                          : ClassSource_Aggregate.ScanPolicy.SEED );

            for ( Container extraLibContainer : getApplicationExtendedContainers() ) {
                String nextPath;
                try {
                    nextPath = getPath(extraLibContainer);
                } catch ( UnableToAdaptException e ) {
                    return; // FFDC
                }

                if (isDebug) {
                    Tr.debug(tc, methodName + ": Extra container [ " + nextPath + " ]");
                }
                
                if ( !addContainerClassSource( nextPath, extraLibContainer, extraPolicy ) ) {
                    return; // FFDC in 'addContainerClassSource'
                } else {
                    addExtraContainer(nextPath);
                }
            }
        }
    }

    /**
     * Tell if a web module descriptor schema version supports metadata-complete.
     * 
     * Metadata-complete is supported at version 2.5 or higher.
     * 
     * Perform limited validation of the version as a dotted string.  Answer
     * false in cases where the value processed.
     * 
     * @param schemaVersion A schema version, as a dotted string.
     * 
     * @return True or false telling if the schema version supports metadata complete.
     */
    private static boolean isMetadataCompleteSupported(String schemaVersion) {
        int vLen = schemaVersion.length();
        if ( vLen == 0 ) {
            return false; // '' broken
        }

        char c0 = schemaVersion.charAt(0);        
        if ( !Character.isDigit(c0) ) {
            return false; // 'c*' broken
        } else if ( c0 >= '3' ) {
            return true;  // '3*'
        } else if ( c0 == '0') {
            return false; // '0*' broken
        }

        if ( vLen == 1 ) {
            return false; // '1', or '2'
        } else if ( schemaVersion.charAt(1) != '.' ) {
            return true; // '1X*'
        } else if ( (c0 == '1') || (vLen == 2) ) {
            return false; // '1.*', or '2.'
        }

        char c2 = schemaVersion.charAt(2);
        return ( Character.isDigit(c2) && (c2 >= '5')); // '2.5*'
    }

    protected boolean isSetExcludes;
    protected boolean isMetadataComplete;
    protected boolean hasExcludes;

    /**
     * Determine if the web application descriptor is metadata complete,
     * and if not, if there any excluded fragments.
     * 
     * If there is no descriptor, set metadata complete to false and set that
     * there are no excludes.
     * 
     * If there is a descriptor, set metadata complete if the schema version doesn't
     * support metadata complete, or if metadata complete is explicitly set on the
     * descriptor.
     * 
     * Set excludes to false if metadata complete is true.  That includes both the
     * case of the schema version being too low and of metadata complete being
     * explicitly set to true.
     * 
     * If metadata complete is false, set excludes according to whether the descriptor
     * has an absolute ordering element and that does not have an others element.
     */
    protected void setExcludes() {
        String methodName = "setExcludes";
        
        if ( isSetExcludes ) {
            return;
        }

        String mcReason;
        String exReason;

        WebApp webApp = adapt( getContainer(), WebApp.class );

        if ( webApp == null ) {
            isMetadataComplete = false;
            hasExcludes = false;
            mcReason = "No WebApp";
            exReason = "No WebApp";

        } else {
            String schemaVersion = webApp.getVersion();
            if ( isMetadataCompleteSupported(schemaVersion) ) {
                isMetadataComplete = webApp.isSetMetadataComplete() && webApp.isMetadataComplete();
                mcReason = "WebApp";
            } else {
                isMetadataComplete = true;
                mcReason = "WebApp Version";
            }

            if ( !isMetadataComplete ) {
                AbsoluteOrdering absOrder = webApp.getAbsoluteOrdering();
                hasExcludes = ( (absOrder != null) && !absOrder.isSetOthers() );
                exReason = "WebApp";
            } else {
                hasExcludes = false;
                exReason = "isMetadataComplete";
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + ": isMetadataComplete [ " + isMetadataComplete + " ]: (" + mcReason + ")");
            Tr.debug(tc, methodName + ": hasExcludes [ " + hasExcludes + " ]: (" + exReason + ")");            
        }                

        isSetExcludes = true;
    }

    /**
     * Tell if the web application is metadata complete.  See the discussion on {@link #setExcludes()}.
     * 
     * @return True or false telling if the web application is metadata complete.
     */
    protected boolean getIsMetadataComplete() {
        setExcludes();
        return isMetadataComplete;
    }

    /**
     * Tell if there is a request to excluded fragments.  That is, when there is
     * a non-metadata complete descriptor that has an absolute ordering element
     * which  does not have an others element.
     * 
     * @return True or false telling there is a request to exclude fragments.
     */
    protected boolean getHasExcludes() {
        setExcludes();
        return hasExcludes;
    }

    /**
     * Tell if any extended scans are enabled.
     * 
     * @return True or false telling if any extended scans are enabled.
     */
    protected boolean getExtendScans() {
        Set<EnterpriseApplicationLibraryType> appScanOptions = getAppScanOptions();

        boolean scanManifestJars = appScanOptions.contains(EnterpriseApplicationLibraryType.MANIFEST_LIB);
        boolean scanEarLibJars = appScanOptions.contains(EnterpriseApplicationLibraryType.EAR_LIB);
        boolean scanSharedLibs = false; // Shared libs are not supported

        return ( scanManifestJars || scanEarLibJars || scanSharedLibs );
    }

    /**
     * Gather all extended containers of the associated web application.
     * 
     * These are the containers of manifest class path jars, containers of application
     * library jars, and containers of shared libraries.
     * 
     * Add each category only if enabled.  (See {@link #getScanOptions()}.
     * 
     * Add the containers in 
     * 
     * @param appScanOptions Application scanning options.
     * @param appCache The application's non-persistent cache.
     * 
     * @return The collection of extended containers of the web application.
     */
    @SuppressWarnings("null")
    private List<Container> getApplicationExtendedContainers() {
        ApplicationClassesContainerInfo appClassesInfo = getAppClassesContainerInfo();
        if ( appClassesInfo == null ) {
            return Collections.emptyList();
        }

        Set<EnterpriseApplicationLibraryType> appScanOptions = getAppScanOptions();

        boolean scanManifestJars = appScanOptions.contains(EnterpriseApplicationLibraryType.MANIFEST_LIB);
        boolean scanEarLibJars = appScanOptions.contains(EnterpriseApplicationLibraryType.EAR_LIB);
        boolean scanSharedLibs = false; // Shared libs are not supported

        List<Container> manifestContainers = ( scanManifestJars ? new LinkedList<>() : null );
        List<Container> earLibContainers = ( scanEarLibJars ? new LinkedList<>() : null );
        List<Container> sharedLibContainers = (scanSharedLibs ? new LinkedList<>() : null );

        if ( scanManifestJars ) {
            for ( ModuleClassesContainerInfo moduleClassesInfo : appClassesInfo.getModuleClassesContainerInfo() ) {
                for ( ContainerInfo containerInfo : moduleClassesInfo.getClassesContainerInfo() ) {
                    if ( containerInfo.getType() != ContainerInfo.Type.MANIFEST_CLASSPATH ) {
                        continue;
                    }
                     
                    Container container = containerInfo.getContainer();
                    if ( container != null ) {
                        manifestContainers.add(container);
                    }
                }
            }
        }

        if ( scanEarLibJars || scanSharedLibs ) {
            for ( ContainerInfo containerInfo : appClassesInfo.getLibraryClassesContainerInfo() ) {
                if ( scanEarLibJars && (containerInfo.getType() == ContainerInfo.Type.EAR_LIB) ) {
                    Container container = containerInfo.getContainer();
                    if ( container != null ) {
                        earLibContainers.add(container);
                    }
                } 

                // TODO: This step is currently disabled: 'scanSharedLibs' is hard coded to be false.
                
                // Note the extra layer of iteration for library classes: Libraries are collections.

                if ( scanSharedLibs && (containerInfo instanceof LibraryClassesContainerInfo) ) {
                    LibraryClassesContainerInfo libContainerInfo = (LibraryClassesContainerInfo) containerInfo;
                    if ( libContainerInfo.getLibraryType() == LibraryContainerInfo.LibraryType.COMMON_LIB ) { // TODO, do we need this check?
                        for ( ContainerInfo classesContainerInfo : libContainerInfo.getClassesContainerInfo() ) {
                            Container container = classesContainerInfo.getContainer();
                            if ( container != null ) {
                                sharedLibContainers.add(container);
                            }
                        }
                    }
                }
            }
        }

        // Put manifest jars ahead of EAR lib jars, ahead of shared library jars.
        //
        // TODO: Verify that this matches the ordering used by the application class loader.
        //       Placing shared libraries last should be correct.  Not sure about the others.

        List<Container> appLibraryContainers = new LinkedList<Container>();
        if ( scanManifestJars ) {
            appLibraryContainers.addAll(manifestContainers);
        }
        if ( scanEarLibJars ) {
            appLibraryContainers.addAll(earLibContainers);
        }
        if ( scanSharedLibs ) {
            appLibraryContainers.addAll(sharedLibContainers);
        }
        return appLibraryContainers ;
    }

    //
    
    private static class WebEJBAnnotationsLock {
        protected WebEJBAnnotationsLock() {
            // EMPTY
        }
    }

    private final WebEJBAnnotationsLock webEJBAnnotationsLock =
        new WebEJBAnnotationsLock();

    private volatile WebAnnotationsImpl webEJBAnnotations;

    @Override
    public WebAnnotationsImpl asEJBAnnotations() throws UnableToAdaptException {
        if ( webEJBAnnotations == null ) {
            synchronized ( webEJBAnnotationsLock ) {
                if ( getAppScanOptions().isEmpty() ) {
                    webEJBAnnotations = this;
                } else {
                    webEJBAnnotations = new WebEJBAnnotationsImpl(this); // throws UnableToAdaptException
                }
            }
        }
        return webEJBAnnotations;
    }    
}
