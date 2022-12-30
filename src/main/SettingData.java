package main;

import gui.SwingDialogs;

public class SettingData {

	/** how many <strong>"characters"</strong> does buffer have? */
	public int charBufSize; 
	/** maximum character per one page */
	public int charPerPage;
	/** if<code>true</code>, every page is ended as a whole line even if number of characters of the page is less than <code>charPerPage</code> */
	public boolean pageEndsWithNewline;
	/** if a file is larger than <code>singlePageFileSizeLimit</code> byte, read it as multi-paged file */
	public long singlePageFileSizeLimit;
	/**
	 * How many page will be stored in memory(except for one page that is placed in <code>TextViewer</code>)?
	 * if 1, use <code>SynchronousQueue</code> not <code>LinkedBlockingQueue</code>  
	 *  */
	public int loadedPagesBufferLength;
	
	
	
	public SettingData(int bufSize, int charPerPage, boolean pageEndsWithNewline, long singlePageFileSizeLimit, int loadedPagesBufferLength) {
		if(!set(bufSize, charPerPage, pageEndsWithNewline, singlePageFileSizeLimit, loadedPagesBufferLength)) {
			this.charBufSize = -1;
			this.charPerPage = -1;
			this.pageEndsWithNewline = false;
			this.singlePageFileSizeLimit = -1;
			this.loadedPagesBufferLength = -1;
		}
	}
	
	/**
	 * Copy constructor
	 * */
	public SettingData(SettingData setting) {
		this(setting.charBufSize, setting.charPerPage, setting.pageEndsWithNewline, setting.singlePageFileSizeLimit, setting.loadedPagesBufferLength);
	}
	
	
	public boolean set(int bufSize, int charPerPage, boolean pageEndsWithNewline, long singlePageFileSizeLimit, int loadedPagesBufferLength) {

		String errContent = null;
		
		if(bufSize < 1) errContent = "Buffer size must be bigger than zero!"; //TODO : bug when 1? 
		if(charPerPage < 1) errContent = "There should be more than zero characters per page!"; 
		if(singlePageFileSizeLimit < 1) errContent = "Single-paged file limit must be bigger than zero!"; 
		if(loadedPagesBufferLength < 1) errContent = "Page buffer size must be positive!";
		
		this.charBufSize = bufSize;
		this.charPerPage = charPerPage;
		this.pageEndsWithNewline = pageEndsWithNewline;
		this.singlePageFileSizeLimit = singlePageFileSizeLimit;
		this.loadedPagesBufferLength = loadedPagesBufferLength;

		if(errContent != null) {
			SwingDialogs.error("Invalid input!", errContent, null, false);
			return false;
		} else { return true; }
	}
	
	
}
