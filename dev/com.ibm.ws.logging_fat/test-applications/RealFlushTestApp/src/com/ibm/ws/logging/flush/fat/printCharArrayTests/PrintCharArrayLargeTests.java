package com.ibm.ws.logging.flush.fat.printCharArrayTests;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@ApplicationScoped
@Path("/printCharArrayLargeTests")
public class PrintCharArrayLargeTests {

    private static final Logger MYlOGGER = Logger.getLogger(PrintCharArrayLargeTests.class.getName());

    private String make1892() {
        String starter = "";
        String string = "R";
        for (int i = 0; i < 8192; i++) {
            starter = starter + string;
        }
        return starter;
    }

    private String make1893() {
        String starter = "";
        String string = "R";
        for (int i = 0; i < 8193; i++) {
            starter = starter + string;
        }
        return starter;
    }

    @GET
    @Path("/printCharArray8192")
    public String printCharArray8192() {

        String done = make1892();
        System.out.print(done.toCharArray());

        return "---- DONE ----";
    }

    @GET
    @Path("/printlnCharArray8192")
    public String printlnCharArray8192() {

        String done = make1892();
        System.out.println(done.toCharArray());

        return "---- DONE ----";
    }

    @GET
    @Path("/printCharArray8193")
    public String printCharArray8193() {

        String done = make1893();
        System.out.print(done.toCharArray());

        return "---- DONE ----";
    }

    @GET
    @Path("/printlnCharArray8193")
    public String printlnCharArray8193() {

        String done = make1893();
        System.out.println(done.toCharArray());

        return "---- DONE ----";
    }
}
