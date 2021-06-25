package org.eclipse.microprofile.rest.client.tck;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.FeatureReplacementAction;

public class SeRestClientReplacementAction extends FeatureReplacementAction {

	static final String ID = "SeRestClient";
	
	// would love to use Set.of(...) here... 
	static final Set<String> featuresToRemove = Arrays.stream(new String[]{
			"jaxrsClient-2.1",
			"cdi-2.0",
			"jsonb-1.0",
			"mpConfig-2.0",
			"mpRestClient-2.0",
			"ssl-1.0"
			}).collect(Collectors.toSet());
	static final Set<String> featuresToAdd = Collections.singleton("servlet-4.0");

	static boolean isActive() {
		return RepeatTestFilter.isRepeatActionActive(ID);
	}

	public SeRestClientReplacementAction() {
		super(featuresToRemove, featuresToAdd);
		withID(ID);
	}
}
