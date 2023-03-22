package dev.notalpha.hyphen.util;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class TestSupplierUtil {
	public static final Supplier<? extends Stream<? extends String>> STRINGS = () -> Arrays.stream(new String[]{
			"",
			"ඞඞඞ",
			"UWU69¤#r4t3yq8",
			"\u2603\uFE0F",
	});

	public static final Supplier<? extends IntStream> INTS = () -> IntStream.of(
			0,
			-1,
			Integer.MAX_VALUE,
			Integer.MIN_VALUE);

	public static final Supplier<? extends Stream<? extends Integer>> INTEGERS = () -> INTS.get().mapToObj(i -> i);

	public static final Supplier<Stream<? extends Float>> FLOATS = () -> Arrays.stream(new Float[]{
			0.0f, -0.0f,
			Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_NORMAL,
			-Float.MAX_VALUE, -Float.MIN_VALUE, -Float.MIN_NORMAL,
			Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY
	});

	public static final Supplier<Stream<? extends Number>> NUMBERS_IF = TestSupplierUtil.subClasses(INTEGERS, FLOATS);

	public static Supplier<Stream<? extends int[]>> ints(int seed, int count, int minSize, int maxSize) {
		return () -> array(INTEGERS, seed, count, minSize, maxSize, Integer.class).get().map(i -> Arrays.stream(i).mapToInt(x -> x).toArray());
	}

	/**
	 * given a stream, will limit the length to approx sqrt(stream.count)
	 */
	public static <T> Supplier<Stream<? extends T>> reduce(Supplier<? extends Stream<? extends T>> stream, int seed) {
		Random random = new Random(seed);
		return () -> {
			// reset the count for each stream
			AtomicInteger ai = new AtomicInteger();
			Stream<? extends T> s = stream.get();
			if (random.nextBoolean()) s = s.skip(1); // 50% to skip first
			return s.filter(i -> {
				double v = random.nextDouble();
				return (1 - v * v) * ai.incrementAndGet() < 1;
			});
		};
	}

	/**
	 * given a stream, will limit the length to approx powth-rooth(stream.count).
	 * For higher powers this is the correct big O, but a more accurate formula is
	 * count^(1/p)+1/p
	 */
	public static <T> Supplier<Stream<? extends T>> reduce(Supplier<? extends Stream<? extends T>> stream, int seed, int pow) {
		Random random = new Random(seed);
		return reduce(stream, pow, random);
	}

	public static <T> Supplier<Stream<? extends T>> reduce(Supplier<? extends Stream<? extends T>> stream, int pow, Random random) {
		return () -> {
			// reset the count for each stream, while also reducing the chance
			AtomicInteger ai = new AtomicInteger();
			Stream<? extends T> s = stream.get();
			if (random.nextDouble() * pow < 1) s = s.skip(1); // 1/p to keep first

			return s.filter(i -> {
				double v = random.nextDouble();
				return (1 - Math.pow(v, pow)) * ai.incrementAndGet() < 1;
			});
		};
	}

	@SafeVarargs
	public static <T> Supplier<Stream<? extends T>> subClasses(Supplier<? extends Stream<? extends T>>... subclasses) {
		return () -> Arrays.stream(subclasses).flatMap(Supplier::get);
	}


	public static <T> Supplier<Stream<? extends T>> nullable(Supplier<? extends Stream<? extends T>> supplier) {
		return () -> Stream.concat(Stream.of((T) null), supplier.get());
	}

	@SafeVarargs
	public static <T> Supplier<Stream<? extends T>> nullableSubClasses(Supplier<? extends Stream<? extends T>>... subclasses) {
		return () -> Stream.concat(Stream.of((T) null), Arrays.stream(subclasses).flatMap(Supplier::get));
	}

	/**
	 * produces arrays between lengths of min and max size (inclusive). the resulting stream will have approx count * elements.size items
	 *
	 * @param elements
	 * @param seed
	 * @param maxSize
	 */
	public static <B, T extends B> Supplier<Stream<? extends T[]>> array(Supplier<? extends Stream<? extends T>> elements, int seed, int maxSize, Class<? extends B> tClass) {
		return array(elements, seed, 1 + maxSize / 2, 0, maxSize, tClass);
	}

	/**
	 * produces arrays between lengths of min and max size (inclusive). the resulting stream will have approx count * elements.size items
	 */
	@SuppressWarnings("unchecked")
	public static <B, T extends B> Supplier<Stream<? extends T[]>> array(Supplier<? extends Stream<? extends T>> elements, int seed, int count, int minSize, int maxSize, Class<? extends B> tClass) {
		if (maxSize == 0)
			return () -> Stream.<T[]>of((T[]) Array.newInstance(tClass, 0));
		if (minSize == 0) return subClasses(() -> Stream.<T[]>of((T[]) Array.newInstance(tClass, 0)), array(
				elements, seed, count, minSize + 1, maxSize, tClass));
		Random random = new Random(seed);
		return () -> IntStream.range(0, count).mapToObj(u -> null).flatMap(u -> {
			int size = random.nextInt(maxSize - minSize + 1) + minSize;
			if (size == 0)
				return Stream.<T[]>of((T[]) Array.newInstance(tClass, 0));
			// they can't be inferred
			//noinspection RedundantTypeArguments
			return TestSupplierUtil.<B, T>crossArray(IntStream.range(0, size).mapToObj(u2 -> reduce(elements, random.nextInt(), size))
					.<Supplier<? extends Stream<? extends T>>>toArray(Supplier[]::new), tClass);
		});
	}

	public static <K, V> Supplier<Stream<? extends Map<K, V>>> map(
			Supplier<? extends Stream<? extends K>> keySupplier,
			Supplier<? extends Stream<? extends V>> valSupplier,
			int seed, int count
	) {

		var reducedValSupplier = reduce(valSupplier, seed);

		return () -> IntStream.range(0, count).mapToObj(u -> {
			var hashMap = new HashMap<K, V>();

			int emptyValueCount = 0;
			int valueIndex = 0;

			//noinspection unchecked
			var keyIterator = keySupplier.get().iterator();
			var valIterator = reducedValSupplier.get().iterator();


			while(keyIterator.hasNext()){
				var key = keyIterator.next();
				while (!valIterator.hasNext()) {
					if (valueIndex++ == 0 && ++emptyValueCount >= 3)
						return hashMap;
					// reset iterator
					valIterator = reducedValSupplier.get().iterator();
				}

				hashMap.put(key, valIterator.next());
			}

			return hashMap;
		});
	}

	public static <A, T> Supplier<Stream<? extends T>> cross(
			Supplier<? extends Stream<? extends A>> aSupplier,
			Function<? super A, ? extends T> converter
	) {
		return () -> aSupplier.get().map(converter);
	}

	public static <A, B, T> Supplier<Stream<? extends T>> cross(
			Supplier<? extends Stream<? extends A>> aSupplier,
			Supplier<? extends Stream<? extends B>> bSupplier,
			BiFunction<? super A, ? super B, ? extends T> converter
	) {
		return () -> aSupplier.get().flatMap(a ->
				bSupplier.get().map(b ->
						converter.apply(a, b)));
	}

	public static <A, B, C, T> Supplier<Stream<? extends T>> cross(
			Supplier<? extends Stream<? extends A>> aSupplier,
			Supplier<? extends Stream<? extends B>> bSupplier,
			Supplier<? extends Stream<? extends C>> cSupplier,
			TriFunction<? super A, ? super B, ? super C, ? extends T> converter
	) {
		return () -> aSupplier.get().flatMap(a ->
				bSupplier.get().flatMap(b ->
						cSupplier.get().map(c ->
								converter.apply(a, b, c))));
	}

	public static <A, B, C, D, T> Supplier<Stream<? extends T>> cross(
			Supplier<? extends Stream<? extends A>> aSupplier,
			Supplier<? extends Stream<? extends B>> bSupplier,
			Supplier<? extends Stream<? extends C>> cSupplier,
			Supplier<? extends Stream<? extends D>> dSupplier,
			QuadFunction<? super A, ? super B, ? super C, ? super D, ? extends T> converter
	) {
		return () -> aSupplier.get().flatMap(a ->
				bSupplier.get().flatMap(b ->
						cSupplier.get().flatMap(c ->
								dSupplier.get().map(d ->
										converter.apply(a, b, c, d)))));
	}

	public static <B, T extends B> Stream<? extends T[]> crossArray(
			Supplier<? extends Stream<? extends T>>[] tSuppliers,
			Class<? extends B> tClass
	) {
		//noinspection unchecked
		T[] o = (T[]) Array.newInstance(tClass, tSuppliers.length);

		return cross(tSuppliers, 0, o);
	}

	private static <T> Stream<? extends T[]> cross(
			Supplier<? extends Stream<? extends T>>[] tSuppliers, int index, T[] current
	) {
		if (index >= tSuppliers.length) return Stream.<T[]>of(current);
		if (index + 1 == tSuppliers.length) {
			return tSuppliers[index].get().map(t -> {
				T[] clone = current.clone();
				clone[index] = t;
				return clone;
			});
		} else {
			return tSuppliers[index].get().flatMap(t -> {
				T[] clone = current.clone();
				clone[index] = t;
				return cross(tSuppliers, index + 1, clone);
			});
		}
	}

	@FunctionalInterface
	public interface TriFunction<A, B, C, T> {
		T apply(A a, B b, C c);
	}

	@FunctionalInterface
	public interface QuadFunction<A, B, C, D, T> {
		T apply(A a, B b, C c, D d);
	}

	public static boolean arrayEquals(Object[] a, Object[] a2) {
		if (a == a2)
			return true;
		if (a == null || a2 == null || a.getClass() != a2.getClass())
			return false;

		return Arrays.equals(a, a2);
	}

	public static boolean arrayDeepEquals(Object[] a1, Object[] a2) {
		if (a1 == a2)
			return true;
		if (a1 == null || a2 == null || a1.getClass() != a2.getClass())
			return false;
		int length = a1.length;
		if (a2.length != length)
			return false;

		for (int i = 0; i < length; i++) {
			Object e1 = a1[i];
			Object e2 = a2[i];

			if (e1 == e2)
				continue;
			if (e1 == null || e2 == null) return false;

			Class<?> e1Class = e1.getClass();
			boolean eq;
			if (e1Class.isArray()) {
				if (e1Class != e2.getClass()) return false;
				if (e1Class.componentType().isArray())
					eq = arrayDeepEquals((Object[]) e1, (Object[]) e2);
				else if (e1 instanceof Object[])
					eq = arrayEquals((Object[]) e1, (Object[]) e2);
				else
					eq = Arrays.deepEquals(new Object[]{e1}, new Object[]{e2});
			} else
				eq = Objects.equals(e1, e2);
			if (!eq) return false;
		}
		return true;
	}

	public static int arrayHashCode(Object[] a) {
		if (a == null)
			return 0;

		int result = a.getClass().toString().hashCode();
		// toString to provide consistent hashcode over multiple runs
		// in case that is ever needed

		return result * 31 + Arrays.hashCode(a);
	}

	public static int arrayDeepHashCode(Object[] a) {
		if (a == null)
			return 0;

		int result = a.getClass().toString().hashCode();

		for (Object element : a) {
			final int elementHash;
			final Class<?> cl;
			if (element == null)
				elementHash = 0;
			else if (!element.getClass().isArray())
				elementHash = element.hashCode();
			else if (element instanceof Object[])
				elementHash = arrayDeepHashCode((Object[]) element);
			else
				elementHash = Arrays.deepHashCode(new Object[]{element});

			result = 31 * result + elementHash;
		}

		return result;
	}

	// TODO make a deep to String
	public static String arrayToString(Object[] a) {
		if (a == null)
			return "null";

		return a.getClass() + Arrays.deepToString(a);
	}
}
