/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package jakarta.data.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.QueryHint;

/**
 * This is a temporary copy of jakarta.persistence.query.QueryOptions
 * that can be experimented with prior to availability of the
 * Jakarta Persistence 4.0 API and Liberty feature.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface QueryOptions {
    public enum QueryFlushMode { // also added in Persistence 4.0
        FLUSH,
        NO_FLUSH,
        DEFAULT
    }

    CacheStoreMode cacheStoreMode() default CacheStoreMode.USE;

    CacheRetrieveMode cacheRetrieveMode() default CacheRetrieveMode.USE;

    int timeout() default -1;

    QueryHint[] hints() default {};

    LockModeType lockMode() default LockModeType.NONE;

    PessimisticLockScope lockScope() default PessimisticLockScope.NORMAL;

    String entityGraph() default "";

    QueryFlushMode flush() default QueryFlushMode.DEFAULT;
}
