/*
 * 版权 2002-2022 原作者或作者。
 *
 * 根据Apache许可证2.0版（“许可证”）授权；
 * 除非符合许可证，否则不得使用此文件。
 * 您可以在以下位置获得许可证副本：
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则在许可证下分发的软件
 * 将按“原样”基础分发，不附带任何明示或暗示的保证或条件。
 * 有关许可证下允许和限制的特定语言，请参阅许可证。
 */

package org.springframework.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * 了解常见Java和Spring集合类型的集合工厂。
 *
 * <p>主要用于框架内部使用。
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Oliver Gierke
 * @author Sam Brannen
 * @since 1.1.1
 */
public final class CollectionFactory {

	private static final Set<Class<?>> approximableCollectionTypes = Set.of(
			// 标准集合接口
			Collection.class,
					List.class,
			Set.class,
			SortedSet.class,
			NavigableSet.class,
			// 常见的具体集合类
			ArrayList.class,
			LinkedList.class,
			HashSet.class,
			LinkedHashSet.class,
			TreeSet.class,
			EnumSet.class);

	private static final Set<Class<?>> approximableMapTypes = Set.of(
			// 标准映射接口
			Map.class,
			SortedMap.class,
			NavigableMap.class,
			// 常见的具体映射类
			HashMap.class,
			LinkedHashMap.class,
			TreeMap.class,
			EnumMap.class);


	private CollectionFactory() {
	}


	/**
	 * 确定给定的集合类型是否为<em>可近似</em>类型，
	 * 即 {@link #createApproximateCollection} 可以近似的类型。
	 * @param collectionType 要检查的集合类型
	 * @return 如果类型是<em>可近似</em>，则返回{@code true}
	 */
	public static boolean isApproximableCollectionType(@Nullable Class<?> collectionType) {
		return (collectionType != null && approximableCollectionTypes.contains(collectionType));
	}

	/**
	 * 为给定的集合创建最接近的集合。
	 * <p><strong>警告</strong>：由于参数化类型 {@code E} 没有绑定到所提供的
	 * {@code collection} 中包含的元素类型，因此如果所提供的 {@code collection} 是
	 * {@link EnumSet}，则无法保证类型安全。在这种情况下，调用者有责任确保所提供的
	 * {@code collection} 的元素类型是与类型 {@code E} 匹配的枚举类型。或者，调用者可能希望将返回值视为原始集合或 {@link Object} 的集合。
	 * @param collection 原始集合对象，可能为 {@code null}
	 * @param capacity 初始容量
	 * @return 一个新的、空的集合实例
	 * @see #isApproximableCollectionType
	 * @see java.util.LinkedList
	 * @see java.util.ArrayList
	 * @see java.util.EnumSet
	 * @see java.util.TreeSet
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <E> Collection<E> createApproximateCollection(@Nullable Object collection, int capacity) {
		if (collection instanceof LinkedList) {
			return new LinkedList<>();
		}
		else if (collection instanceof List) {
			return new ArrayList<>(capacity);
		}
		else if (collection instanceof EnumSet enumSet) {
			Collection<E> copy = EnumSet.copyOf(enumSet);
			copy.clear();
			return copy;
		}
		else if (collection instanceof SortedSet sortedSet) {
			return new TreeSet<>(sortedSet.comparator());
		}
		else {
			return new LinkedHashSet<>(capacity);
		}
	}

	/**
	 * 为给定的集合类型创建最合适的集合。
	 * <p>将委托给 {@link #createCollection(Class, Class, int)} 使用
	 * {@code null} 元素类型。
	 * @param collectionType 目标集合的期望类型（永远不会是 {@code null}）
	 * @param capacity 初始容量
	 * @return 一个新的集合实例
	 * @throws IllegalArgumentException 如果提供的 {@code collectionType}
	 * 是 {@code null} 或类型为 {@link EnumSet}
	 */
	public static <E> Collection<E> createCollection(Class<?> collectionType, int capacity) {
		return createCollection(collectionType, null, capacity);
	}

	/**
	 * 为给定的集合类型创建最合适的集合。
	 * <p><strong>警告</strong>：由于参数化类型 {@code E} 没有绑定到所提供的
	 * {@code elementType}，因此如果所需的 {@code collectionType} 是 {@link EnumSet}，
	 * 则无法保证类型安全。在这种情况下，调用者有责任确保所提供的 {@code elementType} 是与类型 {@code E} 匹配的枚举类型。
	 * 或者，调用者可能希望将返回值视为原始集合或 {@link Object} 的集合。
	 * @param collectionType 目标集合的期望类型（永远不会是 {@code null}）
	 * @param elementType 集合的元素类型，或 {@code null} 如果未知
	 * （注意：仅对 {@link EnumSet} 创建相关）
	 * @param capacity 初始容量
	 * @return 一个新的集合实例
	 * @throws IllegalArgumentException 如果提供的 {@code collectionType} 是
	 * {@code null}；或者所需的 {@code collectionType} 是 {@link EnumSet} 并且
	 * 提供的 {@code elementType} 不是 {@link Enum} 的子类型
	 * @since 4.1.3
	 * @see java.util.LinkedHashSet
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.EnumSet
	 */
	@SuppressWarnings("unchecked")
	public static <E> Collection<E> createCollection(Class<?> collectionType, @Nullable Class<?> elementType, int capacity) {
		Assert.notNull(collectionType, "Collection type must not be null");
		if (LinkedHashSet.class == collectionType || HashSet.class == collectionType ||
				Set.class == collectionType || Collection.class == collectionType) {
			return new LinkedHashSet<>(capacity);
		}
		else if (ArrayList.class == collectionType || List.class == collectionType) {
			return new ArrayList<>(capacity);
		}
		else if (LinkedList.class == collectionType) {
			return new LinkedList<>();
		}
		else if (TreeSet.class == collectionType || NavigableSet.class == collectionType
				|| SortedSet.class == collectionType) {
			return new TreeSet<>();
		}
		else if (EnumSet.class.isAssignableFrom(collectionType)) {
			Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
			return EnumSet.noneOf(asEnumType(elementType));
		}
		else {
			if (collectionType.isInterface() || !Collection.class.isAssignableFrom(collectionType)) {
				throw new IllegalArgumentException("Unsupported Collection type: " + collectionType.getName());
			}
			try {
							return (Collection<E>) ReflectionUtils.accessibleConstructor(collectionType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
					"Could not instantiate Collection type: " + collectionType.getName(), ex);
			}
		}
	}

	/**
	 * 确定给定的映射类型是否是一个<em>可近似</em>类型，
	 * 即 {@link #createApproximateMap} 可以近似的类型。
	 * @param mapType 要检查的映射类型
	 * @return 如果类型是<em>可近似</em>，则返回 {@code true}
	 */
	public static boolean isApproximableMapType(@Nullable Class<?> mapType) {
		return (mapType != null && approximableMapTypes.contains(mapType));
	}

	/**
	 * 为给定的映射创建最近似的映射。
	 * <p><strong>警告</strong>：由于参数化类型 {@code K} 没有绑定到所提供的
	 * {@code map} 中包含的键类型，因此如果所提供的 {@code map} 是
	 * {@link EnumMap}，则无法保证类型安全。在这种情况下，调用者有责任确保所提供的
	 * {@code map} 的键类型是与类型 {@code K} 匹配的枚举类型。或者，调用者可能希望将返回值视为原始映射或由 {@link Object} 键入的映射。
	 * @param map 原始映射对象，可能为 {@code null}
	 * @param capacity 初始容量
	 * @return 一个新的、空的映射实例
	 * @see #isApproximableMapType
	 * @see java.util.EnumMap
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <K, V> Map<K, V> createApproximateMap(@Nullable Object map, int capacity) {
		if (map instanceof EnumMap enumMap) {
			EnumMap copy = new EnumMap(enumMap);
			copy.clear();
			return copy;
		}
		else if (map instanceof SortedMap sortedMap) {
			return new TreeMap<>(sortedMap.comparator());
		}
		else {
			return new LinkedHashMap<>(capacity);
		}
	}

	/**
	 * 为给定的映射类型创建最合适的映射。
	 * <p>将委托给 {@link #createMap(Class, Class, int)} 使用
	 * {@code null} 键类型。
	 * @param mapType 目标映射的期望类型
	 * @param capacity 初始容量
	 * @return 一个新的映射实例
	 * @throws IllegalArgumentException 如果提供的 {@code mapType} 是
	 * {@code null} 或类型为 {@link EnumMap}
	 */
	public static <K, V> Map<K, V> createMap(Class<?> mapType, int capacity) {
		return createMap(mapType, null, capacity);
	}

	/**
	 * 为给定的映射类型创建最合适的映射。
	 * <p><strong>警告</strong>：由于参数化类型 {@code K}
	 * 没有绑定到所提供的 {@code keyType}，因此如果所需的 {@code mapType} 是 {@link EnumMap}，
	 * 则无法保证类型安全。在这种情况下，调用者有责任确保 {@code keyType}
	 * 是与类型 {@code K} 匹配的枚举类型。或者，调用者可能希望将返回值视为原始映射或由
	 * {@link Object} 键入的映射。类似地，如果所需的 {@code mapType} 是 {@link MultiValueMap}，
	 * 也无法强制执行类型安全。
	 * @param mapType 目标映射的期望类型（永远不会是 {@code null}）
	 * @param keyType 映射的键类型，或 {@code null} 如果未知
	 * （注意：仅对 {@link EnumMap} 创建相关）
	 * @param capacity 初始容量
	 * @return 一个新的映射实例
	 * @throws IllegalArgumentException 如果提供的 {@code mapType} 是
	 * {@code null}；或者所需的 {@code mapType} 是 {@link EnumMap} 并且
	 * 提供的 {@code keyType} 不是 {@link Enum} 的子类型
	 * @since 4.1.3
	 * @see java.util.LinkedHashMap
	 * @see java.util.TreeMap
	 * @see org.springframework.util.LinkedMultiValueMap
	 * @see java.util.EnumMap
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <K, V> Map<K, V> createMap(Class<?> mapType, @Nullable Class<?> keyType, int capacity) {
		Assert.notNull(mapType, "Map type must not be null");
		if (mapType.isInterface()) {
			if (Map.class == mapType) {
				return new LinkedHashMap<>(capacity);
			}
			else if (SortedMap.class == mapType || NavigableMap.class == mapType) {
				return new TreeMap<>();
			}
			else if (MultiValueMap.class == mapType) {
				return new LinkedMultiValueMap();
			}
			else {
				throw new IllegalArgumentException("Unsupported Map interface: " + mapType.getName());
			}
		}
		else if (EnumMap.class == mapType) {
			Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
			return new EnumMap(asEnumType(keyType));
		}
		else {
			if (!Map.class.isAssignableFrom(mapType)) {
				throw new IllegalArgumentException("Unsupported Map type: " + mapType.getName());
			}
			try {
				return (Map<K, V>) ReflectionUtils.accessibleConstructor(mapType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Could not instantiate Map type: " + mapType.getName(), ex);
			}
		}
	}

	/**
	 * 创建一个 {@link java.util.Properties} 的变体，该变体在 {@link Properties#getProperty} 中自动将非字符串值适配为字符串表示形式。
	 * <p>此外，返回的 {@code Properties} 实例会根据键对属性进行字母数字排序。
	 * @return 一个新的 {@code Properties} 实例
	 * @since 4.3.4
	 * @see #createSortedProperties(boolean)
	 * @see #createSortedProperties(Properties, boolean)
	 */
	@SuppressWarnings("serial")
	public static Properties createStringAdaptingProperties() {
		return new SortedProperties(false) {
			@Override
			@Nullable
			public String getProperty(String key) {
				Object value = get(key);
				return (value != null ? value.toString() : null);
			}
		};
	}

	/**
	 * 创建一个 {@link java.util.Properties} 的变体，该变体根据键对属性进行字母数字排序。
	 * <p>当将 {@code Properties} 实例存储在属性文件中时，这可能很有用，因为它允许以可重复的方式生成具有一致属性排序的属性文件。生成的属性文件中的注释也可以选择省略。
	 * @param omitComments 当将属性存储在文件中时，如果省略注释，则为 {@code true}
	 * @return 一个新的 {@code Properties} 实例
	 * @since 5.2
	 * @see #createStringAdaptingProperties()
	 * @see #createSortedProperties(Properties, boolean)
	 */
	public static Properties createSortedProperties(boolean omitComments) {
		return new SortedProperties(omitComments);
	}

	/**
	 * 创建一个 {@link java.util.Properties} 的变体，该变体根据键对属性进行字母数字排序。
	 * <p>当将 {@code Properties} 实例存储在属性文件中时，这可能很有用，因为它允许以可重复的方式生成具有一致属性排序的属性文件。生成的属性文件中的注释也可以选择省略。
	 * <p>返回的 {@code Properties} 实例将使用从提供的 {@code properties} 对象中的属性填充，但不会复制提供的 {@code properties} 对象中的默认属性。
	 * @param properties 从中复制初始属性的 {@code Properties} 对象
	 * @param omitComments 当将属性存储在文件中时，如果省略注释，则为 {@code true}
	 * @return 一个新的 {@code Properties} 实例
	 * @since 5.2
	 * @see #createStringAdaptingProperties()
	 * @see #createSortedProperties(boolean)
	 */
	public static Properties createSortedProperties(Properties properties, boolean omitComments) {
		return new SortedProperties(properties, omitComments);
	}

	/**
	 * 将给定类型转换为 {@link Enum} 的子类型。
	 * @param enumType 枚举类型，永远不会是 {@code null}
	 * @return 给定类型作为 {@link Enum} 的子类型
	 * @throws IllegalArgumentException 如果给定类型不是 {@link Enum} 的子类型
	 */
	@SuppressWarnings("rawtypes")
	private static Class<? extends Enum> asEnumType(Class<?> enumType) {
		Assert.notNull(enumType, "Enum type must not be null");
		if (!Enum.class.isAssignableFrom(enumType)) {
			throw new IllegalArgumentException("Supplied type is not an enum: " + enumType.getName());
		}
		return enumType.asSubclass(Enum.class);
	}

}
