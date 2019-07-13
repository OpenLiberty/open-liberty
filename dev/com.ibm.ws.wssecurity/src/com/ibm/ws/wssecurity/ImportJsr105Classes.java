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
package com.ibm.ws.wssecurity;

import javax.xml.crypto.Data;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.Manifest;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.spec.XPathType;

// This class is a dumy class
// its purpose is to force bnd.bnd to generate import-package entries for javax.xml.crypto 
public class ImportJsr105Classes {

    Object[] dummyObjects = new Object[10];

    public ImportJsr105Classes() {
        int iCnt = 0;

        dummyObjects[iCnt++] = DOMCryptoContext.class;
        dummyObjects[iCnt++] = Manifest.class;
        dummyObjects[iCnt++] = DOMSignContext.class;
        dummyObjects[iCnt++] = KeyInfo.class;
        dummyObjects[iCnt++] = XPathType.class;
        dummyObjects[iCnt++] = Data.class;
    }

}
