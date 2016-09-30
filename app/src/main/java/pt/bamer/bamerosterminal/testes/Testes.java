package pt.bamer.bamerosterminal.testes;


import android.util.Log;

import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Document;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;

import pt.bamer.bamerosterminal.couchbase.ServicoCouchBase;
@SuppressWarnings("unused")
public class Testes {
    private static final String TAG = Testes.class.getSimpleName();

    public static void eliminarTemposLocal() {
        Query query = ServicoCouchBase.getInstancia().viewTemposPorDossier.createQuery();
        try {
            QueryEnumerator queryEnumerator = query.run();
            if (queryEnumerator.getCount() == 0) {
                Log.w(TAG, "Não existem registos de tempo para eliminar.");
            }
            while (queryEnumerator.hasNext()) {
                QueryRow row = queryEnumerator.next();
                Document doc = row.getDocument();
                doc.delete();
                Log.e(TAG, "A eliminar o documento com o id " + doc.getId());
            }

        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }

    public static void eliminarProdsLocal() {
        try {
            QueryEnumerator query = ServicoCouchBase.getInstancia().viewOSPROD.createQuery().run();
            Log.w(TAG, "Existem " + query.getCount() + " registos OSPROD para eliminar!");
            for (QueryRow queryRow : query) {
                boolean delete = queryRow.getDocument().delete();
                if (!delete) {
                    Log.e(TAG, "Não eliminou OSPROD " + queryRow.getDocument().toString());
                }
                Log.w(TAG, "Eliminou o registo prod " + queryRow.getDocument().toString());
            }
        } catch (CouchbaseLiteException e) {
            e.printStackTrace();
        }
    }
}
