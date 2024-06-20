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
package com.ibm.ws390.ola;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.zos.channel.wola.internal.WOLAConfig;

public class GroupNameCaseTest {

    private static final String CLASS_NAME = GroupNameCaseTest.class.getName();

	/**
     * Tests case enforcement in group names.
     */
    @Test
    public void testWOLAGroupNameCase() {
        System.out.println(CLASS_NAME + ".testWOLAGroupNameCase.Entry.");

        Map<String,Object> config = new HashMap<String,Object>();
        
        String key1 = "wolaGroup";
        String key2 = "wolaName2";
        String key3 = "wolaName3";
        
        String val1 = "group";
        String val2 = "name2";
        String val3 = "name3";
        
        config.put(key1, val1);
        config.put(key2, val2);
        config.put(key3, val3);
        
        MyWOLAConfig wc = new MyWOLAConfig();
        wc.activate(config);
        
        // check vals in config and use getters
        String wg1 = wc.getWolaGroup();
        String wg2 = wc.getWolaName2();
        String wg3 = wc.getWolaName3();

        System.out.println(wg1);
        System.out.println(wg2);
        System.out.println(wg3);

        // observe uppercase group names
        Assert.assertTrue(wg1.equals("GROUP")
                       && wg2.equals("NAME2")
                       && wg3.equals("NAME3"));

        System.out.println(CLASS_NAME + ".testWOLAGroupNameCase.Exit");
    }
    
    /**
     * expose protected methods
     */
    private class MyWOLAConfig extends WOLAConfig {
    	@Override
    	public void activate(Map<String,Object> config) {
    		super.activate(config);
    	}
    	
    	@Override
    	public String getWolaGroup() {
    		return super.getWolaGroup();
    	}
   	
    	@Override
    	public String getWolaName2() {
    		return super.getWolaName2();
    	}
    	
    	@Override
    	public String getWolaName3() {
    		return super.getWolaName3();
    	}
    }
}