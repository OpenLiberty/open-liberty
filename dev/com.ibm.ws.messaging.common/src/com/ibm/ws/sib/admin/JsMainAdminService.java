/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.admin;

import java.util.Map;

/**
 * This class provides the functionality to consume the messaging properties sent by the configuration admin
 * and to use it. 
 */
public interface JsMainAdminService {

    /**
     * Get the state of the Messaging Engine.
     * 
     * @return String
     */
    String getMeState();
 
    void start(Map<String, Object> properties);  
    void stop();
    void modify(Map<String, Object> properties);    
}
