package com.almondtools.stringbench;

import java.util.List;

import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import com.almondtools.stringsandchars.search.SetBackwardOracleMatching;
import com.almondtools.stringsandchars.search.StringSearchAlgorithm;

@State(Scope.Thread)
public class SCSetBackwardOracleMatchingBenchmark extends StringsAndCharsMultiBenchmark {

	private static final String ID = "Strings and Chars Set Backward Oracle Matching";

	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public StringSearchAlgorithm create(List<String> pattern) {
		return new SetBackwardOracleMatching(pattern);
	}

}