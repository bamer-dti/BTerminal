package pt.bamer.bamerosterminal.pojos;

import org.joda.time.LocalDateTime;

import pt.bamer.bamerosterminal.utils.Funcoes;

///**
// * Created by miguel.silva on 18-07-2016.
// */
public class ArtigoOSBO {
    private final String bostamp;
    private final int obrano;
    private final String fref;
    private final String nmfref;
    private final String estado;
    private final String seccao;
    private final String obs;
    private LocalDateTime dtcortef;
    private final LocalDateTime dttransf;
    private final LocalDateTime dtembala;
    private final LocalDateTime dtexpedi;
    private int ordem;

    public ArtigoOSBO(String bostamp, int obrano, String fref, String nmfref, String estado, String seccao, String obs, String dtcortef, String dttransf, String dtembala, String dtexpedi, int ordem) {
        this.bostamp = bostamp;
        this.obrano = obrano;
        this.fref = fref;
        this.nmfref = nmfref;
        this.estado = estado;
        this.seccao = seccao;
        this.obs = obs;
        this.dtcortef = Funcoes.cToT(dtcortef);
        this.dttransf = Funcoes.cToT(dttransf);
        this.dtembala = Funcoes.cToT(dtembala);
        this.dtexpedi = Funcoes.cToT(dtexpedi);
        this.ordem = ordem;
    }

    public String getBostamp() {
        return bostamp;
    }

    public int getObrano() {
        return obrano;
    }

    public String getFref() {
        return fref;
    }

    public String getNmfref() {
        return nmfref;
    }

    public String getEstado() {
        return estado;
    }

    public String getSeccao() {
        return seccao;
    }

    public String getObs() {
        return obs;
    }

    public LocalDateTime getDtcortef() {
        return dtcortef;
    }

    public LocalDateTime getDttransf() {
        return dttransf;
    }

    public LocalDateTime getDtembala() {
        return dtembala;
    }

    public LocalDateTime getDtexpedi() {
        return dtexpedi;
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }

    public void setDtcortef(LocalDateTime dtcorte) {
        this.dtcortef = dtcorte;
    }
}
