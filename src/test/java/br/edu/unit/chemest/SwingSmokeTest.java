package br.edu.unit.chemest;
import br.edu.unit.chemest.ui.MainFrame;
import org.junit.jupiter.api.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.Callable;
import static org.junit.jupiter.api.Assertions.*;
class SwingSmokeTest {
    private <T> T edt(Callable<T> callable) throws Exception {
        var task=new java.util.concurrent.FutureTask<T>(callable);
        SwingUtilities.invokeAndWait(task);return task.get();
    }
    private <T extends Component> java.util.List<T> find(Container root,Class<T> type) {
        var found=new java.util.ArrayList<T>();
        for(Component component:root.getComponents()){
            if(type.isInstance(component))found.add(type.cast(component));
            if(component instanceof Container child)found.addAll(find(child,type));
        }
        return found;
    }
    private void await(Callable<Boolean> condition) throws Exception {
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<deadline){if(edt(condition))return;Thread.sleep(40);}
        fail("A operação Swing não terminou no prazo.");
    }
    @Test @Timeout(30) void appStartsValidatesAndSurvivesInvalidSmiles() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),"Requer ambiente gráfico.");
        App.main(new String[0]);
        await(()->java.util.Arrays.stream(Window.getWindows()).anyMatch(w->w instanceof MainFrame && w.isShowing()));
        MainFrame frame=edt(()->(MainFrame)java.util.Arrays.stream(Window.getWindows()).filter(w->w instanceof MainFrame).findFirst().orElseThrow());
        try {
            assertEquals(5,edt(()->find(frame,JTabbedPane.class).getFirst().getTabCount()));
            JButton validate=edt(()->find(frame,JButton.class).stream().filter(b->b.getText().equals("Validar")).findFirst().orElseThrow());
            JTextField smiles=edt(()->find(frame,JTextField.class).stream().filter(t->t.getText().equals("CCO")).findFirst().orElseThrow());
            edt(()->{validate.doClick();return null;});
            await(()->validate.isEnabled() && find(frame,JLabel.class).stream().anyMatch(l->l.getIcon() instanceof ImageIcon));
            edt(()->{
                BufferedImage image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics=image.createGraphics();frame.paint(graphics);graphics.dispose();
                Files.createDirectories(Path.of("target"));javax.imageio.ImageIO.write(image,"png",Path.of("target/swing-smoke.png").toFile());
                smiles.setText("C1(");validate.doClick();return null;
            });
            await(()->java.util.Arrays.stream(Window.getWindows()).anyMatch(w->w instanceof JDialog && w.isShowing()));
            edt(()->{for(Window w:Window.getWindows())if(w instanceof JDialog)w.dispose();return null;});
            await(validate::isEnabled);
            assertTrue(edt(frame::isShowing));
            edt(()->{smiles.setText("CCO");validate.doClick();return null;});
            await(()->validate.isEnabled() && find(frame,JLabel.class).stream().anyMatch(l->l.getIcon() instanceof ImageIcon));
        } finally { edt(()->{for(Window w:Window.getWindows())w.dispose();return null;}); }
    }
}
