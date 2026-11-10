package com.ibm.ws.security.audit.fat.config.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.audit.fat.common.tooling.AuditCommonTest;

import componenttest.topology.impl.LibertyServer;

@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class TestableLoggingStream extends AuditCommonTest {

	
	public TestableLoggingStream(LibertyServer server, Class<?> logClass) {
	    super(server,logClass);
	}

	// Values to be set by the child class
    protected LibertyServer server;
    protected Class<?> logClass;
    protected String auditLogFilePath = null;

    protected String getCurrentTestName() {
        return "Test name not set";
    }
   
   
    /**
     * Convert json logging format representation of a JSON key=value pair into a JSON object which will
     * be used to compare against audit data in the audit.log.
     * @param jsonLogData - sequence of test patterns
     * @return JSONObject representation of the test patterns
     * @throws Exception
     */
    public JSONObject asJsonObj(String... jsonLogData) throws Exception {
        Pattern pattern = Pattern.compile("^\\s*(.*?)\\s*=\\s*(.*)\\s*$");
        JSONObject entry = new JSONObject();
        for (String testPattern : jsonLogData) {
            Matcher matcher = pattern.matcher(testPattern);
            if (matcher.matches()) {
                String keys = matcher.group(1);
                String value = matcher.group(2);
                String[] subKeys = keys.split("\\.", -1);
                JSONObject current = entry;
                for (int i = 0; i < subKeys.length; i++) {
                    String subkey = subKeys[i];
                    boolean atEnd = (i == subKeys.length - 1);
                    if (!atEnd) {
                        if (!current.containsKey(subkey)) {
                            JSONObject no = new JSONObject();
                            current.put(subkey, no);
                            current = no;
                        } else {
                            current = (JSONObject) current.get(subkey);
                        }
                    } else {
                        current.put(subkey, value);
                    }
                }

            } else {

                throw new Exception("Bad test pattern: " + testPattern);
            }
        }
        return entry;
    }

    /**
     * Determine whether the expected key/value pairs from a JSONObject are contained within the
     * JSONObject for comparison.    
     * @param JSONObject expected - the key/value pairs in this object should all be found
     * @param JSONObject toCompare - the key/value pairs in this object should contain the expected 
     * @return boolean - true if the expected key/value pairs are contained in those in toCompare; false if not.
     */
	public boolean contains(JSONObject expected, JSONObject toCompare) {
		
		Set<String> auditKeys = expected.keySet();
	
		for (String expectedKey : auditKeys) {
			// Log.info(logClass, "thisMethod", "============ Searching for key: " + expectedKey + " \n"); //ENABLE FOR DEBUG
			if (!toCompare.containsKey(expectedKey)) 
				return false;
			
			Object expectedValue = expected.get(expectedKey);
			Object foundValue = toCompare.get(expectedKey);
			boolean expectedIsScalar= isScalar(expectedValue); 
			boolean foundIsScalar= isScalar(foundValue); 
			
			if (expectedIsScalar && foundIsScalar) {
				
				// Log.info(logClass, "thisMethod", "++++++++++  " + expectedValue  + " = " + foundValue + " ?\n"); // ENABLE FOR DEBUG
				if (!expectedValue.toString().equals(foundValue.toString()))
					return false;
			} else if (!expectedIsScalar && foundIsScalar) {
				return false;
			} else if (expectedIsScalar && !foundIsScalar) {
				return false;
			} else {
				 contains ((JSONObject) foundValue, (JSONObject) expectedValue);
			}
		}

		return true;
	}

    /**
     * Determine whether the object is a scalar (single value) versus complex like an Array.
     *   
     * @param Object obj - object being tested
     * @return boolean - true if scalar; false if complex.
     */
	private boolean isScalar(Object obj) {
		if (obj instanceof String)
			return true;
		if (obj instanceof Integer)
			return true;
		if (obj instanceof Boolean)
			return true;
		if (obj instanceof Float)
			return true;
		if (obj instanceof Long)
			return true;
		
		return false;
	}

    /**
     * Converts a
     * JSONObject for comparison.    
     * @param JSONObject expected - the key/value pairs in this object should all be found
     * @param JSONObject toCompare - the key/value pairs in this object should contain the expected 
     * @return boolean - true if the expected key/value pairs are contained within object toCompare; false if not.
     */
	public JSONObject asJson(String line) {
		try {
			List<String> recordsList = new ArrayList<>();
			JSONObject record = JSONObject.parse(line); 
			Set<String> keys = record.keySet();

			for (String key : keys) {
				recordsList.add(key + "=" + record.get(key));
			}
			
			String[] records = recordsList.toArray(new String[0]);
			JSONObject recordsJson = asJsonObj(records);
			return recordsJson;
			
		} catch (Exception e) {
			throw new RuntimeException("Could not convert line to JSON",e);
		}
	}
	
	/**
     * Replaces a string in a line with the specified string.
     * @param String line - the line which is the target for replacement
     * @param String replaceThis - the string to be replaced
     * @param String replaceWith - value to use for replace
     * @return String - line which has has specified string replaced with the replacement value
     */
	public  String replaceText(String line, String replaceThis, String replaceWith) {
	     line = line.replaceAll(replaceThis, replaceWith);
	     return line;
	}
	
}
