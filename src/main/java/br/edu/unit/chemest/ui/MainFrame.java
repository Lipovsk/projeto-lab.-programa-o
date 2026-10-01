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
    private final JLabel image=new JLabel("Valide um SMILES para visualizar a estrutura 2D.",SwingConstants.CENTER) {
        protected void paintComponent(Graphics graphics) {
            if (!(getIcon() instanceof ImageIcon icon)) { super.paintComponent(graphics); return; }
            Graphics2D canvas=(Graphics2D)graphics.create();
            try {
                canvas.setColor(getBackground());canvas.fillRect(0,0,getWidth(),getHeight());
                double scale=Math.min(3.0,Math.min((getWidth()-24.0)/icon.getIconWidth(),(getHeight()-24.0)/icon.getIconHeight()));
                int width=Math.max(1,(int)(icon.getIconWidth()*scale)),height=Math.max(1,(int)(icon.getIconHeight()*scale));
                canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                canvas.drawImage(icon.getImage(),(getWidth()-width)/2,(getHeight()-height)/2,width,height,this);
            } finally { canvas.dispose(); }
        }
    };
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
    private int operationFailures;
    private List<Path> analysisFiles=List.of();
    private final JLabel batchSource=new JLabel("Selecione CSV e pasta com NUM.xyz.");
    private final JLabel analysisSource=new JLabel("Nenhum arquivo selecionado.");
    private final JLabel csvPath=new JLabel("Nenhum CSV selecionado."), folderPath=new JLabel("Nenhuma pasta selecionada.");
    private final JLabel[] orbitalValues=new JLabel[4];

    public MainFrame() {
        super("ChemEST Java");
        UiTheme.install();
        SwingUtilities.updateComponentTreeUI(this);
        for(JComponent field: new JComponent[]{smiles,name,method,basis,job,memory,processors,charge,multiplicity}) { SwingUtilities.updateComponentTreeUI(field);UiTheme.field(field); }
        for(JComponent label:new JComponent[]{coordinateSource,batchSource,analysisSource,csvPath,folderPath}) { label.setFont(UiTheme.BODY);label.setForeground(UiTheme.TEXT_PRIMARY); }
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(900,640));
        setSize(1120,820);
        setLocationRelativeTo(null);
        JTabbedPane tabs=new JTabbedPane();
        tabs.setFont(UiTheme.BODY.deriveFont(Font.BOLD));
        tabs.setBackground(UiTheme.BACKGROUND);
        tabs.addTab("MOLÉCULA",moleculePanel());
        tabs.addTab("ENTRADA GAUSSIAN",configPanel());
        tabs.addTab("LOTE",batchPanel());
        tabs.addTab("ANÁLISE",analysisPanel());
        log.setEditable(false); log.setLineWrap(true); log.setWrapStyleWord(true);
        log.setBackground(UiTheme.CONSOLE); log.setForeground(UiTheme.TEXT_PRIMARY); log.setFont(UiTheme.MONO);
        log.setBorder(BorderFactory.createEmptyBorder(12,14,12,14));
        JPanel logPanel=panel(); logPanel.add(UiTheme.card("Console de operações",UiTheme.scroll(log)),BorderLayout.CENTER);
        tabs.addTab("LOG",logPanel);
        JPanel header=panel(); header.setBorder(BorderFactory.createEmptyBorder(16,24,14,24));
        header.add(UiTheme.label("ChemEST Java",UiTheme.TITLE,UiTheme.TEXT_PRIMARY),BorderLayout.NORTH);
        header.add(UiTheme.label("Química Computacional e Estrutura Eletrônica",UiTheme.AUXILIARY,UiTheme.TEXT_SECONDARY),BorderLayout.CENTER);
        JPanel footer=UiTheme.panel(new BorderLayout());
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1,0,0,0,UiTheme.BORDER),BorderFactory.createEmptyBorder(10,24,10,24)));
        setStatus("Pronto",UiTheme.TEXT_SECONDARY); footer.add(status);
        getContentPane().setBackground(UiTheme.BACKGROUND);
        add(header,BorderLayout.NORTH); add(tabs,BorderLayout.CENTER); add(footer,BorderLayout.SOUTH);
        smiles.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { invalidateGeometry(); }
            public void removeUpdate(DocumentEvent e) { invalidateGeometry(); }
            public void changedUpdate(DocumentEvent e) { invalidateGeometry(); }
        });
    }
    private static DefaultTableModel table(String... columns) {
        return new DefaultTableModel(columns,0) { public boolean isCellEditable(int row,int column) { return false; } };
    }
    private JPanel panel() { JPanel p=UiTheme.panel(new BorderLayout(UiTheme.GAP,UiTheme.GAP)); p.setBorder(BorderFactory.createEmptyBorder(UiTheme.PADDING,UiTheme.PADDING,UiTheme.PADDING,UiTheme.PADDING)); return p; }
    private JPanel flow() { JPanel p=UiTheme.panel(new FlowLayout(FlowLayout.LEFT,UiTheme.GAP,6)); p.setOpaque(false); return p; }
    private JPanel stack() { JPanel p=UiTheme.panel(new GridLayout(0,1,UiTheme.GAP,UiTheme.GAP)); p.setOpaque(false); return p; }
    private JPanel fields(Object[][] entries) {
        JPanel form=UiTheme.panel(new GridBagLayout()); form.setOpaque(false);
        for(int i=0;i<entries.length;i++) {
            GridBagConstraints c=new GridBagConstraints(); c.gridx=0;c.gridy=i;c.anchor=GridBagConstraints.WEST;c.insets=new Insets(5,0,5,18);
            form.add(UiTheme.label((String)entries[i][0],UiTheme.BODY,UiTheme.TEXT_SECONDARY),c);
            c.gridx=1;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(5,0,5,0);
            form.add((Component)entries[i][1],c);
        }
        return form;
    }
    private void setStatus(String text,Color color) { status.setText("● "+text);status.setForeground(color);status.setFont(UiTheme.BODY); }
    private JButton button(String title,Runnable action) {
        JButton b=new JButton(title); buttons.add(b);
        UiTheme.button(b,title.equals("ANALISAR") || title.startsWith("GERAR") || title.equals("Gerar GJF do lote"));
        b.addActionListener(e->{ try { action.run(); } catch(Exception ex) { error(title,"",ex); } }); return b;
    }
    private JPanel moleculePanel() {
        JPanel p=panel(), top=UiTheme.panel(new BorderLayout(12,12)), data=UiTheme.panel(new BorderLayout(12,12)), actions=flow();
        data.setOpaque(false);
        data.add(fields(new Object[][]{{"Nome",name},{"SMILES",smiles}}),BorderLayout.CENTER);
        actions.add(button("Validar SMILES",this::validateSmiles)); actions.add(button("Importar coordenadas XYZ",this::importCoordinates));
        data.add(actions,BorderLayout.SOUTH);
        top.add(UiTheme.card("Dados da molécula",data),BorderLayout.CENTER);
        coordinateSource.setText("● Não importada"); coordinateSource.setForeground(UiTheme.TEXT_SECONDARY);
        top.add(UiTheme.card("Geometria 3D",coordinateSource),BorderLayout.SOUTH);
        image.setForeground(UiTheme.TEXT_SECONDARY); image.setOpaque(true); image.setBackground(UiTheme.SURFACE);
        image.setPreferredSize(new Dimension(600,260));
        p.add(top,BorderLayout.NORTH); p.add(UiTheme.card("Representação 2D",image),BorderLayout.CENTER);
        p.add(UiTheme.label("Para gerar GJF, importe XYZ em angstroms com todos os átomos, incluindo H.",UiTheme.AUXILIARY,UiTheme.TEXT_SECONDARY),BorderLayout.SOUTH);
        return p;
    }
    private void invalidateGeometry() { atoms=List.of(); coordinateSource.setText("● SMILES alterado: importe novamente a geometria."); coordinateSource.setForeground(UiTheme.TEXT_SECONDARY); coordinateSource.setToolTipText(null); image.setIcon(null); image.setText("Valide o SMILES."); }
    private void validateSmiles() {
        String input=smiles.getText();
        work("Validar SMILES",input,()->new SmilesService().depict(input),img->{image.setText(""); image.setIcon(UiTheme.depictionIcon(img));});
    }
    private void importCoordinates() {
        new SmilesService().parse(smiles.getText());
        Path selected=choose(false,false,"xyz");
        if(selected==null)return;
        work("Importar XYZ",selected.toString(),()->new FileCoordinateProvider().read(selected),value->{
            atoms=value; coordinateSource.setText("● "+value.size()+" átomos importados — "+selected);coordinateSource.setToolTipText(selected.toString());coordinateSource.setForeground(UiTheme.SUCCESS);
        });
    }
    private JPanel configPanel() {
        JPanel p=panel(), form=stack();
        form.add(UiTheme.card("Configuração do cálculo",fields(new Object[][]{{"Método",method},{"Base",basis},{"Tarefa",job}})));
        JPanel groups=UiTheme.panel(new GridLayout(1,2,12,12));
        groups.add(UiTheme.card("Recursos computacionais",fields(new Object[][]{{"Memória",memory},{"Processadores",processors}})));
        groups.add(UiTheme.card("Estado molecular",fields(new Object[][]{{"Carga",charge},{"Multiplicidade",multiplicity}})));
        form.add(groups);
        p.add(form,BorderLayout.NORTH); JPanel actions=flow();
        actions.add(button("GERAR ARQUIVO GJF",()->{
            new SmilesService().parse(smiles.getText());
            var molecule=new MoleculeRecord("molecule",name.getText(),smiles.getText(),atoms);
            if(atoms.isEmpty())throw new CoordinateException("A molécula não possui coordenadas 3D.");
            var config=config(); Path out=choose(true,false,"gjf"); if(out==null)return;
            work("Gerar GJF",out.toString(),()->{new GaussianInputWriter().write(molecule,config,out);return out;},value->{});
        }));
        actions.add(UiTheme.label("Gaussian/PySCF executam o cálculo quântico externamente.",UiTheme.AUXILIARY,UiTheme.TEXT_SECONDARY));
        p.add(actions,BorderLayout.SOUTH); return p;
    }
    private CalculationConfig config() {
        try { processors.commitEdit(); charge.commitEdit(); multiplicity.commitEdit(); }
        catch(java.text.ParseException e){throw new IllegalArgumentException("Informe números inteiros na configuração.");}
        return new CalculationConfig((int)processors.getValue(),memory.getText(),method.getText(),basis.getText(),job.getText(),(int)charge.getValue(),(int)multiplicity.getValue());
    }
    private JPanel batchPanel() {
        JPanel p=panel(), top=UiTheme.panel(new BorderLayout(12,12)), selection=stack(), actions=flow(), generation=flow();
        actions.add(button("Selecionar CSV",()->{
            Path selected=choose(false,false,"csv"); if(selected==null)return;
            work("Importar CSV",selected.toString(),()->new CsvMoleculeReader().read(selected),value->{
                rows=value; batchTable.setRowCount(0);
                for(var r:rows)batchTable.addRow(new Object[]{r.id(),r.smiles(),r.valid()?"Válido; aguardando XYZ":r.error()});
                operationFailures=(int)rows.stream().filter(r->!r.valid()).count();
                long valid=rows.stream().filter(CsvMoleculeReader.Row::valid).count();
                batchSource.setText("CSV: "+selected.getFileName()+" — "+rows.size()+" processados; "+(rows.size()-valid)+" falharam. Pasta: "+(folder==null?"nenhuma selecionada":folder));
                csvPath.setText(selected.toString());csvPath.setToolTipText(selected.toString());
            });
        }));
        actions.add(button("Selecionar pasta",()->{Path selected=choose(false,true,"");if(selected!=null){folder=selected;batchSource.setText("Pasta XYZ/GJF: "+folder);folderPath.setText(folder.toString());folderPath.setToolTipText(folder.toString());}}));
        generation.add(button("Gerar GJF do lote",()->{
            if(folder==null || rows.isEmpty())throw new IllegalArgumentException("Selecione CSV e pasta com NUM.xyz.");
            var config=config(); Path selected=folder; var input=rows;
            work("Gerar lote",selected.toString(),()->new BatchGenerationService().generate(input,selected,config),value->{
                batchTable.setRowCount(0);
                for(var r:value){batchTable.addRow(new Object[]{r.id(),r.smiles(),r.message()});writeLog("Lote",r.id(),r.success()?"Sucesso":r.message());}
                long success=value.stream().filter(BatchGenerationService.Status::success).count();
                operationFailures=(int)(value.size()-success);
                batchSource.setText(value.size()+" processados; "+success+" gerados; "+(value.size()-success)+" falharam.");
            });
        }));
        generation.add(button("Gerar BCF",()->{
            if(folder==null)throw new IllegalArgumentException("Selecione a pasta dos GJF.");
            Path selected=folder,out=choose(true,false,"bcf"); if(out==null)return;
            work("Gerar BCF",out.toString(),()->{new GaussianBatchWriter().write(selected,out);return out;},value->{});
        }));
        selection.add(actions);selection.add(fields(new Object[][]{{"CSV selecionado",csvPath},{"Pasta XYZ / GJF",folderPath}}));
        JPanel summary=UiTheme.panel(new BorderLayout(12,6));summary.add(generation,BorderLayout.NORTH);summary.add(batchSource,BorderLayout.SOUTH);
        batchSource.setForeground(UiTheme.TEXT_SECONDARY);
        top.add(UiTheme.card("Arquivos do lote",selection),BorderLayout.CENTER);top.add(summary,BorderLayout.SOUTH);
        p.add(top,BorderLayout.NORTH);p.add(UiTheme.scroll(UiTheme.table(batchTable,false)),BorderLayout.CENTER);return p;
    }
    private JPanel analysisPanel() {
        JPanel p=panel(), top=UiTheme.panel(new BorderLayout(12,12)), selection=UiTheme.panel(new BorderLayout(12,8)), actions=flow();
        selection.setOpaque(false);
        JPanel heading=UiTheme.panel(new BorderLayout(0,6));
        heading.add(UiTheme.label("ANÁLISE HOMO-LUMO",UiTheme.SECTION,UiTheme.TEXT_PRIMARY),BorderLayout.NORTH);
        heading.add(UiTheme.label("Importe arquivos de saída Gaussian para extrair os orbitais de fronteira.",UiTheme.AUXILIARY,UiTheme.TEXT_SECONDARY),BorderLayout.SOUTH);
        actions.add(button("Selecionar arquivos de saída (.out, .log, .txt)",()->{
            JFileChooser chooser=new JFileChooser();chooser.setMultiSelectionEnabled(true);
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Saída Gaussian (.out, .log, .txt)","out","log","txt"));
            if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION){
                analysisFiles=java.util.Arrays.stream(chooser.getSelectedFiles()).map(java.io.File::toPath).toList();
                analysisSource.setText(analysisFiles.size()+" arquivo(s) selecionado(s).");
                analysisSource.setToolTipText(analysisFiles.toString());
            }
        }));
        actions.add(button("ANALISAR",()->{
            if(analysisFiles.isEmpty())throw new IllegalArgumentException("Selecione um arquivo de saída.");
            var selected=analysisFiles;
            work("Análise",selected.size()+" arquivo(s)",()->new AnalysisService().analyze(selected),this::displayAnalysis);
        }));
        analysisSource.setForeground(UiTheme.TEXT_SECONDARY);
        selection.add(actions,BorderLayout.NORTH);selection.add(analysisSource,BorderLayout.SOUTH);
        top.add(heading,BorderLayout.NORTH);top.add(UiTheme.card("Arquivo(s) selecionado(s)",selection),BorderLayout.CENTER);
        JPanel metrics=UiTheme.panel(new GridLayout(1,4,12,12));
        String[] titles={"HOMO","LUMO","GAP","GAP eV"};
        for(int i=0;i<4;i++) {
            JPanel value=UiTheme.panel(new BorderLayout(0,6));value.setOpaque(false);
            orbitalValues[i]=UiTheme.label("—",UiTheme.MONO.deriveFont(Font.BOLD,20f),UiTheme.TEXT_PRIMARY);
            value.add(orbitalValues[i],BorderLayout.CENTER);
            value.add(UiTheme.label(i==3?"eV":"Hartree",UiTheme.AUXILIARY,UiTheme.TEXT_SECONDARY),BorderLayout.SOUTH);
            JPanel metric=UiTheme.card(titles[i],value);metric.setToolTipText("Último resultado válido analisado");metrics.add(metric);
        }
        top.add(metrics,BorderLayout.SOUTH);p.add(top,BorderLayout.NORTH);p.add(UiTheme.scroll(UiTheme.table(analysisTable,true)),BorderLayout.CENTER);
        JPanel exports=flow();
        exports.add(button("Exportar CSV",()->export(false)));
        exports.add(button("Gerar relatório HTML",()->export(true)));
        p.add(exports,BorderLayout.SOUTH);
        return p;
    }
    private void displayAnalysis(List<AnalysisService.Outcome> outcomes) {
        results.clear(); analysisTable.setRowCount(0);
        for(JLabel label:orbitalValues) { label.setText("—");label.setToolTipText(null); }
        int failures=0;
        for(var outcome:outcomes) {
            if(outcome.record()==null){failures++;writeLog("Análise",outcome.source().toString(),outcome.error());continue;}
            var r=outcome.record();results.add(r);
            analysisTable.addRow(new Object[]{r.source().toString(),r.result().homoHartree(),r.result().lumoHartree(),r.result().gapHartree(),r.result().gapEv()});
            updateOrbitals(r.result());
            writeLog("Análise",r.source().toString(),"Sucesso");
        }
        operationFailures=failures;
        analysisSource.setText(outcomes.size()+" processados; "+failures+" falharam. Consulte a aba LOG.");
    }
    private void updateOrbitals(OrbitalResult result) {
        double[] values={result.homoHartree(),result.lumoHartree(),result.gapHartree(),result.gapEv()};
        for(int i=0;i<values.length;i++) {
            orbitalValues[i].setText(String.format(java.util.Locale.ROOT,"%.6f",values[i]));
            orbitalValues[i].setToolTipText(Double.toString(values[i]));
        }
    }
    private void export(boolean html) {
        if(results.isEmpty())throw new IllegalArgumentException("Analise pelo menos um arquivo antes de exportar.");
        var snapshot=List.copyOf(results);
        var configuration=html?config():null;
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
        operationFailures=0;
        buttons.forEach(b->b.setEnabled(false));smiles.setEditable(false);setStatus(operation+" — em andamento...",UiTheme.PRIMARY_HOVER);
        new SwingWorker<T,Void>() {
            protected T doInBackground() throws Exception { return task.call(); }
            protected void done() {
                if(!isDisplayable())return;
                try {
                    complete.accept(get());
                    String outcome=operationFailures==0?"Concluído":"Concluído com "+operationFailures+" falha(s)";
                    writeLog(operation,file,outcome);
                    setStatus(operation+" — "+outcome+". Consulte o resultado e o LOG.",operationFailures==0?UiTheme.SUCCESS:UiTheme.ERROR);
                }
                catch(Exception e) { error(operation,file,e.getCause()==null?e:e.getCause()); }
                finally { buttons.forEach(b->b.setEnabled(true));smiles.setEditable(true); }
            }
        }.execute();
    }
    private void error(String operation,String file,Throwable error) {
        String message=error.getMessage()==null?"Não foi possível concluir a operação.":error.getMessage();
        writeLog(operation,file,"Erro: "+message);setStatus(operation+" — erro.",UiTheme.ERROR);
        JOptionPane.showMessageDialog(this,message,"ChemEST Java",JOptionPane.ERROR_MESSAGE);
    }
    private void writeLog(String operation,String file,String outcome) {
        log.append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))+" | "+operation+" | "+file+" | "+outcome+"\n");
    }
}



