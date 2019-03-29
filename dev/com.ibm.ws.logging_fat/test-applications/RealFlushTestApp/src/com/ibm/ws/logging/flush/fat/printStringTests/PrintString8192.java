package com.ibm.ws.logging.flush.fat.printStringTests;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@ApplicationScoped
@Path("/printString8192")
public class PrintString8192 {

    private static final Logger MYlOGGER = Logger.getLogger(PrintString8192.class.getName());

    @GET
    @Path("/printString8192")
    public String makeString() {

        String starter = "";
        String string = "R";
        for (int i = 0; i < 8192; i++) {
            starter = starter + string;
        }
        System.out.print(starter);

        return "---- DONE ----";
    }
}
