public class Registradores {
    public int MAR = 0; 
    public int MDR = 0; 
    public int PC  = 0; 
    public int MBR = 0; 
    public int SP  = 0; 
    public int LV  = 0; 
    public int CPP = 0; 
    public int TOS = 0; 
    public int OPC = 0; 
    public int H   = 0; 
    
    public int AC = 0; 

    public int MPC = 0; 
    
    public boolean N = false; 
    public boolean Z = false; 

    public void reset() {
        MAR = MDR = PC = MBR = SP = LV = CPP = TOS = OPC = H = MPC = AC = 0;
        N = Z = false;
    }
}