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
	 * <pre>
	 * How many page will be stored in memory(include one that displayed in <code>TextViewer</code>)
	 * At least one page should loaded, so this must be positive number
	 * if value is 2, use <code>SynchronousQueue</code> not <code>LinkedBlockingQueue</code>
	 * if value is 1, use <code>SynchronousQueue</code>, AND <code>SelectedFileHandler</code> will wait until <code>TextViewer</code> requests new page
	 * </pre> 
	 *  */
	public int loadedPagesNumber;
	
	
	
	public SettingData(int bufSize, int charPerPage, boolean pageEndsWithNewline, long singlePageFileSizeLimit, int loadedPagesNumber) {
		if(!set(bufSize, charPerPage, pageEndsWithNewline, singlePageFileSizeLimit, loadedPagesNumber)) {
			this.charBufSize = -1;
			this.charPerPage = -1;
			this.pageEndsWithNewline = false;
			this.singlePageFileSizeLimit = -1;
			this.loadedPagesNumber = -1;
		}
	}
	
	/**
	 * Copy constructor
	 * */
	public SettingData(SettingData setting) {
		this(setting.charBufSize, setting.charPerPage, setting.pageEndsWithNewline, setting.singlePageFileSizeLimit, setting.loadedPagesNumber);
	}
	
	
	public boolean set(int bufSize, int charPerPage, boolean pageEndsWithNewline, long singlePageFileSizeLimit, int loadedPagesNumber) {

		String errContent = null;
		
		if(bufSize < 1) errContent = "Buffer size must be bigger than zero!"; //TODO : bug when 1? 
		if(charPerPage < 1) errContent = "There should be more than zero characters per page!"; 
		if(singlePageFileSizeLimit < 1) errContent = "Single-paged file limit must be bigger than zero!"; 
		if(loadedPagesNumber < 1) errContent = "Number of loaded page must be positive number!";

		if(errContent != null) {
			SwingDialogs.error("Invalid input!", errContent, null, false);
			return false;
		} else {
			
			this.charBufSize = bufSize;
			this.charPerPage = charPerPage;
			this.pageEndsWithNewline = pageEndsWithNewline;
			this.singlePageFileSizeLimit = singlePageFileSizeLimit;
			this.loadedPagesNumber = loadedPagesNumber;
			
			return true;
		}
	}
	
	
}
