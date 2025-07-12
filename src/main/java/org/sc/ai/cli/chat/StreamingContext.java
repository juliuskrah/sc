package org.sc.ai.cli.chat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import reactor.core.Disposable;

/**
 * Tracks the current streaming subscription so it can be cancelled from signal handlers.
 */
@Component
public class StreamingContext {
    private final AtomicReference<Disposable> subscription = new AtomicReference<>();
    private final AtomicReference<CountDownLatch> latchRef = new AtomicReference<>();

    public void register(Disposable disposable, CountDownLatch latch) {
        subscription.set(disposable);
        latchRef.set(latch);
    }

    public void clear() {
        subscription.set(null);
        latchRef.set(null);
    }

    public void cancel() {
        Disposable d = subscription.getAndSet(null);
        CountDownLatch latch = latchRef.getAndSet(null);
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
        if (latch != null) {
            latch.countDown();
        }
    }
}
