package com.ibm.ws.logging.flush.fat.printDoubleTests;

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
@Path("/printDoubleTests")
public class PrintDoubleTests {

    private static final Logger MYlOGGER = Logger.getLogger(PrintDoubleTests.class.getName());
    
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
}
