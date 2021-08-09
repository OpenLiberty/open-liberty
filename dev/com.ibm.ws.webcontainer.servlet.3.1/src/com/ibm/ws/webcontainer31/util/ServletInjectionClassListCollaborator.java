/*******************************************************************************
 * Copyright (c) 2015,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer31.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.webcontainer31.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.InfoStoreException;
import com.ibm.wsspi.anno.info.ClassInfo;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInjectionClassListCollaborator;
import com.ibm.wsspi.webcontainer.filter.IFilterConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;
/**
 * Helper class which is responsible for gathering the list of active servlet
 * classes on which CDI injection is performed.
 *
 * Three types of classes are targets of CDI injection: Servlets, Filters, and Listeners.
 */
@Component(name = "com.ibm.ws.webcontainer31.util.ServletInjectionClassListCollaborator",
           service = WebAppInjectionClassListCollaborator.class,
           immediate = true,
           property = { "service.vendor=IBM"})
public class ServletInjectionClassListCollaborator implements WebAppInjectionClassListCollaborator {
    private final static TraceComponent tc =
        Tr.register(ServletInjectionClassListCollaborator.class,
                    WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    private static final String CLASS_NAME =
        ServletInjectionClassListCollaborator.class.getName();

    // Abstract classes in Servlet 3.1 for which extenders require support for CDI 1.2.

    private final String[] SERVLET_CLASS_NAMES =
        new String[] { "javax.servlet.http.HttpServlet" };

    // Interfaces in Servlet 3.1 for which implementors require support for CDI 1.2.

    private final String[] FILTER_CLASS_NAMES =
        new String[] { "javax.servlet.Filter" };

    private final String[] LISTENER_CLASS_NAMES =
        new String[] { "javax.servlet.ServletContextListener",
                       "javax.servlet.ServletContextAttributeListener",
                       "javax.servlet.ServletRequestListener",
                       "javax.servlet.ServletRequestAttributeListener",
                       "javax.servlet.http.HttpUpgradeHandler",
                       "javax.servlet.http.HttpSessionListener",
                       "javax.servlet.http.HttpSessionAttributeListener",
                       "javax.servlet.http.HttpSessionIdListener",
                       "javax.servlet.AsyncListener" };

    /**
     * Widget used to delay opening the info store as long as possible.
     */
    private static class DeferredInfoStore {
        private final WebAnnotations webAnnotations;

        private boolean didInit;
        private InfoStore infoStore;

        public DeferredInfoStore(WebAnnotations webAnnotations) {
            this.webAnnotations = webAnnotations;

            this.didInit = false;
            this.infoStore = null;
        }

        public InfoStore get() {
            if ( !didInit ) {
                didInit = true;

                try {
                    infoStore = webAnnotations.getInfoStore(); // throws UnableToAdaptException
                } catch ( UnableToAdaptException e ) {
                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                        Tr.warning(tc, CLASS_NAME, "Failed to obtain web info store", e);
                    }
                    infoStore = null;
                }

                if ( infoStore != null ) {
                    try {
                        infoStore.open();
                    } catch ( InfoStoreException e ) {
                        infoStore = null;
                        Tr.warning(tc, CLASS_NAME, "Failed to open web info store", e);
                    }
                }
            }

            return infoStore;
        }

        public void clear() {
            if ( !didInit ) {
                return;
            }

            didInit = false;

            if ( infoStore != null ) {
                InfoStore useInfoStore = infoStore;
                infoStore = null;

                try {
                    useInfoStore.close();
                } catch ( InfoStoreException e ) {
                    Tr.warning(tc, CLASS_NAME, "Failed to close web info store", e);
                }
            }
        }
    }

    /**
     * Answer the JavaEE classes of a web module on which CDI injection is performed.
     * These are the servlet, filter, and listener classes of the web module.
     *
     * The returned class names are in no particular order.
     *
     * An empty list is returned if web configuration data is not available
     * for the container, or if annotations could not be obtained for the container.
     *
     * @param moduleContainer The container of the web module.
     *
     * @return The names of classes on which CDI injection is performed.
     */
    public List<String> getInjectionClasses(Container moduleContainer) {
        String methodName = "getInjectionClasses";

        List<String> injectionClassNames = new ArrayList<String>();

        // A web module always has a WebAppConfig.  If the descriptor
        // is present, the configuration is read from the descriptor.
        // Otherwise, an empty configuration is created.

        WebAppConfig webDD;
        try {
            webDD = moduleContainer.adapt(WebAppConfig.class);
        } catch ( UnableToAdaptException e ) {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.warning(tc, CLASS_NAME, "Failed to obtain WebAppConfig", e);
            }
            return injectionClassNames;
        }
        if ( webDD == null ) {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled() ) {
                Tr.warning(tc, methodName, "WebAppConfig was null");
            }
            return injectionClassNames;
        }

        // Do not obtain the annotation targets if the web module is metadata-complete.
        // Instead, use the info store and test the candidate classes directly.

        AnnotationTargets_Targets annotationTargets = null;
        DeferredInfoStore deferredInfoStore = null;

        try {
            WebAnnotations webAnnotations = AnnotationsBetaHelper.getWebAnnotations(moduleContainer); // throws UnableToAdaptException
            if ( !webDD.isMetadataComplete() ) {
                annotationTargets = webAnnotations.getAnnotationTargets(); // throws UnableToAdaptException
            } else {
                deferredInfoStore = new DeferredInfoStore(webAnnotations);
            }
        } catch ( UnableToAdaptException e ) {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.warning(tc, CLASS_NAME, "Failed to obtain web annotations", e);
            }
        }

        try {
            // Servlet ...

            Collection<String> servletClassNames = new ArrayList<String>();

            // Start with servlets explicitly listed in the descriptor.
            Iterator<IServletConfig> servletsDD = webDD.getServletInfos();
            while ( servletsDD.hasNext() ) {
                IServletConfig servletDD = servletsDD.next();
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "Servlet in web.xml: " + servletDD);
                }

                String servletClassName = servletDD.getClassName();
                if ( servletClassName != null ) {
                    servletClassNames.add(servletClassName);
                }
            }

            // Add classes annotated with @WebServlet.  Ignore duplicates.
            if ( annotationTargets != null ) {
                servletClassNames.addAll( getAnnotatedClasses(annotationTargets, WebServlet.class.getName(), servletClassNames) );
            }

            // Inject only on servlet classes which extend HttpServlet.
            if ( !servletClassNames.isEmpty() ) {
                if ( annotationTargets != null ) {
                    servletClassNames = selectValid(annotationTargets, SERVLET_CLASS_NAMES, IS_ABSTRACT_CLASS, servletClassNames);
                } else if ( deferredInfoStore != null ) {
                    InfoStore infoStore = deferredInfoStore.get();
                    if ( infoStore != null ) {
                        servletClassNames = selectValid(infoStore, SERVLET_CLASS_NAMES, IS_ABSTRACT_CLASS, servletClassNames);
                    }
                } else {
                    // Can't validate the servlet class names!
                }
                injectionClassNames.addAll(servletClassNames);
            }

            // Filter ...

            Collection<String> filterClassNames = new ArrayList<String>();

            // Start with filters explicitly listed in the descriptor.
            Iterator<IFilterConfig> filtersDD = webDD.getFilterInfos();
            while ( filtersDD.hasNext() ) {
                IFilterConfig filterDD = filtersDD.next();
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "Filter in web.xml: " + filterDD);
                }

                String filterClassName = filterDD.getClassName();
                if ( filterClassName != null ) {
                    filterClassNames.add(filterClassName);
                }
            }

            // Add classes annotated with @WebFilter.  Ignore duplicates.
            if ( annotationTargets != null ) {
                filterClassNames.addAll( getAnnotatedClasses(annotationTargets, WebFilter.class.getName(), filterClassNames) );
            }

            // Inject only filter classes which extend Filter.
            if ( !filterClassNames.isEmpty() ) {
                if ( annotationTargets != null ) {
                    filterClassNames = selectValid(annotationTargets, FILTER_CLASS_NAMES, IS_INTERFACE, filterClassNames);
                } else if ( deferredInfoStore != null ) {
                    InfoStore infoStore = deferredInfoStore.get();
                    if ( infoStore != null ) {
                        filterClassNames = selectValid(infoStore, FILTER_CLASS_NAMES, IS_INTERFACE, filterClassNames);
                    }
                } else {
                    // Can't validate the filter class names!
                }
                injectionClassNames.addAll(filterClassNames);
            }

            // Listener ...

            Collection<String> listenerClassNames = new ArrayList<String>();

            // Start with listeners explicitly listed in the descriptor.
            for ( Object listenerClassName : webDD.getListeners() ) {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "Listener in web.xml: " + listenerClassName);
                }
                listenerClassNames.add( (String) listenerClassName );
            }

            // Add classes annotated with @WebListener.
            if ( annotationTargets != null ) {
                listenerClassNames.addAll( getAnnotatedClasses(annotationTargets, WebListener.class.getName(), listenerClassNames) );
            }

            // Inject only listener classes which implement one of the required listener types.
            if ( !listenerClassNames.isEmpty() ) {
                if ( annotationTargets != null ) {
                    listenerClassNames = selectValid(annotationTargets, LISTENER_CLASS_NAMES, IS_INTERFACE, listenerClassNames);
                } else if ( deferredInfoStore != null ) {
                    InfoStore infoStore = deferredInfoStore.get();
                    if ( infoStore != null ) {
                        listenerClassNames = selectValid(infoStore, LISTENER_CLASS_NAMES, IS_INTERFACE, listenerClassNames);
                    }
                } else {
                    // Can't validate the listener class names!
                }
                injectionClassNames.addAll(listenerClassNames);
            }

        } finally {
            if ( deferredInfoStore != null ) {
                deferredInfoStore.clear();
            }
        }

        if ( TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled() ) {
            Tr.exit(tc, methodName);
        }
        return injectionClassNames;
    }

    /**
     * Select classes which have a specified class annotation.
     *
     * Select only classes in non-metadata complete and non-excluded regions.
     *
     * @param annotationTargets Database of annotations targets data.
     * @param annotationClassName The name of the target class annotation.
     *
     * @param knownClasses The names of classes which are to be ignored when
     *     selecteding classes.
     *
     * @return The names of classes which have the specified class annotation.
     */
    private Collection<String> getAnnotatedClasses(
        AnnotationTargets_Targets annotationTargets, String annotationClassName,
        Collection<String> knownClassNames) {

        Collection<String> annotatedClassNames = new ArrayList<String>();

        for ( String annotatedClassName : annotationTargets.getAnnotatedClasses(annotationClassName) ) {
            if ( knownClassNames.contains(annotatedClassName) ) {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "Descriptor class " + annotatedClassName + " is redundantly annotated with " + annotationClassName);
                }
            } else {
                annotatedClassNames.add(annotatedClassName);
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "Class " + annotatedClassName + " is annotated with " + annotationClassName);
                }
            }
        }

        return annotatedClassNames;
    }

    private static final boolean IS_INTERFACE = true;
    private static final boolean IS_ABSTRACT_CLASS = false;

    /**
     * Select from candidate class names those which, according to the
     * control parameter, either implement one of several specified
     * class, or extend on of the several specified classes.
     *
     * @param annotationTargets The annotation targets data which is used
     *    to test the class names.
     * @param requiredClassNames Required interface or superclass names.
     * @param isInterface Control parameter: Are the required class names
     *     the names of interfaces or of abstract classes.
     * @param candidateClassNames The class names from which to select
     *    implementing or extending class names.
     */
    private Collection<String> selectValid(
        AnnotationTargets_Targets annotationTargets,
        String[] requiredClassNames, boolean isInterface,
        Collection<String> candidateClassNames) {

        // Expect all of the candidates to be valid.
        Set<String> selectedClassNames = new HashSet<String>( candidateClassNames.size() );

        // For each of the required class names ...

        for ( String requiredClassName : requiredClassNames ) {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                if ( isInterface ) {
                    Tr.debug(tc, "Searching for implementors of " + requiredClassName);
                } else {
                    Tr.debug(tc, "Searching for extenders of " + requiredClassName);
                }
            }

            // Find the classes which satisfy (implement or extend) the required class.

            Set<String> validClassNames;
            if ( isInterface ) {
                validClassNames = annotationTargets.getAllImplementorsOf(requiredClassName);
            } else {
                validClassNames = annotationTargets.getSubclassNames(requiredClassName);
            }

            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                for ( String validClassName : validClassNames ) {
                    if ( isInterface ) {
                        Tr.debug(tc, "Found implementor of " + requiredClassName + " : " + validClassName);
                    } else {
                        Tr.debug(tc, "Found extender of " + requiredClassName + " : " + validClassName);
                    }
                }
            }

            // Then select the candidate class names which satisfy the required class.

            // Duplicates are unexpected: A candidate should implement or subclass at most
            // one of the required classes.  If a duplication occurs, it will be ignored
            // because the selected classes are stored as a set.

            for ( String candidateClassName : candidateClassNames ) {
                if ( !validClassNames.contains(candidateClassName) ) {
                    continue;
                }

                selectedClassNames.add(candidateClassName);

                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    if ( isInterface ) {
                        Tr.debug(tc, "Found valid implementor of " + requiredClassName + " : " + candidateClassName);
                    } else {
                        Tr.debug(tc, "Found valid extender of " + requiredClassName + " : " + candidateClassName);
                    }
                }
            }
        }

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            if ( selectedClassNames.size() != candidateClassNames.size() ) {
                for ( String candidateClassName : candidateClassNames ) {
                    if ( !selectedClassNames.contains(candidateClassName) ) {
                        if ( isInterface ) {
                            Tr.debug(tc, "Candidate " + candidateClassName + " does not implement any of the required interfaces");
                        } else {
                            Tr.debug(tc, "Candidate " + candidateClassName + " does not extend any of the required interfaces");
                        }
                    }
                }
            }
        }

        return selectedClassNames;
    }

    private Collection<String> selectValid(
        InfoStore infoStore,
        String[] requiredClassNames, boolean isInterface,
        Collection<String> candidateClassNames) {

        // Expect all of the candidates to be valid.
        Set<String> selectedClassNames = new HashSet<String>( candidateClassNames.size() );

        for ( String candidateClassName : candidateClassNames ) {
            ClassInfo classInfo = infoStore.getDelayableClassInfo(candidateClassName);
            if ( classInfo == null ) {
                Tr.warning(tc, "Failed to load web module injection target class " + candidateClassName);
                continue;
            }

            boolean didAdd = false;
            for ( String requiredClassName : requiredClassNames ) {
                if ( classInfo.isInstanceOf(requiredClassName) ) {
                    selectedClassNames.add(candidateClassName);
                    didAdd = true;
                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                        Tr.debug(tc, "Matched  " + requiredClassName + " against " + requiredClassName);
                    }
                    break;
                } else {
                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                        Tr.debug(tc, "No match of   " + requiredClassName + " against " + requiredClassName);
                    }
                }
            }
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                if ( !didAdd ) {
                    Tr.debug(tc, "Candidate " + candidateClassName + " matched none of the required classes");
                }
            }
        }
        return selectedClassNames;
    }
}
