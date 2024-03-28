package com.ibm.ws.kernel.reporting.fat.response;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

@ApplicationPath("")
@Path("/endpoints")
public class CVEReportingResponseEndpoints extends Application {

	private static JsonData data;

	@POST
	@Path("/response")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public JsonData post(JsonData dataReceived) {

		data = dataReceived;

		System.out.println("POST COMPLETED");

		return data;
	}

	@GET
	@Path("/getResponse")
	@Produces(MediaType.APPLICATION_JSON)
	public JsonData get() {

		return data;
	}

}
