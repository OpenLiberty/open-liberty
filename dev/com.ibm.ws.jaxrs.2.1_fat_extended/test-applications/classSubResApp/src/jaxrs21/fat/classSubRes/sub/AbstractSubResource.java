/**
 *
 */
package jaxrs21.fat.classSubRes.sub;

import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;

public class AbstractSubResource {

    @GET
    public Response get() {
        return Response.ok(returnString("")).build();
    }

    @POST
    public Response post(String postData) {
        return Response.ok(returnString(" " + postData)).build();
    }

    @PATCH
    public Response patch(String patchData) {
        return Response.ok(returnString(" " + patchData)).build();
    }

    String returnString(String suffix) {
        return this.getClass().getSimpleName() + suffix;
    }
}
