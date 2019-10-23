/*******************************************************************************
 * Copyright (c) 2012, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.persistra.resourceadapter;

import java.io.Serializable;

import javax.enterprise.concurrent.Trigger;
import javax.resource.spi.AdministeredObject;

/**
 * Serializable trigger that is provided by a resource adapter.
 */
@AdministeredObject(adminObjectInterfaces = Trigger.class)
public class RASerializableTrigger extends RATrigger implements Serializable {
    private static final long serialVersionUID = 9017586520727373111L;
}
