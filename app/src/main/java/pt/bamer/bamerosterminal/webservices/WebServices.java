package pt.bamer.bamerosterminal.webservices;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.Priority;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.json.JSONException;
import org.json.JSONObject;

import pt.bamer.bamerosterminal.ListaOS;
import pt.bamer.bamerosterminal.MrApp;
import pt.bamer.bamerosterminal.adapters.TarefaRecyclerAdapter;
import pt.bamer.bamerosterminal.couchbase.ServicoCouchBase;
import pt.bamer.bamerosterminal.pojos.JSONObjectQtd;
import pt.bamer.bamerosterminal.pojos.JSONObjectTimer;
import pt.bamer.bamerosterminal.utils.Funcoes;

public class WebServices {
    private static final String TAG = WebServices.class.getSimpleName();
    //    public static final String JSON_URL_REGISTAR_TEMPO = "http://server.bamer.pt:99/bameros.svc/registartempo";
    public static final String SERVER_WEBSERVICES = "http://192.168.0.1:99/bameros.svc/";
    //    public static final String SERVER_WEBSERVICES = "http://server.bamer.pt:99/bameros.svc/";
    public static final String JSON_URL_REGISTAR_TEMPO = SERVER_WEBSERVICES + "registartempo";
    public static final String JSON_URL_REGISTAR_QTD = SERVER_WEBSERVICES + "registarqtd";
    private static final String HTTP_OK = "ok";
    private static final String HTTP_MENSAGEM = "mensagem";

    public static void registarTempoemSQL(final ListaOS activity, final JSONObjectTimer jsonObjectTimer) {
        MrApp.mostrarAlertToWait(activity, "A gravar no servidor, aguarde...");
        AndroidNetworking.post(JSON_URL_REGISTAR_TEMPO)
                .addStringBody(jsonObjectTimer.toString())
                .setTag("test")
                .addHeaders("Content-Type", "text/plain")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            boolean resultado = response.getBoolean(HTTP_OK);
                            String mensagem = response.getString(HTTP_MENSAGEM);
                            Log.i(TAG, "code  = " + resultado + ": " + mensagem);
                            if (!resultado) {
                                MrApp.esconderAlertToWait(activity);
                                Log.e(TAG, "Erro ao gravar\n" + jsonObjectTimer.toString());
                                Funcoes.alerta(activity, "Erro", "A gravação de tempo no webservice não foi efectuada. O erro é:\n" + mensagem);
                            } else {
                                Toast.makeText(activity, "Gravado com sucesso, aguarde um momento para actualizar informação", Toast.LENGTH_LONG).show();
                                MrApp.mostrarAlertToWait(activity, "A obter dados do servidor, aguarde...");
                            }
                        } catch (JSONException e) {
                            MrApp.esconderAlertToWait(activity);
                            Funcoes.alerta(activity, "Erro", "Ocorreu um erro interno no webservice.\nTente novamente. Se o erro persistir, contacte o DTI: " + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError error) {
                        if (error.getErrorCode() != 0) {
                            Log.e(TAG, "onError errorCode : " + error.getErrorCode());
                            Log.e(TAG, "onError errorBody : " + error.getErrorBody());
                            Log.e(TAG, "onError errorDetail : " + error.getErrorDetail());
                        } else {
                            Log.e(TAG, "onError errorDetail : " + error.getErrorDetail());
                        }
                        MrApp.esconderAlertToWait(activity);
                        Funcoes.alerta(activity, "Erro", "Ocorreu um erro em <registarTempoemSQL> ao gravar via webservice.\nTente novamente. Se o erro persistir, contacte o DTI: " + error.getErrorDetail());
                        Log.i(TAG, jsonObjectTimer.toString());
                    }
                });
    }

    private static void dismissDialog(ProgressDialog dialog) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @SuppressWarnings("unused")
    private static void lancarTempoEmCouch(final Context context, final JSONObjectTimer jsonObject, final String idcouch, final ProgressDialog dialog) {
        Log.i(TAG, "Json\n" + jsonObject.toString());
        AndroidNetworking.put(ServicoCouchBase.COUCH_SERVER_AND_DB_URL + idcouch)
                .addJSONObjectBody(jsonObject)
                .setTag("test")
                .addHeaders("Content-Type", "application/json")
                .addHeaders("Authorization", "Basic c3luY3VzZXI6U3luY1VzZXIjMTAh")
                .addHeaders("If-Match", "")
                .addHeaders("Content-Length", "" + jsonObject.toString().length())
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.i(TAG, "resposta  = " + response.getBoolean("ok"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        dismissDialog(dialog);
//                        if (jsonObject.getPosicao() == Constantes.MODO_STARTED) {
//                            ListaOS.ColocarOSemTrabalhoAposWebService((ListaOS) context, jsonObject);
////                            ListaOS.retirarRegistoDaLista((ListaOS) context, jsonObject);
//                        } else {
//                            ListaOS.pararCronometro((ListaOS) context);
//                        }
                    }

                    @Override
                    public void onError(ANError error) {
                        if (error.getErrorCode() != 0) {
                            Log.e(TAG, "onError errorCode : " + error.getErrorCode());
                            Log.e(TAG, "onError errorBody : " + error.getErrorBody());
                            Log.e(TAG, "onError errorDetail : " + error.getErrorDetail());
                        } else {
                            // error.getErrorDetail() : connectionError, parseError, requestCancelledError
                            Log.e(TAG, "onError errorDetail : " + error.getErrorDetail());
                        }
                        dismissDialog(dialog);
                        Funcoes.alerta(context, "Erro", "Ocorreu um erro ao gravar via webservice.\nTente novamente. Se o erro persistir, contacte o DTI: " + error.getErrorDetail());
                    }
                });
    }

    public static void registarQtdEmSQL(final Activity activity, final Object viewOrigem, final int qtd_total, final int qtd, final JSONObjectQtd jsonObjectQtd) {
        MrApp.mostrarAlertToWait(activity, "A gravar no servidor, aguarde...");
        Log.w(TAG, "JSON OSPROD:\n" + jsonObjectQtd.toString());

        AndroidNetworking.post(JSON_URL_REGISTAR_QTD)
                .addStringBody(jsonObjectQtd.toString())
                .setTag("test")
                .addHeaders("Content-Type", "text/plain")
                .setPriority(Priority.MEDIUM)
                .build()
                .getAsJSONObject(new JSONObjectRequestListener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            boolean resultado = response.getBoolean(HTTP_OK);
                            String mensagem = response.getString(HTTP_MENSAGEM);
                            Log.i(TAG, "code  = " + resultado + ": " + mensagem);
                            if (!resultado) {
                                Log.e(TAG, "Erro ao gravar\n" + jsonObjectQtd.toString());
                                Funcoes.alerta(activity, "Erro", "A gravação de tempo no webservice não foi efectuada. O erro é:\n" + mensagem);
                            } else {
                                if (viewOrigem != null) {
                                    if (viewOrigem instanceof Button) {
                                        Button but = (Button) viewOrigem;
                                        but.setText((qtd + qtd_total) + "");
                                        MrApp.esconderAlertToWait(activity);
                                    }
                                    if (viewOrigem instanceof TarefaRecyclerAdapter.ViewHolder) {
                                        MrApp.mostrarAlertToWait(activity, "A obter dados do servidor, aguarde...");
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            MrApp.esconderAlertToWait(activity);
                            Funcoes.alerta(activity, "Erro", "Ocorreu um erro interno no webservice.\nTente novamente. Se o erro persistir, contacte o DTI: " + e.getLocalizedMessage());
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(ANError error) {
                        if (error.getErrorCode() != 0) {
                            Log.e(TAG, "onError errorCode : " + error.getErrorCode());
                            Log.e(TAG, "onError errorBody : " + error.getErrorBody());
                            Log.e(TAG, "onError errorDetail : " + error.getErrorDetail());
                        } else {
                            Log.e(TAG, "onError errorDetail : " + error.getErrorDetail());
                        }
                        MrApp.esconderAlertToWait(activity);
                        Log.w(TAG, "JSON: " + jsonObjectQtd.toString());
                        Funcoes.alerta(activity, "Erro", "Ocorreu um erro em <registarQtdEmSQL> ao gravar via webservice.\nTente novamente. Se o erro persistir, contacte o DTI: " + error.getErrorDetail());
                    }
                });
    }
}
