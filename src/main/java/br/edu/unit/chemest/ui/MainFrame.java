package br.edu.unit.chemest.ui;
import br.edu.unit.chemest.io.*;
import br.edu.unit.chemest.model.*;
import br.edu.unit.chemest.service.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class MainFrame extends JFrame {
    private final JTextField smiles=new JTextField("CCO",28), name=new JTextField("Molécula",20);
    private final JLabel image=new JLabel("Valide um SMILES para visualizar a estrutura 2D.",SwingConstants.CENTER);
    private final JLabel coordinateSource=new JLabel("Sem geometria 3D importada.");
    private final JLabel status=new JLabel("Pronto.");
    private final JTextArea log=new JTextArea();
    private final JTextField method=new JTextField("B3LYP"), basis=new JTextField("6-31G(d)"), job=new JTextField("Opt"), memory=new JTextField("2GB");
    private final JSpinner processors=new JSpinner(new SpinnerNumberModel(2,1,1024,1));
    private final JSpinner charge=new JSpinner(new SpinnerNumberModel(0,-100,100,1));
    private final JSpinner multiplicity=new JSpinner(new SpinnerNumberModel(1,1,100,1));
    private final List<JButton> buttons=new ArrayList<>();
    private final DefaultTableModel batchTable=table("NUM","SMILES","Status");
    private final DefaultTableModel analysisTable=table("Arquivo","HOMO (Hartree)","LUMO (Hartree)","Gap (Hartree)","Gap (eV)");
    private List<Atom3D> atoms=List.of();
    private List<CsvMoleculeReader.Row> rows=List.of();
    private final List<AnalysisRecord> results=new ArrayList<>();
    private Path folder;
    private List<Path> analysisFiles=List.of();
    private final JLabel batchSource=new JLabel("Selecione CSV e pasta com NUM.xyz.");
    private final JLabel analysisSource=new JLabel("Nenhum arquivo selecionado.");

    public MainFrame() {
        super("ChemEST Java");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(900,640));
        setSize(1040,760);
        setLocationRelativeTo(null);
        JTabbedPane tabs=new JTabbedPane();
        tabs.addTab("MOLÉCULA",moleculePanel());
        tabs.addTab("ENTRADA GAUSSIAN",configPanel());
        tabs.addTab("LOTE",batchPanel());
        tabs.addTab("ANÁLISE",analysisPanel());
        log.setEditable(false); log.setLineWrap(true); log.setWrapStyleWord(true);
        tabs.addTab("LOG",new JScrollPane(log));
        add(tabs,BorderLayout.CENTER); add(status,BorderLayout.SOUTH);
        smiles.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { invalidateGeometry(); }
            public void removeUpdate(DocumentEvent e) { invalidateGeometry(); }
            public void changedUpdate(DocumentEvent e) { invalidateGeometry(); }
        });
    }
    private static DefaultTableModel table(String... columns) {
        return new DefaultTableModel(columns,0) { public boolean isCellEditable(int row,int column) { return false; } };
    }
    private JPanel panel() { JPanel p=new JPanel(new BorderLayout(12,12)); p.setBorder(BorderFactory.createEmptyBorder(18,18,18,18)); return p; }
    private JPanel flow() { return new JPanel(new FlowLayout(FlowLayout.LEFT)); }
    private JButton button(String title,Runnable action) {
        JButton b=new JButton(title); buttons.add(b);
        b.addActionListener(e->{ try { action.run(); } catch(Exception ex) { error(title,"",ex); } }); return b;
    }
    private JPanel moleculePanel() {
        JPanel p=panel(), top=new JPanel(new GridLayout(0,1,6,6)), fields=flow(), actions=flow();
        fields.add(new JLabel("Nome:")); fields.add(name); fields.add(new JLabel("SMILES:")); fields.add(smiles);
        actions.add(button("Validar",this::validateSmiles)); actions.add(button("Importar coordenadas XYZ",this::importCoordinates));
        top.add(fields); top.add(actions); top.add(coordinateSource);
        p.add(top,BorderLayout.NORTH); p.add(new JScrollPane(image),BorderLayout.CENTER);
        p.add(new JLabel("Representação 2D. Para GJF, importe XYZ em angstroms com todos os átomos, incluindo H."),BorderLayout.SOUTH);
        return p;
    }
    private void invalidateGeometry() { atoms=List.of(); coordinateSource.setText("SMILES alterado: importe novamente a geometria."); image.setIcon(null); image.setText("Valide o SMILES."); }
    private void validateSmiles() {
        String input=smiles.getText();
        work("Validar SMILES",input,()->new SmilesService().depict(input),img->{image.setText(""); image.setIcon(new ImageIcon(img));});
    }
    private void importCoordinates() {
        new SmilesService().parse(smiles.getText());
        Path selected=choose(false,false,"xyz");
        if(selected==null)return;
        work("Importar XYZ",selected.toString(),()->new FileCoordinateProvider().read(selected),value->{
            atoms=value; coordinateSource.setText(value.size()+" átomos — "+selected);
        });
    }
    private JPanel configPanel() {
        JPanel p=panel(), form=new JPanel(new GridLayout(0,2,12,12));
        Object[][] fields={{"Método",method},{"Base",basis},{"Tarefa",job},{"Carga",charge},{"Multiplicidade",multiplicity},{"Memória",memory},{"Processadores",processors}};
        for(Object[] field:fields){form.add(new JLabel((String)field[0]));form.add((Component)field[1]);}
        p.add(form,BorderLayout.NORTH); JPanel actions=flow();
        actions.add(button("Gerar GJF",()->{
            new SmilesService().parse(smiles.getText());
            var molecule=new MoleculeRecord("molecule",name.getText(),smiles.getText(),atoms);
            if(atoms.isEmpty())throw new CoordinateException("A molécula não possui coordenadas 3D.");
            var config=config(); Path out=choose(true,false,"gjf"); if(out==null)return;
            work("Gerar GJF",out.toString(),()->{new GaussianInputWriter().write(molecule,config,out);return out;},value->{});
        }));
        actions.add(new JLabel("Gaussian/PySCF executam o cálculo quântico externamente."));
        p.add(actions,BorderLayout.SOUTH); return p;
    }
    private CalculationConfig config() {
        try { processors.commitEdit(); charge.commitEdit(); multiplicity.commitEdit(); }
        catch(java.text.ParseException e){throw new IllegalArgumentException("Informe números inteiros na configuração.");}
        return new CalculationConfig((int)processors.getValue(),memory.getText(),method.getText(),basis.getText(),job.getText(),(int)charge.getValue(),(int)multiplicity.getValue());
    }
    private JPanel batchPanel() {
        JPanel p=panel(), top=new JPanel(new GridLayout(0,1)), actions=flow();
        actions.add(button("Selecionar CSV",()->{
            Path selected=choose(false,false,"csv"); if(selected==null)return;
            work("Importar CSV",selected.toString(),()->new CsvMoleculeReader().read(selected),value->{
                rows=value; batchTable.setRowCount(0);
                for(var r:rows)batchTable.addRow(new Object[]{r.id(),r.smiles(),r.valid()?"Válido; aguardando XYZ":r.error()});
                long valid=rows.stream().filter(CsvMoleculeReader.Row::valid).count();
                batchSource.setText("CSV: "+selected.getFileName()+" — "+rows.size()+" processados; "+(rows.size()-valid)+" falharam. Pasta: "+folder);
            });
        }));
        actions.add(button("Selecionar pasta",()->{Path selected=choose(false,true,"");if(selected!=null){folder=selected;batchSource.setText("Pasta XYZ/GJF: "+folder);}}));
        actions.add(button("Gerar GJF do lote",()->{
            if(folder==null || rows.isEmpty())throw new IllegalArgumentException("Selecione CSV e pasta com NUM.xyz.");
            var config=config(); Path selected=folder; var input=rows;
            work("Gerar lote",selected.toString(),()->new BatchGenerationService().generate(input,selected,config),value->{
                batchTable.setRowCount(0);
                for(var r:value){batchTable.addRow(new Object[]{r.id(),r.smiles(),r.message()});writeLog("Lote",r.id(),r.success()?"Sucesso":r.message());}
                long success=value.stream().filter(BatchGenerationService.Status::success).count();
                batchSource.setText(value.size()+" processados; "+success+" gerados; "+(value.size()-success)+" falharam.");
            });
        }));
        actions.add(button("Gerar BCF",()->{
            if(folder==null)throw new IllegalArgumentException("Selecione a pasta dos GJF.");
            Path selected=folder,out=choose(true,false,"bcf"); if(out==null)return;
            work("Gerar BCF",out.toString(),()->{new GaussianBatchWriter().write(selected,out);return out;},value->{});
        }));
        top.add(actions);top.add(batchSource);p.add(top,BorderLayout.NORTH);p.add(new JScrollPane(new JTable(batchTable)),BorderLayout.CENTER);return p;
    }
    private JPanel analysisPanel() {
        JPanel p=panel(), top=new JPanel(new GridLayout(0,1)), actions=flow();
        actions.add(button("Selecionar OUT",()->{
            JFileChooser chooser=new JFileChooser();chooser.setMultiSelectionEnabled(true);
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Saída Gaussian","out","log","txt"));
            if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){
                analysisFiles=java.util.Arrays.stream(chooser.getSelectedFiles()).map(java.io.File::toPath).toList();
                analysisSource.setText(analysisFiles.size()+" arquivo(s) selecionado(s).");
            }
        }));
        actions.add(button("Analisar arquivos",()->{
            if(analysisFiles.isEmpty())throw new IllegalArgumentException("Selecione um arquivo de saída.");
            var selected=analysisFiles;
            work("Análise",selected.size()+" arquivo(s)",()->new AnalysisService().analyze(selected),outcomes->{
                int failures=0;
                for(var outcome:outcomes) {
                    if(outcome.record()==null){failures++;writeLog("Análise",outcome.source().toString(),outcome.error());continue;}
                    var r=outcome.record();results.add(r);
                    analysisTable.addRow(new Object[]{r.source().toString(),r.result().homoHartree(),r.result().lumoHartree(),r.result().gapHartree(),r.result().gapEv()});
                    writeLog("Análise",r.source().toString(),"Sucesso");
                }
                analysisSource.setText(outcomes.size()+" processados; "+failures+" falharam. Consulte a aba LOG.");
            });
        }));
        top.add(actions);top.add(analysisSource);p.add(top,BorderLayout.NORTH);p.add(new JScrollPane(new JTable(analysisTable)),BorderLayout.CENTER);
        JPanel exports=flow();
        exports.add(button("Exportar CSV",()->export(false)));
        exports.add(button("Exportar relatório HTML",()->export(true)));
        p.add(exports,BorderLayout.SOUTH);
        return p;
    }
    private void export(boolean html) {
        if(results.isEmpty())throw new IllegalArgumentException("Analise pelo menos um arquivo antes de exportar.");
        var snapshot=List.copyOf(results);
        var configuration=config();
        String notes=html?JOptionPane.showInputDialog(this,"Observações para o relatório:",""):"";
        if(html && notes==null)return;
        Path output=choose(true,false,html?"html":"csv"); if(output==null)return;
        work("Exportar "+(html?"HTML":"CSV"),output.toString(),()->{
            if(html)new br.edu.unit.chemest.report.HtmlReportWriter().write(snapshot,configuration,notes,output);
            else new ResultCsvWriter().write(snapshot,output);
            return output;
        },value->{});
    }
    private Path choose(boolean save,boolean directory,String extension) {
        JFileChooser chooser=new JFileChooser();
        if(directory)chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        else chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(extension.toUpperCase(),extension));
        int answer=save?chooser.showSaveDialog(this):chooser.showOpenDialog(this);
        if(answer!=JFileChooser.APPROVE_OPTION)return null;
        Path selected=chooser.getSelectedFile().toPath();
        if(save && !selected.toString().toLowerCase(java.util.Locale.ROOT).endsWith("."+extension))selected=Path.of(selected+"."+extension);
        if(save && Files.exists(selected) && JOptionPane.showConfirmDialog(this,"Substituir "+selected.getFileName()+"?","Arquivo existente",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return null;
        return selected;
    }
    private <T> void work(String operation,String file,Callable<T> task,Consumer<T> complete) {
        buttons.forEach(b->b.setEnabled(false));smiles.setEditable(false);status.setText(operation+" — em andamento...");
        new SwingWorker<T,Void>() {
            protected T doInBackground() throws Exception { return task.call(); }
            protected void done() {
                try { complete.accept(get()); writeLog(operation,file,"Concluído");status.setText(operation+" — concluído. Consulte o resultado e o LOG."); }
                catch(Exception e) { error(operation,file,e.getCause()==null?e:e.getCause()); }
                finally { buttons.forEach(b->b.setEnabled(true));smiles.setEditable(true); }
            }
        }.execute();
    }
    private void error(String operation,String file,Throwable error) {
        String message=error.getMessage()==null?"Não foi possível concluir a operação.":error.getMessage();
        writeLog(operation,file,"Erro: "+message);status.setText(operation+" — erro.");
        JOptionPane.showMessageDialog(this,message,"ChemEST Java",JOptionPane.ERROR_MESSAGE);
    }
    private void writeLog(String operation,String file,String outcome) {
        log.append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+" | "+operation+" | "+file+" | "+outcome+"\n");
    }
}



