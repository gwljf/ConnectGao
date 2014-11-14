package com.wj.LoveGao;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.io.IOException;

public class MActivity extends Activity {
    /**
     * Called when the activity is first created.
     */

    private final static String QUEUE_NAME = "Connection between W and G";
    private final static String WJ = "me";
    private final static String GLF = "she";
    Button sendBtn;
    EditText editText;
    TextView textView;
    ScrollView scrollView;
    ConnectionFactory factory;
    Connection connection;
    Channel channel;
    StringBuilder sb;
    StringBuilder recvSB;
    Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        sendBtn = (Button) findViewById(R.id.sendBtn);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        scrollView = (ScrollView) findViewById(R.id.scrollView);


        textView.setTextSize(20);
        sb = new StringBuilder();
        recvSB = new StringBuilder();
        textView.setGravity(Gravity.LEFT);
        ConnectionThread initConnection = new ConnectionThread("init");
        initConnection.start();

        handler = new Handler() {
            public void handleMessage(Message msg) {
              recvSB.delete(0, recvSB.length());

//                if(!((String) msg.obj).contains("\n"+GLF+": ")){        // for glf
//                    recvSB.append("\n"+WJ+"：");                        // for glf

              if(!((String) msg.obj).contains("\n"+WJ+": ")){        // for wj
                  recvSB.append("\n"+GLF+"：");                        // for wj
                  recvSB.append((String) msg.obj+"\n");
                  textView.append(recvSB.toString());
                  scrollView.fullScroll(View.FOCUS_DOWN);
                }
            }
        };


        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sb.delete(0, sb.length());
            //    sb.append("\n"+GLF+": "+editText.getText());            // for glf
              sb.append("\n"+WJ+": "+editText.getText());            // for wj
                try {
                    send_Msg(sb.toString());
                    textView.append(sb.toString()+"\n");
                    editText.setText("");
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    public void send_Msg(String text) throws IOException{
        channel.basicPublish("", QUEUE_NAME, null, text.getBytes());
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            channel.close();
            connection.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private class ConnectionThread extends HandlerThread{
        public ConnectionThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            try {
                factory = new ConnectionFactory();
                factory.setHost("ec2-54-67-6-99.us-west-1.compute.amazonaws.com");
                connection = factory.newConnection();
                channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                sb.delete(0, sb.length());
                RecvThread recvThread = new RecvThread("Receive");
                recvThread.start();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private class RecvThread extends HandlerThread{
        public RecvThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            try {
                QueueingConsumer consumer = new QueueingConsumer(channel);
                channel.basicConsume(QUEUE_NAME, true, consumer);

                while (true) {
                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                    String message = new String(delivery.getBody());
                    Message msg = Message.obtain();
                    msg.obj = message;
                    handler.sendMessage(msg);
                    System.out.println(message);
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
