package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HomoLumoAnalyzer {
    private static final double HARTREE_PARA_EV = 27.211386245988;

    public static Resultado analisarArquivo(String caminho)
            throws IOException {

        double homoFinal = Double.NaN;
        double lumoFinal = Double.NaN;

        double homoTemporario = Double.NaN;

        boolean aguardandoLumo = false;

        Path arquivo = Path.of(caminho);

        if (!Files.exists(arquivo)) {
            throw new IllegalArgumentException(
                    "O arquivo informado não existe."
            );
        }

        try (
                BufferedReader leitor =
                        Files.newBufferedReader(
                                arquivo,
                                StandardCharsets.ISO_8859_1
                        )
        ) {

            String linha;

            while ((linha = leitor.readLine()) != null) {

                if (linha.contains(
                        "Alpha  occ. eigenvalues --"
                )) {

                    homoTemporario =
                            ultimoNumeroDepoisDoSeparador(linha);

                    aguardandoLumo = true;
                }

                else if (
                        linha.contains(
                                "Alpha virt. eigenvalues --"
                        )
                                && aguardandoLumo
                ) {

                    homoFinal = homoTemporario;

                    lumoFinal =
                            primeiroNumeroDepoisDoSeparador(linha);

                    aguardandoLumo = false;
                }
            }
        }

        if (
                Double.isNaN(homoFinal)
                        || Double.isNaN(lumoFinal)
        ) {

            throw new IllegalArgumentException(
                    "Valores HOMO/LUMO não encontrados."
            );
        }

        double gapHartree = lumoFinal - homoFinal;

        double gapEv =
                gapHartree * HARTREE_PARA_EV;

        return new Resultado(
                homoFinal,
                lumoFinal,
                gapHartree,
                gapEv
        );
    }


    private static double ultimoNumeroDepoisDoSeparador(
            String linha
    ) {

        String valores =
                linha.substring(
                        linha.indexOf("--") + 2
                ).trim();

        String[] numeros =
                valores.split("\\s+");

        return Double.parseDouble(
                numeros[numeros.length - 1]
        );
    }


    private static double primeiroNumeroDepoisDoSeparador(
            String linha
    ) {

        String valores =
                linha.substring(
                        linha.indexOf("--") + 2
                ).trim();

        String[] numeros =
                valores.split("\\s+");

        return Double.parseDouble(
                numeros[0]
        );
    }


}
