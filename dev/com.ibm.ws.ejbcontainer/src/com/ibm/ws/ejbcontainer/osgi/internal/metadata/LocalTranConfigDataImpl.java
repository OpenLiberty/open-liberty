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
package com.ibm.ws.ejbcontainer.osgi.internal.metadata;

import com.ibm.ejs.csi.BasicLocalTranConfigDataImpl;
import com.ibm.tx.jta.embeddable.LocalTransactionSettings;
import com.ibm.websphere.csi.LocalTranConfigData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.commonext.LocalTransaction;
import com.ibm.ws.javaee.dd.ejbext.EnterpriseBean;

public class LocalTranConfigDataImpl extends BasicLocalTranConfigDataImpl implements LocalTransactionSettings {

    private static final TraceComponent tc = Tr.register(LocalTranConfigDataImpl.class);

    public LocalTranConfigDataImpl(EnterpriseBean enterpriseBeanExtension) {

        if (enterpriseBeanExtension != null) {

            LocalTransaction localTransaction = enterpriseBeanExtension.getLocalTransaction();

            if (localTransaction != null) {
                final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

                if (localTransaction.isSetBoundary()) {

                    LocalTransaction.BoundaryEnum boundaryEnum = localTransaction.getBoundary();

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "LocalTransaction boundary is set to " + boundaryEnum);

                    switch (boundaryEnum) {
                        case ACTIVITY_SESSION:
                            boundary = LocalTranConfigData.BOUNDARY_ACTIVITY_SESSION;
                            break;
                        case BEAN_METHOD:
                            boundary = LocalTranConfigData.BOUNDARY_BEAN_METHOD;
                            break;
                    }

                }

                if (localTransaction.isSetResolver()) {

                    LocalTransaction.ResolverEnum resolverEnum = localTransaction.getResolver();

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "LocalTransaction resolver is set to " + resolverEnum);

                    switch (resolverEnum) {
                        case APPLICATION:
                            resolver = LocalTranConfigData.RESOLVER_APPLICATION;
                            break;
                        case CONTAINER_AT_BOUNDARY:
                            resolver = LocalTranConfigData.RESOLVER_CONTAINER_AT_BOUNDARY;
                            break;
                    }
                }

                if (localTransaction.isSetUnresolvedAction()) {

                    LocalTransaction.UnresolvedActionEnum unresolvedActionEnum = localTransaction.getUnresolvedAction();

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "LocalTransaction unresolved-action is set to " + unresolvedActionEnum);

                    switch (unresolvedActionEnum) {
                        case COMMIT:
                            unresolvedAction = LocalTranConfigData.UNRESOLVED_COMMIT;
                            break;
                        case ROLLBACK:
                            unresolvedAction = LocalTranConfigData.UNRESOLVED_ROLLBACK;
                            break;
                    }
                }

                if (localTransaction.isSetShareable()) {

                    isShareable = localTransaction.isShareable();

                    if (isTraceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "LocalTransaction shareable is set to " + isShareable);
                }
            }
        }
    }

    @Trivial
    @Override
    public int getBoundary() {
        return boundary;
    }

    @Trivial
    @Override
    public int getResolver() {
        return resolver;
    }

    @Trivial
    @Override
    public int getUnresolvedAction() {
        return unresolvedAction;
    }
}
