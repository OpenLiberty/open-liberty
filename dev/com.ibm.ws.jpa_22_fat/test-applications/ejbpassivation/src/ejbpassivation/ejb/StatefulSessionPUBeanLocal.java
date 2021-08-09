/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package ejbpassivation.ejb;

import ejbpassivation.data.PassivateEntity;

/**
 *
 */
public interface StatefulSessionPUBeanLocal {
    public String getSessionId();

    public PassivateEntity newEntity(String description) throws Exception;

    public PassivateEntity findEntity(int id);

    public PassivateEntity updateEntity(PassivateEntity entity) throws Exception;

    public void removeEntity(PassivateEntity entity) throws Exception;

    public void remove();
}
