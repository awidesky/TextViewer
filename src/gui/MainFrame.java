package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

public class MainFrame extends JFrame {

	private JTextArea ta = new JTextArea();
	
	public MainFrame() {
		
		setSize(800, 700);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		addMenubar();
		
		
		JPanel jp = new JPanel();
		jp.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		jp.setBackground(Color.WHITE);
		jp.setLayout(new BorderLayout());
		
		ta.setBackground(Color.LIGHT_GRAY);
		ta.setEditable(false);
		jp.add(ta);
		
		add(jp);
		
		setVisible(true);
		
	}
	
	private void addMenubar() {
		
		JMenuBar menuBar = new JMenuBar();


		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.getAccessibleContext().setAccessibleDescription("File menu");

		JMenuItem openFile = new JMenuItem("Open file...", KeyEvent.VK_O);
		openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.ALT_MASK));
		openFile.getAccessibleContext().setAccessibleDescription("Open a file");
		openFile.addActionListener((e) -> {
			
			/** Read file in EDT */
			ta.setText(readSelectedFile());
			
		});
		fileMenu.add(openFile);

		
		
		JMenu formatMenu = new JMenu("Format");
		formatMenu.setMnemonic(KeyEvent.VK_O);
		formatMenu.getAccessibleContext().setAccessibleDescription("Format menu");
		
		JMenuItem font = new JMenuItem("Change font", KeyEvent.VK_C);
		font.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
		font.getAccessibleContext().setAccessibleDescription("Change font type or size");
		font.addActionListener((e) -> {
			//TODO
		});
		fileMenu.add(font);
		
		
		menuBar.add(fileMenu);
		menuBar.add(formatMenu);
		
		setJMenuBar(menuBar);
		
	}
	
	/**
	 *  This method returns content of the file.
	 * 	
	 * 	File may be read in another thread, or in EDT.
	 *  
	 *  TODO: Deal with super large file(that should be paged)
	 *  
	 *  */
	private String readSelectedFile() {

		StringBuilder result = new StringBuilder();
		
		try {
			BufferedReader br = selectFile();
			String line = null;
			
			while((line = br.readLine()) != null) {
				result.append(line);
				result.append("\n");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result.toString();
		
	}

	public BufferedReader selectFile() throws IOException {
	    TestFilechooser f = new TestFilechooser();
	    if (f.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
	    	return null;
	    return new BufferedReader(new FileReader(f.getSelectedFile(), f.getSelectedCharset()));
	}
}
