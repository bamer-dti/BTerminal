package pt.bamer.bamerosterminal;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.SharedPreferences;

import com.androidnetworking.AndroidNetworking;
import com.couchbase.lite.replicator.Replication;

import java.util.Observable;

import pt.bamer.bamerosterminal.couchbase.ServicoCouchBase;
import pt.bamer.bamerosterminal.utils.Constantes;
import pt.bamer.bamerosterminal.utils.ValoresDefeito;

public class MrApp extends Application {
    @SuppressWarnings("unused")
    private static final String TAG = MrApp.class.getSimpleName();
    private static ProgressDialog dialogoInterminavel;
    private ServicoCouchBase servicoCouchBase;
    private static OnSyncProgressChangeObservable onSyncProgressChangeObservable;

    public static SharedPreferences getPrefs() {
        return prefs;
    }

    private static SharedPreferences prefs;
    private static String maquina;
    private static String operador;
    private static String seccao;

    public static void setMaquina(String maquina) {
        MrApp.maquina = maquina;
    }

    public static void setOperador(String operador) {
        MrApp.operador = operador;
    }

    public static String getMaquina() {
        return maquina;
    }

    public static String getOperador() {
        return operador;
    }

    public static String getSeccao() {
        return seccao;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(Constantes.PREFS_NAME, MODE_PRIVATE);
        seccao = prefs.getString(Constantes.PREF_SECCAO, ValoresDefeito.SECCAO);

        servicoCouchBase = ServicoCouchBase.getInstancia();
        servicoCouchBase.start(this);
        onSyncProgressChangeObservable = new OnSyncProgressChangeObservable();
        AndroidNetworking.initialize(getApplicationContext());

        //todo eliminar as linhas seguintes
//        Testes.eliminarTemposLocal();
//        Testes.eliminarProdsLocal();
    }

    @Override
    public void onTerminate() {
        servicoCouchBase.stop();
        super.onTerminate();
    }

    public static void mostrarAlertToWait(final Activity activity, final String mensagem) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialogoInterminavel == null) {
                    dialogoInterminavel = new ProgressDialog(activity);
                    dialogoInterminavel.setMessage(mensagem);
                    dialogoInterminavel.show();
                } else {
                    dialogoInterminavel.setMessage(mensagem);
                }
            }
        });
    }

    public static void esconderAlertToWait(Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialogoInterminavel != null) {
                    dialogoInterminavel.hide();
                    dialogoInterminavel.dismiss();
                    dialogoInterminavel = null;
                }
            }
        });
    }

    public synchronized void updateSyncProgress(int completedCount, int totalCount, Replication.ReplicationStatus status) {
        onSyncProgressChangeObservable.notifyChanges(completedCount, totalCount, status);
    }

    public static OnSyncProgressChangeObservable getOnSyncProgressChangeObservable() {
        return onSyncProgressChangeObservable;
    }

    public static class OnSyncProgressChangeObservable extends Observable {
        private void notifyChanges(int completedCount, int totalCount, Replication.ReplicationStatus status) {
            SyncProgress progress = new SyncProgress();
            progress.completedCount = completedCount;
            progress.totalCount = totalCount;
            progress.status = status;
            setChanged();
            notifyObservers(progress);
        }
    }

    public static class SyncProgress {
        public int completedCount;
        public int totalCount;
        public Replication.ReplicationStatus status;
    }
}


