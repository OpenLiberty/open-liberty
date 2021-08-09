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
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.security.config.tss;

import java.io.Serializable;

import org.omg.CSIIOP.SCS_GSSExportedName;
import org.omg.CSIIOP.SCS_GeneralNames;
import org.omg.CSIIOP.ServiceConfiguration;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.config.ConfigException;
import com.ibm.ws.transport.iiop.security.util.Util;

/**
 * @version $Revision: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public abstract class TSSServiceConfigurationConfig implements Serializable {
    public abstract ServiceConfiguration generateServiceConfiguration() throws ConfigException;

    public static TSSServiceConfigurationConfig decodeIOR(ServiceConfiguration sc) throws Exception {
        TSSServiceConfigurationConfig result = null;

        if (sc.syntax == SCS_GeneralNames.value) {
            result = new TSSGeneralNameConfig(sc.name);
        } else if (sc.syntax == SCS_GSSExportedName.value) {
            result = new TSSGSSExportedNameConfig(Util.decodeGSSExportedName(sc.name));
        } else {
            result = new TSSUnknownServiceConfigurationConfig(sc.syntax, sc.name);
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString("", buf);
        return buf.toString();
    }

    @Trivial
    abstract void toString(String spaces, StringBuilder buf);

}
