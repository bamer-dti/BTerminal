package pt.bamer.bamerosterminal.couchbase;

import android.content.Context;
import android.support.v7.app.AlertDialog;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Database;
import com.couchbase.lite.Document;
import com.couchbase.lite.DocumentChange;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.Reducer;
import com.couchbase.lite.View;
import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.Replication;
import com.couchbase.lite.util.Log;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import pt.bamer.bamerosterminal.MrApp;
import pt.bamer.bamerosterminal.utils.Constantes;
import pt.bamer.bamerosterminal.utils.ValoresDefeito;

public class ServicoCouchBase {
    private static final String TAG = ServicoCouchBase.class.getSimpleName();

    @SuppressWarnings("FieldCanBeLocal")
    private final int VERSAO_INTERNA_DAS_VIEWS = 34;

    private static final String COUCH_DB_NAME = "bameros001";
    private static final String COUCH_USER_ID = "syncuser";
    private static final String COUCH_USER_PASSWORD = "SyncUser#10!";
    //    public static final String COUCH_SERVER_URL = "http://server.bamer.pt:4984";
    private static final String COUCH_SERVER_URL = "http://192.168.0.3:4984";
    public static final String COUCH_SERVER_AND_DB_URL = COUCH_SERVER_URL + "/" + COUCH_DB_NAME + "/";

    private static ServicoCouchBase instancia;
    private Database database;
    private Manager manager;
    private Replication pullReplication;

    public View viewOS_CABOrderByCorteOrdemBostampCor;
    public View viewLinhasDoBostamp;
    private View viewPecasPorDossier;
    private View viewOSPRODpecasFeitasPorDossier;
    public View viewTemposPorDossier;
    public View viewOSPROD;
    public View viewLinhasOSBIQttAGrupada;
    private View viewQtdAgrupadaOSBI;
    private View viewOSPRODpecasFeitasAgrupadas;
    public View viewOS_CAB;

    public static ServicoCouchBase getInstancia() {
        if (instancia == null) {
            instancia = new ServicoCouchBase();
        }
        return instancia;
    }

    public void start(final Context context) {
        URL syncUrl;
        try {
            syncUrl = new URL(COUCH_SERVER_AND_DB_URL);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        try {
            manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
//            database = manager.getDatabase(COUCH_DB_NAME);
            database = manager.getDatabase(COUCH_DB_NAME);
            database.addChangeListener(new Database.ChangeListener() {
                @Override
                public void changed(Database.ChangeEvent event) {
                    List<DocumentChange> changes = event.getChanges();
//                    Log.i("BASEDEDADOSLISTENER", "Alteração na base de dados, registos = " + changes.size());
                    MrApp mrApp = (MrApp) context.getApplicationContext();
                    for (int i = 0; i < changes.size(); i++) {
                        mrApp.updateSyncProgress(i + 1, changes.size(), Replication.ReplicationStatus.REPLICATION_ACTIVE);
                    }
                    mrApp.updateSyncProgress(1, 1, Replication.ReplicationStatus.REPLICATION_IDLE);
                }
            });

            setupViews();

            Authenticator basicAuthenticator = AuthenticatorFactory.createBasicAuthenticator(COUCH_USER_ID, COUCH_USER_PASSWORD);

            pullReplication = database.createPullReplication(syncUrl);
            pullReplication.setContinuous(true);
            pullReplication.setAuthenticator(basicAuthenticator);

            Replication.ChangeListener changeListener = new Replication.ChangeListener() {
                @Override
                public void changed(Replication.ChangeEvent event) {
                    Replication replication = event.getSource();
                    MrApp mrApp = (MrApp) context.getApplicationContext();
                    mrApp.updateSyncProgress(
                            replication.getCompletedChangesCount(),
                            replication.getChangesCount(),
                            replication.getStatus()
                    );
//                    if (replication.getCompletedChangesCount() != replication.getChangesCount())
//                        Log.i(TAG, "Alteração pullReplication " + replication.getCompletedChangesCount() + "/" + replication.getChangesCount() + " -> " + replication.getStatus().toString());
                }
            };
            pullReplication.addChangeListener(changeListener);

            String estado = Constantes.ESTADO_CORTE;
            String seccao = MrApp.getPrefs().getString(Constantes.PREF_SECCAO, ValoresDefeito.SECCAO);
            List<String> canais = new ArrayList<>();
            canais.add(seccao.trim());
            canais.add(estado.trim());
            canais.add(CamposCouch.DOC_TIPO_OSPROD);
            canais.add(CamposCouch.DOC_TIPO_TIMER);
            canais.add(CamposCouch.NATUREZA_CORTE);
            pullReplication.setChannels(canais);


            pullReplication.start();
            Log.i(TAG, "Manager COUCHBASE iniciou com sucesso");
        } catch (IOException | CouchbaseLiteException e) {
            e.printStackTrace();
            AlertDialog alert = new AlertDialog.Builder(context)
                    .setCancelable(true)
                    .setMessage("Ocorreu um erro ao iniciar o serviço COUCHBASE")
                    .setTitle("ERRO")
                    .setNeutralButton("OK", null)
                    .create();
            alert.show();
        }
    }

    private void setupViews() {
        Log.i(TAG, "Versão da Views: " + getVersao());

        viewOS_CABOrderByCorteOrdemBostampCor = database.getView("view_oscab_CorteOrdemBostampCor");
        Mapper mapper = new Mapper() {
            public void map(Map<String, Object> document, Emitter emitter) {
                if (CamposCouch.DOC_TIPO_OSBO.equals(document.get(CamposCouch.FIELD_TIPO))
                        && document.get(CamposCouch.FIELD_ESTADO).equals(Constantes.ESTADO_CORTE)
                        && document.get(CamposCouch.FIELD_SECCAO).equals(MrApp.getSeccao())
                        && !document.get(CamposCouch.FIELD_ORDEM).equals(0)
                        ) {
                    ArrayList<Object> listaDeKeys = new ArrayList<>();
                    listaDeKeys.add(document.get(CamposCouch.FIELD_DTCORTEF));
                    listaDeKeys.add(document.get(CamposCouch.FIELD_ORDEM));
                    listaDeKeys.add(document.get(CamposCouch.FIELD_BOSTAMP));
                    listaDeKeys.add(document.get(CamposCouch.FIELD_COR));

                    emitter.emit(listaDeKeys, document);
                }
            }
        };
        viewOS_CABOrderByCorteOrdemBostampCor.setMap(mapper, getVersao());

        viewOS_CAB = database.getView("view_oscab");
        mapper = new Mapper() {
            public void map(Map<String, Object> document, Emitter emitter) {
                if (CamposCouch.DOC_TIPO_OSBO.equals(document.get(CamposCouch.FIELD_TIPO))
                        && document.get(CamposCouch.FIELD_ESTADO).equals(Constantes.ESTADO_CORTE)
                        && document.get(CamposCouch.FIELD_SECCAO).equals(MrApp.getSeccao())
                        && !document.get(CamposCouch.FIELD_ORDEM).equals(0)
                        ) {
                    emitter.emit(document.get(CamposCouch.FIELD_BOSTAMP), document);
                }
            }
        };
        viewOS_CAB.setMap(mapper, getVersao());

        viewLinhasDoBostamp = database.getView("view_linhas_os");
        Mapper mapperOSBO = new Mapper() {
            public void map(Map<String, Object> document, Emitter emitter) {
                if (CamposCouch.DOC_TIPO_OSBI.equals(document.get(CamposCouch.FIELD_TIPO))
                        ) {
                    Object bostamp = document.get(CamposCouch.FIELD_BOSTAMP);
                    Object dim = document.get(CamposCouch.FIELD_DIM);
                    Object design = document.get(CamposCouch.FIELD_DESIGN);
                    emitter.emit(Arrays.asList(bostamp, dim, design), document);
                }
            }
        };
        viewLinhasDoBostamp.setMap(mapperOSBO, getVersao());

        viewOSPROD = database.getView("viewOSPROD");
        mapper = new Mapper() {
            public void map(Map<String, Object> document, Emitter emitter) {
                if (CamposCouch.DOC_TIPO_OSPROD.equals(document.get(CamposCouch.FIELD_TIPO))) {
                    Object dim = document.get(CamposCouch.FIELD_DIM);
                    Object mk = document.get(CamposCouch.FIELD_MK);
                    Object ref = document.get(CamposCouch.FIELD_REF);
                    Object design = document.get(CamposCouch.FIELD_DESIGN);
                    Object bostamp = document.get(CamposCouch.FIELD_BOSTAMP);
                    emitter.emit(Arrays.asList(bostamp, dim, mk, ref, design), document);
                }
            }
        };
        viewOSPROD.setMap(mapper, getVersao());

        viewPecasPorDossier = database.getView("pecasPorDossier");
        viewPecasPorDossier.setMapReduce(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (CamposCouch.DOC_TIPO_OSBI.equals(document.get(CamposCouch.FIELD_TIPO))
                        ) {
                    emitter.emit(document.get(CamposCouch.FIELD_BOSTAMP), document.get(CamposCouch.FIELD_QTT));
                }
            }
        }, new Reducer() {
            @Override
            public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
                int total = 0;
                for (Object value : values) {
                    int newVal;
                    if (value instanceof Double) {
                        newVal = ((Double) value).intValue();
                    } else {
                        newVal = (int) value;
                    }
                    total += newVal;
                }
                return total;
            }
        }, getVersao());


        viewOSPRODpecasFeitasPorDossier = database.getView("pecasFeitasPorDossier");
        viewOSPRODpecasFeitasPorDossier.setMapReduce(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (CamposCouch.DOC_TIPO_OSPROD.equals(document.get(CamposCouch.FIELD_TIPO))) {
                    emitter.emit(document.get(CamposCouch.FIELD_BOSTAMP), document.get(CamposCouch.FIELD_QTT));
                }
            }
        }, new Reducer() {
            @Override
            public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
                int total = 0;
                for (Object value : values) {
                    int newVal;
                    if (value instanceof Double) {
                        newVal = ((Double) value).intValue();
                    } else {
                        newVal = (int) value;
                    }
                    total += newVal;
                }
                return total;
            }
        }, getVersao());

        viewOSPRODpecasFeitasAgrupadas = database.getView("pecasFeitasPorAgrupamento");
        viewOSPRODpecasFeitasAgrupadas.setMapReduce(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (CamposCouch.DOC_TIPO_OSPROD.equals(document.get(CamposCouch.FIELD_TIPO))) {
                    Object dim = document.get(CamposCouch.FIELD_DIM);
                    Object mk = document.get(CamposCouch.FIELD_MK);
                    Object ref = document.get(CamposCouch.FIELD_REF);
                    Object design = document.get(CamposCouch.FIELD_DESIGN);
                    Object bostamp = document.get(CamposCouch.FIELD_BOSTAMP);
                    emitter.emit(Arrays.asList(bostamp, dim, mk, ref, design), document.get(CamposCouch.FIELD_QTT));
                }
            }
        }, new Reducer() {
            @Override
            public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
                int total = 0;
                for (Object value : values) {
                    int newVal;
                    if (value instanceof Double) {
                        newVal = ((Double) value).intValue();
                    } else {
                        newVal = (int) value;
                    }
                    total += newVal;
                }
                return total;
            }
        }, getVersao());

        viewLinhasOSBIQttAGrupada = database.getView("viewLinhasOSBIQttAGrupada");
        mapper = new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (CamposCouch.DOC_TIPO_OSBI.equals(document.get(CamposCouch.FIELD_TIPO))
                        ) {
                    Object bostamp = document.get(CamposCouch.FIELD_BOSTAMP);
                    Object ref = document.get(CamposCouch.FIELD_REF);
                    Object design = document.get(CamposCouch.FIELD_DESIGN);
                    Object dim = document.get(CamposCouch.FIELD_DIM);
                    Object mk = document.get(CamposCouch.FIELD_MK);
                    emitter.emit(Arrays.asList(bostamp, dim, mk, ref, design), document);
                }
            }
        };
        viewLinhasOSBIQttAGrupada.setMap(mapper, getVersao());

        viewQtdAgrupadaOSBI = database.getView("viewQtdAgrupadaOSBI");
        viewQtdAgrupadaOSBI.setMapReduce(new Mapper() {
            @Override
            public void map(Map<String, Object> document, Emitter emitter) {
                if (CamposCouch.DOC_TIPO_OSBI.equals(document.get(CamposCouch.FIELD_TIPO))
                        ) {
                    Object bostamp = document.get(CamposCouch.FIELD_BOSTAMP);
                    Object dim = document.get(CamposCouch.FIELD_DIM);
                    Object mk = document.get(CamposCouch.FIELD_MK);
                    Object ref = document.get(CamposCouch.FIELD_REF);
                    Object design = document.get(CamposCouch.FIELD_DESIGN);
                    emitter.emit(Arrays.asList(bostamp, dim, mk, ref, design), document.get(CamposCouch.FIELD_QTT));
                }
            }
        }, new Reducer() {
            @Override
            public Object reduce(List<Object> keys, List<Object> values, boolean rereduce) {
                int total = 0;
                for (Object value : values) {
                    int newVal;
                    if (value instanceof Double) {
                        newVal = ((Double) value).intValue();
                    } else {
                        newVal = (int) value;
                    }
                    total += newVal;
                }
                return total;
            }
        }, getVersao());

        viewTemposPorDossier = database.getView("view_tempos_dossiers");
        mapperOSBO = new Mapper() {
            public void map(Map<String, Object> document, Emitter emitter) {
//                Log.i(TAG, "" + document.get(CamposCouch.FIELD_TIPO + " - " + document.get(CamposCouch.FIELD_ESTADO)));
                if (CamposCouch.DOC_TIPO_TIMER.equals(document.get(CamposCouch.FIELD_TIPO))
                        && Constantes.ESTADO_CORTE.equals(document.get(CamposCouch.FIELD_ESTADO))
                        ) {
                    emitter.emit(Arrays.asList(
                            document.get(CamposCouch.FIELD_BOSTAMP)
                            , document.get(CamposCouch.FIELD_UNIXTIME)
                            , document.get(CamposCouch.FIELD_LASTTIME)
                            , document.get(CamposCouch.FIELD_MAQUINA)
                    ), document);
                }
            }
        };
        viewTemposPorDossier.setMap(mapperOSBO, getVersao());
    }

    private String getVersao() {
        int versaoPublica = MrApp.getPrefs().getInt(Constantes.PREF_VERSAO_VIEWS_COUCHBASE, 1);
        return versaoPublica + "." + VERSAO_INTERNA_DAS_VIEWS;
    }

    public void stop() {
        pullReplication.stop();
        database.close();
        manager.close();
        Log.w(TAG, "Terminou os serviços COUCHBASE");
    }

    public int getPecasPorOS(String bostamp) {
        Query queryPorData = viewPecasPorDossier.createQuery();
        queryPorData.setStartKey(bostamp);
        queryPorData.setEndKey(bostamp);
        int somaQtt = 0;
        try {
            QueryEnumerator queryEnumerator = queryPorData.run();
            while (queryEnumerator.hasNext()) {
                QueryRow queryRow = queryEnumerator.next();
                somaQtt = (int) queryRow.getValue();
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return somaQtt;
    }

    public int getPecasFeitasPorOS(String bostamp) {
        Query query = viewOSPRODpecasFeitasPorDossier.createQuery();
        query.setStartKey(bostamp);
        query.setEndKey(bostamp);
        int somaQtt = 0;
        try {
            QueryEnumerator queryEnumerator = query.run();
            while (queryEnumerator.hasNext()) {
                QueryRow queryRow = queryEnumerator.next();
                somaQtt = (int) queryRow.getValue();
//                out.println("QTT Feita por OS: " + somaQtt + " bostamp = " + bostamp);
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return somaQtt;
    }

    public int getPecasOSPROD(String bostamp, String dim, String mk, String ref, String design) {
        Query query = viewOSPRODpecasFeitasAgrupadas.createQuery();
        query.setStartKey(Arrays.asList(bostamp, dim, mk, ref, design));
        query.setEndKey(Arrays.asList(bostamp, dim, mk, ref, design));
        int qtt = 0;
        try {
            QueryEnumerator queryEnumerator = query.run();
            for (QueryRow queryRow : queryEnumerator) {
                int q = (int) queryRow.getValue();
                qtt += q;
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "getPecasOSPROD(" + bostamp + ", " + dim + ", " + mk + ", " + ref + ", " + design + ") = " + qtt);
        return qtt;
    }

    public long getTempoTotal(String bostamp) {
        com.couchbase.lite.View view = ServicoCouchBase.getInstancia().viewTemposPorDossier;
        Query query = view.createQuery();
        String maxText = Long.MAX_VALUE + "";
        query.setStartKey(Arrays.asList(bostamp, "", "", MrApp.getMaquina()));
        query.setEndKey(Arrays.asList(bostamp, maxText, maxText, MrApp.getMaquina()));
        long tempoCalculado = 0;
        try {
            QueryEnumerator queryEnumerator = query.run();
            Document document;
            while (queryEnumerator.hasNext()) {
                QueryRow queryRow = queryEnumerator.next();
                document = queryRow.getDocument();
                if (Integer.parseInt(document.getProperty(CamposCouch.FIELD_POSICAO).toString()) == 2) {
                    long inicio = Long.parseLong(document.getProperty(CamposCouch.FIELD_LASTTIME).toString());
                    long fim = Long.parseLong(document.getProperty(CamposCouch.FIELD_UNIXTIME).toString());
                    tempoCalculado += (fim - inicio);
//                    Log.i(TAG, bostamp + ", Tempo Total Calculado (TTC): " + tempoCalculado);
                }
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return tempoCalculado;
    }

    public long getUltimoTempo(String bostamp) {
        com.couchbase.lite.View view = ServicoCouchBase.getInstancia().viewTemposPorDossier;
        Query query = view.createQuery();
        String maxText = Long.MAX_VALUE + "";
        query.setStartKey(Arrays.asList(bostamp, "", "", MrApp.getMaquina()));
        query.setEndKey(Arrays.asList(bostamp, maxText, maxText, MrApp.getMaquina()));
        long tempoUnix = 0;
        try {
            QueryEnumerator queryEnumerator = query.run();
            Document document;
            int lastPosition = 0;
            while (queryEnumerator.hasNext()) {
                QueryRow queryRow = queryEnumerator.next();
                document = queryRow.getDocument();
                tempoUnix = Long.parseLong(document.getProperty(CamposCouch.FIELD_UNIXTIME).toString());
                lastPosition = Integer.parseInt(document.getProperty(CamposCouch.FIELD_POSICAO).toString());
            }
            if (tempoUnix != 0)
                Log.d(TAG, bostamp + ": posição = " + lastPosition + ", tempo: " + tempoUnix);

            if (lastPosition != 1) {
                tempoUnix = 0;
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return tempoUnix;
    }

    public int getQttAgrupadasOSBI(String bostamp, String dim, String mk, String ref, String design) {
        Query query = viewQtdAgrupadaOSBI.createQuery();
        query.setStartKey(Arrays.asList(bostamp, dim, mk, ref, design));
        query.setEndKey(Arrays.asList(bostamp, dim, mk, ref, design));
        int qtt = 0;
        try {
            QueryEnumerator queryEnumerator = query.run();
            while (queryEnumerator.hasNext()) {
                int q = (int) queryEnumerator.next().getValue();
                qtt += q;
            }

        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
        return qtt;
    }

    public Database getDatabase() {
        return database;
    }
}
