/*******************************************************************************
 * Copyright (c) 2001, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

import com.ibm.tx.jta.embeddable.LocalTransactionSettings;

/**
 * 
 * This interface defines methods to retrieve
 * ActivitySession or Local Tran attributes as defined in the deployment XML
 */
public interface LocalTranConfigData {
    public static final int RESOLVER_APPLICATION = LocalTransactionSettings.RESOLVER_APPLICATION;
    public static final int RESOLVER_CONTAINER_AT_BOUNDARY = LocalTransactionSettings.RESOLVER_CONTAINER_AT_BOUNDARY;

    public static final int BOUNDARY_ACTIVITY_SESSION = LocalTransactionSettings.BOUNDARY_ACTIVITY_SESSION;
    public static final int BOUNDARY_BEAN_METHOD = LocalTransactionSettings.BOUNDARY_BEAN_METHOD;

    public static final int UNRESOLVED_COMMIT = LocalTransactionSettings.UNRESOLVED_COMMIT;
    public static final int UNRESOLVED_ROLLBACK = LocalTransactionSettings.UNRESOLVED_ROLLBACK;

    public static final int UNKNOWN = LocalTransactionSettings.UNKNOWN;

    /**
     * @return int The value of the Boundary (ACTIVITY_SESSION | BEAN METHOD)
     */
    public int getValueBoundary();

    /**
     * @return int The value of resolution control ( Application )
     */
    public int getValueResolver();

    /**
     * @return int The value of UnResolved Action (ROLLBACK | COMMIT)
     */
    public int getValueUnresolvedAction();

    /**
     * @return boolean The value of isShareable
     */
    public boolean isShareable();

}
