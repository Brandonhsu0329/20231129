package com.example.x; //注意

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.Arrays;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import be.tarsos.dsp.AudioDispatcher;  //注意
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.mfcc.MFCC;
import be.tarsos.dsp.util.fft.FFT;

public class MainActivity extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;
    // private MFCCCalculator mfccCalculator;
    private TextView mfccTextView;

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Audio.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            cursor.moveToFirst();
            String filePath = cursor.getString(column_index);
            cursor.close();
            return filePath;
        }

        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化 TextView
        mfccTextView = findViewById(R.id.mfccTextView);

        // 設置按鈕點擊事件
        Button loadFileButton = findViewById(R.id.loadFileButton);
        loadFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 在按鈕點擊事件中處理 MFCC 計算邏輯
                showFileChooser();
            }
        });
    }

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*"); // 接受所有音訊格式

        startActivityForResult(intent, READ_REQUEST_CODE); //注意
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == READ_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                // 獲取選擇的檔案路徑
                String filePath = getPathFromUri(data.getData());

                if (filePath != null) {
                    // 計算 MFCC 並更新 TextView
                    float[] mfccValues = calculateMFCC(filePath);
                    updateMFCCTextView(mfccValues);

                    // 在這裡添加上傳成功的處理代碼
                    showUploadSuccessMessage();
                }
            } else {
                // 處理取消選擇或其他情況
                Toast.makeText(this, "選擇取消或發生錯誤", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private float[] calculateMFCC(String filePath) {
        try {
            int sampleRate = 44100;
            int bufferSize = 1024;
            int bufferOverlap = 512;

            AudioDispatcher dispatcher = AudioDispatcherFactory.fromPipe(filePath, sampleRate, bufferSize, bufferOverlap);
            MFCC mfcc = new MFCC(bufferSize, sampleRate, 13, 40, 300, 3000);

            dispatcher.addAudioProcessor(mfcc);
//            dispatcher.addAudioProcessor(new AndroidAudioPlayer(TarsosDSPAudioFormat.toTarsosDSPFormat(TarsosDSPAudioFormat.Encoding.PCM_SIGNED, sampleRate, 16, 1, 2, sampleRate), bufferSize, bufferOverlap));

            dispatcher.run();

            return mfcc.getMFCC();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new float[0];
    }

    private void updateMFCCTextView(float[] mfccValues) {
        StringBuilder sb = new StringBuilder("MFCC Values:\n");
        for (float value : mfccValues) {
            sb.append(value).append("\n");
        }
        mfccTextView.setText(sb.toString());
        // 在這裡添加上傳成功的處理代碼
        showUploadSuccessMessage();
    }

    private void showUploadSuccessMessage() {
        // 顯示一個 Toast 消息，或者你可以使用其他方式通知用戶上傳成功
        Toast.makeText(getApplicationContext(), "上傳成功", Toast.LENGTH_SHORT).show();
    }
}

//////錄音

