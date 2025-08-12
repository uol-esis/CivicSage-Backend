package de.uol.pgdoener.civicsage.business.embedding.backlog;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class InMemoryEmbeddingBacklog implements EmbeddingBacklog {

    private final Map<UUID, EmbeddingTask> taskMap = new HashMap<>();
    private final Map<EmbeddingPriority, Deque<EmbeddingTask>> backlog = new EnumMap<>(EmbeddingPriority.class);

    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public InMemoryEmbeddingBacklog() {
        for (EmbeddingPriority priority : EmbeddingPriority.values()) {
            backlog.put(priority, new LinkedList<>());
        }
    }

    @Override
    public void add(EmbeddingTask task, EmbeddingPriority priority) {
        lock.lock();
        try {
            if (taskMap.containsKey(task.sourceId()))
                return;
            taskMap.put(task.sourceId(), task);
            Queue<EmbeddingTask> queue = backlog.get(priority);
            queue.add(task);
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public EmbeddingTask peek() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (taskMap.isEmpty()) {
                notEmpty.await();
            }
            for (EmbeddingPriority priority : EmbeddingPriority.values()) {
                Deque<EmbeddingTask> queue = backlog.get(priority);
                EmbeddingTask task = queue.peek();
                if (task != null) {
                    return task;
                }
            }
            // This should never happen
            throw new IllegalStateException("No tasks available in the backlog. This indicates a bug in the implementation.");
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<EmbeddingTask> remove(EmbeddingTask task) {
        lock.lock();
        try {
            if (!taskMap.containsKey(task.sourceId()))
                return Optional.empty();
            taskMap.remove(task.sourceId());
            for (EmbeddingPriority priority : EmbeddingPriority.values()) {
                Deque<EmbeddingTask> queue = backlog.get(priority);
                Iterator<EmbeddingTask> iterator = queue.iterator();
                while (iterator.hasNext()) {
                    EmbeddingTask currentTask = iterator.next();
                    if (currentTask.equals(task)) {
                        iterator.remove();
                        // If the task was found and removed, we can exit early.
                        return Optional.of(currentTask);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
        return Optional.empty();
    }

    @Override
    public void defer(EmbeddingTask task) {
        lock.lock();
        try {
            remove(task);
            add(task, EmbeddingPriority.LOW);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Collection<UUID> getSourceIds() {
        lock.lock();
        try {
            return Collections.unmodifiableSet(taskMap.keySet());
        } finally {
            lock.unlock();
        }
    }

}
