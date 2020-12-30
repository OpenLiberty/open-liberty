/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.database.container.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.DataSource;
import com.ibm.websphere.simplicity.config.DataSourceProperties;
import com.ibm.websphere.simplicity.config.DatabaseStore;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.config.dsprops.Properties;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.database.container.DatabaseContainerType;
import componenttest.topology.impl.LibertyServer;

/**
 * Server specific liberty config utilities used by the fattest.database project
 */
public class LibertyServerUtil {
    //Logging Constants
    private static final Class<LibertyServerUtil> c = LibertyServerUtil.class;
    
    //No objects should be created from this class
    private LibertyServerUtil() {}
    
    /**
     * For use when attempting to use <b>database rotation</b>. <br>
     *
     * Retrieves database specific properties from the provided JdbcDatabaseContainer, such as;
     * username, password, etc. <br>
     *
     * Using the ServerConfiguration API. Retrieves all &lt;dataSource&gt; elements and modifies
     * those that have the <b>fat.modify=true</b> attribute set. <br>
     *
     * This will replace the datasource &lt;properties.derby.*... &gt; with the specific &lt;properties.db... &gt
     * for the provided JdbcDatabaseContainer. <br>
     *
     * @see com.ibm.websphere.simplicity.config.ServerConfiguration
     *
     * @param serv - LibertyServer server instance being used for this FAT suite.
     * @param cont - JdbcDatabaseContainer instance being used for database connectivity.
     *
     * @throws Exception
     */
    public static void setupDatabaseProperties(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws Exception {
    	//Get server config
    	ServerConfiguration cloneConfig = serv.getServerConfiguration().clone();
    	
    	try {
    		//Add server variables
    		addVariables(cloneConfig);
        	//Get datasources to be changed
        	List<DataSource> datasources = getDataSources(serv, cloneConfig);
        	//Modify those datasources
        	modify(datasources, cloneConfig, cont, false);    		
    	} finally {
            //Update config
            serv.updateServerConfiguration(cloneConfig);
    	}
    }
    
    /**
     * For use when attempting to use <b>database rotation</b>. <br>
     *
     * Retrieves database specific properties from the provided JdbcDatabaseContainer, such as;
     * username, password, etc. <br>
     *
     * Using the ServerConfiguration API. Retrieves all &lt;dataSource&gt; elements and modifies
     * those that have the <b>fat.modify=true</b> attribute set. <br>
     *
     * This will replace the datasource &lt;properties.derby.*... &gt; with the generic &lt;properties... &gt;
     * for the provided JdbcDatabaseContainer. <br>
     *
     * @see com.ibm.websphere.simplicity.config.ServerConfiguration
     *
     * @param serv - LibertyServer server instance being used for this FAT suite.
     * @param cont - JdbcDatabaseContainer instance being used for database connectivity.
     *
     * @throws Exception
     */
    public static void setupGenericProperties(LibertyServer serv, JdbcDatabaseContainer<?> cont) throws CloneNotSupportedException, Exception {
    	//Get server config
    	ServerConfiguration cloneConfig = serv.getServerConfiguration().clone();
    	
    	try {
    		//Add server variables
    		addVariables(cloneConfig);
        	//Get datasources to be changed
        	List<DataSource> datasources = getDataSources(serv, cloneConfig);
        	//Modify those datasources
        	modify(datasources, cloneConfig, cont, true);    		
    	} finally {
            //Update config
            serv.updateServerConfiguration(cloneConfig);
    	}
    }
    
    /*
     * Helper method to add variables to server config
     */
    private static void addVariables(ServerConfiguration cloneConfig) {
    	ConfigElementList<Variable> vars = cloneConfig.getVariables();
    	List<Variable> configVars = LibertyConfigUtil.getConfigVariables();
    	Log.info(c, "addVariables", "Adding variables to server config: " + configVars); 
    	
    	for(Variable var : configVars) {  
    		vars.add(var);
    	}
    	LibertyConfigUtil.clearVariables();
    }
    
    /*
     * Helper method to get a list of datasources from server that need to be updated
     */
    private static List<DataSource> getDataSources(LibertyServer serv, ServerConfiguration cloneConfig) {
        //Get a list of datasources that need to be updated
        List<DataSource> datasources = new ArrayList<>();    
        
        //Get general datasources
        for (DataSource ds : cloneConfig.getDataSources())
            if (ds.getFatModify() != null && ds.getFatModify().equals("true"))
                datasources.add(ds);

        //Get datasources that are nested under databasestores
        for (DatabaseStore dbs : cloneConfig.getDatabaseStores())
            for (DataSource ds : dbs.getDataSources())
                if (ds.getFatModify() != null && ds.getFatModify().equals("true"))
                    datasources.add(ds);
        
        return datasources;
    }
    
    /*
     * Modifies server config
     */
    private static void modify(List<DataSource> datasources, ServerConfiguration cloneConfig,
                                              JdbcDatabaseContainer<?> cont, boolean generic) throws Exception {
    	//Skip if modification is unnecessary 
    	if (!LibertyConfigUtil.needsModification(cont))
    		return;
    	
    	//Get database type
    	DatabaseContainerType type = DatabaseContainerType.valueOf(cont);

        //Create general properties
        DataSourceProperties props = generic ? new Properties() : type.getDataSourceProps();
        props.setUser(cont.getUsername());
        props.setPassword(cont.getPassword());
        props.setServerName(cont.getContainerIpAddress());
    	props.setPortNumber(Integer.toString(cont.getFirstMappedPort()));
    	try { props.setDatabaseName(cont.getDatabaseName()); } catch (UnsupportedOperationException e) {}
        

        //TODO this should not be required even when using general datasource properties
        // investigating here: https://github.com/OpenLiberty/open-liberty/issues/10066
        if (type.equals(DatabaseContainerType.DB2) && generic) {
            props.setExtraAttribute("driverType", "4");
        }
        
        if (type.equals(DatabaseContainerType.Oracle)) {
        	Class<?> clazz = type.getContainerClass();
        	Method getSid = clazz.getMethod("getSid");
        	props.setDatabaseName((String) getSid.invoke(cont));
        	if (generic)
        		props.setExtraAttribute("driverType", "thin");
        }
        
        Log.info(c, "modify", "DataSources to be enlisted in database rotation.  ID: " + datasources);   
    	
    	for(DataSource ds : datasources) {         
            //Replace derby properties
            ds.replaceDatasourceProperties(props);	
    	}
    }

}
