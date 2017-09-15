/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.ejb;

import com.ibm.ws.javaee.dd.common.Describable;

/**
 * Represents &lt;ejb-relationship-role>.
 */
public interface EJBRelationshipRole
                extends Describable
{
    /**
     * Represents "One" for {@link #getMultiplicityTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.MultiplicityKind#ONE
     */
    int MULTIPLICITY_TYPE_ONE = 0;

    /**
     * Represents "Many" for {@link #getMultiplicityTypeValue}.
     * 
     * @see org.eclipse.jst.j2ee.ejb.MultiplicityKind#MANY
     */
    int MULTIPLICITY_TYPE_MANY = 1;

    /**
     * @return &lt;ejb-relationship-role-name>, or null if unspecified
     */
    String getName();

    /**
     * @return &lt;multiplicity>
     *         <ul>
     *         <li>{@link #MULTIPLICITY_TYPE_ONE} - One
     *         <li>{@link #MULTIPLICITY_TYPE_MANY} - Many
     *         </ul>
     */
    int getMultiplicityTypeValue();

    /**
     * @return true if &lt;cascade-delete> is specified
     */
    boolean isCascadeDelete();

    /**
     * @return &lt;relationship-role-source>
     */
    RelationshipRoleSource getSource();

    /**
     * @return &lt;cmr-field>, or null if unspecified
     */
    CMRField getCmrField();
}
