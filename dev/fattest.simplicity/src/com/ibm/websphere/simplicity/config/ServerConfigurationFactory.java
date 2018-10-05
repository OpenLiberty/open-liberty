/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import com.ibm.websphere.simplicity.config.dsprops.Properties_derby_embedded;

import componenttest.topology.impl.LibertyFileManager;

/**
 * Reads server.xml into memory, writes changes back to server.xml
 *
 * @author Tim Burns
 *
 */
public class ServerConfigurationFactory {

    private static ServerConfigurationFactory INSTANCE;

    public static ServerConfigurationFactory getInstance() throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new ServerConfigurationFactory();
        }
        return INSTANCE;
    }

    public static ServerConfiguration fromFile(File file) throws Exception {
        return getInstance().unmarshal(new FileInputStream(file));
    }

    public static void toFile(File targetFile, ServerConfiguration config) throws Exception {
        // write contents to a temporary file
        RemoteFile originalFile = LibertyFileManager.createRemoteFile(Machine.getLocalMachine(), targetFile.getAbsolutePath());
        RemoteFile newServerFile = LibertyFileManager.createRemoteFile(Machine.getLocalMachine(), targetFile.getAbsolutePath() + ".tmp");
        OutputStream os = newServerFile.openForWriting(false);
        ServerConfigurationFactory.getInstance().marshal(config, os);

        if (newServerFile.length() == originalFile.length()) {
            config.setDescription(config.getDescription() + " (this is some random text to make the file size bigger)");
            os = newServerFile.openForWriting(false);
            ServerConfigurationFactory.getInstance().marshal(config, os);
        }

        // replace the file
        // This logic does not need to be time protected (as we do in method
        // replaceServerConfiguration) because of the "extra random text" logic
        // above. Even if the timestamp would not be changed, the size out be.
        LibertyFileManager.moveLibertyFile(newServerFile, originalFile);
    }

    private final Marshaller marshaller;
    private final Unmarshaller unmarshaller;

    private ServerConfigurationFactory() throws Exception {
        JAXBContext context = JAXBContext.newInstance(ServerConfiguration.class);
        this.marshaller = context.createMarshaller();
        this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        this.unmarshaller = context.createUnmarshaller();
    }

    /**
     * Expresses a server configuration in an XML document.
     *
     * @param sourceConfig
     *            the configuration you want to marshal
     * @param targetFile
     *            the location where you want to marshal state information.
     *            parent directories will be created automatically
     * @throws Exception
     *             if generation fails
     */
    public void marshal(ServerConfiguration sourceConfig, File targetFile) throws Exception {
        if (targetFile == null) {
            return; // if the target location is null, there's nothing to do
        }
        targetFile.getParentFile().mkdirs();
        this.marshal(sourceConfig, new FileOutputStream(targetFile));
    }

    /**
     * Expresses a server configuration in an XML document.
     *
     * @param sourceConfig
     *            the configuration you want to marshal
     * @param outputStream
     *            the stream where you want to marshal state information. the
     *            stream will be closed before this method returns.
     * @throws Exception
     *             if generation fails
     */
    public void marshal(ServerConfiguration sourceConfig, OutputStream outputStream) throws Exception {
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
     * Converts a server configuration XML file into a series of Java objects.
     *
     * @param inputStream
     *            a server configuration XML file as a stream
     * @return a Java object representation of the server configuration, or null
     *         if the input stream is null
     * @throws Exception
     *             if the XML can't be parsed
     */
    public ServerConfiguration unmarshal(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            return null; // nothing to unmarshall
        }
        try {
            return (ServerConfiguration) this.unmarshaller.unmarshal(inputStream);
        } finally {
            inputStream.close();
        }
    }

    /**
     * Tests server configuration factory; prints server.xml for easy debug
     *
     * @param args
     *            nothing
     * @throws Exception
     *             if any test fails
     */
    public static void main(String[] args) throws Exception {
        File serverConfig = File.createTempFile("server", ".xml");
        System.out.println("Created temporary file: " + serverConfig);
        if (!serverConfig.exists()) {
            throw new Exception("Failed to create tmp file");
        }

        Integer expectedInvalidationTimeout = Integer.valueOf(120);
        String expectedCreateDatabase = "create";

        ServerConfiguration server = new ServerConfiguration();
        server.getFeatureManager().getFeatures().add(FeatureManager.FEATURE_SERVLET_3_0);
        server.getFeatureManager().getFeatures().add(FeatureManager.FEATURE_JSP_2_2);
        server.getFeatureManager().getFeatures().add(FeatureManager.FEATURE_SESSION_DATABASE_1_0);
        server.getFeatureManager().getFeatures().add(FeatureManager.FEATURE_APP_SECURITY_1_0);
        HttpEndpoint httpEndpoint = new HttpEndpoint();
        httpEndpoint.setId("defaultHttpEndpoint");
        httpEndpoint.setHost("*");
        httpEndpoint.setHttpPort("9080");
        httpEndpoint.setHttpsPort("9443");
        httpEndpoint.getTcpOptions().setSoReuseAddr(Boolean.TRUE);
        server.getHttpEndpoints().add(httpEndpoint);
        Fileset fileset = server.getFilesets().getOrCreateById("DerbyFileset", Fileset.class);
        fileset.setDir("${shared.resource.dir}/derby");
        fileset.setIncludes("derby.jar");
        Library library = server.getLibraries().getOrCreateById("DerbyLib", Library.class);
        library.setFileset(fileset);
        JdbcDriver jdbcDriver = server.getJdbcDrivers().getOrCreateById("DerbyEmbedded", JdbcDriver.class);
        jdbcDriver.setLibrary(library);
        DataSource dataSource = server.getDataSources().getOrCreateById("SessionDS", DataSource.class);
        dataSource.setJndiName("jdbc/Sessions");
        dataSource.setJdbcDriverRef(jdbcDriver.getId());
        Properties_derby_embedded dataSourceProperties = new Properties_derby_embedded();
        dataSourceProperties.setDatabaseName("${shared.resource.dir}/data/sessionDB");
        dataSourceProperties.setUser("user1");
        dataSourceProperties.setPassword("password");
        dataSourceProperties.setCreateDatabase(expectedCreateDatabase);
        dataSource.getProperties_derby_embedded().add(dataSourceProperties);
        HttpSessionDatabase sessionDB = server.getHttpSessionDatabase();
        sessionDB.setId("SessionDB");
        sessionDB.setDataSource(dataSource);
        sessionDB.setUseMultiRowSchema(Boolean.TRUE);
        HttpSession smgr = server.getHttpSession();
        smgr.setInvalidationTimeout(expectedInvalidationTimeout);
        smgr.setCookieDomain("\"\"");
        smgr.setStorage(sessionDB);
        server.getLogging().setTraceSpecification("*=info=enabled:com.ibm.ws.session*=all=enabled");
        BasicRegistry registry = server.getBasicRegistries().getOrCreateById("BasicRealm", BasicRegistry.class);
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
        server.getApplications().add(application);

        ServerConfigurationFactory scf = ServerConfigurationFactory.getInstance();
        scf.marshal(server, serverConfig);
        scf.marshaller.marshal(server, System.out); // call private variable to avoid calling System.out!

        ServerConfiguration unmarshalled = scf.unmarshal(new FileInputStream(serverConfig));
        scf.marshaller.marshal(unmarshalled, System.out); // call private variable to avoid calling System.out!
        Integer actualInvalidationTimeout = unmarshalled.getHttpSession().getInvalidationTimeout();
        String actualCreateDatabase = unmarshalled.getDataSources().get(0).getProperties_derby_embedded().get(0).getCreateDatabase();
        if (!expectedInvalidationTimeout.equals(actualInvalidationTimeout)) {
            throw new Exception("Expected invalidation timeout does not match actual invalidation timeout.  Expected: " + expectedInvalidationTimeout + " Actual: "
                                + actualInvalidationTimeout);
        }
        if (!expectedCreateDatabase.equals(actualCreateDatabase)) {
            throw new Exception("Expected data source property does not match actual invalidation timeout.  Expected: " + expectedCreateDatabase + " Actual: "
                                + actualCreateDatabase);
        }
        if (!serverConfig.delete()) {
            throw new Exception("Failed to delete tmp file");
        }
        System.out.println("Deleted " + serverConfig);
        System.out.println("All tests passed");
    }

}
