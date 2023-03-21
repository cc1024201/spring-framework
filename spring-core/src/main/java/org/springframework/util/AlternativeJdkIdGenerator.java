/*
 * 版权 2002-2015 原作者或作者。
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
 * 有关许可证下允许和禁止的特定语言，请参阅许可证。
 */

package org.springframework.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

/**
 * 一个 {@link IdGenerator}，它使用 {@link SecureRandom} 作为初始种子，
 * 然后使用 {@link Random}，而不是像 {@link org.springframework.util.JdkIdGenerator JdkIdGenerator} 那样每次调用 {@link UUID#randomUUID()}。
 * 这样可以在安全随机 id 和性能之间取得更好的平衡。
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 4.0
 */
public class AlternativeJdkIdGenerator implements IdGenerator {

	// 用于生成随机数的 Random 对象
	private final Random random;

	/**
	 * 构造函数，使用 SecureRandom 生成初始种子，然后创建一个 Random 对象。
	 */
	public AlternativeJdkIdGenerator() {
		SecureRandom secureRandom = new SecureRandom();
		byte[] seed = new byte[8];
		secureRandom.nextBytes(seed);
		this.random = new Random(new BigInteger(seed).longValue());
	}

	/**

	 * 生成一个 UUID。
	 * 使用当前的 Random 对象生成 16 个随机字节，然后将这些字节转换为两个 long 值，最后创建一个新的 UUID。
	 *
	 * @return 生成的 UUID
	 */
	@Override
	public UUID generateId() {
		byte[] randomBytes = new byte[16];
		this.random.nextBytes(randomBytes);

		long mostSigBits = 0;
		for (int i = 0; i < 8; i++) {
			mostSigBits = (mostSigBits << 8) | (randomBytes[i] & 0xff);
		}

		long leastSigBits = 0;
		for (int i = 8; i < 16; i++) {
			leastSigBits = (leastSigBits << 8) | (randomBytes[i] & 0xff);
		}

		return new UUID(mostSigBits, leastSigBits);
	}

}
