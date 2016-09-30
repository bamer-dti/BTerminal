package pt.bamer.bamerosterminal.utils;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

import pt.bamer.bamerosterminal.ListaOS;
import pt.bamer.bamerosterminal.MrApp;
import pt.bamer.bamerosterminal.R;
import pt.bamer.bamerosterminal.adapters.OSRecyclerAdapter;
import pt.bamer.bamerosterminal.couchbase.ServicoCouchBase;


public class AsyncTasks {
    public static class TaskCalcularTempo extends android.os.AsyncTask<Void, Void, Void> {

        private final Button bt_posicao;
        private final String bostamp;
        private final TextView tv_temporal;
        private final Timer timer;
        private long tempoCalculado;
        private int posicaoSQL = 0;

        public TaskCalcularTempo(String bostamp, Button bt_posicao, TextView tv_temporal, Timer timer) {
            this.bostamp = bostamp;
            this.bt_posicao = bt_posicao;
            this.tv_temporal = tv_temporal;
            this.timer = timer;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            posicaoSQL = ListaOS.getPosicao(bostamp);
            tempoCalculado = ServicoCouchBase.getInstancia().getTempoTotal(bostamp);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (timer == null) {
                bt_posicao.setVisibility(View.VISIBLE);

            } else {
                bt_posicao.setVisibility(View.INVISIBLE);
            }
            String texto = bt_posicao.getContext().getString(R.string.iniciar_upper);
            texto = posicaoSQL == Constantes.MODO_STOPED ? bt_posicao.getContext().getString(R.string.continuar_upper) : texto;
            bt_posicao.setText(texto);
            String textoTempo = Funcoes.milisegundos_em_HH_MM_SS(tempoCalculado * 1000);
            tv_temporal.setText("" + (tempoCalculado == 0 ? "" : textoTempo));
        }
    }

    public static class TaskCalculoQtt extends android.os.AsyncTask<Void, Void, Void> {

        private final String bostamp;
        private final TextView tv_qtt;
        private final TextView tv_qttFeita;
        private final LinearLayout ll_root;
        private final OSRecyclerAdapter osRecyclerAdapter;
        private final int position;
        private List<Integer> listaTask;

        public TaskCalculoQtt(String bostamp, TextView tv_qtt, TextView tv_qttFeita, LinearLayout ll_root, OSRecyclerAdapter adapter, int position) {
            this.bostamp = bostamp;
            this.tv_qtt = tv_qtt;
            this.tv_qttFeita = tv_qttFeita;
            this.ll_root = ll_root;
            this.osRecyclerAdapter = adapter;
            this.position = position;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<Integer> lista = new ArrayList<>();
            lista.add(ServicoCouchBase.getInstancia().getPecasPorOS(bostamp));
            lista.add(ServicoCouchBase.getInstancia().getPecasFeitasPorOS(bostamp));
            listaTask = lista;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            tv_qtt.setText("" + listaTask.get(0));
            tv_qttFeita.setText("" + (listaTask.get(1) == 0 ? "" : listaTask.get(1)));
            if (listaTask.get(0).equals(listaTask.get(1))) {
                ll_root.setBackgroundColor(ContextCompat.getColor(ll_root.getContext(), R.color.md_blue_grey_500));
                SharedPreferences prefs = MrApp.getPrefs();
                boolean mostra = prefs.getBoolean(Constantes.PREF_MOSTRAR_OS_COMPLETOS, true);
                if (!mostra) {
                    osRecyclerAdapter.getListaQueryRows().remove(position);
                    osRecyclerAdapter.notifyItemRemoved(position);
                    osRecyclerAdapter.notifyItemRangeChanged(position, osRecyclerAdapter.getListaQueryRows().size());
                }
            } else {
                ll_root.setBackgroundColor(Color.WHITE);
            }
        }
    }
}
