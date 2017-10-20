package jaxrs21.fat.providerPriority;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Priority(Priorities.USER + 1)
@Provider
public class MyHighPriorityThrowableMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable ex) {
        return Response.status(409).entity("MyHighPriorityThrowableMapper").build();
    }

}
