package processitem.artifacts;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

import javax.batch.api.chunk.ItemWriter;

public class MyItemWriter implements ItemWriter {

    Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    protected int count = 0;

    @Override
    public Serializable checkpointInfo() throws Exception {
        return null;
    }

    @Override
    public void close() throws Exception {
        logger.fine("MyItemWriter close()");
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        logger.fine("MyItemWriter open()");
        if (checkpoint == null) {
            logger.fine("MyItemWriter open() checkpoint is null");
        } else {
            logger.fine("MyItemWriter open() checkpoint is not null");
        }
    }

    @Override
    public void writeItems(List<Object> itemList) throws Exception {
        long localCount = 0;
        for (Object obj : itemList) {
            String line = (String) obj;
            localCount++;
        }
        count += localCount;
    }
}
