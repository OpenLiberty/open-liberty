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

package com.ibm.tra.ann;

import java.io.PrintStream;

import javax.resource.ResourceException;
import javax.resource.spi.Activation;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

import com.ibm.tra.inbound.impl.TRAMessageListener1;
import com.ibm.tra.inbound.impl.TRAMessageListener2;
import com.ibm.tra.trace.DebugTracer;

@Activation(
            messageListeners = { TRAMessageListener2.class, TRAMessageListener1.class })
public class ConfigPropertyActivationAnn3 extends ConfigPropertyActivationSuperClass implements ActivationSpec {

    public ConfigPropertyActivationAnn3() {
        super();
    }

    private ResourceAdapter ra;

    private String userName1 = "TestAnnName";

    private String password1;

    @ConfigProperty
    private String serverName = "localhost";

    public String getPassword1() {
        return password1;
    }

    @ConfigProperty(description = "The Password",
                    supportsDynamicUpdates = false, confidential = true)
    public void setPassword1(String password) {
        this.password1 = password;
    }

    public String getUserName1() {
        return userName1;
    }

    @ConfigProperty(description = "AnnDescription", defaultValue = "Tester",
                    type = java.lang.String.class, supportsDynamicUpdates = true, confidential = true, ignore = true)
    public void setUserName1(String userName) {
        this.userName1 = userName;
    }

    @Override
    public void validate() throws InvalidPropertyException {
        if (DebugTracer.isDebugActivationSpec()) {
            PrintStream out = DebugTracer.getPrintStream();
            out.println("ConfigPropertyActivationAnn2.validate()");
        }

    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        if (DebugTracer.isDebugActivationSpec()) {
            PrintStream out = DebugTracer.getPrintStream();
            out.println("ConfigPropertyActivationAnn2.getResourceAdapter() : RA: " + ra);
        }
        return ra;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter arg0) throws ResourceException {
        if (DebugTracer.isDebugActivationSpec()) {
            PrintStream out = DebugTracer.getPrintStream();
            out.println("ConfigPropertyActivationAnn2.setResourceAdapter() : RA: " + arg0);
        }
        this.ra = arg0;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

}
