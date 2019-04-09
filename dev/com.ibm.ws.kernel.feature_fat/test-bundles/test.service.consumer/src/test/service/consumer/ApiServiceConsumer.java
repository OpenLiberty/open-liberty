/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package test.service.consumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.ws.kernel.feature.test.api.ApiService;

/**
 *
 */
@Component(service = {})
public class ApiServiceConsumer {
    private ApiService apiService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setApiService(ApiService apiService) {
        this.apiService = apiService;
    }

    @Activate
    protected void activate() {
        if (apiService == null) {
            // This is unexpected because ApiService should be accessible
            System.out.println("ApiService - FAILED");
        } else {
            System.out.println(apiService.doTest());
        }
    }
}
