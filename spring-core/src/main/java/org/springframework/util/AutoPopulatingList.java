/*
 * 版权 2002-2018 原作者或作者。
 *
 * 根据Apache许可证2.0版（“许可证”）获得许可；
 * 除非符合许可证，否则不得使用此文件。
 * 您可以在以下位置获得许可证副本：
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则软件
 * 根据许可证分发的是“按原样”基础，
 * 不附带任何明示或暗示的保证或条件。
 * 有关许可证下允许和禁止的权限和
 * 限制，请参阅许可证。
 */

package org.springframework.util;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.springframework.lang.Nullable;

/**
 * 简单的{@link List}包装类，允许在请求时自动填充元素。这对于数据绑定到{@link List Lists}特别有用，
 * 允许元素按照“及时”方式创建并添加到{@link List}中。
 *
 * <p>注意：此类不是线程安全的。要创建线程安全版本，
 * 请使用{@link java.util.Collections#synchronizedList }实用方法。
 *
 * <p>受Commons Collections中的{@code LazyList}启发。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @param <E> 元素类型
 */
@SuppressWarnings("serial")
public class AutoPopulatingList<E> implements List<E>, Serializable {

	/**
	 * 所有操作最终委托给的{@link List}。
	 */
	private final List<E> backingList;

	/**
	 * 按需创建新的{@link List}元素的{@link ElementFactory}。
	 */
	private final ElementFactory<E> elementFactory;


	/**
	 * 创建一个新的{@code AutoPopulatingList}，它由标准的{@link ArrayList}支持，
	 * 并根据需要向支持的{@link List}中添加提供的{@link Class element Class}的新实例。
	 */
	public AutoPopulatingList(Class<? extends E> elementClass) {
		this(new ArrayList<>(), elementClass);
	}

	/**
	 * 创建一个新的{@code AutoPopulatingList}，它由提供的{@link List}支持，
	 * 并根据需要向支持的{@link List}中添加提供的{@link Class element Class}的新实例。
	 */
	public AutoPopulatingList(List<E> backingList, Class<? extends E> elementClass) {
		this(backingList, new ReflectiveElementFactory<>(elementClass));
	}

	/**
	 * 创建一个新的{@code AutoPopulatingList}，它由标准的{@link ArrayList}支持，
	 * 并使用提供的{@link ElementFactory}按需创建新元素。
	 */
	public AutoPopulatingList(ElementFactory<E> elementFactory) {
		this(new ArrayList<>(), elementFactory);
	}

	/**
	 * 创建一个新的{@code AutoPopulatingList}，它由提供的{@link List}支持，
	 * 并使用提供的{@link ElementFactory}按需创建新元素。
	 */
	public AutoPopulatingList(List<E> backingList, ElementFactory<E> elementFactory) {
		Assert.notNull(backingList, "Backing List 不能为空");
		Assert.notNull(elementFactory, "Element factory 不能为空");
		this.backingList = backingList;
		this.elementFactory = elementFactory;
	}


	@Override
	public void add(int index, E element) {
		this.backingList.add(index, element);
	}

	@Override
	public boolean add(E o) {
		return this.backingList.add(o);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return this.backingList.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return this.backingList.addAll(index, c);
	}

	@Override
	public void clear() {
		this.backingList.clear();
	}

	@Override
	public boolean contains(Object o) {
		return this.backingList.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.backingList.containsAll(c);
	}

	/**
	 * 获取提供的索引处的元素，如果该索引处没有元素，则创建它。
	 */
	@Override
	public E get(int index) {
		int backingListSize = this.backingList.size();
		E element = null;
		if (index < backingListSize) {
			element = this.backingList.get(index);
			if (element == null) {
				element = this.elementFactory.createElement(index);
				this.backingList.set(index, element);
			}
		}
		else {
			for (int x = backingListSize; x < index; x++) {
				this.backingList.add(null);
			}
			element = this.elementFactory.createElement(index);
			this.backingList.add(element);
		}
		return element;
	}

	@Override
	public int indexOf(Object o) {
		return this.backingList.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return this.backingList.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return this.backingList.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return this.backingList.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return this.backingList.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return this.backingList.listIterator(index);
	}

	@Override
	public E remove(int index) {
		return this.backingList.remove(index);
	}

	@Override
	public boolean remove(Object o) {
		return this.backingList.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return this.backingList.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return this.backingList.retainAll(c);
	}

	@Override
	public E set(int index, E element) {
		return this.backingList.set(index, element);
	}

	@Override
	public int size() {
		return this.backingList.size();
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return this.backingList.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return this.backingList.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return this.backingList.toArray(a);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return this.backingList.equals(other);
	}

	@Override
	public int hashCode() {
		return this.backingList.hashCode();
	}


	/**
	 * 用于为基于索引访问的数据结构（如{@link java.util.List}）创建元素的工厂接口。
	 *
	 * @param <E> 元素类型
	 */
	@FunctionalInterface
	public interface ElementFactory<E> {

		/**
		 * 为提供的索引创建元素。
		 * @return 元素对象
		 * @throws ElementInstantiationException 如果实例化过程失败
		 * (目标构造函数抛出的任何异常都应原样传播)
		 */
		E createElement(int index) throws ElementInstantiationException;
	}


	/**
	 * ElementFactory抛出的异常。
	 */
	public static class ElementInstantiationException extends RuntimeException {

		public ElementInstantiationException(String msg) {
			super(msg);
		}

		public ElementInstantiationException(String message, Throwable cause) {
			super(message, cause);
		}
	}


	/**
	 * ElementFactory接口的反射实现，使用
	 * {@code Class.getDeclaredConstructor().newInstance()}在给定的元素类上。
	 */
	private static class ReflectiveElementFactory<E> implements ElementFactory<E>, Serializable {

		private final Class<? extends E> elementClass;

		public ReflectiveElementFactory(Class<? extends E> elementClass) {
			Assert.notNull(elementClass, "Element class 不能为空");
			Assert.isTrue(!elementClass.isInterface(), "Element class 不能是接口类型");
			Assert.isTrue(!Modifier.isAbstract(elementClass.getModifiers()), "Element class 不能是抽象类");
			this.elementClass = elementClass;
		}

		@Override
		public E createElement(int index) {
			try {
				return ReflectionUtils.accessibleConstructor(this.elementClass).newInstance();
			}
			catch (NoSuchMethodException ex) {
				throw new ElementInstantiationException(
						"元素类没有默认构造函数: " + this.elementClass.getName(), ex);
			}
			catch (InstantiationException ex) {
				throw new ElementInstantiationException(
						"无法实例化元素类: " + this.elementClass.getName(), ex);
			}
			catch (IllegalAccessException ex) {
				throw new ElementInstantiationException(
						"无法访问元素构造函数: " + this.elementClass.getName(), ex);
			}
			catch (InvocationTargetException ex) {
				throw new ElementInstantiationException(
						"调用元素构造函数失败: " + this.elementClass.getName(), ex.getTargetException());
			}
		}
	}

}
