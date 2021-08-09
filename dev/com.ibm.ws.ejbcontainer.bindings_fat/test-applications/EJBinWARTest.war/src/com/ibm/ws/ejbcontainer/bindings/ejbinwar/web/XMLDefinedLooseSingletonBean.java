/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.ejbinwar.web;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.XMLDefinedStatelessInJarInWarInterface;

public class XMLDefinedLooseSingletonBean {
    private final static String CLASSNAME = XMLDefinedLooseSingletonBean.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    public String verifyBNDofSLSBLocal() throws NamingException {
        svLogger.info("--> In the XMLSingleton's verifyBNDofSLSBLocal().");

        XMLDefinedStatelessInJarInWarInterface xmlSLSBLocal = (XMLDefinedStatelessInJarInWarInterface) new InitialContext().lookup("ejblocal:ejb/core/LocalXMLSLSB");

        return xmlSLSBLocal.verifyLookup("Success");
    }
}
