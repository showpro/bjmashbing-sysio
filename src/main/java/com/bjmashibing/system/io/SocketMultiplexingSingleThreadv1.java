package com.bjmashibing.system.io;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 多路复用器 单线程版本
 */
public class SocketMultiplexingSingleThreadv1 {

    //坦克 一期 二期
    private ServerSocketChannel server = null;
    // Java中Selector 是对Linux 多路复用器的封装
    private Selector selector = null; // Linux 多路复用器（select poll   epoll kqueue） nginx  envent{}
    int port = 9090;

    //初始化
    public void initServer() {
        try {
            // 服务端三部曲
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            //如果在epoll模型下，open 就完成了-> epoll_creat的创建 -> 返回一个fd3
            selector = Selector.open();  //  select  poll  *epoll 优先选择：epoll 但是可以 -D修正

            //server 约等于 listen状态的fd4
            /**
             register  注册到多路复用器上，且接收
             如果：
             select, poll: register行为是在jvm里开辟一个数组 将 fd4 放进去
             epoll: 上面open的时候已经开辟了一个空间fd3，这个时候完成 epoll_ctl(fd3,ADD,fd4,EPOLLIN)
             */
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initServer();
        System.out.println("服务器启动了。。。。。");
        try {
            //当前一个线程
            while (true) {//死循环
                Set<SelectionKey> keys = selector.keys();
                System.out.println(keys.size()+"   size");

                //1、调多路复用器（select,poll  or  epoll(调epoll_wait)）
                /*
                Java中调用selector.select()是啥意思：
                1，select，poll  其实  调内核的select（fd4）  poll(fd4)
                2，epoll：  其实 调内核的 epoll_wait()
                *, 参数可以带时间：没有时间，或者0  ：  阻塞，有时间设置一个超时
                selector.wakeup()  结果返回0

                懒加载：
                其实再触碰到selector.select()调用的时候触发了epoll_ctl的调用

                 */
                while (selector.select(500) > 0) {//是否建立连接返回事件
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();//返回的有状态的fd集合
                    Iterator<SelectionKey> iter = selectionKeys.iterator();
                    //so，管你啥多路复用器，你呀只能给我状态，我还得一个一个的去处理他们的R/W。同步好辛苦！！！！！！！！
                    //  NIO  自己对着每一个fd调用系统调用，浪费资源，那么你看，这里是不是调用了一次select方法，知道具体的那些可以R/W了？
                    //是不是很省力？
                    //我前边刻意强调过，socket有两种：  listen   通信 R/W
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove(); //set  不移除会重复循环处理
                        if (key.isAcceptable()) {
                            //看代码的时候，这里是重点，如果要去接受一个新的连接
                            //语义上，accept接受连接且返回新连接的FD对吧？
                            //那新的FD怎么办？
                            //如果是select，poll，因为他们内核没有开辟空间，那么在jvm中保存和前边的fd4那个listen的一起
                            //如果是epoll： 我们希望通过epoll_ctl把新的客户端fd注册到内核空间
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            readHandler(key);
                            //在当前线程，这个方法可能会阻塞，如果阻塞10年，其他的IO早就没电了。。。
                            //所以，为什么提出了 IO THREADS
                            //redis 是不是也用了epoll, redis是不是有个io threads的概念，redis是不是单线程的
                            // tomcat 8，9 版本以后，提出了异步处理方式 IO  和   处理上解耦
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept();//来啦，目的是调用accept接受客户端  fd7
            client.configureBlocking(false);

            ByteBuffer buffer = ByteBuffer.allocate(8192); //前边讲过了

            // 0.0  我类个去
            //你看，调用了register
            /*
            select，poll：jvm里开辟一个数组 fd7 放进去
            epoll：  epoll_ctl(fd3,ADD,fd7,EPOLLIN
             */
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("-------------------------------------------");
            System.out.println("新客户端：" + client.getRemoteAddress());
            System.out.println("-------------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readHandler(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int read = 0;
        try {
            while (true) {
                read = client.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (read == 0) {
                    break;
                } else {
                    client.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SocketMultiplexingSingleThreadv1 service = new SocketMultiplexingSingleThreadv1();
        service.start();
    }
}
