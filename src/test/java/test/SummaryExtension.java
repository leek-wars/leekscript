package test;

import java.util.Locale;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that:
 * - Sets the locale to French before all tests
 * - Calls TestCommon.summary() once after all test classes have finished
 */
public class SummaryExtension implements BeforeAllCallback, AfterAllCallback {

	private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(SummaryExtension.class);

	@Override
	public void beforeAll(ExtensionContext context) {
		// Set locale once (idempotent, safe to call multiple times)
		Locale.setDefault(Locale.FRENCH);
	}

	@Override
	public void afterAll(ExtensionContext context) {
		// Register a cleanup callback in the root context (shared across all test classes)
		// The CloseableResource.close() will be called once when all tests are done
		context.getRoot().getStore(NAMESPACE).getOrComputeIfAbsent("summary", key -> new CloseableResource());
	}

	private static class CloseableResource implements ExtensionContext.Store.CloseableResource {
		@Override
		public void close() {
			TestCommon.summary();
		}
	}
}
