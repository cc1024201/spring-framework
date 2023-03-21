/*
 * 版权 2002-2023 原作者或作者。
 *
 * 根据 Apache 许可证，版本 2.0（“许可证”）获得许可；
 * 除非符合许可证，否则不得使用此文件。
 * 您可以在以下位置获取许可证副本：
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * 除非适用法律要求或书面同意，否则在许可证下分发的软件
 * 将按“原样”基础提供，不附带任何明示或暗示的保证或条件。
 * 有关许可证中规定的权限和限制的详细信息，请参阅许可证。
 */

package org.springframework.util;

import java.util.Base64;

/**
 * 一个简单的 Base64 编码和解码的实用工具类。
 *
 * <p>以便捷方式适应 Java 8 的 {@link java.util.Base64}。
 *
 * @author Juergen Hoeller
 * @author Gary Russell
 * @since 4.1
 * @see java.util.Base64
 * @deprecated 自 Spring Framework 6.0.5 起，建议使用 {@link Base64}。
 */
@Deprecated(since = "6.0.5", forRemoval = true)
public abstract class Base64Utils {

	/**
	 * 对给定的字节数组进行 Base64 编码。
	 * @param src 原始字节数组
	 * @return 编码后的字节数组
	 */
	public static byte[] encode(byte[] src) {
		if (src.length == 0) {
			return src;
		}
		return Base64.getEncoder().encode(src);
	}

	/**
	 * 对给定的字节数组进行 Base64 解码。
	 * @param src 编码后的字节数组
	 * @return原始字节数组
	 */
	public static byte[] decode(byte[] src) {
		if (src.length == 0) {
			return src;
		}
		return Base64.getDecoder().decode(src);
	}

	/**
	 * 使用 RFC 4648 中的
	 * “URL 和文件名安全字母表”对给定的字节数组进行 Base64 编码。
	 * @param src 原始字节数组
	 * @return 编码后的字节数组
	 * @since 4.2.4
	 */
	public static byte[] encodeUrlSafe(byte[] src) {
		if (src.length == 0) {
			return src;
		}
		return Base64.getUrlEncoder().encode(src);
	}

	/**
	 * 使用 RFC 4648 中的
	 * “URL 和文件名安全字母表”对给定的字节数组进行 Base64 解码。
	 * @param src 编码后的字节数组
	 * @return 原始字节数组
	 * @since 4.2.4
	 */
	public static byte[] decodeUrlSafe(byte[] src) {
		if (src.length == 0) {
			return src;
		}
		return Base64.getUrlDecoder().decode(src);
	}

	/**
	 * 将给定的字节数组 Base64 编码为字符串。
	 * @param src 原始字节数组
	 * @return 编码后的字节数组作为 UTF-8 字符串
	 */
	public static String encodeToString(byte[] src) {
		if (src.length == 0) {
			return "";
		}
		return Base64.getEncoder().encodeToString(src);
	}

	/**
	 * 从 UTF-8 字符串中对给定的字节数组进行 Base64 解码。
	 * @param src 编码后的 UTF-8 字符串
	 * @return 原始字节数组
	 */
	public static byte[] decodeFromString(String src) {
		if (src.isEmpty()) {
			return new byte[0];
		}
		return Base64.getDecoder().decode(src);
	}

	/**
	 * 使用 RFC 4648 中的
	 * “URL 和文件名安全字母表”将给定的字节数组 Base64 编码为字符串。
	 * @param src 原始字节数组
	 * @return 编码后的字节数组作为 UTF-8 字符串
	 */
	public static String encodeToUrlSafeString(byte[] src) {
		return Base64.getUrlEncoder().encodeToString(src);
	}

	/**
	 * 使用 RFC 4648 中的
	 * “URL 和文件名安全字母表”从 UTF-8 字符串中对给定的字节数组进行 Base64 解码。
	 * @param src 编码后的 UTF-8 字符串
	 * @return 原始字节数组
	 */
	public static byte[] decodeFromUrlSafeString(String src) {
		return Base64.getUrlDecoder().decode(src);
	}

}
