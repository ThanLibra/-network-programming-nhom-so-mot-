
// CL1.2

import java.net.*;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/*
 * TRAN KIM HIEU 
 * 17020750
 * 
 * client connect den <host> va port 8080
 * mo ta: 
 * 		cac cau lenh dieu khien:
 * 		1- @logout : client va server ngat ket noi
 * 		2- @show : client yeu cau nhan danh sach file tu server
 * 		3- download <ten_file> : client yeu cau downlaod file : vi du: download video.mp4
 * 		4- upload <ten_file> : client yeu cau upload file len server: vi du: upload video.mp4
 * 
*/
public class client {// CLIENT 1.2 -----------
	static String pathFolder = "SharedFolder";

	static synchronized String getPathFolder() {
		return pathFolder;
	}

	public static void main(String[] args) throws IOException {
		String downFile = new String("download");
		System.out.print("hello i'm client!");
		InputStream rBinary = null;
		String host;
		Socket client = null;
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		// create fake server
		Thread fServer = new ThreadServer(8282);
		fServer.start();

		System.out.print("\n\nEnter IP server:");
		host = input.readLine();

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
//			while (true) {
//				
//			}

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
 * thread client fake server
 */
class ThreadServer extends Thread {

	private int port;

	public ThreadServer(int p) {
		this.port = p;
	}

	@Override
	public void run() {
		// server is listening on port
		try {
			ServerSocket ss = new ServerSocket(this.port);
			System.out.print("hello fake server at " + String.valueOf(this.port) + "\n");
			Socket s = null;

			try {
				// socket object to receive incoming client requests
				s = ss.accept();

				System.out.println("A new client is connected : " + s);

				// obtaining input and out streams
				InputStream dis = s.getInputStream();
				OutputStream dos = s.getOutputStream();

				while (true) {
					ReceiveProtocol rec = new ReceiveProtocol(dos, dis);
					rec.saveFile();
					DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
					LocalDateTime now = LocalDateTime.now();
					System.out.println("download success ! : time: "+dtf.format(now));
					
				}
			} catch (Exception e) {
				e.printStackTrace();
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
