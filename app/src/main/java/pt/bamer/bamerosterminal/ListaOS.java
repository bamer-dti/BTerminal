package pt.bamer.bamerosterminal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.replicator.Replication;
import com.daimajia.numberprogressbar.NumberProgressBar;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Timer;
import java.util.TimerTask;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import pt.bamer.bamerosterminal.adapters.OSRecyclerAdapter;
import pt.bamer.bamerosterminal.couchbase.CamposCouch;
import pt.bamer.bamerosterminal.couchbase.ServicoCouchBase;
import pt.bamer.bamerosterminal.pojos.JSONObjectQtd;
import pt.bamer.bamerosterminal.pojos.JSONObjectTimer;
import pt.bamer.bamerosterminal.utils.Constantes;
import pt.bamer.bamerosterminal.utils.Funcoes;
import pt.bamer.bamerosterminal.webservices.WebServices;

public class ListaOS extends AppCompatActivity {
    private ListaOS activityListaOS = this;
    private static String TAG = ListaOS.class.getSimpleName();
    private LinearLayout ll_working_os;
    private TextView tv_os;
    private TextView tv_tempo_total;
    private TextView tv_tempo_parcial;
    private TextView tv_qtt_total;
    private TextView bt_qtt_feita;
    private List<Object> keyBaseStarted;
    public Timer cronometroOS;
    private ListaOS activityContext = this;
    private Document documentoEmTrabalho;
    private RecyclerView recyclerView;
    private Menu menu;
    private LiveQuery liveQueryOSBO;
    private LiveQuery livequeryQtdDossier;
    private QueryEnumerator queryEnumeratorRowsListaOs;
    private SmoothProgressBar pbSmooth;
    private NumberProgressBar number_progress_bar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_listaos);

        pbSmooth = (SmoothProgressBar) findViewById(R.id.pb_smooth);
        pbSmooth.setVisibility(View.INVISIBLE);

        number_progress_bar = (NumberProgressBar) findViewById(R.id.number_progress_bar);
        number_progress_bar.setVisibility(View.INVISIBLE);

        //noinspection ConstantConditions
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager recyclerViewLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(recyclerViewLayoutManager);

        ll_working_os = (LinearLayout) findViewById(R.id.ll_working_os);
        ll_working_os.setVisibility(View.GONE);

        tv_os = (TextView) findViewById(R.id.tv_os);

        tv_tempo_total = (TextView) findViewById(R.id.tv_tempo_total);
        tv_tempo_total.setVisibility(View.INVISIBLE);

        tv_tempo_parcial = (TextView) findViewById(R.id.tv_tempo_parcial);
        tv_tempo_parcial.setVisibility(View.INVISIBLE);

        tv_qtt_total = (TextView) findViewById(R.id.tv_qtt_total);
        ll_working_os.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (keyBaseStarted == null) {
                    Funcoes.alerta(activityListaOS, "Erro", "O servidor está ocupado. Tente dentro de momentos");
                    return;
                }
                Intent intent = new Intent(view.getContext(), Dossier.class);
                intent.putExtra(Constantes.INTENT_EXTRA_BOSTAMP, keyBaseStarted.get(2).toString());
                intent.putExtra(Constantes.INTENT_EXTRA_MODO_OPERACIONAL, Constantes.MODO_STARTED);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        bt_qtt_feita = (TextView) findViewById(R.id.bt_qtt_feita);
        bt_qtt_feita.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater li = LayoutInflater.from(activityListaOS);
                @SuppressLint("InflateParams")
                View promptsView = li.inflate(R.layout.popup_qtt, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                        activityListaOS);
                alertDialogBuilder.setView(promptsView);
                final EditText userInput = (EditText) promptsView.findViewById(R.id.et_qtt);
                int qttTotal = Integer.parseInt(tv_qtt_total.getText().toString());
                int qttParcial = Integer.parseInt(bt_qtt_feita.getText().toString());
                int qttRestante = qttTotal - qttParcial;
                userInput.setHint("" + qttRestante);
                userInput.setText("" + qttRestante);
                userInput.setSelection(userInput.getText().length());
                alertDialogBuilder
                        .setCancelable(false)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        emitirQtdProduzidaPorAvulso(activityListaOS, Integer.parseInt(userInput.getText().toString()));
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                // create alert dialog
                AlertDialog alertDialog = alertDialogBuilder.create();

                // show it
                alertDialog.show();
            }
        });

        Button bt_stop_OS_Em_Trabalho = (Button) findViewById(R.id.bt_stop);
        bt_stop_OS_Em_Trabalho.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    JSONObjectTimer jsonObject = new JSONObjectTimer(keyBaseStarted.get(2).toString(), "", Constantes.ESTADO_CORTE, 2, -1);
                    WebServices.registarTempoemSQL(activityContext, jsonObject);
                    ll_working_os.setVisibility(View.GONE);
                    pararCronometro();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        configObservables();

        fazerRecyclerViewOSBO();
    }

    @Override
    protected void onResume() {
        super.onResume();
        liveQueryOSBO.start();
        livequeryQtdDossier.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        liveQueryOSBO.stop();
        livequeryQtdDossier.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pararCronometro();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_listaos, menu);
        this.menu = menu;
        boolean visi = MrApp.getPrefs().getBoolean(Constantes.PREF_MOSTRAR_OS_COMPLETOS, true);
        menu.findItem(R.id.itemmenu_mostrar_tudo).setTitle(visi ? Constantes.MOSTRAR_TUDO : Constantes.MOSTRAR_FILTRADO);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.itemmenu_mostrar_tudo:
                SharedPreferences prefs = MrApp.getPrefs();
                boolean now = prefs.getBoolean(Constantes.PREF_MOSTRAR_OS_COMPLETOS, true);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(Constantes.PREF_MOSTRAR_OS_COMPLETOS, !now);
                editor.commit();
                actionbarSetup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void configObservables() {
        MrApp.getOnSyncProgressChangeObservable().addObserver(new Observer() {
            @Override
            public void update(final Observable observable, final Object data) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        MrApp.SyncProgress progress = (MrApp.SyncProgress) data;
                        com.couchbase.lite.util.Log.v(TAG, "Sync progress changed.  Completed: %d Total: %d Status: %s", progress.completedCount, progress.totalCount, progress.status);

                        if (progress.status == Replication.ReplicationStatus.REPLICATION_ACTIVE) {
                            pbSmooth.setVisibility(View.VISIBLE);
                            number_progress_bar.setVisibility(View.VISIBLE);
                            number_progress_bar.setMax(progress.totalCount);
                            number_progress_bar.setProgress(progress.completedCount);
                        } else {
                            pbSmooth.setVisibility(View.INVISIBLE);
                            number_progress_bar.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        });
    }

    private void emitirQtdProduzidaPorAvulso(Context context, int qtd) {
        int qtd_anterior = Integer.parseInt(bt_qtt_feita.getText().toString());
        try {
            if (documentoEmTrabalho == null) {
                Funcoes.alerta(context, "Erro", "O servidor está ocupado. Tente dentro de momentos");
                return;
            }
            String dim = "";
            String mk = "";
            String ref = "";
            String design = "Qtd Avulso";
            String bostamp = documentoEmTrabalho.getProperty(CamposCouch.FIELD_BOSTAMP).toString();
            JSONObjectQtd jsonObjectQtd = new JSONObjectQtd(bostamp, dim, mk, ref, design, qtd);
            WebServices.registarQtdEmSQL(activityContext, bt_qtt_feita, qtd_anterior, qtd, jsonObjectQtd);
        } catch (JSONException e) {
            e.printStackTrace();
            Funcoes.alerta(activityContext, "ERRO", "Erro ao construir o objecto JSON.\nListaOS - método emitirQtdProduzidaPorAvulso");
        }
    }

    private void fazerRecyclerViewOSBO() {
        SharedPreferences prefs = MrApp.getPrefs();
        int limiteOsEmLista = prefs.getInt(Constantes.PREF_NUM_OS_EM_LISTA, 40);

        com.couchbase.lite.View view = ServicoCouchBase.getInstancia().viewOS_CABOrderByCorteOrdemBostampCor;
        Query queryOS_CAB = view.createQuery();
        queryOS_CAB.setLimit(limiteOsEmLista);
        liveQueryOSBO = queryOS_CAB.toLiveQuery();
        liveQueryOSBO.addChangeListener(new LiveQuery.ChangeListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void changed(LiveQuery.ChangeEvent event) {
                queryEnumeratorRowsListaOs = event.getRows();
                Log.i(TAG, "**** LIVEQUERY viewOS_CABOrderByCorteOrdemBostampCor RUNNING **** " + event.getRows().getCount());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ll_working_os.setVisibility(View.GONE);
                    }
                });
                pararCronometro();
                new aplicarAdapter().execute();
            }
        });

        view = ServicoCouchBase.getInstancia().viewOSPROD;
        Query query1 = view.createQuery();
        livequeryQtdDossier = query1.toLiveQuery();
        livequeryQtdDossier.addChangeListener(new LiveQuery.ChangeListener() {
            @Override
            public void changed(LiveQuery.ChangeEvent event) {
                QueryEnumerator i = event.getRows();
                com.couchbase.lite.util.Log.i(TAG, "**** LIVEQUERY QTD PRODUZIDA NO DOSSIER RUNNING **** " + i.getCount());
                for (QueryRow queryRow : i) {
                    Document document = queryRow.getDocument();
                    String bostamp = document.getProperty(CamposCouch.FIELD_BOSTAMP).toString();
                    try {
                        ((OSRecyclerAdapter) recyclerView.getAdapter()).actualizar(bostamp);
                        actualizarQtdNaOsEmProducao(bostamp);
                    } catch (Exception e) {
                        Log.e(TAG, "Ocorreu um erro ao refrescar o adapter OSRecyclerAdapter. Estará vazio?!");
                    }
                }
            }
        });
    }

    private void actualizarQtdNaOsEmProducao(String bostamp) {
        final int qtt = ServicoCouchBase.getInstancia().getPecasFeitasPorOS(bostamp);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bt_qtt_feita.setText("" + qtt);
            }
        });
    }

    private void iniciarTemposOSAposReplicacao(String bostamp) {
        Log.i(TAG, "iniciarTemposOSAposReplicacao: '" + bostamp + "' em trabalho visivel!");
        Query query = ServicoCouchBase.getInstancia().viewOS_CAB.createQuery();
        query.setStartKey(bostamp);
        query.setEndKey(bostamp);
        int os = 0;
        int qtt = 0;
        int qttFeita = 0;
        try {
            QueryEnumerator queryEnumerator = query.run();
            QueryRow queryRow = queryEnumerator.next();
            if (queryEnumerator.getCount() == 0) {
                Funcoes.alerta(activityListaOS, "ERRO", "Ocorreu um erro interno ao colocar a OS em trabalho");
                return;
            }
            documentoEmTrabalho = queryRow.getDocument();
            if (documentoEmTrabalho == null) {
                Funcoes.alerta(activityListaOS, "ERRO", "Ocorreu um erro interno ao colocar DOCUMENTO em trabalho");
                return;
            }
            os = Integer.parseInt(documentoEmTrabalho.getProperty(CamposCouch.FIELD_OBRANO).toString());
            qtt = ServicoCouchBase.getInstancia().getPecasPorOS(bostamp);
            qttFeita = ServicoCouchBase.getInstancia().getPecasFeitasPorOS(bostamp);
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        final int finalOs = os;
        final int finalQtt = qtt;
        final int finalQttFeita = qttFeita;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_os.setText("OS " + finalOs);
                tv_qtt_total.setText("" + finalQtt);
                bt_qtt_feita.setText("" + finalQttFeita);
            }
        });
        final String finalBostamp = bostamp;
        TimerTask actualizarTempos = new TimerTask() {
            @Override
            public void run() {
                final long tempoTotal = ServicoCouchBase.getInstancia().getTempoTotal(finalBostamp);
                long ultimoTempo = ServicoCouchBase.getInstancia().getUltimoTempo(finalBostamp);
                long unixNow = System.currentTimeMillis() / 1000L;
                final long intervaloTempo = unixNow - ultimoTempo;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String textoTempoTotal = "TT: " + Funcoes.milisegundos_em_HH_MM_SS(tempoTotal * 1000 + intervaloTempo * 1000);
                        tv_tempo_total.setText(textoTempoTotal);
                        tv_tempo_total.setVisibility(View.VISIBLE);

                        String textoIntervaloTempo = "" + Funcoes.milisegundos_em_HH_MM_SS(intervaloTempo * 1000);
                        tv_tempo_parcial.setText(textoIntervaloTempo);
                        tv_tempo_parcial.setVisibility(View.VISIBLE);
                        Log.d("CRONOGRAFO", textoTempoTotal + " ** " + textoIntervaloTempo);
                    }
                });
            }
        };

        pararCronometro();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ll_working_os.setVisibility(View.VISIBLE);
            }
        });
        cronometroOS = new Timer();
        cronometroOS.schedule(actualizarTempos, 1000, 1000);
    }

    public static int getPosicao(String bostamp) {
        com.couchbase.lite.View view = ServicoCouchBase.getInstancia().viewTemposPorDossier;
        Query query = view.createQuery();
        String maxText = Long.MAX_VALUE + "";
        query.setStartKey(Arrays.asList(bostamp, "", "", MrApp.getMaquina()));
        query.setEndKey(Arrays.asList(bostamp, maxText, maxText, MrApp.getMaquina()));
        int posicaoSQL = 0;
        try {
            QueryEnumerator queryEnumerator = query.run();
//            Log.i(TAG, "QueryEnumerator: " + queryEnumerator.getCount());
            Document document = null;
            while (queryEnumerator.hasNext()) {
                QueryRow queryRow = queryEnumerator.next();
                document = queryRow.getDocument();
            }
            if (document != null) {
                posicaoSQL = Integer.parseInt(document.getProperty(CamposCouch.FIELD_POSICAO).toString());
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        if (posicaoSQL > 0) {
            Log.v("GETPOSICAO", "O registo " + bostamp + " tem posição = " + posicaoSQL);
        }
        return posicaoSQL;
    }

//    }

    private void pararCronometro() {
        Log.i(TAG, "********** A parar o cronometro...");
        if (cronometroOS != null) {
            cronometroOS.cancel();
            cronometroOS.purge();
            cronometroOS = null;
        }
    }

    private void actionbarSetup() {
        SharedPreferences prefs = MrApp.getPrefs();
        boolean vis = prefs.getBoolean(Constantes.PREF_MOSTRAR_OS_COMPLETOS, true);
        menu.findItem(R.id.itemmenu_mostrar_tudo).setTitle(vis ? Constantes.MOSTRAR_TUDO : Constantes.MOSTRAR_FILTRADO);
        new aplicarAdapter().execute();
    }

    private class aplicarAdapter extends AsyncTask<Void, Void, Void> {
        private boolean vis;

        private ArrayList<QueryRow> listaDeOs;

        @Override
        protected void onPreExecute() {
            SharedPreferences prefs = MrApp.getPrefs();
            vis = prefs.getBoolean(Constantes.PREF_MOSTRAR_OS_COMPLETOS, true);
            MrApp.mostrarAlertToWait(activityContext, "A organizar dados...");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            listaDeOs = new ArrayList<>();
            Log.i(TAG, "Linhas de queryEnumeratorRowsListaOs: " + queryEnumeratorRowsListaOs.getCount());
            if (queryEnumeratorRowsListaOs.getCount() != 0) {
                for (int i = 0; i < queryEnumeratorRowsListaOs.getCount(); i++) {
                    QueryRow queryRow = queryEnumeratorRowsListaOs.getRow(i);
                    Log.v(TAG, "QUERYROW OSCAB: " + queryRow.getKey().toString());
                    @SuppressWarnings("unchecked")
                    List<Object> key = (List<Object>) queryRow.getKey();
                    String bostamp = key.get(2).toString();

                    //Está em modo started?
                    if (getPosicao(bostamp) == Constantes.MODO_STARTED) {
                        iniciarTemposOSAposReplicacao(bostamp);
                        keyBaseStarted = key;
                    } else {
                        if (vis) {
                            listaDeOs.add(queryRow);
                        } else {
                            int qttProd = ServicoCouchBase.getInstancia().getPecasFeitasPorOS(bostamp);
                            int qttPed = ServicoCouchBase.getInstancia().getPecasPorOS(bostamp);
                            if (qttProd != qttPed) {
                                listaDeOs.add(queryRow);
                            }
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Log.i(TAG, "A mostrar o OSRecyclerAdapter...");
            final OSRecyclerAdapter adapter = new OSRecyclerAdapter(activityListaOS, listaDeOs);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    recyclerView.setAdapter(adapter);
                }
            });
            MrApp.esconderAlertToWait(activityListaOS);
        }

    }

    public Timer getCronometroOS() {
        return cronometroOS;
    }

}
