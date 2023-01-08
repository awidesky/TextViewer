package gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import main.Main;
import main.SettingData;

public class SettingDialog extends JDialog {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2127297466348994156L;
	
	
	private JLabel label1 = new JLabel("Prefered Buffer size :");
	private JTextField tf1 = new JTextField();
	
	private JLabel label2 = new JLabel("Characters per page :");
	private JTextField tf2 = new JTextField();
	private JLabel chars = new JLabel("char(s)");
	
	private JLabel label3 = new JLabel("Page always ends with newline");
	private JCheckBox chb = new JCheckBox();

	private JLabel label4= new JLabel("Single-paged file limit :");
	private JTextField tf4 = new JTextField();
	private JComboBox<String> cb = new JComboBox<String>(new String[] { "B", "KB", "MB", "GB" });
	
	private JLabel label5 = new JLabel("Pre-read ");
	private JTextField tf5 = new JTextField();
	private JLabel page = new JLabel("page(s) in buffer");
	
	private JButton done = new JButton("done");
	
	public SettingDialog(SettingData setting) {
		
		super((Window)null);
		
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setModal(true);
		setTitle("Setting");
		setSize(250, 220);
		setLocation(dim.width/2-getSize().width/2, dim.height/2-getSize().height/2);
		setResizable(false);
		setLayout(null);
		
		label1.setBounds(5, 8, label1.getPreferredSize().width, label1.getPreferredSize().height);
		tf1.setBounds(8 + label1.getPreferredSize().width, 5, 60, tf1.getPreferredSize().height);
		tf1.setText("" + setting.charBufSize);
		
		label2.setBounds(5, 38, label2.getPreferredSize().width, label2.getPreferredSize().height);
		tf2.setBounds(8 + label2.getPreferredSize().width, 35, 80, tf2.getPreferredSize().height);
		tf2.setText("" + setting.charPerPage);
		chars.setBounds(70 + label1.getPreferredSize().width, 5, chars.getPreferredSize().width, 22);
		
		label3.setBounds(5, 68, label3.getPreferredSize().width, label3.getPreferredSize().height);
		chb.setBounds(8 + label3.getPreferredSize().width, 65, chb.getPreferredSize().width, chb.getPreferredSize().height);
		chb.setSelected(setting.pageEndsWithNewline);
		
		label4.setBounds(5, 98, label4.getPreferredSize().width, label4.getPreferredSize().height);
		tf4.setBounds(8 + label4.getPreferredSize().width, 95, 40, tf4.getPreferredSize().height);
		tf4.setText("" + setting.singlePageFileSizeLimit);
		cb.setSelectedIndex(0); // 1024로 계속 나눠 보면서 딱 떨어질 때까지만 하고 정수로 해서 KB 등으로 표시
		cb.setBounds(51 + label4.getPreferredSize().width, 95, cb.getPreferredSize().width, 22);
		
		label5.setBounds(5, 128, label5.getPreferredSize().width, label5.getPreferredSize().height);
		tf5.setBounds(8 + label5.getPreferredSize().width, 125, 20, tf5.getPreferredSize().height);
		tf5.setText("" + setting.loadedPagesNumber);
		page.setBounds(35 + label5.getPreferredSize().width, 124, page.getPreferredSize().width, 22);
		
		done.setBounds(getSize().width/2 - done.getPreferredSize().width/2 - 10, getSize().height - done.getPreferredSize().height - 45, done.getPreferredSize().width, done.getPreferredSize().height);
		done.addActionListener((e) -> {
			try {
				if(!setting.set(Integer.valueOf(tf1.getText()), Integer.valueOf(tf2.getText()), chb.isSelected(), Long.valueOf(Main.getByteSize(tf4.getText() + cb.getSelectedItem())), Integer.valueOf(tf5.getText())))
					return;
			} catch(NumberFormatException ex) {
				SwingDialogs.error("Invalid input!", "%e%", ex, true);
				return;
			}
			setVisible(false);
			dispose();
		});
		
		
		add(label1);
		add(tf1);
		add(label2);
		add(tf2);
		add(chars);
		add(label3);
		add(chb);
		add(label4);
		add(tf4);
		add(cb);
		add(label5);
		add(tf5);
		add(page);
		add(done);
		
		setVisible(true);
		
	}
	
}
