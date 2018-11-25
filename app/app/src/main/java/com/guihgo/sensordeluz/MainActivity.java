package com.guihgo.sensordeluz;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Context context;

    private BluetoothAdapter btAdapter;
    private List<BluetoothDevice> listBTDevices;

    BtConexao btConexao = null;
    Handler handler;
    private MyHandler mHandler;

    //UI
    private TextView tvStatus, tvStatusConexao, tvDadoRecebido;
    private ListView lvPairedDevices;
    private Button btnDesconecta, btnEnviaTeste;


    //Lanternan
    CameraManager cameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.context = this;
        this.btAdapter = BluetoothAdapter.getDefaultAdapter();
        this.cameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvStatusConexao = (TextView) findViewById(R.id.tvStatusConexao);
        tvDadoRecebido = (TextView) findViewById(R.id.tvDadoRecebido);
        lvPairedDevices = (ListView) findViewById(R.id.lvPairedDevices);
        btnDesconecta = (Button) findViewById(R.id.btnDesconecta);
        btnEnviaTeste = (Button) findViewById(R.id.btnEnviaTeste);


        lvPairedDevices.setOnItemClickListener(onItemClickLvPairedDevices);
        btnDesconecta.setOnClickListener(btnDesconectaClick);
        btnEnviaTeste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (btConexao != null && btConexao.isConectado==true) {
                    btConexao.writeMsg("Teste");
                    btConexao.write(10); //10  o \n
                }
            }
        });


        registerReceiver(brBTState, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        verificaBT();
    }

    BroadcastReceiver brBTState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String stateExtra = BluetoothAdapter.EXTRA_STATE;
            int state = intent.getIntExtra(stateExtra, -1);
            String toastText = "-STATE-";

            switch (state) {
                case BluetoothAdapter.STATE_TURNING_ON:
                {
                    toastText = "Bluetooth está Ligando...";
                    break;
                }
                case BluetoothAdapter.STATE_ON:
                {
                    toastText = "Bluetooth está LIGADO !";
                    verificaBT();
                    break;
                }
                case BluetoothAdapter.STATE_TURNING_OFF:
                {
                    toastText = "Bluetooth está Desligando...";
                    break;
                }
                case BluetoothAdapter.STATE_OFF:
                {
                    toastText = "Bluetooth está DESLIGADO !";
                    break;
                }
                default:
                    break;
            }
            tvStatus.setText(toastText);
        }
    };

    private boolean verificaBT() {
        if(btAdapter.isEnabled()) //se o Bluetooth esta LIGADO
        {
            tvStatus.setText("Bluetooth está LIGADO !");
            getBoundedDevices();
        } else {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0); //faz a intent para ligar o bluetooth
        }
        return btAdapter.isEnabled();
    }

    private void getBoundedDevices() {
        if(btAdapter.isEnabled()) {
            listBTDevices = new ArrayList<>( btAdapter.getBondedDevices());
            List<String> listNameBTDevices = new ArrayList<>();
            for(int i=0; i < listBTDevices.size(); i++){
                listNameBTDevices.add(listBTDevices.get(i).getName() + " - " + listBTDevices.get(i).getAddress());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listNameBTDevices);
            lvPairedDevices.setAdapter(adapter);
        }
    }

    AdapterView.OnItemClickListener onItemClickLvPairedDevices = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(verificaBT()) {
                conectaBT(listBTDevices.get(position).getAddress() );  //Conecta ao clicar num item da lista
            }
        }
    };

    private void conectaBT(String MAC) {
        desconectaBT();

        this.mHandler = new MyHandler(this);

        this.btConexao = new BtConexao(this, this.mHandler, btAdapter,MAC);
    }

    private void desconectaBT() {
        if (btConexao != null) { btConexao.closeSocket(); }
        switchFlashLight(false);
    }

    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        private boolean lastStatusLight = false;
        private boolean statusLight = false;

        public MyHandler(MainActivity activity) {
            this.mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                if(msg.what == BtConexao.WHAT_RECEBE_DADOS) {
                    String s = (String) msg.obj;
                    Log.d("handlerMessage", s);
//                    activity.tvDadoRecebido.setText( activity.tvDadoRecebido.getText() + "\n\r" + (String) msg.obj );
                    statusLight = ((String) msg.obj).equals("1");
                    Log.d("statusLight", String.valueOf(statusLight));
                    if(lastStatusLight != statusLight) {
                        activity.switchFlashLight(statusLight);
                    }
                    lastStatusLight = statusLight;
                }

                if(msg.what == BtConexao.WHAT_STATUS_DE_CONEXAO) {
                    if(msg.arg1==1) {
                        BluetoothDevice btDevice = (BluetoothDevice) msg.obj;
                        activity.tvStatusConexao.setText("Conectado com " + btDevice.getName());
                        activity.tvDadoRecebido.setText("");
                    } else {
                        activity.tvStatusConexao.setText("Desconectado");
                    }
                }

                super.handleMessage(msg);
            }
        }
    }

    View.OnClickListener btnDesconectaClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            desconectaBT();
        }
    };

    private void switchFlashLight(boolean offOn){
        try {
            String cameraIds [] = cameraManager.getCameraIdList();
            for(int i=0; i<cameraIds.length; i++) {
                Log.d("MainActivity", "cameraIds["+i+"]: "+cameraIds[i]);
                cameraManager.setTorchMode(cameraIds[i], offOn);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            Log.e("MainActivity", e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        desconectaBT();
        super.onDestroy();
    }
}
