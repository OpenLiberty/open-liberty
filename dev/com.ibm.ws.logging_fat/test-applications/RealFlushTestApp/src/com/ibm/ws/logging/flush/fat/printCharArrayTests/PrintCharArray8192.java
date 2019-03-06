package com.ibm.ws.logging.flush.fat.printCharArrayTests;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@ApplicationScoped
@Path("/printCharArray8192")
public class PrintCharArray8192 {

    private static final Logger MYlOGGER = Logger.getLogger(PrintCharArray8192.class.getName());

    @GET
    @Path("/printCharArray8192")
    public String makeString() {

        String starter = "";
        String string = "R";
        for (int i = 0; i < 8192; i++) {
            starter = starter + string;
        }
        char[] done = starter.toCharArray();
        System.out.print(done);

        return "---- DONE ----";
    }
}
