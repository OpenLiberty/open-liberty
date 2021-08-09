package com.ibm.websphere.samples.batch.artifacts;

import java.util.List;

import javax.batch.api.chunk.listener.AbstractItemWriteListener;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Though we may have been able to overload the reader's checkpoint, we purposely
 * go out of our way to instead make use of persistent userdata in this class to
 * count the number of records validated in this step or this partition.
 */
@Named("ValidationCountUpdatingWriteListener")
public class ValidationCountUpdatingWriteListener extends AbstractItemWriteListener {

	@Inject
	StepContext stepCtx;

	@Override
	public void afterWrite(List<Object> items) throws Exception {
		updateCumulativeCounts(items.size());
	}

	private void updateCumulativeCounts(int size) {

		Integer previousCumulativeCount = (Integer) stepCtx.getPersistentUserData();
		if (previousCumulativeCount == null) {
			previousCumulativeCount = 0;
		}

		// Update count based on new chunk size
		Integer newCumulativeCount = previousCumulativeCount + size;

		stepCtx.setExitStatus(Integer.toString(newCumulativeCount));
		stepCtx.setPersistentUserData(newCumulativeCount);
	}

}
