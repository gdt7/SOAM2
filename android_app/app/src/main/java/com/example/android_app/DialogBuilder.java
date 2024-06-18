package com.example.android_app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

public class DialogBuilder {


    static void buildAuthorizationDialog(Context ctx){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctx);
        alertDialogBuilder.setPositiveButton("hola", authorizeDriver(ctx));
        alertDialogBuilder.setNegativeButton("chau", null);
        alertDialogBuilder.setTitle("Desea autorizar al usuario?");
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private static DialogInterface.OnClickListener authorizeDriver(Context ctx) {

        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ChoferDbHelper help = new ChoferDbHelper(ctx);
                help.insertChofer("pepito","122 322 112 442","15");
            }
        };
    }

    //PARA ENVIAR MENSAJE AL ESP32
//    private void sendMessage(String message) {
//        // Check that we're actually connected before trying anything
//        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
//            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Check that there's actually something to send
//        if (message.length() > 0) {
//            // Get the message bytes and tell the BluetoothChatService to write
//            byte[] send = message.getBytes();
//            mChatService.write(send);
//
//            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            mOutEditText.setText(mOutStringBuffer);
//        }
//    }

}


