package processitem.artifacts;

import java.util.logging.Logger;

import javax.batch.api.chunk.ItemProcessor;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import batch.fat.common.util.TestForcedException;

public class MyItemProcessor implements ItemProcessor {

    Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    protected long count = 0L;
    @Inject
    protected StepContext stepContext;

    @Override
    public Object processItem(Object item) throws Exception {
        if (item == null) {
            return null;
        }
        count++;
        String line = (String) item;
        stepContext.setPersistentUserData(new Long(count));
        stepContext.setTransientUserData(new Long(count));
        if (count > 30) {
            logger.fine("*** throwing test Exception ***");
            throw new TestForcedException("test Exception from MyItemProcessor#processItem()");
        }
        return line.toLowerCase();
    }

}
