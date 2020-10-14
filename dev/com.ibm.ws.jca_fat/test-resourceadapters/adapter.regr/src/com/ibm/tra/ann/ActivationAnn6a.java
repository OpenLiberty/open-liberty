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

import javax.jms.Destination;
import javax.resource.spi.Activation;
import javax.resource.spi.InvalidPropertyException;

import com.ibm.tra.inbound.base.ActivationSpecBase;
import com.ibm.tra.inbound.impl.TRAAdminObject1;
import com.ibm.tra.inbound.impl.TRAAdminObject2;
import com.ibm.tra.inbound.impl.TRAMessageListener6;
import com.ibm.tra.trace.DebugTracer;

@Activation(
            messageListeners = { TRAMessageListener6.class })
public class ActivationAnn6a extends ActivationSpecBase {

    public ActivationAnn6a() {
        super();
    }

    private String prop1;

    public String getProp1() {
        return this.prop1;
    }

    public void setProp1(String parm1) {
        this.prop1 = parm1;
    }

    private Destination destination;

    public Destination getDestination() {
        return this.destination;
    }

    public void setDestination(Destination dest) {
        destination = dest;
        if (DebugTracer.isDebugActivationSpec()) {
            PrintStream out = DebugTracer.getPrintStream();
            out.println("ActivationAnn6a.setDestination(): recieved dest: ");
            out.println("toString(): " + destination.toString());
            out.println("Class: " + destination.getClass().getName());
            out.println("End ActivationAnn6a.setDestination()");
        }
    }

    private String destinationType;

    public String getDestinationType() {
        return this.destinationType;
    }

    public void setDestinationType(String dt) {
        destinationType = dt;
        if (DebugTracer.isDebugActivationSpec()) {
            PrintStream out = DebugTracer.getPrintStream();
            out.println("ActivationAnn6a.setDestinationType() : destination type: " + dt);
        }
    }

    @Override
    public void validate() throws InvalidPropertyException {
        PrintStream out = DebugTracer.getPrintStream();
        boolean good = false;
        boolean debug = DebugTracer.isDebugActivationSpec();
        if (debug) {
            out.println("ActivationAnn6a.validate(): Current contents of Prop1: " + prop1);
            out.println("ActivationAnn6a.validate(): Current destinationType: " + destinationType);
        }
        if (destination != null) {
            if (destination instanceof TRAAdminObject1) {
                if (debug)
                    out.println("ActivationAnn6a.validate(): destination is of type TRAAdminObject1");
                good = true;
                // Return from here... Not a pretty way to control the
                // flow, but it works.
                // Returning because we have a valid situation, otherwise we want to
                // throw an exception
            } else if (destination instanceof TRAAdminObject2) {
                if (debug)
                    out.println("ActivationAnn6a.validate(): destination is of type TRAAdminObject2");
                good = true;
            } else {
                if (debug)
                    out.println("ActivationAnn6a.validate(): destination is of type: " + destination.getClass().getName());
                good = true; // for now
            }
        } else {
            if (debug)
                out.println("ActivationAnn6a.validate(): destination is null");
            good = true; // why not.
        }
        if (!good) {
            throw new InvalidPropertyException("ActivationAnn6a.validate() failed due to unusable type.");
        }
    }

}
