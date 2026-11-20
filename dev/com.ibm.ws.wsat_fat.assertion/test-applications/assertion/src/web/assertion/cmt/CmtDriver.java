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
package web.assertion.cmt;

import javax.enterprise.inject.spi.CDI;
import javax.naming.InitialContext;

import web.assertion.client.assertion.Hello;

/**
 * Calls the EJB and CDI beans. Kept out of the servlet so that it is only loaded when a test asks for it,
 * because servers that run other tests have no EJB container.
 */
public class CmtDriver {

    /**
     * @param kind  EJB (container-managed) | BMT | CDI
     * @param attr  REQUIRED | REQUIRES_NEW | MANDATORY | SUPPORTS | NOT_SUPPORTED | NEVER (ignored for BMT)
     */
    public static String call(String kind, String attr, Hello hello, String key, boolean rollback) throws Exception {
        switch (kind) {
            case "EJB": {
                final CmtBean b = (CmtBean) new InitialContext().lookup("java:module/CmtBean");
                switch (attr) {
                    case "REQUIRED":
                        return b.required(hello, key, rollback);
                    case "REQUIRES_NEW":
                        return b.requiresNew(hello, key, rollback);
                    case "MANDATORY":
                        return b.mandatory(hello, key, rollback);
                    case "SUPPORTS":
                        return b.supports(hello, key, rollback);
                    case "NOT_SUPPORTED":
                        return b.notSupported(hello, key, rollback);
                    case "NEVER":
                        return b.never(hello, key, rollback);
                    default:
                        throw new IllegalArgumentException("Unknown attr: " + attr);
                }
            }
            case "BMT":
                return ((BmtBean) new InitialContext().lookup("java:module/BmtBean")).call(hello, key, rollback);
            case "CDI": {
                final CdiTxBean b = CDI.current().select(CdiTxBean.class).get();
                try {
                    switch (attr) {
                        case "REQUIRED":
                            return b.required(hello, key, rollback);
                        case "REQUIRES_NEW":
                            return b.requiresNew(hello, key, rollback);
                        case "MANDATORY":
                            return b.mandatory(hello, key, rollback);
                        case "SUPPORTS":
                            return b.supports(hello, key, rollback);
                        case "NOT_SUPPORTED":
                            return b.notSupported(hello, key, rollback);
                        case "NEVER":
                            return b.never(hello, key, rollback);
                        default:
                            throw new IllegalArgumentException("Unknown attr: " + attr);
                    }
                } catch (RollbackRequested e) {
                    // The bean rolled back its own transaction as asked; this is the reply of its call.
                    return e.getMessage();
                }
            }
            default:
                throw new IllegalArgumentException("Unknown kind: " + kind);
        }
    }
}
