/*
 * Copyright (c) 2023 Eugene Hong
 *
 * This software is distributed under license. Use of this software
 * implies agreement with all terms and conditions of the accompanying
 * software license.
 * Please refer to LICENSE
 * */

package gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Optional;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;

import main.ReferenceDTO;
import util.SwingDialogs;

public class FontDialog extends JDialog {

	private static final long serialVersionUID = -737932341920503748L;
	
	public static boolean showAll = false;
	
	private JLabel name = new JLabel("Name :");
	private JLabel size = new JLabel("Size :");
	private JLabel pt = new JLabel("pt");
	private JComboBox<String> fontName;
	private JCheckBox bold;
	private JCheckBox italic;
	private JTextField fontSize;
	private JButton done = new JButton("Done");
	
	private int fontStyle = Font.PLAIN;
	
	public FontDialog(ReferenceDTO<Font> ref, Font now, String content) {
		
		super((Window)null);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setModal(true);
		setTitle("Change font...");
		setSize(350, 150);
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
		setResizable(false);
		setLayout(null);
			
		fontName = new JComboBox<>(
				Arrays.stream(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
							.map((s) -> new Font(s, Font.PLAIN, 20))
							.filter( (f) -> showAll || (f.canDisplayUpTo("".equals(content) ? "abcdefg" : content) == -1) )
							.map(Font::getFamily)
							.toArray(String[]::new));
		fontName.setSelectedItem(now.getFamily());
		if(!showAll) fontName.setRenderer(new FontCellRenderer());
		bold = new JCheckBox("Bold", now.isBold());
		italic = new JCheckBox("Italic", now.isItalic());
		fontSize = new JTextField("" + now.getSize()); //default font size
		
		name.setBounds(14, 10, name.getPreferredSize().width, name.getPreferredSize().height);
		size.setBounds(265, 14, size.getPreferredSize().width, size.getPreferredSize().height);
		pt.setBounds(305, 39, pt.getPreferredSize().width, pt.getPreferredSize().height);
		
		fontName.setBounds(14, 35, 175, 22);
		
		bold.setBounds(200, 10, bold.getPreferredSize().width, bold.getPreferredSize().height);
		bold.addItemListener((e) -> {
			if(e.getStateChange() == ItemEvent.SELECTED) {
				if(fontStyle == Font.PLAIN) fontStyle = Font.BOLD;
				else fontStyle |= Font.BOLD;
			}
		});
		italic.setBounds(200, 35, italic.getPreferredSize().width, italic.getPreferredSize().height);
		italic.addItemListener((e) -> {
			if(e.getStateChange() == ItemEvent.SELECTED) {
				if(fontStyle == Font.PLAIN) fontStyle = Font.ITALIC;
				else fontStyle |= Font.ITALIC;
			}
		});
		
		fontSize.setBounds(265, 35, 37, 22);
		
		done.setBounds(144, 70, done.getPreferredSize().width, done.getPreferredSize().height);
		done.setMnemonic('d');
		done.addActionListener((e) -> {
			try {
				int fontsize = Integer.parseInt(fontSize.getText());
				if(fontsize < 1) throw new NumberFormatException("No negative number! : " + fontsize);
				ref.set(new Font(Optional.ofNullable(fontName.getSelectedItem()).orElseGet(() -> new JLabel().getFont()).toString(), fontStyle, fontsize));
			} catch (NumberFormatException err) {
				SwingDialogs.error("Invalid font size!!", "%e%", err, true);
				return;
			}
			setVisible(false);
			dispose();
		});
		
		
		add(name);
		add(size);
		add(pt);
		add(fontName);
		add(bold);
		add(italic);
		add(fontSize);
		add(done);
		
		//addWindowListener(null);
		setVisible(true);
		
	}
	
	
	private class FontCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 5433840238044092528L;

		@Override
	    public Component getListCellRendererComponent(
	        JList<?> list,
	        Object value,
	        int index,
	        boolean isSelected,
	        boolean cellHasFocus) {
	        JLabel label = (JLabel)super.getListCellRendererComponent(
	            list,value,index,isSelected,cellHasFocus);// System.out.println(value); 
	        Font font = new Font((String)value, Font.PLAIN, label.getFont().getSize());
	        label.setFont(font);
	        return label;
	    }
		
	}
}
