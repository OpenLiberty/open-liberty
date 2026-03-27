/*******************************************************************************
 * Copyright (c) 2025,2026 IBM Corporation and others.
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

import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Save;
import jakarta.data.repository.Update;

/**
 * Type of repository method query.
 */
@Trivial
public enum QueryType {
    // repository query method count
    COUNT(null, //
          !Require.AUTO_START_TX, //
          !Require.DETACH_ENTITIES, //
          !Require.RETURN_HIDDEN),

    // stateful repository life cycle method @Detach
    DETACH("Detach", //
           !Require.AUTO_START_TX, //
           Require.DETACH_ENTITIES, //
           !Require.RETURN_HIDDEN),

    // repository query method exists
    EXISTS(null, //
           !Require.AUTO_START_TX, //
           !Require.DETACH_ENTITIES, //
           !Require.RETURN_HIDDEN),

    // repository query method find/@Find/@Query(SELECT/FROM/WHERE)
    FIND(Find.class.getSimpleName(), //
         !Require.AUTO_START_TX, //
         Require.DETACH_ENTITIES, //
         Require.RETURN_HIDDEN),

    // stateless repository query method delete/@Delete with entity result
    FIND_AND_DELETE(Delete.class.getSimpleName(), //
                    Require.AUTO_START_TX, //
                    Require.DETACH_ENTITIES, //
                    Require.RETURN_HIDDEN),

    // stateless repository life cycle method @Insert
    INSERT(Insert.class.getSimpleName(), //
           Require.AUTO_START_TX, //
           Require.DETACH_ENTITIES, //
           Require.RETURN_HIDDEN),

    // stateless repository life cycle method @Delete
    LC_DELETE(Delete.class.getSimpleName(), //
              Require.AUTO_START_TX, //
              !Require.DETACH_ENTITIES, //
              !Require.RETURN_HIDDEN),

    // stateless repository life cycle method @Update
    LC_UPDATE(Update.class.getSimpleName(), //
              Require.AUTO_START_TX, //
              !Require.DETACH_ENTITIES, //
              !Require.RETURN_HIDDEN),

    // stateless repository life cycle method @Update with entity result (find & merge)
    LC_UPDATE_MERGE(Update.class.getSimpleName(), //
                    Require.AUTO_START_TX, //
                    Require.DETACH_ENTITIES, //
                    Require.RETURN_HIDDEN),

    // stateful repository life cycle method @Merge
    MERGE("Merge", //
          !Require.AUTO_START_TX, //
          !Require.DETACH_ENTITIES, //
          Require.RETURN_HIDDEN),

    // stateful repository life cycle method @Persist
    PERSIST("Persist", //
            !Require.AUTO_START_TX, //
            !Require.DETACH_ENTITIES, //
            !Require.RETURN_HIDDEN),

    // stateless repository query method delete/@Delete/@Query(DELETE)
    QM_DELETE(Delete.class.getSimpleName(), //
              Require.AUTO_START_TX, //
              !Require.DETACH_ENTITIES, //
              !Require.RETURN_HIDDEN),

    // stateless repository query method update/@Update/@Query(UPDATE)
    QM_UPDATE(Update.class.getSimpleName(), //
              Require.AUTO_START_TX, //
              !Require.DETACH_ENTITIES, //
              !Require.RETURN_HIDDEN),

    // stateful repository life cycle method @Refresh
    REFRESH("Refresh", //
            !Require.AUTO_START_TX, //
            !Require.DETACH_ENTITIES, //
            !Require.RETURN_HIDDEN),

    // stateful repository life cycle method @Remove
    REMOVE("Remove", //
           !Require.AUTO_START_TX, //
           !Require.DETACH_ENTITIES, //
           !Require.RETURN_HIDDEN),

    // resource accessor method
    RESOURCE_ACCESS(null, //
                    !Require.AUTO_START_TX, //
                    !Require.DETACH_ENTITIES, //
                    !Require.RETURN_HIDDEN),

    // stateless repository life cycle method @Save
    SAVE(Save.class.getSimpleName(), //
         Require.AUTO_START_TX, //
         Require.DETACH_ENTITIES, //
         Require.RETURN_HIDDEN);

    /**
     * Indicate if we must automatically start a transaction before invoking
     * the repository operation if a transaction is not already present.
     * For stateful entities, this will always be false.
     */
    public final boolean autoStartTransaction;

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
     * Name of the operation performed by the repository method,
     * suitable for display in messages to the user.
     * If an equivalent annotation (such as Find, Save, or Delete) exists,
     * then the name is the simple name of the annotation. Otherwise, the
     * name is the {@link #name()} of the enumeration constant.
     */
    public final String operationName;

    /**
     * Internal constructor for enumeration values.
     *
     * @param annoName             Simple name of the equivalent repository method
     *                                 annotation.
     * @param autoStartTransaction automatically start a transaction for the
     *                                 operation.
     * @param detachEntities       require a stateless repository to detach entities
     *                                 after the operation.
     * @param hideReturnValue      suppress logging/tracing of the method's return
     *                                 value by default
     */
    private QueryType(String annoName,
                      boolean autoStartTransaction,
                      boolean detachEntities,
                      boolean hideReturnValue) {
        this.autoStartTransaction = autoStartTransaction;
        this.detachEntities = detachEntities;
        this.hideReturnValue = hideReturnValue;
        this.operationName = annoName == null ? name() : annoName;
    }

    /**
     * Constants used internally by the enumeration.
     * The constants cannot be declared directly on the enumeration because the
     * enumerated values need access to them and cannot access fields that are
     * declared later in the file.
     */
    private static final class Require {
        static final boolean AUTO_START_TX = true;
        static final boolean DETACH_ENTITIES = true;
        static final boolean RETURN_HIDDEN = true;
    }
}
