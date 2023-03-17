package com.ibm.ws.install.featureUtility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.ibm.ws.install.InstallConstants.VerifyOption;
import com.ibm.ws.install.InstallException;

public class FeatureUtilityTest {

    FeatureUtility featureUtility = new FeatureUtility(null, null, null, null, null, null, null, null, null, null, null,
	    null, false, null);

    @Test
    public void testCheckVerifyOptionPass() throws InstallException {
	Map<String, Object> envMap = new HashMap<>();
	envMap.put("FEATURE_VERIFY", "all");
	VerifyOption opt = featureUtility.getVerifyOption("all", envMap);
	assertEquals(VerifyOption.all, opt);
    }

    @Test
    public void testCheckVerifyOptionDefault() throws InstallException {
	Map<String, Object> envMap = new HashMap<>();
	VerifyOption opt = featureUtility.getVerifyOption(null, envMap);
	assertEquals(VerifyOption.enforce, opt);
    }

    @Test
    public void testCheckVerifyOptionMismatch() throws InstallException {
	Map<String, Object> envMap = new HashMap<>();
	envMap.put("FEATURE_VERIFY", "all");
	boolean pass = false;
	try {
	    featureUtility.getVerifyOption("enforce", envMap);
	} catch (InstallException e) {
	    pass = true;
	    assertTrue(e.getMessage().contains("CWWKF1504E"));
	}
	assertTrue(pass);
    }

    @Test
    public void testCheckVerifyOptionNull() throws InstallException {
	Map<String, Object> envMap = new HashMap<>();
	envMap.put("FEATURE_VERIFY", "all");
	assertEquals(VerifyOption.all, featureUtility.getVerifyOption(null, envMap));
    }

    @Test
    public void testCheckVerifyOptionEnvMapNull() throws InstallException {
	Map<String, Object> envMap = new HashMap<>();
	assertEquals(VerifyOption.all, featureUtility.getVerifyOption("all", envMap));
    }


    @Test
    public void testCheckVerifyOptionBothInvalid() throws InstallException {
	Map<String, Object> envMap = new HashMap<>();
	envMap.put("FEATURE_VERIFY", "invalid");
	boolean pass = false;
	try {
	    featureUtility.getVerifyOption("invalid", envMap);
	} catch (InstallException e) {
	    pass = true;
	    assertTrue(e.getMessage().contains("CWWKF1505E"));
	}
	assertTrue(pass);
    }

    @Test
    public void testCheckVerifyOptionInvalid() throws InstallException {
	Map<String, Object> envMap = new HashMap<>();
	boolean pass = false;
	try {
	    featureUtility.getVerifyOption("invalid", envMap);
	} catch (InstallException e) {
	    pass = true;
	    assertTrue(e.getMessage().contains("CWWKF1505E"));
	}
	assertTrue(pass);
    }

    @Test
    public void testCheckVerifyOptionEnvInvalid() throws InstallException {
	Map<String, Object> envMap = new HashMap<>();
	envMap.put("FEATURE_VERIFY", "invalid");
	boolean pass = false;
	try {
	    featureUtility.getVerifyOption(null, envMap);
	} catch (InstallException e) {
	    pass = true;
	    assertTrue(e.getMessage().contains("CWWKF1505E"));
	}
	assertTrue(pass);
    }

//    Expect mismtach error message
    @Test
    public void testCheckVerifyOptionOnlyEnvInvalid() throws InstallException {
	Map<String, Object> envMap = new HashMap<>();
	envMap.put("FEATURE_VERIFY", "invalid");
	boolean pass = false;
	try {
	    featureUtility.getVerifyOption("all", envMap);
	} catch (InstallException e) {
	    pass = true;
	    assertTrue(e.getMessage().contains("CWWKF1504E"));
	}
	assertTrue(pass);
    }

    @Test
    public void testCheckVerifyOptionOnlySyntaxInvalid() throws InstallException {
	Map<String, Object> envMap = new HashMap<>();
	envMap.put("FEATURE_VERIFY", "all");
	boolean pass = false;
	try {
	    featureUtility.getVerifyOption("invalid", envMap);
	} catch (InstallException e) {
	    pass = true;
	    assertTrue(e.getMessage().contains("CWWKF1504E"));
	}
	assertTrue(pass);
    }



}
