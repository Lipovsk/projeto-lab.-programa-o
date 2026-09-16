package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main{

    private static final double HARTREE_PARA_EV = 27.211386245988;

    public static void main(String[] args) {

        String arquivo = "ethanol.out";

        try {

            Resultado resultado = analisarArquivo(arquivo);

            System.out.println("===== RESULTADO =====");

            System.out.printf(
                    "HOMO: %.6f Hartree%n",
                    resultado.homo()
            );

            System.out.printf(
                    "LUMO: %.6f Hartree%n",
                    resultado.lumo()
            );

            System.out.printf(
                    "Gap: %.6f Hartree%n",
                    resultado.gapHartree()
            );

            System.out.printf(
                    "Gap: %.6f eV%n",
                    resultado.gapEv()
            );

        } catch (IOException e) {

            System.out.println(
                    "Erro durante a leitura do arquivo: "
                            + e.getMessage()
            );

        } catch (IllegalArgumentException e) {

            System.out.println(
                    "Arquivo Gaussian inválido: "
                            + e.getMessage()
            );
        }
    }


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


    record Resultado(
            double homo,
            double lumo,
            double gapHartree,
            double gapEv
    ) {
    }
}