/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.embeddable;

/**
 * Local transaction settings
 */
public interface LocalTransactionSettings {
    public static final int RESOLVER_APPLICATION = 0;//com.ibm.ejs.models.base.extensions.commonext.localtran.LocalTransactionResolverKind.APPLICATION;               // 0   // d121352
    public static final int RESOLVER_CONTAINER_AT_BOUNDARY = 1;//com.ibm.ejs.models.base.extensions.commonext.localtran.LocalTransactionResolverKind.CONTAINER_AT_BOUNDARY;     // 1   // d121352

    public static final int BOUNDARY_ACTIVITY_SESSION = 1;//com.ibm.ejs.models.base.extensions.commonext.localtran.LocalTransactionBoundaryKind.ACTIVITY_SESSION;               // 1   // d121352
    public static final int BOUNDARY_BEAN_METHOD = 0;//com.ibm.ejs.models.base.extensions.commonext.localtran.LocalTransactionBoundaryKind.BEAN_METHOD;                    // 2   // d121352

    public static final int UNRESOLVED_COMMIT = 1;//com.ibm.ejs.models.base.extensions.commonext.localtran.LocalTransactionUnresolvedActionKind.COMMIT;                       // 1   // d121352
    public static final int UNRESOLVED_ROLLBACK = 0;//com.ibm.ejs.models.base.extensions.commonext.localtran.LocalTransactionUnresolvedActionKind.ROLLBACK;                     // 0   // d121352

    public static final int UNKNOWN = -1;

    /**
     * @return int The value of the Boundary (ACTIVITY_SESSION | BEAN METHOD)
     */
    public int getBoundary();

    /**
     * @return int The value of resolution control ( Application )
     */
    public int getResolver();

    /**
     * @return int The value of UnResolved Action (ROLLBACK | COMMIT)
     */
    public int getUnresolvedAction();

    /**
     * @return boolean The value of isShareable
     */
    public boolean isShareable();
}
