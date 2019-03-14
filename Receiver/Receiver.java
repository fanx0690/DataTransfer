
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

public class Receiver extends JFrame {
	public static final int WIDTH = 500;
	public static final int HEIGHT = 600;

	public static final int INNER_WIDTH = 800;
	public static final int INNER_HEIGHT = 1200;

	public static JTextField host = new JTextField();
	public static JTextField udpAck = new JTextField();
	public static JTextField udpData = new JTextField();
	public static JTextField fileName = new JTextField();

	public static JLabel mode;
	public static JTextArea output = new JTextArea();

	public static Socket st = null;
	public static PrintWriter out = null;
	public static BufferedReader in = null;

	// trans
	public static DatagramSocket dataSocket;
	public static byte[] receiveByte;
	public static DatagramPacket dataPacket;
	// flag
	public static final byte[] successData = "success data mark".getBytes();
	public static final byte[] exitData = "exit data mark".getBytes();

	public Receiver() {
		super();
		setSize(WIDTH, HEIGHT);
		setTitle("Receiver");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(null);

		JLabel labelHost = new JLabel("host address of sender:");
		add(labelHost);
		labelHost.setBounds(20, 50, 400, 30);
		JLabel labelUdpACK = new JLabel("UDP port number to receive ACKs from the receiver:");
		add(labelUdpACK);
		labelUdpACK.setBounds(20, 120, 400, 30);
		JLabel labelUdpData = new JLabel("UDP port number to receive data from the sender:");
		add(labelUdpData);
		labelUdpData.setBounds(20, 190, 400, 30);
		JLabel labelfile = new JLabel("file name:");
		add(labelfile);
		labelfile.setBounds(20, 260, 400, 30);
		JButton trans = new JButton("TRANSFER");
		trans.setBounds(20, 330, 200, 30);
		add(trans);
		JLabel state = new JLabel("current mode:");
		state.setBounds(20, 370, 100, 25);
		add(state);
		mode = new JLabel("reliable");
		mode.setBounds(130, 370, 100, 25);
		add(mode);
		JButton changeMode = new JButton("change mode");
		changeMode.setBounds(240, 370, 180, 25);
		add(changeMode);
		JLabel out = new JLabel("Output:");
		add(out);
		out.setBounds(20, 400, 200, 25);
		JScrollPane outputScroll = new JScrollPane(output);
		outputScroll.setBounds(20, 425, 420, 100);
		add(outputScroll);

		// set
		outputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		outputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		outputScroll.setAutoscrolls(true);

		host.setBounds(20, 80, 400, 30);
		add(host);

		udpAck.setBounds(20, 150, 400, 30);
		add(udpAck);

		udpData.setBounds(20, 220, 400, 30);
		add(udpData);

		fileName.setBounds(20, 290, 400, 30);
		add(fileName);

		changeMode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				if (mode.getText().equals("reliable")) {
					mode.setText("unreliable");
				} else {
					mode.setText("reliable");
				}
			}
		});
		trans.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent a) {
				new subThread().start();
			}
		});

	}

	public static boolean isEqualsByteArray(byte[] compareBuf, byte[] buf, int len) {
		if (buf == null || buf.length == 0 || buf.length < len || compareBuf.length < len)
			return false;

		boolean flag = true;

		int innerMinLen = Math.min(compareBuf.length, len);
		for (int i = 0; i < innerMinLen; i++) {
			if (buf[i] != compareBuf[i]) {
				flag = false;
				break;
			}
		}
		return flag;
	}

	public static void main(String[] args) {
		Receiver c = new Receiver();
		c.setResizable(false);
		c.setVisible(true);
	}

}

class subThread extends Thread {
	public subThread() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		String address = "";
		int portR = -1;
		int portS = -1;
		String file = "";
		boolean tf = true;
		try {
			address = Receiver.host.getText();
			portS = Integer.parseInt(Receiver.udpAck.getText());
			portR = Integer.parseInt(Receiver.udpData.getText());
			file = Receiver.fileName.getText();
		} catch (Exception e) {
			Receiver.output.setText("wrong input, retry");
			tf = false;
		}
		if (tf == true) {
			try {
				DatagramSocket socket = new DatagramSocket();
				byte[] buf = "ffstartff".getBytes();
				DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(address), portS);
				socket.send(packet);
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}

			////////////////
			byte[] buf = new byte[125];

			DatagramPacket dpk = null;
			DatagramSocket dsk = null;
			BufferedOutputStream bos = null;
			try {
				dpk = new DatagramPacket(buf, buf.length, new InetSocketAddress(InetAddress.getByName(address), portS));
				dsk = new DatagramSocket(portR);
				bos = new BufferedOutputStream(new FileOutputStream(file));

				System.out.println("Wait Sender ....");
				dsk.receive(dpk);

				int readSize = 0;
				int readCount = 0;
				int flushSize = 0;
				int drop = 1;
				byte lastsn = -1;
				byte sn = 2;
				while ((readSize = dpk.getLength()) != 0) {
					if (Receiver.mode.getText().equals("unreliable")) {
						if (drop == 10) {
							drop = 0;
							dpk.setData(buf, 0, buf.length);
							dsk.receive(dpk);
							continue;
						}
					}
					if (Receiver.isEqualsByteArray(Receiver.exitData, buf, readSize)) {
						System.out.println("Receive finish ...");
						// send exit flag
						dpk.setData(Receiver.exitData, 0, Receiver.exitData.length);
						dsk.send(dpk);
						break;
					}

					sn = buf[0];
					if (lastsn == sn) {
						dpk.setData(buf, 0, buf.length);
						dsk.receive(dpk);
						continue;
					}
					lastsn = sn;
					bos.write(buf, 1, readSize - 1);
					if (++flushSize % 1000 == 0) {
						flushSize = 0;
						bos.flush();
					}

					dpk.setData(Receiver.successData, 0, Receiver.successData.length);
					dsk.send(dpk);

					dpk.setData(buf, 0, buf.length);
					Receiver.output.append("receive count of " + (++readCount) + "\n");
					Receiver.output.setCaretPosition(Receiver.output.getText().length());
					dsk.receive(dpk);
					drop++;

				}

				// last flush
				bos.flush();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (bos != null)
						bos.close();
					if (dsk != null)
						dsk.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			//////////////////////

		}
	}
}
