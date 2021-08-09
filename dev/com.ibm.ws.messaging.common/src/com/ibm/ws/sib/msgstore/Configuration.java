/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.msgstore;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Enumeration;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.utils.PasswordSuppressingProperties;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Holds the configuration information needed by the Message Store. Objects of
 * this class are only used directly when the message store is run outside of
 * WAS.
 *
 * @author kschloss
 * @author pradine
 * @see com.ibm.ws.sib.msgstore.WASConfiguration
 */
public class Configuration implements FFDCSelfIntrospectable
{
    private static TraceComponent tc = SibTr.register(Configuration.class, MessageStoreConstants.MSG_GROUP, MessageStoreConstants.MSG_BUNDLE);

    /**
     * The property file that contains an alternative message store configuration.
     */
    public static final String PROPERTY_FILE = "MessageStore.properties";
    public static final int POOLED_CONNECTIONS = 3;

    protected String persistentMessageStoreClassname = MessageStoreConstants.PROP_PERSISTENT_MESSAGE_STORE_CLASS_DEFAULT;
    protected String objectManagerLogDirectory = MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_PREFIX_DEFAULT;
    protected long objectManagerLogSize = Long.parseLong(MessageStoreConstants.PROP_OBJECT_MANAGER_LOG_FILE_SIZE_DEFAULT);

    protected String objectManagerPermanentStoreDirectory = MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_PREFIX_DEFAULT;
    protected long objectManagerMinimumPermanentStoreSize = Long.parseLong(MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MINIMUM_SIZE_DEFAULT);
    protected long objectManagerMaximumPermanentStoreSize = Long.parseLong(MessageStoreConstants.PROP_OBJECT_MANAGER_PERMANENT_STORE_FILE_MAXIMUM_SIZE_DEFAULT);
    protected boolean objectManagerPermanentStoreSizeUnlimited = false;

    protected String objectManagerTemporaryStoreDirectory = MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_PREFIX_DEFAULT;
    protected long objectManagerMinimumTemporaryStoreSize = Long.parseLong(MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_MINIMUM_SIZE_DEFAULT);
    protected long objectManagerMaximumTemporaryStoreSize = Long.parseLong(MessageStoreConstants.PROP_OBJECT_MANAGER_TEMPORARY_STORE_FILE_MAXIMUM_SIZE_DEFAULT);
    protected boolean objectManagerTemporaryStoreSizeUnlimited = false;

    protected String datasourceClassname = null;
    protected String datasourceUsername = null;
    protected String datasourcePassword = null;
    protected Properties datasourceProperties = new PasswordSuppressingProperties();
    protected String schemaName = MessageStoreConstants.DEFAULT_SCHEMA_NAME;

   
    protected int numberOfPooledConnections = POOLED_CONNECTIONS;
    protected int numberOfPermanentTables = MessageStoreConstants.NUMBER_OF_PERMANENT_TABLES;
    protected int numberOfTemporaryTables = MessageStoreConstants.NUMBER_OF_TEMPORARY_TABLES;
    protected boolean createTablesAutomatically = true;
    protected boolean cleanPersistenceOnStart = false;
    protected boolean verbose = false;

    /**
     * Constructor
     *
     */
    protected Configuration()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.entry(tc, "<ctor>()");
            SibTr.exit(tc, "<ctor>()", this);
        }
    }

    /**
     * Create a new Configuration object. This object will have either the default
     * configuration or the configuration specified in {@link #PROPERTY_FILE}
     *
     * @return The new Configuration object.
     */
    public static Configuration createBasicConfiguration()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createBasicConfiguration()");

        Configuration config = new Configuration();

        try
        {
            config.loadFromPropertyFile();
        }
        catch (IOException ioe)
        {
            //No FFDC Code Needed.
            config.datasourceClassname = "org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource";
            config.datasourceProperties.setProperty("databaseName",System.getProperty("js.test.dbname","msdb"));
            config.datasourceProperties.setProperty("createDatabase","create");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createBasicConfiguration()", config);

        return(config);
    }

    /**
     * Loads defaults from {@link #PROPERTY_FILE}. This method should only be
     * called outside of WAS therefore no nls support should be required
     *
     * @throws IOException
     */
    public void loadFromPropertyFile() throws IOException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "loadFromPropertyFile()");

        String key        = null;
        Properties config = new Properties();
        InputStream is    = this.getClass().getResourceAsStream(PROPERTY_FILE);

        if (is == null) {
            throw new IOException("Resource " + PROPERTY_FILE + " not found in current classloader");
        }

        config.load(is);

        Enumeration vEnum = config.propertyNames();

        while (vEnum.hasMoreElements())
        {
            key = (String) vEnum.nextElement();

            if (key.equals("datasourceClassname"))
                datasourceClassname = config.getProperty(key);
            else if (key.equals("username"))
                datasourceUsername  = config.getProperty(key);
            else if (key.equals("password"))
                datasourcePassword  = config.getProperty(key);
            else if (key.equals("schemaname"))
                schemaName = config.getProperty(key);
            else if (key.equals("poolSize"))
                numberOfPooledConnections = Integer.parseInt(config.getProperty(key));
            else if (key.equals("verbose"))
                verbose = Boolean.valueOf(config.getProperty(key)).booleanValue();
            else
                datasourceProperties.setProperty(key, config.getProperty(key));
        }

        if (datasourceClassname == null)
            throw new IllegalStateException("The properties file must specify datasourceClassname");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "loadFromPropertyFile()");
    }

    /**
     * Returns the class name of the persistence layer
     *
     * @return the class name of the persistence layer
     */
    public String getPersistentMessageStoreClassname()
    {
        // No trace required
        return persistentMessageStoreClassname;
    }

    /**
     * Sets the class name of the persistence layer.
     *
     * @param persistentMessageStoreClassname the class name of the persistence layer
     */
    public void setPersistentMessageStoreClassname(String persistentMessageStoreClassname)
    {
        //No trace required
        this.persistentMessageStoreClassname = persistentMessageStoreClassname;
    }

    /**
     * Returns the directory prefix of the object manager log file
     *
     * @return the directory prefix of the object manager log file
     */
    public String getObjectManagerLogDirectory()
    {
        // No trace required
        return objectManagerLogDirectory;
    }

    /**
     * Sets the the directory prefix of the object manager log file
     *
     * @param objectManagerLogDirectory the directory prefix of the object manager log file
     */
    public void setObjectManagerLogDirectory(String objectManagerLogDirectory)
    {
        //No trace required
        this.objectManagerLogDirectory = objectManagerLogDirectory;
    }

    /**
     * Returns the size of the object manager log file
     *
     * @return the size of the object manager log file
     */
    public long getObjectManagerLogSize()
    {
        // No trace required
        return objectManagerLogSize;
    }

    /**
     * Sets the size of the object manager log file
     *
     * @param objectManagerLogSize the size of the object manager log file
     */
    public void setObjectManagerLogSize(long objectManagerLogSize)
    {
        //No trace required
        this.objectManagerLogSize = objectManagerLogSize;
    }

    /**
     * Returns the directory prefix of the object manager store files
     *
     * @return the directory prefix of the object manager store files
     */
    public String getObjectManagerPermanentStoreDirectory()
    {
        // No trace required
        return objectManagerPermanentStoreDirectory;
    }

    /**
     * Sets the directory prefix of the object manager store files
     *
     * @param objectManagerStoreDirectory the directory prefix of the object manager store files
     */
    public void setObjectManagerPermanentStoreDirectory(String objectManagerPermanentStoreDirectory)
    {
        //No trace required
        this.objectManagerPermanentStoreDirectory = objectManagerPermanentStoreDirectory;
    }

    /**
     * Returns the minimum reserved size of the object manager store files
     *
     * @return the minimum reserved size of the object manager store files
     */
    public long getObjectManagerMinimumPermanentStoreSize()
    {
        // No trace required
        return objectManagerMinimumPermanentStoreSize;
    }

    /**
     * Sets the minimum reserved size of the object manager store files
     *
     * @param objectManagerMinimumStoreSize the size of the object manager store files
     */
    public void setObjectManagerMinimumPermanentStoreSize(long objectManagerMinimumPermanentStoreSize)
    {
        //No trace required
        this.objectManagerMinimumPermanentStoreSize = objectManagerMinimumPermanentStoreSize;
    }

    /**
     * Returns the maximum size of the object manager store files
     *
     * @return the maximum size of the object manager store files
     */
    public long getObjectManagerMaximumPermanentStoreSize()
    {
        // No trace required
        return objectManagerMaximumPermanentStoreSize;
    }

    /**
     * Sets the maximum size of the object manager store files
     *
     * @param objectManagerMaximumStoreSize the maximum size of the object manager store files
     */
    public void setObjectManagerMaximumPermanentStoreSize(long objectManagerMaximumPermanentStoreSize)
    {
        //No trace required
        this.objectManagerMaximumPermanentStoreSize = objectManagerMaximumPermanentStoreSize;
    }

    /**
     * Replies whether the object manager store files have unlimited size
     *
     * @return whether the object manager store files have unlimited size
     */
    public boolean isObjectManagerPermanentStoreSizeUnlimited()
    {
        // No trace required
        return objectManagerPermanentStoreSizeUnlimited;
    }

    /**
     * Sets whether the object manager store files have unlimited size
     *
     * @param objectManagerStoreSizeUnlimited whether the object manager store files have unlimited size
     */
    public void setObjectManagerPermanentStoreSizeUnlimited(boolean objectManagerPermanentStoreSizeUnlimited)
    {
        //No trace required
        this.objectManagerPermanentStoreSizeUnlimited = objectManagerPermanentStoreSizeUnlimited;
    }

    /**
     * Returns the directory prefix of the object manager store files
     *
     * @return the directory prefix of the object manager store files
     */
    public String getObjectManagerTemporaryStoreDirectory()
    {
        // No trace required
        return objectManagerTemporaryStoreDirectory;
    }

    /**
     * Sets the directory prefix of the object manager store files
     *
     * @param objectManagerStoreDirectory the directory prefix of the object manager store files
     */
    public void setObjectManagerTemporaryStoreDirectory(String objectManagerTemporaryStoreDirectory)
    {
        //No trace required
        this.objectManagerTemporaryStoreDirectory = objectManagerTemporaryStoreDirectory;
    }

    /**
     * Returns the minimum reserved size of the object manager store files
     *
     * @return the minimum reserved size of the object manager store files
     */
    public long getObjectManagerMinimumTemporaryStoreSize()
    {
        // No trace required
        return objectManagerMinimumTemporaryStoreSize;
    }

    /**
     * Sets the minimum reserved size of the object manager store files
     *
     * @param objectManagerMinimumStoreSize the size of the object manager store files
     */
    public void setObjectManagerMinimumTemporaryStoreSize(long objectManagerMinimumTemporaryStoreSize)
    {
        //No trace required
        this.objectManagerMinimumTemporaryStoreSize = objectManagerMinimumTemporaryStoreSize;
    }

    /**
     * Returns the maximum size of the object manager store files
     *
     * @return the maximum size of the object manager store files
     */
    public long getObjectManagerMaximumTemporaryStoreSize()
    {
        // No trace required
        return objectManagerMaximumTemporaryStoreSize;
    }

    /**
     * Sets the maximum size of the object manager store files
     *
     * @param objectManagerMaximumStoreSize the maximum size of the object manager store files
     */
    public void setObjectManagerMaximumTemporaryStoreSize(long objectManagerMaximumTemporaryStoreSize)
    {
        //No trace required
        this.objectManagerMaximumTemporaryStoreSize = objectManagerMaximumTemporaryStoreSize;
    }

    /**
     * Replies whether the object manager store files have unlimited size
     *
     * @return whether the object manager store files have unlimited size
     */
    public boolean isObjectManagerTemporaryStoreSizeUnlimited()
    {
        // No trace required
        return objectManagerTemporaryStoreSizeUnlimited;
    }

    /**
     * Sets whether the object manager store files have unlimited size
     *
     * @param objectManagerStoreSizeUnlimited whether the object manager store files have unlimited size
     */
    public void setObjectManagerTemporaryStoreSizeUnlimited(boolean objectManagerTemporaryStoreSizeUnlimited)
    {
        //No trace required
        this.objectManagerTemporaryStoreSizeUnlimited = objectManagerTemporaryStoreSizeUnlimited;
    }

    /**
     * Returns the class name of the data source
     *
     * @return the class name of the data source
     */
    public String getDatasourceClassname()
    {
        //No trace required
        return datasourceClassname;
    }

    /**
     * Returns the password to use with the data source
     *
     * @return the password to use with the data source
     */
    public String getDatasourcePassword()
    {
        //No trace required
        return datasourcePassword;
    }

    /**
     * Returns the user name to use with the data source
     *
     * @return the user name to use with the data source
     */
    public String getDatasourceUsername()
    {
        //No trace required
        return datasourceUsername;
    }

    /**
     * Sets the data source class name.
     *
     * @param dataSourceClassname the name of the data source class
     */
    public void setDatasourceClassname(String dataSourceClassname)
    {
        //No trace required
        this.datasourceClassname = dataSourceClassname;
    }

    /**
     * Sets the data source password.
     *
     * @param dataSourcePassword to password to set
     */
    public void setDatasourcePassword(String dataSourcePassword)
    {
        //No trace required
        this.datasourcePassword = dataSourcePassword;
    }

    /**
     * Sets the data source user name
     *
     * @param dataSourceUsername the user name to set
     */
    public void setDatasourceUsername(String dataSourceUsername)
    {
        //No trace required
        this.datasourceUsername = dataSourceUsername;
    }

    /**
     * Returns the configuration properties for the message store
     *
     * @return the message store properties
     */
    public Properties getDatasourceProperties()
    {
        //No trace required
        return datasourceProperties;
    }

    /**
     * Indicates whether the message store will try to create the database tables,
     * if they do not already exist.
     *
     * @return <UL>
     *         <LI>true  - if the message store should attempt to create the tables</LI>
     *         <LI>false - otherwise</LI>
     *         </UL>
     */
    public boolean isCreateTablesAutomatically()
    {
        //No trace required
        return createTablesAutomatically;
    }

    /**
     * Set whether the message store should try to create the database tables,
     * if they do not alreay exist.
     *
     * @param createTablesAutomatically
     * @see #isCreateTablesAutomatically
     */
    public void setCreateTablesAutomatically(boolean createTablesAutomatically)
    {
        //No trace required
        this.createTablesAutomatically = createTablesAutomatically;
    }

    /**
     * Set the name of the database schema that holds the tables.
     *
     * @param schemaName the database schema name
     */
    public void setDatabaseSchemaName(String schemaName)
    {
        //No trace required
        this.schemaName = schemaName;
    }

    /**
     * Returns the database schema name that holds the tables
     *
     * @return the database schema name
     */
    public String getDatabaseSchemaName()
    {
        //No trace required
        return schemaName;
    }
  
    
    /**
     * Indicates whether the message store is to attempt to clear any existing
     * data from the tables.
     *
     * @return <UL>
     *         <LI>true  - if the message store is to attempt to clear existing data</LI>
     *         <LI>false - otherwise</LI>
     *         </UL>
     */
    public boolean isCleanPersistenceOnStart()
    {
        //No trace required
        return cleanPersistenceOnStart;
    }

    /**
     * Sets the cleanPersistenceOnStart.
     * @param cleanPersistenceOnStart The cleanPersistenceOnStart to set
     */
    public void setCleanPersistenceOnStart(boolean cleanPersistenceOnStart)
    {
        //No trace required
        this.cleanPersistenceOnStart = cleanPersistenceOnStart;
    }

    /**
     * Returns the number of connections to pool. Used only when running outside of
     * WAS
     *
     * @return the number of connections to pool
     */
    public int getNumberOfPooledConnections() {
        //No trace required
        return numberOfPooledConnections;
    }

    /**
     * Set the number of connections to pool.
     *
     * @param numberOfPooledConnections the number of connections to pool
     */
    public void setNumberOfPooledConnections(int numberOfPooledConnections) {
        //No trace required
        this.numberOfPooledConnections = numberOfPooledConnections;
    }

    /**
     * Returns the number of {@link com.ibm.ws.sib.msgstore.persistence.impl.ItemTable#TEMPORARY}
     * tables.
     *
     * @return
     */
    public int getNumberOfTemporaryTables() {
        //No trace require
        return numberOfTemporaryTables;
    }

    /**
     * Sets the number of {@link com.ibm.ws.sib.msgstore.persistence.impl.ItemTable#TEMPORARY}
     * tables.
     *
     * @param numberOfTemporaryTables
     */
    public void setNumberOfTemporaryTables(int numberOfTemporaryTables) {
        //No trace required
        this.numberOfTemporaryTables = numberOfTemporaryTables;
    }

    /**
     * Returns the number of {@link com.ibm.ws.sib.msgstore.persistence.impl.ItemTable#PERMANENT}
     * tables.
     *
     * @return
     */
    public int getNumberOfPermanentTables() {
        //No trace require
        return numberOfPermanentTables;
    }

    /**
     * Sets the number of {@link com.ibm.ws.sib.msgstore.persistence.impl.ItemTable#PERMANENT}
     * tables.
     *
     * @param numberOfPermanentTables
     */
    public void setNumberOfPermanentTables(int numberOfPermanentTables) {
        //No trace required
        this.numberOfPermanentTables = numberOfPermanentTables;
    }

    /**
     * Indicate that some additional trace, not WAS trace, is required.
     * Used only outside of WAS.
     *
     * @return turn on special trace.
     */
    public boolean verbose() {
        //No trace required
        return verbose;
    }

    public String toString() {
        return "Pers MS Class name: " + persistentMessageStoreClassname
               + ", OM Log directory: " + objectManagerLogDirectory
               + ", OM Log size: " + objectManagerLogSize
               + ", OM Permanent Store directory: " + objectManagerPermanentStoreDirectory
               + ", OM Permanent Store size minimum: " + objectManagerMinimumPermanentStoreSize
               + ", OM Permanent Store size maximum: " + objectManagerMaximumPermanentStoreSize
               + ", OM Permanent Store size unlimited: " + objectManagerPermanentStoreSizeUnlimited
               + ", OM Temporary Store directory: " + objectManagerTemporaryStoreDirectory
               + ", OM Temporary Store size minimum: " + objectManagerMinimumTemporaryStoreSize
               + ", OM Temporary Store size maximum: " + objectManagerMaximumTemporaryStoreSize
               + ", OM Temporary Store size unlimited: " + objectManagerTemporaryStoreSizeUnlimited
               + ", Data source class name: " + datasourceClassname
               + ", User name: " + datasourceUsername
               + ", Password: " + "**********"
               + ", Schema name: " + schemaName
               + ", Pool size: " + numberOfPooledConnections
               + ", Create tables: " + createTablesAutomatically
               + ", Clean start: " + cleanPersistenceOnStart
               + ", Properties: " + datasourceProperties
               + ", Permanent tables: " + numberOfPermanentTables
               + ", Temporary tables: " + numberOfTemporaryTables;
    }

    /**
     * Return the information in this object for FFDC
     *
     * @return an array of strings to be added to the ffdc log
     */
    public String[] introspectSelf() {
        return new String[] { toString() };
    }
}
