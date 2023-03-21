/*
 * 版权 2002-2022 原作者或作者。
 *
 * 根据 Apache 许可证 2.0 版（“许可证”）获得许可；
 * 除非遵守许可证，否则不得使用此文件。
 * 您可以在以下位置获取许可证副本：
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则在许可证下分发的软件
 * 将按“原样”基础分发，不附带任何明示或暗示的担保或条件。
 * 有关许可证下允许和限制的特定语言，请参阅许可证。
 */

package org.springframework.core;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * 辅助类，用于将合成的 {@link Method#isBridge bridge 方法} 解析为
 * 被桥接的 {@link Method}。
 *
 * <p>给定一个合成的 {@link Method#isBridge bridge 方法}，返回 {@link Method}
 * 被桥接。当扩展具有参数化参数的方法的参数化类型时，编译器可能会创建桥接方法。
 * 在运行时调用期间，可能会调用和/或使用桥接 {@link Method} 反射。
 * 当尝试在 {@link Method 方法} 上查找注释时，最好检查
 * 桥接 {@link Method 方法} 是否适当，并找到被桥接的 {@link Method}。
 *
 * <p>有关桥接方法的使用详细信息，请参阅
 * <a href="https://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.12.4.5">
 * Java 语言规范</a>。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 2.0
 */
public final class BridgeMethodResolver {

	private static final Map<Method, Method> cache = new ConcurrentReferenceHashMap<>();

	private BridgeMethodResolver() {
	}


	/**
	 * 查找提供的 {@link Method bridge 方法} 的原始方法。
	 * <p>在传入非桥接 {@link Method} 实例的情况下调用此方法是安全的。
	 * 在这种情况下，直接将提供的 {@link Method} 实例返回给调用者。
	 * 调用者<strong>不</strong>需要在调用此方法之前检查桥接。
	 * @param bridgeMethod 要自省的方法
	 * @return 原始方法（无论是桥接方法还是传入的方法
	 * 如果找不到更具体的方法）
	 */
	public static Method findBridgedMethod(Method bridgeMethod) {
		if (!bridgeMethod.isBridge()) {
			return bridgeMethod;
		}
		Method bridgedMethod = cache.get(bridgeMethod);
		if (bridgedMethod == null) {
			// 收集具有匹配名称和参数大小的所有方法。
			List<Method> candidateMethods = new ArrayList<>();
			MethodFilter filter = candidateMethod ->
					isBridgedCandidateFor(candidateMethod, bridgeMethod);
			ReflectionUtils.doWithMethods(bridgeMethod.getDeclaringClass(), candidateMethods::add, filter);
			if (!candidateMethods.isEmpty()) {
				bridgedMethod = candidateMethods.size() == 1 ?
						candidateMethods.get(0) :
						searchCandidates(candidateMethods, bridgeMethod);
			}
			if (bridgedMethod == null) {
				// 传入了桥接方法，但我们找不到桥接方法。
				// 让我们继续使用传入的方法，希望能获得最好的结果...
				bridgedMethod = bridgeMethod;
			}
			cache.put(bridgeMethod, bridgedMethod);
		}
		return bridgedMethod;
	}

	/**
	 * 如果提供的 '{@code candidateMethod}' 可以被认为是
	 * 由提供的 {@link Method bridge 方法} 桥接的 {@link Method} 的有效候选项，则返回 {@code true}。
	 * 此方法执行低成本检查，可用于快速筛选一组可能的匹配项。
	 */
	private static boolean isBridgedCandidateFor(Method candidateMethod, Method bridgeMethod) {
		return (!candidateMethod.isBridge() &&
				candidateMethod.getName().equals(bridgeMethod.getName()) &&
				candidateMethod.getParameterCount() == bridgeMethod.getParameterCount());
	}

	/**
	 * 在给定的候选项中搜索桥接方法。
	 * @param candidateMethods 候选方法列表
	 * @param bridgeMethod 桥接方法
	 * @return 桥接方法，如果找不到则返回 {@code null}
	 */
	@Nullable
	private static Method searchCandidates(List<Method> candidateMethods, Method bridgeMethod) {
		if (candidateMethods.isEmpty()) {
			return null;
		}
		Method previousMethod = null;
		boolean sameSig = true;
		for (Method candidateMethod : candidateMethods) {
			if (isBridgeMethodFor(bridgeMethod, candidateMethod, bridgeMethod.getDeclaringClass())) {
				return candidateMethod;
			}
			else if (previousMethod != null) {
				sameSig = sameSig &&
						Arrays.equals(candidateMethod.getGenericParameterTypes(), previousMethod.getGenericParameterTypes());
			}
			previousMethod = candidateMethod;
		}
		return (sameSig ? candidateMethods.get(0) : null);
	}

	/**
	 * 确定桥接 {@link Method} 是否是
	 * 提供的候选 {@link Method} 的桥接。
	 */
	static boolean isBridgeMethodFor(Method bridgeMethod, Method candidateMethod, Class<?> declaringClass) {
		if (isResolvedTypeMatch(candidateMethod, bridgeMethod, declaringClass)) {
			return true;
		}
		Method method = findGenericDeclaration(bridgeMethod);
		return (method != null && isResolvedTypeMatch(method, candidateMethod, declaringClass));
	}

	/**
	 * 如果提供的
	 * {@link Method#getGenericParameterTypes() 泛型方法} 和具体 {@link Method} 的 {@link Type} 签名
	 * 在针对 declaringType 解析所有类型后相等，则返回 {@code true}，
	 * 否则返回 {@code false}。
	 */
	private static boolean isResolvedTypeMatch(Method genericMethod, Method candidateMethod, Class<?> declaringClass) {
		Type[] genericParameters = genericMethod.getGenericParameterTypes();
		if (genericParameters.length != candidateMethod.getParameterCount()) {
			return false;
		}
		Class<?>[] candidateParameters = candidateMethod.getParameterTypes();
		for (int i = 0; i < candidateParameters.length; i++) {
			ResolvableType genericParameter = ResolvableType.forMethodParameter(genericMethod, i, declaringClass);
			Class<?> candidateParameter = candidateParameters[i];
			if (candidateParameter.isArray()) {
				// 数组类型：比较组件类型。
				if (!candidateParameter.getComponentType().equals(genericParameter.getComponentType().toClass())) {
					return false;
				}
			}
			// 非数组类型：比较类型本身。
			if (!ClassUtils.resolvePrimitiveIfNecessary(candidateParameter).equals(ClassUtils.resolvePrimitiveIfNecessary(genericParameter.toClass()))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 搜索具有与提供的桥接方法相匹配的擦除签名的泛型 {@link Method} 声明。
	 * @throws IllegalStateException 如果找不到泛型声明
	 */
	@Nullable
	private static Method findGenericDeclaration(Method bridgeMethod) {
		// 在父类型中搜索具有与桥接方法相同签名的方法。
		Class<?> superclass = bridgeMethod.getDeclaringClass().getSuperclass();
		while (superclass != null && Object.class != superclass) {
			Method method = searchForMatch(superclass, bridgeMethod);
			if (method != null && !method.isBridge()) {
				return method;
			}
			superclass = superclass.getSuperclass();
		}

		Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(bridgeMethod.getDeclaringClass());
		return searchInterfaces(interfaces, bridgeMethod);
	}

	@Nullable
	private static Method searchInterfaces(Class<?>[] interfaces, Method bridgeMethod) {
		for (Class<?> ifc : interfaces) {
			Method method = searchForMatch(ifc, bridgeMethod);
			if (method != null && !method.isBridge()) {
				return method;
			}
			else {
				method = searchInterfaces(ifc.getInterfaces(), bridgeMethod);
				if (method != null) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * 如果提供的 {@link Class} 有一个声明的 {@link Method}，其签名与
	 * 提供的 {@link Method} 相匹配，则返回这个匹配的 {@link Method}，
	 * 否则返回 {@code null}。
	 */
	@Nullable
	private static Method searchForMatch(Class<?> type, Method bridgeMethod) {
		try {
			return type.getDeclaredMethod(bridgeMethod.getName(), bridgeMethod.getParameterTypes());
		}
		catch (NoSuchMethodException ex) {
			return null;
		}
	}

	/**
	 * 比较桥接方法和桥接方法的签名。如果
	 * 参数和返回类型相同，它是 Java 6 中引入的“可见性”桥接方法
	 * 用于修复 https://bugs.openjdk.org/browse/JDK-6342411。
	 * 另请参阅 https://stas-blogspot.blogspot.com/2010/03/java-bridge-methods-explained.html
	 * @return 签名是否如所述匹配
	 */
	public static boolean isVisibilityBridgeMethodPair(Method bridgeMethod, Method bridgedMethod) {
		if (bridgeMethod == bridgedMethod) {
			return true;
		}
		return (bridgeMethod.getReturnType().equals(bridgedMethod.getReturnType()) &&
				bridgeMethod.getParameterCount() == bridgedMethod.getParameterCount() &&
				Arrays.equals(bridgeMethod.getParameterTypes(), bridgedMethod.getParameterTypes()));
	}

}
