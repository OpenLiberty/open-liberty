/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.jca.resourceadapter;

import java.util.Collections;
import java.util.Enumeration;

import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;

public class FVTConnectionMetaData implements ConnectionMetaData {
    FVTManagedConnection mc;

    FVTConnectionMetaData(FVTManagedConnection mc) {
        this.mc = mc;
    }

    @Override
    public int getJMSMajorVersion() throws JMSException {
        return 2;
    }

    @Override
    public int getJMSMinorVersion() throws JMSException {
        return 0;
    }

    @Override
    public String getJMSProviderName() throws JMSException {
        // (ab)using this method as a convenient way to externalize the user name to the application
        if (mc == null || mc.user == null)
            return "Fake JMS Provider for Tests";
        else
            return "Fake JMS Provider for Tests, created for " + mc.user;
    }

    @Override
    public String getJMSVersion() throws JMSException {
        return "2.0";
    }

    @Override
    public Enumeration<?> getJMSXPropertyNames() throws JMSException {
        return Collections.emptyEnumeration();
    }

    @Override
    public int getProviderMajorVersion() throws JMSException {
        return 96;
    }

    @Override
    public int getProviderMinorVersion() throws JMSException {
        return 247;
    }

    @Override
    public String getProviderVersion() throws JMSException {
        return "96.247.265";
    }
}
