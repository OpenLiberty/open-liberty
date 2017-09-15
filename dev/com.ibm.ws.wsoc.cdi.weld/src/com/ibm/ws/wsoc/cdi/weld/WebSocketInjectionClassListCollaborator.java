/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import com.ibm.ws.container.service.annotations.WebAnnotations;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.anno.targets.AnnotationTargets_Targets;
import com.ibm.wsspi.webcontainer.collaborator.WebAppInjectionClassListCollaborator;

public class WebSocketInjectionClassListCollaborator  implements WebAppInjectionClassListCollaborator {

	private static final TraceComponent tc = Tr.register(WebSocketInjectionClassListCollaborator.class);

	// List of abstract classes in WebSockets for which extenders require support for CDI 1.2    
    private final String[] injectionSubClasses = new String[]{"javax.websocket.Endpoint"}; 

	@Override
	@FFDCIgnore(UnableToAdaptException.class)
	public List<String> getInjectionClasses(Container moduleContainer) {

		ArrayList<String> classList = new ArrayList<String>();
		Set<String> injectionClassNames;

		try {
			WebAnnotations webAnnotations = moduleContainer.adapt(WebAnnotations.class);
			AnnotationTargets_Targets annotationTargets = webAnnotations.getAnnotationTargets();

			// Find POJOs that have been annotated to be Endpoints.  Annotated Endpoints can not be inherited, so don't look for inherited classes  
			Set<String> annotatedPojoClassNames = annotationTargets.getAnnotatedClasses("javax.websocket.server.ServerEndpoint");

			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "getInjectionClasses",	"Found and added annotated server endpoint classes of: " + annotatedPojoClassNames);
			}
			
			classList.addAll(annotatedPojoClassNames);

			// Look for objects which extend classes which must support CDI 1.2
			for (String injectionSubClass : injectionSubClasses) {

				injectionClassNames = annotationTargets.getSubclassNames(injectionSubClass);

				Iterator<String> iterator = injectionClassNames.iterator();

				while (iterator.hasNext()) {
					String element = iterator.next();
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(tc, "getInjectionClasses",	"Found extender of " + injectionSubClass + " : " + element);
					}
					
					// add if not previously found
					if (!classList.contains(element)) {
						classList.add(element);
						if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
							Tr.debug(tc, "getInjectionClasses",	"Add sub class :" + element);
						}
					}
				}
			}

		} catch (UnableToAdaptException e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "failed to adapt to for class annotations", e);
			}
		}

		return classList;

	}

}
