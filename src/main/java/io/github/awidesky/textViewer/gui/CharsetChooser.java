/*
 * Copyright (c) 2023 Eugene Hong
 *
 * This software is distributed under license. Use of this software
 * implies agreement with all terms and conditions of the accompanying
 * software license.
 * Please refer to LICENSE
 * */

package io.github.awidesky.textViewer.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.nio.charset.Charset;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;

public class CharsetChooser extends JDialog {

	private static final long serialVersionUID = 4886466228058237262L;
	
	private JButton btn_open = new JButton("open");
	private JComboBox<String> combo = TextFilechooser.getCharsetChooseComboBox();
	
	public CharsetChooser(Charset encoding) {
		
		super((Window)null);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setModal(true);
		setTitle("Choose Encoding...");
		setSize(200, 110);
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
		setResizable(false);
		setLayout(null);

		combo.setSelectedIndex(TextFilechooser.charsetNameList.indexOf(encoding.name()));
		combo.setBounds(getSize().width/2 - combo.getPreferredSize().width/2 - 10, 10, combo.getPreferredSize().width, combo.getPreferredSize().height);
		btn_open.setBounds(getSize().width/2 - btn_open.getPreferredSize().width/2 - 10, 40, btn_open.getPreferredSize().width, btn_open.getPreferredSize().height);
		btn_open.addActionListener((e) -> {
			setVisible(false);
			dispose();
		});
		
		add(combo);
		add(btn_open);
		
		setVisible(true);
		
	}

	public Charset getSelectedCharset() {
		combo.setSelectedIndex(combo.getSelectedIndex());
		return Charset.forName(TextFilechooser.charsetNameList.get(combo.getSelectedIndex()));
	}
	
}