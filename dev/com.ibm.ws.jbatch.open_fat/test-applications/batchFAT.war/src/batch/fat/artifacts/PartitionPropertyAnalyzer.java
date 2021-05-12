package batch.fat.artifacts;

import java.io.Serializable;

import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

public class PartitionPropertyAnalyzer extends AbstractPartitionAnalyzer{

	@Inject
	StepContext stepCtx;
	
	@Override
	public void analyzeCollectorData(Serializable data) throws Exception {
		stepCtx.setPersistentUserData(stepCtx.getPersistentUserData() + (String) data);
	}
}