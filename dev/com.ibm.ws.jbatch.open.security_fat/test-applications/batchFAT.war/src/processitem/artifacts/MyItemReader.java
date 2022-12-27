package processitem.artifacts;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.batch.api.chunk.ItemReader;

public class MyItemReader implements ItemReader {

    Logger logger = Logger.getLogger("com.ibm.ws.jbatch.open_fat");

    protected long count = 0L;
    public static final long MAX_RECORDS = 100;

    @Override
    public Serializable checkpointInfo() throws Exception {
        return new Long(count);
    }

    @Override
    public void close() throws Exception {
        logger.fine("MyItemReader close() count: " + count);
    }

    @Override
    public void open(Serializable checkpoint) throws Exception {
        logger.fine("MyItemReader.open");
        if (checkpoint == null) {
            logger.fine("MyItemReader open() checkpoint is null");
        } else {
            logger.fine("MyItemReader open() checkpoint is not null");
            count = ((Long) checkpoint).longValue();
        }
    }

    @Override
    public Object readItem() throws Exception {
        if (count < MAX_RECORDS) {
            return "DUMMY DATA: " + count++;
        }
        return null;
    }
}
