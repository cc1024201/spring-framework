/*
 * 版权 2002-2023 原作者或作者。
 *
 * 根据Apache许可证2.0版（“许可证”）获得许可；
 * 除非符合许可证，否则不得使用此文件。
 * 您可以在以下位置获得许可证副本：
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则在许可证下分发的软件
 * 将按“原样”基础分发，不附带任何明示或暗示的担保或条件。
 * 有关许可证下允许和限制的特定语言，请参阅许可证。
 */

package org.springframework.util;

import java.io.Closeable;
import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.springframework.lang.Nullable;

/**
 * 杂项 {@code java.lang.Class} 实用方法。
 * 主要用于框架内部使用。
 *
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 1.1
 * @see TypeUtils
 * @see ReflectionUtils
 */
public abstract class ClassUtils {

	/** 数组类名的后缀：{@code "[]"}. */
	public static final String ARRAY_SUFFIX = "[]";

	/** 内部数组类名的前缀：{@code "["}.	 */
	private static final String INTERNAL_ARRAY_PREFIX = "[";

	/** 内部非基元数组类名的前缀：{@code "[L"}. */
	private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

	/** 可重用的空类数组常量。 */
	private static final Class<?>[] EMPTY_CLASS_ARRAY = {};

	/** 包分隔符字符：{@code '.'}. */
	private static final char PACKAGE_SEPARATOR = '.';

	/** 路径分隔符字符：{@code '/'}. */
	private static final char PATH_SEPARATOR = '/';

	/** 嵌套类分隔符字符：{@code '$'}. */
	private static final char NESTED_CLASS_SEPARATOR = '$';

	/** CGLIB类分隔符：{@code "$$"}. */
	public static final String CGLIB_CLASS_SEPARATOR = "$$";

	/** ".class" 文件后缀。 */
	public static final String CLASS_FILE_SUFFIX = ".class";


	/**
	 * 具有原始包装类型为键和相应原始类型为值的映射，
	 * 例如：Integer.class -> int.class.
	 */
	private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(9);

	/**
	 * 具有原始类型为键和相应包装类型为值的映射，
	 * 例如：int.class -> Integer.class.
	 */
	private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(9);
	/**
	 * 具有原始类型名称为键和相应原始类型为值的映射，
	 * 例如："int" -> "int.class".
	 */
	private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);

	/**
	 * 以常见的Java语言类名为键，相应的Class为值的映射。
	 * 主要用于远程调用的高效反序列化。
	 */
	private static final Map<String, Class<?>> commonClassCache = new HashMap<>(64);

	/**
	 * 在搜索“主要”用户级接口时应忽略的常见Java语言接口。
	 */
	private static final Set<Class<?>> javaLanguageInterfaces;

	/**
	 * 在声明类实现的接口上的等效方法的缓存。
	 */
	private static final Map<Method, Method> interfaceMethodCache = new ConcurrentReferenceHashMap<>(256);


	static {
		primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
		primitiveWrapperTypeMap.put(Byte.class, byte.class);
		primitiveWrapperTypeMap.put(Character.class, char.class);
		primitiveWrapperTypeMap.put(Double.class, double.class);
		primitiveWrapperTypeMap.put(Float.class, float.class);
		primitiveWrapperTypeMap.put(Integer.class, int.class);
		primitiveWrapperTypeMap.put(Long.class, long.class);
		primitiveWrapperTypeMap.put(Short.class, short.class);
		primitiveWrapperTypeMap.put(Void.class, void.class);

		// 映射条目迭代比使用lambda的forEach初始化更便宜
		for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
			primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
			registerCommonClasses(entry.getKey());
		}

		Set<Class<?>> primitiveTypes = new HashSet<>(32);
		primitiveTypes.addAll(primitiveWrapperTypeMap.values());
		Collections.addAll(primitiveTypes, boolean[].class, byte[].class, char[].class,
				double[].class, float[].class, int[].class, long[].class, short[].class);
		for (Class<?> primitiveType : primitiveTypes) {
			primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
		}

		registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
				Float[].class, Integer[].class, Long[].class, Short[].class);
		registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
				Class.class, Class[].class, Object.class, Object[].class);
		registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
				Error.class, StackTraceElement.class, StackTraceElement[].class);
		registerCommonClasses(Enum.class, Iterable.class, Iterator.class, Enumeration.class,
				Collection.class, List.class, Set.class, Map.class, Map.Entry.class, Optional.class);

		Class<?>[] javaLanguageInterfaceArray = {Serializable.class, Externalizable.class,
				Closeable.class, AutoCloseable.class, Cloneable.class, Comparable.class};
		registerCommonClasses(javaLanguageInterfaceArray);
		javaLanguageInterfaces = Set.of(javaLanguageInterfaceArray);
	}


	/**
	 * 使用ClassUtils缓存注册给定的常见类。
	 */
	private static void registerCommonClasses(Class<?>... commonClasses) {
		for (Class<?> clazz : commonClasses) {
			commonClassCache.put(clazz.getName(), clazz);
		}
	}

	/**
	 * 返回要使用的默认ClassLoader：通常是线程上下文
	 * ClassLoader（如果可用）；将使用加载ClassUtils类的ClassLoader作为备用。
	 * <p>如果您打算在明确首选非空ClassLoader引用的场景中使用线程上下文ClassLoader，
	 * 例如，用于类路径资源加载（但不一定是
	 * {@code Class.forName}，它接受一个{@code null} ClassLoader
	 * 引用）。
	 * @return 默认的ClassLoader（仅在系统
	 * ClassLoader也无法访问时为{@code null}）
	 * @see Thread#getContextClassLoader()
	 * @see ClassLoader#getSystemClassLoader()
	 */
	@Nullable
	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		}
		catch (Throwable ex) {
			// 无法访问线程上下文ClassLoader - 返回...
		}
		if (cl == null) {
			// 没有线程上下文类加载器 -> 使用此类的类加载器。
			cl = ClassUtils.class.getClassLoader();
			if (cl == null) {
				// getClassLoader()返回null表示引导ClassLoader
				try {
					cl = ClassLoader.getSystemClassLoader();
				}
				catch (Throwable ex) {
					// 无法访问系统ClassLoader - 好吧，也许调用方可以使用null...
				}
			}
		}
		return cl;
	}

	/**
	 * 如果需要，使用环境的bean ClassLoader覆盖线程上下文ClassLoader，
	 * 即，如果bean ClassLoader与线程上下文ClassLoader不相等。
	 * @param classLoaderToUse 实际用于线程上下文的ClassLoader
	 *	 * @return 原始线程上下文ClassLoader，如果没有覆盖，则返回{@code null}
	 */
	@Nullable
	public static ClassLoader overrideThreadContextClassLoader(@Nullable ClassLoader classLoaderToUse) {
		Thread currentThread = Thread.currentThread();
		ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
		if (classLoaderToUse != null && !classLoaderToUse.equals(threadContextClassLoader)) {
			currentThread.setContextClassLoader(classLoaderToUse);
			return threadContextClassLoader;
		}
		else {
			return null;
		}
	}

	/**
	 * 替换{@code Class.forName()}，同时返回原始类实例
	 * （例如 "int"）和数组类名（例如 "String[]"）。
	 * 此外，它还能够解析Java源代码中的嵌套类名
	 * 样式（例如 "java.lang.Thread.State" 而不是 "java.lang.Thread$State"）。
	 * @param name 类的名称
	 * @param classLoader 要使用的类加载器
	 * （可以是{@code null}，表示默认类加载器）
	 * @return 为提供的名称提供一个类实例
	 * @throws ClassNotFoundException 如果找不到类
	 * @throws LinkageError 如果无法加载类文件
	 * @see Class#forName(String, boolean, ClassLoader)
	 */
	public static Class<?> forName(String name, @Nullable ClassLoader classLoader)
			throws ClassNotFoundException, LinkageError {

		Assert.notNull(name, "名称不能为空");

		Class<?> clazz = resolvePrimitiveClassName(name);
		if (clazz == null) {
			clazz = commonClassCache.get(name);
		}
		if (clazz != null) {
			return clazz;
		}

		// "java.lang.String[]" 样式数组
		if (name.endsWith(ARRAY_SUFFIX)) {
			String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
			Class<?> elementClass = forName(elementClassName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[Ljava.lang.String;" 样式数组
		if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
			String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

		// "[[I" 或 "[[Ljava.lang.String;" 样式数组
		if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
			String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
			Class<?> elementClass = forName(elementName, classLoader);
			return Array.newInstance(elementClass, 0).getClass();
		}

	
		ClassLoader clToUse = classLoader;
		if (clToUse == null) {
			clToUse = getDefaultClassLoader();
		}
		try {
			return Class.forName(name, false, clToUse);
		}
		catch (ClassNotFoundException ex) {
			int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
			if (lastDotIndex != -1) {
				String nestedClassName =
						name.substring(0, lastDotIndex) + NESTED_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
				try {
					return Class.forName(nestedClassName, false, clToUse);
				}
				catch (ClassNotFoundException ex2) {
					// 忽略 - 让原始异常通过
				}
			}
			throw ex;
		}
	}

	/**
	 * 将给定的类名解析为一个Class实例。支持原始类型（如 "int"）和数组类名（如 "String[]"）。
	 * <p>实际上，这与具有相同参数的{@code forName}方法等效，唯一的区别是在类加载失败时抛出的异常。
	 * @param className 要解析的类名
	 * @param classLoader 要使用的类加载器
	 * （可以是{@code null}，表示默认类加载器）
	 * @return 为提供的名称提供一个类实例
	 * @throws IllegalArgumentException 如果类名无法解析
	 * （即，找不到类或无法加载类文件）
	 * @throws IllegalStateException 如果相应的类可解析，但类的继承层次结构中存在可读性不匹配
	 * （通常是Jigsaw模块定义中缺少对由此处加载的类实现的超类或接口的依赖声明）
	 * @see #forName(String, ClassLoader)
	 */
	public static Class<?> resolveClassName(String className, @Nullable ClassLoader classLoader)
			throws IllegalArgumentException {

		try {
			return forName(className, classLoader);
		}
		catch (IllegalAccessError err) {
			throw new IllegalStateException("类 [" +
					className + "] 的继承层次结构中的可读性不匹配: " + err.getMessage(), err);
		}
		catch (LinkageError err) {
			throw new IllegalArgumentException("无法解析类定义 [" + className + "]", err);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("找不到类 [" + className + "]", ex);
		}
	}

	/**
	 * 判断由提供的名称标识的{@link Class}是否存在并且可以加载。如果类或其依赖项不存在或无法加载，则返回{@code false}。
	 * @param className 要检查的类名
	 * @param classLoader 要使用的类加载器
	 * （可以是{@code null}，表示默认类加载器）
	 * @return 指定的类是否存在（包括其所有超类和接口）
	 * @throws IllegalStateException 如果相应的类可解析，但类的继承层次结构中存在可读性不匹配
	 * （通常是Jigsaw模块定义中缺少对由此处加载的类实现的超类或接口的依赖声明）
	 */
	public static boolean isPresent(String className, @Nullable ClassLoader classLoader) {
		try {
			forName(className, classLoader);
			return true;
		}
		catch (IllegalAccessError err) {
			throw new IllegalStateException("类 [" +
					className + "] 的继承层次结构中的可读性不匹配: " + err.getMessage(), err);
		}
		catch (Throwable ex) {
			// 通常是 ClassNotFoundException 或 NoClassDefFoundError...
			return false;
		}
	}

	/**
	 * Check whether the given class is visible in the given ClassLoader.
	 * @param clazz the class to check (typically an interface)
	 * @param classLoader the ClassLoader to check against
	 * (may be {@code null} in which case this method will always return {@code true})
	 */
	public static boolean isVisible(Class<?> clazz, @Nullable ClassLoader classLoader) {
		if (classLoader == null) {
			return true;
		}
		try {
			if (clazz.getClassLoader() == classLoader) {
				return true;
			}
		}
		catch (SecurityException ex) {
			// Fall through to loadable check below
		}

		// Visible if same Class can be loaded from given ClassLoader
		return isLoadable(clazz, classLoader);
	}

	/**
	 * Check whether the given class is cache-safe in the given context,
	 * i.e. whether it is loaded by the given ClassLoader or a parent of it.
	 * @param clazz the class to analyze
	 * @param classLoader the ClassLoader to potentially cache metadata in
	 * (may be {@code null} which indicates the system class loader)
	 */
	public static boolean isCacheSafe(Class<?> clazz, @Nullable ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			ClassLoader target = clazz.getClassLoader();
			// Common cases
			if (target == classLoader || target == null) {
				return true;
			}
			if (classLoader == null) {
				return false;
			}
			// Check for match in ancestors -> positive
			ClassLoader current = classLoader;
			while (current != null) {
				current = current.getParent();
				if (current == target) {
					return true;
				}
			}
			// Check for match in children -> negative
			while (target != null) {
				target = target.getParent();
				if (target == classLoader) {
					return false;
				}
			}
		}
		catch (SecurityException ex) {
			// Fall through to loadable check below
		}

		// Fallback for ClassLoaders without parent/child relationship:
		// safe if same Class can be loaded from given ClassLoader
		return (classLoader != null && isLoadable(clazz, classLoader));
	}

	/**
	 * Check whether the given class is loadable in the given ClassLoader.
	 * @param clazz the class to check (typically an interface)
	 * @param classLoader the ClassLoader to check against
	 * @since 5.0.6
	 */
	private static boolean isLoadable(Class<?> clazz, ClassLoader classLoader) {
		try {
			return (clazz == classLoader.loadClass(clazz.getName()));
			// Else: different class with same name found
		}
		catch (ClassNotFoundException ex) {
			// No corresponding class found at all
			return false;
		}
	}

	/**
	 * Resolve the given class name as primitive class, if appropriate,
	 * according to the JVM's naming rules for primitive classes.
	 * <p>Also supports the JVM's internal class names for primitive arrays.
	 * Does <i>not</i> support the "[]" suffix notation for primitive arrays;
	 * this is only supported by {@link #forName(String, ClassLoader)}.
	 * @param name the name of the potentially primitive class
	 * @return the primitive class, or {@code null} if the name does not denote
	 * a primitive class or primitive array class
	 */
	@Nullable
	public static Class<?> resolvePrimitiveClassName(@Nullable String name) {
		Class<?> result = null;
		// Most class names will be quite long, considering that they
		// SHOULD sit in a package, so a length check is worthwhile.
		if (name != null && name.length() <= 7) {
			// Could be a primitive - likely.
			result = primitiveTypeNameMap.get(name);
		}
		return result;
	}

	/**
	 * Check if the given class represents a primitive wrapper,
	 * i.e. Boolean, Byte, Character, Short, Integer, Long, Float, Double, or
	 * Void.
	 * @param clazz the class to check
	 * @return whether the given class is a primitive wrapper class
	 */
	public static boolean isPrimitiveWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return primitiveWrapperTypeMap.containsKey(clazz);
	}

	/**
	 * Check if the given class represents a primitive (i.e. boolean, byte,
	 * char, short, int, long, float, or double), {@code void}, or a wrapper for
	 * those types (i.e. Boolean, Byte, Character, Short, Integer, Long, Float,
	 * Double, or Void).
	 * @param clazz the class to check
	 * @return {@code true} if the given class represents a primitive, void, or
	 * a wrapper class
	 */
	public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
	}

	/**
	 * Check if the given class represents an array of primitives,
	 * i.e. boolean, byte, char, short, int, long, float, or double.
	 * @param clazz the class to check
	 * @return whether the given class is a primitive array class
	 */
	public static boolean isPrimitiveArray(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isArray() && clazz.getComponentType().isPrimitive());
	}

	/**
	 * Check if the given class represents an array of primitive wrappers,
	 * i.e. Boolean, Byte, Character, Short, Integer, Long, Float, or Double.
	 * @param clazz the class to check
	 * @return whether the given class is a primitive wrapper array class
	 */
	public static boolean isPrimitiveWrapperArray(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isArray() && isPrimitiveWrapper(clazz.getComponentType()));
	}

	/**
	 * Resolve the given class if it is a primitive class,
	 * returning the corresponding primitive wrapper type instead.
	 * @param clazz the class to check
	 * @return the original class, or a primitive wrapper for the original primitive type
	 */
	public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return (clazz.isPrimitive() && clazz != void.class ? primitiveTypeToWrapperMap.get(clazz) : clazz);
	}

	/**
	 * Check if the right-hand side type may be assigned to the left-hand side
	 * type, assuming setting by reflection. Considers primitive wrapper
	 * classes as assignable to the corresponding primitive types.
	 * @param lhsType the target type
	 * @param rhsType the value type that should be assigned to the target type
	 * @return if the target type is assignable from the value type
	 * @see TypeUtils#isAssignable(java.lang.reflect.Type, java.lang.reflect.Type)
	 */
	public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
		Assert.notNull(lhsType, "Left-hand side type must not be null");
		Assert.notNull(rhsType, "Right-hand side type must not be null");
		if (lhsType.isAssignableFrom(rhsType)) {
			return true;
		}
		if (lhsType.isPrimitive()) {
			Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
			return (lhsType == resolvedPrimitive);
		}
		else {
			Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
			return (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper));
		}
	}

	/**
	 * Determine if the given type is assignable from the given value,
	 * assuming setting by reflection. Considers primitive wrapper classes
	 * as assignable to the corresponding primitive types.
	 * @param type the target type
	 * @param value the value that should be assigned to the type
	 * @return if the type is assignable from the value
	 */
	public static boolean isAssignableValue(Class<?> type, @Nullable Object value) {
		Assert.notNull(type, "Type must not be null");
		return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
	}

	/**
	 * Convert a "/"-based resource path to a "."-based fully qualified class name.
	 * @param resourcePath the resource path pointing to a class
	 * @return the corresponding fully qualified class name
	 */
	public static String convertResourcePathToClassName(String resourcePath) {
		Assert.notNull(resourcePath, "Resource path must not be null");
		return resourcePath.replace(PATH_SEPARATOR, PACKAGE_SEPARATOR);
	}

	/**
	 * Convert a "."-based fully qualified class name to a "/"-based resource path.
	 * @param className the fully qualified class name
	 * @return the corresponding resource path, pointing to the class
	 */
	public static String convertClassNameToResourcePath(String className) {
		Assert.notNull(className, "Class name must not be null");
		return className.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	/**
	 * Return a path suitable for use with {@code ClassLoader.getResource}
	 * (also suitable for use with {@code Class.getResource} by prepending a
	 * slash ('/') to the return value). Built by taking the package of the specified
	 * class file, converting all dots ('.') to slashes ('/'), adding a trailing slash
	 * if necessary, and concatenating the specified resource name to this.
	 * <br/>As such, this function may be used to build a path suitable for
	 * loading a resource file that is in the same package as a class file,
	 * although {@link org.springframework.core.io.ClassPathResource} is usually
	 * even more convenient.
	 * @param clazz the Class whose package will be used as the base
	 * @param resourceName the resource name to append. A leading slash is optional.
	 * @return the built-up resource path
	 * @see ClassLoader#getResource
	 * @see Class#getResource
	 */
	public static String addResourcePathToPackagePath(Class<?> clazz, String resourceName) {
		Assert.notNull(resourceName, "Resource name must not be null");
		if (!resourceName.startsWith("/")) {
			return classPackageAsResourcePath(clazz) + '/' + resourceName;
		}
		return classPackageAsResourcePath(clazz) + resourceName;
	}

	/**
	 * Given an input class object, return a string which consists of the
	 * class's package name as a pathname, i.e., all dots ('.') are replaced by
	 * slashes ('/'). Neither a leading nor trailing slash is added. The result
	 * could be concatenated with a slash and the name of a resource and fed
	 * directly to {@code ClassLoader.getResource()}. For it to be fed to
	 * {@code Class.getResource} instead, a leading slash would also have
	 * to be prepended to the returned value.
	 * @param clazz the input class. A {@code null} value or the default
	 * (empty) package will result in an empty string ("") being returned.
	 * @return a path which represents the package name
	 * @see ClassLoader#getResource
	 * @see Class#getResource
	 */
	public static String classPackageAsResourcePath(@Nullable Class<?> clazz) {
		if (clazz == null) {
			return "";
		}
		String className = clazz.getName();
		int packageEndIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		if (packageEndIndex == -1) {
			return "";
		}
		String packageName = className.substring(0, packageEndIndex);
		return packageName.replace(PACKAGE_SEPARATOR, PATH_SEPARATOR);
	}

	/**
	 * Build a String that consists of the names of the classes/interfaces
	 * in the given array.
	 * <p>Basically like {@code AbstractCollection.toString()}, but stripping
	 * the "class "/"interface " prefix before every class name.
	 * @param classes an array of Class objects
	 * @return a String of form "[com.foo.Bar, com.foo.Baz]"
	 * @see java.util.AbstractCollection#toString()
	 */
	public static String classNamesToString(Class<?>... classes) {
		return classNamesToString(Arrays.asList(classes));
	}

	/**
	 * Build a String that consists of the names of the classes/interfaces
	 * in the given collection.
	 * <p>Basically like {@code AbstractCollection.toString()}, but stripping
	 * the "class "/"interface " prefix before every class name.
	 * @param classes a Collection of Class objects (may be {@code null})
	 * @return a String of form "[com.foo.Bar, com.foo.Baz]"
	 * @see java.util.AbstractCollection#toString()
	 */
	public static String classNamesToString(@Nullable Collection<Class<?>> classes) {
		if (CollectionUtils.isEmpty(classes)) {
			return "[]";
		}
		StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");
		for (Class<?> clazz : classes) {
			stringJoiner.add(clazz.getName());
		}
		return stringJoiner.toString();
	}

	/**
	 * Copy the given {@code Collection} into a {@code Class} array.
	 * <p>The {@code Collection} must contain {@code Class} elements only.
	 * @param collection the {@code Collection} to copy
	 * @return the {@code Class} array
	 * @since 3.1
	 * @see StringUtils#toStringArray
	 */
	public static Class<?>[] toClassArray(@Nullable Collection<Class<?>> collection) {
		return (!CollectionUtils.isEmpty(collection) ? collection.toArray(EMPTY_CLASS_ARRAY) : EMPTY_CLASS_ARRAY);
	}

	/**
	 * Return all interfaces that the given instance implements as an array,
	 * including ones implemented by superclasses.
	 * @param instance the instance to analyze for interfaces
	 * @return all interfaces that the given instance implements as an array
	 */
	public static Class<?>[] getAllInterfaces(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getAllInterfacesForClass(instance.getClass());
	}

	/**
	 * Return all interfaces that the given class implements as an array,
	 * including ones implemented by superclasses.
	 * <p>If the class itself is an interface, it gets returned as sole interface.
	 * @param clazz the class to analyze for interfaces
	 * @return all interfaces that the given object implements as an array
	 */
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
		return getAllInterfacesForClass(clazz, null);
	}

	/**
	 * Return all interfaces that the given class implements as an array,
	 * including ones implemented by superclasses.
	 * <p>If the class itself is an interface, it gets returned as sole interface.
	 * @param clazz the class to analyze for interfaces
	 * @param classLoader the ClassLoader that the interfaces need to be visible in
	 * (may be {@code null} when accepting all declared interfaces)
	 * @return all interfaces that the given object implements as an array
	 */
	public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, @Nullable ClassLoader classLoader) {
		return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
	}

	/**
	 * Return all interfaces that the given instance implements as a Set,
	 * including ones implemented by superclasses.
	 * @param instance the instance to analyze for interfaces
	 * @return all interfaces that the given instance implements as a Set
	 */
	public static Set<Class<?>> getAllInterfacesAsSet(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getAllInterfacesForClassAsSet(instance.getClass());
	}

	/**
	 * Return all interfaces that the given class implements as a Set,
	 * including ones implemented by superclasses.
	 * <p>If the class itself is an interface, it gets returned as sole interface.
	 * @param clazz the class to analyze for interfaces
	 * @return all interfaces that the given object implements as a Set
	 */
	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz) {
		return getAllInterfacesForClassAsSet(clazz, null);
	}

	/**
	 * Return all interfaces that the given class implements as a Set,
	 * including ones implemented by superclasses.
	 * <p>If the class itself is an interface, it gets returned as sole interface.
	 * @param clazz the class to analyze for interfaces
	 * @param classLoader the ClassLoader that the interfaces need to be visible in
	 * (may be {@code null} when accepting all declared interfaces)
	 * @return all interfaces that the given object implements as a Set
	 */
	public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, @Nullable ClassLoader classLoader) {
		Assert.notNull(clazz, "Class must not be null");
		if (clazz.isInterface() && isVisible(clazz, classLoader)) {
			return Collections.singleton(clazz);
		}
		Set<Class<?>> interfaces = new LinkedHashSet<>();
		Class<?> current = clazz;
		while (current != null) {
			Class<?>[] ifcs = current.getInterfaces();
			for (Class<?> ifc : ifcs) {
				if (isVisible(ifc, classLoader)) {
					interfaces.add(ifc);
				}
			}
			current = current.getSuperclass();
		}
		return interfaces;
	}

	/**
	 * Create a composite interface Class for the given interfaces,
	 * implementing the given interfaces in one single Class.
	 * <p>This implementation builds a JDK proxy class for the given interfaces.
	 * @param interfaces the interfaces to merge
	 * @param classLoader the ClassLoader to create the composite Class in
	 * @return the merged interface as Class
	 * @throws IllegalArgumentException if the specified interfaces expose
	 * conflicting method signatures (or a similar constraint is violated)
	 * @see java.lang.reflect.Proxy#getProxyClass
	 */
	@SuppressWarnings("deprecation")
	public static Class<?> createCompositeInterface(Class<?>[] interfaces, @Nullable ClassLoader classLoader) {
		Assert.notEmpty(interfaces, "Interface array must not be empty");
		return Proxy.getProxyClass(classLoader, interfaces);
	}

	/**
	 * Determine the common ancestor of the given classes, if any.
	 * @param clazz1 the class to introspect
	 * @param clazz2 the other class to introspect
	 * @return the common ancestor (i.e. common superclass, one interface
	 * extending the other), or {@code null} if none found. If any of the
	 * given classes is {@code null}, the other class will be returned.
	 * @since 3.2.6
	 */
	@Nullable
	public static Class<?> determineCommonAncestor(@Nullable Class<?> clazz1, @Nullable Class<?> clazz2) {
		if (clazz1 == null) {
			return clazz2;
		}
		if (clazz2 == null) {
			return clazz1;
		}
		if (clazz1.isAssignableFrom(clazz2)) {
			return clazz1;
		}
		if (clazz2.isAssignableFrom(clazz1)) {
			return clazz2;
		}
		Class<?> ancestor = clazz1;
		do {
			ancestor = ancestor.getSuperclass();
			if (ancestor == null || Object.class == ancestor) {
				return null;
			}
		}
		while (!ancestor.isAssignableFrom(clazz2));
		return ancestor;
	}

	/**
	 * Determine whether the given interface is a common Java language interface:
	 * {@link Serializable}, {@link Externalizable}, {@link Closeable}, {@link AutoCloseable},
	 * {@link Cloneable}, {@link Comparable} - all of which can be ignored when looking
	 * for 'primary' user-level interfaces. Common characteristics: no service-level
	 * operations, no bean property methods, no default methods.
	 * @param ifc the interface to check
	 * @since 5.0.3
	 */
	public static boolean isJavaLanguageInterface(Class<?> ifc) {
		return javaLanguageInterfaces.contains(ifc);
	}

	/**
	 * Determine if the supplied class is a static class.
	 * @return {@code true} if the supplied class is a static class
	 * @since 6.0
	 * @see Modifier#isStatic(int)
	 * @see #isInnerClass(Class)
	 */
	public static boolean isStaticClass(Class<?> clazz) {
		return Modifier.isStatic(clazz.getModifiers());
	}

	/**
	 * Determine if the supplied class is an <em>inner class</em>,
	 * i.e. a non-static member of an enclosing class.
	 * @return {@code true} if the supplied class is an inner class
	 * @since 5.0.5
	 * @see Class#isMemberClass()
	 * @see #isStaticClass(Class)
	 */
	public static boolean isInnerClass(Class<?> clazz) {
		return (clazz.isMemberClass() && !isStaticClass(clazz));
	}

	/**
	 * Determine if the supplied {@link Class} is a JVM-generated implementation
	 * class for a lambda expression or method reference.
	 * <p>This method makes a best-effort attempt at determining this, based on
	 * checks that work on modern, mainstream JVMs.
	 * @param clazz the class to check
	 * @return {@code true} if the class is a lambda implementation class
	 * @since 5.3.19
	 */
	public static boolean isLambdaClass(Class<?> clazz) {
		return (clazz.isSynthetic() && (clazz.getSuperclass() == Object.class) &&
				(clazz.getInterfaces().length > 0) && clazz.getName().contains("$$Lambda"));
	}

	/**
	 * Check whether the given object is a CGLIB proxy.
	 * @param object the object to check
	 * @see org.springframework.aop.support.AopUtils#isCglibProxy(Object)
	 * @deprecated as of 5.2, in favor of custom (possibly narrower) checks
	 * such as for a Spring AOP proxy
	 */
	@Deprecated
	public static boolean isCglibProxy(Object object) {
		return isCglibProxyClass(object.getClass());
	}

	/**
	 * Check whether the specified class is a CGLIB-generated class.
	 * @param clazz the class to check
	 * @see #getUserClass(Class)
	 * @deprecated as of 5.2, in favor of custom (possibly narrower) checks
	 * or simply a check for containing {@link #CGLIB_CLASS_SEPARATOR}
	 */
	@Deprecated
	public static boolean isCglibProxyClass(@Nullable Class<?> clazz) {
		return (clazz != null && isCglibProxyClassName(clazz.getName()));
	}

	/**
	 * Check whether the specified class name is a CGLIB-generated class.
	 * @param className the class name to check
	 * @see #CGLIB_CLASS_SEPARATOR
	 * @deprecated as of 5.2, in favor of custom (possibly narrower) checks
	 * or simply a check for containing {@link #CGLIB_CLASS_SEPARATOR}
	 */
	@Deprecated
	public static boolean isCglibProxyClassName(@Nullable String className) {
		return (className != null && className.contains(CGLIB_CLASS_SEPARATOR));
	}

	/**
	 * Return the user-defined class for the given instance: usually simply
	 * the class of the given instance, but the original class in case of a
	 * CGLIB-generated subclass.
	 * @param instance the instance to check
	 * @return the user-defined class
	 */
	public static Class<?> getUserClass(Object instance) {
		Assert.notNull(instance, "Instance must not be null");
		return getUserClass(instance.getClass());
	}

	/**
	 * Return the user-defined class for the given class: usually simply the given
	 * class, but the original class in case of a CGLIB-generated subclass.
	 * @param clazz the class to check
	 * @return the user-defined class
	 * @see #CGLIB_CLASS_SEPARATOR
	 */
	public static Class<?> getUserClass(Class<?> clazz) {
		if (clazz.getName().contains(CGLIB_CLASS_SEPARATOR)) {
			Class<?> superclass = clazz.getSuperclass();
			if (superclass != null && superclass != Object.class) {
				return superclass;
			}
		}
		return clazz;
	}

	/**
	 * Return a descriptive name for the given object's type: usually simply
	 * the class name, but component type class name + "[]" for arrays,
	 * and an appended list of implemented interfaces for JDK proxies.
	 * @param value the value to introspect
	 * @return the qualified name of the class
	 */
	@Nullable
	public static String getDescriptiveType(@Nullable Object value) {
		if (value == null) {
			return null;
		}
		Class<?> clazz = value.getClass();
		if (Proxy.isProxyClass(clazz)) {
			String prefix = clazz.getName() + " implementing ";
			StringJoiner result = new StringJoiner(",", prefix, "");
			for (Class<?> ifc : clazz.getInterfaces()) {
				result.add(ifc.getName());
			}
			return result.toString();
		}
		else {
			return clazz.getTypeName();
		}
	}

	/**
	 * Check whether the given class matches the user-specified type name.
	 * @param clazz the class to check
	 * @param typeName the type name to match
	 */
	public static boolean matchesTypeName(Class<?> clazz, @Nullable String typeName) {
		return (typeName != null &&
				(typeName.equals(clazz.getTypeName()) || typeName.equals(clazz.getSimpleName())));
	}

	/**
	 * Get the class name without the qualified package name.
	 * @param className the className to get the short name for
	 * @return the class name of the class without the package name
	 * @throws IllegalArgumentException if the className is empty
	 */
	public static String getShortName(String className) {
		Assert.hasLength(className, "Class name must not be empty");
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR);
		if (nameEndIndex == -1) {
			nameEndIndex = className.length();
		}
		String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
		shortName = shortName.replace(NESTED_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
		return shortName;
	}

	/**
	 * Get the class name without the qualified package name.
	 * @param clazz the class to get the short name for
	 * @return the class name of the class without the package name
	 */
	public static String getShortName(Class<?> clazz) {
		return getShortName(getQualifiedName(clazz));
	}

	/**
	 * Return the short string name of a Java class in uncapitalized JavaBeans
	 * property format. Strips the outer class name in case of a nested class.
	 * @param clazz the class
	 * @return the short name rendered in a standard JavaBeans property format
	 * @see StringUtils#uncapitalizeAsProperty(String)
	 */
	public static String getShortNameAsProperty(Class<?> clazz) {
		String shortName = getShortName(clazz);
		int dotIndex = shortName.lastIndexOf(PACKAGE_SEPARATOR);
		shortName = (dotIndex != -1 ? shortName.substring(dotIndex + 1) : shortName);
		return StringUtils.uncapitalizeAsProperty(shortName);
	}

	/**
	 * Determine the name of the class file, relative to the containing
	 * package: e.g. "String.class"
	 * @param clazz the class
	 * @return the file name of the ".class" file
	 */
	public static String getClassFileName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		String className = clazz.getName();
		int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
		return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
	}

	/**
	 * Determine the name of the package of the given class,
	 * e.g. "java.lang" for the {@code java.lang.String} class.
	 * @param clazz the class
	 * @return the package name, or the empty String if the class
	 * is defined in the default package
	 */
	public static String getPackageName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return getPackageName(clazz.getName());
	}

	/**
	 * Determine the name of the package of the given fully-qualified class name,
	 * e.g. "java.lang" for the {@code java.lang.String} class name.
	 * @param fqClassName the fully-qualified class name
	 * @return the package name, or the empty String if the class
	 * is defined in the default package
	 */
	public static String getPackageName(String fqClassName) {
		Assert.notNull(fqClassName, "Class name must not be null");
		int lastDotIndex = fqClassName.lastIndexOf(PACKAGE_SEPARATOR);
		return (lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : "");
	}

	/**
	 * Return the qualified name of the given class: usually simply
	 * the class name, but component type class name + "[]" for arrays.
	 * @param clazz the class
	 * @return the qualified name of the class
	 */
	public static String getQualifiedName(Class<?> clazz) {
		Assert.notNull(clazz, "Class must not be null");
		return clazz.getTypeName();
	}

	/**
	 * Return the qualified name of the given method, consisting of
	 * fully qualified interface/class name + "." + method name.
	 * @param method the method
	 * @return the qualified name of the method
	 */
	public static String getQualifiedMethodName(Method method) {
		return getQualifiedMethodName(method, null);
	}

	/**
	 * Return the qualified name of the given method, consisting of
	 * fully qualified interface/class name + "." + method name.
	 * @param method the method
	 * @param clazz the clazz that the method is being invoked on
	 * (may be {@code null} to indicate the method's declaring class)
	 * @return the qualified name of the method
	 * @since 4.3.4
	 */
	public static String getQualifiedMethodName(Method method, @Nullable Class<?> clazz) {
		Assert.notNull(method, "Method must not be null");
		return (clazz != null ? clazz : method.getDeclaringClass()).getName() + '.' + method.getName();
	}

	/**
	 * Determine whether the given class has a public constructor with the given signature.
	 * <p>Essentially translates {@code NoSuchMethodException} to "false".
	 * @param clazz the clazz to analyze
	 * @param paramTypes the parameter types of the method
	 * @return whether the class has a corresponding constructor
	 * @see Class#getConstructor
	 */
	public static boolean hasConstructor(Class<?> clazz, Class<?>... paramTypes) {
		return (getConstructorIfAvailable(clazz, paramTypes) != null);
	}

	/**
	 * Determine whether the given class has a public constructor with the given signature,
	 * and return it if available (else return {@code null}).
	 * <p>Essentially translates {@code NoSuchMethodException} to {@code null}.
	 * @param clazz the clazz to analyze
	 * @param paramTypes the parameter types of the method
	 * @return the constructor, or {@code null} if not found
	 * @see Class#getConstructor
	 */
	@Nullable
	public static <T> Constructor<T> getConstructorIfAvailable(Class<T> clazz, Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		try {
			return clazz.getConstructor(paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	/**
	 * Determine whether the given class has a public method with the given signature.
	 * @param clazz the clazz to analyze
	 * @param method the method to look for
	 * @return whether the class has a corresponding method
	 * @since 5.2.3
	 */
	public static boolean hasMethod(Class<?> clazz, Method method) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(method, "Method must not be null");
		if (clazz == method.getDeclaringClass()) {
			return true;
		}
		String methodName = method.getName();
		Class<?>[] paramTypes = method.getParameterTypes();
		return getMethodOrNull(clazz, methodName, paramTypes) != null;
	}

	/**
	 * Determine whether the given class has a public method with the given signature.
	 * <p>Essentially translates {@code NoSuchMethodException} to "false".
	 * @param clazz the clazz to analyze
	 * @param methodName the name of the method
	 * @param paramTypes the parameter types of the method
	 * @return whether the class has a corresponding method
	 * @see Class#getMethod
	 */
	public static boolean hasMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
		return (getMethodIfAvailable(clazz, methodName, paramTypes) != null);
	}

	/**
	 * Determine whether the given class has a public method with the given signature,
	 * and return it if available (else throws an {@code IllegalStateException}).
	 * <p>In case of any signature specified, only returns the method if there is a
	 * unique candidate, i.e. a single public method with the specified name.
	 * <p>Essentially translates {@code NoSuchMethodException} to {@code IllegalStateException}.
	 * @param clazz the clazz to analyze
	 * @param methodName the name of the method
	 * @param paramTypes the parameter types of the method
	 * (may be {@code null} to indicate any signature)
	 * @return the method (never {@code null})
	 * @throws IllegalStateException if the method has not been found
	 * @see Class#getMethod
	 */
	public static Method getMethod(Class<?> clazz, String methodName, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		if (paramTypes != null) {
			try {
				return clazz.getMethod(methodName, paramTypes);
			}
			catch (NoSuchMethodException ex) {
				throw new IllegalStateException("Expected method not found: " + ex);
			}
		}
		else {
			Set<Method> candidates = findMethodCandidatesByName(clazz, methodName);
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			else if (candidates.isEmpty()) {
				throw new IllegalStateException("Expected method not found: " + clazz.getName() + '.' + methodName);
			}
			else {
				throw new IllegalStateException("No unique method found: " + clazz.getName() + '.' + methodName);
			}
		}
	}

	/**
	 * Determine whether the given class has a public method with the given signature,
	 * and return it if available (else return {@code null}).
	 * <p>In case of any signature specified, only returns the method if there is a
	 * unique candidate, i.e. a single public method with the specified name.
	 * <p>Essentially translates {@code NoSuchMethodException} to {@code null}.
	 * @param clazz the clazz to analyze
	 * @param methodName the name of the method
	 * @param paramTypes the parameter types of the method
	 * (may be {@code null} to indicate any signature)
	 * @return the method, or {@code null} if not found
	 * @see Class#getMethod
	 */
	@Nullable
	public static Method getMethodIfAvailable(Class<?> clazz, String methodName, @Nullable Class<?>... paramTypes) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		if (paramTypes != null) {
			return getMethodOrNull(clazz, methodName, paramTypes);
		}
		else {
			Set<Method> candidates = findMethodCandidatesByName(clazz, methodName);
			if (candidates.size() == 1) {
				return candidates.iterator().next();
			}
			return null;
		}
	}

	/**
	 * Return the number of methods with a given name (with any argument types),
	 * for the given class and/or its superclasses. Includes non-public methods.
	 * @param clazz	the clazz to check
	 * @param methodName the name of the method
	 * @return the number of methods with the given name
	 */
	public static int getMethodCountForName(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		int count = 0;
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (methodName.equals(method.getName())) {
				count++;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			count += getMethodCountForName(ifc, methodName);
		}
		if (clazz.getSuperclass() != null) {
			count += getMethodCountForName(clazz.getSuperclass(), methodName);
		}
		return count;
	}

	/**
	 * Does the given class or one of its superclasses at least have one or more
	 * methods with the supplied name (with any argument types)?
	 * Includes non-public methods.
	 * @param clazz	the clazz to check
	 * @param methodName the name of the method
	 * @return whether there is at least one method with the given name
	 */
	public static boolean hasAtLeastOneMethodWithName(Class<?> clazz, String methodName) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		Method[] declaredMethods = clazz.getDeclaredMethods();
		for (Method method : declaredMethods) {
			if (method.getName().equals(methodName)) {
				return true;
			}
		}
		Class<?>[] ifcs = clazz.getInterfaces();
		for (Class<?> ifc : ifcs) {
			if (hasAtLeastOneMethodWithName(ifc, methodName)) {
				return true;
			}
		}
		return (clazz.getSuperclass() != null && hasAtLeastOneMethodWithName(clazz.getSuperclass(), methodName));
	}

	/**
	 * Given a method, which may come from an interface, and a target class used
	 * in the current reflective invocation, find the corresponding target method
	 * if there is one &mdash; for example, the method may be {@code IFoo.bar()},
	 * and the target class may be {@code DefaultFoo}. In this case, the method may be
	 * {@code DefaultFoo.bar()}. This enables attributes on that method to be found.
	 * <p><b>NOTE:</b> In contrast to {@link org.springframework.aop.support.AopUtils#getMostSpecificMethod},
	 * this method does <i>not</i> resolve bridge methods automatically.
	 * Call {@link org.springframework.core.BridgeMethodResolver#findBridgedMethod}
	 * if bridge method resolution is desirable &mdash; for example, to obtain
	 * metadata from the original method definition.
	 * <p><b>NOTE:</b> If Java security settings disallow reflective access &mdash;
	 * for example, calls to {@code Class#getDeclaredMethods}, etc. &mdash; this
	 * implementation will fall back to returning the originally provided method.
	 * @param method the method to be invoked, which may come from an interface
	 * @param targetClass the target class for the current invocation
	 * (may be {@code null} or may not even implement the method)
	 * @return the specific target method, or the original method if the
	 * {@code targetClass} does not implement it
	 * @see #getInterfaceMethodIfPossible(Method, Class)
	 */
	public static Method getMostSpecificMethod(Method method, @Nullable Class<?> targetClass) {
		if (targetClass != null && targetClass != method.getDeclaringClass() && isOverridable(method, targetClass)) {
			try {
				if (Modifier.isPublic(method.getModifiers())) {
					try {
						return targetClass.getMethod(method.getName(), method.getParameterTypes());
					}
					catch (NoSuchMethodException ex) {
						return method;
					}
				}
				else {
					Method specificMethod =
							ReflectionUtils.findMethod(targetClass, method.getName(), method.getParameterTypes());
					return (specificMethod != null ? specificMethod : method);
				}
			}
			catch (SecurityException ex) {
				// Security settings are disallowing reflective access; fall back to 'method' below.
			}
		}
		return method;
	}

	/**
	 * Determine a corresponding interface method for the given method handle, if possible.
	 * <p>This is particularly useful for arriving at a public exported type on Jigsaw
	 * which can be reflectively invoked without an illegal access warning.
	 * @param method the method to be invoked, potentially from an implementation class
	 * @return the corresponding interface method, or the original method if none found
	 * @since 5.1
	 * @deprecated in favor of {@link #getInterfaceMethodIfPossible(Method, Class)}
	 */
	@Deprecated
	public static Method getInterfaceMethodIfPossible(Method method) {
		return getInterfaceMethodIfPossible(method, null);
	}

	/**
	 * Determine a corresponding interface method for the given method handle, if possible.
	 * <p>This is particularly useful for arriving at a public exported type on Jigsaw
	 * which can be reflectively invoked without an illegal access warning.
	 * @param method the method to be invoked, potentially from an implementation class
	 * @param targetClass the target class to check for declared interfaces
	 * @return the corresponding interface method, or the original method if none found
	 * @since 5.3.16
	 * @see #getMostSpecificMethod
	 */
	public static Method getInterfaceMethodIfPossible(Method method, @Nullable Class<?> targetClass) {
		if (!Modifier.isPublic(method.getModifiers()) || method.getDeclaringClass().isInterface()) {
			return method;
		}
		// Try cached version of method in its declaring class
		Method result = interfaceMethodCache.computeIfAbsent(method,
				key -> findInterfaceMethodIfPossible(key, key.getDeclaringClass(), Object.class));
		if (result == method && targetClass != null) {
			// No interface method found yet -> try given target class (possibly a subclass of the
			// declaring class, late-binding a base class method to a subclass-declared interface:
			// see e.g. HashMap.HashIterator.hasNext)
			result = findInterfaceMethodIfPossible(method, targetClass, method.getDeclaringClass());
		}
		return result;
	}

	private static Method findInterfaceMethodIfPossible(Method method, Class<?> startClass, Class<?> endClass) {
		Class<?> current = startClass;
		while (current != null && current != endClass) {
			Class<?>[] ifcs = current.getInterfaces();
			for (Class<?> ifc : ifcs) {
				try {
					return ifc.getMethod(method.getName(), method.getParameterTypes());
				}
				catch (NoSuchMethodException ex) {
					// ignore
				}
			}
			current = current.getSuperclass();
		}
		return method;
	}

	/**
	 * Determine whether the given method is declared by the user or at least pointing to
	 * a user-declared method.
	 * <p>Checks {@link Method#isSynthetic()} (for implementation methods) as well as the
	 * {@code GroovyObject} interface (for interface methods; on an implementation class,
	 * implementations of the {@code GroovyObject} methods will be marked as synthetic anyway).
	 * Note that, despite being synthetic, bridge methods ({@link Method#isBridge()}) are considered
	 * as user-level methods since they are eventually pointing to a user-declared generic method.
	 * @param method the method to check
	 * @return {@code true} if the method can be considered as user-declared; {@code false} otherwise
	 */
	public static boolean isUserLevelMethod(Method method) {
		Assert.notNull(method, "Method must not be null");
		return (method.isBridge() || (!method.isSynthetic() && !isGroovyObjectMethod(method)));
	}

	private static boolean isGroovyObjectMethod(Method method) {
		return method.getDeclaringClass().getName().equals("groovy.lang.GroovyObject");
	}

	/**
	 * Determine whether the given method is overridable in the given target class.
	 * @param method the method to check
	 * @param targetClass the target class to check against
	 */
	private static boolean isOverridable(Method method, @Nullable Class<?> targetClass) {
		if (Modifier.isPrivate(method.getModifiers())) {
			return false;
		}
		if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
			return true;
		}
		return (targetClass == null ||
				getPackageName(method.getDeclaringClass()).equals(getPackageName(targetClass)));
	}

	/**
	 * Return a public static method of a class.
	 * @param clazz the class which defines the method
	 * @param methodName the static method name
	 * @param args the parameter types to the method
	 * @return the static method, or {@code null} if no static method was found
	 * @throws IllegalArgumentException if the method name is blank or the clazz is null
	 */
	@Nullable
	public static Method getStaticMethod(Class<?> clazz, String methodName, Class<?>... args) {
		Assert.notNull(clazz, "Class must not be null");
		Assert.notNull(methodName, "Method name must not be null");
		try {
			Method method = clazz.getMethod(methodName, args);
			return Modifier.isStatic(method.getModifiers()) ? method : null;
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}


	@Nullable
	private static Method getMethodOrNull(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
		try {
			return clazz.getMethod(methodName, paramTypes);
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	private static Set<Method> findMethodCandidatesByName(Class<?> clazz, String methodName) {
		Set<Method> candidates = new HashSet<>(1);
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			if (methodName.equals(method.getName())) {
				candidates.add(method);
			}
		}
		return candidates;
	}

}
