package com.ibm.ws.logging.flush.fat.printStringTests;

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
@Path("/printStringLongTests")
public class PrintStringLongTests {

    private static final Logger MYlOGGER = Logger.getLogger(PrintStringLongTests.class.getName());
    
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
}
