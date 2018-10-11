package ch.heigvd.iict.dmg.labo1.queries;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.misc.HighFreqTerms;
import org.apache.lucene.misc.TermStats;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

public class QueriesPerformer {

    private Analyzer analyzer = null;
    private IndexReader indexReader = null;
    private IndexSearcher indexSearcher = null;

    public QueriesPerformer(Analyzer analyzer, Similarity similarity) {
        this.analyzer = analyzer;
        Path path = FileSystems.getDefault().getPath("index");
        Directory dir;
        try {
            dir = FSDirectory.open(path);
            this.indexReader = DirectoryReader.open(dir);
            this.indexSearcher = new IndexSearcher(indexReader);
            if (similarity != null)
                this.indexSearcher.setSimilarity(similarity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printTopRankingTerms(String field, int numTerms) {
        // This methods print the top ranking term for a field.
        try {

            TermStats[] foundTerms = HighFreqTerms.getHighFreqTerms(indexReader, numTerms, field,
                    new HighFreqTerms.TotalTermFreqComparator());

            System.out.println("\nMost frequent terms in " + field + " :");

            for (TermStats term : foundTerms) {
                System.out.println(term.totalTermFreq + " : " + term.termtext.utf8ToString());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void query(String q) {
        final int LIMIT = 20;

        QueryParser parser = new QueryParser("summary", analyzer);

        try {
            Query query = parser.parse(q);
            IndexSearcher searcher = new IndexSearcher(indexReader);
            searcher.search(query, 100);

            TopDocs results = searcher.search(query, LIMIT);
            ScoreDoc[] hits = results.scoreDocs;

            System.out.println("\nSearching for: " + q + " (" + results.totalHits + " results)");

            for (int i = 0; i < LIMIT && i < results.totalHits; ++i) {
                Document doc = searcher.doc(hits[i].doc);
                System.out.println(doc.get("id") + ": " + doc.get("title"));
            }

            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("\n");
    }

    public void close() {
        if (this.indexReader != null)
            try {
                this.indexReader.close();
            } catch (IOException e) { /* BEST EFFORT */ }
    }

}
