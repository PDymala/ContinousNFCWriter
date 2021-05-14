package com.diplabels.continousnfcwriter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;


public class ReadActivity extends AppCompatActivity  {
    Button buttonExit;
    Button writeActivity;
    TextView textviewRead;
    Handler handler = new Handler();
    private NfcAdapter mNfcAdapter;
    public static final String MIME_TEXT_PLAIN = "text/plain";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        if (!isNfcSupported()) {
            Toast.makeText(this, "Nfc is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC disabled on this device. Turn on to proceed", Toast.LENGTH_SHORT).show();
        }
        permission();
        init();
    }
    @Override
    protected void onNewIntent(Intent intent) {
        // also reading NFC message from here in case this activity is already started in order
        // not to start another instance of this activity
        super.onNewIntent(intent);
        receiveMessageFromDevice(intent);
    }
    private boolean isNfcSupported() {
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        return this.mNfcAdapter != null;
    }

    private void permission() {

        if (ContextCompat.checkSelfPermission(ReadActivity.this, Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ReadActivity.this, new String[]{
                            Manifest.permission.NFC
                    },
                    100);

        }
        if (ContextCompat.checkSelfPermission(ReadActivity.this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ReadActivity.this, new String[]{
                            Manifest.permission.VIBRATE
                    },
                    100);

        }

    }

    public void init() {
        buttonExit = findViewById(R.id.buttonExit);
        writeActivity = findViewById(R.id.buttonWrite);
        textviewRead = findViewById(R.id.textViewRead);

    }

    public void exitButton(View v) {
        this.finishAffinity();
    }

    public void writeButton(View v) {
        Intent myIntent = new Intent(this, WriterActivity.class);
        this.startActivity(myIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableForegroundDispatch(this, this.mNfcAdapter);
        receiveMessageFromDevice(getIntent());
    }


    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatch(this, this.mNfcAdapter);
    }


    public void enableForegroundDispatch(AppCompatActivity activity, NfcAdapter adapter) {

        // here we are setting up receiving activity for a foreground dispatch
        // thus if activity is already started it will take precedence over any other activity or app
        // with the same intent filters


        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        //
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (IntentFilter.MalformedMimeTypeException ex) {
            throw new RuntimeException("Check your MIME type");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }


    private void receiveMessageFromDevice(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            NdefMessage inNdefMessage = (NdefMessage) parcelables[0];
            NdefRecord[] inNdefRecords = inNdefMessage.getRecords();
            NdefRecord ndefRecord_0 = inNdefRecords[0];

            String inMessage = new String(ndefRecord_0.getPayload());


           handler.post(new Runnable() {
               @Override
               public void run() {
                   textviewRead.setText(inMessage.substring(3));
               }
           }) ;
            // !!!



        }
    }
    public void disableForegroundDispatch(final AppCompatActivity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

}



