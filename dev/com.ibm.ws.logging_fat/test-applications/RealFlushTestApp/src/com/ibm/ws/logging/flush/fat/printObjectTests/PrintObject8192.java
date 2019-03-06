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
@Path("/printObject8192")
public class PrintObject8192 {

    private static final Logger MYlOGGER = Logger.getLogger(PrintObject8192.class.getName());
    
    @GET
    @Path("/printObject8192")
    public String makeString() {
    	
    	DummyObject obj = new DummyObject();
    	System.out.print(obj.toString8192());
    	
        return "---- DONE ----";
    }
}
