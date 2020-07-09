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
package io.openliberty.ejbcontainer.v40.internal;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.ejbcontainer.osgi.EJBRuntimeVersion;

@Component(service = EJBRuntimeVersion.class, property = { "version=4.0", "service.ranking:Integer=40" })
public class EJBRuntimeVersionV40 extends EJBRuntimeVersion {
}
