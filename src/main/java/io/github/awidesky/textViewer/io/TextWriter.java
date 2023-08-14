/*
 * Copyright (c) 2023 Eugene Hong
 *
 * This software is distributed under license. Use of this software
 * implies agreement with all terms and conditions of the accompanying
 * software license.
 * Please refer to LICENSE
 * */

package io.github.awidesky.textViewer.io;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import io.github.awidesky.jCipherUtil.cipher.symmetric.aes.AES_GCMCipherUtil;

public class TextWriter implements AutoCloseable {

	public static final int BUFSIZE = 32 * 1024;
	
	/** A file to write at */
	private TextFile writeFile;
	/** Directly attached to the destination file */
	private BufferedOutputStream out;
	
	/** For encoding String to bytes */
	private OutputStreamWriter strEncoder;
	private ByteArrayOutputStream bufferStream;
	private AES_GCMCipherUtil cipher = null;
	
	private byte[] buf;
	
	public TextWriter(TextFile writeTo) throws IOException {
		writeFile = writeTo;
		out = new BufferedOutputStream(new FileOutputStream(writeTo.file));
		bufferStream = new ByteArrayOutputStream(BUFSIZE);
		strEncoder = new OutputStreamWriter(bufferStream, writeTo.encoding);
		//if(writeFile.isEncrypted) cipher = new AES_GCMCipherUtil.Builder(buf, null);
	}
	
	public void writePage(Page p) throws IOException {
		writeString(p.text);
	}
	public void writeString(String str) throws IOException {
		str = replaceNewLine(str);
		if (writeFile.isEncrypted) {
			
		} else {
			
		}
	}
	
	private String replaceNewLine(String str) { return str.replaceAll("\\R", writeFile.lineSep.getStr()); }
	
	@Override
	public void close() throws Exception {
		if(out != null) out.close();
	}

}
