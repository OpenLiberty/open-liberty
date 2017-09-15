/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.passivator;

/**
 * Objects implementing this interface will be replaced during passivation.
 */
public interface PassivatorSerializable
{
    /**
     * Returns an object that should be serialized instead of this object.
     * 
     * <p>The return value is intended to help implementors via type-safety.
     * The current implementation does not depend on the value being a handle.
     * 
     * @return a replacement object
     */
    public PassivatorSerializableHandle getSerializableObject();
}
