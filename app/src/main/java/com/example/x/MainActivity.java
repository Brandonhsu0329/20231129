package com.example.x; //注意

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

//    private String convertStreamToString(InputStream is) {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//        StringBuilder stringBuilder = new StringBuilder();
//        String line;
//
//        try {
//            while ((line = reader.readLine()) != null) {
//                stringBuilder.append(line).append('\n');
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                is.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        return stringBuilder.toString();
//    }

    private String getPathFromUri(Uri uri) {
        String[] projection = {MediaStore.Audio.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

//        try {
//            InputStream inputStream = getContentResolver().openInputStream(uri);
//
//            if (inputStream != null) {
//                // 將 InputStream 轉換為字串
//                String content = convertStreamToString(inputStream);
//
//                // 在這裡處理完整的內部字串
//                Log.d("Tag", "Content: " + content);
//
//                // 關閉 InputStream
//                inputStream.close();
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            // 在這裡處理檔案不存在的情況
//        } catch (IOException e) {
//            e.printStackTrace();
//            // 在這裡處理其他 IO 異常
//        }


        if (cursor != null && cursor.moveToFirst()) {
//            String filePath = getPathFromUri(uri);

            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            Log.d("Tag", "uri: "+ uri);
//            cursor.moveToFirst();
            String filePath = cursor.getString(column_index);
            Log.d("Tag", "cursor: "+ cursor);
            cursor.close();
            Log.d("Tag", "filePath: "+ filePath);
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
                Log.d("Tag", "i am here_1");
                showFileChooser();
            }
        });
    }

    private final ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
//                    Log.d("Tag", String.format("data: %s", data));
                    doSomeOperations(data);
                }
            }
    );

    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*"); // 接受所有音訊格式
        Log.d("Tag", "i am here_2");

        someActivityResultLauncher.launch(intent);
        Log.d("Tag", "i am here_3");
    }
    private void doSomeOperations() {
        // 在這裡執行你希望在 Activity 結束後執行的操作
        Log.d("Tag", "doSomeOperations executed");
        // 其他操作...
    }

    private void doSomeOperations(Intent data) {

        if (data != null) {
            // 獲取選擇的檔案路徑
//            Log.d("Tag", "data: "+ data);
            String filePath = getPathFromUri(data.getData());
//            Log.d("Tag", "fillPath: "+ getPathFromUri(data.getData()));

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

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (requestCode == READ_REQUEST_CODE) {
//            if (resultCode == RESULT_OK && data != null) {
//                // 獲取選擇的檔案路徑
//                String filePath = getPathFromUri(data.getData());
//
//                if (filePath != null) {
//                    // 計算 MFCC 並更新 TextView
//                    float[] mfccValues = calculateMFCC(filePath);
//                    updateMFCCTextView(mfccValues);
//
//                    // 在這裡添加上傳成功的處理代碼
//                    showUploadSuccessMessage();
//                }
//            } else {
//                // 處理取消選擇或其他情況
//                Toast.makeText(this, "選擇取消或發生錯誤", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

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
            Log.d("Tag", "mfcc: "+ Arrays.toString(mfcc.getMFCC()));
            return mfcc.getMFCC();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("Tag", "error123");
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

