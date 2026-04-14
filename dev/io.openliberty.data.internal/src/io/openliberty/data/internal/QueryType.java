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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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
          !Is.LIFE_CYCLE_METHOD, //
          !Require.AUTO_START_TX, //
          !Require.DETACH_ENTITIES, //
          !Require.RETURN_HIDDEN, //
          null), // stateful or stateless

    // stateful repository life cycle method @Detach
    DETACH("Detach", //
           Is.LIFE_CYCLE_METHOD, //
           !Require.AUTO_START_TX, //
           Require.DETACH_ENTITIES, //
           !Require.RETURN_HIDDEN, //
           Require.STATEFUL),

    // repository query method exists
    EXISTS(null, //
           !Is.LIFE_CYCLE_METHOD, //
           !Require.AUTO_START_TX, //
           !Require.DETACH_ENTITIES, //
           !Require.RETURN_HIDDEN, //
           null), // stateful or stateless

    // repository query method find/@Find/@Query(SELECT/FROM/WHERE)
    FIND(Find.class.getSimpleName(), //
         !Is.LIFE_CYCLE_METHOD, //
         !Require.AUTO_START_TX, //
         null, // detach depends on stateless vs stateful repository
         Require.RETURN_HIDDEN, //
         null), // stateful or stateless

    // stateless repository query method delete/@Delete with entity result
    FIND_AND_DELETE(Delete.class.getSimpleName(), //
                    !Is.LIFE_CYCLE_METHOD, //
                    Require.AUTO_START_TX, //
                    Require.DETACH_ENTITIES, //
                    Require.RETURN_HIDDEN, //
                    Require.STATELESS),

    // stateless repository life cycle method @Insert
    INSERT(Insert.class.getSimpleName(), //
           Is.LIFE_CYCLE_METHOD, //
           Require.AUTO_START_TX, //
           Require.DETACH_ENTITIES, //
           Require.RETURN_HIDDEN, //
           Require.STATELESS),

    // stateless repository life cycle method @Delete
    LC_DELETE(Delete.class.getSimpleName(), //
              Is.LIFE_CYCLE_METHOD, //
              Require.AUTO_START_TX, //
              !Require.DETACH_ENTITIES, //
              !Require.RETURN_HIDDEN, //
              Require.STATELESS),

    // stateless repository life cycle method @Update
    LC_UPDATE(Update.class.getSimpleName(), //
              Is.LIFE_CYCLE_METHOD, //
              Require.AUTO_START_TX, //
              !Require.DETACH_ENTITIES, //
              !Require.RETURN_HIDDEN, //
              Require.STATELESS),

    // stateless repository life cycle method @Update with entity result (find & merge)
    LC_UPDATE_MERGE(Update.class.getSimpleName(), //
                    Is.LIFE_CYCLE_METHOD, //
                    Require.AUTO_START_TX, //
                    Require.DETACH_ENTITIES, //
                    Require.RETURN_HIDDEN, //
                    Require.STATELESS),

    // stateful repository life cycle method @Merge
    MERGE("Merge", //
          Is.LIFE_CYCLE_METHOD, //
          !Require.AUTO_START_TX, //
          !Require.DETACH_ENTITIES, //
          Require.RETURN_HIDDEN, //
          Require.STATEFUL),

    // stateful repository life cycle method @Persist
    PERSIST("Persist", //
            Is.LIFE_CYCLE_METHOD, //
            Require.AUTO_START_TX, //
            !Require.DETACH_ENTITIES, //
            !Require.RETURN_HIDDEN, //
            Require.STATEFUL),

    // stateless repository query method delete/@Delete/@Query(DELETE)
    QM_DELETE(Delete.class.getSimpleName(), //
              !Is.LIFE_CYCLE_METHOD, //
              Require.AUTO_START_TX, //
              !Require.DETACH_ENTITIES, //
              !Require.RETURN_HIDDEN, //
              Require.STATELESS),

    // stateless repository query method update/@Update/@Query(UPDATE)
    QM_UPDATE(Update.class.getSimpleName(), //
              !Is.LIFE_CYCLE_METHOD, //
              Require.AUTO_START_TX, //
              !Require.DETACH_ENTITIES, //
              !Require.RETURN_HIDDEN, //
              Require.STATELESS),

    // stateful repository life cycle method @Refresh
    REFRESH("Refresh", //
            Is.LIFE_CYCLE_METHOD, //
            !Require.AUTO_START_TX, //
            !Require.DETACH_ENTITIES, //
            !Require.RETURN_HIDDEN, //
            Require.STATEFUL),

    // stateful repository life cycle method @Remove
    REMOVE("Remove", //
           Is.LIFE_CYCLE_METHOD, //
           Require.AUTO_START_TX, //
           !Require.DETACH_ENTITIES, //
           !Require.RETURN_HIDDEN, //
           Require.STATEFUL),

    // resource accessor method
    RESOURCE_ACCESS(null, //
                    !Is.LIFE_CYCLE_METHOD, //
                    !Require.AUTO_START_TX, //
                    !Require.DETACH_ENTITIES, //
                    !Require.RETURN_HIDDEN, //
                    null), // stateful or stateless

    // stateless repository life cycle method @Save
    SAVE(Save.class.getSimpleName(), //
         Is.LIFE_CYCLE_METHOD, //
         Require.AUTO_START_TX, //
         Require.DETACH_ENTITIES, //
         Require.RETURN_HIDDEN, //
         Require.STATELESS);

    private final static TraceComponent tc = Tr.register(QueryType.class);

    /**
     * Indicate if we must automatically start a transaction before invoking
     * the repository operation if a transaction is not already present.
     */
    public final boolean autoStartTransaction;

    /**
     * Indicates that a repository must clear the entity manager to detach entities
     * after the operation. Null indicates detach depends on the repository type:
     * For stateful repositories, never detach entities. For stateless repositories,
     * always detach entities.
     */
    private final Boolean detachEntities;

    /**
     * Indicates if a return value from this type of method must be hidden from
     * trace and logs.
     */
    public final boolean hideReturnValue;

    /**
     * Indicates if the repository method is a life cycle method.
     */
    public final boolean isLifeCycleMethod;

    /**
     * Name of the operation performed by the repository method,
     * suitable for display in messages to the user.
     * If an equivalent annotation (such as Find, Save, or Delete) exists,
     * then the name is the simple name of the annotation. Otherwise, the
     * name is the {@link #name()} of the enumeration constant.
     */
    public final String operationName;

    /**
     * TRUE indicates the operation is for stateful repositories only.
     * FALSE indicates the operation is for stateless repositories only.
     * NULL indicates operation is not limited to stateful or stateless
     * and applies to either repository type.
     */
    private final Boolean stateful;

    /**
     * Internal constructor for enumeration values.
     *
     * @param annoName             Simple name of the equivalent repository method
     *                                 annotation.
     * @param isLifeCycleMethod    indivates if the repository method is a life
     *                                 cycle method, such as Insert, Save, Detach.
     * @param autoStartTransaction automatically start a transaction for the
     *                                 operation.
     * @param detachEntities       require that the repository does (vs does not)
     *                                 detach entities after the operation.
     *                                 Null defers to the type of repository.
     * @param hideReturnValue      suppress logging/tracing of the method's return
     *                                 value by default
     * @param stateful             indicates if the repository method requires a
     *                                 stateful repository (TRUE), if it requires a
     *                                 stateless repository (FALSE), or if can be
     *                                 placed on either type of repository (NULL).
     */
    private QueryType(String annoName,
                      boolean isLifeCycleMethod,
                      boolean autoStartTransaction,
                      Boolean detachEntities,
                      boolean hideReturnValue,
                      Boolean stateful) {
        this.autoStartTransaction = autoStartTransaction;
        this.detachEntities = detachEntities;
        this.hideReturnValue = hideReturnValue;
        this.isLifeCycleMethod = isLifeCycleMethod;
        this.operationName = annoName == null ? name() : annoName;
        this.stateful = stateful;
    }

    /**
     * Indicates if the repository must clear the entity manager to
     * detach entities after the operation completes.
     *
     * @param stateful indicates if the repository is stateful (vs stateless).
     * @return whether the repository should detach entities.
     */
    public boolean detachEntities(boolean stateful) {
        if (stateful && this.stateful == Boolean.FALSE ||
            !stateful && this.stateful == Boolean.TRUE) // internal error
            throw new IllegalStateException(name() + ": " + this.stateful +
                                            " vs " + stateful);

        boolean detach = detachEntities == null ? !stateful : detachEntities;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc,
                     name() + " detachEntities stateful? " + stateful,
                     detach);
        return detach;
    }

    /**
     * Constants used internally by the enumeration.
     * The constants cannot be declared directly on the enumeration because the
     * enumerated values need access to them and cannot access fields that are
     * declared later in the file.
     */
    private static final class Is {
        static final boolean LIFE_CYCLE_METHOD = true;
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
        static final boolean STATEFUL = true;
        static final boolean STATELESS = false;
    }
}
