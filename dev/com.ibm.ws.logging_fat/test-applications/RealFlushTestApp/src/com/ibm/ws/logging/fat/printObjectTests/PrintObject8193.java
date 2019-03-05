package printObjectTests;

import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.enterprise.inject.Produces;

import com.ibm.websphere.filetransfer.FileServiceMXBean.MetaData;
import com.ibm.websphere.logging.hpel.LogRecordContext;


@ApplicationScoped
@Path("/printObject8193")
public class PrintObject8193 {

    private static final Logger MYlOGGER = Logger.getLogger(PrintObject8193.class.getName());
    
    @GET
    @Path("/printObject8193")
    public String makeString() {
    	
    	DummyObject obj = new DummyObject();
    	System.out.print(obj.toString8193());
    	
        return "---- DONE ----";
    }
}
