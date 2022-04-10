package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.*;

public class MainFrame extends JFrame {

	private JScrollPane sp;
	private JTextArea ta = new JTextArea();
	private File lastOpened = new File (System.getProperty("user.home"));
	private File lastSaved = new File (System.getProperty("user.home"));
	private String version = "TextViewer v1.0";
	
	private TestFilechooser f = new TestFilechooser();
	
	public MainFrame() {
		
		setTitle(version);
		setSize(800, 700);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		addMenubar();
		
		ta.setBackground(Color.LIGHT_GRAY);
		ta.setEditable(false);

		sp = new JScrollPane(ta, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		sp.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		sp.setBackground(Color.WHITE);
		
		add(sp);
		
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
		JMenuItem saveFile = new JMenuItem("Save file in another encoding...", KeyEvent.VK_S);
		saveFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
		saveFile.getAccessibleContext().setAccessibleDescription("Save file in another encoding");
		saveFile.addActionListener((e) -> {
			
			/** Read file in EDT */
			saveFile();
			
		});
		fileMenu.add(openFile);
		fileMenu.add(saveFile);

		
		
		JMenu formatMenu = new JMenu("Format");
		formatMenu.setMnemonic(KeyEvent.VK_R);
		formatMenu.getAccessibleContext().setAccessibleDescription("Format menu");
		
		JMenuItem font = new JMenuItem("Change font", KeyEvent.VK_C);
		font.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
		font.getAccessibleContext().setAccessibleDescription("Change font type or size");
		font.addActionListener((e) -> {
			//TODO
		});
		formatMenu.add(font);
		
		
		menuBar.add(fileMenu);
		menuBar.add(formatMenu);
		
		setJMenuBar(menuBar);
		
	}
	
	/**
	 *  This method saves content to the file.
	 * 	
	 * 	File may be written in another thread, or in EDT.
	 *  
	 *  TODO: Deal with super large file(that should be paged)
	 *  
	 *  */
	private void saveFile() {
		
		try {
			BufferedWriter bw = selectSaveLocation();
			if(bw == null) return;
			bw.write(ta.getText().replace("\n", System.lineSeparator()));
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
			if(br == null) return ta.getText();
			String line = null;
			while((line = br.readLine()) != null) {
				result.append(line);
				result.append("\n");
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return result.toString();
		
	}

	public BufferedReader selectFile() throws IOException {
	    f.setCurrentDirectory(lastOpened.getParentFile());
	    if (f.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
	    	return null;
	    
	    setTitle(version + " - " + f.getSelectedFile().getAbsolutePath());
	    return new BufferedReader(new FileReader((lastOpened = f.getSelectedFile()), f.getSelectedCharset()));
	}
	
	public BufferedWriter selectSaveLocation() throws IOException {
		f.setSelectedFile(lastOpened);
		f.setCurrentDirectory(lastSaved.getParentFile());
		if (f.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) 
	    	return null;
		
		lastSaved = f.getSelectedFile();
		if(lastSaved.exists() && JOptionPane.showConfirmDialog(null, "replace file?", lastSaved.getName() + " already exists!", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
			return null;
		}
		
	    return new BufferedWriter(new FileWriter(lastSaved, f.getSelectedCharset()));
	}
}
