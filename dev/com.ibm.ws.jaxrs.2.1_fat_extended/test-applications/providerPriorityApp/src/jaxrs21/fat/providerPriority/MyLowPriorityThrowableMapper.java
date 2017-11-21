package jaxrs21.fat.providerPriority;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Priority(Priorities.USER + 2)
@Provider
public class MyLowPriorityThrowableMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable ex) {
        return Response.status(410).entity("MyLowPriorityThrowableMapper").build();
    }

}
