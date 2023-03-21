/*
 * 版权 2002-2023 原作者或作者。
 *
 * 根据 Apache 许可证 2.0 版（“许可证”）获得许可；
 * 除非符合许可证，否则不得使用此文件。
 * 您可以在以下位置获得许可证副本：
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则在许可证下分发的软件
 * 将按“原样”基础分发，不附带任何明示或暗示的担保或条件。
 * 有关许可证所规定的权限和限制的详细信息，请参阅许可证。
 */

package org.springframework.core;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link AttributeAccessor AttributeAccessors} 的支持类，为所有方法提供基本实现。由子类扩展。
 *
 * <p>如果子类和所有属性值都是 {@link Serializable}，则为 {@link Serializable}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0
 */
@SuppressWarnings("serial")
public abstract class AttributeAccessorSupport implements AttributeAccessor, Serializable {

	/** 具有 String 键和 Object 值的 Map。 */
	private final Map<String, Object> attributes = new LinkedHashMap<>();


	@Override
	public void setAttribute(String name, @Nullable Object value) {
		Assert.notNull(name, "名称不能为空");
		if (value != null) {
			this.attributes.put(name, value);
		}
		else {
			removeAttribute(name);
		}
	}

	@Override
	@Nullable
	public Object getAttribute(String name) {
		Assert.notNull(name, "名称不能为空");
		return this.attributes.get(name);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T computeAttribute(String name, Function<String, T> computeFunction) {
		Assert.notNull(name, "名称不能为空");
		Assert.notNull(computeFunction, "计算函数不能为空");
		Object value = this.attributes.computeIfAbsent(name, computeFunction);
		Assert.state(value != null,
				() -> String.format("计算函数不能为名为 '%s' 的属性返回 null", name));
		return (T) value;
	}

	@Override
	@Nullable
	public Object removeAttribute(String name) {
		Assert.notNull(name, "名称不能为空");
		return this.attributes.remove(name);
	}

	@Override
	public boolean hasAttribute(String name) {
		Assert.notNull(name, "名称不能为空");
		return this.attributes.containsKey(name);
	}

	@Override
	public String[] attributeNames() {
		return StringUtils.toStringArray(this.attributes.keySet());
	}


	/**
	 * 从提供的 AttributeAccessor 复制属性到此访问器。
	 * @param source 要从中复制的 AttributeAccessor
	 */
	protected void copyAttributesFrom(AttributeAccessor source) {
		Assert.notNull(source, "源不能为空");
		String[] attributeNames = source.attributeNames();
		for (String attributeName : attributeNames) {
			setAttribute(attributeName, source.getAttribute(attributeName));
		}
	}


	@Override
	public boolean equals(@Nullable Object obj) {
		return (this == obj || (obj instanceof AttributeAccessorSupport that &&
				this.attributes.equals(that.attributes)));
	}

	@Override
	public int hashCode() {
		return this.attributes.hashCode();
	}

}
