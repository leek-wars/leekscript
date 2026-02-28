package test;

import leekscript.runner.AI;
import leekscript.runner.LeekRunException;
import leekscript.runner.Session;
import leekscript.runner.values.ArrayLeekValue;

public class BenchRAM {
    
    public static void main() {

        var ai = new AI(0, 4) {
            @Override
            public Object runIA(Session session) throws LeekRunException {
                Long t = System.nanoTime();
                int I = 1000000;
                int S = 1000;
                for (int i = 0; i < I; ++i) {

                    var a = new ArrayLeekValue(this);
                    for (int j = 0; j < S; ++j) {
                        a.push(this, j);
                    }
                    if (i % 100000 == 0)
                    System.out.println("RAM:" + this.getUsedRAM());
                }
                System.out.println("RAM:" + this.getUsedRAM());
                System.out.println("Time: " + (System.nanoTime() - t) / 1_000_000 + "s");
                return null;
            }
        };

        try {
            ai.runIA();
        } catch (LeekRunException e) {
            e.printStackTrace();
        }
    }
}
