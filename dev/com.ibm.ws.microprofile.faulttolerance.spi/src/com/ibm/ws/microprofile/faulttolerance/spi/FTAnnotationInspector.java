/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.microprofile.faulttolerance.spi;

import java.lang.annotation.Annotation;

/**
 * Other features can implement this service to extend where the Fault Tolerance interceptor looks for annotations.
 */
public interface FTAnnotationInspector {

    /**
     * Returns the annotations that Fault Tolerance should consider as being on the given class
     * <p>
     * May be implemented by other features which Fault Tolerance to consider annotations located anywhere other than directly on the bean class.
     * <p>
     * E.g. Rest Client needs Fault Tolerance to consider annotations on the client interface, rather than on the proxy class.
     *
     * @param clazz the class of the target object that the Fault Tolerance interceptor is intercepting
     * @return the annotations for that class, or {@code null} to not override the annotations for this class
     */
    Annotation[] getAnnotations(Class<?> clazz);

}
