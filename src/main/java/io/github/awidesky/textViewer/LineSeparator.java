/*
 * Copyright (c) 2023 Eugene Hong
 *
 * This software is distributed under license. Use of this software
 * implies agreement with all terms and conditions of the accompanying
 * software license.
 * Please refer to LICENSE
 * */

package io.github.awidesky.textViewer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.github.awidesky.textViewer.util.SwingDialogs;

public enum LineSeparator {
	
	DETECT(null, "", "Detect from file"),
	CR(String.valueOf(Character.toChars(0x000D)), "CR", "Carriage Return"),
	LF(String.valueOf(Character.toChars(0x000A)), "LF", "Line Feed"),
	CRLF("\r\n", "CRLF", "Windows CRLF"),
	VT(String.valueOf(Character.toChars(0x000B)), "VT", "Vertical Tabulation"),
	FF(String.valueOf(Character.toChars(0x000C)), "FF", "Form Feed"),
	NEL(String.valueOf(Character.toChars(0x0085)), "NEL", "Next Line"),
	LS(String.valueOf(Character.toChars(0x2028)), "LS", "Line Separator"),
	PS(String.valueOf(Character.toChars(0x2029)), "PS", "Paragraph Separator");
	
	private String str;
	private String abbreviation;
	private String explain;
	
	private LineSeparator(String str, String abbreviation, String explain) {
		this.str = str;
		this.abbreviation = abbreviation;
		this.explain = explain;
	}

	public String getStr() { return str; }
	public String getAbbreviation() { return abbreviation; }
	public String getExplain() { return (System.lineSeparator().equals(str) ? "*" : "") + abbreviation + " (" + explain + ")"; }

	public static LineSeparator getFromExplain(String explain) {
		for(LineSeparator l : values()) {
			if(explain.equals(l.getExplain())) return l;
		}
		SwingDialogs.error("Invalid Line Separator!", "Invalid Line Separator : " + explain, null, false);
		return getDefault();
	}
	public static LineSeparator getFromStr(String lineSepStr) {
		for(LineSeparator l : values()) {
			if(lineSepStr.equals(l.getStr())) return l;
		}
		SwingDialogs.error("Invalid Line Separator!", "Invalid Line Separator : \"" +
				getUnicodePoints(lineSepStr) + "\"\n\n" +
				getUnicodeExplane(lineSepStr, "\n") , null, false);
		return getDefault();
	}
	public static LineSeparator getDefault() {
		for(LineSeparator l : values()) {
			if(System.lineSeparator().equals(l.str)) return l;
		}
		SwingDialogs.error("Unknown system default Line Separator!", "Unknown system default Line Separator : \"" +
				getUnicodePoints(System.lineSeparator()) + "\"\n\n" +
				getUnicodeExplane(System.lineSeparator(), "\n"), null, false);
		return DETECT;
	}
	
	
	private static String getUnicodeExplane(String s, String delimeter) {
		List<String> l = new ArrayList<>(s.length());
		char[] arr = s.toCharArray();
		for(int i = 0; i < arr.length; i++) {
			l.add(String.format("U+%04x", Character.codePointAt(arr, i)).toUpperCase() + " : " + Character.getName(arr[i]));
		}
		return l.stream().collect(Collectors.joining(delimeter));
	}
	private static String getUnicodePoints(String s) {
		StringBuilder sb = new StringBuilder();
		char[] arr = s.toCharArray();
		for(int i = 0; i < arr.length; i++) {
			sb.append(String.format("U+%04x", Character.codePointAt(arr, i)).toUpperCase());
		}
		return sb.toString();
	}
	
}
