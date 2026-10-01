package br.edu.unit.chemest;
import br.edu.unit.chemest.ui.MainFrame;
import javax.swing.*;
public final class App {
    public static void main(String[] args) throws Exception {
        if(args.length>0 && args[0].equals("--web")) { new br.edu.unit.chemest.experimental.LocalServer().start(); return; }
        SwingUtilities.invokeLater(()->{
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch(Exception ignored) { /* Usar aparência padrão disponível. */ }
            new MainFrame().setVisible(true);
        });
    }
}
