package com.rdz.client;

import com.rdz.common.InfoStruct;
import com.rdz.common.LinkType;
import com.rdz.common.MessageType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.UUID;

public class BIOClient {
    public static void main(String[] args) throws IOException {
        BIOClient client = new BIOClient();
        client.startClient(6666, "Saber");
    }

    public void startClient(int port, String clientName) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(port));

        String clientID = UUID.randomUUID().toString().replace("-", "");
        ByteBuffer buff = InfoStruct.getByteBuff(LinkType.INIT, MessageType.TEXT, clientID+InfoStruct.BREAK_STR+clientName);
        socket.getOutputStream().write(buff.array());

        new Thread(()->{
            int length = 0, pos = -1;
            String linkType = "", contType = "";
            while (true) {
                byte[] bt = new byte[4];
                try {
                    //读取连接类型
                    socket.getInputStream().read(bt);
                    linkType = new String(bt);
                    //读取内容类型
                    socket.getInputStream().read(bt);
                    contType = new String(bt);

                    if(LinkType.MESSAGE.getStr().equals(linkType)) {
                        //读取内容长度
                        socket.getInputStream().read(bt);
                        length = InfoStruct.byte2Int(bt);
                        //读取内容
                        if(length <= 4096) {
                            bt = new byte[length];
                            socket.getInputStream().read(bt);
                            System.out.println(new String(bt));
                        } else {
                            bt = new byte[4096];
                            while((pos = socket.getInputStream().read(bt))!=-1) {
                                System.out.println(new String(bt));
                            }
                        }
                    } else if (LinkType.CLOSE.getStr().equals(linkType)) {
                        System.exit(0);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Scanner sc = new Scanner(System.in);
        System.out.println("客户端启动成功，请输入内容:");
        while(true) {
            String next = sc.next();
            if("/close".equals(next)) {
                buff = InfoStruct.getByteBuff(LinkType.CLOSE, MessageType.TEXT, clientID+InfoStruct.BREAK_STR+clientName);
            } else {
                buff = InfoStruct.getByteBuff(LinkType.MESSAGE, MessageType.TEXT, clientID+InfoStruct.BREAK_STR+next);
            }
            socket.getOutputStream().write(buff.array());
        }
    }
}
