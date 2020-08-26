/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.mp.jwt.osgi.v12;

import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Component;

import io.openliberty.security.mp.jwt.osgi.MpJwtRuntimeVersion;

@Component(service = MpJwtRuntimeVersion.class, immediate = true, property = { "version=1.2", "service.ranking:Integer=12" }, name = "mpJwtRuntimeVersionService")
public class MpJwt12Runtime implements MpJwtRuntimeVersion {

    @Override
    public Version getVersion() {
        return VERSION_1_2;
    }

}
