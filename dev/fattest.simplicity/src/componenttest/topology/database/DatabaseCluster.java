/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.database;

import static componenttest.common.apiservices.BootstrapProperty.DB_VENDORNAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.common.apiservices.Bootstrap;
import componenttest.common.apiservices.BootstrapProperty;
import componenttest.exception.UnavailableDatabaseException;
import componenttest.topology.impl.LibertyServer;

/**
 * A collection of Databases
 */
public class DatabaseCluster {
    private final Class<?> c = DatabaseCluster.class;
    public final Bootstrap b;
    public final String testBucketPath;

    private BootstrapProperty dbType;
    private final List<Database> databases = new ArrayList<Database>();
    private Database dbInUse;

    public static String[] getAllDbProps(String dir) {
        File dirFile = new File(dir);
        return dirFile.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".dbprops");
            }
        });
    }

    private static Properties[] toPropsArr(String... dbPropsArr) throws IOException {
        Properties[] propsArr = new Properties[dbPropsArr.length];
        for (int i = 0; i < dbPropsArr.length; i++) {
            InputStream is = new FileInputStream(dbPropsArr[i]);
            Properties dbProps = new Properties();
            dbProps.load(is);
            is.close();
            propsArr[i] = dbProps;
        }
        return propsArr;
    }

    private static Properties[] toPropsArr(String pathToProps, String... dbPropNames) throws IOException {
        String[] dbPropsArr = new String[dbPropNames.length];
        for (int i = 0; i < dbPropNames.length; i++)
            dbPropsArr[i] = pathToProps + '/' + dbPropNames[i];
        return toPropsArr(dbPropsArr);
    }

    public DatabaseCluster() throws Exception {
        this(getAllDbProps(System.getProperty("user.dir")));
    }

    /**
     * Will use Bootstrap.getInstance() to obtain an instance of bootstrapping.properties.
     * Will also assume that the test bucket path is the current working dir (i.e. user.dir)
     *
     * @param dbPropsArr A varargs array file names to use for the database properties
     */
    public DatabaseCluster(String... dbPropsArr) throws Exception {
        this(toPropsArr(dbPropsArr));
    }

    /**
     * Will use Bootstrap.getInstance() to obtain an instance of bootstrapping.properties.
     * Will also assume that the test bucket path is the current working dir (i.e. user.dir)
     *
     * @param dbPropsArr A varargs array of java.util.Properties with database info
     */
    public DatabaseCluster(Properties... dbPropsArr) throws Exception {
        this(Bootstrap.getInstance(), System.getProperty("user.dir"), dbPropsArr);
    }

    public DatabaseCluster(Bootstrap bootstrap, String testBucketPath) throws Exception {
        this(bootstrap, testBucketPath, toPropsArr(testBucketPath, getAllDbProps(testBucketPath)));
    }

    public DatabaseCluster(Bootstrap bootstrap, String testBucketPath, String dbPropsPath) throws Exception {
        this(bootstrap, testBucketPath, toPropsArr(dbPropsPath, getAllDbProps(dbPropsPath)));
    }

    /**
     * @param bootstrap The instance of a bootstrapping.properties file to use
     * @param testBucketPath The location of the test bucket using this cluster
     * @param dbPropsArr A varargs array of java.util.Properties with database info
     */
    public DatabaseCluster(Bootstrap bootstrap, String testBucketPath, Properties... dbPropsArr) throws Exception {
        this.b = bootstrap;
        this.testBucketPath = testBucketPath;

        Log.info(c, "<init>", "bootstrap, testBucketPath, [dbPropsArr]",
                 new Object[] { bootstrap, testBucketPath, Arrays.toString(dbPropsArr) });

        for (Properties dbProps : dbPropsArr)
            addMachine(dbProps);

        dbInUse = getInUseDatabase();
    }

    public boolean addMachine(Properties dbProps) throws Exception {
        final String method = "addMachine";

        BootstrapProperty dbType = BootstrapProperty.fromPropertyName(dbProps.getProperty(DB_VENDORNAME.toString()));

        if (dbType == null)
            throw new IllegalArgumentException("Invalid database.vendorname specified: " + dbProps.getProperty(DB_VENDORNAME.toString()));
        else if (this.dbType == null)
            this.dbType = dbType;
        else if (this.dbType != dbType)
            throw new IllegalArgumentException("Unable to add a database of type " + dbType + " to a cluster of databases with type " + this.dbType);

        try {
            Database db;
            switch (dbType) {
                case DB_DB2:
                    db = new DB2Database(b, dbProps, testBucketPath);
                    break;
                case DB_INFORMIX:
                    db = new InformixDatabase(b, dbProps, testBucketPath);
                    break;
                case DB_ORACLE:
                    db = new OracleDatabase(b, dbProps, testBucketPath);
                    break;
                case DB_SQLSERVER:
                    db = new SQLServerDatabase(b, dbProps, testBucketPath);
                    break;
                case DB_SYBASE:
                    db = new SybaseDatabase(b, dbProps, testBucketPath);
                    break;
                case DB_CLOUDANT:
                    db = new CloudantDatabase(b, dbProps, testBucketPath);
                    break;
                default:
                    throw new IllegalArgumentException("database.vendorname " + dbType + " is not spelled correctly or is not yet supported.");
            }
            Log.info(c, method, "Adding database " + db + " to the cluster.");
            return databases.add(db);
        } catch (UnavailableDatabaseException unavailable) {
            return false;
        }
    }

    public void createDatabase() throws Exception {
        if (dbInUse != null)
            throw new IllegalStateException("Cannot call createDatabase more than once per cluster instance.");

        // Randomize the database ordering in attempt to distribute the load
        Collections.shuffle(databases);

        Exception last = null;
        for (Database db : databases)
            try {
                db.createDatabase();
                dbInUse = db;
                return;
            } catch (Exception e) {
                last = e;
                Log.warning(c, "Failed to create a database for " + db + ".  Removing this machine from the cluster.");
            }
        throw new UnavailableDatabaseException("Unable to create a database on any of the servers in the Database cluster." +
                                               " Servers attempted were: " + databases, last);
    }

    public void runDDL() throws Exception {
        checkInUse();
        dbInUse.runDDL();
    }

    public void dropDatabase() throws Exception {
        checkInUse();
        dbInUse.dropDatabase();
    }

    private void checkInUse() {
        if (dbInUse == null)
            throw new IllegalStateException("There is no in-use database for this cluster instance, first call createDatabase()");
    }

    public void addConfigTo(LibertyServer server) throws Exception {
        checkInUse();
        getInUseDatabase().addConfigTo(server);
    }

    public Database getInUseDatabase() throws Exception {
        if (dbInUse != null)
            return dbInUse;

        // When a database is 'in use' it will update the bootstrapping.properties file
        // with a its own hostname.  Match the hostname to one of the machines in the cluster
        String targetHost = b.getValue(BootstrapProperty.DB_HOSTNAME.toString());

        if (targetHost == null)
            return null; // there is no DB in use yet

        for (Database db : databases)
            if (targetHost.equalsIgnoreCase(db.dbhostname)) {
                Log.info(c, "getInUseDatabase", "Marking DB in use: " + db);
                return db;
            }
        return null;
    }
}
