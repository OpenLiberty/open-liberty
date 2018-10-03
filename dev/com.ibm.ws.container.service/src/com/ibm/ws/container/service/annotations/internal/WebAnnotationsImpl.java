/*******************************************************************************
  * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annotations.internal;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.annotations.FragmentAnnotations;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.container.service.config.WebFragmentInfo;
import com.ibm.ws.container.service.config.WebFragmentsInfo;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate.ScanPolicy;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
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
public class WebAnnotationsImpl extends ModuleAnnotationsImpl implements WebAnnotations {

    public WebAnnotationsImpl(
        AnnotationsAdapterImpl annotationsAdapter,
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

    private Map<WebFragmentInfo, String> fragmentPaths;

    private void putFragmentPath(WebFragmentInfo fragment, String path) {
        if ( fragmentPaths == null ) {
            fragmentPaths = new IdentityHashMap<WebFragmentInfo, String>();
        }
        fragmentPaths.put(fragment, path);
    }

    private String getFragmentPath(WebFragmentInfo fragment) {
        if ( fragmentPaths == null ) {
            return null;
        }
        return fragmentPaths.get(fragment);
    }

    //

    @Override
    protected void addInternalToClassSource() {
        if ( rootClassSource == null ) {
            return;
        }

        ClassSource_Factory classSourceFactory = getClassSourceFactory();
        if ( classSourceFactory == null ) {
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

        for ( WebFragmentInfo nextFragment : getOrderedItems() ) {
            String nextUri = nextFragment.getLibraryURI();
            Container nextContainer = nextFragment.getFragmentContainer();

            ScanPolicy nextPolicy;
            if ( nextFragment.isSeedFragment() ) {
                nextPolicy = ClassSource_Aggregate.ScanPolicy.SEED;
            } else {
                nextPolicy = ClassSource_Aggregate.ScanPolicy.PARTIAL;
            }

            String nextPrefix;
            if ( nextUri.equals("WEB-INF/classes") ) {
                // The expectation is that the supplied container is twice nested
                // local child of the module container.
                nextContainer = nextContainer.getEnclosingContainer().getEnclosingContainer();
                nextPrefix = "WEB-INF/classes/";
            } else {
                nextPrefix = null;
            }

            String nextPath = getContainerPath(nextContainer);
            if ( nextPath == null ) {
                return; // FFDC in 'getContainerPath'
            }

            if ( !addContainerClassSource(nextPath, nextContainer, nextPrefix, nextPolicy) ) {
                return; // FFDC in 'addContainerClassSource'
            }

            putFragmentPath(nextFragment, nextPath);
        }

        for ( WebFragmentInfo nextFragment : getExcludedItems() ) {
            Container nextContainer = nextFragment.getFragmentContainer();

            String nextPath = getContainerPath(nextContainer);
            if ( nextPath == null ) {
                return; // FFDC in 'getContainerPath'
            }

            if ( !addContainerClassSource(nextPath, nextContainer, ClassSource_Aggregate.ScanPolicy.EXCLUDED) ) {
                return; // FFDC in 'addContainerClassSource'
            }

            putFragmentPath(nextFragment, nextPath);
        }
    }

    //

    @Override
    public FragmentAnnotations getFragmentAnnotations(WebFragmentInfo fragment) {
        AnnotationTargets_Targets useTargets = getTargets();
        if ( useTargets == null ) {
            return null;
        }
        return new FragmentAnnotationsImpl( useTargets, getFragmentPath(fragment) );
    }
}
