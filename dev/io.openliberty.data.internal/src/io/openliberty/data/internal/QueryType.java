/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.data.internal;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Type of repository method query.
 */
@Trivial
public enum QueryType {
    // query method count
    COUNT(!Require.TX, !Require.DETACH_ENTITIES, !Require.RETURN_HIDDEN),

    // query method exists
    EXISTS(!Require.TX, !Require.DETACH_ENTITIES, !Require.RETURN_HIDDEN),

    // query method find/@Find/@Query(SELECT/FROM/WHERE)
    FIND(!Require.TX, Require.DETACH_ENTITIES, Require.RETURN_HIDDEN),

    // query method delete/@Delete with entity result
    FIND_AND_DELETE(Require.TX, Require.DETACH_ENTITIES, Require.RETURN_HIDDEN),

    // life cycle @Insert
    INSERT(Require.TX, Require.DETACH_ENTITIES, Require.RETURN_HIDDEN),

    // life cycle @Delete
    LC_DELETE(Require.TX, !Require.DETACH_ENTITIES, !Require.RETURN_HIDDEN),

    // life cycle @Update
    LC_UPDATE(Require.TX, !Require.DETACH_ENTITIES, !Require.RETURN_HIDDEN),

    // life cycle @Update often with entity result (find & merge)
    LC_UPDATE_MERGE(Require.TX, Require.DETACH_ENTITIES, Require.RETURN_HIDDEN),

    // query method delete/@Delete/@Query(DELETE)
    QM_DELETE(Require.TX, !Require.DETACH_ENTITIES, !Require.RETURN_HIDDEN),

    // query method update/@Update/@Query(UPDATE)
    QM_UPDATE(Require.TX, !Require.DETACH_ENTITIES, !Require.RETURN_HIDDEN),

    // resource accessor method
    RESOURCE_ACCESS(!Require.TX, !Require.DETACH_ENTITIES, !Require.RETURN_HIDDEN),

    // life cycle @Save
    SAVE(Require.TX, Require.DETACH_ENTITIES, Require.RETURN_HIDDEN);

    /**
     * Indicates that a stateless repository must clear the entity manager to
     * detach entities after the operation.
     */
    public final boolean detachEntities;

    /**
     * Indicates if a return value from this type of method must be hidden from
     * trace and logs.
     */
    public final boolean hideReturnValue;

    /**
     * Indicate if the operation must be run within a transaction.
     */
    public final boolean requiresTransaction;

    /**
     * Internal constructor for enumeration values.
     *
     * @param requiresTransaction require a transaction for the operation.
     * @param detachEntities      require a stateless repository to detach entities
     *                                after the operation.
     * @param hideReturnValue     suppress logging/tracing of the method's return
     *                                value by default
     */
    private QueryType(boolean requiresTransaction,
                      boolean detachEntities,
                      boolean hideReturnValue) {
        this.detachEntities = detachEntities;
        this.hideReturnValue = hideReturnValue;
        this.requiresTransaction = requiresTransaction;
    }

    /**
     * Constants used internally by the enumeration.
     * The constants cannot be declared directly on the enumeration because the
     * enumerated values need access to them and cannot access fields that are
     * declared later in the file.
     */
    private static final class Require {
        static final boolean DETACH_ENTITIES = true;
        static final boolean RETURN_HIDDEN = true;
        static final boolean TX = true;
    }
}
