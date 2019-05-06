/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.jms;

import java.util.LinkedHashMap;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.rest.handler.validator.jca.JMSValidator;

public class JMSConnectionFactoryValidator implements JMSValidator {
    @Override
    @Trivial
    public String getErrorCode(Throwable x) {
        return x instanceof JMSException ? ((JMSException) x).getErrorCode() : null;
    }

    @Override
    @Trivial
    public boolean isJMSException(Throwable x) {
        return x instanceof JMSException;
    }

    @Override
    public void validate(Object cf, String user, @Sensitive String password, LinkedHashMap<String, Object> result) throws JMSException {
        ConnectionFactory jmscf = (ConnectionFactory) cf;

        Connection con = user == null ? jmscf.createConnection() : jmscf.createConnection(user, password);
        try {
            try {
                ConnectionMetaData conData = con.getMetaData();

                String provName = conData.getJMSProviderName();
                if (provName != null && provName.length() > 0)
                    result.put("jmsProviderName", provName);

                String provVersion = conData.getProviderVersion();
                if (provVersion != null && provVersion.length() > 0)
                    result.put("jmsProviderVersion", provVersion);
            } catch (UnsupportedOperationException ignore) {
            }

            try {
                String clientID = con.getClientID();
                if (clientID != null && clientID.length() > 0)
                    result.put("clientID", clientID);
            } catch (UnsupportedOperationException ignore) {
            }

            try {
                con.createSession().close();
            } catch (UnsupportedOperationException ignore) {
            }
        } finally {
            con.close();
        }
    }
}