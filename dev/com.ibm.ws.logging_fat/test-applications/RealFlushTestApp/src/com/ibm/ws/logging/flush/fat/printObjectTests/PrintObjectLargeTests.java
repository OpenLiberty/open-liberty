package com.ibm.ws.logging.flush.fat.printObjectTests;

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
@Path("/printObjectLargeTests")
public class PrintObjectLargeTests {

    private static final Logger MYlOGGER = Logger.getLogger(PrintObjectLargeTests.class.getName());
    
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
}
