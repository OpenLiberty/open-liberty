package com.ibm.ws.logging.flush.fat.printTestApp;

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
@Path("/test")
public class RestThing {

    private static final Logger MYlOGGER = Logger.getLogger(RestThing.class.getName());
    
    @GET
    @Path("/get1")
    public String makeString() {
    	
    	//This Class exists as a test
    	
        return "---- DONE ----";
    }
}
