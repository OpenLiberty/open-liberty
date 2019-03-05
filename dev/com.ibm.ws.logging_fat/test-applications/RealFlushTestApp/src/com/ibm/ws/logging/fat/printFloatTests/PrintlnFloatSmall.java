package printFloatTests;

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
@Path("/printlnFloatSmall")
public class PrintlnFloatSmall {

    private static final Logger MYlOGGER = Logger.getLogger(PrintlnFloatSmall.class.getName());
    
    @GET
    @Path("/printlnFloatSmall")
    public String makeString() {
    	
    	float num = 222222222;
    	System.out.println(num);
    	
        return "---- DONE ----";
    }
}
