package com.basement.mark.testapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;

import java.security.KeyStore;
import java.util.UUID;


public class MainActivity extends Activity {
    //public final static String EXTRA_MESSAGE = "com.basement.mark.testapp.MESSAGE";

    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    // AWS connection settings
    //IoT Endpoint
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a35dhtz2sdce3t.iot.us-east-1.amazonaws.com";
    // Cognito Pool ID
    private static final String COGNITO_POOL_ID = "us-east-1:cdb647c6-0997-46c3-b038-7fd55ee1cb15";
    // Name of AWS IoT Policy
    private static final String AWS_IOT_POLICY_NAME = "MarksDevPCPolicy";
    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_EAST_1;
    // Filename of KeyStore file on file system
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the Keystore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    // Text Views
    TextView tvClientId;
    TextView tvStatus;
    TextView tvLocker;

    // Buttons
    Button btnConnect;
    Button btnDisconnect;
    Button btnOpen;

    // AWS Variables
    AWSIotClient mIoTAndroidClient;
    AWSIotMqttManager mqttManager;
    String clientId;
    String keystorePath;
    String keystoreName;
    String keystorePw;

    KeyStore clientKeyStore = null;
    String certificateId;

    CognitoCachingCredentialsProvider credentialsProvider;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        tvClientId = (TextView) findViewById(R.id.tvClientId);
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvLocker = (TextView) findViewById(R.id.tvLocker);
        
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(connectClick);
        btnConnect.setEnabled(false);
        
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(disconnectClick);
        // btnDisconnect.setEnabled(false);
        
        btnOpen = (Button) findViewById(R.id.btnOpen);
        btnOpen.setOnClickListener(openClick);
        // btnOpen.setEnabled(false);
        
        // Create a unique MQTT client ID. 
        clientId = UUID.randomUUID().toString();
        tvClientId.setText(clientId);
        
        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );
        
        Region region = Region.getRegion(MY_REGION);
        
        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);
        
        // Set keepalive to 10 seconds. Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);
        
        // Set last Will and Testament for MQTT. On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic", 
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);
        
        // Iot Client (for creation of certificate if needed)
        mIoTAndroidClient = new AWSIotClient(credentialsProvider);
        mIoTAndroidClient.setRegion(region);
        
        keystorePath = getFilesDir().getPath();
        keystoreName = "awsIotKey";
        keystorePw = "master";
        certificateId = "testapp";
        
        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)){
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePw)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                    + " found in keystore - using for MQTT.");
                    // Load Keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePw);
                    btnConnect.setEnabled(true);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                } 
            } else { 
                Log.i(LOG_TAG, "keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occured retrieving cert/key from keystore.", e);
        }
        
        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/Key was not found in keystore - creating new Key and Certificate.");
            new Thread((Runnable) () -> {
                try {
                    // Create a new private key and certificate. This call
                    // creates both on the server and returns them to the 
                    // device.
                    CreateKeysAndCertificateRequest createKeysAndCertificateRequest = 
                            new CreateKeysAndCertificateRequest();
                    createKeysAndCertificateRequest.setSetAsActive(true);
                    final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                    createKeysAndCertificateResult = 
                            mIoTAndroidClient.createKeysAndCertificate(createKeysAndCertificateRequest);
                    Log.i(LOG_TAG,
                            "Cert ID: " +
                                    createKeysAndCertificateResult.getCertificateId() +
                                    " create.");
                    // store in keystore for use in MQTT client
                    // saved as alias "default" so a new certificate
                    //





                }
            }
        })
        }





   /* public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    } */

}
