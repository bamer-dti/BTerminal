package pt.bamer.bamerosterminal.pojos;

import org.json.JSONException;
import org.json.JSONObject;

import pt.bamer.bamerosterminal.MrApp;
import pt.bamer.bamerosterminal.couchbase.CamposCouch;

public class JSONObjectTimer extends JSONObject {
    private final int posicao;
    private final int posicaoNoArray;
    private String bostamp;

    public JSONObjectTimer(String bostamp, String bistamp, String estado, int posicao, int position) throws JSONException {
        long unixTime = System.currentTimeMillis() / 1000L;

        put(CamposCouch.FIELD_BOSTAMP, bostamp);
        put(CamposCouch.FIELD_BISTAMP, bistamp);
        put(CamposCouch.FIELD_MAQUINA, MrApp.getMaquina());
        put(CamposCouch.FIELD_OPERADOR, MrApp.getOperador());
        put(CamposCouch.FIELD_ESTADO, estado);
        put(CamposCouch.FIELD_POSICAO, "" + posicao);
        put(CamposCouch.FIELD_TIPO, CamposCouch.DOC_TIPO_TIMER);
        put(CamposCouch.FIELD_UNIXTIME, "" + unixTime);
        put(CamposCouch.FIELD_LASTTIME, "" + unixTime);
        put(CamposCouch.FIELD_SECCAO, MrApp.getSeccao());

        this.posicao = posicao;
        this.bostamp = bostamp;
        this.posicaoNoArray = position;
    }

    public int getPosicao() {
        return posicao;
    }

    public String getBostamp() {
        return bostamp;
    }

    public int getPosicaoNoArray() {
        return posicaoNoArray;
    }
}
