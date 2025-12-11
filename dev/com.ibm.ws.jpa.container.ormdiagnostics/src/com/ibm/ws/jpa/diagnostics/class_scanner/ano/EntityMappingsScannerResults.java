/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.class_scanner.ano;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import com.ibm.ws.common.crypto.CryptoUtils;

import com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10.ClassInformationType;
import com.ibm.ws.common.crypto.CryptoUtils;

public class EntityMappingsScannerResults {
    private final String shaDigestAlg = CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA_256;
    public static final String KEY_SHA256HASH = "SHA256HASH"; // Value is a String
    public static final String KEY_CITXML = "CITXML";   // Value is byte[]
    
    private final ClassInformationType cit;
    private final URL targetArchive;
    
    EntityMappingsScannerResults(ClassInformationType cit, URL targetArchive) {
        this.cit = cit;
        this.targetArchive = targetArchive;
    }

    public ClassInformationType getCit() {
        return cit;
    }

    public URL getTargetArchive() {
        return targetArchive;
    }  
    

    public final Map<String, Object> produceXMLWithHash() throws ClassScannerException {
        final HashMap<String, Object> retMap = new HashMap<String, Object>();
        
        try {
            final JAXBContext jaxbCtx = JAXBContext.newInstance("com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10");
            final Marshaller marshaller = jaxbCtx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final MessageDigest md = MessageDigest.getInstance(shaDigestAlg);   
            try (final DigestOutputStream dos = new DigestOutputStream(baos, md)) {
                marshaller.marshal(cit, baos);
                
                BigInteger digestBigInt = new BigInteger(1, md.digest());
                final String hashStr = digestBigInt.toString(16);
                
                retMap.put(KEY_SHA256HASH, hashStr);
                retMap.put(KEY_CITXML, baos.toByteArray());
            }  
        } catch (Exception e) {
            throw new ClassScannerException(e);
        } 
        
        return retMap;
    }
    
    public final byte[] produceXML() throws ClassScannerException {
        try {
            final JAXBContext jaxbCtx = JAXBContext.newInstance("com.ibm.ws.jpa.diagnostics.class_scanner.ano.jaxb.classinfo10");
            final Marshaller marshaller = jaxbCtx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            marshaller.marshal(cit, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ClassScannerException(e);
        }  
    }
    
    @SuppressWarnings("unchecked")
    public final <T> T produceXML(Class<T> type) throws ClassScannerException {
        byte[] xmlByteData = produceXML();
        
        if (type.isAssignableFrom(String.class)) {
            return (T) new String(xmlByteData);
        } else if (type.isAssignableFrom(byte[].class)) {
            return (T) xmlByteData;
        } else if (type.isAssignableFrom(InputStream.class)) {
            return (T) new ByteArrayInputStream(xmlByteData);
        } else {
            throw new ClassScannerException("EntityMappingsScannerResults.produceXML() does not support type: " + type.getName());
        }
    }

    @Override
    public String toString() {
        return "EntityMappingsScannerResults [cit=" + cit + ", targetArchive=" + targetArchive + "]";
    }
}
