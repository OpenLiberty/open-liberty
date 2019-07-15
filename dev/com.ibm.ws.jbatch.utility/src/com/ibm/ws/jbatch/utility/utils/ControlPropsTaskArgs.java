/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;


/**
 * Wrapper around ArgMap that looks in a --controlPropertiesFile for 
 * args if they are not already specified in the ArgMap (i.e. args
 * specified on the command line (ArgMap) take precedence over those
 * in the controlProps file).
 */
public class ControlPropsTaskArgs extends Properties {
    
    private Properties jobParameters;
    private boolean jobParametersFileInCmd;
    private boolean jobPropertiesFileInCmd;;
    final static private List<String> boolContProps = Arrays.asList(new String[] {"--trustSslCertificates","--stopOnShutdown",
    																				"--returnExitStatus","--wait","--getJobLog",
    																				"--verbose"});
    
    public ControlPropsTaskArgs(String[] args) throws IOException {
        super();
        jobParameters = new Properties();
        jobParametersFileInCmd = false;
        jobPropertiesFileInCmd = false;
        String contPropsFile = "";
        //Get --controlPropertiesFile property to add these parameters first
        for(String arg : args){
        	String argname = parseArgName(arg);
        	if(argname.equalsIgnoreCase("--controlPropertiesFile")){
        		contPropsFile = parseArgValue(arg);
        	} else if(argname.equalsIgnoreCase("--jobParametersFile")){
                jobParametersFileInCmd = true;
        	} else if(argname.equalsIgnoreCase("--jobPropertiesFile")){
                jobPropertiesFileInCmd = true;
        	}
        }
        //Parse --controlPropertiesFile if it exists
        if(contPropsFile.length() > 0){
        	parseControlPropsFile(contPropsFile);
        }
    	parseArgs(args);
    	//resolve all job parameters
        jobParameters = resolveJobParameters();
    }
        
    /**
     * @return a Map of argName=argValue pairs.
     */
    protected Properties parseArgs(String[] args) {
        for (String arg : args) {
            processArg(parseArgName(arg), parseArgValue(arg));
        }
        
        return this;
    }
    
    /**
     * Process the parsed command-line arg or control props arg. 
     */
    protected void processArg(String argName, String argValue) {
        if ("--jobParameter".equalsIgnoreCase(argName)) {
            processJobParameter(argValue);
        } else if("--jobParametersFile".equalsIgnoreCase(argName)){
        	setProperty(argName, argValue);
        } else if("--jobPropertiesFile".equalsIgnoreCase(argName) && !jobParametersFileInCmd){
        	remove("--jobParametersFile");
        	setProperty(argName, argValue);
        } else if(argValue == null){//matches zos 
        	setProperty(argName, argName);
        } else{
        	setProperty(argName, argValue);
        }
    }
    
    /**
     * Parse the --jobParameter arg value and put it in the "jobParameters" Props object.
     */
    protected void processJobParameter(String jobParameter) {
        if (StringUtils.isEmpty(jobParameter) || parseArgValue(jobParameter) == null ) {
            throw new InvalidArgumentValueException("--jobParameter",jobParameter, Arrays.asList("--jobParameter=[key]=[value]"));
        } 
        
        jobParameters.setProperty(parseArgName(jobParameter), parseArgValue(jobParameter));
    }
    
    /**
     * @return the value for key "jobParameters", a Properties object
     */
    public synchronized Properties getJobParameters() {
        return jobParameters;
    }
    
    /**
     * @return true if the given argName was specified (i.e exists in the map).
     */
    public boolean isSpecified(String argName) {
        return containsKey(argName);
    }
    
    /**
     * 
     * @return the given key value as a Properties object.  The properties are read from
     *         the fileNames associated with the given key.
     * @throws IOException 
     */
    public void parseControlPropsFile( String path ) throws IOException{
        Properties temp = new Properties();
        String[] files = path.split(",");//Javadoc guarantees order in which they occur
        for(String value : files){
        	if(value.length() > 0){
		        File propsFile = getFileFromValue( value );
		        if (propsFile != null) {
		            InputStream is;
		            try {
						is = new FileInputStream(propsFile);
					} catch (FileNotFoundException fe) {
						throw fe;
					}
					try {
			            temp.load(is);
					} catch (IOException ioe){
						throw ioe;
					} finally{
						is.close();
					}
					//collapse jobParametersFile and jobPropertiesFile at each individual level
					if(temp.containsKey("--jobParametersFile")&&temp.containsKey("--jobPropertiesFile")){
						temp.remove("--jobPropertiesFile");
						this.remove("--jobParametersFile");
						this.remove("--jobPropertiesFile");
					} else if(temp.containsKey("--jobParametersFile")){
						this.remove("--jobParametersFile");
						this.remove("--jobPropertiesFile");
					} else if(temp.containsKey("--jobPropertiesFile")){
						this.remove("--jobParametersFile");
						this.remove("--jobPropertiesFile");
					}
					this.putAll(temp);
		        }
            }
        }
        
        //Trim trailing whitespace
        for (Enumeration keys = propertyNames(); keys.hasMoreElements();) {
			String tmpKey = (String)keys.nextElement();
			String tmpValue = getProperty(tmpKey);
			remove(tmpKey);
			tmpValue = tmpValue.trim();
			tmpKey = tmpKey.trim();
			if(isBooleanControlProp(tmpKey)){
				put(tmpKey, tmpKey);
			} else{
				put(tmpKey, tmpValue);
			}
        }
    }
    
    public boolean isBooleanControlProp(String tmpKey){
    	/*if(tmpKey.compareTo("--trustSslCertificates") == 0 || tmpKey.compareTo("--stopOnShutdown") == 0 ||
    			tmpKey.compareTo("--returnExitStatus") == 0 || tmpKey.compareTo("--wait") == 0 ||
    			tmpKey.compareTo("--getJobLog") == 0 || tmpKey.compareTo("--verbose") == 0)*/
    	if(boolContProps.contains(tmpKey)){
    		return true;
    	}
    	return false;
    }
    
    /**
     * 
     * @return the given key value as a Properties object.  The properties are read from
     *         the fileNames associated with the given key.
     */
    public Properties getControlPropsInlineJobParameters( String path ) throws IOException {
        Properties retMe = new Properties();
        String[] files = path.split(",");//Javadoc guarantees order in which they occur
        for(String value : files){
        	if(value.length() > 0){
		        File propsFile = getFileFromValue( value );
		        if (propsFile != null) {
		            BufferedReader br = new BufferedReader(new InputStreamReader( new FileInputStream(propsFile), Charset.defaultCharset()));
		            String line;
		            while((line = br.readLine()) != null){
		            	if(parseArgName(line).compareTo("--jobParameter")==0){
		            		retMe.setProperty(parseArgName(parseArgValue(line)), parseArgValue(parseArgValue(line)));
		            	}
		            }
		            br.close();
		        }
        	}
        }
        
        //Trim trailing whitespace
        for (Enumeration keys = retMe.propertyNames(); keys.hasMoreElements();) {
			String tmpKey = (String)keys.nextElement();
			String tmpValue = retMe.getProperty(tmpKey);
			retMe.remove(tmpKey);
			tmpValue = tmpValue.trim();
			tmpKey = tmpKey.trim();
			retMe.put(tmpKey, tmpValue);
        }
        return retMe;
    }
    
    /**
     * 
     * @return the given key value as a Properties object.  The properties are read from
     *         the fileNames associated with the given key.  If the key does not exist,
     *         an empty Properties object is returned.
     * @throws IOException 
     */
    public Properties getJobPropsFileProps(String key) throws IOException{
        Properties retMe = new Properties();
        String unsplit = (String) getProperty(key);
        if(unsplit != null){
	        String[] files = unsplit.split(",");//Javadoc guarantees order in which they occur
	        for(String value : files){
	        	if(value.length() > 0){
			        File propsFile = getFileFromValue( value );
			        if (propsFile != null) {
			            InputStream is;
			            try {
							is = new FileInputStream(propsFile);
						} catch (FileNotFoundException fe) {
							throw fe;
						}
						try {
				            retMe.load(is);
						} catch (IOException ioe){
							throw ioe;
						} finally{
							is.close();
			            }
			        }
	        	}
	        }
	        
	        //Trim trailing whitespace
	        for (Enumeration keys = retMe.propertyNames(); keys.hasMoreElements();) {
				String tmpKey = (String)keys.nextElement();
				String tmpValue = retMe.getProperty(tmpKey);
				retMe.remove(tmpKey);
				tmpValue = tmpValue.trim();
				tmpKey = tmpKey.trim();
				retMe.put(tmpKey, tmpValue);
	        }
        }
        return retMe;
    }
    
    /**
     * Consolidate The properties within --controlPropertiesFile, --jobParametersFile, and --jobParameter 
     * 
     * @return the consolidated jobParameters Properties object
     * @throws IOException 
     */
    public Properties resolveJobParameters() throws IOException{
    	
    	Properties fileProps = new Properties();
    	
    	
    	if(jobParametersFileInCmd || jobPropertiesFileInCmd){
    		//If jobParametersFile or alias designated in command line we want the file parameters to overwrite the
    		//inline controlPropertiesjob parameters.
    		
    		//Put all control props specifically designated --jobParameter=key=value
        	if(isSpecified("--controlPropertiesFile")){
        		fileProps.putAll(getControlPropsInlineJobParameters(getProperty("--controlPropertiesFile")));
        	}
        	
        	//Get whichever alias is specified
        	if(isSpecified("--jobParametersFile")){
        		fileProps.putAll(getJobPropsFileProps("--jobParametersFile"));
        	} else if(isSpecified("--jobPropertiesFile")){
        		fileProps.putAll(getJobPropsFileProps("--jobPropertiesFile"));
        	}
    	}else {
    		//If jobParametersFile or alias is not designated in command line we want the inline controlProperties job
    		//parameters to overwrite the file parameters.
    		
        	//Get whichever alias is specified
        	if(isSpecified("--jobParametersFile")){
        		fileProps.putAll(getJobPropsFileProps("--jobParametersFile"));
        	} else if(isSpecified("--jobPropertiesFile")){
        		fileProps.putAll(getJobPropsFileProps("--jobPropertiesFile"));
        	}
        	
    		//Put all control props specifically designated --jobParameter=key=value
        	if(isSpecified("--controlPropertiesFile")){
        		fileProps.putAll(getControlPropsInlineJobParameters(getProperty("--controlPropertiesFile")));
        	}
    	}
        
        //Put all Command-line props specifically designated --jobParameter=key=value
        fileProps.putAll(getJobParameters());
        if(isSpecified("--verbose")){
        	printControlProps(fileProps);
        }
        return fileProps;
    }

    /**
     * If verbose is specified print out job parameters and control properties
     */
    public void printControlProps(Properties fileProps){
    	String printme = "";
    	StringBuffer printmebuf = new StringBuffer();
    	printmebuf.append("\n{");
    	for(java.util.Map.Entry<Object, Object> entry : entrySet()){
    		if(entry.getKey().toString().equals("--password")){
    			printmebuf.append(entry.getKey().toString()+"=XXXX"+"\n");
    		}else if (entry.getKey().toString().equals("--jobParameter")){
    			//Do nothing
    		}else{
    			printmebuf.append(entry.getKey().toString());
    			if(entry.getValue() != null){
    				printmebuf.append("=" + entry.getValue().toString());
    			}
    			printmebuf.append("\n");
    		}
    	}
    	printme = printmebuf.toString();
    	printme = printme.substring(0, printme.length()-1);
    	printme += "}";
    	TaskIO taskIO = new TaskIO(null,System.out,null);
    	taskIO.info(ResourceBundleUtils.getMessage("control.props.vals", printme));
    	StringBuffer printmebuf2 = new StringBuffer();
    	printmebuf2.append("\n{");
    	for(java.util.Map.Entry<Object, Object> entry : fileProps.entrySet()){
			printmebuf2.append(entry.getKey());
			if(entry.getValue() != null){
				printmebuf2.append("=" + entry.getValue().toString());
			}
    		printmebuf2.append("\n");
    	}
    	printme = printmebuf2.toString();
    	printme = printme.substring(0, printme.length()-1);
    	printme += "}";
    	taskIO.info(ResourceBundleUtils.getMessage("job.parms.vals", printme));
    } 
    
    /**
     * If --restartTokenFile was specified, write the instanceId into the file.
     */
    public void writeRestartTokenFileValue( String name, String value ) throws IOException {
    
    	File restartFile = getFileValue( name );
    	
    	if (restartFile != null) {
    		
    		DataOutputStream dos = new DataOutputStream(new FileOutputStream(restartFile, false));
    		try {
    			StringBuffer sb = new StringBuffer("restartJob=").append(value);
    			dos.writeBytes(sb.toString());
    			dos.flush();
    		} finally {
    			dos.close();
    		}
    	} 
       	
    }
    
    /**
     * If --restartTokenFile was specified, clear the instanceId in the file.
     */
    public void clearRestartTokenFromFile( String name ) throws IOException {
    
    	File restartFile = getFileValue( name );
    	
    	if (restartFile != null) {    		
    		DataOutputStream dos = new DataOutputStream(new FileOutputStream(restartFile, false));
    		try {
    			dos.writeBytes("");
    		} finally {
    			dos.close();
    		}
    	}
       	
    }
    
	/**
	* Check the given set of expected args against the actual args.
	* 
	* @throws UnrecognizedArgumentException if an argument is not in the list of expectedArgs.
	*/
	public void validateExpectedArgs(Collection<String> expectedArgs) {
	  for (Object key : keySet() ) {
	      if (! expectedArgs.contains((String)key) ) {
	          handleUnexpectedArg(expectedArgs, (String)key);
	      }
	  }
	}
    
    protected void handleUnexpectedArg(Collection<String> expectedArgs, String unexpectedArg) {
        // Check specifically if the arg has only 1 leading dash (when it should have 2)
        if ( expectedArgs.contains( "-" + unexpectedArg ) ) {
            throw new UnrecognizedArgumentException( unexpectedArg, "-" + unexpectedArg );
        } else if ( !unexpectedArg.startsWith("internal-") ){
            throw new UnrecognizedArgumentException( unexpectedArg );
        }
    }
    
    /**
     * Verify the value of the given argName is in the list of permittedValues.
     * 
     * If the value is empty or null, return the defaultValue.
     * 
     * @param argName the arg name
     * @param permittedValues permitted values for this arg
     * @param defaultValue returned if arg value is empty
     * 
     * @return the arg value, if permitted, or defaultValue if arg value is empty
     * 
     * @throws InvalidArgumentValueException
     */
    public String verifyStringValue(String argName, List<String> permittedValues, String defaultValue) {
        String argValue = getStringValue(argName);
        if (StringUtils.isEmpty(argValue)) {
            return defaultValue;
        } else if (permittedValues.contains(argValue)) {
            return argValue;
        } else {
            throw new InvalidArgumentValueException(argName, argValue, permittedValues);
        }
    }
    
    /**
     * @return the arg name (e.g. "--argName=argValue" returns "--argName")
     */
    protected String parseArgName(String arg) {
        int idx = arg.indexOf("=");
        return (idx >= 0) ? arg.substring(0, idx) : arg;
    }
    
    /**
     * @return the arg value (e.g. "--argName=argValue" returns "argValue"),
     *         or null if the argument doesn't have a value.
     */
    protected String parseArgValue(String arg) {
        int idx = arg.indexOf("=");
        return (idx >= 0) ? arg.substring(idx + 1) : null;
    }

    /**
     * @return the long value associated with the given arg
     */
    public Long getLongValue(String argName, Long defaultValue) {
        String val = getStringValue(argName);
        return ( val != null) ? new Long( val ) : defaultValue;
    }
    
    /**
     * @return the long value associated with the given arg.
     * 
     * @throws IllegalArgumentException if the value is not specified.
     */
    public Long getRequiredLongValue(String argName) {
        Long retMe = getLongValue(argName, null);
        
        if ( retMe == null ) {
            throw new ArgumentRequiredException(argName);
        }
        
        return retMe;
    }
    
    /**
     * @return the value associated with the given arg.
     * 
     * @throws IllegalArgumentException if the value is null or empty.
     */
    public String getRequiredString(String argName) {
        String retMe = getStringValue(argName);
        
        if ( StringUtils.isEmpty(retMe) ) {
            throw new ArgumentRequiredException(argName);
        }
        
        return retMe;
    }
    
    /**
     * @return the long value associated with the given arg
     */
    public Integer getIntValue(String argName, Integer defaultValue) {
        String val = getStringValue(argName);
        return ( val != null) ? new Integer( val ) : defaultValue;
    }
    
    /**
     * @return the value associated with the given arg.
     */
    public String getOrPromptForMaskedValue(String argName, ConsoleWrapper console, String prompt) {
        String retMe = getStringValue(argName);
        if (retMe == null) {
            retMe = console.readMaskedText(prompt);
            put(argName, retMe);    // save for later.
        }
        return retMe;
    }
    
    /**
     * @return the value associated with the given arg.
     */
    public String getStringValue(String argName) {
        return (String) get(argName);
    }
    
    /**
     * @return the given key value as a File, or null if the key doesn't exist.
     */
    public File getFileFromValue( String value ) {
        
        String fileName = value;
        
        return (StringUtils.isEmpty(fileName)) ? null : new File(fileName);
    }
    
    /**
     * @return the given key value as a File, or null if the key doesn't exist.
     */
    public File getFileValue( String key ) {
        
        String fileName = (String) getProperty(key);
        
        return (StringUtils.isEmpty(fileName)) ? null : new File(fileName);
    }
}