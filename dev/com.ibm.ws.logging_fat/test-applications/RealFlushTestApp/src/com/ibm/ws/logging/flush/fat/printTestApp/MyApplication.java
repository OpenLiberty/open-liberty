package com.ibm.ws.logging.flush.fat.printTestApp;

import javax.ws.rs.core.Application;

import com.ibm.json.java.JSONObject;

import java.util.Set;
import java.util.SortedMap;

import javax.inject.Inject;
import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;;

@ApplicationScoped
@ApplicationPath("/")
@Path("/")
public class MyApplication extends Application {}