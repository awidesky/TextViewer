package gui;

import java.util.function.Consumer;

import main.Main;

public class TitleGeneartor {

	private static Consumer<String> titleConsumer = null;
	
	private static String filePath = null;
	private static String fileSize = null;
	private static boolean paged = false;
	private static String charset = null;
	private static boolean edited = false;
	private static boolean loading = false;
	private static long pageNum = -1L;
	
	
	public static void titleConsumer(Consumer<String> titleConsumer) {
		TitleGeneartor.titleConsumer = titleConsumer;
	}
	
	public static void loading(boolean b) {
		TitleGeneartor.loading = b;
		generate();
	}

	public static void edited(boolean b) {
		TitleGeneartor.edited = b;
		generate();
	}
	
	public static void fileClosed() {
		filePath = null;
	}

	public static void reset(String filePath, String fileSize, boolean paged, String charset, boolean edited, boolean loading, long pageNum) {
		TitleGeneartor.filePath = filePath;
		TitleGeneartor.fileSize = fileSize;
		TitleGeneartor.paged = paged;
		TitleGeneartor.charset = charset;
		TitleGeneartor.edited = edited;
		TitleGeneartor.loading = loading;
		TitleGeneartor.pageNum = pageNum;
		
		generate();
	}
	
	public static void generate() {
		
		String fileExplainPart;
		if(filePath == null) fileExplainPart = " - \"Untitled\"";
		else fileExplainPart = " - \"" + filePath + "\" (" + fileSize + (paged ? ", page #" + pageNum : "") + ") in " + charset;
		titleConsumer.accept((edited ? "*" : "") + Main.VERSION + fileExplainPart + (loading ? " (loading...)" : ""));
	}

	public static void pageNum(long l) {
		TitleGeneartor.pageNum = l;
		generate();
	}


	
}
