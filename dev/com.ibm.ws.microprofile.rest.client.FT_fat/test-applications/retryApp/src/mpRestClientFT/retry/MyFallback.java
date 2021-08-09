/**
 * 
 */
package mpRestClientFT.retry;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;

public class MyFallback implements FallbackHandler<String>{

    @Override
    public String handle(ExecutionContext context) {
        return "MyFallbackClass";
    }
}
