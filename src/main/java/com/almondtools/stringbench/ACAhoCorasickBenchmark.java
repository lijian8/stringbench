package com.almondtools.stringbench;

import java.util.ArrayList;
import java.util.List;

import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.ahocorasick.trie.Trie.TrieBuilder;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class ACAhoCorasickBenchmark extends MultiPatternMatcherBenchmark {

	private static final String ID = "AhoCorasick Aho-Corasick";
	
	private Trie trie;

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void prepare(String[] patterns) {
		TrieBuilder builder = Trie.builder().removeOverlaps();
		for (String pattern : patterns) {
	        builder.addKeyword(pattern);
		}
		trie = builder.build();
}

	@Override
	public List<Integer> find(int i, String text) {
		List<Integer> result = new ArrayList<Integer>();
		for (Emit emit : trie.parseText(text)) {
			result.add(emit.getStart());
		}
		return result;
	}
	
	
}
