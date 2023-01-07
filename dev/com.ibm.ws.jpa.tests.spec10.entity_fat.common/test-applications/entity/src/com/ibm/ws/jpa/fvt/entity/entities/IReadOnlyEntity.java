/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.entity.entities;

public interface IReadOnlyEntity {
    public int getId();

    public void setId(int id);

    /*
     * Regular persistable attribut with no read-only protection.
     */
    public int getIntVal();

    public void setIntVal(int intVal);

    /*
     * Persistable attribute with insertable=false protection.
     */
    public int getNoInsertIntVal();

    public void setNoInsertIntVal(int noInsertIntVal);

    /*
     * Persistable attribute with updateable=false protection.
     */
    public int getNoUpdatableIntVal();

    public void setNoUpdatableIntVal(int noUpdatableIntVal);

    /*
     * Persistable attribute with insertable=false and updatable=false protection
     */
    public int getReadOnlyIntVal();

    public void setReadOnlyIntVal(int readOnlyIntVal);
}
