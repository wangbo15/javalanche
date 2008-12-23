package de.unisb.cs.st.javalanche.mutation.runtime.jmx;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import de.unisb.cs.st.javalanche.mutation.analyze.MutationAnalyzer;
import de.unisb.cs.st.javalanche.mutation.javaagent.MutationForRun;
import de.unisb.cs.st.javalanche.mutation.properties.MutationProperties;
import de.unisb.cs.st.javalanche.mutation.results.Mutation;
import de.unisb.cs.st.javalanche.mutation.results.MutationTestResult;
import de.unisb.cs.st.javalanche.mutation.results.TestMessage;
import de.unisb.cs.st.javalanche.mutation.results.persistence.HibernateUtil;
import de.unisb.cs.st.javalanche.mutation.results.persistence.QueryManager;
import de.unisb.cs.st.javalanche.mutation.runtime.testDriver.MutationTestDriver;

public class MutationMxClient {

	private static final boolean DEBUG_ADD = false;

	public static boolean connect(int i) {
		JMXConnector jmxc = null;
		JMXServiceURL url = null;

		try {
			url = new JMXServiceURL(MXBeanRegisterer.ADDRESS + i);
			jmxc = JMXConnectorFactory.connect(url, null);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			return false;
			// System.out.println("Could not connect to address: " + url);
			// e.printStackTrace();
		}
		if (jmxc != null) {
			try {
				MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
				ObjectName objectName = new ObjectName(
						MXBeanRegisterer.OBJECT_NAME);
				Object numberOfMutations = mbsc.getAttribute(objectName,
						"NumberOfMutations");
				Object currentTest = mbsc.getAttribute(objectName,
						"CurrentTest");
				Object currentMutation = mbsc.getAttribute(objectName,
						"CurrentMutation");
				Object allMutations = mbsc
						.getAttribute(objectName, "Mutations");
				Object mutationsDuration = mbsc.getAttribute(objectName,
						"MutationDuration");
				Object testDuration = mbsc.getAttribute(objectName,
						"TestDuration");
//				Object mutationSummary = mbsc.getAttribute(objectName,
//						"MutationSummary");

				final RuntimeMXBean remoteRuntime = ManagementFactory
						.newPlatformMXBeanProxy(mbsc,
								ManagementFactory.RUNTIME_MXBEAN_NAME,
								RuntimeMXBean.class);

				final MemoryMXBean remoteMemory = ManagementFactory
						.newPlatformMXBeanProxy(mbsc,
								ManagementFactory.MEMORY_MXBEAN_NAME,
								MemoryMXBean.class);
				System.out.print("Connection: " + i + "  ");
				System.out.println("Target VM: " + remoteRuntime.getName()
						+ " - " + remoteRuntime.getVmVendor() + " - "
						+ remoteRuntime.getSpecVersion() + " - "
						+ remoteRuntime.getVmVersion());
				System.out.println("Running for: "
						+ DurationFormatUtils.formatDurationHMS(remoteRuntime
								.getUptime()));
				System.out.println("Memory usage: Heap - "
						+ formatMemory(remoteMemory.getHeapMemoryUsage()
								.getUsed())
						+ "  Non Heap - "
						+ formatMemory(remoteMemory.getNonHeapMemoryUsage()
								.getUsed()));

				String mutationDurationFormatted = DurationFormatUtils
						.formatDurationHMS(Long.parseLong(mutationsDuration
								.toString()));
				String testDurationFormatted = DurationFormatUtils
						.formatDurationHMS(Long.parseLong(testDuration
								.toString()));
				if (DEBUG_ADD) {
					System.out.println("Classpath: "
							+ remoteRuntime.getClassPath());
					System.out.println("Args: "
							+ remoteRuntime.getInputArguments());
					System.out.println("All Mutations: " + allMutations);
				}
//				System.out.println(mutationSummary);
				System.out.println("Current mutation (Running for: "
						+ mutationDurationFormatted + "): " + currentMutation);
				System.out.println("Mutations tested: " + numberOfMutations);
				System.out.println("Current test:   (Running for: "
						+ testDurationFormatted + "): " + currentTest);


			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (MalformedObjectNameException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (AttributeNotFoundException e) {
				e.printStackTrace();
			} catch (InstanceNotFoundException e) {
				e.printStackTrace();
			} catch (MBeanException e) {
				e.printStackTrace();
			} catch (ReflectionException e) {
				e.printStackTrace();
			}finally{
				try {
					jmxc.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

	private static String formatMemory(long used) {
		double mem = used / (1024. * 1024.);
		return String.format("%.2f MB", mem);
	}

	public static void main(String[] args) throws IOException {
		List<Integer> noConnection = new ArrayList<Integer>();
		boolean oneConnection = false;
		for (int i = 0; i < 100; i++) {
			boolean result = connect(i);
			if (!result) {
				noConnection.add(i);
			} else {
				oneConnection = true;
				System.out
						.println("--------------------------------------------------------------------------------");
			}

		}
		if (!oneConnection) {
			System.out.println("Got no connection for ids: " + noConnection);
		}
		analyzeDB();
	}

	private static void analyzeDB() {
		Session session = HibernateUtil.openSession();
		Transaction transaction = session.beginTransaction();
		Query query = session
				.createQuery("from Mutation WHERE mutationResult != null AND className LIKE '"
						+ MutationProperties.PROJECT_PREFIX + "%' ");
		// query.setMaxResults(100);
		List<Mutation> list = query.list();
		transaction.commit();
		session.close();
//		RunInfo r = getRunInfo(list);
		RunInfo r = getFastRunInfo(list);
		long averageRuntimeMutation = r.totalDuration / r.mutations;
		long averageRuntimeTest = r.totalDuration / r.tests;
		System.out.printf("Total runtime for %d mutations with %d tests: %s\n",
				r.mutations, r.tests, DurationFormatUtils
						.formatDurationHMS(r.totalDuration));
		System.out.printf("Average mutation runtime: %s\n", DurationFormatUtils
				.formatDurationHMS(averageRuntimeMutation));
		System.out.printf("Average test runtime: %s\n", DurationFormatUtils
				.formatDurationHMS(averageRuntimeTest));
		System.out.printf("Restarts %d.\n", r.restarts);

	}

	private static RunInfo getFastRunInfo(List<Mutation> list) {
		long totalDuration = QueryManager.getResultFromSQLCountQuery("SELECT sum(duration) FROM TestMessage T");
		long mutations = QueryManager.getResultFromSQLCountQuery("SELECT count(*) FROM Mutation M WHERE mutationResult_id AND className LIKE '" + MutationProperties.PROJECT_PREFIX + "%'");
		long restarts = QueryManager.getResultFromSQLCountQuery("SELECT count(*) FROM TestMessage T WHERE message LIKE '" + MutationTestDriver.RESTART_MESSAGE + "%'");
		long tests = QueryManager.getResultFromSQLCountQuery("SELECT count(*) FROM TestMessage T");
		return new RunInfo(totalDuration, (int) mutations, (int) restarts, tests);
	}

	public static RunInfo getRunInfo(List<Mutation> list) {
		long totalDuration = 0;
		int mutations = 0;
		int restarts = 0;
		long tests = 0;

		for (Mutation mutation : list) {
			mutations++;
			MutationTestResult mutationResult = mutation.getMutationResult();
			Collection<TestMessage> allTestMessages = mutationResult
					.getAllTestMessages();
			for (TestMessage testMessage : allTestMessages) {
				tests++;
				totalDuration += testMessage.getDuration();
				if (testMessage.getMessage().equals(
						MutationTestDriver.RESTART_MESSAGE)) {
					restarts++;
				}
			}
		}
		return new RunInfo(totalDuration, mutations, restarts, tests);
	}

	private static class RunInfo {
		long totalDuration = 0;
		int mutations = 0;
		int restarts = 0;
		long tests = 0;

		public RunInfo(long totalDuration, int mutations, int restarts,
				long tests) {
			super();
			this.totalDuration = totalDuration;
			this.mutations = mutations;
			this.restarts = restarts;
			this.tests = tests;
		}

	}
}
