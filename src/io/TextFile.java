package io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gui.MetadataGenerator;
import gui.SwingDialogs;
import main.LineSeparator;

public class TextFile {

	public final File file;
	public final Charset encoding;
	public final LineSeparator lineSep;
	
	public TextFile(File file, Charset encoding, LineSeparator lineSep) {
		this.file = file;
		this.encoding = encoding;
		this.lineSep = lineSep == LineSeparator.DETECT ? detectLineSepator() : lineSep;
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
