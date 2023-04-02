/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.mp.jwt.osgi.v21;

import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Component;

import io.openliberty.security.mp.jwt.osgi.MpJwtRuntimeVersion;

@Component(service = MpJwtRuntimeVersion.class, immediate = true, property = { "version=2.1", "service.ranking:Integer=12" }, name = "mpJwtRuntimeVersionService")
public class MpJwt21Runtime implements MpJwtRuntimeVersion {

    @Override
    public Version getVersion() {
        return VERSION_2_1;
    }

}
