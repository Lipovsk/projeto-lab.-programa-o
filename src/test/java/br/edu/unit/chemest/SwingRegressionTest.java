package br.edu.unit.chemest;
import br.edu.unit.chemest.ui.*;
import br.edu.unit.chemest.model.*;
import br.edu.unit.chemest.service.AnalysisService;
import org.junit.jupiter.api.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(20)
class SwingRegressionTest {
    MainFrame frame;
    <T> T edt(Callable<T> action) throws Exception {
        var task=new FutureTask<T>(action);SwingUtilities.invokeAndWait(task);return task.get();
    }
    @BeforeEach void create() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),"Requer ambiente gráfico.");
        frame=edt(()->{var window=new MainFrame();window.setVisible(true);return window;});
    }
    @AfterEach void close() throws Exception {
        if(frame!=null)edt(()->{for(Window window:Window.getWindows())window.dispose();return null;});
    }
    <T> T field(String name,Class<T> type) throws Exception {
        var field=MainFrame.class.getDeclaredField(name);field.setAccessible(true);return type.cast(field.get(frame));
    }
    Object call(String name,Class<?>[] types,Object... args) throws Exception {
        var method=MainFrame.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(frame,args);
    }
    void await(Callable<Boolean> condition) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(System.nanoTime()<deadline){if(edt(condition))return;Thread.sleep(40);}
        fail("Operação Swing não terminou.");
    }
    List<AnalysisService.Outcome> valid() {
        Path source=Path.of("sample.out");
        return List.of(new AnalysisService.Outcome(source,new AnalysisRecord(source,new OrbitalResult(-.25,-.05)),""));
    }
    void display(List<AnalysisService.Outcome> outcomes) throws Exception {call("displayAnalysis",new Class<?>[]{List.class},outcomes);}
    @Test void repeatAnalysisReplacesResultsAndFailureClearsOldMetrics() throws Exception {
        edt(()->{
            display(valid());display(valid());
            assertEquals(1,field("results",List.class).size());
            assertEquals(1,field("analysisTable",javax.swing.table.DefaultTableModel.class).getRowCount());
            display(List.of(new AnalysisService.Outcome(Path.of("missing.out"),null,"Arquivo ausente")));
            assertTrue(field("results",List.class).isEmpty());
            assertEquals(0,field("analysisTable",javax.swing.table.DefaultTableModel.class).getRowCount());
            assertEquals("—",field("orbitalValues",JLabel[].class)[0].getText());return null;
        });
    }
    @Test void failedAnalysisShowsFailureStatusInsteadOfSuccess() throws Exception {
        Callable<List<AnalysisService.Outcome>> task=()->List.of(new AnalysisService.Outcome(Path.of("missing.out"),null,"Arquivo ausente"));
        Consumer<List<AnalysisService.Outcome>> complete=outcomes->{try{display(outcomes);}catch(Exception e){throw new RuntimeException(e);}};
        edt(()->{call("work",new Class<?>[]{String.class,String.class,Callable.class,Consumer.class},"Análise","missing.out",task,complete);return null;});
        await(()->field("status",JLabel.class).getText().contains("1 falha"));
        assertEquals(UiTheme.ERROR,edt(()->field("status",JLabel.class).getForeground()));
    }
    @Test void closedWindowDoesNotApplyBackgroundCompletion() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var finished=new CountDownLatch(1);
        var completed=new AtomicBoolean();
        Callable<String> task=()->{entered.countDown();release.await();finished.countDown();return "done";};
        Consumer<String> complete=value->completed.set(true);
        try {
            edt(()->{call("work",new Class<?>[]{String.class,String.class,Callable.class,Consumer.class},"Test","",task,complete);return null;});
            assertTrue(entered.await(5,TimeUnit.SECONDS));
            edt(()->{frame.dispose();return null;});release.countDown();assertTrue(finished.await(5,TimeUnit.SECONDS));
            Thread.sleep(150);edt(()->{assertFalse(completed.get());return null;});
        } finally {release.countDown();}
    }
    @Test void csvExportDoesNotRequireGaussianConfiguration() throws Exception {
        edt(()->{display(valid());field("method",JTextField.class).setText("");return null;});
        SwingUtilities.invokeLater(()->{try{call("export",new Class<?>[]{boolean.class},false);}catch(Exception e){throw new RuntimeException(e);}});
        await(()->java.util.Arrays.stream(Window.getWindows()).anyMatch(w->w instanceof JDialog && w.isShowing()));
        edt(()->{
            JFileChooser chooser=null;
            for(Window window:Window.getWindows())if(window instanceof JDialog dialog && dialog.isShowing()) {
                var option=find(dialog,JFileChooser.class);if(option!=null)chooser=option;
            }
            assertNotNull(chooser,"CSV deve abrir o seletor de destino mesmo com método vazio.");
            chooser.cancelSelection();return null;
        });
    }
    <T extends Component> T find(Container root,Class<T> type) {
        for(Component component:root.getComponents()) {
            if(type.isInstance(component))return type.cast(component);
            if(component instanceof Container child){T found=find(child,type);if(found!=null)return found;}
        }
        return null;
    }
}
