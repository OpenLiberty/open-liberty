/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.serialization;

import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Allows replacing non-serializable objects prior to serialization. Typical
 * scenarios are:
 * 
 * <ul>
 * <li>A bundle does not want to allow its object to be serialized arbitrarily
 * by user code (e.g., because the object itself contains references to other
 * objects that aren't visible to that bundle). In this case, the bundle would
 * not mark its objects Serializable to disallow serialization.
 * 
 * <li>A bundle wants to allow serialization of a known object from another
 * bundle, which normally does not allow serialization of its objects.
 * </ul>
 * 
 * <p>In either case, the bundle would provide a replacer to recognize the
 * object and return a serialized form, and would add a resolveObject method to
 * the class of the serialized form class to recreate the original object. Note
 * that the class of the serialized form will typically need to be made visible
 * via {@link DeserializationClassProvider}.
 */
public interface SerializationObjectReplacer {
    /**
     * Replaces non-serialization objects prior to serialization. If the
     * implementation does not recognize the object then null should be
     * returned. If an object is returned, it must be either Serializable or
     * Externalizable.
     * <p>
     * Implementations are strongly encouraged to annotate the parameter
     * with {@link Sensitive} to avoid tracing user data.
     * 
     * @param object the object being serialized
     * @return the replacement object, or null if no replacement is needed
     */
    Object replaceObject(@Sensitive Object object);
}
