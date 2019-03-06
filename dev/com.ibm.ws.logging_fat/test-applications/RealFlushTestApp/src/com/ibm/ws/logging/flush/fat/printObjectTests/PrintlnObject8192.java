package com.ibm.ws.logging.flush.fat.printObjectTests;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@ApplicationScoped
@Path("/printlnObject8192")
public class PrintlnObject8192 {

    private static final Logger MYlOGGER = Logger.getLogger(PrintlnObject8192.class.getName());

    @GET
    @Path("/printlnObject8192")
    public String makeString() {

        DummyObject obj = new DummyObject();
        System.out.println(obj.toString8192());

        return "---- DONE ----";
    }
}
