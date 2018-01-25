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
package session.cache.web;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class AppObject implements Serializable {
    private static final long serialVersionUID = -4193991329967907503L;

    /**
     * True if the object was deserialized. Note, we use this to determine
     * if the object was deserialized properly and to ensure that it was
     * ever serialized in the first place.
     */
    boolean deserialized;

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        deserialized = true;
    }
}