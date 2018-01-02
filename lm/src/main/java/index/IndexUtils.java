package index;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.wltea.analyzer.lucene.IKAnalyzer;

public class IndexUtils {

    //0. 创建增量索引
    public static void buildIndex(String indexFile, String storeIdFile) {
        IncrementIndex.buildIndex(indexFile, storeIdFile);
    }

    //1. 单字段查询
    public static List<IndexResult> queryByOneKey(IndexSearcher indexSearcher,
    												String field,
    												String key) {
        try {
            Date date1 = new Date();
            QueryParser queryParser = new QueryParser(field, new StandardAnalyzer());
            Query query = queryParser.parse(key);
            ScoreDoc[] hits = indexSearcher.search(query,1000).scoreDocs;
            Date date2 = new Date();
            System.out.println("耗时：" + (date2.getTime() - date1.getTime()) + "ms");
            List<IndexResult> list = new ArrayList<IndexResult>();
            for (int i = 0; i < hits.length; i++) {
                list.add(getIndexResult(indexSearcher.doc(hits[i].doc)));
            }
            return list;
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //2. 多条件查询。这里实现的是and操作
    //注：要查询的字段必须是index的
    //即doc.add(new Field("pid", rs.getString("pid"), Field.Store.YES,Field.Index.TOKENIZED));   
    @SuppressWarnings("deprecation")
    public static List<IndexResult> queryByMultiKeys(IndexSearcher indexSearcher, String[] fields,
            String[] keys) {

        try {
            BooleanQuery m_BooleanQuery = new BooleanQuery();
            if (keys != null && keys.length > 0) {
                for (int i = 0; i < keys.length; i++) {
                    QueryParser queryParser = new QueryParser(fields[i], new StandardAnalyzer());
                    Query query = queryParser.parse(keys[i]);
                    m_BooleanQuery.add(query, BooleanClause.Occur.MUST);//and操作
                }
                 ScoreDoc[] hits = indexSearcher.search(m_BooleanQuery,1000).scoreDocs;
                List<IndexResult> list = new ArrayList<IndexResult>();
                for (int i = 0; i < hits.length; i++) {
                    list.add(getIndexResult(indexSearcher.doc(hits[i].doc)));
                }
                return list;
            }
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //3.高亮显示  实现了单条件查询
    //可改造为多条件查询
    public static List<IndexResult> highlight(IndexSearcher indexSearcher, String key) throws InvalidTokenOffsetsException {
        try {
            QueryParser queryParser = new QueryParser("name", new StandardAnalyzer());
            Query query = queryParser.parse(key);
            TopDocsCollector<ScoreDoc> collector = new TopDocsCollector<ScoreDoc>(null) {

				public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
					return null;
				}

				public boolean needsScores() {
					return false;
				}
			}; 
            indexSearcher.search(query, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            Highlighter highlighter = null;
            SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("<font color='red'>", "</font>");
            highlighter = new Highlighter(simpleHTMLFormatter, new QueryScorer(query));
            highlighter.setTextFragmenter(new SimpleFragmenter(200));
            List<IndexResult> list = new ArrayList<IndexResult>();
            Document doc;
            StandardAnalyzer analyzer = new StandardAnalyzer();
            for (int i = 0; i < hits.length; i++) {
                //System.out.println(hits[i].score);
                doc = indexSearcher.doc(hits[i].doc);
                TokenStream tokenStream = analyzer.tokenStream("name",
                        new StringReader(doc.get("name")));
                IndexResult ir = getIndexResult(doc);
                ir.setName(highlighter.getBestFragment(tokenStream, doc.get("name")));
                list.add(ir);
            }
            analyzer.close();
            return list;
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    //4. 多字段查询
    public static List<IndexResult> queryByMultiFileds(IndexSearcher indexSearcher,
            String[] fields, String key) {
        try {
            MultiFieldQueryParser mfq = new MultiFieldQueryParser(fields, new StandardAnalyzer());
            Query query = mfq.parse(key);
             ScoreDoc[] hits = indexSearcher.search(query,1000).scoreDocs;
            List<IndexResult> list = new ArrayList<IndexResult>();
            for (int i = 0; i < hits.length; i++) {
                list.add(getIndexResult(indexSearcher.doc(hits[i].doc)));
            }

            return list;
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //5. 删除索引
    public static void deleteIndex(String indexFile, String id) throws CorruptIndexException,
            IOException {
    	Analyzer luceneAnalyzer = new StandardAnalyzer();
        IndexWriterConfig config  = new IndexWriterConfig(luceneAnalyzer);
        config.setUseCompoundFile(true);
        File indexDir = new File(indexFile);
        Directory dir =  FSDirectory.open(indexDir.toPath());
        IndexWriter writer = new IndexWriter(dir, config);
        writer.deleteDocuments(new Term(id));
        writer.close();
    }

    //6. 一元分词
    public static String Standard_Analyzer(String str) {
        Analyzer analyzer = new StandardAnalyzer();
        StringReader r = new StringReader(str);
        StopFilter sf = (StopFilter) analyzer.tokenStream("", r);
        CharTermAttribute term=sf.getAttribute(CharTermAttribute.class);  
        System.out.println("=====StandardAnalyzer====");
        System.out.println("分析方法：默认没有词只有字（一元分词）");
        String results = "";
        try {
        	while (sf.incrementToken()) {
                System.out.println(term.toString());
                results = results + " " + term.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        analyzer.close();
        return results;
    }

    //7. 字典分词
    public static String ik_CAnalyzer(String str) {
        Analyzer analyzer = new IKAnalyzer(true);
        StringReader  r = new StringReader(str);
        TokenStream ts = analyzer.tokenStream("", r);
        CharTermAttribute term=ts.getAttribute(CharTermAttribute.class);  
        System.out.println("=====IK_CAnalyzer====");
        System.out.println("分析方法:字典分词,正反双向搜索");
        String results = "";
        try {
            while (ts.incrementToken()) {
                System.out.println(term.toString());
                results = results + " " + term.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        analyzer.close();
        return results;
    }

    //在结果中搜索
    public static void queryFromResults() {

    }

    //组装对象
    public static IndexResult getIndexResult(Document doc) {
        IndexResult ir = new IndexResult();
        ir.setId(doc.get("id"));
        ir.setName(doc.get("name"));
        ir.setAddress(doc.get("address"));
        ir.setCitycode(doc.get("citycode"));
        return ir;
    }
}
