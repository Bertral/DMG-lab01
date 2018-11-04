package ch.heigvd.iict.dmg.labo2;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class Evaluation {

    private static void readFile(String filename, Function<String, Void> parseLine)
            throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(filename),
                        StandardCharsets.UTF_8)
        )) {
            String line = br.readLine();
            while (line != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    parseLine.apply(line);
                }
                line = br.readLine();
            }
        }
    }

    /*
     * Reading CACM queries and creating a list of queries.
     */
    private static List<String> readingQueries() throws IOException {
        final String QUERY_SEPARATOR = "\t";

        List<String> queries = new ArrayList<>();

        readFile("evaluation/query.txt", line -> {
            String[] query = line.split(QUERY_SEPARATOR);
            queries.add(query[1]);
            return null;
        });
        return queries;
    }

    /*
     * Reading stopwords
     */
    private static List<String> readingCommonWords() throws IOException {
        List<String> commonWords = new ArrayList<>();

        readFile("common_words.txt", line -> {
            commonWords.add(line);
            return null;
        });
        return commonWords;
    }


    /*
     * Reading CACM qrels and creating a map that contains list of relevant
     * documents per query.
     */
    private static Map<Integer, List<Integer>> readingQrels() throws IOException {
        final String QREL_SEPARATOR = ";";
        final String DOC_SEPARATOR = ",";

        Map<Integer, List<Integer>> qrels = new HashMap<>();

        readFile("evaluation/qrels.txt", line -> {
            String[] qrel = line.split(QREL_SEPARATOR);
            int query = Integer.parseInt(qrel[0]);

            List<Integer> docs = qrels.get(query);
            if (docs == null) {
                docs = new ArrayList<>();
            }

            String[] docsArray = qrel[1].split(DOC_SEPARATOR);
            for (String doc : docsArray) {
                docs.add(Integer.parseInt(doc));
            }

            qrels.put(query, docs);
            return null;
        });
        return qrels;
    }

    public static void main(String[] args) throws IOException {
        ///
        /// Reading queries and queries relations files
        ///
        List<String> queries = readingQueries();
        System.out.println("Number of queries: " + queries.size());

        Map<Integer, List<Integer>> qrels = readingQrels();
        System.out.println("Number of qrels: " + qrels.size());

        double avgQrels = 0.0;
        for (int q : qrels.keySet()) {
            avgQrels += qrels.get(q).size();
        }
        avgQrels /= qrels.size();
        System.out.println("Average number of relevant docs per query: " + avgQrels);

        //TODO student: use this when doing the english analyzer + common words
        List<String> commonWords = readingCommonWords();

        ///
        ///  Part I - Select an analyzer
        ///
        // TODO student: compare Analyzers here i.e. change analyzer to
        // the asked analyzers once the metrics have been implemented
        Analyzer analyzer = new StandardAnalyzer();


        ///
        ///  Part I - Create the index
        ///

        // For a given analyser, so one index per analyser
        Lab2Index lab2Index = new Lab2Index(analyzer);
        lab2Index.index("documents/cacm.txt");

        ///
        ///  Part II and III:
        ///  Execute the queries and assess the performance of the
        ///  selected analyzer using performance metrics like F-measure,
        ///  precision, recall,...
        ///

        // TODO student
        // Metrics to use (using mainly queryResults and qrelResults):
        // Summary stats (number of docs, retrieved docs etc)
        // MAP, Average R-precision, Average precision at standard recall levels,

        Map<Integer, List<Integer>> queryResults = new HashMap(queries.size());

        for (int i = 1; i <= queries.size(); ++i) {
            // Search results
            queryResults.put(i, lab2Index.search(queries.get(i - 1)));
        }


        // compute the metrics asked in the instructions
        // you may want to call these methods to get:
        // -  The query results returned by Lucene i.e. computed/empirical
        //    documents retrieved
        //        List<Integer> queryResults = lab2Index.search(query);
        //
        // - The true query results from qrels file i.e. genuine documents
        //   returned matching a query
        //        List<Integer> qrelResults = qrels.get(queryNumber);

        int totalRelevantDocs = 0;
        int totalRetrievedDocs = 0;
        int totalRetrievedRelevantDocs = 0;
        double avgRPrecision = 0.0;
        double meanAveragePrecision = 0.0;

        // average precision at the 11 recall levels (0,0.1,0.2,...,1) over all queries
        double[] avgPrecisionAtRecallLevels = createZeroedRecalls();

        for (int i = 1; i <= queryResults.size(); ++i) {
            // Number of retrieved docs is the size of the queryResult List
            totalRetrievedDocs += queryResults.get(i).size();
            // Number of relevant docs, can be null
            List<Integer> relevantDocsForQuery = qrels.get(i);

            // null check since some queries have no relevant docs
            if (relevantDocsForQuery != null) {
                totalRelevantDocs += relevantDocsForQuery.size();

                // Compare values of both lists to calculate retrieved relevant docs
                List<Integer> relevantDocsInSearch = queryResults.get(i);
                relevantDocsInSearch.retainAll(relevantDocsForQuery);
                totalRetrievedRelevantDocs += relevantDocsInSearch.size();
            }

            // Increment R-precision
            avgRPrecision += getRPrecision(queryResults.get(i), relevantDocsForQuery);

            // Increment Interpolated precision
            double[] interpolatedPrecisions = getInterpolatedPrecisions(queryResults.get(i), relevantDocsForQuery);
            for (int j = 0; j < avgPrecisionAtRecallLevels.length; ++j) {
                avgPrecisionAtRecallLevels[j] += interpolatedPrecisions[j];
            }

            // Increment MAP
            meanAveragePrecision += averagePrecision(queryResults.get(i), relevantDocsForQuery);

        }

        // Divide relevant metrics by number of queries to get the mean
        meanAveragePrecision /= queries.size();
        avgRPrecision /= queries.size();
        for(int i = 0; i < avgPrecisionAtRecallLevels.length; ++i){
            avgPrecisionAtRecallLevels[i] /= queries.size();
        }


        ///
        ///  Part IV - Display the metrics
        ///

        //TODO student implement what is needed (i.e. the metrics) to be able
        // to display the results
        displayMetrics(totalRetrievedDocs, totalRelevantDocs,
                totalRetrievedRelevantDocs,
                meanAveragePrecision, avgRPrecision,
                avgPrecisionAtRecallLevels);
    }

    private static double averagePrecision(List<Integer> searchResults, List<Integer> trueResults) {
        if (trueResults == null && searchResults.isEmpty()) { // No true results means precision of 1.0 if 0 search results (no false positives) or 0.0 if there were results.
                return 1.0;
            } else if (trueResults == null || searchResults.isEmpty()){
                return 0.0;
        } else {
            double precision = 0.0;
            int numberOfRanks = 0;
            for (int i = 1; i <= searchResults.size(); i++) {
                List<Integer> firstNResults = searchResults.subList(0, i);
                List<Integer> relevantInFirstNResults = new ArrayList<>(firstNResults);
                relevantInFirstNResults.retainAll(trueResults);
                precision += relevantInFirstNResults.size() / (double) firstNResults.size();
                numberOfRanks++;
            }
            // Average precision over each rank
            return precision/numberOfRanks;
        }
    }


    private static double getRPrecision(List<Integer> searchResults, List<Integer> trueResults) {
        if (trueResults == null) { // No true results means R-precision of 1
            return 1.0;
        } else {
            List<Integer> sublist;
            if (searchResults.size() >= trueResults.size()) {
                sublist = searchResults.subList(0, trueResults.size());
            } else {
                /*
                http://www.ccs.neu.edu/home/ekanou/ISU535.09X2/Handouts/Review_Material/evaluation.pdf
                "If this list contains less than R documents, we can always pad with
                 list with non-relevant documents and evaluate R-precision of this list. "
                 */
                sublist = searchResults;
            }
            //Keep only relevant docs
            sublist.retainAll(trueResults);
            // We simply divide by R to avoid having to pad with non relevant results
            return sublist.size() / trueResults.size();
        }
    }

    private static double[] getInterpolatedPrecisions(List<Integer> searchResults, List<Integer> trueResults) {
        double[] interpolatedPrecisions = createZeroedRecalls();
        if(trueResults == null){
            if(searchResults.isEmpty()){
                for(int i = 0; i < interpolatedPrecisions.length; ++i){
                    interpolatedPrecisions[i] = 1.0;
                }
            }
            return interpolatedPrecisions;
        }
        double recall = 0.0;
        double precision = 0.0;
        for (int i = 1; i <= searchResults.size(); i++) {
            List<Integer> firstNResults = searchResults.subList(0, i);
            List<Integer> relevantInFirstNResults = firstNResults;
            relevantInFirstNResults.retainAll(trueResults);
            recall = relevantInFirstNResults.size() / (double) trueResults.size();
            precision = relevantInFirstNResults.size() / (double) firstNResults.size();
            // Update max precisions for current recall point
            for (int level = 0; level <= 10 * Math.floor(recall); ++level) {
                if (interpolatedPrecisions[level] < precision) {
                    interpolatedPrecisions[level] = precision;
                }
            }
        }
        return interpolatedPrecisions;
    }

    private static void displayMetrics(
            int totalRetrievedDocs,
            int totalRelevantDocs,
            int totalRetrievedRelevantDocs,
            double meanAveragePrecision,
            double avgRPrecision,
            double[] avgPrecisionAtRecallLevels
    ) {
        System.out.println("Number of retrieved documents: " + totalRetrievedDocs);
        System.out.println("Number of relevant documents: " + totalRelevantDocs);
        System.out.println("Number of relevant documents retrieved: " + totalRetrievedRelevantDocs);

        System.out.println("MAP: " + meanAveragePrecision);

        System.out.println("Average R-Precision: " + avgRPrecision);

        System.out.println("Average precision at recall levels: ");
        for (int i = 0; i < avgPrecisionAtRecallLevels.length; i++) {
            System.out.println(String.format("\t%s: %s", i, avgPrecisionAtRecallLevels[i]));
        }
    }

    private static double[] createZeroedRecalls() {
        double[] recalls = new double[11];
        Arrays.fill(recalls, 0.0);
        return recalls;
    }
}

/*
 else { // No true results for that query
                avgRPrecision += 1.0; // See report for justification

            }
 */