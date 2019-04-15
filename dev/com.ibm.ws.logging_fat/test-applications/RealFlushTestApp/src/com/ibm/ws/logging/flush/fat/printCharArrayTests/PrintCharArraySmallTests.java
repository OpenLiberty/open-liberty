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
@Path("/printCharArraySmallTests")
public class PrintCharArraySmallTests {

    private static final Logger MYlOGGER = Logger.getLogger(PrintCharArraySmallTests.class.getName());
    
    @GET
    @Path("/printCharArraySmall")
    public String printCharArraySmall() {
    	
    	char[] str = "smallStr".toCharArray();
    	System.out.print(str);
    	
        return "---- DONE ----";
    }

    @GET
    @Path("/printlnCharArraySmall")
    public String printlnCharArraySmall() {
    	
    	char[] str = "smallStr".toCharArray();
    	System.out.println(str);
    	
        return "---- DONE ----";
    }
}
