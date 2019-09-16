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
package com.ibm.ws.logging.flush.fat.printTests;

import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.enterprise.inject.Produces;

import com.ibm.websphere.filetransfer.FileServiceMXBean.MetaData;
import com.ibm.websphere.logging.hpel.LogRecordContext;


@ApplicationScoped
@Path("/printTests")
public class PrintTests {

    private static final Logger MYlOGGER = Logger.getLogger(PrintTests.class.getName());

    private String make8193() {
        String starter = "";
    	String string = "R";
    	for(int i=0; i < 8193; i++) {
    		starter = starter + string;
        }
        return starter;
    }

    private String make8192() {
        String starter = "";
        String string = "R";
        for (int i = 0; i < 8192; i++) {
            starter = starter + string;
        }
        return starter;
    }
    
    @GET
    @Path("/printFalse")
    public String printFalse() {
    	
    	System.out.print(false);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printTrue")
    public String printTrue() {
    	
    	System.out.print(true);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnFalse")
    public String printlnFalse() {
    	
    	System.out.println(false);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnTrue")
    public String printlnTrue() {
    	
    	System.out.println(true);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printCharArray8192")
    public String printCharArray8192() {

        String done = make8192();
        System.out.print(done.toCharArray());

        return "---- DONE ----";
    }

    @GET
    @Path("/printlnCharArray8192")
    public String printlnCharArray8192() {

        String done = make8192();
        System.out.println(done.toCharArray());

        return "---- DONE ----";
    }

    @GET
    @Path("/printCharArray8193")
    public String printCharArray8193() {

        String done = make8193();
        System.out.print(done.toCharArray());

        return "---- DONE ----";
    }

    @GET
    @Path("/printlnCharArray8193")
    public String printlnCharArray8193() {

        String done = make8193();
        System.out.println(done.toCharArray());

        return "---- DONE ----";
    }

    @GET
    @Path("/printCharArraySmall")
    public String printCharArraySmall() {
    	
    	char[] str = "smallStr".toCharArray();
    	System.out.print(str);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnCharArraySmall")
    public String printlnCharArraySmall() {
    	
    	char[] str = "smallStr".toCharArray();
    	System.out.println(str);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printChar")
    public String printChar() {
    	
    	char c = 'z';
    	System.out.println(c);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnChar")
    public String printlnChar() {
    	
    	char c = 'z';
    	System.out.println(c);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printDouble")
    public String printDoubleSmall() {
    	
    	double num = 222222222;
    	System.out.print(num);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnDouble")
    public String printlnDoubleSmall() {
    	
    	double num = 222222222;
    	System.out.println(num);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printFloat")
    public String printFloat() {
    	
    	float num = 222222222;
    	System.out.print(num);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnFloat")
    public String printlnFloat() {
    	
    	float num = 222222222;
    	System.out.println(num);
    	
        return "---- DONE ----";
    }
    @GET
    @Path("/printInt")
    public String printInt() {
    	
    	int num = 222222222;
    	System.out.print(num);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnInt")
    public String printlnInt() {
    	
    	int num = 222222222;
    	System.out.println(num);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printLong")
    public String printLong() {
    	
    	long num = 222222222;
    	System.out.print(num);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnLong")
    public String printlnLong() {
    	
    	long num = 222222222;
    	System.out.println(num);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnObject8193")
    public String printlnObject8193() {
    	
    	DummyObject obj = new DummyObject();
    	System.out.println(obj.toString8193());
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printObject8193")
    public String printObject8193() {
    	
    	DummyObject obj = new DummyObject();
    	System.out.print(obj.toString8193());
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printObject8192")
    public String printObject8192() {
    	
    	DummyObject obj = new DummyObject();
    	System.out.print(obj.toString8192());
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnObject8192")
    public String printlnObject8192() {

        DummyObject obj = new DummyObject();
        System.out.println(obj.toString8192());

        return "---- DONE ----";
    }

    @GET
    @Path("/printlnObjectSmall")
    public String printlnObjectSmall() {
    	
    	DummyObject obj = new DummyObject();
    	System.out.println(obj.toStringSmall());
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printObjectSmall")
    public String printObjectSmall() {
    	
    	DummyObject obj = new DummyObject();
    	System.out.print(obj.toStringSmall());
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printString8193")
    public String printString8193() {
        
        String starter = make8193();
    	System.out.print(starter);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnString8193")
    public String printlnString8193() {
    	
    	String starter = make8193();
    	System.out.println(starter);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printString8192")
    public String printString8192() {

        String starter = make8192();
        System.out.print(starter);

        return "---- DONE ----";
    }

    @GET
    @Path("/printlnString8192")
    public String printlnString8192() {

        String starter = make8192();
        System.out.print(starter);

        return "---- DONE ----";
    }

    @GET
    @Path("/printStringSmall")
    public String printStringSmall() {
    	
    	String str = "smallStr";
    	System.out.print(str);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnStringSmall")
    public String printlnStringSmall() {
    	
    	String str = "smallStr";
    	System.out.println(str);
    	
        return "---- DONE ----";
    }
}
