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
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.ConfigProperty;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

import com.ibm.tra.trace.DebugTracer;

public class ConfigPropertyActivationAnn2 implements ActivationSpec {

    public ConfigPropertyActivationAnn2() {
        super();
    }

    private ResourceAdapter ra;

    @ConfigProperty(description = "AnnDescription",
                    supportsDynamicUpdates = false)
    private String userName1 = "TestAnnName";

    @ConfigProperty(description = "The Password",
                    supportsDynamicUpdates = false, confidential = true)
    private String password1;

    public String getPassword1() {
        return password1;
    }

    public void setPassword1(String password1) {
        this.password1 = password1;
    }

    @ConfigProperty(description = "The config property marked confidential, but overridden in the deployment descriptor as non-confidential.",
                    supportsDynamicUpdates = false, confidential = true)
    private String conf1;

    public String getConf1() {
        return conf1;
    }

    public void setConf1(String conf1) {
        this.conf1 = conf1;
    }

    public String getUserName1() {
        return userName1;
    }

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

}
