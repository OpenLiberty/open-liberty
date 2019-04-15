package com.ibm.ws.logging.flush.fat.printBoolTests;

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
@Path("/printBoolTests")
public class PrintBoolTests {

    private static final Logger MYlOGGER = Logger.getLogger(PrintBoolTests.class.getName());
    
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
}
