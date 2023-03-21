/*
 * 版权 2002-2022 原作者或作者。
 *
 * 根据 Apache 许可证 2.0 版（“许可证”）许可；
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.lang.Nullable;

/**
 * Ant 样式路径模式的 {@link PathMatcher} 实现。
 *
 * <p>此映射代码的一部分已经从 <a href="https://ant.apache.org">Apache Ant</a> 借用。
 *
 * <p>映射使用以下规则匹配 URL：<br>
 * <ul>
 * <li>{@code ?} 匹配一个字符</li>
 * <li>{@code *} 匹配零个或多个字符</li>
 * <li>{@code **} 匹配路径中的零个或多个<em>目录</em></li>
 * <li>{@code {spring:[a-z]+}} 将正则表达式 {@code [a-z]+} 与名为 "spring" 的路径变量匹配</li>
 * </ul>
 *
 * <h3>示例</h3>
 * <ul>
 * <li>{@code com/t?st.jsp} &mdash; 匹配 {@code com/test.jsp} 以及
 * {@code com/tast.jsp} 或 {@code com/txst.jsp}</li>
 * <li>{@code com/*.jsp} &mdash; 匹配
 * {@code com} 目录中的所有 {@code .jsp} 文件</li>
 * <li><code>com/&#42;&#42;/test.jsp</code> &mdash; 匹配 {@code com} 路径下的所有 {@code test.jsp}
 * 文件</li>
 * <li><code>org/springframework/&#42;&#42;/*.jsp</code> &mdash; 匹配
 * {@code org/springframework} 路径下的所有 {@code .jsp} 文件</li>
 * <li><code>org/&#42;&#42;/servlet/bla.jsp</code> &mdash; 匹配
 * {@code org/springframework/servlet/bla.jsp} 以及
 * {@code org/springframework/testing/servlet/bla.jsp} 和 {@code org/servlet/bla.jsp}</li>
 * <li>{@code com/{filename:\\w+}.jsp} 将匹配 {@code com/test.jsp} 并将值 {@code test}
 * 分配给 {@code filename} 变量</li>
 * </ul>
 *
 * <p><strong>注意：</strong> 模式和路径必须都是绝对的，或者都是相对的，以便两者匹配。因此，建议
 * 此实现的用户对模式进行清理，以便根据它们在上下文中的使用情况为它们添加 "/" 前缀。
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @author Vladislav Kisel
 * @since 16.07.2003
 */
public class AntPathMatcher implements PathMatcher {

	/**
	 * 默认路径分隔符："/"。
	 */
	public static final String DEFAULT_PATH_SEPARATOR = "/";

	private static final int CACHE_TURNOFF_THRESHOLD = 65536;

	// 定义一个正则表达式模式，用于匹配变量，例如：{variable}
	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^/]+?\\}");

	private static final char[] WILDCARD_CHARS = {'*', '?', '{'};


	private String pathSeparator;

	// 路径分隔符模式缓存
	private PathSeparatorPatternCache pathSeparatorPatternCache;

	private boolean caseSensitive = true;

	private boolean trimTokens = false;

	@Nullable
	private volatile Boolean cachePatterns;

 // 缓存已分词的模式，键为模式字符串，值为分词后的字符串数组
	private final Map<String, String[]> tokenizedPatternCache = new ConcurrentHashMap<>(256);

// 缓存 AntPathStringMatcher 实例，键为模式字符串，值为 AntPathStringMatcher 实例
final Map<String, AntPathStringMatcher> stringMatcherCache = new ConcurrentHashMap<>(256);


	/**
	 * 使用 {@link #DEFAULT_PATH_SEPARATOR} 创建一个新实例。
	 */
	public AntPathMatcher() {
		this.pathSeparator = DEFAULT_PATH_SEPARATOR;
		this.pathSeparatorPatternCache = new PathSeparatorPatternCache(DEFAULT_PATH_SEPARATOR);
	}

	/**
	 * 使用自定义路径分隔符的方便替代构造函数。
	 *
	 * @param pathSeparator 要使用的路径分隔符，不能为空。
	 * @since 4.1
	 */
	public AntPathMatcher(String pathSeparator) {
		Assert.notNull(pathSeparator, "'pathSeparator' 不能为空");
		this.pathSeparator = pathSeparator;
		this.pathSeparatorPatternCache = new PathSeparatorPatternCache(pathSeparator);
	}


	/**
	 * 设置用于模式解析的路径分隔符。
	 * <p>默认为 "/", 如 Ant。
	 */
	public void setPathSeparator(@Nullable String pathSeparator) {
		this.pathSeparator = (pathSeparator != null ? pathSeparator : DEFAULT_PATH_SEPARATOR);
		this.pathSeparatorPatternCache = new PathSeparatorPatternCache(this.pathSeparator);
	}

	/**
	 * 指定是否以区分大小写的方式执行模式匹配。
	 * <p>默认值为 {@code true}。将其切换为 {@code false} 以进行不区分大小写的匹配。
	 *
	 * @since 4.2
	 */
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	/**
	 * 指定是否修剪标记化的路径和模式。
	 * <p>默认值为 {@code false}。
	 */
	public void setTrimTokens(boolean trimTokens) {
		this.trimTokens = trimTokens;
	}

	/**
	 * 指定是否缓存传递到此匹配器的 {@link #match} 方法的模式的解析模式元数据。 值为 {@code true} 激活无限制模式缓存；值为 {@code false} 完全关闭模式缓存。
	 * <p>默认情况下，缓存处于打开状态，但是在运行时遇到太多模式以缓存时（阈值为 65536），
	 * 自动关闭缓存，假设模式的任意排列正在进入，很少有机会遇到重复模式。
	 *
	 * @see #getStringMatcher(String)
	 * @since 4.0.1
	 */
	public void setCachePatterns(boolean cachePatterns) {
		this.cachePatterns = cachePatterns;
	}

	/**
	 * 禁用模式缓存。
	 * 将 cachePatterns 设置为 false，清空 tokenizedPatternCache 和 stringMatcherCache。
	 */
	private void deactivatePatternCache() {
		this.cachePatterns = false;
		this.tokenizedPatternCache.clear();
		this.stringMatcherCache.clear();
	}


	/**
	 * 判断给定的路径是否包含模式匹配字符。
	 * <p>如果路径为 null，则返回 false。
	 * <p>如果路径中包含 '*', '?' 或者 '{}'（表示 URI 变量），则返回 true。
	 *
	 * @param path 要检查的路径
	 * @return 如果路径包含模式匹配字符，则返回 true，否则返回 false
	 */
	@Override
	public boolean isPattern(@Nullable String path) {
		if (path == null) {
			return false;
		}
		boolean uriVar = false;
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (c == '*' || c == '?') {
				return true;
			}
			if (c == '{') {
				uriVar = true;
				continue;
			}
			if (c == '}' && uriVar) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 根据给定的模式和路径进行匹配。
	 * 
	 * @param pattern 要匹配的模式
	 * @param path 要匹配的路径
	 * @return 如果模式和路径匹配，则返回 true，否则返回 false
	 */
	@Override
	public boolean match(String pattern, String path) {
		return doMatch(pattern, path, true, null);
	}

	/**
	 * 根据给定的模式和路径进行匹配的开始部分。
	 *
	 * @param pattern 要匹配的模式
	 * @param path 要匹配的路径
	 * @return 如果模式和路径的开始部分匹配，则返回 true，否则返回 false
	 */
	@Override
	public boolean matchStart(String pattern, String path) {
		return doMatch(pattern, path, false, null);
	}

	/**
	 * 实际匹配给定的 {@code path} 和给定的 {@code pattern}。
	 *
	 * @param pattern   要匹配的模式
	 * @param path      要测试的路径
	 * @param fullMatch 是否需要完整的模式匹配（否则只需匹配给定基本路径的模式匹配就足够了）
	 * @return 如果提供的 {@code path} 匹配，则返回 {@code true}，否则返回 {@code false}
	 */
	protected boolean doMatch(String pattern, @Nullable String path, boolean fullMatch,
			@Nullable Map<String, String> uriTemplateVariables) {

		if (path == null || path.startsWith(this.pathSeparator) != pattern.startsWith(this.pathSeparator)) {
			return false;
		}

		String[] pattDirs = tokenizePattern(pattern);
		if (fullMatch && this.caseSensitive && !isPotentialMatch(path, pattDirs)) {
			return false;
		}

		String[] pathDirs = tokenizePath(path);
		int pattIdxStart = 0;
		int pattIdxEnd = pattDirs.length - 1;
		int pathIdxStart = 0;
		int pathIdxEnd = pathDirs.length - 1;

		// 匹配第一个 ** 之前的所有元素
		while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
			String pattDir = pattDirs[pattIdxStart];
			if ("**".equals(pattDir)) {
				break;
			}
			if (!matchStrings(pattDir, pathDirs[pathIdxStart], uriTemplateVariables)) {
				return false;
			}
			pattIdxStart++;
			pathIdxStart++;
		}

		if (pathIdxStart > pathIdxEnd) {
			// 路径已用尽，只有当模式剩余部分为 * 或 ** 时才匹配
			if (pattIdxStart > pattIdxEnd) {
				return (pattern.endsWith(this.pathSeparator) == path.endsWith(this.pathSeparator));
			}
			if (!fullMatch) {
				return true;
			}
			if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*") && path.endsWith(this.pathSeparator)) {
				return true;
			}
			for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
				if (!pattDirs[i].equals("**")) {
					return false;
				}
			}
			return true;
		} else if (pattIdxStart > pattIdxEnd) {
			// 字符串未用尽，但模式已用尽。失败。
			return false;
		} else if (!fullMatch && "**".equals(pattDirs[pattIdxStart])) {
			// 由于模式中的 "**" 部分，路径开始部分肯定匹配。
			return true;
		}

		// 直到最后一个 '**'
		while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
			String pattDir = pattDirs[pattIdxEnd];
			if (pattDir.equals("**")) {
				break;
			}
			if (!matchStrings(pattDir, pathDirs[pathIdxEnd], uriTemplateVariables)) {
				return false;
			}
			if (pattIdxEnd == (pattDirs.length - 1)
					&& pattern.endsWith(this.pathSeparator) != path.endsWith(this.pathSeparator)) {
				return false;
			}
			pattIdxEnd--;
			pathIdxEnd--;
		}
		if (pathIdxStart > pathIdxEnd) {
			// 字符串已用尽
			for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
				if (!pattDirs[i].equals("**")) {
					return false;
				}
			}
			return true;
		}

		while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
			int patIdxTmp = -1;
			for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
				if (pattDirs[i].equals("**")) {
					patIdxTmp = i;
					break;
				}
			}
			if (patIdxTmp == pattIdxStart + 1) {
				// '**/**' 情况，所以跳过一个
				pattIdxStart++;
				continue;
			}
			// 在 str 之间找到 padIdxStart 和 padIdxTmp 之间的模式
			// strIdxStart 和 strIdxEnd 之间
			int patLength = (patIdxTmp - pattIdxStart - 1);
			int strLength = (pathIdxEnd - pathIdxStart + 1);
			int foundIdx = -1;

			strLoop:
			for (int i = 0; i <= strLength - patLength; i++) {
				for (int j = 0; j < patLength; j++) {
					String subPat = pattDirs[pattIdxStart + j + 1];
					String subStr = pathDirs[pathIdxStart + i + j];
					if (!matchStrings(subPat, subStr, uriTemplateVariables)) {
						continue strLoop;
					}
				}
				foundIdx = pathIdxStart + i;
				break;
			}

			if (foundIdx == -1) {
				return false;
			}

			pattIdxStart = patIdxTmp;
			pathIdxStart = foundIdx + patLength;
		}

		for (int i = pattIdxStart; i <= pattIdxEnd; i++) {
			if (!pattDirs[i].equals("**")) {
				return false;
			}
		}

		return true;
	}

	/**
	 * 判断给定的路径是否与模式目录数组中的某个模式可能匹配。
	 * 如果不需要修剪令牌，此方法将跳过路径分隔符和模式段，并检查是否存在通配符字符。
	 *
	 * @param path     要检查的路径
	 * @param pattDirs 模式目录数组
	 * @return 如果路径可能与模式匹配，则返回 true，否则返回 false
	 */
	private boolean isPotentialMatch(String path, String[] pattDirs) {
		if (!this.trimTokens) {
			int pos = 0;
			for (String pattDir : pattDirs) {
				int skipped = skipSeparator(path, pos, this.pathSeparator);
				pos += skipped;
				skipped = skipSegment(path, pos, pattDir);
				if (skipped < pattDir.length()) {
					return (skipped > 0 || (pattDir.length() > 0 && isWildcardChar(pattDir.charAt(0))));
				}
				pos += skipped;
			}
		}
		return true;
	}

	private int skipSegment(String path, int pos, String prefix) {
		int skipped = 0;
		for (int i = 0; i < prefix.length(); i++) {
			char c = prefix.charAt(i);
			if (isWildcardChar(c)) {
				return skipped;
			}
			int currPos = pos + skipped;
			if (currPos >= path.length()) {
				return 0;
			}
			if (c == path.charAt(currPos)) {
				skipped++;
			}
		}
		return skipped;
	}

	private int skipSeparator(String path, int pos, String separator) {
		int skipped = 0;
		while (path.startsWith(separator, pos + skipped)) {
			skipped += separator.length();
		}
		return skipped;
	}

	private boolean isWildcardChar(char c) {
		for (char candidate : WILDCARD_CHARS) {
			if (c == candidate) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 根据此匹配器的设置，将给定的路径模式分割成部分。
	 * <p>根据 {@link #setCachePatterns} 进行缓存，将实际的分词算法委托给
	 * {@link #tokenizePath(String)}。
	 *
	 * @param pattern 要分词的模式
	 * @return 分词后的模式部分
	 */
	protected String[] tokenizePattern(String pattern) {
		String[] tokenized = null;
		Boolean cachePatterns = this.cachePatterns;
		if (cachePatterns == null || cachePatterns.booleanValue()) {
			tokenized = this.tokenizedPatternCache.get(pattern);
		}
		if (tokenized == null) {
			tokenized = tokenizePath(pattern);
			if (cachePatterns == null && this.tokenizedPatternCache.size() >= CACHE_TURNOFF_THRESHOLD) {
				// 尝试适应我们遇到的运行时情况：
				// 这里显然有太多不同的模式进来...
				// 所以让我们关闭缓存，因为模式不太可能重复出现。
				deactivatePatternCache();
				return tokenized;
			}
			if (cachePatterns == null || cachePatterns.booleanValue()) {
				this.tokenizedPatternCache.put(pattern, tokenized);
			}
		}
		return tokenized;
	}

	/**
	 * 根据此匹配器的设置，将给定的路径分割成部分。
	 *
	 * @param path 要分割的路径
	 * @return 分割后的路径部分
	 */
	protected String[] tokenizePath(String path) {
		return StringUtils.tokenizeToStringArray(path, this.pathSeparator, this.trimTokens, true);
	}

	/**
	 * 测试字符串是否与模式匹配。
	 *
	 * @param pattern              要匹配的模式（永远不为 {@code null}）
	 * @param str                  必须与模式匹配的字符串（永远不为 {@code null}）
	 * @param uriTemplateVariables 可选的URI模板变量映射
	 * @return 如果字符串与模式匹配，则为 {@code true}，否则为 {@code false}
	 */
	private boolean matchStrings(String pattern, String str,
			@Nullable Map<String, String> uriTemplateVariables) {

		return getStringMatcher(pattern).matchStrings(str, uriTemplateVariables);
	}

	/**
	 * 为给定的模式构建或检索一个 {@link AntPathStringMatcher}。
	 * <p>默认实现检查此 AntPathMatcher 的内部缓存
	 * (参见 {@link #setCachePatterns})，如果没有找到缓存的副本，则创建一个新的 AntPathStringMatcher 实例。
	 * <p>在运行时遇到太多模式以进行缓存（阈值为 65536）时，
	 * 它会关闭默认缓存，假设这里有太多不同的模式进来，重复出现的模式的几率很小。
	 * <p>此方法可以重写以实现自定义缓存策略。
	 *
	 * @param pattern 要匹配的模式（永远不为 {@code null}）
	 * @return 相应的 AntPathStringMatcher（永远不为 {@code null}）
	 * @see #setCachePatterns
	 */
	protected AntPathStringMatcher getStringMatcher(String pattern) {
		AntPathStringMatcher matcher = null;
		Boolean cachePatterns = this.cachePatterns;
		if (cachePatterns == null || cachePatterns.booleanValue()) {
			matcher = this.stringMatcherCache.get(pattern);
		}
		if (matcher == null) {
			matcher = new AntPathStringMatcher(pattern, this.caseSensitive);
			if (cachePatterns == null && this.stringMatcherCache.size() >= CACHE_TURNOFF_THRESHOLD) {
				// 尝试适应我们遇到的运行时情况：
				// 这里显然有太多不同的模式进来...
				// 所以让我们关闭缓存，因为模式不太可能重复出现。
				deactivatePatternCache();
				return matcher;
			}
			if (cachePatterns == null || cachePatterns.booleanValue()) {
				this.stringMatcherCache.put(pattern, matcher);
			}
		}
		return matcher;
	}

	/**
	 * 给定一个模式和一个完整路径，确定模式映射的部分。
	 * <p>例如：<ul>
	 * <li>'{@code /docs/cvs/commit.html}' 和 '{@code /docs/cvs/commit.html} &rarr; ''</li>
	 * <li>'{@code /docs/*}' 和 '{@code /docs/cvs/commit} &rarr; '{@code cvs/commit}'</li>
	 * <li>'{@code /docs/cvs/*.html}' 和 '{@code /docs/cvs/commit.html} &rarr; '{@code commit.html}'</li>
	 * <li>'{@code /docs/**}' 和 '{@code /docs/cvs/commit} &rarr; '{@code cvs/commit}'</li>
	 * <li>'{@code /docs/**\/*.html}' 和 '{@code /docs/cvs/commit.html} &rarr; '{@code cvs/commit.html}'</li>
	 * <li>'{@code /*.html}' 和 '{@code /docs/cvs/commit.html} &rarr; '{@code docs/cvs/commit.html}'</li>
	 * <li>'{@code *.html}' 和 '{@code /docs/cvs/commit.html} &rarr; '{@code /docs/cvs/commit.html}'</li>
	 * <li>'{@code *}' 和 '{@code /docs/cvs/commit.html} &rarr; '{@code /docs/cvs/commit.html}'</li> </ul>
	 * <p>假设 {@link #match} 对于 '{@code pattern}' 和 '{@code path}' 返回 {@code true}，但
	 * 不强制执行此操作。
	 */
	@Override
	public String extractPathWithinPattern(String pattern, String path) {
		String[] patternParts = StringUtils.tokenizeToStringArray(pattern, this.pathSeparator, this.trimTokens, true);
		String[] pathParts = StringUtils.tokenizeToStringArray(path, this.pathSeparator, this.trimTokens, true);
		StringBuilder builder = new StringBuilder();
		boolean pathStarted = false;

		for (int segment = 0; segment < patternParts.length; segment++) {
			String patternPart = patternParts[segment];
			if (patternPart.indexOf('*') > -1 || patternPart.indexOf('?') > -1) {
				for (; segment < pathParts.length; segment++) {
					if (pathStarted || (segment == 0 && !pattern.startsWith(this.pathSeparator))) {
						builder.append(this.pathSeparator);
					}
					builder.append(pathParts[segment]);
					pathStarted = true;
				}
			}
		}

		return builder.toString();
	}

	/**
	 * 根据给定的模式和路径，提取URI模板变量。 如果模式和路径不匹配，将抛出 IllegalStateException 异常。
	 *
	 * @param pattern 要匹配的模式（永远不为 {@code null}）
	 * @param path    要从中提取变量的路径（永远不为 {@code null}）
	 * @return 一个映射，其中键是变量名，值是从路径中提取的变量值
	 * @throws IllegalStateException 如果模式和路径不匹配
	 */
	@Override
	public Map<String, String> extractUriTemplateVariables(String pattern, String path) {
		Map<String, String> variables = new LinkedHashMap<>();
		boolean result = doMatch(pattern, path, true, variables);
		if (!result) {
			throw new IllegalStateException("模式 \"" + pattern + "\" 与 \"" + path + "\" 不匹配");
		}
		return variables;
	}

	/**
	 * 将两个模式组合成一个新模式。
	 * <p>此实现简单地连接两个模式，除非
	 * 第一个模式包含文件扩展名匹配（例如，{@code *.html}）。 在这种情况下，第二个模式将合并到第一个。否则， 将抛出 {@code IllegalArgumentException} 异常。
	 * <h4>示例</h4>
	 * <table border="1">
	 * <tr><th>模式 1</th><th>模式 2</th><th>结果</th></tr>
	 * <tr><td>{@code null}</td><td>{@code null}</td><td>&nbsp;</td></tr>
	 * <tr><td>/hotels</td><td>{@code null}</td><td>/hotels</td></tr>
	 * <tr><td>{@code null}</td><td>/hotels</td><td>/hotels</td></tr>
	 * <tr><td>/hotels</td><td>/bookings</td><td>/hotels/bookings</td></tr>
	 * <tr><td>/hotels</td><td>bookings</td><td>/hotels/bookings</td></tr>
	 * <tr><td>/hotels/*</td><td>/bookings</td><td>/hotels/bookings</td></tr>
	 * <tr><td>/hotels/&#42;&#42;</td><td>/bookings</td><td>/hotels/&#42;&#42;/bookings</td></tr>
	 * <tr><td>/hotels</td><td>{hotel}</td><td>/hotels/{hotel}</td></tr>
	 * <tr><td>/hotels/*</td><td>{hotel}</td><td>/hotels/{hotel}</td></tr>
	 * <tr><td>/hotels/&#42;&#42;</td><td>{hotel}</td><td>/hotels/&#42;&#42;/{hotel}</td></tr>
	 * <tr><td>/*.html</td><td>/hotels.html</td><td>/hotels.html</td</tr>
	 * <tr><td>/*.html</td><td>/hotels</td><td>/*.html</td></tr>
	 * <tr><td>/*.html</td><td>/*.txt</td><td>/*.html</td></tr>
	 * </table>
	 *
	 * @param pattern1 第一个模式
	 * @param pattern2 第二个模式
	 * @return 组合后的模式
	 * @throws IllegalArgumentException 如果无法组合模式
	 */
	@Override
	public String combine(String pattern1, String pattern2) {
		if (StringUtils.hasText(pattern1) && StringUtils.hasText(pattern2)) {
			throw new IllegalArgumentException("Cannot combine two empty patterns");
		}
		if (StringUtils.hasText(pattern1)) {
			return pattern1;
		}
		if (StringUtils.hasText(pattern2)) {
			return pattern2;
		}
		if (pattern1.contains(pattern2)) {
			return pattern1;
		}

		boolean pattern1ContainsUriVar = (pattern1.indexOf('{') != -1);
		if (!pattern1.equals(pattern2) && !pattern1ContainsUriVar && match(pattern1, pattern2)) {
			// /* + /hotel -> /hotel ; "/*.*" + "/*.html" -> /*.html
			// 但是 /user + /user -> /usr/user ; /{foo} + /bar -> /{foo}/bar
			return pattern2;
		}

		// /hotels/* + /booking -> /hotels/booking
		// /hotels/* + booking -> /hotels/booking
		if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnWildCard())) {
			return concat(pattern1.substring(0, pattern1.length() - 2), pattern2);
		}

		// /hotels/** + /booking -> /hotels/**/booking
		// /hotels/** + booking -> /hotels/**/booking
		if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnDoubleWildCard())) {
			return concat(pattern1, pattern2);
		}

		int starDotPos1 = pattern1.indexOf("*.");
		if (pattern1ContainsUriVar || starDotPos1 == -1 || this.pathSeparator.equals(".")) {
			// 简单地连接两个模式
			return concat(pattern1, pattern2);
		}

		String ext1 = pattern1.substring(starDotPos1 + 1);
		int dotPos2 = pattern2.indexOf('.');
		String file2 = (dotPos2 == -1 ? pattern2 : pattern2.substring(0, dotPos2));
		String ext2 = (dotPos2 == -1 ? "" : pattern2.substring(dotPos2));
		boolean ext1All = (ext1.equals(".*") || ext1.isEmpty());
		boolean ext2All = (ext2.equals(".*") || ext2.isEmpty());
		if (!ext1All && !ext2All) {
			throw new IllegalArgumentException("无法组合模式: " + pattern1 + " 与 " + pattern2);
		}
		String ext = (ext1All ? ext2 : ext1);
		return file2 + ext;
	}

	/**
	 * 连接两个路径。
	 *
	 * @param path1 第一个路径
	 * @param path2 第二个路径
	 * @return 连接后的路径
	 */
	private String concat(String path1, String path2) {
		boolean path1EndsWithSeparator = path1.endsWith(this.pathSeparator);
		boolean path2StartsWithSeparator = path2.startsWith(this.pathSeparator);

		if (path1EndsWithSeparator && path2StartsWithSeparator) {
			return path1 + path2.substring(1);
		} else if (path1EndsWithSeparator || path2StartsWithSeparator) {
			return path1 + path2;
		} else {
			return path1 + this.pathSeparator + path2;
		}
	}

	/**
	 * 根据完整路径，返回一个适合按明确性排序模式的{@link Comparator}。
	 * <p>此{@code Comparator}将{@linkplain java.util.List#sort(Comparator) sort}
	 * 一个列表，以便更具体的模式（没有URI模板或通配符）在通用模式之前。 因此，给定一个具有以下模式的列表，返回的比较器将对此列表进行排序，以便按照指示的顺序。
	 * <ol>
	 * <li>{@code /hotels/new}</li>
	 * <li>{@code /hotels/{hotel}}</li>
	 * <li>{@code /hotels/*}</li>
	 * </ol>
	 * <p>作为参数给出的完整路径用于测试精确匹配。因此，当给定路径为{@code /hotels/2}时，
	 * 模式{@code /hotels/2}将在{@code /hotels/1}之前排序。
	 *
	 * @param path 用于比较的完整路径
	 * @return 能够按明确性顺序排序模式的比较器
	 */
	@Override
	public Comparator<String> getPatternComparator(String path) {
		return new AntPatternComparator(path);
	}


	/**
	 * 通过{@link Pattern}测试字符串是否与模式匹配。
	 * <p>模式可能包含特殊字符：'*'表示零个或多个字符；'?'表示一个且只有一个字符；
	 * '{'和'}'表示URI模板模式。例如{@code /users/{user}}。
	 */
	protected static class AntPathStringMatcher {

		private static final Pattern GLOB_PATTERN = Pattern.compile(
				"\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");

		private static final String DEFAULT_VARIABLE_PATTERN = "((?s).*)";

		private final String rawPattern;

		private final boolean caseSensitive;

		private final boolean exactMatch;

		@Nullable
		private final Pattern pattern;

		private final List<String> variableNames = new ArrayList<>();

		/**
		 * 使用给定的模式构造一个新的{@code AntPathStringMatcher}。
		 *
		 * @param pattern 要匹配的模式
		 */
		public AntPathStringMatcher(String pattern) {
			this(pattern, true);
		}

		/**
		 * 使用给定的模式和大小写敏感性构造一个新的{@code AntPathStringMatcher}。
		 *
		 * @param pattern       要匹配的模式
		 * @param caseSensitive 是否区分大小写
		 */
		public AntPathStringMatcher(String pattern, boolean caseSensitive) {
			this.rawPattern = pattern;
			this.caseSensitive = caseSensitive;
			StringBuilder patternBuilder = new StringBuilder();
			Matcher matcher = GLOB_PATTERN.matcher(pattern);
			int end = 0;
			while (matcher.find()) {
				patternBuilder.append(quote(pattern, end, matcher.start()));
				String match = matcher.group();
				if ("?".equals(match)) {
					patternBuilder.append('.');
				} else if ("*".equals(match)) {
					patternBuilder.append(".*");
				} else if (match.startsWith("{") && match.endsWith("}")) {
					int colonIdx = match.indexOf(':');
					if (colonIdx == -1) {
						patternBuilder.append(DEFAULT_VARIABLE_PATTERN);
						this.variableNames.add(matcher.group(1));
					} else {
						String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
						patternBuilder.append('(');
						patternBuilder.append(variablePattern);
						patternBuilder.append(')');
						String variableName = match.substring(1, colonIdx);
						this.variableNames.add(variableName);
					}
				}
				end = matcher.end();
			}
			// 如果没有找到glob模式，这是一个精确的字符串匹配
			if (end == 0) {
				this.exactMatch = true;
				this.pattern = null;
			} else {
				this.exactMatch = false;
				patternBuilder.append(quote(pattern, end, pattern.length()));
				this.pattern = Pattern.compile(patternBuilder.toString(),
						Pattern.DOTALL | (this.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE));
			}
		}

		/**
		 * 对给定的字符串进行引用。
		 *
		 * @param s     要引用的字符串
		 * @param start 开始引用的位置
		 * @param end   结束引用的位置
		 * @return 引用后的字符串
		 */
		private String quote(String s, int start, int end) {
			if (start == end) {
				return "";
			}
			return Pattern.quote(s.substring(start, end));
		}

		/**
		 * 主要入口点。
		 *
		 * @return 如果字符串与模式匹配，则返回{@code true}，否则返回{@code false}。
		 */
		public boolean matchStrings(String str, @Nullable Map<String, String> uriTemplateVariables) {
			if (this.exactMatch) {
				// 如果区分大小写，则使用equals，否则使用equalsIgnoreCase
				return this.caseSensitive ? this.rawPattern.equals(str) : this.rawPattern.equalsIgnoreCase(str);
			} else if (this.pattern != null) {
				Matcher matcher = this.pattern.matcher(str);
				if (matcher.matches()) {
					if (uriTemplateVariables != null) {
						if (this.variableNames.size() != matcher.groupCount()) {
							throw new IllegalArgumentException("模式段中的捕获组数量 " +
									this.pattern + " 与其定义的URI模板变量数量不匹配，" +
									"这可能是因为在URI模板正则表达式中使用了捕获组。" +
									"请改用非捕获组。");
						}
						for (int i = 1; i <= matcher.groupCount(); i++) {
							String name = this.variableNames.get(i - 1);
							if (name.startsWith("*")) {
								throw new IllegalArgumentException("AntPathMatcher不支持捕获模式（" + name + "）。" +
										"请改用PathPatternParser。");
							}
							String value = matcher.group(i);
							uriTemplateVariables.put(name, value);
						}
					}
					return true;
				}
			}
			return false;
		}

	}


	/**
	 * 默认的{@link Comparator}实现，由 {@link #getPatternComparator(String)}返回。
	 * <p>按顺序，最“通用”的模式由以下因素确定：
	 * <ul>
	 * <li>如果它是null或捕获所有模式（即等于"/**"）</li>
	 * <li>如果另一个模式是实际匹配</li>
	 * <li>如果它是一个捕获所有模式（即以"**"结尾）</li>
	 * <li>如果它的"*"比另一个模式多</li>
	 * <li>如果它的"{foo}"比另一个模式多</li>
	 * <li>如果它比另一个模式短</li>
	 * </ul>
	 */
	protected static class AntPatternComparator implements Comparator<String> {

		private final String path;

		public AntPatternComparator(String path) {
			this.path = path;
		}

		/**
		 * 比较两个模式以确定哪个应该先匹配，即哪个 对于当前路径来说是最具体的。
		 *
		 * @return 一个负整数、零或正整数，因为pattern1比 pattern2更具体、同样具体或不太具体。
		 */
		@Override
		public int compare(String pattern1, String pattern2) {
			PatternInfo info1 = new PatternInfo(pattern1);
			PatternInfo info2 = new PatternInfo(pattern2);

			if (info1.isLeastSpecific() && info2.isLeastSpecific()) {
				return 0;
			} else if (info1.isLeastSpecific()) {
				return 1;
			} else if (info2.isLeastSpecific()) {
				return -1;
			}

			boolean pattern1EqualsPath = pattern1.equals(this.path);
			boolean pattern2EqualsPath = pattern2.equals(this.path);
			if (pattern1EqualsPath && pattern2EqualsPath) {
				return 0;
			} else if (pattern1EqualsPath) {
				return -1;
			} else if (pattern2EqualsPath) {
				return 1;
			}

			if (info1.isPrefixPattern() && info2.isPrefixPattern()) {
				return info2.getLength() - info1.getLength();
			} else if (info1.isPrefixPattern() && info2.getDoubleWildcards() == 0) {
				return 1;
			} else if (info2.isPrefixPattern() && info1.getDoubleWildcards() == 0) {
				return -1;
			}

			if (info1.getTotalCount() != info2.getTotalCount()) {
				return info1.getTotalCount() - info2.getTotalCount();
			}

			if (info1.getLength() != info2.getLength()) {
				return info2.getLength() - info1.getLength();
			}

			if (info1.getSingleWildcards() < info2.getSingleWildcards()) {
				return -1;
			} else if (info2.getSingleWildcards() < info1.getSingleWildcards()) {
				return 1;
			}

			if (info1.getUriVars() < info2.getUriVars()) {
				return -1;
			} else if (info2.getUriVars() < info1.getUriVars()) {
				return 1;
			}

			return 0;
		}


	/**
	 * 一个值类，用于保存有关模式的信息，例如 "*"、"**" 和 "{" 模式元素的出现次数。
	 */
	private static class PatternInfo {

		@Nullable
		private final String pattern;

		private int uriVars;

		private int singleWildcards;

		private int doubleWildcards;

		private boolean catchAllPattern;

		private boolean prefixPattern;

		@Nullable
		private Integer length;

		/**
		 * 使用给定的模式构造一个新的 PatternInfo。
		 *
		 * @param pattern 要匹配的模式
		 */
		public PatternInfo(@Nullable String pattern) {
			this.pattern = pattern;
			if (this.pattern != null) {
				initCounters();
				this.catchAllPattern = this.pattern.equals("/**");
				this.prefixPattern = !this.catchAllPattern && this.pattern.endsWith("/**");
			}
			if (this.uriVars == 0) {
				this.length = (this.pattern != null ? this.pattern.length() : 0);
			}
		}

		/**
		 * 初始化计数器。
		 */
		protected void initCounters() {
			int pos = 0;
			if (this.pattern != null) {
				while (pos < this.pattern.length()) {
					if (this.pattern.charAt(pos) == '{') {
						this.uriVars++;
						pos++;
					} else if (this.pattern.charAt(pos) == '*') {
						if (pos + 1 < this.pattern.length() && this.pattern.charAt(pos + 1) == '*') {
							this.doubleWildcards++;
							pos += 2;
						} else if (pos > 0 && !this.pattern.substring(pos - 1).equals(".*")) {
							this.singleWildcards++;
							pos++;
						} else {
							pos++;
						}
					} else {
						pos++;
					}
				}
			}
		}

		/**
		 * 获取 URI 变量的数量。
		 *
		 * @return URI 变量的数量
		 */
		public int getUriVars() {
			return this.uriVars;
		}

		/**
		 * 获取单个通配符的数量。
		 *
		 * @return 单个通配符的数量
		 */
		public int getSingleWildcards() {
			return this.singleWildcards;
		}

		/**
		 * 获取双通配符的数量。
		 *
		 * @return 双通配符的数量
		 */
		public int getDoubleWildcards() {
			return this.doubleWildcards;
		}

		/**
		 * 判断是否为最不具体的模式。
		 *
		 * @return 如果模式为 null 或捕获所有模式，则返回 true；否则返回 false。
		 */
		public boolean isLeastSpecific() {
			return (this.pattern == null || this.catchAllPattern);
		}

		/**
		 * 判断是否为前缀模式。
		 *
		 * @return 如果为前缀模式，则返回 true；否则返回 false。
		 */
		public boolean isPrefixPattern() {
			return this.prefixPattern;
		}

		/**
		 * 获取模式的总计数。
		 *
		 * @return 模式的总计数
		 */
		public int getTotalCount() {
			return this.uriVars + this.singleWildcards + (2 * this.doubleWildcards);
		}

		/**
		 * 返回给定模式的长度，其中模板变量被认为是 1 长。
		 *
		 * @return 模式的长度
		 */
		public int getLength() {
			if (this.length == null) {
				this.length = (this.pattern != null ?
						VARIABLE_PATTERN.matcher(this.pattern).replaceAll("#").length() : 0);
			}
			return this.length;
		}
	}
}

/**
 * 一个简单的缓存，用于存储依赖于配置的路径分隔符的模式。
 */
private static class PathSeparatorPatternCache {

	private final String endsOnWildCard;

	private final String endsOnDoubleWildCard;

	/**
	 * 使用给定的路径分隔符构造一个新的 PathSeparatorPatternCache。
	 *
	 * @param pathSeparator 路径分隔符
	 */
	public PathSeparatorPatternCache(String pathSeparator) {
		this.endsOnWildCard = pathSeparator + "*";
		this.endsOnDoubleWildCard = pathSeparator + "**";
	}

	/**
	 * 获取以通配符结尾的模式。
	 *
	 * @return 以通配符结尾的模式
	 */
	public String getEndsOnWildCard() {
		return this.endsOnWildCard;
	}

	/**
	 * 获取以双通配符结尾的模式。
	 *
	 * @return 以双通配符结尾的模式
	 */
	public String getEndsOnDoubleWildCard() {
		return this.endsOnDoubleWildCard;
	}
}

}
