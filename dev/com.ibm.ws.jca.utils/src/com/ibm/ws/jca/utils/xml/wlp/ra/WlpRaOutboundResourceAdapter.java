/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.xml.wlp.ra;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * ra.xml outbound-resourseadapter element
 */
@Trivial
@XmlType
public class WlpRaOutboundResourceAdapter {
    @XmlElement(name = "connection-definition")
    private final List<WlpRaConnectionDefinition> connectionDefinitions = new LinkedList<WlpRaConnectionDefinition>();

    public List<WlpRaConnectionDefinition> getConnectionDefinitions() {
        return connectionDefinitions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("WlpRaOutboundResourceAdapter{");
        for (WlpRaConnectionDefinition connectionDefinition : connectionDefinitions) {
            sb.append(connectionDefinition.toString()).append(",");
        }
        sb.append("}");
        return sb.toString();
    }
}
