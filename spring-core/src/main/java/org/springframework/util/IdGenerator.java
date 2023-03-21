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

package org.springframework.util;

import java.util.UUID;

/**
 * 生成通用唯一标识符（{@link UUID UUIDs}）的合同。
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@FunctionalInterface
public interface IdGenerator {

	/**
	 * 生成新的标识符。
	 * @return 生成的标识符
	 */
	UUID generateId();

}
