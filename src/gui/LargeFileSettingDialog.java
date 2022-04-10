package gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JDialog;

import main.LargeFileHandlingRule;
import main.ReferenceDTO;

public class LargeFileSettingDialog extends JDialog {

	public LargeFileSettingDialog(ReferenceDTO<LargeFileHandlingRule> ref) {
		
		super((Window)null);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setModal(true);
		setTitle("Large file setting...");
		setSize(350, 150);
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
		setResizable(false);
		setLayout(null);
		
		new LargeFileHandlingRule(10 * 1024 * 1024 * 1024, 0);
	}
	
}
