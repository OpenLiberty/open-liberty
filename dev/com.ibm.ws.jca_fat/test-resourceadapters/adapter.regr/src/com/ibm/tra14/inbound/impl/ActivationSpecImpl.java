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

package com.ibm.tra14.inbound.impl;

import java.io.PrintStream;

import javax.jms.Destination;
import javax.resource.spi.InvalidPropertyException;

import com.ibm.tra14.inbound.base.ActivationSpecBase;
import com.ibm.tra14.trace.DebugTracer;

public class ActivationSpecImpl extends ActivationSpecBase {

    private String _prop1;
    private Destination _dest;
    private String _destType;

    @Override
    public void validate() throws InvalidPropertyException {
        PrintStream out = DebugTracer.getPrintStream();
        boolean good = false;
        boolean debug = DebugTracer.isDebugActivationSpec();
        if (debug) {
            out.println("ActivationSpecImpl.validate(): Current contents of Prop1: " + _prop1);
            out.println("ActivationSpecImpl.validate(): Current destinationType: " + _destType);
        }
        if (_dest != null) {
            if (_dest instanceof TRAAdminObject1) {
                if (debug)
                    out.println("ActivationSpecImpl.validate(): destination is of type TRAAdminObject1");
                good = true; // Return from here... Not a pretty way to control the flow, but it works.
                // Returning because we have a valid situation, otherwise we want to throw an exception
            } else if (_dest instanceof TRAAdminObject2) {
                if (debug)
                    out.println("ActivationSpecImpl.validate(): destination is of type TRAAdminObject2");
                good = true;
            } else {
                if (debug)
                    out.println("ActivationSpecImpl.validate(): destination is of type: " + _dest.getClass().getName());
                good = true; // for now
            }

        } else {
            if (debug)
                out.println("ActivationSpecImpl.validate(): destination is null");
            good = true; // why not.
        }
        if (!good) {
            throw new InvalidPropertyException("ActivationSpecImpl.validate() failed due to unusable type.");
        }
    }

    public void setDestination(Object dest) {
        _dest = (Destination) dest;
        if (DebugTracer.isDebugActivationSpec()) {
            PrintStream out = DebugTracer.getPrintStream();
            out.println("ActivationSpecImpl.setDestination(): recieved dest: ");
            out.println("toString(): " + dest.toString());
            out.println("Class: " + dest.getClass().getName());
            out.println("End ActivationSpecImpl.setDestination()");
        }
    }

    public Destination getDestination() {
        return _dest;
    }

    public void setProp1(String prop) {
        _prop1 = prop;
    }

    public String getProp1() {
        return _prop1;
    }

    public void setDestinationType(String dt) {
        _destType = dt;
        if (DebugTracer.isDebugActivationSpec()) {
            PrintStream out = DebugTracer.getPrintStream();
            out.println("ActivationSpecImpl.setDestinationType() : dest type: " + dt);
        }
    }

    public String getDestinationType() {
        return _destType;
    }
}