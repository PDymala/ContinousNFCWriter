package com.diplabels.continousnfcwriter;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import java.io.IOException;
/**
 * Application for continuous writing NFC TAGS. There's also a reader activity.
 * It is hard coded to write as EN language and text type of code. If others, like URL, has to be changed.
 *
 * @author Piotr Dymala p.dymala@gmail.com
 * @version 1.0
 * @since 2021-05-14
 */

public class WriterActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {
    Handler handler = new Handler();
    private NfcAdapter mNfcAdapter;
    EditText editTextNumber;
    EditText editTextTextPrefix;
    EditText editTextTextSufix;
    Switch switchRun;
    Switch switchLock;
    Switch switchDynamic;
    Button buttonUp;
    Button buttonDown;
    Button buttonExit;
    Button readActivity;
    private int number = 0;
    private String prefix = "-";
    private String sufix = "-";


    // to be noted:
// - theres keep screen on in XML
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_writer);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        permission();
        init();
    }


    public void init() {
        editTextNumber = findViewById(R.id.editTextNumber);
        editTextTextPrefix = findViewById(R.id.editTextTextPrefix);
        editTextTextSufix = findViewById(R.id.editTextTextSufix);
        switchRun = findViewById(R.id.switchRun);
        switchLock = findViewById(R.id.switchLock);
        switchDynamic = findViewById(R.id.switchDynamic);
        buttonUp = findViewById(R.id.buttonUp);
        buttonDown = findViewById(R.id.buttonDown);
        buttonExit = findViewById(R.id.buttonExit);
        readActivity = findViewById(R.id.buttonWrite);

    }


    private void permission() {

        if (ContextCompat.checkSelfPermission(WriterActivity.this, Manifest.permission.NFC) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(WriterActivity.this, new String[]{
                            Manifest.permission.NFC
                    },
                    100);

        }
        if (ContextCompat.checkSelfPermission(WriterActivity.this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(WriterActivity.this, new String[]{
                            Manifest.permission.VIBRATE
                    },
                    100);

        }

    }

    public void upNumber(View v){
        number++;
        updateNumber();
    }
    public void downNumber(View v){
        number--;
        updateNumber();
    }
    public void updateNumber(){

        handler.post(new Runnable() {
            @Override
            public void run() {
                editTextNumber.setText(Integer.toString(number));
            }
        });


    }

    public void exitButton(View v){
        switchRun.setChecked(false);
        this.finishAffinity();
    }

    public void readButton(View v){
        switchRun.setChecked(false);
        Intent myIntent = new Intent(this, ReadActivity.class);
        this.startActivity(myIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {
            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        switchRun.setChecked(false);
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this);
    }

    // This method is run in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    public void onTagDiscovered(Tag tag) {

        if (switchRun.isChecked()) {

            // Read and or write to Tag here to the appropriate Tag Technology type class
            // in this example the card should be an Ndef Technology Type
            Ndef mNdef = Ndef.get(tag);

            // Check that it is an Ndef capable card
            if (mNdef != null) {



                //!!!!! jeżeli miał by być URL zapisywany, wtedy należy go poprawnie zakodować. Zmienić typ danych etc.
                //https://stackoverflow.com/questions/52942151/how-to-properly-encode-a-url-onto-an-nfc-tag
                // na razie idzie to jako text/plain, format NFC Forum type 2

                // Or if we want to write a Ndef message
                String fulltext =  editTextTextPrefix.getText().toString() + editTextNumber.getText().toString() + editTextTextSufix.getText().toString();
                // Create a Ndef Record
                NdefRecord mRecord = NdefRecord.createTextRecord("en", fulltext);



                // Add to a NdefMessage
                NdefMessage mMsg = new NdefMessage(mRecord);

                // Catch errors
                try {
                    mNdef.connect();
                    mNdef.writeNdefMessage(mMsg);

                    // Success if got to here
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(),
                                "NFC OK - written: "+ fulltext,
                                Toast.LENGTH_SHORT).show();
                    });

                    // Make a Sound
                    try {
                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(),
                                notification);
                        r.play();
                    } catch (Exception e) {
                        // Some error playing sound
                    }

                } catch (FormatException e) {
                    // if the NDEF Message to write is malformed
                } catch (TagLostException e) {
                    // Tag went out of range before operations were complete
                } catch (IOException e) {
                    // if there is an I/O failure, or the operation is cancelled
                } finally {


                    //lock the TAG!
                    if (switchLock.isChecked()) {
                        if (mNdef != null) {
                            try {
                                mNdef.makeReadOnly();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }


                    if (switchDynamic.isChecked()){
                        number++;
                        updateNumber();
                    }


                    // Be nice and try and close the tag to
                    // Disable I/O operations to the tag from this TagTechnology object, and release resources.
                    try {
                        mNdef.close();
                    } catch (IOException e) {
                        // if there is an I/O failure, or the operation is cancelled
                    }
                }

            }

        }
    }
}
