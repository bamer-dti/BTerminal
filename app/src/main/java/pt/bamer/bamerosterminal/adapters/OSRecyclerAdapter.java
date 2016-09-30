package pt.bamer.bamerosterminal.adapters;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.couchbase.lite.Document;
import com.couchbase.lite.QueryRow;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import pt.bamer.bamerosterminal.Dossier;
import pt.bamer.bamerosterminal.ListaOS;
import pt.bamer.bamerosterminal.R;
import pt.bamer.bamerosterminal.couchbase.CamposCouch;
import pt.bamer.bamerosterminal.pojos.JSONObjectTimer;
import pt.bamer.bamerosterminal.utils.AsyncTasks;
import pt.bamer.bamerosterminal.utils.Constantes;
import pt.bamer.bamerosterminal.utils.Funcoes;
import pt.bamer.bamerosterminal.webservices.WebServices;

public class OSRecyclerAdapter extends RecyclerView.Adapter implements View.OnClickListener {
    private static final String TAG = OSRecyclerAdapter.class.getSimpleName();
    private final ListaOS contextoActivity;
    private ArrayList<QueryRow> listaQueryRows;
    private final int queryEnumerator;

    public OSRecyclerAdapter(ListaOS activity, ArrayList<QueryRow> listaDocsOSBO) {
        this.contextoActivity = activity;
        this.listaQueryRows = listaDocsOSBO;
        this.queryEnumerator = listaDocsOSBO.size();

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(contextoActivity).inflate(R.layout.view_osbo, parent, false);
        return new ViewHolder(view);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        OSRecyclerAdapter.ViewHolder viewHolder = (OSRecyclerAdapter.ViewHolder) holder;
        QueryRow queryRow = getItem(position);
        Document document;
        if (queryRow != null) {
            document = queryRow.getDocument();
        }else {
            return;
        }

        String dtcortef = document.getProperty(CamposCouch.FIELD_DTCORTEF).toString();
        final String bostamp = document.getProperty(CamposCouch.FIELD_BOSTAMP).toString();

//                Log.i(TAG, qrow.toString());
        String dttransf = document.getProperty(CamposCouch.FIELD_DTTRANSF).toString();
        Object obrano = document.getProperty(CamposCouch.FIELD_OBRANO);
        String fref = document.getProperty(CamposCouch.FIELD_FREF).toString();
        String nmfref = document.getProperty(CamposCouch.FIELD_NMFREF).toString();
        String obs = document.getProperty(CamposCouch.FIELD_OBS).toString();

        viewHolder.tv_fref.setText(fref + " - " + nmfref);
        viewHolder.tv_fref.setTag(queryRow);
        viewHolder.tv_fref.setOnClickListener(this);

        viewHolder.tv_obrano.setText("OS " + obrano);
        viewHolder.tv_obrano.setOnClickListener(this);

        viewHolder.tv_descricao.setText(obs);

        DateTimeFormatter dtf = DateTimeFormat.forPattern("dd.MM.yyyy");

        LocalDateTime localDateTime = Funcoes.cToT(dtcortef);
        viewHolder.tv_dtcortef.setText(dtf.print(localDateTime));

        localDateTime = Funcoes.cToT(dttransf);
        viewHolder.tv_dttransf.setText(dtf.print(localDateTime));

        new AsyncTasks.TaskCalculoQtt(bostamp, viewHolder.tv_qtt, viewHolder.tv_qttfeita, viewHolder.ll_root, this, position).execute();
        viewHolder.tv_qtt.setTag(bostamp);

        viewHolder.bt_posicao.setVisibility(View.INVISIBLE);
        new AsyncTasks.TaskCalcularTempo(bostamp, viewHolder.bt_posicao, viewHolder.tv_temporal, contextoActivity.getCronometroOS()).execute();

        final int finalPosition = holder.getAdapterPosition();
        viewHolder.bt_posicao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                JSONObjectTimer jsonObject;
                try {
                    jsonObject = new JSONObjectTimer(bostamp, "", Constantes.ESTADO_CORTE, 1, finalPosition);
                    WebServices.registarTempoemSQL(contextoActivity, jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        viewHolder.bt_alertas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG, "Não implementado!");
                Funcoes.alerta(view.getContext(), "Info", "Não implementado!");
            }
        });

        viewHolder.ll_root.setTag(bostamp);
        viewHolder.ll_root.setOnClickListener(this);
    }

    private QueryRow getItem(int position) {
        try {
            return listaQueryRows != null ? listaQueryRows.get(position) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public int getItemCount() {
        return queryEnumerator;
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.ll_root) {
            Intent intent = new Intent(view.getContext(), Dossier.class);
            intent.putExtra(Constantes.INTENT_EXTRA_BOSTAMP, view.getTag().toString());
            intent.putExtra(Constantes.INTENT_EXTRA_MODO_OPERACIONAL, Constantes.MODO_STOPED);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            contextoActivity.startActivity(intent);
        }
    }

    @SuppressWarnings("unused")
    public void removerItem(int posicao) {
        listaQueryRows.remove(posicao);
        notifyItemRemoved(posicao);
        notifyItemRangeChanged(posicao, listaQueryRows.size());
    }

    public ArrayList<QueryRow> getListaQueryRows() {
        return listaQueryRows;
    }

    public void actualizar(String bostamp_) {
        for (int i = 0; i < listaQueryRows.size(); i++) {
            QueryRow queryRow = listaQueryRows.get(i);
            @SuppressWarnings("unchecked")
            List<Object> keys = (List<Object>) queryRow.getKey();
            String bostamp = keys.get(2).toString();
            if (bostamp.equals(bostamp_)) {
                final int pos = i;
                contextoActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        notifyItemChanged(pos);
                    }
                });

            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tv_fref;
        private final TextView tv_obrano;
        private final TextView tv_descricao;
        private final TextView tv_dtcortef;
        private final TextView tv_dttransf;
        private final TextView tv_qtt;
        private final TextView tv_qttfeita;
        private final LinearLayout ll_root;
        private final Button bt_posicao;
        private final Button bt_alertas;
        private final TextView tv_temporal;

        public ViewHolder(View itemView) {
            super(itemView);
            tv_fref = (TextView) itemView.findViewById(R.id.tv_fref);
            tv_obrano = (TextView) itemView.findViewById(R.id.tv_obrano);
            tv_descricao = (TextView) itemView.findViewById(R.id.tv_descricao);
            tv_dtcortef = (TextView) itemView.findViewById(R.id.tv_dtcortef);
            tv_dttransf = (TextView) itemView.findViewById(R.id.tv_dttransf);
            tv_qtt = (TextView) itemView.findViewById(R.id.tv_qtt);
            tv_qttfeita = (TextView) itemView.findViewById(R.id.tv_qttfeita);
            ll_root = (LinearLayout) itemView.findViewById(R.id.ll_root);

            bt_posicao = (Button) itemView.findViewById(R.id.bt_posicao);
            bt_alertas = (Button) itemView.findViewById(R.id.bt_alertas);

            tv_temporal = (TextView) itemView.findViewById(R.id.tv_temporal);
        }
    }
}
