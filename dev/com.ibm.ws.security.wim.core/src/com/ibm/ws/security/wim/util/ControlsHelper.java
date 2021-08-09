/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.security.wim.model.Control;
import com.ibm.wsspi.security.wim.model.Root;

/**
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
@Trivial
public class ControlsHelper {

    public static Map<String, Control> getControlMap(Root root) {
        Map ctrlMap = new HashMap();
        List controls = root.getControls();
        if (controls != null) {
            for (int i = 0; i < controls.size(); i++) {
                Control control = (Control) controls.get(i);
                String type = control.getTypeName();
                if (ctrlMap.get(type) == null) {
                    ctrlMap.put(type, control);
                }
            }
        }
        return ctrlMap;
    }

}
