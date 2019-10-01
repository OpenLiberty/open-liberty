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
package com.ibm.ws.wsoc.cdi.weld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.annocache.AnnotationsBetaHelper;
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInjectionClassListCollaborator;

public class WebSocketInjectionClassListCollaborator  implements WebAppInjectionClassListCollaborator {
    private static final TraceComponent tc = Tr.register(WebSocketInjectionClassListCollaborator.class);

    //

    // Abstract WebSocket classes.  Extenders of these require support for CDI 1.2.
    private static final String[] INJECTION_SUPER_CLASSES =
        new String[] { "javax.websocket.Endpoint" };

    @Override
    @FFDCIgnore(UnableToAdaptException.class)
    public List<String> getInjectionClasses(Container moduleContainer) {
        String methodName = "getInjectionClasses";

        List<String> injectionClassNames = new ArrayList<String>();

        try {
            WebAnnotations webAnno = AnnotationsBetaHelper.getWebAnnotations(moduleContainer);
            AnnotationTargets_Targets annoTargets = webAnno.getAnnotationTargets();

            // Find POJOs that have been annotated to be end points.
            // The annotation is not inherited: Look for immediate targets, not for inherited targets.
            Set<String> endpointClassNames =
                annoTargets.getAnnotatedClasses("javax.websocket.server.ServerEndpoint");
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "ServerEndpoint annotated classes: " + endpointClassNames);
            }
            injectionClassNames.addAll(endpointClassNames);

            for ( String injectionSuperClass : INJECTION_SUPER_CLASSES ) {
                Set<String> implementorClassNames = annoTargets.getSubclassNames(injectionSuperClass);
                for ( String implementorClassName : implementorClassNames ) {
                    if ( injectionClassNames.contains(implementorClassName) ) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, methodName,    "Already added: Extender of: " + injectionSuperClass + ": " + implementorClassName);
                        }
                    } else {
                        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                            Tr.debug(tc, methodName, "Add: Extender of: " + injectionSuperClass + ": " + implementorClassName);
                        }
                        injectionClassNames.add(implementorClassName);
                    }
                }
            }

        } catch (UnableToAdaptException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to adapt class annotations", e);
            }
        }

        return injectionClassNames;
    }
}
