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
 * Objects implementing this interface will be replaced during activation.
 * 
 * @see PassivatorSerializable
 */
public interface PassivatorSerializableHandle
{
    /**
     * Returns an object that should replace this object during deserialization.
     * The return value can be the called object.
     * 
     * @return a replacement object
     */
    public Object getSerializedObject();
}
