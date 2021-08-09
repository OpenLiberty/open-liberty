/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.adaptable.module;

import com.ibm.ws.adaptable.module.structure.StructureHelper;

/**
 *
 */
public interface InterpretedContainer extends Container {
    /**
     * Set a structure helper into this interpreted container.<p>
     * Structure helpers can promote Containers to become isRoot = true.
     * 
     * @param sh
     * @throws IllegalStateException if a StructureHelper is already set.
     */
    public void setStructureHelper(StructureHelper sh);
}
