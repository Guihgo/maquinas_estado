/*
 * Copyright (c) 2016 Guihgo.
 * Atenção: Este programa é protegido por leis de direitos autorais.  A reprodução ou a distribuição não autorizada deste programa,
 * ou de qualquer parte do mesmo, pode resultar em penalidades civis e criminais severas, e será processada até a extensão máxima permitida por lei.
 */

package com.guihgo.sensordeluz;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BtConexao extends Thread {

    public static int WHAT_STATUS_DE_CONEXAO = 100;
    public static int WHAT_RECEBE_DADOS = 101;

    private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket btSocket = null;
    private OutputStream btOutStream;
    private InputStream btInStream;

    boolean bConexaoTempoLimite = true;
    long conexaoTempoLimite = 15*1000;

    Context context;
    Handler handler;
    BtConexao btConexao;
    Thread thOuve;

    boolean isConectado = false;

    public BtConexao(Context context, Handler handler, BluetoothAdapter btAdapter, String MAC) {
        this.context = context;
        this.handler = handler;
        btConexao = this;

        //Inicia A CONEXÃO
        final conectaBTAsyncTask cbtat = new conectaBTAsyncTask(context, btAdapter);
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.e("conectaBTAsyncTask", "doInBackground TEMPO ESGOTADO PARA CONEXAO ");
                cbtat.cancela();

            }
        }, conexaoTempoLimite);
        cbtat.execute(MAC);

    }

    public void ligaOuvinte() {
        //Mantem uma thread para fica ouvindo...
        thOuve = new Thread() {
            byte[] buffer = new byte[1];
            int bytes;

            @Override
            public void run() {

                while (true) {
                    try {
                        bytes = btInStream.read(buffer); // Lê dados que chegou no InputStream
                        // Manda dados pra Activity (UI)
                        Message msg = handler.obtainMessage();
                        msg.what = WHAT_RECEBE_DADOS;
                        msg.obj = new String(buffer, 0, bytes);
                        handler.sendMessage(msg);
                    } catch (IOException e) {
                        Log.e("read dados", "error", e);
                        break;
                    }
                }

            }
        };
        thOuve.start();
    }

    public void closeSocket() {
        try {
            isConectado = false;
            thOuve = null;
            btSocket.close();
            handler.sendMessage(handler.obtainMessage(WHAT_STATUS_DE_CONEXAO, 0, 0, null));
            Log.i("thread", "Socket fechado !");
        } catch (Exception e) { e.printStackTrace(); }

    }

    public void write(int i) {
        try {
            btOutStream.write(i);
            Log.e("thread", "enviou");
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void writeMsg(String message) {
        byte[] msgBuffer = message.getBytes();
        try {
            btOutStream.write(msgBuffer);
            Log.e("thread", "enviou " + msgBuffer);
        } catch (IOException e) {}
    }



    //AsyncTask - <Parametro, Progresso, Resultado>
    class conectaBTAsyncTask extends AsyncTask<String, String, Boolean> {

        ProgressDialog pd;

        Context context;
        BluetoothAdapter btAdapter;

        public conectaBTAsyncTask( Context context, BluetoothAdapter btAdapter) {
            this.context = context;
            this.btAdapter = btAdapter;
        }

        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(context);
            pd.setIndeterminate(true);
            pd.setCancelable(true);
            pd.setCanceledOnTouchOutside(false);
            pd.setTitle("Conectando...");

            pd.show();

            pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    cancela();
                }
            });

            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... params)
        {
            Log.i("conectaBTAsyncTask", "doInBackground params[0] = " + params[0]);
            publishProgress(params[0]);

            while (bConexaoTempoLimite) {
                try {
                    btSocket = btAdapter.getRemoteDevice(params[0]).createRfcommSocketToServiceRecord(MY_UUID);
                    //btSocket = (BluetoothSocket) btAdapter.getRemoteDevice(params[0]).getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(btAdapter.getRemoteDevice(params[0]),1);
                    publishProgress(btSocket.getRemoteDevice().getName());
                    btSocket.connect();
                    btOutStream = btSocket.getOutputStream();
                    btInStream = btSocket.getInputStream();
                    Log.e("conectaBTAsyncTask", "conectou");
                    return true;
                } catch (Exception e1) {
                    Log.e("conectaBTAsyncTask", "nao conectou :( ", e1);
                }
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(String... valor) {
            Log.w("conectaBTAsyncTask", "onProgressUpdate => valor = " + valor[0]);
            pd.setMessage("Conectando" + " a " + valor[0]);
            super.onProgressUpdate(valor);
        }

        @Override
        protected void onPostExecute(Boolean bConectado) {
            Log.i("conectaBTAsyncTask", "onPostExecute => bConectado = " + bConectado.toString());

            if (bConectado) {
                btConexao.isConectado = true;
                btConexao.ligaOuvinte();
                handler.sendMessage(handler.obtainMessage(WHAT_STATUS_DE_CONEXAO, 1, 1, btSocket.getRemoteDevice()));
            } else {
                btConexao.isConectado = false;
                handler.sendMessage(handler.obtainMessage(WHAT_STATUS_DE_CONEXAO, 0, 0, null));
            }

            pd.dismiss();
            super.onPostExecute(bConectado);
        }

        public void cancela() {
            bConexaoTempoLimite = false;
            pd.dismiss();
        }

    }

}
