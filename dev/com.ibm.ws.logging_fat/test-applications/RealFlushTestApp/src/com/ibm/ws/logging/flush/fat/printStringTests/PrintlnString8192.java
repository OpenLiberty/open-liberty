package com.ibm.ws.logging.flush.fat.printStringTests;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@ApplicationScoped
@Path("/printlnString8192")
public class PrintlnString8192 {

    private static final Logger MYlOGGER = Logger.getLogger(PrintlnString8192.class.getName());

    @GET
    @Path("/printlnString8192")
    public String makeString() {

        String starter = "";
        String string = "R";
        for (int i = 0; i < 8192; i++) {
            starter = starter + string;
        }
        System.out.println(starter);

        return "---- DONE ----";
    }
}
