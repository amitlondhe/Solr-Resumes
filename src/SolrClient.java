import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;

public class SolrClient {
	static final Set<String> UNIGRAM_SKILL_REFERENCE_LIST = new HashSet<String>(
			Arrays.asList("java", "ejb", "soap", "jsp", "servlet", "ooad",
					"ibatis", "hibernate", "jpa"
			,"orm","webservice","axis","jax-ws","jax-rs","restful","restlet","wsdl","javascript","dojo","yui","jquery","struts","tiles","junit",
 "spring", "tomcat", "weblogic", "websphere",
					"was", "mq", "uml", "json", "oracle", "mysql", "esql",
					"sql", ""));
	static final Set<String> BIGRAM_SKILL_REFERENCE_LIST = new HashSet<String>(
			Arrays.asList("java bean", "websphere mq", "rational rose",
					"message broker", "session bean", "design patterns",
					"apache poi", "adobe flex"));
	static final Set<String> TRIGRAM_SKILL_REFERENCE_LIST = new HashSet<String>(
			Arrays.asList("enterprise java bean", "message driven bean",
					"stateless session bean", "stateful session bean"));
	enum SKILL_GRAM {
		UNIGRAM(UNIGRAM_SKILL_REFERENCE_LIST),
		BIGRAM(BIGRAM_SKILL_REFERENCE_LIST),
		TRIGRAM(TRIGRAM_SKILL_REFERENCE_LIST);
		
		Set<String> masterList;

		SKILL_GRAM(Set<String> masterList) {
			this.masterList = masterList;
		}
		
		Set<String> masterList() {
			return this.masterList;
		}
	}
	
	public static void main(String args[]) throws Exception {
		readDocument();
		String str = "please divide this sentence into shingles";
		// parseDocument(str);
		// addDocument();
	}

	private static Set<String> parseDocument(String doc) throws IOException {
		Set<String> uniGramList = parseDocumentForUniGrams(doc);
		Set<String> biGramList = parseDocumentForNGrams(doc, 2);
		Set<String> triGramList = parseDocumentForNGrams(doc, 3);
		uniGramList.retainAll(SKILL_GRAM.UNIGRAM.masterList());
		biGramList.retainAll(SKILL_GRAM.BIGRAM.masterList());	
		triGramList.retainAll(SKILL_GRAM.TRIGRAM.masterList());		
		
		Set<String> skills = new HashSet<String>();
		skills.addAll(uniGramList);
		skills.addAll(biGramList);
		skills.addAll(triGramList);
		System.out.println("---list of skills----");
		System.out.println(skills);
		System.out.println("---list of skills----");
		return skills;
	}

	private static Set<String> parseDocumentForUniGrams(String in)
			throws IOException {
		Set<String> uniGramList = new HashSet<String>();
		Analyzer analyzer = new StandardAnalyzer();
		TokenStream stream = null;
		try {
			stream = analyzer.tokenStream("dummy", in);
			stream.reset();
			while (stream.incrementToken()) {
				CharTermAttribute attr = stream
						.getAttribute(CharTermAttribute.class);
				uniGramList.add(attr.toString());
			}
		} finally {
			analyzer.close();
			if (stream != null) {
				stream.end();
				stream.close();
			}
		}
		System.out.println("---UniGramSkillList-----");
		System.out.println(uniGramList);
		System.out.println("---UniGramSkillList-----");

		return uniGramList;
		
	}

	private static Set<String> parseDocumentForNGrams(String doc,
			int shingleSize) throws IOException {
		Set<String> nGramList = new HashSet<String>();
		Analyzer analyzer = new MyShingleAnalyzer(shingleSize);
		TokenStream stream = null;

		try {
			stream = analyzer.tokenStream("dummy", doc);
			stream.reset();
			while (stream.incrementToken()) {
				if (stream.hasAttribute(TypeAttribute.class)) {
					TypeAttribute type = stream
							.getAttribute(TypeAttribute.class);
					if (type.type().equals("shingle")) {
						CharTermAttribute attr = stream
								.getAttribute(CharTermAttribute.class);
						nGramList.add(attr.toString());
					}
				}
			}
			// System.out.println(nGramList);
		} finally {
			analyzer.close();
			if (stream != null) {
				stream.end();
				stream.close();
			}
		}
		return nGramList;
	}
	
	static class MyShingleAnalyzer extends Analyzer {
		int shingleSize = 2;

		MyShingleAnalyzer(int shingleSize) {
			this.shingleSize = shingleSize;
		}


		@Override
		protected TokenStreamComponents createComponents(String fieldName,
				Reader reader) {
		    WhitespaceTokenizer whitespaceTokenizer = new org.apache.lucene.analysis.core.WhitespaceTokenizer(reader);
		    final LowerCaseFilter lowerCaseFilter = new LowerCaseFilter(whitespaceTokenizer);
//		    StopFilter stopFilter = new StopFilter(source, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
				ShingleFilter shingleFilter = new ShingleFilter(
						lowerCaseFilter, shingleSize, shingleSize);
				return new TokenStreamComponents(whitespaceTokenizer,
						shingleFilter);
		}
	}

	private static void addDocument() {
		try {
			HttpSolrServer server = new HttpSolrServer(
					"http://localhost:8983/solr/");
			SolrPingResponse pingResponse = server.ping();
			System.out.println(pingResponse.getStatus());

			SolrInputDocument doc = new SolrInputDocument();
			doc.addField("id", "tsetstst3r4", 1.0f);
			doc.addField("name", "doc1", 1.0f);
			doc.addField("price", 10);
			server.add(doc);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String readDocument() throws Exception {
		String doc = StringUtils.EMPTY;
		File f = new File("data");
		// System.out.println(f.getCanonicalPath());
		String[] contents = f.list();
		for (String file : contents) {
			System.out.println("Processing file" + file);
			File f1 = new File("data/" + file);
			InputStream stream = new FileInputStream(f1);
			Tika tika = new Tika();
			Metadata metadata = new Metadata();
			try {
				doc = tika.parseToString(stream, metadata);
				doc = doc.substring(0, 10000);
				// System.out.println(doc);
				parseDocument(doc);
				for (String data : metadata.names()) {
					// System.out.println("Metadata Property - " + data + " - "
					// + metadata.get(data));
				}
			} finally {
				stream.close();
			}
		}
		return doc;
	}

}
