/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
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
package test.jca.adapter;

import java.util.Queue;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.InvalidPropertyException;
import jakarta.resource.spi.ResourceAdapter;

public class BVTActivationSpec implements ActivationSpec {

    private transient BVTResourceAdapter adapter;
    private Queue<String> destination;
    private int expectedDuration = -1;

    public Queue<String> getDestination() {
        return destination;
    }

    public String getDestinationType() {
        return Queue.class.getName();
    }

    public int getExpectedDuration() {
        return expectedDuration;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return adapter;
    }

    public void setDestination(Queue<String> destination) {
        this.destination = destination;
    }

    public void setDestinationType(String destinationType) {
        if (!Queue.class.getName().equals(destinationType))
            throw new IllegalArgumentException(destinationType);
    }

    public void setExpectedDuration(int expectedDuration) {
        this.expectedDuration = expectedDuration;
    }

    @Override
    public void setResourceAdapter(ResourceAdapter adapter) throws ResourceException {
        this.adapter = (BVTResourceAdapter) adapter;
    }

    @Override
    public void validate() throws InvalidPropertyException {
        if (adapter == null)
            throw new InvalidPropertyException("ResourceAdapter");
        if (destination == null)
            throw new InvalidPropertyException("Destination");
        if (expectedDuration < 0)
            throw new InvalidPropertyException("ExpectedDuration");
    }
}
