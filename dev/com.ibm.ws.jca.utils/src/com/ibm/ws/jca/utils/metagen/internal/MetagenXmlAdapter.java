/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.metagen.internal;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Ensures that values read from XMLs are trimmed to remove extra white spaces
 */
@Trivial
public class MetagenXmlAdapter extends XmlAdapter<String, String> {
    @Override
    public String unmarshal(String v) throws Exception {
        if (v == null)
            return null;

        return v.trim();
    }

    @Override
    public String marshal(String v) throws Exception {
        if (v == null)
            return null;

        return v.trim();
    }

}
