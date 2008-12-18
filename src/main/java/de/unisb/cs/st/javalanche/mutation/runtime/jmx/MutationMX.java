package de.unisb.cs.st.javalanche.mutation.runtime.jmx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang.time.StopWatch;

import com.google.common.base.Join;

import de.unisb.cs.st.javalanche.mutation.javaagent.MutationForRun;
import de.unisb.cs.st.javalanche.mutation.results.Mutation;
import de.unisb.cs.st.javalanche.mutation.results.MutationTestResult;
import de.unisb.cs.st.javalanche.mutation.results.TestMessage;

public class MutationMX implements MutationMXMBean {

	private List<Long> mutations = new ArrayList<Long>();

	private String currentTest;

	private Mutation currentMutation;

	private StopWatch mutationStopWatch = new StopWatch();

	private StopWatch testStopWatch = new StopWatch();

	public void setTest(String testName) {
		testStopWatch.reset();
		testStopWatch.start();
		currentTest = testName;
	}

	public void addMutation(Mutation mutation) {
		mutationStopWatch.reset();
		mutationStopWatch.start();
		currentMutation = mutation;
		mutations.add(mutation.getId());
	}

	public String getCurrentTest() {
		return currentTest;
	}

	public long getTestDuration() {
		return testStopWatch.getTime();
	}

	public String getMutations() {
		return Join.join(",", mutations);
	}

	public int getNumberOfMutations() {
		return mutations.size();
	}

	public long getMutationDuration() {
		return mutationStopWatch.getTime();
	}

	public String getCurrentMutation() {
		if (currentMutation != null) {
			return currentMutation.toShortString();
		}
		return "";
	}

	public String getMutationSummary() {
		MutationForRun instance = MutationForRun.getInstance();
		List<Mutation> mutationList = instance.getMutations();
		int withResult = 0;
		long totalDuration = 0;
		for (Mutation mutation : mutationList) {
			if (mutation.getMutationResult() != null) {
				withResult++;
				MutationTestResult mutationResult = mutation
						.getMutationResult();
				Collection<TestMessage> allTestMessages = mutationResult
						.getAllTestMessages();
				long duration = 0;
				for (TestMessage tm : allTestMessages) {
					duration += tm.getDuration();
				}
				totalDuration += duration;
			}
		}
		return String.format("Out of %d mutations for this run %d got results (Run for %s)",
				mutationList.size(), withResult, DurationFormatUtils.formatDurationHMS(totalDuration));
	}
}
