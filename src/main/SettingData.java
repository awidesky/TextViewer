package main;

import gui.SwingDialogs;

public class SettingData {

	/** how many <strong>"characters"</strong> does buffer have? */
	private int charBufSize; 
	/** maximum character per one page */
	private int charPerPage;
	/** if<code>true</code>, every page is ended as a whole line even if number of characters of the page is less than <code>charPerPage</code> */
	private boolean pageEndsWithNewline;
	/** if a file is larger than <code>singlePageFileSizeLimit</code> byte, read it as multi-paged file */
	private long singlePageFileSizeLimit;
	/**
	 * <pre>
	 * How many page will be stored in memory(include one that displayed in <code>TextViewer</code>)
	 * At least one page should loaded, so this must be positive number
	 * if value is 2, use <code>SynchronousQueue</code> not <code>LinkedBlockingQueue</code>
	 * if value is 1, use <code>SynchronousQueue</code>, AND <code>SelectedFileHandler</code> will wait until <code>TextViewer</code> requests new page
	 * </pre> 
	 *  */
	private int loadedPagesNumber;
	/** NewLine character (e.g. CRLF, CR, LF) */
	private LineSeparator lineSeparator;
	
	
	
	public SettingData(int charBufSize, int charPerPage, boolean pageEndsWithNewline, long singlePageFileSizeLimit, int loadedPagesNumber, LineSeparator lineSeparator) {
		if(!set(charBufSize, charPerPage, pageEndsWithNewline, singlePageFileSizeLimit, loadedPagesNumber, lineSeparator)) {
			this.charBufSize = -1;
			this.charPerPage = -1;
			this.pageEndsWithNewline = false;
			this.singlePageFileSizeLimit = -1;
			this.loadedPagesNumber = -1;
			this.lineSeparator = LineSeparator.getDefault();
		}
	}
	
	/**
	 * Copy constructor
	 * */
	public SettingData(SettingData setting) {
		this(setting.charBufSize, setting.charPerPage, setting.pageEndsWithNewline, setting.singlePageFileSizeLimit, setting.loadedPagesNumber, setting.lineSeparator);
	}
	
	
	public boolean set(int charBufSize, int charPerPage, boolean pageEndsWithNewline, long singlePageFileSizeLimit, int loadedPagesNumber, LineSeparator lineSeparator) {

		String errContent = null;
		
		if(charBufSize < 1) errContent = "Buffer size must be bigger than zero!";
		if(charPerPage < 1) errContent = "There should be more than zero characters per page!"; 
		if(singlePageFileSizeLimit < 1) errContent = "Single-paged file limit must be bigger than zero!"; 
		if(loadedPagesNumber < 1) errContent = "Number of loaded page must be positive number!";

		if(errContent != null) {
			SwingDialogs.error("Invalid input!", errContent, null, true);
			return false;
		} else {
			
			this.charBufSize = charBufSize;
			this.charPerPage = charPerPage;
			this.pageEndsWithNewline = pageEndsWithNewline;
			this.singlePageFileSizeLimit = singlePageFileSizeLimit;
			this.loadedPagesNumber = loadedPagesNumber;
			this.lineSeparator = lineSeparator;
			
			return true;
		}
	}

	public int getCharBufSize() { return charBufSize; }
	public int getCharPerPage() { return charPerPage; }
	public boolean getPageEndsWithNewline() { return pageEndsWithNewline; }
	public long getSinglePageFileSizeLimit() { return singlePageFileSizeLimit; }
	public int getLoadedPagesNumber() { return loadedPagesNumber; }
	public LineSeparator getLineSeparator() { return lineSeparator; }
	
	
}
