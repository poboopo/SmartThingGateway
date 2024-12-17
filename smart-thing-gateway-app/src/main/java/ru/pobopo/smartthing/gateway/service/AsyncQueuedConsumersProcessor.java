package ru.pobopo.smartthing.gateway.service;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

@Slf4j
public class AsyncQueuedConsumersProcessor<C extends Consumer<D>, D> extends Thread {
    private final List<C> consumers;
    private final BlockingQueue<D> processingQueue = new LinkedBlockingQueue<>();

    public AsyncQueuedConsumersProcessor(String name, List<C> consumers) {
        super(name);
        this.consumers = consumers;
    }

    @Override
    public void start() {
        super.start();
        log.info("Processor started");
    }

    public boolean process(D data) {
        if (data == null) {
            return false;
        }
        return processingQueue.offer(data);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                D data = processingQueue.take();
                log.debug("Processing {}", data);
                for (Consumer<D> consumer : consumers) {
                    try {
                        log.debug("Calling consumer {}", consumer.getClass());
                        consumer.accept(data);
                    } catch (Exception e) {
                        log.error("Consumer {} error: {}", consumer.getClass(), e.getMessage());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
