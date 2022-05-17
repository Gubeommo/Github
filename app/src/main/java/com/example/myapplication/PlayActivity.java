package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayActivity extends AppCompatActivity {



    private int port = 2020;
    EditText inputText;
    ImageButton sendButton, finishButton;
    TextView outPutText, resultText;
    Handler handler = new Handler();
    Socket sock;
    String contents, readData, imread, youread;
    int lastIndex;
    char lastchar;
    boolean gameset = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        //UI 초기화화
        init();
        //액티비티 실행을 하자마자 서버 연결
        connectedServer();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendtoServer();

            }
        });
        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                PlayActivity.this.finish();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            sock.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();

            }
        });

    }

    public void init(){
        inputText = (EditText) findViewById(R.id.inputText);
        sendButton = (ImageButton) findViewById(R.id.sendButton);
        outPutText = (TextView) findViewById(R.id.outputTextView);
        outPutText.setText("라디오");
        finishButton = (ImageButton) findViewById(R.id.container);
        resultText = (TextView) findViewById(R.id.resultTextView);

    }

    public void connectedServer(){
        String data = outPutText.getText().toString();
        Toast.makeText(getApplicationContext(), "서버에 연결됨." , Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    //소켓연결
                    sock = new Socket("172.17.0.2", port);
                    //서버 연결 메세지 전송을 위한 outputstream
                    OutputStream os = sock.getOutputStream();

                    //byte ordering을 위한 byteBuffer 선언
                    ByteBuffer sendByteBuffer = null;

                    sendByteBuffer = ByteBuffer.allocate(100);
                    sendByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                    sendByteBuffer.put(data.getBytes());
                    sendByteBuffer.put(new byte[data.getBytes().length + 1]);
                    //outputstream객체를 통해 write
                    os.write(sendByteBuffer.array());
                    os.flush();

                    //서버에서 메세지를 받기 위해 inputstream객체 선언
                    BufferedInputStream bis = new BufferedInputStream(sock.getInputStream());
                    byte[] buff = new byte[1024];

                    int read = 0;

                    while(true){
                        if(sock == null)
                            break;

                        read = bis.read(buff,0,1024);

                        if(read < 0)
                            break;

                        byte[] tempArr = new byte[read];
                        System.arraycopy(buff, 0, tempArr, 0, read);
                        readData = new String(tempArr);
                        readData = readData.substring(0,3);
                        printClientLog(readData);

                        youread = readData.substring(0,2);
                        String win = "승리";
                        if(youread == null || youread.equals(win)){
                            printResultLog("승리");
                        }


                        System.out.println("연결요청 : read : " + youread);
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void sendtoServer(){
        //데이터 전송 담당하는 메소드
        final String data = inputText.getText().toString(); //정답입력

        contents = outPutText.getText().toString();
        lastIndex = contents.length() -1;
        lastchar = contents.charAt(lastIndex);
        System.out.println(contents);

        //서버에게 데이터를 전송한다.
        if(data.charAt(0) != lastchar){
            gameset = false;
            Toast.makeText(getApplicationContext(), "졌습니다. ",Toast.LENGTH_SHORT).show();
            //게임결과메소드 호출
            resultGame("lose");
            System.out.println(contents);
        }
        else if(data.length() != 3){
            Toast.makeText(getApplicationContext(), "3글자만 입력해주세요.", Toast.LENGTH_LONG).show();
        }
        else{
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        OutputStream os = sock.getOutputStream();

                        ByteBuffer sendByteBuffer = null;

                        sendByteBuffer = ByteBuffer.allocate(100);
                        sendByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                        sendByteBuffer.put(data.getBytes());
                        sendByteBuffer.put(new byte[data.getBytes().length + 1]);

                        os.write(sendByteBuffer.array());
                        os.flush();

                        BufferedInputStream bis = new BufferedInputStream(sock.getInputStream());
                        byte[] buff = new byte[1024];

                        int read = 0;

                        while(true){
                            if(sock == null)
                                break;

                            read = bis.read(buff,0,1024);

                            if(read < 0)
                                break;

                            byte[] tempArr = new byte[read];
                            System.arraycopy(buff, 0, tempArr, 0, read);
                            readData = new String(tempArr);
                            readData = readData.substring(0,3);
                            printClientLog(readData);
                            System.out.println("송수신 : read : " + readData);
                            youread = readData.substring(0,2);
                            String win = "승리";
                            System.out.println("두글자바꾼거 : " + youread);
                            if(youread == null || youread.equals(win)){
                                printResultLog("승리");
                            }
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }).start();

        }


    }

    public void resultGame(String msg){
        final String resultMsg = msg;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    OutputStream os = sock.getOutputStream();

                    ByteBuffer sendByteBuffer = null;

                    sendByteBuffer = ByteBuffer.allocate(100);
                    sendByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

                    sendByteBuffer.put(resultMsg.getBytes());
                    sendByteBuffer.put(new byte[resultMsg.getBytes().length + 1]);

                    os.write(sendByteBuffer.array());
                    os.flush();

                    BufferedInputStream bis = new BufferedInputStream(sock.getInputStream());
                    byte[] buff = new byte[1024];

                    int read = 0;

                    while(true){
                        if(sock == null)
                            break;

                        read = bis.read(buff,0,1024);

                        if(read < 0)
                            break;


                        byte[] tempArr = new byte[read];
                        System.arraycopy(buff, 0, tempArr, 0, read);
                        imread = new String(tempArr);
                        imread = imread.substring(0,2);
                        printResultLog(imread);



                        System.out.println("결과 : read : " + imread);


                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();


    }


    public void printClientLog(final String data){
        handler.post(new Runnable() {
            @Override
            public void run() {
                outPutText.setText("");
                outPutText.setText(data);


            }
        });
    }
    public void printResultLog(final String data){

        handler.post(new Runnable() {
            @Override
            public void run() {
                outPutText.setText("");
                resultText.setText(data);
                if(data == "승리"){
                    showDialog("승리");
                }else{
                    showDialog("패배");
                }

            }
        });
    }
    public void showDialog(String data) {


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(data);
        System.out.println(data);
        builder.setMessage("게임을 다시 하시겠습니까?");
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        builder.setNegativeButton("아니오",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        PlayActivity.this.finish();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    sock.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();

                    }
                });
        builder.show();
    }

}