/*
 * Copyright (c) 2023 Eugene Hong
 *
 * This software is distributed under license. Use of this software
 * implies agreement with all terms and conditions of the accompanying
 * software license.
 * Please refer to LICENSE
 * */

package io.github.awidesky.textViewer.io;

import java.io.IOException;

import io.github.awidesky.jCipherUtil.cipher.symmetric.aes.AESKeySize;
import io.github.awidesky.jCipherUtil.cipher.symmetric.aes.AES_GCMCipherUtil;
import io.github.awidesky.jCipherUtil.messageInterface.MessageConsumer;
import io.github.awidesky.jCipherUtil.util.UpdatableEncrypter;

public class EncryptedTextWriter extends TextWriter {

	private UpdatableEncrypter cipher = null;
	
	EncryptedTextWriter(TextFile writeTo) throws IOException {
		writeFile = writeTo;
		cipher = new AES_GCMCipherUtil.Builder(writeTo.getPassword(), AESKeySize.SIZE_256).build().UpdatableEncryptCipher(MessageConsumer.to(writeTo.file));
	}
	
	@Override
	public void writeString(String str) throws IOException {
		cipher.update(replaceNewLine(str, writeFile.lineSep.getStr()).getBytes(writeFile.encoding));
	}

	@Override
	public void close() throws IOException {
		cipher.doFinal(null);
		writeFile.clearPassword();
	}

}
