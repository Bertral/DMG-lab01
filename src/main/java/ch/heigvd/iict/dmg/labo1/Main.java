package ch.heigvd.iict.dmg.labo1;

import ch.heigvd.iict.dmg.labo1.indexer.CACMIndexer;
import ch.heigvd.iict.dmg.labo1.parsers.CACMParser;
import ch.heigvd.iict.dmg.labo1.queries.QueriesPerformer;
import ch.heigvd.iict.dmg.labo1.similarities.MySimilarity;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.Similarity;

import java.util.HashMap;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        // 1.1. create an analyzer
        Analyzer analyser = getAnalyzer();

        Similarity similarity = new MySimilarity();
        CACMIndexer indexer = new CACMIndexer(analyser, similarity);
        indexer.openIndex();
        CACMParser parser = new CACMParser("documents/cacm.txt", indexer);

        long time = System.currentTimeMillis();
        parser.startParsing(); // parse and index
        System.out.println("Index built in " + (System.currentTimeMillis() - time) + " ms");

        indexer.finalizeIndex(); // write to disk

        QueriesPerformer queriesPerformer = new QueriesPerformer(analyser, similarity);

        // Section "Reading Index"
        readingIndex(queriesPerformer);

        // Section "Searching"
        searching(queriesPerformer);

        queriesPerformer.close();

    }

    private static void readingIndex(QueriesPerformer queriesPerformer) {
        queriesPerformer.printTopRankingTerms("authors", 1);
        queriesPerformer.printTopRankingTerms("title", 10);
    }

    private static void searching(QueriesPerformer queriesPerformer) {
        queriesPerformer.query("\"Information Retrieval\"");
        queriesPerformer.query("Information Retrieval");
        queriesPerformer.query("Information +Retrieval -Database");
        queriesPerformer.query("Info*");
        queriesPerformer.query("\"Information Retrieval\"~5");
    }

    private static Analyzer getAnalyzer() {
        // "Using different Analyzers" tests :
//        return new StandardAnalyzer();
//        return new WhitespaceAnalyzer();
//        return new EnglishAnalyzer();
//        return new ShingleAnalyzerWrapper(new StandardAnalyzer(), 2, 2);
//        return new ShingleAnalyzerWrapper(new StandardAnalyzer(), 3, 3);
//        return new StopAnalyzer();

        // treat the author field as a single word
        Map<String, Analyzer> analyzerMap = new HashMap<>();
        analyzerMap.put("authors", new KeywordAnalyzer());
        return new PerFieldAnalyzerWrapper(
                new EnglishAnalyzer(), analyzerMap);
    }

}
