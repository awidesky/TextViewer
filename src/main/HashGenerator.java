package main;

import java.security.MessageDigest;
import java.util.zip.Checksum;

public abstract class HashGenerator {

	private HashGenerator() {}
	
	public abstract String getHash(String src);
	
	
	public static HashGenerator getCryptoHashInstance(MessageDigest md) {
		return new HashGenerator() {
			@Override
			public String getHash(String src) {
				StringBuilder hashtext = new StringBuilder();
				md.reset();
				for (byte b : md.digest(src.getBytes())) {
					hashtext.append(Integer.toHexString(Byte.toUnsignedInt(b)));
				}
				md.reset();
				return hashtext.toString();
			}
		};
	}
	
	public static HashGenerator getChecksumHashInstance(Checksum cs) {
		return new HashGenerator() {
			@Override
			public String getHash(String src) {
				cs.reset();
				cs.update(src.getBytes());
				String ret = Long.toHexString(cs.getValue());
				cs.reset();
				return ret;
			}
		};
	}
	
	public static HashGenerator getRawCompareInstance() {
		return new HashGenerator() {
			@Override
			public String getHash(String src) {
				return src;
			}
		};
	}
	
}
