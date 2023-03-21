/*
 * 版权 2002-2022 原作者或作者。
 *
 * 根据Apache许可证2.0版（“许可证”）授权；
 * 除非符合许可证，否则不得使用此文件。
 * 您可以在以下位置获得许可证副本：
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则软件
 * 根据许可证分发的是“按原样”基础，
 * 不作任何明示或暗示的保证或条件。
 * 有关许可证下的权限和限制的详细信息，请参阅许可证。
 */

package org.springframework.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.lang.Nullable;

/**
 * 断言实用程序类，用于验证参数。
 *
 * <p>在运行时早期清晰地识别程序员错误。
 *
 * <p>例如，如果公共方法的合同规定不允许{@code null}参数，
 * 可以使用{@code Assert}来验证该合同。这样做清楚地表明了合同违规，
 * 并保护了类的不变式。
 *
 * <p>通常用于验证方法参数而不是配置属性，
 * 以检查通常是程序员错误而不是配置错误的情况。与配置初始化代码相反，
 * 这些方法通常没有回退到默认值的必要。
 *
 * <p>此类类似于JUnit的断言库。如果参数值被认为是无效的，
 * 通常会抛出{@link IllegalArgumentException}。例如：
 *
 * <pre class="code">
 * Assert.notNull(clazz, "类不能为空");
 * Assert.isTrue(i &gt; 0, "值必须大于零");</pre>
 *
 * <p>主要用于框架内部；对于更全面的断言实用程序套件，
 * 请考虑从<a href="https://commons.apache.org/proper/commons-lang/">Apache Commons Lang</a>的
 * {@code org.apache.commons.lang3.Validate}，Google Guava的
 * <a href="https://github.com/google/guava/wiki/PreconditionsExplained">Preconditions</a>，
 * 或类似的第三方库。
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Colin Sampaleanu
 * @author Rob Harrop
 * @since 1.1.2
 */
public abstract class Assert {

	/**
	 * 断言一个布尔表达式，如果表达式计算为{@code false}，则抛出一个{@code IllegalStateException}。
	 * <p>如果您希望在断言失败时抛出一个{@code IllegalArgumentException}，请调用{@link #isTrue}。
	 * <pre class="code">Assert.state(id == null, "id属性不能已经初始化");</pre>
	 * @param expression 一个布尔表达式
	 * @param message 如果断言失败，要使用的异常消息
	 * @throws IllegalStateException 如果{@code expression}为{@code false}
	 */
	public static void state(boolean expression, String message) {
		if (!expression) {
			throw new IllegalStateException(message);
		}
	}

	/**
	 * 断言一个布尔表达式，如果表达式计算为{@code false}，则抛出一个{@code IllegalStateException}。
	 * <p>如果您希望在断言失败时抛出一个{@code IllegalArgumentException}，请调用{@link #isTrue}。
	 * <pre class="code">
	 * Assert.state(entity.getId() == null,
	 *     () -&gt; "实体 " + entity.getName() + " 的ID不能已经初始化");
	 * </pre>
	 * @param expression 一个布尔表达式
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者
	 * @throws IllegalStateException 如果{@code expression}为{@code false}
	 * @since 5.0
	 */
	public static void state(boolean expression, Supplier<String> messageSupplier) {
		if (!expression) {
			throw new IllegalStateException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言一个布尔表达式，如果表达式计算为{@code false}，则抛出一个{@code IllegalStateException}。
	 * @deprecated 自4.3.7起，改用{@link #state(boolean, String)}；
	 * 在6.1中删除
	 */
	@Deprecated(forRemoval = true)
	public static void state(boolean expression) {
		state(expression, "[断言失败] - 此状态不变量必须为真");
	}

	/**
	 * 断言一个布尔表达式，如果表达式计算为{@code false}，则抛出一个{@code IllegalArgumentException}。
	 * <pre class="code">Assert.isTrue(i &gt; 0, "值必须大于零");</pre>
	 * @param expression 一个布尔表达式
	 * @param message 如果断言失败，要使用的异常消息
	 * @throws IllegalArgumentException 如果{@code expression}为{@code false}
	 */
	public static void isTrue(boolean expression, String message) {
		if (!expression) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言一个布尔表达式，如果表达式计算为{@code false}，则抛出一个{@code IllegalArgumentException}。
	 * <pre class="code">
	 * Assert.isTrue(i &gt; 0, () -&gt; "值 '" + i + "' 必须大于零");
	 * </pre>
	 * @param expression 一个布尔表达式
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者
	 * @throws IllegalArgumentException 如果{@code expression}为{@code false}
	 * @since 5.0
	 */
	public static void isTrue(boolean expression, Supplier<String> messageSupplier) {
		if (!expression) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言一个布尔表达式，如果表达式计算为{@code false}，则抛出一个{@code IllegalArgumentException}。
	 * @deprecated 自4.3.7起，改用{@link #isTrue(boolean, String)}；
	 * 在6.1中删除
	 */
	@Deprecated(forRemoval = true)
	public static void isTrue(boolean expression) {
		isTrue(expression, "[断言失败] - 此表达式必须为真");
	}

	/**
	 * 断言一个对象是{@code null}。
	 * <pre class="code">Assert.isNull(value, "值必须为null");</pre>
	 * @param object 要检查的对象
	 * @param message 如果断言失败，要使用的异常消息
	 * @throws IllegalArgumentException 如果对象不是{@code null}
	 */
	public static void isNull(@Nullable Object object, String message) {
		if (object != null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言一个对象是{@code null}。
	 * <pre class="code">
	 * Assert.isNull(value, () -&gt; "值 '" + value + "' 必须为null");
	 * </pre>
	 * @param object 要检查的对象
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者
	 * @throws IllegalArgumentException 如果对象不是{@code null}
	 * @since 5.0
	 */
	public static void isNull(@Nullable Object object, Supplier<String> messageSupplier) {
		if (object != null) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言一个对象是{@code null}。
	 * @deprecated 自4.3.7起，改用{@link #isNull(Object, String)}；
	 * 在6.1中删除
	 */
	@Deprecated(forRemoval = true)
	public static void isNull(@Nullable Object object) {
		isNull(object, "[断言失败] - 对象参数必须为null");
	}

	/**
	 * 断言一个对象不是{@code null}。
	 * <pre class="code">Assert.notNull(clazz, "类不能为空");</pre>
	 * @param object 要检查的对象
	 * @param message 如果断言失败，要使用的异常消息
	 * @throws IllegalArgumentException 如果对象是{@code null}
	 * @since 5.0
	 */
	public static void notNull(@Nullable Object object, String message) {
		if (object == null) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言一个对象不是{@code null}。
	 * <pre class="code">
	 * Assert.notNull(clazz, () -&gt; "类 '" + className + "' 不能为空");
	 * </pre>
	 * @param object 要检查的对象
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者
	 * @throws IllegalArgumentException 如果对象是{@code null}
	 * @since 5.0
	 */
	public static void notNull(@Nullable Object object, Supplier<String> messageSupplier) {
		if (object == null) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言一个对象不是{@code null}。
	 * @deprecated 自4.3.7起，改用{@link #notNull(Object, String)}；
	 * 在6.1中删除
	 */
	@Deprecated(forRemoval = true)
	public static void notNull(@Nullable Object object) {
		notNull(object, "[断言失败] - 此参数是必需的；它不能为null");
	}

	/**
	 * 断言给定的字符串不为空；也就是说，
	 * 它不能为{@code null}，也不能是空字符串。
	 * <pre class="code">Assert.hasLength(name, "名称不能为空");</pre>
	 * @param text 要检查的字符串
	 * @param message 如果断言失败，要使用的异常消息
	 * @throws IllegalArgumentException 如果文本为空
	 * @see StringUtils#hasLength
	 */
	public static void hasLength(@Nullable String text, String message) {
		if (!StringUtils.hasLength(text)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言给定的字符串不为空；也就是说，
	 * 它不能为{@code null}，也不能是空字符串。
	 * <pre class="code">
	 * Assert.hasLength(account.getName(),
	 *     () -&gt; "帐户 '" + account.getId() + "' 的名称不能为空");
	 * </pre>
	 * @param text 要检查的字符串
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者
	 * @throws IllegalArgumentException 如果文本为空
	 * @since 5.0
	 * @see StringUtils#hasLength
	 */
	public static void hasLength(@Nullable String text, Supplier<String> messageSupplier) {
		if (!StringUtils.hasLength(text)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言给定的字符串不为空；也就是说，
	 * 它不能为{@code null}，也不能是空字符串。
	 * @deprecated 自4.3.7起，改用{@link #hasLength(String, String)}；
	 * 在6.1中删除
	 */
	@Deprecated(forRemoval = true)
	public static void hasLength(@Nullable String text) {
		hasLength(text,
				"[断言失败] - 此字符串参数必须有长度；它不能为null或空");
	}

	/**
	 * 断言给定的字符串包含有效的文本内容；也就是说，它必须不是
	 * {@code null}，并且必须至少包含一个非空白字符。
	 * <pre class="code">Assert.hasText(name, "'name' 不能为空");</pre>
	 * @param text 要检查的字符串
	 * @param message 如果断言失败，要使用的异常消息
	 * @throws IllegalArgumentException 如果文本不包含有效的文本内容
	 * @see StringUtils#hasText
	 */
	public static void hasText(@Nullable String text, String message) {
		if (!StringUtils.hasText(text)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言给定的字符串包含有效的文本内容；也就是说，它必须不是
	 * {@code null}，并且必须至少包含一个非空白字符。
	 * <pre class="code">
	 * Assert.hasText(account.getName(),
	 *     () -&gt; "帐户 '" + account.getId() + "' 的名称不能为空");
	 * </pre>
	 * @param text 要检查的字符串
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者
	 * @throws IllegalArgumentException 如果文本不包含有效的文本内容
	 * @since 5.0
	 * @see StringUtils#hasText
	 */
	public static void hasText(@Nullable String text, Supplier<String> messageSupplier) {
		if (!StringUtils.hasText(text)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言给定的字符串包含有效的文本内容；也就是说，它必须不是
	 * {@code null}，并且必须至少包含一个非空白字符。
	 * @deprecated 自4.3.7起，改用{@link #hasText(String, String)}；
	 * 在6.1中删除
	 */
	@Deprecated(forRemoval = true)
	public static void hasText(@Nullable String text) {
		hasText(text,
				"[断言失败] - 此字符串参数必须有文本；它不能为null、空或仅包含空白字符");
	}

	/**
	 * 断言给定的文本不包含给定的子字符串。
	 * <pre class="code">Assert.doesNotContain(name, "rod", "名称不能包含'rod'");</pre>
	 * @param textToSearch 要搜索的文本
	 * @param substring 要在文本中查找的子字符串
	 * @param message 如果断言失败，要使用的异常消息
	 * @throws IllegalArgumentException 如果文本包含子字符串
	 */
	public static void doesNotContain(@Nullable String textToSearch, String substring, String message) {
		if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
				textToSearch.contains(substring)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言给定的文本不包含给定的子字符串。
	 * <pre class="code">
	 * Assert.doesNotContain(name, "rod", () -&gt; "名称不能包含'rod'");
	 * </pre>
	 * @param textToSearch 要搜索的文本
	 * @param substring 要在文本中查找的子字符串
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者
	 * @throws IllegalArgumentException 如果文本包含子字符串
	 * @since 5.0
	 */
	public static void doesNotContain(@Nullable String textToSearch, String substring, Supplier<String> messageSupplier) {
		if (StringUtils.hasLength(textToSearch) && StringUtils.hasLength(substring) &&
				textToSearch.contains(substring)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言数组包含元素；也就是说，它必须不是
	 * {@code null}，并且必须至少包含一个元素。
	 * <pre class="code">Assert.notEmpty(array, "数组必须包含元素");</pre>
	 * @param array 要检查的数组
	 * @param message 如果断言失败，要使用的异常消息
	 * @throws IllegalArgumentException 如果数组是{@code null}或
	 * 不包含元素
	 */
	public static void notEmpty(@Nullable Object[] array, String message) {
		if (ObjectUtils.isEmpty(array)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言数组包含元素；也就是说，它必须不是
	 * {@code null}，并且必须至少包含一个元素。
	 * <pre class="code">
	 * Assert.notEmpty(array, () -&gt; "数组 " + arrayType + " 必须包含元素");
	 * </pre>
	 * @param array 要检查的数组
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者
	 * @throws IllegalArgumentException 如果数组是{@code null}或
	 * 不包含元素
	 * @since 5.0
	 */
	public static void notEmpty(@Nullable Object[] array, Supplier<String> messageSupplier) {
		if (ObjectUtils.isEmpty(array)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言数组包含元素；也就是说，它必须不是
	 * {@code null}，并且必须至少包含一个元素。
	 * @deprecated 自4.3.7起，改用{@link #notEmpty(Object[], String)}；
	 * 在6.1中删除
	 */
	@Deprecated(forRemoval = true)
	public static void notEmpty(@Nullable Object[] array) {
		notEmpty(array,
				"[断言失败] - 这个数组不能为空：它必须至少包含1个元素");
	}

	/**
	 * 断言数组不包含{@code null}元素。
	 * <p>注意：如果数组为空，不会抱怨！
	 * <pre class="code">Assert.noNullElements(array, "数组必须包含非空元素");</pre>
	 * @param array 要检查的数组
	 * @param message 如果断言失败，要使用的异常消息
	 * @throws IllegalArgumentException 如果对象数组包含{@code null}元素
	 */
	public static void noNullElements(@Nullable Object[] array, String message) {
		if (array != null) {
			for (Object element : array) {
				if (element == null) {
					throw new IllegalArgumentException(message);
				}
			}
		}
	}

	/**
	 * 断言数组不包含{@code null}元素。
	 * <p>注意：如果数组为空，不会抱怨！
	 * <pre class="code">
	 * Assert.noNullElements(array, () -&gt; "数组 " + arrayType + " 必须包含非空元素");
	 * </pre>
	 * @param array 要检查的数组
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者
	 * @throws IllegalArgumentException 如果对象数组包含{@code null}元素
	 * @since 5.0
	 */
	public static void noNullElements(@Nullable Object[] array, Supplier<String> messageSupplier) {
		if (array != null) {
			for (Object element : array) {
				if (element == null) {
					throw new IllegalArgumentException(nullSafeGet(messageSupplier));
				}
			}
		}
	}

	/**
	 * 断言数组不包含{@code null}元素。
	 * @deprecated 自4.3.7起，改用{@link #noNullElements(Object[], String)}；
	 * 在6.1中删除
	 */
	@Deprecated(forRemoval = true)
	public static void noNullElements(@Nullable Object[] array) {
		noNullElements(array, "[断言失败] - 这个数组不能包含任何空元素");
	}

	/**
	 * 断言集合包含元素；也就是说，它必须不是
	 * {@code null}，并且必须至少包含一个元素。
	 * <pre class="code">Assert.notEmpty(collection, "集合必须包含元素");</pre>
	 * @param collection 要检查的集合
	 * @param message 如果断言失败，要使用的异常消息
	 * @throws IllegalArgumentException 如果集合是{@code null}或
	 * 不包含元素
	 */
	public static void notEmpty(@Nullable Collection<?> collection, String message) {
		if (CollectionUtils.isEmpty(collection)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言集合包含元素；也就是说，它必须不是
	 * {@code null}，并且必须至少包含一个元素。
	 * <pre class="code">
	 * Assert.notEmpty(collection, () -&gt; "集合 " + collectionType + " 必须包含元素");
	 * </pre>
	 * @param collection 要检查的集合
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者
	 * @throws IllegalArgumentException 如果集合是{@code null}或
	 * 不包含元素
	 * @since 5.0
	 */
	public static void notEmpty(@Nullable Collection<?> collection,Supplier<String> messageSupplier) {
		if (CollectionUtils.isEmpty(collection)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言映射包含条目；也就是说，它必须不是
	 * {@code null}，并且必须至少包含一个条目。
	 * <pre class="code">Assert.notEmpty(map, "映射必须包含条目");</pre>
	 * @param map 要检查的映射
	 * @param message 如果断言失败，要使用的异常消息
	 * @throws IllegalArgumentException 如果映射是{@code null}或
	 * 不包含条目
	 */
	public static void notEmpty(@Nullable Map<?, ?> map, String message) {
		if (CollectionUtils.isEmpty(map)) {
			throw new IllegalArgumentException(message);
		}
	}

	/**
	 * 断言映射包含条目；也就是说，它必须不是
	 * {@code null}，并且必须至少包含一个条目。
	 * <pre class="code">
	 * Assert.notEmpty(map, () -&gt; "映射 " + mapType + " 必须包含条目");
	 * </pre>
	 * @param map 要检查的映射
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者
	 * @throws IllegalArgumentException 如果映射是{@code null}或
	 * 不包含条目
	 * @since 5.0
	 */
	public static void notEmpty(@Nullable Map<?, ?> map, Supplier<String> messageSupplier) {
		if (CollectionUtils.isEmpty(map)) {
			throw new IllegalArgumentException(nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言映射包含条目；也就是说，它必须不是
	 * {@code null}，并且必须至少包含一个条目。
	 * @deprecated 自4.3.7起，改用{@link #notEmpty(Map, String)}；
	 * 在6.1中删除
	 */
	@Deprecated(forRemoval = true)
	public static void notEmpty(@Nullable Map<?, ?> map) {
		notEmpty(map, "[断言失败] - 这个映射不能为空；它必须至少包含一个条目");
	}

	/**
	 * 断言提供的对象是提供的类的实例。
	 * <pre class="code">Assert.instanceOf(Foo.class, foo, "Foo类型预期");</pre>
	 * @param type 要检查的类型
	 * @param obj 要检查的对象
	 * @param message 一个消息，将在前面添加以提供进一步的上下文。
	 * 如果为空或以":"或";"或","或"."结尾，将附加完整的异常消息。
	 * 如果以空格结尾，将附加违规对象的类型的名称。
	 * 在任何其他情况下，将附加一个带有空格和违规对象类型名称的":"。
	 * @throws IllegalArgumentException 如果对象不是类型的实例
	 */
	public static void isInstanceOf(Class<?> type, @Nullable Object obj, String message) {
		notNull(type, "要检查的类型不能为空");
		if (!type.isInstance(obj)) {
			instanceCheckFailed(type, obj, message);
		}
	}

	/**
	 * 断言提供的对象是提供的类的实例。
	 * <pre class="code">
	 * Assert.instanceOf(Foo.class, foo, () -&gt; "处理 " + Foo.class.getSimpleName() + ":");
	 * </pre>
	 * @param type 要检查的类型
	 * @param obj 要检查的对象
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者。有关详细信息，请参见{@link #isInstanceOf(Class, Object, String)}。
	 * @throws IllegalArgumentException 如果对象不是类型的实例
	 * @since 5.0
	 */
	public static void isInstanceOf(Class<?> type, @Nullable Object obj, Supplier<String> messageSupplier) {
		notNull(type, "要检查的类型不能为空");
		if (!type.isInstance(obj)) {
			instanceCheckFailed(type, obj, nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言提供的对象是提供的类的实例。
	 * <pre class="code">Assert.instanceOf(Foo.class, foo);</pre>
	 * @param type 要检查的类型
	 * @param obj 要检查的对象
	 * @throws IllegalArgumentException 如果对象不是类型的实例
	 */
	public static void isInstanceOf(Class<?> type, @Nullable Object obj) {
		isInstanceOf(type, obj, "");
	}

	/**
	 * 断言{@code superType.isAssignableFrom(subType)}为{@code true}。
	 * <pre class="code">Assert.isAssignable(Number.class, myClass, "Number类型预期");</pre>
	 * @param superType 要检查的超类型
	 * @param subType 要检查的子类型
	 * @param message 一个消息，将在前面添加以提供进一步的上下文。
	 * 如果为空或以":"或";"或","或"."结尾，将附加完整的异常消息。
	 * 如果以空格结尾，将附加违规子类型的名称。
	 * 在任何其他情况下，将附加一个带有空格和违规子类型名称的":"。
	 * @throws IllegalArgumentException 如果类不可分配
	 */
	public static void isAssignable(Class<?> superType, @Nullable Class<?> subType, String message) {
		notNull(superType, "要检查的超类型不能为空");
		if (subType == null || !superType.isAssignableFrom(subType)) {
			assignableCheckFailed(superType, subType, message);
		}
	}

	/**
	 * 断言{@code superType.isAssignableFrom(subType)}为{@code true}。
	 * <pre class="code">
	 * Assert.isAssignable(Number.class, myClass, () -&gt; "处理 " + myAttributeName + ":");
	 * </pre>
	 * @param superType 要检查的超类型
	 * @param subType 要检查的子类型
	 * @param messageSupplier 如果断言失败，要使用的异常消息的提供者。有关详细信息，请参见{@link #isAssignable(Class, Class, String)}。
	 * @throws IllegalArgumentException 如果类不可分配
	 * @since 5.0
	 */
	public static void isAssignable(Class<?> superType, @Nullable Class<?> subType, Supplier<String> messageSupplier) {
		notNull(superType, "要检查的超类型不能为空");
		if (subType == null || !superType.isAssignableFrom(subType)) {
			assignableCheckFailed(superType, subType, nullSafeGet(messageSupplier));
		}
	}

	/**
	 * 断言{@code superType.isAssignableFrom(subType)}为{@code true}。
	 * <pre class="code">Assert.isAssignable(Number.class, myClass);</pre>
	 * @param superType 要检查的超类型
	 * @param subType 要检查的子类型
	 * @throws IllegalArgumentException 如果类不可分配
	 */
	public static void isAssignable(Class<?> superType, Class<?> subType) {
		isAssignable(superType, subType, "");
	}


	private static void instanceCheckFailed(Class<?> type, @Nullable Object obj, @Nullable String msg) {
		String className = (obj != null ? obj.getClass().getName() : "null");
		String result = "";
		boolean defaultMessage = true;
		if (StringUtils.hasLength(msg)) {
			if (endsWithSeparator(msg)) {
				result = msg + " ";
			}
			else {
				result = messageWithTypeName(msg, className);
				defaultMessage = false;
			}
		}
		if (defaultMessage) {
			result = result + ("对象的类 [" + className + "] 必须是 " + type + " 的实例");
		}
		throw new IllegalArgumentException(result);
	}

	private static void assignableCheckFailed(Class<?> superType, @Nullable Class<?> subType, @Nullable String msg) {
		String result = "";
		boolean defaultMessage = true;
		if (StringUtils.hasLength(msg)) {
			if (endsWithSeparator(msg)) {
				result = msg + " ";
			}
			else {
				result = messageWithTypeName(msg, subType);
				defaultMessage = false;
			}
		}
		if (defaultMessage) {
			result = result + (subType + " 不可分配给 " + superType);
		}
		throw new IllegalArgumentException(result);
	}

	private static boolean endsWithSeparator(String msg) {
		return (msg.endsWith(":") || msg.endsWith(";") || msg.endsWith(",") || msg.endsWith("."));
	}

	private static String messageWithTypeName(String msg, @Nullable Object typeName) {
		return msg + (msg.endsWith(" ") ? "" : ": ") + typeName;
	}

	@Nullable
	private static String nullSafeGet(@Nullable Supplier<String> messageSupplier) {
		return (messageSupplier != null ? messageSupplier.get() : null);
	}

}
