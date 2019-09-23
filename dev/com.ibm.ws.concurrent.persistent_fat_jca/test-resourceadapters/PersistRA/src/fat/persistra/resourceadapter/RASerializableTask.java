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
import java.util.concurrent.Callable;

import javax.enterprise.concurrent.ManagedTask;
import javax.resource.spi.AdministeredObject;

/**
 * Serializable task that is provided by a resource adapter.
 */
@AdministeredObject(adminObjectInterfaces = { Callable.class, ManagedTask.class })
public class RASerializableTask extends RATask implements Serializable {
    private static final long serialVersionUID = -3417561161209707368L;
}
