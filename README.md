# FTP Server And Client By Java

##How to run
1.  Kill the 21 port

```
sudo kill -9 $(sudo lsof -t -i:21)
```
If you can't kill the 21 port and the reason is the port's PID is 1, please run the following command (For Mac user).

```
sudo -s launchctl unload -w /System/Library/LaunchDaemons/ftp.plist
```


2. Compile and run the server

```
javac FtpServer.java
sudo java FtpServer
```
If you can see the following screenshot, that means the server runs successfully.

  ![image](https://user-images.githubusercontent.com/10615153/31123955-13c0ffca-a88e-11e7-9fa1-bd1ff1c80dfb.png)

3. Compile and run the client

```
javac FtpClient.java
java FtpClient
```
4. Then, it asks you to input the removal address, please input 127.0.0.1, and if you can see the following screenshot, tht means the client runs successfully.

The Client screenshot:

  ![image](https://user-images.githubusercontent.com/10615153/31124151-e5279b28-a88e-11e7-912e-a3ba0ffe4932.png)

And the Server screenshot:

![image](https://user-images.githubusercontent.com/10615153/31124207-211a03e6-a88f-11e7-8d85-569e4c76df28.png)

