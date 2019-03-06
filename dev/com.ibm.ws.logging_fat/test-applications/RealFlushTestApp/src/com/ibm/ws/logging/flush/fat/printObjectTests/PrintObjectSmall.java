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
@Path("/printObjectSmall")
public class PrintObjectSmall {

    private static final Logger MYlOGGER = Logger.getLogger(PrintObjectSmall.class.getName());
    
    @GET
    @Path("/printObjectSmall")
    public String makeString() {
    	
    	DummyObject obj = new DummyObject();
    	System.out.print(obj.toStringSmall());
    	
        return "---- DONE ----";
    }
}
