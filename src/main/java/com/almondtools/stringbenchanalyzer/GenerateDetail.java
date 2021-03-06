package com.almondtools.stringbenchanalyzer;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class GenerateDetail {

	private String file;
	private int number;
	private int alphabet;
	private int pattern;

	public GenerateDetail(String file, int number, int alphabet, int pattern) {
		this.file = file;
		this.number = number;
		this.alphabet = alphabet;
		this.pattern = pattern;
	}

	public void run() throws IOException {
		Map<BenchmarkKey, List<BenchmarkRecord>> records = allRecords().stream()
			.filter(record -> record.getNumber() == number)
			.filter(record -> record.getAlphabet() == alphabet)
			.filter(record -> record.getPatternSize() == pattern)
			.collect(groupingBy(benchmark -> new BenchmarkKey(benchmark)));
		for (Map.Entry<BenchmarkKey, List<BenchmarkRecord>> entry : records.entrySet()) {
			BenchmarkKey key = entry.getKey();
			List<BenchmarkRecord> benchmarks = entry.getValue();
			String csv = file.substring(0, file.length() - 4) + "_" + key + "-" + alphabet + "-" + pattern + ".csv";
			String html = file.substring(0, file.length() - 4) + "_" + key + "-" + alphabet + "-" + pattern + ".html";
			writeSCSV(key, benchmarks, csv);
			writeChart(key, html, csv);
		}

	}

	private void writeSCSV(BenchmarkKey key, List<BenchmarkRecord> benchmarks, String filename) throws IOException {
		Files.write(Paths.get(filename), new Iterable<String>() {

			@Override
			public Iterator<String> iterator() {
				return Stream.concat(Stream.of("name,family,number,alphabet,pattern,time"), benchmarks.stream()
					.sorted(comparing(BenchmarkRecord::getAlphabet).thenComparing(BenchmarkRecord::getPatternSize))
					.map(value -> value.benchmark()))
					.iterator();
			}

		}, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private void writeChart(BenchmarkKey key, String name, String csv) throws IOException {
		int pos = csv.lastIndexOf("benchmarkresults");
		String fileName = csv.substring(pos + "benchmarkresults".length() + 1);
		
		String templateName = "benchmarkresults/detail.html";
		String template = Files.readAllLines(Paths.get(templateName)).stream().collect(joining("\n"));

		String title = key.getTitle();

		String content = template.replace("${title}", title).replace("${file}", fileName);

		Files.write(Paths.get(name), asList(content), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

	}

	public Stream<BenchmarkRecord> noBenchmarks() {
		return Stream.<BenchmarkRecord> empty();
	}

	public Comparator<BenchmarkRecord> benchmarkOrder() {
		return naturalOrder();
	}

	public List<BenchmarkRecord> allRecords() throws IOException {
		List<BenchmarkRecord> records = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(file), Charset.forName("UTF-8"))) {
			skipFirstLine(reader);
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				StreamTokenizer tokens = tokenizer(line);

				String benchmark = stringLiteral(tokens);

				String benchmarkName = benchmark(benchmark);
				String name = name(benchmark);
				Family family = family(benchmark);

				skip(tokens, 7);

				double time = time(stringLiteral(tokens));

				skip(tokens, 5);

				int alphabet = intLiteral(tokens);

				skip(tokens);

				int patternNumber = optIntLiteral(tokens).orElse(1);

				skip(tokens);

				int patternSize = intLiteral(tokens);

				records.add(new BenchmarkRecord(benchmarkName, patternNumber, alphabet, patternSize, time, name, family));

			}
		}
		return records;
	}

	public void skip(StreamTokenizer tokens, int times) throws IOException {
		for (int i = 0; i < times; i++) {
			skip(tokens);
		}
	}

	public void skip(StreamTokenizer tokens) throws IOException {
		tokens.nextToken();
	}

	public String stringLiteral(StreamTokenizer tokens) throws IOException {
		int type = tokens.nextToken();
		if (type != '\"') {
			throw new RuntimeException();
		}
		return tokens.sval;
	}

	public int intLiteral(StreamTokenizer tokens) throws IOException {
		int type = tokens.nextToken();
		if (type != StreamTokenizer.TT_NUMBER) {
			throw new RuntimeException();
		}
		return (int) tokens.nval;
	}

	public Optional<Integer> optIntLiteral(StreamTokenizer tokens) throws IOException {
		int type = tokens.nextToken();
		if (type == ',') {
			tokens.pushBack();
			return Optional.empty();
		} else if (type != StreamTokenizer.TT_NUMBER) {
			throw new RuntimeException();
		}
		return Optional.of((int) tokens.nval);
	}

	public StreamTokenizer tokenizer(String line) {
		StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(line));
		tokenizer.quoteChar('"');
		return tokenizer;
	}

	private String benchmark(String string) {
		int pos = string.lastIndexOf('.');
		String benchmarkName = string.substring(pos + 1);
		return benchmarkName;
	}

	private String name(String string) {
		try {
			String[] parts = string.substring(1, string.length() - 1).split("\\.");
			return parts[parts.length - 2].replace("Benchmark", "");
		} catch (NullPointerException | NumberFormatException e) {
			return "<unknown>";
		}
	}

	private Family family(String string) {
		try {
			int pos = string.lastIndexOf('.');
			String className = string.substring(0, pos);
			Class<?> clazz = Class.forName(className);
			Method familyMethod = clazz.getMethod("getFamily");
			return (Family) familyMethod.invoke(clazz.newInstance());
		} catch (NullPointerException | ReflectiveOperationException e) {
			return Family.SPECIAL;
		}
	}

	private double time(String string) {
		try {
			return NumberFormat.getNumberInstance().parse(string).doubleValue();
		} catch (NullPointerException | NumberFormatException | ParseException e) {
			return Double.NaN;
		}
	}

	public void skipFirstLine(BufferedReader reader) throws IOException {
		reader.readLine();
	}

}
