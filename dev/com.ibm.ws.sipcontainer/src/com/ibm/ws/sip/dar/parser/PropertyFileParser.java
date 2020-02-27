/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.dar.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.sip.ar.SipApplicationRouterInfo;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.dar.SARInfoFactory;

/**
 * The class is responsible for property file parsing.
 * 
 * @author Roman Mandeleil
 */
public class PropertyFileParser {
	
	private static final LogMgr c_logger = Log.get(PropertyFileParser.class);

	
	private HashMap<String, List<SipApplicationRouterInfo>> methodForApplicationMap = null; 
	private StringBuffer propData = null;
	private File propFile = null;  


	public PropertyFileParser(File propFile, 
			HashMap<String, List<SipApplicationRouterInfo>> methodForApplicationMap) throws IOException {

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug("CWSCT0416I: Default application router, property strategy, loading property file - " +
					propFile.getPath() +
					" .");
		}		
		this.propFile = propFile;
		this.methodForApplicationMap = methodForApplicationMap;
		loadFile();
	}

	
	/**
	 * Load application composition property file
	 * @throws IOException
	 */
	public void loadFile () throws IOException{
		
		InputStream inputStream = null;
		try {

	    	if (c_logger.isTraceEntryExitEnabled())
	        {
	            c_logger.traceEntry(this, "loadFile");
	        }

			inputStream = new FileInputStream(propFile);
			
			Properties appRouterProp = new Properties();
			appRouterProp.load(inputStream);
			parse(appRouterProp);
			

	    	if (c_logger.isTraceEntryExitEnabled())
	        {
	            c_logger.traceExit(this, "loadFile");
	        }

		}catch (IOException e) {
			if( c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug(null, "loadFile", "Unable to load appilcation composition property file = " +
						"" + propFile);

				c_logger.traceDebug(null, "loadFile", "" + e.getMessage());
			}
			throw e;
		}finally {
			if (inputStream!=null) {
				inputStream.close();
			}
		}
		

	}
	
	/**
	 * reload the configuration from the file, 
	 * used every application deploy/undeploy event 
	 */
	public void reload(){
		if (propFile != null){
			try {
	    		if (c_logger.isTraceDebugEnabled()) {
	    			c_logger.traceDebug("CWSCT0418I: Default application router, property strategy, property file has been reloaded.");
	    		}
				
				loadFile();
			} catch (IOException e) {
				c_logger.error("error.dar.property.parser.1"); 
				e.printStackTrace();
			}
		}
	}

	/**
	 * parse config string in to data structures, 
	 * the parsing is performed according to structure defined 
	 * in JSR289 Appendix C
	 * 
	 * @param appRouterProps - properties loaded from a property file. 
	 */
	private void parse(Properties appRouterProps){
		
		Set keys = appRouterProps.keySet();
		
		Pattern appPattern = Pattern.compile("\\([^\\(]*\\)");
		
		for (Object methodName : keys){
			String appInfoString = appRouterProps.getProperty(methodName.toString());
			Matcher appMatcher = appPattern.matcher(appInfoString);

			List appList = exctractApplications(appMatcher, appInfoString);
			methodForApplicationMap.put(methodName.toString(), appList);
		}
		
	}
	
	/**
	 * 
	 * @param appMatcher
	 * @param appInfoString
	 * @return
	 */
	private List exctractApplications(Matcher appMatcher, 
			String appInfoString){
		
		List appList = new ArrayList();
		while (appMatcher.find()) {

			String currApplicationInfo = appInfoString.substring(appMatcher
					.start(), appMatcher.end());
			SipApplicationRouterInfo sarInfo = SARInfoFactory
					.createSARInfoFactory(currApplicationInfo);
			appList.add(sarInfo);
		}
		
		
		return appList;
	}

	/**
	 * Getter for loaded applications data
	 * @return
	 */
	private HashMap<String, List<SipApplicationRouterInfo>> getMethodForApplicationMap() {
		return methodForApplicationMap;
	}

}
