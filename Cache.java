import java.awt.Color;
import java.util.Map;

public class Cache {
    private int tamanho;
    private int[] dados;
    private int[] tags;
    private boolean[] valid;
    
    public int hits = 0;
    public int misses = 0;
    public String ultimoStatus = "Inativo";
    public Color ultimaCor = Color.GRAY;

    public Cache(int tamanho) {
        this.tamanho = tamanho;
        this.dados = new int[tamanho];
        this.tags = new int[tamanho];
        this.valid = new boolean[tamanho];
    }

    public int ler(int endereco, Map<Integer, Integer> ram) {
        int indice = endereco % tamanho;
        int tag = endereco / tamanho;

        if (valid[indice] && tags[indice] == tag) {
            hits++;
            ultimoStatus = "ACERTO (End " + endereco + ")";
            ultimaCor = Color.GREEN;
            return dados[indice];
        } else {
            misses++;
            ultimoStatus = "FALHA (End " + endereco + ")";
            ultimaCor = Color.RED;
            
            int valor = ram.getOrDefault(endereco, 0);
            
            valid[indice] = true;
            tags[indice] = tag;
            dados[indice] = valor;
            
            return valor;
        }
    }

    public void escrever(int endereco, int valor, Map<Integer, Integer> ram) {
        int indice = endereco % tamanho;
        int tag = endereco / tamanho;

        ram.put(endereco, valor);

        if (valid[indice] && tags[indice] == tag) {
            dados[indice] = valor;
            ultimoStatus = "ESCRITA ACERTO";
        } else {
            ultimoStatus = "ESCRITA FALHA";
        }
        ultimaCor = Color.ORANGE;
    }
    
    public void reset() {
        hits = 0;
        misses = 0;
        ultimoStatus = "Reiniciado";
        for(int i=0; i<tamanho; i++) valid[i] = false;
    }
}