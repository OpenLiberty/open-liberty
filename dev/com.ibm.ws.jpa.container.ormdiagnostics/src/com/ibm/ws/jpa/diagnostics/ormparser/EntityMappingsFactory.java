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

package com.ibm.ws.jpa.diagnostics.ormparser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.ibm.ws.jpa.diagnostics.ormparser.entitymapping.IEntityMappings;

public class EntityMappingsFactory {
    private static final String digestType = System.getProperty(Constants.JVM_Property_ORMXML_DIGEST_ALGORITHM,
                                                                Constants.DEFAULT_DIGEST_ALGORITHM);

    public static EntityMappingsDefinition parseEntityMappings(URL srcUrl, byte[] mappingFileData) throws EntityMappingsException {
        if (mappingFileData == null || mappingFileData.length == 0) {
            return null;
        }

        // Determine the Schema Version of the Mappings File
        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(mappingFileData);

            final String pkg = determineJAXBPackage(bais).getJaxbPackage();
            bais.reset();

            final JAXBContext jaxbCtx = JAXBContext.newInstance(pkg);
            final Unmarshaller unmarshaller = jaxbCtx.createUnmarshaller();

            MessageDigest md = MessageDigest.getInstance(digestType);
            md.reset();
            DigestInputStream dis = new DigestInputStream(bais, md);
            IEntityMappings entityMapping = (IEntityMappings) unmarshaller.unmarshal(dis);

            byte[] digest = md.digest();
            BigInteger digestBigInt = new BigInteger(1, digest);

            return new EntityMappingsDefinition(srcUrl, mappingFileData, digestBigInt, entityMapping);
        } catch (EntityMappingsException eme) {
            throw eme;
        } catch (Throwable t) {
            throw new EntityMappingsException(t);
        }
    }

    public static EntityMappingsDefinition parseEntityMappings(File file) throws EntityMappingsException {
        if (file == null) {
            return null;
        }

        if (!file.exists()) {
            throw new EntityMappingsException(new FileNotFoundException("File does not exist: " + file.getAbsolutePath()));
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();

            final byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = fis.read(buffer)) > -1) {
                baos.write(buffer, 0, bytesRead);
            }

            byte[] fileData = baos.toByteArray();
            return parseEntityMappings(file.toURI().toURL(), fileData);
        } catch (Throwable t) {
            throw new EntityMappingsException(t);
        }
    }

    public static EntityMappingsDefinition parseEntityMappings(URL url) throws EntityMappingsException {
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
            return parseEntityMappings(url, fileData);
        } catch (Throwable t) {
            throw new EntityMappingsException(t);
        }
    }

    private enum JPAJAXBPackage {
        V1_0("1.0", Constants.JPA_10_JAXB_PACKAGE),
        V2_0("2.0", Constants.JPA_20_JAXB_PACKAGE),
        V2_1("2.1", Constants.JPA_21_JAXB_PACKAGE),
        V2_2("2.2", Constants.JPA_21_JAXB_PACKAGE);

        private final String version;
        private final String jaxbPackage;

        private JPAJAXBPackage(String version, String jaxbPackage) {
            this.version = version;
            this.jaxbPackage = jaxbPackage;
        }

        public String getJaxbPackage() {
            return jaxbPackage;
        }

        public String getVersion() {
            return version;
        }

        public static JPAJAXBPackage getByVersion(String vStr) {
            if (vStr == null || vStr.isEmpty()) {
                return null;
            }

            for (JPAJAXBPackage item : JPAJAXBPackage.values()) {
                if (vStr.equals(item.getVersion())) {
                    return item;
                }
            }

            return null;
        }

        public static JPAJAXBPackage getDefault() {
            for (JPAJAXBPackage item : JPAJAXBPackage.values()) {
                if (Constants.JPA_DEFAULT_JAXB_PACKAGE.equals(item.getJaxbPackage())) {
                    return item;
                }
            }

            throw new IllegalStateException("Unable to identify default JAXB package.");
        }
    }

    private static JPAJAXBPackage determineJAXBPackage(InputStream is) throws EntityMappingsException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setValidating(true);
            SAXParser parser = factory.newSAXParser();
            PersistenceUnitDocumentHandler handler = new PersistenceUnitDocumentHandler();

            parser.parse(is, handler);
            String version = handler.getVersion();

            if (version == null) {
                return JPAJAXBPackage.getDefault();
            }

            return JPAJAXBPackage.getByVersion(version);
        } catch (Throwable t) {
            throw new EntityMappingsException(t);
        }
    }
}
