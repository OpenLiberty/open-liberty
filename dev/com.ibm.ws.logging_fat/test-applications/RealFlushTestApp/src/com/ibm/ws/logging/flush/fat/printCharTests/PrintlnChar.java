package com.ibm.ws.logging.flush.fat.printCharTests;

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
@Path("/printlnChar")
public class PrintlnChar {

    private static final Logger MYlOGGER = Logger.getLogger(PrintlnChar.class.getName());
    
    @GET
    @Path("/printlnChar")
    public String makeString() {
    	
    	char c = 'z';
    	System.out.println(c);
    	
        return "---- DONE ----";
    }
}
