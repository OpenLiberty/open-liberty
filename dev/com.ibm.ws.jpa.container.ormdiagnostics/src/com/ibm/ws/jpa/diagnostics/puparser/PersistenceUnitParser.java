/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.diagnostics.puparser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.ibm.ws.jpa.diagnostics.puparser.pu.PUP_Persistence;

public final class PersistenceUnitParser {
    private static final String digestType = System.getProperty(Constants.JVM_Property_PXML_DIGEST_ALGORITHM, 
            Constants.DEFAULT_DIGEST_ALGORITHM);
    
    public final static PersistenceParseResult parsePersistenceUnit(byte[] puByteData) throws PersistenceUnitParserException {
        if (puByteData == null) {
            return null;
        }
        
        // Determine the Schema Version
        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(puByteData);
            JPA_Schema jpaSchema = determineJPASchema(bais);
            
            if (jpaSchema == null) {
                throw new PersistenceUnitParserException("Unable to resolve the Schema Level of the target persistence.xml document.");
            }
            
            bais.reset();
            
            final JAXBContext jaxbCtx = JAXBContext.newInstance(jpaSchema.getJaxbPackage());       
            final Unmarshaller unmarshaller = jaxbCtx.createUnmarshaller();
            
            MessageDigest md = MessageDigest.getInstance(digestType);   
            md.reset();
            DigestInputStream dis = new DigestInputStream(bais, md);
            
            PUP_Persistence persistence = (PUP_Persistence) unmarshaller.unmarshal(dis);
            byte[] digest = md.digest();
            BigInteger digestBigInt = new BigInteger(1, digest);
            
            return new PersistenceParseResult(digestBigInt, persistence);
        } catch (Throwable t) {
            throw new PersistenceUnitParserException(t);
        }
    }
    
    public final static PersistenceParseResult parsePersistenceUnit(InputStream is) throws PersistenceUnitParserException {
        if (is == null) {
            return null;
        }
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead = -1;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }            
            
            final byte[] puByteData = baos.toByteArray();
            return parsePersistenceUnit(puByteData);
        } catch (IOException ioe) {
            throw new PersistenceUnitParserException(ioe);
        } finally {
            try {
                is.close();
            } catch (Throwable t) {}
        }
    }
    
    public final static PersistenceParseResult parsePersistenceUnit(File file) throws PersistenceUnitParserException {
        if (file == null) {
            return null;
        }
        
        if (!file.exists()) {
            throw new PersistenceUnitParserException(new FileNotFoundException("File does not exist: " + file.getAbsolutePath()));
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            final byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = fis.read(buffer)) > -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            byte[] fileData = baos.toByteArray();
            return parsePersistenceUnit(fileData);
        } catch (PersistenceUnitParserException pupe) {
            throw pupe;
        } catch (Throwable t) {
            throw new PersistenceUnitParserException(t);
        }
    }
    
    public final static PersistenceParseResult parsePersistenceUnit(URL url) throws PersistenceUnitParserException {
        if (url == null) {
            return null;
        }
        
        try (InputStream is = url.openStream()) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            final byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = is.read(buffer)) > -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            byte[] fileData = baos.toByteArray();
            return parsePersistenceUnit(fileData);
        } catch (PersistenceUnitParserException pupe) {
            throw pupe;
        } catch (Throwable t) {
            throw new PersistenceUnitParserException(t);
        }
    }
    
    private static JPA_Schema determineJPASchema(InputStream is) throws PersistenceUnitParserException {
        try {           
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(true);
            SAXParser parser = factory.newSAXParser();
            PersistenceUnitDocumentHandler handler = new PersistenceUnitDocumentHandler();
            
            parser.parse(is, handler);  
            return handler.getJpaSchema();
        } catch (Throwable t) {
            throw new PersistenceUnitParserException(t);
        }
    }
}
