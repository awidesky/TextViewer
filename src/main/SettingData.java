package main;

import gui.SwingDialogs;

public class SettingData {

	public int bufSize;
	public int charPerPage;
	public boolean pageEndsWithNewline;
	public long SinglePageFileSizeLimit;
	public int contentQueueLength;
	
	public SettingData(int bufSize, int charPerPage, boolean pageEndsWithNewline, long singlePageFileSizeLimit, int contentQueueLength) {
		this.bufSize = bufSize;
		this.charPerPage = charPerPage;
		this.pageEndsWithNewline = pageEndsWithNewline;
		this.SinglePageFileSizeLimit = singlePageFileSizeLimit;
		this.contentQueueLength = contentQueueLength;
	}
	
	public boolean set(int bufSize, int charPerPage, boolean pageEndsWithNewline, long singlePageFileSizeLimit, int contentQueueLength) {

		String errContent = null;
		
		if(bufSize < 1) errContent = "Buffer size must be bigger than zero!"; //TODO : bug when 1? 
		if(charPerPage < 1) errContent = "There should be more than zero characters per page!"; 
		if(singlePageFileSizeLimit < 1) errContent = "Single-paged file limit must be bigger than zero!"; 
		if(contentQueueLength < 0) errContent = "Page buffer size must not be negative!";  //TODO : if zero, use SynchronosQueue
		
		this.bufSize = bufSize;
		this.charPerPage = charPerPage;
		this.pageEndsWithNewline = pageEndsWithNewline;
		this.SinglePageFileSizeLimit = singlePageFileSizeLimit;
		this.contentQueueLength = contentQueueLength;

		if(errContent != null) {
			SwingDialogs.error("Invalid input!", errContent, null, false);
			return false;
		} else { return true; }
	}
	
}
