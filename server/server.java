
//Server

import java.net.*;
import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

/*
 * server bat cong 8080
 *  tao thread khi client connect 
 */

public class server {
	static String pathFolder = "SharedFolder";
	static int countFile = 0;
	static int countClient = 0;
	static String command = "";
	static String fName = "";
	public static String addClient = "";

	static synchronized String getAddClient() {
		return addClient;
	};

	static synchronized void setAddClient(String add) {
		addClient = add;
	};

	static synchronized int getCountFile() {
		return countFile;
	}

	static synchronized void setCountFileDown() {
		countFile--;
	}

	static synchronized void setCountFileUp() {
		countFile++;
	}

	static synchronized String getPathFolder() {
		return pathFolder;
	}

	static synchronized void setCountThread() {
		countClient = ThreadRun.activeCount() - 1;
	}

	static synchronized void setCmd(String c) {
		command = c;
	}

	static synchronized String getCmd() {
		return command;
	}

	static synchronized String[] getListFile() {
		String pFile = server.getPathFolder();
		File fileL = new File(pFile);
		String[] fileList = fileL.list();
		return fileList;
	}

	public static void main(String[] args) throws IOException {
		System.out.print("hello i'm server!\n");
		String port = "8080";
		// server is listening on port
		ServerSocket ss = new ServerSocket(8080);
		// client request
		Thread u = new ThreadUser();
		u.start();
		while (true) {
			Socket s = null;

			try {
				// socket object to receive incoming client requests
				s = ss.accept();

				System.out.println("A new client is connected : " + s);

				// obtaining input and out streams
				InputStream dis = s.getInputStream();
				OutputStream dos = s.getOutputStream();

				System.out.println("Assigning new thread for this client");

				// create a new thread object
				String add = getAddClient();
				if (add.equals("")) {
					setAddClient(s.toString());
				}
				Thread t = new ThreadRun(s, dis, dos);

				// Invoking the start() method
				t.start();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}
}

/*
 * thread user control
 */
class ThreadUser extends Thread {
	final String downloadFile = new String("download");
	final String showFile = new String("show");

	public ThreadUser() {
	}

	@Override
	public void run() {
		String cmd;
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		try {
			while (true) {
				System.out.println("COMMAND SERVER: ");
				cmd = input.readLine();
				if (cmd.equals("show")) {
					String[] listFile = server.getListFile();
					for (String name : listFile) {
						System.out.println("> " + name);
					}

				}
				server.setCmd(cmd);
				if (cmd.contains(this.downloadFile) && cmd.length() > 9) {
					DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
					LocalDateTime now = LocalDateTime.now();
					System.out.println(dtf.format(now));
				}

			}
		} catch (IOException e) {

		}

	}
}

/*
 * Thread server transfer file
 */
class ThreadRun extends Thread {
	String pathF = "";
	OutputStream wBinary = null;
	final InputStream is;
	final OutputStream os;
	final Socket sClient;
	final String downFile = new String("download");

	public ThreadRun(Socket sClient, InputStream is, OutputStream os) {
		this.is = is;
		this.os = os;
		this.sClient = sClient;
		this.pathF = server.getPathFolder();
	}

	@Override
	public void run() {
		server.setCountThread();
		System.out.println("count thread ------- " + String.valueOf(server.countClient));
		String cmd = "";
		boolean auth = false;
		String add = this.sClient.toString();
		String addAuth = server.getAddClient();
		System.out.println(add);
		if (addAuth.equals(add)) {
			auth = true;
		}
		while (auth) {
			cmd = server.getCmd();
			if (cmd.contains(downFile)) {
				if (cmd.contains(downFile)) {
					String[] fileList = server.getListFile();
					String fn = cmd.substring(9, cmd.length());
					boolean hasFile = false;
					for (String name : fileList) {
						if (name.equals(fn)) {
							hasFile = true;
						}
					}
					if (!hasFile) {
						server.setCmd("");
						cmd = "";
					} else {
						System.out.println("CMD success: " + cmd + "\n");
						SendProtocol send = new SendProtocol(os, is, this.pathF + "/" + fn, fn);
						send.send();
						server.setCmd("");
						server.setCountFileUp();
						cmd = "";
					}
				}
			}
		}

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
