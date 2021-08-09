/*******************************************************************************
 * Copyright (c) 2012, 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.wim.adapter.urbridge;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;

import com.ibm.ws.security.wim.ConfigManager;

public class URConfigManager extends ConfigManager {

    @Activate
    @Override
    public void activate(ComponentContext cc, Map<String, Object> properties) {
        // TODO Auto-generated method stub
        super.activate(cc, properties);
    }
}
