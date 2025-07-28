package org.sc.ai.cli.chat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.sc.ai.cli.command.Spinner;
import org.springframework.stereotype.Component;

import reactor.core.Disposable;

/**
 * Tracks the current streaming subscription so it can be cancelled from signal handlers.
 */
@Component
public class StreamingContext {
    private final AtomicReference<Disposable> subscription = new AtomicReference<>();
    private final AtomicReference<CountDownLatch> latchRef = new AtomicReference<>();
    private final AtomicReference<Spinner> spinnerRef = new AtomicReference<>();

    public void register(Disposable disposable, CountDownLatch latch, Spinner spinner) {
        subscription.set(disposable);
        latchRef.set(latch);
        spinnerRef.set(spinner);
    }

    public void clear() {
        subscription.set(null);
        latchRef.set(null);
        spinnerRef.set(null);
    }

    public void cancel() {
        Disposable d = subscription.getAndSet(null);
        CountDownLatch latch = latchRef.getAndSet(null);
        Spinner spinner = spinnerRef.getAndSet(null);
        if (spinner != null) {
            spinner.stop();
        }
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
        if (latch != null) {
            latch.countDown();
        }
    }
}
