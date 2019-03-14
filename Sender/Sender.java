import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Sender {

	public Sender() {
		// TODO Auto-generated constructor stub
	}

	public static DatagramSocket dataSocket;
	public static byte[] sendDataByte;
	public static DatagramPacket dataPacket;

	public static final byte[] successData = "success data mark".getBytes();
	public static final byte[] exitData = "exit data mark".getBytes();

	public static void main(String[] args) throws IOException {
		String address = "";
		int portR = -1;
		int portS = -1;
		String fileName = "";
		int timeout = -1;
		try {
			address = args[0];
			portR = Integer.parseInt(args[1]);
			portS = Integer.parseInt(args[2]);
			fileName = args[3];
			timeout = Integer.parseInt(args[4]);
		} catch (Exception e) {
			System.out.println("wrong input, retry");
		}
		boolean flag = false;
		boolean exitflag = false;
		DatagramSocket socket = new DatagramSocket(portS);
		byte[] buf = new byte[124];
		while (true) {
			try {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				String ip = packet.getAddress().getHostAddress();
				buf = packet.getData();
				String data = new String(buf, 0, packet.getLength());
				if (data.equals("ffstartff")) {
					flag = true;
					socket.close();

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (flag == true) {
				System.out.println("transfer start!");
				exitflag = true;
				//////////////
				long startTime = System.currentTimeMillis();
				buf = new byte[125];
				byte[] receiveBuf = new byte[1];

				RandomAccessFile accessFile = null;
				DatagramPacket dpk = null;
				DatagramSocket dsk = null;
				int readSize = -1;
				try {
					accessFile = new RandomAccessFile(fileName, "r");
					dpk = new DatagramPacket(buf, buf.length,
							new InetSocketAddress(InetAddress.getByName(address), portR));
					dsk = new DatagramSocket(portS);
					int sendCount = 0;

					dsk.setSoTimeout(timeout);
					byte sn = (byte) 0;

					while ((readSize = accessFile.read(buf, 1, buf.length - 1)) != -1) {
						buf[0] = sn;
						dpk.setData(buf, 0, readSize + 1);
						dsk.send(dpk);

						// wait Receiver response
						{
							while (true) {
								dpk.setData(receiveBuf, 0, receiveBuf.length);
								try {
									dsk.receive(dpk);
								} catch (SocketTimeoutException a) {
									receiveBuf = "time out".getBytes();

								}

								// confirm Receiver receive
								if (!isEqualsByteArray(successData, receiveBuf, dpk.getLength())) {
									System.out.println("resend package...");
									dpk.setData(buf, 0, readSize + 1);
									dsk.send(dpk);
								} else {
									break;
								}
							}
						}
						if (sn == (byte) 1) {
							sn = (byte) 0;
						} else if (sn == (byte) 0) {
							sn = (byte) 1;
						}
						System.out.println("send package " + (++sendCount));
					}

					System.out.println("Send EOT ....");
					dpk.setData(exitData, 0, exitData.length);
					dsk.send(dpk);

					while (true) {
						dpk.setData(receiveBuf, 0, receiveBuf.length);
						try {
							dsk.receive(dpk);
						} catch (SocketTimeoutException t) {
							receiveBuf = "time out".getBytes();
							break;
						}
						if (!isEqualsByteArray(exitData, receiveBuf, dpk.getLength())) {
							System.out.println("Resend EOT ....");
							dsk.send(dpk);
						} else
							break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						if (accessFile != null)
							accessFile.close();
						if (dsk != null)
							dsk.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				long endTime = System.currentTimeMillis();
				System.out.println("Total transmission time: " + (endTime - startTime) + " (microseconds)");

			}
			if (exitflag == true) {
				break;
			}
		}

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

}
