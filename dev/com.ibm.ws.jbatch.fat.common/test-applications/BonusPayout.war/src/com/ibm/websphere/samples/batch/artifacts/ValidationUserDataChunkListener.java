package com.ibm.websphere.samples.batch.artifacts;

import java.util.logging.Logger;

import javax.batch.api.chunk.listener.ChunkListener;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;


import com.ibm.websphere.samples.batch.util.BonusPayoutConstants;

@Named("ValidationUserDataChunkListener")
public class ValidationUserDataChunkListener implements ChunkListener, BonusPayoutConstants {

    private final static Logger logger = Logger.getLogger(BONUSPAYOUT_LOGGER);

    @Inject
    private StepContext stepCtx;
    
    private Integer committedUserData = null;

    @Override
    public void beforeChunk() throws Exception {
    	committedUserData = (Integer)stepCtx.getPersistentUserData();       	
    }

    /**
     * Roll back the user data value on a failure during the chunk
     */
    @Override
    public void onError(Exception ex) throws Exception {
        logger.fine("Rolling back the persistent user data to: " + committedUserData);
    	stepCtx.setPersistentUserData(committedUserData);
    }

    @Override
    public void afterChunk() throws Exception {

    }

}
