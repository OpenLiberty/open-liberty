/**
 * This program may be used, executed, copied, modified and distributed
 * without royalty for the purpose of developing, using, marketing, or distributing.
 **/
package com.ibm.was.wssample.client;

import java.util.concurrent.ExecutionException;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import com.ibm.was.wssample.sei.echo.EchoStringResponse;

public class CallbackHandler implements AsyncHandler <EchoStringResponse> {

	private EchoStringResponse output;
    /*
     *
     * @see javax.xml.ws.AsyncHandler#handleResponse(javax.xml.ws.Response)
     */
    public void handleResponse(Response<EchoStringResponse> response) {
        try {
            output = response.get();
        } catch (ExecutionException e) {
        	System.out.println(">> CLIENT: CALLBACK Connection Exception");
        } catch (InterruptedException e) {
        	System.out.println(">> CLIENT: CALLBACK Interrupted Exception");
        }
    }

    public EchoStringResponse getResponse(){
        return output;
    }
}
