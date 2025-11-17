package com.atividade;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

public class Main {

    private static final Random RANDOM = new Random();
    private static final int NUM_BITS = 5;
    private static final int MAX_GENERATIONS = 10;
    private static final int NUM_EXECUTIONS = 100;

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.US);

        Main main = new Main();

        int[] populationSizes = { 4, 8, 12 };

        double[][] resultsTable = new double[populationSizes.length][4];

        System.out.println("Gerando tabela e CSV...");

        for (int p = 0; p < populationSizes.length; p++) {

            int popSize = populationSizes[p];
            double[] bestFitnesses = new double[NUM_EXECUTIONS];

            for (int exec = 0; exec < NUM_EXECUTIONS; exec++) {
                GAResult r = main.runGeneticAlgorithm(popSize, false);
                bestFitnesses[exec] = r.bestFitness; // maior aptidão
            }

            double media = mean(bestFitnesses);
            double mediana = median(bestFitnesses);
            double melhor = max(bestFitnesses);
            double pior = min(bestFitnesses);

            resultsTable[p][0] = media;
            resultsTable[p][1] = mediana;
            resultsTable[p][2] = melhor;
            resultsTable[p][3] = pior;
        }

        main.saveResultsTableToCSV("resultados_populacoes.csv", populationSizes, resultsTable);
        System.out.println("✔ CSV gerado: resultados_populacoes.csv");

        // ================================
        // GRÁFICO E CSV DA POPULAÇÃO = 8
        // ================================
        System.out.println("Gerando gráfico e CSV da evolução...");

        GAResult evol = main.runGeneticAlgorithm(8, true);

        main.saveEvolutionToCSV("evolucao_pop8.csv", evol);
        System.out.println("✔ CSV gerado: evolucao_pop8.csv");

        main.generateEvolutionChart("grafico_pop8.png", evol);
        System.out.println("✔ Gráfico gerado: grafico_pop8.png");
    }

    // ================================================================
    // GAResult STRUCT
    // ================================================================

    static class GAResult {
        int bestX;
        double bestFitness;
        int[] bestXPerGeneration;
        double[] bestFxPerGeneration;
    }

    // ================================================================
    // METODO PRINCIPAL DO ALGORITMO GENÉTICO
    // ================================================================

    public GAResult runGeneticAlgorithm(int populationSize, boolean trackPerGen) {
        int[][] population = createPopulation(populationSize);

        int[] decodedX = decode(population);
        double[] fitness = evaluateFitness(decodedX);

        GAResult res = new GAResult();
        if (trackPerGen) {
            res.bestXPerGeneration = new int[MAX_GENERATIONS + 1];
            res.bestFxPerGeneration = new double[MAX_GENERATIONS + 1];
        }

        int bestIndex = indexOfBestFitness(fitness);
        int bestX = decodedX[bestIndex];
        double bestFitness = fitness[bestIndex];

        if (trackPerGen) {
            res.bestXPerGeneration[0] = bestX;
            res.bestFxPerGeneration[0] = f(bestX);
        }

        // Evolução
        for (int gen = 1; gen <= MAX_GENERATIONS; gen++) {

            int[][] newPop = new int[populationSize][NUM_BITS];

            for (int i = 0; i < populationSize; i += 2) {

                int p1 = tournament(fitness);
                int p2 = tournament(fitness);

                int[][] children = crossover(population[p1], population[p2]);
                mutate(children[0]);
                if (i + 1 < populationSize)
                    mutate(children[1]);

                newPop[i] = children[0];
                if (i + 1 < populationSize)
                    newPop[i + 1] = children[1];
            }

            population = newPop;
            decodedX = decode(population);
            fitness = evaluateFitness(decodedX);

            // Atualiza melhor global
            bestIndex = indexOfBestFitness(fitness);
            if (fitness[bestIndex] > bestFitness) {
                bestFitness = fitness[bestIndex];
                bestX = decodedX[bestIndex];
            }

            if (trackPerGen) {
                res.bestXPerGeneration[gen] = bestX;
                res.bestFxPerGeneration[gen] = f(bestX);
            }
        }

        res.bestFitness = bestFitness;
        res.bestX = bestX;
        return res;
    }

    // ================================================================
    // CRIAÇÃO E DECODIFICAÇÃO
    // ================================================================

    public int[][] createPopulation(int n) {
        int[][] pop = new int[n][NUM_BITS];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < NUM_BITS; j++)
                pop[i][j] = RANDOM.nextInt(2);
        return pop;
    }

    public int[] decode(int[][] pop) {
        int[] xs = new int[pop.length];
        for (int i = 0; i < pop.length; i++) {
            int sign = pop[i][0] == 1 ? 1 : -1;
            int val = 0;
            for (int b = 1; b < NUM_BITS; b++) {
                val += pop[i][b] * Math.pow(2, NUM_BITS - 1 - b);
            }
            xs[i] = sign * val;
        }
        return xs;
    }

    // ================================================================
    // AVALIAÇÃO E APTIDÃO
    // ================================================================

    public double f(int x) {
        return x * x - 4 * x + 4;
    }

    public double[] evaluateFitness(int[] decodedX) {
        double[] fit = new double[decodedX.length];
        for (int i = 0; i < decodedX.length; i++)
            fit[i] = -f(decodedX[i]); // aptidão = maximizar
        return fit;
    }

    public int indexOfBestFitness(double[] fitness) {
        int idx = 0;
        for (int i = 1; i < fitness.length; i++)
            if (fitness[i] > fitness[idx])
                idx = i;
        return idx;
    }

    // ================================================================
    // SELEÇÃO, CROSSOVER E MUTAÇÃO
    // ================================================================

    public int tournament(double[] fitness) {
        int a = RANDOM.nextInt(fitness.length);
        int b = RANDOM.nextInt(fitness.length);
        return fitness[a] > fitness[b] ? a : b;
    }

    public int[][] crossover(int[] p1, int[] p2) {
        int cut = 1 + RANDOM.nextInt(NUM_BITS - 1);

        int[][] kids = { Arrays.copyOf(p1, NUM_BITS), Arrays.copyOf(p2, NUM_BITS) };

        for (int i = cut; i < NUM_BITS; i++) {
            int tmp = kids[0][i];
            kids[0][i] = kids[1][i];
            kids[1][i] = tmp;
        }
        return kids;
    }

    public void mutate(int[] ind) {
        double rate = 0.05;
        if (RANDOM.nextDouble() < rate) {
            int g = RANDOM.nextInt(NUM_BITS);
            ind[g] = 1 - ind[g];
        }
    }

    // ================================================================
    // ESTATÍSTICAS
    // ================================================================

    public static double mean(double[] a) {
        double s = 0;
        for (double v : a)
            s += v;
        return s / a.length;
    }

    public static double median(double[] a) {
        double[] x = Arrays.copyOf(a, a.length);
        Arrays.sort(x);
        int mid = x.length / 2;
        return x.length % 2 == 1 ? x[mid] : (x[mid - 1] + x[mid]) / 2;
    }

    public static double max(double[] a) {
        double m = a[0];
        for (double v : a)
            if (v > m)
                m = v;
        return m;
    }

    public static double min(double[] a) {
        double m = a[0];
        for (double v : a)
            if (v < m)
                m = v;
        return m;
    }

    // ================================================================
    // SALVAR TABELA EM CSV
    // ================================================================

    public void saveResultsTableToCSV(String filename, int[] popSizes, double[][] table) throws IOException {
        FileWriter fw = new FileWriter(filename);

        fw.write(
                "Tamanho da População,Média da Melhor Aptidão,Mediana da Melhor Aptidão,Melhor das Melhores,Pior das Melhores\n");

        for (int i = 0; i < popSizes.length; i++) {
            fw.write(popSizes[i] + "," +
                    table[i][0] + "," +
                    table[i][1] + "," +
                    table[i][2] + "," +
                    table[i][3] + "\n");
        }
        fw.close();
    }

    // ================================================================
    // SALVAR EVOLUÇÃO PARA O GRÁFICO
    // ================================================================

    public void saveEvolutionToCSV(String filename, GAResult r) throws IOException {
        FileWriter fw = new FileWriter(filename);

        fw.write("Geracao,MelhorX,f(x),Aptidao\n");

        for (int g = 0; g < r.bestXPerGeneration.length; g++) {
            int x = r.bestXPerGeneration[g];
            double fx = r.bestFxPerGeneration[g];
            double fit = -fx;

            fw.write(g + "," + x + "," + fx + "," + fit + "\n");
        }
        fw.close();
    }

    // ================================================================
    // GERAR GRÁFICO AUTOMÁTICO
    // ================================================================

    public void generateEvolutionChart(String filename, GAResult r) throws Exception {

        double[] gen = new double[r.bestFxPerGeneration.length];
        double[] fx = new double[r.bestFxPerGeneration.length];

        for (int i = 0; i < gen.length; i++) {
            gen[i] = i;
            fx[i] = r.bestFxPerGeneration[i];
        }

        XYChart chart = new XYChartBuilder()
                .width(800)
                .height(600)
                .title("Evolução da Melhor f(x) por Geração (População = 8)")
                .xAxisTitle("Geração")
                .yAxisTitle("f(x)")
                .build();

        chart.addSeries("Melhor f(x)", gen, fx);

        BitmapEncoder.saveBitmap(chart, filename, BitmapEncoder.BitmapFormat.PNG);
    }
}
