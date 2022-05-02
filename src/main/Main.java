package main;

import javax.swing.SwingUtilities;

import gui.FontDialog;
import gui.MainFrame;

public class Main {
  
	public static void main(String[] args) {
	 
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("--ShowAllFont")) {
				FontDialog.showAll = true;
			} else {
				System.out.println("Usage : java -jar TextViewer.jar [options]");
				System.out.println("Options : ");
				System.out.println("\t--ShowAllFont\tShow whole font in font list in Change font Dialog");
				return;
			}
		}
		
		
		SwingUtilities.invokeLater(() ->{
			new MainFrame();
		});
	 
    }
}