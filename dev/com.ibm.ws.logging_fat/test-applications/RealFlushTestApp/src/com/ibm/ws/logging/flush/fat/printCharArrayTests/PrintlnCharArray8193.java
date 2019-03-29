package com.ibm.ws.logging.flush.fat.printCharArrayTests;

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
@Path("/printlnCharArray8193")
public class PrintlnCharArray8193 {

    private static final Logger MYlOGGER = Logger.getLogger(PrintlnCharArray8193.class.getName());
    
    @GET
    @Path("/printlnCharArray8193")
    public String makeString() {
    	
    	String starter = "";
    	String string = "R";
    	for(int i=0; i < 8193; i++) {
    		starter = starter + string;
    	}
    	char[] done = starter.toCharArray();
    	System.out.println(done);
    	
        return "---- DONE ----";
    }
}
