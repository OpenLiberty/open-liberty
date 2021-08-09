/*******************************************************************************
 * Copyright (c) 2012,2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    // WMQ has a property, "useJNDI", that has special handling in the jca component. 
    // The testing in this bucket makes sure the special handling does not occur for resource adapters
    // other than wmqJms.
    private Boolean useJNDI = false;

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

    public Boolean getUseJNDI() {
        return useJNDI;
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

    public void setUseJNDI(Boolean useJNDI) {
        this.useJNDI = useJNDI;
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
        if (useJNDI && destination.contains("jms/"))
            throw new InvalidPropertyException("useJNDI");
        String pwd = userPwds.get(userName);
        if (pwd == null)
            throw new InvalidPropertyException("UserName: " + userName);
        if (!pwd.equals(password))
            throw new InvalidPropertyException("Password: " + password);
    }
}
