package net.gudenau.minecraft.multihead;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

final class Reprobate {
    private Reprobate() {
        throw new AssertionError();
    }

    private static final Unsafe UNSAFE = findUnsafe();
    private static final MethodHandles.Lookup IMPL_LIKE = createLookup(findOverride());
    private static final Object INTERNAL_UNSAFE = findInternalUnsafe();

    // private native void copyMemory0(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes);
    private static final MethodHandle copyMemory0;
    static {
        try {
            copyMemory0 = IMPL_LIKE.bind(INTERNAL_UNSAFE, "copyMemory0", MethodType.methodType(void.class, Object.class, long.class, Object.class, long.class, long.class));
        } catch (Throwable e) {
            throw new RuntimeException("Failed to find method(s) from the internal Unsafe", e);
        }
    }

    @NotNull
    private static Unsafe findUnsafe() {
        var exceptions = new ArrayList<Throwable>();

        for (var field : Unsafe.class.getDeclaredFields()) {
            if(field.getType() == Unsafe.class && Modifier.isStatic(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                    if(field.get(null) instanceof Unsafe unsafe) {
                        return unsafe;
                    }
                } catch (Throwable e) {
                    exceptions.add(e);
                }
            }
        }

        var exception = new AssertionError("No unsafe, can not continue.");
        exceptions.forEach(exception::addSuppressed);
        throw exception;
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocateInstance(Class<T> type) {
        try {
            return (T) UNSAFE.allocateInstance(type);
        } catch (InstantiationException e) {
            throw new RuntimeException("Java is being mean to our hacks", e);
        }
    }

    private static long findOverride() {
        var object = allocateInstance(AccessibleObject.class);

        for(long offset = 4; offset < 16; offset++) {
            object.setAccessible(false);
            if(UNSAFE.getBoolean(object, offset)) {
                continue;
            }
            object.setAccessible(true);
            if(UNSAFE.getBoolean(object, offset)) {
                return offset;
            }
        }

        throw new RuntimeException("Failed to find override");
    }

    private static MethodHandles.Lookup createLookup(long override) {
        try {
            var constructor = MethodHandles.Lookup.class.getDeclaredConstructor(Class.class, Class.class, int.class);
            UNSAFE.putBoolean(constructor, override, true);
            return constructor.newInstance(Object.class, null, -1);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to create lookup", e);
        }
    }

    private static Object findInternalUnsafe() {
        try {
            var type = Class.forName("jdk.internal.misc.Unsafe");
            var unsafe = Stream.of(type.getDeclaredFields())
                .filter((field) -> field.getType() == type && Modifier.isStatic(field.getModifiers()))
                .findFirst()
                .orElseThrow();

            return IMPL_LIKE.unreflectGetter(unsafe).invoke();
        } catch (Throwable e) {
            throw new RuntimeException("The JVM is being mean", e);
        }
    }

    private record CopyInfo(
        long offset,
        long size
    ) {}

    private static final Map<Class<?>, CopyInfo> COPY_INFO = new Object2ObjectOpenHashMap<>();

    @NotNull
    private static CopyInfo copyInfo(Class<?> type) {
        synchronized (COPY_INFO) {
            return COPY_INFO.computeIfAbsent(type, Reprobate::createCopyInfo);
        }
    }

    @NotNull
    public static <T> T copy(@NotNull T value) {
        try {
            @SuppressWarnings("unchecked")
            var type = (Class<T>) value.getClass();
            var info = copyInfo(type);
            var copy = allocateInstance(type);
            copyMemory(value, info.offset(), copy, info.offset(), info.size());
            return copy;
        } catch (Throwable e) {
            throw new AssertionError("Oh no, how could this happen to me?", e);
        }
    }

    private static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        try {
            copyMemory0.invokeExact(srcBase, srcOffset, destBase, destOffset, bytes);
        } catch (Throwable e) {
            throw new RuntimeException("Failed to copy memory", e);
        }
    }

    @NotNull
    private static CopyInfo createCopyInfo(Class<?> type) {
        record FieldInfo(Field field, long offset) {
            private FieldInfo(Field field) {
                this(field, UNSAFE.objectFieldOffset(field));
            }
        }

        var offsets = Stream.of(type.getDeclaredFields())
            .filter((field) -> !Modifier.isStatic(field.getModifiers()))
            .map(FieldInfo::new)
            .toArray(FieldInfo[]::new);

        var min = Stream.of(offsets).min(Comparator.comparingLong(a -> a.offset)).orElseThrow();
        var max = Stream.of(offsets).max(Comparator.comparingLong(a -> a.offset)).orElseThrow();

        int maxSize = 4;
        if(max.field.getType() == long.class || max.field.getType() == double.class) {
            maxSize = 8;
        }

        return new CopyInfo(min.offset, max.offset + maxSize);
    }
}
