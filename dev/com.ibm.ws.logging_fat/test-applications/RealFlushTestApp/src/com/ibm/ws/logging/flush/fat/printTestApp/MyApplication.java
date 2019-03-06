package com.ibm.ws.logging.flush.fat.printTestApp;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;;

@ApplicationScoped
@ApplicationPath("/")
@Path("/")
public class MyApplication extends Application {}
