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
package com.ibm.ws.kernel.instrument.serialfilter.config;

public interface Config extends SimpleConfig {
    String FILE_PROPERTY = "com.ibm.websphere.serialfilter.config.file";

    /**
     * Check whether a class is allowed to be deserialized.
     *
     * @param cls The class to be checked
     * @param toSkip A holder for the superclass so re-checking can be avoided.
     *               This array must have at least one slot.
     *               The caller has responsibility for maintaining the state
     *               of this array and passing it in again when the superclass
     *               is being checked.
     *               The caller also has responsibility to nullify the slot at
     *               index 0 at the start of each call to readObject().
     *               To avoid leaking class loaders it is recommended that the
     *               slot be cleared at the end of readObject() as well.
     */
    boolean allows(Class<?> cls, Class<?> [] toSkip);

    /**
     * The functionality is same as above, but support suppressing logging messages if enableMessage is set to false. true is the default.
     */
    boolean allows(Class<?> cls, Class<?> [] toSkip, boolean enableMessage);

    /**
     * Discover the appropriate validation mode for a stream at construction,
     * taking into account the call stack and the class of the stream being
     * constructed.
     * <br>
     * <em>N.B. Must be called from the ObjectInputStream constructor.</em>
     *
     * @param caller the class of the stream being constructed.
     * @return the appropriate validation mode for the constructed stream.
     */
    ValidationMode getModeForStack(Class<?> caller);
}
