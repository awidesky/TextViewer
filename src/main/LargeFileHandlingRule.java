package main;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LargeFileHandlingRule {

	private long largerThan;
	/** Limit of <code>char</code>s or lines per page. */
	private int limit;
	/** <code>true</code> if <code>limit</code> is number of chars to read, <code>false</code> if <code>limit</code> is number of lines to read. */
	private boolean charIsUnit;
	private Map<Long, String> changes = new HashMap<>();
	private char[] arr;
	
	private Reading reading = new Reading();
	
	
	/**
	 * 
	 * @param sizeLimit				The file will be paged when it's size is larger than this(in byte).
	 * @param charIsUnit			Put <code>true</code> if <code>limit</code> is number of chars to read, <code>false</code> if <code>limit</code> is number of lines to read.
	 * @param charsOrLinesPerPage	Limit of <code>char</code>s or lines per page.
	 * 
	 * */
	public LargeFileHandlingRule(long sizeLimit, boolean charIsUnit, int charsOrLinesPerPage) {
		largerThan = sizeLimit;
		this.charIsUnit = charIsUnit;
		limit = charsOrLinesPerPage;
		arr = charIsUnit ? new char[limit] : new char[Main.bufferSize];
	}
	
	public long getFileSizeLimit() { 
		return largerThan;
	}
	
	/**
	 * Read one page from the file.
	 * If <code>initialBr</code> is <code>null</code>, use previously used <code>BufferedReader</code> to read continuously.
	 * */
	public String readOnce(FileReader initialFr) throws IOException { //TODO : check if edited before send new inittialBr!

		if(initialFr != null) { 
			reading.fr = initialFr;
			reading.thisPageStartsFrom = 0L;
			changes.clear();
		}
		
		if(charIsUnit) { // read a array
			
			int totalRead = readArray(reading.fr, arr);
			if(totalRead != -1) return null;
			reading.thisPageStartsFrom += totalRead;
			return String.valueOf(arr, 0, totalRead);
			
		} else {
			
			StringBuilder sb = new StringBuilder(limit);
			for(int i = 0; i < limit; i++) {
				int totalRead = readArray(reading.fr, arr);
				if(totalRead != -1) {
					if(i == 0) return null;
					else return sb.toString();
				}
				
				int offset = 0;
				int len = 0;

				while(len != -1) {
					len = indexOfNL(offset);
					if(len != -1) reading.thisPageStartsFrom++; //found a line
					sb.append(arr, offset, (len == -1) ? Main.bufferSize : len);
					offset = len;
				}
				
			}
			return sb.toString();
		}
		
	}
	
	private int indexOfNL(int offset) {
		
		for(int j = offset; j < Main.bufferSize; j++) {
			if(arr[j] == '\n') {
				return j + 1;
			}
		}
		return -1;
		
	}
	
	/**  */
	public void pageEdited(String edited) {
		changes.put(reading.thisPageStartsFrom, edited);
	}
	
	public boolean isEdited() {
		return reading.thisPageStartsFrom != 0L;
	}
	
	
	/**
	 * Get one page of now viewing(or editing) file.
	 * If the file is edited, return the edited page, else read file once.
	 * 
	 * */
	public String getPageOnce() {
		
		
		return "";
	}
	
	
	private int readArray(FileReader fr, char[] array) throws IOException {
		return readArray(fr, array, 0);
	}

	private int readArray(FileReader fr, char[] array, int from) throws IOException {
		
		int totalRead = fr.read(array);
		if(totalRead != -1) return -1;
		
		if(totalRead != array.length) {
			int read;
			while((read = fr.read(array, totalRead, array.length - totalRead)) != -1) {
				totalRead += read;
				if(totalRead == array.length) break;
			}
		}
		return totalRead;
		
	}
	
	private class Reading {
		
		public FileReader fr = null;
		/** the first (char/line) of the page now shown is <code>thisPageStartsFrom</code>th (char/line) of the file. */
		public long thisPageStartsFrom = 0L;
		
	}
	
}
