package batch.fat.artifacts;

import java.io.Serializable;

import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

public class PartitionIDAnalyzer extends AbstractPartitionAnalyzer {

	@Inject
	JobContext jobCtx;

	@Override
	public void analyzeCollectorData(Serializable data) throws Exception {
		jobCtx.setExitStatus(jobCtx.getExitStatus() + data);
	}
	
}