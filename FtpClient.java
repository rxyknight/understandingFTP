import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * This is a passive FTP Client
 * 
 * @author Xiangyu Ren Test example: pub/apache/favicon.ico
 */
public class FtpClient {

	private static final int BUFFER_SIZE = 4096;

	private String host;
	private String savePath = "/Users/royren/Desktop";
	private Socket conSocket;
	private InputStream conis;
	private OutputStream conos;
	private BufferedReader br;

	private Socket datSocket;
	private InputStream datis;
	private OutputStream datos;

	private static Scanner scanner = new Scanner(System.in);

	public FtpClient() {
		savePath = System.getProperties().getProperty("user.dir");
	}
	public static void main(String[] args) {
		// System.out.println(System.getProperty("user.dir"));
		FtpClient ftpClient = new FtpClient();

		ftpClient.establish();

		while (true) {
			System.out.print("ftp>");
			String command = scanner.nextLine().trim();
			ftpClient.executeCommand(command);

		}

	}

	private void changeDirectory(String path) throws IOException {
		commandSender("CWD " + path + "\n");
		System.out.println(getReply());

	}

	private void showList() throws IOException {
		// 设置被动模式
		commandSender("EPSV\n");
		String feedback = getReply();
		System.out.println(feedback);

		// 取得FTP被动监听的data port
		String dataPortStr = feedback.substring(feedback.indexOf("(|||") + 4,
				feedback.indexOf("|)"));
		// 监听端口
		datSocket = new Socket(host, Integer.parseInt(dataPortStr));
		datis = datSocket.getInputStream();
		// 发送LIST指令
		commandSender("LIST\n");
		feedback = getReply();
		System.out.println(feedback);
		// 显示列表
		if (!feedback.contains("500")) {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = -1;

			while ((bytesRead = datis.read(buffer)) != -1) {
				System.out.write(buffer, 0, bytesRead);
			}

			System.out.flush();
			System.out.println(getReply());
		}

		datis.close();
		datSocket.close();

	}

	void upload(String param) {
		String absolutefilePath = savePath
				+ (savePath.endsWith("/") ? "" : "/") + param;
		File file = new File(absolutefilePath);
		if (!file.exists()) {
			System.out.println("No such file.");
			return;
		}
		try {
			// 设置被动模式
			commandSender("EPSV\n");
			String feedback = getReply();
			System.out.println(feedback);

			// 取得FTP被动监听的data port
			String dataPortStr = feedback.substring(
					feedback.indexOf("(|||") + 4, feedback.indexOf("|)"));
			// 监听端口
			if (datSocket != null)
				datSocket.close();
			datSocket = new Socket(host, Integer.parseInt(dataPortStr));
			if (datos != null)
				datos.close();
			datos = datSocket.getOutputStream();
			// 上传
			String store = "STOR " + param + "\n";
			commandSender(store);
			feedback = getReply();
			System.out.println(feedback);
			
			if (feedback.contains("150")) {
				DataOutputStream dos = new DataOutputStream(
						new BufferedOutputStream(datos));
				FileInputStream fileInputStream = new FileInputStream(file);
				byte[] buffer = new byte[BUFFER_SIZE];
				int length = -1;
				while ((length = fileInputStream.read(buffer)) != -1) {
					dos.write(buffer, 0, length);
				}

				dos.flush();
				dos.close();
				fileInputStream.close();
				System.out.println("upload done");
				System.out.println(getReply());

			}
			
			datos.close();
			datSocket.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	void download(String filePath) throws IOException {

		// 设置被动模式
		commandSender("EPSV\n");
		String feedback = getReply();
		System.out.println(feedback);
		//System.out.println("test1");//test
		// 取得FTP被动监听的data port
		String dataPortStr = feedback.substring(feedback.indexOf("(|||") + 4,
				feedback.indexOf("|)"));
		// 监听端口
		if (datSocket != null)
			datSocket.close();
		datSocket = new Socket(host, Integer.parseInt(dataPortStr));
		if (datis != null)
			datis.close();
		datis = datSocket.getInputStream();
		// 发送下载指令
		commandSender("REST 0\n");
		System.out.println(getReply());
		//System.out.println("test2");//test
		String retrive = "RETR " + filePath + "\n";
		commandSender(retrive);
		feedback = getReply();
		System.out.println(feedback);
		//System.out.println("test3");//test
		if (feedback.contains("150")) {
			// 开始下载
			String[] strArray = filePath.split("/");
			String fileName = strArray[strArray.length - 1];
			String absolutefilePath = savePath
					+ (savePath.endsWith("/") ? "" : "/") + fileName;
			// System.out.println(absolutefilePath);
			FileOutputStream fileOutputStream = new FileOutputStream(
					absolutefilePath);
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead = -1;
			while ((bytesRead = datis.read(buffer)) != -1) {
				fileOutputStream.write(buffer, 0, bytesRead);
			}
			fileOutputStream.close();
			System.out.println(getReply());
			//System.out.println("test4");//test
			// commandSender(""); // don't know why!? WTF
		}

		datis.close();
		datSocket.close();

	}

	void executeCommand(String command) {

		try {

			if (command.trim().equals("ls")) {

				showList();
			} else if (command.substring(0, 2).equals("cd")) {
				String param = command.substring(command.indexOf("cd ") + 3)
						.trim();
				changeDirectory(param);
			} else if (command.substring(0, 3).equals("put")) {
				String param = command.substring(command.indexOf("put ") + 4)
						.trim();
				// System.out.println(param);
				upload(param);

			} else if (command.substring(0, 3).equals("get")) {
				String param = command.substring(command.indexOf("get ") + 4)
						.trim();
				download(param);
			} else if (command.trim().equals("quit")) {
				quit();
			} else {
				System.out.println("Command invalid");
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void quit() {
		try {
			commandSender("QUIT\n");
			if (conis != null)
				conis.close();
			if (conos != null)
				conos.close();
			if (datis != null)
				datis.close();
			if (conSocket != null)
				conSocket.close();
			if (datSocket != null)
				datSocket.close();
			if(br !=null)
				br.close();
			System.exit(0);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	void establish() {
		
		//host = "mirror.aarnet.edu.au";
		//host = "127.0.0.1";
		//host = "10.19.239.137";
		// System.out.println("URL: " + host);
		
		 System.out.print("Please input the removal address:");
		 host = scanner.nextLine();
		 
		// 连接服务器
		try {
			conSocket = new Socket(host, 21);
			// 与控制port 21 相连
			conis = conSocket.getInputStream();
			conos = conSocket.getOutputStream();
			br = new BufferedReader(new InputStreamReader(conis));
			// 得到连接后反馈信息
			
			System.out.println(getReply());

			// 发送用户名
			commandSender("USER anonymous\n");
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String s = getReply();
			System.out.println(s);

			// 发送密码
			if (s.contains("331")) {
				commandSender("PASS\n");
				System.out.println(getReply());
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	void commandSender(String command) {

		try {
			conos.write(command.getBytes());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	String getReply() {
		/*BufferedReader br = new BufferedReader(new InputStreamReader(conis));
		String s = "";
		try {
			s = br.readLine();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return s;*/
		String s = "";
		byte[] buffer = new byte[BUFFER_SIZE];
		int length = -1;
		try {
			//while(true){
				/*try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}*/
			//	int len = conis.available();
			//	System.out.println(len);
			//	if(len>0)
			//		break;
				
			//}
			
			length = conis.read(buffer);
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		s = new String(buffer, 0, length).trim();
		return s;
	}
}