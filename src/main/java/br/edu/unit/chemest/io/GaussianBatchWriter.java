package br.edu.unit.chemest.io;
import java.nio.file.*;
import java.io.*;
import java.util.*;
public final class GaussianBatchWriter {
    public void write(Path folder,Path destination) throws IOException {
        List<Path> files;
        try(var stream=Files.list(folder)) {
            files=stream.filter(Files::isRegularFile)
                .filter(p->p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gjf"))
                .sorted(Comparator.comparing(p->p.getFileName().toString())).toList();
        }
        if(files.isEmpty()) throw new IOException("A pasta não contém arquivos GJF.");
        StringBuilder content=new StringBuilder("!\n!batch file\n!start=1\n!\n");
        for(Path file:files) {
            String name=file.getFileName().toString();
            Path input=folder.resolve(name).toAbsolutePath().normalize();
            Path output=folder.resolve(name.substring(0,name.length()-4)+".out").toAbsolutePath().normalize();
            if(input.toString().matches("(?s).*[\r\n,].*")) throw new IOException("O caminho do lote não pode conter vírgulas ou quebras de linha.");
            content.append(input).append(", ").append(output).append("\n");
        }
        Files.writeString(destination,content);
    }
}
