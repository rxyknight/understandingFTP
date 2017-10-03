import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class FtpServer {
	private static final int FTP_PORT = 21;
	private static ServerSocket serverSocket;

	public static void main(String args[]) {
		try {
			serverSocket = new ServerSocket(FTP_PORT);
			System.out.println("Local port:" + serverSocket.getLocalPort());
			// serverSocket.close();
			while (true) {
				Socket socket = serverSocket.accept();
				FtpConnection ftpConnection = new FtpConnection(socket);
				Thread thread = new Thread(ftpConnection);
				thread.start();

			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class FtpConnection implements Runnable {
	private static final int BUFFER_SIZE = 4096;
	private Socket controlScoket;
	private String IP;
	private InputStream conis = null;
	private OutputStream conos = null;
	private String currentDir = "/Users/royren/Desktop/ftproot";
	private ServerSocket pSocket = null;

	public FtpConnection(Socket socket) {
		currentDir = System.getProperties().getProperty("user.dir");
		controlScoket = socket;
		this.IP = socket.getInetAddress().getHostAddress();
	}

	public void run() {
		try {
			System.out.println(IP + " connected ");
			conis = controlScoket.getInputStream();
			conos = controlScoket.getOutputStream();
			reply("220-Welcome COMP5116 FTP Server");
			reply("220-created by Xiangyu(Roy) Ren");
			reply("220 ");
			try{
				while (true) {

					String command = getCommand();
					System.out.println(IP + ":" + command);

					// String user =
					// userInfo.substring(userInfo.indexOf("USER ")+5,userInfo.length()-2);
					if (command.startsWith("USER ")) {
						reply("331 Please specify the password.");
					} else if (command.startsWith("PASS")) {
						reply("230 Login successful.");
					} else if (command.startsWith("SYST")) {
						reply("215 Unix");
					} else if (command.startsWith("FEAT")) {
						reply("211 ");
					} else if (command.startsWith("PWD")) {
						reply("257 ");
					} else if (command.startsWith("REST 0")) {
						reply("150 ");
					} else if (command.startsWith("TYPE I")) {
						reply("200 Type set to I.");
					} else if (command.startsWith("TYPE A")) {
						reply("200 Type set to A.");
					} else if (command.startsWith("PASV")) {
						if (pSocket != null)
							pSocket.close();

						pSocket = new ServerSocket(0);
						int pPort = pSocket.getLocalPort();
						String s_port;
						if (pPort <= 255)
							s_port = "255";
						else {
							int p1 = pPort / 256;
							int p2 = pPort - p1 * 256;
							s_port = p1 + "," + p2;
						}
						String replyInfo = "227 Entering Passive Mode ("
								+ InetAddress.getLocalHost().getHostAddress()
										.replace('.', ',') + "," + s_port + ")";
						reply(replyInfo);
					} else if (command.startsWith("EPSV")) {
						if (pSocket != null)
							pSocket.close();
						pSocket = new ServerSocket(0);
						int portNum = pSocket.getLocalPort();
						String replyInfo = "229 Entering Extended Passive Mode (|||"
								+ String.valueOf(portNum) + "|)";
						reply(replyInfo);
					} else if (command.startsWith("QUIT")) {
						reply("221 Goodbye.");
						controlScoket.close();
						conis.close();
						conos.close();
						break;
					} else if (command.startsWith("RETR ")) {
						String relativeFilePath = getCommandParam(command);
						String absolutefilePath = currentDir
								+ (currentDir.endsWith("/") ? "" : "/")
								+ relativeFilePath;

						Socket dataSocket = pSocket.accept(); //

						// OutputStream os = dataSocket.getOutputStream();
						DataOutputStream dos = new DataOutputStream(
								new BufferedOutputStream(
										dataSocket.getOutputStream()));
						try {
							File file = new File(absolutefilePath);
							if (file.exists()) {
								reply("150 Opening BINARY mode data connection.");

								FileInputStream fileInputStream = new FileInputStream(
										file);
								BufferedInputStream fis = new BufferedInputStream(
										fileInputStream);
								byte[] buffer = new byte[BUFFER_SIZE];
								int len = -1;
								while ((len = (fis.read(buffer,0,buffer.length))) != -1) {
									dos.write(buffer,0,len);
								}
								dos.flush();
								System.out.println("Download file:"
										+ absolutefilePath);
								fileInputStream.close();
								try {
									Thread.sleep(100);
								} catch (InterruptedException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								reply("226 File send OK.");
							} else {
								reply("550 ERROR: File not found or access denied.");
							}

						} catch (FileNotFoundException e) {
							reply("550 ERROR: File not found or access denied.");
							e.printStackTrace();
						} finally {
							dos.close();
							dataSocket.close();
							pSocket.close();
						}

					} else if (command.startsWith("STOR ")) {
						
						String relativeFilePath = getCommandParam(command);
						String absolutefilePath = currentDir
								+ (currentDir.endsWith("/") ? "" : "/")
								+ relativeFilePath;
						Socket dataSocket = pSocket.accept();
						reply("150 Opening BINARY mode data connection.");
						BufferedInputStream bis = new BufferedInputStream(
								dataSocket.getInputStream());

						FileOutputStream fos = new FileOutputStream(
								absolutefilePath);
						byte[] buffer = new byte[BUFFER_SIZE];
						int len = -1;
						while ( (len = bis.read(buffer,0,buffer.length))!= -1) {
							fos.write(buffer,0,len);
						}
						
						fos.flush();
						fos.close();
						bis.close();
						dataSocket.close();
						pSocket.close();
						System.out.println("Upload to :" + absolutefilePath);
						try {
							Thread.sleep(100);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						reply("226 transfer complete.");

					} else {
						reply("500 invalid command");
						continue;
					}

				}//end while
			}catch (Exception e){
				conis.close();
				conos.close();
				pSocket.close();
				System.out.println(IP+"exception quit");
			}
			

		} catch (IOException e) {
			try {
				if (controlScoket != null)
					controlScoket.close();
				if (conis != null)
					conis.close();
				if (conos != null)
					conos.close();

			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}

	}

	void reply(String msg) {
		
		try {
			conos.write((msg + "\r\n").getBytes());
			/*
			 * conos.write(msg.getBytes()); conos.write("\n".getBytes())
			 */;
			conos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	String getCommand() {
		BufferedReader br = new BufferedReader(new InputStreamReader(conis));
		String s = "";
		//byte[] buffer = new byte[BUFFER_SIZE];
		//int length = 0;
		try {
			s = br.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return s;
		//return new String(buffer, 0, length);
	}

	String getCommandParam(String command) {
		
		String param = "";
		if (command.startsWith("RETR ")) {
			param = command.substring(command.indexOf("RETR ") + 5).trim();
		} else if (command.startsWith("STOR ")) {
			param = command.substring(command.indexOf("STOR ") + 5).trim();
		}
		return param;
	}

}