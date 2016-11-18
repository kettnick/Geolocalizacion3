package unitec.org.geolocalizacion3;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

/**
 * Getting the Location Address.
 *
 * Clase que usa el Geocoder {@link android.location.Geocoder}
 *.
 *
 */
public class MainActivity extends ActionBarActivity implements
        ConnectionCallbacks, OnConnectionFailedListener {

    protected static final String TAG = "main-activity";

    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";


    protected GoogleApiClient mGoogleApiClient;


    protected Location mLastLocation;


    protected boolean mAddressRequested;

    protected String mAddressOutput;


    private AddressResultReceiver mResultReceiver;

    protected TextView mLocalizacionTextView;

    ProgressBar mProgressBar;


    Button mFetchAddressButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultReceiver = new AddressResultReceiver(new Handler());

        mLocalizacionTextView = (TextView) findViewById(R.id.loclizacion_direccion);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mFetchAddressButton = (Button) findViewById(R.id.fetch_address_button);

        // Ajustamos los valores de defecto.
        mAddressRequested = false;
        mAddressOutput = "";
        updateValuesFromBundle(savedInstanceState);

        updateUIWidgets();
        buildGoogleApiClient();
    }

    /**
     * Actualizamos los campos
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {

            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }

            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                displayAddressOutput();
            }
        }
    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }


    public void fetchAddressButtonHandler(View view) {

        if (mGoogleApiClient.isConnected() && mLastLocation != null) {
            startIntentService();
        }

        mAddressRequested = true;
        updateUIWidgets();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }


    @Override
    public void onConnected(Bundle connectionHint) {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            //LOCALIZAMOS CON EL GEOCODER
            if (!Geocoder.isPresent()) {
                Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
                return;
            }

            if (mAddressRequested) {
                startIntentService();
            }
        }
    }


    protected void startIntentService() {

        Intent intent = new Intent(this, FetchIntentAddressService.class);


        intent.putExtra(Constants.RECEIVER, mResultReceiver);


        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);


        startService(intent);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

        Log.i(TAG, "Conexion fallida: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {

        Log.i(TAG, "Conexion suspendida");
        mGoogleApiClient.connect();
    }


    protected void displayAddressOutput() {
        mLocalizacionTextView.setText(mAddressOutput);
    }


    private void updateUIWidgets() {
        if (mAddressRequested) {
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
            mFetchAddressButton.setEnabled(false);
        } else {
            mProgressBar.setVisibility(ProgressBar.GONE);
            mFetchAddressButton.setEnabled(true);
        }
    }


    protected void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);


        savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);
        super.onSaveInstanceState(savedInstanceState);
    }


    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }


        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {


            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();


            if (resultCode == Constants.SUCCESS_RESULT) {
                showToast(getString(R.string.address_found));
            }


            mAddressRequested = false;
            updateUIWidgets();
        }
    }
}

