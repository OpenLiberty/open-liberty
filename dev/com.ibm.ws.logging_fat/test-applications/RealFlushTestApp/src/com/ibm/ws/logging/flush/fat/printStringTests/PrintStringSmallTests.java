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
@Path("/printStringSmallTests")
public class PrintStringSmallTests {

    private static final Logger MYlOGGER = Logger.getLogger(PrintStringSmallTests.class.getName());
    
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
