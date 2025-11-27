public class MicroInstrucao {
    public int nextAddress; 
    public int jam;         
    public int alu;         
    public int cBus;        
    public int mem;         
    public int bBus;        

    public MicroInstrucao(int nextAddress, int jam, int alu, int cBus, int mem, int bBus) {
        this.nextAddress = nextAddress;
        this.jam = jam;
        this.alu = alu;
        this.cBus = cBus;
        this.mem = mem;
        this.bBus = bBus;
    }
    
    public static final int JAMZ = 4, JAMN = 2, JMPC = 1;
    public static final int WRITE = 2, READ = 1, FETCH = 4;
}