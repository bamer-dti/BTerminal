package pt.bamer.bamerosterminal.pojos;

import org.json.JSONException;
import org.json.JSONObject;

import pt.bamer.bamerosterminal.couchbase.CamposCouch;

public class JSONObjectQtd extends JSONObject {
    public JSONObjectQtd(String bostamp, String dim, String mk, String ref, String design, int qttEfectuada) throws JSONException {
        put(CamposCouch.FIELD_DIM, dim);
        put(CamposCouch.FIELD_MK, mk);
        put(CamposCouch.FIELD_REF, ref);
        put(CamposCouch.FIELD_DESIGN, design);
        put(CamposCouch.FIELD_BOSTAMP, bostamp);
        put(CamposCouch.FIELD_QTT, qttEfectuada);
    }
}
