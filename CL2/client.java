
//CL2

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.net.*;
import java.io.*;
import java.net.*;

/*
 * chu ki doc data stream: 
 * 			  - read data dtream vao buffer -> ghi vao file -> luu vao buffer
 * 			  - tao flag cho 2 thraed doc buffer
 * 			  - 2 thread doc buffer -> ghi vao tream -> chuyen trang thai
 *<- quay lai chu ki 
 * 
*/
class BufferFileReader {
	private byte[] buffer = new byte[1000];
	private int length = 0;
	private int queue = 2;

	public BufferFileReader(byte[] buf, int len) {
		this.buffer = buf;
		this.length = len;
	}

	public synchronized void setQueueDown() {
		if (this.queue > 0) {
			this.queue--;
		}
	}

	public synchronized int getQueue() {
		return this.queue;
	}

	// for thread client read buffer -> down queue -> get buffer
	public synchronized byte[] getBuffer() {
		this.setQueueDown();
		return this.buffer;
	}

	public synchronized int getLength() {
		return this.length;
	}
}

public class client {// CLIENT 2.1 -----------
	static String pathFolder = "SharedFolder";
	public static int status1Th = 0;
	public static int status2Th = 0;
	public static String fileCurrent = "";
	public static BufferFileReader buffer;
	public static int lengthFile = 0;

	static synchronized int getLengthBufferReader() {
		return buffer.getLength();
	};

	static synchronized byte[] getBufferReader() {
		return buffer.getBuffer();
	};

	static synchronized void setBufferReader(byte[] buf, int len) {
		buffer = new BufferFileReader(buf, len);
	};

	static synchronized int getQueueBuffer() {
		return buffer.getQueue();
	}

	static synchronized int getStatus1Th() {
		return status1Th;
	};

	static synchronized int getStatus2Th() {
		return status2Th;
	};

	static synchronized void setFile(String file) {
		fileCurrent = file;
	};

	static synchronized String getFile() {
		return fileCurrent;
	};

	static synchronized void setStatusThread1Th(int i) {
		status1Th = i;
	}

	static synchronized void setStatusThread2Th(int i) {
		status2Th = i;
	}

	static synchronized String[] getListFile() {
		File fileL = new File("SharedFolder");
		String[] fileList = fileL.list();
		return fileList;
	}

	static synchronized String getPathFolder() {
		return pathFolder;
	}

	public static void main(String[] args) throws IOException {
		String downFile = new String("download");
		System.out.print("hello i'm client!");
		String host;
		Socket client = null;
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

		System.out.print("\n\nEnter IP server:");
		host = input.readLine();

		// create thread client connect to ...
		Thread fClient1 = new ThreadClient("127.0.0.1", 8181);

		// create thread client connect to ...
		Thread fClient2 = new ThreadClient("127.0.0.1", 8282);
		fClient1.start();
		fClient2.start();
		try {
			InetAddress address = InetAddress.getByName(host);
			System.out.println("IP server Address: " + address.toString());
		} catch (UnknownHostException e) {
			System.out.println("Could not find: " + host);
		}
		try {
			client = new Socket(host, 8080);
//			System.out.println(client.toString());
			// create stream in/output
			InputStream is = client.getInputStream();
			OutputStream os = client.getOutputStream();
			String line;
			while (true) {
				String sts = "";
				ReceiveProtocol rec = new ReceiveProtocol(os, is);
				sts = rec.saveFile();
				DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
				LocalDateTime now = LocalDateTime.now();
				System.out.println("download success ! : time: " + dtf.format(now));
				if (sts != "") {
//					setStatusThread1Th(1);
//					setStatusThread2Th(1);
//					setFile(sts);
				}
			}

		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + host);
			return;
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to " + host);
			return;
		}

	}

}

/*
 * thread fake client
 */
class ThreadClient extends Thread {

	private int port;
	private String host;
	private String path;

	public ThreadClient(String host, int port) {
		this.port = port;
		this.host = host;
		this.path = client.getPathFolder();
	}

	@Override
	public void run() {
		Socket cl = null;
		// server is listening on port
		try {
			cl = new Socket(this.host, this.port);
			InputStream is = cl.getInputStream();
			OutputStream os = cl.getOutputStream();
			System.out.println("create connect to server fake at " + String.valueOf(this.port + "\n"));
			while (true) {
				if (this.port == 8181) {
					if (client.getStatus1Th() != 0) {
						os.write(client.getBufferReader(), 0, client.getLengthBufferReader());
//						String fileName = client.getFile();
//						SendProtocol send = new SendProtocol(os, is, this.path + "/" + fileName, fileName);
//						send.send();
						client.setStatusThread1Th(0);
					}
				} else {
					if (client.getStatus2Th() != 0) {
						os.write(client.getBufferReader(), 0, client.getLengthBufferReader());
//						String fileName = client.getFile();
//						SendProtocol send = new SendProtocol(os, is, this.path + "/" + fileName, fileName);
//						send.send();
						client.setStatusThread2Th(0);
					}
				}

			}

		} catch (IOException e) {

		}

	}

}

class ReceiveProtocol {
	private InputStream is = null;
	private OutputStream os = null;
	private final String path = client.getPathFolder();
	private String type = "";

	public ReceiveProtocol(OutputStream os, InputStream is) {
		this.is = is;
		this.os = os;
	}

	public String saveFile() {
		int count = 0;
		byte[] buffer = new byte[1];
		byte[] dataFile = new byte[1000];
		String length = "";
		String fileName = "";
		try {
			boolean switchName = false;
			while ((count = is.read(buffer, 0, 1)) != -1) {

				client.setBufferReader(buffer, count);
				client.setStatusThread1Th(1);
				client.setStatusThread2Th(1);
				int que = 0;
				do {
					que = client.getStatus1Th() + client.getStatus2Th();
				} while (que > 0);

				String s = new String(buffer);
				if (s.equals(":") && !switchName) {
					switchName = true;
				} else {
					if (!s.equals("@") && switchName) {
						fileName += s;
					} else if (!switchName) {
						length += s;
					} else {
						int len = Integer.parseInt(length);
						int total = 0;
						File f = new File(this.path + "/" + fileName);
						FileOutputStream fOut = new FileOutputStream(f);
						BufferedOutputStream wFile = new BufferedOutputStream(fOut);
						// get each byte
						while ((count = is.read(dataFile)) != -1) {
							total += count;
							wFile.write(dataFile, 0, count);
							client.setBufferReader(dataFile, count);
							client.setStatusThread1Th(1);
							client.setStatusThread2Th(1);
							que = 0;
							do {
								que = client.getStatus1Th() + client.getStatus2Th();
							} while (que > 0);

							if (total == len) {
								wFile.flush();
								wFile.close();
								return fileName;
							}

						}
					}

				}

			}
		} catch (IOException e) {
		}
		return "";
	}

	/*
	 * get message from stream receive message protocol form:
	 * <size_message>@<content_message>
	 * 
	 * @return content message
	 */
	public String getMessage() {
		int count = 0;
		byte[] buffer = new byte[1];
		String length = "";
		try {
			while ((count = is.read(buffer, 0, 1)) != -1) {
				String s = new String(buffer);
				if (s.equals("@")) {
					int len = Integer.parseInt(length);
					int total = 0;
					String mes = "";
					// get each byte
					while ((count = is.read(buffer, 0, 1)) != -1) {
						total += count;
						String element = new String(buffer);
						mes += element;
						if (total == len) {
							return mes;
						}
					}

				} else {
					length += s;
				}

			}
		} catch (IOException e) {
		}
		return "";
	}

}

class SendProtocol {
	private OutputStream os = null;
	private InputStream is = null;
	private String dataStr = "";
	private String path = "";
	private String fileName = "";
	private final String key = "@";
	private final String beforeStr = "s";
	private final String beforeFile = "f";

	public SendProtocol(OutputStream os, InputStream is, String data) {
		this.is = is;
		this.os = os;
		this.dataStr = data;
	}

	public SendProtocol(OutputStream os, InputStream is, String path, String fileName) {
		this.is = is;
		this.os = os;
		this.path = path;
		this.fileName = fileName;
	}

	public void send() {

		if (this.path.isEmpty() && !this.dataStr.isEmpty()) {
			/*
			 * send message protocol form: <size_message>@<content_message> type: byte[]
			 */
			try {
				int len = this.dataStr.length();
				String l = String.valueOf(len) + "@";
				byte[] bb = this.dataStr.getBytes();
				this.os.write(l.getBytes());
				this.os.write(this.dataStr.getBytes());
				this.os.flush();
			} catch (IOException e) {
				return;
			}
		} else {
			/*
			 * send file protocol form: <size_file>:<file_name>@<data_file> type: byte[]
			 */
			try {
				File fTemp = new File(this.path);
				FileInputStream fIn = new FileInputStream(fTemp);
				BufferedInputStream inputStream = new BufferedInputStream(fIn);
				long sizeFL = fTemp.length();
				String sizeFS = String.valueOf(sizeFL);
				String messFile = sizeFS + ":" + this.fileName + "@";
				this.os.write(messFile.getBytes());
				int total = 0;
				int nRead = 0;
				byte[] buffer = new byte[1000];
				while ((nRead = inputStream.read(buffer)) != -1) {
					total += nRead;
					this.os.write(buffer, 0, nRead);
				}
				System.out.println("send file < " + this.fileName + " > success!! :<>: " + "Read " + total + " bytes");
			} catch (IOException e) {
				System.out.println(e);
			}
			return;
		}
	}

}
