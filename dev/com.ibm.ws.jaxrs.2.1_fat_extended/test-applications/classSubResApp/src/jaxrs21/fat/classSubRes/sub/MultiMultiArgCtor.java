/**
 *
 */
package jaxrs21.fat.classSubRes.sub;

import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

public class MultiMultiArgCtor extends AbstractSubResource {

    private final String ctor;

    public MultiMultiArgCtor(@Context HttpHeaders headers) {
        ctor = " 1";
        System.out.println(this.getClass().getSimpleName() + " 1");
    }

    public MultiMultiArgCtor(@Context HttpHeaders headers, @Context UriInfo uriInfo) {
        ctor = " 2";
        System.out.println(this.getClass().getSimpleName() + " 2");
    }

    public MultiMultiArgCtor(@QueryParam("a") String a, @Context HttpHeaders headers, @Context UriInfo uriInfo) {
        ctor = " 3";
        System.out.println(this.getClass().getSimpleName() + " 3 " + a);
    }

    @Override
    String returnString(String suffix) {
        return super.returnString(suffix) + ctor;
    }
}
