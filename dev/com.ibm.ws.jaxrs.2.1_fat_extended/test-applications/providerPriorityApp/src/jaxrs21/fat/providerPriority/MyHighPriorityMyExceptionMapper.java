package jaxrs21.fat.providerPriority;

import javax.annotation.Priority;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Priority(7)
@Provider
public class MyHighPriorityMyExceptionMapper implements ExceptionMapper<MyException> {

    @Override
    public Response toResponse(MyException ex) {
        return Response.status(418).entity("MyHighPriorityMyExceptionMapper").build();
    }

}
