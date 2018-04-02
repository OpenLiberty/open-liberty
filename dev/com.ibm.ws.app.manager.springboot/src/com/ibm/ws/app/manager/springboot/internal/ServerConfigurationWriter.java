/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.internal;

import java.io.IOException;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.ibm.ws.app.manager.springboot.container.config.ServerConfiguration;

/**
 * Reads server.xml into memory, writes changes back to server.xml
 *
 */
public class ServerConfigurationWriter {

    private static ServerConfigurationWriter INSTANCE;

    public static ServerConfigurationWriter getInstance() throws JAXBException {
        if (INSTANCE == null) {
            INSTANCE = new ServerConfigurationWriter();
        }
        return INSTANCE;
    }

    private final Marshaller marshaller;

    private ServerConfigurationWriter() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(ServerConfiguration.class);
        this.marshaller = context.createMarshaller();
        this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
    }

    /**
     * Expresses a server configuration in an XML document.
     *
     * @param sourceConfig
     *            the configuration you want to marshal
     * @param outputStream
     *            the stream where you want to marshal state information. the
     *            stream will be closed before this method returns.
     * @throws JAXBException on failure
     * @throws IOException on IO failure
     *
     */
    public void write(ServerConfiguration sourceConfig, Writer writer) throws JAXBException, IOException {
        if (writer == null) {
            return; // if the outputStream is null, there's nothing to do
        }
        try {
            this.marshaller.marshal(sourceConfig, writer);
        } finally {
            writer.close();
        }
    }

}
