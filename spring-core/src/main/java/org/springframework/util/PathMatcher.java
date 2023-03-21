/*
 * 版权 2002-2019 原作者或作者。
 *
 * 根据 Apache 许可证 2.0 版（“许可证”）获得许可；
 * 除非符合许可证，否则不得使用此文件。
 * 您可以在以下位置获得许可证副本：
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则软件
 * 根据许可证分发的是“按原样”基础，
 * 无任何明示或暗示的保证或条件。
 * 有关许可证所规定的权限和
 * 限制的具体语言，请参阅许可证。
 */

package org.springframework.util;

import java.util.Comparator;
import java.util.Map;

/**
 * 基于 {@code String} 的路径匹配策略接口。
 *
 * <p>由 {@link org.springframework.core.io.support.PathMatchingResourcePatternResolver}，
 * {@link org.springframework.web.servlet.handler.AbstractUrlHandlerMapping}，
 * 和 {@link org.springframework.web.servlet.mvc.WebContentInterceptor} 使用。
 *
 * <p>默认实现是 {@link AntPathMatcher}，支持
 * Ant 样式的模式语法。
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see AntPathMatcher
 */
public interface PathMatcher {

	/**
	 * 给定的 {@code path} 是否表示一个可以由此接口的实现进行匹配的模式？
	 * <p>如果返回值为 {@code false}，则无需使用 {@link #match}
	 * 方法，因为对静态路径字符串进行直接相等比较将产生相同的结果。
	 * @param path 要检查的路径
	 * @return 如果给定的 {@code path} 表示一个模式，则返回 {@code true}
	 */
	boolean isPattern(String path);

	/**
	 * 根据此 PathMatcher 的匹配策略，将给定的 {@code path} 与给定的 {@code pattern} 进行匹配。
	 * @param pattern 要匹配的模式
	 * @param path 要测试的路径
	 * @return 如果提供的 {@code path} 匹配，则返回 {@code true}，否则返回 {@code false}
	 */
	boolean match(String pattern, String path);

	/**
	 * 根据此 PathMatcher 的匹配策略，将给定的 {@code path} 与给定的 {@code pattern} 的相应部分进行匹配。
	 * <p>确定模式是否至少与给定基本路径匹配，假设完整路径可能也匹配。
	 * @param pattern 要匹配的模式
	 * @param path 要测试的路径
	 * @return 如果提供的 {@code path} 匹配，则返回 {@code true}，否则返回 {@code false}
	 */
	boolean matchStart(String pattern, String path);

	/**
	 * 给定一个模式和一个完整路径，确定模式映射的部分。
	 * <p>此方法应找出通过实际模式动态匹配的路径的哪一部分，
	 * 也就是说，它从给定的完整路径中去掉静态定义的前导路径，只返回实际模式匹配的路径部分。
	 * <p>例如：对于 "myroot/*.html" 作为模式和 "myroot/myfile.html" 作为完整路径，
	 * 此方法应返回 "myfile.html"。详细的确定规则由此 PathMatcher 的匹配策略指定。
	 * <p>简单的实现可能在实际模式的情况下返回给定的完整路径，
	 * 而在模式不包含任何动态部分的情况下返回空字符串（即 {@code pattern} 参数为
	 * 不符合实际 {@link #isPattern pattern} 的静态路径）。
	 * 复杂的实现将区分给定路径模式的静态部分和动态部分。
	 * @param pattern 路径模式
	 * @param path 要内省的完整路径
	 * @return 给定 {@code path} 的模式映射部分（永远不为 {@code null}）
	 */
	String extractPathWithinPattern(String pattern, String path);

	/**
	 * 给定一个模式和一个完整路径，提取 URI 模板变量。URI 模板变量通过花括号（'{' 和 '}'）表示。
	 * <p>例如：对于模式 "/hotels/{hotel}" 和路径 "/hotels/1"，此方法将返回一个包含 "hotel" &rarr; "1" 的映射。
	 * @param pattern 可能包含 URI 模板的路径模式
	 * @param path 要从中提取模板变量的完整路径
	 * @return 一个映射，包含变量名作为键；变量值作为值
	 */
	Map<String, String> extractUriTemplateVariables(String pattern, String path);

	/**
	 * 给定一个完整路径，返回一个适用于按该路径的明确顺序对模式进行排序的 {@link Comparator}。
	 * <p>使用的完整算法取决于底层实现，但通常，返回的 {@code Comparator} 将
	 * {@linkplain java.util.List#sort(java.util.Comparator) sort}
	 * 一个列表，以便更具体的模式在通用模式之前。
	 * @param path 用于比较的完整路径
	 * @return 能够按明确顺序对模式进行排序的比较器
	 */
	Comparator<String> getPatternComparator(String path);

	/**
	 * 将两个模式组合成一个新模式并返回。
	 * <p>组合两个模式的完整算法取决于底层实现。
	 * @param pattern1 第一个模式
	 * @param pattern2 第二个模式
	 * @return 两个模式的组合
	 * @throws IllegalArgumentException 当两个模式无法组合时
	 */
	String combine(String pattern1, String pattern2);

}
