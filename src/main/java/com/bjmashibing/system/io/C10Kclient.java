package com.bjmashibing.system.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

/**
 * @author: 马士兵教育
 * @create: 2020-06-06 15:12
 *
 * C10K问题：1万个连接
 * 通过端口号65535的限制，模拟windows下单机单进程对一台服务器发起11万次连接
 * 在运行过程中会出现问题：linux服务端【192.168.150.11】返回的时候只有windows虚拟机的网卡【192.168.150.1】能接收，windows不能接收，
 * 因为192.168.150.11 和  192.168.150.1 都在 192.168.150.0网段，而192.168.150.11 和 192.168.110.100 不在同一个网段
 * 因此需要手动添加路由 route add -host 192.168.110.100 gw 192.168.150.1
 */
public class C10Kclient {

    public static void main(String[] args) {
        LinkedList<SocketChannel> clients = new LinkedList<>();
        //要连接的服务端地址和端口号
        InetSocketAddress serverAddr = new InetSocketAddress("192.168.150.11", 9090);

        //端口号的问题：2个字节大小 65535
        //  windows 11W个连接
        for (int i = 10000; i < 65000; i++) {
            try {
                //每循环1次，准备两个客户端
                SocketChannel client1 = SocketChannel.open();

                SocketChannel client2 = SocketChannel.open();

                /*
                linux中你看到的连接就是：
                client...port: 10508
                client...port: 10508
                 */
                //客户端1绑定本地ip地址【虚拟机VMnet8网卡地址】
                client1.bind(new InetSocketAddress("192.168.150.1", i));
                //去连接服务端
                //  192.168.150.1：10000   192.168.150.11：9090
                client1.connect(serverAddr);
                clients.add(client1);

                //客户端2绑定本地ip地址【windows物理网卡地址】
                client2.bind(new InetSocketAddress("192.168.110.100", i));
                //去连接服务端
                //  192.168.110.100：10000  192.168.150.11：9090
                client2.connect(serverAddr);
                clients.add(client2);

            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        System.out.println("clients "+ clients.size());

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
