package io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import main.Main;
import main.SettingData;

public class TextReader implements AutoCloseable{

	private TextFile readFile;
	private InputStreamReader ir;
	private StringBuilder leftOver = new StringBuilder();
	private SettingData setting;
	private long nextPageNum = 1L;
	private String taskID = null;
	
	private char[] arr;
	
	public TextReader(SettingData setting, TextFile readFile, String taskID) throws IOException {
		
		this.setting = setting;
		this.readFile = readFile;
		this.arr = new char[setting.getCharBufSize()];
		this.taskID = taskID;
		if (setting.getPageEndsWithNewline()) { leftOver = new StringBuilder(""); }
		this.ir = new InputStreamReader(new FileInputStream(readFile.file), readFile.encoding);
		Main.logger.log("");
	}
	
	
	public long getNextPageNum() { return nextPageNum; }
	
	private static String replaceNewLine(String str) { return str.replaceAll(Main.setting.getLineSeparator().getStr(), "\n"); }
	
	public Page readAll() throws IOException {
		Main.logger.log(taskID + "start reading the file until reach EOF");
		StringBuilder sb = new StringBuilder((int) readFile.file.length());
		int read = 0;
		while (true) {
			if ((read = readArray()) == -1) break;
			sb.append(arr, 0, read);
		}
		Main.logger.log(taskID + "Reached EOF");
		return new Page(replaceNewLine(sb.toString()), -1, true);
	}
	

	/**
	 * Read a <code>Page</code>.
	 * @return <code>Page.EOF</code> when EOF reached
	 * @throws IOException 
	 * */
	public Page readOnePage() throws IOException {

		int totalRead = 0;
		boolean isLastPage = false;
		boolean lastNewlineRemoved = false;
		
		StringBuilder strBuf = new StringBuilder("");
		String result = null;
		
		int nextRead = Math.min(arr.length, setting.getCharPerPage());
		while (true) {
			int read = readArray(nextRead);
			if (read == -1) { // no more chars to read
				if(totalRead == 0 && leftOver.length() == 0) {
					Main.logger.log("Reached EOF in first attempt reading a new Page, No more page to read!");
					//Does not close this TextReader until close() is explicitly called.
					//Just like InputStream behaves.
					return Page.EOF; 
				} else {
					isLastPage = true;
					break;
				}
			}
			strBuf.append(arr, 0, read);
			totalRead += read;
			if(totalRead >= setting.getCharPerPage()) break;
			
			nextRead = Math.min(arr.length, setting.getCharPerPage() - totalRead);
		}
		
		String res = strBuf.toString();
		
		/**
		 * <code>lastLittlePortion</code> Exist only because of following case may exist.
		 * in Windows, Line break(\R) is \r\n. if TextReader only reads \r and buffer got full, following \n(which should be considered as a pair with preceding \r)
		 * will considered as separate line break in next page reading.
		 * to avoid that, I used a punt : trim off last 3~5 chars, since it contains unfinished line break.
		 * unless I find a line break sequence longer than 3~4 characters, 3 will (hopefully) work fine.   
		 * */
		final int LASTLITTLEPORTIONMARGIN = 3;
		int lastLittlePortionStartsAt = TextReader.lastLineBreak(res) - LASTLITTLEPORTIONMARGIN;
		String lastLittlePortion = "";
		if(lastLittlePortionStartsAt > 0) { 
			lastLittlePortion = res.substring(lastLittlePortionStartsAt);
		} else {
			lastLittlePortionStartsAt = res.length();
		}
		res = replaceNewLine(res.substring(0, lastLittlePortionStartsAt)); //Replace \R so that we can easily find newline
		
		if (setting.getPageEndsWithNewline()) {
			int lastLineFeedIndex = res.lastIndexOf("\n"); 

			if (lastLineFeedIndex != -1) {
				result = leftOver.append(res.substring(0, lastLineFeedIndex)).toString().replaceAll("\\R", "\n");// system-dependent \\R might be exist in leftOver because lastLittlePortion
				leftOver = new StringBuilder();
				leftOver.append(res.substring(lastLineFeedIndex + "\n".length())).append(lastLittlePortion); // remove trailing \n so that next page won't be starting as \n
				lastNewlineRemoved = true;
			} else {
				result = leftOver.append(res).toString();
				leftOver = new StringBuilder();
			}
		} else {
			result = res;
		}

		return new Page(result, nextPageNum++, isLastPage, lastNewlineRemoved);
	}
	
	

	private static int lastLineBreak(String res) {
		String lineSep = Main.setting.getLineSeparator().getStr();
		int lineSepLen = lineSep.length();
		for(int i = res.length() - lineSepLen; i > -1; i -= lineSepLen) {
			if (res.substring(i, i + lineSepLen).equals(lineSep)) return i;
		}
		return res.length();
	}


	/** Fills the array by reading <code>ir</code>
	 *  This method makes sure that <code>array</code> is fully filled unless EOF is read during the reading. 
	 *  @throws IOException 
	 * */
	private int readArray() throws IOException {
		return readArray(arr.length);
	}

	/** Fills the array by reading <code>ir</code>
	 *  This method makes sure that <code>array</code> is fully filled unless EOF is read during the reading.
	 *  
	 *  @param len amount of chars to read
	 *  
	 *  @return How many char(s) read<p>-1 when EOF reached<p>-2 when Exception occured
	 *  @throws IOException 
	 *  
	 * */
	private int readArray(int len) throws IOException {

		Main.logger.logVerbose(taskID + "Try reading " + len + " char(s)...");
		int totalRead = ir.read(arr, 0, len);
		Main.logger.logVerbose(taskID + "Read " + totalRead + " char(s)");
		if (totalRead == -1) {
			Main.logger.logVerbose(taskID + "File pointer position was at EOF");
			return -1;
		}

		if (totalRead != len) {
			Main.logger.logVerbose(taskID + "Buffer not full, try reading more...");
			int read;
			while ((read = ir.read(arr, totalRead, len - totalRead)) != -1) {
				Main.logger.logVerbose(taskID + "Read " + read + " char(s), total : " + totalRead);
				totalRead += read;
				if (totalRead == len) {
					Main.logger.logVerbose(taskID + "Buffer is full!");
					break;
				}
			}
			if (read == -1)
				Main.logger.logVerbose(taskID + "EOF reached!");
		}

		Main.logger.logVerbose(taskID + "total read char(s) : " + totalRead);
		return totalRead;

	}

	@Override
	public void close() throws IOException {
		if(ir != null) ir.close();
	}
	
	
}
