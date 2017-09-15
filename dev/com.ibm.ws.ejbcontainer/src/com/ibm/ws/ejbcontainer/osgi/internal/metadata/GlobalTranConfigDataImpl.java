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

import com.ibm.ejs.csi.BasicGlobalTranConfigDataImpl;
import com.ibm.tx.jta.embeddable.GlobalTransactionSettings;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.commonext.GlobalTransaction;
import com.ibm.ws.javaee.dd.ejbext.EnterpriseBean;

public class GlobalTranConfigDataImpl extends BasicGlobalTranConfigDataImpl implements GlobalTransactionSettings {

    private static final TraceComponent tc = Tr.register(GlobalTranConfigDataImpl.class);

    public GlobalTranConfigDataImpl(EnterpriseBean enterpriseBeanExtension) {

        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        if (enterpriseBeanExtension != null) {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "CTOR was passed non-null EnterpriseBeanExtension object for config data");

            GlobalTransaction globalTransaction = enterpriseBeanExtension.getGlobalTransaction();

            if (globalTransaction != null) {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "We have a globlTransaction object, so use the 5.0 or later config data");

                timeout = globalTransaction.getTransactionTimeOut();
                isSendWSAT = globalTransaction.isSendWSATContext();
            }
        }
    }
}
