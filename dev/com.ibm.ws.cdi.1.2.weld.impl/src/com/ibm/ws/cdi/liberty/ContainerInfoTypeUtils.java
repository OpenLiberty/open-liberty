/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.liberty;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.interfaces.ArchiveType;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;

/**
 *
 */
public class ContainerInfoTypeUtils {

    private static final TraceComponent tc = Tr.register(ContainerInfoTypeUtils.class);

    static ArchiveType getType(ContainerInfo.Type ciType) {

        switch (ciType) {
            case MANIFEST_CLASSPATH:
                return ArchiveType.MANIFEST_CLASSPATH;
            case WEB_INF_LIB:
                return ArchiveType.WEB_INF_LIB;
            case EAR_LIB:
                return ArchiveType.EAR_LIB;
            case WEB_MODULE:
                return ArchiveType.WEB_MODULE;
            case EJB_MODULE:
                return ArchiveType.EJB_MODULE;
            case CLIENT_MODULE:
                return ArchiveType.CLIENT_MODULE;
            case RAR_MODULE:
                return ArchiveType.RAR_MODULE;
            case JAR_MODULE:
                return ArchiveType.JAR_MODULE;
            case SHARED_LIB:
                return ArchiveType.SHARED_LIB;
            default:
                throw new IllegalArgumentException(Tr.formatMessage(tc,
                                                                    "unknown.container.type.CWOWB1004E",
                                                                    ciType));
        }
    }
}
