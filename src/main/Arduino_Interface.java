/*
 * Copyright 2017 Andy Heil (Tekks).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package main;

import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import com.fazecast.jSerialComm.SerialPort;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

/**
 *
 * @author Andy Heil (Tekks)
 */

public class Arduino_Interface {
	static SerialPort chosenPort;
	static JButton connectButton;
	static JComboBox<String> portList;
	static String portListselect;
	static TrayIcon trayIcon;
	static JFrame window;
	private static final int PORT = 9999;

	public static void main(String[] args) {
		checkIfRunning();
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		UIManager.put("swing.boldMetal", Boolean.FALSE);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
		portList = new JComboBox<String>();
	}

	@SuppressWarnings("resource")
	private static void checkIfRunning() {
		try {
			new ServerSocket(PORT, 0, InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Another Instance is Running", "Warning", JOptionPane.WARNING_MESSAGE);
			System.exit(1);
		}
	}

	private static void select_comport() {
		if (window != null)
			return;
		window = new JFrame();
		window.setIconImage(createImage("/img/arduino.png", "tray icon"));
		window.setTitle("Arduino Interface");
		window.setSize(450, 100);
		window.setLayout(new BorderLayout());
		window.setLocationRelativeTo(null);
		portList.removeAllItems();
		SerialPort[] portNames = SerialPort.getCommPorts();
		for (int i = 0; i < portNames.length; i++) {
			portList.addItem(portNames[i].getDescriptivePortName());
		}
		if (chosenPort != null) {
			connectButton = new JButton("Deselect");
			portList.setSelectedItem(portListselect);
			portList.setEnabled(false);
		} else {
			connectButton = new JButton("Select");
		}

		JPanel topPanel = new JPanel();
		topPanel.add(portList);
		topPanel.add(connectButton);
		window.add(topPanel, BorderLayout.NORTH);

		// begin transfer

		SystemInfo si = new SystemInfo();
		CentralProcessor processor = si.getHardware().getProcessor();

		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (connectButton.getText().equals("Select")) {
					String comport = portList.getSelectedItem().toString();
					trayIcon.setToolTip("Connected: " + comport);
					portListselect = comport;
					comport = comport.substring(comport.indexOf("(") + 1);
					comport = comport.substring(0, comport.indexOf(")"));
					chosenPort = SerialPort.getCommPort(comport);
					chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
					if (chosenPort.openPort()) {
						connectButton.setText("Deselect");
						portList.setEnabled(false);
						Thread thread = new Thread() {
							@Override
							public void run() {
								try {
									Thread.sleep(1000);
								} catch (Exception e) {
								}
								PrintWriter output = new PrintWriter(chosenPort.getOutputStream());
								while (chosenPort != null) {

									String cpu = Double
											.toString((int) (processor.getSystemCpuLoad() * 100 * 100) / 100.0);
									String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
									long availMem = si.getHardware().getMemory().getAvailable();
									long totalMem = si.getHardware().getMemory().getTotal();
									String memory = (totalMem - availMem) * 100 / (int) Math.pow(1024, 3) / 100.0
											+ " / " + totalMem * 100 / (int) Math.pow(1024, 3) / 100.0;
									String finalstring = "3;" + "TIME:;" + timestamp + ';' + "CPU:;" + cpu + ';'
											+ "MEM:;" + memory;
									System.out.println(finalstring);
									output.print(finalstring);
									output.flush();
									// output.close();
									try {
										Thread.sleep(1000);
									} catch (Exception e) {
									}
								}
							}
						};
						thread.setPriority(Thread.MAX_PRIORITY);
						thread.start();
					}
				} else {
					trayIcon.setToolTip("Disconnected");
					chosenPort.closePort();
					chosenPort = null;
					portList.setEnabled(true);
					connectButton.setText("Select");
				}
			}
		});

		window.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				window.setVisible(false);
				window.dispose();
				window = null;
			}
		});
		window.setVisible(true);
	}

	private static void createAndShowGUI() {
		if (!SystemTray.isSupported()) {
			System.out.println("SystemTray is not supported");
			return;
		}
		final PopupMenu popup = new PopupMenu();
		trayIcon = new TrayIcon(createImage("/img/arduino.png", "tray icon"));
		final SystemTray tray = SystemTray.getSystemTray();

		MenuItem aboutItem = new MenuItem("About");
		MenuItem selectItem = new MenuItem("Select ComPort");
		MenuItem exitItem = new MenuItem("Exit");

		popup.add(aboutItem);
		popup.addSeparator();
		popup.add(selectItem);
		popup.addSeparator();
		popup.add(exitItem);

		trayIcon.setPopupMenu(popup);
		trayIcon.setToolTip("Disconnected");

		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			System.out.println("TrayIcon could not be added.");
			return;
		}

		trayIcon.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				select_comport();
			}
		});

		aboutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JLabel label = new JLabel();
				Font font = label.getFont();
				StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
				style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
				style.append("font-size:" + font.getSize() + "pt;");
				JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "\">" //
						+ "Developed by Tekks<br>" + "Licensed under the Apache License, Version 2.0<br>"
						+ "visit <a href=\"http://tekks.de/\">Tekks.de</a>  for more Information" //
						+ "</body></html>");
				HyperlinkListener hyperlinkListener = new ExtHyperLinkListener(ep);
				ep.addHyperlinkListener(hyperlinkListener);
				ep.setEditable(false);
				ep.setBackground(label.getBackground());
				JOptionPane.showMessageDialog(null, ep, "Arduino Interface | About", JOptionPane.INFORMATION_MESSAGE,
						null);
			}
		});

		selectItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				select_comport();
			}
		});

		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tray.remove(trayIcon);
				System.exit(0);
			}
		});
	}

	protected static Image createImage(String path, String description) {
		URL imageURL = Arduino_Interface.class.getResource(path);

		if (imageURL == null) {
			System.err.println("Resource not found: " + path);
			return null;
		} else {
			return (new ImageIcon(imageURL, description)).getImage();
		}
	}
}