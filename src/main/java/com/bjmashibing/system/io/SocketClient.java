package com.bjmashibing.system.io;

import java.io.*;
import java.net.Socket;

/**
 * @author: 马士兵教育
 * @create: 2020-05-17 16:18
 *
 * 客户端
 */
public class SocketClient {

    public static void main(String[] args) {

        try {
            //要连接的机器和端口号
            Socket client = new Socket("192.168.150.11",9090);
            //发送缓冲区大小20个字节
            client.setSendBufferSize(20);
            //是否开启TCP优化，false:是，true:否，数据不会通过缓冲区
            client.setTcpNoDelay(true);
            //和服务器建立连接后，从连接中拿到一个输出流
            OutputStream out = client.getOutputStream();

            InputStream in = System.in;
            //将输入流进行包装
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            while(true){
                String line = reader.readLine();
                if(line != null ){
                    byte[] bb = line.getBytes();
                    for (byte b : bb) {
                        out.write(b);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
