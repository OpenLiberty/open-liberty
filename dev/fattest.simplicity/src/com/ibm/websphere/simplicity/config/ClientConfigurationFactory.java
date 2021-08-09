/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;

import componenttest.topology.impl.LibertyFileManager;

/**
 * Reads client.xml into memory, writes changes back to client.xml
 *
 * @author Tim Burns
 *
 */
public class ClientConfigurationFactory {

    private static ClientConfigurationFactory INSTANCE;

    public static ClientConfigurationFactory getInstance() throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new ClientConfigurationFactory();
        }
        return INSTANCE;
    }

    public static ClientConfiguration fromFile(File file) throws Exception {
        return getInstance().unmarshal(new FileInputStream(file));
    }

    public static void toFile(File targetFile, ClientConfiguration config) throws Exception {
        // write contents to a temporary file
        RemoteFile originalFile = LibertyFileManager.createRemoteFile(Machine.getLocalMachine(), targetFile.getAbsolutePath());
        RemoteFile newServerFile = LibertyFileManager.createRemoteFile(Machine.getLocalMachine(), targetFile.getAbsolutePath() + ".tmp");
        OutputStream os = newServerFile.openForWriting(false);
        ClientConfigurationFactory.getInstance().marshal(config, os);

        if (newServerFile.length() == originalFile.length()) {
            config.setDescription(config.getDescription() + " (this is some random text to make the file size bigger)");
            os = newServerFile.openForWriting(false);
            ClientConfigurationFactory.getInstance().marshal(config, os);
        }

        // replace the file
        // This logic does not need to be time protected (as we do in method
        // replaceServerConfiguration) because of the "extra random text" logic
        // above. Even if the timestamp would not be changed, the size out be.
        LibertyFileManager.moveLibertyFile(newServerFile, originalFile);
    }

    private final Marshaller marshaller;
    private final Unmarshaller unmarshaller;

    private ClientConfigurationFactory() throws Exception {
        JAXBContext context = JAXBContext.newInstance(ClientConfiguration.class);
        this.marshaller = context.createMarshaller();
        this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        this.unmarshaller = context.createUnmarshaller();
    }

    /**
     * Expresses a client configuration in an XML document.
     *
     * @param sourceConfig
     *            the configuration you want to marshal
     * @param targetFile
     *            the location where you want to marshal state information.
     *            parent directories will be created automatically
     * @throws Exception
     *             if generation fails
     */
    public void marshal(ClientConfiguration sourceConfig, File targetFile) throws Exception {
        if (targetFile == null) {
            return; // if the target location is null, there's nothing to do
        }
        targetFile.getParentFile().mkdirs();
        this.marshal(sourceConfig, new FileOutputStream(targetFile));
    }

    /**
     * Expresses a client configuration in an XML document.
     *
     * @param sourceConfig
     *            the configuration you want to marshal
     * @param outputStream
     *            the stream where you want to marshal state information. the
     *            stream will be closed before this method returns.
     * @throws Exception
     *             if generation fails
     */
    public void marshal(ClientConfiguration sourceConfig, OutputStream outputStream) throws Exception {
        if (outputStream == null) {
            return; // if the outputStream is null, there's nothing to do
        }
        try {
            this.marshaller.marshal(sourceConfig, outputStream);
        } finally {
            outputStream.close();
        }
    }

    /**
     * Converts a client configuration XML file into a series of Java objects.
     *
     * @param inputStream
     *            a client configuration XML file as a stream
     * @return a Java object representation of the client configuration, or null
     *         if the input stream is null
     * @throws Exception
     *             if the XML can't be parsed
     */
    public ClientConfiguration unmarshal(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return null; // nothing to unmarshall
        }
        try {
            return (ClientConfiguration) this.unmarshaller.unmarshal(inputStream);
        } finally {
            inputStream.close();
        }
    }

    /**
     * Tests client configuration factory; prints client.xml for easy debug
     *
     * @param args
     *            nothing
     * @throws Exception
     *             if any test fails
     */
    public static void main(String[] args) throws Exception {
        File clientConfig = File.createTempFile("client", ".xml");
        System.out.println("Created temporary file: " + clientConfig);
        if (!clientConfig.exists()) {
            throw new Exception("Failed to create tmp file");
        }

        ClientConfiguration client = new ClientConfiguration();
        client.getLogging().setTraceSpecification("*=info=enabled:com.ibm.ws.session*=all=enabled");
        BasicRegistry registry = client.getBasicRegistries().getOrCreateById("BasicRealm", BasicRegistry.class);
        registry.setRealm(registry.getId());
        BasicRegistry.User user1 = registry.getUsers().getOrCreateById("user1", BasicRegistry.User.class);
        user1.setName(user1.getId());
        user1.setPassword("security");
        Application application = new Application();
        application.setId("appName");
        application.setName(application.getId());
        application.setType("ear");
        application.setLocation("C:/some/path/on/my/machine/app.ear");
        SecurityRole role = application.getApplicationBnd().getSecurityRoles().getOrCreateById("securityRole", SecurityRole.class);
        role.setName(role.getId());
        SpecialSubject specialSubject = role.getSpecialSubjects().getOrCreateById("specialSubject", SpecialSubject.class);
        specialSubject.set(SpecialSubject.Type.ALL_AUTHENTICATED_USERS);
        client.getApplications().add(application);

        ClientConfigurationFactory scf = ClientConfigurationFactory.getInstance();
        scf.marshal(client, clientConfig);
        scf.marshaller.marshal(client, System.out); // call private variable to avoid calling System.out!

        ClientConfiguration unmarshalled = scf.unmarshal(new FileInputStream(clientConfig));
        scf.marshaller.marshal(unmarshalled, System.out); // call private variable to avoid calling System.out!
        if (!clientConfig.delete()) {
            throw new Exception("Failed to delete tmp file");
        }
        System.out.println("Deleted " + clientConfig);
        System.out.println("All tests passed");
    }

}
