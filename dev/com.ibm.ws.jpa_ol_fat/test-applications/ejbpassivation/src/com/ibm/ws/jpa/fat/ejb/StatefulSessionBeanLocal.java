/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fat.ejb;

import com.ibm.ws.jpa.fat.data.PassivateEntity;

/**
 *
 */
public interface StatefulSessionBeanLocal {
    public String getSessionId();

    public PassivateEntity newEntity(String description) throws Exception;

    public PassivateEntity findEntity(int id);

    public PassivateEntity updateEntity(PassivateEntity entity) throws Exception;

    public void removeEntity(PassivateEntity entity) throws Exception;

    public void remove();
}
