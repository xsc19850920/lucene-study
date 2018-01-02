package com.genpact.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexUpgrader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;

public class DemoDir {
	private static String content = "";
	private static Analyzer analyzer = null;
	private static Directory directory = null;
	private static String INDEX_DIR = Thread.currentThread().getContextClassLoader().getResource("luceneIndex").getPath();
	private static IndexWriter indexWriter = null;
	private static String DATA_DIR = Thread.currentThread().getContextClassLoader().getResource("luceneData").getPath();
	
	public static boolean createIndex(String path) {
		Date beginDate = new Date();
		List<File> fileList = getFileList(path);
		for (File file : fileList) {
			content = "";
			String ext = file.getName().substring(file.getName().lastIndexOf(".") + 1);
			if ("txt".equalsIgnoreCase(ext)) {
				content += txt2String(file);
			} else if ("doc".equalsIgnoreCase(ext)) {
				content += doc2String(file);
			} else if ("xls".equalsIgnoreCase(ext)) {
				content += xls2String(file);
			}
			System.out.println("{name:" + file.getName() + ",path:" + file.getPath() + "}");

			try {
				analyzer = new StandardAnalyzer();
				File indexDir = new File(INDEX_DIR);
				directory = FSDirectory.open(indexDir.toPath());
				if (!indexDir.exists()) {
					indexDir.mkdirs();
				}
				IndexWriterConfig config = new IndexWriterConfig(analyzer);
				indexWriter = new IndexWriter(directory, config);

				Document doc = new Document();
				doc.add(new TextField("filename", file.getName(), Store.YES));
				doc.add(new TextField("content", content, Store.YES));
				doc.add(new TextField("path", file.getPath(), Store.YES));
				indexWriter.addDocument(doc);
				indexWriter.commit();
				closeWriter();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

			content = "";
		}
		Date endDate = new Date();
		System.out.println("创建索引-----耗时：" + (endDate.getTime() - beginDate.getTime()) + "ms\n");
		return true;
	}

	public static void closeWriter() throws Exception {
		if (indexWriter != null) {
			indexWriter.close();
		}
	}

	private static String xls2String(File file) {
		String result = "";
		try {
			FileInputStream fis = new FileInputStream(file);
			StringBuilder sb = new StringBuilder();
			Workbook rwb = Workbook.getWorkbook(fis);
			Sheet[] sheet = rwb.getSheets();
			for (int i = 0; i < sheet.length; i++) {
				Sheet rs = rwb.getSheet(i);
				for (int j = 0; j < rs.getRows(); j++) {
					Cell[] cells = rs.getRow(j);
					for (int k = 0; k < cells.length; k++)
						sb.append(cells[k].getContents());
				}
			}
			fis.close();
			result += sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static String doc2String(File file) {
		String result = "";
		try {
			FileInputStream fis = new FileInputStream(file);
			HWPFDocument doc = new HWPFDocument(fis);
			Range rang = doc.getRange();
			result += rang.text();
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static String txt2String(File file) {
		String result = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String s = null;
			while ((s = br.readLine()) != null) {
				result = result + "\n" + s;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static List<File> getFileList(String path) {
		File[] files = new File(path).listFiles();
		List<File> fileList = new ArrayList<File>();
		for (File file : files) {
			if (isTxtFile(file.getName())) {
				fileList.add(file);
			}
		}
		return fileList;
	}

	private static boolean isTxtFile(String name) {
		if (name.lastIndexOf(".txt") > 0) {
			return true;
		} else if (name.lastIndexOf(".xls") > 0) {
			return true;
		} else if (name.lastIndexOf(".doc") > 0) {
			return true;
		}
		return false;
	}

	public static void searchIndex(String text) {
		Date date1 = new Date();
		try {
			File indexDir = new File(INDEX_DIR);
			directory = FSDirectory.open(indexDir.toPath());
			analyzer = new StandardAnalyzer();
			DirectoryReader ireader = DirectoryReader.open(directory);
			IndexSearcher isearcher = new IndexSearcher(ireader);

			QueryParser parser = new QueryParser("content", analyzer);
			Query query = parser.parse(text);

			ScoreDoc[] hits = isearcher.search(query, 1000).scoreDocs;

			for (int i = 0; i < hits.length; i++) {
				Document hitDoc = isearcher.doc(hits[i].doc);
				System.out.println("________________________________________________________");
				System.out.println(hitDoc.get("filename"));
				System.out.println(hitDoc.get("content"));
				System.out.println(hitDoc.get("path"));
				System.out.println("________________________________________________________");
			}
			ireader.close();
			directory.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Date date2 = new Date();
		System.out.println("查看索引-----耗时：" + (date2.getTime() - date1.getTime()) + "ms\n");
	}

	

	private static boolean deleteDir(File fileIndex) {
		if (fileIndex.isDirectory()) {
			File[] files = fileIndex.listFiles();
			for (int i = 0; i < files.length; i++) {
				deleteDir(files[i]);
			}
		}
		fileIndex.delete();
		return true;
	}
	
	
	public static void main(String[] args) {
//		File fileIndex = new File(INDEX_DIR);
//		if (deleteDir(fileIndex)) {
//			fileIndex.mkdir();
//		} else {
//			fileIndex.mkdir();
//		}
//
//		createIndex(DATA_DIR);
		searchIndex("容易");
		
	}
}
