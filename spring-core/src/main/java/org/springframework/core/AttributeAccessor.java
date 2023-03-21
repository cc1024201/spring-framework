/*
 * 版权 2002-2021 原作者或作者。
 *
 * 根据 Apache 许可证 2.0 版（“许可证”）获得许可；
 * 除非符合许可证，否则不得使用此文件。
 * 您可以在以下位置获得许可证副本：
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则在许可证下分发的软件
 * 将按“原样”基础分发，不附带任何明示或暗示的担保或条件。
 * 有关许可证下允许和限制的特定语言，请参阅许可证。
 */

package org.springframework.core;

import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 定义附加和访问元数据的通用合同接口
 * 从任意对象。
 *
 * @author Rob Harrop
 * @author Sam Brannen
 * @since 2.0
 */
public interface AttributeAccessor {

	/**
	 * 将由 {@code name} 定义的属性设置为提供的 {@code value}。
	 * <p>如果 {@code value} 为 {@code null}，则删除属性 {@link #removeAttribute}。
	 * <p>通常，用户应注意使用完全限定的名称来防止与其他
	 * 元数据属性重叠，也许使用
	 * 类或包名称作为前缀。
	 * @param name 唯一属性键
	 * @param value 要附加的属性值
	 */
	void setAttribute(String name, @Nullable Object value);

	/**
	 * 获取由 {@code name} 标识的属性的值。
	 * <p>如果属性不存在，则返回 {@code null}。
	 * @param name 唯一属性键
	 * @return 属性的当前值（如果有）
	 */
	@Nullable
	Object getAttribute(String name);

	/**
	 * 如果需要，计算由 {@code name} 标识的属性的新值
	 * 并在此 {@code AttributeAccessor} 中 {@linkplain #setAttribute 设置} 新值。
	 * <p>如果此 {@code AttributeAccessor} 中已存在
	 * 由 {@code name} 标识的属性的值，则返回现有值
	 * 而不应用提供的计算函数。
	 * <p>此方法的默认实现不是线程安全的，但可以
	 * 由此接口的具体实现覆盖。
	 * @param <T> 属性值的类型
	 * @param name 唯一属性键
	 * @param computeFunction 一个计算属性的新值的函数
	 * 名称；函数不能返回 {@code null} 值
	 * @return 名为属性的现有值或新计算值
	 * @since 5.3.3
	 * @see #getAttribute(String)
	 * @see #setAttribute(String, Object)
	 */
	@SuppressWarnings("unchecked")
	default <T> T computeAttribute(String name, Function<String, T> computeFunction) {
		Assert.notNull(name, "名称不能为空");
		Assert.notNull(computeFunction, "计算函数不能为空");
		Object value = getAttribute(name);
		if (value == null) {
			value = computeFunction.apply(name);
			Assert.state(value != null,
					() -> String.format("计算函数不能为名为 '%s' 的属性返回 null", name));
			setAttribute(name, value);
		}
		return (T) value;
	}

	/**
	 * 删除由 {@code name} 标识的属性并返回其值。
	 * <p>如果没有找到 {@code name} 下的属性，则返回 {@code null}。
	 * @param name 唯一属性键
	 * @return 属性的最后一个值（如果有）
	 */
	@Nullable
	Object removeAttribute(String name);

	/**
	 * 如果存在由 {@code name} 标识的属性，则返回 {@code true}。
	 * <p>否则返回 {@code false}。
	 * @param name 唯一属性键
	 */
	boolean hasAttribute(String name);

	/**
	 * 返回所有属性的名称。
	 */
	String[] attributeNames();

}
