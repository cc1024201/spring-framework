/*
 * 版权 2002-2020 原作者或作者。
 *
 * 根据 Apache 许可证 2.0 版（“许可证”）获得许可；
 * 除非符合许可证，否则不得使用此文件。
 * 您可以在以下位置获取许可证副本：
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则在许可证下分发的软件
 * 将按“原样”基础分发，不附带任何明示或暗示的担保或条件。
 * 有关许可证所规定的权限和限制的详细信息，请参阅许可证。
 */

package org.springframework.core;

/**
 * 管理别名的通用接口。作为
 * {@link org.springframework.beans.factory.support.BeanDefinitionRegistry}的超级接口。
 *
 * @author 贾根·霍勒
 * @since 2.5.2
 */
public interface AliasRegistry {

	/**
	 * 给定一个名称，为其注册一个别名。
	 * @param name 规范名称
	 * @param alias 要注册的别名
	 * @throws IllegalStateException 如果别名已在使用中
	 * 且不允许覆盖
	 */
	void registerAlias(String name, String alias);

	/**
	 * 从此注册表中删除指定的别名。
	 * @param alias 要删除的别名
	 * @throws IllegalStateException 如果找不到此别名
	 */
	void removeAlias(String alias);

	/**
	 * 确定给定名称是否定义为别名
	 * （而不是实际注册组件的名称）。
	 * @param name 要检查的名称
	 * @return 给定名称是否为别名
	 */
	boolean isAlias(String name);

	/**
	 * 如果已定义，返回给定名称的别名。
	 * @param name 要检查别名的名称
	 * @return 别名，如果没有，则为空数组
	 */
	String[] getAliases(String name);

}
