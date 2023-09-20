import * as Sentry from '@sentry/node';
import { ProfilingIntegration } from '@sentry/profiling-node';
import { config } from '..';

export const isEnabled = () => {
	return config.sentry.enabled;
};

if (isEnabled())
	Sentry.init({
		dsn: config.sentry.dsn,
		integrations: [new ProfilingIntegration()],
		// Performance Monitoring
		tracesSampleRate: 1.0, // Capture 100% of the transactions, reduce in production!
		// Set sampling rate for profiling - this is relative to tracesSampleRate
		profilesSampleRate: 1.0 // Capture 100% of the transactions, reduce in production!
	});

const transaction = Sentry.startTransaction({
	op: 'test',
	name: 'My First Test Transaction'
});

setTimeout(() => {
	try {
		throw new Error('This is a test error');
	} catch (e) {
		Sentry.captureException(e);
	} finally {
		transaction.finish();
	}
}, 99);
