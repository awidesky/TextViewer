/*
 * Copyright (c) 2023 Eugene Hong
 *
 * This software is distributed under license. Use of this software
 * implies agreement with all terms and conditions of the accompanying
 * software license.
 * Please refer to LICENSE
 * */

package io.github.awidesky.textViewer.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.textViewer.LineSeparator;
import io.github.awidesky.textViewer.gui.MetadataGenerator;

public class TextFile {

	public final File file;
	public final Charset encoding;
	public final LineSeparator lineSep;
	public final boolean isEncrypted;
	
	public TextFile(File file, Charset encoding, LineSeparator lineSep, boolean isEncrypted) {
		this.file = file;
		this.encoding = encoding;
		this.lineSep = lineSep == LineSeparator.DETECT ? detectLineSepator() : lineSep;
		this.isEncrypted = isEncrypted;
	}
	
	@Override
	public String toString() {
		return file.getAbsolutePath() + " as " + encoding.displayName() + ", line separator : " + lineSep.getExplain();
	}

	
	public LineSeparator detectLineSepator() {
		
		try (InputStreamReader ir = new InputStreamReader(new FileInputStream(file), encoding)) {
			while (true) {
				char arr[] = new char[4 * 1024];
				int read = 0;
				while (read != arr.length)
					read += ir.read(arr, read, arr.length - read);

				// trim 3 chars from both side to properly handle multi-charactor line
				// separator(like windows CRLF)
				Matcher m = Pattern.compile("\\R").matcher(String.valueOf(arr, 3, arr.length - 6));
				if (m.find()) {
					LineSeparator ls = LineSeparator.getFromStr(m.group());
					MetadataGenerator.lineSeparator(ls);
					return ls;
				}
			}
		} catch (IOException e) {
			SwingDialogs.error("Cannot detect Line Separator!", "Unable to detect Line Separator!\nUse system default instead..", null, false);
		}
		return LineSeparator.getDefault();
	}

}
