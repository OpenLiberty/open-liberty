package jaxrs21.fat.providerPriority;

import javax.annotation.Priority;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Priority(8)
@Provider
public class MyLowPriorityMyExceptionMapper implements ExceptionMapper<MyException> {

    @Override
    public Response toResponse(MyException ex) {
        return Response.status(412).entity("MyLowPriorityMyExceptionMapper").build();
    }

}
