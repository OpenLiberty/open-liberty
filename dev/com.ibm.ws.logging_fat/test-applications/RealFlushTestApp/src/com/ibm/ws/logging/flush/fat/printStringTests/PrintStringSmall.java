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
@Path("/printStringSmall")
public class PrintStringSmall {

    private static final Logger MYlOGGER = Logger.getLogger(PrintStringSmall.class.getName());
    
    @GET
    @Path("/printStringSmall")
    public String makeString() {
    	
    	String str = "smallStr";
    	System.out.print(str);
    	
        return "---- DONE ----";
    }
}
