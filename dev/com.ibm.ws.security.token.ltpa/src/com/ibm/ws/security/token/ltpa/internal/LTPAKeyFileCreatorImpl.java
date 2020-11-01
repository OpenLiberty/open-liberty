/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtilityImpl;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.location.WsResource.Type;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

/**
 *
 */
public class LTPAKeyFileCreatorImpl extends LTPAKeyFileUtilityImpl implements LTPAKeyFileCreator {

    /** {@inheritDoc} */
    @Override
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive byte[] keyPasswordBytes, String realmName) throws Exception {
        Properties ltpaProps = generateLTPAKeys(keyPasswordBytes, realmName);
        addLTPAKeysToFile(getOutputStream(locService, keyFile), ltpaProps);
        return ltpaProps;
    }

    private OutputStream getOutputStream(WsLocationAdmin locService, final String keyImportFile) throws IOException {
        // Get the WsResource and create the file
        WsResource ltpaFile = locService.resolveResource(keyImportFile);
        ltpaFile.create();
        if (ltpaFile.isType(Type.FILE)) {
            FileUtils.setUserReadWriteOnly(ltpaFile.asFile());
        }

        // Get the output stream form the resource service
        return ltpaFile.putStream();
    }

}
