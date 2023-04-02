/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package fat.jca.resourceadapter;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Queue;
import javax.jms.Topic;
import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

public class FVTActivationSpec implements ActivationSpec {

    private static final Map<String, String> userPwds = new HashMap<String, String>();
    static {
        userPwds.put("ACTV1USER", "ACTV1PWD");
    }

    private transient FVTResourceAdapter adapter;
    private String destination;
    private String destinationType = Queue.class.getName();
    private String password;
    private String userName;

    public String getDestination() {
        return destination;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public String getPassword() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public String getUserName() {
        throw new UnsupportedOperationException();
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setDestinationType(String destinationType) {
        if (!Queue.class.getName().equals(destinationType) && !Topic.class.getName().equals(destinationType))
            throw new IllegalArgumentException(destinationType);
        this.destinationType = destinationType;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (FVTResourceAdapter) adapter;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public void validate() throws InvalidPropertyException {
        if (adapter == null)
            throw new InvalidPropertyException("ResourceAdapter");
        if (destination == null)
            throw new InvalidPropertyException("Destination");
        String pwd = userPwds.get(userName);
        if (pwd == null)
            throw new InvalidPropertyException("UserName: " + userName);
        if (!pwd.equals(password))
            throw new InvalidPropertyException("Password: " + password);
    }
}
