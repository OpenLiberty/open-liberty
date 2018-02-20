/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annotations.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.ContainerAnnotations;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.adaptable.module.adapters.ContainerAdapter;
import com.ibm.wsspi.anno.classsource.ClassSource;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.classsource.ClassSource_Exception;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Options;
import com.ibm.wsspi.anno.service.AnnotationService_Service;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
public class ContainerAnnotationsAdapter implements ContainerAdapter<ContainerAnnotations> {

    private static final TraceComponent tc = Tr.register(ContainerAnnotationsAdapter.class);

    private final AtomicServiceReference<AnnotationService_Service> annoServiceSRRef = new AtomicServiceReference<AnnotationService_Service>("annoService");

    //

    public void activate(ComponentContext context) {
        annoServiceSRRef.activate(context);
    }

    public void deactivate(ComponentContext context) {
        annoServiceSRRef.deactivate(context);
    }

    protected void setAnnoService(ServiceReference<AnnotationService_Service> ref) {
        annoServiceSRRef.setReference(ref);
    }

    protected void unsetAnnoService(ServiceReference<AnnotationService_Service> ref) {
        annoServiceSRRef.unsetReference(ref);
    }

    //

    @Override
    public ContainerAnnotations adapt(Container root,
                                      OverlayContainer rootOverlay,
                                      ArtifactContainer artifactContainer,
                                      Container containerToAdapt) throws UnableToAdaptException {

        AnnotationService_Service annotationService = annoServiceSRRef.getService();
        if (annotationService == null) {
            String msg = Tr.formatMessage(tc, "annotation.service.not.available.CWWKM0451E", "Annotation service not available", containerToAdapt);
            throw new UnableToAdaptException(msg);
        }

        return new ContainerAnnotationsImpl(root, rootOverlay, artifactContainer, containerToAdapt, annotationService);
    }

    private static final class ContainerAnnotationsImpl implements ContainerAnnotations {

        @SuppressWarnings("unused")
        private static final TraceComponent _tc = Tr.register(ContainerAnnotationsImpl.class);

        // Reference to the underlying annotation service.  That links to the
        // more detailed service entities.
        private final AnnotationService_Service annotationService;

        // Debugging values ... these three are related to the container which is being adapted.
        private final Container rootContainer;

        // The actual container which is being adapted.  This is the root
        // container of a target module.  Currently, the root container must
        // be for a web module.
        private final Container adaptableContainer;

        public ContainerAnnotationsImpl(Container root, OverlayContainer rootOverlay,
                                        ArtifactContainer artifactContainer, Container containerToAdapt,
                                        AnnotationService_Service annotationService) {
            this.rootContainer = root;
            this.adaptableContainer = containerToAdapt;
            this.annotationService = annotationService;
        }

        @Override
        public boolean hasSpecifiedAnnotations(List<String> annotationTypeNames, boolean useJandex) {
            try {
                ClassSource_Aggregate useClassSource = getClassSource(useJandex);
                AnnotationTargets_Targets useTargets = getAnnotationTargets();
                useTargets.scan(useClassSource);

                for (String annotationTypeName : annotationTypeNames) {
                    // d95160: The prior implementation obtained classes from the SEED location.
                    //         That implementation is not changed by d95160.

                    Set<String> classesWithAnno = useTargets.getAnnotatedClasses(annotationTypeName, AnnotationTargets_Targets.POLICY_SEED);
                    if (!classesWithAnno.isEmpty()) {
                        return true;
                    }
                }
            } catch (AnnotationTargets_Exception e) {
                e.getClass();
            }
            return false;
        }

        @Override
        public Set<String> getClassesWithSpecifiedInheritedAnnotations(List<String> annotationTypeNames, boolean useJandex) {
            Set<String> classesWithAnnotations = new HashSet<String>();
            try {
                ClassSource_Aggregate useClassSource = getClassSource(useJandex);
                AnnotationTargets_Targets useTargets = getAnnotationTargets();
                useTargets.scan(useClassSource);

                for (String annotationTypeName : annotationTypeNames) {
                    Set<String> classesWithAnno = useTargets.getAllInheritedAnnotatedClasses(annotationTypeName, AnnotationTargets_Targets.POLICY_SEED);
                    classesWithAnnotations.addAll(classesWithAnno);
                }
            } catch (AnnotationTargets_Exception e) {
                e.getClass();
            }
            return classesWithAnnotations;
        }

        private ClassSource_Aggregate getClassSource(boolean useJandex) {
            ClassSource_Aggregate useClassSource = null;
            try {
                String containerName = rootContainer.getName();

                ClassSource_Factory useClassSourceFactory = annotationService.getClassSourceFactory();

                ClassSource_Options options = useClassSourceFactory.createOptions();
                options.setUseJandex(useJandex);

                useClassSource = useClassSourceFactory.createAggregateClassSource(containerName, options);

                //Naming convention of "<containerName> + container" is proper, provided it is used consistently.
                String containerClassSourceName = containerName + " container";

                //Add the container from the module
                ClassSource containerClassSource = useClassSourceFactory.createContainerClassSource(useClassSource.getInternMap(),
                                                                                                    containerClassSourceName,
                                                                                                    this.adaptableContainer);
                useClassSource.addClassSource(containerClassSource, ClassSource_Aggregate.ScanPolicy.SEED);

            } catch (ClassSource_Exception e) {
                e.getClass();
            }
            return useClassSource;
        }

        private AnnotationTargets_Targets getAnnotationTargets() {
            AnnotationTargets_Targets useTargets = null;
            try {
                AnnotationTargets_Factory useTargetsFactory = annotationService.getAnnotationTargetsFactory();

                useTargets = useTargetsFactory.createTargets();
            } catch (AnnotationTargets_Exception e) {
                e.getClass();
            }
            return useTargets;
        }
    }
}
