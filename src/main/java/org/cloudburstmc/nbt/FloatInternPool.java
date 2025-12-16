package org.cloudburstmc.nbt;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * A pool for interning {@link Float} values, with support for very high concurrency.
 *
 * @author DaPorkchop_, adapted by Camotoy
 */
public final class FloatInternPool {
    private static final Map<Object, WeakFloat> CACHE = new ConcurrentSkipListMap<>((a, b) -> {
        if (a instanceof Float) { //this is a retrieval, unbox the temporary boxed float before comparison
            return Float.compare((Float) a, ((WeakFloat) b).value);
        } else { //this is an insertion/removal, compare the values
            return Float.compare(((WeakFloat) a).value, ((WeakFloat) b).value);
        }
    });

    private static final ReferenceQueue<?>[] REFERENCE_QUEUES = IntStream.range(0, Runtime.getRuntime().availableProcessors())
            .mapToObj(i -> new ReferenceQueue<>())
            .toArray(ReferenceQueue[]::new);

    private static void expungeStaleEntries(@NotNull ReferenceQueue<?> referenceQueue) {
        WeakFloat entry;
        while ((entry = (WeakFloat) referenceQueue.poll()) != null) {
            CACHE.remove(entry, entry);
        }
    }

    public static Float intern(float value) {
        Float boxed = value;
        Float interned;

        //check to see if value is already cached
        WeakFloat entry = CACHE.get(boxed);
        if (entry != null) {
            expungeStaleEntries(entry.referenceQueue);

            if ((interned = entry.get()) != null) { //cached value hasn't been GC'd
                return interned; //re-use existing cached value
            }
        }

        //try to insert a new element
        WeakFloat newEntry = new WeakFloat(boxed, REFERENCE_QUEUES[ThreadLocalRandom.current().nextInt(REFERENCE_QUEUES.length)]);
        do {
            entry = CACHE.putIfAbsent(newEntry, newEntry);
            if (entry != null) { //another thread inserted their entry before us, let's try to re-use that
                expungeStaleEntries(entry.referenceQueue);
                interned = entry.get();
            } else { //we won the race to insert a new entry
                interned = boxed;
            }
        } while (interned == null);

        return interned;
    }

    private static final class WeakFloat extends WeakReference<Float> {
        private final float value;
        private final ReferenceQueue<?> referenceQueue;

        @SuppressWarnings("unchecked")
        public WeakFloat(@NotNull Float value, @NotNull ReferenceQueue referenceQueue) {
            super(value, referenceQueue);

            this.value = value;
            this.referenceQueue = referenceQueue;
        }
    }
}
