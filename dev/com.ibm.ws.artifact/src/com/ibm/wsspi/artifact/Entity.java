/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.artifact;

/**
 * Represents common aspects of an Entity, which may be a Container, or an Entry.
 */
public interface Entity {

    /**
     * Get the name of this entity.
     * 
     * @return name of this entity.
     */
    public String getName();

}
