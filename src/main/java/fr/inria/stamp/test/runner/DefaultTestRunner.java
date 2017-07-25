package fr.inria.stamp.test.runner;

import fr.inria.diversify.logger.Logger;
import fr.inria.stamp.test.filter.MethodFilter;
import fr.inria.stamp.test.listener.TestListener;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;

import java.util.Collection;
import java.util.concurrent.*;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 30/06/17
 */
public class DefaultTestRunner extends AbstractTestRunner {

	public DefaultTestRunner(String classpath) {
		super(classpath);
	}

	public DefaultTestRunner(String[] classpath) {
		super(classpath);
	}

	@Override
	public TestListener run(Class<?> classTest, Collection<String> testMethodNames) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		final TestListener listener = new TestListener();
		final Future<?> submit = executor.submit(() -> {
			Request request = Request.aClass(classTest);
			request = request.filterWith(new MethodFilter(testMethodNames));
			Runner runner = request.getRunner();
			RunNotifier runNotifier = new RunNotifier();
			runNotifier.addFirstListener(listener);
			runner.run(runNotifier);
		});
		try {
			submit.get(10000, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			submit.cancel(true);
			executor.shutdownNow();
			Logger.stopLogging();
			Logger.close();
		}
		return listener;
	}

	@Override
	public TestListener run(Class<?> classTest) {
		TestListener listener = new TestListener();
		Request request = Request.classes(classTest);
		Runner runner = request.getRunner();
		RunNotifier runNotifier = new RunNotifier();
		runNotifier.addFirstListener(listener);
		runner.run(runNotifier);
		return listener;
	}
}