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

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.ibm.ws.jpa.diagnostics.ormparser.entitymapping.IEntityMappings;

public class EntityMappingsDefinition {
    private final URL source;
    private final byte[] fileData;
    private final BigInteger hash;
    private final IEntityMappings entityMappings;

    public EntityMappingsDefinition(URL source, byte[] fileData, BigInteger hash, IEntityMappings entityMappings) {
        if (source == null || hash == null || entityMappings == null) {
            throw new NullPointerException("Constructor cannot accept any null arguments.");
        }

        this.source = source;
        this.fileData = fileData;
        this.hash = hash;
        this.entityMappings = entityMappings;
    }

    public BigInteger getHash() {
        return hash;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public URL getSource() {
        return source;
    }

    public IEntityMappings getEntityMappings() {
        return entityMappings;
    }

    public String getEntityMappingsAsXML() {
        try {
            final JAXBContext jaxbCtx = JAXBContext.newInstance(entityMappings.getClass());
            Marshaller m = jaxbCtx.createMarshaller();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.marshal(entityMappings, baos);
            return baos.toString();
        } catch (JAXBException e) {
            return "";
        }
    }

    public byte[] getEntityMappingsAsXMLBytes() {
        try {
            final JAXBContext jaxbCtx = JAXBContext.newInstance(entityMappings.getClass());
            Marshaller m = jaxbCtx.createMarshaller();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.marshal(entityMappings, baos);
            return baos.toByteArray();
        } catch (JAXBException e) {
            return new byte[0];
        }
    }

    public String getVersion() {
        return entityMappings.getVersion();
    }
}
